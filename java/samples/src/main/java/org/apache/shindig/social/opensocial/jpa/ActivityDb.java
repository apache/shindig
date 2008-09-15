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
package org.apache.shindig.social.opensocial.jpa;

import static javax.persistence.GenerationType.IDENTITY;

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.MediaItem;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import static javax.persistence.CascadeType.ALL;

@Entity
@Table(name = "activity")
public class ActivityDb implements Activity, DbObject {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "oid")
  protected long objectId;

  @Version
  @Column(name = "version")
  protected long version;

  @Basic
  @Column(name = "app_id", length = 255)
  protected String appId;

  @Basic
  @Column(name = "body", length = 255)
  protected String body;

  @Basic
  @Column(name = "body_id", length = 255)
  protected String bodyId;

  @Basic
  @Column(name = "external_id", length = 255)
  protected String externalId;

  @Basic
  @Column(name = "activity_id", length = 255)
  protected String id;

  @Basic
  @Column(name = "updated")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date updated;

  /*
   * Do a many to many join using table activity_media
   */
  @ManyToMany(targetEntity = MediaItemDb.class)
  @JoinTable(name = "activity_media", joinColumns = @JoinColumn(name = "activity_id", referencedColumnName = "oid"), inverseJoinColumns = @JoinColumn(name = "media_id", referencedColumnName = "oid"))
  protected List<MediaItem> mediaItems;

  @Basic
  @Column(name = "posted_time")
  protected Long postedTime;

  @Basic
  @Column(name = "priority")
  protected Float priority;

  @Basic
  @Column(name = "stream_favicon_url", length = 255)
  protected String streamFaviconUrl;

  @Basic
  @Column(name = "stream_source_url", length = 255)
  protected String streamSourceUrl;

  @Basic
  @Column(name = "stream_title", length = 255)
  protected String streamTitle;

  @Basic
  @Column(name = "stream_url", length = 255)
  protected String streamUrl;

  /*
   * Create map using ActivityTemplateParamsDb such that ActivityTemplateParams are joined on oid ->
   * activity_id and then the name value becomes the key, and the value becomes the value
   * unfortunately JPA wont do Map<String,String> so this is handled in the getter and setter.
   */
  @OneToMany(targetEntity = ActivityTemplateParamsDb.class, mappedBy = "activities", cascade = ALL)
  @MapKey(name = "name")
  protected Map<String, ActivityTemplateParamsDb> templateParamsDb;

  @Transient
  protected Map<String, String> templateParams;

  @Basic
  @Column(name = "title", length = 255)
  protected String title;

  @Basic
  @Column(name = "title_id", length = 255)
  protected String titleId;

  @Basic
  @Column(name = "url", length = 255)
  protected String url;

  @Basic
  @Column(name = "user_id", length = 255)
  protected String userId;

  public ActivityDb() {
  }

  public ActivityDb(String id, String userId) {
    this.id = id;
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

  public Date getUpdated() {
    if (updated == null) {
      return null;
    }
    return new Date(updated.getTime());
  }

  public void setUpdated(Date updated) {
    if (updated == null) {
      this.updated = null;
    } else {
      this.updated = new Date(updated.getTime());
    }
  }

  public List<MediaItem> getMediaItems() {
    return mediaItems;
  }

  public void setMediaItems(List<MediaItem> mediaItems) {
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

  public Map<String, String> getTemplateParams() {
    return templateParams;
  }

  public void setTemplateParams(Map<String, String> templateParams) {
    this.templateParams = templateParams;
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

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(long objectId) {
    this.objectId = objectId;
  }

  @PrePersist
  public void populateDbFields() {
    // add new entries
    for (Entry<String, String> e : templateParams.entrySet()) {
      ActivityTemplateParamsDb a = templateParamsDb.get(e.getKey());
      if (a == null) {
        a = new ActivityTemplateParamsDb();
        a.name = e.getKey();
        a.value = e.getValue();
        a.activities = new ArrayList<Activity>();
        a.activities.add(this);
        templateParamsDb.put(e.getKey(), a);
      } else {
        a.value = e.getValue();
      }
    }
    // remove old entries
    List<String> toRemove = new ArrayList<String>();
    for (Entry<String, ActivityTemplateParamsDb> e : templateParamsDb.entrySet()) {
      if (!templateParams.containsKey(e.getKey())) {
        toRemove.add(e.getKey());
      }
    }
    for (String r : toRemove) {
      templateParamsDb.remove(r);
    }
  }

  @PostLoad
  public void loadTransientFields() {
    templateParams = new ConcurrentHashMap<String, String>();
    for (Entry<String, ActivityTemplateParamsDb> e : templateParamsDb.entrySet()) {
      templateParams.put(e.getKey(), e.getValue().value);
    }
  }

}
