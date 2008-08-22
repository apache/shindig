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
package org.apache.shindig.gadgets;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.View;

import org.easymock.EasyMock;
import org.junit.Test;
import org.w3c.dom.Element;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Tests for ViewContentFetcher
 */
public class ViewContentFetcherTest {
  private final HttpFetcher fetcher = EasyMock.createNiceMock(HttpFetcher.class);
  private final static URI CONTENT_URI = URI.create("http://example.com/content.htm");
  private final static String CONTENT = "<h2>foo</h2>";
  private final static String VIEW_XML = "<Content type='html' href='" + CONTENT_URI + "' />";

  @Test
  public void getViewContent() throws Exception {
    List<Element> elementList = new ArrayList<Element>();
    elementList.add(XmlUtil.parse(VIEW_XML));
    View view = new View(null, elementList);
    CountDownLatch latch = new CountDownLatch(1);

    HttpResponse response = new HttpResponse(CONTENT);
    expect(fetcher.fetch(isA(HttpRequest.class))).andReturn(response).once();

    replay(fetcher);

    ViewContentFetcher viewContentFetcher = new ViewContentFetcher(view, latch, fetcher, false);
    viewContentFetcher.run();

    verify(fetcher);

    assertNull(view.getHref());
    assertEquals(CONTENT, view.getContent());
    assertEquals(0, latch.getCount());
  }

  @Test
  public void httpFetchException() throws Exception {
    List<Element> elementList = new ArrayList<Element>();
    elementList.add(XmlUtil.parse(VIEW_XML));
    View view = new View(null, elementList);
    CountDownLatch latch = new CountDownLatch(1);

    expect(fetcher.fetch(isA(HttpRequest.class))).andThrow(
        new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR)).once();

    replay(fetcher);

    ViewContentFetcher viewContentFetcher = new ViewContentFetcher(view, latch, fetcher, false);
    viewContentFetcher.run();

    verify(fetcher);

    assertEquals(CONTENT_URI, view.getHref());
    assertEquals("", view.getContent());
    assertEquals(0, latch.getCount());
  }

  @Test
  public void httpResponseNotOk() throws Exception {
    List<Element> elementList = new ArrayList<Element>();
    elementList.add(XmlUtil.parse(VIEW_XML));
    View view = new View(null, elementList);
    CountDownLatch latch = new CountDownLatch(1);

    HttpResponse response = HttpResponse.notFound();
    expect(fetcher.fetch(isA(HttpRequest.class))).andReturn(response).once();

    replay(fetcher);

    ViewContentFetcher viewContentFetcher = new ViewContentFetcher(view, latch, fetcher, false);
    viewContentFetcher.run();

    verify(fetcher);

    assertEquals(CONTENT_URI, view.getHref());
    assertEquals("", view.getContent());
    assertEquals(0, latch.getCount());
  }
}
