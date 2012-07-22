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
package org.apache.shindig.gadgets.servlet;

import static org.junit.Assert.*;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.shindig.gadgets.servlet.ETaggingHttpResponse.BufferServletOutputStream;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link ETaggingHttpResponse}.
 */
public class ETaggingHttpResponseTest {

  // Using Spanish string and UTF-8 encoding to check for encoding errors.
  private static final String ENCODING = "UTF-8";
  private static final String RESPONSE_BODY = "¡Hola, niño!";
  private static final byte[] RESPONSE_BODY_BYTES =
      new byte[] {-62, -95, 72, 111, 108, 97, 44, 32, 110, 105, -61, -79, 111, 33};
  private static final String SECOND_RESPONSE_BODY = "你好";
  private static final byte[] AFTER_SECOND_RESPONSE_BODY_BYTES =
      new byte[] {-62, -95, 72, 111, 108, 97, 44, 32, 110, 105, -61, -79, 111, 33,
          -28, -67, -96, -27, -91, -67};
  private static final int RESPONSE_BODY_LENGTH = RESPONSE_BODY_BYTES.length;
  private static final String GOOD_ETAG = "dae018f624d09423e7c4d7209fbea597";
  private static final String SECOND_ETAG = "b6e56fb0129c3530f23dbb795daa3200";
  private static final String BAD_ETAG = "some bogus etag";
  private static final String EMPTY_CONTENT_ETAG = "d41d8cd98f00b204e9800998ecf8427e";

  private static final Function<String, String> ETAG_QUOTER = new Function<String, String>() {
    public String apply(String input) {
      return '"' + input + '"';
    }
  };

  private IMocksControl control;
  private HttpServletRequest request;
  private HttpServletResponse origResponse;
  private MockServletOutputStream stream;
  private ETaggingHttpResponse response;

  @Before
  public void setUp() throws Exception {
    control = EasyMock.createControl();
    request = control.createMock(HttpServletRequest.class);
    origResponse = control.createMock(HttpServletResponse.class);
    stream = new MockServletOutputStream();
    response = new ETaggingHttpResponse(request, origResponse);

    EasyMock.expect(origResponse.isCommitted()).andReturn(false).anyTimes();
    EasyMock.expect(origResponse.getOutputStream()).andReturn(stream).anyTimes();
    EasyMock.expect(origResponse.getCharacterEncoding()).andReturn(ENCODING).anyTimes();
    origResponse.flushBuffer();
    EasyMock.expectLastCall().anyTimes();
  }

  @Test
  public void testTagContentWithPrint() throws Exception {
    expectRequestETag();
    expectFullResponse();
    control.replay();

    response.getWriter().print(RESPONSE_BODY);
    response.flushBuffer();

    assertResponseHasBody();
    control.verify();
  }

  @Test
  public void testNotModifiedWithPrint() throws Exception {
    expectRequestETag(GOOD_ETAG);
    expectNotModifiedResponse(GOOD_ETAG);
    control.replay();

    response.getWriter().print(RESPONSE_BODY);
    response.flushBuffer();

    assertResponseBodyIsEmpty();
    control.verify();
  }

  @Test
  public void testNotModifiedWithManyETagsInRequest() throws Exception {
    expectRequestETag(SECOND_ETAG, GOOD_ETAG, BAD_ETAG);
    expectNotModifiedResponse(GOOD_ETAG);
    control.replay();

    response.getWriter().print(RESPONSE_BODY);
    response.flushBuffer();

    assertResponseBodyIsEmpty();
    control.verify();
  }

  @Test
  public void testNonMatchingETagWithPrint() throws Exception {
    expectRequestETag(BAD_ETAG);
    expectFullResponse();
    control.replay();

    response.getWriter().print(RESPONSE_BODY);
    response.flushBuffer();

    assertResponseHasBody();
    control.verify();
  }

  @Test
  public void testNonMatchingETagWithManyETagsInRequest() throws Exception {
    expectRequestETag(BAD_ETAG, SECOND_ETAG, EMPTY_CONTENT_ETAG);
    expectFullResponse();
    control.replay();

    response.getWriter().print(RESPONSE_BODY);
    response.flushBuffer();

    assertResponseHasBody();
    control.verify();
  }

  @Test
  public void testTagContentWithWrite() throws Exception {
    expectRequestETag();
    expectFullResponse();
    control.replay();

    response.getOutputStream().write(RESPONSE_BODY_BYTES);
    response.flushBuffer();

    assertResponseHasBody();
    control.verify();
  }

  @Test
  public void testNotModifiedWithWrite() throws Exception {
    expectRequestETag(GOOD_ETAG);
    expectNotModifiedResponse(GOOD_ETAG);
    control.replay();

    response.getOutputStream().write(RESPONSE_BODY_BYTES);
    response.flushBuffer();

    assertResponseBodyIsEmpty();
    control.verify();
  }

  @Test
  public void testNonMatchingETagWithWrite() throws Exception {
    expectRequestETag(BAD_ETAG);
    expectFullResponse();
    control.replay();

    response.getOutputStream().write(RESPONSE_BODY_BYTES);
    response.flushBuffer();

    assertResponseHasBody();
    control.verify();
  }

  @Test
  public void testTagEmptyContent() throws Exception {
    expectRequestETag();
    origResponse.setHeader(ETaggingHttpResponse.RESPONSE_HEADER, '"' + EMPTY_CONTENT_ETAG + '"');
    origResponse.setContentLength(0);
    control.replay();

    response.getOutputStream();
    response.flushBuffer();

    assertEquals(0, stream.getBuffer().length);
    control.verify();
  }

  @Test
  public void testStreamingMode() throws Exception {
    expectRequestETag();
    control.replay();

    response.getWriter().print(RESPONSE_BODY);
    assertEquals(0, stream.getBuffer().length);

    response.startStreaming();
    assertArrayEquals(RESPONSE_BODY_BYTES, stream.getBuffer());

    response.getOutputStream().write(SECOND_RESPONSE_BODY.getBytes("UTF-8"));
    assertArrayEquals(AFTER_SECOND_RESPONSE_BODY_BYTES, stream.getBuffer());
  }

  @Test
  public void testCanCalculateHashSeveralTimes() throws Exception {
    expectRequestETag(GOOD_ETAG);
    expectNotModifiedResponse(GOOD_ETAG);
    control.replay();

    response.getOutputStream().write(RESPONSE_BODY.getBytes("UTF-8"));
    String hash = ((BufferServletOutputStream) response.getOutputStream()).getContentHash();
    assertEquals(GOOD_ETAG, hash);
    hash = ((BufferServletOutputStream) response.getOutputStream()).getContentHash();
    assertEquals(GOOD_ETAG, hash);

    response.flushBuffer();
    assertResponseBodyIsEmpty();
    control.verify();
  }

  @Test
  public void testHashVariesAsDataIsAdded() throws Exception {
    expectRequestETag(SECOND_ETAG);
    expectNotModifiedResponse(SECOND_ETAG);
    control.replay();

    response.getOutputStream().write(RESPONSE_BODY.getBytes("UTF-8"));
    String hash = ((BufferServletOutputStream) response.getOutputStream()).getContentHash();
    assertEquals(GOOD_ETAG, hash);
    response.getOutputStream().write(SECOND_RESPONSE_BODY.getBytes("UTF-8"));
    hash = ((BufferServletOutputStream) response.getOutputStream()).getContentHash();
    assertEquals(SECOND_ETAG, hash);

    response.flushBuffer();
    assertResponseBodyIsEmpty();
    control.verify();
  }

  private void expectRequestETag(String... eTag) {
    String eTags = null;
    if (eTag.length > 0) {
      eTags = Joiner.on(',').join(Lists.transform(Arrays.asList(eTag), ETAG_QUOTER));
    }
    EasyMock.expect(request.getHeader(ETaggingHttpResponse.REQUEST_HEADER)).andReturn(eTags);
  }

  private void expectFullResponse() {
    origResponse.setHeader(ETaggingHttpResponse.RESPONSE_HEADER, '"' + GOOD_ETAG + '"');
    origResponse.setContentLength(RESPONSE_BODY_LENGTH);
  }

  private void expectNotModifiedResponse(String eTag) {
    origResponse.setHeader(ETaggingHttpResponse.RESPONSE_HEADER, '"' + eTag + '"');
    origResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    origResponse.setContentLength(0);
  }

  private void assertResponseHasBody() {
    assertArrayEquals(RESPONSE_BODY_BYTES, stream.getBuffer());
  }

  private void assertResponseBodyIsEmpty() {
    assertEquals(0, stream.getBuffer().length);
  }

  private class MockServletOutputStream extends ServletOutputStream {
    private ByteArrayBuffer buffer = new ByteArrayBuffer(1024);

    @Override
    public void write(int b) {
      buffer.append(b);
    }

    public byte[] getBuffer() {
      return buffer.toByteArray();
    }
  }
}
