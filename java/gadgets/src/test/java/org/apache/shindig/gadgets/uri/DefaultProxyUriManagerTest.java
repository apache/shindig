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

import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.eq;
import static org.easymock.classextension.EasyMock.isA;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import org.junit.Test;

import java.util.List;

public class DefaultProxyUriManagerTest extends UriManagerTestBase {
  private static final Uri RESOURCE_1 = Uri.parse("http://example.com/one.dat?param=value");
  private static final Uri RESOURCE_2 = Uri.parse("http://gadgets.com/two.dat");
  private static final Uri RESOURCE_3 = Uri.parse("http://foobar.com/three.dat");
  
  @Test
  public void basicProxyQueryStyle() throws Exception {
    checkQueryStyle(false, false, null);
  }
  
  @Test
  public void altParamsProxyQueryStyle() throws Exception {
    checkQueryStyle(true, true, "version");
  }
  
  private void checkQueryStyle(boolean debug, boolean noCache, String version) throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    List<Uri> resources = ImmutableList.<Uri>of(RESOURCE_1);
    List<Uri> uris = makeAndGet(host, path, debug, noCache, resources, version);
    assertEquals(1, uris.size());
    verifyQueryUri(RESOURCE_1, uris.get(0), debug, noCache, version, host, path);
  }
  
  @Test
  public void basicProxyChainedStyle() throws Exception {
    checkChainedStyle(false, false, null);
  }
  
  @Test
  public void altParamsProxyChainedStyle() throws Exception {
    checkChainedStyle(true, true, "version");
  }
  
  private void checkChainedStyle(boolean debug, boolean noCache, String version) throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path";
    List<Uri> resources = ImmutableList.<Uri>of(RESOURCE_1);
    List<Uri> uris = makeAndGet(host, path, debug, noCache, resources, version);
    assertEquals(1, uris.size());
    verifyChainedUri(RESOURCE_1, uris.get(0), debug, noCache, version, false, host, path);
  }
  
  @Test
  public void basicProxyChainedStyleEndOfPath() throws Exception {
    checkChainedStyleEndOfPath(false, false, null);
  }
  
  @Test
  public void altParamsProxyChainedStyleEndOfPath() throws Exception {
    checkChainedStyleEndOfPath(true, true, "version");
  }
  
  private void checkChainedStyleEndOfPath(boolean debug, boolean noCache, String version) throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN;
    List<Uri> resources = ImmutableList.<Uri>of(RESOURCE_1);
    List<Uri> uris = makeAndGet(host, path, debug, noCache, resources, version);
    assertEquals(1, uris.size());
    verifyChainedUri(RESOURCE_1, uris.get(0), debug, noCache, version, true, host, path);
  }
  
  @Test
  public void batchedProxyQueryStyle() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    List<Uri> resources = ImmutableList.<Uri>of(RESOURCE_1, RESOURCE_2, RESOURCE_3);
    String[] versions = new String[] { "v1", "v2", "v3" };
    List<Uri> uris = makeAndGet(host, path, true, true, resources, versions);
    assertEquals(3, uris.size());
    for (int i = 0; i < 3; ++i) {
      verifyQueryUri(resources.get(i), uris.get(i), true, true, versions[i], host, path);
    }
  }
  
  @Test
  public void batchedProxyChainedStyle() throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path";
    List<Uri> resources = ImmutableList.<Uri>of(RESOURCE_1, RESOURCE_2, RESOURCE_3);
    String[] versions = new String[] { "v1", "v2", "v3" };
    List<Uri> uris = makeAndGet(host, path, true, true, resources, versions);
    assertEquals(3, uris.size());
    for (int i = 0; i < 3; ++i) {
      verifyChainedUri(resources.get(i), uris.get(i), true, true, versions[i], false, host, path);
    }
  }
  
  @Test
  public void validateQueryStyleUnversioned() throws Exception {
    // Validate tests also serve as end-to-end tests: create, unpack.
    checkValidate("/proxy/path", UriStatus.VALID_UNVERSIONED, null);
  }
  
  @Test
  public void validateChainedStyleUnversioned() throws Exception {
    checkValidate("/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path",
        UriStatus.VALID_UNVERSIONED, null);
  }
  
  @Test
  public void validateQueryStyleVersioned() throws Exception {
    checkValidate("/proxy/path", UriStatus.VALID_VERSIONED, "version");
  }
  
  @Test
  public void validateChainedStyleVersioned() throws Exception {
    checkValidate("/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path",
        UriStatus.VALID_VERSIONED, "version");
  }
  
  private void checkValidate(String path, UriStatus status, String version) throws Exception {
    String host = "host.com";
    // Pass null for status if version is null, since null version shouldn't result
    // in a check to the versioner.
    ProxyUriManager.Versioner versioner = makeVersioner(version == null ? null : status, version);
    DefaultProxyUriManager manager = makeManager(host, path, versioner);
    Gadget gadget = mockGadget(false, false);
    List<Uri> resources = ImmutableList.of(RESOURCE_1);
    List<Uri> uris = manager.make(
        ProxyUriManager.ProxyUri.fromList(gadget, resources), 123);
    assertEquals(1, uris.size());
    ProxyUriManager.ProxyUri proxyUri = manager.process(uris.get(0));
    assertEquals(RESOURCE_1, proxyUri.getResource());
    assertEquals(CONTAINER, proxyUri.getContainer());
    assertEquals(SPEC_URI.toString(), proxyUri.getGadget());
    assertEquals(123, (int)proxyUri.getRefresh());
    assertEquals(status, proxyUri.getStatus());
    assertEquals(false, proxyUri.isDebug());
    assertEquals(false, proxyUri.isNoCache());
  }
  
  @Test(expected = GadgetException.class)
  public void mismatchedHostStrict() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    DefaultProxyUriManager manager = makeManager("foo" + host, path, null);
    manager.setUseStrictParsing("true");
    Uri testUri = new UriBuilder().setAuthority(host).setPath(path)
        .addQueryParameter(Param.URL.getKey(), "http://foo.com").toUri();
    manager.process(testUri);
  }
  
  @Test
  public void mismatchedHostNonStrict() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    DefaultProxyUriManager manager = makeManager("foo" + host, path, null);
    Uri testUri = new UriBuilder().setAuthority(host).setPath(path)
        .addQueryParameter(Param.URL.getKey(), "http://foo.com")
        .addQueryParameter(Param.CONTAINER.getKey(), CONTAINER).toUri();
    manager.process(testUri);
  }
  
  @Test(expected = GadgetException.class)
  public void missingContainerParamQuery() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    DefaultProxyUriManager manager = makeManager(host, path, null);
    Uri testUri = new UriBuilder().setAuthority(host).setPath(path)
        .addQueryParameter(Param.URL.getKey(), "http://foo.com").toUri();
    manager.process(testUri);
  }
  
  @Test(expected = GadgetException.class)
  public void missingContainerParamChained() throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path";
    DefaultProxyUriManager manager = makeManager(host, path, null);
    Uri testUri = new UriBuilder().setAuthority(host).setPath(
        "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_START_BEACON + "&refresh=123" +
        DefaultProxyUriManager.CHAINED_PARAMS_END_BEACON + "/path/http://foo.com").toUri();
    manager.process(testUri);
  }
  
  @Test(expected = GadgetException.class)
  public void missingUrlQuery() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    DefaultProxyUriManager manager = makeManager(host, path, null);
    Uri testUri = new UriBuilder().setAuthority(host).setPath(path)
        .addQueryParameter(Param.CONTAINER.getKey(), CONTAINER).toUri();
    manager.process(testUri);
  }
  
  @Test(expected = GadgetException.class)
  public void missingUrlChained() throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path";
    DefaultProxyUriManager manager = makeManager(host, path, null);
    Uri testUri = new UriBuilder().setAuthority(host).setPath(
        "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_START_BEACON + "&container=" +
        CONTAINER + DefaultProxyUriManager.CHAINED_PARAMS_END_BEACON + "/path/").toUri();
    manager.process(testUri);
  }
  
  @Test(expected = GadgetException.class)
  public void invalidUrlParamQuery() throws Exception {
    // Only test query style, since chained style should be impossible.
    String host = "host.com";
    String path = "/proxy/path";
    DefaultProxyUriManager manager = makeManager(host, path, null);
    Uri testUri = new UriBuilder().setAuthority(host).setPath(path)
        .addQueryParameter(Param.CONTAINER.getKey(), CONTAINER)
        .addQueryParameter(Param.URL.getKey(), "!^!").toUri();
    manager.process(testUri);
  }

  private List<Uri> makeAndGet(String host, String path, boolean debug, boolean noCache,
      List<Uri> resources, String... version) {
    ProxyUriManager.Versioner versioner = makeVersioner(null, version);
    DefaultProxyUriManager manager = makeManager(host, path, versioner);
    Gadget gadget = mockGadget(debug, noCache);
    return manager.make(
        ProxyUriManager.ProxyUri.fromList(gadget, resources), 123);
  }
  
  private void verifyQueryUri(Uri orig, Uri uri, boolean debug, boolean noCache, String version,
      String host, String path) throws Exception {
    // Make sure the manager can parse out results.
    DefaultProxyUriManager manager = makeManager(host, path, null);
    ProxyUri proxyUri = manager.process(uri);
    assertEquals(orig, proxyUri.getResource());
    assertEquals(debug, proxyUri.isDebug());
    assertEquals(noCache, proxyUri.isNoCache());
    assertEquals(noCache ? 0 : 123, (int)proxyUri.getRefresh());
    assertEquals(CONTAINER, proxyUri.getContainer());
    assertEquals(SPEC_URI.toString(), proxyUri.getGadget());
    
    // "Raw" query param verification.
    assertEquals("123", uri.getQueryParameter(Param.REFRESH.getKey()));
    if (version != null) {
      assertEquals(version, uri.getQueryParameter(Param.VERSION.getKey()));
    }
  }
  
  private void verifyChainedUri(Uri orig, Uri uri, boolean debug, boolean noCache, String version,
      boolean endOfPath, String host, String path)
      throws Exception {
    // Make sure the manager can parse out results.
    DefaultProxyUriManager manager = makeManager(host, path, null);
    ProxyUri proxyUri = manager.process(uri);
    assertEquals(orig, proxyUri.getResource());
    assertEquals(debug, proxyUri.isDebug());
    assertEquals(noCache, proxyUri.isNoCache());
    assertEquals(noCache ? 0 : 123, (int)proxyUri.getRefresh());
    assertEquals(CONTAINER, proxyUri.getContainer());
    assertEquals(SPEC_URI.toString(), proxyUri.getGadget());
    
    // URI should end with the proxied content.
    String uriStr = uri.toString();
    assertTrue(uriStr.endsWith(orig.toString()));
    
    int proxyEnd = uriStr.indexOf("/proxy/") + "/proxy/".length();
    String paramsUri = uriStr.substring(
        proxyEnd +
        DefaultProxyUriManager.CHAINED_PARAMS_START_BEACON.length(),
        (endOfPath ? uriStr.indexOf("/", proxyEnd) : uriStr.indexOf("/path"))
        - DefaultProxyUriManager.CHAINED_PARAMS_END_BEACON.length());
    uri = new UriBuilder().setQuery(paramsUri).toUri();
    
    // "Raw" query param verification.
    assertEquals("123", uri.getQueryParameter(Param.REFRESH.getKey()));
    if (version != null) {
      assertEquals(version, uri.getQueryParameter(Param.VERSION.getKey()));
    }
  }
  
  private DefaultProxyUriManager makeManager(String host, String path,
      ProxyUriManager.Versioner versioner) {
    ContainerConfig config = createMock(ContainerConfig.class);
    expect(config.getString(CONTAINER, DefaultProxyUriManager.PROXY_HOST_PARAM))
        .andReturn(host).anyTimes();
    expect(config.getString(CONTAINER, DefaultProxyUriManager.PROXY_PATH_PARAM))
        .andReturn(path).anyTimes();
    replay(config);
    return new DefaultProxyUriManager(config, versioner);
  }
  
  @SuppressWarnings("unchecked")
  private ProxyUriManager.Versioner makeVersioner(UriStatus status, String... versions) {
    ProxyUriManager.Versioner versioner = createMock(ProxyUriManager.Versioner.class);
    expect(versioner.version(isA(List.class), eq(CONTAINER)))
        .andReturn(Lists.newArrayList(versions)).anyTimes();
    expect(versioner.validate(isA(Uri.class), eq(CONTAINER), isA(String.class)))
        .andReturn(status).anyTimes();
    replay(versioner);
    return versioner;
  }
}
