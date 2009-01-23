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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.util.Arrays;

public class HashLockedDomainServiceTest extends EasyMockTestCase {
  private HashLockedDomainService lockedDomainService;
  private final GadgetSpec wantsLocked = makeSpec(true, "http://somehost.com/somegadget.xml");
  private final GadgetSpec notLocked = makeSpec(false, "not-locked");
  private final ContainerConfig requiredConfig = mock(ContainerConfig.class);
  private final ContainerConfig enabledConfig = mock(ContainerConfig.class);

  private static GadgetSpec makeSpec(boolean wantsLocked, String url) {
    String gadgetXml;
    if (wantsLocked) {
      gadgetXml =
          "<Module><ModulePrefs title=''>" +
          "  <Require feature='locked-domain'/>" +
          "</ModulePrefs><Content/></Module>";
    } else {
      gadgetXml = "<Module><ModulePrefs title=''/><Content/></Module>";
    }

    try {
      return new GadgetSpec(Uri.parse(url), gadgetXml);
    } catch (GadgetException e) {
      return null;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    expect(requiredConfig.get(ContainerConfig.DEFAULT_CONTAINER,
        LOCKED_DOMAIN_SUFFIX_KEY)).andReturn("-a.example.com:8080").anyTimes();
    expect(requiredConfig.get(ContainerConfig.DEFAULT_CONTAINER,
        LOCKED_DOMAIN_REQUIRED_KEY)).andReturn("true").anyTimes();
    expect(requiredConfig.getContainers())
        .andReturn(Arrays.asList(ContainerConfig.DEFAULT_CONTAINER)).anyTimes();

    expect(enabledConfig.get(ContainerConfig.DEFAULT_CONTAINER,
        LOCKED_DOMAIN_SUFFIX_KEY)).andReturn("-a.example.com:8080").anyTimes();
    expect(enabledConfig.getContainers())
        .andReturn(Arrays.asList(ContainerConfig.DEFAULT_CONTAINER)).anyTimes();
  }


  public void testDisabledGlobally() {
    replay();

    lockedDomainService = new HashLockedDomainService(requiredConfig, false);
    assertTrue(lockedDomainService.isSafeForOpenProxy("anywhere.com"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", notLocked, "default"));

    lockedDomainService = new HashLockedDomainService(enabledConfig, false);
    assertTrue(lockedDomainService.isSafeForOpenProxy("anywhere.com"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("embed.com", notLocked, "default"));
  }

  public void testEnabledForGadget() {
    replay();

    lockedDomainService = new HashLockedDomainService(enabledConfig, true);
    assertFalse(lockedDomainService.isSafeForOpenProxy("images-a.example.com:8080"));
    assertFalse(lockedDomainService.isSafeForOpenProxy("-a.example.com:8080"));
    assertTrue(lockedDomainService.isSafeForOpenProxy("embed.com"));
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "default"));
    String target = lockedDomainService.getLockedDomainForGadget(wantsLocked, "default");
    assertEquals("8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", target);
  }

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

  public void testMissingConfig() throws Exception {
    ContainerConfig containerMissingConfig = mock(ContainerConfig.class);
    expect(containerMissingConfig.getContainers())
      .andReturn(Arrays.asList(ContainerConfig.DEFAULT_CONTAINER));
    replay();

    lockedDomainService = new HashLockedDomainService(containerMissingConfig, true);
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", wantsLocked, "default"));
    assertTrue(lockedDomainService.gadgetCanRender("www.example.com", notLocked, "default"));
  }

  public void testMultiContainer() throws Exception {
    ContainerConfig inheritsConfig  = mock(ContainerConfig.class);
    expect(inheritsConfig.getContainers())
        .andReturn(Arrays.asList(ContainerConfig.DEFAULT_CONTAINER, "other"));
    expect(inheritsConfig.get(isA(String.class), eq(LOCKED_DOMAIN_REQUIRED_KEY)))
        .andReturn("true").anyTimes();
    expect(inheritsConfig.get(isA(String.class), eq(LOCKED_DOMAIN_SUFFIX_KEY)))
        .andReturn("-a.example.com:8080").anyTimes();
    replay();

    lockedDomainService = new HashLockedDomainService(inheritsConfig, true);
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", wantsLocked, "other"));
    assertFalse(lockedDomainService.gadgetCanRender("www.example.com", notLocked, "other"));
    assertTrue(lockedDomainService.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080", wantsLocked, "other"));
  }
}
