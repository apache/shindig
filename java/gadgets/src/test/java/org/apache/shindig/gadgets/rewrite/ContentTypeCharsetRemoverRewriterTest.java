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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for ContentTypeCharsetRemoverRewriter.
 */
public class ContentTypeCharsetRemoverRewriterTest extends DomWalkerTestBase {
  private CajaHtmlParser htmlParser;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ParseModule.DOMImplementationProvider domImpl =
        new ParseModule.DOMImplementationProvider();
    htmlParser = new CajaHtmlParser(domImpl.get());
  }

  @Test
  public void testContentTypeCharsetRemoved() throws Exception {
    String html = "<html><head>"
                  + "<META Content=\"hello world\" "
                  + "Http-equiv=\"Content-Title\">"
                  + "<META Content=\"text/html ; charset = \'GBK\'\" "
                  + "Http-equiv=\"Content-TYPE\">"
                  + "<META Content=\"gzip\" "
                  + "Http-EQuIv=\"Content-Encoding\">"
                  + "</head><body><a href=\"hello\">Hello</a>"
                  + "</body></html>";
    String expected = "<html><head>"
                      + "<meta content=\"hello world\" "
                      + "http-equiv=\"Content-Title\">"
                      + "<meta content=\"text/html \" "
                      + "http-equiv=\"Content-TYPE\">"
                      + "<meta content=\"gzip\" "
                      + "http-equiv=\"Content-Encoding\">"
                      + "</head><body><a href=\"hello\">Hello</a>"
                      + "</body></html>";

    ContentTypeCharsetRemoverRewriter rewriter =
        new ContentTypeCharsetRemoverRewriter();
    Gadget gadget = DomWalker.makeGadget(new HttpRequest(
        Uri.parse("http://1.com/")));
    MutableContent mc = new MutableContent(htmlParser, html);
    rewriter.rewrite(gadget, mc);

    assertEquals(expected, mc.getContent());
  }

  @Test
  public void testNoMetaNode() throws Exception {
    String html = "<html><head><title>hello</title>"
                  + "</head><body><a href=\"hello\">Hello</a>"
                  + "</body></html>";
    String expected = "<html><head><title>hello</title>"
                      + "</head><body><a href=\"hello\">Hello</a>"
                      + "</body></html>";

    ContentTypeCharsetRemoverRewriter rewriter =
        new ContentTypeCharsetRemoverRewriter();
    Gadget gadget = DomWalker.makeGadget(new HttpRequest(
        Uri.parse("http://1.com/")));
    MutableContent mc = new MutableContent(htmlParser, html);
    rewriter.rewrite(gadget, mc);

    assertEquals(expected, mc.getContent());
  }

  @Test
  public void testMalformedCharset() throws Exception {
    String html = "<html><head>"
                  + "<META Content=\"text/html ; pharset=\'hello\'; hello=world\" "
                  + "Http-equiv=\"Content-TYPE\">"
                  + "</head><body><a href=\"hello\">Hello</a>"
                  + "</body></html>";
    String expected = "<html><head>"
                      + "<meta content=\"text/html ; pharset=&#39;hello&#39;; hello=world\" "
                      + "http-equiv=\"Content-TYPE\">"
                      + "</head><body><a href=\"hello\">Hello</a>"
                      + "</body></html>";

    ContentTypeCharsetRemoverRewriter rewriter =
        new ContentTypeCharsetRemoverRewriter();
    Gadget gadget = DomWalker.makeGadget(new HttpRequest(
        Uri.parse("http://1.com/")));
    MutableContent mc = new MutableContent(htmlParser, html);
    rewriter.rewrite(gadget, mc);

    assertEquals(expected, mc.getContent());

    html = "<html><head>"
           + "<META Content=\"text/html ; charsett=\'hello\'; hello=world\" "
           + "Http-equiv=\"Content-TYPE\">"
           + "</head><body><a href=\"hello\">Hello</a>"
           + "</body></html>";
    expected = "<html><head>"
               + "<meta content=\"text/html ; charsett=&#39;hello&#39;; hello=world\" "
               + "http-equiv=\"Content-TYPE\">"
               + "</head><body><a href=\"hello\">Hello</a>"
               + "</body></html>";

    mc = new MutableContent(htmlParser, html);
    rewriter.rewrite(gadget, mc);

    assertEquals(expected, mc.getContent());
  }
}
