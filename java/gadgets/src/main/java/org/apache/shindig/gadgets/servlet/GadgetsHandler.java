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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;
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

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

@Service(name = "gadgets")
public class GadgetsHandler {
  Set<String> ALL_METADATA_FIELDS = ImmutableSet.of("iframeUrl", "userPrefs", "modulePrefs", "views", "views.name", "views.type",
      "views.type", "views.href", "views.quirks", "views.content", "views.preferredHeight", "views.preferredWidth",
      "views.needsUserPrefsSubstituted", "views.attributes");
  Set<String> DEFAULT_METADATA_FIELDS = ImmutableSet.of("iframeUrl", "userPrefs", "modulePrefs", "views");

  protected final ExecutorService executor;
  protected final Processor processor;
  protected final IframeUriManager iframeUriManager;

  @Inject
  public GadgetsHandler(ExecutorService executor, Processor processor, IframeUriManager iframeUriManager) {
    this.executor = executor;
    this.processor = processor;
    this.iframeUriManager = iframeUriManager;
  }

  @Operation(httpMethods = {"POST","GET"}, path = "/metadata/{view}")
  public Map<String,MetadataGadgetSpec> metadata(BaseRequestItem request) throws ProtocolException {
    Set<String> gadgetUrls = ImmutableSet.copyOf(request.getListParameter("ids"));

    if (gadgetUrls.isEmpty())
      return ImmutableMap.of();

    Set<String> fields = request.getFields(DEFAULT_METADATA_FIELDS);

    CompletionService<MetadataGadgetSpec> completionService =  new ExecutorCompletionService<MetadataGadgetSpec>(executor);

    for (String gadgetUri : gadgetUrls) {
      completionService.submit(createNewJob(new MetadataGadgetContext(gadgetUri,request), fields));
    }

    int numJobs = gadgetUrls.size();
    Map<String,MetadataGadgetSpec> response = Maps.newHashMap();

    while (numJobs > 0) {
      try {
        MetadataGadgetSpec spec = completionService.take().get();
        response.put(spec.getUrl(), spec);
      } catch (InterruptedException e) {
        throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Processing interrupted", e);
      } catch (ExecutionException ee) {
        if (!(ee.getCause() instanceof RpcException)) {
          throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Processing error", ee);
        }
        RpcException e = (RpcException)ee.getCause();
        // Just one gadget failed; mark it as such.
        GadgetContext context = e.getContext();
        if (context != null) {
          response.put(context.getUrl().toString(), new MetadataGadgetSpec().setError(e.getCause().getLocalizedMessage()));
        }
      } finally {
        numJobs--;
      }
    }
    return response;
  }

  @Operation(httpMethods = "GET", path="/@supportedFields")
  public Set<String> supportedFields(RequestItem request) {
    return ALL_METADATA_FIELDS;
  }

  protected MetadataJob createNewJob(GadgetContext context, Set<String> fields) {
    return new MetadataJob(context, fields);
  }

  protected class MetadataJob implements Callable<MetadataGadgetSpec> {
    protected final GadgetContext context;
    protected final Set<String> fields;

    public MetadataJob(GadgetContext context, Set<String> fields) {
      this.context = context;
      this.fields = fields;
    }

    public MetadataGadgetSpec call() throws RpcException {
      try {
        Gadget gadget = processor.process(context);
        String iframeUrl =  fields.contains("iframeUrl") ? iframeUriManager.makeRenderingUri(gadget).toString() : null;

        FilteringGadgetSpec spec = new FilteringGadgetSpec(gadget.getSpec(), iframeUrl, fields);
        spec.setUrl(context.getUrl().toString());
        return spec;
      } catch (Exception e) {
        throw new RpcException(context, e);
      }
    }
  }

  /**
   * Localized implementation of GadgetContext that uses information from the request.
   */
  private static class MetadataGadgetContext extends GadgetContext {
    final BaseRequestItem request;
    final Uri uri;
    final Locale locale;
    final boolean ignoreCache;
    final boolean debug;
    final String container;

    public MetadataGadgetContext(String uri, BaseRequestItem request) {
      this.request = Preconditions.checkNotNull(request);
      this.uri = Uri.parse(Preconditions.checkNotNull(uri));

      String lang = request.getParameter("language");
      String country = request.getParameter("country");

      this.locale = (lang != null && country != null) ? new Locale(lang,country) :
                    (lang != null) ? new Locale(lang) :
                    GadgetSpec.DEFAULT_LOCALE;

      this.ignoreCache = Boolean.valueOf(request.getParameter("ignoreCache"));
      this.debug = Boolean.valueOf(request.getParameter("debug"));
      this.container = request.getToken().getContainer();
    }

    @Override
    public Uri getUrl() {
      return uri;
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
    public RenderingContext getRenderingContext() {
      return RenderingContext.METADATA;
    }

    @Override
    public boolean getIgnoreCache() {
      return ignoreCache;
    }

    @Override
    public String getContainer() {
      return container;
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
    public UserPrefs getUserPrefs() {
            // TODO
      return new UserPrefs(Maps.<String,String>newHashMap());
    }

    @Override
    public SecurityToken getToken() {
      return request.getToken();
    }
  }


  // has to be public for reflection to work.
  public static final class FilteringView {
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

    public FilteringView(View view, Set<String> fields) {
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

  // has to be public for reflection to work..
  public static class MetadataGadgetSpec {
    private String msg = null;
    private String url = null;

    public MetadataGadgetSpec setError(String msg) {
      this.msg = msg;
      return this;
    }
    public MetadataGadgetSpec setUrl(String url) {
      this.url = url;
      return this;
    }
    public String getError() {
      return msg;  
    }
    public String getUrl() {
      return url;
    }
  }
  
  // has to be public for reflection to work.
  public static final class FilteringGadgetSpec extends MetadataGadgetSpec {
    private final GadgetSpec spec;
    private final String iframeUrl;
    private final Map<String,FilteringView> views;
    private final Set<String> fields;

    public FilteringGadgetSpec(GadgetSpec spec, String iframeUrl, Set<String> fields) {
      this.spec = Preconditions.checkNotNull(spec);
      this.iframeUrl = iframeUrl; // can be null
      this.fields = Preconditions.checkNotNull(fields);

      // Do we need view data?
      boolean viewsRequested = fields.contains("views");
      for (String f: fields) {
        if (f.startsWith("views")) {
          viewsRequested = true;
        }
      }
      if (viewsRequested) {
        ImmutableMap.Builder<String,FilteringView> builder = ImmutableMap.builder();
        for (Map.Entry<String,View> entry : spec.getViews().entrySet()) {
          builder.put(entry.getKey(), new FilteringView(entry.getValue(), fields));
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

    public Map<String,UserPref> getUserPrefs() {
      return fields.contains("userPrefs") ? spec.getUserPrefs() : null;
    }

    public Map<String, FilteringView> getViews() {
      return views;
    }
  }
}