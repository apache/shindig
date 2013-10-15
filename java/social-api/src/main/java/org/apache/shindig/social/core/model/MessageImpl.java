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
import java.util.Date;

import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.Url;

/**
 * Default implementation for a {@link org.apache.shindig.social.opensocial.model.Message}
 */
public class MessageImpl implements Message {

  private String appUrl;
  private String body;
  private String bodyId;
  private List<String> collectionIds;
  private String id;
  private String inReplyTo;
  private List<String> recipients;
  private List<String> replies;
  private String senderId;
  private Status status;
  private Date timeSent;
  private String title;
  private String titleId;
  private Type type;
  private Date updated;
  private List<Url> urls;


  public MessageImpl() {
  }

  public MessageImpl(String initBody, String initTitle, Type initType) {
    this.body = initBody;
    this.title = initTitle;
    this.type = initType;
  }

  public String getAppUrl() {
    return appUrl;
  }

  public void setAppUrl(String appUrl) {
    this.appUrl = appUrl;
  }

  public String getBody() {
    return this.body;
  }

  public void setBody(String newBody) {
    this.body = newBody;
  }

  public String getBodyId() {
    return bodyId;
  }

  public void setBodyId(String bodyId) {
    this.bodyId = bodyId;
  }

  public List<String> getCollectionIds() {
    return collectionIds;
  }

  public void setCollectionIds(List<String> collectionIds) {
    this.collectionIds = collectionIds;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getInReplyTo() {
    return inReplyTo;
  }

  public void setInReplyTo(String parentId) {
    this.inReplyTo = parentId;
  }

  public List<String> getRecipients() {
    return this.recipients;
  }

  public void setRecipients(List<String> recipients) {
    this.recipients = recipients;
  }

  public List<String> getReplies() {
    return replies;
  }

  public void setReplies(List<String> replies) {
    this.replies = replies;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Date getTimeSent() {
    return timeSent;
  }

  public void setTimeSent(Date timeSent) {
    this.timeSent = timeSent;
  }

  public String getTitle() {
    return this.title;
  }

  public void setTitle(String newTitle) {
    this.title = newTitle;
  }

  public String getTitleId() {
    return titleId;
  }

  public void setTitleId(String titleId) {
    this.titleId = titleId;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type newType) {
    this.type = newType;
  }

  public Date getUpdated() {
    return this.updated;
  }

  public void setUpdated(Date updated) {
    this.updated = updated;
  }

  public List<Url> getUrls() {
    return this.urls;
  }

  public void setUrls(List<Url> urls) {
    this.urls = urls;
  }

  public String sanitizeHTML(String htmlStr) {
    return htmlStr;
  }

}
