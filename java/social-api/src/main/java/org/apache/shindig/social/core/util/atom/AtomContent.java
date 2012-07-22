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
package org.apache.shindig.social.core.util.atom;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.Person;

import com.google.common.collect.Lists;

/**
 * Represents and atom:content element.
 */
public class AtomContent {

  @SuppressWarnings("unused")
  private Person person;
  @SuppressWarnings("unused")
  private Activity activity;
  @SuppressWarnings("unused")
  private ActivityEntry activityEntry;
  @SuppressWarnings("unused")
  private AtomAttribute type = new AtomAttribute("application/xml");
  @SuppressWarnings("unused")
  private Object entry;
  @SuppressWarnings("unused")
  private Object value;

  /**
   * @param person
   */
  public AtomContent(Person person) {
    this.person = person;
  }

  /**
   * @param activity
   */
  public AtomContent(Activity activity) {
    this.activity = activity;
  }

  /**
   * @param activityEntry
   */
  public AtomContent(ActivityEntry activityEntry) {
    this.activityEntry = activityEntry;
  }

  /**
   * @param value
   */
  public AtomContent(Object value) {
    if (value instanceof Map<?, ?>) {
      Map<?, ?> entries = (Map<?, ?>) value;
      List<AtomKeyValue> keyValues = Lists.newArrayList();
      for ( Entry<?, ?> e : entries.entrySet() ) {
        keyValues.add(new AtomKeyValue(e));
      }
      entry = keyValues;
    } else {
      this.value = value;
    }
  }

}
