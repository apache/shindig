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
package org.apache.shindig.social.core.oauth;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.social.opensocial.oauth.OAuthConsumerStore;
import org.apache.shindig.social.opensocial.oauth.OAuthTokenPrincipalMapper;
import org.apache.shindig.social.opensocial.oauth.OAuthTokenStore;
import org.apache.shindig.social.opensocial.oauth.PrincipalMapperException;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.server.OAuthServlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filter that attempts to authenticate an incoming HTTP request. There are
 * three different cases:
 *
 * (1) If the incoming request is from the container's own gadget server, the
 *     request will include a gadget security token. The gadget security token
 *     includes the owner and the viewer, expressed in identities native to the
 *     container. The principal that will be represented to the servlets as
 *     "making the request" is a {@link DelegatedPrincipal} naming the viewer
 *     as the delegator and the container as the delegatee (i.e., it's
 *     considered to be the container speaking on behalf of the viewer, which
 *     may carry less privileges with it than if the user spoke directly).
 *
 * (2) If the incoming request is from a third-party-server that uses "full"
 *     (as opposed to "two-legged") OAuth, then the request will include an
 *     OAuth token that identifies the user. The
 *     {@link OAuthTokenPrincipalMapper} allows containers to map this OAuth
 *     token to a principal that represents the delegation afforded by the
 *     OAuth token. The delegator named in that principal will be the user who
 *     issued the token, and the delegatee will be the OAuth consumer making
 *     the call.
 *
 * (3) If the incoming request is from a third-party-server that uses
 *     "two-legged" OAuth (aka Consumer Request), it won't include a gadget
 *     security token. Instead, it must include a xoauth_requestor_id parameter,
 *     which names the delegator in a format native to the container.
 *     The principal that will be represented to the servlets as "making the
 *     request" is the {@linkp DelegatedPrincipal} naming the that delegator,
 *     and naming the OAuth consumer as the delegatee.
 *
 * (4) Finally, the filter can also authenticate pure "two-legged" OAuth, i.e.,
 *     an OAuth request authenticated only by the consumer making the call, but
 *     not carrying any information identifying the principal "making the
 *     request".
 *
 * In each case a suitable {@link HttpServletRequest} will be passed onto the
 * next filter and/or gadget in the filter chain. In each of the above cases,
 * <code>HttpServletRequest.getAuthType()</code> will return an appropriate
 * string ("GadgetSecurityToken" in case (1), "OAuth" in case (2), and
 * "OAuth-ConsumerRequest" in cases (3) and (4)). In the
 * three cases in which he principal making the request can be authenticated,
 * <code>HttpServletRequest.getUserPrincipal()</code> will return the principal
 * making the request. In case (4), getUserPrincipal() returns null.
 *
 * We resolve the various cases (in case they are overlapping) like this:
 *
 * - If there is a gadget security token present we use that to
 *   establish the identity of the delegator, and we ignore any
 *   possibly-present xoauth_requestor_id parameter or OAuth signatures.
 *
 * - Else, the request has to carry an OAuth signature. If there is an OAuth
 *   token present we use this to establish the identity of the delegator, and
 *   we ignore any possibly-present xoauth_requestor_id parameters.
 *
 * - Else, if there is an xoauth_requestor_id parameter available, we use
 *   that to establish the identity of the delegator.
 *
 * - Else, we don't identify the user principal at all, and just return
 *   "OAuth-ConsumerRequest" in HttpServletRequest.getAuthType()
 */
public class AuthenticationServletFilter implements Filter {

  public static final String ALLOW_UNAUTHENTICATED = "auth.allow-unauthenticated";

  public static final String AUTH_TYPE_OAUTH = "OAuth";
  public static final String AUTH_TYPE_OAUTH_CR = "OAuth-ConsumerRequest";
  public static final String AUTH_TYPE_SECURITY_TOKEN = "GadgetSecurityToken";

  // the name of the URI query parameter that is the gadget security token
  protected static final String SECURITY_TOKEN_PARAM = "st";

  // the name of the parameter that identifies an explicitly-named requestor
  // in the case of two-legged OAuth in the absence of a gadget security token
  protected static final String REQUESTOR_ID_PARAM = "xoauth_requestor_id";

  private static final Logger LOG =
      Logger.getLogger(AuthenticationServletFilter.class.getName());

  /*
  * When this is set to true, the filter will forward HTTP requests down the
  * filter chain (and, ultimately, to the servlet), even if the request could not be
  * authenticated by this filter
  */
  private boolean allowUnauthenticated;

  // object that can validate OAuth requests (includes signature check)
  private OAuthValidator oauthValidator;

  // object that can map consumer keys to consumer secrets
  private OAuthConsumerStore consumerStore;

  // object that can map tokens to token secrets/
  private OAuthTokenStore tokenStore;

  // object that can map tokens to principals (user ids).
  private OAuthTokenPrincipalMapper tokenMapper;

  // object that can decode a gadget security token, e.g. extract the viewer
  // id from it
  private SecurityTokenDecoder securityTokenDecoder;

  /**
   * Initializes the filter. We retrieve the Guice injector and ask for all
   * the injected methods to be called, setting a variety of helper objects
   * and configuration state.
   */
  public void init(FilterConfig filterConfig) throws ServletException {
    ServletContext context = filterConfig.getServletContext();
    Injector injector = (Injector)
        context.getAttribute(GuiceServletContextListener.INJECTOR_ATTRIBUTE);
    if (injector == null) {
      throw new UnavailableException(
          "Guice Injector not found! Make sure you registered " 
          + GuiceServletContextListener.class.getName() + " as a listener");
    }
    injector.injectMembers(this);
  }

  @Inject
  public void setAuthConfiguration(
      @Named(ALLOW_UNAUTHENTICATED) Boolean allowUnauthenticated) {

    this.allowUnauthenticated = allowUnauthenticated;
  }

  @Inject
  public void setOAuthUtils(OAuthValidator oauthValidator,
      OAuthTokenStore tokenStore,
      OAuthConsumerStore consumerStore,
      OAuthTokenPrincipalMapper tokenMapper) {

    this.oauthValidator = oauthValidator;
    this.consumerStore = consumerStore;
    this.tokenStore = tokenStore;
    this.tokenMapper = tokenMapper;
  }

  @Inject
  public void setSecurityTokenDecoder(SecurityTokenDecoder decoder) {
    this.securityTokenDecoder = decoder;
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    if ( !((request instanceof HttpServletRequest)
        && (response instanceof HttpServletResponse))) {
      throw new ServletException("Auth filter can only handle HTTP");
    }

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    try {
      HttpServletRequest wrappedRequest = handleRequest(req);
      chain.doFilter(wrappedRequest, response);
    } catch (OAuthProblemException e) {
      if (allowUnauthenticated) {
        chain.doFilter(request, response);
      } else {
        handleException(resp, e);
      }
    }
  }

  /**
   * Handles an incoming HTTP request.
   * @return an {@link HttpServletRequest} suitable for passing on to the next
   *         element in the filter chain.
   */
  protected HttpServletRequest handleRequest(HttpServletRequest request)
      throws ServletException, IOException, OAuthProblemException {

    if (requestUsesSecurityToken(request)) {
      return handleSecurityTokenRequest(request);

    } else {  // this means the request has to use OAuth

      OAuthMessage requestMessage = getOAuthMessageFromRequest(request);
      requireOAuth(requestMessage);

      // it uses OAuth, but is there an OAuth token?
      if (requestMessage.getToken() == null) {

        // this means it's either using xoauth_requestor_id or plain signedFetch
        return handleConsumerRequest(request, requestMessage);
      } else {

        // this is the case for full OAuth
        return handleFullOAuth(request, requestMessage);
      }
    }
  }

  protected boolean requestUsesSecurityToken(HttpServletRequest request) {
    return (getSecurityToken(request) != null);
  }

  /**
   * Handles the case where a GadgetSecurityToken is used to authenticate the
   * request.
   */
  protected HttpServletRequest handleSecurityTokenRequest(HttpServletRequest request) {
    SecurityToken securityToken = getSecurityToken(request);
    return new AuthServletRequest(request,
        new GadgetSecurityTokenPrincipal(securityToken),
        AUTH_TYPE_SECURITY_TOKEN);
  }

  /**
   * Handles a request that includes an OAuth signature, but no OAuth access
   * token.
   * @param request the HTTP request.
   * @param requestMessage the HTTP request, parsed out into an OAuthMessage.
   * @return an {@link HttpServletRequest} suitable for passing on to the next
   *         element in the filter chain.
   * @throws OAuthProblemException if there was a problem with the HTTP request.
   */
  protected HttpServletRequest handleConsumerRequest(HttpServletRequest request,
      OAuthMessage requestMessage)
      throws IOException, ServletException, OAuthProblemException {

    if (requestUsesRequestorId(request)) {

      // handle the case when xoauth_requestor_id is specified
      return handleRequestorIdRequest(request, requestMessage);

    } else {  // no requestorId, but oauth signature

      // if signature verifies, we'll set the auth method to OAuth-ConsumerRequest.
      // (and a null principal).
      validateConsumerRequestMessage(requestMessage);

      return new AuthServletRequest(request, null, AUTH_TYPE_OAUTH_CR);
    }
  }

  /**
   * Returns true if the request contains an xoauth_requestor_id parameter.
   */
  protected boolean requestUsesRequestorId(HttpServletRequest request) {
    return (getRequestorId(request) != null);
  }

  /**
   * Reads the requestor id out of an {@link HttpServletRequest}, making sure
   * to return null if there are problems.
   */
  protected String getRequestorId(HttpServletRequest request) {
    String id = request.getParameter(REQUESTOR_ID_PARAM);
    if (id == null || id.trim().length() == 0) {
      return null;
    } else {
      return id.trim();
    }
  }

  /**
   * Handles a request that includes an xoauth_requestor_id and an OAuth
   * signature.
   * @param request the HTTP request.
   * @param requestMessage the HTTP request, parsed out into an OAuthMessage.
   * @return an {@link HttpServletRequest} suitable for passing on to the next
   *         element in the filter chain.
   * @throws OAuthProblemException if there was a problem with the HTTP request.
   */
  protected HttpServletRequest handleRequestorIdRequest(HttpServletRequest request,
      OAuthMessage requestMessage)
      throws IOException, OAuthProblemException, ServletException {

    validateConsumerRequestMessage(requestMessage);

    String userId = getRequestorId(request);

    return new AuthServletRequest(request,
        new RequestorIdPrincipal(userId, requestMessage.getConsumerKey()),
        AUTH_TYPE_OAUTH_CR);
  }

  /**
   * Reads the gadget security token out of an {@link HttpServletRequest},
   * making sure to return null if there are problems.
   */
  protected SecurityToken getSecurityToken(HttpServletRequest request) {
    String token = request.getParameter(SECURITY_TOKEN_PARAM);

    if (token == null || token.trim().length() == 0) {
      return null;
    }

    try {
      Map<String, String> params =
          Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME,
              token);
      return securityTokenDecoder.createToken(params);
    } catch (SecurityTokenException e) {
      String message = new StringBuilder()
          .append("found security token, but couldn't decode it ")
          .append("(treating it as not present). token is: ")
          .append(token)
          .toString();
      LOG.log(Level.WARNING, message, e);
      return null;
    }
  }

  /**
   * Handles the normal ("full") OAuth case, i.e., an {@link HttpServletRequest}
   * that includes an oauth_token alongside the other OAuth parameters.
   * @param request the {@link HttpServletRequest}
   * @param requestMessage the {@link HttpServletRequest}, parsed out into a
   *        {@link OAuthMessage}.
   * @return an {@link HttpServletRequest} suitable for passing on to the next
   *         element in the filter chain.
   * @throws OAuthProblemException when there is a problem authenticating the
   *         request.
   */
  protected HttpServletRequest handleFullOAuth(HttpServletRequest request,
      OAuthMessage requestMessage)
      throws IOException, ServletException, OAuthProblemException {

    // this throws if the message signature doesn't check out
    validateFullOAuthMessage(requestMessage);

    // map from token to identity
    Principal user;
    try {
      user = tokenMapper.getPrincipalForToken(requestMessage);
    } catch (PrincipalMapperException e) {
      throw new ServletException(e);
    }

    return new AuthServletRequest(request, user, AUTH_TYPE_OAUTH);
  }

  /**
   * Validates an OAuth message that includes a oauth_token. We look up the
   * verification key in the tokenStore.
   */
  protected void validateFullOAuthMessage(OAuthMessage requestMessage)
      throws IOException, OAuthProblemException, ServletException {
    String consumerKey = requestMessage.getConsumerKey();
    String accessToken = requestMessage.getToken();

    OAuthAccessor accessor = tokenStore.getAccessor(accessToken, consumerKey);

    validateMessage(requestMessage, accessor);
  }

  /**
   * Validates an OAuth ConsumerRequest message (i.e., one that doean't include
   * an oauth_token). We look up the verification key in the consumerStore.
   */
  protected void validateConsumerRequestMessage(OAuthMessage requestMessage)
      throws IOException, OAuthProblemException, ServletException {
    String consumerKey = requestMessage.getConsumerKey();
    OAuthAccessor accessor = consumerStore.getAccessor(consumerKey);

    validateMessage(requestMessage, accessor);
  }

  /**
   * Validates an OAuth request, making sure to throw appropriate exceptions
   * when things don't work out. In particular, an {@link OAuthProblemException}
   * is thrown when problems about the OAuth request should be communicated
   * back to the HTTP client.
   * @param requestMessage the OAuth message (HTTP request) to be verified
   * @param accessor the OAuth accessor that contains the key material needed
   *        to verify the request.
   */
  protected void validateMessage(OAuthMessage requestMessage,
      OAuthAccessor accessor)
      throws IOException, ServletException, OAuthProblemException {
    try {
      oauthValidator.validateMessage(requestMessage, accessor);
    } catch (OAuthException e) {
      if (e instanceof OAuthProblemException) {
        // this one we'll just re-throw since the caller knows how to handle it
        // (this includes the case where the signature doesn't verify)
        throw (OAuthProblemException) e;
      } else {
        throw new ServletException(e);
      }
    } catch (URISyntaxException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Determines whether the incoming HTTP request uses OAuth at all.
   * @throws IOException
   * @throws OAuthProblemException
   */
  protected void requireOAuth(OAuthMessage requestMessage) 
      throws OAuthProblemException, IOException {
    requestMessage.requireParameters(OAuth.OAUTH_CONSUMER_KEY, OAuth.OAUTH_SIGNATURE);
  }

  /**
   * Parses an incoming HTTP request out into an {@link OAuthMessage} object.
   */
  protected OAuthMessage getOAuthMessageFromRequest(HttpServletRequest req)
      throws OAuthProblemException {
    return OAuthServlet.getMessage(req, null);
  }

  /**
   * Sends back a properly-formatted OAuth problem report.
   */
  protected void handleException(HttpServletResponse response,
      OAuthProblemException e) throws IOException, ServletException {
    OAuthServlet.handleException(response, e, "");
  }

  /**
   * An {@link HttpServletRequest} that reports "OAuth" as the auth type and
   * a specific given user as the userPrincipal making the request.
   *
   */
  private static class AuthServletRequest extends HttpServletRequestWrapper {
    private final Principal user;
    private final String authType;

    public AuthServletRequest(HttpServletRequest request, Principal user,
        String authType) {
      super(request);
      this.user = user;
      this.authType = authType;
    }

    @Override
    public Principal getUserPrincipal() {
      return user;
    }

    @Override
    public String getAuthType() {
      return authType;
    }
  }
}
