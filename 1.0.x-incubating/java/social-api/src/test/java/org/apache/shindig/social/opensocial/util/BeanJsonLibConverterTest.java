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

import org.apache.shindig.social.JsonLibTestsGuiceModule;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.core.model.MediaItemImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.util.BeanJsonLibConversionException;
import org.apache.shindig.social.core.util.BeanJsonLibConverter;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Person;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BeanJsonLibConverterTest extends TestCase {

  private static final Log log = LogFactory.getLog(BeanJsonLibConverterTest.class);
  // taken from opensocial-reference/person.js
  private static final String[] PERSON_FIELDS = { "id", "name", "nickname", "thumbnailUrl",
      "profileUrl", "currentLocation", "addresses", "emails", "phoneNumbers", "aboutMe", "status",
      "profileSong", "profileVideo", "gender", "sexualOrientation", "relationshipStatus", "age",
      "dateOfBirth", "bodyType", "ethnicity", "smoker", "drinker", "children", "pets",
      "livingArrangement", "timeZone", "languagesSpoken", "jobs", "jobInterests", "schools",
      "interests", "urls", "music", "movies", "tvShows", "books", "activities", "sports", "heroes",
      "quotes", "cars", "food", "turnOns", "turnOffs", "tags", "romance", "scaredOf",
      "happiestWhen", "fashion", "humor", "lookingFor", "religion", "politicalViews", "hasApp",
      "networkPresence" };

  // taken from opensocial-reference/name.js
  private static final String[] NAME_FIELDS = { "familyName", "givenName", "additionalName",
      "honorificPrefix", "honorificSuffix", "unstructured" };

  private Person johnDoe;
  private Activity activity;

  private BeanJsonLibConverter beanJsonConverter;
  private ApiValidator apiValidator;
  // set to true to get loging output at info level
  private boolean outputInfo = false;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    johnDoe = new PersonImpl("johnDoeId", "Johnny", new NameImpl("John Doe"));
    johnDoe.setPhoneNumbers(Lists.<ListField>newArrayList(
        new ListFieldImpl("home", "+33H000000000"),
        new ListFieldImpl("mobile", "+33M000000000"),
        new ListFieldImpl("work", "+33W000000000")));

    johnDoe.setAddresses(Lists.<Address>newArrayList(new AddressImpl("My home address")));

    johnDoe.setEmails(Lists.<ListField>newArrayList(new ListFieldImpl("work", "john.doe@work.bar"),
        new ListFieldImpl("home", "john.doe@home.bar")));

    activity = new ActivityImpl("activityId", johnDoe.getId());

    activity.setMediaItems(Lists.<MediaItem>newArrayList(new MediaItemImpl("image/jpg",
        MediaItem.Type.IMAGE, "http://foo.bar")));

    Injector injector = Guice.createInjector(new JsonLibTestsGuiceModule());
    beanJsonConverter = injector.getInstance(BeanJsonLibConverter.class);

    apiValidator = new ApiValidator();

  }

  public static class SpecialPerson extends PersonImpl {
    public static final String[] OPTIONALFIELDS = {};
    public static final String[] NULLFIELDS = { "jobInterests", "nickname", "romance", "religion",
        "timeZone", "relationshipStatus", "tags", "networkPresence", "books", "quotes",
        "phoneNumbers", "languagesSpoken", "activities", "jobs", "dateOfBirth", "profileVideo",
        "bodyType", "urls", "schools", "music", "addresses", "livingArrangement", "thumbnailUrl",
        "humor", "sports", "scaredOf", "movies", "age", "pets", "hasApp", "turnOffs", "gender",
        "fashion", "drinker", "aboutMe", "children", "sexualOrientation", "heroes", "profileSong",
        "lookingFor", "cars", "turnOns", "tvShows", "profileUrl", "status", "currentLocation",
        "smoker", "happiestWhen", "ethnicity", "food", "emails", "politicalViews", "interests",
        "familyName", "honorificSuffix", "additionalName", "honorificPrefix", "givenName" };

    private String newfield;

    public SpecialPerson() {
      super();
    }

    public SpecialPerson(String id, String name, String newfield) {
      super(id, name, new NameImpl(name));
      this.newfield = newfield;
    }

    public String getNewfield() {
      return newfield;
    }

    public void setNewfield(String newfield) {
      this.newfield = newfield;
    }

  }

  public void testToJsonOnInheritedClass() throws Exception {
    SpecialPerson cassie = new SpecialPerson("5", "robot", "nonsense");

    String result = beanJsonConverter.convertToString(cassie);

    validatePerson(result, "5", "robot", SpecialPerson.OPTIONALFIELDS, SpecialPerson.NULLFIELDS);

    String[] optional = {};
    String[] nullfields = {};
    Map<String, Object> special = apiValidator.validate(result, new String[] { "newfield" },
        optional, nullfields);
    assertNotNull(special.get("newfield"));
    assertEquals(String.class, special.get("newfield").getClass());
    assertEquals("nonsense", special.get("newfield"));

    // convert back into an object Tree

    SpecialPerson parseCassie = beanJsonConverter.convertToObject(result, SpecialPerson.class);

    assertNotNull(parseCassie);
    assertEquals(cassie.getId(), parseCassie.getId());
    assertEquals(cassie.getNewfield(), parseCassie.getNewfield());

    Name name = parseCassie.getName();
    Name cassieName = cassie.getName();
    assertNotNull(name);
    assertEquals(cassieName.getUnstructured(), name.getUnstructured());
    assertEquals(cassieName.getAdditionalName(), name.getAdditionalName());
    assertEquals(cassieName.getFamilyName(), name.getFamilyName());
    assertEquals(cassieName.getGivenName(), name.getGivenName());
    assertEquals(cassieName.getHonorificPrefix(), name.getHonorificPrefix());
    assertEquals(cassieName.getHonorificSuffix(), name.getHonorificSuffix());

  }

  /**
   * @param result
   * @throws ApiValidatorExpcetion
   */
  private void validatePerson(String result, String id, String name, String[] optional,
      String[] nullfields) throws ApiValidatorExpcetion {

    Map<String, Object> standard = apiValidator.validate(result, PERSON_FIELDS, optional,
        nullfields);
    assertNotNull(standard.get("id"));
    assertEquals(String.class, standard.get("id").getClass());
    assertEquals(id, standard.get("id"));

    assertNotNull(standard.get("name"));
    Map<String, Object> nameJSON = apiValidator.validateObject(standard.get("name"), NAME_FIELDS,
        optional, nullfields);
    ApiValidator.dump(nameJSON);

    assertNotNull(nameJSON.get("unstructured"));
    assertEquals(String.class, nameJSON.get("unstructured").getClass());
    assertEquals(name, nameJSON.get("unstructured"));

    // additional name
    assertNull(nameJSON.get("additionalName"));

  }

  public void testPersonToJson() throws Exception {
    String result = beanJsonConverter.convertToString(johnDoe);
    if (outputInfo) {
      log.info("JSON (" + result + ")");
    }
    Person parsedPerson = beanJsonConverter.convertToObject(result, Person.class);

    assertEquals(johnDoe.getId(), parsedPerson.getId());
    assertEquals(johnDoe.getName().getUnstructured(), parsedPerson.getName().getUnstructured());

    List<Address> addresses = parsedPerson.getAddresses();
    if (outputInfo) {
      for (Object o : addresses) {
        log.info("Address " + o);
      }
    }

    assertEquals(1, addresses.size());
    Address address = addresses.get(0);
    String formatted = address.getFormatted();

    assertNotNull(formatted);
    assertEquals(johnDoe.getAddresses().get(0).getFormatted(), parsedPerson
        .getAddresses().get(0).getFormatted());

    assertEquals(3, parsedPerson.getPhoneNumbers().size());

    for (int i = 0; i < johnDoe.getPhoneNumbers().size(); i++) {
      ListField expectedPhone = johnDoe.getPhoneNumbers().get(i);
      ListField actualPhone = parsedPerson.getPhoneNumbers().get(i);
      assertEquals(expectedPhone.getType(), actualPhone.getType());
      assertEquals(expectedPhone.getValue(), actualPhone.getValue());
    }

    assertEquals(2, parsedPerson.getEmails().size());

    for (int i = 0; i < johnDoe.getEmails().size(); i++) {
      ListField expectedEmail = johnDoe.getEmails().get(i);
      ListField actualEmail = parsedPerson.getEmails().get(i);
      assertEquals(expectedEmail.getType(), actualEmail.getType());
      assertEquals(expectedEmail.getValue(), actualEmail.getValue());
    }
  }

  public void testActivityToJson() throws Exception {

    String result = beanJsonConverter.convertToString(activity);
    if (outputInfo) {
      log.info("JSON (" + result + ")");
    }
    Activity parsedActivity = beanJsonConverter.convertToObject(result, Activity.class);
    assertEquals(activity.getUserId(), parsedActivity.getUserId());
    assertEquals(activity.getId(), parsedActivity.getId());

    assertEquals(1, parsedActivity.getMediaItems().size());

    MediaItem expectedItem = activity.getMediaItems().get(0);
    MediaItem actualItem = parsedActivity.getMediaItems().get(0);

    assertEquals(expectedItem.getUrl(), actualItem.getUrl());
    assertEquals(expectedItem.getMimeType(), actualItem.getMimeType());
    assertEquals(expectedItem.getType().toString(), actualItem.getType().toString());
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

    String result = beanJsonConverter.convertToString(map);
    if (outputInfo) {
      log.info("JSON (" + result + ")");
    }
    // there is introspection that can tell jsonobject -> bean converter what a
    // map should contain, so we have to tell it
    beanJsonConverter.addMapping("item1", Map.class);
    beanJsonConverter.addMapping("item2", Map.class);
    Map<?, ?> parsedMap = beanJsonConverter.convertToObject(result, Map.class);

    if (outputInfo) {
      log.info("Dumping Map (" + parsedMap + ")");
    }
    ApiValidator.dump(parsedMap);

    assertEquals("1", ((Map<?, ?>) parsedMap.get("item1")).get("value"));
    assertEquals("2", ((Map<?, ?>) parsedMap.get("item2")).get("value"));
  }

  public void testListsToJson() throws Exception {
    Map<String, String> item1Map = Maps.newHashMap();
    item1Map.put("value", "1");

    Map<String, String> item2Map = Maps.newHashMap();
    item2Map.put("value", "2");

    // put the list into a container before serializing, top level lists dont
    // appear
    // to be allowed in json
    // just check that the list is in the holder correctly
    List<Map<String, String>> list = new ArrayList<Map<String, String>>();
    list.add(item1Map);
    list.add(item2Map);
    String result = beanJsonConverter.convertToString(list);
    if (outputInfo) {
      log.info("JSON (" + result + ")");
    }
    Map<?, ?>[] parsedList = beanJsonConverter.convertToObject(result, Map[].class);

    assertEquals("1", parsedList[0].get("value"));
    assertEquals("2", parsedList[1].get("value"));
  }

  public void testArrayToJson() throws Exception {
    String[] colors = { "blue", "green", "aquamarine" };
    String result = beanJsonConverter.convertToString(colors);
    if (outputInfo) {
      log.info("JSON (" + result + ")");
    }
    String[] parsedColors = beanJsonConverter.convertToObject(result, String[].class);
    assertEquals(colors.length, parsedColors.length);
    assertEquals(colors[0], parsedColors[0]);
    assertEquals(colors[1], parsedColors[1]);
    assertEquals(colors[2], parsedColors[2]);
  }

  public void testJsonToActivity() throws Exception {
    String jsonActivity = "{userId : 5, id : 6, mediaItems : ["
        + "{url : 'hello', mimeType : 'mimey', type : 'VIDEO'}" + "]}";
    Activity result = beanJsonConverter.convertToObject(jsonActivity, Activity.class);

    assertEquals("5", result.getUserId());
    assertEquals("6", result.getId());

    assertEquals(1, result.getMediaItems().size());

    MediaItem actualItem = result.getMediaItems().get(0);

    assertEquals("hello", actualItem.getUrl());
    assertEquals("mimey", actualItem.getMimeType());
    assertEquals("video", actualItem.getType().toString());
  }

  @SuppressWarnings("unchecked")
  public void testJsonToMap() throws Exception {
    String jsonActivity = "{count : 0, favoriteColor : 'yellow'}";
    Map<String, Object> data = Maps.newHashMap();
    data = beanJsonConverter.convertToObject(jsonActivity, (Class<Map<String, Object>>) data
        .getClass());

    assertEquals(2, data.size());

    for (Entry<String, Object> entry : data.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals("count")) {
        assertEquals(0, value);
      } else if (key.equals("favoriteColor")) {
        assertEquals("yellow", value);
      }
    }
  }

  public void testEmptyObject() {
    assertEquals("{}", beanJsonConverter.convertToString(""));
    assertEquals(0, beanJsonConverter.convertToObject("", Map.class).size());
    assertEquals(0, beanJsonConverter.convertToObject("[]", String[].class).length);
    assertEquals(2, beanJsonConverter.convertToObject("[\"a\",\"b\"]", String[].class).length);
  }

  public void testException() {
    // a bit brain dead, but makes certain the exception is available in all forms
    assertNotNull(new BeanJsonLibConversionException());
    assertNotNull(new BeanJsonLibConversionException("message"));
    assertNotNull(new BeanJsonLibConversionException(new Exception()));
    assertNotNull(new BeanJsonLibConversionException("message", new Exception()));
  }

}
