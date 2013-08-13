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
 * ObjectId as defined by the OpenSocial 2.0.1 Spec
 * @see "http://opensocial-resources.googlecode.com/svn/spec/2.0.1/Core-Data.xml#Object-Id"
 */
public class ObjectId {

  private Object objectId;

  /**
   * This constructor allows for a LocalId to be passed in order
   * to create an ObjectId.
   *
   * @param localId the localId used to create the ObjectId
   */
  public ObjectId(LocalId localId) {
    this.objectId = localId;
  }

  /**
   * This constructor allows for a GlobalId to be passed in order
   * to create an ObjectId.
   *
   * @param globalId the globalId used to create the ObjectId
   */
  public ObjectId(GlobalId globalId) {
    this.objectId = globalId;
  }

  /**
   * This constructor allows for a String to be passed in order
   * to create an ObjectId. It will store it as a LocalId and
   * verify it as such.
   *
   * @param id The id of the new LocalId that will be created
   * @throws IllegalArgumentException when the id provided could not be parsed
   *   into either a GlobalId or a LocalId
   */
  public ObjectId(String id) throws IllegalArgumentException {
	try {
	  this.objectId = new GlobalId(id);
	} catch(IllegalArgumentException e1) {
      // Not a valid globalId, try localId
      try {
        this.objectId = new LocalId(id);
      } catch(IllegalArgumentException e2) {
        // Not either so throw exception
	      throw new IllegalArgumentException("The provided ObjectId is not valid");
      }
	}
  }

  /**
   * Get the objectId.
   *
   * @return objectId Object
   */
  public Object getObjectId() {
    return this.objectId;
  }

  /**
   * Set the objectId with an ObjectId
   *
   * @param objectId ObjectId
   */
  public void setObjectId(ObjectId objectId) {
	this.objectId = objectId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ObjectId)) {
      return false;
    }
    ObjectId actual = (ObjectId) o;
    return this.getObjectId().equals(actual.getObjectId());
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
