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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LinkSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletResponse;

@Service(name = "gadgets")
public class GadgetsHandler {
  @VisibleForTesting
  static final String FAILURE_METADATA = "Failed to get gadget metadata.";
  @VisibleForTesting
  static final String FAILURE_TOKEN = "Failed to get gadget token.";

  private static final Set<String> DEFAULT_METADATA_FIELDS =
      ImmutableSet.of("iframeUrl", "userPrefs.*", "modulePrefs.*", "views.*", "token");

  protected final ExecutorService executor;
  protected final Processor processor;
  protected final IframeUriManager iframeUriManager;
  protected final SecurityTokenCodec securityTokenCodec;

  protected final BeanDelegator beanDelegator;
  protected final BeanFilter beanFilter;

  // Map shindig data class to API interfaces
  @VisibleForTesting
  static final Map<Class<?>, Class<?>> apiClasses =
      new ImmutableMap.Builder<Class<?>, Class<?>>()
          .put(BaseResponseData.class, GadgetsHandlerApi.BaseResponse.class)
          .put(MetadataResponseData.class, GadgetsHandlerApi.MetadataResponse.class)
          .put(TokenResponseData.class, GadgetsHandlerApi.TokenResponse.class)
          .put(View.class, GadgetsHandlerApi.View.class)
          .put(UserPref.class, GadgetsHandlerApi.UserPref.class)
          .put(EnumValuePair.class, GadgetsHandlerApi.EnumValuePair.class)
          .put(ModulePrefs.class, GadgetsHandlerApi.ModulePrefs.class)
          .put(Feature.class, GadgetsHandlerApi.Feature.class)
          .put(LinkSpec.class, GadgetsHandlerApi.LinkSpec.class)
          // Enums
          .put(View.ContentType.class, GadgetsHandlerApi.ViewContentType.class)
          .put(UserPref.DataType.class, GadgetsHandlerApi.UserPrefDataType.class)
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
          .build();

  @Inject
  public GadgetsHandler(ExecutorService executor, Processor processor,
      IframeUriManager iframeUriManager, SecurityTokenCodec securityTokenCodec,
      BeanFilter beanFilter) {
    this.executor = executor;
    this.processor = processor;
    this.iframeUriManager = iframeUriManager;
    this.securityTokenCodec = securityTokenCodec;
    this.beanFilter = beanFilter;

    // TODO: maybe make this injectable
    this.beanDelegator = new BeanDelegator(apiClasses, enumConversionMap);
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "metadata.get")
  public Map<String, GadgetsHandlerApi.MetadataResponse> metadata(BaseRequestItem request)
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
  public Map<String, GadgetsHandlerApi.TokenResponse> token(BaseRequestItem request)
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
    public Map<String, R> execute(BaseRequestItem request) {
      Set<String> gadgetUrls = ImmutableSet.copyOf(request.getListParameter("ids"));
      if (gadgetUrls.isEmpty()) {
        return ImmutableMap.of();
      }

      CompletionService<R> completionService = new ExecutorCompletionService<R>(executor);
      for (String gadgetUrl : gadgetUrls) {
        Callable<R> job = createJob(gadgetUrl, request);
        completionService.submit(job);
      }

      ImmutableMap.Builder<String, R> builder = ImmutableMap.builder();
      for (int numJobs = gadgetUrls.size(); numJobs > 0; numJobs--) {
        R response;
        try {
          response = completionService.take().get();
          builder.put(response.getUrl(), response);
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
            String url = context.getUrl().toString();
            R errorResponse =
                (R) beanDelegator.createDelegator(new BaseResponseData(url, cause.getMessage()));
            builder.put(url, errorResponse);
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
    final GadgetContext context = new MetadataGadgetContext(url, request);
    final Set<String> fields =
        beanFilter.processBeanFields(request.getFields(DEFAULT_METADATA_FIELDS));
    return new Callable<GadgetsHandlerApi.MetadataResponse>() {
      public GadgetsHandlerApi.MetadataResponse call() throws Exception {
        try {
          Gadget gadget = processor.process(context);
          String iframeUrl =
              fields.contains("iframeurl") ? iframeUriManager.makeRenderingUri(gadget).toString()
                  : null;
          MetadataResponseData response =
              new MetadataResponseData(context.getUrl().toString(), gadget.getSpec(), iframeUrl);
          return (GadgetsHandlerApi.MetadataResponse) beanFilter.createFilteredBean(beanDelegator
              .createDelegator(response), fields);
        } catch (Exception e) {
          // Note: this error message is publicly visible in JSON-RPC response.
          throw new RpcException(context, FAILURE_METADATA, e);
        }
      }
    };
  }

  // Hook to override in sub-class.
  protected Callable<GadgetsHandlerApi.TokenResponse> createTokenJob(String url,
      BaseRequestItem request) {
    final TokenGadgetContext context = new TokenGadgetContext(url, request);
    final Set<String> fields =
        beanFilter.processBeanFields(request.getFields(DEFAULT_METADATA_FIELDS));
    return new Callable<GadgetsHandlerApi.TokenResponse>() {
      public GadgetsHandlerApi.TokenResponse call() throws Exception {
        try {
          String token = securityTokenCodec.encodeToken(context.getToken());
          TokenResponseData response = new TokenResponseData(context.getUrl().toString(), token);
          return (GadgetsHandlerApi.TokenResponse) beanFilter.createFilteredBean(beanDelegator
              .createDelegator(response), fields);
        } catch (Exception e) {
          // Note: this error message is publicly visible in JSON-RPC response.
          throw new RpcException(context, FAILURE_TOKEN, e);
        }
      }
    };
  }

  /**
   * Gadget context classes used to translate JSON BaseRequestItem into a more
   * meaningful model objects that Java can work with.
   */

  private abstract class AbstractGadgetContext extends GadgetContext {
    protected final Uri uri;
    protected final String container;
    protected final BaseRequestItem request;

    public AbstractGadgetContext(String url, BaseRequestItem request) {
      this.uri = Uri.parse(Preconditions.checkNotNull(url));
      this.request = Preconditions.checkNotNull(request);
      this.container = Preconditions.checkNotNull(request.getParameter("container"));
    }

    @Override
    public Uri getUrl() {
      return uri;
    }

    @Override
    public String getContainer() {
      return container;
    }

    @Override
    public RenderingContext getRenderingContext() {
      return RenderingContext.METADATA;
    }
  }

  protected class MetadataGadgetContext extends AbstractGadgetContext {
    protected final Locale locale;
    protected final boolean ignoreCache;
    protected final boolean debug;

    public MetadataGadgetContext(String url, BaseRequestItem request) {
      super(url, request);
      String lang = request.getParameter("language");
      String country = request.getParameter("country");
      this.locale =
          (lang != null && country != null) ? new Locale(lang, country) : (lang != null)
              ? new Locale(lang) : GadgetSpec.DEFAULT_LOCALE;
      this.ignoreCache = Boolean.valueOf(request.getParameter("ignoreCache"));
      this.debug = Boolean.valueOf(request.getParameter("debug"));
    }

    @Override
    public int getModuleId() {
      return 1; // TODO calculate?
    }

    @Override
    public Locale getLocale() {
      return locale;
    }

    @Override
    public boolean getIgnoreCache() {
      return ignoreCache;
    }

    @Override
    public boolean getDebug() {
      return debug;
    }

    @Override
    public String getView() {
      return request.getParameter("view", "default");
    }

    @Override
    public SecurityToken getToken() {
      return request.getToken();
    }
  }

  protected class TokenGadgetContext extends AbstractGadgetContext {
    public TokenGadgetContext(String url, BaseRequestItem request) {
      super(url, request);
    }

    @Override
    public SecurityToken getToken() {
      return request.getToken();
    }
  }

  /**
   * Response classes to represent data structure returned in JSON to common
   * container JS. They must be public for reflection to work.
   */

  public static class BaseResponseData {
    private final String url;
    private final String error;

    // Call this to indicate an error.
    public BaseResponseData(String url, String error) {
      this.url = url;
      this.error = error;
    }

    // Have sub-class call this to indicate a success response.
    protected BaseResponseData(String url) {
      this(url, null);
    }

    public String getUrl() {
      return url;
    }

    public String getError() {
      return error;
    }
  }

  public static class MetadataResponseData extends BaseResponseData {
    private final GadgetSpec spec;
    private final String iframeUrl;

    public MetadataResponseData(String url, GadgetSpec spec, String iframeUrl) {
      super(url);
      this.spec = spec;
      this.iframeUrl = iframeUrl;
    }

    public String getIframeUrl() {
      return iframeUrl;
    }

    public String getChecksum() {
      return spec.getChecksum();
    }

    public ModulePrefs getModulePrefs() {
      return spec.getModulePrefs();
    }

    public Map<String, UserPref> getUserPrefs() {
      return spec.getUserPrefs();
    }

    public Map<String, View> getViews() {
      return spec.getViews();
    }
  }


  public static class TokenResponseData extends BaseResponseData {
    private final String token;

    public TokenResponseData(String url, String token) {
      super(url);
      this.token = token;
    }

    public String getToken() {
      return token;
    }
  }

}
