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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LinkSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

/**
 * Service that interfaces with the system to provide information about gadgets.
 *
 * @since 2.0.0
 */
public class GadgetsHandlerService {

  private static final Locale DEFAULT_LOCALE = new Locale("all", "all");

  private static final Logger LOG = Logger.getLogger(GadgetsHandler.class.getName());

  // Map shindig data class to API interfaces
  @VisibleForTesting
  static final Map<Class<?>, Class<?>> apiClasses =
      new ImmutableMap.Builder<Class<?>, Class<?>>()
          .put(View.class, GadgetsHandlerApi.View.class)
          .put(UserPref.class, GadgetsHandlerApi.UserPref.class)
          .put(EnumValuePair.class, GadgetsHandlerApi.EnumValuePair.class)
          .put(ModulePrefs.class, GadgetsHandlerApi.ModulePrefs.class)
          .put(Feature.class, GadgetsHandlerApi.Feature.class)
          .put(LinkSpec.class, GadgetsHandlerApi.LinkSpec.class)
          // Enums
          .put(View.ContentType.class, GadgetsHandlerApi.ViewContentType.class)
          .put(UserPref.DataType.class, GadgetsHandlerApi.UserPrefDataType.class)
          .put(GadgetsHandlerApi.RenderingContext.class, RenderingContext.class)
          .build();

  // Provide mapping for internal enums to api enums
  @VisibleForTesting
  static final Map<Enum<?>, Enum<?>> enumConversionMap =
      new ImmutableMap.Builder<Enum<?>, Enum<?>>()
          // View.ContentType mapping
          .putAll(BeanDelegator.createDefaultEnumMap(View.ContentType.class,
              GadgetsHandlerApi.ViewContentType.class))
          // UserPref.DataType mapping
          .putAll(BeanDelegator.createDefaultEnumMap(UserPref.DataType.class,
              GadgetsHandlerApi.UserPrefDataType.class))
          .putAll(BeanDelegator.createDefaultEnumMap(GadgetsHandlerApi.RenderingContext.class,
              RenderingContext.class))
          .build();

  protected final TimeSource timeSource;
  protected final Processor processor;
  protected final IframeUriManager iframeUriManager;
  protected final SecurityTokenCodec securityTokenCodec;
  protected final ProxyUriManager proxyUriManager;
  protected final JsUriManager jsUriManager;
  protected final JsHandler jsHandler;
  protected final ProxyHandler proxyHandler;
  protected final BeanDelegator beanDelegator;
  protected final long specRefreshInterval;
  protected final BeanFilter beanFilter;

  @Inject
  public GadgetsHandlerService(TimeSource timeSource, Processor processor,
      IframeUriManager iframeUriManager, SecurityTokenCodec securityTokenCodec,
      ProxyUriManager proxyUriManager, JsUriManager jsUriManager, ProxyHandler proxyHandler,
      JsHandler jsHandler,
      @Named("shindig.cache.xml.refreshInterval") long specRefreshInterval,
      BeanFilter beanFilter) {
    this.timeSource = timeSource;
    this.processor = processor;
    this.iframeUriManager = iframeUriManager;
    this.securityTokenCodec = securityTokenCodec;
    this.proxyUriManager = proxyUriManager;
    this.jsUriManager = jsUriManager;
    this.proxyHandler = proxyHandler;
    this.jsHandler = jsHandler;
    this.specRefreshInterval = specRefreshInterval;
    this.beanFilter = beanFilter;

    this.beanDelegator = new BeanDelegator(apiClasses, enumConversionMap);
  }

  /**
   * Get gadget metadata information and iframe url. Support filtering of fields
   * @param request request parameters
   * @return gadget metadata nd iframe url
   * @throws ProcessingException
   */
  public GadgetsHandlerApi.MetadataResponse getMetadata(GadgetsHandlerApi.MetadataRequest request)
      throws ProcessingException {
    verifyBaseParams(request, true);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    GadgetContext context = new MetadataGadgetContext(request);
    Gadget gadget = processor.process(context);
    String iframeUrl =
        isFieldIncluded(fields, "iframeurl")  ?
            iframeUriManager.makeRenderingUri(gadget).toString() : null;
    Boolean needsTokenRefresh =
        isFieldIncluded(fields, "needstokenrefresh") ?
            gadget.getAllFeatures().contains("auth-refresh") : null;
    return createMetadataResponse(context.getUrl(), gadget.getSpec(), iframeUrl,
        needsTokenRefresh, fields, timeSource.currentTimeMillis() + specRefreshInterval);
  }

  private boolean isFieldIncluded(Set<String> fields, String name) {
    return fields.contains(BeanFilter.ALL_FIELDS) || fields.contains(name.toLowerCase());
  }
  /**
   * Create security token
   * @param request token paramaters (gadget, owner and viewer)
   * @return Security token
   * @throws SecurityTokenException
   */
  public GadgetsHandlerApi.TokenResponse getToken(GadgetsHandlerApi.TokenRequest request)
      throws SecurityTokenException, ProcessingException {
    verifyBaseParams(request, true);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    SecurityToken tokenData = convertToken(request.getToken(), request.getContainer(),
        request.getUrl().toString());
    String token = securityTokenCodec.encodeToken(tokenData);
    Long expiryTimeMs = securityTokenCodec.getTokenExpiration(tokenData);
    return createTokenResponse(request.getUrl(), token, fields, expiryTimeMs);
  }

  public GadgetsHandlerApi.JsResponse getJs(GadgetsHandlerApi.JsRequest request)
      throws ProcessingException {
    verifyBaseParams(request, false);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    RenderingContext context = (RenderingContext)
        (request.getContext() != null ?
            // TODO: Figure out why maven complain about casting and clean the dummy cast
            (Object) beanDelegator.convertEnum(request.getContext())
            : RenderingContext.GADGET);
    JsUri jsUri = new JsUri(request.getRefresh(), request.getDebug(), request.getIgnoreCache(),
        request.getContainer(), request.getGadget(), request.getFeatures(), request.getOnload(),
        false, context);

    Uri servedUri = jsUriManager.makeExternJsUri(jsUri);

    String content = null;
    Long expireMs = null;
    if (isFieldIncluded(fields, "content")) {
      JsHandler.Response response = jsHandler.getJsContent(jsUri, servedUri.getAuthority());
      content = response.getJsData().toString();
      if (content.length() == 0) {
        // No content, meaning no such feature
        throw new ProcessingException("Feature(s) " + Joiner.on(",").join(jsUri.getLibs())
            + " not found", HttpResponse.SC_NOT_FOUND);
      }
      if (response.isProxyCacheable()) {
        expireMs = timeSource.currentTimeMillis() + (HttpUtil.getDefaultTtl() * 1000);
      }
    }
    return createJsResponse(request.getUrl(), servedUri, content, fields, expireMs);
  }

  public GadgetsHandlerApi.ProxyResponse getProxy(GadgetsHandlerApi.ProxyRequest request)
      throws ProcessingException {
    verifyBaseParams(request, true);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    ProxyUri proxyUri = createProxyUri(request);
    List<Uri> uris = proxyUriManager.make(ImmutableList.of(proxyUri), null);

    HttpResponse httpResponse = null;
    try {
      if (isFieldIncluded(fields, "proxyContent")) {
        httpResponse = proxyHandler.fetch(proxyUri);
      }
    } catch (IOException e) {
      LOG.log(Level.INFO, "Failed to fetch resource " + proxyUri.getResource().toString(), e);
      throw new ProcessingException("Error getting response content", HttpResponse.SC_BAD_GATEWAY);
    } catch (GadgetException e) {
      // TODO: Clean this log if it is too spammy
      LOG.log(Level.INFO, "Failed to fetch resource " + proxyUri.getResource().toString(), e);
      throw new ProcessingException("Error getting response content", HttpResponse.SC_BAD_GATEWAY);
    }

    try {
      return createProxyResponse(uris.get(0), httpResponse, fields,
          getProxyExpireMs(proxyUri, httpResponse));
    } catch (IOException e) {
      // Should never happen!
      LOG.log(Level.WARNING, "Error creating proxy response", e);
      throw new ProcessingException("Error getting response content",
          HttpResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Verify request parameter are defined.
   */
  protected void verifyBaseParams(GadgetsHandlerApi.BaseRequest request, boolean checkUrl)
      throws ProcessingException {
    if (checkUrl && request.getUrl() == null) {
      throw new ProcessingException("Missing url paramater", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getContainer() == null) {
      throw new ProcessingException("Missing container paramater", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getFields() == null) {
      throw new ProcessingException("Missing fields paramater", HttpResponse.SC_BAD_REQUEST);
    }
  }

  protected Long getProxyExpireMs(ProxyUri proxyUri, HttpResponse httpResponse) {
    Long expireMs = null;
    if (httpResponse != null) {
      expireMs = httpResponse.getCacheExpiration();
    } else if (proxyUri.getRefresh() != null) {
      expireMs = timeSource.currentTimeMillis() + proxyUri.getRefresh() * 1000;
    }
    return expireMs;
  }

  /**
   * GadgetContext for metadata request. Used by the gadget processor
   */
  protected class MetadataGadgetContext extends GadgetContext {

    private final GadgetsHandlerApi.MetadataRequest request;
    private final SecurityToken token;

    public MetadataGadgetContext(GadgetsHandlerApi.MetadataRequest request) {
      this.request = request;
      this.token = convertToken(
          request.getToken(), request.getContainer(), request.getUrl().toString());
    }

    @Override
    public Uri getUrl() {
      return request.getUrl();
    }

    @Override
    public String getContainer() {
      return request.getContainer();
    }

    @Override
    public RenderingContext getRenderingContext() {
      return RenderingContext.METADATA;
    }

    @Override
    public int getModuleId() {
      return 1;
    }

    @Override
    public Locale getLocale() {
      return (request.getLocale() == null ? DEFAULT_LOCALE : request.getLocale());
    }

    @Override
    public boolean getIgnoreCache() {
      return request.getIgnoreCache();
    }

    @Override
    public boolean getDebug() {
      return request.getDebug();
    }

    @Override
    public String getView() {
      return request.getView();
    }

    @Override
    public SecurityToken getToken() {
      return token;
    }
  }

  private SecurityToken convertToken(GadgetsHandlerApi.TokenData token,
      String container, String url) {
    if (token == null) {
      return null;
    }
    return beanDelegator.createDelegator(token, SecurityToken.class,
        ImmutableMap.<String, Object>of("container", container,
            "appid", url, "appurl", url));
  }

  public GadgetsHandlerApi.BaseResponse createErrorResponse(
    Uri uri, Exception e, String defaultMsg) {
    if (e instanceof ProcessingException) {
      ProcessingException processingExc = (ProcessingException) e;
      return createErrorResponse(uri, processingExc.getHttpStatusCode(),
          processingExc.getMessage());
    }
    LOG.log(Level.WARNING, "Error handling request: " + (uri != null ? uri.toString() : ""), e);
    return createErrorResponse(uri, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, defaultMsg);
  }

  public GadgetsHandlerApi.BaseResponse createErrorResponse(Uri url, int code, String error) {
    GadgetsHandlerApi.Error errorBean = beanDelegator.createDelegator(
        null, GadgetsHandlerApi.Error.class, ImmutableMap.<String, Object>of(
          "message", BeanDelegator.nullable(error), "code", code));

    return beanDelegator.createDelegator(error, GadgetsHandlerApi.BaseResponse.class,
        ImmutableMap.<String, Object>of("url", BeanDelegator.nullable(url), "error", errorBean,
            "responsetimems", BeanDelegator.NULL, "expiretimems", BeanDelegator.NULL));
  }

  @VisibleForTesting
  GadgetsHandlerApi.MetadataResponse createMetadataResponse(
      Uri url, GadgetSpec spec, String iframeUrl, Boolean needsTokenRefresh,
      Set<String> fields, Long expireTime) {
    return (GadgetsHandlerApi.MetadataResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator(spec, GadgetsHandlerApi.MetadataResponse.class,
            ImmutableMap.<String, Object>builder()
                .put("url", url)
                .put("error", BeanDelegator.NULL)
                .put("iframeurl", BeanDelegator.nullable(iframeUrl))
                .put("needstokenrefresh", BeanDelegator.nullable(needsTokenRefresh))
                .put("responsetimems", timeSource.currentTimeMillis())
                .put("expiretimems", BeanDelegator.nullable(expireTime)).build()),
        fields);
  }

  @VisibleForTesting
  GadgetsHandlerApi.TokenResponse createTokenResponse(
      Uri url, String token, Set<String> fields, Long tokenExpire) {
    return (GadgetsHandlerApi.TokenResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator(null, GadgetsHandlerApi.TokenResponse.class,
            ImmutableMap.<String, Object>of(
                "url", url,
                "error", BeanDelegator.NULL,
                "token", BeanDelegator.nullable(token),
                "responsetimems", timeSource.currentTimeMillis(),
                "expiretimems", BeanDelegator.nullable(tokenExpire))),
        fields);
  }

  @VisibleForTesting
  GadgetsHandlerApi.JsResponse createJsResponse(
      Uri url, Uri jsUri, String content, Set<String> fields, Long expireMs) {
    return (GadgetsHandlerApi.JsResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator(null, GadgetsHandlerApi.JsResponse.class,
            ImmutableMap.<String, Object>builder()
                .put("url", BeanDelegator.nullable(url))
                .put("error", BeanDelegator.NULL)
                .put("jsurl", jsUri)
                .put("jscontent", BeanDelegator.nullable(content))
                .put("responsetimems", timeSource.currentTimeMillis())
                .put("expiretimems", BeanDelegator.nullable(expireMs)).build()),
        fields);
  }

  @VisibleForTesting
  ProxyUri createProxyUri(GadgetsHandlerApi.ProxyRequest request) {
    ProxyUriManager.ProxyUri proxyUri = new ProxyUriManager.ProxyUri(request.getRefresh(),
        request.getDebug(), request.getIgnoreCahce(), request.getContainer(),
        request.getGadget(), request.getUrl());

    proxyUri.setFallbackUrl(request.getFallbackUrl())
        .setRewriteMimeType(request.getRewriteMimeType())
        .setSanitizeContent(request.getSanitize())
        .setCajoleContent(request.getCajole());

    GadgetsHandlerApi.ImageParams image = request.getImageParams();
    if (image != null) {
      proxyUri.setResize( image.getWidth(), image.getHeight(),
          image.getQuality(), image.getDoNotExpand());
    }
    return proxyUri;
  }

  @VisibleForTesting
  GadgetsHandlerApi.ProxyResponse createProxyResponse(Uri uri, HttpResponse httpResponse,
      Set<String> fields, Long expireMs) throws IOException {

    GadgetsHandlerApi.HttpResponse beanHttp = null;
    if (httpResponse != null) {
      // Stream out the base64-encoded data.
      // Ctor args indicate to encode w/o line breaks.
      Base64InputStream b64input = new Base64InputStream(httpResponse.getResponse(), true, 0, null);
      String content = IOUtils.toString(b64input);

      ImmutableList.Builder<GadgetsHandlerApi.NameValuePair> headersBuilder =
          ImmutableList.builder();
      for (final Map.Entry<String, String> entry : httpResponse.getHeaders().entries()) {
        headersBuilder.add(
            beanDelegator.createDelegator(null, GadgetsHandlerApi.NameValuePair.class,
                ImmutableMap.<String, Object>of("name", entry.getKey(), "value", entry.getValue()))
        );
      }

      beanHttp = beanDelegator.createDelegator(null, GadgetsHandlerApi.HttpResponse.class,
          ImmutableMap.<String, Object>of(
              "code", httpResponse.getHttpStatusCode(),
              "encoding", httpResponse.getEncoding(),
              "contentbase64", content,
              "headers", headersBuilder.build()));
    }

    return (GadgetsHandlerApi.ProxyResponse) beanFilter.createFilteredBean(
      beanDelegator.createDelegator(null, GadgetsHandlerApi.ProxyResponse.class,
          ImmutableMap.<String, Object>builder()
              .put("proxyurl", uri)
              .put("proxycontent", BeanDelegator.nullable(beanHttp))
              .put("url", BeanDelegator.NULL)
              .put("error", BeanDelegator.NULL)
              .put("responsetimems", timeSource.currentTimeMillis())
              .put("expiretimems", BeanDelegator.nullable(expireMs))
              .build()),
      fields);
  }
}
