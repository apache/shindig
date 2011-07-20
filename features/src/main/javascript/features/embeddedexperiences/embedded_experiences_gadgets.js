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

gadgets['embeddedExperiences'] = gadgets['embeddedExperiences'] || {};

(function() {
  /**
   * Sets the context for this embedded experience.
   * @param {Object} context the embedded experiences context.
   */
  function setDataContext(context) {
    opensocial.data.DataContext.putDataSet('org.opensocial.ee.context', context);
  };

  /**
   * Init the embedded experiences feature.  This calls an RPC handler to get
   * the embedded experiences context and puts it in the gadgets data context.
   * @param {Object} config configuration for the feature.
   */
  function init(config) {
    gadgets.rpc.call(null, 'ee_gadget_rendered', setDataContext, {});
    gadgets.rpc.register('ee_set_context', setDataContext);
  };

  if (gadgets.config) {
    gadgets.config.register('embedded-experiences', null, init);
  }

}());
