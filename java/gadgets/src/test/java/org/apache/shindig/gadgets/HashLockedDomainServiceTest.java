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
package org.apache.shindig.gadgets;

import static org.apache.shindig.gadgets.HashLockedDomainService.LOCKED_DOMAIN_REQUIRED_KEY;
import static org.apache.shindig.gadgets.HashLockedDomainService.LOCKED_DOMAIN_SUFFIX_KEY;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HashLockedDomainServiceTest extends EasyMockTestCase {

  private HashLockedDomainService lockedDomainService;
  private Gadget wantsLocked = null;
  private Gadget notLocked = null;
  private Gadget wantsSecurityToken = null;
  private Gadget wantsBoth = null;
  private ContainerConfig requiredConfig;
  private ContainerConfig enabledConfig;

  @SuppressWarnings("unchecked")
  private Gadget makeGadget(boolean wantsLocked, boolean wantsSecurityToken, String url) {

    List<String> gadgetFeatures = Lists.newArrayList();
    String requires = "";
    if (wantsLocked || wantsSecurityToken) {
      gadgetFeatures.add("locked-domain");
      if (wantsLocked) {
        requires += "  <Require feature='locked-domain'/>";
      }
      if (wantsSecurityToken) {
        requires += "  <Require feature='security-token'/>";
        gadgetFeatures.add("security-token");
      }
    }
    
    String gadgetXml = "<Module><ModulePrefs title=''>" + requires + "</ModulePrefs><Content/></Module>";

    GadgetSpec spec = null;
    try {
      spec = new GadgetSpec(Uri.parse(url), gadgetXml);
    } catch (GadgetException e) {
      return null;
    }

    FeatureRegistry registry = mock(FeatureRegistry.class);
    expect(registry.getFeatures(isA(Collection.class))).andReturn(gadgetFeatures).anyTimes();
    return new Gadget().setSpec(spec).setGadgetFeatureRegistry(registry);
  }

  @Before
  public void setUp() throws Exception {
    requiredConfig = new BasicContainerConfig();
    requiredConfig.newTransaction().addContainer(
        makeContainer(ContainerConfig.DEFAULT_CONTAINER, LOCKED_DOMAIN_SUFFIX_KEY,
            "-a.example.com:8080", LOCKED_DOMAIN_REQUIRED_KEY, true)).commit();

    enabledConfig = new BasicContainerConfig();
    enabledConfig.newTransaction().addContainer(
        makeContainer(ContainerConfig.DEFAULT_CONTAINER, LOCKED_DOMAIN_SUFFIX_KEY,
            "-a.example.com:8080")).commit();

    wantsLocked = makeGadget(true, false, "http://somehost.com/somegadget.xml");
    notLocked = makeGadget(false, false, "not-locked");
    wantsSecurityToken = makeGadget(false, true, "http://somehost.com/securitytoken.xml");
    wantsBoth =
        makeGadget(true, true, "http://somehost.com/tokenandlocked.xml");
  }

  @Test
  public void testDisabledGlobally() {
    replay();

    lockedDomainService = new HashLockedDomainService(requiredConfig, false);
    assertTrue(lockedDomainService.isSafeForOpenProxy("anywhere.com"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", notLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsSecurityToken, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsBoth, "default"));

    lockedDomainService = new HashLockedDomainService(enabledConfig, false);
    assertTrue(lockedDomainService.isSafeForOpenProxy("anywhere.com"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", notLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsSecurityToken, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsBoth, "default"));
  }

  @Test
  public void testEnabledForGadget() {
    replay();

    lockedDomainService = new HashLockedDomainService(enabledConfig, true);
    assertFalse(lockedDomainService.isSafeForOpenProxy("images-a.example.com:8080"));
    assertFalse(lockedDomainService.isSafeForOpenProxy("-a.example.com:8080"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "default"));
    assertFalse(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsSecurityToken, "default"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "h2nlf2a2dqou2lul3n50jb4v7e8t34kc-a.example.com:8080", wantsBoth, "default"));
    
    String target = lockedDomainService.getLockedDomainForGadget(wantsLocked, "default");
    assertEquals("8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", target);
    
    target = lockedDomainService.getLockedDomainForGadget(wantsBoth, "default");
    assertEquals("h2nlf2a2dqou2lul3n50jb4v7e8t34kc-a.example.com:8080", target);
    
    lockedDomainService.setLockSecurityTokens(true);
    assertTrue(lockedDomainService.gadgetCanRender(
        "lrrq12l8s5flpqcjoj1h1872lp9p93nk-a.example.com:8080", wantsSecurityToken, "default"));
    target = lockedDomainService.getLockedDomainForGadget(wantsSecurityToken, "default");
    assertEquals("lrrq12l8s5flpqcjoj1h1872lp9p93nk-a.example.com:8080", target);
    
    // Direct includes work as before.
    target = lockedDomainService.getLockedDomainForGadget(wantsLocked, "default");
    assertEquals("8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", target);
    
    target = lockedDomainService.getLockedDomainForGadget(wantsBoth, "default");
    assertEquals("h2nlf2a2dqou2lul3n50jb4v7e8t34kc-a.example.com:8080", target);
  }

  @Test
  public void testNotEnabledForGadget() {
    replay();

    lockedDomainService = new HashLockedDomainService(enabledConfig, true);

    assertFalse(lockedDomainService.isSafeForOpenProxy("images-a.example.com:8080"));
    assertFalse(lockedDomainService.isSafeForOpenProxy("-a.example.com:8080"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));

    assertTrue(lockedDomainService.gadgetCanRender("www.example.com", notLocked, "default"));
    assertFalse(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", notLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "auvn86n7q0l4ju2tq5cq8akotcjlda66-a.example.com:8080", notLocked, "default"));
    assertNull(lockedDomainService.getLockedDomainForGadget(notLocked, "default"));
  }

  @Test
  public void testRequiredForContainer() {
    replay();

    lockedDomainService = new HashLockedDomainService(requiredConfig, true);

    assertFalse(lockedDomainService.isSafeForOpenProxy("images-a.example.com:8080"));
    assertFalse(lockedDomainService.isSafeForOpenProxy("-a.example.com:8080"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));

    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", wantsLocked, "default"));
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", notLocked, "default"));

    String target = lockedDomainService.getLockedDomainForGadget(wantsLocked, "default");
    assertEquals("8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", target);
    target = lockedDomainService.getLockedDomainForGadget(notLocked, "default");
    assertEquals("auvn86n7q0l4ju2tq5cq8akotcjlda66-a.example.com:8080", target);

    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "default"));
    assertFalse(lockedDomainService.gadgetCanRender(
        "auvn86n7q0l4ju2tq5cq8akotcjlda66-a.example.com:8080", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "auvn86n7q0l4ju2tq5cq8akotcjlda66-a.example.com:8080", notLocked, "default"));
    assertFalse(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", notLocked, "default"));

  }

  @Test
  public void testMissingConfig() throws Exception {
    ContainerConfig containerMissingConfig = new BasicContainerConfig();
    containerMissingConfig.newTransaction().addContainer(makeContainer(ContainerConfig.DEFAULT_CONTAINER)).commit();

    lockedDomainService = new HashLockedDomainService(containerMissingConfig, true);
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("www.example.com", notLocked, "default"));
  }

  @Test
  public void testMultiContainer() throws Exception {
    ContainerConfig inheritsConfig = new BasicContainerConfig();
    inheritsConfig
        .newTransaction()
        .addContainer(
            makeContainer(ContainerConfig.DEFAULT_CONTAINER, LOCKED_DOMAIN_SUFFIX_KEY,
                "-a.example.com:8080", LOCKED_DOMAIN_REQUIRED_KEY, true))
        .addContainer(makeContainer("other"))
        .commit();

    lockedDomainService = new HashLockedDomainService(inheritsConfig, true);
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", wantsLocked, "other"));
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", notLocked, "other"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "other"));
  }
  
  @Test
  public void testConfigurationChanged() throws Exception {
    ContainerConfig config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(makeContainer(ContainerConfig.DEFAULT_CONTAINER))
        .addContainer(
            makeContainer("container", LOCKED_DOMAIN_REQUIRED_KEY, true, LOCKED_DOMAIN_SUFFIX_KEY,
                "-a.example.com:8080"))
        .commit();

    lockedDomainService = new HashLockedDomainService(config, true);
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "container"));
    assertFalse(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "other"));

    config.newTransaction().addContainer(makeContainer(
        "other", LOCKED_DOMAIN_REQUIRED_KEY, true, LOCKED_DOMAIN_SUFFIX_KEY, "-a.example.com:8080"))
        .commit();
    lockedDomainService.containersChanged(
        config, ImmutableSet.of("other"), ImmutableSet.<String>of());
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "container"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "other"));

    config.newTransaction().removeContainer("container").commit();
    assertFalse(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "container"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "other"));
  }
  
  private Map<String, Object> makeContainer(String name, Object... props) {
    ImmutableMap.Builder<String, Object> builder =
        ImmutableMap.<String, Object>builder().put(ContainerConfig.CONTAINER_KEY, name);
    for (int i = 0; i < props.length; i += 2) {
      builder.put((String) props[i], props[i + 1]);
    }
    return builder.build();
  }
}
