/*
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

/*global ActiveXObject */

/**
 * @fileoverview This library provides a standard and convenient way to embed
 * Flash content into gadgets.
 */

/**
 * @class Embeds Flash content in gadgets.
 */
gadgets.flash = gadgets.flash || {};

/**
 * Detects Flash Player and its major version.
 * @return {number} The major version of Flash Player
 *                  or 0 if Flash is not supported.
 */
gadgets.flash.getMajorVersion = function() {
  var flashMajorVersion = 0;
  if (navigator.plugins && navigator.mimeTypes && navigator.mimeTypes.length) {
    // Flash detection for browsers using Netscape's plugin architecture
    var i = navigator.plugins['Shockwave Flash'];
    if (i && i['description']) {
      flashMajorVersion = parseInt(i['description'].match(/[0-9]+/)[0], 10);
    }
  } else {
    // Flash detection for IE
    // This is done by trying to create an ActiveX object with the name
    // "ShockwaveFlash.ShockwaveFlash.{majorVersion}".
    for (var version = 10; version > 0; version--) {
      try {
        var dummy = new ActiveXObject('ShockwaveFlash.ShockwaveFlash.' + version);
        return version;
      } catch (e) {
      }
    }
  }
  return flashMajorVersion;
};

/**
 * Used for unique IDs.
 * @type {number}
 * @private
 */
gadgets.flash.swfContainerId_ = 0;

/**
 * Injects a Flash file into the DOM tree.
 * @param {string} swfUrl SWF URL.
 * @param {string | Object} swfContainer The id or object reference of an
 *     existing html container element.
 * @param {number} swfVersion Minimal Flash Player version required.
 * @param {Object=} opt_params An optional object that may contain any valid html
 *     parameter. All attributes will be passed through to the flash movie on
 *     creation.
 * @return {boolean} Whether the function call completes successfully.
 */
gadgets.flash.embedFlash = function(swfUrl, swfContainer, swfVersion, opt_params) {
  switch (typeof swfContainer) {
    case 'string':
      swfContainer = document.getElementById(swfContainer);
    case 'object':
      if (swfContainer && (typeof swfContainer.innerHTML === 'string')) {
        break;
      }
    default:
      return false;
  }

  switch (typeof opt_params) {
    case 'undefined':
      opt_params = {};
    case 'object':
      break;
    default:
      return false;
  }

  if (swfUrl.indexOf('//') == 0) {
    swfUrl = document.location.protocol + swfUrl;
  }

  var ver = gadgets.flash.getMajorVersion();
  if (ver) {
    var swfVer = parseInt(swfVersion, 10);
    if (isNaN(swfVer)) {
      swfVer = 0;
    }
    if (ver >= swfVer) {
      // Set default size
      if (opt_params['width'] === void 0) {
        opt_params['width'] = '100%';
      }
      if (opt_params['height'] === void 0) {
        opt_params['height'] = '100%';
      }
      // Set the default "base" attribute
      if (typeof opt_params['base'] !== 'string') {
        var a = document.createElement('a');
        a.href = swfUrl;
        // Get the part up to the last slash
        opt_params['base'] = a.href.match(/^(.*\/)[^\/]*$/)[1];
      }
      // Set wmode to "opaque" if it's not defined. The default value
      // "window" is undesirable because browsers will render Flash
      // on top of other html elements.
      if (typeof opt_params['wmode'] !== 'string') {
        opt_params['wmode'] = 'opaque';
      }
      while (!opt_params['id']) {
        var newId = 'swfContainer' + gadgets.flash.swfContainerId_++;
        if (!document.getElementById(newId)) {
          opt_params['id'] = newId;
        }
      }
      // Prepare flash object
      var flashObj;
      if (navigator.plugins && navigator.mimeTypes &&
          navigator.mimeTypes.length) {
        // Use <embed> tag for Netscape and Mozilla browsers
        opt_params['type'] = 'application/x-shockwave-flash';
        opt_params['src'] = swfUrl;

        flashObj = document.createElement('embed');
        for (var prop in opt_params) {
          if (!/^swf_/.test(prop) && !/___$/.test(prop)) {
            flashObj.setAttribute(prop, opt_params[prop]);
          }
        }
        // Inject flash object
        swfContainer.innerHTML = '';
        swfContainer.appendChild(flashObj);
        return true;
      } else {
        // Use <object> tag for IE
        // For some odd reason IE demands that innerHTML be used to set <param>
        // values; they're otherwise ignored. As such, we need to be careful
        // what values we accept in opt_params to avoid it being possible to
        // use this HTML generation for nefarious purposes.
        var propIsHtmlSafe = function(val) {
          return !/["<>]/.test(val);
        };

        opt_params['movie'] = swfUrl;
        var attr = {
          'width': opt_params['width'],
          'height': opt_params['height'],
          'classid': 'clsid:D27CDB6E-AE6D-11CF-96B8-444553540000'
        };
        if (opt_params['id']) {
          attr['id'] = opt_params['id'];
        }

        var html = '<object';
        for (var attrProp in attr) {
          if (!/___$/.test(attrProp) &&
              propIsHtmlSafe(attrProp) &&
              propIsHtmlSafe(attr[attrProp])) {
            html += ' ' + attrProp + '="' + attr[attrProp] + '"';
          }
        }
        html += '>';

        for (var paramsProp in opt_params) {
          var param = document.createElement('param');
          if (!/^swf_/.test(paramsProp) &&
              !attr[paramsProp] &&
              !/___$/.test(paramsProp) &&
              propIsHtmlSafe(paramsProp) &&
              propIsHtmlSafe(opt_params[paramsProp])) {
            html += '<param name="' + paramsProp + '" value="'
                 + opt_params[paramsProp] + '" />';
          }
        }
        html += '</object>';
      }
      swfContainer.innerHTML = html;
      return true;
    }
  }
  return false;
};

/**
 * Injects a cached Flash file into the DOM tree.
 * Accepts the same parameters as gadgets.flash.embedFlash does.
 * @param {string} swfUrl SWF URL.
 * @param {string | Object} swfContainer The id or object reference of an
 *     existing html container element.
 * @param {number} swfVersion Minimal Flash Player version required.
 * @param {Object=} opt_params An optional object that may contain any valid html
 *     parameter. All attributes will be passed through to the flash movie on
 *     creation.
 * @return {boolean} Whether the function call completes successfully.
 *
 * @member gadgets.flash
 */
gadgets.flash.embedCachedFlash = function(swfUrl, swfContainer, swfVersion, opt_params) {
  var url = gadgets.io.getProxyUrl(swfUrl, { rewriteMime: 'application/x-shockwave-flash' });
  return gadgets.flash.embedFlash(url, swfContainer, swfVersion, opt_params);
};

/**
 * iGoogle compatible way to get flash version.
 * @deprecated use gadgets.flash.getMajorVersion instead.
 * @see gadgets.flash.getMajorVersion
 */
var _IG_GetFlashMajorVersion = gadgets.flash.getMajorVersion;


/**
 * iGoogle compatible way to embed flash
 * @deprecated use gadgets.flash.embedFlash instead.
 * @see gadgets.flash.embedFlash
 */
var _IG_EmbedFlash = function(swfUrl, swfContainer, opt_params) {
  return gadgets.flash.embedFlash(swfUrl, swfContainer, opt_params['swf_version'],
      opt_params);
};

/**
 * iGoogle compatible way to embed cached flash
 * @deprecated use gadgets.flash.embedCachedFlash() instead.
 * @see gadgets.flash.embedCachedFlash
 */
var _IG_EmbedCachedFlash = function(swfUrl, swfContainer, opt_params) {
  return gadgets.flash.embedCachedFlash(swfUrl, swfContainer, opt_params['swf_version'],
      opt_params);
};

