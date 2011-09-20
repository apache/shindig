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

/**
 * @fileoverview This feature provides a means for multiple instances of a gadget to share
 *   a single frame for loading script code.
 *
 *   This is the container specific code.
 */

osapi.container.Container.addMixin('SharedScriptFrame', function(container) {

  /**
   * Map of gadgetUrls to script frame sites.
   * @type {!Object.<string, osapi.container.GadgetSite>}
   * @constant
   */
  var siteMap = {};

  /**
   * Creates a new shared script frame gadget instance on the page.
   *
   * @param {!string} url The gadget URL that requested the script frame.
   * @param {!Object} feature The feature segment of the gadget for the
   *   shared-script-frame feature.
   */
  var createScriptFrame = function(url, feature) {
    var view = osapi.container.GadgetSite.DEFAULT_VIEW_;
    if (feature.params && feature.params.view) {
      view = feature.params.view[0];
    }

    var elem = document.createElement('div');
    elem.style.display = 'none';
    document.body.appendChild(elem);

    var site = siteMap[url] = container.newGadgetSite(elem);
    var params = {};
    params[osapi.container.RenderParam.VIEW] = view;
    container.navigateGadget(site, url, undefined, params);
  };

  /**
   * Searches the map for the script frame being requested, if not found
   * it will create it.  Will call the rpc callback with the found or created
   * script frame name.
   *
   * @param {!Object} rpcArgs The arguments from the RPC call
   * @returns {?string} The name of the script frame
   */
  var getScriptFrameName = function(rpcArgs) {
    var fromURL = rpcArgs.gs.getActiveGadgetHolder().getUrl();

    var name, scriptSite = siteMap[fromURL];
    if (scriptSite) {
      name = scriptSite.getActiveGadgetHolder().getIframeId();
    }
    return name;
  };

  /**
   * Holds functions to respond to life-cycle events
   *
   * @type {!Object}
   * @constant
   */
  var lifeCycleHandlers = {};

  /**
   * Respond to the ON_RENDER event by creating a script frame for the
   * loading gadget, but only if we need one.
   *
   * @param {!Object} metadata The gadget metadata
   */
  lifeCycleHandlers[osapi.container.CallbackType.ON_RENDER] = function(metadata) {
    var url = metadata.url;
    try {
      var feature = metadata.modulePrefs.features['shared-script-frame'];
    } catch(e) {}
    if (feature && !siteMap[url]) {
      createScriptFrame(url, feature);
    }
  };

  container.addGadgetLifecycleCallback('shared-script-frame-setup', lifeCycleHandlers);
  container.rpcRegister('get_script_frame_name', getScriptFrameName);
  return {};
});