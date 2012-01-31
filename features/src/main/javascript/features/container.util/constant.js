/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */


/**
 * @fileoverview Constants used throughout common container.
 */


/**
 * Set up namespace.
 * @type {Object}
 */
osapi.container = {};


/**
 * Constants to key into gadget metadata state.
 * @const
 * @enum {string}
 */
osapi.container.MetadataParam = {
    LOCAL_EXPIRE_TIME: 'localExpireTimeMs',
    URL: 'url'
};


/**
 * Constants to key into gadget metadata response JSON.
 * @enum {string}
 */

osapi.container.MetadataResponse = {
  IFRAME_URLS: 'iframeUrls',
  NEEDS_TOKEN_REFRESH: 'needsTokenRefresh',
  VIEWS: 'views',
  EXPIRE_TIME_MS: 'expireTimeMs',
  FEATURES: 'features',
  HEIGHT: 'height',
  MODULE_PREFS: 'modulePrefs',
  PREFERRED_HEIGHT: 'preferredHeight',
  PREFERRED_WIDTH: 'preferredWidth',
  RESPONSE_TIME_MS: 'responseTimeMs',
  WIDTH: 'width',
  TOKEN_TTL: 'tokenTTL'
};


/**
 * Constants to key into gadget token response JSON.
 * @enum {string}
 */
osapi.container.TokenResponse = {
  TOKEN: 'token',
  TOKEN_TTL: 'tokenTTL',
  MODULE_ID: 'moduleId'
};


/**
 * Constants to key into timing response JSON.
 * @enum {string}
 */
osapi.container.NavigateTiming = {
  /** The gadget URL reporting this timing information. */
  URL: 'url',
  /** The gadget site ID reporting this timing information. */
  ID: 'id',
  /** Absolute time (ms) when gadget navigation is requested. */
  START: 'start',
  /** Time (ms) to receive XHR response time. In CC, for metadata and token. */
  XRT: 'xrt',
  /** Time (ms) to receive first byte. Typically timed at start of page. */
  SRT: 'srt',
  /** Time (ms) to load the DOM. Typically timed at end of page. */
  DL: 'dl',
  /** Time (ms) when body onload is called. */
  OL: 'ol',
  /** Time (ms) when page is ready for use. Typically happen after data XHR (ex:
   * calendar, email) is received/presented to users. Overridable by user.
   */
  PRT: 'prt'
};


/**
 * Constants to key into request renderParam JSON.
 * @enum {string}
 * @const
 */
osapi.container.RenderParam = {
    /** Allow gadgets to render in unspecified view. */
    ALLOW_DEFAULT_VIEW: 'allowDefaultView',

    /** Whether to enable cajole mode. */
    CAJOLE: 'cajole',

    /** Style class to associate to iframe. */
    CLASS: 'class',

    /** Whether to enable debugging mode. */
    DEBUG: 'debug',

    /** The starting gadget iframe height (in pixels). */
    HEIGHT: 'height',

    /** Whether to disable cache. */
    NO_CACHE: 'nocache',

    /** Whether to enable test mode. */
    TEST_MODE: 'testmode',

    /** The gadget user prefs to render with. */
    USER_PREFS: 'userPrefs',

    /** The view of gadget to render. */
    VIEW: 'view',

    /** The starting gadget iframe width (in pixels). */
    WIDTH: 'width',

    /**
     * The modduleId of this gadget.  Used to identify saved instances of gadgets.
     * Defaults to 0, which means the instance of the gadget is not saved.
     */
    MODULE_ID: 'moduleid'
};

/**
 * Constants to key into request viewParam JSON.
 * @enum {string}
 */
osapi.container.ViewParam = {
  VIEW: 'view'
};

/**
 * Constants to define lifecycle callback
 * @enum {string}
 */
osapi.container.CallbackType = {
    /** Called before a gadget(s) is preloaded. */
    ON_BEFORE_PRELOAD: 'onBeforePreload',

    /** Called after a gadget(s) has finished preloading. */
    ON_PRELOADED: 'onPreloaded',

    /** Called before navigate is called. */
    ON_BEFORE_NAVIGATE: 'onBeforeNavigate',

    /** Called after navigation has completed. */
    ON_NAVIGATED: 'onNavigated',

    /** Called before a gadget is closed. */
    ON_BEFORE_CLOSE: 'onBeforeClose',

    /** Called after a gadget has been closed. */
    ON_CLOSED: 'onClosed',

    /** Called before a gadget has been unloaded. */
    ON_BEFORE_UNLOAD: 'onBeforeUnload',

    /** Called after a gadget has been unloaded. */
    ON_UNLOADED: 'onUnloaded',

    /** Called before render is called. */
    ON_BEFORE_RENDER: 'onBeforeRender',

    /** Called after a gadget has rendered. */
    ON_RENDER: 'onRender',

    /** Name of the global function all gadgets will call when they are loaded. */
    GADGET_ON_LOAD: '__gadgetOnLoad'
};

/**
 * Enumeration of configuration keys for a osapi.container.Container. This is specified in
 * JSON to provide extensible configuration. These enum values are for
 * documentation purposes only, it is expected that clients use the string
 * values.
 * @enum {string}
 */
osapi.container.ContainerConfig = {
  /**
   * Allow gadgets to render in unspecified view.
   * @type {string}
   * @const
   */
  ALLOW_DEFAULT_VIEW: 'allowDefaultView',

  /**
   * Whether cajole mode is turned on.
   * @type {string}
   * @const
   */
  RENDER_CAJOLE: 'renderCajole',

  /**
   * Whether debug mode is turned on.
   * @type {string}
   * @const
   */
  RENDER_DEBUG: 'renderDebug',

  /**
   * The debug param name to look for in container URL for per-request debugging.
   * @type {string}
   * @const
   */
  RENDER_DEBUG_PARAM: 'renderDebugParam',

  /**
   * Whether test mode is turned on.
   * @type {string}
   * @const
   */
  RENDER_TEST: 'renderTest',

  /**
   * Security token refresh interval (in ms). Set to 0 in config to disable
   * token refresh.
   *
   * This number should always be >= 0. The smallest encountered token ttl or this
   * number will be used as the refresh interval, whichever is smaller.
   *
   * @type {string}
   * @const
   */
  TOKEN_REFRESH_INTERVAL: 'tokenRefreshInterval',

  /**
   * Globally-defined callback function upon gadget navigation. Useful to
   * broadcast timing and stat information back to container.
   * @type {string}
   * @const
   */
  NAVIGATE_CALLBACK: 'navigateCallback',

  /**
   * Provide server reference time for preloaded data.
   * This time is used instead of each response time in order to support server
   * caching of results.
   * @type {number}
   * @const
   */
  PRELOAD_REF_TIME: 'preloadRefTime',

  /**
   * Preloaded hash of gadgets metadata
   * @type {Object}
   * @const
   */
  PRELOAD_METADATAS: 'preloadMetadatas',

  /**
   * Preloaded hash of gadgets tokens
   * @type {Object}
   * @const
   */
  PRELOAD_TOKENS: 'preloadTokens',

  /**
   * Used to query the language locale part of the container page.
   * @type {function}
   */
  GET_LANGUAGE: 'GET_LANGUAGE',

  /**
   * Used to query the country locale part of the container page.
   * @type {function}
   */
  GET_COUNTRY: 'GET_COUNTRY',

  /**
   * Used to retrieve the persisted preferences for a gadget.
   * @type {function}
   */
  GET_PREFERENCES: 'GET_PREFERENCES',

  /**
   * Used to persist preferences for a gadget.
   * @type {function}
   */
  SET_PREFERENCES: 'SET_PREFERENCES',

  /**
   * Used to arbitrate RPC calls.
   * @type {function}
   */
  RPC_ARBITRATOR: 'rpcArbitrator',

  /**
   * Used to retrieve security tokens for gadgets.
   * @type {function}
   */
  GET_GADGET_TOKEN: 'GET_GADGET_TOKEN',

  /**
   * Used to retrieve a security token for the container.
   * Containers who specify this config value can call
   * CommonContainer.updateContainerSecurityToken after the creation of the
   * common container to start the scheduling of container token refreshes.
   *
   * @type {function(function)=}
   * @param {function(String, number)} callback The function to call to report
   *   the updated token and the token's new time-to-live in seconds. This
   *   callback function must be called with unspecified values in the event of
   *   an error.
   *
   *   The first and second arguments to this callback function are the same as
   *   the second and third arguments to:
   *     osapi.container.Container.prototype.updateContainerSecurityToken
   *   Example:
   *   <code>
   *     var config = {};
   *     config[osapi.container.ContainerConfig.GET_CONTAINER_TOKEN] = function(result) {
   *       var token, ttl, error = false;
   *       // Do work to set token and ttl values
   *       if (error) {
   *         result();
   *       } else {
   *         result(token, ttl);
   *       }
   *     };
   *   </code>
   * @see osapi.container.Container.prototype.updateContainerSecurityToken
   */
  GET_CONTAINER_TOKEN: 'GET_CONTAINER_TOKEN'
};

/**
 * Enumeration of configuration keys for a osapi.container.Service. This is specified in
 * JSON to provide extensible configuration.
 * @enum {string}
 */
osapi.container.ServiceConfig = {
  /**
   * Host to fetch gadget information, via XHR.
   * @type {string}
   * @const
   */
  API_HOST: 'apiHost',

  /**
   * Path to fetch gadget information, via XHR.
   * @type {string}
   * @const
   */
  API_PATH: 'apiPath'
}