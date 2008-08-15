/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse;

/**
 * Mutable wrapper around a {@code ParsedCssDeclaration}.
 * Used by rewriting to manipulate parsed gadget CSS, and
 * to separate parsing from manipulation code.
 */
public class GadgetCssDeclaration {
  private String name;
  private String value;
  
  /**
   * Construct a mutable attribute out of an immutable parsed one.
   * @param source Parsed CSS declaration
   */
  public GadgetCssDeclaration(ParsedCssDeclaration source) {
    this.name = source.getName();
    this.value = source.getValue();
  }
  
  /**
   * Construct a mutable CSS declaration from a name/value pair.
   * @param name Name of attribute
   * @param value Value of attribute
   */
  public GadgetCssDeclaration(String name, String value) {
    this.name = name;
    this.value = value;
  }
  
  /**
   * @return Name of the HTML attribute
   */
  public String getName() {
    return name;
  }
  
  /**
   * @return Value of the HTML attribute
   */
  public String getValue() {
    return value;
  }
  
  /**
   * Only provide an API for setting value. To set
   * a new attribute a developer can simply create
   * a new one. To replace, the developer can delete
   * the existing one before doing so.
   * @param value New HTML attribute value.
   */
  public void setValue(String value) {
    this.value = value;
  }
}
