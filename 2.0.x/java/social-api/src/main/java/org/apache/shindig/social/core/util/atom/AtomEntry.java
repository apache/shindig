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
package org.apache.shindig.social.core.util.atom;

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Person;

import java.util.Date;
import java.util.Map.Entry;

/**
 * This bean represents a Atom Entry for serialization. It contains, optionally
 * a person or an activity, from which are extracted the key atom fields.
 */
public class AtomEntry {

  @SuppressWarnings("unused")
  private String id;
  @SuppressWarnings("unused")
  private String title;
  @SuppressWarnings("unused")
  private String summary;
  @SuppressWarnings("unused")
  private String icon;
  @SuppressWarnings("unused")
  private AtomSource source;
  @SuppressWarnings("unused")
  private AtomGenerator generator;
  @SuppressWarnings("unused")
  private AtomAuthor author;
  @SuppressWarnings("unused")
  private Date updated;
  @SuppressWarnings("unused")
  private AtomLink link;
  @SuppressWarnings("unused")
  private Object content;

  /**
   * @param o
   */
  public AtomEntry(Object o) {
    if (o instanceof Person) {
      Person person = (Person) o;
      content = new AtomContent(person);
      id = "urn:guid:" + person.getId();
      updated = person.getUpdated();
    } else if (o instanceof Activity) {
      Activity activity = (Activity) o;
      content = new AtomContent(activity);
      title = activity.getTitle();
      summary = activity.getBody();
      link = new AtomLink("self", activity.getUrl());
      icon = activity.getStreamFaviconUrl();
      source = new AtomSource(activity);
      generator = new AtomGenerator(activity);
      author = new AtomAuthor(activity);
      updated = activity.getUpdated();
    } else if ( o instanceof Entry ) {
      Entry<?, ?> e = (Entry<?, ?>) o;
      id = (String) e.getKey();
      content = new AtomContent(e.getValue());
    } else {
      content = o;
    }
  }

}
