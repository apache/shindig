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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GadgetFeatureRegistryTest {
  private GadgetFeatureRegistry registry;
  private static final String FEATURE_NAME = "feature";
  private static final String DEP_NAME = "dependency";
  private static final String CORE_NAME = "core.feature";
  private static final String CONTENT = "var foo = 'bar'";
  private static final String CORE_CONTENT = "var core = 'dependency'";
  private static final String DEP_CONTENT = "var bar ='foo'";
  private static final String[] FEATURE_LIST = new String[] {
    "feature0", "feature1", "feature2", "feature3"
  };


  @Before
  public void setUp() throws Exception {
    // TODO: Add a mock fetcher here and add tests for retrieving remote files
    registry = new GadgetFeatureRegistry(null, null);
    registry.register(makeFeature(CORE_NAME, CORE_CONTENT, null));
  }

  private GadgetFeature makeFeature(String name, String content, String dep)
      throws GadgetException {
    JsLibrary lib = JsLibrary.create(JsLibrary.Type.INLINE, content, name, null);
    List<String> deps = new LinkedList<String>();
    if (deps != null) {
      deps.add(dep);
    }
    return new GadgetFeature(name, Arrays.asList(lib), deps);
  }

  @Test
  public void getLibraries() throws Exception {
    registry.register(makeFeature(DEP_NAME, DEP_CONTENT, null));
    registry.register(makeFeature(FEATURE_NAME, CONTENT, DEP_NAME));

    Collection<GadgetFeature> features
        = registry.getFeatures(Arrays.asList(FEATURE_NAME));

    assertEquals(3, features.size());
    // Order must be preserved.
    Iterator<GadgetFeature> i = features.iterator();
    assertEquals(CORE_NAME, i.next().getName());
    assertEquals(DEP_NAME, i.next().getName());
    assertEquals(FEATURE_NAME, i.next().getName());
  }

  @Test
  public void getUnknownLibraries() throws GadgetException {
    registry.register(makeFeature(FEATURE_NAME, CONTENT, DEP_NAME));
    List<String> unsupported = new ArrayList<String>();
    registry.getFeatures(Arrays.asList(FEATURE_NAME, "FAKE FAKE FAKE"),
                         unsupported);
    assertEquals("FAKE FAKE FAKE", unsupported.get(0));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void getFeaturesUsesCache() throws GadgetException {
    registry.register(makeFeature(DEP_NAME, DEP_CONTENT, null));
    registry.register(makeFeature("feat0", CONTENT, DEP_NAME));
    registry.register(makeFeature("feat1", CONTENT, DEP_NAME));

    Set<String> setKeys = ImmutableSortedSet.of("feat0", "feat1");
    List<String> listKeys = Lists.newLinkedList("feat0", "feat1");
    Collection<String> collectKeys
        = Collections.unmodifiableCollection(Lists.newArrayList("feat0", "feat1"));

    // Fill the cache.
    assertEquals(0, registry.cache.size());
    registry.getFeatures(collectKeys);
    assertEquals(1, registry.cache.size());

    Collection<GadgetFeature> setFeatures = registry.getFeatures(setKeys);
    assertEquals(1, registry.cache.size());
    Collection<GadgetFeature> listFeatures = registry.getFeatures(listKeys);
    assertEquals(1, registry.cache.size());
    Collection<GadgetFeature> collectFeatures = registry.getFeatures(collectKeys);
    assertEquals(1, registry.cache.size());
    assertSame(listFeatures, collectFeatures);
    assertSame(setFeatures, listFeatures);
  }

  @Test
  public void getAllFeatures() throws Exception {
    for (String feature : FEATURE_LIST) {
      registry.register(makeFeature(feature, CONTENT, DEP_NAME));
    }

    Set<String> found = new HashSet<String>();
    for (GadgetFeature feature : registry.getAllFeatures()) {
      found.add(feature.getName());
    }
    for (String feature : FEATURE_LIST) {
      assertTrue(feature + " not returned.", found.contains(feature));
    }
  }
}
