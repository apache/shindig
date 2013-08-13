/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.servlet;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Parser;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.PipelineMaker;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.LoaderType;
import com.google.caja.plugin.UriEffect;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.plugin.UriFetcher.UriFetchException;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.service.ServiceMessageType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A GadgetRewriter based on caja technology
 */
public class CajaContentRewriter implements GadgetRewriter {
  public static final String CAJOLED_MODULES = "cajoledModules";

  //class name for logging purpose
  private static final String CLASS_NAME = CajaContentRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(CLASS_NAME, MessageKeys.MESSAGES);


  private final Cache<ModuleCacheKey, ImmutableList<Job>> moduleCache;
  private final RequestPipeline requestPipeline;
  private final HtmlSerializer htmlSerializer;
  private final ProxyUriManager proxyUriManager;

  @Inject
  public CajaContentRewriter(CacheProvider cacheProvider, RequestPipeline requestPipeline,
                             HtmlSerializer htmlSerializer, ProxyUriManager proxyUriManager) {
    if (null == cacheProvider) {
      this.moduleCache = null;
    } else {
      this.moduleCache = cacheProvider.createCache(CAJOLED_MODULES);
    }
    if (LOG.isLoggable(Level.FINE)) {
      LOG.logp(Level.FINE, CLASS_NAME, "CajaContentRewriter", MessageKeys.CAJOLED_CACHE_CREATED,
               new Object[] {moduleCache});
    }
    this.requestPipeline = requestPipeline;
    this.htmlSerializer = htmlSerializer;
    this.proxyUriManager = proxyUriManager;
  }

  public class CajoledResult {
    public final Node html;
    public final CajoledModule js;
    public final List<Message> messages;
    public final boolean hasErrors;
    CajoledResult(Node html, CajoledModule js, List<Message> messages, boolean hasErrors) {
      this.html = html;
      this.js = js;
      this.messages = messages;
      this.hasErrors= hasErrors;
    }
    @Override
    public String toString() {
      return "[html:'" + html + "', js: '" + js + "', messages: '" + messages + "']";
    }
  }

  @VisibleForTesting
  static ParseTreeNode parse(InputSource is, CharProducer cp, String mime, MessageQueue mq)
      throws ParseException {
    ParseTreeNode ptn;
    if (mime.contains("javascript")) {
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(lexer, is);
      if (tq.isEmpty()) { return null; }
      Parser p = new Parser(tq, mq);
      ptn = p.parse();
      tq.expectEmpty();
    } else {
      DomParser p = new DomParser(new HtmlLexer(cp), false, is, mq);
      ptn = new Dom(p.parseFragment());
      p.getTokenQueue().expectEmpty();
    }
    return ptn;
  }

  public CajoledResult rewrite(Uri uri, String container, String mime,
      boolean es53, boolean debug) {
    URI javaUri = uri.toJavaUri();
    InputSource is = new InputSource(javaUri);
    MessageQueue mq = new SimpleMessageQueue();
    try {
      UriFetcher fetcher = makeFetcher(uri, container);
      ExternalReference extRef = new ExternalReference(javaUri,
          FilePosition.instance(is, /*lineNo*/ 1, /*charInFile*/ 1, /*charInLine*/ 1));
      // If the fetch fails, a UriFetchException is thrown and serialized as part of the
      // message queue.
      CharProducer cp = fetcher.fetch(extRef, mime).getTextualContent();
      ParseTreeNode ptn = parse(is, cp, mime, mq);
      return rewrite(uri, container, ptn, es53, debug);
    } catch (UnsupportedEncodingException e) {
      LOG.severe("Unexpected inability to recognize mime type: " + mime);
      mq.addMessage(ServiceMessageType.UNEXPECTED_INPUT_MIME_TYPE,
          MessagePart.Factory.valueOf(mime));
    } catch (UriFetchException e) {
      LOG.info("Failed to retrieve: " + e.toString());
    } catch (ParseException e) {
      mq.addMessage(MessageType.PARSE_ERROR, FilePosition.UNKNOWN);
    }
    return new CajoledResult(null, null, mq.getMessages(), /* hasErrors */ true);
  }

  public CajoledResult rewrite(Uri gadgetUri, String container,
      ParseTreeNode root, boolean es53, boolean debug) {
    UriFetcher fetcher = makeFetcher(gadgetUri, container);
    UriPolicy policy = makePolicy(gadgetUri);
    URI javaGadgetUri = gadgetUri.toJavaUri();
    MessageQueue mq = new SimpleMessageQueue();
    MessageContext context = new MessageContext();
    PluginMeta meta = new PluginMeta(fetcher, policy);
    PluginCompiler compiler = makePluginCompiler(meta, mq);
    compiler.setMessageContext(context);
    if (moduleCache != null) {
      compiler.setJobCache(new ModuleCache(moduleCache));
    }

    if (debug) {
      compiler.setGoals(compiler.getGoals()
          .without(PipelineMaker.ONE_CAJOLED_MODULE)
          .with(PipelineMaker.ONE_CAJOLED_MODULE_DEBUG));
    }

    compiler.addInput(root, javaGadgetUri);

    boolean hasErrors = false;
    if (!compiler.run()) {
      hasErrors = true;
    }

    return new CajoledResult(compiler.getStaticHtml(),
        compiler.getJavascript(),
        compiler.getMessageQueue().getMessages(),
        hasErrors);
  }

  public void rewrite(Gadget gadget, MutableContent mc) {
    if (!gadget.requiresCaja()) return;

    GadgetContext gadgetContext = gadget.getContext();
    boolean debug = gadgetContext.getDebug();
    Document doc = mc.getDocument();

    // Serialize outside of MutableContent, to prevent a re-parse.
    String docContent = HtmlSerialization.serialize(doc);
    DocumentFragment root = doc.createDocumentFragment();
    root.appendChild(doc.getDocumentElement());

    if (debug) {
      gadget.addFeature("caja-debug");
    }

    InputSource is = new InputSource(gadgetContext.getUrl().toJavaUri());
    CajoledResult result =
      rewrite(gadgetContext.getUrl(), gadgetContext.getContainer(),
          new Dom(root), true, debug);

    if (result.hasErrors) {
      // Content is only used to produce useful snippets with error messages
      List<Message> messages = result.messages;
      createContainerFor(doc,
          formatErrors(doc, is, docContent, messages, true /* visible */));
      mc.documentChanged();
      logException("rewrite", messages);
      return;
    }

    Element cajoledOutput = doc.createElement("div");
    cajoledOutput.setAttribute("id", "cajoled-output");

    List<Message> messages = result.messages;
    Element messagesNode = formatErrors(doc, is, docContent, messages,
        /* invisible */ false);
    cajoledOutput.appendChild(messagesNode);

    Element outerDiv = doc.createElement("div");
    outerDiv.setAttribute("id", "caja_outerContainer___");
    outerDiv.setAttribute("style", "position: relative; overflow: hidden;");
    cajoledOutput.appendChild(outerDiv);

    Element innerDiv = doc.createElement("div");
    innerDiv.setAttribute("id", "caja_innerContainer___");
    innerDiv.setAttribute("class", "g___");
    outerDiv.appendChild(innerDiv);

    innerDiv.appendChild(doc.importNode(result.html, true));

    String cajoledJs = renderJs(result.js, debug);
    cajoledOutput.appendChild(cajaStart(doc, cajoledJs, debug));

    createContainerFor(doc, cajoledOutput);
    mc.documentChanged();
    HtmlSerialization.attach(doc, htmlSerializer, null);
  }

  UriFetcher makeFetcher(final Uri gadgetUri, final String container) {
    return new UriFetcher() {
      public FetchedData fetch(ExternalReference ref, String mimeType)
          throws UriFetchException {
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, CLASS_NAME, "makeFetcher", MessageKeys.RETRIEVE_REFERENCE,
              new Object[] {ref.toString()});
        }
        Uri resourceUri = gadgetUri.resolve(Uri.fromJavaUri(ref.getUri()));
        HttpRequest request =
            new HttpRequest(resourceUri).setContainer(container).setGadget(gadgetUri).setInternalRequest( true );
        try {
          HttpResponse response = requestPipeline.execute(request);
          byte[] responseBytes = IOUtils.toByteArray(response.getResponse());
          return FetchedData.fromBytes(responseBytes, mimeType, response.getEncoding(),
              new InputSource(ref.getUri()));
        } catch (GadgetException e) {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, CLASS_NAME, "makeFetcher", MessageKeys.FAILED_TO_RETRIEVE,
                new Object[] {ref.toString()});
          }
          throw new UriFetchException(ref, mimeType, e);
        } catch (IOException e) {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, CLASS_NAME, "makeFetcher", MessageKeys.FAILED_TO_READ,
                new Object[] {ref.toString()});
          }
          throw new UriFetchException(ref, mimeType, e);
        }
      }

    };
  }

  protected UriPolicy makePolicy(final Uri gadgetUri) {
    return new UriPolicy() {
      public String rewriteUri(ExternalReference ref, UriEffect effect,
          LoaderType loader, Map<String, ?> hints) {
        try {
          switch(effect) {
            case SAME_DOCUMENT:
                ProxyUriManager.ProxyUri proxyUri =
                    new ProxyUriManager.ProxyUri(
                        UriStatus.VALID_UNVERSIONED, Uri.fromJavaUri(ref.getUri()), null);
                return proxyUriManager.make(ImmutableList.of(proxyUri), null).get(0).toString();
            case NEW_DOCUMENT:
            case NOT_LOADED:
                return ref.getUri().toString();
            default:
                return null;
          }
        } catch (RuntimeException e) {
          // if there are unexpected errors, fail safe - drop the uri
        }
        return null;
      }
    };
  }

  private void createContainerFor(Document doc, Node el) {
    Element docEl = doc.createElement("html");
    Element head = doc.createElement("head");
    Element body = doc.createElement("body");
    doc.appendChild(docEl);
    docEl.appendChild(head);
    docEl.appendChild(body);
    body.appendChild(el);
  }

  private Element formatErrors(Document doc, InputSource is,
      CharSequence orig, List<Message> messages, boolean visible) {
    MessageContext mc = new MessageContext();
    Map<InputSource, CharSequence> originalSrc = Maps.newHashMap();
    originalSrc.put(is, orig);
    mc.addInputSource(is);
    SnippetProducer sp = new SnippetProducer(originalSrc, mc);

    Element errElement = doc.createElement("ul");
    // Style defined in gadgets.css
    errElement.setAttribute("class", "gadgets-messages");
    if (!visible) {
      errElement.setAttribute("style", "display: none");
    }
    for (Message msg : messages) {
      // Ignore LINT messages
      if (MessageLevel.LINT.compareTo(msg.getMessageLevel()) <= 0) {
        String snippet = sp.getSnippet(msg);
        String messageText = msg.getMessageLevel().name() + ' ' +
          html(msg.format(mc)) + ':' + snippet;
        Element li = doc.createElement("li");
        li.appendChild(doc.createTextNode(messageText));
        errElement.appendChild(li);
      }
    }
    return errElement;
  }

  private static String html(CharSequence s) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(s, false, sb);
    return sb.toString();
  }

  private String renderJs(CajoledModule cajoled, boolean debug) {
    StringBuilder rendered = new StringBuilder();
    TokenConsumer tc = debug
        ? new JsPrettyPrinter(new Concatenator(rendered))
        : new JsMinimalPrinter(new Concatenator(rendered));
    cajoled.render(new RenderContext(tc)
        .withAsciiOnly(true)
        .withEmbeddable(true));
    tc.noMoreTokens();
    return rendered.toString();
  }

  private Element cajaStart(Document doc, String cajoledJs, boolean debug) {
    Element scriptElement = doc.createElement("script");
    scriptElement.setAttribute("type", "text/javascript");
    StringBuilder start = new StringBuilder();
    start.append("caja___.start(\n'");
    Escaping.escapeJsString(cajoledJs, true, true, start);
    start.append("', ");
    start.append(debug ? "true" : "false");
    start.append(");\n");
    scriptElement.appendChild(doc.createTextNode(start.toString()));
    return scriptElement;
  }

  private void logException(String methodname, List<Message> messages) {
    StringBuilder errbuilder = new StringBuilder();
    MessageContext mc = new MessageContext();
    for (Message m : messages) {
      errbuilder.append(m.format(mc)).append('\n');
    }
    if (LOG.isLoggable(Level.INFO)) {
      LOG.logp(Level.INFO, CLASS_NAME, methodname, MessageKeys.UNABLE_TO_CAJOLE,
          new Object[] {errbuilder});
    }
  }

  protected PluginCompiler makePluginCompiler(
      PluginMeta meta, MessageQueue mq) {
    return new PluginCompiler(
        BuildInfo.getInstance(), meta, mq);
  }
}
