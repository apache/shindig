// Copyright 2007 Google Inc.
// All Rights Reserved.

/**
 * @fileoverview Open Gadget Container
 *
 * @author jyang@google.com (Jun Yang)
 * @author wangz@google.com (Zhen Wang)
 */

// -----
// Utils

Function.prototype.inherits = function(parentCtor) {
  function tempCtor() {};
  tempCtor.prototype = parentCtor.prototype;
  this.superClass_ = parentCtor.prototype;
  this.prototype = new tempCtor();
  this.prototype.constructor = this;
};


// -----------
// gadgets

var gadgets = {};

gadgets.error = {};
gadgets.error.SUBCLASS_RESPONSIBILITY = 'subclass responsibility';
gadgets.error.TO_BE_DONE = 'to be done';

gadgets.log = function(message) {
  if (window.console && console.log) {
    console.log(message);
  } else {
    var logEntry = document.createElement('div');
    logEntry.className = 'gadgets-log-entry';
    logEntry.innerHTML = message;
    document.body.appendChild(logEntry);
  }
};

/**
 * Calls an array of asynchronous functions and calls the continuation
 * function when all are done.
 * @param {Array} functions Array of asynchronous functions, each taking
 *     one argument that is the continuation function that handles the result
 *     That is, each function is something like the following:
 *     function(continuation) {
 *       // compute result asynchronously
 *       continuation(result);
 *     }
 * @param {Function} continuation Function to call when all results are in.  It
 *     is pass an array of all results of all functions
 * @param {Object} opt_this Optional object used as "this" when calling each
 *     function
 */
gadgets.callAsyncAndJoin = function(functions, continuation, opt_this) {
  var pending = functions.length;
  var results = [];
  for (var i = 0; i < functions.length; i++) {
    // we need a wrapper here because i changes and we need once index
    // variable per closure
    var wrapper = function(index) {
      functions[index].call(opt_this, function(result) {
        results[index] = result;
        if (--pending == 0) {
          continuation(results);
        }
      });
    };
    wrapper(i);
  }
};


// ----------
// Extensible

gadgets.Extensible = function() {
};

/**
 * Sets the dependencies.
 * @param {Object} dependencies Object whose properties are set on this
 *     container as dependencies
 */
gadgets.Extensible.prototype.setDependencies = function(dependencies) {
  for (var p in dependencies) {
    this[p] = dependencies[p];
  }
};

/**
 * Returns a dependency given its name.
 * @param {String} name Name of dependency
 * @return {Object} Dependency with that name or undefined if not found
 */
gadgets.Extensible.prototype.getDependencies = function(name) {
  return this[name];
};



// -------------
// UserPrefStore

/**
 * User preference store interface.
 * @constructor
 */
gadgets.UserPrefStore = function() {
};

/**
 * Gets all user preferences of a gadget.
 * @param {Object} gadget Gadget object
 * @return {Object} All user preference of given gadget
 */
gadgets.UserPrefStore.prototype.getPrefs = function(gadget) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};

/**
 * Saves user preferences of a gadget in the store.
 * @param {Object} gadget Gadget object
 * @param {Object} prefs User preferences
 */
gadgets.UserPrefStore.prototype.savePrefs = function(gadget) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};


// ------------------------
// CookieBasedUserPrefStore

/**
 * Cookie-based user preference store.
 * @constructor
 */
gadgets.CookieBasedUserPrefStore = function() {
  gadgets.UserPrefStore.call(this);
};

gadgets.CookieBasedUserPrefStore.inherits(gadgets.UserPrefStore);

gadgets.CookieBasedUserPrefStore.prototype.USER_PREFS_PREFIX =
    'gadgetUserPrefs-';

gadgets.CookieBasedUserPrefStore.prototype.getPrefs = function(gadget) {
  var userPrefs = {};
  var cookieName = this.USER_PREFS_PREFIX + gadget.id;
  var cookie = goog.net.cookies.get(cookieName);
  if (cookie) {
    var pairs = cookie.split('&');
    for (var i = 0; i < pairs.length; i++) {
      var nameValue = pairs[i].split('=');
      var name = decodeURIComponent(nameValue[0]);
      var value = decodeURIComponent(nameValue[1]);
      userPrefs[name] = value;
    }
  }

  return userPrefs;
};

gadgets.CookieBasedUserPrefStore.prototype.savePrefs = function(gadget) {
  var pairs = [];
  for (var name in gadget.getUserPrefs()) {
    var value = gadget.getUserPref(name);
    var pair = encodeURIComponent(name) + '=' + encodeURIComponent(value);
    pairs.push(pair);
  }

  var cookieName = this.USER_PREFS_PREFIX + gadget.id;
  var cookieValue = pairs.join('&');
  goog.net.cookies.set(cookieName, cookieValue);
};


// -------------
// GadgetService

/**
 * Interface of service provided to gadgets for resizing gadgets,
 * setting title, etc.
 * @constructor
 */
gadgets.GadgetService = function() {
};

gadgets.GadgetService.prototype.setHeight = function(elementId, height) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};

gadgets.GadgetService.prototype.setTitle = function(gadget, title) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};

gadgets.GadgetService.prototype.setUserPref = function(id) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};


// ----------------
// IfrGadgetService

/**
 * Base implementation of GadgetService.
 * @constructor
 */
gadgets.IfrGadgetService = function() {
  gadgets.GadgetService.call(this);

  _IFPC.registerService('resize_iframe', this.setHeight);
  _IFPC.registerService('set_pref', this.setUserPref);
};

gadgets.IfrGadgetService.inherits(gadgets.GadgetService);

gadgets.IfrGadgetService.prototype.setHeight = function(elementId, height) {
  var element = document.getElementById(elementId);
  if (element) {
    element.style.height = height + 'px';
  }
};

gadgets.IfrGadgetService.prototype.setTitle = function(gadget, title) {
  throw Error(gadgets.error.TO_BE_DONE);
};

/**
 * Sets one or more user preferences
 * @param {String} gadgetFrameId Frame ID of the gadget that initiates the call
 * @param {String} dummy
 * @param {String} name Name of user preference
 * @param {String} value Value of user preference
 * More names and values may follow
 */
gadgets.IfrGadgetService.prototype.setUserPref = function(gadgetFrameId) {
  // Quick hack to extract the gadget id from module id
  var id = parseInt(gadgetFrameId.match(/_([0-9]+)$/)[1], 10);
  var gadget = gadgets.container.getGadget(id);
  var prefs = gadget.getUserPrefs();
  for (var i = 2; i < arguments.length; i += 2) {
    prefs[arguments[i]] = arguments[i + 1];
  }
  gadget.setUserPrefs(prefs);
};


// -------------
// LayoutManager

/**
 * Layout manager interface.
 * @constructor
 */
gadgets.LayoutManager = function() {
};

/**
 * Gets the HTML element that is the chrome of a gadget into which the cotnent
 * of the gadget can be rendered.
 * @param {Object} gadget Gadget instance
 * @return {Object} HTML element that is the chrome for the given gadget
 */
gadgets.LayoutManager.prototype.getGadgetChrome = function(gadget) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};

// -------------------
// StaticLayoutManager

/**
 * Static layout manager where gadget ids have a 1:1 mapping to chrome ids.
 * @constructor
 */
gadgets.StaticLayoutManager = function() {
  gadgets.LayoutManager.call(this);
};

gadgets.StaticLayoutManager.inherits(gadgets.LayoutManager);

/**
 * Sets chrome ids, whose indexes are gadget instance ids (starting from 0).
 * @param {Array} gadgetIdToChromeIdMap Gadget id to chrome id map
 */
gadgets.StaticLayoutManager.prototype.setGadgetChromeIds =
    function(gadgetChromeIds) {
  this.gadgetChromeIds_ = gadgetChromeIds;
};

gadgets.StaticLayoutManager.prototype.getGadgetChrome = function(gadget) {
  var chromeId = this.gadgetChromeIds_[gadget.id];
  return chromeId ? document.getElementById(chromeId) : null;
};


// ----------------------
// FloatLeftLayoutManager

/**
 * FloatLeft layout manager where gadget ids have a 1:1 mapping to chrome ids.
 * @constructor
 * @param {String} layoutRootId Id of the element that is the parent of all
 *     gadgets.
 */
gadgets.FloatLeftLayoutManager = function(layoutRootId) {
  gadgets.LayoutManager.call(this);
  this.layoutRootId_ = layoutRootId;
};

gadgets.FloatLeftLayoutManager.inherits(gadgets.LayoutManager);

gadgets.FloatLeftLayoutManager.prototype.getGadgetChrome =
    function(gadget) {
  var layoutRoot = document.getElementById(this.layoutRootId_);
  if (layoutRoot) {
    var chrome = document.createElement('div');
    chrome.className = 'gadgets-gadget-chrome';
    chrome.style.float = 'left'
    layoutRoot.appendChild(chrome);
    return chrome;
  } else {
    return null;
  }
};


// ------
// Gadget

/**
 * Creates a new instance of gadget.  Optoinal parameters are set as instance
 * variables.
 * @constructor
 * @param {Object} params Parameters to set on gadget.  Common parameters:
 *    "specUrl": URL to gadget specification
 *    "private": Whether gadget spec is accessible only privately, which means
 *        browser can load it but not gadget server
 *    "spec": Gadget Specification in XML
 */
gadgets.Gadget = function(params) {
  this.userPrefs_ = {};

  if (params) {
    for (var name in params) {
      this[name] = params[name];
    }
  }
};

gadgets.Gadget.prototype.getUserPrefs = function() {
  return this.userPrefs_;
};

gadgets.Gadget.prototype.setUserPrefs = function(userPrefs) {
  this.userPrefs_ = userPrefs;
  gadgets.container.userPrefStore.savePrefs(this);
};

gadgets.Gadget.prototype.getUserPref = function(name) {
  return this.userPrefs_[name];
};

gadgets.Gadget.prototype.setUserPref = function(name, value) {
  this.userPrefs_[name] = value;
  gadgets.container.userPrefStore.savePrefs(this);
};

gadgets.Gadget.prototype.render = function(chrome) {
  if (chrome) {
    this.getContent(function(content) {
      chrome.innerHTML = content;
    });
  }
};

gadgets.Gadget.prototype.getContent = function(continuation) {
  gadgets.callAsyncAndJoin([
      this.getTitleBarContent, this.getUserPrefsDialogContent,
      this.getMainContent], function(results) {
        continuation(results.join(''));
      }, this);
};

/**
 * Gets title bar content asynchronously or synchronously.
 * @param {Function} continutation Function that handles title bar content as
 *     the one and only argument
 */
gadgets.Gadget.prototype.getTitleBarContent = function(continuation) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};

/**
 * Gets user preferences dialog content asynchronously or synchronously.
 * @param {Function} continutation Function that handles user preferences
 *     content as the one and only argument
 */
gadgets.Gadget.prototype.getUserPrefsDialogContent = function(continuation) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};

/**
 * Gets gadget content asynchronously or synchronously.
 * @param {Function} continutation Function that handles gadget content as
 *     the one and only argument
 */
gadgets.Gadget.prototype.getMainContent = function(continuation) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};


// ---------
// IfrGadget

gadgets.IfrGadget = function(opt_params) {
  gadgets.Gadget.call(this, opt_params);
  this.serverBase_ = 'http://www.gmodules.com/ig/';  // default server
};

gadgets.IfrGadget.inherits(gadgets.Gadget);

gadgets.IfrGadget.prototype.GADGET_IFRAME_PREFIX_ = 'remote_iframe_';

gadgets.IfrGadget.prototype.SYND = 'gadgets';

gadgets.IfrGadget.prototype.cssClassGadget = 'gadgets-gadget';
gadgets.IfrGadget.prototype.cssClassTitleBar = 'gadgets-gadget-title-bar';
gadgets.IfrGadget.prototype.cssClassTitle = 'gadgets-gadget-title';
gadgets.IfrGadget.prototype.cssClassTitleButtonBar =
    'gadgets-gadget-title-button-bar';
gadgets.IfrGadget.prototype.cssClassGadgetUserPrefsDialog =
    'gadgets-gadget-user-prefs-dialog';
gadgets.IfrGadget.prototype.cssClassGadgetUserPrefsDialogActionBar =
    'gadgets-gadget-user-prefs-dialog-action-bar';
gadgets.IfrGadget.prototype.cssClassTitleButton = 'gadgets-gadget-title-button';
gadgets.IfrGadget.prototype.cssClassGadgetContent = 'gadgets-gadget-content';

gadgets.IfrGadget.prototype.getTitleBarContent = function(continuation) {
  continuation('<div class="' + this.cssClassTitleBar + '"><span class="' +
      this.cssClassTitle + '">Title</span> | <span class="' +
      this.cssClassTitleButtonBar +
      '"><a href="#" onclick="gadgets.container.getGadget(' + this.id +
      ').handleOpenUserPrefsDialog()" class="' + this.cssClassTitleButton +
      '">settings</a> <a href="#" onclick="gadgets.container.getGadget(' +
      this.id + ').handleToggle()" class="' + this.cssClassTitleButton +
      '">toggle</a></span></div>');
};

gadgets.IfrGadget.prototype.getUserPrefsDialogContent = function(continuation) {
  continuation('<div id="' + this.getUserPrefsDialogId() + '" class="' +
      this.cssClassGadgetUserPrefsDialog + '"></div>');
};

// TODO: Move this API to Container rather than Gadget
gadgets.IfrGadget.prototype.setServerBase = function(url) {
  this.serverBase_ = url;
};

gadgets.IfrGadget.prototype.getServerBase = function() {
  return this.serverBase_;
};

gadgets.IfrGadget.prototype.getMainContent = function(continuation) {
  var iframeId = this.getIframeId();
  continuation('<div class="' + this.cssClassGadgetContent + '"><iframe id="' +
      iframeId + '" name="' + iframeId + '" class="' + this.cssClassGadget +
      '" src="' + this.getIframeUrl() +
      '" frameborder="0" scrolling="no"></iframe></div>');
};

gadgets.IfrGadget.prototype.getIframeId = function() {
  return this.GADGET_IFRAME_PREFIX_ + this.id;
};

gadgets.IfrGadget.prototype.getUserPrefsDialogId = function() {
  return this.getIframeId() + '_userPrefsDialog';
};

gadgets.IfrGadget.prototype.getIframeUrl = function() {
  return this.serverBase_ + 'ifr?url=' +
      encodeURIComponent(this.specUrl) + '&synd=' + this.SYND + '&mid=' +
      this.id + '&parent=' + encodeURIComponent(gadgets.container.parentUrl_) +
      '&ogc=' + document.location.host + this.getUserPrefsParams();
};

gadgets.IfrGadget.prototype.getUserPrefsParams = function() {
  var params = '';
  if (this.getUserPrefs()) {
    for(var name in this.getUserPrefs()) {
      var value = this.getUserPref(name);
      params += '&up_' + encodeURIComponent(name) + '=' +
          encodeURIComponent(value);
    }
  }
  return params;
}

gadgets.IfrGadget.prototype.handleToggle = function() {
  var gadgetIframe = document.getElementById(this.getIframeId());
  if (gadgetIframe) {
    var gadgetContent = gadgetIframe.parentNode;
    var display = gadgetContent.style.display;
    gadgetContent.style.display = display ? '' : 'none';
  }
};

gadgets.IfrGadget.prototype.handleOpenUserPrefsDialog = function() {
  if (this.userPrefsDialogContentLoaded) {
    this.showUserPrefsDialog();
  } else {
    var gadget = this;
    window['ig_callback_' + this.id] = function(userPrefsDialogContent) {
      gadget.userPrefsDialogContentLoaded = true;
      gadget.buildUserPrefsDialog(userPrefsDialogContent);
      gadget.showUserPrefsDialog();
    };

    var script = document.createElement('script');
    script.src = 'http://gmodules.com/ig/gadgetsettings?url=' + this.specUrl +
        '&mid=' + this.id + '&output=js' + this.getUserPrefsParams();
    document.body.appendChild(script);
  }
};

gadgets.IfrGadget.prototype.buildUserPrefsDialog = function(content) {
  var userPrefsDialog = document.getElementById(this.getUserPrefsDialogId());
  userPrefsDialog.innerHTML = content +
      '<div class="' + this.cssClassGadgetUserPrefsDialogActionBar +
      '"><input type="button" value="Save" onclick="gadgets.container.getGadget(' +
      this.id +').handleSaveUserPrefs()"> <input type="button" value="Cancel" onclick="gadgets.container.getGadget(' +
      this.id +').handleCancelUserPrefs()"></div>';
  userPrefsDialog.childNodes[0].style.display = '';
};

gadgets.IfrGadget.prototype.showUserPrefsDialog = function(show) {
  var userPrefsDialog = document.getElementById(this.getUserPrefsDialogId());
  userPrefsDialog.style.display = (show || show == undefined) ? '' : 'none';
}

gadgets.IfrGadget.prototype.hideUserPrefsDialog = function() {
  this.showUserPrefsDialog(false);
};

gadgets.IfrGadget.prototype.handleSaveUserPrefs = function() {
  this.hideUserPrefsDialog();

  var prefs = {};
  var numFields = document.getElementById('m_' + this.id +
      '_numfields').value;
  for (var i = 0; i < numFields; i++) {
    var input = document.getElementById('m_' + this.id + '_' + i);
    if (input.type != 'hidden') {
      var userPrefNamePrefix = 'm_' + this.id + '_up_';
      var userPrefName = input.name.substring(userPrefNamePrefix.length);
      var userPrefValue = input.value;
      prefs[userPrefName] = userPrefValue;
    }
  }

  this.setUserPrefs(prefs);
  this.refresh();
};

gadgets.IfrGadget.prototype.handleCancelUserPrefs = function() {
  this.hideUserPrefsDialog();
};

gadgets.IfrGadget.prototype.refresh = function() {
  var iframeId = this.getIframeId();
  document.getElementById(iframeId).src = this.getIframeUrl();
};


// ---------
// Container

/**
 * Container interface.
 * @constructor
 */
gadgets.Container = function() {
  this.gadgets_ = {};
  this.parentUrl_ = '';
};

gadgets.Container.inherits(gadgets.Extensible);

/**
 * Known dependencies:
 *     gadgetClass: constructor to create a new gadget instance
 *     userPrefStore: instance of a subclass of gadgets.UserPrefStore
 *     gadgetService: instance of a subclass of gadgets.GadgetService
 *     layoutManager: instance of a subclass of gadgets.LayoutManager
 */

gadgets.Container.prototype.gadgetClass = gadgets.Gadget;

gadgets.Container.prototype.userPrefStore =
    new gadgets.CookieBasedUserPrefStore();

gadgets.Container.prototype.gadgetService = new gadgets.GadgetService();

gadgets.Container.prototype.layoutManager =
    new gadgets.StaticLayoutManager();

gadgets.Container.prototype.setParentUrl = function(url) {
  this.parentUrl_ = url;
};

gadgets.Container.prototype.getGadgetKey_ = function(instanceId) {
  return 'gadget_' + instanceId;
};

gadgets.Container.prototype.getGadget = function(instanceId) {
  return this.gadgets_[this.getGadgetKey_(instanceId)];
};

gadgets.Container.prototype.createGadget = function(opt_params) {
  return new this.gadgetClass(opt_params);
};

gadgets.Container.prototype.addGadget = function(gadget) {
  gadget.id = this.getNextGadgetInstanceId();
  gadget.setUserPrefs(this.userPrefStore.getPrefs(gadget));
  this.gadgets_[this.getGadgetKey_(gadget.id)] = gadget;
};

gadgets.Container.prototype.addGadgets = function(gadgets) {
  for (var i = 0; i < gadgets.length; i++) {
    this.addGadget(gadgets[i]);
  }
};

/**
 * Renders all gadgets in the container.
 */
gadgets.Container.prototype.renderGadgets = function() {
  for (var key in this.gadgets_) {
    this.renderGadget(this.gadgets_[key]);
  }
};

/**
 * Renders a gadget.  Gadgets are rendered inside their chrome element.
 * @param {Object} gadget Gadget object
 */
gadgets.Container.prototype.renderGadget = function(gadget) {
  throw Error(gadgets.error.SUBCLASS_RESPONSIBILITY);
};

gadgets.Container.prototype.nextGadgetInstanceId_ = 0;

gadgets.Container.prototype.getNextGadgetInstanceId = function() {
  return this.nextGadgetInstanceId_++;
};


// ------------
// IfrContainer

/**
 * Container that renders gadget using ifr.
 * @constructor
 */
gadgets.IfrContainer = function() {
  gadgets.Container.call(this);
};

gadgets.IfrContainer.inherits(gadgets.Container);

gadgets.IfrContainer.prototype.gadgetClass = gadgets.IfrGadget;

gadgets.IfrContainer.prototype.gadgetService = new gadgets.IfrGadgetService();

gadgets.IfrContainer.prototype.setParentUrl = function(url) {
  if (!url.match(/^http[s]?:\/\//)) {
    url = document.location.href.match(/^[^?#]+\//)[0] + url;
  }

  /* Nasty hack to get around the hardcoded /ig/ifpc_relay URL */
  this.parentUrl_ = url + '?';
};

/**
 * Renders a gadget using ifr.
 * @param {Object} gadget Gadget object
 */
gadgets.IfrContainer.prototype.renderGadget = function(gadget) {
  var chrome = this.layoutManager.getGadgetChrome(gadget);
  gadget.render(chrome);
};

/**
 * Default container.
 */
gadgets.container = new gadgets.IfrContainer();
