<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Exception class thrown when a paramter is incorrectly configured on the
 * MakeRequestOptions class.
 */
class MakeRequestParameterException extends Exception {}

/**
 * Class that manages the configuration of a makeRequest fetch call.
 * Example - initializing a feed fetch manually:
 * <code>
 *   $context = new GadgetContext('GADGET');
 *   $params = new MakeRequestOptions('http://www.example.com');
 *   $params->setResponseFormat('FEED')
 *          ->setNoCache(true)
 *          ->setNumEntries(10)
 *          ->setGetSummaries(true));
 *   $result = $this->makeRequest->fetch($context, $params);
 * </code>
 *
 * Additionally, this class can configure itself from the current HTTP request
 * (useful in a servlet).  Example:
 * <code>
 *   $context = new GadgetContext('GADGET');
 *   $params = MakeRequestOptions::fromCurrentRequest();
 *   $result = $this->makeRequest->fetch($context, $params);
 * </code>
 */
class MakeRequestOptions {
  const DEFAULT_REFRESH_INTERVAL = 3600;
  const DEFAULT_HTTP_METHOD = 'GET';
  const DEFAULT_OUTPUT_FORMAT = 'JSON';
  const DEFAULT_AUTHZ = 'NONE';
  const DEFAULT_SIGN_VIEWER = true;
  const DEFAULT_SIGN_OWNER = true;
  const DEFAULT_OAUTH_USE_TOKEN = 'IF_AVAILABLE';
  const DEFAULT_NUM_ENTRIES = 3;
  const DEFAULT_GET_SUMMARIES = false;

  static $VALID_HTTP_METHODS = array('GET', 'PUT', 'POST', 'HEAD', 'DELETE');
  static $VALID_OUTPUT_FORMATS = array('TEXT', 'JSON', 'FEED', 'DOM');
  static $VALID_AUTHZ = array('OAUTH', 'NONE', 'SIGNED');
  static $VALID_OAUTH_USE_TOKEN = array('NEVER', 'IF_AVAILABLE', 'ALWAYS');

  private $href;
  private $method;
  private $body;
  private $headers;
  private $format;
  private $authz;
  private $signViewer;
  private $signOwner;
  private $oauthServiceName;
  private $oauthTokenName;
  private $oauthRequestToken;
  private $oauthRequestTokenSecret;
  private $oauthUseToken;
  private $oauthClientState;
  private $noCache;
  private $refreshInterval;
  private $numEntries;
  private $getSummaries;
  private $st;

  /**
   * Constructor.
   *
   * @param string $href Url to fetch.
   */
  public function __construct($href) {
    $this->href = MakeRequestOptions::validateUrl($href);
  }

  /**
   * Throws an exception if the supplied parameter is not in a set of values.
   *
   * @param mixed $param Parameter to check.
   * @param array $values Valid values.
   * @return mixed The value if the parameter exists in the array.  If a
   *     string is passed, the string is set to uppercase before being checked
   *     against the array and returned as an uppercase string.
   * @throws MakeRequestParameterException If the value was not found in the
   *     array.
   */
  private function assertParameterIsOneOf($param, $values) {
    if (is_string($param)) {
      $param = strtoupper($param);
    }
    if (!in_array($param, $values)) {
      throw new MakeRequestParameterException("Got an invalid value, was expecting one of " . implode(', ', $values));
    }
    return $param;
  }

  /**
   * Validates that a passed in argument is actually an url.
   *
   * @param string $url The parameter to check.
   * @return string The url if it can be parsed as an url.
   * @throws MakeRequestParameterException If the url could not be parsed.
   */
  public static function validateUrl($url) {
    if (empty($url) || !@parse_url($url)) {
      throw new MakeRequestParameterException("Invalid Url");
    } else {
      return $url;
    }
  }

  /**
   * Attempts to pull the requested parameter from the current HTTP request
   * and return it.  If a type is specified and the requests contains a
   * parameter with the specified name, the value of the parameter is attempted
   * to be coerced to the requested type.  PHP's settype is used to perform
   * the cast, although a special case is considered for the string "false"
   * which should evaluate to boolean false in the case of http requests.
   *
   * @param string $name The name of the parameter to check for.  This method
   *     examines the $_GET superglobal first, then the $_POST.
   * @param string $optType An optional name of a type to cerce to.  Check the
   *     documentation for PHP's <code>settype</code> function to see valid
   *     values for this parameter.
   * @return mixed The value of the parameter, or null if it didn't exist.
   * @throws MakeRequestParameterException If a type was specified and the
   *     argument could not be converted to the correct type.
   */
  private static function getRequestParam($name, $optType = null) {
    $param = null;
    if (array_key_exists($name, $_GET)) {
      $param = $_GET[$name];
    } else if (array_key_exists($name, $_POST)) {
      $param = $_POST[$name];
    }
    if (empty($param)) {
      $param = null;
    }
    if (isset($param) && isset($optType)) {
      switch (strtolower($optType)) {
        case 'boolean':
        case 'bool':
          if (($param) === "false") {
            $param = "0";
          }
      }
      if (!settype($param, $optType)) {
        throw new MakeRequestParameterException("Parameter '$name' should be convertable to $optType.");
      }
    }
    return $param;
  }

  /**
   * Builds a MakeRequestOptions object from the current $_GET and $_POST
   * superglobals.
   *
   * @return MakeRequestOptions An object initialized from the current request.
   * @throws MakeRequestParameterException If any of the parameters were
   *     invalid.
   */
  public static function fromCurrentRequest(){
    $href = MakeRequestOptions::getRequestParam('href');
    if (!isset($href)) {
      $href = MakeRequestOptions::getRequestParam('url');
    }

    $options = new MakeRequestOptions($href);
    $options->setHttpMethod(MakeRequestOptions::getRequestParam('httpMethod'))
            ->setRequestBody(MakeRequestOptions::getRequestParam('postData'))
            ->setFormEncodedRequestHeaders(MakeRequestOptions::getRequestParam('headers'))
            ->setResponseFormat(MakeRequestOptions::getRequestParam('contentType'))
            ->setAuthz(MakeRequestOptions::getRequestParam('authz'))
            ->setSignViewer(MakeRequestOptions::getRequestParam('signViewer', 'boolean'))
            ->setSignOwner(MakeRequestOptions::getRequestParam('signOwner', 'boolean'))
            ->setNumEntries(MakeRequestOptions::getRequestParam('numEntries', 'integer'))
            ->setGetSummaries(MakeRequestOptions::getRequestParam('getSummaries', 'boolean'))
            ->setRefreshInterval(MakeRequestOptions::getRequestParam('refreshInterval', 'integer'))
            ->setNoCache(MakeRequestOptions::getRequestParam('bypassSpecCache', 'boolean'))
            ->setOAuthServiceName(MakeRequestOptions::getRequestParam('OAUTH_SERVICE_NAME'))
            ->setOAuthTokenName(MakeRequestOptions::getRequestParam('OAUTH_TOKEN_NAME'))
            ->setOAuthRequestToken(MakeRequestOptions::getRequestParam('OAUTH_REQUEST_TOKEN'))
            ->setOAuthRequestTokenSecret(MakeRequestOptions::getRequestParam('OAUTH_REQUEST_TOKEN_SECRET'))
            ->setOAuthUseToken(MakeRequestOptions::getRequestParam('OAUTH_USE_TOKEN'))
            ->setOAuthClientState(MakeRequestOptions::getRequestParam('oauthState'))
            ->setSecurityTokenString(MakeRequestOptions::getRequestParam('st'));

    return $options;
  }

  /**
   * Builds a MakeRequestOptions object from a RequestItem instance.  This is
   * a helper for dealing with Handler services which need to call MakeRequest.
   * The parameter names were taken from the osapi.http spec documents, although
   * several parameters not in the spec are also supported to allow full
   * functionality.
   *
   * @param RpcRequestItem $request The RpcRequestItem to parse.  The reason
   *     RpcRequestItem is needed is because of the way getService() and
   *     getMethod() are overloaded in the RequestItem subclasses.  This
   *     function needs a reliable way to get the http method.
   * @return MakeRequestOptions An object initialized from the current request.
   * @throws MakeRequestParameterException If any of the parameters were
   *     invalid.
   */
  public static function fromRpcRequestItem(RpcRequestItem $request) {
    $href = $request->getParameter('href');
    if (!isset($href)) {
      $href = $request->getParameter('url');
    }

    $options = new MakeRequestOptions($href);
    $options->setHttpMethod($request->getMethod())
            ->setRequestBody($request->getParameter('body'))
            ->setRequestHeaders($request->getParameter('headers'))
            ->setResponseFormat($request->getParameter('format'))
            ->setAuthz($request->getParameter('authz'))
            ->setSignViewer($request->getParameter('sign_viewer'))     
            ->setSignOwner($request->getParameter('sign_owner'))
            ->setNumEntries($request->getParameter('numEntries')) // Not in osapi.http spec, but nice to support
            ->setGetSummaries($request->getParameter('getSummaries')) // Not in osapi.http spec, but nice to support
            ->setRefreshInterval($request->getParameter('refreshInterval'))
            ->setNoCache($request->getParameter('nocache')) // Not in osapi.http spec, but nice to support
            ->setOAuthServiceName($request->getParameter('oauth_service_name'))
            ->setOAuthTokenName($request->getParameter('oauth_token_name'))
            ->setOAuthRequestToken($request->getParameter('oauth_request_token'))
            ->setOAuthRequestTokenSecret($request->getParameter('oauth_request_token_secret'))
            ->setOAuthUseToken($request->getParameter('oauth_use_token'))
            ->setOAuthClientState($request->getParameter('oauth_state')) // Not in osapi.http spec, but nice to support
            ->setSecurityTokenString(urlencode(base64_encode($request->getToken()->toSerialForm())));

    return $options;
  }

  /**
   * Gets the configured URL.
   *
   * @return string The value of this parameter.
   */
  public function getHref() {
    return $this->href;
  }

  /**
   * Sets the http method to use for this request.  Must be one of
   * {@link MakeRequestOptions::$VALID_HTTP_METHODS}.
   *
   * @param string $method The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setHttpMethod($method) {
    if (isset($method)) {
      $this->method = $this->assertParameterIsOneOf($method, MakeRequestOptions::$VALID_HTTP_METHODS);
    }
    return $this;
  }

  /**
   * Gets the configured HTTP method.
   *
   * @return string The value of this parameter.
   */
  public function getHttpMethod() {
    return isset($this->method) ? $this->method : MakeRequestOptions::DEFAULT_HTTP_METHOD;
  }

  /**
   * Sets the request body.
   *
   * @param string $body The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setRequestBody($body) {
    if (isset($body)) {
      $this->body = $body;
    }
    return $this;
  }

  public function getRequestBody() {
    return isset($this->body) ? $this->body : null;
  }

  /**
   * Sets the headers to use when making the request.
   *
   * @param array $headers An array of key/value pairs to use as HTTP headers.
   *     Example:
   *     <code>
   *     $params->setRequestHeaders(array(
   *         'Content-Type' => 'text/plain',
   *         'Accept-Language' => 'en-us'
   *     ));
   *     </code>
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setRequestHeaders(array $headers) {
    if (isset($headers)) {
      $this->headers = $headers;
    }
    return $this;
  }

  /**
   * Sets the headers to use when making the request.
   *
   * @param string $headers A form-urlencoded string of key/values to use as
   *     HTTP headers for the request.  The OpenSocial JavaScript library
   *     passes makeRequest headers in this format, so this is just a
   *     convenience method.
   *     Example:
   *     <code>
   *     $params->setFormEncodedRequestHeaders(
   *       "Content-Type=text/plain&Accept-Language=en-us"
   *     );
   *     </code>
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setFormEncodedRequestHeaders($headers) {
    if (isset($headers)) {
      $headerLines = explode("&", $headers);
      $this->headers = array();
      foreach ($headerLines as $line) {
        $parts = explode("=", $line);
        if (count($parts) == 2) {
          $this->headers[urldecode($parts[0])] = urldecode($parts[1]);
        }
      }
    }
    return $this;
  }

  /**
   * Gets the configured request headers in the HTTP header format (separated
   * by newlines, with a key-colon-space-value format).  Example:
   * <code>
   *   Content-Type: text/plain
   *   Accept-Language: en-us
   * </code>
   *
   * @return string The value of this parameter.
   */
  public function getFormattedRequestHeaders() {
    if (isset($this->headers)) {
      $headerString = http_build_query($this->headers);
      return urldecode(str_replace("&", "\n", str_replace("=", ": ", $headerString)));
    } else {
      return false;
    }
  }

  /**
   * Returns the request headers as an array.
   *
   * @return array The request header array.
   */
  public function getRequestHeadersArray() {
    if (isset($this->headers)) {
      return $this->headers;
    } else {
      return false;
    }
  }

  /**
   * Sets the expected response format for this type of request.  Valid values
   * are one of {@link MakeRequestOptions::$VALID_OUTPUT_FORMATS}.
   *
   * @param string $format The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setResponseFormat($format) {
    if (isset($format)) {
      $this->format = $this->assertParameterIsOneOf($format, MakeRequestOptions::$VALID_OUTPUT_FORMATS);
    }
    return $this;
  }

  /**
   * Gets the configured response format.
   *
   * @return string The value of this parameter.
   */
  public function getResponseFormat() {
    return isset($this->format) ? $this->format : MakeRequestOptions::DEFAULT_OUTPUT_FORMAT;
  }

  /**
   * Sets the authorization type of the request.  Must be one of
   * {@link MakeRequestOptions::$VALID_AUTHZ}.
   *
   * @param string $authz The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setAuthz($authz) {
    if (isset($authz)) {
      $this->authz = $this->assertParameterIsOneOf($authz, MakeRequestOptions::$VALID_AUTHZ);
    }
    return $this;
  }

  /**
   * Gets the configured authz parameter.
   *
   * @return string The value of this parameter.
   */
  public function getAuthz() {
    return isset($this->authz) ? $this->authz : MakeRequestOptions::DEFAULT_AUTHZ;
  }

  /**
   * Sets whether to include the viewer's ID in a signed request.
   *
   * @param bool $signViewer True to include the viewer's ID.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setSignViewer($signViewer) {
    if (isset($signViewer)) {
      if (!is_bool($signViewer)) {
        throw new MakeRequestParameterException("signViewer must be a boolean.");
      }
      $this->signViewer = $signViewer;
    }
    return $this;
  }

  /**
   * Gets the configured value of whether to sign with the viewer ID or not.
   *
   * @return string The value of this parameter.
   */
  public function getSignViewer() {
    return isset($this->signViewer) ? $this->signViewer : MakeRequestOptions::DEFAULT_SIGN_VIEWER;
  }

  /**
   * Sets whether to include the owner's ID in a signed request.
   *
   * @param bool $signOwner True to include the owner's ID.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setSignOwner($signOwner) {
    if (isset($signOwner)) {
      if (!is_bool($signOwner)) {
        throw new MakeRequestParameterException("signOwner must be a boolean.");
      }
      $this->signOwner = $signOwner;
    }
    return $this;
  }

  /**
   * Gets the configured value of whether to sign with the owner ID or not.
   *
   * @return string The value of this parameter.
   */
  public function getSignOwner() {
    return isset($this->signOwner) ? $this->signOwner : MakeRequestOptions::DEFAULT_SIGN_OWNER;
  }

  /**
   * Sets the OAuth service name.
   *
   * @param string $serviceName The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setOAuthServiceName($serviceName) {
    if (isset($serviceName)) {
      $this->oauthServiceName = $serviceName;
    }
    return $this;
  }

  /**
   * Gets the configured OAuth service name.
   *
   * @return string The value of this parameter.
   */
  public function getOAuthServiceName() {
    return isset($this->oauthServiceName) ? $this->oauthServiceName : '';
  }

  /**
   * Sets the OAuth token name.
   *
   * @param string $tokenName The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setOAuthTokenName($tokenName) {
    if (isset($tokenName)) {
      $this->oauthTokenName = $tokenName;
    }
    return $this;
  }

  /**
   * Gets the configured OAuth token name.
   *
   * @return string The value of this parameter.
   */
  public function getOAuthTokenName() {
    return isset($this->oauthTokenName) ? $this->oauthTokenName : '';
  }

  /**
   * Sets the OAuth request token.
   *
   * @param string $requestToken The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setOAuthRequestToken($requestToken) {
    if (isset($requestToken)) {
      $this->oauthRequestToken = $requestToken;
    }
    return $this;
  }

  /**
   * Gets the configured OAuth request token.
   *
   * @return string The value of this parameter.
   */
  public function getOAuthRequestToken() {
    return isset($this->oauthRequestToken) ? $this->oauthRequestToken : '';
  }

  /**
   * Sets the OAuth request token secret.
   *
   * @param string $requestTokenSecret The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setOAuthRequestTokenSecret($requestTokenSecret) {
    if (isset($requestTokenSecret)) {
      $this->oauthRequestTokenSecret = $requestTokenSecret;
    }
    return $this;
  }

  /**
   * Gets the configured OAuth request token secret.
   *
   * @return string The value of this parameter.
   */
  public function getOAuthRequestTokenSecret() {
    return isset($this->oauthRequestTokenSecret) ? $this->oauthRequestTokenSecret : '';
  }

  /**
   * Sets whether to use an OAuth token.  Must be one of
   * {@link MakeRequestOptions::$VALID_OAUTH_USE_TOKEN}.
   *
   * @param string $oauthUseToken The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setOAuthUseToken($oauthUseToken) {
    if (isset($oauthUseToken)) {
      $this->oauthUseToken = $this->assertParameterIsOneOf($oauthUseToken, MakeRequestOptions::$VALID_OAUTH_USE_TOKEN);
    }
    return $this;
  }

  /**
   * Gets the configured value of whether to use the OAuth token.
   *
   * @return string The value of this parameter.
   */
  public function getOAuthUseToken() {
    return isset($this->oauthUseToken) ? $this->oauthUseToken : MakeRequestOptions::DEFAULT_OAUTH_USE_TOKEN;
  }

  /**
   * Sets the OAuth client state.
   *
   * @param string $oauthClientState The value to use.
   * @return MakeRequestOptions This object (for chaining purporses).
   */
  public function setOAuthClientState($oauthClientState) {
    if (isset($oauthClientState)) {
      $this->oauthClientState = $oauthClientState;
    }
    return $this;
  }

  /**
   * Gets the configured OAuth client state.
   *
   * @return string The value of this parameter.
   */
  public function getOAuthClientState() {
    return isset($this->oauthClientState) ? $this->oauthClientState : null;
  }

  /**
   * Gets all of the configured OAuth parameters as an OAuthRequestParams
   * object.
   *
   * @return OAuthRequestParams The collection of OAuth parameters.
   */
  public function getOAuthRequestParameters() {
    return new OAuthRequestParams(array(
        OAuthRequestParams::$SERVICE_PARAM => $this->getOAuthServiceName(),
        OAuthRequestParams::$TOKEN_PARAM => $this->getOAuthTokenName(),
        OAuthRequestParams::$REQUEST_TOKEN_PARAM => $this->getOAuthRequestToken(),
        OAuthRequestParams::$REQUEST_TOKEN_SECRET_PARAM => $this->getOAuthRequestTokenSecret(),
        OAuthRequestParams::$BYPASS_SPEC_CACHE_PARAM => $this->getNoCache(),
        OAuthRequestParams::$CLIENT_STATE_PARAM => $this->getOAuthClientState()
    ));
  }

  /**
   * Sets whether to bypass the cache for this request.
   *
   * @param bool $noCache True if the request should bypass the cache.
   * @return MakeRequestOptions This object (for chaining purporses)
   */
  public function setNoCache($noCache) {
    if (isset($noCache)) {
      if (!is_bool($noCache)) {
        throw new MakeRequestParameterException("noCache must be a boolean.");
      }
      $this->noCache = $noCache;
    }
    return $this;
  }

  /**
   * Gets the configured value of whether to bypass the cache.
   *
   * @return string The value of this parameter.
   */
  public function getNoCache() {
    return isset($this->noCache) ? $this->noCache : false;
  }

  /**
   * Sets the refresh interval for this request.  Must be an integer equal to
   * or greater than 0.
   *
   * @param int $refreshInterval The value to use
   * @return MakeRequestOptions This object (for chaining purporses)
   */
  public function setRefreshInterval($refreshInterval) {
    if (isset($refreshInterval)) {
      if (!is_int($refreshInterval) || $refreshInterval < 0) {
        throw new MakeRequestParameterException('Refresh interval must be greater than or equal to 0');
      }
      $this->refreshInterval = $refreshInterval;
    }
    return $this;
  }

  /**
   * Gets the configured refresh interval.
   *
   * @return string The value of this parameter.
   */
  public function getRefreshInterval() {
    return isset($this->refreshInterval) ? $this->refreshInterval : MakeRequestOptions::DEFAULT_REFRESH_INTERVAL;
  }

  /**
   * Sets the number of entries to return for format = FEED requests.  Must
   * be an integer greater than 0.
   *
   * @param int $numEntries The value to use
   * @return MakeRequestOptions This object (for chaining purporses)
   */
  public function setNumEntries($numEntries) {
    if (isset($numEntries)) {
      if (!is_int($numEntries) || $numEntries <= 0) {
        throw new MakeRequestParameterException('NumEntries must be greater than 0');
      }
      $this->numEntries = $numEntries;
    }
    return $this;
  }

  /**
   * Gets the configured number of entries to return for a feed request.
   *
   * @return string The value of this parameter.
   */
  public function getNumEntries() {
    return isset($this->numEntries) ? $this->numEntries : MakeRequestOptions::DEFAULT_NUM_ENTRIES;
  }

  /**
   * Sets whether to fetch summaries for format = FEED requests.
   *
   * @param bool $getSummaries The value to use
   * @return MakeRequestOptions This object (for chaining purporses)
   */
  public function setGetSummaries($getSummaries) {
    if (isset($getSummaries)) {
      if (!is_bool($getSummaries)) {
        throw new MakeRequestParameterException("getSummaries must be a boolean.");
      }
      $this->getSummaries = $getSummaries;
    }
    return $this;
  }

  /**
   * Gets the configured value of whether to fetch summaries for a feed request.
   *
   * @return string The value of this parameter.
   */
  public function getGetSummaries() {
    return isset($this->getSummaries) ? $this->getSummaries : MakeRequestOptions::DEFAULT_GET_SUMMARIES;
  }

  /**
   * Sets a security token string.  Required for signed or OAuth requests.
   *
   * @param string $st The value to use
   * @return MakeRequestOptions This object (for chaining purporses)
   */
  public function setSecurityTokenString($st) {
    if (isset($st)) {
      $this->st = $st;
    }
    return $this;
  }

  /**
   * Gets the configured security token string.
   *
   * @return string The value of this parameter.
   */
  public function getSecurityTokenString() {
    return isset($this->st) ? $this->st : false;
  }
}
