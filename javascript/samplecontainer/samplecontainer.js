/**
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
 * @Functions for the samplecontainer
 */

/**
 * Public Shindig namespace with samplecontainer object
 */

var shindig = shindig || {};
shindig.samplecontainer = {};

/**
 * Hide our functions and variables from other javascript
 */

(function(){


  /**
   * Private Variables
  */

  var parentUrl = document.location.href;
  var baseUrl = parentUrl.substring(0, parentUrl.indexOf('samplecontainer.html'))

  // TODO: This is gross, it needs to use the config just like the gadget js does
  var socialDataPath = document.location.protocol + "//" + document.location.host
    + "/social/rest/samplecontainer/";

  var gadgetUrl = baseUrl + 'examples/SocialHelloWorld.xml';
  var gadgetUrlCookie = 'sampleContainerGadgetUrl';

  var stateFileUrl = baseUrl + '../sampledata/canonicaldb.json';
  var stateFileUrlCookie = 'sampleContainerStateFileUrl';

  var useCaja;
  var useCache;
  var usePermissive;
  var doEvil;
  var gadget;

  var viewerId = "john.doe";
  var ownerId = "john.doe";

  /**
  * Public Variables
  */

  /**
  * Private Functions
  */

  function generateSecureToken() {
    // TODO: Use a less silly mechanism of mapping a gadget URL to an appid
    var appId = 0;
    for (var i = 0; i < gadgetUrl.length; i++) {
      appId += gadgetUrl.charCodeAt(i);
    }
    var fields = [ownerId, viewerId, appId, "shindig", gadgetUrl, "0"];
    for (var i = 0; i < fields.length; i++) {
      // escape each field individually, for metachars in URL
      fields[i] = escape(fields[i]);
    }
    return fields.join(":");
  }

  SampleContainerGadget = function(opt_params) {
    gadgets.IfrGadget.call(this, opt_params);
  };

  SampleContainerGadget.inherits(gadgets.IfrGadget);

  SampleContainerGadget.prototype.getAdditionalParams = function() {
    var params = ''

    if (useCaja) {
      params += "&caja=1";
    }
    if (usePermissive) {
      params += "&usepermissive=1";
    }
    return params;
  };
  
  gadgets.container.gadgetClass = SampleContainerGadget;

  function setEvilBit() {
    sendRequestToServer('setevilness/' + doEvil, 'POST');
  };

  function reloadStateFile(opt_callback) {
    sendRequestToServer('setstate', 'POST',
        {"fileurl" : stateFileUrl},
        opt_callback);
  };

  function sendRequestToServer(url, method, opt_postParams, opt_callback) {
    // TODO: Should re-use the jsoncontainer code somehow
    opt_postParams = opt_postParams || {};

    var makeRequestParams = {
      "CONTENT_TYPE" : "JSON",
      "METHOD" : method,
      "POST_DATA" : encodeValues(opt_postParams)};

    makeRequest(socialDataPath + url + "?st=" + gadget.secureToken,
      function(data) {
        data = data.data;
        if (opt_callback) {
            opt_callback(data);
        }
      },
      makeRequestParams
    );
  };


  // Xhr stuff that is copied from io.js.
  // TODO: We should really get rid of the duplication
  function makeXhr() {
    if (window.XMLHttpRequest) {
      return new XMLHttpRequest();
    } else if (window.ActiveXObject) {
      var x = new ActiveXObject("Msxml2.XMLHTTP");
      if (!x) {
          x = new ActiveXObject("Microsoft.XMLHTTP");
      }
      return x;
    }
  };

  function processResponse(url, callback, params, xobj) {
    if (xobj.readyState !== 4) {
      return;
    }
    if (xobj.status !== 200) {
      callback({errors : ["Error " + xobj.status] });
      return;
    }
    var txt = xobj.responseText;

    // We are using eval directly here because the outer response comes from a
    // trusted source, and json parsing is slow in IE.
    var data = txt ? eval("(" + txt + ")") : "";
    var resp = {
      data: data
    };

    callback(resp);
  };

  function makeRequest(url, callback, params) {
    var xhr = makeXhr();
    xhr.open(params.METHOD, url, true);
    xhr.onreadystatechange = gadgets.util.makeClosure(
        null, processResponse, url, callback, params, xhr);
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    if (params.METHOD == 'POST') {
      xhr.send(params.POST_DATA);
    } else {
      xhr.send();
    }
  };

  function encodeValues(fields, opt_noEscaping) {
    var escape = !opt_noEscaping;

    var buf = [];
    var first = false;
    for (var i in fields) if (fields.hasOwnProperty(i)) {
      if (!first) {
          first = true;
      } else {
          buf.push("&");
      }
      buf.push(escape ? encodeURIComponent(i) : i);
      buf.push("=");
      buf.push(escape ? encodeURIComponent(fields[i]) : fields[i]);
    }
    return buf.join("");
  };
  
  /**
  * Public Functions
  */  

  shindig.samplecontainer.initGadget = function() {
    // Fetch cookies
    var cookieGadgetUrl = decodeURIComponent(goog.net.cookies.get(gadgetUrlCookie));
    if (cookieGadgetUrl && cookieGadgetUrl != "undefined") {
      gadgetUrl = cookieGadgetUrl;
    }

    var cookieStateFileUrl = decodeURIComponent(goog.net.cookies.get(stateFileUrlCookie));
    if (cookieStateFileUrl && cookieStateFileUrl != "undefined") {
      stateFileUrl = cookieStateFileUrl;
    }

    // Setup state file
    document.getElementById("stateFileUrl").value = stateFileUrl;

    // Render gadget
    document.getElementById("gadgetUrl").value = gadgetUrl;

    gadget = gadgets.container.createGadget({'specUrl': gadgetUrl});;
    gadget.setServerBase('../../');
  
    // Viewer and Owner
    document.getElementById("viewerId").value = viewerId;
    document.getElementById("ownerId").value = ownerId;
    gadget.secureToken = escape(generateSecureToken());

    gadgets.container.addGadget(gadget);
    gadgets.container.layoutManager.setGadgetChromeIds(['gadget-chrome']);
    reloadStateFile(function() {
      gadgets.container.renderGadgets();
    });
  };

  shindig.samplecontainer.unpackFormState = function() {
    useCaja = document.getElementById("useCajaCheckbox").checked;
    useCache = document.getElementById("useCacheCheckbox").checked;
    usePermissive = document.getElementById("usePermissiveCheckbox").checked;
    doEvil = document.getElementById("doEvilCheckbox").checked;
  }

  shindig.samplecontainer.changeGadgetUrl = function() {
    shindig.samplecontainer.unpackFormState();
    gadgets.container.nocache_ = useCache ? 0 : 1;

    setEvilBit();

    stateFileUrl = document.getElementById("stateFileUrl").value;
    goog.net.cookies.set(stateFileUrlCookie, encodeURIComponent(stateFileUrl));

    viewerId = document.getElementById("viewerId").value;
    ownerId = document.getElementById("ownerId").value;
    gadgetUrl = document.getElementById("gadgetUrl").value;

    gadget.secureToken = escape(generateSecureToken());
    gadget.specUrl = gadgetUrl;
    goog.net.cookies.set(gadgetUrlCookie, encodeURIComponent(gadgetUrl));

    reloadStateFile(function() {
      gadgets.container.renderGadgets();
    });
  };

  shindig.samplecontainer.dumpStateFile = function() {
    sendRequestToServer('dumpstate', 'GET', null,
      function(data) {
        if (!data) {
          alert("Could not dump the current state.");
        }
        document.getElementById('gadgetState').innerHTML
          = gadgets.json.stringify(data);
      }
    );
  };

})();