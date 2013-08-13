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

import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.DefaultProxyUriManager;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;

/**
 * Tests for {@code ImageResizeRewriter}
 */
public class ImageResizeRewriterTest {
  static final String CONTAINER = "test";

  private ImageResizeRewriter rewriter;
  private CajaHtmlParser parser;
  private ParseModule.DOMImplementationProvider domImpl;
  private ContainerConfig config;
  private ContentRewriterFeature.Config featureConfig;
  private ContentRewriterFeature.Factory factory;

  @Before
  public void setUp() {
    config = EasyMock.createMock(ContainerConfig.class);
    factory = EasyMock.createMock(ContentRewriterFeature.Factory.class);
    featureConfig = EasyMock.createMock(ContentRewriterFeature.Config.class);

    ProxyUriManager proxyUriManager = new DefaultProxyUriManager(config, null);
    rewriter = new ImageResizeRewriter(proxyUriManager, factory);
    domImpl = new ParseModule.DOMImplementationProvider();
    parser = new CajaHtmlParser(domImpl.get());
    EasyMock.expect(factory.get(EasyMock.isA(HttpRequest.class))).andReturn(featureConfig).anyTimes();
    EasyMock.expect(factory.get(EasyMock.isA(GadgetSpec.class))).andReturn(featureConfig).anyTimes();
    EasyMock.expect(config.getString(CONTAINER, DefaultProxyUriManager.PROXY_HOST_PARAM))
        .andReturn("shindig.com").anyTimes();
    EasyMock.expect(config.getString(CONTAINER, DefaultProxyUriManager.PROXY_PATH_PARAM))
        .andReturn("/proxy").anyTimes();
    EasyMock.expect(featureConfig.getExpires()).andReturn(new Integer(0)).anyTimes();
  }

  @Test
  public void testImageResizeRewriter() throws Exception {

    String content = "<html><head></head><body>"
        + "<p> p tag </p>"
        + "<img src=\"shindig.com/proxy?container=test&url=1.jpg\">"
        + "<img height=\"50px\" id=\"img\" src=\"shindig.com/proxy?container=test&url=2.jpg\">"
        + "<img src=\"shindig.com/proxy?container=test&url=3.jpg\" width=\"50px\">"
        + "<img height=\"50px\" id=\"id\" src=\"shindig.com/proxy?container=test&url=4.jpg\""
        + " width=\"110px\">"
        + "<img height=\"5\" src=\"shindig.com/proxy?container=test&url=5.jpg\" width=\"10em\">"
        + "<img height=\"50\" src=\"shindig.com/proxy?container=test&url=6.jpg\" width=\"110px\">"
        + "<img src=\"shindig.com/proxy?container=test&url=7.jpg\""
        + " style=\"height:50px; width:110px\">"
        + "<img src=\"example.com/8.jpg\" style=\"height:50px; width:110px\">"
        + "<img height=\"60px\" width=\"120px\" src=\"shindig.com/proxy?container=test&url=9.jpg\""
        + " style=\"height:50px; width:110px\">"
        + "<img width=\"120px\" src=\"shindig.com/proxy?container=test&url=10.jpg\""
        + " style=\"height:50px;\">"
        + "<img height=\"60px\" src=\"shindig.com/proxy?container=test&url=11.jpg\""
        + " style=\"width:110px\">"
        + "<img height=\"60px\" src=\"shindig.com/proxy?container=test&url=12.jpg\""
        + " style=\"width:110px\" width=\"50px\">"
        + "</body></html>";

    String expected = "<html><head></head><body>"
        + "<p> p tag </p>"
        + "<img src=\"shindig.com/proxy?container=test&amp;url=1.jpg\">"
        + "<img height=\"50px\" id=\"img\" src=\"shindig.com/proxy?container=test&amp;url=2.jpg\">"
        + "<img src=\"shindig.com/proxy?container=test&amp;url=3.jpg\" width=\"50px\">"
        + "<img height=\"50px\" id=\"id\" src=\"shindig.com/proxy?container=test&amp;url=4.jpg\""
        + " width=\"110px\">"
        + "<img height=\"5\" src=\"shindig.com/proxy?container=test&amp;url=5.jpg\" width=\"10em\">"
        + "<img height=\"50\" src=\"" + getProxiedUrl("6.jpg", "50", "110") + "\" width=\"110px\">"
        + "<img src=\"" + getProxiedUrl("7.jpg", "50", "110") + "\""
        + " style=\"height:50px; width:110px\">"
        + "<img src=\"example.com/8.jpg\" style=\"height:50px; width:110px\">"
        + "<img  height=\"60px\" src=\"" + getProxiedUrl("9.jpg", "50", "110") + "\""
        + " style=\"height:50px; width:110px\" width=\"120px\">"
        + "<img src=\"shindig.com/proxy?container=test&amp;url=10.jpg\""
        + " style=\"height:50px;\" width=\"120px\">"
        + "<img height=\"60px\" src=\"shindig.com/proxy?container=test&amp;url=11.jpg\""
        + " style=\"width:110px\">"
        + "<img height=\"60px\" src=\"" + getProxiedUrl("12.jpg", "60", "110") + "\""
        + " style=\"width:110px\" width=\"50px\">"
        + "</body></html>";

    HttpRequest req = new HttpRequest(Uri.parse("http://www.shindig.com/"));
    req.setGadget(UriBuilder.parse("http://www.shindig.com/").toUri());
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(200)
        .setHeader("Content-Type", "text/html")
        .setResponse(content.getBytes())
        .create();
    HttpResponseBuilder builder = new HttpResponseBuilder(parser, resp);

    EasyMock.replay(config, featureConfig, factory);
    rewriter.rewrite(req, builder, null);
    assertEquals(StringUtils.deleteWhitespace(expected),
                 StringUtils.deleteWhitespace(builder.getContent()));
    EasyMock.verify(config, featureConfig, factory);
  }

  private String getProxiedUrl(String resource, String height, String width) {
    return "//shindig.com/proxy?container=test&amp;debug=0&amp;nocache=0&amp;refresh=0&amp;"
           + "resize_h=" + height + "&amp;resize_w=" + width + "&amp;no_expand=1&amp;url="
           + resource;
  }
}
