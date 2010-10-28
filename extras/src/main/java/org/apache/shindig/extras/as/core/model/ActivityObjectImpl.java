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

import org.apache.shindig.extras.as.opensocial.model.ActionLink;
import org.apache.shindig.extras.as.opensocial.model.ActivityObject;
import org.apache.shindig.extras.as.opensocial.model.MediaLink;
import org.apache.shindig.extras.as.opensocial.model.StandardLink;

/**
 * <p>ActivityObjectImpl class.</p>
 *
 */
public class ActivityObjectImpl implements ActivityObject {
  
  private String id;
  private String displayName;
  private String summary;
  private MediaLink media;
  private String link;
  private String objectType;
  private ActivityObject inReplyTo;
  private List<ActivityObject> attachedObjects;
  private List<ActivityObject> replies;
  private List<ActivityObject> reactions;
  private List<ActionLink> actionLinks;
  private List<String> upstreamDuplicates;
  private List<String> downstreamDuplicates;
  private List<StandardLink> standardLinks;
  
  /**
   * A simple implementation of an ActivtyObject
   */
  public ActivityObjectImpl() {
  }

  /** {@inheritDoc} */
  public String getId() {
    return id;
  }

  /** {@inheritDoc} */
  public void setId(String id) {
    this.id = id;
  }

  /** {@inheritDoc} */
  public String getDisplayName() {
    return displayName;
  }

  /** {@inheritDoc} */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /** {@inheritDoc} */
  public String getSummary() {
    return summary;
  }

  /** {@inheritDoc} */
  public void setSummary(String summary) {
    this.summary = summary;
  }

  /** {@inheritDoc} */
  public MediaLink getMedia() {
    return media;
  }

  /** {@inheritDoc} */
  public void setMedia(MediaLink media) {
    this.media = media;
  }

  /** {@inheritDoc} */
  public String getLink() {
    return link;
  }

  /** {@inheritDoc} */
  public void setLink(String link) {
    this.link = link;
  }

  /** {@inheritDoc} */
  public String getObjectType() {
    return objectType;
  }

  /** {@inheritDoc} */
  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  /** {@inheritDoc} */
  public ActivityObject getInReplyTo() {
    return inReplyTo;
  }

  /** {@inheritDoc} */
  public void setInReplyTo(ActivityObject inReplyTo) {
    this.inReplyTo = inReplyTo;
  }

  /** {@inheritDoc} */
  public List<ActivityObject> getAttachedObjects() {
    return attachedObjects;
  }

  /** {@inheritDoc} */
  public void setAttachedObjects(List<ActivityObject> attachedObjects) {
    this.attachedObjects = attachedObjects;
  }

  /** {@inheritDoc} */
  public List<ActivityObject> getReplies() {
    return replies;
  }

  /** {@inheritDoc} */
  public void setReplies(List<ActivityObject> replies) {
    this.replies = replies;
  }

  /** {@inheritDoc} */
  public List<ActivityObject> getReactions() {
    return reactions;
  }

  /** {@inheritDoc} */
  public void setReactions(List<ActivityObject> reactions) {
    this.reactions = reactions;
  }

  /** {@inheritDoc} */
  public List<ActionLink> getActionLinks() {
    return actionLinks;
  }

  /** {@inheritDoc} */
  public void setActionLinks(List<ActionLink> actionLinks) {
    this.actionLinks = actionLinks;
  }

  /** {@inheritDoc} */
  public List<String> getUpstreamDuplicates() {
    return upstreamDuplicates;
  }

  /** {@inheritDoc} */
  public void setUpstreamDuplicates(List<String> upstreamDuplicates) {
    this.upstreamDuplicates = upstreamDuplicates;
  }

  /** {@inheritDoc} */
  public List<String> getDownstreamDuplicates() {
    return downstreamDuplicates;
  }

  /** {@inheritDoc} */
  public void setDownstreamDuplicates(List<String> downstreamDuplicates) {
    this.downstreamDuplicates = downstreamDuplicates;
  }

  /** {@inheritDoc} */
  public List<StandardLink> getStandardLinks() {
    return standardLinks;
  }

  /** {@inheritDoc} */
  public void setStandardLinks(List<StandardLink> standardLinks) {
    this.standardLinks = standardLinks;
  }
}
