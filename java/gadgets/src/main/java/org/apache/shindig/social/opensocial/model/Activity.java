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
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.social.Mandatory;
import org.apache.shindig.social.AbstractGadgetData;

public class Activity extends AbstractGadgetData {
  private String appId;
  private String body;
  private String bodyId;
  private String externalId;
  @Mandatory private String id;
  private MediaItem[] mediaItems;
  private Long postedTime;
  private Float priority;
  private String streamFaviconUrl;
  private String streamSourceUrl;
  private String streamTitle;
  private String streamUrl;
  // private Map<String, String> templateParams
  private String title;
  private String titleId;
  private String url;
  @Mandatory private String userId;

  public Activity(String id, String appId, String userId) {
    this.id = id;
    this.appId = appId;
    this.userId = userId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getBodyId() {
    return bodyId;
  }

  public void setBodyId(String bodyId) {
    this.bodyId = bodyId;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public MediaItem[] getMediaItems() {
    return mediaItems;
  }

  public void setMediaItems(MediaItem[] mediaItems) {
    this.mediaItems = mediaItems;
  }

  public Long getPostedTime() {
    return postedTime;
  }

  public void setPostedTime(Long postedTime) {
    this.postedTime = postedTime;
  }

  public Float getPriority() {
    return priority;
  }

  public void setPriority(Float priority) {
    this.priority = priority;
  }

  public String getStreamFaviconUrl() {
    return streamFaviconUrl;
  }

  public void setStreamFaviconUrl(String streamFaviconUrl) {
    this.streamFaviconUrl = streamFaviconUrl;
  }

  public String getStreamSourceUrl() {
    return streamSourceUrl;
  }

  public void setStreamSourceUrl(String streamSourceUrl) {
    this.streamSourceUrl = streamSourceUrl;
  }

  public String getStreamTitle() {
    return streamTitle;
  }

  public void setStreamTitle(String streamTitle) {
    this.streamTitle = streamTitle;
  }

  public String getStreamUrl() {
    return streamUrl;
  }

  public void setStreamUrl(String streamUrl) {
    this.streamUrl = streamUrl;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitleId() {
    return titleId;
  }

  public void setTitleId(String titleId) {
    this.titleId = titleId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
}
