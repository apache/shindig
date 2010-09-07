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

package org.apache.shindig.extras.as.core.model;

import java.util.List;

import org.apache.shindig.extras.as.opensocial.model.ActivityEntry;
import org.apache.shindig.extras.as.opensocial.model.ActivityObject;

/**
 * A simple bean implementation of an ActivityStream Entry.
 *
 */
public class ActivityEntryImpl implements ActivityEntry {

  private String icon;
  private String time;
  private ActivityObject actor;
  private List<String> verb;
  private ActivityObject object;
  private ActivityObject target;
  private ActivityObject generator;
  private ActivityObject serviceProvider;
  private String title;
  private String body;
  private List<String> standardLink;
  
  /**
   * Create a new empty ActivityEntry
   */
  public ActivityEntryImpl() {
    this.icon = null;
    this.time = null;
    this.actor = null;
    this.verb = null;
    this.object = null;
    this.target = null;
    this.generator = null;
    this.serviceProvider = null;
    this.title = null;
    this.body = null;
    this.standardLink = null;
  }

  /** {@inheritDoc} */
  public String getIcon() {
    return icon;
  }

  /** {@inheritDoc} */
  public void setIcon(String icon) {
    this.icon = icon;
  }

  /** {@inheritDoc} */
  public String getTime() {
    return time;
  }

  /** {@inheritDoc} */
  public void setTime(String time) {
    this.time = time;
  }

  /** {@inheritDoc} */
  public ActivityObject getActor() {
    return actor;
  }

  /** {@inheritDoc} */
  public void setActor(ActivityObject actor) {
    this.actor = actor;
  }

  /** {@inheritDoc} */
  public List<String> getVerb() {
    return verb;
  }

  /** {@inheritDoc} */
  public void setVerb(List<String> verb) {
    this.verb = verb;
  }

  /** {@inheritDoc} */
  public ActivityObject getObject() {
    return object;
  }

  /** {@inheritDoc} */
  public void setObject(ActivityObject object) {
    this.object = object;
  }

  /** {@inheritDoc} */
  public ActivityObject getTarget() {
    return target;
  }

  /** {@inheritDoc} */
  public void setTarget(ActivityObject target) {
    this.target = target;
  }

  /** {@inheritDoc} */
  public ActivityObject getGenerator() {
    return generator;
  }

  /** {@inheritDoc} */
  public void setGenerator(ActivityObject generator) {
    this.generator = generator;
  }

  /** {@inheritDoc} */
  public ActivityObject getServiceProvider() {
    return serviceProvider;
  }

  /** {@inheritDoc} */
  public void setServiceProvider(ActivityObject serviceProvider) {
    this.serviceProvider = serviceProvider;
  }

  /** {@inheritDoc} */
  public String getTitle() {
    return title;
  }

  /** {@inheritDoc} */
  public void setTitle(String title) {
    this.title = title;
  }

  /** {@inheritDoc} */
  public String getBody() {
    return body;
  }

  /** {@inheritDoc} */
  public void setBody(String body) {
    this.body = body;
  }

  /** {@inheritDoc} */
  public List<String> getStandardLink() {
    return standardLink;
  }

  /** {@inheritDoc} */
  public void setStandardLink(List<String> standardLink) {
    this.standardLink = standardLink;
  }
}
