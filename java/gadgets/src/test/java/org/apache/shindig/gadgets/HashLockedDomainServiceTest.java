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

import org.apache.shindig.gadgets.HashLockedDomainService.GadgetReader;

import java.util.Arrays;

public class HashLockedDomainServiceTest extends EasyMockTestCase {

  HashLockedDomainService domainLocker;
  Gadget gadget;
  FakeSpecReader wantsLocked = new FakeSpecReader(
      true, "http://somehost.com/somegadget.xml");
  FakeSpecReader noLocked = new FakeSpecReader(
      false, "http://somehost.com/somegadget.xml");
  ContainerConfig containerEnabledConfig;
  ContainerConfig containerRequiredConfig;

  /**
   * Mocked out spec reader, rather than mocking the whole
   * Gadget object.
   */
  public static class FakeSpecReader extends GadgetReader {
    private boolean wantsLockedDomain;
    private String gadgetUrl;

    public FakeSpecReader(boolean wantsLockedDomain, String gadgetUrl) {
      this.wantsLockedDomain = wantsLockedDomain;
      this.gadgetUrl = gadgetUrl;
    }

    @Override
    protected boolean gadgetWantsLockedDomain(Gadget gadget) {
      return wantsLockedDomain;
    }

    @Override
    protected String getGadgetUrl(Gadget gadget) {
      return gadgetUrl;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    containerRequiredConfig  = mock(ContainerConfig.class);
    expect(containerRequiredConfig.get(ContainerConfig.DEFAULT_CONTAINER,
        LOCKED_DOMAIN_REQUIRED_KEY)).andReturn("true").anyTimes();
    expect(containerRequiredConfig.get(ContainerConfig.DEFAULT_CONTAINER,
        LOCKED_DOMAIN_SUFFIX_KEY)).andReturn("-a.example.com:8080").anyTimes();
    expect(containerRequiredConfig.getContainers())
        .andReturn(Arrays.asList(ContainerConfig.DEFAULT_CONTAINER)).anyTimes();

    containerEnabledConfig = mock(ContainerConfig.class);
    expect(containerEnabledConfig.get(ContainerConfig.DEFAULT_CONTAINER,
        LOCKED_DOMAIN_REQUIRED_KEY)).andReturn("false").anyTimes();
    expect(containerEnabledConfig.get(ContainerConfig.DEFAULT_CONTAINER,
        LOCKED_DOMAIN_SUFFIX_KEY)).andReturn("-a.example.com:8080").anyTimes();
    expect(containerEnabledConfig.getContainers())
        .andReturn(Arrays.asList(ContainerConfig.DEFAULT_CONTAINER)).anyTimes();
  }


  public void testDisabledGlobally() {
    replay();

    domainLocker = new HashLockedDomainService(
        containerRequiredConfig, "embed.com", false);
    assertTrue(domainLocker.embedCanRender("anywhere.com"));
    assertTrue(domainLocker.embedCanRender("embed.com"));
    assertTrue(domainLocker.gadgetCanRender("embed.com", gadget, "default"));

    domainLocker = new HashLockedDomainService(
        containerEnabledConfig, "embed.com", false);
    assertTrue(domainLocker.embedCanRender("anywhere.com"));
    assertTrue(domainLocker.embedCanRender("embed.com"));
    assertTrue(domainLocker.gadgetCanRender("embed.com", gadget, "default"));
  }

  public void testEnabledForGadget() {
    replay();

    domainLocker = new HashLockedDomainService(
        containerEnabledConfig, "embed.com", true);
    assertFalse(domainLocker.embedCanRender("anywhere.com"));
    assertTrue(domainLocker.embedCanRender("embed.com"));
    domainLocker.setSpecReader(wantsLocked);
    assertFalse(domainLocker.gadgetCanRender(
        "www.example.com", gadget, "default"));
    assertTrue(domainLocker.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080",
        gadget,
        "default"));
    String target = domainLocker.getLockedDomainForGadget(
        wantsLocked.getGadgetUrl(gadget), "default");
    assertEquals(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080",
        target);
  }

  public void testNotEnabledForGadget() {
    replay();

    domainLocker = new HashLockedDomainService(
        containerEnabledConfig, "embed.com", true);
    domainLocker.setSpecReader(noLocked);
    assertTrue(domainLocker.gadgetCanRender(
        "www.example.com", gadget, "default"));
    assertFalse(domainLocker.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080",
        gadget,
        "default"));
    assertFalse(domainLocker.gadgetCanRender(
        "foo-a.example.com:8080",
        gadget,
        "default"));
    assertFalse(domainLocker.gadgetCanRender(
        "foo-a.example.com:8080",
        gadget,
        "othercontainer"));
    String target = domainLocker.getLockedDomainForGadget(
        wantsLocked.getGadgetUrl(gadget), "default");
    assertEquals(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080",
        target);
  }

  public void testRequiredForContainer() {
    replay();

    domainLocker = new HashLockedDomainService(
        containerRequiredConfig, "embed.com", true);
    domainLocker.setSpecReader(noLocked);
    assertFalse(domainLocker.gadgetCanRender(
        "www.example.com", gadget, "default"));
    assertTrue(domainLocker.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080",
        gadget,
        "default"));
    String target = domainLocker.getLockedDomainForGadget(
        wantsLocked.getGadgetUrl(gadget), "default");
    assertEquals(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080",
        target);
  }

  public void testMissingConfig() throws Exception {
    ContainerConfig containerMissingConfig = mock(ContainerConfig.class);
    expect(containerMissingConfig.getContainers())
      .andReturn(Arrays.asList(ContainerConfig.DEFAULT_CONTAINER));
    replay();

    domainLocker = new HashLockedDomainService(
        containerMissingConfig, "embed.com", false);
    domainLocker.setSpecReader(wantsLocked);
    assertTrue(domainLocker.gadgetCanRender(
        "www.example.com", gadget, "default"));
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

    domainLocker = new HashLockedDomainService(
        inheritsConfig, "embed.com", true);
    domainLocker.setSpecReader(wantsLocked);
    assertFalse(domainLocker.gadgetCanRender(
        "www.example.com", gadget, "other"));
    assertTrue(domainLocker.gadgetCanRender(
        "8uhr00296d2o3sfhqilj387krjmgjv3v-a.example.com:8080",
        gadget,
        "other"));
  }
}
