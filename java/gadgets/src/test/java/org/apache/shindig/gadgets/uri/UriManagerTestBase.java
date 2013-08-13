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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.spec.View.ContentType;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class UriManagerTestBase {
  protected static final String CONTAINER = "container";
  protected static final Uri SPEC_URI = Uri.parse("http://example.com/gadget.xml");
  protected static final String VIEW = "theview";
  protected static final String ANOTHER_VIEW = "anotherview";
  protected static final String LANG = "en";
  protected static final String COUNTRY = "US";

  // Used for "feature-focused" tests, eg. security token and locked domain
  protected Gadget mockGadget(String... features) {
    Map<String, String> prefs = Maps.newHashMap();
    return mockGadget(SPEC_URI.toString(), false, false, false, false, false, prefs, prefs,
        false, Lists.newArrayList(features));
  }

  // Used for prefs-focused tests
  protected Gadget mockGadget(boolean prefsForRendering, Map<String, String> specPrefs,
      Map<String, String> inPrefs) {
    return mockGadget(SPEC_URI.toString(), false, false, false, false, false, specPrefs,
        inPrefs, prefsForRendering, Lists.<String>newArrayList());
  }

  // Used for "base" tests.
  protected Gadget mockGadget(String targetUrl, boolean isTypeUrl, boolean isDebug,
      boolean ignoreCache, boolean sanitize, boolean cajoled, Map<String, String> specPrefs,
      Map<String, String> inPrefs, boolean needsPrefSubst, List<String> features) {
    return mockGadget(targetUrl, isTypeUrl, VIEW, LANG, COUNTRY, isDebug, ignoreCache,
        sanitize, cajoled, specPrefs, inPrefs, needsPrefSubst, features);
  }

  // Used for tests that don't care much about prefs or gadget type.
  protected Gadget mockGadget(boolean isDebug, boolean ignoreCache) {
    return mockGadget(SPEC_URI.toString(), false, isDebug, ignoreCache,
        false, false, Maps.<String, String>newHashMap(),
        Maps.<String, String>newHashMap(), false, Lists.<String>newArrayList());
  }

  // Actually generates the mock gadget. Used for error (null value) tests.
  protected Gadget mockGadget(String targetUrl, boolean isTypeUrl, String currentViewStr, String lang,
      String country, boolean isDebug, boolean ignoreCache, boolean sanitize, boolean cajoled,
      Map<String, String> specPrefs, Map<String, String> inPrefs, boolean needsPrefSubst, List<String> features) {
    View currentView = createMock(View.class);
    View secondView = createMock(View.class);
    ModulePrefs modulePrefs = createMock(ModulePrefs.class);
    GadgetSpec spec = createMock(GadgetSpec.class);
    GadgetContext context = createMock(GadgetContext.class);
    Gadget gadget = createMock(Gadget.class);

    // Base URL/view.
    Uri targetUri = Uri.parse(targetUrl);
    if (isTypeUrl) {
      expect(currentView.getType()).andReturn(ContentType.URL).anyTimes();
      expect(currentView.getHref()).andReturn(targetUri).anyTimes();
      expect(secondView.getType()).andReturn(ContentType.HTML).anyTimes();
      expect(spec.getUrl()).andReturn(targetUri).anyTimes();
    } else {
      expect(currentView.getType()).andReturn(ContentType.HTML).anyTimes();
      expect(spec.getUrl()).andReturn(targetUri).anyTimes();
      expect(secondView.getType()).andReturn(ContentType.URL).anyTimes();
      expect(secondView.getHref()).andReturn(targetUri).anyTimes();
    }
    expect(currentView.getName()).andReturn(currentViewStr).anyTimes();
    expect(secondView.getName()).andReturn(ANOTHER_VIEW).anyTimes();

    // Basic context info
    Locale locale = new Locale(lang, country);
    expect(context.getUrl()).andReturn(SPEC_URI).anyTimes();
    expect(context.getContainer()).andReturn(CONTAINER).anyTimes();
    expect(context.getLocale()).andReturn(locale).anyTimes();
    expect(context.getDebug()).andReturn(isDebug).anyTimes();
    expect(context.getIgnoreCache()).andReturn(ignoreCache).anyTimes();
    expect(context.getToken()).andReturn(null).anyTimes();
    expect(context.getSanitize()).andReturn(sanitize).anyTimes();
    expect(context.getCajoled()).andReturn(cajoled).anyTimes();

    // All Features (doesn't distinguish between transitive and not)
    expect(gadget.getAllFeatures()).andReturn(features).anyTimes();
    Map<String, Feature> featureMap = Maps.newLinkedHashMap();
    for (String feature : features) {
      featureMap.put(feature, null);
    }
    expect(gadget.getViewFeatures()).andReturn(featureMap).anyTimes();
    expect(modulePrefs.getFeatures()).andReturn(featureMap).anyTimes();

    // User prefs
    Map<String, UserPref> specPrefMap = Maps.newLinkedHashMap();
    for (Map.Entry<String, String> specPref : specPrefs.entrySet()) {
      UserPref up = createMock(UserPref.class);
      expect(up.getName()).andReturn(specPref.getKey()).anyTimes();
      expect(up.getDefaultValue()).andReturn(specPref.getValue()).anyTimes();
      replay(up);
      specPrefMap.put(up.getName(),up);
    }
    expect(spec.getUserPrefs()).andReturn(specPrefMap).anyTimes();
    UserPrefs ctxPrefs = new UserPrefs(inPrefs);
    expect(context.getUserPrefs()).andReturn(ctxPrefs).anyTimes();
    expect(context.getParameter(Param.REFRESH.getKey())).andReturn(null).anyTimes();
    expect(currentView.needsUserPrefSubstitution()).andReturn(needsPrefSubst).anyTimes();
    expect(secondView.needsUserPrefSubstitution()).andReturn(!needsPrefSubst).anyTimes();

    Map<String, View> views = Maps.newHashMap();
    views.put(VIEW, currentView);
    views.put(ANOTHER_VIEW, secondView);

    // Link all the mocks together
    expect(spec.getViews()).andReturn(views).anyTimes();
    expect(spec.getModulePrefs()).andReturn(modulePrefs).anyTimes();
    expect(gadget.getCurrentView()).andReturn(currentView).anyTimes();
    expect(gadget.getSpec()).andReturn(spec).anyTimes();
    expect(gadget.getContext()).andReturn(context).anyTimes();

    // Replay all
    replay(currentView, secondView, modulePrefs, spec, context, gadget);

    // Return the gadget
    return gadget;
  }
}
