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

import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;

import com.google.caja.util.Maps;

public class FakeIframeUriManager implements IframeUriManager {
  protected boolean throwRandomFault = false;
  private static final String URI = "http://example.org/gadgets/foo-does-not-matter";
  public static final Uri DEFAULT_IFRAME_URI = Uri.parse(URI);
  protected Uri iframeUrl = DEFAULT_IFRAME_URI;
  public static Map<String, Uri> IFRAME_URIS = Maps.newHashMap();
  public static Map<String, String> IFRAME_URIS_STRINGS = Maps.newHashMap();
  static {
    IFRAME_URIS.put("default", DEFAULT_IFRAME_URI);
    IFRAME_URIS_STRINGS.put("default", URI);
  }

  FakeIframeUriManager() { }

  public Uri makeRenderingUri(Gadget gadget) {
    if (throwRandomFault) {
      throw new RuntimeException("BROKEN");
    }
    return iframeUrl;
  }

  public UriStatus validateRenderingUri(Uri uri) {
    throw new UnsupportedOperationException();
  }

  public Map<String, Uri> makeAllRenderingUris(Gadget gadget) {
    return IFRAME_URIS;
  }
}
