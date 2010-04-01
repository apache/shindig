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
 * @fileoverview Emulate XMLHttpRequest using gadgets.io.makeRequest.
 * 
 * This is not a complete implementation of XMLHttpRequest:
 * - synchronous send() is unsupported;
 * - the callback function will not get full header information, as makeRequest
 *   only provides the Set-Cookie and Location headers.
 */

shindig.xhrwrapper = shindig.xhrwrapper || {};

(function () {

  // Save the browser's XMLHttpRequest and ActiveXObject constructors.
  var RealXMLHttpRequest = window.XMLHttpRequest;
  var RealActiveXObject = window.ActiveXObject;

  /**
   * Creates a real XMLHttpRequest object.
   * 
   * This function is to be used by code that needs access to the browser's
   * XMLHttpRequest functionality, such as the code that implements
   * gadgets.io.makeRequest itself.
   * 
   * @return {Object|undefined} A XMLHttpRequest object, if one could
   *     be created.
   */
  shindig.xhrwrapper.createXHR = function() {
    var activeXIdents =
        ['MSXML2.XMLHTTP.6.0', 'MSXML2.XMLHTTP.3.0', 'MSXML2.XMLHTTP'];
    if (typeof RealActiveXObject != 'undefined') {
      for (var i = 0; i < activeXIdents.length; i++) {
        try {
          return new RealActiveXObject(activeXIdents[i]);
        } catch (x) {
          // do nothing, if none exists we'll do something later
        }
      }
    }
    if (typeof RealXMLHttpRequest != 'undefined') {
      return new RealXMLHttpRequest();
    }
    return undefined;
  };

  /**
   * @class XhrWrapper class.
   * 
   * @constructor
   * @description Implements the XMLHttpRequest interface, using
   *     gadgets.io.makeRequest to make the actual network accesses.
   */
  shindig.xhrwrapper.XhrWrapper = function() {
    this.config_ = gadgets.config.get('shindig.xhrwrapper');

    // XMLHttpRequest event listeners
    this.onreadystatechange = null;

    // XMLHttpRequest properties
    this.readyState = 0;
  };

  /**
   * Aborts the request if it has already been sent.
   */
  shindig.xhrwrapper.XhrWrapper.prototype.abort = function() {
    this.aborted_ = true;
  };

  /**
   * Returns all response headers as a string.
   * 
   * @return {string?} The text of all response headers, or null if no response
   *     has been received.
   */
  shindig.xhrwrapper.XhrWrapper.prototype.getAllResponseHeaders = function() {
    if (!this.responseHeaders_) {
      return null;
    }

    var allHeaders = '';
    for (var header in this.responseHeaders_) {
      allHeaders += header + ': ' + this.responseHeaders_[header] + '\n';
    }
    return allHeaders;
  };

  /**
   * Returns the value of a particular response header.
   * 
   * @param {string} The name of the header to return.
   * @return {string?} The value of the header, or null if no response has
   *     been received or the header doesn't exist in the response.
   */
  shindig.xhrwrapper.XhrWrapper.prototype.getResponseHeader = function(header) {
    if (!this.responseHeaders_) {
      return null;
    }

    var value = this.responseHeaders_[header.toLowerCase()];
    return value ? value : null;
  };

  /**
   * Initializes a request.
   * 
   * @param {string} method The HTTP method to use ('POST' or 'GET').
   * @param {string} url The URL to which to send the request.
   * @param {boolean=} opt_async Whether to perform the operation
   *     asynchronously (defaults to true). Synchronous operations are not
   *     supported, so it must presently be omitted or true.
   */
  shindig.xhrwrapper.XhrWrapper.prototype.open =
      function(method, url, opt_async) {
    this.method_ = method;
    this.url_ = new Url(url);
    this.aborted_ = false;
    this.requestHeaders_ = {};
    this.responseHeaders_ = {};

    this.baseUrl_ = new Url(this.config_.contentUrl);

    this.fixRequestUrl_();

    if (!this.baseUrl_.hasSameOrigin(this.url_)) {
      throw new Error('A gadget at ' + this.config_.contentUrl +
                      ' tried to access ' + url + ' via XMLHttpRequest.');
    }

    if (opt_async === false) {
      throw new Error('xhrwrapper does not support synchronous XHR.');
    }

    // XMLHttpRequest properties
    this.multipart = false;
    this.readyState = 1;
    this.responseText = null;
    this.responseXML = null;
    this.status = 0;
    this.statusText = null;
  };

  /**
   * Sends the request.
   * 
   * @param {string=} opt_data The data used to populate the body of a POST
   *     request.
   */
  shindig.xhrwrapper.XhrWrapper.prototype.send = function(opt_data) {
    this.aborted_ = false;
    var that = this;
    var params = {};
    params[gadgets.io.RequestParameters.METHOD] = this.method_;
    params[gadgets.io.RequestParameters.HEADERS] = this.requestHeaders_;
    params[gadgets.io.RequestParameters.POST_DATA] = opt_data;
    gadgets.io.makeRequest(this.url_.toString(),
                           function(response) { that.callback_(response); },
                           params);
  };

  /**
   * Sets the value of an HTTP request header.
   * 
   * @param {string} header The name of the header to set.
   * @param {string} value The value for the header.
   */
  shindig.xhrwrapper.XhrWrapper.prototype.setRequestHeader =
      function(header, value) {
    this.requestHeaders_[header] = value;
  };

  /**
   * Processes the results from makeRequest and calls onreadystatechange.
   * 
   * @param {Object} response The response from makeRequest.
   * @private
   */
  shindig.xhrwrapper.XhrWrapper.prototype.callback_ = function(response) {
    if (this.aborted_) {
      return;
    }
    this.readyState = 4;
    this.responseHeaders_ = response.headers;
    this.responseText = response.text;
    try {
      this.responseXML = opensocial.xmlutil.parseXML(response.text);
    } catch (x) {
      this.responseXML = null;
    }
    this.status = response.rc;
    if (response.errors) {
      this.statusText = response.errors[0];
    }
    if (this.onreadystatechange) {
      var event = {};
      event.type = 'readystatechange';
      event.srcElement = this;
      event.target = this;
      this.onreadystatechange(event);
    }
  };

  /**
   * Points the request URL to the correct server.
   * 
   * If the URL is pointing to the gadget server, this function assumes the
   * gadget's author wanted to point to the gadget contents location and
   * changes it so that it points to the right place.
   * 
   * For example, if the gadget is rendered in https://shindig/gadgets/ifr
   * and the gadget's contents are at http://foo.com/bar/baz.html:
   * 
   * - foo.xml gets turned into http://foo.com/bar/foo.xml
   * - /foo/bar.xml gets turned into http://foo.com/foo/bar.xml
   * - //foo.com/bar.xml gets turned into http://foo.com/bar.xml
   * - http://foo.com/bar.xml is untouched
   * - https://shindig/bar.xml is turned into http://foo.com/bar.xml
   * - https://shindig/gadgets/bar.xml is turned into
   *     http://foo.com/bar/bar.xml
   */
  shindig.xhrwrapper.XhrWrapper.prototype.fixRequestUrl_ = function() {
    this.url_.fullyQualify(this.baseUrl_);
    var loc = new Url(window.location.href);
    if (this.url_.hasSameOrigin(loc)) {
      this.url_.schema = this.baseUrl_.schema;
      this.url_.authority = this.baseUrl_.authority;
      var pathLen = loc.path.length;
      if (this.url_.path.substr(0, pathLen) == loc.path) {
        this.url_.path = this.baseUrl_.path + this.url_.path.substr(pathLen);
      }
    }
  };

  /**
   * @class A class for processing URLs.
   * 
   * @constructor
   * @description Pries apart the components of a URL, so it can be sliced
   * and diced and combined with other URLs as needed.
   */
  function Url(url) {
    this.schema = "";
    this.authority = "";
    this.path = "";
    this.filename = "";
    this.query = "";
    this.fragment = "";

    var parse = url;
    var sharp = parse.indexOf('#');
    if (sharp != -1) {
      this.fragment = parse.substr(sharp);
      parse = parse.substr(0, sharp);
    }
    var question = parse.indexOf('?');
    if (question != -1) {
      this.query = parse.substr(question);
      parse = parse.substr(0, question);
    }
    var doubleSlash = parse.indexOf('//');
    if (doubleSlash != -1) {
      this.schema = parse.substr(0, doubleSlash);
      parse = parse.substr(doubleSlash + 2);
      var firstSlash = parse.indexOf('/');
      if (firstSlash != -1) {
        this.authority = parse.substr(0, firstSlash);
        parse = parse.substr(firstSlash);
      } else {
        this.authority = parse;
        parse = '';
      }
    }
    var lastSlash = parse.lastIndexOf('/');
    if (lastSlash != -1) {
      this.path = parse.substr(0, lastSlash + 1);
      parse = parse.substr(lastSlash + 1);
    }
    this.filename = parse;
  };

  /**
   * Checks that a URL has the same origin as this URL.
   *
   * Two URLs have the same origin if they point to the same schema, server
   * and port.
   *
   * @param {Url} other The URL to compare to this URL.
   * @return {boolean} Whether the URLs have the same origin.
   */
  Url.prototype.hasSameOrigin = function(other) {
    return this.schema == other.schema && this.authority == other.authority;
  };

  /**
   * Fully qualifies this URL if it is relative, using a given base URL.
   * 
   * @param {Url} base The base URL.
   */
  Url.prototype.fullyQualify = function(base) {
    if (this.schema == '') {
      this.schema = base.schema;
    }
    if (this.authority == '') {
      this.authority = base.authority;
      if (this.path == '' || this.path[0] != '/') {
        this.path = base.path + this.path;
      }
    }
  };

  /**
   * Returns a readable representation of the URL.
   * 
   * @return {string} A readable URL.
   */
  Url.prototype.toString = function() {
    var url = "";
    if (this.schema) {
      url += this.schema;
    }
    if (this.authority) {
      url += '//' + this.authority;
    }
    if (this.path) {
      url += this.path;
    }
    if (this.filename) {
      url += this.filename;
    }
    if (this.query) {
      url += this.query;
    }
    if (this.fragment) {
      url += this.fragment;
    }
    return url;
  };

  /**
   * Acts as a drop-in replacement for IE's ActiveXObject.
   * @param {string} className The name of the class to create.
   */
  function ActiveXObjectReplacement(className) {
    var obj;
    if (typeof className == 'string' &&
        (className.substr(0, 14).toLowerCase() == 'msxml2.xmlhttp' ||
         className.toLowerCase() == 'microsoft.xmlhttp')) {
      obj = new shindig.xhrwrapper.XhrWrapper();
    } else {
      obj = new RealActiveXObject(className);
    }
    for (var f in obj) {
      this[f] = obj[f];
    }
  };

  // Replace the browser's XMLHttpRequest and ActiveXObject constructors with
  // xhrwrapper's.
  if (window.XMLHttpRequest) {
    window.XMLHttpRequest = shindig.xhrwrapper.XhrWrapper;
  }
  if (window.ActiveXObject) {
    window.ActiveXObject = ActiveXObjectReplacement;
  }

  var config = {
    contentUrl: gadgets.config.NonEmptyStringValidator
  };
  gadgets.config.register('shindig.xhrwrapper', config);

})();

