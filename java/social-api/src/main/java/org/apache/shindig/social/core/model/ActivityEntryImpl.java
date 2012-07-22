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
package org.apache.shindig.social.core.model;

import org.apache.shindig.protocol.model.ExtendableBean;
import org.apache.shindig.protocol.model.ExtendableBeanImpl;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.ActivityObject;
import org.apache.shindig.social.opensocial.model.MediaLink;

/**
 * A simple bean implementation of an ActivityStream Entry.
 */
public class ActivityEntryImpl extends ExtendableBeanImpl implements ActivityEntry {

  private static final long serialVersionUID = 1L;
  private ActivityObject actor;
  private String content;
  private ActivityObject generator;
  private MediaLink icon;
  private String id;
  private ActivityObject object;
  private String published;
  private ActivityObject provider;
  private ActivityObject target;
  private String title;
  private String updated;
  private String url;
  private String verb;
  private ExtendableBean openSocial;
  private ExtendableBean extensions;

  /**
   * Create a new empty ActivityEntry
   */
  public ActivityEntryImpl() { }

  public ActivityObject getActor() {
    return actor;
  }

  /** {@inheritDoc} */
  public void setActor(ActivityObject actor) {
    this.actor = actor;
    put("actor", actor);
  }

  /** {@inheritDoc} */
  public String getContent() {
    return content;
  }

  /** {@inheritDoc} */
  public void setContent(String content) {
    this.content = content;
    put("content", content);
  }

  /** {@inheritDoc} */
  public ActivityObject getGenerator() {
    return generator;
  }

  /** {@inheritDoc} */
  public void setGenerator(ActivityObject generator) {
    this.generator = generator;
    put("generator", generator);
  }

  /** {@inheritDoc} */
  public MediaLink getIcon() {
    return icon;
  }

  /** {@inheritDoc} */
  public void setIcon(MediaLink icon) {
    this.icon = icon;
    put("icon", icon);
  }

  /** {@inheritDoc} */
  public String getId() {
    return id;
  }

  /** {@inheritDoc} */
  public void setId(String id) {
    this.id = id;
    put("id", id);
  }

  /** {@inheritDoc} */
  public ActivityObject getObject() {
    return object;
  }

  /** {@inheritDoc} */
  public void setObject(ActivityObject object) {
    this.object = object;
    put("object", object);
  }

  /** {@inheritDoc} */
  public String getPublished() {
    return published;
  }

  /** {@inheritDoc} */
  public void setPublished(String published) {
    this.published = published;
    put("published", published);
  }

  /** {@inheritDoc} */
  public ActivityObject getProvider() {
    return provider;
  }

  /** {@inheritDoc} */
  public void setProvider(ActivityObject provider) {
    this.provider = provider;
    put("provider", provider);
  }

  /** {@inheritDoc} */
  public ActivityObject getTarget() {
    return target;
  }

  /** {@inheritDoc} */
  public void setTarget(ActivityObject target) {
    this.target = target;
    put("target", target);
  }

  /** {@inheritDoc} */
  public String getTitle() {
    return title;
  }

  /** {@inheritDoc} */
  public void setTitle(String title) {
    this.title = title;
    put("title", title);
  }

  /** {@inheritDoc} */
  public String getUpdated() {
    return updated;
  }

  /** {@inheritDoc} */
  public void setUpdated(String updated) {
    this.updated = updated;
    put("updated", updated);
  }

  /** {@inheritDoc} */
  public String getUrl() {
    return url;
  }

  /** {@inheritDoc} */
  public void setUrl(String url) {
    this.url = url;
    put("url", url);
  }

  /** {@inheritDoc} */
  public String getVerb() {
    return verb;
  }

  /** {@inheritDoc} */
  public void setVerb(String verb) {
    this.verb = verb;
    put("verb", verb);
  }

  /** {@inheritDoc} */
  public ExtendableBean getOpenSocial() {
    return openSocial;
  }

  /** {@inheritDoc} */
  public void setOpenSocial(ExtendableBean openSocial) {
    this.openSocial = openSocial;
    put("openSocial", openSocial);
  }

  /** {@inheritDoc} */
  public ExtendableBean getExtensions() {
    return extensions;
  }

  /** {@inheritDoc} */
  public void setExtensions(ExtendableBean extensions) {
    this.extensions = extensions;
    put("extensions", extensions);
  }

  /**
   * Sorts ActivityEntries in ascending order based on publish date.
   *
   * @param that is the ActivityEntry to compare to this ActivityEntry
   *
   * @return int represents how the ActivityEntries compare
   */
  public int compareTo(ActivityEntry that) {
    if (this.getPublished() == null && that.getPublished() == null) {
      return 0;   // both are null, equal
    } else if (this.getPublished() == null) {
      return -1;  // this is null, comes before real date
    } else if (that.getPublished() == null) {
      return 1;   // that is null, this comes after
    } else {      // compare publish dates in lexicographical order
      return this.getPublished().compareTo(that.getPublished());
    }
  }
}
