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

var gadgets = gadgets || {};

/**
 * @fileoverview Provides remote content retrieval facilities.
 *     Available to every gadget.
 */

/**
 * @static
 * @class Provides remote content retrieval functions.
 * @name gadgets.io
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
    if (xobj.status !== 200) {
      // TODO Need to work on standardizing errors
      callback({errors : ["Error " + xobj.status] });
      return;
    }
    var txt = xobj.responseText;
    // remove unparseable cruft.
    // TODO: really remove this by eliminating it. It's not any real security
    //    to begin with, and we can solve this problem by using post requests
    //    and / or passing the url in the http headers.
    txt = txt.substr(UNPARSEABLE_CRUFT.length);
    // We are using eval directly here because the outer response comes from a
    // trusted source, and json parsing is slow in IE.
    var data = eval("(" + txt + ")");
    data = data[url];
    var resp = {
     text: data.body,
     errors: []
    };
    switch (params.CONTENT_TYPE) {
      case "JSON":
      case "FEED":
        resp.data = gadgets.json.parse(resp.text);
        if (!resp.data) {
          resp.errors.push("failed to parse JSON");
          resp.data = null;
        }
        break;
      case "DOM":
        var dom;
        if (window.ActiveXObject) {
          dom = new ActiveXObject("Microsoft.XMLDOM");
          dom.async = false;
          dom.validateOnParse = false;
          dom.resolveExternals = false;
          if (!dom.loadXML(resp.text)) {
            resp.errors.push("failed to parse XML");
          } else {
            resp.data = dom;
          }
        } else {
          var parser = new DOMParser();
          dom = parser.parseFromString(resp.text, "text/xml");
          if ("parsererror" === dom.documentElement.nodeName) {
            resp.errors.push("failed to parse XML");
          } else {
            resp.data = dom;
          }
        }
        break;
      default:
        resp.data = resp.text;
        break;
    }
    callback(resp);
  }

  /**
   * @param {Object} configuration Configuration settings
   * @private
   */
  function init (configuration) {
    config = configuration["core.io"];
  }

  var requiredConfig = {
    proxyUrl: new gadgets.config.RegExValidator(/.*%url%.*/),
    jsonProxyUrl: gadgets.config.NonEmptyStringValidator
  };
  gadgets.config.register("core.io", requiredConfig, init);

  return /** @scope gadgets.io */ {
    /**
     * Fetches content from the provided URL and feeds that content into the
     * callback function.
     *
     * Example:
     * <pre>
     * gadgets.io.makeRequest(url, fn,
     *    {contentType: gadgets.io.ContentType.FEED});
     * </pre>
     *
     * @param {String} url The URL where the content is located
     * @param {Function} callback The function to call with the data from the
     *     URL once it is fetched
     * @param {Map.&lt;gadgets.io.RequestParameters, Object&gt;} opt_params
     *     Additional
     *     <a href="gadgets.io.RequestParameters.html">parameters</a>
     *     to pass to the request
     *
     * @member gadgets.io
     */
    makeRequest : function (url, callback, opt_params) {
      // TODO: This method also needs to respect all members of
      // gadgets.io.RequestParameters, and validate them.
      var xhr = makeXhr();
      var params = opt_params || {};

      xhr.open("POST", config.jsonProxyUrl, true);
      if (callback) {
        xhr.onreadystatechange = gadgets.util.makeClosure(
            null, processResponse, url, callback, params, xhr);
      }
      // We always send a POST request; we just hide the details.
      xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');

      // Check if authorization is requested
      var auth, st;
      if (params.AUTHORIZATION && params.AUTHORIZATION !== "NONE") {
        auth = params.AUTHORIZATION.toLowerCase();
        st = gadgets.util.getUrlParameters().st;
      }

      var headers = params.HEADERS || {};
      if (params.METHOD === "POST" && !headers["Content-Type"]) {
        headers["Content-Type"] = "application/x-www-form-urlencoded";
      }

      var postData = {
        url: url,
        httpMethod : params.METHOD || "GET",
        headers: gadgets.io.encodeValues(headers, false),
        postData : params.POST_DATA || "",
        authz : auth || "",
        st : st || ""
      };
      xhr.send(gadgets.io.encodeValues(postData));
    },

    /**
     * Converts an input object into a URL-encoded data string.
     * (key=value&amp;...)
     *
     * @param {Object} fields The post fields you wish to encode
     * @param {Boolean} opt_noEscaping An optional parameter specifying whether
     *     to turn off escaping of the parameters. Defaults to false.
     * @return {String} The processed post data in www-form-urlencoded format.
     *
     * @member gadgets.io
     */
    encodeValues : function (fields, opt_noEscaping) {
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
    },

    /**
     * Gets the proxy version of the passed-in URL.
     *
     * @param {String} url The URL to get the proxy URL for
     * @return {String} The proxied version of the URL
     *
     * @member gadgets.io
     */
    getProxyUrl : function (url) {
      return config.proxyUrl.replace("%url%", encodeURIComponent(url));
    }
  };
}();

gadgets.io.RequestParameters = gadgets.util.makeEnum([
  "METHOD",
  "CONTENT_TYPE",
  "POST_DATA",
  "HEADERS",
  "AUTHORIZATION",
  "NUM_ENTRIES",
  "GET_SUMMARIES"
]);

// PUT, DELETE, and HEAD not supported currently.
gadgets.io.MethodType = gadgets.util.makeEnum([
  "GET", "POST", "PUT", "DELETE", "HEAD"
]);

gadgets.io.ContentType = gadgets.util.makeEnum([
  "TEXT", "DOM", "JSON", "FEED"
]);

gadgets.io.AuthorizationType = gadgets.util.makeEnum([
  "NONE", "SIGNED", "AUTHENTICATED"
]);