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

/**
 * @fileoverview Augments shindig.uri class with various useful helper methods.
 */

shindig._uri = shindig.uri;
shindig.uri = (function() {
  var oldCtor = shindig._uri;
  shindig._uri = null;

  /**
   * Checks that a Uri has the same origin as this Uri.
   *
   * Two Uris have the same origin if they point to the same schema, server
   * and port.
   *
   * @param {Url} other The Uri to compare to this Uri.
   * @return {boolean} Whether the Uris have the same origin.
   */
  function hasSameOrigin(self, other) {
    return self.getOrigin() == other.getOrigin();
  }

  /**
   * Fully qualifies this Uri if it is relative, using a given base Uri.
   *
   * @param {Uri} self The base Uri.
   * @param {Uri} base The Uri to resolve.
   */
  function resolve(self, base) {
    if (self.getSchema() == '') {
      self.setSchema(base.getSchema());
    }
    if (self.getAuthority() == '') {
      self.setAuthority(base.getAuthority());
    }
    var selfPath = self.getPath();
    if (selfPath == '' || selfPath.charAt(0) != '/') {
      var basePath = base.getPath();
      var lastSlash = basePath.lastIndexOf('/');
      if (lastSlash != -1) {
        basePath = basePath.substring(0, lastSlash + 1);
      }
      self.setPath(base.getPath() + selfPath);
    }
  }

  return function(opt_in) {
    var self = oldCtor(opt_in);
    self.hasSameOrigin = function(other) {
      return hasSameOrigin(self, other);
    };
    self.resolve = function(other) {
      return resolve(self, other);
    };
    return self;
  };
})();
