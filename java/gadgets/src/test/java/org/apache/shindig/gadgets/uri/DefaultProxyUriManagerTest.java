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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Test;

import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultProxyUriManagerTest extends UriManagerTestBase {
  private static final Uri RESOURCE_1 = Uri.parse("http://example.com/one.dat?param=value");
  private static final Uri RESOURCE_2 = Uri.parse("http://gadgets.com/two.dat");
  private static final Uri RESOURCE_3 = Uri.parse("http://foobar.com/three.dat");
  private static final Uri RESOURCE_4 = Uri.parse("//foobar.com/three.dat");

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
    List<Uri> resources = ImmutableList.of(RESOURCE_1);
    List<Uri> uris = makeAndGet(host, path, debug, noCache, resources, version);
    assertEquals(1, uris.size());
    verifyQueryUri(RESOURCE_1, uris.get(0), debug, noCache, version, host, path);
  }

  @Test
  public void testSchemaLessProxy() throws Exception {
    boolean debug = false;
    boolean noCache = false;
    String version = "ver";
    String host = "host.com";
    String path = "/proxy/path";
    List<Uri> resources = ImmutableList.of(RESOURCE_4);
    List<Uri> uris = makeAndGet(host, path, debug, noCache, resources, version);
    assertEquals(1, uris.size());
    verifyQueryUri(new UriBuilder(RESOURCE_4).setScheme("http").toUri(),
        new UriBuilder(uris.get(0)).setScheme("http").toUri(),
        debug, noCache, version, host, path);
  }

  @Test
  public void refreshVerifyBasic() throws Exception {
    verifyRefresh(false, false, "version", 20);
  }

  @Test
  public void refreshVerifyNoCache() throws Exception {
    verifyRefresh(false, true, "version", 20);
  }

  @Test
  public void refreshVerifyNoRefresh() throws Exception {
    verifyRefresh(false, false, "version", null);
  }

  public void verifyRefresh(boolean debug, boolean noCache, String version, Integer refresh)
      throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    ProxyUriManager.Versioner versioner = makeVersioner(null, version);
    DefaultProxyUriManager manager = makeManager(host, path, versioner);
    List<ProxyUri> proxyUris = Lists.newLinkedList();
    proxyUris.add(new ProxyUri(refresh, debug, noCache, CONTAINER, SPEC_URI.toString(),
        RESOURCE_1));

    List<Uri> uris = manager.make(proxyUris, null);
    assertEquals(1, uris.size());
    verifyQueryUriWithRefresh(RESOURCE_1, uris.get(0), debug, noCache,
        version, refresh, host, path);
  }

  @Test
  public void verifyAddedParamsQuery() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    ProxyUriManager.Versioner versioner = makeVersioner(null, "version1", "version2");
    DefaultProxyUriManager manager = makeManager(host, path, versioner);
    List<ProxyUri> proxyUris = Lists.newLinkedList();
    ProxyUri pUri = new ProxyUri(null, false, true, CONTAINER, SPEC_URI.toString(),
        RESOURCE_1);
    pUri.setResize(100, 10, 90, true);
    proxyUris.add(pUri);

    pUri = new ProxyUri(null, false, true, CONTAINER, SPEC_URI.toString(),
        RESOURCE_2);
    pUri.setResize(null, 10, null, false);
    proxyUris.add(pUri);

    List<Uri> uris = manager.make(proxyUris, null);
    assertEquals(2, uris.size());
    verifyQueryUriWithRefresh(RESOURCE_1, uris.get(0), false, true,
        "version1", null, host, path);
    // Verify added param:
    assertEquals("100", uris.get(0).getQueryParameter("resize_w"));
    assertEquals("10", uris.get(0).getQueryParameter("resize_h"));
    assertEquals("90", uris.get(0).getQueryParameter("resize_q"));
    assertEquals("1", uris.get(0).getQueryParameter("no_expand"));
    assertEquals(null, uris.get(1).getQueryParameter("resize_w"));
    assertEquals("10", uris.get(1).getQueryParameter("resize_h"));
    assertEquals(null, uris.get(1).getQueryParameter("resize_q"));
    assertEquals(null, uris.get(1).getQueryParameter("no_expand"));
  }

  @Test
  public void verifyAddedParamsChained() throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path";
    ProxyUriManager.Versioner versioner = makeVersioner(null, "version");
    DefaultProxyUriManager manager = makeManager(host, path, versioner);
    List<ProxyUri> proxyUris = Lists.newLinkedList();
    ProxyUri pUri = new ProxyUri(null, false, true, CONTAINER, SPEC_URI.toString(),
        RESOURCE_1);
    pUri.setExtensionParams(ImmutableMap.<String, String>of("test", "1"));
    pUri.setResize(100, 10, 90, true);
    proxyUris.add(pUri);

    List<Uri> uris = manager.make(proxyUris, null);
    assertEquals(1, uris.size());
    verifyChainedUri(RESOURCE_1, uris.get(0), false, true,
        null, false, host, path);
    // Verify added param:
    assertEquals("/proxy/container=container&gadget=http%3A%2F%2Fexample.com%2Fgadget.xml" +
        "&debug=0&nocache=1&v=version&test=1&resize_h=10&resize_w=100&resize_q=90&no_expand=1" +
        "/path/http://example.com/one.dat",
        uris.get(0).getPath());
  }

  @Test
  public void testFallbackUrl() throws Exception {
    ProxyUri uri = new ProxyUri(null, false, false, "open", "http://example.com/gadget",
        Uri.parse("http://example.com/resource"));
    uri.setFallbackUrl("http://example.com/fallback");

    assertEquals("http://example.com/fallback", uri.getFallbackUri().toString());
  }

  @Test(expected = GadgetException.class)
  public void testBadFallbackUrl() throws Exception {
    ProxyUri uri = new ProxyUri(null, false, false, "open", "http://example.com/gadget",
        Uri.parse("http://example.com/resource"));
    uri.setFallbackUrl("bad url");
    uri.getFallbackUri(); // throws exception!
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
    List<Uri> resources = ImmutableList.of(RESOURCE_1);
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
    List<Uri> resources = ImmutableList.of(RESOURCE_1);
    List<Uri> uris = makeAndGet(host, path, debug, noCache, resources, version);
    assertEquals(1, uris.size());
    verifyChainedUri(RESOURCE_1, uris.get(0), debug, noCache, version, true, host, path);
  }

  @Test
  public void batchedProxyQueryStyle() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    List<Uri> resources = ImmutableList.of(RESOURCE_1, RESOURCE_2, RESOURCE_3);
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
    List<Uri> resources = ImmutableList.of(RESOURCE_1, RESOURCE_2, RESOURCE_3);
    String[] versions = new String[] { "v1", "v2", "v3" };
    List<Uri> uris = makeAndGet(host, path, true, true, resources, versions);
    assertEquals(3, uris.size());
    for (int i = 0; i < 3; ++i) {
      verifyChainedUri(resources.get(i), uris.get(i), true, true, versions[i], false, host, path);
    }
  }

  @Test
  public void batchedProxyChainedStyleNoVerisons() throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN + "/path";
    List<Uri> resources = ImmutableList.of(RESOURCE_1, RESOURCE_2, RESOURCE_3);
    List<Uri> uris = makeAndGet(host, path, true, true, resources);
    assertEquals(3, uris.size());
    for (int i = 0; i < 3; ++i) {
      verifyChainedUri(resources.get(i), uris.get(i), true, true, null, false, host, path);
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

  @Test
  public void containerFallsBackToSynd() throws Exception {
    String host = "host.com";
    String path = "/path";
    DefaultProxyUriManager manager = makeManager(host, path, null);
    UriBuilder uriBuilder = new UriBuilder();
    uriBuilder.setScheme("http").setAuthority(host).setPath(path);
    uriBuilder.addQueryParameter(Param.URL.getKey(), RESOURCE_1.toString());
    uriBuilder.addQueryParameter("synd", CONTAINER);
    uriBuilder.addQueryParameter(Param.GADGET.getKey(), SPEC_URI.toString());
    uriBuilder.addQueryParameter(Param.REFRESH.getKey(), "321");
    ProxyUriManager.ProxyUri proxyUri = manager.process(uriBuilder.toUri());
    assertEquals(RESOURCE_1, proxyUri.getResource());
    assertEquals(CONTAINER, proxyUri.getContainer());
    assertEquals(SPEC_URI.toString(), proxyUri.getGadget());
    assertEquals(321, (int)proxyUri.getRefresh());
    assertEquals(false, proxyUri.isDebug());
    assertEquals(false, proxyUri.isNoCache());
  }

  @Test(expected = GadgetException.class)
  public void mismatchedHostStrict() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    DefaultProxyUriManager manager = makeManager("foo" + host, path, null);
    manager.setUseStrictParsing(true);
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
        "/proxy/refresh=123/path/http://foo.com").toUri();
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
        "/proxy/container=" +
        CONTAINER + "/path/").toUri();
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

  @Test
  public void testHtmlTagContext() throws Exception {
    String host = "host.com";
    String path = "/proxy/path";
    DefaultProxyUriManager manager = makeManager(host, path, null);
    Uri testUri = new UriBuilder().setAuthority(host).setPath(path)
        .addQueryParameter(Param.CONTAINER.getKey(), CONTAINER)
        .addQueryParameter(Param.URL.getKey(), "http://www.example.org/")
        .addQueryParameter(Param.HTML_TAG_CONTEXT.getKey(), "htmlTag")
        .toUri();
    ProxyUri proxyUri = manager.process(testUri);
    assertEquals("htmlTag", proxyUri.getHtmlTagContext());

    Uri targetUri = Uri.parse("http://www.example2.org/");
    HttpRequest req = proxyUri.makeHttpRequest(targetUri);
    assertEquals("htmlTag", req.getParam(Param.HTML_TAG_CONTEXT.getKey()));

    UriBuilder builder = proxyUri.makeQueryParams(1, "2");
    assertEquals("htmlTag", builder.getQueryParameter(Param.HTML_TAG_CONTEXT.getKey()));
  }

  private List<Uri> makeAndGet(String host, String path, boolean debug, boolean noCache,
      List<Uri> resources, String... version) {
    return makeAndGetWithRefresh(host, path, debug, noCache, resources, 123, version);
  }

  private List<Uri> makeAndGetWithRefresh(String host, String path, boolean debug,
      boolean noCache, List<Uri> resources, Integer refresh, String... version) {
    ProxyUriManager.Versioner versioner = makeVersioner(null, version);
    DefaultProxyUriManager manager = makeManager(host, path, versioner);
    Gadget gadget = mockGadget(debug, noCache);
    return manager.make(
        ProxyUriManager.ProxyUri.fromList(gadget, resources), refresh);
  }

  private void verifyQueryUri(Uri orig, Uri uri, boolean debug, boolean noCache, String version,
      String host, String path) throws Exception {
    verifyQueryUriWithRefresh(orig, uri, debug, noCache, version, 123, host, path);
  }

  private void verifyQueryUriWithRefresh(Uri orig, Uri uri, boolean debug, boolean noCache,
      String version, Integer refresh, String host, String path) throws Exception {
    // Make sure the manager can parse out results.
    DefaultProxyUriManager manager = makeManager(host, path, null);
    ProxyUri proxyUri = manager.process(uri);
    assertEquals(orig, proxyUri.getResource());
    assertEquals(debug, proxyUri.isDebug());
    assertEquals(noCache, proxyUri.isNoCache());
    assertEquals(noCache ? Integer.valueOf(0) : refresh, proxyUri.getRefresh());
    assertEquals(CONTAINER, proxyUri.getContainer());
    assertEquals(SPEC_URI.toString(), proxyUri.getGadget());

    // "Raw" query param verification.
    assertEquals(noCache || refresh == null ? null : refresh.toString(),
        uri.getQueryParameter(Param.REFRESH.getKey()));
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
        proxyEnd,
        (endOfPath ? uriStr.indexOf('/', proxyEnd) : uriStr.indexOf("/path")));
    uri = new UriBuilder().setQuery(paramsUri).toUri();

    // "Raw" query param verification.
    assertEquals(noCache ? null : "123", uri.getQueryParameter(Param.REFRESH.getKey()));
    if (version != null) {
      assertEquals(version, uri.getQueryParameter(Param.VERSION.getKey()));
    }
  }

  @Test
  public void testProxyGadgetsChainDecode() throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN;
    DefaultProxyUriManager uriManager = makeManager(host, path, null);
    Uri uri = Uri.parse("http://host.com/gadgets/proxy/refresh%3d55%26container%3dcontainer/"
        + "http://www.cnn.com/news?refresh=45");
    ProxyUri pUri = uriManager.process(uri);
    assertEquals(new Integer(55), pUri.getRefresh());
    assertEquals("http://www.cnn.com/news?refresh=45", pUri.getResource().toString());
    assertEquals(CONTAINER, pUri.getContainer());
  }

  @Test
  public void testProxyGadgetsChainDecodeGif() throws Exception {
    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN;
    DefaultProxyUriManager uriManager = makeManager(host, path, null);
    Uri uri = Uri.parse("http://host.com/gadgets/proxy/container%3dcontainer%26" +
        "gadget%3dhttp%3A%2F%2Fwww.orkut.com%2Fcss%2Fgen%2Fbase054.css.int%26" +
        "debug%3d0%26nocache%3d0/http://www.orkut.com/img/castro/i%5freply.gif");
    ProxyUri pUri = uriManager.process(uri);
    assertEquals(false, pUri.isDebug());
    assertEquals("http://www.orkut.com/img/castro/i%5freply.gif", pUri.getResource().toString());
    assertEquals(CONTAINER, pUri.getContainer());
    assertEquals("http://www.orkut.com/css/gen/base054.css.int", pUri.getGadget());
  }

  @Test
  public void testProxyGadgetsChainGif() throws Exception {

    String host = "host.com";
    String path = "/proxy/" + DefaultProxyUriManager.CHAINED_PARAMS_TOKEN;
    DefaultProxyUriManager uriManager = makeManager(host, path, null);
    Uri uri = Uri.parse("http://host.com/gadgets/proxy/container=container&" +
        "gadget=http%3A%2F%2Fwww.orkut.com%2Fcss%2Fgen%2Fbase054.css.int&" +
        "debug=0&nocache=0/http://www.orkut.com/img/castro/i_reply.gif");
    ProxyUri pUri = uriManager.process(uri);
    assertEquals(false, pUri.isDebug());
    assertEquals("http://www.orkut.com/img/castro/i_reply.gif", pUri.getResource().toString());
    assertEquals(CONTAINER, pUri.getContainer());
    assertEquals("http://www.orkut.com/css/gen/base054.css.int", pUri.getGadget());
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
    if (versions.length > 0) {
      expect(versioner.version(isA(List.class), eq(CONTAINER), isA(List.class)))
          .andReturn(Lists.newArrayList(versions)).anyTimes();
    } else {
      expect(versioner.version(isA(List.class), eq(CONTAINER), isA(List.class)))
          .andReturn(null).anyTimes();
    }
    expect(versioner.validate(isA(Uri.class), eq(CONTAINER), isA(String.class)))
        .andReturn(status).anyTimes();
    replay(versioner);
    return versioner;
  }
}
