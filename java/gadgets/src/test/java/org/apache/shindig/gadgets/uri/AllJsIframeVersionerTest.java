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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.List;

public class AllJsIframeVersionerTest {
  // Underscores that neither of these values are even read.
  private static final Uri GADGET_URI = null;
  private static final String CONTAINER = null;

  private AllJsIframeVersioner versioner;
  private String featureChecksum;

  @Before
  public void setUp() {
    String featureContent = "THE_FEATURE_CONTENT";
    String debugContent = "FEATURE_DEBUG_CONTENT";
    String charset = Charset.defaultCharset().name();
    MessageDigest digest = HashUtil.getMessageDigest();
    try{
      digest.update(featureContent.getBytes(charset));
    } catch (UnsupportedEncodingException e) {
      digest.update(featureContent.getBytes());
    }
    try{
      digest.update(debugContent.getBytes(charset));
    } catch (UnsupportedEncodingException e) {
      digest.update(debugContent.getBytes());
    }

    featureChecksum = HashUtil.bytesToHex(digest.digest());
    FeatureRegistry registry = createMock(FeatureRegistry.class);
    FeatureResource resource = new FeatureResource.Simple(featureContent, debugContent, "js");
    List<FeatureResource> allResources = Lists.newArrayList(resource);
    final FeatureRegistry.LookupResult lr = createMock(FeatureRegistry.LookupResult.class);
    expect(lr.getResources()).andReturn(allResources);
    replay(lr);
    expect(registry.getAllFeatures()).andReturn(lr).once();
    replay(registry);
    versioner = new AllJsIframeVersioner(registry);
    verify(registry);
  }

  @Test
  public void versionIsAsExpectedAlwaysTheSame() {
    assertEquals(featureChecksum, versioner.version(GADGET_URI, CONTAINER));
    assertEquals(featureChecksum, versioner.version(Uri.parse("http://valid.com/"), "foo"));
  }

  @Test
  public void validateNull() {
    assertEquals(UriStatus.VALID_UNVERSIONED, versioner.validate(GADGET_URI, CONTAINER, null));
  }

  @Test
  public void validateEmpty() {
    assertEquals(UriStatus.VALID_UNVERSIONED, versioner.validate(GADGET_URI, CONTAINER, ""));
  }

  @Test
  public void validateMismatch() {
    assertEquals(UriStatus.INVALID_VERSION, versioner.validate(GADGET_URI, CONTAINER,
        featureChecksum + "-not"));
  }

  @Test
  public void validateMatch() {
    assertEquals(UriStatus.VALID_VERSIONED, versioner.validate(GADGET_URI, CONTAINER,
        featureChecksum));
  }
}
