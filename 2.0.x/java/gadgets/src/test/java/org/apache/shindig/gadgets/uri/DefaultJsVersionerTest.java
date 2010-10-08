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
package org.apache.shindig.gadgets.uri;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import com.google.common.collect.Lists;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;

import org.easymock.EasyMock;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

/**
 * Tests JS versioner. Ensures that it returns a non-null String which
 * gets appropriately cached and differs when JS content differs.
 */
public class DefaultJsVersionerTest {
  private static final Uri URI = Uri.parse("http://apache.org/gadget.xml");
  private static final String CONTAINER = "container";
  
  private DefaultJsVersioner versioner;
  private FeatureRegistry registry;

  @Before
  public void setUp() {
    registry = createMock(FeatureRegistry.class);
    versioner = new DefaultJsVersioner(registry);
  }
  
  @Test
  public void versionCached() {
    String feature = "feature1";
    expectReq(feature, "content");
    replay(registry);
    Collection<String> libs = Lists.newArrayList(feature);
    String version = versioner.version(URI, CONTAINER, libs);
    assertNotNull(version);
    String versionAgain = versioner.version(URI, CONTAINER, libs);
    assertSame(version, versionAgain);
    verify(registry);
  }
  
  @Test
  public void versionDifferentForDifferentFeatures() {
    String feature1 = "feature1";
    String feature2 = "feature2";
    expectReq(feature1, "content1");
    expectReq(feature2, "content2");
    replay(registry);
    Collection<String> libs1 = Lists.newArrayList(feature1);
    String version1 = versioner.version(URI, CONTAINER, libs1);
    Collection<String> libs2 = Lists.newArrayList(feature2);
    String version2 = versioner.version(URI, CONTAINER, libs2);
    assertNotNull(version1);
    assertNotNull(version2);
    assertFalse(version1.equals(version2));
    verify(registry);
  }
  
  @Test
  public void validateMismatch() {
    String feature = "feature1";
    expectReq(feature, "content");
    replay(registry);
    Collection<String> libs = Lists.newArrayList(feature);
    String version = versioner.version(URI, CONTAINER, libs);
    assertNotNull(version);
    UriStatus status = versioner.validate(URI, CONTAINER, libs, version + "-nomatch");
    assertEquals(UriStatus.INVALID_VERSION, status);
    verify(registry);
  }
  
  @Test
  public void validateNull() {
    String feature = "feature1";
    expectReq(feature, "content");
    replay(registry);
    Collection<String> libs = Lists.newArrayList(feature);
    String version = versioner.version(URI, CONTAINER, libs);
    assertNotNull(version);
    UriStatus status = versioner.validate(URI, CONTAINER, libs, null);
    assertEquals(UriStatus.VALID_UNVERSIONED, status);
    verify(registry);
  }
  
  @Test
  public void validateEmpty() {
    String feature = "feature1";
    expectReq(feature, "content");
    replay(registry);
    Collection<String> libs = Lists.newArrayList(feature);
    String version = versioner.version(URI, CONTAINER, libs);
    assertNotNull(version);
    UriStatus status = versioner.validate(URI, CONTAINER, libs, "");
    assertEquals(UriStatus.VALID_UNVERSIONED, status);
    verify(registry);
  }
  
  @Test
  public void createAndValidateVersion() {
    String feature = "feature1";
    expectReq(feature, "content");
    replay(registry);
    Collection<String> libs = Lists.newArrayList(feature);
    String version = versioner.version(URI, CONTAINER, libs);
    assertNotNull(version);
    UriStatus status = versioner.validate(URI, CONTAINER, libs, version);
    assertEquals(UriStatus.VALID_VERSIONED, status);
    verify(registry);
  }
  
  private void expectReq(String feature, String content) {
    FeatureResource resource = new FeatureResource.Simple(content, "");
    Collection<String> libs = Lists.newArrayList(feature);
    List<FeatureResource> resources = Lists.newArrayList(resource);
    expect(registry.getFeatureResources(isA(GadgetContext.class), eq(libs),
        EasyMock.<List<String>>isNull())).andReturn(resources).anyTimes();
  }
}
