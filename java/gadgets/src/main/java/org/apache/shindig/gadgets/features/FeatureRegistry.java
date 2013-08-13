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
package org.apache.shindig.gadgets.features;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Mechanism for loading feature.xml files from a location keyed by a String.
 * That String might be the location of a text file which in turn contains
 * other feature file locations; a directory; or a feature.xml file itself.
 */
@Singleton
public class FeatureRegistry {
  public static final String CACHE_NAME = "FeatureJsCache";
  public static final String RESOURCE_SCHEME = "res";
  public static final String FILE_SCHEME = "file";
  public static final Splitter CRLF_SPLITTER = Splitter.onPattern("[\r\n]+").trimResults().omitEmptyStrings();

  //class name for logging purpose
  private static final String classname = FeatureRegistry.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

  // Map keyed by FeatureNode object created as a lookup for transitive feature deps.
  private final Cache<String, LookupResult> cache;

  private final FeatureParser parser;
  private final FeatureResourceLoader resourceLoader;
  private final ImmutableMap<String, FeatureNode> featureMap;
  private final FeatureFileSystem fileSystem;
  private final String repository;


/**
 * Construct a new FeatureRegistry, using resourceLoader to load actual resources,
 * and loading data from the list of features provided, each of which points to a
 * directory (whose .xml files will be scanned), file (interpreted as feature.xml),
 * or resource (interpreted as feature.xml).
 * @param resourceLoader
 * @param features
 * @throws GadgetException
 */
  @Inject
  public FeatureRegistry(FeatureResourceLoader resourceLoader,
                         CacheProvider cacheProvider,
                         @Named("org.apache.shindig.features") List<String> features,
                         FeatureFileSystem fileSystem) throws GadgetException {
    this(resourceLoader, cacheProvider, features, fileSystem, null);
  }

  public FeatureRegistry(FeatureResourceLoader resourceLoader,
      CacheProvider cacheProvider,
      @Named("org.apache.shindig.features") List<String> features,
      FeatureFileSystem fileSystem, String repository) throws GadgetException {

    this.parser = new FeatureParser();
    this.resourceLoader = resourceLoader;
    this.fileSystem = fileSystem;
    this.repository = repository;

    this.featureMap = register(features);

    // Connect the dependency graph made up of all features and validate there
    // are no circular deps.
    connectDependencyGraph();

    this.cache = cacheProvider.createCache(CACHE_NAME);
  }

  public String getRepository() {
    return repository;
  }

  /**
   * Reads and registers all of the features in the directory, or the file, specified by
   * the given resourceKey. Invalid features or invalid paths will yield a
   * GadgetException.
   *
   * All features registered by this method must be valid (well-formed XML, resource
   * references all return successfully), and each "batch" of registered features
   * must be able to be assimilated into the current features tree in a valid fashion.
   * That is, their dependencies must all be valid features as well, and the
   * dependency tree must not contain circular dependencies.
   *
   * @param resourceList The files or directories to load the feature from. If feature.xml
   *    is passed in directly, it will be loaded as a single feature. If a
   *    directory is passed, any features in that directory (recursively) will
   *    be loaded. If res://*.txt or res:*.txt is passed, we will look for named resources
   *    in the text file. If path is prefixed with res:// or res:, the file
   *    is treated as a resource, and all references are assumed to be
   *    resources as well. Multiple locations may be specified by separating
   *    them with a comma.
   * @throws GadgetException If any of the files can't be read, are malformed, or invalid.
   */
  protected ImmutableMap<String, FeatureNode> register(List<String> resourceList)
      throws GadgetException {
    Map<String,FeatureNode> featureMapBuilder = Maps.newHashMap();

    try {
      for (String location : resourceList) {
        Uri uriLoc = getComponentUri(location);

        if (uriLoc.getScheme() != null && uriLoc.getScheme().equals(RESOURCE_SCHEME)) {
          List<String> resources = Lists.newArrayList();

          // Load as resource using ResourceLoader.
          location = uriLoc.getPath();
          if (location.startsWith("/")) {
            // Accommodate res:// URIs.
            location = location.substring(1);
          }
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "register",
                MessageKeys.LOAD_RESOURCES_FROM, new Object[] {uriLoc.toString()});
          }
          if (location.endsWith(".txt")) {
            // Text file contains a list of other resource files to load
            for (String resource : CRLF_SPLITTER.split(resourceLoader.getResourceContent(location))) {
              if (resource.charAt(0) != '#') {
                // Skip commented lines.
                resource = getComponentUri(resource).getPath();
                resources.add(resource);
              }
            }
          } else {
            resources.add(location);
          }

          loadResources(resources, featureMapBuilder);
        } else {
          // Load files in directory structure.
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "register",
                MessageKeys.LOAD_FILES_FROM, new Object[] {location});
          }
          loadFile(fileSystem.getFile(uriLoc.getPath()), featureMapBuilder);
        }
      }
      return ImmutableMap.copyOf(featureMapBuilder);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    }
  }

  /**
   * For the given list of needed features, retrieves all the FeatureResource objects that
   * contain their content and, if requested, that of their transitive dependencies.
   *
   * Resources are returned in order of their place in the dependency tree, with "bottom"/
   * depended-on resources returned before those that depend on them. Resource objects
   * within a given feature are returned in the order specified in their corresponding
   * feature.xml file. In the case of a dependency tree "tie" eg. A depends on [B, C], B and C
   * depend on D - resources are returned in the dependency order specified in feature.xml.
   *
   * Fills the "unsupported" list, if provided, with unknown features in the needed list.
   *
   * @param ctx Context for the request.
   * @param needed List of all needed features.
   * @param unsupported If non-null, a List populated with unknown features from the needed list.
   * @return LookupResult object that may be used to render the needed features.
   */
  public LookupResult getFeatureResources(
      GadgetContext ctx, Collection<String> needed, List<String> unsupported, boolean transitive) {
    boolean useCache = (transitive && !ctx.getIgnoreCache());
    String cacheKey = makeCacheKey(needed, ctx, unsupported);

    if (useCache) {
      LookupResult lookup = cache.getElement(cacheKey);
      if (lookup != null) {
        return lookup;
      }
    }

    List<FeatureNode> featureNodes = transitive ?
        getTransitiveDeps(needed, unsupported) : getRequestedNodes(needed, unsupported);

    ImmutableList.Builder<FeatureBundle> bundlesBuilder =
        new ImmutableList.Builder<FeatureBundle>();

    for (FeatureNode entry : featureNodes) {
      boolean specificContainerMatched = false;
      List<FeatureBundle> noContainerBundles = Lists.newLinkedList();
      for (FeatureBundle bundle : entry.getBundles(ctx.getRenderingContext())) {
        String containerAttrib = bundle.getAttribs().get("container");
        if (containerAttrib != null) {
          if (containerMatch(containerAttrib, ctx.getContainer())) {
            bundlesBuilder.add(bundle);
            specificContainerMatched = true;
          }
        } else {
          // Applies only if there were no specific container matches.
          noContainerBundles.add(bundle);
        }
      }
      if (!specificContainerMatched) {
        for (FeatureBundle bundle : noContainerBundles) {
          bundlesBuilder.add(bundle);
        }
      }
    }

    LookupResult result = new LookupResult(bundlesBuilder.build());
    if (useCache && (unsupported == null || unsupported.isEmpty())) {
      cache.addElement(cacheKey, result);
    }

    return result;
  }

  /**
   * Helper method retrieving feature resources, including transitive dependencies.
   * @param ctx Context for the request.
   * @param needed List of all needed features.
   * @param unsupported If non-null, a List populated with unknown features from the needed list.
   * @return List of FeatureResources that may be used to render the needed features.
   */
  public LookupResult getFeatureResources(GadgetContext ctx, Collection<String> needed,
      List<String> unsupported) {
    return getFeatureResources(ctx, needed, unsupported, true);
  }

  /**
   * Returns all known FeatureResources in dependency order, as described in getFeatureResources.
   * Returns only GADGET-context resources. This is a convenience method largely for calculating
   * JS checksum.
   * @return List of all known (RenderingContext.GADGET) FeatureResources.
   */
  public LookupResult getAllFeatures() {
    return getFeatureResources(new GadgetContext(), featureMap.keySet(), null);
  }

  /**
   * Calculates and returns a dependency-ordered (as in getFeatureResources) list of features
   * included directly or transitively from the specified list of needed features.
   * This API ignores any unknown features among the needed list.
   * @param needed List of features for which to obtain an ordered dep list.
   * @return Ordered list of feature names, as described.
   */
  public List<String> getFeatures(Collection<String> needed) {
    List<FeatureNode> fullTree = getTransitiveDeps(needed, Lists.<String>newLinkedList());
    List<String> allFeatures = Lists.newLinkedList();
    for (FeatureNode node : fullTree) {
      allFeatures.add(node.name);
    }
    return allFeatures;
  }

  /**
   * Helper method, returns all known feature names.
   * @return All known feature names.
   */
  public Set<String> getAllFeatureNames() {
    return featureMap.keySet();
  }

  // Provided for backward compatibility with existing feature loader configurations.
  // res://-prefixed URIs are actually scheme = res, host = "", path = "/stuff". We want res:path.
  // Package-private for use by FeatureParser as well.
  static Uri getComponentUri(String str) {
    return (str.startsWith("res://")) ?
      new UriBuilder().setScheme(RESOURCE_SCHEME).setPath(str.substring(6)).toUri() :
      Uri.parse(str);
  }

  private List<FeatureNode> getTransitiveDeps(
      Collection<String> needed, List<String> unsupported) {
    final List<FeatureNode> requested = getRequestedNodes(needed, unsupported);

    Comparator<FeatureNode> nodeDepthComparator = new Comparator<FeatureNode>() {
      public int compare(FeatureNode one, FeatureNode two) {
        if (one.nodeDepth > two.nodeDepth ||
            (one.nodeDepth == two.nodeDepth &&
             requested.indexOf(one) < requested.indexOf(two))) {
          return -1;
        }
        return 1;
      }
    };
    // Before getTransitiveDeps() is called, all nodes and their graphs have been validated
    // to have no circular dependencies, with their tree depth calculated. The requested
    // features here may overlap in the tree, so we need to be sure not to double-include
    // deps. Consider case where feature A depends on B and C, which both depend on D.
    // If the requested features list is [A, C], we want to include A's tree in the appropriate
    // order, and avoid double-including C (and its dependency D). Thus we sort by node depth
    // first - A's tree is deeper than that of C, so *if* A's tree contains C, traversing
    // it first guarantees that C is eventually included.
    Collections.sort(requested, nodeDepthComparator);

    Set<String> alreadySeen = Sets.newHashSet();
    List<FeatureNode> fullDeps = Lists.newLinkedList();
    for (FeatureNode requestedFeature : requested) {
      for (FeatureNode toAdd : requestedFeature.getTransitiveDeps()) {
        if (!alreadySeen.contains(toAdd.name)) {
          alreadySeen.add(toAdd.name);
          fullDeps.add(toAdd);
        }
      }
    }

    return fullDeps;
  }

  private List<FeatureNode> getRequestedNodes(
      Collection<String> needed, List<String> unsupported) {
    List<FeatureNode> requested = Lists.newArrayList();
    for (String featureName : needed) {
      if (featureMap.containsKey(featureName)) {
        requested.add(featureMap.get(featureName));
      } else {
        if (unsupported != null) unsupported.add(featureName);
      }
    }
    return requested;
  }

  private boolean containerMatch(String containerAttrib, String container) {
    for (String attr : Splitter.on(',').trimResults().split(containerAttrib)) {
      if (attr.equals(container)) return true;
    }
    return false;
  }

  private void connectDependencyGraph() throws GadgetException {
    // Iterate through each raw dependency, adding the corresponding feature to the graph.
    // Collect as many feature dep tree errors as possible before erroring out.
    List<String> problems = Lists.newLinkedList();
    List<FeatureNode> theFeatures = Lists.newLinkedList();

    // First hook up all first-order dependencies.
    for (Map.Entry<String, FeatureNode> featureEntry : featureMap.entrySet()) {
      String name = featureEntry.getKey();
      FeatureNode feature = featureEntry.getValue();

      for (String rawDep : feature.getRawDeps()) {
        if (!featureMap.containsKey(rawDep)) {
          problems.add("Feature [" + name + "] has dependency on unknown feature: " + rawDep);
        } else {
          feature.addDep(featureMap.get(rawDep));
          theFeatures.add(feature);
        }
      }
    }

    // Then hook up the transitive dependency graph to validate there are
    // no loops present.
    for (FeatureNode feature : theFeatures) {
      try {
        // Validates the dependency tree ensuring no circular dependencies,
        // and calculates the depth of the dependency tree rooted at the node.
        feature.completeNodeGraph();
      } catch (GadgetException e) {
        problems.add(e.getMessage());
      }
    }

    if (!problems.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Problems found processing features:\n");
      Joiner.on('\n').appendTo(sb, problems);
      throw new GadgetException(GadgetException.Code.INVALID_CONFIG, sb.toString());
    }
  }

  private void loadResources(List<String> resources, Map<String,FeatureNode> featureMapBuilder)
      throws GadgetException {
    try {
      for (String resource : resources) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Processing resource: " + resource);
        }

        String content = resourceLoader.getResourceContent(resource);
        Uri parent = new UriBuilder().setScheme(RESOURCE_SCHEME).setPath(resource).toUri();
        loadFeature(parent, content, featureMapBuilder);
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    }
  }

  private void loadFile(FeatureFile file, Map<String,FeatureNode> featureMapBuilder)
      throws GadgetException, IOException {
    if (!file.exists() || !file.canRead()) {
      throw new GadgetException(GadgetException.Code.INVALID_CONFIG,
          "Feature file '" + file.getPath() + "' doesn't exist or can't be read");
    }

    FeatureFile[] toLoad = file.isDirectory() ? file.listFiles() : new FeatureFile[] { file };

    for (FeatureFile featureFile : toLoad) {
      if (featureFile.isDirectory()) {
        // Traverse into subdirectories.
        loadFile(featureFile, featureMapBuilder);
      } else if (featureFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".xml")) {
        String content = featureFile.getContent();
        Uri parent = Uri.fromJavaUri(featureFile.toURI());
        loadFeature(parent, content, featureMapBuilder);
      } else {
        if (LOG.isLoggable(Level.FINEST)) {
          LOG.finest(featureFile.getAbsolutePath() + " doesn't seem to be an XML file.");
        }
      }
    }
  }

  /**
   * Method that loads gadget features.
   *
   * @param parent uri of parent
   * @param xml xml to parse
   * @throws GadgetException
   */
  protected void loadFeature(Uri parent, String xml, Map<String,FeatureNode> featureMapBuilder)
      throws GadgetException {
    FeatureParser.ParsedFeature parsed = parser.parse(parent, xml);
    // Duplicate feature = OK, just indicate it's being overridden.
    if (featureMapBuilder.containsKey(parsed.getName())) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.logp(Level.WARNING, classname, "doFilter", MessageKeys.OVERRIDING_FEATURE,
            new Object[] {parsed.getName(),parent});
      }
    }

    // Walk through all parsed bundles, pulling resources and creating FeatureBundles/Nodes.
    List<FeatureBundle> bundles = Lists.newArrayList();
    for (FeatureParser.ParsedFeature.Bundle parsedBundle : parsed.getBundles()) {
      List<FeatureResource> resources = Lists.newArrayList();
      for (FeatureParser.ParsedFeature.Resource parsedResource : parsedBundle.getResources()) {
        if (parsedResource.getSource() == null) {

          resources.add(new InlineFeatureResource(parsed.getName() + ":inline.js",
              parsedResource.getContent(), parsedResource.getAttribs()));
        } else {
          // Load using resourceLoader
          resources.add(resourceLoader.load(parsedResource.getSource(),
              getResourceAttribs(parsedBundle.getAttribs(), parsedResource.getAttribs())));
        }
      }
      bundles.add(new FeatureBundle(parsedBundle, resources));
    }

    // Add feature to the master Map. The dependency tree isn't connected/validated/linked yet.
    featureMapBuilder.put(parsed.getName(),
        new FeatureNode(parsed.getName(), bundles, parsed.getDeps()));
  }

  protected String makeCacheKey(Collection<String> needed, GadgetContext ctx, List<String> unsupported) {
    List<String> neededList = Lists.newArrayList(needed);
    Collections.sort(neededList);
    return new StringBuilder().append(StringUtils.join(neededList, ":"))
        .append('|').append(ctx.getRenderingContext())
        .append('|').append(ctx.getContainer())
        .append('|').append(unsupported != null)
        .append('|').append(Strings.nullToEmpty(repository))
        .toString();
  }

  private Map<String, String> getResourceAttribs(Map<String, String> bundleAttribs,
      Map<String, String> resourceAttribs) {
    // For a given resource, attribs are a merge (by key, not by value) of bundle attribs and
    // per-resource attribs, the latter serving as higher-precedence overrides.
    return ImmutableMap.<String, String>builder()
        .putAll(bundleAttribs)
        .putAll(resourceAttribs).build();
  }

  private static final class InlineFeatureResource extends FeatureResource.Attribute {
    private final String name;
    private final String content;

    private InlineFeatureResource(String name, String content, Map<String, String> attribs) {
      super(attribs);
      this.content = content;
      this.name = name;
    }

    public String getContent() {
      return content;
    }

    public String getDebugContent() {
      return content;
    }

    public String getName() {
      return name;
    }
  }

  public static class LookupResult {
    private final List<FeatureBundle> bundles;
    private final List<FeatureResource> allResources;

    public LookupResult(List<FeatureBundle> bundles) {
      this.bundles = bundles;
      ImmutableList.Builder<FeatureResource> resourcesBuilder = ImmutableList.builder();
      for (FeatureBundle bundle : getBundles()) {
        resourcesBuilder.addAll(bundle.getResources());
      }
      this.allResources = resourcesBuilder.build();
    }

    public List<FeatureBundle> getBundles() {
      return bundles;
    }

    public List<FeatureResource> getResources() {
      return allResources;
    }
  }

  public static class FeatureBundle {
    private final FeatureParser.ParsedFeature.Bundle bundle;
    private final List<FeatureResource> resources;

    public FeatureBundle(FeatureParser.ParsedFeature.Bundle bundle,
                         List<FeatureResource> resources) {
      this.bundle = bundle;
      this.resources = ImmutableList.copyOf(resources);
    }

    public String getName() {
      return bundle.getName();
    }

    public String getType() {
      return bundle.getType();
    }

    public Map<String, String> getAttribs() {
      return bundle.getAttribs();
    }

    public List<FeatureResource> getResources() {
      return resources;
    }

    public List<ApiDirective> getApis() {
      return bundle.getApis();
    }

    public boolean isSupportDefer() {
      return bundle.isSupportDefer();
    }

    public List<String> getApis(ApiDirective.Type type, boolean isExports) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      for (ApiDirective api : bundle.getApis()) {
        if (api.getType() == type && api.isExports() == isExports) {
          builder.add(api.getValue());
        }
      }
      return builder.build();
    }
  }

  private static final class FeatureNode {
    private final String name;
    private final List<FeatureBundle> bundles;
    private final List<String> requestedDeps;
    private final List<FeatureNode> depList;
    private List<FeatureNode> transitiveDeps;
    private boolean calculatedDepsStale;
    private int nodeDepth = 0;

    private FeatureNode(String name, List<FeatureBundle> bundles, List<String> rawDeps) {
      this.name = name;
      this.bundles = ImmutableList.copyOf(bundles);
      this.requestedDeps = ImmutableList.copyOf(rawDeps);
      this.depList = Lists.newLinkedList();
      this.transitiveDeps = Lists.newArrayList(this);
      this.calculatedDepsStale = false;
    }

    /**
     * Returns all bundles matching the given rendering context.
     * If there's >= 1 bundle matching a non-ALL context, return it.
     * Otherwise, return any ALL bundles.
     * @param rctx
     */
    public Iterable<FeatureBundle> getBundles(RenderingContext rctx) {
      String tagMatch = null;
      String directTag = rctx.getFeatureBundleTag();
      for (FeatureBundle bundle : bundles) {
        if (directTag != null && directTag.equalsIgnoreCase(bundle.getType())) {
          tagMatch = directTag;
        }
      }
      if (tagMatch == null) {
        tagMatch = RenderingContext.ALL.getFeatureBundleTag();
      }
      final String useForMatching = tagMatch;

      // Return an Iterator rather than coping a new
      // list containing the types over which to iterate.
      return new Iterable<FeatureBundle>() {
        public Iterator<FeatureBundle> iterator() {
          return new Iterator<FeatureBundle>() {
            private FeatureBundle next;
            private final Iterator<FeatureBundle> it = bundles.iterator();

            public boolean hasNext() {
              while (next == null && it.hasNext()) {
                FeatureBundle candidate = it.next();
                if (useForMatching.equalsIgnoreCase(candidate.getType())) {
                  next = candidate;
                }
              }
              return next != null;
            }

            public FeatureBundle next() {
              hasNext();
              FeatureBundle ret = next;
              next = null;
              return ret;
            }

            public void remove() {
              throw new UnsupportedOperationException("Can't remove from bundle iterator");
            }
          };
        }
      };
    }

    public List<String> getRawDeps() {
      return requestedDeps;
    }

    public void addDep(FeatureNode dep) {
      depList.add(dep);
      calculatedDepsStale = true;
    }

    private List<FeatureNode> getDepList() {
      List<FeatureNode> revOrderDeps = Lists.newArrayList(depList);
      Collections.reverse(revOrderDeps);
      return ImmutableList.copyOf(revOrderDeps);
    }

    public void completeNodeGraph() throws GadgetException {
      if (!calculatedDepsStale) {
        return;
      }

      // Detect feature dependency loop
      Set<String> tempList  = new HashSet<String>();
      this.checkDependencyLoop(this, tempList);

      this.nodeDepth = 0;
      this.transitiveDeps = Lists.newLinkedList();
      this.transitiveDeps.add(this);

      Queue<Pair<FeatureNode, Pair<Integer, String>>> toTraverse = Lists.newLinkedList();
      toTraverse.add(Pair.of(this, Pair.of(0, "")));

      while (!toTraverse.isEmpty()) {
        Pair<FeatureNode, Pair<Integer, String>> next = toTraverse.poll();
        String debug = next.two.two + (next.two.one > 0 ? " -> " : "") + next.one.name;
        // Breadth-first list of dependencies.
        this.transitiveDeps.add(next.one);
        this.nodeDepth = Math.max(this.nodeDepth, next.two.one);
        for (FeatureNode nextDep : next.one.getDepList()) {
          toTraverse.add(Pair.of(nextDep, Pair.of(next.two.one + 1, debug)));
        }
      }

      Collections.reverse(this.transitiveDeps);
      calculatedDepsStale = false;
    }

    public List<FeatureNode> getTransitiveDeps() {
      return this.transitiveDeps;
    }

    /**
     * Check whether current feature node has dependency loop
     * @param featureNode
     *     feature node which needs to check whether it has dependency loop.
     * @param dependencyList
     *     used to check whether current branch of feature dependency tree has dependency loop or not.
     * @throws GadgetException
     *     a new GadgetException will be thrown if a loop is detected.
     */
    private void checkDependencyLoop(FeatureNode featureNode, Set<String> dependencyList) throws GadgetException {
        String featureNodeName = featureNode.name;
        if (dependencyList.contains(featureNodeName)) {
            // If dependencyList already contains this feature node, then current branch has dependency loop.
            throw new GadgetException(GadgetException.Code.INVALID_CONFIG, "Feature " + featureNodeName + " has dependency loop problem.");
        }
        // Add the current dependency into branch.
        dependencyList.add(featureNodeName);
        List<FeatureNode> deps = featureNode.getDepList();
        for (FeatureNode f : deps) {
            checkDependencyLoop(f, dependencyList);
            // Remove the part which already has been verified.
            dependencyList.remove(f.name);
        }
    }
  }
}
