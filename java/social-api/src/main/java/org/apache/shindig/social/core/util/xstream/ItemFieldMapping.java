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
package org.apache.shindig.social.core.util.xstream;

/**
 * ItemFieldMapping defines a mapping of a class within a class to an element
 * name. Where classes are tested, the must implement or extend the specified
 * classes, unlike the standard behaviour of XStream they dont need to be the
 * classes in question.
 */
public class ItemFieldMapping {

  /**
   * The Class that the field is defined in.
   */
  private Class<?> definedIn;
  /**
   * The class of the item that is being defined.
   */
  private Class<?> itemType;
  /**
   * The name of the element that should be used for this field.
   */
  private String elementName;

  /**
   * Create a Item Field Mapping object specifying that where the class itemType
   * appears in the Class definedIn, the elementName should be used for the
   * Element Name.
   *
   * @param definedIn
   *          the class which contains the class of interest.
   * @param itemType
   *          the class of the class of interest.
   * @param elementName
   *          the element name to use for this class.
   *
   */
  public ItemFieldMapping(Class<?> definedIn, Class<?> itemType,
      String elementName) {
    this.definedIn = definedIn;
    this.itemType = itemType;
    this.elementName = elementName;
  }

  /**
   * Does this ItemFieldMapping match the supplied classes.
   *
   * @param definedIn
   *          the class that the target test class is defiend in, this is a real
   *          class
   * @param itemType
   *          the target class, the real class
   * @return true if the defiendIn class implements the defiendIn class of this
   *         ItemFieldMapping and the itemType class implements the itemType
   *         class of this ItemFieldMapping.
   */
  public boolean matches(Class<?> definedIn, Class<?> itemType) {
    return (this.definedIn.isAssignableFrom(definedIn) && this.itemType
        .isAssignableFrom(itemType));
  }

  /**
   * @return the element name for this ItemFieldMapping.
   */
  public String getElementName() {
    return elementName;
  }

}
