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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;
import org.easymock.Capture;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import java.util.List;

public class SanitizingProxyUriManagerTest {
  private ProxyUriManager uriManager;
  private Uri uri;
  private ProxyUri proxyUri;

  @Before
  public void setUp() throws Exception {
    uriManager = createMock(ProxyUriManager.class);
    uri = new UriBuilder().setScheme("http").setAuthority("host.com").setPath("/path").toUri();
    proxyUri = createMock(ProxyUri.class);
  }

  @Test
  public void processPassesThrough() throws Exception {
    Capture<Uri> uriCapture = new Capture<Uri>();
    expect(uriManager.process(capture(uriCapture))).andReturn(proxyUri).once();
    replay(uriManager);

    SanitizingProxyUriManager rewriter = makeRewriter(null);
    ProxyUri returned = rewriter.process(uri);

    verify(uriManager);
    assertSame(uri, uriCapture.getValue());
    assertSame(returned, proxyUri);
  }

  @Test
  public void makeSingleNoMime() throws Exception {
    Capture<List<ProxyUri>> uriCapture = new Capture<List<ProxyUri>>();
    Capture<Integer> intCapture = new Capture<Integer>();
    List<ProxyUri> input = Lists.newArrayList(proxyUri);
    List<Uri> output = Lists.newArrayList(uri);
    Integer refresh = new Integer(0);
    expect(uriManager.make(capture(uriCapture), capture(intCapture)))
        .andReturn(output).once();
    replay(uriManager);
    expect(proxyUri.setSanitizeContent(true)).andReturn(proxyUri).once();
    replay(proxyUri);

    SanitizingProxyUriManager rewriter = makeRewriter(null);
    List<Uri> returned = rewriter.make(input, refresh);

    verify(uriManager);
    assertSame(uriCapture.getValue(), input);
    assertSame(intCapture.getValue(), refresh);
    assertEquals(1, returned.size());
    verify(proxyUri);
  }

  @Test
  public void makeSingleExpectedMime() throws Exception {
    Capture<List<ProxyUri>> uriCapture = new Capture<List<ProxyUri>>();
    Capture<Integer> intCapture = new Capture<Integer>();
    List<ProxyUri> input = Lists.newArrayList(proxyUri);
    List<Uri> output = Lists.newArrayList(uri);
    Integer refresh = new Integer(0);
    String mime = "my/mime";
    expect(uriManager.make(capture(uriCapture), capture(intCapture)))
        .andReturn(output).once();
    replay(uriManager);
    expect(proxyUri.setSanitizeContent(true)).andReturn(proxyUri).once();
    expect(proxyUri.setRewriteMimeType(mime)).andReturn(proxyUri).once();
    replay(proxyUri);

    SanitizingProxyUriManager rewriter = makeRewriter(mime);
    List<Uri> returned = rewriter.make(input, refresh);

    verify(uriManager);
    assertSame(uriCapture.getValue(), input);
    assertSame(intCapture.getValue(), refresh);
    assertEquals(1, returned.size());
    verify(proxyUri);
  }

  @Test
  public void makeList() throws Exception {
    Capture<List<ProxyUri>> uriCapture = new Capture<List<ProxyUri>>();
    Capture<Integer> intCapture = new Capture<Integer>();
    ProxyUri proxyUri2 = createMock(ProxyUri.class);
    List<ProxyUri> input = Lists.newArrayList(proxyUri, proxyUri2);
    Uri uri2 = new UriBuilder().toUri();
    List<Uri> output = Lists.newArrayList(uri, uri2);
    Integer refresh = new Integer(0);
    String mime = "my/mime";
    expect(uriManager.make(capture(uriCapture), capture(intCapture)))
        .andReturn(output).once();
    replay(uriManager);
    expect(proxyUri.setSanitizeContent(true)).andReturn(proxyUri).once();
    expect(proxyUri.setRewriteMimeType(mime)).andReturn(proxyUri).once();
    expect(proxyUri2.setSanitizeContent(true)).andReturn(proxyUri2).once();
    expect(proxyUri2.setRewriteMimeType(mime)).andReturn(proxyUri2).once();
    replay(proxyUri, proxyUri2);

    SanitizingProxyUriManager rewriter = makeRewriter(mime);
    List<Uri> returned = rewriter.make(input, refresh);

    verify(uriManager);
    assertSame(uriCapture.getValue(), input);
    assertSame(intCapture.getValue(), refresh);
    assertEquals(2, returned.size());
    verify(proxyUri, proxyUri2);
  }

  private SanitizingProxyUriManager makeRewriter(String mime) {
    return new SanitizingProxyUriManager(uriManager, mime);
  }
}
