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
    var txt = xobj.responseText;
    // remove unparseable cruft.
    // TODO: really remove this by eliminating it. It's not any real security
    //    to begin with, and we can solve this problem by using post requests
    //    and / or passing the url in the http headers.
    txt = txt.substr(UNPARSEABLE_CRUFT.length);
    var data = gadgets.json.parse(txt);
    data = data[url];
    var resp = {
     text: data.body,
     errors: []
    };
    switch (params.CONTENT_TYPE) {
      case "JSON":
      case "FEED":
        resp.data = gadgets.json.parse(resp.text);
        break;
     case "DOM":
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
      var newUrl = config.jsonProxyUrl.replace("%url%",
          encodeURIComponent(url));
      xhr.open(params.METHOD || "GET", newUrl, true);
      if (callback) {
        xhr.onreadystatechange = gadgets.util.makeClosure(null,
            processResponse, url, callback, params, xhr);
      }
      xhr.send(params.postData);
    },

    /**
     * Converts an input object into a URL-encoded data string.
     * (key=value&amp;...)
     *
     * @param {Object} fields The post fields you wish to encode
     * @return {String} The processed post data; this will include a trailing
     *    ampersand (&)
     *
     * @member gadgets.io
     */
    encodeValues : function (fields) {
      var buf = [];
      for (var i in fields) {
        buf.push(encodeURIComponent(i));
        buf.push("=");
        buf.push(encodeURIComponent(fields[i]));
        buf.push("&");
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
    },

    /**
     * Initializes fetchers
     *
     * @param {Object} configuration Configuration settings
     *     Required:
     *       - proxyUrl: The url for content proxy requests. Include %url%
     *           as a placeholder for the actual url.
     *       - jsonProxyUrl: The url for dynamic proxy requests. Include %url%
     *           as a placeholder for the actual url.
     * @private
     */
    init : function (configuration) {
      config = configuration;
      if (!config.proxyUrl || !config.jsonProxyUrl) {
        throw new Error("proxyUrl and jsonProxyUrl are required.");
      }
    }
  };
}();

// TODO: This can all be removed after the spec is published. This is only
// here to satisfy documentation requirements.

/**
 * @static
 * @class
 * Used by the
 * <a href="gadgets.io.html#makeRequest">
 * <code>gadgets.io.makeRequest()</code></a> method.
 * @name gadgets.io.RequestParameters
 */
gadgets.io.RequestParameters = {
  /**
   * The method to use when fetching content from the URL;
   * defaults to <code>MethodType.GET</a></code>.
   * Specified as a
   * <a href="gadgets.io.MethodType.html">MethodType</a>.
   *
   * @member gadgets.io.RequestParameters
   */
   METHOD : 'METHOD',

  /**
   * The type of content that lives at the URL;
   * defaults to <code>ContentType.HTML</code>.
   * Specified as a
   * <a href="gadgets.io.ContentType.html">
   * ContentType</a>.
   *
   * @member gadgets.io.RequestParameters
   */
  CONTENT_TYPE : "CONTENT_TYPE",

  /**
   * The data to send to the URL using the POST method;
   * defaults to null.
   * Specified as a <code>String</code>.
   *
   * @member gadgets.io.RequestParameters
   */
  POST_DATA : "POST_DATA",

  /**
   * The HTTP headers to send to the URL;
   * defaults to null.
   * Specified as a <code>Map.&lt;String,String&gt;</code>.
   *
   * @member gadgets.io.RequestParameters
   */
  HEADERS : "HEADERS",

  /**
   * The type of authentication to use when fetching the content;
   * defaults to <code>AuthorizationType.NONE</code>.
   * Specified as an
   * <a href="gadgets.io.AuthorizationType.html">
   * AuthorizationType</a>.
   *
   * @member gadgets.io.RequestParameters
   */
  AUTHORIZATION : 'AUTHORIZATION',


  /**
   * If the content is a feed, the number of entries to fetch;
   * defaults to 3.
   * Specified as a <code>Number</code>.
   *
   * @member gadgets.io.RequestParameters
   */
  NUM_ENTRIES : 'NUM_ENTRIES',

  /**
   * If the content is a feed, whether to fetch summaries for that feed;
   * defaults to false.
   * Specified as a <code>Boolean</code>.
   *
   * @member gadgets.io.RequestParameters
   */
  GET_SUMMARIES : 'GET_SUMMARIES'
};


/**
 * @static
 * @class
 * Used by
 * <a href="gadgets.io.RequestParameters.html">
 * RequestParameters</a>.
 * @name gadgets.io.MethodType
 */
gadgets.io.MethodType = {
  /**
   * The default type.
   * @member gadgets.io.MethodType
   */
  GET : 'GET',

  /**
   * Not supported by all containers.
   * @member gadgets.io.MethodType
   */
  POST : 'POST',

  /**
   * Not supported by all containers.
   * @member gadgets.io.MethodType
   */
  PUT : 'PUT',

  /**
   * Not supported by all containers.
   * @member gadgets.io.MethodType
   */
  DELETE : 'DELETE',

  /**
   * Not supported by all containers.
   * @member gadgets.io.MethodType
   */
  HEAD : 'HEAD'
};


/**
 * @static
 * @class
 * Used by
 * <a href="gadgets.io.RequestParameters.html">
 * RequestParameters</a>.
 * @name gadgets.io.ContentType
 */
gadgets.io.ContentType = {
  /**
   * Returns text; used for fetching HTML.
   * @member gadgets.io.ContentType
   */
  TEXT : 'TEXT',

  /**
   * Returns a DOM object; used for fetching XML.
   * @member gadgets.io.ContentType
   */
  DOM : 'DOM',

  /**
   * Returns a JSON object.
   * @member gadgets.io.ContentType
   */
  JSON : 'JSON',

  /**
   * Returns a JSON representation of a feed.
   * @member gadgets.io.ContentType
   */
  FEED : 'FEED'
};


/**
 * @static
 * @class
 * Used by
 * <a href="gadgets.io.RequestParameters.html">
 * RequestParameters</a>.
 * @name gadgets.io.AuthorizationType
 */
gadgets.io.AuthorizationType = {
  /**
   * No authorization.
   * @member gadgets.io.AuthorizationType
   */
  NONE : 'NONE',

  /**
   * The request will be signed by the container.
   * @member gadgets.io.AuthorizationType
   */
  SIGNED : 'SIGNED',

  /**
   * The container will use full authentication.
   * @member gadgets.io.AuthorizationType
   */
  AUTHENTICATED : 'AUTHENTICATED'
};
