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

import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.IFRAME_BASE_PATH_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.LOCKED_DOMAIN_FEATURE_NAME;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.LOCKED_DOMAIN_REQUIRED_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.LOCKED_DOMAIN_SUFFIX_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.SECURITY_TOKEN_ALWAYS_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.SECURITY_TOKEN_FEATURE_NAME;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.UNLOCKED_DOMAIN_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.tplKey;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.shindig.auth.BasicSecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.ContainerConfigException;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.HashLockedDomainService;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DefaultIframeUriManagerTest extends UriManagerTestBase {
  private static final String LD_PREFIX = "locked";
  private static final String IFRAME_PATH = "/gadgets/ifr";
  private static final String JS_HOST = "//server.com";
  private static final String JS_PATH = "/gadgets/js";
  private static final String LD_SUFFIX = ".lockeddomain.com";
  private static final String LD_SUFFIX_ALT = ".altld.com";
  private static final String UNLOCKED_DOMAIN = "unlockeddomain.com";
  private static final String UNLOCKED_DOMAIN_CONFIG_VALUE = UNLOCKED_DOMAIN;
  private static final int TYPE_URL_NUM_BASE_PARAMS = 8;
  private static final int TYPE_HTML_NUM_BASE_PARAMS = 8;

  private static final LockedDomainPrefixGenerator prefixGen = new LockedDomainPrefixGenerator() {
    public String getLockedDomainPrefix(Uri gadgetUri) {
      return LD_PREFIX;
    }

    public String getLockedDomainPrefix(String token) {
      // TODO Auto-generated method stub
      return LD_PREFIX;
    }
  };

  private static final SecurityTokenCodec tokenCodec = new BasicSecurityTokenCodec();

  @Test
  public void typeHtmlBasicOptions() throws Exception {
    String prefKey = "prefKey";
    String prefVal = "prefVal";
    Map<String, String> prefs = Maps.newHashMap();
    prefs.put(prefKey, prefVal);
    List<String> features = Lists.newArrayList();

    // Make the gadget.
    Gadget gadget = mockGadget(
        SPEC_URI.toString(),
        false,  // not type=url
        false,  // isDebug
        false,  // ignoreCache
        false,  // sanitize
        false,  // cajoled
        prefs,  // spec-contained prefs
        prefs,  // prefs supplied by the requester, same k/v as spec w/ default val for simplicity
        false,  // no pref substitution needed, ergo prefs in fragment
        features);

    // Generate a default-option manager
    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        false);  // locked domain not required

    // Generate URI, turn into UriBuilder for validation
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);

    UriBuilder uri = new UriBuilder(result);
    assertEquals("", uri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());
    assertEquals(SPEC_URI.toString(), uri.getQueryParameter(Param.URL.getKey()));
    assertEquals(CONTAINER, uri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(VIEW, uri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(LANG, uri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(COUNTRY, uri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals("0", uri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", uri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", uri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(prefVal, uri.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(1, uri.getFragmentParameters().size());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());
    assertEquals(SPEC_URI.toString(), htmlGadgetUri.getQueryParameter(Param.URL.getKey()));
    assertEquals(CONTAINER, htmlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(VIEW, htmlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(LANG, htmlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(COUNTRY, htmlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals("0", htmlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", htmlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", htmlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(prefVal, htmlGadgetUri.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(1, htmlGadgetUri.getFragmentParameters().size());

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget.xml", urlGadgetUri.getPath());
    assertNull(urlGadgetUri.getQueryParameter(Param.URL.getKey()));
    assertEquals(CONTAINER, urlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(ANOTHER_VIEW, urlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(LANG, urlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(COUNTRY, urlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals("0", urlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", urlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", urlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(prefVal, urlGadgetUri.getQueryParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_URL_NUM_BASE_PARAMS + 1, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());

    assertFalse(manager.tokenForRenderingCalled());
    assertTrue(manager.schemeCalled());
    assertTrue(manager.addExtrasCalled());
  }

  @Test
  public void typeHtmlCajoled() throws Exception {
    String prefKey = "prefKey";
    String prefVal = "prefVal";
    Map<String, String> prefs = Maps.newHashMap();
    prefs.put(prefKey, prefVal);
    List<String> features = Lists.newArrayList();

    // Make the gadget.
    Gadget gadget = mockGadget(
        SPEC_URI.toString(),
        false,  // not type=url
        false,  // isDebug
        false,  // ignoreCache
        false,  // sanitize
        true,  // cajoled
        prefs,  // spec-contained prefs
        prefs,  // prefs supplied by the requester, same k/v as spec w/ default val for simplicity
        false,  // no pref substitution needed, ergo prefs in fragment
        features);

    // Generate a default-option manager
    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        false);  // locked domain not required

    // Generate URI, turn into UriBuilder for validation
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);

    UriBuilder uri = new UriBuilder(result);
    assertEquals("0", uri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals("1", uri.getQueryParameter(Param.CAJOLE.getKey()));
    assertEquals(prefVal, uri.getFragmentParameter("up_" + prefKey));

    // Cajoled is and added param
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, uri.getQueryParameters().size());
    assertEquals(1, uri.getFragmentParameters().size());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("0", htmlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals("1", htmlGadgetUri.getQueryParameter(Param.CAJOLE.getKey()));
    assertEquals(prefVal, htmlGadgetUri.getFragmentParameter("up_" + prefKey));

    // Cajoled is and added param
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, htmlGadgetUri.getQueryParameters().size());
    assertEquals(1, htmlGadgetUri.getFragmentParameters().size());

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("0", urlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals("1", urlGadgetUri.getQueryParameter(Param.CAJOLE.getKey()));
    assertEquals(prefVal, urlGadgetUri.getQueryParameter("up_" + prefKey));

    // Cajoled is and added param
    assertEquals(TYPE_URL_NUM_BASE_PARAMS + 2, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());

    assertFalse(manager.tokenForRenderingCalled());
    assertTrue(manager.schemeCalled());
    assertTrue(manager.addExtrasCalled());
  }

  @Test
  public void typeHtmlBasicOptionsTpl() throws Exception {
    String prefKey = "prefKey";
    String prefVal = "prefVal";
    Map<String, String> prefs = Maps.newHashMap();
    prefs.put(prefKey, prefVal);
    List<String> features = Lists.newArrayList();

    // Make the gadget.
    Gadget gadget = mockGadget(
        SPEC_URI.toString(),
        false,  // not type=url
        false,  // isDebug
        false,  // ignoreCache
        false,  // sanitize
        false,  // cajoled
        prefs,  // spec-contained prefs
        prefs,  // prefs supplied by the requester, same k/v as spec w/ default val for simplicity
        false,  // no pref substitution needed, ergo prefs in fragment
        features);

    // Create another manager, this time templatized.
    TestDefaultIframeUriManager managerTpl = makeManager(
        false,   // security token beacon not required
        false);  // locked domain not required
    managerTpl.setTemplatingSignal(tplSignal(true));

    // Templatized results.
    Uri resultTpl = managerTpl.makeRenderingUri(gadget);
    assertNotNull(resultTpl);

    UriBuilder uriTpl = new UriBuilder(resultTpl);
    assertEquals("", uriTpl.getScheme());
    assertEquals(UNLOCKED_DOMAIN, uriTpl.getAuthority());
    assertEquals(IFRAME_PATH, uriTpl.getPath());
    assertEquals(SPEC_URI.toString(), uriTpl.getQueryParameter(Param.URL.getKey()));
    assertEquals(CONTAINER, uriTpl.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(tplKey(Param.VIEW.getKey()), uriTpl.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(tplKey(Param.LANG.getKey()), uriTpl.getQueryParameter(Param.LANG.getKey()));
    assertEquals(tplKey(Param.COUNTRY.getKey()), uriTpl.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(tplKey(Param.DEBUG.getKey()), uriTpl.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(tplKey(Param.NO_CACHE.getKey()),
        uriTpl.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(tplKey(Param.SANITIZE.getKey()),
        uriTpl.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(tplKey("up_" + prefKey), uriTpl.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uriTpl.getQueryParameters().size());
    assertEquals(1, uriTpl.getFragmentParameters().size());

    Map<String, Uri> uris = managerTpl.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());
    assertEquals(SPEC_URI.toString(), htmlGadgetUri.getQueryParameter(Param.URL.getKey()));
    assertEquals(CONTAINER, htmlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(tplKey(Param.VIEW.getKey()), htmlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(tplKey(Param.LANG.getKey()), htmlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(tplKey(Param.COUNTRY.getKey()), htmlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(tplKey(Param.DEBUG.getKey()), htmlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(tplKey(Param.NO_CACHE.getKey()),
            htmlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(tplKey(Param.SANITIZE.getKey()),
            htmlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(tplKey("up_" + prefKey), htmlGadgetUri.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(1, htmlGadgetUri.getFragmentParameters().size());

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget.xml", urlGadgetUri.getPath());
    assertNull(urlGadgetUri.getQueryParameter(Param.URL.getKey()));
    assertEquals(CONTAINER, urlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(tplKey(Param.VIEW.getKey()), urlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(tplKey(Param.LANG.getKey()), urlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(tplKey(Param.COUNTRY.getKey()), urlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(tplKey(Param.DEBUG.getKey()), urlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(tplKey(Param.NO_CACHE.getKey()),
            urlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(tplKey(Param.SANITIZE.getKey()),
            urlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(tplKey("up_" + prefKey), urlGadgetUri.getQueryParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_URL_NUM_BASE_PARAMS + 1, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());

    assertFalse(managerTpl.tokenForRenderingCalled());
    assertTrue(managerTpl.schemeCalled());
    assertTrue(managerTpl.addExtrasCalled());
  }

  @Test
  public void typeUrlDefaultOptions() throws Exception {
    String gadgetSite = "http://example.com/gadget";
    String prefKey = "prefKey";
    String prefVal = "prefVal";
    Map<String, String> prefs = Maps.newHashMap();
    prefs.put(prefKey, prefVal);
    List<String> features = Lists.newArrayList("rpc", "setprefs");

    // Make the gadget.
    Gadget gadget = mockGadget(
        gadgetSite,
        true,   // type=url
        true,   // isDebug
        true,   // ignoreCache
        true,   // sanitize
        false,  // cajoled
        prefs,  // spec-contained prefs
        prefs,  // prefs supplied by the requester, same k/v as spec w/ default val for simplicity
        false,  // no pref substitution needed, ergo prefs in fragment
        features);

    // Generate a default-option manager
    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        false);  // locked domain not required

    // Generate URI, turn into UriBuilder for validation
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);

    UriBuilder uri = new UriBuilder(result);
    assertEquals("http", uri.getScheme());
    assertEquals("example.com", uri.getAuthority());
    assertEquals("/gadget", uri.getPath());
    assertEquals(CONTAINER, uri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(VIEW, uri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(LANG, uri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(COUNTRY, uri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(JS_HOST + JS_PATH + "/rpc:setprefs" + DefaultJsUriManager.JS_SUFFIX, uri.getQueryParameter(Param.LIBS.getKey()));
    assertEquals("1", uri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("1", uri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("1", uri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(prefVal, uri.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(1, uri.getFragmentParameters().size());

    assertFalse(manager.tokenForRenderingCalled());
    assertFalse(manager.schemeCalled());
    assertTrue(manager.addExtrasCalled());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget", urlGadgetUri.getPath());
    assertEquals(CONTAINER, urlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(VIEW, urlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(LANG, urlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(COUNTRY, urlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(JS_HOST + JS_PATH + "/rpc:setprefs" + DefaultJsUriManager.JS_SUFFIX, urlGadgetUri.getQueryParameter(Param.LIBS.getKey()));
    assertEquals("1", urlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("1", urlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("1", urlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(prefVal, urlGadgetUri.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(1, urlGadgetUri.getFragmentParameters().size());

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());
    assertEquals(CONTAINER, htmlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(ANOTHER_VIEW, htmlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(LANG, htmlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(COUNTRY, htmlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertNull(htmlGadgetUri.getQueryParameter(Param.LIBS.getKey()));
    assertEquals("1", htmlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("1", htmlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("1", htmlGadgetUri.getQueryParameter(Param.SANITIZE.getKey()));
    assertEquals(prefVal, htmlGadgetUri.getQueryParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());

    assertFalse(manager.tokenForRenderingCalled());
    assertTrue(manager.schemeCalled());
    assertTrue(manager.addExtrasCalled());
  }

  @Test
  public void typeUrlDefaultOptionsTpl() throws Exception {
    String gadgetSite = "http://example.com/gadget";
    String prefKey = "prefKey";
    String prefVal = "prefVal";
    Map<String, String> prefs = Maps.newHashMap();
    prefs.put(prefKey, prefVal);
    List<String> features = Lists.newArrayList();

    // Make the gadget.
    Gadget gadget = mockGadget(
        gadgetSite,
        true,   // type=url
        true,   // isDebug
        true,   // ignoreCache
        true,   // sanitize
        false,  // cajoled
        prefs,  // spec-contained prefs
        prefs,  // prefs supplied by the requester, same k/v as spec w/ default val for simplicity
        false,  // no pref substitution needed, ergo prefs in fragment
        features);

    // Generate a default-option manager
    TestDefaultIframeUriManager managerTpl = makeManager(
        false,   // security token beacon not required
        false);  // locked domain not required
    managerTpl.setTemplatingSignal(tplSignal(true));

    // Generate URI, turn into UriBuilder for validation
    Uri resultTpl = managerTpl.makeRenderingUri(gadget);
    assertNotNull(resultTpl);

    UriBuilder uriTpl = new UriBuilder(resultTpl);
    assertEquals("http", uriTpl.getScheme());
    assertEquals("example.com", uriTpl.getAuthority());
    assertEquals("/gadget", uriTpl.getPath());
    assertEquals(CONTAINER, uriTpl.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("", uriTpl.getQueryParameter(Param.LIBS.getKey()));
    assertEquals(tplKey(Param.VIEW.getKey()), uriTpl.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(tplKey(Param.LANG.getKey()), uriTpl.getQueryParameter(Param.LANG.getKey()));
    assertEquals(tplKey(Param.COUNTRY.getKey()), uriTpl.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(tplKey(Param.DEBUG.getKey()), uriTpl.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(tplKey(Param.NO_CACHE.getKey()),
        uriTpl.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(tplKey("up_" + prefKey), uriTpl.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, uriTpl.getQueryParameters().size());
    assertEquals(1, uriTpl.getFragmentParameters().size());

    assertFalse(managerTpl.tokenForRenderingCalled());
    assertFalse(managerTpl.schemeCalled());
    assertTrue(managerTpl.addExtrasCalled());

    Map<String, Uri> uris = managerTpl.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget", urlGadgetUri.getPath());
    assertEquals(CONTAINER, urlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("", urlGadgetUri.getQueryParameter(Param.LIBS.getKey()));
    assertEquals(tplKey(Param.VIEW.getKey()), urlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(tplKey(Param.LANG.getKey()), urlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(tplKey(Param.COUNTRY.getKey()), urlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(tplKey(Param.DEBUG.getKey()), urlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(tplKey(Param.NO_CACHE.getKey()),
            urlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(tplKey("up_" + prefKey), urlGadgetUri.getFragmentParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(1, urlGadgetUri.getFragmentParameters().size());

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());
    assertEquals(CONTAINER, htmlGadgetUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertNull(htmlGadgetUri.getQueryParameter(Param.LIBS.getKey()));
    assertEquals(tplKey(Param.VIEW.getKey()), htmlGadgetUri.getQueryParameter(Param.VIEW.getKey()));
    assertEquals(tplKey(Param.LANG.getKey()), htmlGadgetUri.getQueryParameter(Param.LANG.getKey()));
    assertEquals(tplKey(Param.COUNTRY.getKey()), htmlGadgetUri.getQueryParameter(Param.COUNTRY.getKey()));
    assertEquals(tplKey(Param.DEBUG.getKey()), htmlGadgetUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(tplKey(Param.NO_CACHE.getKey()),
            htmlGadgetUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(tplKey("up_" + prefKey), htmlGadgetUri.getQueryParameter("up_" + prefKey));

    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());

    assertFalse(managerTpl.tokenForRenderingCalled());
    assertTrue(managerTpl.schemeCalled());
    assertTrue(managerTpl.addExtrasCalled());
  }

  @Test
  public void securityTokenAddedWhenGadgetNeedsItFragment() throws Exception {
    Gadget gadget = mockGadget(SECURITY_TOKEN_FEATURE_NAME);
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setTokenForRendering(false);
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(1, uri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
        uri.getFragmentParameter(Param.SECURITY_TOKEN.getKey()));
    assertTrue(manager.tokenForRenderingCalled());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(1, htmlGadgetUri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
            htmlGadgetUri.getFragmentParameter(Param.SECURITY_TOKEN.getKey()));

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(1, urlGadgetUri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
            urlGadgetUri.getFragmentParameter(Param.SECURITY_TOKEN.getKey()));

    assertTrue(manager.tokenForRenderingCalled());
  }

  @Test
  public void securityTokenAddedWhenGadgetNeedsItQuery() throws Exception {
    Gadget gadget = mockGadget(SECURITY_TOKEN_FEATURE_NAME);
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setTokenForRendering(true);
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
        uri.getQueryParameter(Param.SECURITY_TOKEN.getKey()));
    assertTrue(manager.tokenForRenderingCalled());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
            htmlGadgetUri.getQueryParameter(Param.SECURITY_TOKEN.getKey()));

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals(TYPE_URL_NUM_BASE_PARAMS + 1, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
            urlGadgetUri.getQueryParameter(Param.SECURITY_TOKEN.getKey()));

    assertTrue(manager.tokenForRenderingCalled());
  }

  @Test
  public void securityTokenAddedWhenForced() throws Exception {
    Gadget gadget = mockGadget("foo", "bar");
    TestDefaultIframeUriManager manager = makeManager(true, false);  // security token forced
    manager.setTokenForRendering(false);
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(1, uri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
        uri.getFragmentParameter(Param.SECURITY_TOKEN.getKey()));
    assertTrue(manager.tokenForRenderingCalled());

    manager.setTokenForRendering(false);
    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(1, htmlGadgetUri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
            htmlGadgetUri.getFragmentParameter(Param.SECURITY_TOKEN.getKey()));

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(1, urlGadgetUri.getFragmentParameters().size());
    assertEquals(tplKey(Param.SECURITY_TOKEN.getKey()),
            urlGadgetUri.getFragmentParameter(Param.SECURITY_TOKEN.getKey()));

    assertTrue(manager.tokenForRenderingCalled());

  }

  @Test
  public void ldAddedGadgetRequests() throws Exception {
    Gadget gadget = mockGadget(LOCKED_DOMAIN_FEATURE_NAME);

    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        false,   // locked domain not (always) required
        true);

    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);

    UriBuilder uri = new UriBuilder(result);
    assertEquals("", uri.getScheme());
    assertEquals(LD_PREFIX + LD_SUFFIX, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(LD_PREFIX + LD_SUFFIX, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget.xml", urlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());
  }

  @Test
  public void ldAddedForcedAlways() throws Exception {
    Gadget gadget = mockGadget();

    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        true);   // locked domain always required

    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);

    UriBuilder uri = new UriBuilder(result);
    assertEquals("", uri.getScheme());
    assertEquals(LD_PREFIX + LD_SUFFIX, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(LD_PREFIX + LD_SUFFIX, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget.xml", urlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());
  }

  @Test
  public void ldNotAddedIfDisabled() throws Exception {
    Gadget gadget = mockGadget(LOCKED_DOMAIN_FEATURE_NAME);

    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        false);   // locked domain always required

    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);

    UriBuilder uri = new UriBuilder(result);
    assertEquals("", uri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget.xml", urlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());
  }

  @Test
  public void versionAddedWithVersioner() throws Exception {
    String version = "abcdlkjwef";
    Gadget gadget = mockGadget();
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(this.mockVersioner(version, true));
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, uri.getQueryParameters().size());
    assertEquals(version, uri.getQueryParameter(Param.VERSION.getKey()));

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, htmlGadgetUri.getQueryParameters().size());
    assertEquals(version, htmlGadgetUri.getQueryParameter(Param.VERSION.getKey()));

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals(TYPE_URL_NUM_BASE_PARAMS + 1, urlGadgetUri.getQueryParameters().size());
    assertEquals(version, urlGadgetUri.getQueryParameter(Param.VERSION.getKey()));
  }

  @Test
  public void userPrefsAddedQuery() throws Exception {
    // Scenario exercises all prefs cases: overridden/known key, unknown key, missing key
    Map<String, String> specPrefs = Maps.newHashMap();
    specPrefs.put("specKey1", "specDefault1");
    specPrefs.put("specKey2", "specDefault2");
    Map<String, String> inPrefs = Maps.newHashMap();
    inPrefs.put("specKey1", "inVal1");
    inPrefs.put("otherKey1", "inVal2");

    Gadget gadget = mockGadget(true, specPrefs, inPrefs);
    TestDefaultIframeUriManager manager = makeManager(false, false);
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);

    // otherKey1/inVal2 pair ignored; not known by the gadget
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 2, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());
    assertEquals("inVal1", uri.getQueryParameter("up_specKey1"));
    assertEquals("specDefault2", uri.getQueryParameter("up_specKey2"));

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 2, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());
    assertEquals("inVal1", htmlGadgetUri.getQueryParameter("up_specKey1"));
    assertEquals("specDefault2", htmlGadgetUri.getQueryParameter("up_specKey2"));

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(2, urlGadgetUri.getFragmentParameters().size());
    assertEquals("inVal1", urlGadgetUri.getFragmentParameter("up_specKey1"));
    assertEquals("specDefault2", urlGadgetUri.getFragmentParameter("up_specKey2"));
  }

  @Test
  public void userPrefsAddedFragment() throws Exception {
    // Scenario exercises all prefs cases: overridden/known key, unknown key, missing key
    Map<String, String> specPrefs = Maps.newHashMap();
    specPrefs.put("specKey1", "specDefault1");
    specPrefs.put("specKey2", "specDefault2");
    Map<String, String> inPrefs = Maps.newHashMap();
    inPrefs.put("specKey1", "inVal1");
    inPrefs.put("otherKey1", "inVal2");

    Gadget gadget = mockGadget(false, specPrefs, inPrefs);
    TestDefaultIframeUriManager manager = makeManager(false, false);
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);

    // otherKey1/inVal2 pair ignored; not known by the gadget
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(2, uri.getFragmentParameters().size());
    assertEquals("inVal1", uri.getFragmentParameter("up_specKey1"));
    assertEquals("specDefault2", uri.getFragmentParameter("up_specKey2"));

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(2, htmlGadgetUri.getFragmentParameters().size());
    assertEquals("inVal1", htmlGadgetUri.getFragmentParameter("up_specKey1"));
    assertEquals("specDefault2", htmlGadgetUri.getFragmentParameter("up_specKey2"));

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals(TYPE_URL_NUM_BASE_PARAMS + 2, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());
    assertEquals("inVal1", urlGadgetUri.getQueryParameter("up_specKey1"));
    assertEquals("specDefault2", urlGadgetUri.getQueryParameter("up_specKey2"));
  }

  @Test
  public void honorSchemeOverride() throws Exception {
    String scheme = "file";
    Gadget gadget = mockGadget();
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setScheme(scheme);
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);
    assertEquals(scheme, uri.getScheme());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);
    assertEquals(scheme, new UriBuilder(uris.get(VIEW)).getScheme());
    assertFalse(scheme.equalsIgnoreCase(new UriBuilder(uris.get(ANOTHER_VIEW)).getScheme()));
  }

  @Test
  public void badUriValidatingUri() throws Exception {
    Uri uri = new UriBuilder().addQueryParameter(Param.URL.getKey(), "^':   bad:").toUri();
    TestDefaultIframeUriManager manager = makeManager(false, false);
    UriStatus status = manager.validateRenderingUri(uri);
    assertEquals(UriStatus.BAD_URI, status);
  }

  @Test
  public void validUnversionedNoVersioner() throws Exception {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, "version");
    DefaultIframeUriManager manager = makeManager(false, false);
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }

  @Test
  public void validUnversionedNoVersion() throws Exception {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, null);
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(this.mockVersioner("version", false));  // Invalid, if present.
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }

  @Test
  public void versionerVersionInvalid() throws Exception {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, "in-version");
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(mockVersioner("test-version", false));  // Invalid, if present.
    assertEquals(UriStatus.INVALID_VERSION, manager.validateRenderingUri(uri));
  }

  @Test
  public void versionerVersionMatch() throws Exception {
    String version = "abcdefg";
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, version);
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(mockVersioner(version, true));
    assertEquals(UriStatus.VALID_VERSIONED, manager.validateRenderingUri(uri));
  }

  @Test
  public void containerConfigurationChanges() throws Exception {
    ContainerConfig config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(ImmutableMap
            .<String, Object>builder()
            .put(ContainerConfig.CONTAINER_KEY, ContainerConfig.DEFAULT_CONTAINER)
            .put(LOCKED_DOMAIN_SUFFIX_KEY, LD_SUFFIX)
            .put(IFRAME_BASE_PATH_KEY, IFRAME_PATH)
            .put(LOCKED_DOMAIN_REQUIRED_KEY, true)
            .build())
        .commit();
    LockedDomainService ldService = new HashLockedDomainService(config, true, prefixGen);
    TestDefaultIframeUriManager manager = new TestDefaultIframeUriManager(config, ldService);

    Uri testUri = Uri.parse("http://foobar" + LD_SUFFIX + "/?url=http://example.com");

    config.newTransaction().addContainer(ImmutableMap
        .<String, Object>builder()
        .put(ContainerConfig.CONTAINER_KEY, ContainerConfig.DEFAULT_CONTAINER)
        .put(LOCKED_DOMAIN_SUFFIX_KEY, LD_SUFFIX_ALT)
        .build()).commit();
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(testUri));
  }

  @Test
  public void schemeLessUnlockedDomain() throws Exception {
    Gadget gadget = mockGadget();
    ContainerConfig config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(ImmutableMap.<String, Object>builder()
            .put(ContainerConfig.CONTAINER_KEY, ContainerConfig.DEFAULT_CONTAINER)
            .put(LOCKED_DOMAIN_SUFFIX_KEY, LD_SUFFIX)
            .build())
                .addContainer(ImmutableMap.<String, Object> builder()
                    .put(ContainerConfig.CONTAINER_KEY, CONTAINER)
                    .put(IFRAME_BASE_PATH_KEY, IFRAME_PATH)
                    .put(UNLOCKED_DOMAIN_KEY, UNLOCKED_DOMAIN)
                    .put(DefaultJsUriManager.JS_HOST_PARAM, JS_HOST)
                     .put(DefaultJsUriManager.JS_PATH_PARAM, JS_PATH)
                    .build())
                .commit();

    LockedDomainService ldService = new HashLockedDomainService(config, false, prefixGen);
    TestDefaultIframeUriManager manager = new TestDefaultIframeUriManager(config, ldService);

    Uri renderingUri = manager.makeRenderingUri(gadget);
    assertNotNull(renderingUri);

    UriBuilder uri = new UriBuilder(renderingUri);
    assertEquals("", uri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());

    Map<String, Uri> uris = manager.makeAllRenderingUris(gadget);
    assertNotNull(uris);

    UriBuilder htmlGadgetUri = new UriBuilder(uris.get(VIEW));
    assertEquals("", htmlGadgetUri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, htmlGadgetUri.getAuthority());
    assertEquals(IFRAME_PATH, htmlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, htmlGadgetUri.getQueryParameters().size());
    assertEquals(0, htmlGadgetUri.getFragmentParameters().size());

    UriBuilder urlGadgetUri = new UriBuilder(uris.get(ANOTHER_VIEW));
    assertEquals("http", urlGadgetUri.getScheme());
    assertEquals("example.com", urlGadgetUri.getAuthority());
    assertEquals("/gadget.xml", urlGadgetUri.getPath());

    // Basic sanity checks on params
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, urlGadgetUri.getQueryParameters().size());
    assertEquals(0, urlGadgetUri.getFragmentParameters().size());
  }

  private Uri makeValidationTestUri(String domain, String version) {
    UriBuilder uri = new UriBuilder();
    uri.setAuthority(domain);
    uri.setPath(IFRAME_PATH);
    uri.addQueryParameter(Param.URL.getKey(), SPEC_URI.toString());
    uri.addQueryParameter(Param.CONTAINER.getKey(), CONTAINER);
    if (version != null) {
      uri.addQueryParameter(Param.VERSION.getKey(), version);
    }
    return uri.toUri();
  }

  private TestDefaultIframeUriManager makeManager(boolean alwaysToken, boolean ldRequired)
      throws ContainerConfigException {
    return makeManager(alwaysToken, ldRequired, ldRequired ? true: false);
  }
  private TestDefaultIframeUriManager makeManager(boolean alwaysToken, boolean ldRequired, boolean enabled)
      throws ContainerConfigException {
    String altContainer = CONTAINER + "-alt";
    ContainerConfig config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(ImmutableMap
            .<String, Object>builder()
            .put(ContainerConfig.CONTAINER_KEY, ContainerConfig.DEFAULT_CONTAINER)
            .put(LOCKED_DOMAIN_SUFFIX_KEY, LD_SUFFIX)
            .put(DefaultJsUriManager.JS_HOST_PARAM, JS_HOST)
            .put(DefaultJsUriManager.JS_PATH_PARAM, JS_PATH)
            .build())
        .addContainer(ImmutableMap
            .<String, Object>builder()
            .put(ContainerConfig.CONTAINER_KEY, CONTAINER)
            .put(IFRAME_BASE_PATH_KEY, IFRAME_PATH)
            .put(LOCKED_DOMAIN_SUFFIX_KEY, LD_SUFFIX)
            .put(UNLOCKED_DOMAIN_KEY, UNLOCKED_DOMAIN_CONFIG_VALUE)
            .put(SECURITY_TOKEN_ALWAYS_KEY, alwaysToken)
            .put(LOCKED_DOMAIN_REQUIRED_KEY, ldRequired)
            .put(DefaultJsUriManager.JS_HOST_PARAM, JS_HOST)
            .put(DefaultJsUriManager.JS_PATH_PARAM, JS_PATH)
            .build())
        .addContainer(ImmutableMap
            .<String, Object>builder()
            .put(ContainerConfig.CONTAINER_KEY, altContainer)
            .put(ContainerConfig.PARENT_KEY, CONTAINER)
            .put(LOCKED_DOMAIN_SUFFIX_KEY, LD_SUFFIX_ALT)
            .put(DefaultJsUriManager.JS_HOST_PARAM, JS_HOST)
            .put(DefaultJsUriManager.JS_PATH_PARAM, JS_PATH)
            .build())
        .commit();
    LockedDomainService ldService = new HashLockedDomainService(config, enabled, prefixGen);
    return new TestDefaultIframeUriManager(config, ldService);
  }

  private IframeUriManager.Versioner mockVersioner(String version, boolean valid) {
    IframeUriManager.Versioner versioner = createMock(IframeUriManager.Versioner.class);
    expect(versioner.version(isA(Uri.class), isA(String.class))).andReturn(version).anyTimes();
    expect(versioner.validate(isA(Uri.class), isA(String.class), isA(String.class))).andReturn(
        valid ? UriStatus.VALID_VERSIONED : UriStatus.INVALID_VERSION).anyTimes();
    replay(versioner);
    return versioner;
  }

  private DefaultIframeUriManager.TemplatingSignal tplSignal(boolean value) {
    DefaultIframeUriManager.DefaultTemplatingSignal tplSignal =
        new DefaultIframeUriManager.DefaultTemplatingSignal();
    tplSignal.setUseTemplates(value);
    return tplSignal;
  }

  private static final class TestDefaultIframeUriManager extends DefaultIframeUriManager {
    private String scheme = "";
    private boolean schemeCalled = false;
    private boolean tokenForRendering = true;
    private boolean tokenForRenderingCalled = false;
    private boolean addExtrasCalled = false;

    private TestDefaultIframeUriManager(ContainerConfig config, LockedDomainService ldService) {
      super(config, ldService, tokenCodec);
    }

    private TestDefaultIframeUriManager setScheme(String scheme) {
      this.scheme = scheme;
      return this;
    }

    private TestDefaultIframeUriManager setTokenForRendering(boolean tokenForRendering) {
      this.tokenForRendering = tokenForRendering;
      return this;
    }

    @Override
    protected String getScheme(Gadget gadget, String container) {
      this.schemeCalled = true;
      return scheme;
    }

    private boolean schemeCalled() {
      return schemeCalled;
    }

    @Override
    protected boolean isTokenNeededForRendering(Gadget gadget) {
      this.tokenForRenderingCalled = true;
      return tokenForRendering;
    }

    private boolean tokenForRenderingCalled() {
      return tokenForRenderingCalled;
    }

    @Override
    protected void addExtras(UriBuilder uri, Gadget gadget) {
      this.addExtrasCalled = true;
    }

    private boolean addExtrasCalled() {
      return addExtrasCalled;
    }
  }
}
