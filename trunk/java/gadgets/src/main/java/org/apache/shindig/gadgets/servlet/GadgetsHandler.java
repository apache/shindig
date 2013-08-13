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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.Uri.UriException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.servlet.GadgetsHandlerApi.RenderingContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.uri.UriCommon;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * Provides endpoints for gadget metadata lookup and more.
 *
 * @since 2.0.0
 */
@Service(name = "gadgets")
public class GadgetsHandler {
  @VisibleForTesting
  static final String FAILURE_METADATA = "Failed to get gadget metadata.";
  @VisibleForTesting
  static final String FAILURE_TOKEN = "Failed to get gadget token.";
  @VisibleForTesting
  static final String FAILURE_PROXY = "Failed to get proxy data.";
  @VisibleForTesting
  static final String FAILURE_CAJA = "Failed to cajole data.";
  @VisibleForTesting
  static final String FAILURE_JS = "Failed to get js data.";

  private static final List<String> DEFAULT_METADATA_FIELDS =
      ImmutableList.of("iframeUrls", "userPrefs.*", "modulePrefs.*", "views.*");

  private static final List<String> DEFAULT_TOKEN_FIELDS = ImmutableList.of("token");

  private static final List<String> DEFAULT_PROXY_FIELDS = ImmutableList.of("proxyUrl");

  private static final List<String> DEFAULT_CAJA_FIELDS = ImmutableList.of("*");
  private static final List<String> DEFAULT_JS_FIELDS = ImmutableList.of("jsUrl");

  private static final Logger LOG = Logger.getLogger(GadgetsHandler.class.getName());

  /**
   *  Enum to list the used JSON/JSONP request parameters
   *  It mostly reference the UriCommon fields for consistency,
   *  This enum defined the API names, Do not change the names!
   */
  enum Param {
    IDS("ids"),
    CONTAINER(UriCommon.Param.CONTAINER.getKey()),
    FIELDS("fields"),
    DEBUG(UriCommon.Param.DEBUG),
    NO_CACHE(UriCommon.Param.NO_CACHE),
    REFRESH(UriCommon.Param.REFRESH),
    LANG(UriCommon.Param.LANG),
    COUNTRY(UriCommon.Param.COUNTRY),
    VIEW(UriCommon.Param.VIEW),
    RENDER_TYPE("render"),
    SANITIZE(UriCommon.Param.SANITIZE),
    GADGET(UriCommon.Param.GADGET),
    FALLBACK_URL(UriCommon.Param.FALLBACK_URL_PARAM),
    REWRITE_MIME(UriCommon.Param.REWRITE_MIME_TYPE),
    NO_EXPAND(UriCommon.Param.NO_EXPAND),
    RESIZE_HEIGHT(UriCommon.Param.RESIZE_HEIGHT),
    RESIZE_WIDTH(UriCommon.Param.RESIZE_WIDTH),
    RESIZE_QUALITY(UriCommon.Param.RESIZE_QUALITY),
    FEATURES("features"),
    LOADED_FEATURES("loadedFeatures"),
    CONTAINER_MODE(UriCommon.Param.CONTAINER_MODE),
    ONLOAD(UriCommon.Param.ONLOAD),
    REPOSITORY(UriCommon.Param.REPOSITORY_ID),
    MIME_TYPE("mime_type");

    private final String name;
    Param(String name) { this.name = name; }
    Param(UriCommon.Param param) { this.name = param.getKey(); }
    String getName() { return name; }
  }

  protected final ExecutorService executor;
  protected final GadgetsHandlerService handlerService;

  protected final BeanFilter beanFilter;
  protected final BeanDelegator beanDelegator;

  @Inject
  public GadgetsHandler(ExecutorService executor,
                        GadgetsHandlerService handlerService,
                        BeanFilter beanFilter) {
    this.executor = executor;
    this.handlerService = handlerService;
    this.beanFilter = beanFilter;

    this.beanDelegator = new BeanDelegator();
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "metadata")
  public Map<String, GadgetsHandlerApi.BaseResponse> metadata(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor() {
      @Override
      protected Callable<CallableData> createJob(String url, BaseRequestItem request)
          throws ProcessingException {
        return createMetadataJob(url, request);
      }
    }.execute(request);
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "token")
  public Map<String, GadgetsHandlerApi.BaseResponse> token(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor() {
      @Override
      protected Callable<CallableData> createJob(String url, BaseRequestItem request)
          throws ProcessingException {
        return createTokenJob(url, request);
      }
    }.execute(request);
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "js")
  public GadgetsHandlerApi.BaseResponse js(BaseRequestItem request)
      throws ProtocolException {
    // No need for threading since it is one request
    GadgetsHandlerApi.BaseResponse response;
    try {
      JsRequestData jsRequest = new JsRequestData(request);
      response = handlerService.getJs(jsRequest);
    } catch (ProcessingException e) {
      response = handlerService.createErrorResponse(null, e.getHttpStatusCode(), e.getMessage());
    } catch (Exception e) {
      LOG.log(Level.INFO, "Error fetching JS", e);
      response = handlerService.createErrorResponse(null, HttpResponse.SC_INTERNAL_SERVER_ERROR,
          FAILURE_JS);
    }
    return response;
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "proxy")
  public Map<String, GadgetsHandlerApi.BaseResponse> proxy(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor() {
      @Override
      protected Callable<CallableData> createJob(String url, BaseRequestItem request)
          throws ProcessingException {
        return createProxyJob(url, request);
      }
    }.execute(request);
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "cajole")
  public Map<String, GadgetsHandlerApi.BaseResponse> cajole(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor() {
      @Override
      protected Callable<CallableData> createJob(String url, BaseRequestItem request)
          throws ProcessingException {
        return createCajaJob(url, request);
      }
    }.execute(request);
  }

  @Operation(httpMethods = "GET", path = "/@metadata.supportedFields")
  public Set<String> supportedFields(RequestItem request) {
    return ImmutableSet.copyOf(beanFilter
        .getBeanFields(GadgetsHandlerApi.MetadataResponse.class, 5));
  }

  @Operation(httpMethods = "GET", path = "/@token.supportedFields")
  public Set<String> tokenSupportedFields(RequestItem request) {
    return ImmutableSet.copyOf(
        beanFilter.getBeanFields(GadgetsHandlerApi.TokenResponse.class, 5));
  }

  @Operation(httpMethods = "GET", path = "/@js.supportedFields")
  public Set<String> jsSupportedFields(RequestItem request) {
    return ImmutableSet.copyOf(
        beanFilter.getBeanFields(GadgetsHandlerApi.JsResponse.class, 5));
  }

  @Operation(httpMethods = "GET", path = "/@proxy.supportedFields")
  public Set<String> proxySupportedFields(RequestItem request) {
    return ImmutableSet.copyOf(
        beanFilter.getBeanFields(GadgetsHandlerApi.ProxyResponse.class, 5));
  }

  @Operation(httpMethods = "GET", path = "/@cajole.supportedFields")
  public Set<String> cajaSupportedFields(RequestItem request) {
    return ImmutableSet.copyOf(beanFilter
        .getBeanFields(GadgetsHandlerApi.CajaResponse.class, 5));
  }

  /**
   * Class to handle threaded reply.
   * Mainly it made to support filtering the id (url)
   */
  class CallableData {
    private final String id;
    private final GadgetsHandlerApi.BaseResponse data;
    public CallableData(String id, GadgetsHandlerApi.BaseResponse data) {
      this.id = id;
      this.data = data;
    }
    public String getId() { return id; }
    public GadgetsHandlerApi.BaseResponse getData() { return data; }
  }

  private abstract class AbstractExecutor {
    public Map<String, GadgetsHandlerApi.BaseResponse> execute(BaseRequestItem request) {
      Set<String> gadgetUrls = ImmutableSet.copyOf(request.getListParameter(Param.IDS.getName()));
      if (gadgetUrls.isEmpty()) {
        return ImmutableMap.of();
      }

      if (Strings.isNullOrEmpty(request.getParameter(Param.CONTAINER.getName()))) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
            "Missing container for request.");
      }

      ImmutableMap.Builder<String, GadgetsHandlerApi.BaseResponse> builder = ImmutableMap.builder();
      int badReq = 0;
      CompletionService<CallableData> completionService =
          new ExecutorCompletionService<CallableData>(executor);
      for (String gadgetUrl : gadgetUrls) {
        try {
          Callable<CallableData> job = createJob(gadgetUrl, request);
          completionService.submit(job);
        } catch (ProcessingException e) {
          // Fail to create and submit job
          builder.put(gadgetUrl, handlerService.createErrorResponse(null,
              e.getHttpStatusCode(), e.getMessage()));
          badReq++;
        }
      }

      for (int numJobs = gadgetUrls.size() - badReq; numJobs > 0; numJobs--) {
        CallableData response;
        try {
          response = completionService.take().get();
          builder.put(response.getId(), response.getData());
        } catch (InterruptedException e) {
          throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Processing interrupted.", e);
        } catch (ExecutionException e) {
          throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Processing error.", e);
        }
      }
      return builder.build();
    }

    protected abstract Callable<CallableData> createJob(String url, BaseRequestItem request)
        throws ProcessingException;
  }

  // Hook to override in sub-class.
  protected Callable<CallableData> createMetadataJob(final String url,
      BaseRequestItem request) throws ProcessingException {
    final MetadataRequestData metadataRequest = new MetadataRequestData(url, request);
    return new Callable<CallableData>() {
      public CallableData call() throws Exception {
        try {
          return new CallableData(url, handlerService.getMetadata(metadataRequest));
        } catch (Exception e) {
          return new CallableData(url,
              handlerService.createErrorResponse(null, e, FAILURE_METADATA));
        }
      }
    };
  }

  // Hook to override in sub-class.
  protected Callable<CallableData> createTokenJob(final String url,
      BaseRequestItem request) throws ProcessingException {
    // TODO: Get token duration from requests
    final TokenRequestData tokenRequest = new TokenRequestData(url, request);
    return new Callable<CallableData>() {
      public CallableData call() throws Exception {
        try {
          return new CallableData(url, handlerService.getToken(tokenRequest));
        } catch (Exception e) {
          return new CallableData(url,
            handlerService.createErrorResponse(null, e, FAILURE_TOKEN));
        }
      }
    };
  }

  // Hook to override in sub-class.
  protected Callable<CallableData> createProxyJob(final String url,
      BaseRequestItem request) throws ProcessingException {
    final ProxyRequestData proxyRequest = new ProxyRequestData(url, request);
    return new Callable<CallableData>() {
      public CallableData call() throws Exception {
        try {
          return new CallableData(url, handlerService.getProxy(proxyRequest));
        } catch (Exception e) {
          return new CallableData(url,
            handlerService.createErrorResponse(null, e, FAILURE_PROXY));
        }
      }
    };
  }

  // Hook to override in sub-class.
  protected Callable<CallableData> createCajaJob(final String url,
      BaseRequestItem request) throws ProcessingException {
    final CajaRequestData cajaRequest = new CajaRequestData(url, request);
    return new Callable<CallableData>() {
      public CallableData call() throws Exception {
        try {
          return new CallableData(url, handlerService.getCaja(cajaRequest));
        } catch (Exception e) {
          return new CallableData(url,
            handlerService.createErrorResponse(null, e, FAILURE_CAJA));
        }
      }
    };
  }


  /**
   * Gadget context classes used to translate JSON BaseRequestItem into a more
   * meaningful model objects that Java can work with.
   */
  private abstract class AbstractRequest implements GadgetsHandlerApi.BaseRequest {
    protected final Uri uri;
    protected final String container;
    protected final List<String> fields;
    protected final BaseRequestItem request;

    public AbstractRequest(String url, BaseRequestItem request, List<String> defaultFields)
        throws ProcessingException {
      try {
        this.uri = (url != null ? Uri.parse(url) : null);
      } catch (UriException e) {
        throw new ProcessingException("Bad url - " + url, HttpServletResponse.SC_BAD_REQUEST);
      }
      this.request = request;
      this.container = request.getParameter(Param.CONTAINER.getName());
      this.fields = processFields(request, defaultFields);
    }

    protected String getParam(BaseRequestItem request, Param field) {
      return request.getParameter(field.getName());
    }

    protected String getParam(BaseRequestItem request, Param field, String defaultValue) {
      return request.getParameter(field.getName(), defaultValue);
    }

    protected List<String> getListParam(BaseRequestItem request, Param field) {
      return request.getListParameter(field.getName());
    }

    protected Boolean getBooleanParam(BaseRequestItem request, Param field) {
      String val = request.getParameter(field.getName());
      if (val != null) {
        return "1".equals(val) || Boolean.valueOf(val);
      }
      return false;
    }

    protected Integer getIntegerParam(BaseRequestItem request, Param field)
        throws ProcessingException {
      String val = request.getParameter(field.getName());
      Integer intVal = null;
      if (val != null) {
        try {
          intVal = Integer.valueOf(val);
        } catch (NumberFormatException e) {
          throw new ProcessingException("Error parsing " + field + " parameter",
              HttpServletResponse.SC_BAD_REQUEST);
        }
      }
      return intVal;
    }

    public Uri getUrl() {
      return uri;
    }

    public String getContainer() {
      return container;
    }

    public List<String> getFields() {
      return fields;
    }

    private List<String> processFields(BaseRequestItem request, List<String> defaultList) {
      List<String> value = request.getListParameter(BaseRequestItem.FIELDS);
      return ((value == null || value.isEmpty()) ? defaultList : value);
    }
  }

  protected class JsRequestData extends AbstractRequest implements GadgetsHandlerApi.JsRequest {
    private final Integer refresh;
    private final boolean debug;
    private final boolean ignoreCache;
    private final List<String> features;
    private final List<String> loadedFeatures;
    private final RenderingContext context;
    private final String onload;
    private final String gadget;
    private final String repository;

    public JsRequestData(BaseRequestItem request) throws ProcessingException {
      super(null, request, DEFAULT_JS_FIELDS);
      this.ignoreCache = getBooleanParam(request, Param.NO_CACHE);
      this.debug = getBooleanParam(request, Param.DEBUG);
      this.refresh = getIntegerParam(request, Param.REFRESH);
      this.features = getListParam(request, Param.FEATURES);
      this.loadedFeatures = getListParam(request, Param.LOADED_FEATURES);
      this.context = getRenderingContext(getParam(request, Param.CONTAINER_MODE));
      this.onload = getParam(request, Param.ONLOAD);
      this.gadget = getParam(request, Param.GADGET);
      this.repository = getParam(request, Param.REPOSITORY);

    }

    public RenderingContext getContext() { return context; }
    public boolean getDebug() { return debug; }
    public List<String> getFeatures() { return features; }
    public List<String> getLoadedFeatures() { return loadedFeatures; }
    public boolean getIgnoreCache() { return ignoreCache; }
    public String getOnload() { return onload; }
    public Integer getRefresh() { return refresh; }
    public String getGadget() { return gadget; }
    public String getRepository() { return repository; }
  }

  private RenderingContext getRenderingContext(String param) {
    RenderingContext context = RenderingContext.GADGET;
    if ("1".equals(param)) {
      context = RenderingContext.CONTAINER;
    } else if ("2".equals(param)) {
      context = RenderingContext.CONFIGURED_GADGET;
    }
    return context;
  }

  protected class ProxyRequestData extends AbstractRequest
      implements GadgetsHandlerApi.ProxyRequest {

    private final String gadget;
    private final Integer refresh;
    private final boolean debug;
    private final boolean ignoreCache;
    private final String fallbackUrl;
    private final String mimetype;
    private final boolean sanitize;
    private final GadgetsHandlerApi.ImageParams imageParams;

    public ProxyRequestData(String url, BaseRequestItem request) throws ProcessingException {
      super(url, request, DEFAULT_PROXY_FIELDS);
      this.ignoreCache = getBooleanParam(request, Param.NO_CACHE);
      this.debug = getBooleanParam(request, Param.DEBUG);
      this.sanitize = getBooleanParam(request, Param.SANITIZE);
      this.gadget = getParam(request, Param.GADGET);
      this.fallbackUrl = getParam(request, Param.FALLBACK_URL);
      this.mimetype = getParam(request, Param.REWRITE_MIME);
      this.refresh = getIntegerParam(request, Param.REFRESH);
      imageParams = getImageParams(request);
    }

    private GadgetsHandlerApi.ImageParams getImageParams(BaseRequestItem request)
        throws ProcessingException {
      GadgetsHandlerApi.ImageParams params = null;
      Boolean doNotExpand = getBooleanParam(request, Param.NO_EXPAND);
      Integer height = getIntegerParam(request, Param.RESIZE_HEIGHT);
      Integer width = getIntegerParam(request, Param.RESIZE_WIDTH);
      Integer quality = getIntegerParam(request, Param.RESIZE_QUALITY);

      if (height != null || width != null) {
        return beanDelegator.createDelegator(null, GadgetsHandlerApi.ImageParams.class,
            ImmutableMap.<String, Object>of(
                "height", BeanDelegator.nullable(height),
                "width", BeanDelegator.nullable(width),
                "quality", BeanDelegator.nullable(quality),
                "donotexpand", BeanDelegator.nullable(doNotExpand)));
      }
      return params;
    }

    public boolean getDebug() {
      return debug;
    }

    public String getFallbackUrl() {
      return fallbackUrl;
    }

    public boolean getIgnoreCache() {
      return ignoreCache;
    }

    public GadgetsHandlerApi.ImageParams getImageParams() {
      return imageParams;
    }

    public Integer getRefresh() {
      return refresh;
    }

    public String getRewriteMimeType() {
      return mimetype;
    }

    public boolean getSanitize() {
      return sanitize;
    }

    public String getGadget() {
      return gadget;
    }
  }

  protected class TokenRequestData extends AbstractRequest
      implements GadgetsHandlerApi.TokenRequest {

    private Long moduleId;

    public TokenRequestData(String url, BaseRequestItem request)
        throws ProcessingException {
      super(url, request, DEFAULT_TOKEN_FIELDS);

      // The moduleId for the gadget (if it exists) is the fragment of the URI:
      //  ex: http://example.com/gadget.xml#1 or http://example.com/gadget.xml
      // zero is implied if missing.
      String moduleId = this.uri.getFragmentParameter("moduleId");
      this.moduleId = moduleId == null ? 0 : Long.valueOf(moduleId);
    }

    public GadgetsHandlerApi.AuthContext getAuthContext() {
      return beanDelegator.createDelegator(
          request.getToken(), GadgetsHandlerApi.AuthContext.class);
    }

    public Long getModuleId() {
      return moduleId;
    }
  }

  @VisibleForTesting
  static GadgetsHandlerApi.RenderingType getRenderingType(String value)
      throws ProcessingException {
    GadgetsHandlerApi.RenderingType type = GadgetsHandlerApi.RenderingType.DEFAULT;
    if (value != null) {
      try {
        type = GadgetsHandlerApi.RenderingType.valueOf(value.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new ProcessingException("Error parsing rendering type parameter",
            HttpServletResponse.SC_BAD_REQUEST);
      }
    }
    return type;
  }

  protected class CajaRequestData extends AbstractRequest
      implements GadgetsHandlerApi.CajaRequest {
    private final String mimeType;
    private final boolean debug;

    public CajaRequestData(String url, BaseRequestItem request)
        throws ProcessingException {
      super(url, request, DEFAULT_CAJA_FIELDS);
      this.mimeType = getParam(request, Param.MIME_TYPE, "text/html");
      this.debug = getBooleanParam(request, Param.DEBUG);
    }

    public String getMimeType() {
      return mimeType;
    }

    public boolean getDebug() {
      return debug;
    }
  }

  protected class MetadataRequestData extends AbstractRequest
      implements GadgetsHandlerApi.MetadataRequest {
    protected final Locale locale;
    protected final boolean ignoreCache;
    protected final boolean debug;
    protected final GadgetsHandlerApi.RenderingType renderingType;

    public MetadataRequestData(String url, BaseRequestItem request)
        throws ProcessingException {
      super(url, request, DEFAULT_METADATA_FIELDS);
      String lang = request.getParameter("language");
      String country = request.getParameter("country");
      this.locale =
          (lang != null && country != null) ? new Locale(lang, country) : (lang != null)
              ? new Locale(lang) : GadgetSpec.DEFAULT_LOCALE;
      this.ignoreCache = getBooleanParam(request, Param.NO_CACHE);
      this.debug = getBooleanParam(request, Param.DEBUG);
      this.renderingType = GadgetsHandler.getRenderingType(getParam(request, Param.RENDER_TYPE));
    }

    public int getModuleId() {
      return 1; // TODO calculate?
    }

    public Locale getLocale() {
      return locale;
    }

    public boolean getIgnoreCache() {
      return ignoreCache;
    }

    public boolean getDebug() {
      return debug;
    }

    public String getView() {
      return getParam(request, Param.VIEW, "default");
    }

    public GadgetsHandlerApi.AuthContext getAuthContext() {
      return beanDelegator.createDelegator(
        request.getToken(), GadgetsHandlerApi.AuthContext.class);
    }

    public GadgetsHandlerApi.RenderingType getRenderingType() {
      return renderingType;
    }
  }
}
