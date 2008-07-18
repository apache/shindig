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

import com.google.inject.ImplementedBy;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ImplementedBy(ActivityImpl.class)

/**
 * see
 * http://code.google.com/apis/opensocial/docs/0.8/reference/#opensocial.Activity.Field
 * for all field meanings. All fields are represented in the js api at this time except for
 * lastUpdated. This field is currently only in the RESTful spec.
 *
 */
public interface Activity {

  public static enum Field {
    APP_ID("appId"),
    BODY("body"),
    BODY_ID("bodyId"),
    EXTERNAL_ID("externalId"),
    ID("id"),
    LAST_UPDATED("updated"), /** Needed to support the RESTful api **/
    MEDIA_ITEMS("mediaItems"),
    POSTED_TIME("postedTime"),
    PRIORITY("priority"),
    STREAM_FAVICON_URL("streamFaviconUrl"),
    STREAM_SOURCE_URL("streamSourceUrl"),
    STREAM_TITLE("streamTitle"),
    STREAM_URL("streamUrl"),
    TEMPLATE_PARAMS("templateParams"),
    TITLE("title"),
    TITLE_ID("titleId"),
    URL("url"),
    USER_ID("userId");

    private final String jsonString;

    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return jsonString;
    }
  }

  String getAppId();

  void setAppId(String appId);

  String getBody();

  void setBody(String body);

  String getBodyId();

  void setBodyId(String bodyId);

  String getExternalId();

  void setExternalId(String externalId);

  String getId();

  void setId(String id);

  Date getUpdated();

  void setUpdated(Date updated);

  List<MediaItem> getMediaItems();

  void setMediaItems(List<MediaItem> mediaItems);

  Long getPostedTime();

  void setPostedTime(Long postedTime);

  Float getPriority();

  void setPriority(Float priority);

  String getStreamFaviconUrl();

  void setStreamFaviconUrl(String streamFaviconUrl);

  String getStreamSourceUrl();

  void setStreamSourceUrl(String streamSourceUrl);

  String getStreamTitle();

  void setStreamTitle(String streamTitle);

  String getStreamUrl();

  void setStreamUrl(String streamUrl);

  Map<String, String> getTemplateParams();

  void setTemplateParams(Map<String, String> templateParams);

  String getTitle();

  void setTitle(String title);

  String getTitleId();

  void setTitleId(String titleId);

  String getUrl();

  void setUrl(String url);

  String getUserId();

  void setUserId(String userId);
}
