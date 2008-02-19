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

// Default syndicator configuration. Rather than replacing this
// file, you should create your own syndicator.js file and
// load it directly by modifying the value of web.xml.
// All configurations will automatically inherit values from this
// config, so you only need to provide configuration for items
// that you require explicit special casing for.

// Please namespace your attributes using the same conventions
// as you would for javascript objects, e.g. gadgets.features
// rather than "features".

// NOTE: Please leave trailing commas to reduce errors when adding new fields.
// All known common server-side JSON parsers will handle this without issue.

// Syndicator must be an array; this allows multiple syndicators
// to share configuration.
{"gadgets.syndicator" : ["default"],

// Location of opensocial-0.7 javascript (loaded server-side).
"opensocial.0.7.location" : null, // not supported by default. over ride this in
                                  // your own syndicator file.

// This config data will be passed down to javascript. Please
// configure your object using the feature name rather than
// the javascript name.

// Only configuration for required features will be used.
// See individual feature.xml files for configuration details.
"gadgets.features" : {
  "core.io" : {
    "proxyUrl" : "http://www.gmodules.com/ig/proxy?url=%url%",
    "jsonProxyUrl" : "proxy?output=js",
  },
  "views" : {
    "default" : {
      "isOnlyVisible" : false,
      "aliases": ["DASHBOARD"],
    },
    "profile" : {
      "isOnlyVisible" : false,
    },
    "canvas" : {
      "isOnlyVisible" : true,
      "aliases" : ["FULL_PAGE"]
    },
  },
  "rpc" : {
    // This should never be on the same host in a production environment!
    // Only use this for TESTING!
    "parentRelayUrl" : "files/container/rpc_relay.html",
    "useLegacyProtocol" : false,
  },
}}
