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
import java.util.Map;

import org.apache.shindig.extras.as.opensocial.model.ActivityEntry;
import org.apache.shindig.extras.as.opensocial.model.ActivityObject;
import org.apache.shindig.extras.as.opensocial.model.MediaLink;
import org.apache.shindig.extras.as.opensocial.model.StandardLink;

/**
 * A simple bean implementation of an ActivityStream Entry.
 * 
 */
public class ActivityEntryImpl implements ActivityEntry {

  private MediaLink icon;
  private String postedTime;
  private ActivityObject actor;
  private String verb;
  private ActivityObject object;
  private ActivityObject target;
  private ActivityObject generator;
  private ActivityObject provider;
  private String title;
  private String body;
  private Map<String, List<StandardLink>> standardLinks;
  private List<String> to;
  private List<String> cc;
  private List<String> bcc;

  /**
   * Create a new empty ActivityEntry
   */
  public ActivityEntryImpl() {
  }

  /** {@inheritDoc} */
  public MediaLink getIcon() {
    return icon;
  }

  /** {@inheritDoc} */
  public void setIcon(MediaLink icon) {
    this.icon = icon;
  }

  /** {@inheritDoc} */
  public String getPostedTime() {
    return postedTime;
  }

  /** {@inheritDoc} */
  public void setPostedTime(String postedTime) {
    this.postedTime = postedTime;
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
  public String getVerb() {
    return verb;
  }

  /** {@inheritDoc} */
  public void setVerb(String verb) {
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
  public ActivityObject getProvider() {
    return provider;
  }

  /** {@inheritDoc} */
  public void setProvider(ActivityObject provider) {
    this.provider = provider;
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
  public Map<String, List<StandardLink>> getStandardLinks() {
    return standardLinks;
  }

  /** {@inheritDoc} */
  public void setStandardLinks(Map<String, List<StandardLink>> standardLinks) {
    this.standardLinks = standardLinks;
  }
  
  /** {@inheritDoc} */
  public List<String> getTo() {
    return to;
  }
  
  /** {@inheritDoc} */
  public void setTo(List<String> to) {
    this.to = to;
  }
  
  /** {@inheritDoc} */
  public List<String> getCC() {
    return cc;
  }
  
  /** {@inheritDoc} */
  public void setCC(List<String> cc) {
    this.cc = cc;
  }

  /** {@inheritDoc} */
  public List<String> getBCC() {
    return bcc;
  }
  
  /** {@inheritDoc} */
  public void setBCC(List<String> bcc) {
    this.bcc = bcc;
  }
}
