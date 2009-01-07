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
package org.apache.shindig.gadgets.spec;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.variables.Substitutions;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a UserPref tag.
 */
public class UserPref {
  /**
   * UserPref@name
   * Message bundles
   */
  private final String name;
  public String getName() {
    return name;
  }

  /**
   * UserPref@display_name
   * Message bundles
   */
  private String displayName;
  public String getDisplayName() {
    return displayName;
  }

  /**
   * UserPref@default_value
   * Message bundles
   */
  private String defaultValue;
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * UserPref@required
   */
  private final boolean required;
  public boolean getRequired() {
    return required;
  }

  /**
   * UserPref@datatype
   */
  private final DataType dataType;
  public DataType getDataType() {
    return dataType;
  }

  /**
   * UserPref.EnumValue
   * Collapsed so that EnumValue@value is the key and EnumValue@display_value
   * is the value. If display_value is not present, value will be used.
   * Message bundles are substituted into display_value, but not value.
   */
  private Map<String, String> enumValues;
  public Map<String, String> getEnumValues() {
    return enumValues;
  }

  /**
   * UserPref.EnumValue (ordered)
   * Useful for rendering ordered lists of user prefs with enum type.
   */
  private List<EnumValuePair> orderedEnumValues;
  public List<EnumValuePair> getOrderedEnumValues() {
    return orderedEnumValues;
  }

  /**
   * Performs substitutions on the pref. See field comments for details on what
   * is substituted.
   *
   * @param substituter
   * @return The substituted pref.
   */
  public UserPref substitute(Substitutions substituter) {
    UserPref pref = new UserPref(this);
    pref.displayName = substituter.substituteString(displayName);
    pref.defaultValue = substituter.substituteString(defaultValue);
    if (enumValues.isEmpty()) {
      pref.enumValues = Collections.emptyMap();
    } else {
      Map<String, String> values = Maps.newHashMapWithExpectedSize(enumValues.size());
      for (Map.Entry<String, String> entry : enumValues.entrySet()) {
        values.put(entry.getKey(), substituter.substituteString(entry.getValue()));
      }
      pref.enumValues = ImmutableMap.copyOf(values);
    }
    if (orderedEnumValues.isEmpty()) {
      pref.orderedEnumValues = Collections.emptyList();
    } else {
      List<EnumValuePair> orderedValues = Lists.newLinkedList();
      for (EnumValuePair evp : orderedEnumValues) {
        orderedValues.add(new EnumValuePair(evp.getValue(),
            substituter.substituteString(evp.getDisplayValue())));
      }
      pref.orderedEnumValues = Collections.unmodifiableList(orderedValues);
    }
    return pref;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<UserPref name=\"")
       .append(name)
       .append("\" display_name=\"")
       .append(displayName)
       .append("\" default_value=\"")
       .append(defaultValue)
       .append("\" required=\"")
       .append(required)
       .append("\" datatype=\"")
       .append(dataType.toString().toLowerCase())
       .append('\"');
    if (enumValues.isEmpty()) {
      buf.append("/>");
    } else {
      buf.append('\n');
      for (Map.Entry<String, String> entry : enumValues.entrySet()) {
        buf.append("<EnumValue value=\"")
           .append(entry.getKey())
           .append("\" value=\"")
           .append("\" display_value=\"")
           .append(entry.getValue())
           .append("\"/>\n");
      }
      buf.append("</UserPref>");
    }
    return buf.toString();
  }

  /**
   * @param element
   * @throws SpecParserException
   */
  public UserPref(Element element) throws SpecParserException {
    String name = XmlUtil.getAttribute(element, "name");
    if (name == null) {
      throw new SpecParserException("UserPref@name is required.");
    }
    this.name = name;

    displayName = XmlUtil.getAttribute(element, "display_name", name);
    defaultValue = XmlUtil.getAttribute(element, "default_value", "");
    required = XmlUtil.getBoolAttribute(element, "required");

    String dataType = XmlUtil.getAttribute(element, "datatype", "string");
    this.dataType = DataType.parse(dataType);

    NodeList children = element.getElementsByTagName("EnumValue");
    if (children.getLength() > 0) {
      Map<String, String> enumValues = Maps.newHashMap();
      List<EnumValuePair> orderedEnumValues = Lists.newLinkedList();
      for (int i = 0, j = children.getLength(); i < j; ++i) {
        Element child = (Element)children.item(i);
        String value = XmlUtil.getAttribute(child, "value");
        if (value == null) {
          throw new SpecParserException("EnumValue@value is required.");
        }
        String displayValue
            = XmlUtil.getAttribute(child, "display_value", value);
        enumValues.put(value, displayValue);
        orderedEnumValues.add(new EnumValuePair(value, displayValue));
      }
      this.enumValues = Collections.unmodifiableMap(enumValues);
      this.orderedEnumValues = Collections.unmodifiableList(orderedEnumValues);
    } else {
      this.enumValues = Collections.emptyMap();
      this.orderedEnumValues = Collections.emptyList();
    }
  }

  /**
   * Produces a UserPref suitable for substitute()
   * @param userPref
   */
  private UserPref(UserPref userPref) {
    name = userPref.name;
    dataType = userPref.dataType;
    required = userPref.required;
  }

  /**
   * Possible values for UserPref@datatype
   */
  public static enum DataType {
    STRING, HIDDEN, BOOL, ENUM, LIST, NUMBER;

    /**
     * Parses a data type from the input string.
     *
     * @param value
     * @return The data type of the given value.
     */
    public static DataType parse(String value) {
      for (DataType type : DataType.values()) {
        if (type.toString().compareToIgnoreCase(value) == 0) {
          return type;
        }
      }
      return STRING;
    }
  }

  /**
   * Simple data structure representing a value/displayValue pair
   * for UserPref enums. Value is EnumValue@value, and DisplayValue is EnumValue@displayValue.
   */
  public static class EnumValuePair {
    private final String value;
    private final String displayValue;

    private EnumValuePair(String value, String displayValue) {
      this.value = value;
      this.displayValue = displayValue;
    }

    public String getValue() {
      return value;
    }

    public String getDisplayValue() {
      return displayValue;
    }
  }
}
