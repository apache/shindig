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
 */

caja___ = (function() {
  // currently limited to one cajoled gadget per ifr
  var guestFrame;
  var tamingFrame;
  var tame___;      // tame___ === tamingFrame.contentWindow.___
  var pendingScript;
  var guestFrameReady = false;
  var tamingFrameReady = false;

  // URI policy: Rewrites all uris in a cajoled gadget
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

  function fire(globalScope) {
    for (var tamer in tamings___) {
      if (tamings___.hasOwnProperty(tamer)) {
        tamings___[tamer].call(tame___.USELESS, globalScope);
      }
    }
  }

  function getTameGlobal() {
    return tamingFrame.contentWindow;
  }

  function getUseless() {
    return tame___ && tame___.USELESS;
  }

  function markTameAsFunction(func, name) {
    return tame___.markTameAsFunction(func, name);
  }

  function tame(obj) {
    return tame___.tame(obj);
  }

  function tamesTo(feral, tame) {
    tame___.tamesTo(feral, tame);
  }

  function untame(obj) {
    return tame___.untame(obj);
  }

  function whitelistCtors(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        tame___.markTameAsCtor(
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
        tame___.markTameAsFunction(schema[0][schema[1]], schema[1]);
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
        tame___.grantTameAsMethod(schema[0], schema[1]);
      } else {
        gadgets.warn('Error taming method: ' + schema[0] + '.' + schema[1]);
      }
    }
  }

  function whitelistProps(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      tame___.grantTameAsRead(schemas[0], schemas[1]);
    }
  }

  function makeFrame(id) {
    var frame = document.createElement('iframe');
    frame.style.display = 'none';
    frame.id = id;
    frame.height = 0;
    frame.width = 0;
    document.body.appendChild(frame);
    return frame;
  }

  function start(script, debug) {
    // feral object marker for directConstructor
    window.Object.FERAL_FRAME_OBJECT___ = window.Object;

    pendingScript = script;
    debug = debug ? '?debug=1' : '';

    guestFrame = makeFrame('caja-guest-frame');
    var gdoc = guestFrame.contentWindow.document;
    gdoc.write('<!doctype html>\n');
    gdoc.write('<html><head>\n');
    gdoc.write('<script>var cajaIframeDone___ = function(){};<\/script>\n');
    gdoc.write('<script src="js/caja-guest-frame' + debug + '"><\/script>\n');
    gdoc.write('<script>\n');
    gdoc.write('  function cajaRun(script) {\n');
    gdoc.write('    eval(script);\n');
    gdoc.write('    parent.gadgets.util.runOnLoadHandlers();\n');
    gdoc.write('  }\n');
    gdoc.write('  parent.caja___.loadedGuestFrame();\n');
    gdoc.write('<\/script>\n');
    gdoc.write('</head></html>');
    gdoc.close();

    tamingFrame = makeFrame('caja-taming-frame');
    var tdoc = tamingFrame.contentWindow.document;
    tdoc.write('<!doctype html>\n');
    tdoc.write('<html><head>\n');
    tdoc.write('<script>var cajaIframeDone___ = function(){};<\/script>\n');
    tdoc.write('<script src="js/caja-taming-frame' + debug + '"><\/script>\n');
    tdoc.write('<script>parent.caja___.loadedTamingFrame();<\/script>\n');
    tdoc.write('</head></html>');
    tdoc.close();
  }

  function loadedTamingFrame() {
    tamingFrameReady = true;
    readyTrigger();
  }

  function loadedGuestFrame() {
    guestFrameReady = true;
    readyTrigger();
  }

  function readyTrigger() {
    if (guestFrameReady && tamingFrameReady) {
      window.setTimeout(ready, 0);
    }
  }

  function ready() {
    var guestWin = guestFrame.contentWindow;
    var imports = guestWin.___.getNewModuleHandler().getImports();

    var gadgetBody = document.getElementById('caja_innerContainer___');

    var tameWin = tamingFrame.contentWindow;
    tame___ = tameWin.___;

    // TODO(felix8a): pass pseudo-window location
    tameWin.attachDocumentStub('-g___', uriCallback, imports, gadgetBody);
    imports.htmlEmitter___ =
        new tameWin.HtmlEmitter(gadgetBody, imports.document);

    imports.onerror = tame___.tame(
        tame___.markTameAsFunction(function(msg, source, line) {
            gadgets.log([msg, source, line]);
        }));

    fire(imports);

    // these are in globals.js
    imports.gadgets = tame___.tame(window.gadgets);
    imports.opensocial = tame___.tame(window.opensocial);
    imports.osapi = tame___.tame(window.osapi);

    tame___.whitelistAll(imports);

    guestWin.plugin_dispatchEvent___ = tameWin.plugin_dispatchEvent___;
    guestWin.plugin_dispatchToHandler___ = tameWin.plugin_dispatchToHandler___;
    guestWin.___.getNewModuleHandler().setImports(imports);
    guestWin.___.useDebugSymbols = function() {};

    guestWin.cajaRun(pendingScript);
  }

  return {
    getTameGlobal: getTameGlobal,
    getUseless: getUseless,
    loadedGuestFrame: loadedGuestFrame,
    loadedTamingFrame: loadedTamingFrame,
    markTameAsFunction: markTameAsFunction,
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

