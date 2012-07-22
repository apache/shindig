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

import org.apache.shindig.social.opensocial.model.BodyType;

/**
 * see
 * <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.BodyType">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.BodyType</a>.
 */
public class BodyTypeImpl implements BodyType {

  private String build;
  private String eyeColor;
  private String hairColor;
  private Float height;
  private Float weight;

  public String getBuild() {
    return build;
  }

  /** {@inheritDoc} */
  public void setBuild(String build) {
    this.build = build;
  }

  public String getEyeColor() {
    return eyeColor;
  }

  /** {@inheritDoc} */
  public void setEyeColor(String eyeColor) {
    this.eyeColor = eyeColor;
  }

  public String getHairColor() {
    return hairColor;
  }

  /** {@inheritDoc} */
  public void setHairColor(String hairColor) {
    this.hairColor = hairColor;
  }

  public Float getHeight() {
    return height;
  }

  /** {@inheritDoc} */
  public void setHeight(Float height) {
    this.height = height;
  }

  public Float getWeight() {
    return weight;
  }

  /** {@inheritDoc} */
  public void setWeight(Float weight) {
    this.weight = weight;
  }
}
