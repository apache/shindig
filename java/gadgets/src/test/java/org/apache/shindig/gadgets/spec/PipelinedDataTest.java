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
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetContext;

import java.util.Map;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class PipelinedDataTest {
  //TODO: test os:MakeRequest

  private GadgetContext context;

  @Before
  public void setUp() {
    context = new GadgetContext();
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

    assertEquals(1, socialData.getSocialPreloads(context).size());
    assertEquals(expected.toString(), socialData.getSocialPreloads(context).get("key").toString());
  }
  
  @Test
  public void testPeopleRequestWithExpressions() throws Exception {
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

    assertEquals(1, socialData.getSocialPreloads(context).size());
    assertEquals(expected.toString(), socialData.getSocialPreloads(context).get("key").toString());
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

    assertEquals(1, socialData.getSocialPreloads(context).size());
    assertEquals(expected.toString(), socialData.getSocialPreloads(context).get("key").toString());
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

    assertEquals(1, socialData.getSocialPreloads(context).size());
    assertEquals(expected.toString(), socialData.getSocialPreloads(context).get("key").toString());
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

    assertEquals(1, socialData.getSocialPreloads(context).size());
    assertEquals(expected.toString(), socialData.getSocialPreloads(context).get("key").toString());
  }

  @Test
  public void testActivitiesRequest() throws Exception {
    String xml = "<Content><ActivitiesRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " userId=\"@owner,@viewer\""
        + " fields=\"foo,bar\""
        + "/></Content>";

    PipelinedData socialData = new PipelinedData(XmlUtil.parse(xml), null);
    assertTrue(socialData.needsOwner());
    assertTrue(socialData.needsViewer());

    JSONObject expected = new JSONObject("{method: 'activities.get', id: 'key', params:"
        + "{userId: ['@owner','@viewer'],"
        + "fields: ['foo','bar']"
        + "}}");

    assertEquals(1, socialData.getSocialPreloads(context).size());
    assertEquals(expected.toString(), socialData.getSocialPreloads(context).get("key").toString());
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

    assertTrue(socialData.getSocialPreloads(context).isEmpty());
  }

  @Test(expected = SpecParserException.class)
  public void testErrorOnUnknownOpensocialElement() throws Exception {
    String xml = "<Content><NotARealElement xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + "/></Content>";

    new PipelinedData(XmlUtil.parse(xml), null);
  }
  
  @Test
  public void makeRequestDefaults() throws Exception {
    String xml = "<Content><MakeRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " href=\"/example.html\""
        + "/></Content>";

    PipelinedData pipelinedData = new PipelinedData(
        XmlUtil.parse(xml), Uri.parse("http://example.org/"));
    Map<String, RequestAuthenticationInfo> httpPreloads = 
        pipelinedData.getHttpPreloads(context);
    
    assertEquals(1, httpPreloads.size());
    RequestAuthenticationInfo preload = httpPreloads.get("key");
    assertEquals(AuthType.NONE, preload.getAuthType());
    assertEquals(Uri.parse("http://example.org/example.html"), preload.getHref());    
  }

  @Test
  public void makeRequestDefaultsSigned() throws Exception {
    String xml = "<Content><MakeRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " href=\"/example.html\""
        + " authz=\"signed\""
        + " sign_owner=\"false\""
        + "/></Content>";

    PipelinedData pipelinedData = new PipelinedData(
        XmlUtil.parse(xml), Uri.parse("http://example.org/"));
    Map<String, RequestAuthenticationInfo> httpPreloads = 
        pipelinedData.getHttpPreloads(context);
    
    assertEquals(1, httpPreloads.size());
    RequestAuthenticationInfo preload = httpPreloads.get("key");
    assertEquals(AuthType.SIGNED, preload.getAuthType());
    assertTrue(preload.isSignViewer());
    assertFalse(preload.isSignOwner());
  }
}
