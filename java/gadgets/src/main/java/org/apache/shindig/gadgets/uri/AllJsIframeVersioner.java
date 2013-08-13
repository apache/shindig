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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.IframeUriManager.Versioner;

import com.google.inject.Inject;

/**
 * Simple, but naive, implementation of an IFRAME version generator that
 * returns the same version value for all renders: the hash of all JS in the
 * feature system. This serves as an implicit version of the whole build.
 *
 * While often a reasonable heuristic, use of this versioner completely
 * ignores code changes. For instance, a rewriter may be deployed, yet
 * if no JS changed, it would never run since a generated/versioned URL
 * would cache the previously-generated render.
 *
 * More sophisticated Versioner implementations may take these sorts of
 * scenarios into consideration, and even go further, retrieving the
 * referenced gadget from the GadgetSpecFactory. Such an implementation's
 * performance is highly installation-specific, however, so is left as
 * an exercise to integrators to achieve effectively.
 */
public class AllJsIframeVersioner implements Versioner {
  private final String allJsChecksum;

  @Inject
  public AllJsIframeVersioner(FeatureRegistry registry) {
    String charset = Charset.defaultCharset().name();
    MessageDigest digest = HashUtil.getMessageDigest();
    digest.reset();
    for (FeatureResource resource : registry.getAllFeatures().getResources()) {
      // Emulate StringBuilder append of content
      update(digest, resource.getContent(), charset);
      update(digest, resource.getDebugContent(), charset);
    }
    allJsChecksum = HashUtil.bytesToHex(digest.digest());
  }

  private void update(MessageDigest digest, String content, String charset) {
    try {
      digest.update((content == null ? "null" : content).getBytes(charset));
    } catch (UnsupportedEncodingException e) {
      digest.update((content == null ? "null" : content).getBytes());
    }
  }

  public String version(Uri gadgetUri, String container) {
    return allJsChecksum;
  }

  public UriStatus validate(Uri gadgetUri, String container, String value) {
    if (value == null || value.length() == 0) {
      return UriStatus.VALID_UNVERSIONED;
    }

    if (value.equals(allJsChecksum)) {
      return UriStatus.VALID_VERSIONED;
    }

    return UriStatus.INVALID_VERSION;
  }
}
