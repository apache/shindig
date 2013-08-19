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

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.Nullable;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.js.JsException;
import org.apache.shindig.gadgets.js.JsRequestBuilder;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsServingPipeline;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter.CajoledResult;
import org.apache.shindig.gadgets.servlet.GadgetsHandlerApi.AuthContext;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LinkSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.OAuth2Service;
import org.apache.shindig.gadgets.spec.OAuth2Spec;
import org.apache.shindig.gadgets.spec.OAuthService;
import org.apache.shindig.gadgets.spec.OAuthSpec;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.DefaultIframeUriManager;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.html.Nodes;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Sets;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

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
  static final Map<Class<?>, Class<?>> API_CLASSES =
      new ImmutableMap.Builder<Class<?>, Class<?>>()
          .put(View.class, GadgetsHandlerApi.View.class)
          .put(UserPref.class, GadgetsHandlerApi.UserPref.class)
          .put(EnumValuePair.class, GadgetsHandlerApi.EnumValuePair.class)
          .put(ModulePrefs.class, GadgetsHandlerApi.ModulePrefs.class)
          .put(Feature.class, GadgetsHandlerApi.Feature.class)
          .put(LinkSpec.class, GadgetsHandlerApi.LinkSpec.class)
          .put(OAuthSpec.class, GadgetsHandlerApi.OAuthSpec.class)
          .put(OAuthService.class, GadgetsHandlerApi.OAuthService.class)
          .put(OAuthService.EndPoint.class, GadgetsHandlerApi.EndPoint.class)
          .put(OAuth2Spec.class, GadgetsHandlerApi.OAuth2Spec.class)
          .put(OAuth2Service.class, GadgetsHandlerApi.OAuth2Service.class)
          // Enums
          .put(View.ContentType.class, GadgetsHandlerApi.ViewContentType.class)
          .put(UserPref.DataType.class, GadgetsHandlerApi.UserPrefDataType.class)
          .put(GadgetsHandlerApi.RenderingContext.class, RenderingContext.class)
          .put(OAuthService.Method.class, GadgetsHandlerApi.Method.class)
          .put(OAuthService.Location.class, GadgetsHandlerApi.Location.class)
          .build();

  // Provide mapping for internal enums to api enums
  @VisibleForTesting
  static final Map<Enum<?>, Enum<?>> ENUM_CONVERSION_MAP =
      new ImmutableMap.Builder<Enum<?>, Enum<?>>()
          // View.ContentType mapping
          .putAll(BeanDelegator.createDefaultEnumMap(View.ContentType.class,
              GadgetsHandlerApi.ViewContentType.class))
          // UserPref.DataType mapping
          .putAll(BeanDelegator.createDefaultEnumMap(UserPref.DataType.class,
              GadgetsHandlerApi.UserPrefDataType.class))
          .putAll(BeanDelegator.createDefaultEnumMap(OAuthService.Method.class,
              GadgetsHandlerApi.Method.class))
          .putAll(BeanDelegator.createDefaultEnumMap(OAuthService.Location.class,
              GadgetsHandlerApi.Location.class))
          .putAll(BeanDelegator.createDefaultEnumMap(GadgetsHandlerApi.RenderingContext.class,
              RenderingContext.class))
          .build();

  protected final TimeSource timeSource;
  protected final Processor processor;
  protected final IframeUriManager iframeUriManager;
  protected final SecurityTokenCodec securityTokenCodec;
  protected final ProxyUriManager proxyUriManager;
  protected final JsUriManager jsUriManager;
  protected final JsServingPipeline jsPipeline;
  protected final JsRequestBuilder jsRequestBuilder;
  protected final ProxyHandler proxyHandler;
  protected final BeanDelegator beanDelegator;
  protected final long specRefreshInterval;
  protected final BeanFilter beanFilter;
  protected final CajaContentRewriter cajaContentRewriter;
  protected final GadgetAdminStore gadgetAdminStore;
  protected final FeatureRegistryProvider featureRegistryProvider;
  protected final ModuleIdManager moduleIdManager;
  private ContainerConfig config;

  @Inject
  public GadgetsHandlerService(TimeSource timeSource, Processor processor,
      IframeUriManager iframeUriManager, SecurityTokenCodec securityTokenCodec,
      ProxyUriManager proxyUriManager, JsUriManager jsUriManager, ProxyHandler proxyHandler,
      JsServingPipeline jsPipeline, JsRequestBuilder jsRequestBuilder,
      @Named("shindig.cache.xml.refreshInterval") long specRefreshInterval,
      BeanFilter beanFilter, CajaContentRewriter cajaContentRewriter,
      GadgetAdminStore gadgetAdminStore,
      FeatureRegistryProvider featureRegistryProvider,
      ModuleIdManager moduleIdManager,
      ContainerConfig config) {
    this.timeSource = timeSource;
    this.processor = processor;
    this.iframeUriManager = iframeUriManager;
    this.securityTokenCodec = securityTokenCodec;
    this.proxyUriManager = proxyUriManager;
    this.jsUriManager = jsUriManager;
    this.proxyHandler = proxyHandler;
    this.jsPipeline = jsPipeline;
    this.jsRequestBuilder = jsRequestBuilder;
    this.specRefreshInterval = specRefreshInterval;
    this.beanFilter = beanFilter;
    this.cajaContentRewriter = cajaContentRewriter;
    this.gadgetAdminStore = gadgetAdminStore;
    this.featureRegistryProvider = featureRegistryProvider;
    this.moduleIdManager = moduleIdManager;

    this.beanDelegator = new BeanDelegator(API_CLASSES, ENUM_CONVERSION_MAP);
    this.config = config;
  }

  /**
   * Get gadget metadata information and iframe url. Support filtering of fields
   * @param request request parameters
   * @return gadget metadata and iframe urls
   * @throws ProcessingException
   */
  public GadgetsHandlerApi.MetadataResponse getMetadata(GadgetsHandlerApi.MetadataRequest request)
      throws ProcessingException {
    verifyBaseParams(request, true);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    GadgetContext context = new MetadataGadgetContext(request);
    Gadget gadget = processor.process(context);

    boolean needIfrUrls = isFieldIncluded(fields, "iframeurls");
    if (needIfrUrls) {
      if(!gadgetAdminStore.checkFeatureAdminInfo(gadget)) {
        throw new ProcessingException("Gadget is not trusted to render in this container.",
              HttpResponse.SC_BAD_REQUEST);
      }
    }
    Map<String, String> uris = needIfrUrls ?
            translateUris(iframeUriManager.makeAllRenderingUris(gadget)) : null;
    Boolean needsTokenRefresh =
        isFieldIncluded(fields, "needstokenrefresh") ?
            gadget.getAllFeatures().contains("auth-refresh") : null;
    boolean alwaysAppendSecurityToken = config.getBool(gadget.getContext().getContainer(),
            DefaultIframeUriManager.SECURITY_TOKEN_ALWAYS_KEY);
    if (alwaysAppendSecurityToken) {
      needsTokenRefresh = Boolean.TRUE;
    }
    Set<String> rpcServiceIds = getRpcServiceIds(gadget);

    Integer tokenTTL = isFieldIncluded(fields, "tokenTTL") ?
        securityTokenCodec.getTokenTimeToLive(context.getContainer()) : null;

    return createMetadataResponse(context.getUrl(), gadget.getSpec(), uris,
        needsTokenRefresh, fields, timeSource.currentTimeMillis() + specRefreshInterval, tokenTTL,
        rpcServiceIds);
  }

  private Map<String, String> translateUris(Map<String, Uri> iframeUris) {
    Function<Uri, String> toString = new Function<Uri, String>() {
      @Override
      public String apply(Uri input) {
        return input.toString();
      }
    };
    return Maps.transformValues(iframeUris, toString);
  }

  /**
   * Gets the set of allowed RPC service ids.
   *
   * @param gadget
   *          the gadget to get the service ids for.
   * @return the set of allowed RPC service ids.
   */
  private Set<String> getRpcServiceIds(Gadget gadget) {
    GadgetContext context = gadget.getContext();
    Set<String> rpcEndpoints = Sets.newHashSet(gadgetAdminStore.getAdditionalRpcServiceIds(gadget));
    List<Feature> modulePrefFeatures = gadget.getSpec().getModulePrefs().getAllFeatures();
    List<String> featureNames = Lists.newArrayList();
    for(Feature feature : modulePrefFeatures) {
      if(gadgetAdminStore.isAllowedFeature(feature, gadget)) {
        featureNames.add(feature.getName());
      }
    }
    try {
      FeatureRegistry featureRegistry = featureRegistryProvider.get(context.getRepository());
      LookupResult result = featureRegistry.getFeatureResources(context,
          featureRegistry.getFeatures(featureNames), null);
      List<FeatureBundle> bundles = result.getBundles();
      for (FeatureBundle bundle : bundles) {
        rpcEndpoints.addAll(bundle.getApis(ApiDirective.Type.RPC, false));
      }
    } catch (GadgetException e) {
      LOG.log(Level.WARNING, "Error getting features from feature registry", e);
    }
    return rpcEndpoints;
  }

  private boolean isFieldIncluded(Set<String> fields, String name) {
    return fields.contains(BeanFilter.ALL_FIELDS) || fields.contains(name.toLowerCase());
  }
  /**
   * Create security token
   * @param request token parameters (gadget, owner and viewer)
   * @return Security token
   * @throws SecurityTokenException
   */
  public GadgetsHandlerApi.TokenResponse getToken(GadgetsHandlerApi.TokenRequest request)
      throws SecurityTokenException, ProcessingException {
    verifyBaseParams(request, true);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());
    AuthContext authContext = request.getAuthContext();

    SecurityToken tokenData = null;
    String token = null;

    Long moduleId = request.getModuleId();
    if (moduleId == null) {
      // Zero means there's no persisted module instance and the container doesn't care to persist it.
      moduleId = 0L;
    } else if (moduleId < 0) {
      // Please generate a module Id for me
      moduleId = moduleIdManager.generate(request.getUrl(), authContext);
    }
    if (moduleId > 0) {
      moduleId = moduleIdManager.validate(request.getUrl(), authContext, moduleId);
    }

    if (moduleId != null) {
      tokenData = convertAuthContext(authContext, request.getContainer(),
          request.getUrl().toString(), moduleId, request.getUrl().toString());
      token = securityTokenCodec.encodeToken(tokenData);
    }

    Long expiryTimeMs = null;
    Integer tokenTTL = null;
    if (tokenData != null) {
      expiryTimeMs = tokenData.getExpiresAt();
      tokenTTL = isFieldIncluded(fields, "tokenTTL") ?
              securityTokenCodec.getTokenTimeToLive(tokenData.getContainer())
              : null;
    }

    moduleId = isFieldIncluded(fields, "moduleId") ? moduleId : null;

    return createTokenResponse(request.getUrl(), token, fields, expiryTimeMs, tokenTTL, moduleId);
  }

  public GadgetsHandlerApi.JsResponse getJs(GadgetsHandlerApi.JsRequest request)
      throws ProcessingException {
    verifyBaseParams(request, false);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    JsUri jsUri = createJsUri(request);
    Uri servedUri = jsUriManager.makeExternJsUri(jsUri);

    String content = null;
    Long expireMs = null;
    if (isFieldIncluded(fields, "jsContent")) {
      JsResponse response;
      try {
        response = jsPipeline.execute(jsRequestBuilder.build(jsUri, servedUri.getAuthority()));
      } catch (JsException e) {
        throw new ProcessingException(e.getMessage(), e.getStatusCode());
      }
      content = response.toJsString();
      if (response.isProxyCacheable()) {
        expireMs = getDefaultExpiration();
      }
    } else {
      expireMs = getDefaultExpiration();
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
   * Convert message level to Shindig's serializable message type
   */
  public static GadgetsHandlerApi.MessageLevel convertMessageLevel(String name) {
    try {
      return GadgetsHandlerApi.MessageLevel.valueOf(name);
    }
    catch (Exception ex) {
      return GadgetsHandlerApi.MessageLevel.UNKNOWN;
    }
  }

  /**
   * Convert messages from Caja's internal message type to Shindig's serializable message type
   */
  private List<GadgetsHandlerApi.Message> convertMessages(
      List<com.google.caja.reporting.Message> msgs, final MessageContext mc) {
    List<GadgetsHandlerApi.Message> result = Lists.newArrayListWithExpectedSize(msgs.size());
    for (final com.google.caja.reporting.Message m : msgs) {
      MessageImpl msg = new MessageImpl(m.getMessageType().name(),
          m.format(mc), convertMessageLevel(m.getMessageLevel().name()));
      result.add(msg);
    }
    return result;
  }

  public GadgetsHandlerApi.CajaResponse getCaja(GadgetsHandlerApi.CajaRequest request)
      throws ProcessingException {
    verifyBaseParams(request, true);
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    MessageContext mc = new MessageContext();
    CajoledResult result =
      cajaContentRewriter.rewrite(request.getUrl(), request.getContainer(),
          request.getMimeType(), true /* only support es53 */, request.getDebug());
    String html = null;
    String js = null;
    if (!result.hasErrors && null != result.html) {
      html = Nodes.render(result.html);
    }

    if (!result.hasErrors && null != result.js) {
      StringBuilder builder = new StringBuilder();
      TokenConsumer tc = request.getDebug() ?
          new JsPrettyPrinter(new Concatenator(builder))
          : new JsMinimalPrinter(new Concatenator(builder));
      RenderContext rc = new RenderContext(tc)
          .withAsciiOnly(true)
          .withEmbeddable(true);
      result.js.render(rc);
      rc.getOut().noMoreTokens();
      js = builder.toString();
    }

    // TODO(jasvir): Improve Caja responses expiration handling
    return createCajaResponse(request.getUrl(),
        html, js, convertMessages(result.messages, mc), fields,
        timeSource.currentTimeMillis() + specRefreshInterval);
  }

  /**
   * Verify request parameter are defined.
   */
  protected void verifyBaseParams(GadgetsHandlerApi.BaseRequest request, boolean checkUrl)
      throws ProcessingException {
    if (checkUrl && request.getUrl() == null) {
      throw new ProcessingException("Missing url parameter", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getContainer() == null) {
      throw new ProcessingException("Missing container parameter", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getFields() == null) {
      throw new ProcessingException("Missing fields parameter", HttpResponse.SC_BAD_REQUEST);
    }
  }

  protected Long getProxyExpireMs(ProxyUri proxyUri, @Nullable HttpResponse httpResponse) {
    if (httpResponse != null) {
      return httpResponse.getCacheExpiration();
    } else if (proxyUri.getRefresh() != null) {
      return timeSource.currentTimeMillis() + proxyUri.getRefresh() * 1000;
    }

    return getDefaultExpiration();
  }

  protected long getDefaultExpiration() {
    return timeSource.currentTimeMillis() + (HttpUtil.getDefaultTtl() * 1000);
  }

  /**
   * GadgetContext for metadata request. Used by the gadget processor
   */
  protected class MetadataGadgetContext extends GadgetContext {

    private final GadgetsHandlerApi.MetadataRequest request;
    private final SecurityToken authContext;

    public MetadataGadgetContext(GadgetsHandlerApi.MetadataRequest request) {
      this.request = request;
      this.authContext = convertAuthContext(
          request.getAuthContext(), request.getContainer(), request.getUrl().toString());
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
    public long getModuleId() {
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
      return authContext;
    }

    @Override
    public boolean getSanitize() {
      return (request.getRenderingType() == GadgetsHandlerApi.RenderingType.SANITIZED);
    }

    @Override
    public boolean getCajoled() {
      return (request.getRenderingType() == GadgetsHandlerApi.RenderingType.IFRAME_CAJOLED);
    }
  }

  private SecurityToken convertAuthContext(GadgetsHandlerApi.AuthContext authContext,
      String container, String url) {
    return convertAuthContext(authContext, container, url, 0, url);
  }

  private SecurityToken convertAuthContext(GadgetsHandlerApi.AuthContext authContext,
      String container, String url, long moduleId, String activeUrl) {
    if (authContext == null) {
      return null;
    }
    return beanDelegator.createDelegator(authContext, SecurityToken.class,
        ImmutableMap.<String, Object>of("container", container,
            "appid", url, "appurl", url, "moduleId", moduleId, "activeurl", activeUrl));
  }

  public GadgetsHandlerApi.BaseResponse createErrorResponse(Uri uri, Exception e,
      String defaultMsg) {
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
  GadgetsHandlerApi.MetadataResponse createMetadataResponse(Uri url, GadgetSpec spec, Map<String,
        String> iframeUris, Boolean needsTokenRefresh, Set<String> fields, Long expireTime,
        Integer tokenTTL, Set<String> rpcServiceIds) {
    return (GadgetsHandlerApi.MetadataResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator(spec, GadgetsHandlerApi.MetadataResponse.class,
            ImmutableMap.<String, Object>builder()
                .put("url", url)
                .put("error", BeanDelegator.NULL)
                .put("iframeurls", BeanDelegator.nullable(iframeUris))
                .put("needstokenrefresh", BeanDelegator.nullable(needsTokenRefresh))
                .put("responsetimems", timeSource.currentTimeMillis())
                .put("expiretimems", BeanDelegator.nullable(expireTime))
                .put("rpcserviceids", BeanDelegator.nullable(rpcServiceIds))
                .put("tokenttl", BeanDelegator.nullable(tokenTTL)).build()),
        fields);
  }

  @VisibleForTesting
  GadgetsHandlerApi.TokenResponse createTokenResponse(Uri url, String token, Set<String> fields,
      Long tokenExpire, Integer tokenTTL, Long moduleId) {
    return (GadgetsHandlerApi.TokenResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator(null, GadgetsHandlerApi.TokenResponse.class,
            ImmutableMap.<String, Object>builder()
                .put("url", url)
                .put("error", BeanDelegator.NULL)
                .put("token", BeanDelegator.nullable(token))
                .put("responsetimems", timeSource.currentTimeMillis())
                .put("expiretimems", BeanDelegator.nullable(tokenExpire))
                .put("tokenttl", BeanDelegator.nullable(tokenTTL))
                .put("moduleid", BeanDelegator.nullable(moduleId))
            .build()
        ),
        fields
    );
  }

  protected JsUri createJsUri(GadgetsHandlerApi.JsRequest request) {
    RenderingContext context = (RenderingContext)
    (request.getContext() != null ?
        // TODO: Figure out why maven complain about casting and clean the dummy cast
        (Object) beanDelegator.convertEnum(request.getContext())
        : RenderingContext.GADGET);

    return new JsUri(request.getRefresh(), request.getDebug(), request.getIgnoreCache(),
        request.getContainer(), request.getGadget(), request.getFeatures(),
        request.getLoadedFeatures(), request.getOnload(), false, false, context, request.getUrl(),
        request.getRepository());
  }

  @VisibleForTesting
  GadgetsHandlerApi.JsResponse createJsResponse(Uri url, Uri jsUri, String content,
      Set<String> fields, Long expireMs) {
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

  protected ProxyUri createProxyUri(GadgetsHandlerApi.ProxyRequest request) {
    ProxyUriManager.ProxyUri proxyUri = new ProxyUriManager.ProxyUri(request.getRefresh(),
        request.getDebug(), request.getIgnoreCache(), request.getContainer(),
        request.getGadget(), request.getUrl());

    proxyUri.setFallbackUrl(request.getFallbackUrl())
        .setRewriteMimeType(request.getRewriteMimeType())
        .setSanitizeContent(request.getSanitize());

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
      String content = "";
      if (httpResponse.getContentLength() > 0) {
        // Stream out the base64-encoded data.
        // Ctor args indicate to encode w/o line breaks.
        Base64InputStream b64input =
            new Base64InputStream(httpResponse.getResponse(), true, 0, null);
        content = IOUtils.toString(b64input);
      }

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

  @VisibleForTesting
  GadgetsHandlerApi.CajaResponse createCajaResponse(Uri uri, String html, String js,
      List<GadgetsHandlerApi.Message> messages, Set<String> fields, Long expireMs) {
    ImmutableList.Builder<GadgetsHandlerApi.Message> msgBuilder =
      ImmutableList.builder();
    for (final GadgetsHandlerApi.Message m : messages) {
      msgBuilder.add(
        beanDelegator.createDelegator(null, GadgetsHandlerApi.Message.class,
            ImmutableMap.<String, Object>of("name", m.getName(),
                "level", m.getLevel(), "message", m.getMessage())));
    }

    return (GadgetsHandlerApi.CajaResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator(null, GadgetsHandlerApi.CajaResponse.class,
            ImmutableMap.<String, Object>builder()
            .put("url", uri)
            .put("html", BeanDelegator.nullable(html))
            .put("js", BeanDelegator.nullable(js))
            .put("messages", msgBuilder.build())
            .put("error", BeanDelegator.NULL)
            .put("responsetimems", timeSource.currentTimeMillis())
            .put("expiretimems", BeanDelegator.nullable(expireMs))
            .build()),
            fields);
  }

  private static class MessageImpl implements GadgetsHandlerApi.Message {
    private final GadgetsHandlerApi.MessageLevel level;
    private final String message;
    private final String name;

    public MessageImpl(String name, String message, GadgetsHandlerApi.MessageLevel level) {
      this.name = name;
      this.message = message;
      this.level = level;
    }

    public GadgetsHandlerApi.MessageLevel getLevel() {
      return level;
    }

    public String getMessage() {
      return message;
    }

    public String getName() {
      return name;
    }

  }
}
