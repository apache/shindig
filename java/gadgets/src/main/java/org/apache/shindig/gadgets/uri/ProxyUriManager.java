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

import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.util.List;

public interface ProxyUriManager {
  /**
   * Generate a Uri that proxies the given resource Uri.
   * 
   * @param resource Resource Uri to proxy
   * @param forcedRefresh Forced expires value to use for resource
   * @return Uri of proxied resource
   */
  List<Uri> make(List<ProxyUri> resource, Integer forcedRefresh);
  
  public static class ProxyUri extends ProxyUriBase {
    private final Uri resource;
    private String fallbackUrl;
    private Integer resizeHeight;
    private Integer resizeWidth;
    private Integer resizeQuality;
    private boolean resizeNoExpand;
    
    public ProxyUri(Gadget gadget, Uri resource) {
      super(gadget);
      this.resource = resource;
    }

    public ProxyUri(Integer refresh, boolean debug, boolean noCache,
        String container, String gadget, Uri resource) {
      super(null, refresh, debug, noCache, container, gadget);
      this.resource = resource;
    }

    public ProxyUri(UriStatus status, Uri resource, Uri base) {
      super(status, base);
      this.resource = resource;
    }
    
    /* (non-Javadoc)
     * @see org.apache.shindig.gadgets.uri.ProxyUriBase#setFromUri(org.apache.shindig.common.uri.Uri)
     */
    @Override
    public void setFromUri(Uri uri) {
      super.setFromUri(uri);
      if (uri != null) {
        fallbackUrl = uri.getQueryParameter(Param.FALLBACK_URL_PARAM.getKey());
        resizeHeight = getIntegerValue(uri.getQueryParameter(Param.RESIZE_HEIGHT.getKey()));
        resizeWidth = getIntegerValue(uri.getQueryParameter(Param.RESIZE_WIDTH.getKey()));
        resizeQuality = getIntegerValue(uri.getQueryParameter(Param.RESIZE_QUALITY.getKey()));
        resizeNoExpand = getBooleanValue(uri.getQueryParameter(Param.NO_EXPAND.getKey()));
      }
    }

    public ProxyUri setResize(Integer w, Integer h, Integer q, boolean noExpand) {
      this.resizeHeight = h;
      this.resizeWidth = w;
      this.resizeQuality = q;
      this.resizeNoExpand = noExpand;
      return this;
    }

    public ProxyUri setFallbackUrl(String fallbackUrl) {
      this.fallbackUrl = fallbackUrl;
      return this;
    }

    public Uri getResource() {
      return resource;
    }
    
    public Uri getFallbackUri() throws GadgetException {
      if (fallbackUrl == null) {
        return null;
      }
      try {
        // Doing delay parsing.
        return Uri.parse(fallbackUrl);
      } catch (IllegalArgumentException e) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            Param.FALLBACK_URL_PARAM.getKey() + " param is invalid: "
            + e, HttpResponse.SC_BAD_REQUEST);
      }
    }

    @Override
    public UriBuilder makeQueryParams(Integer forcedRefresh, String version) {
      UriBuilder builder = super.makeQueryParams(forcedRefresh, version);
      if (resizeHeight != null) {
        builder.addQueryParameter(Param.RESIZE_HEIGHT.getKey(), resizeHeight.toString());
      }
      if (resizeWidth != null) {
        builder.addQueryParameter(Param.RESIZE_WIDTH.getKey(), resizeWidth.toString());
      }
      if (resizeQuality != null) {
        builder.addQueryParameter(Param.RESIZE_QUALITY.getKey(), resizeQuality.toString());
      }
      if (resizeNoExpand) {
        builder.addQueryParameter(Param.NO_EXPAND.getKey(), "1");
      }
      if (fallbackUrl != null) {
        builder.addQueryParameter(Param.FALLBACK_URL_PARAM.getKey(), fallbackUrl);
      }
      return builder;
    }
    
    @Override
    public HttpRequest makeHttpRequest(Uri targetUri) 
        throws GadgetException {
      HttpRequest req = super.makeHttpRequest(targetUri);
      // Set image params:
      req.setParam(Param.RESIZE_HEIGHT.getKey(), resizeHeight);
      req.setParam(Param.RESIZE_WIDTH.getKey(), resizeWidth);
      req.setParam(Param.RESIZE_QUALITY.getKey(), resizeQuality);
      req.setParam(Param.NO_EXPAND.getKey(), resizeNoExpand);
      return req;
    };
    
    
    public static List<ProxyUri> fromList(Gadget gadget, List<Uri> uris) {
      List<ProxyUri> res = Lists.newArrayListWithCapacity(uris.size());
      for (Uri uri : uris) {
        res.add(new ProxyUri(gadget, uri));
      }
      return res;
    }
  }
  
  /**
   * Parse and validate the proxied Uri.
   * 
   * @param uri A Uri presumed to be a proxied Uri generated
   *     by this class or in a compatible way
   * @return Status of the Uri passed in
   */
  ProxyUri process(Uri uri) throws GadgetException;
  
  public interface Versioner {
    /**
     * Generates a version for each of the provided resources.
     * @param resources Resources to version.
     * @param container Container making the request
     * @return Index-correlated list of version strings
     */
    List<String> version(List<Uri> resources, String container);
    
    /**
     * Validate the version of the resource.
     * @param resource Uri of a proxied resource
     * @param container Container requesting the resource
     * @param value Version value to validate.
     * @return Status of the version.
     */
    UriStatus validate(Uri resource, String container, String value);
  }
}
