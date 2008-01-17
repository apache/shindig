/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var gadgets = gadgets || {};

/**
 * Provides remote content retrieval facilities. Available to every gadget.
 */
gadgets.io = function() {
  /**
   * Holds configuration-related data such as proxy urls.
   */
  var config = {};

  /**
   * Internal facility to create an xhr request.
   */
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
  }

  var UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";

  /**
   * Handles XHR callback processing.
   *
   * @param {String} url
   * @param {Function} callback
   * @param {Object} params
   * @param {Object} xobj
   */
  function processResponse(url, callback, params, xobj) {
    if (xobj.readyState !== 4) {
      return;
    }
    var txt = xobj.responseText;
    // remove unparseable cruft.
    // TODO: really remove this by eliminating it. It's not any real security
    //    to begin with, and we can solve this problem by using post requests
    //    and / or passing the url in the http headers.
    txt = txt.substr(UNPARSEABLE_CRUFT.length);
    // TODO: safe JSON parser.
    var data = gadgets.JSON.parse(txt);
    data = data[url];
    var resp = {
     text: data.body,
     errors: []
    };
    switch (params.contentType) {
      case "json":
        // TODO: safe JSON parser.
        resp.data = gadgets.JSON.parse(resp.text);
        break;
     case "dom":
      var dom;
      if (window.ActiveXObject) {
        dom = new ActiveXObject("Microsoft.XMLDOM");
        dom.async = false;
        dom.validateOnParse = false;
        dom.resolveExternals = false;
        dom.loadXML(resp.text);
      } else {
        var parser = new DOMParser();
        dom = parser.parseFromString(resp.text, "text/xml");
      }
      resp.data = dom;
      break;
    default:
      resp.data = resp.text;
      break;
   }
   callback(resp);
  }

  /**
   * Retrieves the content at the specified url.
   *
   * @param {String} url The url to fetch.
   * @param {Function} callback Invoked when the request completes. The
   *     response object will be passed in as a parameter.
   * @param {Object} opt_params Optional parameters. May be modified.
   *
   * <pre>
   * gadgets.IO.makeRequest(url, fn, {type: gadgets.IO.ContentType.JSON});
   * </pre>
   */
  function makeRequest(url, callback, opt_params) {
    var xhr = makeXhr();
    var params = opt_params || {};
    var newUrl = config.jsonProxyUrl.replace("%url%", encodeURIComponent(url));
    xhr.open(params.postData ? "POST" : "GET", newUrl, true);
    if (callback) {
      xhr.onreadystatechange = gadgets.util.makeClosure(null, processResponse,
                                                        url,
                                                        callback,
                                                        params,
                                                        xhr);
    }
    xhr.send(params.postData);
  }

  /**
   * Converts an input object into a url encoded data string (key=value&...)
   *
   * @param {Object} fields The post fields you wish to encode
   * @return {String} The processed post data. This will include a trialing
   *    ampersand (&).
   */
  function encodeValues(fields) {
    var buf = [];
    for (var i in fields) {
      buf.push(encodeURIComponent(i));
      buf.push("=");
      buf.push(encodeURIComponent(fields[i]));
      buf.push("&");
    }
    return buf.join("");
  }

  /**
   * @param {String} url The url to get the proxy url for.
   * @return {String} The proxied version of the url.
   */
  function getProxyUrl(url) {
    return config.proxyUrl.replace("%url%", encodeURIComponent(url));
  }

  /**
   * Initializes fetchers
   * @param {Object} configuration Configuration settings.
   *     Required:
   *       - proxyUrl: The url for content proxy requests. Include %url%
   *           as a placeholder for the actual url.
   *       - jsonProxyUrl: The url for dynamic proxy requests. Include %url%
   *           as a placeholder for the actual url.
   */
  function init(configuration) {
    config = configuration;
    if (!config.proxyUrl || !config.jsonProxyUrl) {
      throw new Error("proxyUrl and jsonProxyUrl are required.");
    }
  }

  return {
    makeRequest: makeRequest,
    getProxyUrl: getProxyUrl,
    encodeValues: encodeValues,
    init: init
  };
}();
