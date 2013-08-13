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
package org.apache.shindig.gadgets.render;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Parser;
import com.google.caja.plugin.PipelineMaker;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.LoaderType;
import com.google.caja.plugin.UriEffect;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.DomWalker;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterUtils;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rewriter that cajoles Javascript.
 *
 * @since 2.0.0
 */
public class CajaResponseRewriter implements ResponseRewriter {
  //class name for logging purpose
  private static final String classname = CajaResponseRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private final RequestPipeline requestPipeline;

  @Inject
  public CajaResponseRewriter(RequestPipeline requestPipeline) {
    this.requestPipeline = requestPipeline;
  }

  public void rewrite(HttpRequest req, HttpResponseBuilder resp, Gadget gadget)
          throws RewritingException {
    if (!req.isCajaRequested()) { return; }

    // Only accept Javascript for now
    if (!RewriterUtils.isJavascript(req, resp)) {
      resp.setContent("");
      resp.setHttpStatusCode(HttpResponse.SC_BAD_REQUEST);
      return;
    }

    boolean passed = false;

    MessageQueue mq = new SimpleMessageQueue();
    MessageContext mc = new MessageContext();
    Uri contextUri = req.getUri();
    InputSource is = new InputSource(contextUri.toJavaUri());

    PluginMeta pluginMeta = new PluginMeta(
            proxyFetcher(req, contextUri), proxyUriPolicy(req));
    PluginCompiler compiler = new PluginCompiler(BuildInfo.getInstance(),
            pluginMeta, mq);
    compiler.setMessageContext(mc);

    // Parse the javascript
    try {
      StringReader strReader = new StringReader(resp.getContent());
      CharProducer cp = CharProducer.Factory.create(strReader, is);
      JsTokenQueue tq = new JsTokenQueue(new JsLexer(cp), is);
      ParseTreeNode input = new Parser(tq, mq).parse();
      tq.expectEmpty();

      compiler.addInput(AncestorChain.instance(input).node, contextUri.toJavaUri());
    } catch (ParseException e) {
      // Don't bother continuing.
      resp.setContent("");
      return;
    }

    try {
      if (RewriterUtils.isJavascript(req, resp)) {
        compiler.setGoals(
            compiler.getGoals().without(PipelineMaker.HTML_SAFE_STATIC));
      }
      passed = compiler.run();

      CajoledModule outputJs = passed ? compiler.getJavascript() : null;

      StringBuilder jsOut = new StringBuilder();
      TokenConsumer printer;
      if ("1".equals(req.getParam("debug"))) {
        printer = new JsPrettyPrinter(new Concatenator(jsOut));
      } else {
        printer = new JsMinimalPrinter(new Concatenator(jsOut));
      }

      RenderContext renderContext = new RenderContext(printer).withEmbeddable(true);

      if (outputJs != null) {
        outputJs.render(renderContext);
      }

      renderContext.getOut().noMoreTokens();
      resp.setContent(jsOut.toString());
    } finally {
      if (!passed) {
        resp.setContent("");
      }
    }
  }

  private UriPolicy proxyUriPolicy(HttpRequest request) {
    final Uri contextUri = request.getUri();
    final Gadget stubGadget = DomWalker.makeGadget(request);

    return new UriPolicy() {
      public String rewriteUri(ExternalReference ref, UriEffect effect,
          LoaderType loader, Map<String, ?> hints) {

        Uri resourceUri = Uri.fromJavaUri(ref.getUri());
        if (contextUri != null) {
          resourceUri = contextUri.resolve(resourceUri);
        }

        ProxyUriManager.ProxyUri proxyUri = new ProxyUriManager.ProxyUri(
            stubGadget, resourceUri);
        return proxyUri.getResource().toString();
      }
    };
  }

  private UriFetcher proxyFetcher(final HttpRequest req, final Uri contextUri) {
    return new UriFetcher() {
      public FetchedData fetch(ExternalReference ref, String mimeType) throws UriFetchException {
        Uri resourceUri = Uri.fromJavaUri(ref.getUri());
        if (contextUri != null) {
          resourceUri = contextUri.resolve(resourceUri);
        }

        HttpRequest request = new HttpRequest(resourceUri)
                .setContainer(req.getContainer())
                .setGadget(req.getGadget())
                .setInternalRequest( true );

        try {
          HttpResponse response = requestPipeline.execute(request);
          byte[] responseBytes = IOUtils.toByteArray(response.getResponse());
          return FetchedData.fromBytes(responseBytes, mimeType, response.getEncoding(),
              new InputSource(ref.getUri()));
        } catch (GadgetException e) {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "proxyFetcher", MessageKeys.FAILED_TO_RETRIEVE, new Object[] {ref.toString()});
          }
          return null;
        } catch (IOException e) {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "proxyFetcher", MessageKeys.FAILED_TO_READ, new Object[] {ref.toString()});
          }
          return null;
        }
      }
    };
  }
}
