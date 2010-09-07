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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
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

  private static final List<String> DEFAULT_METADATA_FIELDS =
      ImmutableList.of("iframeUrl", "userPrefs.*", "modulePrefs.*", "views.*", "token");

  private static final List<String> DEFAULT_TOKEN_FIELDS = ImmutableList.of("token");

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

  @Operation(httpMethods = {"POST", "GET"}, path = "metadata.get")
  public Map<String, GadgetsHandlerApi.BaseResponse> metadata(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor<GadgetsHandlerApi.MetadataResponse>() {
      @Override
      protected Callable<GadgetsHandlerApi.MetadataResponse> createJob(String url,
          BaseRequestItem request) {
        return createMetadataJob(url, request);
      }
    }.execute(request);
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "token.get")
  public Map<String, GadgetsHandlerApi.BaseResponse> token(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor<GadgetsHandlerApi.TokenResponse>() {
      @Override
      protected Callable<GadgetsHandlerApi.TokenResponse> createJob(String url,
          BaseRequestItem request) {
        return createTokenJob(url, request);
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

  private abstract class AbstractExecutor<R extends GadgetsHandlerApi.BaseResponse> {
    @SuppressWarnings("unchecked")
    public Map<String, GadgetsHandlerApi.BaseResponse> execute(BaseRequestItem request) {
      Set<String> gadgetUrls = ImmutableSet.copyOf(request.getListParameter("ids"));
      if (gadgetUrls.isEmpty()) {
        return ImmutableMap.of();
      }

      CompletionService<R> completionService = new ExecutorCompletionService<R>(executor);
      for (String gadgetUrl : gadgetUrls) {
        Callable<R> job = createJob(gadgetUrl, request);
        completionService.submit(job);
      }

      ImmutableMap.Builder<String, GadgetsHandlerApi.BaseResponse> builder = ImmutableMap.builder();
      for (int numJobs = gadgetUrls.size(); numJobs > 0; numJobs--) {
        R response;
        try {
          response = completionService.take().get();
          builder.put(response.getUrl().toString(), response);
        } catch (InterruptedException e) {
          throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Processing interrupted.", e);
        } catch (ExecutionException e) {
          if (!(e.getCause() instanceof RpcException)) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Processing error.", e);
          }
          RpcException cause = (RpcException) e.getCause();
          GadgetContext context = cause.getContext();
          if (context != null) {
            Uri url = context.getUrl();
            GadgetsHandlerApi.BaseResponse errorResponse =
                handlerService.createBaseResponse(url, cause.getMessage());
            builder.put(url.toString(), errorResponse);
          }
        }
      }
      return builder.build();
    }

    protected abstract Callable<R> createJob(String url, BaseRequestItem request);
  }

  // Hook to override in sub-class.
  protected Callable<GadgetsHandlerApi.MetadataResponse> createMetadataJob(String url,
      BaseRequestItem request) {
    final MetadataRequestData metadataRequest = new MetadataRequestData(url, request);
    return new Callable<GadgetsHandlerApi.MetadataResponse>() {
      public GadgetsHandlerApi.MetadataResponse call() throws Exception {
        try {
          return handlerService.getMetadata(metadataRequest);
        } catch (Exception e) {
          sendError(metadataRequest.getUrl(), e, FAILURE_METADATA);
          return null;
        }
      }
    };
  }

  // Hook to override in sub-class.
  protected Callable<GadgetsHandlerApi.TokenResponse> createTokenJob(String url,
      BaseRequestItem request) {
    final TokenRequestData tokenRequest = new TokenRequestData(url, request);
    return new Callable<GadgetsHandlerApi.TokenResponse>() {
      public GadgetsHandlerApi.TokenResponse call() throws Exception {
        try {
          return handlerService.getToken(tokenRequest);
        } catch (Exception e) {
          sendError(tokenRequest.getUrl(), e, FAILURE_TOKEN);
          return null;
        }
      }
    };
  }

  private void sendError(final Uri url, Exception e, String msg)
      throws RpcException {
    GadgetContext context = new GadgetContext() {
      @Override
      public Uri getUrl() { return url; }
    };
    // Note: this error message is publicly visible in JSON-RPC response.
    throw new RpcException(context, msg, e);
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

    public AbstractRequest(String url, BaseRequestItem request, List<String> defaultFields) {
      this.uri = Uri.parse(Preconditions.checkNotNull(url));
      this.request = Preconditions.checkNotNull(request);
      this.container = Preconditions.checkNotNull(request.getParameter("container"));
      this.fields = processFields(request, defaultFields);
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


  protected class TokenRequestData extends AbstractRequest
      implements GadgetsHandlerApi.TokenRequest {

    public TokenRequestData(String url, BaseRequestItem request) {
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

    public MetadataRequestData(String url, BaseRequestItem request) {
      super(url, request, DEFAULT_METADATA_FIELDS);
      String lang = request.getParameter("language");
      String country = request.getParameter("country");
      this.locale =
          (lang != null && country != null) ? new Locale(lang, country) : (lang != null)
              ? new Locale(lang) : GadgetSpec.DEFAULT_LOCALE;
      this.ignoreCache = Boolean.valueOf(request.getParameter("ignoreCache"));
      this.debug = Boolean.valueOf(request.getParameter("debug"));
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
