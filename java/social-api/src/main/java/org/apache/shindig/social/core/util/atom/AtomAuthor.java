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

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityEntry;

/**
 * Represents and atom:entry/atom:author element.
 */
public class AtomAuthor {

  @SuppressWarnings("unused")
  private String uri;
  @SuppressWarnings("unused")
  private String name;

  /**
   * Default constructor for POSTs to the REST API.
   */
  public AtomAuthor() {
  }

  /**
   * @param activity
   */
  public AtomAuthor(Activity activity) {
    uri = activity.getUserId();
  }

  /**
   * @param activityEntry
   */
  public AtomAuthor(ActivityEntry activityEntry) {
    uri = activityEntry.getActor().getUrl();
    name = activityEntry.getActor().getDisplayName();
    if (name == null) {
      name = activityEntry.getActor().getId();
    }
  }
}
