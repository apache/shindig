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
package org.apache.shindig.gadgets.admin;

import java.util.Set;

import com.google.caja.util.Sets;
import com.google.common.base.Objects;

/**
 * Feature administration data for a gadget.
 *
 * @version $Id: $
 */
public class FeatureAdminData {

  /**
   * Enumerates the type of feature list.
   *
   * @version $Id: $
   */
  public enum Type {
    WHITELIST, BLACKLIST
  }

  private Set<String> features;
  private Type type;

  /**
   * Constructor
   */
  public FeatureAdminData() {
    this(null, null);
  }

  /**
   * Constructor
   *
   * @param features
   *          a set of features
   * @param type
   *          determines which set takes priority over the other
   */
  public FeatureAdminData(Set<String> features, Type type) {
    if (features == null) {
      features = Sets.newHashSet();
    }
    if (type == null) {
      type = Type.WHITELIST;
    }
    this.features = features;
    this.type = type;
  }

  private void addFeatures(Set<String> toAdd, Set<String> features) {
    for (String feature : toAdd) {
      if (feature != null) {
        features.add(feature);
      }
    }
  }

  /**
   * Adds features for this gadget.
   *
   * @param toAdd
   *          the features for this gadget.
   */
  public void addFeatures(Set<String> toAdd) {
    addFeatures(toAdd, this.features);
  }

  private Set<String> createSingleFeatureSet(String feature) {
    Set<String> features = Sets.newHashSet();
    if (feature != null) {
      features.add(feature);
    }
    return features;
  }

  /**
   * Adds an feature for a gadget.
   *
   * @param toAdd
   *          the feature to add.
   */
  public void addFeature(String toAdd) {
    Set<String> features = createSingleFeatureSet(toAdd);
    addFeatures(features);
  }

  /**
   * Clears the set of features.
   */
  public void clearFeatures() {
    this.features.clear();
  }

  private void removeFeatures(Set<String> toRemove, Set<String> features) {
    if (toRemove != null && features != null) {
      for (String feature : toRemove) {
        features.remove(feature);
      }
    }
  }

  /**
   * Removes the list of features for a gadget.
   *
   * @param toRemove
   *          the features to remove.
   */
  public void removeFeatures(Set<String> toRemove) {
    removeFeatures(toRemove, this.features);
  }

  /**
   * Removes an feature for a gadget.
   *
   * @param toRemove
   *          the feature to remove.
   */
  public void removeFeature(String toRemove) {
    Set<String> features = createSingleFeatureSet(toRemove);
    removeFeatures(features, this.features);
  }

  /**
   * Gets the features.
   *
   * @return the features.
   */
  public Set<String> getFeatures() {
    return this.features;
  }

  /**
   * Gets the type of features list.
   *
   * @return the type of features list.
   */
  public Type getType() {
    return this.type;
  }

  /**
   * Sets the type. If this method is passed null than it will default to WHITELIST.
   *
   * @param type
   *          the type to set.
   */
  public void setType(Type type) {
    if (type == null) {
      type = Type.WHITELIST;
    }
    this.type = type;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FeatureAdminData) {
      FeatureAdminData test = (FeatureAdminData) obj;
      return this.getFeatures().equals(test.getFeatures())
              && this.getType().equals(test.getType());
    }
    return false;

  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.features, this.type);
  }
}
