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
package org.apache.shindig.gadgets.http;

import com.google.common.collect.ImmutableList;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.eq;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.http.MultipleResourceHttpFetcher.RequestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.*;
import java.io.*;

/**
 * Tests for {@code MultipleResourceHttpFetcher}.
 */
public class MultipleResourceHttpFetcherTest extends EasyMockTestCase {
  private RequestPipeline requestPipeline;
  private transient ExecutorService executor = Executors.newSingleThreadExecutor();
  private MultipleResourceHttpFetcher fetcher;

  private static final Uri IMG_URI =
      UriBuilder.parse("org/apache/shindig/gadgets/rewrite/image/small.jpg").toUri();
  private static final Uri CSS_URI =
      UriBuilder.parse("org/apache/shindig/gadgets/rewrite/image/large.css").toUri();

  private RequestContext reqCxt1;
  private RequestContext reqCxt2;
  private RequestContext reqCxt3;

  @Before
  public void setUp() throws Exception {
    requestPipeline = mock(RequestPipeline.class);
    fetcher = new MultipleResourceHttpFetcher(requestPipeline, executor);

    reqCxt1 = createRequestContext(IMG_URI, "jpeg image", "image/jpeg");
    reqCxt2 = createRequestContext(CSS_URI, "css files", "text/css");
    reqCxt3 = createRequestContext(IMG_URI, "jpeg image", "image/jpeg");
  }

  @Test
  public void testFetchAll() throws Exception {
    List<HttpRequest> requests = createRequestArray();

    expect(requestPipeline.execute(eq(reqCxt1.getHttpReq()))).andReturn(reqCxt1.getHttpResp());
    expect(requestPipeline.execute(eq(reqCxt2.getHttpReq()))).andReturn(reqCxt2.getHttpResp());
    expect(requestPipeline.execute(eq(reqCxt3.getHttpReq()))).andReturn(reqCxt3.getHttpResp());

    replay();
    List<Pair<Uri, FutureTask<RequestContext>>> futureTasks = fetcher.fetchAll(requests);
    assertEquals(3, futureTasks.size());
    assertEquals(IMG_URI, futureTasks.get(0).one);
    assertEquals(reqCxt1, futureTasks.get(0).two.get());
    assertEquals(CSS_URI, futureTasks.get(1).one);
    assertEquals(reqCxt2, futureTasks.get(1).two.get());
    assertEquals(IMG_URI, futureTasks.get(2).one);
    assertEquals(reqCxt3, futureTasks.get(2).two.get());
    verify();
  }

  @Test
  public void testFetchUnique() throws Exception {
    List<HttpRequest> requests = createRequestArray();

    expect(requestPipeline.execute(eq(reqCxt1.getHttpReq()))).andReturn(reqCxt1.getHttpResp());
    expect(requestPipeline.execute(eq(reqCxt2.getHttpReq()))).andReturn(reqCxt2.getHttpResp());

    replay();
    Map<Uri, FutureTask<RequestContext>> futureTasks = fetcher.fetchUnique(requests);
    assertEquals(2, futureTasks.size());
    assertTrue(futureTasks.containsKey(IMG_URI));
    assertEquals(reqCxt1, futureTasks.get(IMG_URI).get());
    assertTrue(futureTasks.containsKey(CSS_URI));
    assertEquals(reqCxt2, futureTasks.get(CSS_URI).get());
    verify();
  }

  private RequestContext createRequestContext(Uri uri, String content, String mimeType)
      throws IOException {
    HttpRequest request = new HttpRequest(uri);

    HttpResponse response =  new HttpResponseBuilder().addHeader("Content-Type", mimeType)
            .setResponse(content.getBytes()).create();

    return new RequestContext(request, response, null);
  }

  private List<HttpRequest> createRequestArray() {
    return ImmutableList.of(reqCxt1.getHttpReq(), reqCxt2.getHttpReq(), reqCxt3.getHttpReq());
  }
}
