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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.social.ResponseError;

import org.junit.Test;

import junit.framework.Assert;


/**
 * Tests Response Item equality methods.
 */
public class ResponseItemTest {
  
  @Test
  public void testEquals() {
    ResponseItem responseItem = new ResponseItem(ResponseError.BAD_REQUEST, "message1");
    ResponseItem responseItemSame = new ResponseItem(ResponseError.BAD_REQUEST, "message1");
    ResponseItem responseItemDifferent = new ResponseItem(ResponseError.FORBIDDEN, "message2");
    ResponseItem simpleResponse = new ResponseItem("simple");
    ResponseItem simpleResponseSame = new ResponseItem("simple");
    ResponseItem simpleResponseDifferent = new ResponseItem("simpleDiffernt");
    Assert.assertTrue(responseItem.hashCode() == responseItemSame.hashCode());
    Assert.assertTrue(responseItem.equals(responseItem));
    Assert.assertTrue(responseItem.equals(responseItemSame));
    Assert.assertFalse(responseItem.hashCode() == responseItemDifferent.hashCode());
    Assert.assertFalse(responseItem.equals(responseItemDifferent));
    Assert.assertFalse(responseItem.equals(null));
    Assert.assertFalse(responseItem.equals("A String"));
    Assert.assertTrue(simpleResponse.hashCode() == simpleResponseSame.hashCode());
    Assert.assertTrue(simpleResponse.equals(simpleResponse));
    Assert.assertTrue(simpleResponse.equals(simpleResponseSame));
    Assert.assertFalse(simpleResponse.hashCode() == simpleResponseDifferent.hashCode());
    Assert.assertFalse(simpleResponse.equals(simpleResponseDifferent));
    Assert.assertFalse(simpleResponse.equals(null));
    Assert.assertFalse(simpleResponse.equals("A String"));
  }
}
