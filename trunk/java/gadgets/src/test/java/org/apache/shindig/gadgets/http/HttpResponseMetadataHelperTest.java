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
package org.apache.shindig.gadgets.http;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class HttpResponseMetadataHelperTest {

  @Test
  public void testUpdateMetadata() {
    HttpResponse local = new HttpResponseBuilder()
        .setResponseString("data1")
        .create();

    HttpResponse compiled = HttpResponseMetadataHelper.updateMetadata(local,
        ImmutableMap.<String, String>of("K", "V"));
    assertEquals(1, compiled.getMetadata().size());
    assertEquals("V", compiled.getMetadata().get("K"));

    HttpResponse compiled2 = HttpResponseMetadataHelper.updateMetadata(compiled,
        ImmutableMap.<String, String>of("K2", "V2"));
    assertEquals(2, compiled2.getMetadata().size());
    assertEquals("V2", compiled2.getMetadata().get("K2"));

    HttpResponse compiled3 = HttpResponseMetadataHelper.updateMetadata(compiled2,
        ImmutableMap.<String, String>of("K", "V3"));
    assertEquals(2, compiled3.getMetadata().size());
    assertEquals("V3", compiled3.getMetadata().get("K"));
  }

  @Test
  public void testHashCodeSimple() {
    HttpResponse local = new HttpResponseBuilder()
        .setResponseString("data1")
        .create();
    verifyHash(local, 1, "h7cg7f1lrrf74jul5h8k6vvlvk");
  }

  @Test
  public void testHashCodeExtraMeta() {
    HttpResponse local = new HttpResponseBuilder()
        .setResponseString("data1")
        .setMetadata(ImmutableMap.<String, String>of("K","V"))
        .setHeader("X-data", "no data")
        .create();
    verifyHash(local, 2, "h7cg7f1lrrf74jul5h8k6vvlvk");
  }

  @Test
  public void testHashCodeError() {
    verifyHash(HttpResponse.error(), 1, "qgeopmcf02p09qc016cepu22fo");
  }

  @Test
  public void testHashCodeEmpty() {
    HttpResponse local = new HttpResponseBuilder()
        .setHttpStatusCode(200)
        .create();
    verifyHash(local, 1, "qgeopmcf02p09qc016cepu22fo");
  }

  private void verifyHash(HttpResponse resp, int metadataSize, String hash) {
    HttpResponseMetadataHelper metdataHelper = new HttpResponseMetadataHelper();
    HttpResponse compiled = HttpResponseMetadataHelper.updateHash(resp, metdataHelper);
    assertEquals(metadataSize, compiled.getMetadata().size());
    assertEquals(hash, compiled.getMetadata().get(HttpResponseMetadataHelper.DATA_HASH));
  }

}
