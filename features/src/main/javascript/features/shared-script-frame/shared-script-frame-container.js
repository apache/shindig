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
   * Generates a key to hash the script frame by for this gadget.  If the
   * gadget uses locked domains and specifies participants in the locked domain,
   * those other participants will be figured into this key so that they in turn
   * generate the same key.
   *
   * @param {!string} url The gadget URL that requested the script frame.
   * @param {?Object} ldFeature The feature segment of the gadget for the
   *   locked-domain feature.
   */
  var getFrameKey = function(url, ldFeature) {
    var participants, filtered = {};
    filtered[url.toLowerCase()] = 1;

    if (ldFeature && ldFeature.params && (participants = ldFeature.params.participant)) {
      if (typeof(participants) == 'string') {
        filtered[participants.toLowerCase()] = 1;
      }
      else {
        for (var i = 0, participant; participant = participants[i]; i++) {
          filtered[participant.toLowerCase()] = 1;
        }
      }
    }

    var ret = [];
    for (i in filtered) {
      ret.push(i);
    }
    return ret.sort().join('');
  }

  /**
   * Creates a new shared script frame gadget instance on the page.
   *
   * @param {!string} url The gadget URL that requested the script frame.
   * @param {!Object} feature The feature segment of the gadget for the
   *   shared-script-frame feature.
   * @param {?Object} ldFeature The feature segment of the gadget for the
   *   locked-domain feature.
   */
  var createScriptFrame = function(url, feature, ldFeature) {
    var key = getFrameKey(url, ldFeature);
    if (siteMap[key]) {
      return;
    }

    var view = osapi.container.GadgetSite.DEFAULT_VIEW_;
    if (feature.params && feature.params.view) {
      view = feature.params.view[0];
    }

    var elem = document.createElement('div');
    elem.style.display = 'none';
    document.body.appendChild(elem);

    var site = siteMap[key] = container.newGadgetSite(elem);
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
    var info = rpcArgs.gs.getActiveSiteHolder().getGadgetInfo(),
        key = getFrameKey(info.url, info.modulePrefs.features['locked-domain']);

    var name, scriptSite = siteMap[key];
    if (scriptSite) {
      name = scriptSite.getActiveSiteHolder().getIframeId();
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
   * Respond to the ON_BEFORE_RENDER event by creating a script frame for the
   * loading gadget, but only if we need one.
   *
   * @param {!Object} metadata The gadget metadata
   */
  lifeCycleHandlers[osapi.container.CallbackType.ON_BEFORE_RENDER] = function(metadata) {
    var url = metadata.url;
    try {
      var feature = metadata.modulePrefs.features['shared-script-frame'];
      var ldFeature = metadata.modulePrefs.features['locked-domain'];
    } catch(e) {}
    if (feature) {
      createScriptFrame(url, feature, ldFeature);
    }
  };

  container.addGadgetLifecycleCallback('shared-script-frame-setup', lifeCycleHandlers);
  container.rpcRegister('get_script_frame_name', getScriptFrameName);
  return {};
});