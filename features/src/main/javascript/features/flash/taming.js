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

/**
 * @class
 * Tame and expose core gadgets.flash.* API to cajoled gadgets
 */
var tamings___ = tamings___ || [];
tamings___.push(function(imports) {
  ___.tamesTo(gadgets.flash.embedFlash, function () {
    var cleanse = (function () {
      // Gets a fresh Array and Object constructor that 
      // doesn't have the caja properties on it.  This is 
      // important for passing objects across the boundary 
      // to flash code.
      var ifr = document.createElement("iframe");
      ifr.width = 1; ifr.height = 1; ifr.border = 0;
      document.body.appendChild(ifr);
      var A = ifr.contentWindow.Array;
      var O = ifr.contentWindow.Object;
      document.body.removeChild(ifr);
    
      var c = function(obj) {
        var t = typeof obj, i;
        if (t === 'number' || t === 'boolean' || t === 'string') { 
            return obj; 
        }
        if (t === 'object') {
          var o;
          if (obj instanceof Array) { o = new A; }
          else if (obj instanceof Object) { o = new O; }
          for (i in obj) {
            if (/__$/.test(i)) { continue; }
            o[i] = c(obj[i]);
          }
          return o;
        }
        return (void 0);
      };
      return c;
    })();

    return ___.frozenFunc(function tamedEmbedFlash(
           swfUrl, 
           swfContainer,
           swfVersion, 
           opt_params) {
      // Check that swfContainer is a wrapped node
      if (typeof swfContainer === "string") {
        // This assumes that there's only one gadget in the frame.
        var $v = ___.getNewModuleHandler().getImports().$v;
        swfContainer = $v.cm(
            $v.ro("document"), 
            "getElementById", 
            [swfContainer]);
      } else if (typeof swfContainer !== "object" || !swfContainer.node___) {
        return false;
      }

      // Generate a random number for use as the channel name
      // for communication between the bridge and the contained
      // flash object.
      // TODO: Use true randomness.
      var channel = "_flash" + ("" + Math.random()).substring(2);

      // Strip out allowNetworking and allowScriptAccess, 
      //   as well as any caja-specific properties.
      var new_params = {};
      for (i in opt_params) {
        if (i.match(/___$/)) { continue; }
        var ilc = i.toLowerCase();
        if (ilc === "allownetworking" || ilc === "allowscriptaccess") {
          continue;
        }
        var topi = typeof opt_params[i];
        if (topi !== "string" && topi !== "number") { continue; }
        new_params[i] = opt_params[i];
      }
      new_params.allowNetworking = "never";
      new_params.allowScriptAccess = "none";
      if (!new_params.flashVars) { new_params.flashVars = ""; }
      new_params.flashVars += "&channel=" + channel;

      // Load the flash.
      gadgets.flash.embedFlash(swfUrl, swfContainer.node___, 10, new_params);

      if (bridge___.channels) {
        // If the bridge hasn't loaded, queue up the channel names
        // for later registration
        bridge___.channels.push(channel);
      } else {
        // Otherwise, register the channel immediately.
        bridge___.registerChannel(channel);
      }

      // Return the ability to talk to the boxed swf.
      return ___.primFreeze({
        callSWF: (function (channel) { 
          return ___.func(function (methodName, argv) {
              return bridge___.callSWF(
                  "" + channel, 
                  "" + methodName, 
                  cleanse(argv));
            });
        })(channel)
      });
    });
  });

  var d = document.createElement('div');
  d.appendChild(document.createTextNode("bridge"));
  document.body.appendChild(d);
  
  gadgets.flash.embedFlash(
      "/gadgets/files/container/Bridge.swf", 
      d,
      10,
      {
        allowNetworking: "always",
        allowScriptAccess: "all",
        width: 0,
        height: 0,
        flashvars: "logging=true"
      });
  bridge___ = d.childNodes[0];
  bridge___.channels = [];
  
  callJS = function (functionName, argv) {
    // This assumes that there's a single gadget in the frame.
    var $v = ___.getNewModuleHandler().getImports().$v;
    return $v.cf($v.ro(functionName), [argv]);
  };
      
  onFlashBridgeReady = function () {
    var len = bridge___.channels.length;
    for(var i = 0; i < len; ++i) {
      bridge___.registerChannel(bridge___.channels[i]);
    }
    delete bridge___.channels;
    var outers = ___.getNewModuleHandler().getImports().$v.getOuters();
    if (outers.onFlashBridgeReady) {
      callJS("onFlashBridgeReady");
    }
  };
});
