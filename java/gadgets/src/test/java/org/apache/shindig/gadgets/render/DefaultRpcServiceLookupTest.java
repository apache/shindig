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
package org.apache.shindig.gadgets.render;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.apache.shindig.gadgets.http.BasicHttpFetcher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultRpcServiceLookupTest extends Assert {

  private DefaultRpcServiceLookup svcLookup;
  private String socialEndpoint;
  private String host;

  @Before
  public void setUp() throws Exception {
    svcLookup = new DefaultRpcServiceLookup(new DefaultServiceFetcher(null, new BasicHttpFetcher(null)),
                                            60l);
    socialEndpoint = "http://localhost:8080/social/rpc";
    host = "localhost:8080";
  }

  @Test
  public void testGetServicesForContainer_Empty() throws Exception {
    String container = "ig";
    Multimap<String, String> services = svcLookup.getServicesFor(container, host);
    assertEquals(0, services.size());
  }

  @Test
  public void testGetServicesForContainer_Null() throws Exception {
    String container = null;
    Multimap<String, String> services = svcLookup.getServicesFor(container, host);
    assertEquals(0, services.size());
  }

  @Test
  public void testGetServicesForContainer_OneContainerOneService() throws Exception {
    ImmutableSet<String> expectedServiceMethods = ImmutableSet.of("system.listMethods");
    LinkedHashMultimap<String, String> expectedServices = LinkedHashMultimap.create();
    expectedServices.putAll(socialEndpoint, expectedServiceMethods);
    String container = "ig";
    svcLookup.setServicesFor(container, expectedServices);

    Multimap<String, String> actualServices = svcLookup.getServicesFor(container, host);
    assertEquals(1, actualServices.size());
    assertTrue(actualServices.containsKey(socialEndpoint));
    Set<String> actualServiceMethods = (Set<String>) actualServices.get(socialEndpoint);
    assertEquals(expectedServiceMethods, actualServiceMethods);
  }

  @Test
  public void testGetServicesForContainer_OneContainerTwoServices() throws Exception {
    Set<String> expectedServiceMethods = Sets.newHashSet("system.listMethods", "people.get",
            "people.update", "people.create", "people.delete");

    LinkedHashMultimap<String, String> expectedServices = LinkedHashMultimap.create();
    expectedServices.putAll(socialEndpoint, expectedServiceMethods);

    String container = "ig";
    svcLookup.setServicesFor(container, expectedServices);

    assertServiceHasCorrectConfig(socialEndpoint, expectedServiceMethods, container, 1);
  }

  @Test
  public void testGetServiceForContainer_TwoContainersOneEndpoint() throws Exception {
    String socialEndpoint2 = "http://localhost:8080/api/rpc";
    Set<String> expectedServiceMethods = Sets.newHashSet("system.listMethods", "people.get",
            "people.update", "people.create", "people.delete");
    Set<String> expectedServiceMethods2 = Sets.newHashSet("cache.invalidate");

    LinkedHashMultimap<String, String> expectedServices = LinkedHashMultimap.create();
    expectedServices.putAll(socialEndpoint, expectedServiceMethods);

    LinkedHashMultimap<String, String> expectedServices2 = LinkedHashMultimap.create();
    expectedServices2.putAll(socialEndpoint2, expectedServiceMethods2);

    String container = "ig";
    String container2 = "gm";
    svcLookup.setServicesFor(container, expectedServices);
    svcLookup.setServicesFor(container2, expectedServices2);

    assertServiceHasCorrectConfig(socialEndpoint, expectedServiceMethods, container, 1);
    assertServiceHasCorrectConfig(socialEndpoint2, expectedServiceMethods2, container2, 1);
  }

  private void assertServiceHasCorrectConfig(String socialEndpoint,
          Set<String> expectedServiceMethods, String container, int expectedServiceCount) {
    Multimap<String, String> actualServices = svcLookup.getServicesFor(container, host);
    assertEquals(expectedServiceCount, actualServices.keySet().size());
    assertTrue(actualServices.containsKey(socialEndpoint));
    Set<String> actualServiceMethods = (Set<String>) actualServices.get(socialEndpoint);
    assertEquals(expectedServiceMethods, actualServiceMethods);
  }
}
