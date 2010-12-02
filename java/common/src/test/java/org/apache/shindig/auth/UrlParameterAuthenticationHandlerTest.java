package org.apache.shindig.auth;

import com.google.common.collect.ImmutableMap;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class UrlParameterAuthenticationHandlerTest {
  SecurityToken expectedToken;
  UrlParameterAuthenticationHandler authHandler;
  SecurityTokenCodec codec;
  HttpServletRequest req;

  @Before
  public void setup() throws Exception {
    expectedToken = new BasicSecurityToken(
        "owner", "viewer", "app",
        "domain", "appUrl", "moduleId", "container", "activeUrl", 1000L);
    // Mock token codec
    codec = new SecurityTokenCodec() {
      public SecurityToken createToken(Map<String, String> tokenParameters) throws SecurityTokenException {
        return tokenParameters == null ? null :
               "1234".equals(tokenParameters.get(SecurityTokenCodec.SECURITY_TOKEN_NAME)) ? expectedToken : null;
      }

      public String encodeToken(SecurityToken token) throws SecurityTokenException {
        return null;
      }

      public Long getTokenExpiration(SecurityToken token) throws SecurityTokenException {
        return null;
      }
    };

    authHandler = new UrlParameterAuthenticationHandler(codec, true);
  }

  @Test
  public void testGetSecurityTokenFromRequest() throws Exception {
    Assert.assertEquals(authHandler.getName(), AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name());
  }

  @Test
  public void testInvalidRequests() throws Exception {
    // Empty request
    req = new FakeHttpServletRequest();
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));

    // Old behavior, no longer supported
    req = new FakeHttpServletRequest().setHeader("Authorization", "Token token=\"1234\"");
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));
  }

  @Test
  public void testSecurityToken() throws Exception {
    // security token in request
    req = new FakeHttpServletRequest("http://example.org/rpc?st=1234");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));
  }

  @Test
  public void testOAuth1() throws Exception {
    // An OAuth 1.0 request, we should not process this.
    req = new FakeHttpServletRequest()
        .setHeader("Authorization", "OAuth oauth_signature_method=\"RSA-SHA1\"");
    SecurityToken token = authHandler.getSecurityTokenFromRequest(req);
    Assert.assertNull(token);
  }

  @Test
  public void testOAuth2Header() throws Exception {
    req = new FakeHttpServletRequest("https://www.example.org/")
        .setHeader("Authorization", "OAuth  1234");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("https://www.example.org/")
        .setHeader("Authorization", "   OAuth    1234 ");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("https://www.example.org/")
        .setHeader("Authorization", "OAuth 1234 x=1,y=\"2 2 2\"");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("http://www.example.org/")
        .setHeader("Authorization", "OAuth 1234");
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));
  }

  @Test
  public void testOAuth2Param() throws Exception
  {
    req = new FakeHttpServletRequest("https://www.example.com?oauth_token=1234");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("https://www.example.com?oauth_token=1234&oauth_signature_method=RSA-SHA1");
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));
  }
}
