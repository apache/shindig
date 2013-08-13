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
import org.apache.shindig.social.opensocial.model.MediaLink;

/**
 * <p>MediaLinkImpl class.</p>
 *
 */
public class MediaLinkImpl extends ExtendableBeanImpl implements MediaLink {

  private static final long serialVersionUID = 1L;
  private Integer duration;
  private Integer height;
  private String url;
  private Integer width;
  private ExtendableBean openSocial;

  /**
   * Create a new MediaLink
   */
  public MediaLinkImpl() {
  }

  /** {@inheritDoc} */
  public Integer getDuration() {
    return duration;
  }

  /** {@inheritDoc} */
  public void setDuration(Integer duration) {
    this.duration = duration;
    put("duration", duration);
  }

  /** {@inheritDoc} */
  public Integer getHeight() {
    return height;
  }

  /** {@inheritDoc} */
  public void setHeight(Integer height) {
    this.height = height;
    put("height", height);
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
  public Integer getWidth() {
    return width;
  }

  /** {@inheritDoc} */
  public void setWidth(Integer width) {
    this.width = width;
    put("width", width);
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
