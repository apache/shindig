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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterResults;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.opensocial.DefaultGadgetRewriter;
import com.google.caja.opensocial.GadgetRewriteException;
import com.google.caja.opensocial.UriCallback;
import com.google.caja.opensocial.UriCallbackException;
import com.google.caja.opensocial.UriCallbackOption;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.logging.Logger;

public class CajaContentRewriter implements ContentRewriter {
  private final Logger logger = Logger.getLogger(CajaContentRewriter.class.getName());

  public RewriterResults rewrite(HttpRequest req, HttpResponse resp, MutableContent content) {
    return RewriterResults.cacheableIndefinitely();
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    if (gadget.getSpec().getModulePrefs().getFeatures().containsKey("caja") ||
        "1".equals(gadget.getContext().getParameter("caja"))) {

      final URI retrievedUri = gadget.getContext().getUrl();
      UriCallback cb = new UriCallback() {
        public UriCallbackOption getOption(ExternalReference externalReference, String string) {
          return UriCallbackOption.REWRITE;
        }

        public Reader retrieve(ExternalReference externalReference, String string)
            throws UriCallbackException {
          logger.info("Retrieving " + externalReference.toString());
          try {
            Reader in = new InputStreamReader(
                externalReference.getUri().toURL().openConnection().getInputStream(), "UTF-8");
            char[] buf = new char[4096];
            StringBuilder sb = new StringBuilder();
            for (int n; (n = in.read(buf)) > 0;) {
              sb.append(buf, 0, n);
            }
            return new StringReader(sb.toString());
          } catch (java.net.MalformedURLException ex) {
            throw new UriCallbackException(externalReference, ex);
          } catch (IOException ex) {
            throw new UriCallbackException(externalReference, ex);
          }
        }

        public URI rewrite(ExternalReference externalReference, String string) {
          return retrievedUri.resolve(externalReference.getUri());
        }
      };

      MessageQueue mq = new SimpleMessageQueue();
      DefaultGadgetRewriter rw = new DefaultGadgetRewriter(mq);
      CharProducer input = CharProducer.Factory.create(
          new StringReader(content.getContent()),
          FilePosition.instance(new InputSource(retrievedUri), 2, 2, 1, 1));
      StringBuilder output = new StringBuilder();

      try {
        rw.rewriteContent(retrievedUri, input, cb, output);
      } catch (GadgetRewriteException e) {
        throwCajolingException(e, mq);
        return RewriterResults.notCacheable();
      } catch (IOException e) {
        throwCajolingException(e, mq);
        return RewriterResults.notCacheable();
      }      
      content.setContent(output.toString());
    }
    return RewriterResults.notCacheable();
  }

  private void throwCajolingException(Exception cause, MessageQueue mq) {
    StringBuilder errbuilder = new StringBuilder();
    MessageContext mc = new MessageContext();

    if (cause != null) {
      errbuilder.append(cause).append('\n');
    }

    for (Message m : mq.getMessages()) {
      errbuilder.append(m.format(mc)).append('\n');
    }

    logger.info("Unable to cajole gadget: " + errbuilder.toString());

    // throw new GadgetException(
    //    GadgetException.Code.MALFORMED_FOR_SAFE_INLINING, errbuilder.toString());
  }
}
