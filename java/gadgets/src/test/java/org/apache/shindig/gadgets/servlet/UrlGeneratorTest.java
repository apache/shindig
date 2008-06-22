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
package org.apache.shindig.gadgets.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.gadgets.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for UrlGenerator.
 */
public class UrlGeneratorTest {
  private final static String IFR_PREFIX = "shindig/eye-frame?";
  private final static String JS_PREFIX = "get-together/livescript/";

  private final ServletTestFixture fixture = new ServletTestFixture();
  private final GadgetFeatureRegistry featureRegistry = fixture.mock(GadgetFeatureRegistry.class);
  private final ContainerConfig containerConfig = fixture.mock(ContainerConfig.class);
  private final GadgetContext context = fixture.mock(GadgetContext.class);

  @Before
  public void setUp() throws Exception {
    expect(featureRegistry.getAllFeatures()).andReturn(new ArrayList<GadgetFeature>());
  }

  @Test
  public void getBundledJsParam() throws Exception {
    List<String> features = new ArrayList<String>();
    features.add("foo");
    features.add("bar");
    expect(context.getContainer()).andReturn("shindig");
    expect(context.getDebug()).andReturn(true);
    fixture.replay();

    UrlGenerator urlGenerator
        = new UrlGenerator(IFR_PREFIX, JS_PREFIX, featureRegistry, containerConfig);
    String jsParam = urlGenerator.getBundledJsParam(features, context);

    assertTrue(jsParam.matches("foo:bar\\.js\\?v=[0-9a-zA-Z]*&container=shindig&debug=1"));
  }

  @Test
  public void getBundledJsParamWithBadFeatureName() throws Exception {
    List<String> features = new ArrayList<String>();
    features.add("foo!");
    features.add("bar");
    expect(context.getContainer()).andReturn("opensocial.org");
    expect(context.getDebug()).andReturn(true);
    fixture.replay();

    UrlGenerator urlGenerator
        = new UrlGenerator(IFR_PREFIX, JS_PREFIX, featureRegistry, containerConfig);
    String jsParam = urlGenerator.getBundledJsParam(features, context);

    assertTrue(jsParam.matches("bar\\.js\\?v=[0-9a-zA-Z]*&container=opensocial.org&debug=1"));
  }

  @Test
  public void getBundledJsParamWithNoFeatures() throws Exception {
    List<String> features = new ArrayList<String>();
    expect(context.getContainer()).andReturn("apache.org");
    expect(context.getDebug()).andReturn(false);
    fixture.replay();

    UrlGenerator urlGenerator
        = new UrlGenerator(IFR_PREFIX, JS_PREFIX, featureRegistry, containerConfig);
    String jsParam = urlGenerator.getBundledJsParam(features, context);

    assertTrue(jsParam.matches("core\\.js\\?v=[0-9a-zA-Z]*&container=apache.org&debug=0"));
  }

  @Test
  public void getBundledJsUrl() throws Exception {
    List<String> features = new ArrayList<String>();
    expect(context.getContainer()).andReturn("myhibebfacekut");
    expect(context.getDebug()).andReturn(false);
    fixture.replay();

    UrlGenerator urlGenerator
        = new UrlGenerator(IFR_PREFIX, JS_PREFIX, featureRegistry, containerConfig);
    String jsParam = urlGenerator.getBundledJsUrl(features, context);

    assertTrue(
        jsParam.matches(JS_PREFIX + "core\\.js\\?v=[0-9a-zA-Z]*&container=myhibebfacekut&debug=0"));
  }

  // TODO: iframe output tests.
}
