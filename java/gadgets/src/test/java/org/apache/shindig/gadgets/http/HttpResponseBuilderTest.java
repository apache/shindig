/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for HttpResponseBuilder.
 *
 * This test case compliments HttpResponseTest; not all tests are duplicated here.
 */
public class HttpResponseBuilderTest {

  @Test
  public void copyConstructor() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_NOT_FOUND)
        .setMetadata("foo", "bar")
        .addHeader("Foo-bar", "baz");

    HttpResponseBuilder builder2 = new HttpResponseBuilder(builder);
    assertEquals(builder.create(), builder2.create());
  }

  @Test
  public void addHeader() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Foo-bar", "baz");

    assertEquals("baz", builder.getHeaders().get("Foo-bar").iterator().next());
  }

  @Test
  public void addHeadersMap() {
    Map<String, String> headers = Maps.immutableMap("foo", "bar", "blah", "blah");    

    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeaders(headers);

    assertEquals(Arrays.asList("bar"), builder.getHeaders().get("foo"));
    assertEquals(Arrays.asList("blah"), builder.getHeaders().get("blah"));
  }

  @Test
  public void addAllHeaders() {
    Map<String, List<String>> headers = Maps.newHashMap();

    List<String> foo = Lists.newArrayList("bar", "blah");
    List<String> bar = Lists.newArrayList("baz");
    headers.put("foo", foo);
    headers.put("bar", bar);


    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addAllHeaders(headers);

    assertTrue(builder.getHeaders().get("foo").containsAll(foo));
    assertTrue(builder.getHeaders().get("bar").containsAll(bar));
  }

  @Test
  public void setExpirationTime() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .addHeader("Cache-Control", "public,max-age=100")
        .setExpirationTime(100);

    Map<String, List<String>> headers = builder.getHeaders();
    assertTrue("No Expires header added.", headers.containsKey("Expires"));
    assertFalse("Pragma header not removed", headers.containsKey("Pragma"));
    assertFalse("Cache-Control header not removed", headers.containsKey("Cache-Control"));
  }

  @Test
  public void setCacheTtl() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .addHeader("Expires", "some time stamp normally goes here")
        .setCacheTtl(100);

    Map<String, List<String>> headers = builder.getHeaders();
    assertFalse("Expires header not removed.", headers.containsKey("Expires"));
    assertFalse("Pragma header not removed", headers.containsKey("Pragma"));
    assertEquals("public,max-age=100", headers.get("Cache-Control").get(0));
  }

  @Test
  public void setStrictNoCache() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Cache-Control", "public,max-age=100")
        .addHeader("Expires", "some time stamp normally goes here")
        .setStrictNoCache();

    Map<String, List<String>> headers = builder.getHeaders();
    assertFalse("Expires header not removed.", headers.containsKey("Expires"));
    assertEquals("no-cache", headers.get("Cache-Control").iterator().next());
    assertEquals("no-cache", headers.get("Pragma").get(0));
  }


  @Test
  public void setResponseString() {
    HttpResponse resp = new HttpResponseBuilder()
        .setResponseString("foo")
        .create();
    assertEquals("foo", resp.getResponseAsString());
  }
}
