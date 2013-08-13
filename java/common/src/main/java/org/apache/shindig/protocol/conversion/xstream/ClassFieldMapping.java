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

/**
 * This represents the mapping between a class and a field, potentially with a
 * parent element. It is used to define the element names that are used to
 * serialize the contents of a class.
 *
 * eg
 * <pre>
 * &lt;outerobject&gt;
 * &lt;listcontainer&gt;
 *    &lt;listelement&gt;
 *       &lt;objectcontent&gt;
 *    &lt;/listelement&gt;
 * &lt;/listcontainer&gt;
 * ...
 * &lt;/outerobject&gt;
 * </pre>
 * or (not currently used in OS)
 * <pre>
 * &lt;person&gt;
 *    &lt;emails&gt;
 *       &lt;email&gt;
 *          &lt;type&gt;&lt;/type&gt;
 *          &lt;value&gt;&lt;/value&gt;
 *       &lt;/email&gt;
 *       ...
 *    &lt;/emails&gt;
 *    ...
 * &lt;/person&gt;
 * </pre>
 *
 */
public class ClassFieldMapping {

  /**
   * The name of the element to map the class to.
   */
  private String elementName;

  /**
   * The class being mapped.
   */
  private Class<?> mappedClazz;
  /**
   * An optional parent element name.
   */
  private String fieldParentName;

  /**
   * Create a simple element class mapping, applicable to all parent elements.
   *
   * @param elementName
   *          the name of the element
   * @param mappedClazz
   *          the class to map to the name of the element
   */
  public ClassFieldMapping(String elementName, Class<?> mappedClazz) {
    this.elementName = elementName;
    this.mappedClazz = mappedClazz;
    this.fieldParentName = null;
  }

  /**
   * Create a element class mapping, that only applies to one parent element
   * name.
   *
   * @param parentName
   *          the name of the parent element that this mapping applies to
   * @param elementName
   *          the name of the element
   * @param mappedClazz
   *          the class to map to the name of the element
   */
  public ClassFieldMapping(String parentName, String elementName, Class<?> mappedClazz) {
    this.elementName = elementName;
    this.mappedClazz = mappedClazz;
    this.fieldParentName = parentName;
  }

  /**
   * @return get the element name.
   */
  public String getElementName() {
    return elementName;
  }

  /**
   * @return get the mapped class.
   */
  public Class<?> getMappedClass() {
    return mappedClazz;
  }

  /**
   * Does this ClassFieldMapping match the supplied parent and type.
   *
   * @param parent
   *          the parent element, which may be null
   * @param type
   *          the type of the field being stored
   * @return true if this mapping is a match for the combination
   */
  public boolean matches(String parent, Class<?> type) {
    if (fieldParentName == null) {
      return mappedClazz.isAssignableFrom(type);
    }
    return fieldParentName.equals(parent)
        && mappedClazz.isAssignableFrom(type);
  }

}
