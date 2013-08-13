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
package org.apache.shindig.social.opensocial.spi;

import com.google.common.base.Objects;

/**
 * GroupId as defined by the OpenSocial 2.0.1 Spec
 * @see "http://opensocial-resources.googlecode.com/svn/spec/2.0.1/Social-Data.xml#Group-Id"
 */
public class GroupId {

  public enum Type {
    objectId  (0),
    self      (1),
    friends   (2),
    all       (3),
    custom   (4);

    private final int id;
    Type(int id) { this.id = id; }
    public int getValue() { return id; }
  }

  // Can be of type ObjectId or a String that represents a GroupId.Type
  private Object objectId;

  /**
   * If the given object is an objectId, create and store
   * Else we need the string representation to store, including the "@"
   *
   * @param objectId Object
   * @throws IllegalArgumentException
   */
  public GroupId(Object objectId) throws IllegalArgumentException {
    if(objectId == null) {
      this.objectId = new ObjectId("");
    // If it is an objectId, store as is
    } else if(objectId instanceof ObjectId) {
      this.objectId = (ObjectId) objectId;
    // Else it must be a string, store as such
    } else {
      if(Type.objectId.equals(getType(objectId))) {
        this.objectId = new ObjectId(objectId.toString());
      } else {
        this.objectId = objectId.toString();
      }
    }
  }

  /**
   * Backwards Compatibility.
   *
   * @param type
   * @param objectId
   * @throws IllegalArgumentException when the provided objectId is not valid
   */
  public GroupId(Type type, String objectId) throws IllegalArgumentException {
    // If Type is an objectId, convert objectId to ObjectId and store
    if(type.equals(Type.objectId)) {
      this.objectId = new ObjectId(objectId);
    // Else store the string representation of the type
    } else if(Type.custom.equals(type)){
      //Custom @ id
      this.objectId = objectId;
    } else {
      this.objectId = typeToString(type);
    }
  }

  /**
   * Convert a type to string
   *
   * @param type GroupId.Type to convert
   * @return JSON string value
   */
  private String typeToString(Type type) {
    if(Type.all.equals(type)) {
      return "@all";
    } else if(Type.friends.equals(type)) {
      return "@friends";
    } else {
      return "@self";
    }
  }

  /**
   * Get the type of the stored objectId.
   *
   * @return GroupId.Type
   */
  public Type getType() {
    return getType(this.objectId);
  }

  /**
   * Get the type of the given objectId.
   *
   * @return GroupId.Type
   */
  private Type getType(Object objectId) {
	String type = parseType(objectId);
    if(type.equals("self")) {
      return Type.self;
    } else if(type.equals("friends")) {
      return Type.friends;
    } else if(type.equals("all")) {
      return Type.all;
    } else if(objectId instanceof String && ((String)objectId).startsWith("@")) {
    	// Could be a custom @ id, and it certainly is not an object id
    	// return null we don't know the type
    	return Type.custom;
    } else {
      return Type.objectId;
    }
  }

  /**
   * Parse the type of the provided objectId.
   *
   * @param objectId Object to parse
   * @return type String
   */
  private String parseType(Object objectId) {
    if(objectId instanceof String) {
      String o = (String) objectId;
      // Remove the "@"
      return o.substring(1, o.length());
    } else {
      return "";
    }
  }

  /**
   * Set the objectId with a String
   *
   * @param objectId String
   * @throws IllegalArgumentException
   */
  public void setObjectId(String objectId) throws IllegalArgumentException {
    if(getType(objectId).equals(Type.objectId)) {
      this.objectId = new ObjectId(objectId);
    } else {
      this.objectId = objectId;
    }
  }

  /**
   * Get the objectId
   *
   * @return objectId Object
   */
  public Object getObjectId() {
    return this.objectId;
  }

  /**
   * Backwards compatibility.
   *
   * @param jsonId JSON string value of a GroupId
   * @return GroupId
   * @throws IllegalArgumentException
   */
  public static GroupId fromJson(String jsonId) throws IllegalArgumentException {
    return new GroupId(jsonId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof GroupId)) {
      return false;
    }
    GroupId actual = (GroupId) o;
    return this.objectId.equals(actual.objectId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.objectId);
  }

  @Override
  public String toString() {
    return this.objectId.toString();
  }
}
