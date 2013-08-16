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
package org.apache.shindig.gadgets.oauth.testing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.oauth.OAuth;
import net.oauth.OAuth.Parameter;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.signature.RSA_SHA1;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.shindig.auth.OAuthConstants;
import org.apache.shindig.auth.OAuthUtil;
import org.apache.shindig.auth.OAuthUtil.SignatureType;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.LruCache;
import org.apache.shindig.common.cache.SoftExpiringCache;
import org.apache.shindig.common.cache.SoftExpiringCache.CachedObject;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.GenericDigestUtils;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class FakeOAuthServiceProvider implements HttpFetcher {

  public static final String BODY_ECHO_HEADER = "X-Echoed-Body";

  public static final String RAW_BODY_ECHO_HEADER = "X-Echoed-Raw-Body";

  public static final String AUTHZ_ECHO_HEADER = "X-Echoed-Authz";

  public final static String SP_HOST = "http://www.example.com";

  public final static String REQUEST_TOKEN_URL = SP_HOST + "/request?param=foo";
  public final static String ACCESS_TOKEN_URL = SP_HOST + "/access";
  public final static String APPROVAL_URL = SP_HOST + "/authorize";
  public final static String RESOURCE_URL = SP_HOST + "/data";
  public final static String NOT_FOUND_URL = SP_HOST + "/404";
  public final static String ERROR_400 = SP_HOST + "/400";
  public final static String ECHO_URL = SP_HOST + "/echo";

  public final static String CONSUMER_KEY = "consumer";
  public final static String CONSUMER_SECRET = "secret";

  public final static int TOKEN_EXPIRATION_SECONDS = 60;

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

  enum State {
    PENDING,
    APPROVED_UNCLAIMED,
    APPROVED,
    REVOKED,
  }

  private class TokenState {
    String tokenSecret;
    State state;
    String userData;
    String sessionHandle;
    long issued;
    String callbackUrl;
    String verifier;

    public TokenState(String tokenSecret, String callbackUrl) {
      this.tokenSecret = tokenSecret;
      this.state = State.PENDING;
      this.userData = null;
      this.callbackUrl = callbackUrl;
    }

    public void approveToken() {
      // Waiting for the consumer to claim the token
      state = State.APPROVED_UNCLAIMED;
      issued = clock.currentTimeMillis();
      if (callbackUrl != null) {
        verifier = Crypto.getRandomString(8);
      }
    }

    public void claimToken() {
      // consumer taking the token
      state = State.APPROVED;
      sessionHandle = Crypto.getRandomString(8);
    }

    public void renewToken() {
      issued = clock.currentTimeMillis();
    }

    public void revokeToken() {
      state = State.REVOKED;
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
  private final OAuthConsumer signedFetchConsumer;
  private final OAuthConsumer oauthConsumer;
  private final TimeSource clock;
  private final SoftExpiringCache<String, OAuthMessage> nonceCache;

  private boolean unauthorized = false;
  private boolean throttled = false;
  private boolean vagueErrors = false;
  private boolean reportExpirationTimes = true;
  private boolean sessionExtension = false;
  private boolean rejectExtraParams = false;
  private boolean returnAccessTokenData = false;

  private int requestTokenCount = 0;

  private int accessTokenCount = 0;

  private int resourceAccessCount = 0;

  private Set<OAuthParamLocation> validParamLocations;

  private boolean returnNull;

  private GadgetException gadgetException;

  private RuntimeException runtimeException;

  private boolean checkTrustedParams;

  private int trustedParamCount;

  private SecurityToken expectedRequestSecurityToken;

  public FakeOAuthServiceProvider(TimeSource clock) {
    this.clock = clock;
    OAuthServiceProvider provider = new OAuthServiceProvider(
        REQUEST_TOKEN_URL, APPROVAL_URL, ACCESS_TOKEN_URL);

    signedFetchConsumer = new OAuthConsumer(null, null, null, null);
    signedFetchConsumer.setProperty(RSA_SHA1.X509_CERTIFICATE, CERTIFICATE_TEXT);

    oauthConsumer = new OAuthConsumer(null, CONSUMER_KEY, CONSUMER_SECRET, provider);

    tokenState = Maps.newHashMap();
    validParamLocations = Sets.newHashSet();
    validParamLocations.add(OAuthParamLocation.URI_QUERY);
    nonceCache =
        new SoftExpiringCache<String, OAuthMessage>(new LruCache<String, OAuthMessage>(10000));
    nonceCache.setTimeSource(clock);
  }

  public void setVagueErrors(boolean vagueErrors) {
    this.vagueErrors = vagueErrors;
  }

  public void setSessionExtension(boolean sessionExtension) {
    this.sessionExtension = sessionExtension;
  }

  public void setReportExpirationTimes(boolean reportExpirationTimes) {
    this.reportExpirationTimes = reportExpirationTimes;
  }

  public void setRejectExtraParams(boolean rejectExtraParams) {
    this.rejectExtraParams = rejectExtraParams;
  }

  public void setReturnAccessTokenData(boolean returnAccessTokenData) {
    this.returnAccessTokenData = returnAccessTokenData;
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

  public HttpResponse fetch(HttpRequest request) throws GadgetException {
    if (returnNull) {
      return null;
    }
    if (gadgetException != null) {
      throw gadgetException;
    }
    if (runtimeException != null) {
      throw runtimeException;
    }
    if (request.getFollowRedirects()) {
      throw new RuntimeException("Not supposed to follow OAuth redirects");
    }
    String url = request.getUri().toString();
    try {
      if (url.startsWith(REQUEST_TOKEN_URL)) {
        checkSecurityToken(request);
        ++requestTokenCount;
        return handleRequestTokenUrl(request);
      } else if (url.startsWith(ACCESS_TOKEN_URL)) {
        checkSecurityToken(request);
        ++accessTokenCount;
        return handleAccessTokenUrl(request);
      } else if (url.startsWith(RESOURCE_URL)){
        ++resourceAccessCount;
        return handleResourceUrl(request);
      } else if (url.startsWith(NOT_FOUND_URL)) {
        return handleNotFoundUrl(request);
      } else if (url.startsWith(ERROR_400)) {
        return handleError400Url(request);
      } else if (url.startsWith(ECHO_URL)) {
        return handleEchoUrl(request);
      }
    } catch (Exception e) {
      throw new RuntimeException("Problem with request for URL " + url, e);
    }
    throw new RuntimeException("Unexpected request for " + url);
  }

  private void checkSecurityToken(HttpRequest request) {
    if (request.getSecurityToken() == null) {
      throw new RuntimeException("Security token should not be null" );
    }
    if (!request.getSecurityToken().isAnonymous()) {
      throw new RuntimeException("Expected an anonymous security token" );
    }
    if (expectedRequestSecurityToken != null) {
      if (!expectedRequestSecurityToken.getAppUrl().equals( request.getSecurityToken().getAppUrl() )) {
        throw new RuntimeException("Security token AppUrl mismatch" );
      }
    }
  }

  private HttpResponse handleRequestTokenUrl(HttpRequest request)
      throws Exception {
    MessageInfo info = parseMessage(request);
    String requestConsumer = info.message.getParameter(OAuth.OAUTH_CONSUMER_KEY);
    OAuthConsumer consumer;
    if (CONSUMER_KEY.equals(requestConsumer)) {
      consumer = oauthConsumer;
    } else {
      return makeOAuthProblemReport(
          OAuth.Problems.CONSUMER_KEY_UNKNOWN, "invalid consumer: " + requestConsumer,
          HttpResponse.SC_FORBIDDEN);
    }
    if (throttled) {
      return makeOAuthProblemReport(
          OAuth.Problems.CONSUMER_KEY_REFUSED, "exceeded quota exhausted",
          HttpResponse.SC_FORBIDDEN);
    }
    if (unauthorized) {
      return makeOAuthProblemReport(
          OAuth.Problems.PERMISSION_DENIED, "user refused access",
          HttpResponse.SC_BAD_REQUEST);
    }
    if (rejectExtraParams) {
      String extra = hasExtraParams(info.message);
      if (extra != null) {
        return makeOAuthProblemReport(OAuth.Problems.PARAMETER_REJECTED, extra,
            HttpResponse.SC_BAD_REQUEST);
      }
    }
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    validateMessage(accessor, info, true);
    String requestToken = Crypto.getRandomString(16);
    String requestTokenSecret = Crypto.getRandomString(16);
    String callbackUrl = info.message.getParameter(OAuth.OAUTH_CALLBACK);
    tokenState.put(
        requestToken, new TokenState(requestTokenSecret, callbackUrl));
    List<Parameter> responseParams = OAuth.newList(
        "oauth_token", requestToken,
        "oauth_token_secret", requestTokenSecret);
    if (callbackUrl != null) {
      responseParams.add(new Parameter(OAuth.OAUTH_CALLBACK_CONFIRMED, "true"));
    }
    return new HttpResponse(OAuth.formEncode(responseParams));
  }

  private String hasExtraParams(OAuthMessage message) {
    for (Entry<String, String> param : OAuthUtil.getParameters(message)) {
      // Our request token URL allows "param" as a query param, and also oauth params of course.
      if (!param.getKey().startsWith("oauth") && !param.getKey().equals("param")) {
        return param.getKey();
      }
    }
    return null;
  }

  private HttpResponse makeOAuthProblemReport(String code, String text, int rc) throws IOException {
    if (vagueErrors) {
      return new HttpResponseBuilder()
          .setHttpStatusCode(rc)
          .setResponseString("some vague error")
          .create();
    }
    OAuthMessage msg = new OAuthMessage(null, null, null);
    msg.addParameter("oauth_problem", code);
    msg.addParameter("oauth_problem_advice", text);
    return new HttpResponseBuilder()
        .setHttpStatusCode(rc)
        .addHeader("WWW-Authenticate", msg.getAuthorizationHeader("realm"))
        .create();
  }

  // Loosely based off net.oauth.OAuthServlet, and even more loosely related
  // to the OAuth specification
  private MessageInfo parseMessage(HttpRequest request) {
    MessageInfo info = new MessageInfo();
    info.request = request;
    String method = request.getMethod();
    ParsedUrl parsed = new ParsedUrl(request.getUri().toString());

    List<OAuth.Parameter> params = Lists.newArrayList();
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
    info.body = request.getPostBodyAsString();
    try {
      info.rawBody = IOUtils.toByteArray(request.getPostBody());
    } catch (IOException e) {
      throw new RuntimeException("Can't read post body bytes", e);
    }
    if (OAuth.isFormEncoded(request.getHeader("Content-Type"))) {
      params.addAll(OAuth.decodeForm(request.getPostBodyAsString()));
      // If we're not configured to pass oauth parameters in the post body, double check
      // that they didn't end up there.
      if (!validParamLocations.contains(OAuthParamLocation.POST_BODY)) {
        if (info.body.contains("oauth_")) {
          throw new RuntimeException("Found unexpected post body data" + info.body);
        }
      }
    }

    // Return the lot
    info.message = new OAuthMessage(method, parsed.getLocation(), params);

    // Check for trusted parameters
    if (checkTrustedParams) {
      if (!"foo".equals(OAuthUtil.getParameter(info.message, "oauth_magic"))) {
        throw new RuntimeException("no oauth_trusted=foo parameter");
      }
      if (!"bar".equals(OAuthUtil.getParameter(info.message, "opensocial_magic"))) {
        throw new RuntimeException("no opensocial_trusted=foo parameter");
      }
      if (!"quux".equals(OAuthUtil.getParameter(info.message, "xoauth_magic"))) {
        throw new RuntimeException("no xoauth_magic=quux parameter");
      }
      if (!"overridden_opensocial_owner_id".equals(
          OAuthUtil.getParameter(info.message, "opensocial_owner_id"))) {
        throw new RuntimeException("opensocial_owner_id should be overridden");
      }
      trustedParamCount += 4;
    }

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
    public HttpRequest request;
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
          decodedQuery = Lists.newArrayList();
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
   *
   * @return a redirect URL, which may or may not include an oauth verifier
   */
  public String browserVisit(String url) throws Exception {
    ParsedUrl parsed = new ParsedUrl(url);
    String requestToken = parsed.getQueryParam("oauth_token");
    TokenState state = tokenState.get(requestToken);
    state.approveToken();
    // Not part of the OAuth spec, just a handy thing for testing.
    state.setUserData(parsed.getQueryParam("user_data"));
    if (state.callbackUrl != null) {
      UriBuilder callback = UriBuilder.parse(state.callbackUrl);
      callback.addQueryParameter(OAuth.OAUTH_VERIFIER, state.verifier);
      return callback.toString();
    }
    return null;
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
    TokenState state = new TokenState(requestTokenSecret, null);
    state.approveToken();
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
      state.revokeToken();
    }
  }

  /**
   * Changes session handles to prevent renewal from working.
   */
  public void changeAllSessionHandles() throws Exception {
    for (TokenState state : tokenState.values()) {
      state.sessionHandle = null;
    }
  }

  private HttpResponse handleAccessTokenUrl(HttpRequest request)
      throws Exception {
    MessageInfo info = parseMessage(request);
    String requestToken = info.message.getParameter("oauth_token");
    TokenState state = tokenState.get(requestToken);
    if (throttled) {
      return makeOAuthProblemReport(OAuth.Problems.CONSUMER_KEY_REFUSED,
          "exceeded quota", HttpResponse.SC_FORBIDDEN);
    } else if (unauthorized) {
      return makeOAuthProblemReport(OAuth.Problems.PERMISSION_DENIED,
          "user refused access", HttpResponse.SC_UNAUTHORIZED);
    } else if (state == null) {
      return makeOAuthProblemReport(OAuth.Problems.TOKEN_REJECTED,
          "Unknown request token", HttpResponse.SC_UNAUTHORIZED);
    }
    if (rejectExtraParams) {
      String extra = hasExtraParams(info.message);
      if (extra != null) {
        return makeOAuthProblemReport(OAuth.Problems.PARAMETER_REJECTED,
            extra, HttpResponse.SC_BAD_REQUEST);
      }
    }

    OAuthAccessor accessor = new OAuthAccessor(oauthConsumer);
    accessor.requestToken = requestToken;
    accessor.tokenSecret = state.tokenSecret;
    validateMessage(accessor, info, true);

    if (state.getState() == State.APPROVED_UNCLAIMED) {
      String sentVerifier = info.message.getParameter("oauth_verifier");
      if (state.verifier != null && !state.verifier.equals(sentVerifier)) {
        return makeOAuthProblemReport(OAuthConstants.PROBLEM_BAD_VERIFIER, "wrong oauth verifier",
            HttpResponse.SC_UNAUTHORIZED);
      }
      state.claimToken();
    } else if (state.getState() == State.APPROVED) {
      // Verify can refresh
      String sentHandle = info.message.getParameter("oauth_session_handle");
      if (sentHandle == null) {
        return makeOAuthProblemReport(OAuth.Problems.PARAMETER_ABSENT,
            "no oauth_session_handle", HttpResponse.SC_BAD_REQUEST);
      }
      if (!sentHandle.equals(state.sessionHandle)) {
        return makeOAuthProblemReport(OAuthConstants.PROBLEM_TOKEN_INVALID, "token not valid",
            HttpResponse.SC_UNAUTHORIZED);
      }
      state.renewToken();
    } else if (state.getState() == State.REVOKED){
      return makeOAuthProblemReport(OAuth.Problems.TOKEN_REVOKED,
          "Revoked access token can't be renewed", HttpResponse.SC_UNAUTHORIZED);
    } else {
      throw new Exception("Token in weird state " + state.getState());
    }

    String accessToken = Crypto.getRandomString(16);
    String accessTokenSecret = Crypto.getRandomString(16);
    state.tokenSecret = accessTokenSecret;
    tokenState.put(accessToken, state);
    tokenState.remove(requestToken);
    List<OAuth.Parameter> params = OAuth.newList(
        "oauth_token", accessToken,
        "oauth_token_secret", accessTokenSecret);
    if (sessionExtension) {
      params.add(new OAuth.Parameter("oauth_session_handle", state.sessionHandle));
      if (reportExpirationTimes) {
        params.add(new OAuth.Parameter("oauth_expires_in", "" + TOKEN_EXPIRATION_SECONDS));
      }
    }
    if (returnAccessTokenData) {
      params.add(new OAuth.Parameter("userid", "userid value"));
      params.add(new OAuth.Parameter("xoauth_stuff", "xoauth_stuff value"));
      params.add(new OAuth.Parameter("oauth_stuff", "oauth_stuff value"));
    }
    return new HttpResponse(OAuth.formEncode(params));
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
      return makeOAuthProblemReport(OAuth.Problems.PARAMETER_ABSENT,
          "oauth_consumer_key not found", HttpResponse.SC_BAD_REQUEST);
    }
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    String responseBody = null;
    if (throttled) {
      return makeOAuthProblemReport(
          OAuth.Problems.CONSUMER_KEY_REFUSED, "exceeded quota", HttpResponse.SC_FORBIDDEN);
    }
    if (unauthorized) {
      return makeOAuthProblemReport(
          OAuth.Problems.PERMISSION_DENIED, "user refused access",
          HttpResponse.SC_UNAUTHORIZED);
    }
    if (consumer == oauthConsumer) {
      // for OAuth, check the access token.  We skip this for signed fetch
      String accessToken = info.message.getParameter("oauth_token");
      TokenState state = tokenState.get(accessToken);
      if (state == null) {
        return makeOAuthProblemReport(
            OAuth.Problems.TOKEN_REJECTED, "Access token unknown",
            HttpResponse.SC_UNAUTHORIZED);
      }
      // Check the signature
      accessor.accessToken = accessToken;
      accessor.tokenSecret = state.getSecret();
      validateMessage(accessor, info, false);

      if (state.getState() != State.APPROVED) {
        return makeOAuthProblemReport(
            OAuth.Problems.TOKEN_REVOKED, "User revoked permissions",
            HttpResponse.SC_UNAUTHORIZED);
      }
      if (sessionExtension) {
        long expiration = state.issued + TOKEN_EXPIRATION_SECONDS * 1000;
        if (expiration < clock.currentTimeMillis()) {
          return makeOAuthProblemReport(OAuthConstants.PROBLEM_ACCESS_TOKEN_EXPIRED,
              "token needs to be refreshed", HttpResponse.SC_UNAUTHORIZED);
        }
      }
      responseBody = "User data is " + state.getUserData();
    } else {
      // Check the signature
      validateMessage(accessor, info, false);

      // For signed fetch, just echo back the query parameters in the body
      responseBody = request.getUri().getQuery();
    }


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

  private void validateMessage(OAuthAccessor accessor, MessageInfo info, boolean tokenEndpoint)
      throws OAuthException, IOException, URISyntaxException {
    OAuthValidator validator = new FakeTimeOAuthValidator();
    validator.validateMessage(info.message,accessor);

    String bodyHash = info.message.getParameter("oauth_body_hash");
    if (tokenEndpoint && bodyHash != null) {
      throw new RuntimeException("Can't have body hash on token endpoints");
    }
    SignatureType sigType = OAuthUtil.getSignatureType(tokenEndpoint,
        info.request.getHeader("Content-Type"));
    switch (sigType) {
      case URL_ONLY:
        break;
      case URL_AND_FORM_PARAMS:
        if (bodyHash != null) {
          throw new RuntimeException("Can't have body hash in form-encoded request");
        }
        break;
      case URL_AND_BODY_HASH:
        if (bodyHash == null) {
          throw new RuntimeException("Requiring oauth_body_hash parameter");
        }
        byte[] received = Base64.decodeBase64(CharsetUtil.getUtf8Bytes(bodyHash));
        byte[] expected = GenericDigestUtils.digest(info.rawBody);
        if (!Arrays.equals(received, expected)) {
          throw new RuntimeException("oauth_body_hash mismatch");
        }
    }

    // Most OAuth service providers are much laxer than this about checking nonces (rapidly
    // changing server-side state scales badly), but we are very strict in test cases.
    String nonceKey = info.message.getConsumerKey() + ','
        + info.message.getParameter("oauth_nonce");

    CachedObject<OAuthMessage> previousMessage = nonceCache.getElement(nonceKey);
    if (previousMessage != null) {
      throw new RuntimeException("Reused nonce, old message = " + previousMessage.obj
          + ", new message " + info.message);
    }
    nonceCache.addElement(nonceKey, info.message, TimeUnit.SECONDS.toMillis(10 * 60));
  }

  private HttpResponse handleNotFoundUrl(HttpRequest request) throws Exception {
    return new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_NOT_FOUND)
        .setResponseString("not found")
        .create();
  }

  private HttpResponse handleError400Url(HttpRequest request) throws Exception {
    return new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_BAD_REQUEST)
        .setResponseString("bad request")
        .create();
  }

  private HttpResponse handleEchoUrl(HttpRequest request) throws Exception {
    String query = request.getUri().getQuery();
    if (query.contains("add_oauth_token")) {
      query = query + "&oauth_token=abc";
    }
    return new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponseString(query)
        .create();
  }

  public void setConsumersThrottled(boolean throttled) {
    this.throttled = throttled;
  }

  public void setConsumerUnauthorized(boolean unauthorized) {
    this.unauthorized = unauthorized;
  }

  public void setReturnNull(boolean returnNull) {
    this.returnNull = returnNull;
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

  /**
   * Validate oauth messages using a fake time source.
   */
  private class FakeTimeOAuthValidator extends SimpleOAuthValidator {
    @Override
    protected long currentTimeMsec() {
      return clock.currentTimeMillis();
    }
  }

  public void setThrow(GadgetException gadgetException) {
    this.gadgetException = gadgetException;
  }

  public void setThrow(RuntimeException runtimeException) {
    this.runtimeException = runtimeException;
  }

  public void setCheckTrustedParams(boolean checkTrustedParams) {
    this.checkTrustedParams = checkTrustedParams;
  }

  public int getTrustedParamCount() {
    return trustedParamCount;
  }

  public void setExpectedRequestSecurityToken( SecurityToken requestSecurityToken ) {
    this.expectedRequestSecurityToken = requestSecurityToken;
  }
}
