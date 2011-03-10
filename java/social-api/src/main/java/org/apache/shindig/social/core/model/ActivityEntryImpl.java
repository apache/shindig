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

package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.ActivityObject;
import org.apache.shindig.social.opensocial.model.MediaLink;

/**
 * A simple bean implementation of an ActivityStream Entry.
 */
public class ActivityEntryImpl implements ActivityEntry {
  
  private ActivityObject actor;
  private String body;
  private ActivityObject generator;
  private MediaLink icon;
  private ActivityObject object;
  private String postedTime;
  private ActivityObject provider;
  private ActivityObject target;
  private String title;
  private String updatedTime;
  private String verb;

  /**
   * Create a new empty ActivityEntry
   */
  public ActivityEntryImpl() {
  }

  /** {@inheritDoc} */
  public ActivityObject getActor() {
    return this.actor;
  }

  /** {@inheritDoc} */
  public void setActor(ActivityObject actor) {
    this.actor = actor;
  }

  /** {@inheritDoc} */
  public String getBody() {
    return this.body;
  }

  /** {@inheritDoc} */
  public void setBody(String body) {
    this.body = body;
  }

  /** {@inheritDoc} */
  public ActivityObject getGenerator() {
    return this.generator;
  }

  /** {@inheritDoc} */
  public void setGenerator(ActivityObject generator) {
    this.generator = generator;
  }

  /** {@inheritDoc} */
  public MediaLink getIcon() {
    return this.icon;
  }

  /** {@inheritDoc} */
  public void setIcon(MediaLink icon) {
    this.icon = icon;
  }

  /** {@inheritDoc} */
  public ActivityObject getObject() {
    return this.object;
  }

  /** {@inheritDoc} */
  public void setObject(ActivityObject object) {
    this.object = object; 
  }

  /** {@inheritDoc} */
  public String getPostedTime() {
    return this.postedTime;
  }

  /** {@inheritDoc} */
  public void setPostedTime(String postedTime) {
    this.postedTime = postedTime;
  }

  /** {@inheritDoc} */
  public ActivityObject getProvider() {
    return this.provider;
  }

  /** {@inheritDoc} */
  public void setProvider(ActivityObject provider) {
    this.provider = provider;
  }

  /** {@inheritDoc} */
  public ActivityObject getTarget() {
    return this.target;
  }

  /** {@inheritDoc} */
  public void setTarget(ActivityObject target) {
    this.target = target;
  }

  /** {@inheritDoc} */
  public String getTitle() {
    return this.title;
  }

  /** {@inheritDoc} */
  public void setTitle(String title) {
    this.title = title;
  }

  /** {@inheritDoc} */
  public String getUpdatedTime() {
    return this.updatedTime;
  }

  /** {@inheritDoc} */
  public void setUpdatedTime(String updatedTime) {
    this.updatedTime = updatedTime;
  }

  /** {@inheritDoc} */
  public String getVerb() {
    return this.verb;
  }

  /** {@inheritDoc} */
  public void setVerb(String verb) {
    this.verb = verb;
  }
}
