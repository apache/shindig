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

import com.google.common.collect.Lists;

import org.apache.http.util.ByteArrayBuffer;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link ETagFilter}.
 */
public class ETagFilterTest {

  // Using Spanish string and UTF-8 encoding to check for encoding errors.
  private static final String ENCODING = "UTF-8";
  private static final byte[] RESPONSE_BODY_BYTES =
      new byte[] {-62, -95, 72, 111, 108, 97, 44, 32, 110, 105, -61, -79, 111, 33};
  private static final int RESPONSE_BODY_LENGTH = RESPONSE_BODY_BYTES.length;
  private static final String GOOD_ETAG = "dae018f624d09423e7c4d7209fbea597";

  private IMocksControl control;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private MockFilterChain chain;
  private MockServletOutputStream stream;
  private ETagFilter filter;

  @Before
  public void setUp() throws Exception {
    control = EasyMock.createControl();
    request = control.createMock(HttpServletRequest.class);
    response = control.createMock(HttpServletResponse.class);
    chain = new MockFilterChain();
    stream = new MockServletOutputStream();
    filter = new ETagFilter();

    EasyMock.expect(response.isCommitted()).andReturn(false).anyTimes();
    EasyMock.expect(response.getOutputStream()).andReturn(stream).anyTimes();
    EasyMock.expect(response.getCharacterEncoding()).andReturn(ENCODING).anyTimes();
    EasyMock.expect(request.getHeader(ETaggingHttpResponse.REQUEST_HEADER)).andReturn(null);
    response.setHeader(ETaggingHttpResponse.RESPONSE_HEADER, '"' + GOOD_ETAG + '"');
    response.setContentLength(RESPONSE_BODY_LENGTH);
  }

  @Test
  public void testTagContent() throws Exception {
    chain.write(RESPONSE_BODY_BYTES);
    control.replay();

    filter.doFilter(request, response, chain);
    assertArrayEquals(RESPONSE_BODY_BYTES, stream.getBuffer());
    control.verify();
  }

  @Test
  public void testTagContentOnException() throws Exception {
    chain.write(RESPONSE_BODY_BYTES);
    chain.throwException();
    control.replay();

    try {
      filter.doFilter(request, response, chain);
      fail("Should have thrown an IOException");
    } catch (IOException e) {
      // pass
    }
    assertArrayEquals(RESPONSE_BODY_BYTES, stream.getBuffer());
    control.verify();
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

  private class MockFilterChain implements FilterChain {
    private List<Object> commands = Lists.newArrayList();

    public void write(byte[] s) {
      commands.add(s);
    }

    public void throwException() {
      commands.add(new IOException());
    }

    public void doFilter(ServletRequest request, ServletResponse response) throws IOException {
      for (Object cmd : commands) {
        if (cmd instanceof byte[]) {
          response.getOutputStream().write((byte[]) cmd);
        } else if (cmd instanceof IOException) {
          throw (IOException) cmd;
        }
      }
    }
  }
}
