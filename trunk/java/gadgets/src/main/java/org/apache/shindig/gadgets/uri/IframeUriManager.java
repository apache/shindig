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
package org.apache.shindig.gadgets.uri;

import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;

/**
 * Interface defining methods needed to generate iframe URL for the /ifr servlet.
 */
public interface IframeUriManager {
  /**
   * Generates iframe urls for meta data service.
   * Use this rather than generating your own urls by hand.
   *
   * @return The generated iframe url.
   */
  Uri makeRenderingUri(Gadget gadget);

  /**
   * Generates iframe uris for all views in the gadget.
   * @param gadget The gadget to generate the URI for.
   *
   * @return A map of views to iframe uris.
   */
  Map<String, Uri> makeAllRenderingUris (Gadget gadget);

  /**
   * Validates the provided rendering Uri. May include
   * locked-domain, version param, and/or other checks.
   *
   * @Return Validation status of the Uri.
   */
  UriStatus validateRenderingUri(Uri uri);

  public interface Versioner {
    /**
     * @param gadgetUri Gadget whose content to version.
     * @param container Container in which gadget is being rendered.
     * @return Version string for the pair.
     */
    String version(Uri gadgetUri, String container);

    /**
     * @param gadgetUri Gadget whose version to validate.
     * @param container Container in which gadget is being rendered.
     * @param value Previously returned version string for the pair.
     * @return UriStatus indicating version (mis)match.
     */
    UriStatus validate(Uri gadgetUri, String container, String value);
  }
}
