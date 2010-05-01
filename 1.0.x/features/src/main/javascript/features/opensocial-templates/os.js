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
/**
 * @fileoverview Implements os:renderAll tag and OpenSocial-specific
 * identifier resolver.
 */

/**
 * Identifier Resolver function for OpenSocial objects.
 * Checks for:
 * <ul>
 *   <li>Simple property</li>
 *   <li>JavaBean-style getter</li>
 *   <li>OpenSocial Field</li>
 *   <li>Data result set</li>
 * </ul>
 * @param {Object} object The object in the scope of which to get a named 
 * property.
 * @param {string} name The name of the property to get.
 * @return {Object?} The property requested. 
 */
os.resolveOpenSocialIdentifier = function(object, name) {
  // Simple property from object.
  if (typeof(object[name]) != "undefined") {
    return object[name];
  }

  // JavaBean-style getter method.
  var functionName = os.getPropertyGetterName(name);
  if (object[functionName]) {
    return object[functionName]();
  }

  // Check OpenSocial field by dictionary mapping
  if (object.getField) {
    var fieldData = object.getField(name);
    if (fieldData) {
      return fieldData;
    }
  }

  // Multi-purpose get() method
  if (object.get) {
    var responseItem = object.get(name);

    // ResponseItem is a data set
    if (responseItem && responseItem.getData) {
      var data = responseItem.getData();
      // Return array payload where appropriate
      return data.array_ || data;
    }
    return responseItem;
  }
  
  // Return undefined value, to avoid confusing with existing value of "null".
  var und;
  return und;
};

os.setIdentifierResolver(os.resolveOpenSocialIdentifier);

/**
 * Create methods for an object based upon a field map for OpenSocial.
 * @param {Object} object Class object to have methods created for.
 * @param {Object} fields A key-value map object to retrieve fields (keys) and
 * method names (values) from.
 * @private
 */
os.createOpenSocialGetMethods_ = function(object, fields) {
  if (object && fields) {
    for (var key in fields) {
      var value = fields[key];
      var getter = os.getPropertyGetterName(value);
      object.prototype[getter] = function() {
        this.getField(key);
      }
    }
  }
};

/**
 * Automatically register JavaBean-style methods for various OpenSocial objects.
 * @private
 */
os.registerOpenSocialFields_ = function() {
  var fields = os.resolveOpenSocialIdentifier.FIELDS;
  if (opensocial) {
    // TODO: Add more OpenSocial objects.
    if (opensocial.Person) {
      //os.createOpenSocialGetMethods_(opensocial.Person,  opensocial.Person.Field);
    }
  }
};

os.registerOpenSocialFields_();
