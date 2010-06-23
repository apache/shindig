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
 * @fileoverview Initial configuration/boot-strapping work for common container
 * to operate. This includes setting up gadgets config and global environment
 * variables.
 */
(function() {

  function initializeConfig() {
    gadgets.config.init({
      'rpc': {
        parentRelayUrl: ''
      },
      'core.io': {
        jsonProxyUrl: 'http://%host%/gadgets/makeRequest',
        proxyUrl: 'http://%host%/gadgets/proxy' +
            '?refresh=%refresh%' +
            '&container=%container%%rewriteMime%' +
            '&gadget=%gadget%/%rawurl%'
      }
    });
  }

  function initializeGlobalVars() {
    var scriptSrc = getLastScriptSrc();
    if (scriptSrc) {
      window.__API_HOST = shindig.container.util.parseOrigin(scriptSrc);
      window.__API_PREFIX_PATH = shindig.container.util.parsePrefixPath(
          scriptSrc, '/gadgets/js/container.js');
      window.__CONTAINER = shindig.container.util.getParamValue(
          scriptSrc, 'container');
      window.__CONTAINER_HOST = shindig.container.util.parseOrigin(
          document.location.href);
    }
  }

  function getLastScriptSrc() {
    var scriptEls = document.getElementsByTagName('script');
    return (scriptEls.length > 0)
        ? scriptEls[scriptEls.length - 1].src
        : null;
  }

  initializeConfig();
  initializeGlobalVars();
})();
