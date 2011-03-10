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

import java.util.List;

import org.apache.shindig.social.opensocial.model.ActivityObject;
import org.apache.shindig.social.opensocial.model.MediaLink;

/**
 * <p>ActivityObjectImpl class.</p>
 */
public class ActivityObjectImpl implements ActivityObject {
  
  private List<ActivityObject> attachedObjects;
  private String displayName;
  private List<String> downstreamDuplicates;
  private String embedCode;
  private String id;
  private MediaLink image;
  private String objectType;
  private String summary;
  private List<String> upstreamDuplicates;
  private String url;
  
  /**
   * A simple implementation of an ActivtyObject
   */
  public ActivityObjectImpl() {
  }

  /** {@inheritDoc} */
  public List<ActivityObject> getAttachedObjects() {
    return this.attachedObjects;
  }

  /** {@inheritDoc} */
  public void setAttachedObjects(List<ActivityObject> attachedObjects) {
    this.attachedObjects = attachedObjects;
  }

  /** {@inheritDoc} */
  public String getDisplayName() {
    return this.displayName;
  }

  /** {@inheritDoc} */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /** {@inheritDoc} */
  public List<String> getDownstreamDuplicates() {
    return this.downstreamDuplicates;
  }

  /** {@inheritDoc} */
  public void setDownstreamDuplicates(List<String> downstreamDuplicates) {
    this.downstreamDuplicates = downstreamDuplicates;
  }

  /** {@inheritDoc} */
  public String getEmbedCode() {
    return this.embedCode;
  }

  /** {@inheritDoc} */
  public void setEmbedCode(String embedCode) {
    this.embedCode = embedCode;
  }

  /** {@inheritDoc} */
  public String getId() {
    return this.id;
  }

  /** {@inheritDoc} */
  public void setId(String id) {
    this.id = id;
  }

  /** {@inheritDoc} */
  public MediaLink getImage() {
    return this.image;
  }

  /** {@inheritDoc} */
  public void setImage(MediaLink image) {
    this.image = image;
  }

  /** {@inheritDoc} */
  public String getObjectType() {
    return this.objectType;
  }

  /** {@inheritDoc} */
  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  /** {@inheritDoc} */
  public List<String> getUpstreamDuplicates() {
    return this.upstreamDuplicates;
  }

  /** {@inheritDoc} */
  public void setUpstreamDuplicates(List<String> upstreamDuplicates) {
    this.upstreamDuplicates = upstreamDuplicates;
  }

  /** {@inheritDoc} */
  public String getSummary() {
    return this.summary;
  }

  /** {@inheritDoc} */
  public void setSummary(String summary) {
    this.summary = summary;
  }

  /** {@inheritDoc} */
  public String getUrl() {
    return this.url;
  }

  /** {@inheritDoc} */
  public void setUrl(String url) {
    this.url = url;
  }
}