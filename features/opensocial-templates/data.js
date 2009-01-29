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
 * TODO:
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
 * @type {string} The key attribute constant.
 */
os.data.ATTR_KEY = "key";

/**
 * @type {string} The type of script tags that contain data markup.
 */
os.data.SCRIPT_TYPE = "text/os-data";

/**
 * A RequestDescriptor is a wrapper for an XML tag specifying a data request.
 * This object can be used to access attributes of the request - performing
 * necessary variable substitutions from the global DataContext. An instance of
 * this object will be passed to the Data Request Handler so it can obtain its
 * parameters through it.
 * @constructor
 * @param {Element} xmlNode An XML DOM node representing the request.
 */
os.data.RequestDescriptor = function(xmlNode) {
  this.tagName = xmlNode.tagName;
  this.tagParts = this.tagName.split(":");
  this.attributes = {};

  // Flag to indicate that this request depends on other requests.
  this.dependencies = false;

  for (var i = 0; i < xmlNode.attributes.length; ++i) {
    var name = xmlNode.attributes[i].nodeName;
    if (name) {
      var value = xmlNode.getAttribute(name);
      if (name && value) {
        this.attributes[name] = value;
        // TODO: This attribute may not be used by the handler.
        this.computeNeededKeys_(value);
      }
    }
  }

  this.key = this.attributes[os.data.ATTR_KEY];
  this.register_();
};

/**
 * Checks if an attribute has been specified for this tag.
 * @param {string} name The attribute name
 * @return {boolean} The attribute is set.
 */
os.data.RequestDescriptor.prototype.hasAttribute = function(name) {
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
os.data.RequestDescriptor.prototype.getAttribute = function(name) {
  var attrExpression = this.attributes[name];
  if (!attrExpression) {
    return attrExpression;
  }
  // TODO: Don't do this every time - cache the result.
  var expression = os.parseAttribute_(attrExpression);
  if (!expression) {
    return attrExpression;
  }
  return os.data.DataContext.evalExpression(expression);
};

/**
 * Sends this request off to be fulfilled. The current DataContext state will
 * be used to reslove any variable references.
 */
os.data.RequestDescriptor.prototype.sendRequest = function() {
  var handler = os.getCustomTag(this.tagParts[0], this.tagParts[1]);
  if (!handler) {
    throw "Data handler undefined for " + this.tagName;
  }
  handler(this);
};

/**
 * Creates a closure to this RequestDescriptor's sendRequest() method.
 */
os.data.RequestDescriptor.prototype.getSendRequestClosure = function() {
  var self = this;
  return function() {
    self.sendRequest();
  };
};

/**
 * Computes the keys needed by an attribute by looking for variable substitution
 * markup. For example if the attribute is "http://example.com/${user.id}", the
 * "user" key is needed. The needed keys are set as properties into a member of
 * this RequestDescriptor.
 * @param {string} attribute The value of the attribute to inspect.
 * @private
 */
os.data.RequestDescriptor.prototype.computeNeededKeys_ = function(attribute) {
  var substRex = os.regExps_.VARIABLE_SUBSTITUTION;
  var match = attribute.match(substRex);
  while (match) {
    var token = match[2].substring(2, match[2].length - 1);
    var key = token.split(".")[0];
    if (!this.neededKeys) {
      this.neededKeys = {};
    }
    this.neededKeys[key] = true;
    match = match[3].match(substRex);
  }
};

/**
 * Registers this RequestDescriptor using its key.
 * @private
 */
os.data.RequestDescriptor.prototype.register_ = function() {
  os.data.registerRequestDescriptor(this);
};

/**
 * @type {Object} Global DataContext to contain requested data sets.
 */
os.data.DataContext = {};

os.data.DataContext.listeners_ = [];

os.data.DataContext.dataSets_ = {};

/**
 * A shared JsEvalContext for evaluating expressions.
 */
os.data.DataContext.evalContext_ =
  os.createContext(os.data.DataContext.dataSets_);

/**
 * Accessor to the shared context object.
 */
os.data.DataContext.getContext = function() {
  return os.data.DataContext.evalContext_;
};

/**
 * Registers a callback listener for a given set of keys.
 * @param {string|Array.<string>} keys Key or set of keys to listen on.
 * @param {Function(string)} callback Function to call when a listener is fired.
 * TODO: Should return a value that can later be used to return
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
  os.data.DataContext.listeners_.push(listener);

  // Check to see if this one should fire immediately.
  if (os.data.DataContext.isDataReady(listener.keys)) {
    window.setTimeout(function() {
      listener.callback();
    }, 1);
  }
};

/**
 * Retrieve a data set for a given key.
 * @param {string} key Key for the requested data set.
 * @return {Object} The data set object.
 */
os.data.DataContext.getDataSet = function(key) {
  return os.data.DataContext.dataSets_[key];
};

/**
 * Checks if the data for a map of keys is available.
 * @param {Object<string, ?>} An map of keys to check.
 * @return {boolean} Data for all the keys is present.
 */
os.data.DataContext.isDataReady = function(keys) {
  for (var key in keys) {
    if (os.data.DataContext.getDataSet(key) == null) {
      return false;
    }
  }
  return true;
};

/**
 * Puts a data set into the global DataContext object. Fires listeners
 * if they are satisfied by the associated key being inserted.
 * @param {string} key The key to associate with this object.
 * @param {ResponseItem|Object} obj The data object.
 */
os.data.DataContext.putDataSet = function(key, obj) {
  var data = obj;
  if (typeof(data) == "undefined" || data === null) {
    return;
  }

  // NOTE: This is ugly, but required since we need to get access
  // to the JSON/Array payload of API responses.
  if (data.getData) {
   data = data.getData();
   data = data.array_ || data;
  }

  os.data.DataContext.dataSets_[key] = data;
  os.data.DataContext.fireCallbacks_(key);
};

/**
 * Evaluates a JS expression against the DataContext.
 * @param {string} expr The expression to evaluate.
 * @return {Object} The result of evaluation.
 */
os.data.DataContext.evalExpression = function(expr) {
  return os.data.DataContext.evalContext_.evalExpression(expr);
};

/**
 * Fires a listener for a key, but only if the data is ready for other
 * keys this listener is bound to.
 * @param {Object} listener The listener object.
 * @param {string} key The key that this listener is being fired for.
 */
os.data.DataContext.maybeFireListener_ = function(listener, key) {
  if (os.data.DataContext.isDataReady(listener.keys)) {
    listener.callback(key);
  }
};

/**
 * Scans all active listeners and fires off any callbacks that inserting this
 * key satisfies.
 * @param {string} key The key that was updated.
 * @private
 */
os.data.DataContext.fireCallbacks_ = function(key) {
  for (var i = 0; i < os.data.DataContext.listeners_.length; ++i) {
    var listener = os.data.DataContext.listeners_[i];
    if (listener.keys[key] != null) {
      os.data.DataContext.maybeFireListener_(listener, key);
    }
  }
};

/**
 * Accessor to the static DataContext object. At a later date multiple
 * DataContexts may be used.
 */
os.data.getDataContext = function() {
  return os.data.DataContext;
};

/**
 * @type {Object} Map of currently registered RequestDescriptors (by key).
 * @private
 */
os.data.requests_ = {};

/**
 * Registers a RequestDescriptor by key in the global registry.
 * @param {RequestDescriptor} requestDescriptor The RequestDescriptor to
 * register.
 */
os.data.registerRequestDescriptor = function(requestDescriptor) {
  if (os.data.requests_[requestDescriptor.key]) {
    throw "Request already registered for " + requestDescriptor.key;
  }
  os.data.requests_[requestDescriptor.key] = requestDescriptor;
};

/**
 * @type {DataRequest} A shared DataRequest object for batching OS API data
 * calls.
 * @private
 */
os.data.currentAPIRequest_ = null;

/**
 * @type {Array<string>} An array of keys requested by the shared DataRequest.
 * @private
 */
os.data.currentAPIRequestKeys_ = null;

/**
 * @type {Object<string, Function(Object)>} A map of custom callbacks for the
 * keys in the shared DataRequest.
 * @private
 */
os.data.currentAPIRequestCallbacks_ = null;

/**
 * Gets the shared DataRequest, constructing it lazily when needed.
 * Access to this object is provided so that various sub-requests can be
 * constructed (i.e. via newFetchPersonRequest()). Neither add() nor send()
 * should be called on this object - doing so will lead to undefined behavior.
 * Use os.data.addToCurrentAPIRequest() instead.
 * TODO: Create a wrapper that doesn't support add() and send().
 * @return {DataRequest} The shared DataRequest.
 */
os.data.getCurrentAPIRequest = function() {
  if (!os.data.currentAPIRequest_) {
    os.data.currentAPIRequest_ = opensocial.newDataRequest();
    os.data.currentAPIRequestKeys_ = [];
    os.data.currentAPIRequestCallbacks_ = {};
  }
  return os.data.currentAPIRequest_;
};

/**
 * Adds a request to the current shared DataRequest object. Any requests
 * added in a synchronous block of code will be batched. The requests will be
 * automatically sent once the syncronous block is done executing.
 * @param {Object} request Specifies data to fetch
 * (constructed via DataRequest's newFetch???Request() methods)
 * @param {String} key The key to map generated response data to
 * @param {Function(string, ResponseItem)} opt_callback An optional callback
 * function to pass the returned ResponseItem to. If present, the function will
 * be called with the key and ResponseItem as params. If this is omitted, the
 * ResponseItem will be passed to putDataSet() with the specified key.
 */
os.data.addToCurrentAPIRequest = function(request, key, opt_callback) {
  os.data.getCurrentAPIRequest().add(request, key);
  os.data.currentAPIRequestKeys_.push(key);

  if (opt_callback) {
    os.data.currentAPIRequestCallbacks_[key] = opt_callback;
  }

  window.setTimeout(os.data.sendCurrentAPIRequest_, 0);
};

/**
 * Sends out the current shared DataRequest. The reference is removed, so that
 * when new requests are added, a new shared DataRequest object will be
 * constructed.
 * @private
 */
os.data.sendCurrentAPIRequest_ = function() {
  if (os.data.currentAPIRequest_) {
    os.data.currentAPIRequest_.send(os.data.createSharedRequestCallback_());
    os.data.currentAPIRequest_ = null;
  }
};

/**
 * Creates a callback closure for processing a DataResponse. The closure
 * remembers which keys were requested, and what custom callbacks need to be
 * called.
 * @return {Function(DataResponse)} a handler for DataResponse.
 * @private
 */
os.data.createSharedRequestCallback_ = function() {
  var keys = os.data.currentAPIRequestKeys_;
  var callbacks = os.data.currentAPIRequestCallbacks_;
  return function(data) {
    os.data.onAPIResponse(data, keys, callbacks);
  };
};

/**
 * Processes a response to the shared API DataRequest by looping through
 * requested keys and notifying appropriate parties of the received data.
 * @param {DataResonse} data Data received from the server
 * @param {Array<string>} keys The list of keys that were requested
 * @param {Object<string, Function(string, ResponseItem)>} callbacks A map of
 * any custom callbacks by key.
 */
os.data.onAPIResponse = function(data, keys, callbacks) {
  for (var i = 0; i < keys.length; i++) {
    var key = keys[i];
    var item = data.get(key);
    if (callbacks[key]) {
      callbacks[key](key, item);
    } else {
      os.data.DataContext.putDataSet(key, item);
    }
  }
};

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
 * Loads and executes all inline data request sections.
 * @param {Object} opt_doc Optional document to use instead of window.document.
 * TODO: Currently this processes all 'script' blocks together,
 *     instead of collecting them all and then processing together. Not sure
 *     which is preferred yet.
 * TODO: Figure out a way to pass in params used only for data
 *     and not for template rendering.
 */
os.data.processDocumentMarkup = function(opt_doc) {
  var doc = opt_doc || document;
  var nodes = doc.getElementsByTagName("script");
  for (var i = 0; i < nodes.length; ++i) {
    var node = nodes[i];
    if (node.type == os.data.SCRIPT_TYPE) {
      os.data.loadRequests(node);
    }
  }
  os.data.registerRequestDependencies();
  os.data.executeRequests();
};

/**
 * Process the document when it's ready.
 */
if (window['gadgets'] && window['gadgets']['util']) {
  gadgets.util.registerOnLoadHandler(os.data.processDocumentMarkup);
}

/**
 * Parses XML data and constructs the pending request list.
 * @param {Element|string} xml A DOM element or string containing XML.
 */
os.data.loadRequests = function(xml) {
  if (typeof(xml) == 'string') {
    os.data.loadRequestsFromMarkup_(xml);
    return;
  }
  var node = xml;
  xml = node.value || node.innerHTML;
  os.data.loadRequestsFromMarkup_(xml);
};

/**
 * Parses XML data and constructs the pending request list.
 * @param {string} xml A string containing XML markup.
 */
os.data.loadRequestsFromMarkup_ = function(xml) {
  xml = os.prepareTemplateXML_(xml);
  var doc = os.parseXML_(xml);

  // Find the <root> node (skip DOCTYPE).
  var node = doc.firstChild;
  while (node.nodeType != DOM_ELEMENT_NODE) {
    node = node.nextSibling;
  }

  os.data.processDataNode_(node);
};

/**
 * Processes a data request node for data sets.
 * @param {Node} node The node to process.
 * @private
 */
os.data.processDataNode_ = function(node) {
  for (var child = node.firstChild; child; child = child.nextSibling) {
    if (child.nodeType == DOM_ELEMENT_NODE) {
      var requestDescriptor = new os.data.RequestDescriptor(child);
    }
  }
};

os.data.registerRequestDependencies = function() {
  for (var key in os.data.requests_) {
    var request = os.data.requests_[key];
    var neededKeys = request.neededKeys;
    var dependencies = [];
    for (var neededKey in neededKeys) {
      if (os.data.DataContext.getDataSet(neededKey) == null &&
          os.data.requests_[neededKey]) {
        dependencies.push(neededKey);
      }
    }
    if (dependencies.length > 0) {
      os.data.DataContext.registerListener(dependencies,
          request.getSendRequestClosure());
      request.dependencies = true;
    }
  }
};

os.data.executeRequests = function() {
  for (var key in os.data.requests_) {
    var request = os.data.requests_[key];
    if (!request.dependencies) {
      request.sendRequest();
    }
  }
};

/**
 * Transforms "@"-based special values such as "@owner" into uppercase
 * keywords like "OWNER".
 * @param {string} value The value to transform.
 * @return {string} Transformed or original value.
 */
os.data.transformSpecialValue = function(value) {
  if (value.substring(0, 1) == '@') {
    return value.substring(1).toUpperCase();
  }
  return value;
};

/**
 * Anonymous function defines OpenSocial specific requests.
 * Automatically called when this file is loaded.
 */
(function() {
  os.data.registerRequestHandler("os:ViewerRequest", function(descriptor) {
    var req = os.data.getCurrentAPIRequest().newFetchPersonRequest("VIEWER");
    // TODO: Support @fields param.
    os.data.addToCurrentAPIRequest(req, descriptor.key);
  });

  os.data.registerRequestHandler("os:OwnerRequest", function(descriptor) {
    var req = os.data.getCurrentAPIRequest().newFetchPersonRequest("OWNER");
    // TODO: Support @fields param.
    os.data.addToCurrentAPIRequest(req, descriptor.key);
  });

  os.data.registerRequestHandler("os:PeopleRequest", function(descriptor) {
    var userId = descriptor.getAttribute("userId");
    var groupId = descriptor.getAttribute("groupId") || "@self";
    var idSpec = {};
    idSpec.userId = os.data.transformSpecialValue(userId);
    if (groupId != "@self") {
      idSpec.groupId = os.data.transformSpecialValue(groupId);
    }
    // TODO: Support other params.
    var req = os.data.getCurrentAPIRequest().newFetchPeopleRequest(
        opensocial.newIdSpec(idSpec));
    // TODO: Annotate with the @ids property.
    os.data.addToCurrentAPIRequest(req, descriptor.key);
  });

  os.data.registerRequestHandler("os:DataRequest", function(descriptor) {
    var href = descriptor.getAttribute('href');
    var format = descriptor.getAttribute('format') || "json";
    var params = {};
    params[gadgets.io.RequestParameters.CONTENT_TYPE] =
        format.toLowerCase() == "text" ? gadgets.io.ContentType.TEXT :
            gadgets.io.ContentType.JSON;
    params[gadgets.io.RequestParameters.METHOD] =
        gadgets.io.MethodType.GET;
    gadgets.io.makeRequest(href, function(obj) {
        os.data.DataContext.putDataSet(descriptor.key, obj.data);
    }, params);
  });
})();

/**
 * Pre-populate a Data Set based on application's URL parameters.
 */
(os.data.populateParams_ = function() {
  if (window["gadgets"] && gadgets.util.hasFeature("views")) {
    os.data.DataContext.putDataSet("ViewParams", gadgets.views.getParams());
  }
})();