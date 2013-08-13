<?php
namespace apache\shindig\gadgets\oauth;
use apache\shindig\common\RemoteContentRequest;
use apache\shindig\gadgets\GadgetException;
use apache\shindig\common\ShindigOAuthProtocolException;
use apache\shindig\common\RemoteContentFetcher;
use apache\shindig\common\ShindigOAuth;
use apache\shindig\common\ShindigOAuthUtil;
use apache\shindig\common\Config;
use apache\shindig\common\ShindigOAuthRequest;
use apache\shindig\common\BlobCrypterException;
use apache\shindig\common\SecurityToken;

/*
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

// For TokenInfo
/**
 * implements \the OAuth dance (http://oauth.net/core/1.0/) for gadgets.
 *
 * Reading the example in the appendix to the OAuth spec will be helpful to
 * those reading this code.
 *
 * This class is not thread-safe; create a new one for each request that
 * requires OAuth signing.
 */
class OAuthFetcher extends RemoteContentFetcher {

  // We store some blobs of data on the client for later reuse; the blobs
  // contain key/value pairs, and these are the key names.
  protected static $REQ_TOKEN_KEY = "r";
  protected static $REQ_TOKEN_SECRET_KEY = "rs";
  protected static $ACCESS_TOKEN_KEY = "a";
  protected static $ACCESS_TOKEN_SECRET_KEY = "as";
  protected static $OWNER_KEY = "o";

  // names for the JSON values we return to the client
  public static $CLIENT_STATE = "oauthState";
  public static $APPROVAL_URL = "oauthApprovalUrl";
  public static $ERROR_CODE = "oauthError";
  public static $ERROR_TEXT = "oauthErrorText";
  // names of additional OAuth parameters we include in outgoing requests
  public static $XOAUTH_APP_URL = "xoauth_app_url";
  public static $OAUTH_CALLBACK = "oauth_callback";

  /**
   * @var RemoteContentFetcher
   */
  protected $fetcher;

  /**
   * Maximum age for our client state; if this is exceeded we start over. One
   * hour is a fairly arbitrary time limit here.
   */
  protected static $CLIENT_STATE_MAX_AGE_SECS = 3600;

  /**
   * The gadget security token, with info about owner/viewer/gadget.
   */
  protected $authToken;

  /**
   * Parameters from makeRequest
   * @var OAuthRequestParams
   */
  protected $requestParams;

  /**
   * Reference to our persistent store for OAuth metadata.
   */
  protected $tokenStore;

  /**
   * The accessor we use for signing messages. This also holds metadata about
   * the service provider, such as their URLs and the keys we use to access
   * those URLs.
   * @var AccesorInfo
   */
  protected $accessorInfo;

  /**
   * We use this to encrypt and sign the state we cache on the client.
   */
  protected $oauthCrypter;

  /**
   * State the client sent with their request.
   */
  protected $origClientState = array();

  /**
   * The request the client really wants to make.
   * @var RemoteContentRequest
   */
  protected $realRequest;

  /**
   * State to cache on the client.
   */
  protected $newClientState;

  /**
   * Authorization URL for the client
   */
  protected $aznUrl;

  /**
   * Error code for the client
   */
  protected $error;

  /**
   * Error text for the client
   */
  protected $errorText;

  /**
   * Whether or not we're supposed to ignore the spec cache when referring
   * to the gadget spec for information (e.g. OAuth URLs).
   */
  protected $bypassSpecCache;

  protected $responseMetadata = array();

  /**
   *
   * @param $tokenStore storage for long lived tokens.
   * @param $oauthCrypter used to encrypt transient information we store on the
   *        client.
   * @param RemoteContentFetcher $fetcher
   * @param $authToken user's gadget security token
   * @param OAuthRequestParams $params OAuth fetch parameters sent from makeRequest
   */
  public function __construct($tokenStore, $oauthCrypter, $fetcher, $authToken, OAuthRequestParams $params) {
    $this->fetcher = $fetcher;
    $this->oauthCrypter = $oauthCrypter;
    $this->authToken = $authToken;
    $this->bypassSpecCache = $params->getBypassSpecCache();
    $this->requestParams = $params;
    $this->newClientState = null;
    $this->aznUrl = null;
    $this->error = null;
    $this->errorText = null;
    $origClientState = $params->getOrigClientState();
    if ($origClientState != null && strlen($origClientState) > 0) {
      try {
        $this->origClientState = $this->oauthCrypter->unwrap($origClientState, self::$CLIENT_STATE_MAX_AGE_SECS);
      } catch (BlobCrypterException $e) {  // Probably too old, pretend we never saw it at all.
      }
    }
    if ($this->origClientState == null) {
      $this->origClientState = array();
    }
    $this->tokenStore = $tokenStore;
  }

  /**
   *
   * @param Exception $e
   * @return RemoteContentRequest
   */
  protected function buildErrorResponse(\Exception $e) {
    if ($this->error == null) {
      $this->error = OAuthError::$UNKNOWN_PROBLEM;
    }
    // Take a giant leap of faith and assume that the exception message
    // will be useful to a gadget developer.  Also include the exception
    // stack trace, in case the problem report makes it to someone who knows
    // enough to do something useful with the stack.
    $errorBuf = '';
    $errorBuf .= $e->getMessage();
    $errorBuf .= "\n\n";
    $this->errorText = $errorBuf;
    return $this->buildNonDataResponse();
  }

  /**
   * @return RemoteContentRequest
   */
  protected function buildNonDataResponse() {
    $response = new RemoteContentRequest($this->realRequest->getUrl());
    $this->addResponseMetadata($response);
    self::setStrictNoCache($response);
    return $response;
  }

  /**
   * Retrieves metadata from our persistent store.
   *
   * @throws GadgetException
   */
  protected function lookupOAuthMetadata() {
    $tokenKey = $this->buildTokenKey();
    $this->accessorInfo = $this->tokenStore->getOAuthAccessor($tokenKey, $this->bypassSpecCache);
    // The persistent data store may be out of sync with reality; we trust
    // the state we stored on the client to be accurate.
    $accessor = $this->accessorInfo->getAccessor();
    if (isset($this->origClientState[self::$REQ_TOKEN_KEY])) {
      $accessor->requestToken = $this->origClientState[self::$REQ_TOKEN_KEY];
      $accessor->tokenSecret = $this->origClientState[self::$REQ_TOKEN_SECRET_KEY];
    } else if (isset($this->origClientState[self::$ACCESS_TOKEN_KEY])) {
      $accessor->accessToken = $this->origClientState[self::$ACCESS_TOKEN_KEY];
      $accessor->tokenSecret = $this->origClientState[self::$ACCESS_TOKEN_SECRET_KEY];
    } else if ($accessor->accessToken == null && $this->requestParams->getRequestToken() != null) {
      // We don't have an access token yet, but the client sent us a
      // (hopefully) preapproved request token.
      $accessor->requestToken = $this->requestParams->getRequestToken();
      $accessor->tokenSecret = $this->requestParams->getRequestTokenSecret();
    }
  }

  /**
   *
   * @return TokenKey
   */
  protected function buildTokenKey() {
    $tokenKey = new TokenKey();
    // need to URLDecode so when comparing with the ProviderKey it goes thought
    $tokenKey->setGadgetUri(urldecode($this->authToken->getAppUrl()));
    $tokenKey->setModuleId($this->authToken->getModuleId());
    $tokenKey->setAppId($this->authToken->getAppId());
    $tokenKey->setServiceName($this->requestParams->getServiceName());
    $tokenKey->setTokenName($this->requestParams->getTokenName());
    // We should always use the current viewer id as a token key. Using the owner id
    // would mean, that a private access token (with possible write access to the api)
    // could be accessable to other viewers that are visiting the gadget of another
    // owner
    $tokenKey->setUserId($this->authToken->getViewerId());
    return $tokenKey;
  }

  /**
   *
   * @param RemoteContentRequest $request
   * @return RemoteContentRequest
   */
  public function fetch($request) {
  	$this->realRequest = $request;
    try {
      $this->lookupOAuthMetadata();
    } catch (\Exception $e) {
      $this->error = OAuthError::$BAD_OAUTH_CONFIGURATION;
      return $this->buildErrorResponse($e);
    }
    $response = $this->fetchRequest($request);
    return $response;
  }

  /**
   * @param RemoteContentRequest $request
   * @return RemoteContentRequest
   */
  public function fetchRequest(RemoteContentRequest $request) {
  	$this->realRequest = $request;
    $this->checkCanApprove();
    if ($this->needApproval()) {
      // This is section 6.1 of the OAuth spec.
      $this->fetchRequestToken($request);
      // This is section 6.2 of the OAuth spec.
      $this->buildClientApprovalState();
      $this->buildAznUrl();
      // break out of the content fetching chain, we need permission from
      // the user to do this
      return $this->buildOAuthApprovalResponse();
    } elseif ($this->needAccessToken()) {
      // This is section 6.3 of the OAuth spec
      $this->exchangeRequestToken($request);
      $this->saveAccessToken();
      $this->buildClientAccessState();
    }
    return $this->fetchData();
  }

  /**
   *
   * @return RemoteContentRequest
   */
  protected function buildOAuthApprovalResponse() {
    return $this->buildNonDataResponse();
  }

  /**
   * Do we need to get the user's approval to access the data?
   *
   * @return boolean
   */
  protected function needApproval() {
    if ($this->accessorInfo == NULL) {
      return true;
    } else {
      return ($this->accessorInfo->getAccessor()->requestToken == null && $this->accessorInfo->getAccessor()->accessToken == null);
    }
  }

  /**
   * Make sure the user is authorized to approve access tokens.  At the moment
   * we restrict this to page owner's viewing their own pages.
   *
   * @throws GadgetException
   */
  protected function checkCanApprove() {
    $pageOwner = $this->authToken->getOwnerId();
    $pageViewer = $this->authToken->getViewerId();
    $stateOwner = @$this->origClientState[self::$OWNER_KEY];
    if (! $pageOwner) {
      throw new GadgetException('Unauthenticated');
    }
    if ($pageOwner != $pageViewer) {
      throw new GadgetException("Only page owners can grant OAuth approval");
    }
    if ($stateOwner != null && $stateOwner != $pageOwner) {
      throw new GadgetException("Client state belongs to a different person.");
    }
  }

  /**
   *
   * @param RemoteContentRequest $request
   * @throws GadgetException
   */
  protected function fetchRequestToken(RemoteContentRequest $request) {
    try {
      $accessor = $this->accessorInfo->getAccessor();
      //TODO The implementations of oauth differs from the one in JAVA. Fix the type OAuthMessage
      $url = $accessor->consumer->callback_url->requestTokenURL;
      $msgParams = array();
      self::addIdentityParams($msgParams, $request->getToken());
      $callbackState = new OAuthCallbackState($this->oauthCrypter);
      $callbackUrl = "http://" . getenv('HTTP_HOST') . "/gadgets/oauthcallback";
      $callbackState->setRealCallbackUrl($callbackUrl);
      $state = $callbackState->getEncryptedState();
      $msgParams[self::$OAUTH_CALLBACK] = $callbackUrl . "?state=" . urlencode($state);
      $request = $this->newRequestMessageParams($url->url, $msgParams);
      $reply = $this->sendOAuthMessage($request);
      $reply->requireParameters(array(ShindigOAuth::$OAUTH_TOKEN,
          ShindigOAuth::$OAUTH_TOKEN_SECRET));
      $accessor->requestToken = $reply->get_parameter(ShindigOAuth::$OAUTH_TOKEN);
      $accessor->tokenSecret = $reply->get_parameter(ShindigOAuth::$OAUTH_TOKEN_SECRET);
    } catch (\Exception $e) {
      // It's unfortunate the OAuth libraries throw a generic Exception.
      throw new GadgetException($e);
    }
  }

  /**
   * @param string $method
   * @param string $url
   * @param $params
   * @return ShindigOAuthRequest
   */
  protected function newRequestMessageMethod($method, $url, $params) {
    if (! isset($params)) {
      throw new \Exception("params was null in " . "newRequestMessage " . "Use newRequesMessage if you don't have a params to pass");
    }
    switch ($this->accessorInfo->getSignatureType()) {
      case ShindigOAuth::$RSA_SHA1:
        $params[ShindigOAuth::$OAUTH_SIGNATURE_METHOD] = ShindigOAuth::$RSA_SHA1;
        break;
      case "PLAINTEXT":
        $params[ShindigOAuth::$OAUTH_SIGNATURE_METHOD] = "PLAINTEXT";
        break;
      default:
        $params[ShindigOAuth::$OAUTH_SIGNATURE_METHOD] = ShindigOAuth::$HMAC_SHA1;
    }
    $accessor = $this->accessorInfo->getAccessor();
    return $accessor->newRequestMessage($method, $url, $params);
  }

  /*
   * @deprecated (All outgoing messages must send additional params
   * like XOAUTH_APP_URL, so use newRequestMessageParams instead)
   *
   * @param string $url
   * @return ShindigOAuthRequest
   */
  protected function newRequestMessageUrlOnly($url) {
    $params = array();
    return $this->newRequestMessageParams($url, $params);
  }

  /**
   * @param string $url
   * @param string $params
   * @return ShindigOAuthRequest
   */
  protected function newRequestMessageParams($url, $params) {
    $method = "POST";
    if ($this->accessorInfo->getHttpMethod() == OAuthStoreVars::$HttpMethod['GET']) {
      $method = "GET";
    }
    return $this->newRequestMessageMethod($method, $url, $params);
  }

  /**
   *
   * @param string $url
   * @param string $method
   * @param array $params
   * @return ShindigOAuthRequest
   */
  protected function newRequestMessage($url = null, $method = null, $params = null) {
    if (isset($method) && isset($url) && isset($params)) {
      return $this->newRequestMessageMethod($method, $url, $params);
    } else if (isset($url) && isset($params)) {
      return $this->newRequestMessageParams($url, $params);
    } else if (isset($url)) {
      return $this->newRequestMessageUrlOnly($url);
    }
  }

  /**
   *
   * @param array $oauthParams
   * @return string
   */
  protected function getAuthorizationHeader($oauthParams) {
    $result = "OAuth ";
    $first = true;
    foreach ($oauthParams as $key => $val) {
      if (! $first) {
        $result .= ", ";
      } else {
        $first = false;
      }
      $result .= ShindigOAuthUtil::urlencode_rfc3986($key) . "=\"" . ShindigOAuthUtil::urlencode_rfc3986($val) . '"';
    }
    return $result;
  }

  /**
   * @param array $oauthParams
   * @param string $method
   * @param string $url
   * @param array $headers
   * @param string $contentType
   * @param string $postBody
   * @param Options $options
   * @return RemoteContentRequest
   */
  protected function createRemoteContentRequest($oauthParams, $method, $url, $headers, $contentType, $postBody, $options) {
    $paramLocation = $this->accessorInfo->getParamLocation();
    $newHeaders = array();
    // paramLocation could be overriden by a run-time parameter to fetchRequest
    switch ($paramLocation) {
      case OAuthStoreVars::$OAuthParamLocation['AUTH_HEADER']:
        if ($headers != null) {
          $newHeaders = $headers;
        }
        $authHeader = array();
        $authHeader = $this->getAuthorizationHeader($oauthParams);
        $newHeaders["Authorization"] = $authHeader;
        break;

      case OAuthStoreVars::$OAuthParamLocation['POST_BODY']:
        if (! ShindigOAuthUtil::isFormEncoded($contentType)) {
          throw new GadgetException("Invalid param: OAuth param location can only " . "be post_body if post body if of type x-www-form-urlencoded");
        }
        if (! isset($postBody) || count($postBody) == 0) {
          $postBody = ShindigOAuthUtil::getPostBodyString($oauthParams);
        } else {
          $postBody = $postBody . "&" . ShindigOAuthUtil::getPostBodyString($oauthParams);
        }
        break;

      case OAuthStoreVars::$OAuthParamLocation['URI_QUERY']:
        $url = ShindigOAuthUtil::addParameters($url, $oauthParams);
        break;
    }
    $rcr = new RemoteContentRequest($url);
    $rcr->createRemoteContentRequest($method, $url, $newHeaders, null, $options);
    $rcr->setPostBody($postBody);
    return $rcr;
  }

  /**
   * Sends OAuth request token and access token messages.
   *
   * @param ShindigOAuthRequest $request
   * @return ShindigOAuthRequest
   */
  protected function sendOAuthMessage(ShindigOAuthRequest $request) {
    $rcr = $this->createRemoteContentRequest($this->filterOAuthParams($request), $request->get_normalized_http_method(), $request->get_url(), null, RemoteContentRequest::$DEFAULT_CONTENT_TYPE, null, RemoteContentRequest::getDefaultOptions());
    $rcr->setToken($this->authToken);

    $remoteFetcherClass = Config::get('remote_content_fetcher');
    $fetcher = new $remoteFetcherClass();
    $content = $fetcher->fetchRequest($rcr);
    $reply = ShindigOAuthRequest::from_request();
    $params = ShindigOAuthUtil::decodeForm($content->getResponseContent());
    $reply->set_parameters($params);
    return $reply;
  }

  /**
   * Builds the data we'll cache on the client while we wait for approval.
   *
   * @throws GadgetException
   */
  protected function buildClientApprovalState() {
    try {
      $accessor = $this->accessorInfo->getAccessor();
      $oauthState = array();
      $oauthState[self::$REQ_TOKEN_KEY] = $accessor->requestToken;
      $oauthState[self::$REQ_TOKEN_SECRET_KEY] = $accessor->tokenSecret;
      $oauthState[self::$OWNER_KEY] = $this->authToken->getOwnerId();
      $this->newClientState = $this->oauthCrypter->wrap($oauthState);
    } catch (BlobCrypterException $e) {
      throw new GadgetException("INTERNAL SERVER ERROR: " . $e);
    }
  }

  /**
   * Builds the URL the client needs to visit to approve access.
   */
  protected function buildAznUrl() {
    // At some point we can be clever and use a callback URL to improve
    // the user experience, but that's too complex for now.
    $accessor = $this->accessorInfo->getAccessor();
    $azn = $accessor->consumer->callback_url->userAuthorizationURL;
    $authUrl = $azn->url;
    if (strstr($authUrl, "?") == FALSE) {
      $authUrl .= "?";
    } else {
      $authUrl .= "&";
    }
    $authUrl .= ShindigOAuth::$OAUTH_TOKEN;
    $authUrl .= "=";
    $authUrl .= ShindigOAuthUtil::urlencode_rfc3986($accessor->requestToken);
    $this->aznUrl = $authUrl;
  }

  /**
   * Do we need to exchange a request token for an access token?
   *
   * @return boolean
   */
  protected function needAccessToken() {
    return ($this->accessorInfo->getAccessor()->requestToken != null && $this->accessorInfo->getAccessor()->accessToken == null);
  }

  /**
   * implements \section 6.3 of the OAuth spec.
   *
   * @param RemoteContentRequest $request
   * @throws GadgetException
   */
  protected function exchangeRequestToken(RemoteContentRequest $request) {
    try {
      $accessor = $this->accessorInfo->getAccessor();
      $url = $accessor->consumer->callback_url->accessTokenURL;
      $msgParams = array();
      $msgParams[ShindigOAuth::$OAUTH_TOKEN] = $accessor->requestToken;
      self::addIdentityParams($msgParams, $request->getToken());
      $callbackUrl = $this->requestParams->getReceivedCallback();
      if (strlen($callbackUrl) > 0) {
        $parsed_url = parse_url($callbackUrl);
        parse_str($parsed_url["query"], $url_params);
        if (strlen($url_params["oauth_token"]) > 0 &&
            strlen($url_params["oauth_verifier"]) > 0 &&
            $url_params["oauth_token"] == $accessor->requestToken) {
          $msgParams[ShindigOAuth::$OAUTH_VERIFIER] = $url_params["oauth_verifier"];
        } else {
          throw new GadgetException("Invalid received callback URL: ".$callbackUrl);
        }
      }
      $request = $this->newRequestMessageParams($url->url, $msgParams);
      $reply = $this->sendOAuthMessage($request);
      $reply->requireParameters(array(ShindigOAuth::$OAUTH_TOKEN,
          ShindigOAuth::$OAUTH_TOKEN_SECRET));
      $accessor->accessToken = $reply->get_parameter(ShindigOAuth::$OAUTH_TOKEN);
      $accessor->tokenSecret = $reply->get_parameter(ShindigOAuth::$OAUTH_TOKEN_SECRET);
    } catch (\Exception $e) {
      // It's unfortunate the OAuth libraries throw a generic Exception.
      throw new GadgetException("INTERNAL SERVER ERROR: " . $e);
    }
  }

  /**
   * Save off our new token and secret to the persistent store.
   *
   * @throws GadgetException
   */
  protected function saveAccessToken() {
    $accessor = $this->accessorInfo->getAccessor();
    $tokenKey = $this->buildTokenKey();
    $tokenInfo = new TokenInfo($accessor->accessToken, $accessor->tokenSecret);
    $this->tokenStore->storeTokenKeyAndSecret($tokenKey, $tokenInfo);
  }

  /**
   * Builds the data we'll cache on the client while we make requests.
   *
   * @throws GadgetException
   */
  protected function buildClientAccessState() {
    try {
      $oauthState = array();
      $accessor = $this->accessorInfo->getAccessor();
      $oauthState[self::$ACCESS_TOKEN_KEY] = $accessor->accessToken;
      $oauthState[self::$ACCESS_TOKEN_SECRET_KEY] = $accessor->tokenSecret;
      $oauthState[self::$OWNER_KEY] = $this->authToken->getOwnerId();
      $this->newClientState = $this->oauthCrypter->wrap($oauthState);
    } catch (BlobCrypterException $e) {
      throw new GadgetException("INTERNAL SERVER ERROR: " . $e);
    }
  }

  /**
   * Get honest-to-goodness user data.
   *
   * @return RemoteContentRequest
   */
  protected function fetchData() {
    try {
      // TODO: it'd be better using $this->realRequest->getContentType(), but not set before hand. Temporary hack.
      $postBody = $this->realRequest->getPostBody();
      $url = $this->realRequest->getUrl();
      $msgParams = array();
      if (ShindigOAuthUtil::isFormEncoded($this->realRequest->getHeader("Content-Type")) && strlen($postBody) > 0) {
        $entries = explode('&', $postBody);
        foreach ($entries as $entry) {
          $parts = explode('=', $entry);
          if (count($parts) == 2) {
            $msgParams[ShindigOAuthUtil::urldecode_rfc3986($parts[0])] = ShindigOAuthUtil::urldecode_rfc3986($parts[1]);
          }
        }
      }
      $method = $this->realRequest->getMethod();
      $msgParams[self::$XOAUTH_APP_URL] = $this->authToken->getAppUrl();
      // Build and sign the message.
      $oauthRequest = $this->newRequestMessageMethod($method, $url, $msgParams);
      $oauthParams = $this->filterOAuthParams($oauthRequest);
      $newHeaders = array();
      switch ($method) {
        case 'POST' :
          if (empty($postBody) || count($postBody) == 0) {
            $postBody = ShindigOAuthUtil::getPostBodyString($oauthParams);
          } else {
            $postBody = $postBody . "&" . ShindigOAuthUtil::getPostBodyString($oauthParams);
          }
          // To avoid 417 Response from server, adding empty "Expect" header
          $newHeaders['Expect'] = '';
          break;
        case 'GET' :
          $url = ShindigOAuthUtil::addParameters($url, $oauthParams);
          break;
      }
      // To choose HTTP method client requested, we don't use $this->createRemoteContentRequest() here.
      $rcr = new RemoteContentRequest($url);
      $rcr->createRemoteContentRequest($method, $url, $newHeaders, null, $this->realRequest->getOptions());
      $rcr->setPostBody($postBody);
      $remoteFetcherClass = Config::get('remote_content_fetcher');
      $fetcher = new $remoteFetcherClass();
      $content = $fetcher->fetchRequest($rcr);
      $statusCode = $content->getHttpCode();
      //TODO is there a better way to detect an SP error? For example: http://wiki.oauth.net/ProblemReporting
      if ($statusCode == 401) {
        $tokenKey = $this->buildTokenKey();
        $this->tokenStore->removeTokenAndSecret($tokenKey);
      } else if ($statusCode >= 400 && $statusCode < 500) {
        $message = $this->parseAuthHeader(null, $content);
        if ($message->get_parameter(ShindigOAuth::$OAUTH_PROBLEM) != null) {
          throw new ShindigOAuthProtocolException($message);
        }
      }
      // Track metadata on the response
      $this->addResponseMetadata($content);
      return $content;
    } catch (\Exception $e) {
      throw new GadgetException("INTERNAL SERVER ERROR: " . $e);
    }
  }

  /**
   * Parse OAuth WWW-Authenticate header and either add them to an existing
   * message or create a new message.
   *
   * @param ShindigOAuthRequest $msg
   * @param RemoteContentRequest $resp
   * @return string the updated message.
   */
  protected function parseAuthHeader(ShindigOAuthRequest $msg = null, RemoteContentRequest $resp) {
    if ($msg == null) {
      $msg = ShindigOAuthRequest::from_request();
    }
    $authHeaders = $resp->getResponseHeader("WWW-Authenticate");
    if ($authHeaders != null) {
      $msg->set_parameters(ShindigOAuthUtil::decodeAuthorization($authHeaders));
    }
    return $msg;
  }

  /**
   * Extracts only those parameters from an OAuthMessage that are OAuth-related.
   * An OAuthMessage may hold a whole bunch of non-OAuth-related parameters
   * because they were all needed for signing. But when constructing a request
   * we need to be able to extract just the OAuth-related parameters because
   * they, and only they, may have to be put into an Authorization: header or
   * some such thing.
   *
   * @param string $message the OAuthMessage object, which holds non-OAuth parameters
   * such as foo=bar (which may have been in the original URI query part, or
   * perhaps in the POST body), as well as OAuth-related parameters (such as
   * oauth_timestamp or oauth_signature).
   *
   * @return array a list that contains only the oauth_related parameters.
   *
   * @throws IOException
   */
  protected function filterOAuthParams($message) {
    $result = array();
    foreach ($message->get_parameters() as $key => $value) {
      if (preg_match('/^(oauth|xoauth|opensocial)/', strtolower($key))) {
        $result[$key] = $value;
      }
    }
    return $result;
  }

  /**
   *
   * @return array
   */
  public function getResponseMetadata() {
    return $this->responseMetadata;
  }

  /**
   * @param RemoteContentRequest $response
   */
  public function addResponseMetadata(RemoteContentRequest $response) {
    $response->setHttpCode(200);
    if ($this->newClientState != null) {
      $this->responseMetadata[self::$CLIENT_STATE] = $this->newClientState;
      $response->setMetadata(self::$CLIENT_STATE, $this->newClientState);
    }
    if ($this->aznUrl != null) {
      $this->responseMetadata[self::$APPROVAL_URL] = $this->aznUrl;
      $response->setMetadata(self::$APPROVAL_URL, $this->aznUrl);
    }
    if ($this->error != null) {
      $this->responseMetadata[self::$ERROR_CODE] = $this->error;
      $response->setMetadata(self::$ERROR_CODE, $this->error);
    }
    if ($this->errorText != null) {
      $this->responseMetadata[self::$ERROR_TEXT] = $this->errorText;
      $response->setMetadata(self::$ERROR_TEXT, $this->errorText);
    }
  }

  /**
   * @param array $requests
   */
  public function multiFetchRequest(Array $requests) {// Do nothing
  }

  /**
   * @param array $params
   * @param SecurityToken $token
   */
  protected static function addIdentityParams(array & $params, SecurityToken $token) {
    $params['opensocial_owner_id'] = $token->getOwnerId();
    $params['opensocial_viewer_id'] = $token->getViewerId();
    $params['opensocial_app_id'] = $token->getAppId();
    $params['opensocial_app_url'] = $token->getAppUrl();
  }

  /**
   *
   * @param RemoteContentRequest $response
   */
  protected static function setStrictNoCache(RemoteContentRequest $response) {
    $response->setResponseHeader('Pragma', 'no-cache');
    $response->setResponseHeader('Cache-Control', 'no-cache');
  }
}
