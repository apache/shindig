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
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.social.core.model.PersonImpl;

import junit.framework.TestCase;

public class PersonTest extends TestCase {

  public void testInvalidFromUrlString() throws Exception {
    assertNull(Person.Field.fromUrlString("badness"));
  }

  public void testFromUrlString() throws Exception {
    assertUrlStringMaps(Person.Field.NAME);
    assertUrlStringMaps(Person.Field.THUMBNAIL_URL);
  }

  private void assertUrlStringMaps(Person.Field field) {
    assertEquals(field, Person.Field.fromUrlString(field.toString()));
  }

  public void testGetProfileUrl() throws Exception {
    Person person = new PersonImpl();
    assertEquals(null, person.getProfileUrl());

    String address = "hi";
    person.setProfileUrl(address);
    assertEquals(address, person.getProfileUrl());

    assertEquals(address, person.getUrls().get(0).getAddress());
    assertEquals(Person.PROFILE_URL_TYPE, person.getUrls().get(0).getType());
    assertEquals(null, person.getUrls().get(0).getLinkText());

    address = "something new";
    person.setProfileUrl(address);
    assertEquals(address, person.getProfileUrl());

    assertEquals(1, person.getUrls().size());
    assertEquals(address, person.getUrls().get(0).getAddress());
  }

  public void testGetThumbnailUrl() throws Exception {
    Person person = new PersonImpl();
    assertEquals(null, person.getThumbnailUrl());

    String url = "hi";
    person.setThumbnailUrl(url);
    assertEquals(url, person.getThumbnailUrl());

    assertEquals(url, person.getPhotos().get(0).getValue());
    assertEquals(Person.THUMBNAIL_PHOTO_TYPE, person.getPhotos().get(0).getType());

    url = "something new";
    person.setThumbnailUrl(url);
    assertEquals(url, person.getThumbnailUrl());

    assertEquals(1, person.getPhotos().size());
    assertEquals(url, person.getPhotos().get(0).getValue());
  }

}