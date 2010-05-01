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
 * @fileoverview Standard methods invoked by containers to use the template API.
 *
 * Sample usage:
 *  <script type="text/os-template" tag="os:Button">
 *    <button onclick="alert('Clicked'); return false;">
 *      <os:renderAll/>
 *    </button>
 *  </script>
 *
 *  <script type="text/os-template">
 *    <os:Button>
 *      <div>Click me</div>
 *    </os:Button>
 *  </script>
 *
 * os.Container.registerDocumentTemplates();
 * os.Container.renderInlineTemplates();
 */

os.Container = {};

/***
 * @type {Array.<Object>} Array of registered inline templates.
 * @private
 */
os.Container.inlineTemplates_ = [];

/**
 * @type {Array.<Function>} An array of callbacks to fire when the page DOM has
 * loaded. This will be null until the first callback is added 
 * @see registerDomListener_
 * @private
 */
os.Container.domLoadCallbacks_ = null;

/**
 * @type {boolean} A boolean flag determining wether the page DOM has loaded.
 * @private
 */
os.Container.domLoaded_ = false;

/**
 * Registers the DOM Load listener to fire when the page DOM is available.
 * TODO: See if we can use gadgets.util.regiterOnLoadHandler() here.
 * TODO: Currently for everything but Mozilla, this just registers an 
 * onLoad listener on the window. Should use DOMContentLoaded on Opera9, 
 * appropriate hacks (polling?) on IE and Safari.
 * @private
 */
os.Container.registerDomLoadListener_ = function() {
  var gadgets = window['gadgets'];
  if (gadgets && gadgets.util) {
    gadgets.util.registerOnLoadHandler(os.Container.onDomLoad_); 
  } else if (navigator.product == 'Gecko') {
    window.addEventListener("DOMContentLoaded", os.Container.onDomLoad_, false);
  } if (window.addEventListener) {
    window.addEventListener("load", os.Container.onDomLoad_, false);
  } else {          
    if (!document.body) {
      setTimeout(arguments.callee, 0);
      return;
    }
    var oldOnLoad = window.onload || function() {};
    window.onload = function() {
      oldOnLoad();
      os.Container.onDomLoad_();
    }              
  }
};

/**
 * To be called when the page DOM is available - will fire all the callbacks
 * in os.Container.domLoadCallbacks_.
 * @private
 */
os.Container.onDomLoad_ = function() {
  if (os.Container.domLoaded_) {
    return;
  }
  while (os.Container.domLoadCallbacks_.length) {
  try {
      os.Container.domLoadCallbacks_.pop()();
    } catch (e) {
      os.log(e);
    }
  }
  os.Container.domLoaded_ = true;  
};

/**
 * Adds a callback to be fired when the page DOM is available. If the page
 * is already loaded, the callback will execute asynchronously.
 * @param {Function} callback The callback to be fired when DOM is loaded.
 */
os.Container.executeOnDomLoad = function(callback) {
  if (os.Container.domLoaded_) {
    setTimeout(callback, 0);    
  } else {
    if (os.Container.domLoadCallbacks_ == null) {
      os.Container.domLoadCallbacks_ = [];
      os.Container.registerDomLoadListener_();
    }
    os.Container.domLoadCallbacks_.push(callback);
  }
};

/**
 * Compiles and registers all DOM elements in the document. Templates are
 * registered as tags if they specify their name with the "tag" attribute
 * and as templates if they have a name (or id) attribute.
 * @param {Object} opt_doc Optional document to use rather than the global doc.
 */
os.Container.registerDocumentTemplates = function(opt_doc) {
  var doc = opt_doc || document;
  var nodes = doc.getElementsByTagName(os.Container.TAG_script_);
  for (var i = 0; i < nodes.length; ++i) {
    var node = nodes[i];
    if (os.Container.isTemplateType_(node.type)) {
      var tag = node.getAttribute('tag');
      if (tag) {
        os.Container.registerTagElement_(node, tag);
      } else if (node.getAttribute('name')) {
        os.Container.registerTemplateElement_(node, node.getAttribute('name'));
      }
    }
  }
};

os.Container.executeOnDomLoad(os.Container.registerDocumentTemplates);

/**
 * Compiles and registers all unnamed templates in the document.
 * @param {Object} opt_data Optional JSON data.
 * @param {Object} opt_doc Optional document to use instead of window.document.
 */
os.Container.compileInlineTemplates = function(opt_data, opt_doc) {
  var doc = opt_doc || document;
  var nodes = doc.getElementsByTagName(os.Container.TAG_script_);
  for (var i = 0; i < nodes.length; ++i) {
    var node = nodes[i];
    if (os.Container.isTemplateType_(node.type)) {
      var name = node.getAttribute('name') || node.getAttribute('tag');
      if (!name || name.length < 0) {
        var template = os.compileTemplate(node);
        if (template) {
          os.Container.inlineTemplates_.push(
              {'template': template, 'node': node});
        } else {
          os.warn('Failed compiling inline template.');
        }
      }
    }
  }
};

/**
 * Renders any registered inline templates.
 * @param {Object} opt_data Optional JSON data.
 * @param {Object} opt_doc Optional document to use instead of window.document.
 */
os.Container.renderInlineTemplates = function(opt_data, opt_doc) {
  var doc = opt_doc || document;
  var inlined = os.Container.inlineTemplates_;
  for (var i = 0; i < inlined.length; ++i) {
    var template = inlined[i].template;
    var node = inlined[i].node;
    var id = '_T_' + template.id;
    var el = doc.getElementById(id);
    if (!el) {
      el = doc.createElement('div');
      el.setAttribute('id', id);
      node.parentNode.insertBefore(el, node);
    }

    var beforeData = node.getAttribute('beforeData');
    if (beforeData) {
      // Automatically hide this template when specified data is available.
      var keys = beforeData.split(/[\, ]+/);
      os.data.DataContext.registerListener(keys,
          os.createHideElementClosure(el));
    }

    var requiredData = node.getAttribute('requireData');
    if (requiredData) {
      // This template will render when the specified data is available.
      var keys = requiredData.split(/[\, ]+/);
      os.data.DataContext.registerListener(keys,
          os.createRenderClosure(template, el, os.data.DataContext));
    } else {
      template.renderInto(el, opt_data);
    }
  }
};

/**
 * Compiles and registers a template from a DOM element.
 * @param {string} elementId Id of DOM element from which to create a template.
 * @return {Object} The compiled and registered template object.
 */
os.Container.registerTemplate = function(elementId) {
  var element = document.getElementById(elementId);
  return os.Container.registerTemplateElement_(element);
};

/**
 * Registers a custom tag from a namespaced DOM element.
 * @param {string} elementId Id of the DOM element to register.
 */
os.Container.registerTag = function(elementId) {
  var element = document.getElementById(elementId);
  os.Container.registerTagElement_(element, elementId);
};

/**
 * Renders a DOM element with a specified template and contextual data.
 * @param {string} elementId Id of DOM element to inject into.
 * @param {string} templateId Id of the template.
 * @param {Object} opt_data Data to supply to template.
 */
os.Container.renderElement = function(elementId, templateId, opt_data) {
  var template = os.getTemplate(templateId);
  if (template) {
    var element = document.getElementById(elementId);
    if (element) {
      template.renderInto(element, opt_data);
    } else {
      os.warn('Element (' + elementId + ') not found to render into.');
    }
  } else {
    os.warn('Template (' + templateId + ') not registered.');
  }
};

/**
 * Loads and executes all inline data request sections.
 * @param {Object} opt_doc Optional document to use instead of window.document.
 * TODO(davidbyttow): Currently this processes all 'script' blocks together,
 *     instead of collecting them all and then processing together. Not sure
 *     which is preferred yet.
 * TODO(davidbyttow: Figure out a way to pass in params used only for data
 *     and not for template rendering.
 */
os.Container.loadDataRequests = function(opt_doc) {
  var doc = opt_doc || document;
  var nodes = doc.getElementsByTagName(os.Container.TAG_script_);
  for (var i = 0; i < nodes.length; ++i) {
    var node = nodes[i];
    if (node.type == os.Container.dataType_) {
      os.data.loadRequests(node);
    }
  }
  os.data.executeRequests();
};

/**
 * Compiles and renders all inline templates.
 * @param {Object} opt_data Optional JSON data.
 * @param {Object} opt_doc Optional document to use instead of window.document.
 */
os.Container.processInlineTemplates = function(opt_data, opt_doc) {
  var data = opt_data || os.data.DataContext;
  os.Container.compileInlineTemplates(opt_doc);
  os.Container.renderInlineTemplates(data, opt_doc);
};

/**
 * Utility method which will automatically register all templates
 * and render all that are inline.
 * @param {Object} opt_data Optional JSON data.
 * @param {Object} opt_doc Optional document to use instead of window.document.
 */
os.Container.processDocument = function(opt_data, opt_doc) {
  os.Container.loadDataRequests(opt_doc);
  os.Container.registerDocumentTemplates(opt_doc);
  os.Container.processInlineTemplates(opt_data, opt_doc);
};

/***
 * @type {string} Tag name of a template.
 * @private
 */
os.Container.TAG_script_ = 'script';

/***
 * @type {Object} Map of allowed template content types.
 * @private
 * TODO(davidbyttow): Remove text/template.
 */
os.Container.templateTypes_ = {};
os.Container.templateTypes_['text/os-template'] = true;
os.Container.templateTypes_['text/template'] = true;

/***
 * @type {string} Type name of data request sections.
 * @private
 */
os.Container.dataType_ = 'text/os-data';

/**
 * Checks if a given type name is properly named as a template. 
 * @param {string} typeName Name of a given type.
 * @return {boolean} This type is considered a template.
 * @private
 */
os.Container.isTemplateType_ = function(typeName) {
  return os.Container.templateTypes_[typeName] != null;
};

/**
 * Compiles and registers a template from a DOM element.
 * @param {Element} element DOM element from which to create a template.
 * @param {string} opt_id Optional id for template.
 * @return {Object} The compiled and registered template object.
 * @private
 */
os.Container.registerTemplateElement_ = function(element, opt_id) {
  var template = os.compileTemplate(element, opt_id);
  if (template) {
    os.registerTemplate(template);
  } else {
    os.warn('Could not compile template (' + element.id + ')');
  }
  return template;
};

/**
 * Registers a custom tag from a namespaced DOM element.
 * @param {Element} element DOM element to register.
 * @param {string} name Name of the tag.
 * @private
 */
os.Container.registerTagElement_ = function(element, name) {
  var template = os.Container.registerTemplateElement_(element);
  if (template) {
    var tagParts = name.split(':');
    var nsObj = os.getNamespace(tagParts[0]);
    if (nsObj) {
      nsObj[tagParts[1]] = os.createTemplateCustomTag(template);
    } else {
      os.warn('Namespace ' + tagParts[0] + ' is not registered.');
    }
  }
};
