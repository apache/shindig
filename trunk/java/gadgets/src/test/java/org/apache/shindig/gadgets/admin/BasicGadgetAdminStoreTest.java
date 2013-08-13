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
package org.apache.shindig.gadgets.admin;

import static org.easymock.EasyMock.eq;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.admin.FeatureAdminData.Type;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @version $Id: $
 */
public class BasicGadgetAdminStoreTest extends EasyMockTestCase {

  private static final String SAMPLE_STORE = "{" + "\"default\" : {" + "\"gadgets\" : {"
          + "\"http://www.google.com:80/ig/modules/horoscope.xml\" : {"
          + "\"features\" : {"
          + "\"names\" : [\"views\", \"tabs\", \"setprefs\", \"dynamic-height\", \"settitle\"],"
          + "\"type\" : \"whitelist\"" + "}},"
          + "\"http://www.labpixies.com/campaigns/todo/todo.xml\" : {"
          + "\"features\" : {"
          + "\"names\" : [\"setprefs\", \"dynamic-height\", \"views\"],"
          + "\"type\" : \"blacklist\"" + "}},"
          + "\"https://foo.com/*\" : {"
          + "\"features\" : {"
          + "\"names\" : []" + "}},"
          + "\"http://*\" : {"
          + "\"features\" : {"
          + "\"names\" : [],"
          + "\"type\" : \"whitelist\""
          + "},"
          + "\"rpc\" : {"
          + "\"additionalServiceIds\" : [\"rpc1\", \"rpc2\"]"
          +"}}}"
          + "}}";

  private static final String DEFAULT = "default";
  private static final String HOROSCOPE = "http://www.google.com/ig/modules/horoscope.xml";
  private static final String HOROSCOPE_WITH_PORT = "http://www.google.com:80/ig/modules/horoscope.xml";
  private static final String TODO = "http://www.labpixies.com/campaigns/todo/todo.xml";
  private static final String TEST_GADGET = "http://www.example.com/gadget.xml";
  private static final String FOO_GADGET = "https://foo.com/*";
  private static final String HTTP_GADGET = "http://*";
  private Set<String> HOROSCOPE_FEATURES = Sets.newHashSet("views", "tabs", "setprefs",
          "dynamic-height", "settitle", "core");
  private Set<String> TODO_FEATURES = Sets.newHashSet("views", "setprefs", "dynamic-height");
  private Set<String> FOO_FEATURES = Sets.newHashSet("core");
  private Set<String> HTTP_FEATURES = Sets.newHashSet("core");

  private final FeatureRegistry mockRegistry = mock(FeatureRegistry.class);
  private final Gadget mockGadget = mock(Gadget.class);
  private final GadgetContext mockContext = mock(GadgetContext.class);
  private final GadgetSpec mockSpec = mock(GadgetSpec.class);
  private final ModulePrefs mockPrefs = mock(ModulePrefs.class);
  private final ContainerConfig enabledConfig = new FakeContainerConfig(true, true);
  private final ContainerConfig disabledConfig = new FakeContainerConfig(false, false);

  private BasicGadgetAdminStore enabledStore;
  private BasicGadgetAdminStore disabledStore;
  private GadgetAdminData horoscopeAdminData;
  private GadgetAdminData todoAdminData;
  private GadgetAdminData fooAdminData;
  private GadgetAdminData httpAdminData;
  private ContainerAdminData defaultAdminData;
  private FeatureRegistryProvider featureRegistryProvider;
  private RpcAdminData rpcAdminData;

  @Before
  public void setUp() throws Exception {
    featureRegistryProvider = new FeatureRegistryProvider() {
      public FeatureRegistry get(String repository) throws GadgetException {
        return mockRegistry;
      }
    };

    rpcAdminData = new RpcAdminData(Sets.newHashSet("rpc1", "rpc2"));

    enabledStore = new BasicGadgetAdminStore(featureRegistryProvider, enabledConfig,
        new ServerAdminData());
    enabledStore.init(SAMPLE_STORE);

    disabledStore = new BasicGadgetAdminStore(featureRegistryProvider, disabledConfig,
        new ServerAdminData());

    horoscopeAdminData = new GadgetAdminData(new FeatureAdminData(HOROSCOPE_FEATURES,
            Type.WHITELIST), new RpcAdminData());
    todoAdminData = new GadgetAdminData(new FeatureAdminData(TODO_FEATURES,
            Type.BLACKLIST), new RpcAdminData());
    fooAdminData = new GadgetAdminData(new FeatureAdminData(FOO_FEATURES,
            Type.WHITELIST), new RpcAdminData());
    httpAdminData = new GadgetAdminData(new FeatureAdminData(HTTP_FEATURES,
            Type.WHITELIST), rpcAdminData);

    defaultAdminData = new ContainerAdminData();
    defaultAdminData.addGadgetAdminData(TODO, todoAdminData);
    defaultAdminData.addGadgetAdminData(HOROSCOPE_WITH_PORT, horoscopeAdminData);
    defaultAdminData.addGadgetAdminData(FOO_GADGET, fooAdminData);
    defaultAdminData.addGadgetAdminData(HTTP_GADGET, httpAdminData);

  }

  @After
  public void tearDown() throws Exception {
    enabledStore = null;
    horoscopeAdminData = null;
    todoAdminData = null;
    defaultAdminData = null;
    rpcAdminData = null;
  }

  private void mockGadget(List<Feature> allFeatures) {
    mockGadget(allFeatures, DEFAULT, TEST_GADGET);
  }

  private void mockGadget(List<Feature> allFeatures, String container) {
    mockGadget(allFeatures, container, TEST_GADGET);
  }

  private void mockGadget(List<Feature> allFeatures, String container, String gadgetUrl) {
    mockGadgetContext(container);
    mockGadgetSpec(allFeatures, gadgetUrl);
    EasyMock.expect(mockGadget.getContext()).andReturn(mockContext).anyTimes();
    EasyMock.expect(mockGadget.getSpec()).andReturn(mockSpec).anyTimes();
  }

  private void mockGadgetContext(String container) {
    EasyMock.expect(mockContext.getContainer()).andReturn(container).anyTimes();
  }

  private void mockGadgetSpec(List<Feature> allFeatures, String gadgetUrl) {
    mockModulePrefs(allFeatures);
    EasyMock.expect(mockSpec.getUrl()).andReturn(Uri.parse(gadgetUrl)).anyTimes();
    EasyMock.expect(mockSpec.getModulePrefs()).andReturn(mockPrefs).anyTimes();
  }

  private void mockModulePrefs(List<Feature> features) {
    EasyMock.expect(mockPrefs.getAllFeatures()).andReturn(features).anyTimes();
  }

  private Feature createMockFeature(String name, boolean required) {
    Feature feature = mock(Feature.class);
    EasyMock.expect(feature.getName()).andReturn(name).anyTimes();
    EasyMock.expect(feature.getRequired()).andReturn(required).anyTimes();
    return feature;
  }

  private void mockRegistryForFeatureAdmin(Set<String> allowed, List<String> getFeaturesAllowed,
          List<String> allGadgetFeatures, List<String> gadgetRequiredFeatureNames) {
    EasyMock.expect(mockRegistry.getFeatures(eq(Sets.newHashSet(allowed))))
            .andReturn(Lists.newArrayList(getFeaturesAllowed)).anyTimes();
    EasyMock.expect(mockRegistry.getFeatures(eq(Lists.newArrayList("core"))))
            .andReturn(Lists.newArrayList(allGadgetFeatures)).anyTimes();
    EasyMock.expect(mockRegistry.getFeatures(eq(gadgetRequiredFeatureNames)))
            .andReturn(allGadgetFeatures).anyTimes();
  }

  @Test
  public void testGetGadgetAdminData() {
    assertEquals(horoscopeAdminData, enabledStore.getGadgetAdminData(DEFAULT, HOROSCOPE));
    assertEquals(todoAdminData, enabledStore.getGadgetAdminData(DEFAULT, TODO));
    assertEquals(fooAdminData, enabledStore.getGadgetAdminData(DEFAULT, "https://foo.com/bar/gadget.xml"));
    assertEquals(fooAdminData, enabledStore.getGadgetAdminData(DEFAULT, "https://foo.com:443/bar/gadget.xml"));
    assertNull(enabledStore.getGadgetAdminData("my_container", HOROSCOPE));
    assertEquals(httpAdminData, enabledStore.getGadgetAdminData(DEFAULT, "http://example.com/gadget2.xml"));
  }

  @Test
  public void testSetGadgetAdminData() {
    assertEquals(horoscopeAdminData, enabledStore.getGadgetAdminData(DEFAULT, HOROSCOPE));

    horoscopeAdminData.getFeatureAdminData().addFeature("foo_feature");
    enabledStore.setGadgetAdminData(DEFAULT, HOROSCOPE, horoscopeAdminData);
    assertTrue(enabledStore.getGadgetAdminData(DEFAULT, HOROSCOPE).getFeatureAdminData()
            .getFeatures().contains("foo_feature"));

    assertEquals(httpAdminData, enabledStore.getGadgetAdminData(DEFAULT, "http://example.com/gadget2.xml"));
    enabledStore.setGadgetAdminData(DEFAULT, "http://example.com/gadget2.xml", todoAdminData);
    assertEquals(todoAdminData,
            enabledStore.getGadgetAdminData(DEFAULT, "http://example.com/gadget2.xml"));

    enabledStore.setGadgetAdminData(DEFAULT, "http://example.com/gadget1.xml", null);
    assertNotNull(enabledStore.getGadgetAdminData(DEFAULT, "http://example.com/gadget1.xml"));

    enabledStore.setGadgetAdminData(DEFAULT, null, horoscopeAdminData);
    assertNull(enabledStore.getGadgetAdminData(DEFAULT, null));
  }

  @Test
  public void testGetContainerAdminData() {
    assertEquals(defaultAdminData, enabledStore.getContainerAdminData(DEFAULT));
    assertNull(enabledStore.getContainerAdminData("my_constianer"));
  }

  @Test
  public void testSetContainerAdminData() {
    assertEquals(defaultAdminData, enabledStore.getContainerAdminData(DEFAULT));

    defaultAdminData.removeGadgetAdminData(TODO);
    enabledStore.setContainerAdminData(DEFAULT, defaultAdminData);
    assertEquals(defaultAdminData, enabledStore.getContainerAdminData(DEFAULT));

    assertNull(enabledStore.getContainerAdminData("my_container"));
    enabledStore.setContainerAdminData("my_container", defaultAdminData);
    assertEquals(defaultAdminData, enabledStore.getContainerAdminData("my_container"));

    enabledStore.setContainerAdminData(null, defaultAdminData);
    assertNull(enabledStore.getContainerAdminData(null));

    enabledStore.setContainerAdminData("my_container_2", null);
    assertNotNull(enabledStore.getContainerAdminData("my_container_2"));
  }

  @Test
  public void testGetServerAdminData() {
    ServerAdminData test = new ServerAdminData();
    test.addContainerAdminData(DEFAULT, defaultAdminData);
    assertEquals(test, enabledStore.getServerAdminData());
  }

  @Test
  public void testBlacklistAll() throws Exception {
    Set<String> features = Sets.newHashSet();
    List<String> featuresAndDeps = Lists.newArrayList();
    List<String> allGadgetFeatures = Lists.newArrayList("dep1", "dep2", "foo1", "foo2", "foo3");
    FeatureAdminData data = new FeatureAdminData(features, Type.WHITELIST);
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo1", "foo2", "foo3");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true),
            createMockFeature(gadgetRequiredFeatureNames.get(2), true));
    enabledStore.getContainerAdminData(DEFAULT).addGadgetAdminData(TEST_GADGET,
            new GadgetAdminData(data, null));
    mockRegistryForFeatureAdmin(features, featuresAndDeps,
            allGadgetFeatures, gadgetRequiredFeatureNames);
    mockGadget(allFeatures);
    replay();
    assertFalse(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testWhitelistAll() throws Exception {
    Set<String> features = Sets.newHashSet();
    List<String> featuresAndDeps = Lists.newArrayList();
    List<String> allGadgetFeatures = Lists.newArrayList("dep1", "dep2", "foo1", "foo2", "foo3");
    FeatureAdminData data = new FeatureAdminData(features, Type.BLACKLIST);
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo1", "foo2", "foo3");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true),
            createMockFeature(gadgetRequiredFeatureNames.get(2), true));
    enabledStore.getContainerAdminData(DEFAULT).addGadgetAdminData(TEST_GADGET,
            new GadgetAdminData(data, null));
    mockRegistryForFeatureAdmin(features, featuresAndDeps,
            allGadgetFeatures, gadgetRequiredFeatureNames);
    mockGadget(allFeatures);
    replay();
    assertTrue(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testAllowedGadgetWhitelist() throws Exception {
    Set<String> features = Sets.newHashSet("foo4", "foo3");
    List<String> featuresAndDeps = Lists.newArrayList("foo4", "dep1", "dep2", "foo3");
    List<String> allGadgetFeatures = Lists.newArrayList("dep1", "dep2", "foo3", "foo4");
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo3", "foo4");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true));
    FeatureAdminData data = new FeatureAdminData(features,Type.WHITELIST);
    enabledStore.getContainerAdminData(DEFAULT).addGadgetAdminData(TEST_GADGET,
            new GadgetAdminData(data, new RpcAdminData()));
    mockRegistryForFeatureAdmin(features, featuresAndDeps,
            allGadgetFeatures, gadgetRequiredFeatureNames);
    mockGadget(allFeatures);
    replay();
    assertTrue(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testDeniedGadgetWhitelist() throws Exception {
    Set<String> features = Sets.newHashSet("foo4", "foo3");
    List<String> featuresAndDeps = Lists.newArrayList("foo4", "dep1", "dep2", "foo3");
    List<String> allGadgetFeatures = Lists.newArrayList("dep1", "dep2", "foo3", "foo4", "foo5");
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo3", "foo4", "foo5");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true),
            createMockFeature(gadgetRequiredFeatureNames.get(2), true));
    FeatureAdminData data = new FeatureAdminData(features,Type.WHITELIST);
    enabledStore.getContainerAdminData(DEFAULT).addGadgetAdminData(TEST_GADGET,
            new GadgetAdminData(data, new RpcAdminData()));
    mockRegistryForFeatureAdmin(features, featuresAndDeps,
            allGadgetFeatures, gadgetRequiredFeatureNames);
    mockGadget(allFeatures);
    replay();
    assertFalse(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testAllowedGadgetBlacklist() throws Exception {
    Set<String> features = Sets.newHashSet("foo5", "foo6");
    List<String> featuresAndDeps = Lists.newArrayList("foo5", "dep1", "dep2", "foo6");
    List<String> allGadgetFeatures = Lists.newArrayList("dep1", "dep2", "foo3", "foo4");
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo3", "foo4");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true));
    FeatureAdminData data = new FeatureAdminData(features,Type.BLACKLIST);
    enabledStore.getContainerAdminData(DEFAULT).addGadgetAdminData(TEST_GADGET,
            new GadgetAdminData(data, null));
    mockRegistryForFeatureAdmin(features, featuresAndDeps,
            allGadgetFeatures, gadgetRequiredFeatureNames);
    mockGadget(allFeatures);
    replay();
    assertTrue(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testDeniedGadgetBlacklist() throws Exception {
    Set<String> features = Sets.newHashSet("foo4", "foo3");
    List<String> featuresAndDeps = Lists.newArrayList("foo5", "dep1", "dep2", "foo6");
    List<String> allGadgetFeatures = Lists.newArrayList("dep1", "dep2", "foo3", "foo4");
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo3", "foo4");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true));
    FeatureAdminData data = new FeatureAdminData(features,Type.BLACKLIST);
    enabledStore.getContainerAdminData(DEFAULT).addGadgetAdminData(TEST_GADGET,
            new GadgetAdminData(data, null));
    mockRegistryForFeatureAdmin(features, featuresAndDeps,
            allGadgetFeatures, gadgetRequiredFeatureNames);
    mockGadget(allFeatures);
    replay();
    assertFalse(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testDeniedOptionalFeature() throws Exception {
    Set<String> features = Sets.newHashSet("foo4", "foo3");
    List<String> featuresAndDeps = Lists.newArrayList("foo4", "dep1", "dep2", "foo3");
    List<String> allGadgetFeatures = Lists.newArrayList("dep1", "dep2", "foo3", "foo4");
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo3", "foo4");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true),
            createMockFeature("foo5", false));
    FeatureAdminData data = new FeatureAdminData(features,Type.WHITELIST);
    enabledStore.getContainerAdminData(DEFAULT).addGadgetAdminData(TEST_GADGET,
            new GadgetAdminData(data, new RpcAdminData()));
    mockRegistryForFeatureAdmin(features, featuresAndDeps,
            allGadgetFeatures, gadgetRequiredFeatureNames);
    mockGadget(allFeatures);
    replay();
    assertTrue(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testFeatureAdminNullGadgetData() throws Exception {
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo3", "foo4");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true));
    mockGadget(allFeatures, DEFAULT, "https://example.com/dontexist.xml");
    replay();
    assertFalse(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testFeatureAdminNullContainerData() throws Exception {
    List<String> gadgetRequiredFeatureNames = Lists.newArrayList("foo3", "foo4");
    List<Feature> allFeatures = Lists.newArrayList(
            createMockFeature(gadgetRequiredFeatureNames.get(0), true),
            createMockFeature(gadgetRequiredFeatureNames.get(1), true));
    mockGadget(allFeatures, "foocontainer");
    replay();
    assertFalse(enabledStore.checkFeatureAdminInfo(mockGadget));
    assertTrue(disabledStore.checkFeatureAdminInfo(mockGadget));
    verify();
  }

  @Test
  public void testIsWhiteListed() throws Exception {
    assertTrue(enabledStore.isWhitelisted(DEFAULT, HOROSCOPE));
    assertTrue(enabledStore.isWhitelisted(DEFAULT, TEST_GADGET));
    assertFalse(enabledStore.isWhitelisted(DEFAULT, "https://example.com/gadget.xml"));
    assertFalse(enabledStore.isWhitelisted("myContainer", HOROSCOPE));
    assertTrue(enabledStore.isWhitelisted(DEFAULT, "http://foo.com/gadget.xml"));
    assertTrue(enabledStore.isWhitelisted(DEFAULT, "http://example.com/gadget.xml"));
    assertTrue(disabledStore.isWhitelisted(DEFAULT, HOROSCOPE));
    assertTrue(disabledStore.isWhitelisted(DEFAULT, TEST_GADGET));
    assertTrue(disabledStore.isWhitelisted("myContainer", HOROSCOPE));
  }

  @Test
  public void testIsAllowedFeature() throws Exception {
    mockGadget(ImmutableList.<Feature> of(), DEFAULT, TODO);
    Feature denied = createMockFeature("setprefs", true);
    Feature allowed = createMockFeature("foo", true);
    replay();
    assertFalse(enabledStore.isAllowedFeature(denied, mockGadget));
    assertTrue(enabledStore.isAllowedFeature(allowed, mockGadget));
    assertTrue(disabledStore.isAllowedFeature(denied, mockGadget));
    assertTrue(disabledStore.isAllowedFeature(allowed, mockGadget));
  }

  @Test
  public void testGetAdditionalRpcServiceIds() throws Exception {
    mockGadget(ImmutableList.<Feature>of(), DEFAULT, "http://example.com/gadget.xml");
    replay();
    assertEquals(Sets.newHashSet("rpc1", "rpc2"),
            enabledStore.getAdditionalRpcServiceIds(mockGadget));
    assertEquals(Sets.newHashSet(),
            disabledStore.getAdditionalRpcServiceIds(mockGadget));

    reset();
    mockGadget(ImmutableList.<Feature>of(), DEFAULT, "https://example.com/gadget.xml");
    replay();
    assertEquals(Sets.newHashSet(),
            enabledStore.getAdditionalRpcServiceIds(mockGadget));
    assertEquals(Sets.newHashSet(),
            disabledStore.getAdditionalRpcServiceIds(mockGadget));

    reset();
    mockGadget(ImmutableList.<Feature>of(), DEFAULT, HOROSCOPE);
    replay();
    assertEquals(Sets.newHashSet(),
            enabledStore.getAdditionalRpcServiceIds(mockGadget));
    assertEquals(Sets.newHashSet(),
            disabledStore.getAdditionalRpcServiceIds(mockGadget));
  }

  private static class FakeContainerConfig extends BasicContainerConfig {
    protected final Map<String, Object> data;

    public FakeContainerConfig(boolean enableFeatureAdministration, boolean enableGadgetWhitelist) {
      data = ImmutableMap
              .<String, Object> builder()
              .put("gadgets.admin.enableFeatureAdministration",
                      new Boolean(enableFeatureAdministration).toString())
              .put("gadgets.admin.enableGadgetWhitelist", new Boolean(enableGadgetWhitelist))
              .build();
    }

    @Override
    public Object getProperty(String container, String name) {
      return data.get(name);
    }
  }

}
