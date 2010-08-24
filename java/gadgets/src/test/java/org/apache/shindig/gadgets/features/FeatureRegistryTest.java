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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class FeatureRegistryTest {
  private static final String NODEP_TPL =
      getFeatureTpl("nodep", new String[] {});
  private static final String TOP_TPL =
      getFeatureTpl("top", new String[] { "mid_a", "mid_b" });
  private static final String MID_A_TPL =
      getFeatureTpl("mid_a", new String[] { "bottom" });
  private static final String MID_B_TPL = 
      getFeatureTpl("mid_b", new String[] { "bottom" });
  private static final String BOTTOM_TPL = 
      getFeatureTpl("bottom", new String[] {});
  private static final String LOOP_A_TPL = 
      getFeatureTpl("loop_a", new String[] { "loop_b" });
  private static final String LOOP_B_TPL =
      getFeatureTpl("loop_b", new String[] { "loop_c" });
  private static final String LOOP_C_TPL =
      getFeatureTpl("loop_c", new String[] { "loop_a" });
  private static final String BAD_DEP_TPL =
      getFeatureTpl("bad_dep", new String[] { "no-exists" });
  
  private static String RESOURCE_BASE_PATH = "/resource/base/path";
  private static int resourceIdx = 0;
  private FeatureResourceLoader resourceLoader;
  private ResourceMock resourceMock;
  private FeatureRegistry registry;
  private Map<String, String> lastAttribs;

  @Before
  public void setUp() {
    resourceMock = new ResourceMock();
    lastAttribs = null;
    resourceLoader = new FeatureResourceLoader() {
      public FeatureResource load(Uri uri, Map<String, String> attribs) throws GadgetException {
        lastAttribs = ImmutableMap.copyOf(attribs);
        return super.load(uri, attribs);
      }
      @Override
      protected String getResourceContent(String resource) {
        try {
          return resourceMock.get(resource);
        } catch (IOException e) {
          return null;
        }
      }
    };
  }

  private class TestFeatureRegistry extends FeatureRegistry {
    TestFeatureRegistry(String featureFiles) throws GadgetException {
      super(resourceLoader, ImmutableList.<String>of(featureFiles));
    }
    @Override
    String getResourceContent(String resource) throws IOException {
      return resourceMock.get(resource);
    }
  }
  @Test
  public void registerFromFileFeatureXmlFileScheme() throws Exception {
    checkRegisterFromFileFeatureXml(true);
  }
  
  @Test
  public void registerFromFileFeatureXmlNoScheme() throws Exception {
    checkRegisterFromFileFeatureXml(false);
  }
  
  private void checkRegisterFromFileFeatureXml(boolean withScheme) throws Exception {
    String content = "content-" + (withScheme ? "withScheme" : "noScheme");
    Uri resUri = makeFile(content);
    Uri featureFile = makeFile(xml(NODEP_TPL, "gadget",
        withScheme ? resUri.toString() : resUri.getPath(), null));
    registry = new TestFeatureRegistry(withScheme ? featureFile.toString() : featureFile.getPath());
    
    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures();
    assertEquals(1, resources.size());
    assertEquals(content, resources.get(0).getContent());
  }
  
  @Test
  public void registerFromFileInNestedDirectoryFeatureXmlFile() throws Exception {
    // Get the directory from dummyUri and create a subdir.
    File tmpFile = File.createTempFile("dummy", ".dat");
    tmpFile.deleteOnExit();
    File parentDir = tmpFile.getParentFile();
    String childDirName = String.valueOf(Math.random());
    File childDir = new File(parentDir, childDirName);
    childDir.mkdirs();
    childDir.deleteOnExit();
    File featureDir = new File(childDir, "thefeature");
    featureDir.mkdirs();
    featureDir.deleteOnExit();
    File resFile = File.createTempFile("content", ".js", featureDir);
    resFile.deleteOnExit();
    String content = "content-foo";
    BufferedWriter out = new BufferedWriter(new FileWriter(resFile));
    out.write(content);
    out.close();
    File featureFile = File.createTempFile("feature", ".xml", featureDir);
    featureFile.deleteOnExit();
    out = new BufferedWriter(new FileWriter(featureFile));
    out.write(xml(NODEP_TPL, "gadget", resFile.toURI().toString(), null));
    out.close();
    registry = new TestFeatureRegistry(childDir.toURI().toString());
    
    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures();
    assertEquals(1, resources.size());
    assertEquals(content, resources.get(0).getContent());
  }
  
  @Test
  public void registerFromResourceFeatureXml() throws Exception {
    String content = "resource-content()";
    Uri contentUri = expectResource(content);
    Uri featureUri = expectResource(xml(NODEP_TPL, "gadget", contentUri.getPath(), null));
    registry = new TestFeatureRegistry(featureUri.toString());
    
    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures();
    assertEquals(1, resources.size());
    assertEquals(content, resources.get(0).getContent());
  }
  
  @Test
  public void registerFromResourceFeatureXmlRelativeContent() throws Exception {
    String content = "resource-content-relative()";
    Uri contentUri = expectResource(content);
    String relativePath = contentUri.getPath().substring(contentUri.getPath().lastIndexOf('/') + 1);
    Uri featureUri = expectResource(xml(NODEP_TPL, "gadget", relativePath, null));
    registry = new TestFeatureRegistry(featureUri.toString());
    
    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures();
    assertEquals(1, resources.size());
    assertEquals(content, resources.get(0).getContent());
  }
  
  @Test
  public void registerFromResourceIndex() throws Exception {
    // One with extern resource loaded content...
    String content1 = "content1()";
    Uri content1Uri = expectResource(content1);
    Uri feature1Uri = expectResource(xml(MID_A_TPL, "gadget", content1Uri.getPath(), null));

    // One feature with inline content (that it depends on)...
    String content2 = "inline()";
    Uri feature2Uri = expectResource(xml(BOTTOM_TPL, "gadget", null, content2));
    
    // .txt file to join the two
    Uri txtFile = expectResource(feature1Uri.toString() + '\n' + feature2Uri.toString(), ".txt");
    
    // Load resources from the text file and do basic validation they're good.
    registry = new TestFeatureRegistry(txtFile.toString());
    
    // Contents should be ordered based on the way they went in.
    List<FeatureResource> resources = registry.getAllFeatures();
    assertEquals(2, resources.size());
    assertEquals(content2, resources.get(0).getContent());
    assertEquals(content1, resources.get(1).getContent());
  }
  
  @Test
  public void registerOverrideFeature() throws Exception {
    // Feature 1
    String content1 = "content1()";
    Uri content1Uri = expectResource(content1);
    Uri feature1Uri = expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(), null));
    
    String content2 = "content_two()";
    Uri content2Uri = expectResource(content2);
    Uri feature2Uri = expectResource(xml(BOTTOM_TPL, "gadget", content2Uri.getPath(), null));
    
    registry = new TestFeatureRegistry(feature1Uri.toString());
    List<FeatureResource> resources1 = registry.getAllFeatures();
    assertEquals(1, resources1.size());
    assertEquals(content1, resources1.get(0).getContent());
    
    // Register it again, different def.
    registry = new TestFeatureRegistry(feature2Uri.toString());
    List<FeatureResource> resources2 = registry.getAllFeatures();
    assertEquals(1, resources2.size());
    assertEquals(content2, resources2.get(0).getContent());

    // Check cached resources too.
    List<FeatureResource> resourcesAgain = registry.getAllFeatures();
    assertSame(resources2, resourcesAgain);
  }
  
  @Test
  public void cacheAccountsForUnsupportedState() throws Exception {
    String content1 = "content1()";
    Uri content1Uri = expectResource(content1);
    Map<String, String> attribs = Maps.newHashMap();
    String theContainer = "the-container";
    attribs.put("container", theContainer);
    Uri feature1Uri = expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(), null, attribs));
    
    // Register it.
    registry = new TestFeatureRegistry(feature1Uri.toString());
    
    // Retrieve content for matching context.
    List<String> needed = Lists.newArrayList("bottom");
    List<String> unsupported = Lists.newArrayList();
    List<FeatureResource> resources = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported);
    
    // Retrieve again w/ no unsupported list.
    List<FeatureResource> resourcesUnsup = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, null);
    
    assertNotSame(resources, resourcesUnsup);
    assertEquals(resources, resourcesUnsup);
    assertEquals(1, resources.size());
    assertEquals(content1, resources.get(0).getContent());

    // Now make sure the cache DOES work when needed.
    List<FeatureResource> resources2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported);
    assertSame(resources, resources2);

    List<FeatureResource> resourcesUnsup2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, null);
    assertSame(resourcesUnsup, resourcesUnsup2);
    
    // Lastly, ensure that ignoreCache is properly accounted.
    List<FeatureResource> resourcesIgnoreCache = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer, true), needed, unsupported);
    assertNotSame(resources, resourcesIgnoreCache);
    assertEquals(1, resourcesIgnoreCache.size());
    assertEquals(content1, resourcesIgnoreCache.get(0).getContent());
  }
  
  @Test
  public void cacheAccountsForContext() throws Exception {
    String content1 = "content1()";
    Uri content1Uri = expectResource(content1);
    Map<String, String> attribs = Maps.newHashMap();
    String theContainer = "the-container";
    attribs.put("container", theContainer);
    Uri feature1Uri = expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(), null, attribs));
    
    // Register it.
    registry = new TestFeatureRegistry(feature1Uri.toString());
    
    // Retrieve content for matching context.
    List<String> needed = Lists.newArrayList("bottom");
    List<String> unsupported = Lists.newArrayList();
    List<FeatureResource> resources = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported);
    
    // Retrieve again w/ mismatch container.
    List<FeatureResource> resourcesNoMatch = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, "foo"), needed, unsupported);
    
    // Retrieve again w/ mismatched context.
    List<FeatureResource> ctxMismatch = registry.getFeatureResources(
        getCtx(RenderingContext.CONTAINER, theContainer), needed, unsupported);

    assertNotSame(resources, resourcesNoMatch);
    assertNotSame(resources, ctxMismatch);
    
    assertEquals(1, resources.size());
    assertEquals(content1, resources.get(0).getContent());
    
    assertEquals(0, resourcesNoMatch.size());
    assertEquals(0, ctxMismatch.size());
    
    // Make sure caches work with appropriate matching.
    List<FeatureResource> resources2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported);
    assertSame(resources, resources2);
    
    List<FeatureResource> resourcesNoMatch2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, "foo"), needed, unsupported);
    assertSame(resourcesNoMatch, resourcesNoMatch2);
    
    List<FeatureResource> ctxMismatch2 = registry.getFeatureResources(
        getCtx(RenderingContext.CONTAINER, theContainer), needed, unsupported);
    assertSame(ctxMismatch, ctxMismatch2);
    
    // Check ignoreCache
    List<FeatureResource> resourcesIC = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer, true), needed, unsupported);
    assertNotSame(resources, resourcesIC);
    
//    bogus tests - both requests return EMPTY_LIST now, so you can't ascertain cache behavior.
//    List<FeatureResource> resourcesNoMatchIC = registry.getFeatureResources(
//        getCtx(RenderingContext.GADGET, "foo", true), needed, unsupported);
//    assertNotSame(resourcesNoMatch, resourcesNoMatchIC);
//
//    List<FeatureResource> ctxMismatchIC = registry.getFeatureResources(
//        getCtx(RenderingContext.CONTAINER, theContainer, true), needed, unsupported);
//    assertNotSame(ctxMismatch, ctxMismatchIC);
  }
  
  @Test
  public void missingIndexResultsInException() throws Exception {
    try {
      registry = new TestFeatureRegistry(makeResourceUri(".txt").toString());
      fail("Should have thrown an exception for missing .txt file");
    } catch (GadgetException e) {
      // Expected. Verify code.
      assertEquals(GadgetException.Code.INVALID_PATH, e.getCode());
    }
  }
  
  @Test
  public void missingFileResultsInException() throws Exception {
    try {
      registry = new TestFeatureRegistry(new UriBuilder().setScheme("file")
          .setPath("/is/not/there.foo.xml").toUri().toString());
      fail("Should have thrown missing .xml file exception");
    } catch (GadgetException e) {
      // Expected. Verify code.
      assertEquals(GadgetException.Code.INVALID_CONFIG, e.getCode());
    }
  }
  
  @Test
  public void selectExactFeatureResourcesGadget() throws Exception {
    checkExactFeatureResources("gadget", RenderingContext.GADGET);
  }
  
  @Test
  public void selectExactFeatureResourcesContainer() throws Exception {
    checkExactFeatureResources("container", RenderingContext.CONTAINER);
  }
  
  private void checkExactFeatureResources(String type, RenderingContext rctx) throws Exception {
    setupFullRegistry(type, null);
    GadgetContext ctx = getCtx(rctx, null);
    List<String> needed = Lists.newArrayList("nodep", "bottom");
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported);
    assertEquals(0, unsupported.size());
    assertEquals(2, resources.size());
    assertEquals("nodep", resources.get(0).getContent());
    assertEquals("bottom", resources.get(1).getContent());
  }
  
  @Test
  public void selectNoContentValidFeatureResourcesGadget() throws Exception {
    checkNoContentValidFeatureResources("gadget", RenderingContext.CONTAINER);
  }
  
  @Test
  public void selectNoContentValidFeatureResourcesContainer() throws Exception {
    checkNoContentValidFeatureResources("container", RenderingContext.GADGET);
  }
  
  private void checkNoContentValidFeatureResources(
      String type, RenderingContext rctx) throws Exception {
    setupFullRegistry(type, null);
    GadgetContext ctx = getCtx(rctx, null);
    List<String> needed = Lists.newArrayList("nodep", "bottom");
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported);
    assertEquals(0, resources.size());
  }
  
  @Test
  public void testTransitiveFeatureResourcesGadget() throws Exception {
    checkTransitiveFeatureResources("gadget", RenderingContext.GADGET);
  }
  
  @Test
  public void testTransitiveFeatureResourcesContainer() throws Exception {
    checkTransitiveFeatureResources("container", RenderingContext.CONTAINER);
  }
  
  private void checkTransitiveFeatureResources(String type, RenderingContext rctx)
      throws Exception {
    setupFullRegistry(type, null);
    GadgetContext ctx = getCtx(rctx, null);
    List<String> needed = Lists.newArrayList("top", "nodep");
    List<String> unsupported = Lists.newLinkedList();
    
    // Should come back in insertable order (from bottom of the graph up),
    // querying in feature.xml dependency order.
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported);
    assertEquals(5, resources.size());
    assertEquals("bottom", resources.get(0).getContent());
    assertEquals("mid_a", resources.get(1).getContent());
    assertEquals("mid_b", resources.get(2).getContent());
    assertEquals("top", resources.get(3).getContent());
    assertEquals("nodep", resources.get(4).getContent());
  }
  
  @Test
  public void unsupportedFeaturesPopulated() throws Exception {
    // Test only for gadget case; above tests are sufficient to ensure
    // that type and RenderingContext filter results properly.
    setupFullRegistry("gadget", null);
    GadgetContext ctx = getCtx(RenderingContext.GADGET, null);
    List<String> needed = Lists.newArrayList("nodep", "does-not-exist");
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported);
    assertEquals(1, resources.size());
    assertEquals("nodep", resources.get(0).getContent());
    assertEquals(1, unsupported.size());
    assertEquals("does-not-exist", unsupported.get(0));
  }
  
  @Test
  public void filterFeaturesByContainerMatch() throws Exception {
    // Again test only for gadget case; above tests cover type <-> RenderingContext
    setupFullRegistry("gadget", "one, two , three");
    GadgetContext ctx = getCtx(RenderingContext.GADGET, "two");
    List<String> needed = Lists.newArrayList("nodep", "bottom");
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported);
    assertEquals(2, resources.size());
    assertEquals("nodep", resources.get(0).getContent());
    assertEquals("bottom", resources.get(1).getContent());
    assertEquals(0, unsupported.size());
  }
  
  @Test
  public void filterFeaturesByContainerNoMatch() throws Exception {
    // Again test only for gadget case; above tests cover type <-> RenderingContext
    setupFullRegistry("gadget", "one, two, three");
    GadgetContext ctx = getCtx(RenderingContext.GADGET, "four");
    List<String> needed = Lists.newArrayList("nodep", "bottom");
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported);
    assertEquals(0, resources.size());  // no resource matches but all feature keys valid
    assertEquals(0, unsupported.size());
  }
  
  @Test
  public void getFeatureResourcesNoTransitiveSingle() throws Exception {
    setupFullRegistry("gadget", null);
    GadgetContext ctx = getCtx(RenderingContext.GADGET, null);
    List<String> needed = Lists.newArrayList("top", "bottom");
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported, false);
    // Should return in order requested.
    assertEquals(2, resources.size());
    assertEquals("top", resources.get(0).getContent());
    assertEquals("bottom", resources.get(1).getContent());
    assertEquals(0, unsupported.size());
  }
  
  @Test
  public void getAllFeatures() throws Exception {
    setupFullRegistry("gadget", null);
    List<FeatureResource> resources = registry.getAllFeatures();
    
    // No guaranteed order (top/mid/bottom bundle may be before nodep)
    // Just check that there are 5 resources around and let the above tests
    // handle transitivity checks.
    assertEquals(5, resources.size());
  }
  
  @Test
  public void getFeaturesStringsNoTransitive() throws Exception {
    setupFullRegistry("gadget", null);
    List<String> needed = Lists.newArrayList("nodep", "bottom");
    List<String> featureNames = registry.getFeatures(needed);
    assertEquals(2, featureNames.size());
    assertEquals("nodep", featureNames.get(0));
    assertEquals("bottom", featureNames.get(1));
  }
  
  @Test
  public void getFeaturesStringsTransitive() throws Exception {
    setupFullRegistry("gadget", null);
    List<String> needed = Lists.newArrayList("top", "nodep");
    List<String> featureNames = registry.getFeatures(needed);
    assertEquals(5, featureNames.size());
    assertEquals("bottom", featureNames.get(0));
    assertEquals("mid_a", featureNames.get(1));
    assertEquals("mid_b", featureNames.get(2));
    assertEquals("top", featureNames.get(3));
    assertEquals("nodep", featureNames.get(4));
  }
  
  @Test
  public void loopIsDetectedAndCrashes() throws Exception {
    // Set up a registry with features loop_a,b,c. C points back to A, which should
    // cause an exception to be thrown by the register method.
    String type = "gadget";
    Uri loopAUri = expectResource(xml(LOOP_A_TPL, type, null, "loop_a"));
    Uri loopBUri = expectResource(xml(LOOP_B_TPL, type, null, "loop_b"));
    Uri loopCUri = expectResource(xml(LOOP_C_TPL, type, null, "loop_c"));
    Uri txtFile = expectResource(loopAUri.toString() + '\n' + loopBUri.toString() + '\n' +
        loopCUri.toString(), ".txt");
    try {
      registry = new TestFeatureRegistry(txtFile.toString());
      fail("Should have thrown a loop-detected exception");
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.INVALID_CONFIG, e.getCode());
    }
  }
  
  @Test
  public void unavailableFeatureCrashes() throws Exception {
    Uri featUri = expectResource(xml(BAD_DEP_TPL, "gadget", null, "content"));
    try {
      registry = new TestFeatureRegistry(featUri.toString());
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.INVALID_CONFIG, e.getCode());
    }
  }
  
  @Test
  public void returnOnlyContainerFilteredJs() throws Exception {
    String feature = "thefeature";
    String container =  "foo";
    String containerContent = "content1();";
    String defaultContent = "content2();";
    Uri featureUri =
        expectResource(
          getContainerAndDefaultTpl(feature, container, containerContent, defaultContent));
    registry = new TestFeatureRegistry(featureUri.toString());
    List<String> needed = Lists.newArrayList(feature);
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources =
        registry.getFeatureResources(
          getCtx(RenderingContext.GADGET, container), needed, unsupported);
    assertEquals(1, resources.size());
    assertEquals(containerContent, resources.get(0).getContent());
  }
  
  @Test
  public void returnDefaultMatchJs() throws Exception {
    String feature = "thefeature";
    String container =  "foo";
    String containerContent = "content1();";
    String defaultContent = "content2();";
    Uri featureUri =
        expectResource(
          getContainerAndDefaultTpl(feature, container, containerContent, defaultContent));
    registry = new TestFeatureRegistry(featureUri.toString());
    List<String> needed = Lists.newArrayList(feature);
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = 
        registry.getFeatureResources(
          getCtx(RenderingContext.GADGET, "othercontainer"), needed, unsupported);
    assertEquals(1, resources.size());
    assertEquals(defaultContent, resources.get(0).getContent());
  }
  
  private String getContainerAndDefaultTpl(String name, String container, String c1, String c2) {
    StringBuilder sb = new StringBuilder();
    sb.append("<feature><name>").append(name).append("</name>");
    sb.append("<gadget container=\"").append(container).append("\">");
    sb.append("<script>").append(c1).append("</script></gadget>");
    sb.append("<gadget>");
    sb.append("<script>").append(c2).append("</script></gadget>");
    sb.append("</feature>");
    return sb.toString();
  }
  
  @Test
  public void resourceGetsMergedAttribs() throws Exception {
    String content1 = "content1()";
    Uri content1Uri = expectResource(content1);
    Map<String, String> attribs = Maps.newHashMap();
    String theContainer = "the-container";
    attribs.put("container", theContainer);
    attribs.put("one", "bundle-one");
    attribs.put("two", "bundle-two");
    
    Map<String, String> resourceAttribs = Maps.newHashMap();
    attribs.put("two", "attrib-two");
    attribs.put("three", "attrib-three");
    Uri feature1Uri = expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(),
        null, attribs, resourceAttribs));
    
    // Register it.
    registry = new TestFeatureRegistry(feature1Uri.toString());
    
    // Retrieve the resource for matching context.
    List<String> needed = Lists.newArrayList("bottom");
    List<String> unsupported = Lists.newArrayList();
    List<FeatureResource> resources = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported);

    // Sanity test.
    assertEquals(1, resources.size());

    // Check the attribs passed into the resource. This is a little funky, but it works.
    assertNotNull(lastAttribs);
    assertEquals(4, lastAttribs.size());
    assertEquals(theContainer, lastAttribs.get("container"));
    assertEquals("bundle-one", lastAttribs.get("one"));
    assertEquals("attrib-two", lastAttribs.get("two"));
    assertEquals("attrib-three", lastAttribs.get("three"));
  }
  
  private GadgetContext getCtx(final RenderingContext rctx, final String container) {
    return getCtx(rctx, container, false);
  }
  
  private GadgetContext getCtx(final RenderingContext rctx, final String container,
      final boolean ignoreCache) {
    return new GadgetContext() {
      @Override
      public RenderingContext getRenderingContext() {
        return rctx;
      }
      
      @Override
      public String getContainer() {
        return container != null ? container : ContainerConfig.DEFAULT_CONTAINER;
      }
      
      @Override
      public boolean getIgnoreCache() {
        return ignoreCache;
      }
    };
  }
  
  private void setupFullRegistry(String type, String containers) throws Exception {
    // Sets up a "full" gadget feature registry with several features registered:
    // nodep - has no deps on anything else
    // top - depends on mid_a and mid_b
    // mid_a and mid_b - both depend on bottom
    // bottom - depends on nothing else
    // The content registered for each is equal to the feature's name, for simplicity.
    // Also, all content is loaded as inline, also for simplicity.
    
    Map<String, String> attribs = Maps.newHashMap();
    if (containers != null) {
      attribs.put("container", containers);
    }
    
    Uri nodepUri = expectResource(xml(NODEP_TPL, type, null, "nodep", attribs));
    Uri topUri = expectResource(xml(TOP_TPL, type, null, "top", attribs));
    Uri midAUri = expectResource(xml(MID_A_TPL, type, null, "mid_a", attribs));
    Uri midBUri = expectResource(xml(MID_B_TPL, type, null, "mid_b", attribs));
    Uri bottomUri = expectResource(xml(BOTTOM_TPL, type, null, "bottom", attribs));
    Uri txtFile = expectResource(nodepUri.toString() + '\n' + topUri.toString() + '\n' +
        midAUri.toString() + '\n' + midBUri.toString() + '\n' + bottomUri.toString(), ".txt");
    registry = new TestFeatureRegistry(txtFile.toString());
  }
  
  private Uri expectResource(String content) {
    return expectResource(content, ".xml");
  }
  
  private Uri expectResource(String content, String suffix) {
    Uri res = makeResourceUri(suffix);
    resourceMock.put(res.getPath(), content);
    return res;
  }
  
  private static String getFeatureTpl(String name, String[] deps) {
    StringBuilder sb = new StringBuilder();
    sb.append("<feature><name>").append(name).append("</name>");
    for (String dep : deps) {
      sb.append("<dependency>").append(dep).append("</dependency>");
    }
    sb.append("<%type% %type_attribs%><script %uri% %res_attribs%>%content%</script></%type%>");
    sb.append("</feature>");
    return sb.toString();
  }
  
  private static String xml(String tpl, String type, String uri, String content) {
    return xml(tpl, type, uri, content, Maps.<String, String>newHashMap());
  }
  
  private static String xml(String tpl, String type, String uri, String content,
      Map<String, String> attribs) {
    return xml(tpl, type, uri, content, attribs, Maps.<String, String>newHashMap());
  }
  
  private static String xml(String tpl, String type, String uri, String content,
      Map<String, String> attribs, Map<String, String> resourceAttribs) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : attribs.entrySet()) {
      sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
    }
    StringBuilder sbRes = new StringBuilder();
    for (Map.Entry<String, String> entry : resourceAttribs.entrySet()) {
      sbRes.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
    }
    return tpl.replace("%type%", type)
        .replace("%uri%", uri != null ? "src=\"" + uri + '\"' : "")
        .replace("%content%", content != null ? content : "")
        .replace("%type_attribs%", sb.toString())
        .replace("%res_attribs%", sbRes.toString());
  }
  
  private static Uri makeFile(String content) throws Exception {
    // .xml suffix used even for js -- should be OK per FeatureResourceLoader tests
    // which simply indicate not to attempt .opt.js loading in this case.
    File file = File.createTempFile("feat", ".xml");
    file.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(content);
    out.close();
    return Uri.fromJavaUri(file.toURI());
  }
  
  private static Uri makeResourceUri(String suffix) {
    return Uri.parse("res://" + RESOURCE_BASE_PATH + "/file" + (++resourceIdx) + suffix);
  }
  
  private static class ResourceMock {
    private final Map<String, String> resourceMap;
    
    private ResourceMock() {
      this.resourceMap = Maps.newHashMap();
    }
    
    private void put(String key, String value) {
      resourceMap.put(clean(key), value);
    }
    
    private String get(String key) throws IOException {
      key = clean(key);
      if (!resourceMap.containsKey(key)) {
        throw new IOException("Missing resource: " + key);
      }
      return resourceMap.get(key); 
    }
    
    private String clean(String key) {
      // Resource loading doesn't support leading '/'
      return key.startsWith("/") ? key.substring(1) : key;
    }
  }
}
