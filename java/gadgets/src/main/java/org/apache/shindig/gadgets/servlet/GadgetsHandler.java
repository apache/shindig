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
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;

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

  private static final Set<String> ALL_METADATA_FIELDS = ImmutableSet.of(
      "iframeUrl", "userPrefs", "modulePrefs", "views", "views.name", "views.type",
      "views.type", "views.href", "views.quirks", "views.content",
      "views.preferredHeight", "views.preferredWidth",
      "views.needsUserPrefsSubstituted", "views.attributes");
  private static final Set<String> DEFAULT_METADATA_FIELDS = ImmutableSet.of(
      "iframeUrl", "userPrefs", "modulePrefs", "views");

  protected final ExecutorService executor;
  protected final Processor processor;
  protected final IframeUriManager iframeUriManager;
  protected final SecurityTokenDecoder securityTokenDecoder;

  @Inject
  public GadgetsHandler(
      ExecutorService executor,
      Processor processor,
      IframeUriManager iframeUriManager,
      SecurityTokenDecoder securityTokenDecoder) {
    this.executor = executor;
    this.processor = processor;
    this.iframeUriManager = iframeUriManager;
    this.securityTokenDecoder = securityTokenDecoder;
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "metadata.get")
  public Map<String, MetadataResponse> metadata(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor<MetadataResponse>() {
      @Override
      protected Callable<MetadataResponse> createJob(String url, BaseRequestItem request) {
        return createMetadataJob(url, request);
      }
    }.execute(request);
  }

  @Operation(httpMethods = {"POST", "GET"}, path = "token.get")
  public Map<String, TokenResponse> token(BaseRequestItem request)
      throws ProtocolException {
    return new AbstractExecutor<TokenResponse>() {
      @Override
      protected Callable<TokenResponse> createJob(String url, BaseRequestItem request) {
        return createTokenJob(url, request);
      }
    }.execute(request);
  }

  @Operation(httpMethods = "GET", path="/@metadata.supportedFields")
  public Set<String> supportedFields(RequestItem request) {
    return ALL_METADATA_FIELDS;
  }

  private abstract class AbstractExecutor<R extends BaseResponse> {
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
          throw new ProtocolException(
              HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Processing interrupted.", e);
        } catch (ExecutionException e) {
          if (!(e.getCause() instanceof RpcException)) {
            throw new ProtocolException(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Processing error.", e);
          }
          RpcException cause = (RpcException) e.getCause();
          GadgetContext context = cause.getContext();
          if (context != null) {
            String url = context.getUrl().toString();
            R errorResponse = (R) new BaseResponse(url, cause.getMessage());
            builder.put(url, errorResponse);
          }
        }
      }
      return builder.build();
    }

    protected abstract Callable<R> createJob(String url, BaseRequestItem request);
  }

  // Hook to override in sub-class.
  protected Callable<MetadataResponse> createMetadataJob(String url, BaseRequestItem request) {
    final GadgetContext context = new MetadataGadgetContext(url, request);
    final Set<String> fields = request.getFields(DEFAULT_METADATA_FIELDS);
    return new Callable<MetadataResponse>() {
      public MetadataResponse call() throws Exception {
        try {
          Gadget gadget = processor.process(context);
          String iframeUrl = fields.contains("iframeUrl")
              ? iframeUriManager.makeRenderingUri(gadget).toString() : null;
          return new MetadataResponse(context.getUrl().toString(), gadget.getSpec(),
              iframeUrl, fields);
        } catch (Exception e) {
          // Note: this error message is publicly visible in JSON-RPC response.
          throw new RpcException(context, FAILURE_METADATA, e);
        }
      }
    };
  }

  // Hook to override in sub-class.
  protected Callable<TokenResponse> createTokenJob(String url, BaseRequestItem request) {
    final TokenGadgetContext context = new TokenGadgetContext(url, request);
    return new Callable<TokenResponse>() {
      public TokenResponse call() throws Exception {
        try {
          String token = securityTokenDecoder.encodeToken(context.getToken());
          return new TokenResponse(context.getUrl().toString(), token);
        } catch (Exception e) {
          // Note: this error message is publicly visible in JSON-RPC response.
          throw new RpcException(context, FAILURE_TOKEN, e);
        }
      }
    };
  }

  /**
   * Gadget context classes used to translate JSON BaseRequestItem into a
   * more meaningful model objects that Java can work with.
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
      this.locale = (lang != null && country != null) ? new Locale(lang,country) :
                    (lang != null) ? new Locale(lang) :
                    GadgetSpec.DEFAULT_LOCALE;
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

  public static class BaseResponse {
    private final String url;
    private final String error;

    // Call this to indicate an error.
    public BaseResponse(String url, String error) {
      this.url = url;
      this.error = error;
    }

    // Have sub-class call this to indicate a success response.
    protected BaseResponse(String url) {
      this(url, null);
    }

    public String getUrl() {
      return url;
    }

    public String getError() {
      return error;
    }
  }

  public static class MetadataResponse extends BaseResponse {
    private final GadgetSpec spec;
    private final String iframeUrl;
    private final Map<String, ViewResponse> views;
    private final Set<String> fields;

    public MetadataResponse(String url, GadgetSpec spec, String iframeUrl, Set<String> fields) {
      super(url);
      this.spec = spec;
      this.iframeUrl = iframeUrl;
      this.fields = fields;

      // Do we need view data?
      boolean viewsRequested = fields.contains("views");
      for (String f: fields) {
        if (f.startsWith("views")) {
          viewsRequested = true;
        }
      }
      if (viewsRequested) {
        ImmutableMap.Builder<String, ViewResponse> builder = ImmutableMap.builder();
        for (Map.Entry<String,View> entry : spec.getViews().entrySet()) {
          builder.put(entry.getKey(), new ViewResponse(entry.getValue(), fields));
        }
        views = builder.build();
      } else {
        views = null;
      }
    }

    public String getIframeUrl() {
      return fields.contains("iframeUrl") ? iframeUrl : null;
    }

    public String getChecksum() {
      return fields.contains("checksum") ? spec.getChecksum() : null;
    }

    public ModulePrefs getModulePrefs() {
      return fields.contains("modulePrefs") ? spec.getModulePrefs() : null;
    }

    public Map<String, UserPref> getUserPrefs() {
      return fields.contains("userPrefs") ? spec.getUserPrefs() : null;
    }

    public Map<String, ViewResponse> getViews() {
      return views;
    }
  }

  public static class ViewResponse {
    private final View view;
    private final Set<String> fields;

    /**
     * Return the actual item if the requested fields contains "views" or param
     * @param item any item
     * @param param a field to test for
     * @param <T> any type
     * @return Returns item if fields contains "views" or param
     */
    private <T> T filter(T item, String param) {
      return (fields.contains("views") || fields.contains(param)) ? item : null;
    }

    public ViewResponse(View view, Set<String> fields) {
      this.view = view;
      this.fields = fields;
    }

    public String getName() {
      return filter(view.getName(), "views.name");
    }

    public View.ContentType getType() {
      return filter(view.getType(), "views.type");
    }

    public Uri getHref() {
      return filter(view.getHref(), "views.href");
    }

    public Boolean getQuirks() {
      return filter(view.getQuirks(), "views.quirks");
    }

    public String getContent() {
      return fields.contains("views.content") ? view.getContent() : null;
    }

    public Integer getPreferredHeight() {
      return filter(view.getPreferredHeight(), "views.preferredHeight");
    }

    public Integer getPreferredWidth() {
      return filter(view.getPreferredWidth(), "views.preferredWidth");
    }

    public Boolean needsUserPrefSubstitution() {
      return filter(view.needsUserPrefSubstitution(), "views.needsUserPrefSubstitution");
    }

    public Map<String, String> getAttributes() {
      return filter(view.getAttributes(), "views.attributes");
    }
  }

  public static class TokenResponse extends BaseResponse {
    private final String token;

    public TokenResponse(String url, String token) {
      super(url);
      this.token = token;
    }

    public String getToken() {
      return token;
    }
  }

}