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
package org.apache.shindig.gadgets.js;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.config.DefaultConfigProcessor;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class ConfigInjectionProcessorTest {
  private static final List<String> EMPTY_LIST = ImmutableList.of();
  private static final String HOST = "myHost";
  private static final String BASE_CODE = "code\n";
  private static final String CONTAINER = "container";
  private IMocksControl control;
  private JsUri jsUri;
  private JsRequest request;
  private FeatureRegistry registry;
  private ContainerConfig containerConfig;
  private Map<String, ConfigContributor> configContributors;
  private ConfigInjectionProcessor processor;

  @Before
  public void setUp() throws Exception {
    control = createControl();
    jsUri = control.createMock(JsUri.class);
    request = control.createMock(JsRequest.class);
    registry = control.createMock(FeatureRegistry.class);
    containerConfig = control.createMock(ContainerConfig.class);
    configContributors = Maps.newHashMap();
    FeatureRegistryProvider registryProvider = new FeatureRegistryProvider() {
      public FeatureRegistry get(String repository) {
        return registry;
      }
    };

    processor = new ConfigInjectionProcessor(registryProvider,
        new DefaultConfigProcessor(configContributors, containerConfig));
  }

  @Test
  public void gadgetGetsNothing() throws Exception {
    JsResponseBuilder builder = prepareRequestReturnBuilder(RenderingContext.GADGET);
    control.replay();
    assertTrue(processor.process(request, builder));
    control.verify();
    assertEquals(BASE_CODE, builder.build().toJsString());
  }

  @Test
  public void containerNoFeaturesDoesNothing() throws Exception {
    checkNoFeaturesDoesNothing(RenderingContext.CONTAINER);
  }

  @Test
  public void configuredNoFeaturesDoesNothing() throws Exception {
    checkNoFeaturesDoesNothing(RenderingContext.CONFIGURED_GADGET);
  }

  private void checkNoFeaturesDoesNothing(RenderingContext ctx) throws Exception {
    JsResponseBuilder builder = prepareRequestReturnBuilder(ctx);
    expect(containerConfig.getMap(CONTAINER, ConfigInjectionProcessor.GADGETS_FEATURES_KEY))
        .andReturn(null);
    List<String> libs = ImmutableList.of();
    expect(jsUri.getLibs()).andReturn(libs);
    expect(jsUri.getLoadedLibs()).andReturn(EMPTY_LIST);
    expect(registry.getFeatures(libs)).andReturn(libs);
    expect(request.getHost()).andReturn("host");
    control.replay();
    assertTrue(processor.process(request, builder));
    control.verify();
    assertEquals(BASE_CODE, builder.build().toJsString());
  }

  @Test
  public void containerNoMatchingFeaturesDoesNothing() throws Exception {
    checkNoMatchingFeaturesDoesNothing(RenderingContext.CONTAINER);
  }

  @Test
  public void configuredNoMatchingFeaturesDoesNothing() throws Exception {
    checkNoMatchingFeaturesDoesNothing(RenderingContext.CONFIGURED_GADGET);
  }

  private void checkNoMatchingFeaturesDoesNothing(RenderingContext ctx) throws Exception {
    JsResponseBuilder builder = prepareRequestReturnBuilder(ctx);
    Map<String, Object> baseConfig = Maps.newHashMap();
    baseConfig.put("feature1", "config1");
    Map<String, String> f2MapConfig = Maps.newHashMap();
    f2MapConfig.put("key1", "val1");
    f2MapConfig.put("key2", "val2");
    baseConfig.put("feature2", f2MapConfig);
    expect(containerConfig.getMap(CONTAINER, ConfigInjectionProcessor.GADGETS_FEATURES_KEY))
        .andReturn(baseConfig);
    List<String> libs = ImmutableList.of("lib1", "lib2");
    expect(jsUri.getLibs()).andReturn(libs);
    expect(jsUri.getLoadedLibs()).andReturn(EMPTY_LIST);
    expect(registry.getFeatures(libs)).andReturn(libs);
    expect(request.getHost()).andReturn("host");
    control.replay();
    assertTrue(processor.process(request, builder));
    control.verify();
    assertEquals(BASE_CODE, builder.build().toJsString());
  }

  @Test
  public void containerNoContributorsGetsBase() throws Exception {
    checkNoContributorsGetsBase(RenderingContext.CONTAINER);
  }

  @Test
  public void configuredNoContributorsGetsBase() throws Exception {
    checkNoContributorsGetsBase(RenderingContext.CONFIGURED_GADGET);
  }

  private void checkNoContributorsGetsBase(RenderingContext ctx) throws Exception {
    checkInjectConfig(ctx, false);
  }

  @Test
  public void containerModeInjectConfig() throws Exception {
    checkInjectConfig(RenderingContext.CONTAINER);
  }

  @Test
  public void configuredModeInjectConfig() throws Exception {
    checkInjectConfig(RenderingContext.CONFIGURED_GADGET);
  }

  private void checkInjectConfig(RenderingContext ctx) throws Exception {
    checkInjectConfig(ctx, true);
  }

  private void checkInjectConfig(RenderingContext ctx, boolean extraContrib) throws Exception {
    JsResponseBuilder builder = prepareRequestReturnBuilder(ctx);
    Map<String, Object> baseConfig = Maps.newHashMap();
    baseConfig.put("feature1", "config1");
    Map<String, String> f2MapConfig = Maps.newHashMap();
    f2MapConfig.put("key1", "val1");
    f2MapConfig.put("key2", "val2");
    baseConfig.put("feature2", f2MapConfig);
    baseConfig.put("feature3", "contributorListens");
    baseConfig.put("feature4", "unused");
    expect(containerConfig.getMap(CONTAINER, ConfigInjectionProcessor.GADGETS_FEATURES_KEY))
        .andReturn(baseConfig);
    expect(request.getHost()).andReturn(HOST).anyTimes();
    ImmutableList.Builder<String> libsBuilder =
        ImmutableList.<String>builder().add(ConfigInjectionProcessor.CONFIG_FEATURE,
          "feature1", "feature2");
    if (extraContrib) {
      libsBuilder.add("feature3");
      ConfigContributor cc = control.createMock(ConfigContributor.class);
      Capture<Map<String, Object>> captureConfig = new Capture<Map<String, Object>>();
      cc.contribute(capture(captureConfig), eq(CONTAINER), eq(HOST));
      expectLastCall().andAnswer(new IAnswer<Void>() {
        @SuppressWarnings("unchecked")
        public Void answer() throws Throwable {
          Map<String, Object> config = (Map<String, Object>)getCurrentArguments()[0];
          String f3Value = (String)config.get("feature3");
          config.put("feature3", f3Value + ":MODIFIED");
          return null;
        }
      });
      configContributors.put("feature3", cc);
    }
    List<String> libs = libsBuilder.build();
    expect(jsUri.getLibs()).andReturn(libs);
    expect(jsUri.getLoadedLibs()).andReturn(EMPTY_LIST);
    expect(registry.getFeatures(libs)).andReturn(libs);

    control.replay();
    assertTrue(processor.process(request, builder));
    control.verify();
    String jsCode = builder.build().toJsString();
    String baseMatch = "window['___jsl'] = window['___jsl'] || {};" +
        "(window['___jsl']['ci'] = (window['___jsl']['ci'] || [])).push(";
    assertTrue(jsCode.startsWith(baseMatch));
    String endMatch = ");\n";
    assertTrue(jsCode.endsWith(endMatch));
    String injectedConfig = jsCode.substring(baseMatch.length(),
        jsCode.length() - endMatch.length());

    // Convert to JSON object to bypass ordering issues.
    // This is bulky but works. There's probably a better way.
    JSONObject configObj = new JSONObject(injectedConfig);
    JSONObject expected = new JSONObject();
    expected.put("feature1", "config1");
    JSONObject subConfig = new JSONObject();
    subConfig.put("key1", "val1");
    subConfig.put("key2", "val2");
    expected.put("feature2", subConfig);
    if (extraContrib) {
      expected.put("feature3", "contributorListens:MODIFIED");
    }
    assertEquals(expected.length(), configObj.length());
    assertEquals(expected.get("feature1").toString(), configObj.get("feature1").toString());
    assertEquals(expected.get("feature2").toString(), configObj.get("feature2").toString());
    if (extraContrib) {
      assertEquals(expected.get("feature3").toString(), configObj.get("feature3").toString());
    }
  }

  private JsResponseBuilder prepareRequestReturnBuilder(RenderingContext ctx) {
    expect(jsUri.getContext()).andReturn(ctx);
    expect(jsUri.getContainer()).andReturn(CONTAINER);
    expect(jsUri.isDebug()).andReturn(false);
    expect(jsUri.getRepository()).andReturn(null);
    expect(request.getJsUri()).andReturn(jsUri);
    return new JsResponseBuilder().appendJs(BASE_CODE, "source");
  }

  @Test
  public void newGlobalConfigAdded() throws Exception {
    List<String> requested = ImmutableList.of("reqfeature1", "reqfeature2", "already1");
    List<String> alreadyHas = ImmutableList.of("already1");
    expect(jsUri.getLibs()).andReturn(requested);
    expect(jsUri.getLoadedLibs()).andReturn(alreadyHas);
    Map<String, Object> config =
        ImmutableMap.<String, Object>of("reqfeature1", "reqval1", "already1", "alval1");
    expect(containerConfig.getMap(CONTAINER, ConfigInjectionProcessor.GADGETS_FEATURES_KEY))
        .andReturn(config);
    expect(request.getHost()).andReturn(HOST).anyTimes();
    expect(registry.getFeatures(requested)).andReturn(requested);

    JsResponseBuilder builder = prepareRequestReturnBuilder(RenderingContext.CONFIGURED_GADGET);

    ConfigContributor cc = control.createMock(ConfigContributor.class);
    Capture<Map<String, Object>> captureConfig = new Capture<Map<String, Object>>();
    cc.contribute(capture(captureConfig), eq(CONTAINER), eq(HOST));
    expectLastCall().andAnswer(new IAnswer<Void>() {
      @SuppressWarnings("unchecked")
      public Void answer() throws Throwable {
        Map<String, Object> config = (Map<String, Object>)getCurrentArguments()[0];
        String f3Value = (String)config.get("reqfeature1");
        config.put("reqfeature1", f3Value + ":MODIFIED");
        return null;
      }
    });
    configContributors.put("reqfeature1", cc);

    control.replay();
    assertTrue(processor.process(request, builder));
    control.verify();

    String jsCode = builder.build().toJsString();
    String startCode = BASE_CODE + "window['___cfg']=";
    assertTrue(jsCode.startsWith(startCode));
    String json = jsCode.substring(startCode.length(), jsCode.length() - ";\n".length());
    JSONObject configObj = new JSONObject(json);
    assertEquals(1, configObj.names().length());
    assertEquals("reqval1:MODIFIED", configObj.getString("reqfeature1"));
  }
}
