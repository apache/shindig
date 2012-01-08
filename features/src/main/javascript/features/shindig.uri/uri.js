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
 * @fileoverview Pure JS code for processing Uris.
 *
 * Unlike Java Shindig and other code, these Uris are mutable. While
 * this introduces some challenges, ultimately the confusion introduced
 * by passing around a Uri versus a UriBuilder in an untyped language
 * is too great.
 *
 * The class only implements core methods associated with Uris -
 * essentially the minimum required for various helper routines. Lazy evalution
 * of query and fragment params is chosen to avoid undue performance hit.
 * Further, only set operations are provided for query/fragment params,
 * in order to keep the API relatively small, yet sufficiently flexible. Values set to
 * null are equivalent to being removed, for instance.
 *
 * Limitations include, but are not limited to:
 * + Multiple params with the same key not supported via set APIs.
 * + Full RPC-compliant parsing not supported. A "highly useful" subset is impl'd.
 * + Helper methods are all provided in the shindig.uri.full feature.
 * + Query and fragment do not contain their preceding ? and # chars.
 *
 * Example usage:
 * var uri = shindig.uri("http://www.apache.org");
 * uri.setPath("random.xml");
 * alert(uri.toString());  // Emits "http://www.apache.org/random.xml"
 * var other =  // resolve() provided in shindig.uri.full
 *     shindig.uri("http://www.other.com/foo").resolve("/bar").setQP({ hi: "bye" });
 * alert(other);  // Emits "http://other.com/bar?hi=bye"
 */
shindig.uri = (function() {
  var PARSE_REGEX = new RegExp('^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?');

  return function(opt_in) {
    var schema_ = '';
    var authority_ = '';
    var path_ = '';
    var query_ = '';
    var qparms_ = null;
    var fragment_ = '';
    var fparms_ = null;
    var unesc = window.decodeURIComponent ? decodeURIComponent : unescape;
    var esc = window.encodeURIComponent ? encodeURIComponent : escape;
    var bundle = null;

    function parseFrom(url) {
      if (url.match(PARSE_REGEX) === null) {
        throw 'Malformed URL: ' + url;
      }
      schema_ = RegExp.$1;
      authority_ = RegExp.$2;
      path_ = RegExp.$3;
      query_ = RegExp.$4;
      fragment_ = RegExp.$5;
    }

    function serializeParams(params) {
      var str = [];
      for (var i = 0, j = params.length; i < j; ++i) {
        var key = params[i][0];
        var val = params[i][1];
        if (typeof val == 'undefined') {
          continue;
        }
        str.push(esc(key) + (val !== null ? '=' + esc(val) : ''));
      }
      return str.join('&');
    }

    function getQuery() {
      if (qparms_) {
        query_ = serializeParams(qparms_);
        qparms_ = null;
      }
      return query_;
    }

    function getFragment() {
      if (fparms_) {
        fragment_ = serializeParams(fparms_);
        fparms_ = null;
      }
      return fragment_;
    }

    function getQP(key) {
      qparms_ = qparms_ || parseParams(query_);
      return getParam(qparms_, key);
    }

    function getFP(key) {
      fparms_ = fparms_ || parseParams(fragment_);
      return getParam(fparms_, key);
    }

    function setQP(argOne, argTwo) {
      qparms_ = setParams(qparms_ || parseParams(query_), argOne, argTwo);
      return bundle;
    }

    function setFP(argOne, argTwo) {
      fparms_ = setParams(fparms_ || parseParams(fragment_), argOne, argTwo);
      return bundle;
    }

    function getOrigin() {
      return [
        schema_,
        schema_ !== '' ? ':' : '',
        authority_ !== '' ? '//' : '',
        authority_
      ].join('');
    }

    /**
     * Returns a readable representation of the URL.
     *
     * @return {string} A readable URL.
     */
    function toString() {
      var query = getQuery();
      var fragment = getFragment();
      return [
        getOrigin(),
        path_,
        query !== '' ? '?' : '',
        query,
        fragment !== '' ? '#' : '',
        fragment
      ].join('');
    }

    function parseParams(str) {
      var params = [];
      var pairs = str.split('&');
      for (var i = 0, j = pairs.length; i < j; ++i) {
        var kv = pairs[i].split('=');
        var key = kv.shift();
        var value = null;
        if (kv.length > 0) {
          value = kv.join('').replace(/\+/g, ' ');
        }
        params.push([key, value != null ? unesc(value) : null]);
      }
      return params;
    }

    function getParam(pmap, key) {
      for (var i = 0, j = pmap.length; i < j; ++i) {
        if (pmap[i][0] == key) {
          return pmap[i][1];
        }
      }
      // undefined = no key set
      // vs. null = no value set and "" = value is empty
      return undefined;
    }

    function setParams(pset, argOne, argTwo) {
      // Assume by default that we're setting by map (full replace).
      var newParams = argOne;
      if (typeof argOne === 'string') {
        // N/V set (single param override)
        newParams = {};
        newParams[argOne] = argTwo;
      }
      for (var key in newParams) {
        var found = false;
        for (var i = 0, j = pset.length; !found && i < j; ++i) {
          if (pset[i][0] == key) {
            pset[i][1] = newParams[key];
            found = true;
          }
        }
        if (!found) {
          pset.push([key, newParams[key]]);
        }
      }
      return pset;
    }

    function stripPrefix(str, pfx) {
      str = str || '';
      if (str[0] === pfx) {
        str = str.substr(pfx.length);
      }
      return str;
    }

    // CONSTRUCTOR
    if (typeof opt_in === 'object' &&
        typeof opt_in.toString === 'function') {
      // Assume it's another shindig.uri, or something that can be parsed from one.
      parseFrom(opt_in.toString());
    } else if (opt_in) {
      parseFrom(opt_in);
    }

    bundle = {
      // Getters
      getSchema: function() { return schema_; },
      getAuthority: function() { return authority_; },
      getOrigin: getOrigin,
      getPath: function() { return path_; },
      getQuery: getQuery,
      getFragment: getFragment,
      getQP: getQP,
      getFP: getFP,

      // Setters
      setSchema: function(schema) { schema_ = schema; return bundle; },
      setAuthority: function(authority) { authority_ = authority; return bundle; },
      setPath: function(path) { if (typeof path !== 'undefined' && path != null) { path_ = (path[0] === '/' ? '' : '/') + path; } return bundle; },
      setQuery: function(query) { qparms_ = null; query_ = stripPrefix(query, '?'); return bundle; },
      setFragment: function(fragment) { fparms_ = null; fragment_ = stripPrefix(fragment, '#'); return bundle; },
      setQP: setQP,
      setFP: setFP,
      setExistingP: function(key, val) {
        if (typeof(getQP(key, val)) != 'undefined') {
          setQP(key, val);
        }
        if (typeof(getFP(key, val)) != 'undefined') {
          setFP(key, val);
        }
        return bundle;
      },

      // Core utility methods.
      toString: toString
    };

    return bundle;
  }
})();
