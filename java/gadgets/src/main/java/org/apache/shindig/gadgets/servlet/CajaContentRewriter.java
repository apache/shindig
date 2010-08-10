/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.servlet;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.opensocial.DefaultGadgetRewriter;
import com.google.caja.opensocial.GadgetRewriteException;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.util.Pair;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

public class CajaContentRewriter implements GadgetRewriter {
  public static final String CAJOLED_DOCUMENTS = "cajoledDocuments";

  private static final Logger LOG = Logger.getLogger(CajaContentRewriter.class.getName());

  private final Cache<String, Element> cajoledCache;
  private final RequestPipeline requestPipeline;
  private final HtmlSerializer htmlSerializer;

  @Inject
  public CajaContentRewriter(CacheProvider cacheProvider, RequestPipeline requestPipeline,
      HtmlSerializer htmlSerializer) {
    this.cajoledCache = cacheProvider.createCache(CAJOLED_DOCUMENTS);
    LOG.info("Cajoled cache created" + cajoledCache);
    this.requestPipeline = requestPipeline;
    this.htmlSerializer = htmlSerializer;
  }

  public void rewrite(Gadget gadget, MutableContent mc) {
    if (!cajaEnabled(gadget)) return;

    Document doc = mc.getDocument();

    // Serialize outside of MutableContent, to prevent a re-parse.
    String docContent = HtmlSerialization.serialize(doc);
    String cacheKey = HashUtil.checksum(docContent.getBytes());
    Node root = doc.createDocumentFragment();
    root.appendChild(doc.getDocumentElement());

    Node cajoledData = null;
    if (cajoledCache != null) {
      Element cajoledOutput = cajoledCache.getElement(cacheKey);
      if (cajoledOutput != null) {
        cajoledData = doc.adoptNode(cajoledOutput);
        createContainerFor(doc, cajoledData);
        mc.documentChanged();
      }
    }

    if (cajoledData == null) {
      UriFetcher fetcher = makeFetcher(gadget);
      UriPolicy policy = makePolicy(gadget);
      URI javaGadgetUri = gadget.getContext().getUrl().toJavaUri();
      MessageQueue mq = new SimpleMessageQueue();
      BuildInfo bi = BuildInfo.getInstance();
      DefaultGadgetRewriter rw = new DefaultGadgetRewriter(bi, mq);
      InputSource is = new InputSource(javaGadgetUri);
      boolean safe = false;

      try {
        Pair<Node, Element> htmlAndJs =
            rw.rewriteContent(javaGadgetUri, root, fetcher, policy, null);
        Node html = htmlAndJs.a;
        Element script = htmlAndJs.b;

        Element cajoledOutput = doc.createElement("div");
        cajoledOutput.setAttribute("id", "cajoled-output");
        cajoledOutput.setAttribute("classes", "g___");
        cajoledOutput.setAttribute("style", "position: relative;");

        cajoledOutput.appendChild(doc.adoptNode(html));
        cajoledOutput.appendChild(tameCajaClientApi(doc));
        cajoledOutput.appendChild(doc.adoptNode(script));

        Element messagesNode = formatErrors(doc, is, docContent, mq,
            /* is invisible */ false);
        cajoledOutput.appendChild(messagesNode);
        if (cajoledCache != null) {
          cajoledCache.addElement(cacheKey, cajoledOutput);
        }
        safe = true;
        cajoledData = cajoledOutput;
        createContainerFor(doc, cajoledData);
        mc.documentChanged();
        safe = true;
        HtmlSerialization.attach(doc, htmlSerializer, null);
      } catch (GadgetRewriteException e) {
        // There were cajoling errors
        // Content is only used to produce useful snippets with error messages
        createContainerFor(doc,
            formatErrors(doc, is, docContent, mq, true /* visible */));
        logException(e, mq);
        safe = true;
      } finally {
        if (!safe) {
          // Fail safe
          mc.setContent("");
          return;
        }
      }
    }
  }

  private boolean cajaEnabled(Gadget gadget) {
    return (gadget.getAllFeatures().contains("caja") ||
        "1".equals(gadget.getContext().getParameter("caja")));
  }

  private UriFetcher makeFetcher(Gadget gadget) {
    final Uri gadgetUri = gadget.getContext().getUrl();
    final String container = gadget.getContext().getContainer();
    
    return new UriFetcher() {
      public FetchedData fetch(ExternalReference ref, String mimeType)
          throws UriFetchException {
        LOG.info("Retrieving " + ref.toString());
        Uri resourceUri = gadgetUri.resolve(Uri.fromJavaUri(ref.getUri()));
        HttpRequest request =
            new HttpRequest(resourceUri).setContainer(container).setGadget(gadgetUri);
        try {
          HttpResponse response = requestPipeline.execute(request);
          byte[] responseBytes = IOUtils.toByteArray(response.getResponse());
          return FetchedData.fromBytes(responseBytes, mimeType, response.getEncoding(),
              new InputSource(ref.getUri()));
        } catch (GadgetException e) {
          LOG.info("Failed to retrieve: " + ref.toString());
          return null;
        } catch (IOException e) {
          LOG.info("Failed to read: " + ref.toString());
          return null;
        }
      }
      
    };
  }
  
  private UriPolicy makePolicy(Gadget gadget) {
    final Uri gadgetUri = gadget.getContext().getUrl();

    return new UriPolicy() {
      public String rewriteUri(ExternalReference ref, UriEffect effect,
          LoaderType loader, Map<String, ?> hints) {
        URI uri = ref.getUri();
        if (uri.getScheme().equalsIgnoreCase("https") ||
            uri.getScheme().equalsIgnoreCase("http")) {
          return gadgetUri.resolve(Uri.fromJavaUri(uri)).toString();
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
      CharSequence orig, MessageQueue mq, boolean visible) {
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
    for (Message msg : mq.getMessages()) {
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

  private Element tameCajaClientApi(Document doc) {
    Element scriptElement = doc.createElement("script");
    scriptElement.setAttribute("type", "text/javascript");
    scriptElement.appendChild(doc.createTextNode("caja___.enable()"));
    return scriptElement;
  }

  private void logException(Exception cause, MessageQueue mq) {
    StringBuilder errbuilder = new StringBuilder();
    MessageContext mc = new MessageContext();
    if (cause != null) {
      errbuilder.append(cause).append('\n');
    }
    for (Message m : mq.getMessages()) {
      errbuilder.append(m.format(mc)).append('\n');
    }
    LOG.info("Unable to cajole gadget: " + errbuilder);
  }
}
