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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
  private TestFeatureRegistry registry;

  @Test
  public void registerFromFileFeatureXmlFileScheme() throws Exception {
    checkRegisterFromFileFeatureXml(true);
  }

  @Test
  public void registerFromFileFeatureXmlNoScheme() throws Exception {
    checkRegisterFromFileFeatureXml(false);
  }

  private void checkRegisterFromFileFeatureXml(boolean withScheme) throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
    String content = "content-" + (withScheme ? "withScheme" : "noScheme");
    Uri resUri = makeFile(content);
    Uri featureFile = makeFile(xml(NODEP_TPL, "gadget",
        withScheme ? resUri.toString() : resUri.getPath(), null));
    registry = builder.build(withScheme ? featureFile.toString() : featureFile.getPath());

    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures().getResources();
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
    registry = TestFeatureRegistry.newBuilder().build(childDir.toURI().toString());

    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures().getResources();
    assertEquals(1, resources.size());
    assertEquals(content, resources.get(0).getContent());
  }

  @Test
  public void registerFromResourceFeatureXml() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
    String content = "resource-content()";
    Uri contentUri = builder.expectResource(content);
    Uri featureUri = builder.expectResource(xml(NODEP_TPL, "gadget", contentUri.getPath(), null));
    builder.addFeatureFile(featureUri.toString());
    registry = builder.build();

    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures().getResources();
    assertEquals(1, resources.size());
    assertEquals(content, resources.get(0).getContent());
  }

  @Test
  public void registerFromResourceFeatureXmlRelativeContent() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
    String content = "resource-content-relative()";
    Uri contentUri = builder.expectResource(content);
    String relativePath = contentUri.getPath().substring(contentUri.getPath().lastIndexOf('/') + 1);
    Uri featureUri = builder.expectResource(xml(NODEP_TPL, "gadget", relativePath, null));
    registry = builder.build(featureUri.toString());

    // Verify single resource works all the way through.
    List<FeatureResource> resources = registry.getAllFeatures().getResources();
    assertEquals(1, resources.size());
    assertEquals(content, resources.get(0).getContent());
  }

  @Test
  public void registerFromResourceIndex() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    // One with extern resource loaded content...
    String content1 = "content1()";
    Uri content1Uri = builder.expectResource(content1);
    Uri feature1Uri = builder.expectResource(xml(MID_A_TPL, "gadget", content1Uri.getPath(), null));

    // One feature with inline content (that it depends on)...
    String content2 = "inline()";
    Uri feature2Uri = builder.expectResource(xml(BOTTOM_TPL, "gadget", null, content2));

    // .txt file to join the two
    Uri txtFile = builder.expectResource(feature1Uri.toString() + '\n' + feature2Uri.toString(), ".txt");

    // Load resources from the text file and do basic validation they're good.
    registry = builder.build(txtFile.toString());

    // Contents should be ordered based on the way they went in.
    List<FeatureResource> resources = registry.getAllFeatures().getResources();
    assertEquals(2, resources.size());
    assertEquals(content2, resources.get(0).getContent());
    assertEquals(content1, resources.get(1).getContent());
  }

  @Test
  public void registerOverrideFeature() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    // Feature 1
    String content1 = "content1()";
    Uri content1Uri = builder.expectResource(content1);
    Uri feature1Uri = builder.expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(), null));

    String content2 = "content_two()";
    Uri content2Uri = builder.expectResource(content2);
    Uri feature2Uri = builder.expectResource(xml(BOTTOM_TPL, "gadget", content2Uri.getPath(), null));

    registry = builder.build(feature1Uri.toString());
    List<FeatureResource> resources1 = registry.getAllFeatures().getResources();
    assertEquals(1, resources1.size());
    assertEquals(content1, resources1.get(0).getContent());

    // Register it again, different def.
    registry = builder.build(feature2Uri.toString());
    List<FeatureResource> resources2 = registry.getAllFeatures().getResources();
    assertEquals(1, resources2.size());
    assertEquals(content2, resources2.get(0).getContent());

    // Check cached resources too.
    List<FeatureResource> resourcesAgain = registry.getAllFeatures().getResources();
    assertSame(resources2, resourcesAgain);
  }

  @Test
  public void cacheAccountsForUnsupportedState() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    String content1 = "content1()";
    Uri content1Uri = builder.expectResource(content1);
    Map<String, String> attribs = Maps.newHashMap();
    String theContainer = "the-container";
    attribs.put("container", theContainer);
    Uri feature1Uri = builder.expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(), null, attribs));

    // Register it.
    registry = builder.build(feature1Uri.toString());

    // Retrieve content for matching context.
    List<String> needed = Lists.newArrayList("bottom");
    List<String> unsupported = Lists.newArrayList();
    List<FeatureResource> resources = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported).getResources();

    // Retrieve again w/ no unsupported list.
    List<FeatureResource> resourcesUnsup = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, null).getResources();

    assertNotSame(resources, resourcesUnsup);
    assertEquals(resources, resourcesUnsup);
    assertEquals(1, resources.size());
    assertEquals(content1, resources.get(0).getContent());

    // Now make sure the cache DOES work when needed.
    List<FeatureResource> resources2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported).getResources();
    assertSame(resources, resources2);

    List<FeatureResource> resourcesUnsup2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, null).getResources();
    assertSame(resourcesUnsup, resourcesUnsup2);

    // Lastly, ensure that ignoreCache is properly accounted.
    List<FeatureResource> resourcesIgnoreCache = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer, true), needed, unsupported).getResources();
    assertNotSame(resources, resourcesIgnoreCache);
    assertEquals(1, resourcesIgnoreCache.size());
    assertEquals(content1, resourcesIgnoreCache.get(0).getContent());
  }

  @Test
  public void cacheAccountsForContext() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    String content1 = "content1()";
    Uri content1Uri = builder.expectResource(content1);
    Map<String, String> attribs = Maps.newHashMap();
    String theContainer = "the-container";
    attribs.put("container", theContainer);
    Uri feature1Uri = builder.expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(), null, attribs));

    // Register it.
    registry = builder.build(feature1Uri.toString());

    // Retrieve content for matching context.
    List<String> needed = Lists.newArrayList("bottom");
    List<String> unsupported = Lists.newArrayList();
    List<FeatureResource> resources = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported).getResources();

    // Retrieve again w/ mismatch container.
    List<FeatureResource> resourcesNoMatch = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, "foo"), needed, unsupported).getResources();

    // Retrieve again w/ mismatched context.
    List<FeatureResource> ctxMismatch = registry.getFeatureResources(
        getCtx(RenderingContext.CONTAINER, theContainer), needed, unsupported).getResources();

    assertNotSame(resources, resourcesNoMatch);
    assertNotSame(resources, ctxMismatch);

    assertEquals(1, resources.size());
    assertEquals(content1, resources.get(0).getContent());

    assertEquals(0, resourcesNoMatch.size());
    assertEquals(0, ctxMismatch.size());

    // Make sure caches work with appropriate matching.
    List<FeatureResource> resources2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported).getResources();
    assertSame(resources, resources2);

    List<FeatureResource> resourcesNoMatch2 = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, "foo"), needed, unsupported).getResources();
    assertSame(resourcesNoMatch, resourcesNoMatch2);

    List<FeatureResource> ctxMismatch2 = registry.getFeatureResources(
        getCtx(RenderingContext.CONTAINER, theContainer), needed, unsupported).getResources();
    assertSame(ctxMismatch, ctxMismatch2);

    // Check ignoreCache
    List<FeatureResource> resourcesIC = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer, true), needed, unsupported).getResources();
    assertNotSame(resources, resourcesIC);
  }

  @Test
  public void missingIndexResultsInException() throws Exception {
    try {
      registry = TestFeatureRegistry.newBuilder().build(makeResourceUri(".txt").toString());
      fail("Should have thrown an exception for missing .txt file");
    } catch (GadgetException e) {
      // Expected. Verify code.
      assertEquals(GadgetException.Code.INVALID_PATH, e.getCode());
    }
  }

  @Test
  public void missingFileResultsInException() throws Exception {
    try {
      registry = TestFeatureRegistry.newBuilder().build(new UriBuilder().setScheme("file")
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
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();
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
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();
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
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();
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
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();
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
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();
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
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();
    assertEquals(0, resources.size());  // no resource matches but all feature keys valid
    assertEquals(0, unsupported.size());
  }

  @Test
  public void getFeatureResourcesNoTransitiveSingle() throws Exception {
    setupFullRegistry("gadget", null);
    GadgetContext ctx = getCtx(RenderingContext.GADGET, null);
    List<String> needed = Lists.newArrayList("top", "bottom");
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported, false).getResources();
    // Should return in order requested.
    assertEquals(2, resources.size());
    assertEquals("top", resources.get(0).getContent());
    assertEquals("bottom", resources.get(1).getContent());
    assertEquals(0, unsupported.size());
  }

  @Test
  public void getAllFeatures() throws Exception {
    setupFullRegistry("gadget", null);
    List<FeatureResource> resources = registry.getAllFeatures().getResources();

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
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    // Set up a registry with features loop_a,b,c. C points back to A, which should
    // cause an exception to be thrown by the register method.
    String type = "gadget";
    Uri loopAUri = builder.expectResource(xml(LOOP_A_TPL, type, null, "loop_a"));
    Uri loopBUri = builder.expectResource(xml(LOOP_B_TPL, type, null, "loop_b"));
    Uri loopCUri = builder.expectResource(xml(LOOP_C_TPL, type, null, "loop_c"));
    Uri txtFile = builder.expectResource(loopAUri.toString() + '\n' + loopBUri.toString() + '\n' +
        loopCUri.toString(), ".txt");
    try {
      registry = builder.build(txtFile.toString());
      fail("Should have thrown a loop-detected exception");
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.INVALID_CONFIG, e.getCode());
    }
  }

  @Test
  public void unavailableFeatureCrashes() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
    Uri featUri = builder.expectResource(xml(BAD_DEP_TPL, "gadget", null, "content"));
    try {
      registry = builder.build(featUri.toString());
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.INVALID_CONFIG, e.getCode());
    }
  }

  @Test
  public void returnOnlyContainerFilteredJs() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
    String feature = "thefeature";
    String container =  "foo";
    String containerContent = "content1();";
    String defaultContent = "content2();";
    Uri featureUri =
        builder.expectResource(
          getContainerAndDefaultTpl(feature, container, containerContent, defaultContent));
    registry = builder.build(featureUri.toString());
    List<String> needed = Lists.newArrayList(feature);
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources =
        registry.getFeatureResources(
          getCtx(RenderingContext.GADGET, container), needed, unsupported).getResources();
    assertEquals(1, resources.size());
    assertEquals(containerContent, resources.get(0).getContent());
  }

  @Test
  public void returnDefaultMatchJs() throws Exception {
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
    String feature = "thefeature";
    String container =  "foo";
    String containerContent = "content1();";
    String defaultContent = "content2();";
    Uri featureUri =
        builder.expectResource(
          getContainerAndDefaultTpl(feature, container, containerContent, defaultContent));
    registry = builder.build(featureUri.toString());
    List<String> needed = Lists.newArrayList(feature);
    List<String> unsupported = Lists.newLinkedList();
    List<FeatureResource> resources =
        registry.getFeatureResources(
          getCtx(RenderingContext.GADGET, "othercontainer"), needed, unsupported).getResources();
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
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    String content1 = "content1()";
    Uri content1Uri = builder.expectResource(content1);
    Map<String, String> attribs = Maps.newHashMap();
    String theContainer = "the-container";
    attribs.put("container", theContainer);
    attribs.put("one", "bundle-one");
    attribs.put("two", "bundle-two");

    Map<String, String> resourceAttribs = Maps.newHashMap();
    attribs.put("two", "attrib-two");
    attribs.put("three", "attrib-three");
    Uri feature1Uri = builder.expectResource(xml(BOTTOM_TPL, "gadget", content1Uri.getPath(),
        null, attribs, resourceAttribs));

    // Register it.
    registry = builder.build(feature1Uri.toString());

    // Retrieve the resource for matching context.
    List<String> needed = Lists.newArrayList("bottom");
    List<String> unsupported = Lists.newArrayList();
    List<FeatureResource> resources = registry.getFeatureResources(
        getCtx(RenderingContext.GADGET, theContainer), needed, unsupported).getResources();

    // Sanity test.
    assertEquals(1, resources.size());

    // Check the attribs passed into the resource. This is a little funky, but it works.
    Map<String, String> lastAttribs = registry.getLastAttribs();
    assertNotNull(lastAttribs);
    assertEquals(4, lastAttribs.size());
    assertEquals(theContainer, lastAttribs.get("container"));
    assertEquals("bundle-one", lastAttribs.get("one"));
    assertEquals("attrib-two", lastAttribs.get("two"));
    assertEquals("attrib-three", lastAttribs.get("three"));
  }

  @Test
  public void testGetGadgetFromMixedRegistry() throws Exception {
    setupMixedFullRegistry();

    GadgetContext ctx = getCtx(RenderingContext.GADGET, null);
    List<String> needed = Lists.newArrayList("top");
    List<String> unsupported = Lists.newLinkedList();

    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();

    assertEquals(4, resources.size());
    assertEquals("bottom();all();", resources.get(0).getContent());
    assertEquals("mid_b();gadget();", resources.get(1).getContent());
    assertEquals("mid_a();all();", resources.get(2).getContent());
    assertEquals("top();gadget();", resources.get(3).getContent());
  }

  @Test
  public void testGetContainerFromMixedRegistry() throws Exception {
    setupMixedFullRegistry();

    GadgetContext ctx = getCtx(RenderingContext.CONTAINER, null);
    List<String> needed = Lists.newArrayList("top");
    List<String> unsupported = Lists.newLinkedList();

    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();

    assertEquals(4, resources.size());
    assertEquals("bottom();all();", resources.get(0).getContent());
    assertEquals("mid_b();container();", resources.get(1).getContent());
    assertEquals("mid_a();container();", resources.get(2).getContent());
    assertEquals("top();all();", resources.get(3).getContent());
  }

  @Test
  public void testGetConfiguredGadgetFromMixedRegistry() throws Exception {
    setupMixedFullRegistry();

    GadgetContext ctx = getCtx(RenderingContext.CONFIGURED_GADGET, null);
    List<String> needed = Lists.newArrayList("top");
    List<String> unsupported = Lists.newLinkedList();

    List<FeatureResource> resources = registry.getFeatureResources(ctx, needed, unsupported).getResources();

    assertEquals(4, resources.size());
    assertEquals("bottom();all();", resources.get(0).getContent());
    assertEquals("mid_b();gadget();", resources.get(1).getContent());
    assertEquals("mid_a();all();", resources.get(2).getContent());
    assertEquals("top();gadget();", resources.get(3).getContent());
  }

  @Test(expected = GadgetException.class)
  public void testCheckDependencyLoopWithLoopDependencyError() throws Exception {
        TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
        Uri featureA, featureB, featureC, featureD, txtFile;

        // featureA, featureB, featureC and featureD have dependency loop
        // problem.
        featureA = builder.expectResource("<feature>" + "<name>featureA</name>"
                + "<dependency>featureB</dependency>" + "<gadget>"
                + "  <script>top();gadget();</script>" + "</gadget>" + "<all>"
                + "  <script>top();all();</script>" + "</all>" + "</feature>");
        featureB = builder.expectResource("<feature>" + "<name>featureB</name>"
                + "<dependency>featureC</dependency>" + "<container>"
                + "  <script>mid_a();container();</script>" + "</container>"
                + "<all>" + "  <script>mid_a();all();</script>" + "</all>"
                + "</feature>");
        featureC = builder.expectResource("<feature>" + "<name>featureC</name>"
                + "<dependency>featureD</dependency>" + "<container>"
                + "  <script>mid_b();container();</script>" + "</container>"
                + "<gadget>" + "  <script>mid_b();gadget();</script>"
                + "</gadget>" + "</feature>");
        featureD = builder.expectResource("<feature>" + "<name>featureD</name>"
                + "<dependency>featureA</dependency>" + "<all>"
                + "  <script>bottom();all();</script>" + "</all>"
                + "</feature>");
        txtFile = builder.expectResource(
                featureA.toString() + '\n' + featureB.toString() + '\n'
                        + featureC.toString() + '\n' + featureD.toString(),
                ".txt");

        registry = builder.build(txtFile.toString());
  }

  @Test(expected = GadgetException.class)
  public void testCheckDependencyLoopWithPartialLoopDependencyError() throws Exception {
        TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
        Uri featureA, featureB, featureC, featureD, txtFile;

        // featureC and featureD have dependency loop problem.
        featureA = builder.expectResource("<feature>" + "<name>featureA</name>"
                + "<dependency>featureB</dependency>" + "<gadget>"
                + "  <script>top();gadget();</script>" + "</gadget>" + "<all>"
                + "  <script>top();all();</script>" + "</all>" + "</feature>");
        featureB = builder.expectResource("<feature>" + "<name>featureB</name>"
                + "<dependency>featureC</dependency>" + "<container>"
                + "  <script>mid_a();container();</script>" + "</container>"
                + "<all>" + "  <script>mid_a();all();</script>" + "</all>"
                + "</feature>");
        featureC = builder.expectResource("<feature>" + "<name>featureC</name>"
                + "<dependency>featureD</dependency>" + "<container>"
                + "  <script>mid_b();container();</script>" + "</container>"
                + "<gadget>" + "  <script>mid_b();gadget();</script>"
                + "</gadget>" + "</feature>");
        featureD = builder.expectResource("<feature>" + "<name>featureD</name>"
                + "<dependency>featureC</dependency>" + "<all>"
                + "  <script>bottom();all();</script>" + "</all>"
                + "</feature>");
        txtFile = builder.expectResource(
                featureA.toString() + '\n' + featureB.toString() + '\n'
                        + featureC.toString() + '\n' + featureD.toString(),
                ".txt");

        registry = builder.build(txtFile.toString());
  }

  @Test
  public void testCheckDependencyLoopWithNormalDependency() {
        TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();
        Uri featureA, featureB, featureC, featureD, txtFile;

        // There is no loop problem.
        featureA = builder.expectResource("<feature>" + "<name>featureA</name>"
                + "<dependency>featureB</dependency>" + "<gadget>"
                + "  <script>top();gadget();</script>" + "</gadget>" + "<all>"
                + "  <script>top();all();</script>" + "</all>" + "</feature>");
        featureB = builder.expectResource("<feature>" + "<name>featureB</name>"
                + "<dependency>featureC</dependency>" + "<container>"
                + "  <script>mid_a();container();</script>" + "</container>"
                + "<all>" + "  <script>mid_a();all();</script>" + "</all>"
                + "</feature>");
        featureC = builder.expectResource("<feature>" + "<name>featureC</name>"
                + "<dependency>featureD</dependency>" + "<container>"
                + "  <script>mid_b();container();</script>" + "</container>"
                + "<gadget>" + "  <script>mid_b();gadget();</script>"
                + "</gadget>" + "</feature>");
        featureD = builder.expectResource("<feature>" + "<name>featureD</name>"
                + "<all>" + "  <script>bottom();all();</script>" + "</all>"
                + "</feature>");
        txtFile = builder.expectResource(
                featureA.toString() + '\n' + featureB.toString() + '\n'
                        + featureC.toString() + '\n' + featureD.toString(),
                ".txt");

        try {
            registry = builder.build(txtFile.toString());
        } catch (GadgetException e) {
            fail("Shouldn't throw a GadgetException.");
        }
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

  private void setupMixedFullRegistry() throws Exception {
    // Sets up a "full" gadget feature registry with several features registered in linear
    // dependency order: top -> mid_a -> mid_b -> bottom
    // The content registered for each is equal to the feature's name, for simplicity.
    // Also, all content is loaded as inline, also for simplicity.
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    Uri topUri = builder.expectResource(
      "<feature>" +
        "<name>top</name>" +
        "<dependency>mid_a</dependency>" +
        "<dependency>mid_b</dependency>" +
        "<gadget>" +
        "  <script>top();gadget();</script>" +
        "</gadget>" +
        "<all>" +
        "  <script>top();all();</script>" +
        "</all>" +
      "</feature>"
    );
    Uri midAUri = builder.expectResource(
      "<feature>" +
        "<name>mid_a</name>" +
        "<dependency>mid_b</dependency>" +
        "<container>" +
        "  <script>mid_a();container();</script>" +
        "</container>" +
        "<all>" +
        "  <script>mid_a();all();</script>" +
        "</all>" +
      "</feature>"
    );
    Uri midBUri = builder.expectResource(
      "<feature>" +
        "<name>mid_b</name>" +
        "<dependency>bottom</dependency>" +
        "<container>" +
        "  <script>mid_b();container();</script>" +
        "</container>" +
        "<gadget>" +
        "  <script>mid_b();gadget();</script>" +
        "</gadget>" +
      "</feature>"
    );
    Uri bottomUri = builder.expectResource(
        "<feature>" +
          "<name>bottom</name>" +
          "<all>" +
          "  <script>bottom();all();</script>" +
          "</all>" +
        "</feature>"
      );
    Uri txtFile = builder.expectResource(topUri.toString() + '\n' +
        midAUri.toString() + '\n' + midBUri.toString() + '\n' + bottomUri.toString(), ".txt");

    registry = builder.build(txtFile.toString());
  }

  private void setupFullRegistry(String type, String containers) throws Exception {
    // Sets up a "full" gadget feature registry with several features registered:
    // nodep - has no deps on anything else
    // top - depends on mid_a and mid_b
    // mid_a and mid_b - both depend on bottom
    // bottom - depends on nothing else
    // The content registered for each is equal to the feature's name, for simplicity.
    // Also, all content is loaded as inline, also for simplicity.
    TestFeatureRegistry.Builder builder = TestFeatureRegistry.newBuilder();

    Map<String, String> attribs = Maps.newHashMap();
    if (containers != null) {
      attribs.put("container", containers);
    }

    Uri nodepUri = builder.expectResource(xml(NODEP_TPL, type, null, "nodep", attribs));
    Uri topUri = builder.expectResource(xml(TOP_TPL, type, null, "top", attribs));
    Uri midAUri = builder.expectResource(xml(MID_A_TPL, type, null, "mid_a", attribs));
    Uri midBUri = builder.expectResource(xml(MID_B_TPL, type, null, "mid_b", attribs));
    Uri bottomUri = builder.expectResource(xml(BOTTOM_TPL, type, null, "bottom", attribs));
    Uri txtFile = builder.expectResource(nodepUri.toString() + '\n' + topUri.toString() + '\n' +
        midAUri.toString() + '\n' + midBUri.toString() + '\n' + bottomUri.toString(), ".txt");
    registry = builder.build(txtFile.toString());
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
}
