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

import static org.easymock.EasyMock.expect;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Maps;

import junitx.framework.StringAssert;

import org.apache.commons.lang.StringEscapeUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tests for DefaultUrlGenerator.
 */
public class DefaultUrlGeneratorTest extends GadgetTestFixture {
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

  private final GadgetContext context = mock(GadgetContext.class);
  private DefaultUrlGenerator realUrlGenerator;

  @Override
  public void setUp() throws Exception {
    realUrlGenerator = new DefaultUrlGenerator(IFR_PREFIX, JS_PREFIX, registry);

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

  public void testGetBundledJsParam() throws Exception {
    List<String> features = new ArrayList<String>();
    features.add("foo");
    features.add("bar");
    expect(context.getDebug()).andReturn(true);
    replay();

    String jsParam = realUrlGenerator.getBundledJsParam(features, context);

    assertTrue(
        jsParam.matches("foo:bar\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=1"));
  }

  public void testGetBundledJsParamWithBadFeatureName() throws Exception {
    List<String> features = new ArrayList<String>();
    features.add("foo!");
    features.add("bar");
    expect(context.getDebug()).andReturn(true);
    replay();

    String jsParam = realUrlGenerator.getBundledJsParam(features, context);

    assertTrue(jsParam.matches("bar\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=1"));
  }

  public void testGetBundledJsParamWithNoFeatures() throws Exception {
    List<String> features = new ArrayList<String>();
    expect(context.getDebug()).andReturn(false);
    replay();

    String jsParam = realUrlGenerator.getBundledJsParam(features, context);

    assertTrue(jsParam.matches("core\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=0"));
  }

  public void testGetBundledJsUrl() throws Exception {
    List<String> features = new ArrayList<String>();
    expect(context.getDebug()).andReturn(false);
    replay();

    String jsParam = realUrlGenerator.getBundledJsUrl(features, context);

    assertTrue(jsParam.matches(
        JS_PREFIX + "core\\.js\\?v=[0-9a-zA-Z]*&container=" + CONTAINER + "&debug=0"));
  }

  public void testGetIframeUrlTypeHtml() throws Exception {
    String xml
        = "<Module>" +
          " <ModulePrefs title='test'/>" +
          " <Content type='html'/>" +
          " <UserPref name='" + UP_NAME + "' datatype='string'/>" +
          "</Module>";
    GadgetSpec spec = new GadgetSpec(Uri.parse(SPEC_URL), xml);
    replay();

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("default"));

    URI iframeUrl = URI.create(realUrlGenerator.getIframeUrl(gadget));

    assertEquals(IFR_PREFIX, iframeUrl.getPath() + '?');
    StringAssert.assertContains("container=" + CONTAINER, iframeUrl.getQuery());
    StringAssert.assertContains("up_" + UP_NAME + '=' + UP_VALUE, iframeUrl.getQuery());
    StringAssert.assertContains("mid=" + MODULE_ID, iframeUrl.getQuery());
    StringAssert.assertContains("view=" + VIEW, iframeUrl.getQuery());
  }

  public void testGetIframeUrlTypeUrl() throws Exception {
    String xml
        = "<Module>" +
          " <ModulePrefs title='test'/>" +
          " <Content type='url' href='" + StringEscapeUtils.escapeHtml(TYPE_URL_HREF) + "'/>" +
          " <UserPref name='" + UP_NAME + "' datatype='string'/>" +
          "</Module>";
    GadgetSpec spec = new GadgetSpec(Uri.parse(SPEC_URL), xml);
    replay();
    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("default"));

    URI iframeUrl = URI.create(realUrlGenerator.getIframeUrl(gadget));

    assertEquals(TYPE_URL_HREF_HOST, iframeUrl.getAuthority());
    assertEquals(TYPE_URL_HREF_PATH, iframeUrl.getPath());
    StringAssert.assertContains(TYPE_URL_HREF_QUERY, iframeUrl.getQuery());
    StringAssert.assertContains("container=" + CONTAINER, iframeUrl.getQuery());
    StringAssert.assertContains("up_" + UP_NAME + '=' + UP_VALUE, iframeUrl.getQuery());
    StringAssert.assertContains("mid=" + MODULE_ID, iframeUrl.getQuery());
  }
}
