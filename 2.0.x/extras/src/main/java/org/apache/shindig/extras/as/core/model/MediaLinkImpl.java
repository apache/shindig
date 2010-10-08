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

import org.apache.shindig.extras.as.opensocial.model.MediaLink;

/**
 * <p>MediaLinkImpl class.</p>
 *
 */
public class MediaLinkImpl implements MediaLink {
  
  private String target;
  private String type;
  private String width;
  private String height;
  private String duration;
  
  /**
   * Create a new MediaLink
   */
  public MediaLinkImpl() {
    this.target = null;
    this.type = null;
    this.width = null;
    this.height = null;
    this.duration = null;
  }

  /** {@inheritDoc} */
  public String getTarget() {
    return target;
  }

  /** {@inheritDoc} */
  public void setTarget(String target) {
    this.target = target;
  }

  /** {@inheritDoc} */
  public String getType() {
    return type;
  }

  /** {@inheritDoc} */
  public void setType(String type) {
    this.type = type;
  }

  /** {@inheritDoc} */
  public String getWidth() {
    return width;
  }

  /** {@inheritDoc} */
  public void setWidth(String width) {
    this.width = width;
  }

  /** {@inheritDoc} */
  public String getHeight() {
    return height;
  }

  /** {@inheritDoc} */
  public void setHeight(String height) {
    this.height = height;
  }

  /** {@inheritDoc} */
  public String getDuration() {
    return duration;
  }

  /** {@inheritDoc} */
  public void setDuration(String duration) {
    this.duration = duration;
  }
}
