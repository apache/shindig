<?php
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

class OAuthStoreException extends GadgetException {}

/**
 * Higher-level interface that allows callers to store and retrieve
 * OAuth-related data directly from {@code GadgetSpec}s, {@code GadgetContext}s,
 * etc. See {@link OAuthStore} for a more detailed explanation of the OAuth
 * Data Store.
 */
class GadgetOAuthTokenStore {
	
	/**
	 * Internal class used to communicate results of parsing the gadget spec
	 * between methods.
	 */
	// name of the OAuth feature in the gadget spec
	public static $OAUTH_FEATURE = "oauth";
	// name of the Param that identifies the service name
	public static $SERVICE_NAME = "service_name";
	// name of the Param that identifies the access URL
	public static $ACCESS_URL = "access_url";
	// name of the optional Param that identifies the HTTP method for access URL
	public static $ACCESS_HTTP_METHOD = "access_method";
	// name of the Param that identifies the request URL
	public static $REQUEST_URL = "request_url";
	// name of the optional Param that identifies the HTTP method for request URL
	public static $REQUEST_HTTP_METHOD = "request_method";
	// name of the Param that identifies the user authorization URL
	public static $AUTHORIZE_URL = "authorize_url";
	// name of the Param that identifies the location of OAuth parameters
	public static $OAUTH_PARAM_LOCATION = "param_location";
	public static $AUTH_HEADER = "auth_header";
	public static $POST_BODY   = "post_body";
	public static $URI_QUERY = "uri_query";
	
	//public static $DEFAULT_OAUTH_PARAM_LOCATION = AUTH_HEADER;
	public static $DEFAULT_OAUTH_PARAM_LOCATION = "auth_header"; //It has to be like the line above this.
	//TODO: Check why java use AUTH_HEADER
	
	// we use POST if no HTTP method is specified for access and request URLs
	// (user authorization always uses GET)
	public static $DEFAULT_HTTP_METHOD = "POST";
	private $store;

	/**
	 * Public constructor.
	 *
	 * @param store an {@link OAuthStore} that can store and retrieve OAuth
	 *              tokens, as well as information about service providers.
	 */
	public function __construct($store)
	{
		$this->store = $store;
	}

	/**
	 * Parses a gadget spec and stores the service provider information found
	 * in the spec into the OAuth store. The spec passed in <b>must</b> require
	 * "oauth" as a feature. It is an error to pass in a spec that does not
	 * require oauth.
	 *
	 * @param gadgetUrl the URL of the gadget
	 * @param spec the parsed GadgetSpec of the gadget.
	 */
	
	public function storeServiceInfoFromGadgetSpec($gadgetUrl, Gadget $specGadget)
	{
		$gadgetInfo = $this->getGadgetOAuthInfo($specGadget);
		$providerKey = new ProviderKey();
		$providerKey->setGadgetUri($gadgetUrl);
		$providerKey->setServiceName($gadgetInfo->getServiceName());
		$this->store->setOAuthServiceProviderInfo($providerKey, $gadgetInfo->getProviderInfo());
	}

	/**
	 * Stores a negotiated consumer key and secret in the gadget store.
	 * The "secret" can either be a consumer secret in the strict OAuth sense,
	 * or it can be a PKCS8-then-Base64 encoded private key that we'll be using
	 * with this service provider.
	 *
	 * @param gadgetUrl the URL of the gadget
	 * @param serviceName the service provider with whom we have negotiated a
	 *                    consumer key and secret.
	 */
	public function storeConsumerKeyAndSecret($gadgetUrl, $serviceName, $keyAndSecret)
	{
		$providerKey = new ProviderKey();
		$providerKey->setGadgetUri($gadgetUrl);
		$providerKey->setServiceName($serviceName);
		$this->store->setOAuthConsumerKeyAndSecret($providerKey, $keyAndSecret);
	}

	/**
	 * Stores an access token in the OAuth Data Store.
	 * @param tokenKey information about the Gadget storing the token.
	 * @param tokenInfo the TokenInfo to be stored in the OAuth data store.
	 */
	public function storeTokenKeyAndSecret($tokenKey, $tokenInfo)
	{
		$getGadgetUri = $tokenKey->getGadgetUri();
		if (empty($getGadgetUri)) {
			throw new Exception("found empty gadget URI in TokenKey");
		}
		$getServiceName = $tokenKey->getServiceName();
		if (empty($getServiceName)) {
			throw new Exception("found empty service name in TokenKey");
		}
		$getUserId = $tokenKey->getUserId();
		if (empty($getUserId)) {
			throw new Exception("found empty userId in TokenKey");
		}
		$this->store->setTokenAndSecret($tokenKey, $tokenInfo);
	}

	/**
	 * Retrieve an OAuthAccessor that is ready to sign OAuthMessages.
	 *
	 * @param tokenKey information about the gadget retrieving the accessor.
	 *
	 * @return an OAuthAccessorInfo containing an OAuthAccessor (whic can be
	 *         passed to an OAuthMessage.sign method), as well as httpMethod and
	 *         signatureType fields.
	 */
	public function getOAuthAccessor(TokenKey $tokenKey)
	{
		$getGadgetUri = $tokenKey->getGadgetUri();
		if (empty($getGadgetUri)) {
			throw new OAuthStoreException("found empty gadget URI in TokenKey");
		}
		$getServiceName = $tokenKey->getServiceName();
		if (empty($getServiceName)) {
			throw new OAuthStoreException("found empty service name in TokenKey");
		}
		$getUserId = $tokenKey->getUserId();
		if (empty($getUserId)) {
			throw new OAuthStoreException("found empty userId in TokenKey");
		}
		return $this->store->getOAuthAccessorTokenKey($tokenKey);
	}

	/**
	 * Reads OAuth provider information out of gadget spec.
	 * @param spec
	 * @return a GadgetInfo
	 * @throws GadgetException if some information is missing, or something else
	 *                         is wrong with the spec.
	 */
	public function getGadgetOAuthInfo(Gadget $specGadget)
	{
		$requires = $specGadget->getRequires();
		$oauthFeature = $requires[GadgetOAuthTokenStore::$OAUTH_FEATURE];
		if ($oauthFeature == null) {
			$message = "gadget spec is missing oauth feature section";
			throw new GadgetException($message);
		}
		$oauthParams = $oauthFeature->getParams();
		$serviceName = $this->getOAuthParameter($oauthParams, GadgetOAuthTokenStore::$SERVICE_NAME, false);
		$requestUrl = $this->getOAuthParameter($oauthParams, GadgetOAuthTokenStore::$REQUEST_URL, false);
		$requestMethod = $this->getOAuthParameter($oauthParams, GadgetOAuthTokenStore::$REQUEST_HTTP_METHOD, true);
		if (! isset($requestMethod)) {
			$requestMethod = GadgetOAuthTokenStore::$DEFAULT_HTTP_METHOD;
		}
		$accessUrl = $this->getOAuthParameter($oauthParams, GadgetOAuthTokenStore::$ACCESS_URL, false);
		$accessMethod = $this->getOAuthParameter($oauthParams, GadgetOAuthTokenStore::$ACCESS_HTTP_METHOD, true);
		if (! isset($accessMethod)) {
			$accessMethod = GadgetOAuthTokenStore::$DEFAULT_HTTP_METHOD;
		}
		if (! strtoupper($accessMethod) == strtoupper($requestMethod)) {
			$message = "HTTP methods of access and request URLs have to match. " . "access method was: " . $accessMethod . ". request method was: " . $requestMethod;
			throw new GadgetException($message);
		}
		$authorizeUrl = $this->getOAuthParameter($oauthParams, GadgetOAuthTokenStore::$AUTHORIZE_URL, false);
		$provider = new OAuthServiceProvider($requestUrl, $authorizeUrl, $accessUrl);
		$httpMethod = '';
		if (strtoupper($accessMethod) == "GET") {
			$httpMethod = "GET";
		} else if (strtoupper($accessMethod) == "POST") {
			$httpMethod = "POST";
		} else {
			$message = "unknown http method in gadget spec: " . $accessMethod;
			throw new GadgetException($message);
		}
		$paramLocationStr = $this->getOAuthParameter($oauthParams, GadgetOAuthTokenStore::$OAUTH_PARAM_LOCATION, true);
		if (! isset($paramLocationStr)) {
			$paramLocationStr = GadgetOAuthTokenStore::$DEFAULT_OAUTH_PARAM_LOCATION;
		}
		$paramLocation = '';
		if (strtoupper($paramLocationStr) == strtoupper(GadgetOAuthTokenStore::$POST_BODY)) {
			$paramLocation = GadgetOAuthTokenStore::$POST_BODY;
		} else if (strtoupper($paramLocationStr) == strtoupper(GadgetOAuthTokenStore::$AUTH_HEADER)) {
			$paramLocation = GadgetOAuthTokenStore::$AUTH_HEADER;
		} else if (strtoupper($paramLocationStr) == strtoupper(GadgetOAuthTokenStore::$URI_QUERY)) {
			$paramLocation = GadgetOAuthTokenStore::$URI_QUERY;
		} else {
			$message = "unknown OAuth param location in gadget spec: " . $paramLocationStr;
			throw new GadgetException($message);
		}
		if ($httpMethod == "GET" && $paramLocation == GadgetOAuthTokenStore::$POST_BODY) {
			$message = "found incompatible param_location requirement of POST_BODY and http method GET.";
			throw new GadgetException($message);
		}
		$provInfo = new ProviderInfo();
		$provInfo->setHttpMethod($httpMethod);
		$provInfo->setParamLocation($paramLocation);
		
		// TODO: for now, we'll just set the signature type to HMAC_SHA1
		// as this will be ignored later on when retrieving consumer information.
		// There, if we find a negotiated HMAC key, we will use HMAC_SHA1. If we
		// find a negotiated RSA key, we will use RSA_SHA1. And if we find neither,
		// we may use RSA_SHA1 with a default signing key.
		$provInfo->setSignatureType('HMAC_SHA1');
		$provInfo->setProvider($provider);
		$gadgetInfo = new GadgetInfo();
		$gadgetInfo->setProviderInfo($provInfo);
		$gadgetInfo->setServiceName($serviceName);
		return $gadgetInfo;
	}

	/**
	 * Extracts a single oauth-related parameter from a key-value map,
	 * throwing an exception if the parameter could not be found (unless the
	 * parameter is optional, in which case null is returned).
	 *
	 * @param params the key-value map from which to pull the value (parameter)
	 * @param paramName the name of the parameter (key).
	 * @param isOptional if it's optional, don't throw an exception if it's not
	 *                   found.
	 * @return the value corresponding to the key (paramName)
	 * @throws GadgetException if the parameter value couldn't be found.
	 */
	static function getOAuthParameter($params, $paramName, $isOptional)
	{
		$param = @$params[$paramName];
		if ($param == null && !$isOptional) {
			$message = "parameter '" . $paramName . "' missing in oauth feature section of gadget spec";
			throw new GadgetException($message);
		}
		return ($param == null) ? null : trim($param);
	}
}

class GadgetInfo {
	private $serviceName;
	private $providerInfo;

	public function getServiceName()
	{
		return $this->serviceName;
	}
	
	public function setServiceName($serviceName)
	{
		$this->serviceName = $serviceName;
	}
	
	public function getProviderInfo()
	{
		return $this->providerInfo;
	}

	 public function setProviderInfo($providerInfo)
	 {
		$this->providerInfo = $providerInfo;
	}
}
