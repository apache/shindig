/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.features;

import com.google.common.collect.Maps;

import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.eq;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
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
  private final static String UNCOMPRESSED_FILE_JS
      = "/** Some comments* /\n" +
        "gadgets.test.pattern = function() {" +
        "};";
  private final static String URL_JS = "while(true){alert('hello');}";
  
  private FeatureResourceLoader loader;
  
  @Before
  public void setUp() {
    loader = new FeatureResourceLoader();
  }
  
  @Test
  public void loadFileOptOnlyAvailable() throws Exception {
    Uri optUri = makeFile(".opt.js", FILE_JS);
    FeatureResource resource = loader.load(optUri, null);
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }
  
  @Test
  public void loadFileDebugOnlyAvailable() throws Exception {
    Uri dbgUri = makeFile(".js", UNCOMPRESSED_FILE_JS);
    FeatureResource resource = loader.load(dbgUri, null);
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }
  
  @Test
  public void loadFileBothModesAvailable() throws Exception {
    Uri optUri = makeFile(".opt.js", FILE_JS);
    File dbgFile = new File(optUri.getPath().replace(".opt.js", ".js"));
    dbgFile.createNewFile();
    Uri dbgUri = makeFile(dbgFile, UNCOMPRESSED_FILE_JS);
    FeatureResource resource = loader.load(dbgUri, null);
    assertEquals(FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }
  
  @Test
  public void loadFileNothingAvailable() throws Exception {
    Uri nilUri = new UriBuilder().setScheme("file").setPath("/does/not/exist.js").toUri();
    try {
      loader.load(nilUri, null);
      fail("Should have failed indicating could not find: " + nilUri.toString());
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }
  
  @Test
  public void loadFileNoOptPathCalculable() throws Exception {
    // File doesn't end in .js, so it's loaded for both opt and debug.
    Uri dbgUri = makeFile(".notjssuffix", UNCOMPRESSED_FILE_JS);
    FeatureResource resource = loader.load(dbgUri, null);
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, resource.getDebugContent());
    assertFalse(resource.isExternal());
    assertTrue(resource.isProxyCacheable());
  }
  
  @Test
  public void loadUriInline() throws Exception {
    Uri uri = Uri.parse("http://apache.org/resource.js");
    Map<String, String> attribs = Maps.newHashMap();
    attribs.put("inline", "true");
    HttpFetcher fetcher = mockFetcher(uri, URL_JS);
    loader.setHttpFetcher(fetcher);
    FeatureResource resource = loader.load(uri, attribs);
    assertEquals(URL_JS, resource.getContent());
    assertEquals(URL_JS, resource.getDebugContent());
    assertTrue(resource.isProxyCacheable());
    assertFalse(resource.isExternal());
  }
  
  @Test
  public void loadUriInlineNoFetcherSet() throws Exception {
    Uri uri = Uri.parse("http://apache.org/resource.js");
    Map<String, String> attribs = Maps.newHashMap();
    attribs.put("inline", "true");
    FeatureResource resource = loader.load(uri, attribs);
    assertNull(resource.getContent());
    assertNull(resource.getDebugContent());
    assertFalse(resource.isProxyCacheable());
    assertFalse(resource.isExternal());
  }
  
  @Test
  public void loadUriInlineFetcherFailure() throws Exception {
    Uri uri = Uri.parse("http://apache.org/resource.js");
    Map<String, String> attribs = Maps.newHashMap();
    attribs.put("inline", "true");
    HttpFetcher fetcher = createMock(HttpFetcher.class);
    expect(fetcher.fetch(eq(new HttpRequest(uri))))
        .andThrow(new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT));
    replay(fetcher);
    loader.setHttpFetcher(fetcher);
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
    HttpFetcher fetcher = mockFetcher(uri, URL_JS);
    loader.setHttpFetcher(fetcher);  // should have no effect on its own
    FeatureResource resource = loader.load(uri, attribs);
    assertEquals(theUrl, resource.getContent());
    assertEquals(theUrl, resource.getDebugContent());
    assertTrue(resource.isProxyCacheable());
    assertTrue(resource.isExternal());
  }
  
  private Uri makeFile(String suffix, String content) throws Exception {
    return makeFile(File.createTempFile("restmp", suffix), content);
  }
  
  private Uri makeFile(File file, String content) throws Exception {
    file.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(content);
    out.close();
    return new UriBuilder().setScheme("file").setPath(file.getPath()).toUri();
  }
  
  private HttpFetcher mockFetcher(Uri toFetch, String content) throws Exception {
    HttpFetcher fetcher = createMock(HttpFetcher.class);
    HttpRequest req = new HttpRequest(toFetch);
    HttpResponse resp =
        new HttpResponseBuilder().setHttpStatusCode(HttpResponse.SC_OK)
                                 .setResponseString(content).create();
    expect(fetcher.fetch(eq(req))).andReturn(resp);
    replay(fetcher);
    return fetcher;
  }
}