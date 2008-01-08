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
 *   <script src="http://apache.org/shindig/prefs.js"></script>
 *   <script>
 *   gadgets.Prefs.parseUrl();
 *   var prefs = new gadgets.Prefs();
 *   var name = prefs.getString("name");
 *   </script>
 */

var gadgets = gadgets || {};

/**
 * Stores preferences for the default shindig implementation.
 * @private
 */
gadgets.PrefStore_ = function() {
  var modules = {};

  /**
   * Returns the module named by moduleId
   * @param {Number | String} moduleId The module id to fetch.
   * @return {Object} An object containing module data.
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
   * @param {Number | String} moduleId The module id to add the pref for.
   * @param {String} key The key to add. May be an object where keys = key and
   *     values = value.
   * @param {String} value The value for the key. Optional.
   */
  function setPref(moduleId, key, value) {
    var module = getModuleData(moduleId);
    if (typeof key !== "string") {
      for (var i in key) {
        module.prefs[i] = key[i];
      }
    } else {
      module.prefs[key] = value;
    }
  }

  /**
   * Adds a new message to the stored set for the given module id.
   *
   * @param {Number | String} moduleId The module id to add the pref for.
   * @param {String | Object} key The key to add. May be an object where keys =
   *     key and values = value.
   * @param {String} value The value for the key. Optional.
   */
  function setMsg(moduleId, key, value) {
    var module = getModuleData(moduleId);
    if (typeof key !== "string") {
      for (var i in key) {
        module.msgs[i] = key[i];
      }
    } else {
      module.msgs[key] = value;
    }
  }

  var defaultModuleId = 0;

  /**
   * @param {String | Number} moduleId The module id to set as default.
   */
  function setDefaultModuleId(moduleId) {
    defaultModuleId = moduleId;
  }

  /**
   * @return {String | Number} The default module id.
   */
  function getDefaultModuleId() {
    return defaultModuleId;
  }

  /**
   * @param {Number | String} moduleId The module id to set the language for.
   * @param {String} language The language to use.
   */
  function setLanguage(moduleId, language) {
    getModuleData(moduleId).language = language;
  }

  /**
   * Sets the default country for this module id.
   */
  function setCountry(moduleId, country) {
    getModuleData(moduleId).country = country;
  }

  // Export public API.
  return {
    setPref:setPref,
    setMsg:setMsg,
    setCountry:setCountry,
    setLanguage:setLanguage,
    getModuleData:getModuleData,
    setDefaultModuleId:setDefaultModuleId,
    getDefaultModuleId:getDefaultModuleId
  };
}();

/**
 * @constructor
 * @param {String | Number} moduleId The module id to create prefs for.
 */
gadgets.Prefs = function(moduleId) {
  if (typeof moduleId === "undefined") {
    this.moduleId_ = gadgets.PrefStore_.getDefaultModuleId();
  } else {
    this.moduleId_ = moduleId;
  }
  this.data_ = gadgets.PrefStore_.getModuleData(this.moduleId_);
  // This is used to eliminate one hash table lookup per value fetched.
  this.prefs_ = this.data_.prefs;
  this.msgs_ = this.data_.msgs;
};

/**
 * Static pref parser. Parses all parameters from the url and stores them
 * for later use when creating a new gadgets.Prefs object.
 * You should only ever call this if you are a type=url gadget.
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
  gadgets.PrefStore_.setDefaultModuleId(moduleId);
  gadgets.PrefStore_.setPref(moduleId, prefs);
  gadgets.PrefStore_.setMsg(moduleId, msgs);
  gadgets.PrefStore_.setLanguage(moduleId, language);
  gadgets.PrefStore_.setCountry(moduleId, country);
};

/**
 * Internal helper for pref fetching.
 * @param {String} key The key to fetch.
 * @return {String}
 */
gadgets.Prefs.prototype.getPref_ = function(key) {
  var val = this.prefs_[key];
  return typeof val === "undefined" ? null : val;
}

/**
 * Retrieves the named preference as a string.
 * @param {String} key The preference to fetch.
 * @return {String} The preference. If not set, an empty string.
 */
gadgets.Prefs.prototype.getString = function(key) {
  var val = this.getPref_(key);
  return val === null ? "" : val;
};

/**
 * Retrieves the named preference as an integer.
 * @param {String} key The preference to fetch.
 * @return {Number} The preference. If not set, 0.
 */
gadgets.Prefs.prototype.getInt = function(key) {
  var val = parseInt(this.getPref_(key), 10);
  return isNaN(val) ? 0 : val;
};

/**
 * Retrieve the named preference as a floating point value.
 * @param {String} key The preference to fetch.
 * @return {Number} The preference. If not set, 0.
 */
gadgets.Prefs.prototype.getFloat = function(key) {
  var val = parseFloat(this.getPref_(key));
  return isNaN(val) ? 0 : val;
};

/**
 * Retrieves the named preference as a boolean.
 * @param {String} key The preference to fetch.
 * @return {Boolean} The preference. If not set, false.
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
 * @param {String | Object} key The pref to store.
 * @param {String} val The values to store.
 */
gadgets.Prefs.prototype.set = function(key, value) {
  throw new Error("setprefs feature required to make this call.");
};

/**
 * Retrieves the named preference as an array.
 * @param {String} key The preference to fetch.
 * @return {Array.<String>} The preference. If not set, an empty array.
 *     UserPref values that were not declared as lists will be treated as
 *     1 element arrays.
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
 * Stores a preference from the given list.
 * @param {String} key The pref to store.
 * @param {Array.<String | Number>} val The values to store.
 */
gadgets.Prefs.prototype.setArray = function(key, val) {
  throw new Error("setprefs feature required to make this call.");
};

/**
 * Fetches an unformatted message.
 * @param {String} key The message to fetch
 * @return {String} The message.
 */
gadgets.Prefs.prototype.getMsg = function(key) {
  var val = this.msgs_[key];
  return typeof val === "undefined" ? "" : val;
};

/**
 * The regex pulls out the text before and after the positional argument
 * and digs down for a possible example value in case no actual value
 * was provided.  It is used by the function getMsgFormatted.
 *
 * Example: "Foo <ph name="number"><ex>bar</ex>%1</ph> baz."
 * 0 = "Foo <ph name="number"><ex>bar</ex>%1</ph> baz." : match for the
 *     whole regex.
 *
 * 1 = "Foo " : matches first (.*) in regex
 *
 * 2 = "<ph name="number"><ex>bar</ex>%1</ph>" : matches
 *     (\<ph.*?\>\s*(\<ex\>(.*?)\<\/ex\>)?\s*%1\s*\<\/ph\>) in regex
 * 3 = "<ex>bar</ex>" : matches (\<ex\>(.*?)\<\/ex\>)? in regex, since it
 *     is an optional param it may have the value "undefined"
 * 4 = "bar" : matches (.*?) in regex (it is a non-greedy regex)
 *     if 3=undefined then 4 = "undefined".
 *
 * 5 = " baz." : matches final (.*) in regex
 *
 * TODO: this may need to be a single line even though it's > 80 characters
 * because some browsers may not properly interepret the line continuation.
 */
gadgets.Prefs.MESSAGE_SUBST_REGEX =
    /(.*)(\<ph.*?\>\s*(\<ex\>(.*?)\<\/ex\>)?\s*%1\s*\<\/ph\>)(.*)/;

/**
 * Returns a message value with the positional argument opt_subst in place if
 * it is provided or the provided example value if it is not, or the empty
 * string if the message is not found.
 * Eventually we may provide controls to return different default messages.
 *
 * @param {String} key The message to fetch.
 * @param {String} subst ????
 * @return {String} The formatted string.
 */
gadgets.Prefs.prototype.getMsgFormatted = function(key, opt_subst) {
  var val = this.getMsg(key);

  var result = val.match(gadgets.Prefs.MESSAGE_SUBST_REGEX);
  // Allows string that should be getMsg to also call getMsgFormatted
  if (!result || !result[0]) {
    return val;
  }
  if (typeof opt_subst === "undefined") {
    var sub = result[4] || "";
    return result[1] + sub + result[5];
  }
  return result[1] + opt_subst + result[5];
};

/**
 * @return {String} The country for this module instance.
 */
gadgets.Prefs.prototype.getCountry = function() {
  return this.data_.country;
};

/**
 * @return {String} The language for this module instance.
 */
gadgets.Prefs.prototype.getLang = function() {
  return this.data_.language;
};

/**
 * @return {String | Number} The module id for this module instance.
 */
gadgets.Prefs.prototype.getModuleId = function() {
  return this.moduleId_;
};
