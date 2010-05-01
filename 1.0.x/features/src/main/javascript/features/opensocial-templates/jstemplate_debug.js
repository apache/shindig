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
 * @fileoverview Debug functions to be linked against the JSTemplate library
 * to provide debugging output.
 * 
 * TODO(levik): 
 *  - Support more output types (currently Firebug only)
 *  - Filter "mundane" errors (such as non-existent variable lookups) 
 */


function jsToSource(js) {} 

function check(x) {}

function logUrl(url) {}
function logHtml(url) {}
function logError(msg, file, line) {}
function dump(exception) {}
function rethrow(e) {}
function getSourceLine(level) { return ""; }

function Profiler() {}
Profiler.monitor = function(obj, method, trace, opt_l, opt_fmt) {};
Profiler.monitorAll = function(obj, trace, opt_l) {};
Profiler.dump = function() {};
