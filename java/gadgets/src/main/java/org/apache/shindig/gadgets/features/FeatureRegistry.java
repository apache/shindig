/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.features;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.shindig.common.Pair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
  public static final char FILE_SEPARATOR = ',';
  public static final String RESOURCE_SCHEME = "res";
  public static final String FILE_SCHEME = "file";
  
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");
  
  // Map keyed by FeatureNode object created as a lookup for transitive feature deps.
  private final Map<Collection<String>, List<FeatureResource>> cache = new MapMaker().makeMap();
  private final Map<Collection<String>, List<FeatureResource>> cacheIgnoreUnsupported =
      new MapMaker().makeMap();

  private final FeatureParser parser;
  private final FeatureResourceLoader resourceLoader;
  private final Map<String, FeatureNode> featureMap;
  
  @Inject
  public FeatureRegistry(FeatureResourceLoader resourceLoader) {
    this.parser = new FeatureParser();
    this.resourceLoader = resourceLoader;
    this.featureMap = Maps.newHashMap();
  }
  
  /**
   * For compatibility with GadgetFeatureRegistry, and to provide a programmatic hook
   * for adding feature files by config, @Inject the @Named featureFiles variable.
   * @param featureFiles
   * @throws GadgetException
   */
  @Inject(optional = true)
  public void addDefaultFeatures(
      @Named("shindig.features.default") String featureFiles) throws GadgetException {
    register(featureFiles);
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
   * @param resourceKey The file or directory to load the feature from. If feature.xml
   *    is passed in directly, it will be loaded as a single feature. If a
   *    directory is passed, any features in that directory (recursively) will
   *    be loaded. If res://*.txt or res:*.txt is passed, we will look for named resources
   *    in the text file. If path is prefixed with res:// or res:, the file
   *    is treated as a resource, and all references are assumed to be
   *    resources as well. Multiple locations may be specified by separating
   *    them with a comma.
   * @throws GadgetException If any of the files can't be read, are malformed, or invalid.
   */
  public void register(String resourceKey) throws GadgetException {
    try {
      for (String location : StringUtils.split(resourceKey, FILE_SEPARATOR)) {
        Uri uriLoc = getComponentUri(location);
        
        if (uriLoc.getScheme() != null && uriLoc.getScheme().equals(RESOURCE_SCHEME)) {
          List<String> resources = Lists.newArrayList();
          
          // Load as resource using ResourceLoader.
          location = uriLoc.getPath();
          if (location.startsWith("/")) {
            // Accommodate res:// URIs.
            location = location.substring(1);
          }
          logger.info("Loading resources from: " + uriLoc.toString());
          
          if (location.endsWith(".txt")) {
            // Text file contains a list of other resource files to load
            for (String resource : getResourceContent(location).split("[\r\n]+")) {
              resource = resource.trim();
              if (resource.length () > 0 && resource.charAt(0) != '#') {
                // Skip blank/commented lines.
                resource = getComponentUri(resource.trim()).getPath();
                resources.add(resource);
              }
            }
          } else {
            resources.add(location);
          }
          
          loadResources(resources);
        } else {
          // Load files in directory structure.
          logger.info("Loading files from: " + location);
          
          loadFile(uriLoc.getPath());
        }
      }
      
      // Connect the dependency graph made up of all features and validate there
      // are no circular deps.
      connectDependencyGraph();
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
   * @return List of FeatureResources that may be used to render the needed features.
   * @throws GadgetException
   */
  public List<FeatureResource> getFeatureResources(
      GadgetContext ctx, Collection<String> needed, List<String> unsupported, boolean transitive) {
    Map<Collection<String>, List<FeatureResource>> useCache = null;
    if (transitive) {
      useCache = (unsupported != null) ? cache : cacheIgnoreUnsupported;
    }
    
    List<FeatureResource> resources = Lists.newLinkedList();
    
    if (useCache != null && useCache.containsKey(needed)) {
      return useCache.get(needed);
    }
    
    List<FeatureNode> featureNodes = null;
    if (transitive) {
      featureNodes = getTransitiveDeps(needed, unsupported);
    } else {
      featureNodes = getRequestedNodes(needed, unsupported);
    }

    String targetBundleType =
        ctx.getRenderingContext() == RenderingContext.CONTAINER ? "container" : "gadget";
    
    for (FeatureNode entry : featureNodes) {
      for (FeatureBundle bundle : entry.getBundles()) {
        if (bundle.getType().equals(targetBundleType)) {
          if (containerMatch(bundle.getAttribs().get("container"), ctx.getContainer())) {
            resources.addAll(bundle.getResources());
          }
        }
      }
    }
    
    if (useCache != null && (unsupported == null || unsupported.isEmpty())) {
      useCache.put(needed, resources);
    }
      
    return resources;
  }
  
  /**
   * Helper method retrieving feature resources, including transitive dependencies.
   * @param ctx Context for the request.
   * @param needed List of all needed features.
   * @param unsupported If non-null, a List populated with unknown features from the needed list.
   * @return List of FeatureResources that may be used to render the needed features.
   */
  public List<FeatureResource> getFeatureResources(
      GadgetContext ctx, Collection<String> needed, List<String> unsupported) {
    return getFeatureResources(ctx, needed, unsupported, true);
  }
  
  /**
   * Returns all known FeatureResources in dependency order, as described in getFeatureResources.
   * Returns only GADGET-context resources. This is a convenience method largely for calculating
   * JS checksum.
   * @return List of all known (RenderingContext.GADGET) FeatureResources.
   */
  public List<FeatureResource> getAllFeatures() {
    return getFeatureResources(
        new GadgetContext(), Lists.newArrayList(featureMap.keySet()), null);
  }
  
  /**
   * Calculates and returns a dependency-ordered (as in getFeatureResources) list of features
   * included directly or transitively from the specified list of needed features.
   * This API ignores any unknown features among the needed list.
   * @param needed List of features for which to obtain an ordered dep list.
   * @return Ordered list of feature names, as described.
   */
  public List<String> getFeatures(List<String> needed) {
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
  public List<String> getAllFeatureNames() {
    return Lists.newArrayList(featureMap.keySet());
  }
  
  // Visible for testing.
  String getResourceContent(String resource) throws IOException {
    return ResourceLoader.getContent(resource);
  }
  
  // Provided for backward compatibility with existing feature loader configurations.
  // res://-prefixed URIs are actually scheme = res, host = "", path = "/stuff". We want res:path.
  // Package-private for use by FeatureParser as well.
  static Uri getComponentUri(String str) {
    Uri uri = null;
    if (str.startsWith("res://")) {
      uri = new UriBuilder().setScheme(RESOURCE_SCHEME).setPath(str.substring(6)).toUri();
    } else {
      uri = Uri.parse(str);
    }
    return uri;
  }
  
  private List<FeatureNode> getTransitiveDeps(Collection<String> needed, List<String> unsupported) {
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
  
  private List<FeatureNode> getRequestedNodes(Collection<String> needed, List<String> unsupported) {
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
    if (containerAttrib == null || containerAttrib.length() == 0) {
      // Nothing specified = all match.
      return true;
    }
    Set<String> containers = Sets.newHashSet();
    for (String attr : containerAttrib.split(",")) {
      containers.add(attr.trim());
    }
    return containers.contains(container);
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
    
    if (problems.size() > 0) {
      StringBuilder sb = new StringBuilder();
      sb.append("Problems found processing features:\n");
      for (String problem : problems) {
        sb.append(problem).append("\n");
      }
      throw new GadgetException(GadgetException.Code.INVALID_CONFIG, sb.toString());
    }
  }
  
  private void loadResources(List<String> resources) throws GadgetException {
    try {
      for (String resource : resources) {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("Processing resource: " + resource);
        }
        
        String content = getResourceContent(resource);
        Uri parent = new UriBuilder().setScheme(RESOURCE_SCHEME).setPath(resource).toUri();
        loadFeature(parent, content);
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    }
  }
  
  private void loadFile(String filePath) throws GadgetException, IOException {
    File file = new File(filePath);
    if (!file.exists() || !file.canRead()) {
      throw new GadgetException(GadgetException.Code.INVALID_CONFIG,
          "Feature file '" + filePath + "' doesn't exist or can't be read");
    }
    
    File[] toLoad = null;
    if (file.isDirectory()) {
      toLoad = file.listFiles();
    } else {
      toLoad = new File[] { file };
    }
    
    for (File featureFile : toLoad) {
      String featureFilePath = featureFile.getAbsolutePath();
      if (featureFile.isDirectory()) {
        // Traverse into subdirectories.
        loadFile(featureFilePath);
      } else if (featureFilePath.toLowerCase(Locale.ENGLISH).endsWith(".xml")) {
        String content = ResourceLoader.getContent(featureFile);
        Uri parent = new UriBuilder().setScheme("file").setPath(featureFilePath).toUri();
        loadFeature(parent, content);
      } else {
        if (logger.isLoggable(Level.FINEST)) {
          logger.finest(featureFile.getAbsolutePath() + " doesn't seem to be an XML file.");
        }
      }
    }
  }
  
  protected void loadFeature(Uri parent, String xml) throws GadgetException {
    FeatureParser.ParsedFeature parsed = parser.parse(parent, xml);
    
    // Duplicate feature = OK, just indicate it's being overridden.
    if (featureMap.containsKey(parsed.getName())) {
      if (logger.isLoggable(Level.WARNING)) {
        logger.warning("Overriding feature: " + parsed.getName() + " with def at: " + parent);
      }
    }
    
    // Walk through all parsed bundles, pulling resources and creating FeatureBundles/Nodes.
    List<FeatureBundle> bundles = Lists.newArrayList();
    for (FeatureParser.ParsedFeature.Bundle parsedBundle : parsed.getBundles()) {
      List<FeatureResource> resources = Lists.newArrayList();
      for (FeatureParser.ParsedFeature.Resource parsedResource : parsedBundle.getResources()) {
        if (parsedResource.getSource() == null) {
          resources.add(new InlineFeatureResource(parsedResource.getContent()));
        } else {
          // Load using resourceLoader
          resources.add(
              resourceLoader.load(parsedResource.getSource(), parsedResource.getAttribs()));
        }
      }
      bundles.add(new FeatureBundle(parsedBundle.getType(), parsedBundle.getAttribs(), resources));
    }
    
    // Add feature to the master Map. The dependency tree isn't connected/validated/linked yet.
    featureMap.put(parsed.getName(), new FeatureNode(parsed.getName(), bundles, parsed.getDeps()));
  }
  
  private static class InlineFeatureResource extends FeatureResource.Default {
    private final String content;
    
    private InlineFeatureResource(String content) {
      this.content = content;
    }
    
    public String getContent() {
      return content;
    }

    public String getDebugContent() {
      return content;
    }
  }

  private static class FeatureBundle {
    private final String type;
    private final Map<String, String> attribs;
    private final List<FeatureResource> resources;
    
    private FeatureBundle(String type, Map<String, String> attribs,
        List<FeatureResource> resources) {
      this.type = type;
      this.attribs = Collections.unmodifiableMap(attribs);
      this.resources = Collections.unmodifiableList(resources);
    }
    
    public String getType() {
      return type;
    }
    
    public Map<String, String> getAttribs() {
      return attribs;
    }
    
    public List<FeatureResource> getResources() {
      return resources;
    }
  }
  
  private static class FeatureNode {
    private final String name;
    private final List<FeatureBundle> bundles;
    private final List<String> requestedDeps;
    private final List<FeatureNode> depList;
    private List<FeatureNode> transitiveDeps;
    private boolean calculatedDepsStale;
    private int nodeDepth = 0;
    
    private FeatureNode(String name, List<FeatureBundle> bundles, List<String> rawDeps) {
      this.name = name;
      this.bundles = Collections.unmodifiableList(bundles);
      this.requestedDeps = Collections.unmodifiableList(rawDeps);
      this.depList = Lists.newLinkedList();
      this.transitiveDeps = Lists.newArrayList(this);
      this.calculatedDepsStale = false;
    }
    
    public List<FeatureBundle> getBundles() {
      return bundles;
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
      return Collections.unmodifiableList(revOrderDeps);
    }
    
    public void completeNodeGraph() throws GadgetException {
      if (!calculatedDepsStale) {
        return;
      }
      
      this.nodeDepth = 0;
      this.transitiveDeps = Lists.newLinkedList();
      this.transitiveDeps.add(this);
      
      Queue<Pair<FeatureNode, Pair<Integer, String>>> toTraverse = Lists.newLinkedList();
      toTraverse.add(Pair.of(this, Pair.of(0, "")));
      
      while (!toTraverse.isEmpty()) {
        Pair<FeatureNode, Pair<Integer, String>> next = toTraverse.poll();
        String debug = next.two.two + (next.two.one > 0 ? " -> " : "") + next.one.name;
        if (next.one == this && next.two.one != 0) {
          throw new GadgetException(GadgetException.Code.INVALID_CONFIG,
              "Feature dep loop detected: " + debug);
        }
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
  }
}
