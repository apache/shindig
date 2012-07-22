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
package org.apache.shindig.protocol;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.protocol.multipart.FormDataItem;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * A request to pass to a bound service handler
 */
public interface RequestItem {

  // Common OpenSocial API fields
  String APP_ID = "appId";
  String START_INDEX = "startIndex";
  String COUNT = "count";
  String SORT_BY = "sortBy";
  String SORT_ORDER = "sortOrder";
  String FILTER_BY = "filterBy";
  String FILTER_OPERATION = "filterOp";
  String FILTER_VALUE = "filterValue";
  String FIELDS = "fields";// Opensocial defaults
  int DEFAULT_START_INDEX = 0;
  int DEFAULT_COUNT = 20;
  String APP_SUBSTITUTION_TOKEN = "@app";

  /**
   * Gets the Opensocial App ID for this request
   * @return an app ID
   */
  String getAppId();

  /**
   * Gets the value of the updatedSince parameter
   * @return A Date representing the updatedSince value
   */
  Date getUpdatedSince();

  /**
   * Gets the value of the startIndex parameter
   * @return An integer containing the value of startIndex
   */
  int getStartIndex();

  /**
   * Gets the value of the count parameter
   * @return An integer containing the value of count
   */
  int getCount();

  /**
   * Gets the value of the sortBy parameter
   * @return the value of the sortBy parameter
   */
  String getSortBy();

  /**
   * Gets the value of the sortOrder parameter
   * @return a SortOrder enum value representing the sortOrder parameter
   */
  SortOrder getSortOrder();

  /**
   * Gets the value of the filterBy parameter
   * @return the value of the filterBy parameter
   */
  String getFilterBy();

  /**
   * Gets the value of the filterOperation parameter
   * @return a SortOrder enum value representing the filterOperation parameter
   */
  FilterOperation getFilterOperation();

  /**
   * Gets the value of the filterValue parameter
   * @return the value of the filterValue parameter
   */
  String getFilterValue();

  /**
   * Gets the unique set of fields from the request
   *
   * @return Set of field names, empty if no fields specified.
   */
  Set<String> getFields();

  /**
   * Get the unique set of fields from the request with defaults
   * @param defaultValue returned if no fields are specified in the request.
   * @return specified set of fields or default value
   */
  Set<String> getFields(Set<String> defaultValue);

  /**
   * Returns the security token of this request
   * @return the token
   */
  SecurityToken getToken();

  /**
   * Converts a parameter into an object using a converter
   * @param parameterName the name of the parameter with data to convert
   * @param dataTypeClass The class to make
   * @param <T> The type of this object
   * @return A Valid Object of the given type
   */

  <T> T getTypedParameter(String parameterName, Class<T> dataTypeClass);

  /**
   * Assume that all the parameters in the request belong to single aggregate
   * type and convert to it.
   * @param dataTypeClass the class to convert to
   * @return Typed request object
   */

  <T> T getTypedRequest(Class<T> dataTypeClass);

  /**
   * Gets the specified parameter as a string
   * @param paramName the param name to get
   * @return the paramName value or null if the parameter is not found
   */
  String getParameter(String paramName);

  /**
   * Gets the specified parameter as a string, with a default value
   * @param paramName the param name to get
   * @param defaultValue the default value of the parameter
   * @return the paramName value or defaultValue if the parameter is not found
   */
  String getParameter(String paramName, String defaultValue);

  /**
   * Tries to get a list of values for a specified parameter.  This can include splitting
   * text on commas, dereferencing a json array and more.
   * @param paramName The parameter
   * @return A list of strings for the given parameter
   */
  List<String> getListParameter(String paramName);

  /**
   * Returns MIME content data for multipart/mixed form submissions
   * @param partName the part name to retrieve
   * @return The FormDataItem for this part.
   */
  FormDataItem getFormMimePart(String partName);

  /**
   * Gets an attribute for this request.  Attributes are a place to store per-request values that persist across the
   * life cycle.
   *
   * @param val the localized string variable for this request
   * @return the object associated with this requested string value or null if not found
   */
  Object getAttribute(String val);

  /**
   * Sets an attribute on this request object
   * @param val string value
   * @param obj an object
   */
  void setAttribute(String val, Object obj);

  /**
   * Get the list of parameter names for this request object.
   * @return A set of Parameter Names.
   */
   Set<String> getParameterNames();
}
