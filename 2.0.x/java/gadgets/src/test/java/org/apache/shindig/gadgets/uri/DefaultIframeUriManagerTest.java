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

import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.tplKey;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.IFRAME_BASE_PATH_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.LOCKED_DOMAIN_FEATURE_NAME;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.LOCKED_DOMAIN_REQUIRED_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.LOCKED_DOMAIN_SUFFIX_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.SECURITY_TOKEN_ALWAYS_KEY;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.SECURITY_TOKEN_FEATURE_NAME;
import static org.apache.shindig.gadgets.uri.DefaultIframeUriManager.UNLOCKED_DOMAIN_KEY;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.auth.BasicSecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultIframeUriManagerTest extends UriManagerTestBase {
  private static final String LD_PREFIX = "LOCKED";
  private static final String IFRAME_PATH = "/gadgets/ifr";
  private static final String LD_SUFFIX = ".lockeddomain.com";
  private static final String LD_SUFFIX_ALT = ".altld.com";
  private static final String UNLOCKED_DOMAIN = "unlockeddomain.com";
  private static final int TYPE_URL_NUM_BASE_PARAMS = 6;
  private static final int TYPE_HTML_NUM_BASE_PARAMS = TYPE_URL_NUM_BASE_PARAMS + 1;
  
  private static final LockedDomainPrefixGenerator prefixGen = new LockedDomainPrefixGenerator() {
    public String getLockedDomainPrefix(Uri gadgetUri) {
      return LD_PREFIX;
    }
  };

  private static final SecurityTokenCodec tokenCodec = new BasicSecurityTokenCodec();
  
  @Test
  public void typeHtmlBasicOptions() {
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
    assertEquals(prefVal, uri.getFragmentParameter("up_" + prefKey));
    
    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(1, uri.getFragmentParameters().size());
    
    assertFalse(manager.tokenForRenderingCalled());
    assertTrue(manager.schemeCalled());
    assertTrue(manager.ldExclusionCalled());
    assertTrue(manager.addExtrasCalled());
  }
    
  @Test
  public void typeHtmlBasicOptionsTpl() {
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
    assertEquals(tplKey("up_" + prefKey), uriTpl.getFragmentParameter("up_" + prefKey));
    
    // Only the params that are needed.
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uriTpl.getQueryParameters().size());
    assertEquals(1, uriTpl.getFragmentParameters().size());
    
    assertFalse(managerTpl.tokenForRenderingCalled());
    assertTrue(managerTpl.schemeCalled());
    assertTrue(managerTpl.ldExclusionCalled());
    assertTrue(managerTpl.addExtrasCalled());
  }
  
  @Test
  public void typeUrlDefaultOptions() {
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
    assertEquals("1", uri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("1", uri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(prefVal, uri.getFragmentParameter("up_" + prefKey));
    
    // Only the params that are needed.
    assertEquals(TYPE_URL_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(1, uri.getFragmentParameters().size());
    
    assertFalse(manager.tokenForRenderingCalled());
    assertFalse(manager.schemeCalled());
    assertFalse(manager.ldExclusionCalled());
    assertTrue(manager.addExtrasCalled());
  }
  
  @Test
  public void typeUrlDefaultOptionsTpl() {
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
    assertFalse(managerTpl.ldExclusionCalled());
    assertTrue(managerTpl.addExtrasCalled());
  }
  
  @Test
  public void securityTokenAddedWhenGadgetNeedsItFragment() {
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
  }
  
  @Test
  public void securityTokenAddedWhenGadgetNeedsItQuery() {
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
  }
  
  @Test
  public void securityTokenAddedWhenForced() {
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
  }
  
  @Test
  public void ldAddedGadgetRequests() {
    Gadget gadget = mockGadget(LOCKED_DOMAIN_FEATURE_NAME);
    
    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        false);  // locked domain not (always) required
    
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    
    UriBuilder uri = new UriBuilder(result);
    assertEquals("", uri.getScheme());
    assertEquals(LD_PREFIX + LD_SUFFIX, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());
    
    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());
  }
  
  @Test
  public void ldAddedForcedAlways() {
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
  }
  
  @Test
  public void ldNotAddedIfDisabled() {
    Gadget gadget = mockGadget(LOCKED_DOMAIN_FEATURE_NAME);
    
    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        true);   // locked domain always required
    manager.setLockedDomainEnabled(false);  // but alas, not enabled in the 1st place
    
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    
    UriBuilder uri = new UriBuilder(result);
    assertEquals("", uri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());
    
    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());
  }
  
  @Test
  public void ldNotAddedWithExclusion() {
    Gadget gadget = mockGadget(LOCKED_DOMAIN_FEATURE_NAME);
    
    TestDefaultIframeUriManager manager = makeManager(
        false,   // security token beacon not required
        true);   // locked domain always required
    manager.setLdExclusion(true);  // but alas, excluded
    
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    
    UriBuilder uri = new UriBuilder(result);
    assertEquals("", uri.getScheme());
    assertEquals(UNLOCKED_DOMAIN, uri.getAuthority());
    assertEquals(IFRAME_PATH, uri.getPath());
    
    // Basic sanity checks on params
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS, uri.getQueryParameters().size());
    assertEquals(0, uri.getFragmentParameters().size());
  }
  
  @Test
  public void versionAddedWithVersioner() {
    String version = "abcdlkjwef";
    Gadget gadget = mockGadget();
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(this.mockVersioner(version, true));
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);
    assertEquals(TYPE_HTML_NUM_BASE_PARAMS + 1, uri.getQueryParameters().size());
    assertEquals(version, uri.getQueryParameter(Param.VERSION.getKey()));
  }
  
  @Test
  public void userPrefsAddedQuery() {
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
  }
  
  @Test
  public void userPrefsAddedFragment() {
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
  }
  
  @Test
  public void honorSchemeOverride() {
    String scheme = "file";
    Gadget gadget = mockGadget();
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setScheme(scheme);
    Uri result = manager.makeRenderingUri(gadget);
    assertNotNull(result);
    UriBuilder uri = new UriBuilder(result);
    assertEquals(scheme, uri.getScheme());
  }
  
  @Test
  public void badUriValidatingUri() {
    Uri uri = new UriBuilder().addQueryParameter(Param.URL.getKey(), "^':   bad:").toUri();
    TestDefaultIframeUriManager manager = makeManager(false, false);
    UriStatus status = manager.validateRenderingUri(uri);
    assertEquals(UriStatus.BAD_URI, status);
  }
  
  @Test
  public void invalidLockedDomainValidSuffix() {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX_ALT, null);
    DefaultIframeUriManager manager = makeManager(false, false);
    assertEquals(UriStatus.INVALID_DOMAIN, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void invalidLockedDomainInvalidSuffix() {
    Uri uri = makeValidationTestUri(LD_PREFIX + ".bad." + LD_SUFFIX, null);
    DefaultIframeUriManager manager = makeManager(false, false);
    assertEquals(UriStatus.INVALID_DOMAIN, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void invalidLockedDomainValidSuffixExclusionBypass() {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX_ALT, null);
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setLdExclusion(true);
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void invalidLockedDomainInvalidSuffixExclusionBypass() {
    Uri uri = makeValidationTestUri(LD_PREFIX + ".bad." + LD_SUFFIX, null);
    TestDefaultIframeUriManager manager = makeManager(false, false);
    manager.setLdExclusion(true);
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void invalidLockedDomainValidSuffixLdDisabled() {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX_ALT, null);
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setLockedDomainEnabled(false);
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void invalidLockedDomainInvalidSuffixLdDisabled() {
    Uri uri = makeValidationTestUri(LD_PREFIX + ".bad." + LD_SUFFIX, null);
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setLockedDomainEnabled(false);
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void validUnversionedNoVersioner() {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, "version");
    DefaultIframeUriManager manager = makeManager(false, false);
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void validUnversionedNoVersion() {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, null);
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(this.mockVersioner("version", false));  // Invalid, if present.
    assertEquals(UriStatus.VALID_UNVERSIONED, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void versionerVersionInvalid() {
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, "in-version");
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(mockVersioner("test-version", false));  // Invalid, if present.
    assertEquals(UriStatus.INVALID_VERSION, manager.validateRenderingUri(uri));
  }
  
  @Test
  public void versionerVersionMatch() {
    String version = "abcdefg";
    Uri uri = makeValidationTestUri(LD_PREFIX + LD_SUFFIX, version);
    DefaultIframeUriManager manager = makeManager(false, false);
    manager.setVersioner(mockVersioner(version, true));
    assertEquals(UriStatus.VALID_VERSIONED, manager.validateRenderingUri(uri));
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
    
  private TestDefaultIframeUriManager makeManager(boolean alwaysToken, boolean ldRequired) {
    ContainerConfig config = createMock(ContainerConfig.class);
    String altContainer = CONTAINER + "-alt";
    Collection<String> containers = Lists.newArrayList(CONTAINER, altContainer);
    expect(config.getContainers()).andReturn(containers).anyTimes();
    expect(config.getString(CONTAINER, IFRAME_BASE_PATH_KEY)).andReturn(IFRAME_PATH).anyTimes();
    expect(config.getString(CONTAINER, LOCKED_DOMAIN_SUFFIX_KEY)).andReturn(LD_SUFFIX).anyTimes();
    expect(config.getString(altContainer, LOCKED_DOMAIN_SUFFIX_KEY))
        .andReturn(LD_SUFFIX_ALT).anyTimes();
    expect(config.getString(CONTAINER, UNLOCKED_DOMAIN_KEY)).andReturn(UNLOCKED_DOMAIN).anyTimes();
    expect(config.getBool(CONTAINER, SECURITY_TOKEN_ALWAYS_KEY)).andReturn(alwaysToken).anyTimes();
    expect(config.getBool(CONTAINER, LOCKED_DOMAIN_REQUIRED_KEY)).andReturn(ldRequired).anyTimes();
    replay(config);
    return new TestDefaultIframeUriManager(config);
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
    private boolean ldExclusion = false;
    private boolean ldExclusionCalled = false;
    private String scheme = "";
    private boolean schemeCalled = false;
    private boolean tokenForRendering = true;
    private boolean tokenForRenderingCalled = false;
    private boolean addExtrasCalled = false;
    
    private TestDefaultIframeUriManager(ContainerConfig config) {
      super(config, prefixGen, tokenCodec);
    }
    
    private TestDefaultIframeUriManager setLdExclusion(boolean ldExclusion) {
      this.ldExclusion = ldExclusion;
      return this;
    }
    
    private TestDefaultIframeUriManager setScheme(String scheme) {
      this.scheme = scheme;
      return this;
    }
    
    private TestDefaultIframeUriManager setTokenForRendering(boolean tokenForRendering) {
      this.tokenForRendering = tokenForRendering;
      return this;
    }
    
    /** Overridden methods for custom behavior */
    @Override
    protected boolean lockedDomainExclusion() {
      this.ldExclusionCalled = true;
      return ldExclusion;
    }
    
    private boolean ldExclusionCalled() {
      return ldExclusionCalled;
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
    protected void addExtras(UriBuilder uri) {
      this.addExtrasCalled = true;
    }
    
    private boolean addExtrasCalled() {
      return addExtrasCalled;
    }
  }
}
