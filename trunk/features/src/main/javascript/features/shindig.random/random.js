/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/*
 * @fileoverview
 * This code implements a safer random() method that is seeded from
 * screen width/height and (presumably random/unguessable) mouse
 * movement, in an effort to create a better seed for random().
 *
 * Its aim is to solve the problem of gadgets that are relying on
 * secret RPC tokens to validate identity.
 *
 * Another possible solution is to use XHR to get a real random number
 * from the server, though this is not feasible or may be too slow in
 * some circumstances.
 */
shindig.random = (function() {
  var oth = Math.random();
  var hex = '0123456789ABCDEF';
  var start = 1;
  var m = ((screen.width * screen.width) + screen.height) * 1e6;
  var sliceFn = [].slice;

  // TODO: consider using addEventListener
  var orig_onmousemove = window.onmousemove || function() { };

  window.onmousemove = function(e) {
    if (window.event) {
      e = window.event;
    }

    var ac = (e.screenX + e.clientX) << 16;
    ac += (e.screenY + e.clientY);
    ac *= new Date().getTime() % 1e6;
    start = (start * ac) % m;
    return orig_onmousemove.apply(window, sliceFn.call(arguments, 0));
  };

  function sha1(str) {
    var sha1 = shindig.sha1();
    sha1.update(str);
    return sha1.digestString();
  }

  var seed = sha1(
      document.cookie + '|' + document.location + '|' + (new Date()).getTime() + '|' + oth);

  return function() {
    var rnd = start;
    rnd += parseInt(seed.substr(0, 20), 16);
    seed = sha1(seed);
    return rnd / (m + Math.pow(16, 20));
  }
})();
