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
package org.apache.shindig.gadgets.servlet;

import static junitx.framework.ComparableAssert.assertEquals;
import static junitx.framework.ComparableAssert.assertGreater;
import static junitx.framework.ComparableAssert.assertLesser;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.SigningFetcher;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.oauth.OAuthFetcher;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.NoOpContentRewriter;

import org.apache.commons.lang.StringUtils;
import org.easymock.classextension.EasyMock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Contains everything needed for making servlet requests.
 */
public class ServletTestFixture {
  private final List<Object> mocks = new ArrayList<Object>();

  public final HttpServletRequest request = mock(HttpServletRequest.class);
  public final HttpServletResponse response = mock(HttpServletResponse.class);
  public final HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(response);
  public final SecurityTokenDecoder securityTokenDecoder = mock(SecurityTokenDecoder.class);
  public final HttpFetcher httpFetcher = mock(HttpFetcher.class);
  public final SigningFetcher signingFetcher = mock(SigningFetcher.class);
  public final OAuthFetcher oauthFetcher = mock(OAuthFetcher.class);
  public final ContentFetcherFactory contentFetcherFactory = mock(ContentFetcherFactory.class);
  public final LockedDomainService lockedDomainService = mock(LockedDomainService.class);
  public final ContentRewriter rewriter = new NoOpContentRewriter();

  public ServletTestFixture() {
    try {
      // TODO: This is horrible. It needs to be fixed.
      expect(contentFetcherFactory.get()).andReturn(httpFetcher).anyTimes();
      expect(contentFetcherFactory.getSigningFetcher(isA(SecurityToken.class)))
          .andReturn(signingFetcher).anyTimes();
      expect(contentFetcherFactory.getOAuthFetcher(
          isA(SecurityToken.class), isA(OAuthArguments.class)))
          .andReturn(oauthFetcher).anyTimes();
    } catch (GadgetException e) {
      // Blah
    }
  }

  private final long testStartTime = System.currentTimeMillis();

  public void checkCacheControlHeaders(int ttl, boolean noProxy) {

    long expires = DateUtil.parseDate(recorder.getHeader("Expires")).getTime();

    // TODO: A better solution here would be an injectable clock. For now a 1 second fudge is used.
    long lowerBound = testStartTime + (1000L * (ttl - 1));
    long upperBound = lowerBound + 2000L;

    assertGreater("Expires should be at least " + ttl + " seconds more than start time.",
        lowerBound, expires);

    assertLesser("Expires should be within 2 seconds of the requested value.",
        upperBound, expires);

    if (ttl == 0) {
      assertEquals("no-cache", recorder.getHeader("Pragma"));
      assertEquals("no-cache", recorder.getHeader("Cache-Control"));
    } else {
      List<String> directives
          = Arrays.asList(StringUtils.split(recorder.getHeader("Cache-Control"), ','));

      assertTrue("Incorrect max-age set.", directives.contains("max-age=" + ttl));
      if (noProxy) {
        assertTrue("No private Cache-Control directive was set.", directives.contains("private"));
      } else {
        assertTrue("No public Cache-Control directive was set.", directives.contains("public"));
      }

      long lastModified = DateUtil.parseDate(recorder.getHeader("Last-Modified")).getTime();

      assertGreater("Invalid Last-Modified header set.", 0L, lastModified);
    }
  }

  /**
   * Creates a strict mock object for the given class, adds it to the internal
   * list of all mocks, and returns it.
   *
   * @param clazz Class to be mocked.
   * @return A mock instance of the given type.
   **/
  protected <T> T mock(Class<T> clazz) {
    return mock(clazz, false);
  }

  /**
   * Creates a strict or nice mock object for the given class, adds it to the internal
   * list of all mocks, and returns it.
   *
   * @param clazz Class to be mocked.
   * @param strict whether or not to make a strict mock
   * @return A mock instance of the given type.
   **/
  protected <T> T mock(Class<T> clazz, boolean strict) {
    T m = strict ? EasyMock.createMock(clazz) : EasyMock.createNiceMock(clazz);
    mocks.add(m);
    return m;
  }

  /**
   * Sets each mock to replay mode in the order they were created. Call this after setting
   * all of the mock expectations for a test.
   */
  protected void replay() {
    EasyMock.replay(mocks.toArray());
  }

  /**
   * Verifies each mock in the order they were created. Call this at the end of each test
   * to verify the expectations were satisfied.
   */
  protected void verify() {
    EasyMock.verify(mocks.toArray());
  }

  protected void reset() {
    EasyMock.reset(mocks.toArray());
  }
}
