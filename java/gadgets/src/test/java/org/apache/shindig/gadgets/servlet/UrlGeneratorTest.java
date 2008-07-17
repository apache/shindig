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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Maps;

import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;

import junitx.framework.StringAssert;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tests for UrlGenerator.
 */
public class UrlGeneratorTest {
  private final static String IFR_PREFIX = "shindig/eye-frame?";
  private final static String JS_PREFIX = "get-together/livescript/";
  private final static String SPEC_URL = "http://example.org/gadget.xml";
  private final static String TYPE_URL_HREF_HOST = "opensocial.org";
  private final static String TYPE_URL_HREF_PATH = "/app/foo";
  private final static String TYPE_URL_HREF_QUERY = "foo=bar&bar=baz";
  private final static String TYPE_URL_HREF
      = "http://" + TYPE_URL_HREF_HOST + TYPE_URL_HREF_PATH + '?' + TYPE_URL_HREF_QUERY;
  private final static String UP_NAME = "user-pref-name";
  private final static String UP_VALUE = "user-pref-value";
  private final static String CONTAINER = "shindig";
  private final static String VIEW = "canvas";
  private final static int MODULE_ID = 3435;

  private final ServletTestFixture fixture = new ServletTestFixture();
  private final GadgetFeatureRegistry featureRegistry = fixture.mock(GadgetFeatureRegistry.class);
  private final ContainerConfig containerConfig = fixture.mock(ContainerConfig.class);
  private final GadgetContext context = fixture.mock(GadgetContext.class);
  private UrlGenerator urlGenerator;

  @Before
  public void setUp() throws Exception {
    expect(featureRegistry.getAllFeatures()).andReturn(new ArrayList<GadgetFeature>());
    fixture.replay();
    urlGenerator = new UrlGenerator(IFR_PREFIX, JS_PREFIX, featureRegistry, containerConfig);
    fixture.reset();

    expect(context.getContainer()).andReturn(CONTAINER).anyTimes();
    expect(context.getUrl()).andReturn(URI.create(SPEC_URL)).anyTimes();
    Map<String, String> prefMap = Maps.newHashMap();
    prefMap.put(UP_NAME, UP_VALUE);
    UserPrefs prefs = new UserPrefs(prefMap);
    expect(context.getUserPrefs()).andReturn(prefs).anyTimes();
    expect(context.getLocale()).andReturn(Locale.getDefault()).anyTimes();
    expect(context.getModuleId()).andReturn(MODULE_ID).anyTimes();
    expect(context.getView()).andReturn(VIEW).anyTimes();
  }

  @Test
  public void getBundledJsParam() throws Exception {
    List<String> features = new ArrayList<String>();
    features.add("foo");
    features.add("bar");
    expect(context.getDebug()).andReturn(true);
    fixture.replay();

    String jsParam = urlGenerator.getBundledJsParam(features, context);

    assertTrue(
        jsParam.matches("foo:bar\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=1"));
  }

  @Test
  public void getBundledJsParamWithBadFeatureName() throws Exception {
    List<String> features = new ArrayList<String>();
    features.add("foo!");
    features.add("bar");
    expect(context.getDebug()).andReturn(true);
    fixture.replay();

    String jsParam = urlGenerator.getBundledJsParam(features, context);

    assertTrue(jsParam.matches("bar\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=1"));
  }

  @Test
  public void getBundledJsParamWithNoFeatures() throws Exception {
    List<String> features = new ArrayList<String>();
    expect(context.getDebug()).andReturn(false);
    fixture.replay();

    String jsParam = urlGenerator.getBundledJsParam(features, context);

    assertTrue(jsParam.matches("core\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=0"));
  }

  @Test
  public void getBundledJsUrl() throws Exception {
    List<String> features = new ArrayList<String>();
    expect(context.getDebug()).andReturn(false);
    fixture.replay();

    String jsParam = urlGenerator.getBundledJsUrl(features, context);

    assertTrue(jsParam.matches(
        JS_PREFIX + "core\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=0"));
  }

  @Test
  public void getIframeUrlTypeHtml() throws Exception {
    String xml
        = "<Module>" +
          " <ModulePrefs title='test'/>" +
          " <Content type='html'/>" +
          " <UserPref name='" + UP_NAME + "' datatype='string'/>" +
          "</Module>";
    GadgetSpec spec = new GadgetSpec(URI.create(SPEC_URL), xml);
    Gadget gadget = new Gadget(context, spec, Collections.<JsLibrary>emptyList());
    fixture.replay();

    URI iframeUrl = URI.create(urlGenerator.getIframeUrl(gadget));

    assertEquals(IFR_PREFIX, iframeUrl.getPath() + '?');
    StringAssert.assertContains("container=" + CONTAINER, iframeUrl.getQuery());
    StringAssert.assertContains("up_" + UP_NAME + '=' + UP_VALUE, iframeUrl.getQuery());
    StringAssert.assertContains("mid=" + MODULE_ID, iframeUrl.getQuery());
    StringAssert.assertContains("view=" + VIEW, iframeUrl.getQuery());
  }

  @Test
  public void getIframeUrlTypeUrl() throws Exception {
    String xml
        = "<Module>" +
          " <ModulePrefs title='test'/>" +
          " <Content type='url' href='" + StringEscapeUtils.escapeHtml(TYPE_URL_HREF) + "'/>" +
          " <UserPref name='" + UP_NAME + "' datatype='string'/>" +
          "</Module>";
    GadgetSpec spec = new GadgetSpec(URI.create(SPEC_URL), xml);
    Gadget gadget = new Gadget(context, spec, Collections.<JsLibrary>emptyList());
    fixture.replay();

    URI iframeUrl = URI.create(urlGenerator.getIframeUrl(gadget));

    assertEquals(TYPE_URL_HREF_HOST, iframeUrl.getAuthority());
    assertEquals(TYPE_URL_HREF_PATH, iframeUrl.getPath());
    StringAssert.assertContains(TYPE_URL_HREF_QUERY, iframeUrl.getQuery());
    StringAssert.assertContains("container=" + CONTAINER, iframeUrl.getQuery());
    StringAssert.assertContains("up_" + UP_NAME + '=' + UP_VALUE, iframeUrl.getQuery());
    StringAssert.assertContains("mid=" + MODULE_ID, iframeUrl.getQuery());
  }
}
