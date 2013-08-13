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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.protocol.ResponseItem;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import static junitx.framework.Assert.assertNotEquals;


/**
 * Tests Response Item equality methods.
 */
public class ResponseItemTest extends Assert {

  @Test
  public void testEquals() {
    ResponseItem responseItem = new ResponseItem(
        HttpServletResponse.SC_BAD_REQUEST, "message1");
    ResponseItem responseItemSame = new ResponseItem(
        HttpServletResponse.SC_BAD_REQUEST, "message1");
    ResponseItem responseItemDifferent =
      new ResponseItem(HttpServletResponse.SC_FORBIDDEN, "message2");
    ResponseItem simpleResponse = new ResponseItem("simple");
    ResponseItem simpleResponseSame = new ResponseItem("simple");
    ResponseItem simpleResponseDifferent = new ResponseItem("simpleDiffernt");
    assertEquals(responseItem.hashCode(), responseItemSame.hashCode());
    assertEquals(responseItem, responseItem);
    assertEquals(responseItem, responseItemSame);
    assertNotEquals(responseItem.hashCode(), responseItemDifferent.hashCode());
    assertFalse(responseItem.equals(responseItemDifferent));
    assertNotNull(responseItem);
    assertNotEquals(responseItem, "A String");
    assertEquals(simpleResponse.hashCode(), simpleResponseSame.hashCode());
    assertEquals(simpleResponse, simpleResponse);
    assertEquals(simpleResponse, simpleResponseSame);
    assertNotSame(simpleResponse.hashCode(), simpleResponseDifferent.hashCode());
    assertFalse(simpleResponse.equals(simpleResponseDifferent));
    assertNotNull(simpleResponse);
    assertNotEquals(simpleResponse, "A String");
  }
}
