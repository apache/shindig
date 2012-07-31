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
package org.apache.shindig.gadgets.features;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Maps;

import org.apache.shindig.common.Pair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

public class FeatureResourceLoaderTest {
  private final static String FILE_JS = "gadgets.test.pattern = function(){};";
  private final static String UPDATED_FILE_JS = "different.impl.completely = function(){};";
  private final static String UNCOMPRESSED_FILE_JS
      = "/** Some comments* /\n" +
        "gadgets.test.pattern = function() {" +
        "};";
  private final static String UPDATED_UNCOMPRESSED_FILE_JS
  = "/** Different comments* /\n" +
    "different.impl.completely = function() {" +
    "};";
  private final static String URL_JS = "while(true){alert('hello');}";

  private TestFeatureResourceLoader loader;
  private FakeTimeSource timeSource;
  private HttpFetcher fetcher;


  private static class TestFeatureResourceLoader extends FeatureResourceLoader {
    public TestFeatureResourceLoader(
        HttpFetcher fetcher, TimeSource timeSource, FeatureFileSystem fileSystem) {
      super(fetcher, timeSource, fileSystem);
    }

    private final Map<String, Boolean> forceFileChanged = Maps.newHashMap();

    @Override
    protected boolean fileHasChanged(org.apache.shindig.gadgets.features.FeatureFile file, long lastModified) {
      // TODO: Update test to use a mocked file and file system instead of real files
      Boolean changeOverride = forceFileChanged.get(file.getAbsolutePath());
      return file.lastModified() > lastModified ? true :
          changeOverride != null && changeOverride;
    }
  }

  @Before
  public void setUp() {
    fetcher = createMock(HttpFetcher.class);
    timeSource = new FakeTimeSource();
    timeSource.setCurrentTimeMillis(0);
    loader = new TestFeatureResourceLoader(fetcher, timeSource, new DefaultFeatureFileSystem());
  }

  @Test
  public void loadFileOptOnlyAvailable() throws Exception {
    Pair<Uri, File> optUri = makeFile(".opt.js", FILE_JS);
    FeatureResource resource = loader.load(optUri.one, null);
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }

  @Test
  public void loadFileDebugOnlyAvailable() throws Exception {
    Pair<Uri, File> dbgUri = makeFile(".js", UNCOMPRESSED_FILE_JS);
    FeatureResource resource = loader.load(dbgUri.one, null);
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }

  @Test
  public void loadFileBothModesAvailable() throws Exception {
    Pair<Uri, File> optUri = makeFile(".opt.js", FILE_JS);
    File dbgFile = new File(optUri.one.getPath().replace(".opt.js", ".js"));
    dbgFile.createNewFile();
    Pair<Uri, File> dbgUri = makeFile(dbgFile, UNCOMPRESSED_FILE_JS);
    FeatureResource resource = loader.load(dbgUri.one, null);
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }

  @Test(expected=IllegalArgumentException.class)
  public void loadFileNothingAvailable() throws Exception {
    Uri nilUri = new UriBuilder().setScheme("file").setPath("/does/not/exist.js").toUri();
    loader.load(nilUri, null);
    fail("Should have failed indicating could not find: " + nilUri.toString());
  }

  @Test
  public void loadFileNoOptPathCalculable() throws Exception {
    // File doesn't end in .js, so it's loaded for both opt and debug.
    Pair<Uri, File> dbgUri = makeFile(".notjssuffix", UNCOMPRESSED_FILE_JS);
    FeatureResource resource = loader.load(dbgUri.one, null);
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }

  @Test
  public void loadFileUpdateIgnoredIfUpdatesDisabled() throws Exception {
    Pair<Uri, File> optUri = makeFile(".opt.js", FILE_JS);
    FeatureResource resource = loader.load(optUri.one, null);
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
    setFileContent(optUri.two, UPDATED_FILE_JS);

    // Advance the time. Update checks disabled by default.
    timeSource.incrementSeconds(10);

    // Same asserts.
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }

  @Test
  public void loadFileUpdateBehavior() throws Exception {
    loader.setSupportFileUpdates(5000);  // set in millis
    Pair<Uri, File> optUri = makeFile(".opt.js", FILE_JS);
    File dbgFile = new File(optUri.one.getPath().replace(".opt.js", ".js"));
    dbgFile.createNewFile();
    Pair<Uri, File> dbgUri = makeFile(dbgFile, UNCOMPRESSED_FILE_JS);
    FeatureResource resource = loader.load(dbgUri.one, null);
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());

    // Update file contents.
    setFileContent(optUri.two, UPDATED_FILE_JS);
    loader.forceFileChanged.put(optUri.two.getAbsolutePath(), true);
    setFileContent(dbgUri.two, UPDATED_UNCOMPRESSED_FILE_JS);
    loader.forceFileChanged.put(dbgUri.two.getAbsolutePath(), true);

    // Advance the time, but not by 5 seconds.
    timeSource.incrementSeconds(4);

    // Same asserts.
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());

    // Advance the time, now beyond 5 seconds.
    timeSource.incrementSeconds(4);

    // New content should be reflected.
    assertEquals(UPDATED_FILE_JS, resource.getContent());
    assertEquals(UPDATED_UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }

  @Test
  public void loadUriInline() throws Exception {
    Uri uri = Uri.parse("http://apache.org/resource.js");
    Map<String, String> attribs = Maps.newHashMap();
    attribs.put("inline", "true");
    mockFetcher(uri, URL_JS);
    FeatureResource resource = loader.load(uri, attribs);
    assertEquals(URL_JS, resource.getContent());
    assertEquals(URL_JS, resource.getDebugContent());
    assertTrue(resource.isProxyCacheable());
    assertFalse(resource.isExternal());
  }

  @Test
  public void loadUriInlineFetcherFailure() throws Exception {
    Uri uri = Uri.parse("http://apache.org/resource.js");
    Map<String, String> attribs = Maps.newHashMap();
    attribs.put("inline", "true");
    expect(fetcher.fetch(eq(new HttpRequest(uri))))
        .andThrow(new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT));
    replay(fetcher);
    FeatureResource resource = loader.load(uri, attribs);
    assertNull(resource.getContent());
    assertNull(resource.getDebugContent());
    assertFalse(resource.isProxyCacheable());
    assertFalse(resource.isExternal());
  }

  @Test
  public void loadUriExtern() throws Exception {
    String theUrl = "http://apache.org/resource.js";
    Uri uri = Uri.parse(theUrl);
    Map<String, String> attribs = Maps.newHashMap();
    mockFetcher(uri, URL_JS);
    FeatureResource resource = loader.load(uri, attribs);
    assertEquals(theUrl, resource.getContent());
    assertEquals(theUrl, resource.getDebugContent());
    assertTrue(resource.isProxyCacheable());
    assertTrue(resource.isExternal());
  }

  @Test
  public void loadRequestMarkedInternal() throws Exception {
    String theUrl = "http://apache.org/resource.js";
    Uri uri = Uri.parse(theUrl);
    Map<String, String> attribs = Maps.newHashMap();
    attribs.put( "inline", "true" );
    CapturingHttpFetcher fetcher = new CapturingHttpFetcher();
    FeatureResourceLoader frLoader = new TestFeatureResourceLoader(fetcher, timeSource, new DefaultFeatureFileSystem());
    FeatureResource resource = frLoader.load(uri, attribs);
    assertEquals(URL_JS, resource.getContent());
    assertNotNull( fetcher.request );
    assertTrue( fetcher.request.isInternalRequest() );
  }

  private Pair<Uri, File> makeFile(String suffix, String content) throws Exception {
    File tmpFile = File.createTempFile("restmp", suffix);
    return makeFile(tmpFile, content);
  }

  private Pair<Uri, File> makeFile(File file, String content) throws Exception {
    file.deleteOnExit();
    setFileContent(file, content);
    return Pair.of(new UriBuilder().setScheme("file").setPath(file.getPath()).toUri(), file);
  }

  private void setFileContent(File file, String content) throws Exception {
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(content);
    out.close();
  }

  private void mockFetcher(Uri toFetch, String content) throws Exception {
    HttpRequest req = new HttpRequest(toFetch);
    HttpResponse resp =
        new HttpResponseBuilder().setHttpStatusCode(HttpResponse.SC_OK)
                                 .setResponseString(content).create();
    expect(fetcher.fetch(eq(req))).andReturn(resp);
    replay(fetcher);
  }

  static class CapturingHttpFetcher implements HttpFetcher
  {
    public HttpRequest request;

    public CapturingHttpFetcher() {
    }

    public HttpResponse fetch(HttpRequest request) throws GadgetException {
      this.request = request;
      return new HttpResponseBuilder().setHttpStatusCode( HttpResponse.SC_OK )
                                      .setResponseString( URL_JS ).create();
    }
  }
}
