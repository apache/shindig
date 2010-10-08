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

/**
 * <p>ActivityObjectImpl class.</p>
 *
 */
public class ActivityObjectImpl implements ActivityObject {
  
  private String id;
  private String name;
  private String summary;
  private MediaLink media;
  private String permalink;
  private List<String> type;
  private ActivityObject inReplyTo;
  private List<ActivityObject> attached;
  private List<ActivityObject> reply;
  private List<ActivityObject> reaction;
  private ActionLink action;
  private List<String> upstreamDuplicateId;
  private List<String> downstreamDuplicateId;
  private String standardLink;
  
  /**
   * A simple implementation of an ActivtyObject
   */
  public ActivityObjectImpl() {
    this.id = null;
    this.name = null;
    this.summary = null;
    this.media = null;
    this.permalink = null;
    this.type = null;
    this.inReplyTo = null;
    this.attached = null;
    this.reply = null;
    this.reaction = null;
    this.action = null;
    this.upstreamDuplicateId = null;
    this.downstreamDuplicateId = null;
    this.standardLink = null;
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
  public String getName() {
    return name;
  }

  /** {@inheritDoc} */
  public void setName(String name) {
    this.name = name;
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
  public String getPermalink() {
    return permalink;
  }

  /** {@inheritDoc} */
  public void setPermalink(String permalink) {
    this.permalink = permalink;
  }

  /** {@inheritDoc} */
  public List<String> getType() {
    return type;
  }

  /** {@inheritDoc} */
  public void setType(List<String> type) {
    this.type = type;
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
  public List<ActivityObject> getAttached() {
    return attached;
  }

  /** {@inheritDoc} */
  public void setAttached(List<ActivityObject> attached) {
    this.attached = attached;
  }

  /** {@inheritDoc} */
  public List<ActivityObject> getReply() {
    return reply;
  }

  /** {@inheritDoc} */
  public void setReply(List<ActivityObject> reply) {
    this.reply = reply;
  }

  /** {@inheritDoc} */
  public List<ActivityObject> getReaction() {
    return reaction;
  }

  /** {@inheritDoc} */
  public void setReaction(List<ActivityObject> reaction) {
    this.reaction = reaction;
  }

  /** {@inheritDoc} */
  public ActionLink getAction() {
    return action;
  }

  /** {@inheritDoc} */
  public void setAction(ActionLink action) {
    this.action = action;
  }

  /** {@inheritDoc} */
  public List<String> getUpstreamDuplicateId() {
    return upstreamDuplicateId;
  }

  /** {@inheritDoc} */
  public void setUpstreamDuplicateId(List<String> upstreamDuplicateId) {
    this.upstreamDuplicateId = upstreamDuplicateId;
  }

  /** {@inheritDoc} */
  public List<String> getDownstreamDuplicateId() {
    return downstreamDuplicateId;
  }

  /** {@inheritDoc} */
  public void setDownstreamDuplicateId(List<String> downstreamDuplicateId) {
    this.downstreamDuplicateId = downstreamDuplicateId;
  }

  /** {@inheritDoc} */
  public String getStandardLink() {
    return standardLink;
  }

  /** {@inheritDoc} */
  public void setStandardLink(String standardLink) {
    this.standardLink = standardLink;
  }
}
