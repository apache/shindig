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

import org.apache.http.HttpEntity;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BasicHttpFetcherTest {
  private static final int ECHO_PORT = 9003;
  protected static final Uri BASE_URL = Uri.parse("http://localhost:9003/");
  private static EchoServer server;

  protected BasicHttpFetcher fetcher = null;
  protected HttpEntity mockEntity;
  protected InputStream mockInputStream;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    server = new EchoServer();
    server.start(ECHO_PORT);
  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Before
  public void setUp() throws Exception {
    fetcher = new BasicHttpFetcher(BASE_URL.getAuthority());

    mockInputStream = EasyMock.createMock(InputStream.class);
    EasyMock.expect(mockInputStream.available()).andReturn(0);
    mockInputStream.close();

    mockEntity = EasyMock.createMock(HttpEntity.class);
    EasyMock.expect(mockEntity.getContent()).andReturn(mockInputStream);
    EasyMock.expect(mockEntity.getContentLength()).andReturn(16384L).anyTimes();
  }

  @Test
  public void testWithProxy() throws Exception {
    String content = "Hello, Gagan!";
    Uri uri = new UriBuilder(Uri.parse("http://www.google.com/search"))
        .addQueryParameter("body", content)
        .addQueryParameter("status", "201")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(201, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
  }

  @Test
  public void testToByteArraySafeThrowsException1() throws Exception {
    EasyMock.reset(mockInputStream);
    mockInputStream.close();

    String exceptionMessage = "IO Exception and Any Random Cause";
    IOException e = new IOException(exceptionMessage);
    EasyMock.expect(mockInputStream.read(EasyMock.isA(byte[].class))).andThrow(e).anyTimes();

    EasyMock.replay(mockEntity, mockInputStream);
    boolean exceptionCaught = false;

    try {
      fetcher.toByteArraySafe(mockEntity);
    } catch (IOException ioe) {
      assertEquals(exceptionMessage, ioe.getMessage());
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    EasyMock.verify(mockEntity, mockInputStream);
  }

  @Test
  public void testToByteArraySafeThrowsException2() throws Exception {
    String exceptionMessage = "EOF Exception and Any Random Cause";
    EOFException e = new EOFException(exceptionMessage);
    EasyMock.expect(mockInputStream.read(EasyMock.isA(byte[].class))).andThrow(e).anyTimes();

    EasyMock.replay(mockEntity, mockInputStream);
    boolean exceptionCaught = false;

    try {
      fetcher.toByteArraySafe(mockEntity);
    } catch (EOFException eofe) {
      assertEquals(exceptionMessage, eofe.getMessage());
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    EasyMock.verify(mockEntity, mockInputStream);
  }

  @Test
  public void testToByteArraySafeThrowsException3() throws Exception {
    EasyMock.reset(mockInputStream);
    mockInputStream.close();

    // Return non-zero for 'InputStream.available()'. This should violate the other condition.
    EasyMock.expect(mockInputStream.available()).andReturn(1);
    String exceptionMessage = "Unexpected end of ZLIB input stream";
    EOFException e = new EOFException(exceptionMessage);
    EasyMock.expect(mockInputStream.read(EasyMock.isA(byte[].class))).andThrow(e).anyTimes();

    EasyMock.replay(mockEntity, mockInputStream);
    boolean exceptionCaught = false;

    try {
      fetcher.toByteArraySafe(mockEntity);
    } catch (EOFException eofe) {
      assertEquals(exceptionMessage, eofe.getMessage());
      exceptionCaught = true;
    }
    EasyMock.verify(mockEntity, mockInputStream);
    assertTrue(exceptionCaught);
  }

  @Test
  public void testToByteArraySafeHandleException() throws Exception {
    String exceptionMessage = "Unexpected end of ZLIB input stream";
    EOFException e = new EOFException(exceptionMessage);
    EasyMock.expect(mockInputStream.read(EasyMock.isA(byte[].class))).andThrow(e).anyTimes();

    EasyMock.replay(mockEntity, mockInputStream);

    try {
      fetcher.toByteArraySafe(mockEntity);
    } catch (EOFException eofe) {
      fail("Exception Should have been caught");
    }
    EasyMock.verify(mockEntity, mockInputStream);
  }

  @Test
  public void testToByteArraySafeHandlesExceptionWithNoMessage() throws Exception {
    EOFException e = new EOFException();
    EasyMock.expect(mockInputStream.read(EasyMock.isA(byte[].class))).andThrow(e).anyTimes();

    EasyMock.replay(mockEntity, mockInputStream);

    try {
      fetcher.toByteArraySafe(mockEntity);
    } catch (EOFException eofe) {
      fail("Exception Should have been caught");
    }
    EasyMock.verify(mockEntity, mockInputStream);
  }

  /*
   * https://issues.apache.org/jira/browse/SHINDIG-1425
   */
  @Test
  public void testHeadWithMaxObjectSizeBytes() throws Exception {
	fetcher.setMaxObjectSizeBytes(1024 * 1024);
    Uri uri = new UriBuilder(Uri.parse("http://www.google.com/search"))
        .addQueryParameter("body", "")
        .addQueryParameter("status", "200")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    request.setMethod("HEAD");
    HttpResponse response = fetcher.fetch(request);
    assertEquals(200, response.getHttpStatusCode());
    assertEquals("", response.getResponseAsString());
  }
}
