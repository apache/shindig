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
package org.apache.shindig.social.dataservice;

import org.apache.shindig.common.SecurityToken;

import junit.framework.TestCase;

import java.util.Map;

import com.google.common.collect.Maps;

public class RequestItemTest extends TestCase {

  public void testParseUrl() throws Exception {
    String path = "/people/john.doe/@self";

    RequestItem request = new RequestItem();
    request.setUrl(path + "?fields=huey,dewey,louie");

    request.parseUrlParamsIntoParameters();

    assertEquals(path, request.getUrl());
    assertEquals("huey,dewey,louie", request.getParameters().get("fields"));

    // Try it without any params
    request = new RequestItem();
    request.setUrl(path);

    request.parseUrlParamsIntoParameters();

    assertEquals(path, request.getUrl());
    assertEquals(null, request.getParameters().get("fields"));
  }

  public void testBasicFunctions() throws Exception {
    String url = "url";
    Map<String, String> params = Maps.newHashMap();
    SecurityToken token = null;
    String method = "method";
    RequestItem request = new RequestItem(url, params, token, method);

    assertEquals(url, request.getUrl());
    assertEquals(params, request.getParameters());
    assertEquals(token, request.getToken());
    assertEquals(method, request.getMethod());
  }

}