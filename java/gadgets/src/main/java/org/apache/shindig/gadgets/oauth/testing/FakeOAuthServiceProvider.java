/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.oauth.testing;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.signature.RSA_SHA1;

import org.apache.commons.codec.binary.Base64;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FakeOAuthServiceProvider implements HttpFetcher {

  public static final String BODY_ECHO_HEADER = "X-Echoed-Body";

  public static final String RAW_BODY_ECHO_HEADER = "X-Echoed-Raw-Body";

  public static final String AUTHZ_ECHO_HEADER = "X-Echoed-Authz";

  public final static String SP_HOST = "http://www.example.com";

  public final static String REQUEST_TOKEN_URL =
      SP_HOST + "/request?param=foo";
  public final static String ACCESS_TOKEN_URL =
      SP_HOST + "/access";
  public final static String APPROVAL_URL =
      SP_HOST + "/authorize";
  public final static String RESOURCE_URL =
      SP_HOST + "/data";

  public final static String CONSUMER_KEY = "consumer";
  public final static String CONSUMER_SECRET = "secret";
  
  public static final String PRIVATE_KEY_TEXT =
    "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALRiMLAh9iimur8V" +
    "A7qVvdqxevEuUkW4K+2KdMXmnQbG9Aa7k7eBjK1S+0LYmVjPKlJGNXHDGuy5Fw/d" +
    "7rjVJ0BLB+ubPK8iA/Tw3hLQgXMRRGRXXCn8ikfuQfjUS1uZSatdLB81mydBETlJ" +
    "hI6GH4twrbDJCR2Bwy/XWXgqgGRzAgMBAAECgYBYWVtleUzavkbrPjy0T5FMou8H" +
    "X9u2AC2ry8vD/l7cqedtwMPp9k7TubgNFo+NGvKsl2ynyprOZR1xjQ7WgrgVB+mm" +
    "uScOM/5HVceFuGRDhYTCObE+y1kxRloNYXnx3ei1zbeYLPCHdhxRYW7T0qcynNmw" +
    "rn05/KO2RLjgQNalsQJBANeA3Q4Nugqy4QBUCEC09SqylT2K9FrrItqL2QKc9v0Z" +
    "zO2uwllCbg0dwpVuYPYXYvikNHHg+aCWF+VXsb9rpPsCQQDWR9TT4ORdzoj+Nccn" +
    "qkMsDmzt0EfNaAOwHOmVJ2RVBspPcxt5iN4HI7HNeG6U5YsFBb+/GZbgfBT3kpNG" +
    "WPTpAkBI+gFhjfJvRw38n3g/+UeAkwMI2TJQS4n8+hid0uus3/zOjDySH3XHCUno" +
    "cn1xOJAyZODBo47E+67R4jV1/gzbAkEAklJaspRPXP877NssM5nAZMU0/O/NGCZ+" +
    "3jPgDUno6WbJn5cqm8MqWhW1xGkImgRk+fkDBquiq4gPiT898jusgQJAd5Zrr6Q8" +
    "AO/0isr/3aa6O6NLQxISLKcPDk2NOccAfS/xOtfOz4sJYM3+Bs4Io9+dZGSDCA54" +
    "Lw03eHTNQghS0A==";

  public static final String CERTIFICATE_TEXT =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIBpjCCAQ+gAwIBAgIBATANBgkqhkiG9w0BAQUFADAZMRcwFQYDVQQDDA5UZXN0\n" +
    "IFByaW5jaXBhbDAeFw03MDAxMDEwODAwMDBaFw0zODEyMzEwODAwMDBaMBkxFzAV\n" +
    "BgNVBAMMDlRlc3QgUHJpbmNpcGFsMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n" +
    "gQC0YjCwIfYoprq/FQO6lb3asXrxLlJFuCvtinTF5p0GxvQGu5O3gYytUvtC2JlY\n" +
    "zypSRjVxwxrsuRcP3e641SdASwfrmzyvIgP08N4S0IFzEURkV1wp/IpH7kH41Etb\n" +
    "mUmrXSwfNZsnQRE5SYSOhh+LcK2wyQkdgcMv11l4KoBkcwIDAQABMA0GCSqGSIb3\n" +
    "DQEBBQUAA4GBAGZLPEuJ5SiJ2ryq+CmEGOXfvlTtEL2nuGtr9PewxkgnOjZpUy+d\n" +
    "4TvuXJbNQc8f4AMWL/tO9w0Fk80rWKp9ea8/df4qMq5qlFWlx6yOLQxumNOmECKb\n" +
    "WpkUQDIDJEoFUzKMVuJf4KO/FJ345+BNLGgbJ6WujreoM1X/gYfdnJ/J\n" +
    "-----END CERTIFICATE-----";
  
  private static class TokenState {
    String tokenSecret;
    OAuthConsumer consumer;
    State state;
    String userData;

    enum State {
      PENDING,
      APPROVED,
      REVOKED,
    }

    public TokenState(String tokenSecret, OAuthConsumer consumer) {
      this.tokenSecret = tokenSecret;
      this.consumer = consumer;
      this.state = State.PENDING;
      this.userData = null;
    }

    public static TokenState makeAccessTokenState(String tokenSecret,
        OAuthConsumer consumer) {
      TokenState s = new TokenState(tokenSecret, consumer);
      s.setState(State.APPROVED);
      return s;
    }

    public void setState(State state) {
      this.state = state;
    }

    public State getState() {
      return state;
    }

    public String getSecret() {
      return tokenSecret;
    }

    public void setUserData(String userData) {
      this.userData = userData;
    }

    public String getUserData() {
      return userData;
    }
  }

  /**
   * Table of OAuth access tokens
   */
  private final HashMap<String, TokenState> tokenState;
  private final OAuthValidator validator;
  private final OAuthConsumer signedFetchConsumer;
  private final OAuthConsumer oauthConsumer;

  private boolean throttled;
  private boolean vagueErrors;

  private int requestTokenCount = 0;

  private int accessTokenCount = 0;

  private int resourceAccessCount = 0;

  private Set<OAuthParamLocation> validParamLocations;

  public FakeOAuthServiceProvider() {
    OAuthServiceProvider provider = new OAuthServiceProvider(
        REQUEST_TOKEN_URL, APPROVAL_URL, ACCESS_TOKEN_URL);
    
    signedFetchConsumer = new OAuthConsumer(null, null, null, null);
    signedFetchConsumer.setProperty(RSA_SHA1.X509_CERTIFICATE, CERTIFICATE_TEXT);

    oauthConsumer = new OAuthConsumer(null, CONSUMER_KEY, CONSUMER_SECRET, provider);
    
    tokenState = new HashMap<String, TokenState>();
    validator = new SimpleOAuthValidator();
    vagueErrors = false;
    validParamLocations = new HashSet<OAuthParamLocation>();
    validParamLocations.add(OAuthParamLocation.URI_QUERY);
  }

  public void setVagueErrors(boolean vagueErrors) {
    this.vagueErrors = vagueErrors;
  }

  public void addParamLocation(OAuthParamLocation paramLocation) {
    validParamLocations.add(paramLocation);
  }
  
  public void removeParamLocation(OAuthParamLocation paramLocation) {
    validParamLocations.remove(paramLocation);
  }
  
  public void setParamLocation(OAuthParamLocation paramLocation) {
    validParamLocations.clear();
    validParamLocations.add(paramLocation);
  }

  @SuppressWarnings("unused")
  public HttpResponse fetch(HttpRequest request)
      throws GadgetException {
    return realFetch(request);
  }

  private HttpResponse realFetch(HttpRequest request) {
    if (request.getFollowRedirects()) {
      throw new RuntimeException("Not supposed to follow OAuth redirects");
    }
    String url = request.getUri().toString();
    try {
      if (url.startsWith(REQUEST_TOKEN_URL)) {
        ++requestTokenCount;
        return handleRequestTokenUrl(request);
      } else if (url.startsWith(ACCESS_TOKEN_URL)) {
        ++accessTokenCount;
        return handleAccessTokenUrl(request);
      } else if (url.startsWith(RESOURCE_URL)){
        ++resourceAccessCount;
        return handleResourceUrl(request);
      }
    } catch (Exception e) {
      throw new RuntimeException("Problem with request for URL " + url, e);
    }
    throw new RuntimeException("Unexpected request for " + url);
  }

  private HttpResponse handleRequestTokenUrl(HttpRequest request)
      throws Exception {
    OAuthMessage message = parseMessage(request).message;
    String requestConsumer = message.getParameter(OAuth.OAUTH_CONSUMER_KEY);
    OAuthConsumer consumer;
    if (CONSUMER_KEY.equals(requestConsumer)) {
      consumer = oauthConsumer;
    } else {
      return makeOAuthProblemReport(
          "consumer_key_unknown", "invalid consumer: " + requestConsumer);
    }
    if (throttled) {
      return makeOAuthProblemReport(
          "consumer_key_refused", "exceeded quota exhausted");
    }
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    message.validateMessage(accessor, validator);
    String requestToken = Crypto.getRandomString(16);
    String requestTokenSecret = Crypto.getRandomString(16);
    tokenState.put(
        requestToken, new TokenState(requestTokenSecret, accessor.consumer));
    String resp = OAuth.formEncode(OAuth.newList(
        "oauth_token", requestToken,
        "oauth_token_secret", requestTokenSecret));
    return new HttpResponse(resp);
  }

  private HttpResponse makeOAuthProblemReport(String code, String text) throws IOException {
    if (vagueErrors) {
      int rc = HttpResponse.SC_UNAUTHORIZED;
      if ("consumer_key_unknown".equals(code)) {
        rc = HttpResponse.SC_FORBIDDEN;
      }
      return new HttpResponseBuilder().setHttpStatusCode(rc).create();
    }
    OAuthMessage msg = new OAuthMessage(null, null, null);
    msg.addParameter("oauth_problem", code);
    msg.addParameter("oauth_problem_advice", text);    
    return new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_FORBIDDEN)
        .addHeader("WWW-Authenticate", msg.getAuthorizationHeader("realm"))
        .create();
  }

  // Loosely based off net.oauth.OAuthServlet, and even more loosely related
  // to the OAuth specification
  private MessageInfo parseMessage(HttpRequest request) {
    MessageInfo info = new MessageInfo();
    String method = request.getMethod();
    ParsedUrl parsed = new ParsedUrl(request.getUri().toString());
    
    List<OAuth.Parameter> params = new ArrayList<OAuth.Parameter>();
    params.addAll(parsed.getParsedQuery());
    
    if (!validParamLocations.contains(OAuthParamLocation.URI_QUERY)) {
      // Make sure nothing OAuth related ended up in the query string
      for (OAuth.Parameter p : params) {
        if (p.getKey().contains("oauth_")) {
          throw new RuntimeException("Found unexpected query param " + p.getKey());
        }
      }
    }
        
    // Parse authorization header
    if (validParamLocations.contains(OAuthParamLocation.AUTH_HEADER)) {
      String aznHeader = request.getHeader("Authorization");
      if (aznHeader != null) {
        info.aznHeader = aznHeader;
        for (OAuth.Parameter p : OAuthMessage.decodeAuthorization(aznHeader)) {
          if (!p.getKey().equalsIgnoreCase("realm")) {
            params.add(p);
          }
        }
      }
    }
    
    // Parse body
    if (request.getMethod().equals("POST")) {
      String type = request.getHeader("Content-Type");
      if ("application/x-www-form-urlencoded".equals(type)) {
        String body = request.getPostBodyAsString();
        info.body = body;
        params.addAll(OAuth.decodeForm(request.getPostBodyAsString()));
        // If we're not configured to pass oauth parameters in the post body, double check
        // that they didn't end up there.
        if (!validParamLocations.contains(OAuthParamLocation.POST_BODY)) {
          if (body.contains("oauth_")) {
            throw new RuntimeException("Found unexpected post body data" + body);
          }
        }
      } else {
        try {
          InputStream is = request.getPostBody();
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          byte[] buf = new byte[1024];
          int read;
          while ((read = is.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, read);
          }
          info.rawBody = baos.toByteArray();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    
    // Return the lot
    info.message = new OAuthMessage(method, parsed.getLocation(), params);
    return info;
  }
  
  /**
   * Bundles information about a received OAuthMessage.
   */
  private static class MessageInfo {
    public OAuthMessage message;
    public String aznHeader;
    public String body;
    public byte[] rawBody;
  }

  /**
   * Utility class for parsing OAuth URLs.
   */
  private static class ParsedUrl {
    String location = null;
    String query = null;
    List<OAuth.Parameter> decodedQuery = null;

    public ParsedUrl(String url) {
      int queryIndex = url.indexOf('?');
      if (queryIndex != -1) {
        query = url.substring(queryIndex+1, url.length());
        location = url.substring(0, queryIndex);
      } else {
        location = url;
      }
    }

    public String getLocation() {
      return location;
    }

    public String getRawQuery() {
      return query;
    }

    public List<OAuth.Parameter> getParsedQuery() {
      if (decodedQuery == null) {
        if (query != null) {
          decodedQuery = OAuth.decodeForm(query);
        } else {
          decodedQuery = new ArrayList<OAuth.Parameter>();
        }
      }
      return decodedQuery;
    }

    public String getQueryParam(String name) {
      for (OAuth.Parameter p : getParsedQuery()) {
        if (p.getKey().equals(name)) {
          return p.getValue();
        }
      }
      return null;
    }
  }

  /**
   * Used to fake a browser visit to approve a token.
   * @param url
   * @throws Exception
   */
  public void browserVisit(String url) throws Exception {
    ParsedUrl parsed = new ParsedUrl(url);
    String requestToken = parsed.getQueryParam("oauth_token");
    TokenState state = tokenState.get(requestToken);
    state.setState(TokenState.State.APPROVED);
    // Not part of the OAuth spec, just a handy thing for testing.
    state.setUserData(parsed.getQueryParam("user_data"));
  }

  public static class TokenPair {
    public final String token;
    public final String secret;

    public TokenPair(String token, String secret) {
      this.token = token;
      this.secret = secret;
    }
  }

  /**
   * Generate a preapproved request token for the specified user data.
   *
   * @param userData
   * @return the request token and secret
   */
  public TokenPair getPreapprovedToken(String userData) {
    String requestToken = Crypto.getRandomString(16);
    String requestTokenSecret = Crypto.getRandomString(16);
    TokenState state = new TokenState(requestTokenSecret, oauthConsumer);
    state.setState(TokenState.State.APPROVED);
    state.setUserData(userData);
    tokenState.put(requestToken, state);
    return new TokenPair(requestToken, requestTokenSecret);
  }

  /**
   * Used to revoke all access tokens issued by this service provider.
   *
   * @throws Exception
   */
  public void revokeAllAccessTokens() throws Exception {
    for (TokenState state : tokenState.values()) {
      state.setState(TokenState.State.REVOKED);
    }
  }

  private HttpResponse handleAccessTokenUrl(HttpRequest request)
      throws Exception {
    OAuthMessage message = parseMessage(request).message;
    String requestToken = message.getParameter("oauth_token");
    TokenState state = tokenState.get(requestToken);
    if (throttled) {
      return makeOAuthProblemReport(
          "consumer_key_refused", "exceeded quota");
    } else if (state == null) {
      return makeOAuthProblemReport("token_rejected", "Unknown request token");
    }
    if (state.getState() != TokenState.State.APPROVED) {
      throw new Exception("Token not approved");
    }
    OAuthAccessor accessor = new OAuthAccessor(oauthConsumer);
    accessor.requestToken = requestToken;
    accessor.tokenSecret = state.tokenSecret;
    message.validateMessage(accessor, validator);
    String accessToken = Crypto.getRandomString(16);
    String accessTokenSecret = Crypto.getRandomString(16);
    state.tokenSecret = accessTokenSecret;
    tokenState.put(accessToken, state);
    tokenState.remove(requestToken);
    String resp = OAuth.formEncode(OAuth.newList(
        "oauth_token", accessToken,
        "oauth_token_secret", accessTokenSecret));
    return new HttpResponse(resp);
  }

  private HttpResponse handleResourceUrl(HttpRequest request)
      throws Exception {
    MessageInfo info = parseMessage(request);
    String consumerId = info.message.getParameter("oauth_consumer_key");
    OAuthConsumer consumer;
    if (CONSUMER_KEY.equals(consumerId)) {
      consumer = oauthConsumer;
    } else if ("signedfetch".equals(consumerId)) {
      consumer = signedFetchConsumer;
    } else if ("container.com".equals(consumerId)) {
      consumer = signedFetchConsumer;
    } else {
      return makeOAuthProblemReport("parameter_missing", "oauth_consumer_key not found");
    }
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    String responseBody = null;
    if (throttled) {
      return makeOAuthProblemReport(
          "consumer_key_refused", "exceeded quota");
    }
    if (consumer == oauthConsumer) {
      // for OAuth, check the access token.  We skip this for signed fetch
      String accessToken = info.message.getParameter("oauth_token");
      TokenState state = tokenState.get(accessToken);
      if (state == null) {
        return makeOAuthProblemReport(
            "token_rejected", "Access token unknown");
      }
      if (state.getState() != TokenState.State.APPROVED) {
        return makeOAuthProblemReport(
            "token_revoked", "User revoked permissions");
      }
      accessor.accessToken = accessToken;
      accessor.tokenSecret = state.getSecret();
      responseBody = "User data is " + state.getUserData();
    } else {
      // For signed fetch, just echo back the query parameters in the body
      responseBody = request.getUri().getQuery();
    }
    
    // Check the signature
    info.message.validateMessage(accessor, validator);
    
    // Send back a response
    HttpResponseBuilder resp = new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponseString(responseBody);
    if (info.aznHeader != null) {
      resp.setHeader(AUTHZ_ECHO_HEADER, info.aznHeader);
    }
    if (info.body != null) {
      resp.setHeader(BODY_ECHO_HEADER, info.body);
    }
    if (info.rawBody != null) {
      resp.setHeader(RAW_BODY_ECHO_HEADER, new String(Base64.encodeBase64(info.rawBody)));
    }
    return resp.create();
  }

  public void setConsumersThrottled(boolean throttled) {
    this.throttled = throttled;
  }

  /**
   * @return number of hits to request token URL.
   */
  public int getRequestTokenCount() {
    return requestTokenCount;
  }

  /**
   * @return number of hits to access token URL.
   */
  public int getAccessTokenCount() {
    return accessTokenCount;
  }

  /**
   * @return number of hits to resource access URL.
   */
  public int getResourceAccessCount() {
    return resourceAccessCount;
  }
}
