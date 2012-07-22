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

import java.util.List;

import org.apache.shindig.protocol.model.ExtendableBean;
import org.apache.shindig.protocol.model.ExtendableBeanImpl;
import org.apache.shindig.social.opensocial.model.ActivityObject;
import org.apache.shindig.social.opensocial.model.MediaLink;

/**
 * <p>ActivityObjectImpl class.</p>
 */
public class ActivityObjectImpl extends ExtendableBeanImpl implements ActivityObject {

  private static final long serialVersionUID = 1L;
  private List<ActivityObject> attachments;
  private ActivityObject author;
  private String content;
  private String displayName;
  private List<String> downstreamDuplicates;
  private String id;
  private MediaLink image;
  private String objectType;
  private String published;
  private String summary;
  private String updated;
  private List<String> upstreamDuplicates;
  private String url;
  private ExtendableBean openSocial;

  /**
   * Constructs an empty ActivityObject.
   */
  public ActivityObjectImpl() { }

  /** {@inheritDoc} */
  public List<ActivityObject> getAttachments() {
    return attachments;
  }

  /** {@inheritDoc} */
  public void setAttachments(List<ActivityObject> attachments) {
    this.attachments = attachments;
    put("attachments", attachments);
  }

  /** {@inheritDoc} */
  public ActivityObject getAuthor() {
    return author;
  }

  /** {@inheritDoc} */
  public void setAuthor(ActivityObject author) {
    this.author = author;
    put("author", author);
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
  public String getDisplayName() {
    return displayName;
  }

  /** {@inheritDoc} */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
    put("displayName", displayName);
  }

  /** {@inheritDoc} */
  public List<String> getDownstreamDuplicates() {
    return downstreamDuplicates;
  }

  /** {@inheritDoc} */
  public void setDownstreamDuplicates(List<String> downstreamDuplicates) {
    this.downstreamDuplicates = downstreamDuplicates;
    put("downstreamDuplicates", downstreamDuplicates);
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
  public MediaLink getImage() {
    return image;
  }

  /** {@inheritDoc} */
  public void setImage(MediaLink image) {
    this.image = image;
    put("image", image);
  }

  /** {@inheritDoc} */
  public String getObjectType() {
    return objectType;
  }

  /** {@inheritDoc} */
  public void setObjectType(String objectType) {
    this.objectType = objectType;
    put("objectType", objectType);
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
  public String getSummary() {
    return summary;
  }

  /** {@inheritDoc} */
  public void setSummary(String summary) {
    this.summary = summary;
    put("summary", summary);
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
  public List<String> getUpstreamDuplicates() {
    return upstreamDuplicates;
  }

  /** {@inheritDoc} */
  public void setUpstreamDuplicates(List<String> upstreamDuplicates) {
    this.upstreamDuplicates = upstreamDuplicates;
    put("upstreamDuplicates", upstreamDuplicates);
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
  public ExtendableBean getOpenSocial() {
    return openSocial;
  }

  /** {@inheritDoc} */
  public void setOpenSocial(ExtendableBean openSocial) {
    this.openSocial = openSocial;
    put("openSocial", openSocial);
  }
}
