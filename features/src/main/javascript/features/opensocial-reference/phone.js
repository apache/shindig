/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*global opensocial */

/**
 * @fileoverview Representation of an phone number.
 */


/**
 * @class
 * Base interface for all phone objects.
 *
 * @name opensocial.Phone
 */


/**
 * Base interface for all phone objects.
 *
 * @private
 * @constructor
 * @deprecated since 1.0 see http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Gadget.xml#rfc.section.A.47.
 */
opensocial.Phone = function(opt_params) {
  this.fields_ = opt_params || {};
};


/**
 * @static
 * @class
 * All of the fields that a phone has. These are the supported keys for the
 * <a href="opensocial.Phone.html#getField">Phone.getField()</a> method.
 *
 * @name opensocial.Phone.Field
 * @enum {string}
 * @const
 * @deprecated since 1.0 see http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Gadget.xml#rfc.section.A.48.
 */
opensocial.Phone.Field = {
  /**
   * The phone number type or label, specified as a String.
   * Examples: work, my favorite store, my house, etc.
   *
   * @member opensocial.Phone.Field
   */
  TYPE: 'type',

  /**
   * The phone number, specified as a String.
   *
   * @member opensocial.Phone.Field
   */
  NUMBER: 'number'
};


/**
 * Gets data for this phone that is associated with the specified key.
 *
 * @param {string} key The key to get data for;
 *    keys are defined in <a href="opensocial.Phone.Field.html"><code>
 *    Phone.Field</code></a>.
 * @param {Object.<opensocial.DataRequest.DataRequestFields, Object>}
 *  opt_params Additional
 *    <a href="opensocial.DataRequest.DataRequestFields.html">params</a>
 *    to pass to the request.
 * @return {string} The data.
 * @deprecated since 1.0 see http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Gadget.xml#rfc.section.A.47.1.1.
 */
opensocial.Phone.prototype.getField = function(key, opt_params) {
  return opensocial.Container.getField(this.fields_, key, opt_params);
};
