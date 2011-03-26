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

  var fire = function(globalScope) {
    for (var tamer in tamings___) {
      if (tamings___.hasOwnProperty(tamer)) {
        tamings___[tamer].call(___['USELESS'], globalScope);
      }
    }
  }
  function whitelistCtors(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        ___.markCtor(schema[0][schema[1]] /* func */, schema[2] /* parent */, schema[1] /* name */);
      } else {
        gadgets.warn('Error taming constructor: ' + schema[0] + '.' + schema[1]);
      }
    }
  }
  function whitelistFuncs(schemas) {
    var length = schemas.length;
    for (var i = 0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        ___.markFunc(schema[0][schema[1]], schema[1]);
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
        ___.markTameAsXo4a(schema[0].prototype[schema[1]]);
      } else {
        gadgets.warn('Error taming method: ' + schema[0] + '.' + schema[1]);
      }
    }
  }

  function enable() {
    var imports = {};
    imports['outers'] = imports;

    var gadgetRoot = document.getElementById('cajoled-output');
    gadgetRoot['className'] = 'g___';
    document.body.appendChild(gadgetRoot);

    imports['htmlEmitter___'] = new HtmlEmitter(gadgetRoot);
    imports['onerror'] = ___.markFunc(function(x){
        gadgets.warn(x);
        return true; 
    });
    ___.setLogFunc(imports['onerror']);

    attachDocumentStub('-g___', uriCallback, imports, gadgetRoot);

    imports['window'] = {};
    // Use these imports
    for (i in imports) {
      imports['window'][i] = imports[i];
    }
    imports = imports['window'];
    imports['domitaTrace___'] = 1;
    imports['handleSet___'] = void 0;

    // fire(imports);
    ___.grantRead(imports, 'gadgets');
    ___.getNewModuleHandler().setImports(___.whitelistAll(imports));
  }
  return {
    enable: enable,
    whitelistCtors: whitelistCtors,
    whitelistFuncs: whitelistFuncs,
    whitelistMeths: whitelistMeths
  };
})();

