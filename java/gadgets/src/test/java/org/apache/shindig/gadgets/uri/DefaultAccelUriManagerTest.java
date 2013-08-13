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

import com.google.common.collect.ImmutableMap;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for DefaultAccelUriManager.
 */
public class DefaultAccelUriManagerTest {
  DefaultAccelUriManager uriManager;
  private ContainerConfig config;

  private Map<String, Object> makeConfig(String name, String path) {
    return ImmutableMap
        .<String, Object>builder()
        .put(ContainerConfig.CONTAINER_KEY, name)
        .put(AccelUriManager.PROXY_HOST_PARAM, "apache.org")
        .put(AccelUriManager.PROXY_PATH_PARAM, path)
        .build();
  }

  @Before
  public void setUp() throws Exception {
    config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(makeConfig("default", "/gadgets/proxy"))
        .addContainer(makeConfig("accel", "/gadgets/accel"))
        .commit();
    uriManager = new DefaultAccelUriManager(config, new DefaultProxyUriManager(
        config, null));
  }

  @Test
  public void testParseAndNormalizeNonAccelUri() throws Exception {
    Uri uri = Uri.parse("http://www.example.org/index.html");
    HttpRequest req = new HttpRequest(uri);
    assertEquals(Uri.parse("//apache.org/gadgets/accel?container=accel"
                 + "&gadget=http%3A%2F%2Fwww.example.org%2Findex.html"
                 + "&debug=0&nocache=0&refresh=0"
                 + "&rooe=1&url=http%3A%2F%2Fwww.example.org%2Findex.html"),
                 uriManager.parseAndNormalize(req));

    uri = Uri.parse("http://www.example.org/index.html");
    req = new HttpRequest(uri);
    req.setContainer("accel");
    assertEquals(Uri.parse("//apache.org/gadgets/accel?container=accel"
                 + "&gadget=http%3A%2F%2Fwww.example.org%2Findex.html"
                 + "&debug=0&nocache=0&refresh=0"
                 + "&rooe=1&url=http%3A%2F%2Fwww.example.org%2Findex.html"),
                 uriManager.parseAndNormalize(req));
  }

  @Test
  public void testParseAndNormalizeAccelUri() throws Exception {
    Uri uri = Uri.parse("http://apache.org/gadgets/accel?container=accel"
                        + "&gadget=http%3A%2F%2Fwww.1.com%2Fa.html"
                        + "&url=http%3A%2F%2Fwww.example.org%2Findex.html");
    HttpRequest req = new HttpRequest(uri);
    assertEquals(Uri.parse("//apache.org/gadgets/accel?container=accel"
                 + "&gadget=http%3A%2F%2Fwww.1.com%2Fa.html"
                 + "&debug=0&nocache=0&refresh=0"
                 + "&url=http%3A%2F%2Fwww.example.org%2Findex.html"),
                 uriManager.parseAndNormalize(req));
  }

  @Test
  public void testLooksLikeAccelUri() throws Exception {
    Uri uri = Uri.parse("http://apache.org/gadgets/accel?container=accel"
                        + "&gadget=http%3A%2F%2Fwww.1.com%2Fa.html"
                        + "&url=http%3A%2F%2Fwww.example.org%2Findex.html");
    assertTrue(uriManager.looksLikeAccelUri(uri));

    uri = Uri.parse("http://www.example.org/index.html");
    assertFalse(uriManager.looksLikeAccelUri(uri));
  }

  @Test
  public void testContainersChange() throws Exception {
    String beforeUrl = "//apache.org/gadgets/accel?container=accel"
        + "&gadget=http%3A%2F%2Fwww.example.org%2Findex.html"
        + "&debug=0&nocache=0&refresh=0&rooe=1&url=http%3A%2F%2Fwww.example.org%2Findex.html";
    String afterUrl = "//apache.org/random/url?container=accel"
        + "&gadget=http%3A%2F%2Fwww.example.org%2Findex.html"
        + "&debug=0&nocache=0&refresh=0&rooe=1&url=http%3A%2F%2Fwww.example.org%2Findex.html";

    Uri uri = Uri.parse("http://www.example.org/index.html");
    HttpRequest req = new HttpRequest(uri);
    req.setContainer("accel");
    assertEquals(Uri.parse(beforeUrl), uriManager.parseAndNormalize(req));
    assertTrue(uriManager.looksLikeAccelUri(Uri.parse(beforeUrl)));
    assertFalse(uriManager.looksLikeAccelUri(Uri.parse(afterUrl)));

    config.newTransaction().addContainer(makeConfig("accel", "/random/url")).commit();

    uri = Uri.parse("http://www.example.org/index.html");
    req = new HttpRequest(uri);
    req.setContainer("accel");
    assertEquals(Uri.parse(afterUrl), uriManager.parseAndNormalize(req));
    assertFalse(uriManager.looksLikeAccelUri(Uri.parse(beforeUrl)));
    assertTrue(uriManager.looksLikeAccelUri(Uri.parse(afterUrl)));
  }
}
