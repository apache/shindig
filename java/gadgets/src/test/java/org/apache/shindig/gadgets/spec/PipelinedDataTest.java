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
package org.apache.shindig.gadgets.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.spec.PipelinedData.BatchItem;
import org.apache.shindig.gadgets.spec.PipelinedData.BatchType;

import java.util.Map;

import javax.el.ELResolver;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PipelinedDataTest {

  private static final Uri GADGET_URI = Uri.parse("http://example.org/");
  private ELResolver elResolver;
  private Map<String, Object> elValues;
  private Expressions expressions;

  @Before
  public void setUp() {
    elValues = Maps.newHashMap();
    elResolver = new RootELResolver(elValues);
    expressions = Expressions.forTesting();
  }

  @Test
  public void testDataRequest() throws Exception {
    String xml = "<Content><DataRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " method=\"people.get\""
        + " groupId=\"${params.groupId}\""
        + " userId=\"${userIds}\""
        + " startIndex=\"${startIndex}\""
        + " fields=\"${fields}\""
        + "/></Content>";

    elValues.put("startIndex", 10);
    // Test a param that evaluates to null
    elValues.put("params", ImmutableMap.of());
    elValues.put("userIds", Lists.newArrayList("first", "second"));
    elValues.put("fields", new JSONArray("['name','id']"));
    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertFalse(socialData.needsOwner());
    assertFalse(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'people.get', id: 'key', params:"
        + "{userId: ['first','second'],"
        + "startIndex: 10,"
        + "fields: ['name','id']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
  }

  @Test
  public void testPeopleRequest() throws Exception {
    String xml = "<Content><PeopleRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " groupId=\"group\""
        + " userId=\"first,second\""
        + " startIndex=\"20\""
        + " count=\"10\""
        + " fields=\"name,id\""
        + "/></Content>";

    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertFalse(socialData.needsOwner());
    assertFalse(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'people.get', id: 'key', params:"
        + "{groupId: 'group',"
        + "userId: ['first','second'],"
        + "startIndex: 20,"
        + "count: 10,"
        + "fields: ['name','id']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
    assertNull(batch.getNextBatch(elResolver));
  }

  @Test
  public void testPeopleRequestWithExpressions() throws Exception {
    String xml = "<Content><PeopleRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " groupId=\"group\""
        + " userId=\"first,second\""
        + " startIndex=\"20\""
        + " count=\"${count}\""
        + " fields=\"${fields}\""
        + "/></Content>";

    elValues.put("count", 10);
    // TODO: try List, JSONArray
    elValues.put("fields", "name,id");
    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertFalse(socialData.needsOwner());
    assertFalse(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'people.get', id: 'key', params:"
        + "{groupId: 'group',"
        + "userId: ['first','second'],"
        + "startIndex: 20,"
        + "count: 10,"
        + "fields: ['name','id']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
  }

  @Test
  public void testViewerRequest() throws Exception {
    String xml = "<Content><ViewerRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " fields=\"name,id\""
        + "/></Content>";

    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertFalse(socialData.needsOwner());
    assertTrue(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'people.get', id: 'key', params:"
        + "{userId: ['@viewer'],"
        + "fields: ['name','id']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
  }

  @Test
  public void testOwnerRequest() throws Exception {
    String xml = "<Content><OwnerRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " fields=\"name,id\""
        + "/></Content>";

    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertTrue(socialData.needsOwner());
    assertFalse(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'people.get', id: 'key', params:"
        + "{userId: ['@owner'],"
        + "fields: ['name','id']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
  }

  @Test
  public void testPersonAppData() throws Exception {
    String xml = "<Content><PersonAppDataRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " userId=\"@viewer\""
        + " fields=\"foo,bar\""
        + "/></Content>";

    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertFalse(socialData.needsOwner());
    assertTrue(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'appdata.get', id: 'key', params:"
        + "{userId: ['@viewer'],"
        + "fields: ['foo','bar']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
  }

  @Test
  public void testActivitiesRequest() throws Exception {
    String xml = "<Content><ActivitiesRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " userId=\"@owner,@viewer\""
        + " startIndex=\"10\""
        + " count=\"20\""
        + " fields=\"foo,bar\""
        + "/></Content>";


    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertTrue(socialData.needsOwner());
    assertTrue(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'activities.get', id: 'key', params:"
        + "{userId: ['@owner','@viewer'],"
        + "startIndex: 10,"
        + "count: 20,"
        + "fields: ['foo','bar']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
  }

  @Test
  public void testActivityStreamsRequest() throws Exception {
    String xml = "<Content><ActivityStreamsRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " userId=\"@owner,@viewer\""
        + " startIndex=\"10\""
        + " count=\"20\""
        + " fields=\"foo,bar\""
        + "/></Content>";


    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertTrue(socialData.needsOwner());
    assertTrue(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'activitystreams.get', id: 'key', params:"
        + "{userId: ['@owner','@viewer'],"
        + "startIndex: 10,"
        + "count: 20,"
        + "fields: ['foo','bar']"
        + "}}");

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertEquals(1, batch.getPreloads().size());
    PipelinedData.BatchItem batchItem = batch.getPreloads().get("key");
    assertEquals(PipelinedData.BatchType.SOCIAL, batchItem.getType());
    JsonAssert.assertObjectEquals(expected, batchItem.getData());
  }

  @Test
  public void testIgnoreNoNamespace() throws Exception {
    String xml = "<Content><PersonRequest"
        + " key=\"key\""
        + " userId=\"@owner\""
        + " fields=\"name,id\""
        + "/></Content>";

    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertFalse(socialData.needsOwner());

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);
    assertNull(batch);
  }

  @Test(expected = SpecParserException.class)
  public void testErrorOnUnknownOpensocialElement() throws Exception {
    String xml = "<Content><NotARealElement xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + "/></Content>";

    new PipelinedData(XmlUtil.parse(xml), null);
  }

  @Test
  public void testBatching() throws Exception {
    String xml = "<Content xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\">"
        + "<PeopleRequest key=\"key\" userId=\"${userId}\"/>"
        + "<HttpRequest key=\"key2\" href=\"${key}\"/>"
        + "</Content>";

    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), GADGET_URI);

    PipelinedData.Batch batch = socialData.getBatch(expressions, elResolver);

    assertTrue(batch.getPreloads().isEmpty());

    // Now have "userId", the next batch should resolve the people request
    elValues.put("userId", "foo");
    batch = batch.getNextBatch(elResolver);
    assertEquals(1, batch.getPreloads().size());
    assertTrue(batch.getPreloads().containsKey("key"));

    // Now, add "key", the next batch should resolve the HTTP request
    elValues.put("key", "somedata");
    batch = batch.getNextBatch(elResolver);
    assertEquals(1, batch.getPreloads().size());
    assertTrue(batch.getPreloads().containsKey("key2"));

    // And the final batch should be empty
    assertNull(batch.getNextBatch(elResolver));
  }

  @Test
  public void testVariable() throws Exception {
    String xml = "<Content xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\">"
            + "<osx:Variable xmlns:osx=\"" + PipelinedData.EXTENSION_NAMESPACE + "\" "
            +       "key=\"key\" value=\"${1+1}\"/>"
        + "</Content>";

    PipelinedData pipelinedData = new PipelinedData(XmlUtil.parse(xml), GADGET_URI);

    PipelinedData.Batch batch = pipelinedData.getBatch(expressions, elResolver);
    assertFalse(pipelinedData.needsViewer());
    assertFalse(pipelinedData.needsOwner());

    assertEquals(1, batch.getPreloads().size());
    BatchItem output = batch.getPreloads().get("key");
    assertEquals(BatchType.VARIABLE, output.getType());
    assertEquals(2L, output.getData());
  }

 @Test
  public void httpRequestDefaults() throws Exception {
    String xml = "<Content><HttpRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " href=\"/example.html\""
        + "/></Content>";

    PipelinedData pipelinedData = new PipelinedData(XmlUtil.parse(xml), GADGET_URI);
    PipelinedData.Batch batch = pipelinedData.getBatch(expressions, elResolver);
    assertFalse(pipelinedData.needsViewer());
    assertFalse(pipelinedData.needsOwner());

    assertEquals(1, batch.getPreloads().size());
    BatchItem output = batch.getPreloads().get("key");
    assertEquals(BatchType.HTTP, output.getType());
    RequestAuthenticationInfo preload = (RequestAuthenticationInfo) output.getData();
    assertEquals(AuthType.NONE, preload.getAuthType());
    assertEquals(Uri.parse("http://example.org/example.html"), preload.getHref());
  }

  @Test
  public void badHrefTest() throws Exception {
    // unparseable url escape
    String xml = "<Content><HttpRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " href=\"/example.html%\""
        + "/></Content>";

    boolean foundException = false;
    try {
      PipelinedData pipelinedData = new PipelinedData(XmlUtil.parse(xml), GADGET_URI);
      PipelinedData.Batch batch = pipelinedData.getBatch(expressions, elResolver);
    } catch (RuntimeException e) {
      foundException = true;
    }
    assertTrue("found RuntimeException (for now) see SHINDIG-1090", foundException);
  }

  @Test
  public void httpRequestDefaultsSigned() throws Exception {
    String xml = "<Content><HttpRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " href=\"/example.html\""
        + " authz=\"signed\""
        + " sign_owner=\"false\""
        + "/></Content>";

    PipelinedData pipelinedData = new PipelinedData(XmlUtil.parse(xml), GADGET_URI);
    PipelinedData.Batch batch = pipelinedData.getBatch(expressions, elResolver);
    assertTrue(pipelinedData.needsViewer());
    assertFalse(pipelinedData.needsOwner());

    assertEquals(1, batch.getPreloads().size());
    BatchItem output = batch.getPreloads().get("key");
    assertEquals(BatchType.HTTP, output.getType());
    RequestAuthenticationInfo preload = (RequestAuthenticationInfo) output.getData();
    assertEquals(AuthType.SIGNED, preload.getAuthType());
    assertTrue(preload.isSignViewer());
    assertFalse(preload.isSignOwner());
  }
}
