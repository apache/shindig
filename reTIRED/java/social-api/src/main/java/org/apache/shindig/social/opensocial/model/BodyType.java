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

@ImplementedBy(BodyTypeImpl.class)

public interface BodyType {

  public static enum Field {
    BUILD("build"),
    EYE_COLOR("eyeColor"),
    HAIR_COLOR("hairColor"),
    HEIGHT("height"),
    WEIGHT("weight");

    private final String jsonString;

    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }
  
  String getBuild();

  void setBuild(String build);

  String getEyeColor();

  void setEyeColor(String eyeColor);

  String getHairColor();

  void setHairColor(String hairColor);

  String getHeight();

  void setHeight(String height);

  String getWeight();

  void setWeight(String weight);
}
