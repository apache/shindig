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

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

public class ProxyUriBase {
  private final UriStatus status;
  private final Integer refresh;
  private final boolean debug;
  private final boolean noCache;
  private final String container;
  private final String gadget;

  protected ProxyUriBase(Gadget gadget) {
    this(null,  // Meaningless in "context" mode. translateStatusRefresh invalid here.
         parseRefresh(gadget.getContext().getParameter(Param.REFRESH.getKey())),
         gadget.getContext().getDebug(),
         gadget.getContext().getIgnoreCache(),
         gadget.getContext().getContainer(),
         gadget.getSpec().getUrl().toString());
  }
  
  protected ProxyUriBase(UriStatus status, Uri origUri) {
    this(status,
         origUri != null ? parseRefresh(origUri.getQueryParameter(Param.REFRESH.getKey())) : null,
         origUri != null ? getBooleanValue(origUri.getQueryParameter(Param.DEBUG.getKey())) : false,
         origUri != null ? getBooleanValue(origUri.getQueryParameter(Param.NO_CACHE.getKey())) : false,
         origUri != null ? origUri.getQueryParameter(Param.CONTAINER.getKey()) : null,
         origUri != null ? origUri.getQueryParameter(Param.GADGET.getKey()) : null);
  }
  
  private ProxyUriBase(UriStatus status, Integer refresh, boolean debug, boolean noCache,
      String container, String gadget) {
    this.status = status;
    this.refresh = refresh;
    this.debug = debug;
    this.noCache = noCache;
    this.container = container;
    this.gadget = gadget;
  }

  public UriStatus getStatus() {
    return status;
  }

  public Integer getRefresh() {
    return noCache ? Integer.valueOf(0) : refresh;
  }

  public boolean isDebug() {
    return debug;
  }

  public boolean isNoCache() {
    return noCache;
  }

  public String getContainer() {
    return container;
  }

  public String getGadget() {
    return gadget;
  }
  
  public HttpRequest makeHttpRequest(Uri targetUri) throws GadgetException {
    HttpRequest req = new HttpRequest(targetUri)
        .setIgnoreCache(isNoCache())
        .setContainer(getContainer());
    if (!StringUtils.isEmpty(getGadget())) {
      try {
        req.setGadget(Uri.parse(getGadget()));
      } catch (IllegalArgumentException e) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            "Invalid " + Param.GADGET.getKey() + " param: " + getGadget(),
            HttpResponse.SC_BAD_REQUEST);
      }
    }
    if (getRefresh() != null && getRefresh() >= 0) {
      req.setCacheTtl(getRefresh());
    }
    return req;
  }
  
  public Integer translateStatusRefresh(int longVal, int defaultVal)
      throws GadgetException {
    Integer retRefresh = 0;
    switch (getStatus()) {
    case VALID_VERSIONED:
      retRefresh = longVal;
      break;
    case VALID_UNVERSIONED:
      retRefresh = defaultVal;
      break;
    case INVALID_VERSION:
      retRefresh = 0;
      break;
    case INVALID_DOMAIN:
      throw new GadgetException(GadgetException.Code.INVALID_PATH,
          "Invalid path", HttpResponse.SC_BAD_REQUEST);
    case BAD_URI:
      throw new GadgetException(GadgetException.Code.INVALID_PATH,
          "Invalid path", HttpResponse.SC_BAD_REQUEST);
    default:
      // Should never happen.
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          "Unknown status: " + getStatus());
    }
    Integer setVal = getRefresh();
    if (setVal != null) {
      // Override always wins.
      if (setVal != -1) {
        retRefresh = setVal;
      }
    }
    return retRefresh;
  }

  private static boolean getBooleanValue(String str) {
    if (str != null && "1".equals(str)) {
      return true;
    }
    return false;
  }
  
  private static Integer parseRefresh(String refreshStr) {
    Integer refreshVal = null;
    if (refreshStr != null) {
      try {
        refreshVal = Integer.parseInt(refreshStr);
      } catch (NumberFormatException e) {
        // -1 is sentinel for invalid value.
        refreshVal = -1;
      }
    }
    return refreshVal;
  }
}
