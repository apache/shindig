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

/**
 * @fileoverview
 *
 * Provides access to user prefs, module dimensions, and messages.
 *
 * Clients can access their preferences by constructing an instance of
 * gadgets.Prefs and passing in their module id.  Example:
 *
 *   var prefs = new gadgets.Prefs();
 *   var name = prefs.getString("name");
 *   var lang = prefs.getLang();
 *
 * Modules with type=url can also use this library to parse arguments passed
 * by URL, but this is not the common case:
 *
 *   &lt;script src="http://apache.org/shindig/prefs.js"&gt;&lt;/script&gt;
 *   &lt;script&gt;
 *   gadgets.Prefs.parseUrl();
 *   var prefs = new gadgets.Prefs();
 *   var name = prefs.getString("name");
 *   &lt;/script&lg;
 */

var gadgets = gadgets || {};

/**
 * Stores preferences for the default shindig implementation.
 * @private
 */
gadgets.prefs_ = function() {
  var modules = {};

  /**
   * Returns the module named by moduleId
   * @param {Number | String} moduleId The module id to fetch
   * @return {Object} An object containing module data
   */
  function getModuleData(moduleId) {
    if (!modules[moduleId]) {
      modules[moduleId] = {
        prefs:{},
        msgs:{},
        language:"all",
        country:"all"
      };
    }
    return modules[moduleId];
  }

  /**
   * Adds a new user preference to the stored set for the given module id.
   *
   * @param {String | Number} moduleId The module id to add the pref for
   * @param {String} key The key to add; may be an object where keys = key and
   *     values = value
   * @param {String} opt_value An optional value used to set the value of the
   *     key.
   */
  function setPref(moduleId, key, opt_value) {
    var module = getModuleData(moduleId);
    if (typeof key !== "string") {
      for (var i in key) {
        module.prefs[i] = key[i];
      }
    } else {
      module.prefs[key] = opt_value;
    }
  }

  /**
   * Adds a new message to the stored set for the given module id.
   *
   * @param {String | Number} moduleId The module id to add the pref for
   * @param {String | Object} key The key to add; may be an object where keys =
   *     key and values = value
   * @param {String} opt_value An optional value used to set the value of the
   *     key.
   */
  function setMsg(moduleId, key, opt_value) {
    var module = getModuleData(moduleId);
    if (typeof key !== "string") {
      for (var i in key) {
        module.msgs[i] = key[i];
      }
    } else {
      module.msgs[key] = opt_value;
    }
  }

  var defaultModuleId = 0;

  /**
   * Sets the default module id.
   *
   * @param {String | Number} moduleId The module id to set as default
   */
  function setDefaultModuleId(moduleId) {
    defaultModuleId = moduleId;
  }

  /**
   * Gets the default module id.
   *
   * @return {String | Number} The default module id
   */
  function getDefaultModuleId() {
    return defaultModuleId;
  }

  /**
   * Sets the default language for this module id.
   *
   * @param {String | Number} moduleId The module id to set the language for
   * @param {String} language The language code as an ISO 639-1 code
   */
  function setLanguage(moduleId, language) {
    getModuleData(moduleId).language = language;
  }

  /**
   * Sets the default country for this module id.
   *
   * @param {String | Number} moduleId The id of the gagdet instance
   * @param {String} country The country code as an ISO 3166-1 alpha-2 code
   */
  function setCountry(moduleId, country) {
    getModuleData(moduleId).country = country;
  }

  // Export public API for the gadget container code. Gadget authors should
  // not use this class.
  return {
    setPref: setPref,
    setMsg: setMsg,
    setCountry: setCountry,
    setLanguage: setLanguage,
    getModuleData: getModuleData,
    setDefaultModuleId: setDefaultModuleId,
    getDefaultModuleId: getDefaultModuleId
  };
}();

/**
 * @class
 * Provides access to user preferences, module dimensions, and messages.
 *
 * Clients can access their preferences by constructing an instance of
 * gadgets.Prefs and passing in their module id.  Example:
 *
<pre>var prefs = new gadgets.Prefs();
var name = prefs.getString("name");
var lang = prefs.getLang();</pre>
 *
 * @description Creates a new Prefs object.
 * @param {String | Number} opt_moduleId An optional parameter specifying the
 *     module id to create prefs for; if not provided, the default module id
 *     is used
 */
gadgets.Prefs = function(opt_moduleId) {
  if (typeof opt_moduleId === "undefined") {
    this.moduleId_ = gadgets.prefs_.getDefaultModuleId();
  } else {
    this.moduleId_ = opt_moduleId;
  }
  this.data_ = gadgets.prefs_.getModuleData(this.moduleId_);
  // This is used to eliminate one hash table lookup per value fetched.
  this.prefs_ = this.data_.prefs;
  this.msgs_ = this.data_.msgs;
};

/**
 * @static
 * @method
 * @scope gadgets.Prefs
 *
 * Static pref parser. Parses all parameters from the url and stores them
 * for later use when creating a new gadgets.Prefs object.
 * You should only ever call this if you are a type=url gadget.
 *
 * @param {String | Number} moduleId The id of the gadget instance
 * @private
 */
gadgets.Prefs.parseUrl = function(moduleId) {
  var prefs = {};
  var msgs = {};
  var country = "all";
  var language = "all";
  if (gadgets.util) {
    var params = gadgets.util.getUrlParameters();
    for (var i in params) {
      if (i.indexOf("up_") === 0 && i.length > 3) {
        prefs[i.substr(3)] = String(params[i]);
      } else if (i.indexOf("msg_") === 0 && i.length > 4) {
        msgs[i.substr(4)] = String(params[i]);
      } else if (i === "country") {
        country = params[i];
      } else if (i === "lang") {
        language = params[i];
      } else if (i === "mid") {
        moduleId = params[i];
      }
    }
  }
  gadgets.prefs_.setDefaultModuleId(moduleId);
  gadgets.prefs_.setPref(moduleId, prefs);
  gadgets.prefs_.setMsg(moduleId, msgs);
  gadgets.prefs_.setLanguage(moduleId, language);
  gadgets.prefs_.setCountry(moduleId, country);
};

/**
 * Internal helper for pref fetching.
 * @param {String} key The key to fetch
 * @return {Object} The preference
 * @private
 */
gadgets.Prefs.prototype.getPref_ = function(key) {
  var val = this.prefs_[key];
  return typeof val === "undefined" ? null : val;
}

/**
 * Retrieves a preference as a string.
 * @param {String} key The preference to fetch
 * @return {String} The preference; if not set, an empty string
 */
gadgets.Prefs.prototype.getString = function(key) {
  var val = this.getPref_(key);
  return val === null ? "" : val;
};

/**
 * Retrieves a preference as an integer.
 * @param {String} key The preference to fetch
 * @return {Number} The preference; if not set, 0
 */
gadgets.Prefs.prototype.getInt = function(key) {
  var val = parseInt(this.getPref_(key), 10);
  return isNaN(val) ? 0 : val;
};

/**
 * Retrieves a preference as a floating-point value.
 * @param {String} key The preference to fetch
 * @return {Number} The preference; if not set, 0
 */
gadgets.Prefs.prototype.getFloat = function(key) {
  var val = parseFloat(this.getPref_(key));
  return isNaN(val) ? 0 : val;
};

/**
 * Retrieves a preference as a boolean.
 * @param {String} key The preference to fetch
 * @return {Boolean} The preference; if not set, false
 */
gadgets.Prefs.prototype.getBool = function(key) {
  var val = this.getPref_(key);
  if (val !== null) {
    return val === "true" || val === true || !!parseInt(val, 10);
  }
  return false;
};

/**
 * Stores a preference.
 * To use this call,
 * the gadget must require the feature setprefs.
 *
 * <p class="note">
 * <b>Note:</b>
 * If the gadget needs to store an Array it should use setArray instead of
 * this call.
 * </p>
 *
 * @param {String} key The pref to store
 * @param {Object} val The values to store
 */
gadgets.Prefs.prototype.set = function(key, value) {
  throw new Error("setprefs feature required to make this call.");
};

/**
 * Retrieves a preference as an array.
 * UserPref values that were not declared as lists are treated as
 * one-element arrays.
 *
 * @param {String} key The preference to fetch
 * @return {Array.&lt;String&gt;} The preference; if not set, an empty array
 */
gadgets.Prefs.prototype.getArray = function(key) {
  var val = this.getPref_(key);
  if (val !== null) {
    var arr = val.split("|");
    // Decode pipe characters.
    for (var i = 0, j = arr.length; i < j; ++i) {
      arr[i] = arr[i].replace(/%7C/g, "|");
    }
    return arr;
  }
  return [];
};

/**
 * Stores an array preference.
 * To use this call,
 * the gadget must require the feature setprefs.
 *
 * @param {String} key The pref to store
 * @param {Array} val The values to store
 */
gadgets.Prefs.prototype.setArray = function(key, val) {
  throw new Error("setprefs feature required to make this call.");
};

/**
 * Fetches an unformatted message.
 * @param {String} key The message to fetch
 * @return {String} The message
 */
gadgets.Prefs.prototype.getMsg = function(key) {
  var val = this.msgs_[key];
  return typeof val === "undefined" ? "" : val;
};

/**
 * Gets the current country, returned as ISO 3166-1 alpha-2 code.
 *
 * @return {String} The country for this module instance
 */
gadgets.Prefs.prototype.getCountry = function() {
  return this.data_.country;
};

/**
 * Gets the current language the gadget should use when rendering, returned as a
 * ISO 639-1 language code.
 *
 * @return {String} The language for this module instance
 */
gadgets.Prefs.prototype.getLang = function() {
  return this.data_.language;
};

/**
 * Gets the module id for the current instance.
 *
 * @return {String | Number} The module id for this module instance
 */
gadgets.Prefs.prototype.getModuleId = function() {
  return this.moduleId_;
};
