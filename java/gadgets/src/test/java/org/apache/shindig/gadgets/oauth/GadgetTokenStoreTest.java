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
package org.apache.shindig.gadgets.oauth;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;

import junit.framework.TestCase;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.easymock.IArgumentMatcher;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.net.URI;
import java.util.HashMap;

public class GadgetTokenStoreTest extends TestCase {

  public static final String GADGET_SPEC =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "  <Module>\n" +
      "    <ModulePrefs title=\"hello world example\">\n" +
      "   \n" +
      "    <Require feature=\"oauth\">\n" +
      "      <Param name=\"service_name\">\n" +
      "       testservice\n" +
      "      </Param>\n" +
      "      <Param name=\"access_url\">\n" +
      FakeOAuthServiceProvider.ACCESS_TOKEN_URL + "\n" +
      "      </Param>\n" +
      "      <Param name=\"access_method\">\n" +
      "        GET\n" +
      "      </Param>\n" +
      "      \n" +
      "      <Param name=\"request_url\">\n" +
      FakeOAuthServiceProvider.REQUEST_TOKEN_URL + "\n" +
      "      </Param>\n" +
      "      <Param name=\"request_method\">\n" +
      "        GET\n" +
      "      </Param>\n" +
      "      \n" +
      "      <Param name=\"param_location\">\n" +
      "        uri_query\n" +
      "      </Param>\n" +
      "      \n" +
      "      <Param name=\"authorize_url\">\n" +
      FakeOAuthServiceProvider.APPROVAL_URL + "\n" +
      "      </Param>\n" +
      "    </Require>\n" +
      "    \n" +
      "    </ModulePrefs>\n" +
      "    <Content type=\"html\">\n" +
      "       <![CDATA[\n" +
      "         Hello, world!\n" +
      "       ]]>\n" +
      "       \n" +
      "    </Content>\n" +
      "  </Module>";

  private GadgetOAuthTokenStore store;
  private GadgetSpec spec;
  private IMocksControl control;
  private BasicOAuthStore mockStoreImpl;
  private GadgetSpecFactory mockSpecFactory;

  private static class ProviderInfoMatcher implements IArgumentMatcher {

    private OAuthStore.ProviderInfo expectedInfo;

    public ProviderInfoMatcher(OAuthStore.ProviderInfo info) {
      expectedInfo = info;
    }

    public void appendTo(StringBuffer sb) {
      sb.append("eqProvInfo(");
      sb.append("access: ");
      sb.append(expectedInfo.getProvider().accessTokenURL);
      sb.append(", request: ");
      sb.append(expectedInfo.getProvider().requestTokenURL);
      sb.append(", authorize: ");
      sb.append(expectedInfo.getProvider().userAuthorizationURL);
      sb.append(", http_method: ");
      sb.append(expectedInfo.getHttpMethod());
      sb.append(", param_location: ");
      sb.append(expectedInfo.getParamLocation());
      sb.append(", signature_type: ");
      sb.append(expectedInfo.getSignatureType());
      sb.append(')');
    }

    public boolean matches(Object actual) {
      if (! (actual instanceof OAuthStore.ProviderInfo)) {
        return false;
      }

      OAuthStore.ProviderInfo actualInfo = (OAuthStore.ProviderInfo)actual;

      return (actualInfo.getHttpMethod() == expectedInfo.getHttpMethod())
             && (actualInfo.getParamLocation()
                 == expectedInfo.getParamLocation())
             && (actualInfo.getSignatureType()
                 == expectedInfo.getSignatureType())
             && actualInfo.getProvider().accessTokenURL.equals(
                 expectedInfo.getProvider().accessTokenURL)
             && actualInfo.getProvider().requestTokenURL.equals(
                 expectedInfo.getProvider().requestTokenURL)
             && actualInfo.getProvider().userAuthorizationURL.equals(
                 expectedInfo.getProvider().userAuthorizationURL);
    }
  }

  private static OAuthStore.ProviderInfo eqProvInfo(
      OAuthStore.ProviderInfo info) {
    EasyMock.reportMatcher(new ProviderInfoMatcher(info));
    return null;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createStrictControl();
    mockStoreImpl = control.createMock(BasicOAuthStore.class);
    mockSpecFactory = control.createMock(GadgetSpecFactory.class);

    store = new GadgetOAuthTokenStore(mockStoreImpl, mockSpecFactory);
    spec = new GadgetSpec(new URI("http://foo.bar.com/gadget.xml"), GADGET_SPEC);
  }

  public void testStoreConsumerKeyAndSecret() throws Exception {

    URI gadgetUrl = new URI("http://foo.bar.com/gadget.xml");
    String serviceName = "testservice";

    OAuthStore.ProviderKey providerKey = new OAuthStore.ProviderKey();
    providerKey.setGadgetUri(gadgetUrl.toString());
    providerKey.setServiceName(serviceName);

    OAuthStore.ConsumerKeyAndSecret kas = new OAuthStore.ConsumerKeyAndSecret(
        "key", "secret", OAuthStore.KeyType.HMAC_SYMMETRIC);

    mockStoreImpl.setOAuthConsumerKeyAndSecret(eq(providerKey), same(kas));
    expectLastCall().once();

    control.replay();

    store.storeConsumerKeyAndSecret(gadgetUrl, serviceName, kas);

    control.verify();
  }

  public void testStoreTokenKeyAndSecret() throws Exception {

    final String url = "http://foo.bar.com/gadget.xml";
    final int moduleId = 42957;
    final String userId = "testuser";
    final String serviceName = "testservice";
    final String tokenName = "testtoken";

    OAuthStore.TokenInfo tokenInfo = new OAuthStore.TokenInfo("", "");

    OAuthStore.TokenKey tokenKey = new OAuthStore.TokenKey();
    tokenKey.setGadgetUri(url);
    tokenKey.setModuleId(moduleId);
    tokenKey.setServiceName(serviceName);
    tokenKey.setTokenName(tokenName);
    tokenKey.setUserId(userId);

    mockStoreImpl.setTokenAndSecret(eq(tokenKey), same(tokenInfo));
    expectLastCall().once();

    control.replay();

    store.storeTokenKeyAndSecret(tokenKey, tokenInfo);

    control.verify();
  }

  public void testGetOAuthAccessor() throws Exception {

    final String url = "http://foo.bar.com/gadget.xml";
    final int moduleId = 42957;
    final String userId = "testuser";
    final String serviceName = "testservice";
    final String tokenName = "testtoken";
    final boolean ignoreCache = false;

    OAuthStore.TokenKey tokenKey = new OAuthStore.TokenKey();
    tokenKey.setGadgetUri(url);
    tokenKey.setModuleId(moduleId);
    tokenKey.setServiceName(serviceName);
    tokenKey.setTokenName(tokenName);
    tokenKey.setUserId(userId);

    OAuthStore.ProviderInfo info =
        GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);

    OAuthStore.AccessorInfo accessorInfo = new OAuthStore.AccessorInfo();

    expect(mockSpecFactory.getGadgetSpec(eq(new URI(url)), eq(ignoreCache)))
        .andReturn(spec);
    expect(mockStoreImpl.getOAuthAccessor(eq(tokenKey), eqProvInfo(info)))
        .andReturn(accessorInfo);

    control.replay();

    OAuthStore.AccessorInfo compare =
        store.getOAuthAccessor(tokenKey, ignoreCache);

    control.verify();

    assertSame(accessorInfo, compare);
  }

  public void testGetGadgetOAuthInfo() throws Exception {
    String serviceName = "testservice";

    OAuthStore.ProviderInfo provInfo =
        GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);

    assertEquals(FakeOAuthServiceProvider.ACCESS_TOKEN_URL,
                 provInfo.getProvider().accessTokenURL);
    assertEquals(FakeOAuthServiceProvider.REQUEST_TOKEN_URL,
                 provInfo.getProvider().requestTokenURL);
    assertEquals(FakeOAuthServiceProvider.APPROVAL_URL,
                 provInfo.getProvider().userAuthorizationURL);
    assertEquals(OAuthStore.HttpMethod.GET, provInfo.getHttpMethod());
    assertEquals(OAuthStore.SignatureType.HMAC_SHA1,
                 provInfo.getSignatureType());
    assertEquals(OAuthStore.OAuthParamLocation.URI_QUERY,
                 provInfo.getParamLocation());

    // now, let's change the spec a bit

    String newSpecStr = GADGET_SPEC.replaceAll("GET", "POST");
    spec = new GadgetSpec(new URI("http://foo.bar.com/gadget.xml"), newSpecStr);

    provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);
    assertEquals(OAuthStore.HttpMethod.POST, provInfo.getHttpMethod());

    // if we only change one of the GETs, it's an error

    newSpecStr = GADGET_SPEC.replaceFirst("GET", "POST");
    spec = new GadgetSpec(new URI("http://foo.bar.com/gadget.xml"), newSpecStr);

    try {
      provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);
      fail("expected exception, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }

    // let's test some other error conditions

    try {
      provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, "otherservice");
      fail("expected exception, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }

    newSpecStr = GADGET_SPEC.replaceAll("GET", "BLAH");
    spec = new GadgetSpec(new URI("http://foo.bar.com/gadget.xml"), newSpecStr);
    try {
      provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);
      fail("expected exception, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }

    newSpecStr = GADGET_SPEC.replaceFirst("request_url", "blah");
    spec = new GadgetSpec(new URI("http://foo.bar.com/gadget.xml"), newSpecStr);
    try {
      provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);
      fail("expected exception, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }

    newSpecStr = GADGET_SPEC.replaceFirst("access_url", "blah");
    spec = new GadgetSpec(new URI("http://foo.bar.com/gadget.xml"), newSpecStr);
    try {
      provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);
      fail("expected exception, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }

    newSpecStr = GADGET_SPEC.replaceFirst("authorize_url", "blah");
    spec = new GadgetSpec(new URI("http://foo.bar.com/gadget.xml"), newSpecStr);
    try {
      provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);
      fail("expected exception, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }

    // what if the gadget doesn't require oauth?

    newSpecStr = GADGET_SPEC.replaceFirst("feature=\"oauth\"", "feature=\"blah\"");
    spec = new GadgetSpec(
        new URI("http://foo.bar.com/gadget.xml"),
        newSpecStr);
    try {
      provInfo = GadgetOAuthTokenStore.getProviderInfo(spec, serviceName);
      fail("expected exception, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }
  }

  public void testGetOAuthParameter() throws Exception {
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("foo", "bar");

    assertEquals("bar",
                 GadgetOAuthTokenStore.getOAuthParameter(params, "foo", true));
    assertEquals("bar",
                 GadgetOAuthTokenStore.getOAuthParameter(params, "foo", false));
    assertNull(GadgetOAuthTokenStore.getOAuthParameter(params, "bla", true));

    try {
      GadgetOAuthTokenStore.getOAuthParameter(params, "bla", false);
      fail("expected GadgetException, but didn't get it");
    } catch (GadgetException e) {
      // this is expected
    }
  }
}
