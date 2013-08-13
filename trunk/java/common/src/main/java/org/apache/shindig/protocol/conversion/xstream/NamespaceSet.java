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

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A container class that defines namespaces and subsequent element named for an
 * element in the output stack. A set of active namespaces are defined by
 * addNamespace and a name to prefixed name translation is specified by
 * addPrefixedElement.
 */
public class NamespaceSet {

  /**
   * A map namespace attributes to the namespace uri.
   */
  private Map<String, String> namespaces = Maps.newHashMap();
  /**
   * A map of localElement names to prefixed element names.
   */
  private Map<String, String> elementNames = Maps.newHashMap();

  /**
   * Add a namespace to the list.
   *
   * @param nsAttribute
   *          the attribute to be used to specify the namespace
   * @param namespace
   *          the namespace URI
   */
  public void addNamespace(String nsAttribute, String namespace) {
    namespaces.put(nsAttribute, namespace);
  }

  /**
   * Add a localname translation.
   *
   * @param elementName
   *          the local name of the element
   * @param namespacedElementName
   *          the final name of the element with prefix.
   */
  public void addPrefixedElement(String elementName, String namespacedElementName) {
    elementNames.put(elementName, namespacedElementName);
  }

  /**
   * Convert an element name, if necessary.
   *
   * @param name
   *          the name to be converted.
   * @return the converted name, left as is if no conversion was required.
   */
  public String getElementName(String name) {
    return elementNames.get(name) != null ? elementNames.get(name) : name;
  }

  /**
   * @return an Set of entries containing the namespace attributes and uris,
   *         attributes in the key, uri's in the value or each entry.
   */
  public Set<Entry<String, String>> nameSpaceEntrySet() {
    return namespaces.entrySet();
  }

}
