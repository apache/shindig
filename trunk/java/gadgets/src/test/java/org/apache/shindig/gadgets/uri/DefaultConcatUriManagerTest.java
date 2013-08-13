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

import static org.apache.shindig.gadgets.uri.ConcatUriManager.ConcatUri.fromList;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.ConcatUriManager.ConcatData;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import org.junit.Test;

import java.util.List;
import java.util.HashMap;

public class DefaultConcatUriManagerTest extends UriManagerTestBase {
  private static final Uri RESOURCE_1 = Uri.parse("http://example.com/one.dat");
  private static final Uri RESOURCE_2 = Uri.parse("http://gadgets.com/two.dat");
  private static final Uri RESOURCE_3_NOSCHEMA = Uri.parse("//foobar.com/three.dat");
  private static final Uri RESOURCE_3_HTTP = Uri.parse("http://foobar.com/three.dat");
  private static final List<Uri> RESOURCES_ONE =
      ImmutableList.of(RESOURCE_1, RESOURCE_2, RESOURCE_3_HTTP);
  private static final List<Uri> RESOURCES_TWO =
      ImmutableList.of(RESOURCE_3_HTTP, RESOURCE_2, RESOURCE_1);

  @Test
  public void typeCssBasicParams() throws Exception {
    checkBasicParams(ConcatUriManager.Type.CSS);
  }

  @Test
  public void typeCssAltParams() throws Exception {
    checkAltParams(ConcatUriManager.Type.CSS);
  }

  @Test
  public void typeCssBatch() throws Exception {
    checkBatchAdjacent(ConcatUriManager.Type.CSS);
  }

  @Test
  public void typeCssValidatedGeneratedBatch() throws Exception {
    checkValidatedBatchAdjacent(ConcatUriManager.Type.CSS);
  }

  @Test(expected = RuntimeException.class)
  public void typeCssMissingHostConfig() throws Exception {
    checkMissingHostConfig(ConcatUriManager.Type.CSS);
  }

  @Test(expected = RuntimeException.class)
  public void typeCssMissingPathConfig() throws Exception {
    checkMissingPathConfig(ConcatUriManager.Type.CSS);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void typeCssSplitNotSupported() throws Exception {
    // Unique to type=CSS, split isn't supported.
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager("host.com", "/foo", "token", null);
    List<List<Uri>> resourceUris = ImmutableList.of(RESOURCES_ONE);
    manager.make(fromList(gadget, resourceUris, ConcatUriManager.Type.CSS), false);
  }

  @Test
  public void typeJsBasicParams() throws Exception {
    checkBasicParams(ConcatUriManager.Type.JS);
  }

  @Test
  public void typeJsAltParams() throws Exception {
    checkAltParams(ConcatUriManager.Type.JS);
  }

  @Test
  public void typeJsBatchAdjacent() throws Exception {
    checkBatchAdjacent(ConcatUriManager.Type.JS);
  }

  @Test
  public void typeJsBatchSplitBatched() throws Exception {
    // Unique to type=JS, split is supported.
    Gadget gadget = mockGadget(false, false);
    String host = "host.com";
    String path = "/concat/path";
    ConcatUriManager.Type type = ConcatUriManager.Type.JS;
    String splitParam = "token";
    String[] versions = new String[] { "version1" };
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, splitParam, versioner);
    List<List<Uri>> resourceUris =
        ImmutableList.of(RESOURCES_ONE, RESOURCES_TWO, RESOURCES_ONE);
    HashMap<String, String> jsonParams = new HashMap<String, String>();
    List<ConcatData> concatUris =
        manager.make(fromList(gadget, resourceUris, type), false);
    assertEquals(3, concatUris.size());

    for (int i = 0; i < 3; ++i) {
      ConcatData uri = concatUris.get(i);
      assertEquals(1, uri.getUris().size());
      String json = uri.getUris().get(0).getQueryParameter(
          (Param.JSON.toString().toLowerCase()));
      assertTrue(json.startsWith(splitParam));
      String currentUri = uri.getUris().get(0).toString();
      if (jsonParams.keySet().contains(currentUri)) {
        assertEquals(jsonParams.get(currentUri), json);
      } else {
        jsonParams.put(currentUri, json);
      }

      assertEquals(DefaultConcatUriManager.getJsSnippet(json, RESOURCE_1),
          uri.getSnippet(RESOURCE_1));
      assertEquals(DefaultConcatUriManager.getJsSnippet(json, RESOURCE_2),
          uri.getSnippet(RESOURCE_2));
      assertNull(uri.getSnippet(RESOURCE_3_NOSCHEMA));
      assertEquals(DefaultConcatUriManager.getJsSnippet(json, RESOURCE_3_HTTP),
          uri.getSnippet(RESOURCE_3_HTTP));
      checkBasicUriParameters(uri.getUris().get(0), host, path, 10, type, "0", "0", versions[0]);
      List<Uri> resList = (i % 2 == 0) ? RESOURCES_ONE : RESOURCES_TWO;
      assertEquals(resList.get(0).toString(), uri.getUris().get(0).getQueryParameter("1"));
      assertEquals(resList.get(1).toString(), uri.getUris().get(0).getQueryParameter("2"));
      assertEquals(resList.get(2).toString(), uri.getUris().get(0).getQueryParameter("3"));
    }
    assertEquals(2, jsonParams.size());
  }

  @Test
  public void typeJsValidatedGeneratedBatch() throws Exception {
    checkValidatedBatchAdjacent(ConcatUriManager.Type.JS);
  }

  @Test(expected = RuntimeException.class)
  public void typeJsMissingHostConfig() throws Exception {
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager(null, "/foo", "token", null);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(ImmutableList.of(RESOURCE_1));
    manager.make(fromList(gadget, resourceUris, ConcatUriManager.Type.JS), false);
  }

  @Test(expected = RuntimeException.class)
  public void typeJsMissingPathConfig() throws Exception {
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager("host.com", null, "token", null);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(ImmutableList.of(RESOURCE_1));
    manager.make(fromList(gadget, resourceUris, ConcatUriManager.Type.JS), false);
  }

  @Test
  public void typeJsMissingSplitTokenConfig() throws Exception {
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager("host.com", "/foo", null, null);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(ImmutableList.of(RESOURCE_1));
    List<ConcatData> concatUris = manager.make(fromList(gadget, resourceUris, ConcatUriManager.Type.JS), false);
    assertEquals(1, concatUris.size());
    assertEquals(1, concatUris.get(0).getUris().size());
    assertNull(concatUris.get(0).getUris().get(0).getQueryParameter(Param.JSON.getKey()));
  }

  @Test
  public void jsEvalSnippet() {
    assertEquals("eval(_js['" + StringEscapeUtils.escapeEcmaScript(RESOURCE_1.toString()) + "']);",
        DefaultConcatUriManager.getJsSnippet("_js", RESOURCE_1));
  }

  @Test
  public void validateNoContainerStrict() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    manager.setUseStrictParsing(true);
    ConcatUriManager.ConcatUri validated =
        manager.process(Uri.parse("http://host.com/path?q=f"));
    assertEquals(UriStatus.BAD_URI, validated.getStatus());
  }

  @Test
  public void validateNoContainer() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    ConcatUriManager.ConcatUri validated =
        manager.process(Uri.parse("http://host.com/path?q=f"));
    assertEquals(UriStatus.BAD_URI, validated.getStatus());
  }

  @Test
  public void validateHostMismatchStrict() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    manager.setUseStrictParsing(true);
    ConcatUriManager.ConcatUri validated =
        manager.process(Uri.parse("http://another.com/path?" +
            Param.CONTAINER.getKey() + '=' + CONTAINER + "&type=css"));
    assertEquals(UriStatus.BAD_URI, validated.getStatus());
  }

  @Test
  public void validatePathMismatchStrict() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    manager.setUseStrictParsing(true);
    ConcatUriManager.ConcatUri validated =
        manager.process(Uri.parse("http://host.com/another?" +
            Param.CONTAINER.getKey() + '=' + CONTAINER + "&type=css"));
    assertEquals(UriStatus.BAD_URI, validated.getStatus());
  }

  @Test
  public void validateInvalidChildUri() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    ConcatUriManager.ConcatUri validated =
        manager.process(
          Uri.parse("http://host.com/path?" + Param.CONTAINER.getKey() + '=' + CONTAINER +
            "&1=!!!"));
    assertEquals(UriStatus.BAD_URI, validated.getStatus());
  }

  @Test
  public void validateNullTypeUri() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    ConcatUriManager.ConcatUri validated =
        manager.process(
          Uri.parse("http://host.com/path?" + Param.CONTAINER.getKey() + '=' + CONTAINER +
            "&1=http://legit.com/1.dat"));
    assertEquals(UriStatus.BAD_URI, validated.getStatus());
  }

  @Test
  public void validateBadTypeUri() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    ConcatUriManager.ConcatUri validated =
        manager.process(
          Uri.parse("http://host.com/path?" + Param.CONTAINER.getKey() + '=' + CONTAINER +
            "&1=http://legit.com/1.dat&" + Param.TYPE.getKey() + "=NOTATYPE"));
    assertEquals(UriStatus.BAD_URI, validated.getStatus());
  }

  @Test
  public void validateOldStyleTypeUri() {
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, null);
    ConcatUriManager.ConcatUri validated =
        manager.process(
          Uri.parse("http://host.com/path?" + Param.CONTAINER.getKey() + '=' + CONTAINER +
            "&1=http://legit.com/1.dat&" + Param.TYPE.getKey() + "=NOTATYPE&rewriteMime=text/css"));
    assertEquals(UriStatus.VALID_UNVERSIONED, validated.getStatus());
    assertEquals(ConcatUriManager.Type.CSS, validated.getType());
  }

  @Test
  public void validateCssUriUnversioned() {
    checkUnversionedUri(ConcatUriManager.Type.CSS, false);
  }

  @Test
  public void validateCssUriVersioned() {
    checkValidateUri(UriStatus.VALID_VERSIONED, ConcatUriManager.Type.CSS, false);
  }

  @Test
  public void validateCssUriBadVersion() {
    checkValidateUri(UriStatus.INVALID_VERSION, ConcatUriManager.Type.CSS, false);
  }

  @Test
  public void validateJsUriUnversioned() {
    checkUnversionedUri(ConcatUriManager.Type.JS, true);
  }

  @Test
  public void validateJsUriVersioned() {
    checkValidateUri(UriStatus.VALID_VERSIONED, ConcatUriManager.Type.JS, true);
  }

  @Test
  public void validateJsUriBadVersion() {
    checkValidateUri(UriStatus.INVALID_VERSION, ConcatUriManager.Type.JS, true);
  }

  @Test
  public void dontConcatMultipleResourceBeyoundUrlLimitSplitBatched() throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    ConcatUriManager.Type type = ConcatUriManager.Type.JS;

    String[] versions = new String[] { "v1" };
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);

    Uri urlA = Uri.parse(generateUrl(manager.getUrlMaxLength() / 4));
    Uri urlB = Uri.parse(generateUrl(manager.getUrlMaxLength() / 4));
    Uri urlC = Uri.parse(generateUrl(manager.getUrlMaxLength() / 2));

    List<Uri> urlList = ImmutableList.of(urlA, urlB, urlC);
    List<List<Uri>> resourceUris = ImmutableList.of(urlList);
    List<ConcatData> concatUris =
        manager.make(fromList(gadget, resourceUris, type), false);

    assertEquals(2, concatUris.get(0).getUris().size());

    String jsonFirst = concatUris.get(0).getUris().get(0).getQueryParameter(
        (Param.JSON.toString().toLowerCase()));
    checkBasicUriParameters(concatUris.get(0).getUris().get(0), host, path, 9, type,
        "1", "1", versions[0]);
    assertEquals(urlA.toString(), concatUris.get(0).getUris().get(0).getQueryParameter("1"));
    assertEquals(DefaultConcatUriManager.getJsSnippet(jsonFirst, urlA),
        concatUris.get(0).getSnippet(urlA));
    assertEquals(urlB.toString(), concatUris.get(0).getUris().get(0).getQueryParameter("2"));
    assertEquals(DefaultConcatUriManager.getJsSnippet(jsonFirst, urlB),
        concatUris.get(0).getSnippet(urlB));
    assertNull(concatUris.get(0).getUris().get(0).getQueryParameter("3"));

    String jsonSecond = concatUris.get(0).getUris().get(1).getQueryParameter(
            (Param.JSON.toString().toLowerCase()));
    checkBasicUriParameters(concatUris.get(0).getUris().get(1), host, path, 8, type,
        "1", "1", versions[0]);
    assertEquals(urlC.toString(), concatUris.get(0).getUris().get(1).getQueryParameter("1"));
    assertEquals(DefaultConcatUriManager.getJsSnippet(jsonSecond, urlC),
        concatUris.get(0).getSnippet(urlC));
    assertNull(concatUris.get(0).getUris().get(1).getQueryParameter("2"));

  }

  @Test
  public void dontConcatMultipleResourceBeyoundUrlLimitAdjacentBatched() throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    ConcatUriManager.Type type = ConcatUriManager.Type.JS;

    String[] versions = new String[] { "v1" };
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, null, versioner);

    Uri urlA = Uri.parse(generateUrl(manager.getUrlMaxLength() / 4));
    Uri urlB = Uri.parse(generateUrl(manager.getUrlMaxLength() / 4));
    Uri urlC = Uri.parse(generateUrl(manager.getUrlMaxLength() / 2));

    List<Uri> urlList = ImmutableList.of(urlA, urlB, urlC);
    List<List<Uri>> resourceUris = ImmutableList.of(urlList);
    List<ConcatData> concatUris =
        manager.make(fromList(gadget, resourceUris, type), true);

    assertEquals(2, concatUris.get(0).getUris().size());

    checkBasicUriParameters(concatUris.get(0).getUris().get(0), host, path, 8, type,
        "1", "1", versions[0]);
    assertEquals(urlA.toString(), concatUris.get(0).getUris().get(0).getQueryParameter("1"));
    assertNull(concatUris.get(0).getSnippet(urlA));
    assertEquals(urlB.toString(), concatUris.get(0).getUris().get(0).getQueryParameter("2"));
    assertNull(concatUris.get(0).getSnippet(urlB));
    assertNull(concatUris.get(0).getUris().get(0).getQueryParameter("3"));

    checkBasicUriParameters(concatUris.get(0).getUris().get(1), host, path, 7, type,
        "1", "1", versions[0]);
    assertEquals(urlC.toString(), concatUris.get(0).getUris().get(1).getQueryParameter("1"));
    assertNull(concatUris.get(0).getSnippet(urlC));
    assertNull(concatUris.get(0).getUris().get(1).getQueryParameter("2"));
  }

  /**
   * Contains Uri Basic Assert Checks
   */
  private void checkBasicUriParameters(Uri uri,
                                       String host,
                                       String path,
                                       int queryParameterSize,
                                       ConcatUriManager.Type type,
                                       String debug,
                                       String noCache) {
    assertNull(uri.getScheme());
    assertEquals(host, uri.getAuthority());
    assertEquals(path, uri.getPath());
    assertEquals(queryParameterSize, uri.getQueryParameters().size());
    assertEquals(CONTAINER, uri.getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(SPEC_URI.toString(), uri.getQueryParameter(Param.GADGET.getKey()));
    assertEquals(type.getType(), uri.getQueryParameter(Param.TYPE.getKey()));
    assertEquals(debug, uri.getQueryParameter(Param.DEBUG.getKey()));
    assertEquals(noCache, uri.getQueryParameter(Param.NO_CACHE.getKey()));
  }

  /**
   * Contains Uri Basic Assert Checks
   */
  private void checkBasicUriParameters(Uri uri,
                                       String host,
                                       String path,
                                       int queryParameterSize,
                                       ConcatUriManager.Type type,
                                       String debug,
                                       String noCache,
                                       String version) {
    checkBasicUriParameters(uri, host, path, queryParameterSize, type, debug, noCache);
    assertEquals(version, uri.getQueryParameter(Param.VERSION.getKey()));
  }

  private void checkUnversionedUri(ConcatUriManager.Type type, boolean hasSplit) {
    // Returns VALID_VERSIONED, but no version is present.
    ConcatUriManager.Versioner versioner = makeVersioner(UriStatus.VALID_VERSIONED);
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, versioner);
    ConcatUriManager.ConcatUri validated =
        manager.process(
          Uri.parse("http://host.com/path?" + Param.CONTAINER.getKey() + '=' + CONTAINER +
            "&1=http://legit.com/1.dat&2=http://another.com/2.dat&" + Param.TYPE.getKey() +
              '=' + type.getType() + '&' + Param.JSON.getKey() +
            "=split&" + Param.GADGET.getKey() + "=http://www.gadget.com/g.xml&" +
            Param.REFRESH.getKey() + "=123"));
    assertEquals(UriStatus.VALID_UNVERSIONED, validated.getStatus());
    assertEquals(type, validated.getType());
    assertEquals(CONTAINER, validated.getContainer());
    assertEquals("http://www.gadget.com/g.xml", validated.getGadget());
    assertEquals(2, validated.getBatch().size());
    assertEquals("http://legit.com/1.dat", validated.getBatch().get(0).toString());
    assertEquals("http://another.com/2.dat", validated.getBatch().get(1).toString());
    assertEquals(123, validated.getRefresh().intValue());
    assertEquals(hasSplit ? "split" : null, validated.getSplitParam());
  }

  private void checkValidateUri(UriStatus status, ConcatUriManager.Type type, boolean hasSplit) {
    ConcatUriManager.Versioner versioner = makeVersioner(status);
    DefaultConcatUriManager manager = makeManager("host.com", "/path", null, versioner);
    ConcatUriManager.ConcatUri validated =
        manager.process(
          Uri.parse("http://host.com/path?" + Param.CONTAINER.getKey() + '=' + CONTAINER +
            "&1=http://legit.com/1.dat&2=http://another.com/2.dat&" + Param.TYPE.getKey() + '='
            + type.getType() + '&' + Param.VERSION.getKey() + "=something&" + Param.JSON.getKey() +
            "=split&" + Param.GADGET.getKey() + "=http://www.gadget.com/g.xml&" +
            Param.REFRESH.getKey() + "=123"));
    assertEquals(status, validated.getStatus());
    assertEquals(type, validated.getType());
    assertEquals(CONTAINER, validated.getContainer());
    assertEquals("http://www.gadget.com/g.xml", validated.getGadget());
    assertEquals(2, validated.getBatch().size());
    assertEquals("http://legit.com/1.dat", validated.getBatch().get(0).toString());
    assertEquals("http://another.com/2.dat", validated.getBatch().get(1).toString());
    assertEquals(123, validated.getRefresh().intValue());
    assertEquals(hasSplit ? "split" : null, validated.getSplitParam());
  }

  private void checkBasicParams(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(false, false);
    String host = "host.com";
    String path = "/concat/path";
    DefaultConcatUriManager manager = makeManager(host, path, "token", null);
    List<List<Uri>> resourceUris = ImmutableList.of(RESOURCES_ONE);

    List<ConcatData> concatUris =
      manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(1, concatUris.size());

    ConcatData uri = concatUris.get(0);
    assertNull(uri.getSnippet(RESOURCE_1));
    assertNull(uri.getSnippet(RESOURCE_2));
    assertNull(uri.getSnippet(RESOURCE_3_NOSCHEMA));
    assertEquals(1, uri.getUris().size());
    checkBasicUriParameters(uri.getUris().get(0), host, path, 8, type, "0", "0");
    assertEquals(RESOURCES_ONE.get(0).toString(), uri.getUris().get(0).getQueryParameter("1"));
    assertEquals(RESOURCES_ONE.get(1).toString(), uri.getUris().get(0).getQueryParameter("2"));
    assertEquals(RESOURCES_ONE.get(2).toString(), uri.getUris().get(0).getQueryParameter("3"));
  }

  private void checkAltParams(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String version = "version";
    ConcatUriManager.Versioner versioner = makeVersioner(null, version);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris = ImmutableList.of(RESOURCES_ONE);

    List<ConcatData> concatUris =
      manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(1, concatUris.size());

    ConcatData uri = concatUris.get(0);
    assertNull(uri.getSnippet(RESOURCE_1));
    assertNull(uri.getSnippet(RESOURCE_2));
    assertNull(uri.getSnippet(RESOURCE_3_NOSCHEMA));
    assertEquals(1, uri.getUris().size());
    checkBasicUriParameters(uri.getUris().get(0), host, path, 9, type, "1", "1", version);
    assertEquals(RESOURCES_ONE.get(0).toString(), uri.getUris().get(0).getQueryParameter("1"));
    assertEquals(RESOURCES_ONE.get(1).toString(), uri.getUris().get(0).getQueryParameter("2"));
    assertEquals(RESOURCES_ONE.get(2).toString(), uri.getUris().get(0).getQueryParameter("3"));
  }

  private void checkBatchAdjacent(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String[] versions = new String[] { "version1"};
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris =
        ImmutableList.of(RESOURCES_ONE, RESOURCES_TWO, RESOURCES_ONE);

    List<ConcatData> concatUris =
      manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(3, concatUris.size());

    for (int i = 0; i < 3; ++i) {
      ConcatData uri = concatUris.get(i);
      assertNull(uri.getSnippet(RESOURCE_1));
      assertNull(uri.getSnippet(RESOURCE_2));
      assertNull(uri.getSnippet(RESOURCE_3_NOSCHEMA));
      assertEquals(1, uri.getUris().size());
      checkBasicUriParameters(uri.getUris().get(0), host, path, 9, type, "1", "1", versions[0]);
      List<Uri> resList = (i % 2 == 0) ? RESOURCES_ONE : RESOURCES_TWO;
      assertEquals(resList.get(0).toString(), uri.getUris().get(0).getQueryParameter("1"));
      assertEquals(resList.get(1).toString(), uri.getUris().get(0).getQueryParameter("2"));
      assertEquals(resList.get(2).toString(), uri.getUris().get(0).getQueryParameter("3"));
    }
  }

  private void checkValidatedBatchAdjacent(ConcatUriManager.Type type) throws Exception {
    // This is essentially the "integration" test ensuring that a
    // DefaultConcatUriManager's created Uris can be validated by it in turn.
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String[] versions = new String[] { "version1"};
    ConcatUriManager.Versioner versioner = makeVersioner(UriStatus.VALID_VERSIONED, versions);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris =
        ImmutableList.of(RESOURCES_ONE, RESOURCES_TWO, RESOURCES_ONE);

    List<ConcatData> concatUris =
        manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(3, concatUris.size());

    for (int i = 0; i < 3; ++i) {
      ConcatUriManager.ConcatUri validated =
          manager.process(concatUris.get(i).getUris().get(0));
      assertEquals(UriStatus.VALID_VERSIONED, validated.getStatus());
      List<Uri> resList = (i % 2 == 0) ? RESOURCES_ONE : RESOURCES_TWO;
      assertEquals(resList, validated.getBatch());
      assertEquals(type, validated.getType());
    }
  }

  private String generateUrl(int length) {
    return "http://www.url.com/" + RandomStringUtils.randomAlphanumeric(length - 22) + ".js";
  }

  private void checkMissingHostConfig(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager(null, "/foo", "token", null);
    List<List<Uri>> resourceUris = ImmutableList.of(RESOURCES_ONE);
    manager.make(fromList(gadget, resourceUris, type), true);
  }

  private void checkMissingPathConfig(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager("host.com", null, "token", null);
    List<List<Uri>> resourceUris = ImmutableList.of(RESOURCES_ONE);
    manager.make(fromList(gadget, resourceUris, type), false);
  }

  private DefaultConcatUriManager makeManager(String host, String path, String splitToken,
      ConcatUriManager.Versioner versioner) {
    ContainerConfig config = createMock(ContainerConfig.class);
    expect(config.getString(CONTAINER, DefaultConcatUriManager.CONCAT_HOST_PARAM))
        .andReturn(host).anyTimes();
    expect(config.getString(CONTAINER, DefaultConcatUriManager.CONCAT_PATH_PARAM))
        .andReturn(path).anyTimes();
    expect(config.getString(CONTAINER, DefaultConcatUriManager.CONCAT_JS_SPLIT_PARAM))
        .andReturn(splitToken).anyTimes();
    replay(config);
    return new DefaultConcatUriManager(config, versioner);
  }

  @SuppressWarnings("unchecked")
  private ConcatUriManager.Versioner makeVersioner(UriStatus status, String... versions) {
    ConcatUriManager.Versioner versioner = createMock(ConcatUriManager.Versioner.class);
    expect(versioner.version(isA(List.class), eq(CONTAINER), isA(List.class)))
        .andReturn(ImmutableList.copyOf(versions)).anyTimes();
    expect(versioner.validate(isA(List.class), eq(CONTAINER), isA(String.class)))
        .andReturn(status).anyTimes();
    replay(versioner);
    return versioner;
  }
}
