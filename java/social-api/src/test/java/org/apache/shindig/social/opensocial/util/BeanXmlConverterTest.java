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

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;

import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

public class BeanXmlConverterTest extends TestCase {
  private Person johnDoe;
  private Activity activity;

  private BeanXmlConverter beanXmlConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    johnDoe = new Person("johnDoeId", new Name("John Doe"));
    List<Phone> phones = new ArrayList<Phone>();
    phones.add(new Phone("+33H000000000", "home"));
    phones.add(new Phone("+33M000000000", "mobile"));
    phones.add(new Phone("+33W000000000", "work"));
    johnDoe.setPhoneNumbers(phones);

    List<Address> addresses = new ArrayList<Address>();
    addresses.add(new Address("My home address"));
    johnDoe.setAddresses(addresses);

    List<Email> emails = new ArrayList<Email>();
    emails.add(new Email("john.doe@work.bar", "work"));
    emails.add(new Email("john.doe@home.bar", "home"));
    johnDoe.setEmails(emails);

    activity = new Activity("activityId", johnDoe.getId());

    List<MediaItem> mediaItems = new ArrayList<MediaItem>();
    mediaItems.add(new MediaItem("image/jpg", MediaItem.Type.IMAGE,
        "http://foo.bar"));
    activity.setMediaItems(mediaItems);

    beanXmlConverter = new BeanXmlConverter();
  }

  public static class SimplePerson {
    private String id;
    private String name;

    public SimplePerson(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

  public void testToXmlOnSimpleClass() throws Exception {
    SimplePerson cassie = new SimplePerson("5", "robot");
    String xml = beanXmlConverter.convertToXml(cassie);

    assertEquals("<beanxmlconvertertest$simpleperson>"
        + "<id>5</id>"
        + "<name>robot</name>"
        + "</beanxmlconvertertest$simpleperson>",
        StringUtils.deleteWhitespace(xml));
  }

  public void testPersonToXml() throws Exception {
    String xml = beanXmlConverter.convertToXml(johnDoe);
    // TODO: Make the person xml stop returning empty elements!
    // This test is too messy with them
  }

  public void testActivityToXml() throws Exception {
    String xml = beanXmlConverter.convertToXml(activity);
    // TODO: Make the activity xml stop returning empty elements!
  }

  public void testMapsToXml() throws Exception {
    // This is the structure our app data currently takes
    Map<String, Map<String, String>> map =
        new TreeMap<String, Map<String, String>>();

    Map<String, String> item1Map = new HashMap<String, String>();
    item1Map.put("value", "1");
    map.put("item1", item1Map);

    Map<String, String> item2Map = new HashMap<String, String>();
    item2Map.put("value", "2");
    map.put("item2", item2Map);

    String xml = beanXmlConverter.convertToXml(map);

    // TODO: I don't believe this is the output we are looking for for app
    // data... we will probably have to tweak this.
    String expectedXml =
        "<treemap>" +
        "<empty>false</empty>" +
        "<entry>" +
          "<key>item1</key>" +
          "<value>" +
            "<empty>false</empty>" +
            "<entry>" +
              "<key>value</key>" +
              "<value>1</value>" +
            "</entry>" +
          "</value>" +
        "</entry>" +
        "<entry>" +
          "<key>item2</key>" +
          "<value>" +
            "<empty>false</empty>" +
            "<entry>" +
              "<key>value</key>" +
              "<value>2</value>" +
            "</entry>" +
          "</value>" +
        "</entry>" +
        "</treemap>";
    assertEquals(expectedXml, StringUtils.deleteWhitespace(xml));
  }

}
