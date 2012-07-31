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
package org.apache.shindig.protocol.conversion.xstream;

import com.thoughtworks.xstream.mapper.Mapper.ImplicitCollectionMapping;

/**
 * <p>
 * ItemFieldMapping defines a mapping of a class within a class to an element
 * name. Where classes are tested, the must implement or extend the specified
 * classes, unlike the standard behaviour of XStream they don't need to be the
 * classes in question.
 * </p>
 * <p>
 * The structure is used for implicit collections of the form *
 * </p>
 *
 * <pre>
 * &lt;outerobject&gt;
 *    &lt;listelement&gt;
 *       &lt;objectcontent&gt;
 *    &lt;/listelement&gt;
 *    &lt;listelement&gt;
 *       &lt;objectcontent&gt;
 *    &lt;/listelement&gt;
 * ...
 * &lt;/outerobject&gt;
 * </pre>
 * <p>
 * or
 * </p>
 *
 * <pre>
 * &lt;person&gt;
 *     &lt;emails&gt;
 *        &lt;type&gt;&lt;/type&gt;
 *        &lt;value&gt;&lt;/value&gt;
 *     &lt;/emails&gt;
 *     &lt;emails&gt;
 *        &lt;type&gt;&lt;/type&gt;
 *        &lt;value&gt;&lt;/value&gt;
 *     &lt;/emails&gt;
 *     &lt;emails&gt;
 *        &lt;type&gt;&lt;/type&gt;
 *        &lt;value&gt;&lt;/value&gt;
 *     &lt;/emails&gt;
 *     ...
 * &lt;/person&gt;
 * </pre>
 * <p>
 * would be specified with NewItemFieldMapping(Person.class, "emails",
 * ListField.class, "emails");
 * </p>
 */
public class ImplicitCollectionFieldMapping implements ImplicitCollectionMapping {

  /**
   * The Class that the field is defined in.
   */
  private Class<?> definedIn;
  /**
   * The class of the item that is being defined.
   */
  private Class<?> itemType;
  /**
   * The name of the fields in the class (get and set methods)
   */
  private String fieldName;
  /**
   * The name of the element that should be used for this field.
   */
  private String itemFieldName;

  /**
   * Create a Item Field Mapping object specifying that where the class itemType
   * appears in the Class definedIn, the elementName should be used for the
   * Element Name.
   *
   * @param definedIn
   *          the class which contains the method
   * @param fieldName
   *          the name of the method/field in the class.
   * @param itemType
   *          the type of the method/field in the class.
   * @param itemFieldName
   *          the name of element in the xml
   *
   */
  public ImplicitCollectionFieldMapping(Class<?> definedIn, String fieldName,
      Class<?> itemType, String itemFieldName) {
    this.definedIn = definedIn;
    this.itemType = itemType;
    this.itemFieldName = itemFieldName;
    this.fieldName = fieldName;
  }

  /**
   * Does this ItemFieldMapping match the supplied classes.
   *
   * @param definedIn
   *          the class that the target test class is defined in, this is a real
   *          class
   * @param itemType
   *          the target class, the real class
   * @return true if the definedIn class implements the definedIn class of this
   *         ItemFieldMapping and the itemType class implements the itemType
   *         class of this ItemFieldMapping.
   */
  public boolean matches(Class<?> definedIn, Class<?> itemType) {
    return (this.definedIn.isAssignableFrom(definedIn) && this.itemType
        .isAssignableFrom(itemType));
  }

  public boolean matches(Class<?> definedIn, String fieldName) {
    return (this.definedIn.isAssignableFrom(definedIn) && this.fieldName
        .equals(fieldName));
  }

  /**
   * @return
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * @return
   */
  public String getItemFieldName() {
    return itemFieldName;
  }

  /**
   * @return
   */
  public Class<?> getItemType() {
    return itemType;
  }

  public String getKeyFieldName() {
    return null;
  }
}
