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

@ImplementedBy(MediaItemImpl.class)

public interface MediaItem {

  public static enum Field {
    MIME_TYPE("mimeType"),
    TYPE("type"),
    URL("url");

    private final String jsonString;

    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  public enum Type {
    AUDIO("audio"),
    IMAGE("image"),
    VIDEO("video");

    private final String jsonString;

    private Type(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  String getMimeType();

  void setMimeType(String mimeType);

  Type getType();

  void setType(Type type);

  String getUrl();

  void setUrl(String url);

}
