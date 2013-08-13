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

import static org.apache.shindig.gadgets.uri.DefaultJsUriManager.JS_SUFFIX;
import static org.apache.shindig.gadgets.uri.DefaultJsUriManager.addJsLibs;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.JsUriManager.Versioner;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultJsUriManagerTest {
  private static final String CONTAINER = "container";
  private static final String GADGET_URI = "http://example.com/gadget.xml";

  // makeJsUri tests
  @Test(expected = RuntimeException.class)
  public void makeMissingHostConfig() {
    ContainerConfig config = mockConfig(null, "/gadgets/js");
    DefaultJsUriManager manager = makeManager(config, null);
    JsUri ctx = mockGadgetContext(false, false, null);
    manager.makeExternJsUri(ctx);
  }

  @Test(expected = RuntimeException.class)
  public void makeMissingPathConfig() {
    ContainerConfig config = mockConfig("foo", null);
    DefaultJsUriManager manager = makeManager(config, null);
    JsUri ctx = mockGadgetContext(false, false, null);
    manager.makeExternJsUri(ctx);
  }

  @Test
  public void makeJsUriNoPathSlashNoVersion() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js/");
    TestDefaultJsUriManager manager = makeManager(config, null);
    List<String> extern = Lists.newArrayList("feature");
    JsUri ctx = mockGadgetContext(false, false, extern);
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(RenderingContext.GADGET.getParamValue(),
        jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  @Test
  public void makeJsUriExtensionParams() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js/");
    TestDefaultJsUriManager manager = makeManager(config, null);
    List<String> extern = Lists.newArrayList("feature");
    JsUri ctx = mockGadgetContext(false, false, extern, null, false,
        ImmutableMap.of("test", "1"), null, "rep");
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("1", jsUri.getQueryParameter("test"));
    assertEquals("rep", jsUri.getQueryParameter(Param.REPOSITORY_ID.getKey()));
  }

  @Test
  public void makeJsUriAddPathSlashNoVersion() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js");
    TestDefaultJsUriManager manager = makeManager(config, null);
    List<String> extern = Lists.newArrayList("feature");
    JsUri ctx = mockGadgetContext(false, false, extern);
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
    assertNull(jsUri.getQueryParameters(Param.REPOSITORY_ID.getKey()));
  }

  @Test
  public void makeJsUriAddPathSlashVersioned() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js");
    List<String> extern = Lists.newArrayList("feature");
    JsUri ctx = mockGadgetContext(false, false, extern);
    String version = "verstring";
    Versioner versioner = this.mockVersioner(ctx, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(version, jsUri.getQueryParameter(Param.VERSION.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(RenderingContext.GADGET.getParamValue(),
        jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  @Test
  public void makeJsUriWithVersionerNoVersionOnIgnoreCache() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js");
    List<String> extern = Lists.newArrayList("feature");
    JsUri ctx = mockGadgetContext(true, false, extern);  // no cache
    String version = "verstring";
    Versioner versioner = this.mockVersioner(ctx, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    // No version string appended.
    assertEquals(null, jsUri.getQueryParameter(Param.VERSION.getKey()));
    assertEquals("1", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(RenderingContext.GADGET.getParamValue(),
        jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  @Test
  public void makeJsUriWithContainerContext() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js/");
    TestDefaultJsUriManager manager = makeManager(config, null);
    List<String> extern = Lists.newArrayList("feature", "another");
    JsUri ctx = mockGadgetContext(false, false, extern, null, true, null,
        JsCompileMode.CONCAT_COMPILE_EXPORT_ALL, null);
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL.getParamValue(),
        jsUri.getQueryParameter(Param.COMPILE_MODE.getKey()));
    assertEquals(RenderingContext.CONTAINER.getParamValue(),
        jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  @Test
  public void makeJsUriWithLoadedLibraries() throws Exception {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js/");
    TestDefaultJsUriManager manager = makeManager(config, null);
    List<String> extern = Lists.newArrayList("feature", "another");
    List<String> loaded = Lists.newArrayList("another", "onemore");
    JsUri ctx = mockGadgetContext(false, false, extern, loaded);
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + "!" + addJsLibs(loaded) +
        JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(RenderingContext.GADGET.getParamValue(),
        jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  // processJsUri tests
  @Test
  public void processDefaultConfig() throws GadgetException {
    ContainerConfig config = mockDefaultConfig("foo", "/gadgets/js");
    DefaultJsUriManager manager = makeManager(config, null);
    manager.processExternJsUri(Uri.parse("http://example.com?container=" + CONTAINER));
  }

  @Test
  public void processPathPrefixMismatch() throws GadgetException {
    String targetHost = "target-host.org";
    ContainerConfig config = mockConfig("http://" + targetHost, "/gadgets/js");
    TestDefaultJsUriManager manager = makeManager(config, null);
    Uri testUri = Uri.parse("http://target-host.org/gadgets/other-js/feature" + JS_SUFFIX + '?' +
        Param.CONTAINER.getKey() + '=' + CONTAINER);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    List<String> extern = Lists.newArrayList("feature");
    assertEquals(extern, jsUri.getLibs());
    List<String> loaded = Lists.newArrayList();
    assertEquals(loaded, jsUri.getLoadedLibs());
    assertEquals(CONTAINER, jsUri.getContainer());
    assertEquals(RenderingContext.GADGET, jsUri.getContext());
  }

  @Test
  public void processPathWithEncodedSeparator() throws GadgetException {
    String targetHost = "target-host.org";
    ContainerConfig config = mockConfig("http://" + targetHost, "/gadgets/js");
    TestDefaultJsUriManager manager = makeManager(config, null);
    Uri testUri = Uri.parse("http://target-host.org/gadgets/js/feature%3Aanother?" +
        Param.CONTAINER.getKey() + '=' + CONTAINER);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    assertEquals(jsUri.getStatus(), UriStatus.VALID_UNVERSIONED);
    List<String> extern = Lists.newArrayList("feature", "another");
    assertCollectionEquals(jsUri.getLibs(), extern);
  }

  @Test
  public void processPathSuffixNoJs() throws GadgetException {
    String targetHost = "target-host.org";
    ContainerConfig config = mockConfig("http://" + targetHost, "/gadgets/js");
    TestDefaultJsUriManager manager = makeManager(config, null);
    Uri testUri = Uri.parse("http://target-host.org/gadgets/js/feature:another?" +
        Param.CONTAINER.getKey() + '=' + CONTAINER);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    assertEquals(jsUri.getStatus(), UriStatus.VALID_UNVERSIONED);
    List<String> extern = Lists.newArrayList("feature", "another");
    assertCollectionEquals(jsUri.getLibs(), extern);
  }

  @Test
  public void processPathWithLoadedJs() throws GadgetException {
    ContainerConfig config = mockConfig("http://host", "/gadgets/js");
    TestDefaultJsUriManager manager = makeManager(config, null);
    Uri testUri = Uri.parse("http://host/gadgets/js/feature:another!load1:load2.js?" +
        Param.LOADED.getKey() + "=load3:load4&" +
        Param.CONTAINER.getKey() + '=' + CONTAINER);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    List<String> extern = Lists.newArrayList("feature", "another");
    assertCollectionEquals(jsUri.getLibs(), extern);
    assertCollectionEquals(jsUri.getLoadedLibs(), Lists.newArrayList(
        "load1", "load2", "load3", "load4"));
  }

  @Test
  public void processValidUnversionedNoVersioner() throws GadgetException {
    String targetHost = "target-host.org";
    ContainerConfig config = mockConfig("http://" + targetHost, "/gadgets/js");
    List<String> extern = Lists.newArrayList("feature", "another");
    String version = "verstring";
    TestDefaultJsUriManager manager = makeManager(config, null);
    Uri testUri = Uri.parse("http://target-host.org/gadgets/js/" + addJsLibs(extern) +
        JS_SUFFIX + '?' + Param.CONTAINER.getKey() + '=' + CONTAINER + '&' +
        Param.VERSION.getKey() + '=' + version);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    assertEquals(jsUri.getStatus(), UriStatus.VALID_UNVERSIONED);
    assertCollectionEquals(jsUri.getLibs(), extern);
  }

  @Test
  public void processValidInvalidVersion() throws GadgetException {
    String targetHost = "target-host.org";
    ContainerConfig config = mockConfig("http://" + targetHost, "/gadgets/js");
    List<String> extern = Lists.newArrayList("feature", "another");
    String version = "verstring";
    String badVersion = version + "-a";
    JsUri ctx = mockGadgetContext(false, false, extern);
    Versioner versioner = mockVersioner(ctx, version, badVersion);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri testUri = Uri.parse("http://target-host.org/gadgets/js/" + addJsLibs(extern) +
        JS_SUFFIX + '?' + Param.CONTAINER.getKey() + '=' + CONTAINER + '&' +
        Param.VERSION.getKey() + '=' + badVersion);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    assertEquals(jsUri.getStatus(), UriStatus.INVALID_VERSION);
    assertCollectionEquals(jsUri.getLibs(), extern);
  }

  @Test
  public void processValidUnversionedNoParam() throws GadgetException {
    String targetHost = "target-host.org";
    ContainerConfig config = mockConfig("http://" + targetHost, "/gadgets/js");
    List<String> extern = Lists.newArrayList("feature", "another");
    String version = "verstring";
    JsUri ctx = mockGadgetContext(false, false, extern);
    Versioner versioner = mockVersioner(ctx, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri testUri = Uri.parse("http://target-host.org/gadgets/js/" + addJsLibs(extern) +
        JS_SUFFIX + '?' + Param.CONTAINER.getKey() + '=' + CONTAINER);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    assertEquals(jsUri.getStatus(), UriStatus.VALID_UNVERSIONED);
    assertCollectionEquals(jsUri.getLibs(), extern);
  }

  @Test
  public void processValidVersioned() throws GadgetException {
    String targetHost = "target-host.org";
    ContainerConfig config = mockConfig("http://" + targetHost, "/gadgets/js");
    List<String> extern = Lists.newArrayList("feature", "another");
    String version = "verstring";
    JsUri ctx = mockGadgetContext(false, false, extern);
    Versioner versioner = mockVersioner(ctx, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri testUri = Uri.parse("http://target-host.org/gadgets/js/" + addJsLibs(extern) +
        JS_SUFFIX + '?' + Param.CONTAINER.getKey() + '=' + CONTAINER + '&' +
        Param.VERSION.getKey() + '=' + version);
    JsUri jsUri = manager.processExternJsUri(testUri);
    assertFalse(manager.hadError());
    assertEquals(jsUri.getStatus(), UriStatus.VALID_VERSIONED);
    assertCollectionEquals(jsUri.getLibs(), extern);
  }

  // end-to-end integration-ish test: makeX builds a Uri that processX correctly handles
  @Test
  public void makeAndProcessSymmetric() throws GadgetException {
    // Make...
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js");
    List<String> extern = Lists.newArrayList("feature1", "feature2", "feature3");
    JsUri ctx = mockGadgetContext(false, false, extern);
    String version = "verstring";
    Versioner versioner = mockVersioner(ctx, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri jsUri = manager.makeExternJsUri(ctx);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(version, jsUri.getQueryParameter(Param.VERSION.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));

    // ...and process
    JsUri processed = manager.processExternJsUri(jsUri);
    assertEquals(UriStatus.VALID_VERSIONED, processed.getStatus());
    assertCollectionEquals(extern, processed.getLibs());
  }

  private void assertCollectionEquals(Collection<String> expected, Collection<String> test) {
    assertEquals(expected.size(), test.size());
    List<String> expectedCopy = Lists.newArrayList(expected);
    List<String> testCopy = Lists.newArrayList(test);
    assertEquals(expectedCopy, testCopy);
  }

  private ContainerConfig mockConfig(String jsHost, String jsPath) {
    ContainerConfig config = createMock(ContainerConfig.class);
    expect(config.getString(CONTAINER, DefaultJsUriManager.JS_HOST_PARAM))
        .andReturn(jsHost).anyTimes();
    expect(config.getString(CONTAINER, DefaultJsUriManager.JS_PATH_PARAM))
        .andReturn(jsPath).anyTimes();
    expect(config.getString(ContainerConfig.DEFAULT_CONTAINER, DefaultJsUriManager.JS_HOST_PARAM))
        .andReturn(null).anyTimes();
    expect(config.getString(ContainerConfig.DEFAULT_CONTAINER, DefaultJsUriManager.JS_PATH_PARAM))
        .andReturn(null).anyTimes();
    replay(config);
    return config;
  }

  private ContainerConfig mockDefaultConfig(String jsHost, String jsPath) {
    ContainerConfig config = createMock(ContainerConfig.class);
    expect(config.getString(CONTAINER, DefaultJsUriManager.JS_HOST_PARAM))
        .andReturn(null).anyTimes();
    expect(config.getString(CONTAINER, DefaultJsUriManager.JS_PATH_PARAM))
        .andReturn(null).anyTimes();
    expect(config.getString(ContainerConfig.DEFAULT_CONTAINER, DefaultJsUriManager.JS_HOST_PARAM))
        .andReturn(jsHost).anyTimes();
    expect(config.getString(ContainerConfig.DEFAULT_CONTAINER, DefaultJsUriManager.JS_PATH_PARAM))
        .andReturn(jsPath).anyTimes();
    replay(config);
    return config;
  }

  private Versioner mockVersioner(JsUri jsUri, String genVersion, String testVersion) {
    JsUriManager.Versioner versioner = createMock(Versioner.class);
    expect(versioner.version(jsUri)).andStubReturn(genVersion);
    UriStatus status = (genVersion != null && genVersion.equals(testVersion)) ?
        UriStatus.VALID_VERSIONED : UriStatus.INVALID_VERSION;
    expect(versioner.validate(isA(JsUri.class), eq(testVersion))).andStubReturn(status);
    replay(versioner);
    return versioner;
  }

  private JsUri mockGadgetContext(boolean nocache, boolean debug, List<String> extern) {
    return mockGadgetContext(nocache, debug, extern, ImmutableList.<String>of(), false,
    null, null, null);
  }

  private JsUri mockGadgetContext(
      boolean nocache, boolean debug, List<String> extern, List<String> loaded) {
    return mockGadgetContext(nocache, debug, extern, loaded, false, null, null, null);
  }

  private JsUri mockGadgetContext(boolean nocache, boolean debug,
      List<String> extern, List<String> loaded,
      boolean isContainer, Map<String, String> params,
      JsCompileMode compileMode, String repository) {
    JsUri context = createMock(JsUri.class);
    expect(context.getContainer()).andStubReturn(CONTAINER);
    expect(context.isNoCache()).andStubReturn(nocache);
    expect(context.isDebug()).andStubReturn(debug);
    expect(context.getGadget()).andStubReturn(GADGET_URI);
    expect(context.getContext()).andStubReturn(
        isContainer ? RenderingContext.CONTAINER : RenderingContext.GADGET);
    expect(context.getLibs()).andStubReturn(extern);
    expect(context.getLoadedLibs()).andStubReturn(
        loaded == null ? ImmutableList.<String>of() : loaded);
    expect(context.getOnload()).andStubReturn(null);
    expect(context.isJsload()).andStubReturn(false);
    expect(context.isNohint()).andStubReturn(false);
    expect(context.getExtensionParams()).andStubReturn(params);
    expect(context.getOrigUri()).andStubReturn(null);
    expect(context.getCompileMode()).andStubReturn(compileMode);
    expect(context.cajoleContent()).andStubReturn(false);
    expect(context.getRepository()).andStubReturn(repository);
    replay(context);
    return context;
  }

  private TestDefaultJsUriManager makeManager(ContainerConfig config, Versioner versioner) {
    return new TestDefaultJsUriManager(config, versioner);
  }

  private static final class TestDefaultJsUriManager extends DefaultJsUriManager {
    private boolean errorReported = false;

    private TestDefaultJsUriManager(ContainerConfig config, Versioner versioner) {
      super(config, versioner);
    }

    @Override
    protected void issueUriFormatError(String err) {
      this.errorReported = true;
    }

    public boolean hadError() {
      return errorReported;
    }
  }
}
