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
 * @fileoverview Support for basic logging capability for gadgets.
 *
 * This functionality replaces alert(msg) and window.console.log(msg).
 *
 * <p>Currently only works on browsers with a console (WebKit based browsers,
 * Firefox with Firebug extension, or Opera).
 *
 * <p>API is designed to be equivalent to existing console.log | warn | error
 * logging APIs supported by Firebug and WebKit based browsers. The only
 * addition is the ability to call gadgets.setLogLevel().
 */

/**
 * @static
 * @namespace Support for basic logging capability for gadgets.
 * @name gadgets.log
 */

gadgets['log'] = (function() {
  /** @const */
  var info_ = 1;
  /** @const */
  var warning_ = 2;
  /** @const */
  var error_ = 3;
  /** @const */
  var none_ = 4;

  /**
 * Log an informational message
 * @param {Object} message - the message to log.
 * @member gadgets
 * @name log
 * @function
 */
  var log = function(message) {
    logAtLevel(info_, message);
  };

  /**
 * Log a warning
 * @param {Object} message - the message to log.
 * @static
 */
  gadgets.warn = function(message) {
    logAtLevel(warning_, message);
  };

  /**
 * Log an error
 * @param {Object} message - The message to log.
 * @static
 */
  gadgets.error = function(message) {
    logAtLevel(error_, message);
  };

  /**
 * Sets the log level threshold.
 * @param {number} logLevel - New log level threshold.
 * @static
 * @member gadgets.log
 * @name setLogLevel
 */
  gadgets['setLogLevel'] = function(logLevel) {
    logLevelThreshold_ = logLevel;
  };

  /**
 * Logs a log message if output console is available, and log threshold is met.
 * @param {number} level - the level to log with. Optional, defaults to gadgets.log.INFO.
 * @param {Object} message - The message to log.
 * @private
 */
  function logAtLevel(level, message) {
    if (level < logLevelThreshold_ || !_console) {
      return;
    }

    if (level === warning_ && _console.warn) {
      _console.warn(message);
    } else if (level === error_ && _console.error) {
      _console.error(message);
    } else if (_console.log) {
      _console.log(message);
    }
  };

  /**
 * Log level for informational logging.
 * @static
 * @const
 * @member gadgets.log
 * @name INFO
 */
  log['INFO'] = info_;

  /**
 * Log level for warning logging.
 * @static
 * @const
 * @member gadgets.log
 * @name WARNING
 */
  log['WARNING'] = warning_;

  /**
 * Log level for no logging
 * @static
 * @const
 * @member gadgets.log
 * @name NONE
 */
  log['NONE'] = none_;

  /**
 * Current log level threshold.
 * @type {number}
 * @private
 */
  var logLevelThreshold_ = info_;



  /**
 * Console to log to
 * @private
 * @static
 */
  var _console = window.console ? window.console :
                       window.opera ? window.opera.postError : undefined;

  return log;
})();
