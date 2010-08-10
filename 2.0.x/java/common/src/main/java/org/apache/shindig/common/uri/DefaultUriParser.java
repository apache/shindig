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
package org.apache.shindig.common.uri;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Uri parser using java.net.URI as its basis, enforcing RFC 2396 restrictions.
 */
public class DefaultUriParser implements UriParser {
  /**
   * Produces a new Uri from a text representation.
   *
   * @param text The text uri.
   * @return A new Uri, parsed into components.
   */
  public Uri parse(String text) {
    try {
      return Uri.fromJavaUri(new URI(text));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
