/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * @fileoverview Implements the global implicit data context for containers.
 *
 * TODO(davidbyttow):
 *   Variable substitution in markup.
 *   Support cross-cutting predicates (page, sort, search).
 *   URL parameter support.
 */

window.opensocial.data = window.opensocial.data || {};
var gadgets = window.gadgets;

/**
 * @type {Object} The namespace declaration for this file.
 */
os.data = window.opensocial.data;

/**
 * @type {Object} Map of currently pending requests.
 * @private
 */
os.data.requests_ = {};

/**
 * Registers a tag as a data request handler.
 * @param {string} name Prefixed tag name.
 * @param {Function} handler Method to call when this tag is invoked.
 *
 * TODO: Store these tag handlers separately from the ones for UI tags.
 * TODO: Formalize the callback interface.
 */
os.data.registerRequestHandler = function(name, handler) {
  var tagParts = name.split(':');
  var ns = os.getNamespace(tagParts[0]);
  if (!ns) {
    throw 'Namespace ' + tagParts[0] + ' is undefined.';
  } else if (ns[tagParts[1]]) {
    throw 'Request handler ' + tagParts[1] + ' is already defined.';
  }

  ns[tagParts[1]] = handler;
};

/**
 * Parses XML data and constructs the pending request list.
 * @param {Element|string} xmlData A DOM element or string containing XML.
 */
os.data.loadRequests = function(xmlData) {
  if (typeof(xmlData) != 'string') {
    var node = xmlData;
    xmlData = node.value || node.innerHTML;
  }

  xmlData = os.prepareTemplateXML_(xmlData);
  var doc = os.parseXML_(xmlData);

  // Find the <root> node (skip DOCTYPE).
  var node = doc.firstChild;
  while (node.nodeType != DOM_ELEMENT_NODE) {
    node = node.nextSibling;
  }

  os.data.processDataNode_(node);
};

/**
 * Fires off all requests that are currently pending.
 */
os.data.executeRequests = function() {
  for (var nsName in os.data.requests_) {
    var requestList = os.data.requests_[nsName];

    if (nsName == 'os') {
      // Special case, these can be batched.
      var req = opensocial.newDataRequest();

      // Build the requests.
      for (var key in requestList) {
        var tag = requestList[key];
        var callback = os.getCustomTag('os', tag.tagParts[1]);
        req.add(callback(req, tag), key);
      }

      // Send off requests and hang them on the DataContext once loaded.
      req.send(function(response) {
        if (!response || response.hadError()) {
          throw 'Unexpected error with OpenSocial data request.';
        } else {
          for (var key in requestList) {
            var responseItem = response.get(key);
            if (!responseItem) {
              throw 'Request for ' + key + ' could not be loaded.';
            } else if (responseItem.hadError()) {
              throw 'Response error(' + responseItem.getErrorCode() + '): ' +
                  responseItem.getErrorMessage();
            } else {
              os.data.DataContext.putDataSet(key, response);
            }
          }
        }
      });
    } else {
      // Standard processing, callback's responsibility to invoke putDataSet.
      for (var key in requestList) {
        var tag = requestList[key];
        var callback = os.getCustomTag(nsName, tag.tagParts[1]);
        callback(key, tag);
      }
    }
  }

  // Remove all pending requests.
  delete os.data.requests_;
  os.data.requests_ = {};
};

/**
 * Processes a data request node for data sets.
 * @param {Node} node The node to process.
 * @private
 */
os.data.processDataNode_ = function(node) {
  for (var child = node.firstChild; child; child = child.nextSibling) {
    if (child.nodeType == DOM_ELEMENT_NODE) {
      if (child.tagName == 'os:dataSet') {
        os.data.processDataSet_(child);
      }
    }
  }
};

/**
 * Parses and builds the request for the given data set node.
 * @param {Node} node The data set node containing the request.
 * @private
 */
os.data.processDataSet_ = function(node) {
  for (var child = node.firstChild; child; child = child.nextSibling) {
    if (child.nodeType == DOM_ELEMENT_NODE) {
      var key = node.getAttribute('key');
      var tag = new os.data.DataRequestTag(child);
      if (!os.data.requests_[tag.tagParts[0]]) {
        os.data.requests_[tag.tagParts[0]] = {};
      }
      var requestList = os.data.requests_[tag.tagParts[0]];
      requestList[key] = tag;
      break; // Only one request allowed per data set.
    }
  }
};

/**
 * @type {Array.<Object>} Array of registered listeners.
 * @private
 */
os.data.listeners_ = [];


/**
 * Checks if the passed in listener has had all of its keys loaded already.
 * @param {Object} listener The listener to check.
 * @return {boolean} This listener is ready to be fired.
 * @private
 */
os.data.checkListener_ = function(listener) {
  for (var key in listener.keys) {
    if (os.data.DataContext[key] == null) {
      return false;
    }
  }
  return true;
};

/**
 * A DataRequestTag is a wrapper for an XML tag specifying a DataSet request.
 * This object can be used to access attributes of the request - performing
 * necessary variable substitutions from the global DataContext. An instance of
 * this object will be passed to the Data Request Handler so it can obtain its
 * parameters through it.
 * @constructor
 * @param {Element} xmlNode An XML DOM node representing the request.
 *
 * TODO: Support accessing nested nodes for things like predicates.
 */
os.data.DataRequestTag = function(xmlNode) {
  this.tagName = xmlNode.tagName;
  this.tagParts = this.tagName.split(":");
  this.attributes = {};

  for (var i = 0; i < xmlNode.attributes.length; ++i) {
    var name = xmlNode.attributes[i].nodeName;
    if (name) {
      var value = xmlNode.getAttribute(name);
      if (name && value) {
        this.attributes[name] = value;
      }
    }
  }
};

/**
 * Checks if an attribute has been specified for this tag.
 * @param {string} name The attribute name
 * @return {boolean} The attribute is set.
 */
os.data.DataRequestTag.prototype.hasAttribute = function(name) {
  return !!this.attributes[name];
};

/**
 * Returns the value of a specified attribute. If the attribute includes
 * variable substitutions, they will be evaluated against the DataContext and
 * the result returned.
 *
 * @param {string} name The attribute name to look up.
 * @return {Object} The result of evaluation.
 */
os.data.DataRequestTag.prototype.getAttribute = function(name) {
  var attrExpression = this.attributes[name];
  if (!attrExpression) {
    return attrExpression;
  }
  // TODO(levik): Don't do this every time - cache the result.
  var expression = os.parseAttribute_(attrExpression);
  if (!expression) {
    return attrExpression;
  }
  return os.data.DataContext.evalExpression(expression);
};

/**
 * @type {Object} Global DataContext to contain requested data sets.
 */
os.data.DataContext = {};

/**
 * Registers a callback listener for a given set of keys.
 * @param {string|Array.<string>} keys Key or set of keys to listen on.
 * @param {Function} callback Function to call when a listener is fired.
 * TODO(davidbyttow): Should return a value that can later be used to return
 *     a value.
 */
os.data.DataContext.registerListener = function(keys, callback) {
  var listener = {};
  listener.keys = {};

  if (typeof(keys) == 'object') {
    for (var i in keys) {
      listener.keys[keys[i]] = true;
    }
  } else {
    listener.keys[keys] = true;
  }

  listener.callback = callback;
  os.data.listeners_.push(listener);

  // Check to see if this one should fire immediately.
  if (os.data.checkListener_(listener)) {
    window.setTimeout(function() {
      listener.callback()
    }, 1);
  }
};

/**
 * Retrieve a data set for a given key.
 * @param {string} key Key for the requested data set.
 * @return {Object} The data set object.
 */
os.data.DataContext.getDataSet = function(key) {
  return os.data.DataContext[key];
};

/**
 * Puts a data set into the global DataContext object. Fires listeners
 * if they are satisfied by the associated key being inserted.
 * @param {string} key The key to associate with this object.
 * @param {Object} obj The data set object.
 */
os.data.DataContext.putDataSet = function(key, obj) {
  // TODO(davidbyttow): This is ugly, but since we're attaching directly,
  // this is necessary.
  // At least share this code with what is in resolveOpenSocialIdentifier.
  var data = obj;
  if (typeof(data) == "undefined" || data === null) {
    return;
  }
  if (obj.get) {
    var responseItem = obj.get(key);
    if (responseItem && responseItem.getData) {
      data = responseItem.getData();
      data = data.array_ || data;
    }
  }

  os.data.DataContext[key] = data;
  os.data.fireCallbacks_(key);
};

/***
 * Takes a key and a data handler. Invokes the handler and sets the result
 * into the specified key.
 * @param {string} key The key to map the result to.
 * @param {Function} handler The handler function that has the signature
 *   void function(Function). The function passed must be invoked and passed
 *   the data result when it is fetched.
 */
os.data.DataContext.putDataResult = function(key, handler) {
  handler(function(obj) {
    os.data.DataContext.putDataSet(key, obj);
  });
}

/**
 * A shared JsEvalContext for evaluating expressions.
 */
os.data.DataContext.evalContext_ = os.createContext(os.data.DataContext);


/**
 * Evaluates a JS expression against the DataContext.
 * @param {string} expr The expression to evaluate.
 * @return {Object} The result of evaluation.
 */
os.data.DataContext.evalExpression = function(expr) {
  return os.data.DataContext.evalContext_.evalExpression(expr);
};


/**
 * Scans all active listeners and fires off any callbacks that inserting this
 * key satisfies.
 * @param {string} key The key that was recently updated.
 * @private
 */
os.data.fireCallbacks_ = function(key) {
  for (var i = 0; i < os.data.listeners_.length; ++i) {
    var listener = os.data.listeners_[i];
    if (listener.keys[key] != null) {
      if (os.data.checkListener_(listener)) {
        listener.callback();
      }
    }
  }
};

/**
 * Creates a closure that will render the a template into an element with
 * optional data.
 * @param {Object} template The template object to use.
 * @param {Element} element The DOM element to inject the template into.
 * @param {Object} opt_data Optional data to be used as the context.
 * @return {Function} The constructed closure.
 * TODO(davidbyttow): Move this into util.js
 */
os.createRenderClosure = function(template, element, opt_data) {
  var closure = function() {
    template.renderInto(element, opt_data);
  };
  return closure;
};

/**
 * Creates a closure that will hide a DOM element.
 * @param {Element} element The DOM element to inject the template into.
 * @return {Function} The constructed closure.
 * TODO(davidbyttow): Move this into util.js
 */
os.createHideElementClosure = function(element) {
  var closure = function() {
    displayNone(element);
  };
  return closure;
};

/**
 * Creates a closure (handler) that will perform a POST request to the
 * provided URL with some JSON data.
 * @param {string} url The URL to fetch from.
 * @return {Object} opt_postData Optional JSON data to post.
 * TODO(davidbyttow): Add an XHR implementation.
 */
os.data.newJsonPostRequestHandler = function(url, opt_postData) {
  var handler = function(callback) {
    // TODO(davidbyttow): This probably shouldn't even be registered
    // if gadgets does not exist.
    if (!gadgets) {
      return;
    }
    var params = {};
    params[gadgets.io.RequestParameters.METHOD] =
        gadgets.io.MethodType.POST;
    if (opt_postData) {
      params[gadgets.io.RequestParameters.POST_DATA] = opt_postData;
    }
    params[gadgets.io.RequestParameters.CONTENT_TYPE] =
        gadgets.io.ContentType.JSON;
    gadgets.io.makeRequest(url, function(obj) {
      callback(obj.data);
    }, params);
  };
  return handler;
};

/**
 * Anonymous function defines OpenSocial specific requests.
 * Automatically called when this file is loaded.
 */
(os.data.defineRequests_ = function() {
  os.data.registerRequestHandler('os:personRequest', function(req, tag) {
    return req.newFetchPersonRequest(tag.getAttribute('id'));
  });

  os.data.registerRequestHandler('os:peopleRequest', function(req, tag) {
    // TODO(davidbyttow): Support multiple Id's.
    return req.newFetchPeopleRequest(tag.getAttribute('group'));
  });

  // TODO(davidbyttow): Move this to gadgets.js.
  // TODO(davidbyttow): Add an XHR implementation.
  if (gadgets) {
    os.createNamespace('json', 'http://json.org');
    os.data.registerRequestHandler('json:makeRequest', function(key, tag) {
      var url = tag.getAttribute('url');
      var params = {};
      params[gadgets.io.RequestParameters.CONTENT_TYPE] =
          gadgets.io.ContentType.JSON;
      params[gadgets.io.RequestParameters.METHOD] =
          gadgets.io.MethodType.GET;
      gadgets.io.makeRequest(url, function(obj) {
          os.data.DataContext.putDataSet(key, obj.data);
      }, params);
    });
  }

})();

/**
 * Pre-populate a Data Set based on application's URL parameters.
 */
(os.data.populateParams_ = function() {
  var params = {};
  var queryString = document.location.search;
  if (queryString) {
    queryString = queryString.substring(1);
    var queryParts = queryString.split("&");
    for (var i = 0; i < queryParts.length; i++) {
      var paramParts = queryParts[i].split("=");
      // TODO: handle multiple as arrays.
      params[paramParts[0]] = paramParts[1];
    }
  }
  os.data.DataContext.putDataSet("params", params);
})();
