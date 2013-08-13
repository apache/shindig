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

gadgets['ee'] = gadgets['ee'] || {};

(function() {

  var contextListeners = [];
  var isContextSet = false;

  /**
   * Sets the context for this embedded experience.
   * @param {Object} context
   *         The embedded experiences context.
   */
  function setDataContext(context) {
    if (this.f !== '..') {
      return;
    }
    opensocial.data.DataContext.putDataSet('org.opensocial.ee.context', context);
  };

  /**
   * Init the embedded experiences feature.  This calls an RPC handler to get
   * the embedded experiences context and puts it in the gadgets data context.
   * @param {Object} config
   *        Configuration for the feature.
   */
  function init(config) {
    gadgets.rpc.call(null, 'ee_gadget_rendered', setDataContext, {});
    gadgets.rpc.register('ee_set_context', setDataContext);
    opensocial.data.getDataContext().registerListener('org.opensocial.ee.context', function(key) {
      var context = opensocial.data.getDataContext().getDataSet(key);
      isContextSet = true;
      var length = contextListeners.length;
      for(var i = length;  i--;) {
        contextListeners[i](context);
      }
    });
  };

  if (gadgets.config) {
    gadgets.config.register('embedded-experiences', null, init);
  }

  /**
   * Registers a listener for when the embedded experiences context object is set for
   * this gadget.  This listener will be called whenever the context is set for the gadget.
   *
   * @param {Function} listener
   *        A function to be called when the listener is set.
   */
  gadgets.ee.registerContextListener = function(listener) {
    //Add the listener regardless
    contextListeners.push(listener);

    //It could be that the context was already set before the gadget called this function
    //so see if we have a context object in the data context and if we do call the listener
    //back right away
    if(isContextSet) {
      listener(opensocial.data.getDataContext().getDataSet('org.opensocial.ee.context'));
    }
  };
}());
