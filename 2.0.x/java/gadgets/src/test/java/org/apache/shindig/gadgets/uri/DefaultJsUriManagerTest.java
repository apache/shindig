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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.JsUriManager.Versioner;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

public class DefaultJsUriManagerTest {
  private static final String CONTAINER = "container";
  private static final Uri GADGET_URI = Uri.parse("http://example.com/gadget.xml");

  // makeJsUri tests
  @Test(expected = RuntimeException.class)
  public void makeMissingHostConfig() {
    ContainerConfig config = mockConfig(null, "/gadgets/js");
    DefaultJsUriManager manager = makeManager(config, null);
    Gadget gadget = mockGadget(false, false);
    manager.makeExternJsUri(gadget, null);
  }

  @Test(expected = RuntimeException.class)
  public void makeMissingPathConfig() {
    ContainerConfig config = mockConfig("foo", null);
    DefaultJsUriManager manager = makeManager(config, null);
    Gadget gadget = mockGadget(false, false);
    manager.makeExternJsUri(gadget, null);
  }

  @Test
  public void makeJsUriNoPathSlashNoVersion() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js/");
    TestDefaultJsUriManager manager = makeManager(config, null);
    Gadget gadget = mockGadget(false, false);
    List<String> extern = Lists.newArrayList("feature");
    Uri jsUri = manager.makeExternJsUri(gadget, extern);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  @Test
  public void makeJsUriAddPathSlashNoVersion() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js");
    TestDefaultJsUriManager manager = makeManager(config, null);
    Gadget gadget = mockGadget(false, false);
    List<String> extern = Lists.newArrayList("feature");
    Uri jsUri = manager.makeExternJsUri(gadget, extern);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  @Test
  public void makeJsUriAddPathSlashVersioned() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js");
    Gadget gadget = mockGadget(false, false);
    List<String> extern = Lists.newArrayList("feature");
    String version = "verstring";
    Versioner versioner = this.mockVersioner(extern, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri jsUri = manager.makeExternJsUri(gadget, extern);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(version, jsUri.getQueryParameter(Param.VERSION.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  @Test
  public void makeJsUriWithVersionerNoVersionOnIgnoreCache() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js");
    Gadget gadget = mockGadget(true, false);  // no cache
    List<String> extern = Lists.newArrayList("feature");
    String version = "verstring";
    Versioner versioner = this.mockVersioner(extern, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri jsUri = manager.makeExternJsUri(gadget, extern);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    // No version string appended.
    assertEquals(null, jsUri.getQueryParameter(Param.VERSION.getKey()));
    assertEquals("1", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }
  
  @Test
  public void makeJsUriWithContainerContext() {
    ContainerConfig config = mockConfig("http://www.js.org", "/gadgets/js/");
    TestDefaultJsUriManager manager = makeManager(config, null);
    Gadget gadget = mockGadget(false, false, true);
    List<String> extern = Lists.newArrayList("feature", "another");
    Uri jsUri = manager.makeExternJsUri(gadget, extern);
    assertFalse(manager.hadError());
    assertEquals("http", jsUri.getScheme());
    assertEquals("www.js.org", jsUri.getAuthority());
    assertEquals("/gadgets/js/" + addJsLibs(extern) + JS_SUFFIX, jsUri.getPath());
    assertEquals(CONTAINER, jsUri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals("0", jsUri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("1", jsUri.getQueryParameter(Param.CONTAINER_MODE.getKey()));
  }

  // processJsUri tests
  @Test(expected = RuntimeException.class)
  public void processMissingHostConfig() throws GadgetException {
    ContainerConfig config = mockConfig(null, "/gadgets/js");
    DefaultJsUriManager manager = makeManager(config, null);
    manager.processExternJsUri(Uri.parse("http://example.com?container=" + CONTAINER));
  }

  @Test(expected = RuntimeException.class)
  public void processMissingPathConfig() throws GadgetException {
    ContainerConfig config = mockConfig("foo", null);
    DefaultJsUriManager manager = makeManager(config, null);
    manager.processExternJsUri(Uri.parse("http://example.com?container=" + CONTAINER));
  }

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
    assertTrue(manager.hadError());
    assertEquals(jsUri.getStatus(), UriStatus.BAD_URI);
    assertSame(DefaultJsUriManager.INVALID_URI, jsUri);
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
    Versioner versioner = mockVersioner(extern, version, badVersion);
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
    Versioner versioner = mockVersioner(extern, version, version);
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
    Versioner versioner = mockVersioner(extern, version, version);
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
    Gadget gadget = mockGadget(false, false);
    List<String> extern = Lists.newArrayList("feature1", "feature2", "feature3");
    String version = "verstring";
    Versioner versioner = mockVersioner(extern, version, version);
    TestDefaultJsUriManager manager = makeManager(config, versioner);
    Uri jsUri = manager.makeExternJsUri(gadget, extern);
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

  private Versioner mockVersioner(
      Collection<String> extern, String genVersion, String testVersion) {
    JsUriManager.Versioner versioner = createMock(Versioner.class);
    expect(versioner.version(GADGET_URI, CONTAINER, extern)).andReturn(genVersion).anyTimes();
    expect(versioner.version(null, CONTAINER, extern)).andReturn(genVersion).anyTimes();
    UriStatus status = (genVersion != null && genVersion.equals(testVersion)) ?
          UriStatus.VALID_VERSIONED : UriStatus.INVALID_VERSION;
    expect(versioner.validate(GADGET_URI, CONTAINER, extern, testVersion))
        .andReturn(status).anyTimes();
    expect(versioner.validate(null, CONTAINER, extern, testVersion))
        .andReturn(status).anyTimes();
    replay(versioner);
    return versioner;
  }

  private Gadget mockGadget(boolean nocache, boolean debug) {
    return mockGadget(nocache, debug, false);
  }
  
  private Gadget mockGadget(boolean nocache, boolean debug, boolean isContainer) {
    GadgetContext context = createMock(GadgetContext.class);
    expect(context.getContainer()).andReturn(CONTAINER).anyTimes();
    expect(context.getIgnoreCache()).andReturn(nocache).anyTimes();
    expect(context.getDebug()).andReturn(debug).anyTimes();
    expect(context.getUrl()).andReturn(GADGET_URI).anyTimes();
    expect(context.getRenderingContext()).andReturn(
        isContainer ? RenderingContext.CONTAINER : RenderingContext.GADGET).anyTimes();
    replay(context);
    return new Gadget().setContext(context);
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
