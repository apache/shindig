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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.Uri.UriException;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletResponse;

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

  private static final List<String> DEFAULT_METADATA_FIELDS =
      ImmutableList.of("iframeUrl", "userPrefs.*", "modulePrefs.*", "views.*");

  private static final List<String> DEFAULT_TOKEN_FIELDS = ImmutableList.of("token");

  private static final List<String> DEFAULT_PROXY_FIELDS = ImmutableList.of("proxyUrl");


  protected final ExecutorService executor;
  protected final GadgetsHandlerService handlerService;

  protected final BeanFilter beanFilter;
  protected final BeanDelegator beanDelegator;

  @Inject
  public GadgetsHandler(ExecutorService executor, GadgetsHandlerService handlerService,
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

  @Operation(httpMethods = "GET", path = "/@proxy.supportedFields")
  public Set<String> proxySupportedFields(RequestItem request) {
    return ImmutableSet.copyOf(
        beanFilter.getBeanFields(GadgetsHandlerApi.ProxyResponse.class, 5));
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
      Set<String> gadgetUrls = ImmutableSet.copyOf(request.getListParameter("ids"));
      if (gadgetUrls.isEmpty()) {
        return ImmutableMap.of();
      }

      if (StringUtils.isEmpty(request.getParameter("container"))) {
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
    final TokenRequestData tokenRequest = new TokenRequestData(url, request, null);
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
        this.uri = Uri.parse(url);
      } catch (UriException e) {
        throw new ProcessingException("Bad url - " + url, HttpServletResponse.SC_BAD_REQUEST);
      }
      this.request = request;
      this.container = request.getParameter("container");
      this.fields = processFields(request, defaultFields);
    }

    protected Boolean getBooleanParam(BaseRequestItem request, String field) {
      String val = request.getParameter(field);
      if (val != null) {
        return "1".equals(val) || Boolean.valueOf(val);
      }
      return false;
    }

    protected Integer getIntegerParam(BaseRequestItem request, String field)
        throws ProcessingException {
      String val = request.getParameter(field);
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
      return ((value == null || value.size() == 0) ? defaultList : value);
    }
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
    private final boolean cajole;
    private final GadgetsHandlerApi.ImageParams imageParams;

    public ProxyRequestData(String url, BaseRequestItem request) throws ProcessingException {
      super(url, request, DEFAULT_PROXY_FIELDS);
      this.ignoreCache = getBooleanParam(request, "ignoreCache");
      this.debug = getBooleanParam(request, "debug");
      this.sanitize = getBooleanParam(request, "sanitize");
      this.cajole = getBooleanParam(request, "cajole");
      this.gadget = request.getParameter("gadget");
      this.fallbackUrl = request.getParameter("fallback_url");
      this.mimetype = request.getParameter("rewriteMime");
      this.refresh = getIntegerParam(request, "refresh");
      imageParams = getImageParams(request);
    }

    private GadgetsHandlerApi.ImageParams getImageParams(BaseRequestItem request)
        throws ProcessingException {
      GadgetsHandlerApi.ImageParams params = null;
      Boolean doNotExpand = getBooleanParam(request, "no_expand");
      Integer height = getIntegerParam(request, "resize_h");
      Integer width = getIntegerParam(request, "resize_w");
      Integer quality = getIntegerParam(request, "resize_q");

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

    public boolean getIgnoreCahce() {
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

    public boolean getCajole() {
      return cajole;
    }

    public String getGadget() {
      return gadget;
    }
  }

  protected class TokenRequestData extends AbstractRequest
      implements GadgetsHandlerApi.TokenRequest {

    public TokenRequestData(String url, BaseRequestItem request, Long durationSeconds)
        throws ProcessingException {
      super(url, request, DEFAULT_TOKEN_FIELDS);
    }

    public GadgetsHandlerApi.TokenData getToken() {
      return beanDelegator.createDelegator(
          request.getToken(), GadgetsHandlerApi.TokenData.class);
    }
  }


  protected class MetadataRequestData extends AbstractRequest
      implements GadgetsHandlerApi.MetadataRequest {
    protected final Locale locale;
    protected final boolean ignoreCache;
    protected final boolean debug;

    public MetadataRequestData(String url, BaseRequestItem request)
        throws ProcessingException {
      super(url, request, DEFAULT_METADATA_FIELDS);
      String lang = request.getParameter("language");
      String country = request.getParameter("country");
      this.locale =
          (lang != null && country != null) ? new Locale(lang, country) : (lang != null)
              ? new Locale(lang) : GadgetSpec.DEFAULT_LOCALE;
      this.ignoreCache = getBooleanParam(request, "ignoreCache");
      this.debug = getBooleanParam(request, "debug");
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
      return request.getParameter("view", "default");
    }

    public GadgetsHandlerApi.TokenData getToken() {
      return beanDelegator.createDelegator(
        request.getToken(), GadgetsHandlerApi.TokenData.class);
    }
  }
}
