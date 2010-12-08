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
shindig.container = {};


/**
 * Constants to key into gadget metadata state.
 * @enum {string}
 */
shindig.container.MetadataParam = {};
shindig.container.MetadataParam.LOCAL_EXPIRE_TIME = 'localExpireTimeMs';
shindig.container.MetadataParam.URL = 'url';


/**
 * Constants to key into gadget metadata response JSON.
 * @enum {string}
 */
shindig.container.MetadataResponse = {};
shindig.container.MetadataResponse.IFRAME_URL = 'iframeUrl';
shindig.container.MetadataResponse.NEEDS_TOKEN_REFRESH = 'needsTokenRefresh';
shindig.container.MetadataResponse.VIEWS = 'views';
shindig.container.MetadataResponse.EXPIRE_TIME_MS = 'expireTimeMs';
shindig.container.MetadataResponse.FEATURES = 'features';
shindig.container.MetadataResponse.HEIGHT = 'height';
shindig.container.MetadataResponse.MODULE_PREFS = 'modulePrefs';
shindig.container.MetadataResponse.PREFERRED_HEIGHT = 'preferredHeight';
shindig.container.MetadataResponse.PREFERRED_WIDTH = 'preferredWidth';
shindig.container.MetadataResponse.RESPONSE_TIME_MS = 'responseTimeMs';
shindig.container.MetadataResponse.WIDTH = 'width';


/**
 * Constants to key into gadget token response JSON.
 * @enum {string}
 */
shindig.container.TokenResponse = {};
shindig.container.TokenResponse.TOKEN = 'token';


/**
 * Constants to key into timing response JSON.
 * @type {string}
 */
shindig.container.NavigateTiming = {};
// The gadget URL reporting this timing information.
shindig.container.NavigateTiming.URL = 'url';
// The gadget site ID reporting this timing information.
shindig.container.NavigateTiming.ID = 'id';
// Absolute time (ms) when gadget navigation is requested.
shindig.container.NavigateTiming.START = 'start';
// Time (ms) to receive XHR response time. In CC, for metadata and token.
shindig.container.NavigateTiming.XRT = 'xrt';
// Time (ms) to receive first byte. Typically timed at start of page.
shindig.container.NavigateTiming.SRT = 'srt';
// Time (ms) to load the DOM. Typically timed at end of page.
shindig.container.NavigateTiming.DL = 'dl';
// Time (ms) when body onload is called.
shindig.container.NavigateTiming.OL = 'ol';
// Time (ms) when page is ready for use. Typically happen after data XHR (ex:
// calendar, email) is received/presented to users. Overridable by user.
shindig.container.NavigateTiming.PRT = 'prt';


/**
 * Constants to key into request renderParam JSON.
 * @enum {string}
 */
shindig.container.RenderParam = {};
shindig.container.RenderParam.ALLOW_DEFAULT_VIEW = 'allowDefaultView';
shindig.container.RenderParam.CLASS = 'class';
shindig.container.RenderParam.DEBUG = 'debug';
shindig.container.RenderParam.HEIGHT = 'height';
shindig.container.RenderParam.NO_CACHE = 'nocache';
shindig.container.RenderParam.TEST_MODE = 'testmode';
shindig.container.RenderParam.USER_PREFS = 'userPrefs';
shindig.container.RenderParam.VIEW = 'view';
shindig.container.RenderParam.WIDTH = 'width';


/**
 * Constants to key into request viewParam JSON.
 * @enum {string}
 */
shindig.container.ViewParam = {};
shindig.container.ViewParam.VIEW = 'view';
