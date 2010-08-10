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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.ConcatUriManager.ConcatData;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import org.junit.Test;

import java.util.List;
import java.util.NoSuchElementException;

public class DefaultConcatUriManagerTest extends UriManagerTestBase {
  private static final String CONTAINER = "container";
  private static final Uri RESOURCE_1 = Uri.parse("http://example.com/one.dat");
  private static final Uri RESOURCE_2 = Uri.parse("http://gadgets.com/two.dat");
  private static final Uri RESOURCE_3 = Uri.parse("http://foobar.com/three.dat");
  private static final List<Uri> RESOURCES_ONE =
      ImmutableList.of(RESOURCE_1, RESOURCE_2, RESOURCE_3);
  private static final List<Uri> RESOURCES_TWO = 
      ImmutableList.of(RESOURCE_3, RESOURCE_2, RESOURCE_1);
  
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
  
  @Test(expected = NoSuchElementException.class)
  public void typeCssBatchInsufficientVersions() throws Exception {
    checkBatchInsufficientVersions(ConcatUriManager.Type.CSS);
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
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(RESOURCES_ONE);
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
    String[] versions = new String[] { "version1", "v2", "v3" };
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, splitParam, versioner);
    List<List<Uri>> resourceUris =
        ImmutableList.<List<Uri>>of(RESOURCES_ONE, RESOURCES_TWO, RESOURCES_ONE);
    
    List<ConcatData> concatUris =
        manager.make(fromList(gadget, resourceUris, type), false);
    assertEquals(3, concatUris.size());
    
    for (int i = 0; i < 3; ++i) {
      ConcatData uri = concatUris.get(i);
      assertEquals(DefaultConcatUriManager.getJsSnippet(splitParam, RESOURCE_1),
          uri.getSnippet(RESOURCE_1));
      assertEquals(DefaultConcatUriManager.getJsSnippet(splitParam, RESOURCE_2),
          uri.getSnippet(RESOURCE_2));
      assertEquals(DefaultConcatUriManager.getJsSnippet(splitParam, RESOURCE_3),
          uri.getSnippet(RESOURCE_3));
      assertNull(uri.getUri().getScheme());
      assertEquals(host, uri.getUri().getAuthority());
      assertEquals(path, uri.getUri().getPath());
      assertEquals(10, uri.getUri().getQueryParameters().size());
      assertEquals(CONTAINER, uri.getUri().getQueryParameter(Param.CONTAINER.getKey()));
      assertEquals(SPEC_URI.toString(), uri.getUri().getQueryParameter(Param.GADGET.getKey()));
      assertEquals(type.getType(), uri.getUri().getQueryParameter(Param.TYPE.getKey()));
      assertEquals("0", uri.getUri().getQueryParameter(Param.DEBUG.getKey()));
      assertEquals("0", uri.getUri().getQueryParameter(Param.NO_CACHE.getKey()));
      assertEquals(type.getType(), uri.getUri().getQueryParameter(Param.TYPE.getKey()));
      List<Uri> resList = (i % 2 == 0) ? RESOURCES_ONE : RESOURCES_TWO;
      assertEquals(resList.get(0).toString(), uri.getUri().getQueryParameter("1"));
      assertEquals(resList.get(1).toString(), uri.getUri().getQueryParameter("2"));
      assertEquals(resList.get(2).toString(), uri.getUri().getQueryParameter("3"));
      assertEquals(versions[i], uri.getUri().getQueryParameter(Param.VERSION.getKey()));
    }
  }
  
  @Test
  public void typeJsValidatedGeneratedBatch() throws Exception {
    checkValidatedBatchAdjacent(ConcatUriManager.Type.JS);
  }
  
  @Test(expected=NoSuchElementException.class)
  public void typeJsBatchInsufficientVersions() throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String[] versions = new String[] { "v1" };  // Only one for three resources.
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(RESOURCES_ONE, RESOURCES_ONE);
    manager.make(fromList(gadget, resourceUris, ConcatUriManager.Type.JS), true);
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
    assertNull(concatUris.get(0).getUri().getQueryParameter(Param.JSON.getKey()));
  }
  
  @Test
  public void jsEvalSnippet() {
    assertEquals("eval(_js['" + StringEscapeUtils.escapeJavaScript(RESOURCE_1.toString()) + "']);",
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
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(RESOURCES_ONE);
    
    List<ConcatData> concatUris =
      manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(1, concatUris.size());
    
    ConcatData uri = concatUris.get(0);
    assertNull(uri.getSnippet(RESOURCE_1));
    assertNull(uri.getSnippet(RESOURCE_2));
    assertNull(uri.getSnippet(RESOURCE_3));
    assertNull(uri.getUri().getScheme());
    assertEquals(host, uri.getUri().getAuthority());
    assertEquals(path, uri.getUri().getPath());
    assertEquals(8, uri.getUri().getQueryParameters().size());
    assertEquals(CONTAINER, uri.getUri().getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(SPEC_URI.toString(), uri.getUri().getQueryParameter(Param.GADGET.getKey()));
    assertEquals("0", uri.getUri().getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("0", uri.getUri().getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(type.getType(), uri.getUri().getQueryParameter(Param.TYPE.getKey()));
    assertEquals(RESOURCES_ONE.get(0).toString(), uri.getUri().getQueryParameter("1"));
    assertEquals(RESOURCES_ONE.get(1).toString(), uri.getUri().getQueryParameter("2"));
    assertEquals(RESOURCES_ONE.get(2).toString(), uri.getUri().getQueryParameter("3"));
  }
  
  private void checkAltParams(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String version = "version";
    ConcatUriManager.Versioner versioner = makeVersioner(null, version);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(RESOURCES_ONE);
    
    List<ConcatData> concatUris =
      manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(1, concatUris.size());
    
    ConcatData uri = concatUris.get(0);
    assertNull(uri.getSnippet(RESOURCE_1));
    assertNull(uri.getSnippet(RESOURCE_2));
    assertNull(uri.getSnippet(RESOURCE_3));
    assertNull(uri.getUri().getScheme());
    assertEquals(host, uri.getUri().getAuthority());
    assertEquals(path, uri.getUri().getPath());
    assertEquals(9, uri.getUri().getQueryParameters().size());
    assertEquals(CONTAINER, uri.getUri().getQueryParameter(Param.CONTAINER.getKey()));
    assertEquals(SPEC_URI.toString(), uri.getUri().getQueryParameter(Param.GADGET.getKey()));
    assertEquals("1", uri.getUri().getQueryParameter(Param.DEBUG.getKey()));
    assertEquals("1", uri.getUri().getQueryParameter(Param.NO_CACHE.getKey()));
    assertEquals(type.getType(),
        uri.getUri().getQueryParameter(Param.TYPE.getKey()));
    assertEquals(RESOURCES_ONE.get(0).toString(), uri.getUri().getQueryParameter("1"));
    assertEquals(RESOURCES_ONE.get(1).toString(), uri.getUri().getQueryParameter("2"));
    assertEquals(RESOURCES_ONE.get(2).toString(), uri.getUri().getQueryParameter("3"));
    assertEquals(version, uri.getUri().getQueryParameter(Param.VERSION.getKey()));
  }

  private void checkBatchAdjacent(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String[] versions = new String[] { "version1", "v2", "v3" };
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris =
        ImmutableList.<List<Uri>>of(RESOURCES_ONE, RESOURCES_TWO, RESOURCES_ONE);
    
    List<ConcatData> concatUris =
      manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(3, concatUris.size());
    
    for (int i = 0; i < 3; ++i) {
      ConcatData uri = concatUris.get(i);
      assertNull(uri.getSnippet(RESOURCE_1));
      assertNull(uri.getSnippet(RESOURCE_2));
      assertNull(uri.getSnippet(RESOURCE_3));
      assertNull(uri.getUri().getScheme());
      assertEquals(host, uri.getUri().getAuthority());
      assertEquals(path, uri.getUri().getPath());
      assertEquals(9, uri.getUri().getQueryParameters().size());
      assertEquals(CONTAINER, uri.getUri().getQueryParameter(Param.CONTAINER.getKey()));
      assertEquals(SPEC_URI.toString(), uri.getUri().getQueryParameter(Param.GADGET.getKey()));
      assertEquals("1", uri.getUri().getQueryParameter(Param.DEBUG.getKey()));
      assertEquals("1", uri.getUri().getQueryParameter(Param.NO_CACHE.getKey()));
      assertEquals(type.getType(), uri.getUri().getQueryParameter(Param.TYPE.getKey()));
      List<Uri> resList = (i % 2 == 0) ? RESOURCES_ONE : RESOURCES_TWO;
      assertEquals(resList.get(0).toString(), uri.getUri().getQueryParameter("1"));
      assertEquals(resList.get(1).toString(), uri.getUri().getQueryParameter("2"));
      assertEquals(resList.get(2).toString(), uri.getUri().getQueryParameter("3"));
      assertEquals(versions[i], uri.getUri().getQueryParameter(Param.VERSION.getKey()));
    }
  }
  
  private void checkValidatedBatchAdjacent(ConcatUriManager.Type type) throws Exception {
    // This is essentially the "integration" test ensuring that a
    // DefaultConcatUriManager's created Uris can be validated by it in turn.
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String[] versions = new String[] { "version1", "v2", "v3" };
    ConcatUriManager.Versioner versioner = makeVersioner(UriStatus.VALID_VERSIONED, versions);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris =
        ImmutableList.<List<Uri>>of(RESOURCES_ONE, RESOURCES_TWO, RESOURCES_ONE);
    
    List<ConcatData> concatUris =
        manager.make(fromList(gadget, resourceUris, type), true);
    assertEquals(3, concatUris.size());
    
    for (int i = 0; i < 3; ++i) {
      ConcatUriManager.ConcatUri validated =
          manager.process(concatUris.get(i).getUri());
      assertEquals(UriStatus.VALID_VERSIONED, validated.getStatus());
      List<Uri> resList = (i % 2 == 0) ? RESOURCES_ONE : RESOURCES_TWO;
      assertEquals(resList, validated.getBatch());
      assertEquals(type, validated.getType());
    }
  }
  
  private void checkBatchInsufficientVersions(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(true, true);
    String host = "bar.com";
    String path = "/other/path";
    String[] versions = new String[] { "v1" };  // Only one for three resources.
    ConcatUriManager.Versioner versioner = makeVersioner(null, versions);
    DefaultConcatUriManager manager = makeManager(host, path, "token", versioner);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(RESOURCES_ONE, RESOURCES_ONE);
    manager.make(fromList(gadget, resourceUris, type), true);
  }
  
  private void checkMissingHostConfig(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager(null, "/foo", "token", null);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(RESOURCES_ONE);
    manager.make(fromList(gadget, resourceUris, type), true);
  }
  
  private void checkMissingPathConfig(ConcatUriManager.Type type) throws Exception {
    Gadget gadget = mockGadget(false, false);
    DefaultConcatUriManager manager = makeManager("host.com", null, "token", null);
    List<List<Uri>> resourceUris = ImmutableList.<List<Uri>>of(RESOURCES_ONE);
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
    expect(versioner.version(isA(List.class), eq(CONTAINER)))
        .andReturn(ImmutableList.copyOf(versions)).anyTimes();
    expect(versioner.validate(isA(List.class), eq(CONTAINER), isA(String.class)))
        .andReturn(status).anyTimes();
    replay(versioner);
    return versioner;
  }
}
