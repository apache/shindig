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
package org.apache.shindig.social;

import org.json.JSONArray;
import org.json.JSONObject;

import junit.framework.TestCase;

public class SocialDataTest extends TestCase {
  private Person johnDoe;
  private Activity activity;

  @Override
  public void setUp() throws Exception {
    johnDoe = new Person("johnDoeId", new Name("John Doe"));
    Phone[] phones = {
        new Phone("+33H000000000", "home"),
        new Phone("+33M000000000", "mobile"),
        new Phone("+33W000000000", "work")};
    johnDoe.setPhoneNumbers(phones);

    Address[] addresses = {
      new Address("My home address")
    };
    johnDoe.setAddresses(addresses);

    Email[] emails = {
      new Email("john.doe@work.bar", "work"),
      new Email("john.doe@home.bar", "home"),
    };
    johnDoe.setEmails(emails);

    activity = new Activity("activityId", "appId", johnDoe.getId());
    MediaItem[] mediaItems = {
        new MediaItem("image/jpg", MediaItem.Type.IMAGE, "http://foo.bar")
    };
    activity.setMediaItems(mediaItems);
  }

  public void testPersonToJson() throws Exception {
    JSONObject result = johnDoe.toJson();

    assertEquals(johnDoe.getId(), result.getString("id"));

    assertEquals(johnDoe.getName().getUnstructured(),
        result.getJSONObject("name").getString("unstructured"));

    assertEquals(johnDoe.getAddresses()[0].getUnstructuredAddress(),
        result.getJSONArray("addresses").getJSONObject(0)
            .getString("unstructuredAddress"));

    JSONArray phoneArray = result.getJSONArray("phoneNumbers");
    assertEquals(3, phoneArray.length());

    for (int i = 0; i < johnDoe.getPhoneNumbers().length; i++) {
      Phone expectedPhone = johnDoe.getPhoneNumbers()[i];
      JSONObject actualPhone = phoneArray.getJSONObject(i);
      assertEquals(expectedPhone.getType(), actualPhone.getString("type"));
      assertEquals(expectedPhone.getNumber(), actualPhone.getString("number"));
    }

    JSONArray emailArray = result.getJSONArray("emails");
    assertEquals(2, emailArray.length());

    for (int i = 0; i < johnDoe.getEmails().length; i++) {
      Email expectedEmail = johnDoe.getEmails()[i];
      JSONObject actualEmail = emailArray.getJSONObject(i);
      assertEquals(expectedEmail.getType(), actualEmail.getString("type"));
      assertEquals(expectedEmail.getAddress(),
          actualEmail.getString("address"));
    }
  }

  public void testActivityToJson() throws Exception {
    JSONObject result = activity.toJson();
    assertEquals(activity.getUserId(), result.getString("userId"));
    assertEquals(activity.getAppId(), result.getString("appId"));
    assertEquals(activity.getId(), result.getString("id"));

    JSONArray mediaItemsArray = result.getJSONArray("mediaItems");
    assertEquals(1, mediaItemsArray.length());

    MediaItem expectedItem = activity.getMediaItems()[0];
    JSONObject actualItem = mediaItemsArray.getJSONObject(0);

    assertEquals(expectedItem.getUrl(), actualItem.getString("url"));
    assertEquals(expectedItem.getMimeType(), actualItem.getString("mimeType"));
    assertEquals(expectedItem.getType().toString(),
        actualItem.getString("type"));
  }
}
