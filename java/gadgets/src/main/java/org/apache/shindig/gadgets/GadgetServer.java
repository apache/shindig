/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.Preload;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Primary gadget processing facility. Converts an input Context into an output
 * Gadget
 */
@Singleton
public class GadgetServer {
  private final ExecutorService executor;
  private final GadgetFeatureRegistry registry;
  private final GadgetBlacklist blacklist;
  private final ContainerConfig containerConfig;
  private final ContentRewriterRegistry rewriterRegistry;
  private final GadgetHtmlParser htmlParser;

  private ContentFetcherFactory preloadFetcherFactory;
  private GadgetSpecFactory specFactory;
  private MessageBundleFactory bundleFactory;

  @Inject
  public GadgetServer(ExecutorService executor,
      GadgetFeatureRegistry registry,
      GadgetBlacklist blacklist,
      ContainerConfig containerConfig,
      ContentRewriterRegistry rewriterRegistry,
      GadgetHtmlParser htmlParser,
      ContentFetcherFactory preloadFetcherFactory,
      GadgetSpecFactory specFactory,
      MessageBundleFactory bundleFactory) {
    this.executor = executor;
    this.registry = registry;
    this.blacklist = blacklist;
    this.containerConfig = containerConfig;
    this.rewriterRegistry = rewriterRegistry;
    this.htmlParser = htmlParser;
    this.preloadFetcherFactory = preloadFetcherFactory;
    this.specFactory = specFactory;
    this.bundleFactory = bundleFactory;
  }

  /**
   * Process a single gadget. Creates a gadget from a retrieved
   * GadgetSpec and context object. Performs rewriting, then message
   * bundle substitution and feature processing.
   *
   * @param context Gadget request context.
   * @return The processed gadget, ready for consumption.
   * @throws GadgetException
   */
  public Gadget processGadget(GadgetContext context) throws GadgetException {
    if (blacklist.isBlacklisted(context.getUrl())) {
      throw new GadgetException(GadgetException.Code.BLACKLISTED_GADGET);
    }
    // Retrieve the GadgetSpec for the given context.
    GadgetSpec spec = specFactory.getGadgetSpec(context);

    // Create substituted GadgetSpec object, including message bundle substitutions.
    MessageBundle bundle
        = bundleFactory.getBundle(spec, context.getLocale(), context.getIgnoreCache());
    String dir = bundle.getLanguageDirection();
    Substitutions substituter = new Substitutions();
    substituter.addSubstitutions(
        Substitutions.Type.MESSAGE, bundle.getMessages());
    BidiSubstituter.addSubstitutions(substituter, dir);
    substituter.addSubstitution(Substitutions.Type.MODULE, "ID",
        Integer.toString(context.getModuleId()));
    UserPrefSubstituter.addSubstitutions(
        substituter, spec, context.getUserPrefs());
    spec = spec.substitute(substituter);

    Collection<JsLibrary> jsLibraries = getLibraries(spec, context);
    Gadget gadget = new Gadget(context, spec, jsLibraries, containerConfig, htmlParser);

    // Perform rewriting operations on the Gadget.
    if (rewriterRegistry != null) {
      rewriterRegistry.rewriteGadget(gadget);
    }

    startPreloads(gadget);
    return gadget;
  }

  /**
   * Begins processing of preloaded data.
   *
   * Preloads are processed in parallel.
   */
  private void startPreloads(Gadget gadget) {
    RenderingContext renderContext = gadget.getContext().getRenderingContext();
    if (RenderingContext.GADGET.equals(renderContext)) {
      CompletionService<HttpResponse> preloadProcessor
          = new ExecutorCompletionService<HttpResponse>(executor);
      for (Preload preload : gadget.getSpec().getModulePrefs().getPreloads()) {
        // Cant execute signed/oauth preloads without the token
        if ((preload.getAuthType() == AuthType.NONE ||
            gadget.getContext().getToken() != null) &&
            (preload.getViews().isEmpty() ||
            preload.getViews().contains(gadget.getContext().getView()))) {
          PreloadTask task = new PreloadTask(gadget.getContext(), preload);
          Future<HttpResponse> future = preloadProcessor.submit(task);
          gadget.getPreloadMap().put(preload, future);
        }
      }
    }
  }

  /**
   * Constructs a set of dependencies from the given spec.
   */
  private Collection<JsLibrary> getLibraries(GadgetSpec spec,
      GadgetContext context) throws GadgetException {
    // Check all required features for the gadget.
    Map<String, Feature> features = spec.getModulePrefs().getFeatures();
    Set<String> needed = features.keySet();
    Set<String> unsupported = new HashSet<String>();
    Collection<GadgetFeature> feats = registry.getFeatures(needed, unsupported);

    if (!unsupported.isEmpty()) {
      // Remove non-required libs
      for (String missing : unsupported) {
        if (!features.get(missing).getRequired()) {
          unsupported.remove(missing);
        }
      }
      // Throw error with full list of unsupported libraries
      if (!unsupported.isEmpty()) {
        throw new UnsupportedFeatureException(unsupported.toString());
      }
    }

    Collection<JsLibrary> libraries = new LinkedList<JsLibrary>();
    for (GadgetFeature feature : feats) {
      libraries.addAll(feature.getJsLibraries(
          context.getRenderingContext(), context.getContainer()));
    }
    return libraries;
  }

  /**
   * Provides a task for preloading data into the gadget content
   * TODO: Remove when new preloading is committed.
   */
  private class PreloadTask implements Callable<HttpResponse> {
    private final Preload preload;
    private final GadgetContext context;

    public HttpResponse call() {
      try {
        HttpRequest request = new HttpRequest(Uri.fromJavaUri(preload.getHref()))
            .setSecurityToken(context.getToken())
            .setOAuthArguments(new OAuthArguments(preload))
            .setAuthType(preload.getAuthType())
            .setContainer(context.getContainer())
            .setGadget(Uri.fromJavaUri(context.getUrl()));
        return preloadFetcherFactory.fetch(request);
      } catch (GadgetException e) {
        return HttpResponse.error();
      }
    }

    public PreloadTask(GadgetContext context, Preload preload) {
      this.preload = preload;
      this.context = context;
    }
  }
}
