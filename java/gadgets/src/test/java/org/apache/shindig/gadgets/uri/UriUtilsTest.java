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
package org.apache.shindig.gadgets.uri;

import com.google.inject.internal.ImmutableList;
import com.google.inject.internal.ImmutableMap;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.easymock.EasyMock.expect;

/**
 * Tests for UriUtils.
 */
public class UriUtilsTest extends EasyMockTestCase {
  @Before
  public void setUp() throws Exception {
  }

  Enumeration<String> makeEnumeration(String... args) {
    Vector<String> vector = new Vector<String>();
    if (args != null) {
      vector.addAll(Arrays.asList(args));
    }
    return vector.elements();
  }

  @Test
  public void testCopyResponseHeadersAndStatusCode_AddHeader() throws Exception {
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(5000)
        .addHeader("hello", "world1")
        .addHeader("hello", "world2")
        .addHeader("hello\\u2297", "bad header")
        .addHeader("Content-length", "10")
        .addHeader("vary", "1")
        .create();
    HttpServletResponse response = mock(HttpServletResponse.class);

    response.setStatus(5000);
    EasyMock.expectLastCall().once();

    response.addHeader("hello", "world1");
    EasyMock.expectLastCall().once();
    response.addHeader("hello", "world2");
    EasyMock.expectLastCall().once();
    

    replay();
    UriUtils.copyResponseHeadersAndStatusCode(resp, response, false,
        UriUtils.DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES,
        UriUtils.DisallowedHeaders.CACHING_DIRECTIVES);
    verify();
  }

  @Test
  public void testCopyResponseHeadersAndStatusCode_SetHeaders() throws Exception {
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(5000)
        .addHeader("hello", "world1")
        .addHeader("hello", "world2")
        .addHeader("hello\\u2297", "bad header")
        .addHeader("Content-length", "10")
        .addHeader("vary", "1")
        .create();
    HttpServletResponse response = mock(HttpServletResponse.class);

    response.setStatus(5000);
    EasyMock.expectLastCall().once();

    response.setHeader("hello", "world1");
    EasyMock.expectLastCall().once();
    response.setHeader("hello", "world2");
    EasyMock.expectLastCall().once();


    replay();
    UriUtils.copyResponseHeadersAndStatusCode(resp, response, true,
        UriUtils.DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES,
        UriUtils.DisallowedHeaders.CACHING_DIRECTIVES);
    verify();
  }

  @Test
  public void testCopyRequestHeaders() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    EasyMock.expect(request.getHeaderNames())
        .andReturn(makeEnumeration("h1", "h2", "hello\\u2297",
                                   "unchanged_header", "Content-Length"));
    EasyMock.expect(request.getHeaders("h1"))
        .andReturn(makeEnumeration("v1", "v2"));
    EasyMock.expect(request.getHeaders("h2"))
        .andReturn(makeEnumeration("v3", "v4"));
    EasyMock.expect(request.getHeaders("hello\\u2297"))
        .andReturn(makeEnumeration("v5", "v6"));
    EasyMock.expect(request.getHeaders("unchanged_header"))
        .andReturn(makeEnumeration());
    EasyMock.expect(request.getHeaders("Content-Length"))
        .andReturn(makeEnumeration("50", "100"));

    HttpRequest req = new HttpRequest(Uri.parse(
        "http://www.example.org/data.html"));
    req.removeHeader(HttpRequest.DOS_PREVENTION_HEADER);
    req.addHeader("h1", "hello");
    req.addHeader("Content-Length", "10");
    req.addHeader("unchanged_header", "original_value");

    replay();
    UriUtils.copyRequestHeaders(request, req,
        UriUtils.DisallowedHeaders.POST_INCOMPATIBLE_DIRECTIVES);
    verify();

    Map<String, List<String>> headers =
        ImmutableMap.<String, List<String>>builder()
        .put("h1", ImmutableList.of("v1", "v2"))
        .put("h2", ImmutableList.of("v3", "v4"))
        .put("hello\\u2297", ImmutableList.of("v5", "v6"))
        .put("unchanged_header", ImmutableList.of("original_value"))
        .put("Content-Length", ImmutableList.of("10"))
        .build();

    assertEquals(headers, req.getHeaders());
  }

  @Test
  public void testCopyRequestData() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    expect(request.getMethod()).andReturn("Post").anyTimes();

    final String data = "hello world";
    ServletInputStream inputStream = mock(ServletInputStream.class);
    expect(request.getInputStream()).andReturn(inputStream);
    expect(inputStream.read((byte[]) EasyMock.anyObject()))
        .andAnswer(new IAnswer<Integer>() {
          public Integer answer() throws Throwable {
            byte[] byteArray = (byte[]) EasyMock.getCurrentArguments()[0];
            System.arraycopy(data.getBytes(), 0, byteArray, 0, data.length());
            return data.length();
          }
        });
    expect(inputStream.read((byte[]) EasyMock.anyObject()))
        .andAnswer(new IAnswer<Integer>() {
          public Integer answer() throws Throwable {
            return -1;
          }
        });

    HttpRequest req = new HttpRequest(Uri.parse(
        "http://www.example.org/data.html"));

    replay();
    UriUtils.copyRequestData(request, req);
    verify();

    assertEquals(data, req.getPostBodyAsString());
  }
}
