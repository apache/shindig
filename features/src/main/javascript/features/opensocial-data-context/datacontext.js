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
 */

var opensocial = opensocial || {};

/**
 * @type {Object} The namespace declaration for this file.
 */
opensocial.data = opensocial.data || {};

/**
 * @type {Object} Global DataContext to contain requested data sets.
 */
opensocial.data.DataContext = function() {
  var listeners = [];
  var dataSets = {};

  /**
   * Checks if the data for a map of keys is available.
   * @param {Object<string, ?>} An map of keys to check.
   * @return {boolean} Data for all the keys is present.
   */
  var isDataReady = function(keys) {
    if (keys['*']) {
      return true;
    }
    
    for (var key in keys) {
      if (typeof dataSets[key] === 'undefined') {
        return false;
      }
    }
    return true;
  };
    
    
  /**
   * Fires a listener for a key, but only if the data is ready for other
   * keys this listener is bound to.
   * @param {Object} listener The listener object.
   * @param {string} key The key that this listener is being fired for.
   */
  var maybeFireListener = function(listener, key) {
    if (isDataReady(listener.keys)) {
      if (listener.callback(key) == opensocial.data.DataContext.REMOVE_LISTENER) {
        removeListener(listener);
      }
    }
  };
    
  /**
   * Removes a listener from the list.
   * @param {Object} listener The listener to remove.
   */
  var removeListener = function(listener) {
    for (var i = 0; i < listeners.length; ++i) {
      if (listeners[i] == listener) {
        listeners.splice(i, 1);
        return;
      }
    }
  };
    
  /**
   * Scans all active listeners and fires off any callbacks that inserting this
   * key or list of keys satisfies.
   * @param {string|Array<string>} keys The key that was updated.
   * @private
   */
  var fireCallbacks = function(keys) {
    if (typeof(keys) == "string") {
      keys = [ keys ];
    }
    for (var i = 0; i < listeners.length; ++i) {
      var listener = listeners[i];
      for (var j = 0; j < keys.length; j++) {
        var key = keys[j];
        if (listener.keys[key] || listener.keys['*']) {
          maybeFireListener(listener, key);
          break;
        }
      }
    }
  };


  return {
    
    REMOVE_LISTENER : "remove",
    
    /**
     * Map of existing data.  This is used externally by both the
     * opensocial-data and opensocial-templates feature, hence is
     * not hidden.
     */
    dataSets : dataSets,
    
    
    /**
     * Registers a callback listener for a given set of keys.
     * @param {string|Array.<string>} keys Key or set of keys to listen on.
     * @param {Function(string)} callback Function to call when a listener is fired.
     * TODO: Should return a value that can later be used to return
     *     a value.
     */
    registerListener : function(keys, callback) {
      var listener = {keys : {}, callback : callback};

      if (typeof keys === 'string') {
        listener.keys[keys] = true;
      } else {
        for (var i = 0; i < keys.length; i++) {
          listener.keys[keys[i]] = true;
        }
      }
      
      listeners.push(listener);
    
      // Check to see if this one should fire immediately.
      if (keys !== '*' && isDataReady(listener.keys)) {
        window.setTimeout(function() {
          maybeFireListener(listener, keys);
        }, 1);
      }
    },
    
    
    /**
     * Retrieve a data set for a given key.
     * @param {string} key Key for the requested data set.
     * @return {Object} The data set object.
     */
    getDataSet : function(key) {
      return dataSets[key];
    },
    
    
    /**
     * Puts a data set into the global DataContext object. Fires listeners
     * if they are satisfied by the associated key being inserted.
     *
     * Note that if this is passed a ResponseItem object, it will crack it open
     * and extract the JSON payload of the wrapped API Object. This includes
     * iterating over an array of API objects and extracting their JSON into a
     * simple array structure.
     *
     * @param {string} key The key to associate with this object.
     * @param {ResponseItem|Object} obj The data object.
     * @param {boolean} opt_fireListeners Default true.
     */
    putDataSet : function(key, obj, opt_fireListeners) {
      var data = obj;
      if (typeof data === 'undefined' || data === null) {
        return;
      }
    
      // NOTE: This is ugly, but required since we need to get access
      // to the JSON/Array payload of API responses.
      // This will crack the actual API objects and extract their JSON payloads.
      // TODO: this code block is not described by the spec, and should be removed.
      // Developers using ResponseItems should call getData() themselves.
      if (data.getData) {
       data = data.getData();
       if (data.array_) {
         var out = [];
         for (var i = 0; i < data.array_.length; i++) {
           out.push(data.array_[i].fields_);
         }
         data = out;
       } else {
         data = data.fields_ || data;
       }
      }
    
      dataSets[key] = data;
      if (!(opt_fireListeners === false)) {
        fireCallbacks(key);
      }
    }, 
    
    /**
     * Inserts multiple data sets from a JSON object.
     * @param {Object<string, Object>} dataSets a JSON object containing Data
     * sets keyed by Data Set Key. All the DataSets are added, before firing
     * listeners.
     */
    putDataSets : function(dataSets) {
      var keys = [];
      for (var key in dataSets) {
        keys.push(key);
        this.putDataSet(key, dataSets[key], false);
      }
      fireCallbacks(keys);
    }
  }
}();


/**
 * Accessor to the shared, global DataContext.
 */
opensocial.data.getDataContext = function() {
  return opensocial.data.DataContext;
};

