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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.inject.Inject;

import org.json.JSONObject;

/**
 * Produces a valid HTML document for the gadget output, automatically inserting appropriate HTML
 * document wrapper data as needed.
 *
 * Currently, this is only invoked directly since the rewriting infrastructure doesn't properly
 * deal with uncacheable rewrite operations.
 *
 * TODO: Break this up into multiple rewriters if and when rewriting infrastructure supports
 * parse tree manipulation without worrying about caching.
 *
 * Should be:
 *
 * - UserPrefs injection
 * - Javascript injection (including configuration)
 * - html document normalization
 */
public class RenderingContentRewriter implements ContentRewriter {
  static final String DEFAULT_HEAD_CONTENT =
      "<style type=\"text/css\">" +
      "body,td,div,span,p{font-family:arial,sans-serif;}" +
      "a {color:#0000cc;}a:visited {color:#551a8b;}" +
      "a:active {color:#ff0000;}" +
      "body{margin: 0px;padding: 0px;background-color:white;}" +
      "</style>";

  private final MessageBundleFactory messageBundleFactory;

  /**
   * @param messageBundleFactory Used for injecting message bundles into gadget output.
   */
  @Inject
  public RenderingContentRewriter(MessageBundleFactory messageBundleFactory) {
    this.messageBundleFactory = messageBundleFactory;
  }

  public void rewrite(HttpRequest request, HttpResponse original, MutableContent content) {
    throw new UnsupportedOperationException();
  }

  public void rewrite(Gadget gadget) {
    try {
      GadgetContent content = createGadgetContent(gadget);
      insertJavascriptLibraries(gadget, content);
      injectMessageBundles(gadget, content);
      // TODO: Use preloads when RenderedGadget gets promoted to Gadget.
      finalizeDocument(gadget, content);
    } catch (GadgetException e) {
      // TODO: Rewriter interface needs to be modified to handle GadgetException or
      // RewriterException or something along those lines.
      throw new RuntimeException(e);
    }
  }

  /**
   * Injects javascript libraries needed to satisfy feature dependencies.
   */
  private void insertJavascriptLibraries(Gadget gadget, GadgetContent content) {
    // TODO: Need to migrate UrlGenerator and HttpUtil.getJsConfig from servlet code.
  }

  /**
   * Injects message bundles into the gadget output.
   * @throws GadgetException If we are unable to retrieve the message bundle.
   */
  private void injectMessageBundles(Gadget gadget, GadgetContent content) throws GadgetException {
    GadgetContext context = gadget.getContext();
    MessageBundle bundle = messageBundleFactory.getBundle(
        gadget.getSpec(), context.getLocale(), context.getIgnoreCache());

    String msgs = new JSONObject(bundle.getMessages()).toString();
    // TODO: Figure out a simple way to merge scripts.
    content.appendHead("<script>gadgets.Prefs.setMessages_(")
           .appendHead(msgs)
           .appendHead(");</script>");
  }

  /**
   * Produces GadgetContent by parsing the document into 3 pieces (head, body, and tail). If the
   */
  private GadgetContent createGadgetContent(Gadget gadget) {
    GadgetContent content = new GadgetContent();
    content.appendHead("<html><head>");
    content.appendHead(DEFAULT_HEAD_CONTENT);
    content.appendBody("</head>");
    content.appendBody(createDefaultBody(gadget));
    content.appendBody(gadget.getContent());
    content.appendTail("</body></html>");
    // TODO: Parse valid documents into 3 parts.
    return content;
  }

  /**
   * Produces the default body tag, inserting language direction as needed.
   */
  private String createDefaultBody(Gadget gadget) {
    LocaleSpec localeSpec = gadget.getLocale();
    if (localeSpec == null) {
      return "<body>";
    } else {
      return "<body dir='" + localeSpec.getLanguageDirection() + "'>";
    }
  }

  /**
   * Produces a final document for the gadget's content.
   */
  private void finalizeDocument(Gadget gadget, GadgetContent content) {
    gadget.setContent(content.assemble());
  }

  private static class GadgetContent {
    private final StringBuilder head = new StringBuilder();
    private final StringBuilder body = new StringBuilder();
    private final StringBuilder tail = new StringBuilder();

    GadgetContent appendHead(String content) {
      head.append(content);
      return this;
    }

    GadgetContent appendBody(String content) {
      body.append(content);
      return this;
    }

    GadgetContent appendTail(String content) {
      tail.append(content);
      return this;
    }

    /**
     * @return The final content for the gadget.
     */
    String assemble() {
      return new StringBuilder(head.length() + body.length() + tail.length())
          .append(head)
          .append(body)
          .append(tail)
          .toString();
    }

    @Override
    public String toString() {
      return new StringBuilder(head.length() + body.length() + tail.length())
      .append(head)
      .append("\n--BODY--\n")
      .append(body)
      .append("\n--TAIL--\n")
      .append(tail)
      .toString();
    }
  }
}
