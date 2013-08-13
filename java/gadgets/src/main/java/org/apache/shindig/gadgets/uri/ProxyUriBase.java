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

import com.google.common.base.Objects;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.util.Map;

/**
 * Represents state/config information for the proxy.
 *
 * @since 2.0.0
 */
public class ProxyUriBase {

  @Inject(optional=true)
  @Named("org.apache.shindig.gadgets.uri.setAuthorityAsGadgetParam")
  static private boolean setAuthorityAsGadgetParam = false;

  private UriStatus status = null;
  private Integer refresh = null;
  private boolean debug = false;
  private boolean noCache = false;
  private String container = ContainerConfig.DEFAULT_CONTAINER;
  private String gadget = null;
  private String rewriteMimeType = null;
  private boolean sanitizeContent = false;
  private boolean cajoleContent = false;
  /** Extension parameters to support implementation specific flags */
  protected Map<String, String> extensionParams = null;

  protected ProxyUriBase(Gadget gadget) {
    this(null,  // Meaningless in "context" mode. translateStatusRefresh invalid here.
         getIntegerValue(gadget.getContext().getParameter(Param.REFRESH.getKey())),
         gadget.getContext().getDebug(),
         gadget.getContext().getIgnoreCache(),
         gadget.getContext().getContainer(),
         setAuthorityAsGadgetParam ? gadget.getSpec().getUrl().getAuthority() :
             gadget.getSpec().getUrl().toString());
  }

  protected ProxyUriBase(UriStatus status, Uri origUri) {
    this.status = status;
    setFromUri(origUri);
  }

  protected ProxyUriBase(UriStatus status, Integer refresh, boolean debug, boolean noCache,
      String container, String gadget) {
    this.status = status;
    this.refresh = refresh;
    this.debug = debug;
    this.noCache = noCache;
    this.container = container;
    this.gadget = gadget;
  }

  /**
   * Parse uri query paramaters.
   * Note this function is called by a constructor,
   * and can be override to handle derived class parsing
   */
  @SuppressWarnings("deprecation") // we still need to support SYND while parsing
  public void setFromUri(Uri uri) {
    if (uri != null) {
      refresh = getIntegerValue(uri.getQueryParameter(Param.REFRESH.getKey()));
      debug = getTruthyValue(uri.getQueryParameter(Param.DEBUG.getKey()));
      noCache = getBooleanValue(uri.getQueryParameter(Param.NO_CACHE.getKey()));
      String newContainer = uri.getQueryParameter(Param.CONTAINER.getKey());
      if (newContainer == null) {
        // Support "synd" for legacy purposes.
        newContainer = uri.getQueryParameter(Param.SYND.getKey());
      }
      // Preserve "default" container.
      if (newContainer != null) {
        container = newContainer;
      }
      gadget = uri.getQueryParameter(Param.GADGET.getKey());
      rewriteMimeType = uri.getQueryParameter(Param.REWRITE_MIME_TYPE.getKey());
      sanitizeContent = getBooleanValue(uri.getQueryParameter(Param.SANITIZE.getKey()));
      cajoleContent = getBooleanValue(uri.getQueryParameter(Param.CAJOLE.getKey()));
    }
  }

  protected static boolean getTruthyValue(String str) {
    return str != null && !"0".equals(str) && str.length() > 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ProxyUriBase)) {
      return false;
    }
    ProxyUriBase objUri = (ProxyUriBase) obj;
    return (Objects.equal(this.status, objUri.status)
        && (noCache || Objects.equal(this.refresh, objUri.refresh))
        && Objects.equal(this.container, objUri.container)
        && Objects.equal(this.gadget, objUri.gadget)
        && Objects.equal(this.rewriteMimeType, objUri.rewriteMimeType)
        && this.noCache == objUri.noCache
        && this.debug == objUri.debug
        && this.sanitizeContent == objUri.sanitizeContent
        && this.cajoleContent == objUri.cajoleContent
        && Objects.equal(this.extensionParams, objUri.extensionParams));

  }

  @Override
  public int hashCode() {
    return Objects.hashCode(status, refresh, container, gadget, rewriteMimeType,
            noCache, debug, sanitizeContent, cajoleContent);
  }

  public ProxyUriBase setRewriteMimeType(String type) {
    this.rewriteMimeType = type;
    return this;
  }

  public ProxyUriBase setSanitizeContent(boolean sanitize) {
    this.sanitizeContent = sanitize;
    return this;
  }

  public ProxyUriBase setCajoleContent(boolean cajole) {
    this.cajoleContent = cajole;
    return this;
  }

  public ProxyUriBase setGadget(String gadget) {
    this.gadget = gadget;
    return this;
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

  public String getRewriteMimeType() {
    return rewriteMimeType;
  }

  public boolean sanitizeContent() {
    return sanitizeContent;
  }

  public boolean cajoleContent() {
    return cajoleContent;
  }

  public void setExtensionParam(String key, String val) {
    if (extensionParams == null) {
      extensionParams = Maps.newHashMap();
    }
    extensionParams.put(key, val);
  }

  public String getExtensionParam(String key) {
    if (extensionParams == null) {
      return null;
    }
    return extensionParams.get(key);
  }

  public ProxyUriBase setExtensionParams(Map<String, String> extensionParams) {
    this.extensionParams = extensionParams;
    return this;
  }

  public Map<String, String> getExtensionParams() {
    return extensionParams;
  }

  public HttpRequest makeHttpRequest(Uri targetUri) throws GadgetException {
    HttpRequest req = new HttpRequest(targetUri)
        .setIgnoreCache(isNoCache())
        .setContainer(getContainer());
    if (!Strings.isNullOrEmpty(getGadget())) {
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

    // Allow the rewriter to use an externally forced MIME type. This is needed
    // allows proper rewriting of <script src="x"/> where x is returned with
    // a content type like text/html which unfortunately happens all too often
    if (rewriteMimeType != null) {
      req.setRewriteMimeType(getRewriteMimeType());
    }
    req.setSanitizationRequested(sanitizeContent());
    req.setCajaRequested(cajoleContent());

    return req;
  }

  /**
   * Construct the query parameters for proxy url
   * @param forcedRefresh optional overwrite the refresh time
   * @param version optional version
   * @return Url with only query parameters set
   */
  public UriBuilder makeQueryParams(Integer forcedRefresh, String version) {
    UriBuilder queryBuilder = new UriBuilder();

    // Add all params common to both chained and query syntax.
    String container = getContainer();
    queryBuilder.addQueryParameter(Param.CONTAINER.getKey(), container);
    if (getGadget() != null) {
      queryBuilder.addQueryParameter(Param.GADGET.getKey(), getGadget());
    }
    queryBuilder.addQueryParameter(Param.DEBUG.getKey(), isDebug() ? "1" : "0");
    queryBuilder.addQueryParameter(Param.NO_CACHE.getKey(), isNoCache() ? "1" : "0");
    if (!isNoCache()) {
      if (forcedRefresh != null && forcedRefresh >= 0) {
        queryBuilder.addQueryParameter(Param.REFRESH.getKey(), forcedRefresh.toString());
      } else if (getRefresh() != null) {
        queryBuilder.addQueryParameter(Param.REFRESH.getKey(), getRefresh().toString());
      }
    }

    if (version != null) {
      queryBuilder.addQueryParameter(Param.VERSION.getKey(), version);
    }
    if (rewriteMimeType != null) {
      queryBuilder.addQueryParameter(Param.REWRITE_MIME_TYPE.getKey(), rewriteMimeType);
    }
    if (sanitizeContent) {
      queryBuilder.addQueryParameter(Param.SANITIZE.getKey(), "1");
    }
    if (cajoleContent) {
      queryBuilder.addQueryParameter(Param.CAJOLE.getKey(), "1");
    }
    if (extensionParams != null) {
      queryBuilder.addQueryParameters(extensionParams);
    }
    return queryBuilder;
  }

  /**
   * Calculate cache time for a resource url. Provide long period for versioned resource,
   * use original value for unversioned resource, and no cache for invalid versions.
   * Invalid version can happen in multiple instances environemnt where the url can be
   * created on one server and the actually retreival is done on another that have an older version
   * of resource in cache. In that case no caching will cause the user browser to try again next
   * time and hopefully getting the newer version.
   * @param longVal long expiry for versioned resource
   * @param originalResourceTtl original resource ttl, to be served fro unversion resource
   * @return the calculated expiry to the Uri according to status
   * @throws GadgetException
   */
  public Integer translateStatusRefresh(int longVal, int originalResourceTtl)
      throws GadgetException {
    Integer retRefresh = 0;
    switch (getStatus()) {
    case VALID_VERSIONED:
      retRefresh = longVal;
      break;
    case VALID_UNVERSIONED:
      retRefresh = originalResourceTtl;
      break;
    case INVALID_VERSION:
      retRefresh = 0;
      break;
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

  protected static boolean getBooleanValue(String str) {
    return str != null && "1".equals(str);
  }

  protected static Integer getIntegerValue(String str) {
    try {
      return NumberUtils.createInteger(str);
    } catch (NumberFormatException e) {
      // -1 is sentinel for invalid value.
      return -1;
    }
  }
}
