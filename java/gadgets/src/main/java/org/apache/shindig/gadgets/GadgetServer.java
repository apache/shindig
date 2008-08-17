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

import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.Auth;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

/**
 * Primary gadget processing facility. Converts an input Context into an output
 * Gadget
 */
@Singleton
public class GadgetServer {
  private final Executor executor;
  private final GadgetFeatureRegistry registry;
  private final GadgetBlacklist blacklist;

  private ContentFetcherFactory preloadFetcherFactory;
  private GadgetSpecFactory specFactory;
  private MessageBundleFactory bundleFactory;


  @Inject
  public GadgetServer(Executor executor,
      GadgetFeatureRegistry registry,
      GadgetBlacklist blacklist,
      ContentFetcherFactory preloadFetcherFactory,
      GadgetSpecFactory specFactory,
      MessageBundleFactory bundleFactory) {
    this.executor = executor;
    this.registry = registry;
    this.blacklist = blacklist;
    this.preloadFetcherFactory = preloadFetcherFactory;
    this.specFactory = specFactory;
    this.bundleFactory = bundleFactory;
  }

  /**
   * Process a single gadget.
   *
   * @param context
   * @return The processed gadget.
   * @throws GadgetException
   */
  public Gadget processGadget(GadgetContext context) throws GadgetException {
    if (blacklist.isBlacklisted(context.getUrl())) {
      throw new GadgetException(GadgetException.Code.BLACKLISTED_GADGET);
    }
    return createGadgetFromSpec(specFactory.getGadgetSpec(context), context);
  }

  /**
   *
   * @param localeSpec
   * @param context
   * @return A new message bundle
   * @throws GadgetException
   */
  private MessageBundle getBundle(LocaleSpec localeSpec, GadgetContext context)
      throws GadgetException {
    return bundleFactory.getBundle(localeSpec, context);
  }

  /**
   * Creates a Gadget from the specified gadget spec and context objects.
   * This performs message bundle substitution as well as feature processing.
   *
   * @param spec
   * @param context
   * @return The final Gadget, ready for consumption.
   * @throws GadgetException
   */
  private Gadget createGadgetFromSpec(GadgetSpec spec, GadgetContext context)
      throws GadgetException {
    LocaleSpec localeSpec
        = spec.getModulePrefs().getLocale(context.getLocale());
    MessageBundle bundle;
    String dir;
    if (localeSpec == null) {
      bundle = MessageBundle.EMPTY;
      dir = "ltr";
    } else {
      bundle = getBundle(localeSpec, context);
      dir = localeSpec.getLanguageDirection();
    }

    Substitutions substituter = new Substitutions();
    substituter.addSubstitutions(
        Substitutions.Type.MESSAGE, bundle.getMessages());
    BidiSubstituter.addSubstitutions(substituter, dir);
    substituter.addSubstitution(Substitutions.Type.MODULE, "ID",
        Integer.toString(context.getModuleId()));
    UserPrefSubstituter.addSubstitutions(
        substituter, spec, context.getUserPrefs());
    spec = spec.substitute(substituter, !context.getIgnoreCache());

    Collection<JsLibrary> jsLibraries = getLibraries(spec, context);
    Gadget gadget = new Gadget(context, spec, jsLibraries);
    startPreloads(gadget);
    return gadget;
  }

  /**
   * Begins processing of preloaded data.
   *
   * Preloads are processed in parallel.
   */
  private void startPreloads(Gadget gadget) throws GadgetException {
    RenderingContext renderContext = gadget.getContext().getRenderingContext();
    if (RenderingContext.GADGET.equals(renderContext)) {
      CompletionService<HttpResponse> preloadProcessor
          = new ExecutorCompletionService<HttpResponse>(executor);
      for (Preload preload : gadget.getSpec().getModulePrefs().getPreloads()) {
        // Cant execute signed/oauth preloads without the token
        if ((preload.getAuth() == Auth.NONE ||
            gadget.getContext().getToken() != null) &&
            (preload.getViews().isEmpty() ||
            preload.getViews().contains(gadget.getContext().getView()))) {
          PreloadTask task = new PreloadTask(gadget.getContext(), preload,
              preloadFetcherFactory);
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
      for (String missing : unsupported) {
        if (features.get(missing).getRequired()) {
          throw new UnsupportedFeatureException(missing);
        }
      }
    }
    Collection<JsLibrary> libraries = new LinkedList<JsLibrary>();
    for (GadgetFeature feature : feats) {
      libraries.addAll(feature.getJsLibraries(
          context.getRenderingContext(), context.getContainer()));
    }
    return libraries;
  }
}

/**
 * Provides a task for preloading data into the gadget content
 */
class PreloadTask implements Callable<HttpResponse> {
  private final Preload preload;
  private final ContentFetcherFactory preloadFetcherFactory;
  private final GadgetContext context;

  public HttpResponse call() {
    HttpRequest request = new HttpRequest(preload.getHref());
    request.getOptions().ownerSigned = preload.isSignOwner();
    request.getOptions().viewerSigned = preload.isSignViewer();
    try {
      switch (preload.getAuth()) {
        case NONE:
          return preloadFetcherFactory.get().fetch(request);
        case SIGNED:
          return preloadFetcherFactory.getSigningFetcher(context.getToken())
              .fetch(request);
        case OAUTH:
          return preloadFetcherFactory.getOAuthFetcher(context.getToken(),
              new OAuthArguments(preload)).fetch(request);
        default:
          return HttpResponse.error();
      }
    } catch (GadgetException e) {
      return HttpResponse.error();
    }
  }

  public PreloadTask(GadgetContext context, Preload preload,
      ContentFetcherFactory preloadFetcherFactory) {
    this.preload = preload;
    this.preloadFetcherFactory = preloadFetcherFactory;
    this.context = context;
  }
}