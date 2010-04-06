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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import org.junit.Test;

import java.util.Map;

/**
 * Test of FetchResponseUtils
 */
public class FetchResponseUtilsTest {

  @Test
  public void testSimpleResponse() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .create();
    Map<String, Object> obj = FetchResponseUtils.getResponseAsJson(response, "key", "body", false);

    JsonAssert.assertObjectEquals("{'rc':999,'id':'key',body:'body'}", obj);
  }

  @Test
  public void testMetadata() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .setMetadata("metaname", "metavalue")
        .setMetadata("more meta", "more value")
        .create();
    Map<String, Object> obj = FetchResponseUtils.getResponseAsJson(response, null, "body", false);

    JsonAssert.assertObjectEquals(
        "{rc:999,body:'body',metaname:'metavalue','more meta':'more value'}", obj);
  }

  @Test
  public void testHeaders() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .setHeader("Set-Cookie", "cookie")
        .setHeader("location", "here")
        .create();
    Map<String, Object> obj = FetchResponseUtils.getResponseAsJson(response, "key", "body", false);
    JsonAssert.assertObjectEquals(
        "{rc:999,id:'key',body:'body',headers:{set-cookie:['cookie'],location:['here']}}", obj);
  }

  @Test
  public void testMultiValuedHeaders() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .addHeader("Set-Cookie", "cookie")
        .addHeader("Set-Cookie", "cookie2")
        .addHeader("Set-Cookie", "cookie3")
        .create();
    Map<String, Object> obj = FetchResponseUtils.getResponseAsJson(response, "key", "body", false);
    JsonAssert.assertObjectEquals(
        "{rc:999,id:'key',body:'body',headers:{set-cookie:['cookie','cookie2','cookie3']}}", obj);
  }
}
