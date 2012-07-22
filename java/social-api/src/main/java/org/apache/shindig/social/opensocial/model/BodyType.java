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
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.BodyTypeImpl;

import com.google.inject.ImplementedBy;

/**
 * Base interface for all body type objects. see
 * <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Person.Field.BODY_TYPE">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Person.Field.BODY_TYPE</a>
 */
@ImplementedBy(BodyTypeImpl.class)
@Exportablebean
public interface BodyType {

  /**
   * The fields that represent the person object in serialized form.
   */
  public static enum Field {
    /** the field name for build. */
    BUILD("build"),
    /** the field name for build. */
    EYE_COLOR("eyeColor"),
    /** the field name for hairColor. */
    HAIR_COLOR("hairColor"),
    /** the field name for height. */
    HEIGHT("height"),
    /** the field name for weight. */
    WEIGHT("weight");

    /**
     * The field name that the instance represents.
     */
    private final String jsonString;

    /**
     * create a field base on the a json element.
     *
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * emit the field as a json element.
     *
     * @return the field name
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * The build of the person's body, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @return the build of the person's body
   */
  String getBuild();

  /**
   * The build of the person's body, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @param build the build of the person's body
   */
  void setBuild(String build);

  /**
   * The eye color of the person, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @return the eye color of the person
   */
  String getEyeColor();

  /**
   * The eye color of the person, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @param eyeColor the eye color of the person
   */
  void setEyeColor(String eyeColor);

  /**
   * The hair color of the person, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @return the hair color of the person
   */
  String getHairColor();

  /**
   * The hair color of the person, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @param hairColor the hair color of the person
   */
  void setHairColor(String hairColor);

  /**
   * The height of the person in meters, specified as a number. Container support for this field is
   * OPTIONAL.
   *
   * @return the height of the person in meters
   */
  Float getHeight();

  /**
   * The height of the person in meters, specified as a number. Container support for this field is
   * OPTIONAL.
   *
   * @param height the height of the person in meters
   */
  void setHeight(Float height);

  /**
   * The weight of the person in kilograms, specified as a number. Container support for this field
   * is OPTIONAL.
   *
   * @return the weight of the person in kilograms
   */
  Float getWeight();

  /**
   * The weight of the person in kilograms, specified as a number. Container support for this field
   * is OPTIONAL.
   *
   * @param weight weight of the person in kilograms
   */
  void setWeight(Float weight);
}
