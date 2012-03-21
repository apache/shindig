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

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for BaseTagRemoverRewriter.
 */
public class BaseTagRemoverRewriterTest extends RewriterTestBase {
  BaseTagRemoverRewriter rewriter;

  CajaHtmlParser parser;
  ParseModule.DOMImplementationProvider domImpl;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    rewriter = new BaseTagRemoverRewriter();
    domImpl = new ParseModule.DOMImplementationProvider();
    parser = new CajaHtmlParser(domImpl.get());
  }

  public void testRemoveBaseTag(Gadget gadget) throws Exception {
    String content = "<html><head><base href='http://www.ppq.com/'>"
                     + "</head><body>"
                     + "<img src='/img1.png'>"
                     + "</body></html>";
    String expected = "<html><head>"
                     + "</head><body>"
                     + "<img src=\"/img1.png\">"
                     + "</body></html>";

    HttpRequest req = new HttpRequest(Uri.parse("http://www.google.com/"));
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(200)
        .setHeader("Content-Type", "text/html")
        .setResponse(content.getBytes())
        .create();
    HttpResponseBuilder builder = new HttpResponseBuilder(parser, resp);

    rewriter.rewrite(req, builder, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
                 StringUtils.deleteWhitespace(builder.getContent()));
  }

  @Test
  public void testRemoveBaseTagGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testRemoveBaseTag(gadget);
  }

  @Test
  public void testRemoveBaseTagNoGadget() throws Exception {
    testRemoveBaseTag(null);
  }

  public void testNoBaseTag(Gadget gadget) throws Exception {
    String content = "<html><head>"
                     + "</head><body>"
                     + "<img src='/img1.png'>"
                     + "</body></html>";
    String expected = "<html><head>"
                     + "</head><body>"
                     + "<img src=\"/img1.png\">"
                     + "</body></html>";

    HttpRequest req = new HttpRequest(Uri.parse("http://www.google.com/"));
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(200)
        .setHeader("Content-Type", "text/html")
        .setResponse(content.getBytes())
        .create();
    HttpResponseBuilder builder = new HttpResponseBuilder(parser, resp);

    rewriter.rewrite(req, builder, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
                 StringUtils.deleteWhitespace(builder.getContent()));
  }

  @Test
  public void testNoBaseTagGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testNoBaseTag(gadget);
  }

  @Test
  public void testNoBaseTagNoGadget() throws Exception {
    testNoBaseTag(null);
  }

  public void testContentTypeString(Gadget gadget) throws Exception {
    String content = "Hello world. My name is gagan<html><head>"
                     + "<base href='http://hello.com/'></head><body>"
                     + "<img src='/img1.png'>"
                     + "</body></html>";
    String expected = "Hello world. My name is gagan<html><head>"
                     + "<base href='http://hello.com/'></head><body>"
                     + "<img src='/img1.png'>"
                     + "</body></html>";

    HttpRequest req = new HttpRequest(Uri.parse("http://www.google.com/"));
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(200)
        .setHeader("Content-Type", "text/plain")
        .setResponse(content.getBytes())
        .create();
    HttpResponseBuilder builder = new HttpResponseBuilder(parser, resp);

    rewriter.rewrite(req, builder, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
                 StringUtils.deleteWhitespace(builder.getContent()));
  }

  @Test
  public void testContentTypeStringGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testContentTypeString(gadget);
  }

  @Test
  public void testContentTypeStringNoGadget() throws Exception {
    testContentTypeString(null);
  }

  public void testContentTypeXml(Gadget gadget) throws Exception {
    String content = "Hello world. My name is gagan<html><head>"
                     + "<base href='http://hello.com/'></head><body>"
                     + "<img src='/img1.png'>"
                     + "</body></html>";
    String expected = "Hello world. My name is gagan<html><head>"
                     + "<base href='http://hello.com/'></head><body>"
                     + "<img src='/img1.png'>"
                     + "</body></html>";

    HttpRequest req = new HttpRequest(Uri.parse("http://www.google.com/"));
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(200)
        .setHeader("Content-Type", "text/xml")
        .setResponse(content.getBytes())
        .create();
    HttpResponseBuilder builder = new HttpResponseBuilder(parser, resp);

    rewriter.rewrite(req, builder, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
                 StringUtils.deleteWhitespace(builder.getContent()));
  }

  @Test
  public void testContentTypeXmlGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testContentTypeString(gadget);
  }

  @Test
  public void testContentTypeXmlNoGadget() throws Exception {
    testContentTypeString(null);
  }
}
