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

/**
 * @fileoverview Caja is a whitelisting javascript sanitizing rewriter.
 * This file tames the APIs that are exposed to a gadget.
 * Currently limited to one cajoled gadget per ifr.
 */

caja___ = (function() {

  // Rewrites all uris in a cajoled gadget
  var uriCallback = {
    rewrite: function rewrite(uri, mimeTypes) {
      uri = String(uri);
      if (/^#/.test(uri)) {
        // Allow references to anchors within the gadget
        return '#' + encodeURIComponent(decodeURIComponent(uri.substring(1)));
      } else if (/^\/[^\/]/.test(uri)) {
        // Unqualified uris aren't resolved in a useful way in gadgets, so
        // this isn't a real case, but some of the samples use relative
        // uris for images, and it looks odd if they don't work cajoled.
        return gadgets.io.getProxyUrl(
          location.protocol + '//' + location.host + uri);
      } else {
        // Proxy all other dynamically constructed urls
        return gadgets.io.getProxyUrl(uri);
      }
    }
  };

  function getTameGlobal() {
    return caja.iframe.contentWindow;
  }

  function getJSON() {
    return caja.iframe.contentWindow.JSON;
  }

  function getUseless() {
    return caja.USELESS;
  }

  function markFunction(func, name) {
    return caja.markFunction(func, name);
  }

  function tame(obj) {
    return caja.tame(obj);
  }

  function tamesTo(feral, tame) {
    return caja.tamesTo(feral, tame);
  }

  function untame(obj) {
    return caja.untame(obj);
  }

  function whitelistCtors(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        caja.markCtor(
            schema[0][schema[1]] /* func */,
            schema[2] /* parent */,
            schema[1] /* name */);
      } else {
        gadgets.warn('Error taming constructor: ' +
            schema[0] + '.' + schema[1]);
      }
    }
  }

  function whitelistFuncs(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        caja.markFunction(schema[0][schema[1]], schema[1]);
      } else {
        gadgets.warn('Error taming function: ' + schema[0] + '.' + schema[1]);
      }
    }
  }

  function whitelistMeths(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0].prototype[schema[1]] == 'function') {
        caja.grantMethod(schema[0].prototype, schema[1]);
      } else {
        gadgets.warn('Error taming method: ' + schema[0] + '.' + schema[1]);
      }
    }
  }

  function whitelistProps(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      caja.grantRead(schemas[0], schemas[1]);
    }
  }

  function start(script, debug) {
    caja.initialize({
      server: '/gadgets',
      resources: '/gadgets/js',
      // TODO(felix8a): make debug==false work
      debug: true
    });
    var gadgetBody = document.getElementById('caja_innerContainer___');
    caja.load(gadgetBody, uriCallback, function (frame) {
      var api = makeApi();
      frame.api(api).cajoled(void 0, script)
        .run(function (result) {
          gadgets.util.runOnLoadHandlers();
        });
    });
  }

  function makeApi() {
    var api = {};
    for (var tamer in tamings___) {
      if (tamings___.hasOwnProperty(tamer)) {
        tamings___[tamer].call(void 0, api);
      }
    }
    api.gadgets = caja.tame(window.gadgets);
    api.opensocial = caja.tame(window.opensocial);
    api.osapi = caja.tame(window.osapi);
    api.onerror = caja.tame(caja.markFunction(
        function (msg, source, line) {
          gadgets.log([msg, source, line]);
        }));
    return api;
  }

  return {
    getJSON: getJSON,
    getTameGlobal: getTameGlobal,
    getUseless: getUseless,
    markFunction: markFunction,
    start: start,
    tame: tame,
    tamesTo: tamesTo,
    untame: untame,
    whitelistCtors: whitelistCtors,
    whitelistFuncs: whitelistFuncs,
    whitelistMeths: whitelistMeths,
    whitelistProps: whitelistProps
  };
})();

