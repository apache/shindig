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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class JsLibraryTest extends EasyMockTestCase {
  private final static String INLINE_JS = "var hello = 'world'; alert(hello);";
  private final static String FILE_JS = "gadgets.test.pattern = function(){};";
  private final static String UNCOMPRESSED_FILE_JS
      = "/** Some comments*/\n" +
        "gadgets.test.pattern = function() {" +
        "};";
  private final static String URL_JS = "while(true){alert('hello');}";

  public void testInline() throws GadgetException {
    JsLibrary lib = JsLibrary.create(JsLibrary.Type.INLINE, INLINE_JS, null, null);
    assertEquals(JsLibrary.Type.INLINE, lib.getType());
    assertEquals(INLINE_JS, lib.getContent());
  }

  public void testFile() throws Exception {
    File temp = File.createTempFile(this.getName(), ".js-standalone");
    temp.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
    out.write(FILE_JS);
    out.close();

    JsLibrary lib = JsLibrary.create(JsLibrary.Type.FILE, temp.getPath(), null, null);
    assertEquals(JsLibrary.Type.FILE, lib.getType());
    assertEquals(FILE_JS, lib.getContent());
  }

  public void testOptimized() throws Exception {
    File uncompressed = File.createTempFile(this.getName(), ".js");
    uncompressed.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(uncompressed));
    out.write(UNCOMPRESSED_FILE_JS);
    out.close();

    // Test situation when we have no optimized file
    JsLibrary lib = JsLibrary.create(JsLibrary.Type.FILE, uncompressed.getPath(), null, null);
    assertEquals(JsLibrary.Type.FILE, lib.getType());
    assertEquals(UNCOMPRESSED_FILE_JS, lib.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, lib.getDebugContent());

    File compressed = new File(uncompressed.getPath().replace(".js", ".opt.js"));
    // This might fail, but it shouldn't fail if the temp creation worked.
    compressed.createNewFile();
    compressed.deleteOnExit();
    out = new BufferedWriter(new FileWriter(compressed));
    out.write(FILE_JS);
    out.close();

    // Now test situation with compressed and uncompressed 
    lib = JsLibrary.create(JsLibrary.Type.FILE, uncompressed.getPath(), null, null);
    assertEquals(JsLibrary.Type.FILE, lib.getType());
    assertEquals(FILE_JS, lib.getContent());
    assertEquals(UNCOMPRESSED_FILE_JS, lib.getDebugContent());
  }

  public void testUrl() throws Exception {
    HttpFetcher mockFetcher = mock(HttpFetcher.class);
    Uri location = Uri.parse("http://example.org/file.js");
    HttpRequest request = new HttpRequest(location);
    HttpResponse response = new HttpResponseBuilder()
        .setResponse(URL_JS.getBytes())
        .create();
    expect(mockFetcher.fetch(eq(request))).andReturn(response);
    replay();
    JsLibrary lib = JsLibrary.create(
        JsLibrary.Type.URL, location.toString(), null, mockFetcher);
    verify();

    // No type test here because it could potentially change.
    assertEquals(URL_JS, lib.getContent());
    assertEquals(URL_JS, lib.getDebugContent());
  }
}
