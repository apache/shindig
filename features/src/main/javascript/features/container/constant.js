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
 * @enum {string}
 */
osapi.container.MetadataParam = {};
osapi.container.MetadataParam.LOCAL_EXPIRE_TIME = 'localExpireTimeMs';
osapi.container.MetadataParam.URL = 'url';


/**
 * Constants to key into gadget metadata response JSON.
 * @enum {string}
 */
osapi.container.MetadataResponse = {};
osapi.container.MetadataResponse.IFRAME_URL = 'iframeUrl';
osapi.container.MetadataResponse.NEEDS_TOKEN_REFRESH = 'needsTokenRefresh';
osapi.container.MetadataResponse.VIEWS = 'views';
osapi.container.MetadataResponse.EXPIRE_TIME_MS = 'expireTimeMs';
osapi.container.MetadataResponse.FEATURES = 'features';
osapi.container.MetadataResponse.HEIGHT = 'height';
osapi.container.MetadataResponse.MODULE_PREFS = 'modulePrefs';
osapi.container.MetadataResponse.PREFERRED_HEIGHT = 'preferredHeight';
osapi.container.MetadataResponse.PREFERRED_WIDTH = 'preferredWidth';
osapi.container.MetadataResponse.RESPONSE_TIME_MS = 'responseTimeMs';
osapi.container.MetadataResponse.WIDTH = 'width';


/**
 * Constants to key into gadget token response JSON.
 * @enum {string}
 */
osapi.container.TokenResponse = {};
osapi.container.TokenResponse.TOKEN = 'token';


/**
 * Constants to key into timing response JSON.
 * @type {string}
 */
osapi.container.NavigateTiming = {};
// The gadget URL reporting this timing information.
osapi.container.NavigateTiming.URL = 'url';
// The gadget site ID reporting this timing information.
osapi.container.NavigateTiming.ID = 'id';
// Absolute time (ms) when gadget navigation is requested.
osapi.container.NavigateTiming.START = 'start';
// Time (ms) to receive XHR response time. In CC, for metadata and token.
osapi.container.NavigateTiming.XRT = 'xrt';
// Time (ms) to receive first byte. Typically timed at start of page.
osapi.container.NavigateTiming.SRT = 'srt';
// Time (ms) to load the DOM. Typically timed at end of page.
osapi.container.NavigateTiming.DL = 'dl';
// Time (ms) when body onload is called.
osapi.container.NavigateTiming.OL = 'ol';
// Time (ms) when page is ready for use. Typically happen after data XHR (ex:
// calendar, email) is received/presented to users. Overridable by user.
osapi.container.NavigateTiming.PRT = 'prt';


/**
 * Constants to key into request renderParam JSON.
 * @enum {string}
 */
osapi.container.RenderParam = {};

/**
 * Allow gadgets to render in unspecified view.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.ALLOW_DEFAULT_VIEW = 'allowDefaultView';

/**
 * Whether to enable cajole mode.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.CAJOLE = 'cajole';

/**
 * Style class to associate to iframe.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.CLASS = 'class';

/**
 * Whether to enable debugging mode.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.DEBUG = 'debug';

/**
 * The starting gadget iframe height (in pixels).
 * @type {string}
 * @const
 */
osapi.container.RenderParam.HEIGHT = 'height';

/**
 * Whether to disable cache.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.NO_CACHE = 'nocache';

/**
 * Whether to enable test mode.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.TEST_MODE = 'testmode';

/**
 * The gadget user prefs to render with.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.USER_PREFS = 'userPrefs';

/**
 * The view of gadget to render.
 * @type {string}
 * @const
 */
osapi.container.RenderParam.VIEW = 'view';

/**
 * The starting gadget iframe width (in pixels).
 * @type {string}
 * @const
 */
osapi.container.RenderParam.WIDTH = 'width';


/**
 * Constants to key into request viewParam JSON.
 * @enum {string}
 */
osapi.container.ViewParam = {};
osapi.container.ViewParam.VIEW = 'view';
