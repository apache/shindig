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
package org.apache.shindig.social.opensocial.util;

import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.AddressImpl;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.EmailImpl;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.MediaItemImpl;
import org.apache.shindig.social.opensocial.model.NameImpl;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.PhoneImpl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class BeanJsonConverterTest extends TestCase {
  private Person johnDoe;
  private Activity activity;

  private BeanJsonConverter beanJsonConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    johnDoe = new PersonImpl("johnDoeId", new NameImpl("John Doe"));
    johnDoe.setPhoneNumbers(Lists.<Phone>newArrayList(
        new PhoneImpl("+33H000000000", "home"),
        new PhoneImpl("+33M000000000", "mobile"),
        new PhoneImpl("+33W000000000", "work")));

    johnDoe.setAddresses(Lists.<Address>newArrayList(new AddressImpl("My home address")));

    johnDoe.setEmails(Lists.<Email>newArrayList(
        new EmailImpl("john.doe@work.bar", "work"),
        new EmailImpl("john.doe@home.bar", "home")));

    activity = new ActivityImpl("activityId", johnDoe.getId());

    activity.setMediaItems(Lists.<MediaItem>newArrayList(
        new MediaItemImpl("image/jpg", MediaItem.Type.IMAGE, "http://foo.bar")));

    beanJsonConverter = new BeanJsonConverter(
        Guice.createInjector(new SocialApiTestsGuiceModule()));
  }

  public static class SpecialPerson extends PersonImpl {
    private String newfield;

    public SpecialPerson(String id, String name, String newfield) {
      super(id, new NameImpl(name));
      this.newfield = newfield;
    }

    public String getNewfield() {
      return newfield;
    }
  }

  public void testToJsonOnInheritedClass() throws Exception {
    SpecialPerson cassie = new SpecialPerson("5", "robot", "nonsense");

    JSONObject result = (JSONObject) beanJsonConverter.convertToJson(cassie);
    assertEquals(cassie.getId(), result.getString("id"));
    assertEquals(cassie.getNewfield(), result.getString("newfield"));
  }

  public void testPersonToJson() throws Exception {
    JSONObject result = (JSONObject) beanJsonConverter.convertToJson(johnDoe);

    assertEquals(johnDoe.getId(), result.getString("id"));

    assertEquals(johnDoe.getName().getUnstructured(),
        result.getJSONObject("name").getString("unstructured"));

    assertEquals(johnDoe.getAddresses().get(0).getUnstructuredAddress(),
        result.getJSONArray("addresses").getJSONObject(0)
            .getString("unstructuredAddress"));

    JSONArray phoneArray = result.getJSONArray("phoneNumbers");
    assertEquals(3, phoneArray.length());

    for (int i = 0; i < johnDoe.getPhoneNumbers().size(); i++) {
      Phone expectedPhone = johnDoe.getPhoneNumbers().get(i);
      JSONObject actualPhone = phoneArray.getJSONObject(i);
      assertEquals(expectedPhone.getType(), actualPhone.getString("type"));
      assertEquals(expectedPhone.getNumber(), actualPhone.getString("number"));
    }

    JSONArray emailArray = result.getJSONArray("emails");
    assertEquals(2, emailArray.length());

    for (int i = 0; i < johnDoe.getEmails().size(); i++) {
      Email expectedEmail = johnDoe.getEmails().get(i);
      JSONObject actualEmail = emailArray.getJSONObject(i);
      assertEquals(expectedEmail.getType(), actualEmail.getString("type"));
      assertEquals(expectedEmail.getAddress(),
          actualEmail.getString("address"));
    }
  }

  public void testActivityToJson() throws Exception {
    JSONObject result = (JSONObject) beanJsonConverter.convertToJson(activity);

    assertEquals(activity.getUserId(), result.getString("userId"));
    assertEquals(activity.getId(), result.getString("id"));

    JSONArray mediaItemsArray = result.getJSONArray("mediaItems");
    assertEquals(1, mediaItemsArray.length());

    MediaItem expectedItem = activity.getMediaItems().get(0);
    JSONObject actualItem = mediaItemsArray.getJSONObject(0);

    assertEquals(expectedItem.getUrl(), actualItem.getString("url"));
    assertEquals(expectedItem.getMimeType(), actualItem.getString("mimeType"));
    assertEquals(expectedItem.getType().toString(),
        actualItem.getString("type"));
  }

  public void testMapsToJson() throws Exception {
    Map<String, Map<String, String>> map = Maps.newHashMap();

    Map<String, String> item1Map = Maps.newHashMap();
    item1Map.put("value", "1");

    // Null values shouldn't cause exceptions
    item1Map.put("value2", null);
    map.put("item1", item1Map);

    Map<String, String> item2Map = Maps.newHashMap();
    item2Map.put("value", "2");
    map.put("item2", item2Map);

    JSONObject jsonMap = (JSONObject) beanJsonConverter.convertToJson(map);

    assertEquals("1", jsonMap.getJSONObject("item1").getString("value"));
    assertEquals("2", jsonMap.getJSONObject("item2").getString("value"));
  }

  public void testListsToJson() throws Exception {
    Map<String, String> item1Map = Maps.newHashMap();
    item1Map.put("value", "1");

    Map<String, String> item2Map = Maps.newHashMap();
    item2Map.put("value", "2");

    JSONArray jsonArray = (JSONArray) beanJsonConverter.convertToJson(
        Lists.newArrayList(item1Map, item2Map));

    assertEquals("1", ((JSONObject) jsonArray.get(0)).getString("value"));
    assertEquals("2", ((JSONObject) jsonArray.get(1)).getString("value"));
  }

  public void testArrayToJson() throws Exception {
    String[] colors = {"blue", "green", "aquamarine"};
    JSONArray jsonArray = (JSONArray) beanJsonConverter.convertToJson(colors);

    assertEquals(colors.length, jsonArray.length());
    assertEquals(colors[0], jsonArray.get(0));
  }

  public void testJsonToActivity() throws Exception {
    String jsonActivity = "{userId : 5, id : 6, mediaItems : [" +
        "{url : 'hello', mimeType : 'mimey', type : 'video'}" +
        "]}";
    // TODO: rename the enums to be lowercase
    Activity result = beanJsonConverter.convertToObject(jsonActivity,
        Activity.class);

    assertEquals("5", result.getUserId());
    assertEquals("6", result.getId());

    assertEquals(1, result.getMediaItems().size());

    MediaItem actualItem = result.getMediaItems().get(0);

    assertEquals("hello", actualItem.getUrl());
    assertEquals("mimey", actualItem.getMimeType());
    assertEquals("video", actualItem.getType().toString());
  }

  public void testJsonToMap() throws Exception {
    String jsonActivity = "{count : 0, favoriteColor : 'yellow'}";
    Map<String, String> data = Maps.newHashMap();
    data = beanJsonConverter.convertToObject(jsonActivity,
        (Class<Map<String, String>>) data.getClass());

    assertEquals(2, data.size());

    for (String key : data.keySet()) {
      String value = data.get(key);
      if (key.equals("count")) {
        assertEquals("0", value);
      } else if (key.equals("favoriteColor")) {
        assertEquals("yellow", value);
      }
    }
  }

}
