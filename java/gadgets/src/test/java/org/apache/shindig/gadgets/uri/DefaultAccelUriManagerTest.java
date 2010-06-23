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
import org.apache.shindig.config.AbstractContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Tests for DefaultAccelUriManager.
 */
public class DefaultAccelUriManagerTest {
  private static class FakeContainerConfig extends AbstractContainerConfig {
    protected final Map<String, Object> data = ImmutableMap.<String, Object>builder()
        .put(AccelUriManager.PROXY_HOST_PARAM, "apache.org")
        .put(AccelUriManager.PROXY_PATH_PARAM, "/gadgets/accel")
        .build();

    @Override
    public Object getProperty(String container, String name) {
      return data.get(name);
    }
  }

  DefaultAccelUriManager uriManager;

  @Before
  public void setUp() throws Exception {
    ContainerConfig config = new FakeContainerConfig();
    uriManager = new DefaultAccelUriManager(config, new DefaultProxyUriManager(
        config, null));
  }

  @Test
  public void testParseAndNormalizeNonAccelUri() throws Exception {
    Uri uri = Uri.parse("http://www.example.org/index.html");
    assertEquals(Uri.parse("//apache.org/gadgets/accel?container=default"
                 + "&gadget=http%3A%2F%2Fwww.example.org%2Findex.html"
                 + "&debug=0&nocache=0&refresh=0"
                 + "&url=http%3A%2F%2Fwww.example.org%2Findex.html"),
                 uriManager.parseAndNormalize(uri));
  }

  @Test
  public void testParseAndNormalizeAccelUri() throws Exception {
    Uri uri = Uri.parse("http://apache.org/gadgets/accel?container=proxy"
                        + "&gadget=http%3A%2F%2Fwww.1.com%2Fa.html"
                        + "&url=http%3A%2F%2Fwww.example.org%2Findex.html");
    assertEquals(Uri.parse("//apache.org/gadgets/accel?container=proxy"
                 + "&gadget=http%3A%2F%2Fwww.1.com%2Fa.html"
                 + "&debug=0&nocache=0&refresh=0"
                 + "&url=http%3A%2F%2Fwww.example.org%2Findex.html"),
                 uriManager.parseAndNormalize(uri));
  }

  @Test
  public void testLooksLikeAccelUri() throws Exception {
    Uri uri = Uri.parse("http://apache.org/gadgets/accel?container=proxy"
                        + "&gadget=http%3A%2F%2Fwww.1.com%2Fa.html"
                        + "&url=http%3A%2F%2Fwww.example.org%2Findex.html");
    assertTrue(uriManager.looksLikeAccelUri(uri));

    uri = Uri.parse("http://www.example.org/index.html");
    assertFalse(uriManager.looksLikeAccelUri(uri));
  }
}
