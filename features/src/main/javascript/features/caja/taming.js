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
  var tamingFrame;
  var guestFrame;
  var pendingScript;

  // URI policy: Rewrites all uris in a cajoled gadget
  var uriCallback = {
    rewrite: function rewrite(uri, mimeTypes) {
      uri = String(uri);
      // Allow references to anchors within the gadget
      if (/^#/.test(uri)) {
        return '#' + encodeURIComponent(decodeURIComponent(uri.substring(1)));
      } else {
        // Proxy all other dynamically constructed urls
        return gadgets.io.getProxyUrl(uri);
      }
    }
  };

  function fire(globalScope) {
    var USELESS = tamingFrame.contentWindow.___.USELESS;
    for (var tamer in tamings___) {
      if (tamings___.hasOwnProperty(tamer)) {
        tamings___[tamer].call(USELESS, globalScope);
      }
    }
  }

  function grantTameAsRead(obj, prop) {
    tamingFrame.contentWindow.___.grantTameAsRead(obj, prop);
  }

  function tamesTo(feral, tame) {
    tamingFrame.contentWindow.___.tamesTo(feral, tame);
  }

  function whitelistCtors(schemas) {
    var ___ = tamingFrame.contentWindow.___;
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        ___.markTameAsCtor(
            schema[0][schema[1]] /* func */,
            schema[2] /* parent */,
            schema[1] /* name */);
      } else {
        gadgets.warn('Error taming constructor: '
                     + schema[0] + '.' + schema[1]);
      }
    }
  }

  function whitelistFuncs(schemas) {
    var ___ = tamingFrame.contentWindow.___;
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        ___.markTameAsFunction(schema[0][schema[1]], schema[1]);
      } else {
        gadgets.warn('Error taming function: ' + schema[0] + '.' + schema[1]);
      }
    }
  }

  function whitelistMeths(schemas) {
    var ___ = tamingFrame.contentWindow.___;
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0].prototype[schema[1]] == 'function') {
        ___.grantTameAsMethod(schema[0], schema[1]);
      } else {
        gadgets.warn('Error taming method: ' + schema[0] + '.' + schema[1]);
      }
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

  function start(script) {
    // guestFrame must be created before loadedTamingFrame fires
    guestFrame = makeFrame('caja-guest-frame');
    var gdoc = guestFrame.contentWindow.document;
    gdoc.write('<html><head>\n');
    gdoc.write('<script>var cajaIframeDone___ = function(){};<'+'/script>\n');
    gdoc.write('<script src="js/caja-guest-frame"><'+'/script>\n');

    tamingFrame = makeFrame('caja-taming-frame');
    var tdoc = tamingFrame.contentWindow.document;
    tdoc.write('<html><head>\n');
    tdoc.write('<script>var cajaIframeDone___ = function(){};<'+'/script>\n');
    tdoc.write('<script src="js/caja-taming-frame"><'+'/script>\n');
    tdoc.write('<script>parent.caja___.loadedTamingFrame();<'+'/script>\n');
    tdoc.write('</head></html>');
    tdoc.close();

    pendingScript = script;

    // feral object marker for directConstructor
    window.Object.FERAL_FRAME_OBJECT___ = window.Object;
  }

  function loadedTamingFrame() {
    var gdoc = guestFrame.contentWindow.document;
    gdoc.write('<script>parent.caja___.loadedGuestFrame();<'+'/script>\n');
  }

  function loadedGuestFrame() {
    var guestWin = guestFrame.contentWindow;
    var imports = guestWin.___.getNewModuleHandler().getImports();

    var gadgetBody = document.getElementById('caja_innerContainer___');

    var tamingWin = tamingFrame.contentWindow;
    caja___.tameWin = tamingWin;
    // TODO(felix8a): pass pseudo-window location
    tamingWin.attachDocumentStub(
        '-g___', uriCallback, imports, gadgetBody);
    imports.htmlEmitter___ =
        new tamingWin.HtmlEmitter(gadgetBody, imports.document);

    imports.onerror = tamingWin.___.tame(
        tamingWin.___.markTameAsFunction(function (msg, source, line) {
            gadgets.log([msg, source, line]);
        }));

    fire(imports);

    // TODO(felix8a): move these to definition
    imports.gadgets = tamingWin.___.tame(window.gadgets);
    imports.opensocial = tamingWin.___.tame(window.opensocial);
    imports.osapi = tamingWin.___.tame(window.osapi);

    tamingWin.___.whitelistAll(imports);

    guestWin.plugin_dispatchEvent___ =
        tamingWin.plugin_dispatchEvent___;
    guestWin.plugin_dispatchToHandler___ =
        tamingWin.plugin_dispatchToHandler___;
    guestWin.___.getNewModuleHandler().setImports(imports);
    guestWin.___.useDebugSymbols = function(){};

    var gdoc = guestWin.document;
    gdoc.write('<script>\n');
    gdoc.write(pendingScript);
    gdoc.write('<'+'/script>\n');
    gdoc.write('<script>\n');
    gdoc.write('parent.gadgets.util.runOnLoadHandlers();\n');
    gdoc.write('<'+'/script>\n');
    gdoc.write('</head></html>');
    gdoc.close();
  }

  return {
    grantTameAsRead: grantTameAsRead,
    loadedGuestFrame: loadedGuestFrame,
    loadedTamingFrame: loadedTamingFrame,
    start: start,
    tameWin: null,
    tamesTo: tamesTo,
    whitelistCtors: whitelistCtors,
    whitelistFuncs: whitelistFuncs,
    whitelistMeths: whitelistMeths
  };
})();

