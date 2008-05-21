<?php
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Implements signed fetch based on the OAuth request signing algorithm.
 *
 * Subclasses can override signMessage to use their own crypto if they don't
 * like the oauth.net code for some reason.
 *
 * Instances of this class are only accessed by a single thread at a time,
 * but instances may be created by multiple threads.
 */
class SigningFetcher extends RemoteContentFetcher {

	protected static $OPENSOCIAL_OWNERID = "opensocial_owner_id";
	protected static $OPENSOCIAL_VIEWERID = "opensocial_viewer_id";
	protected static $OPENSOCIAL_APPID = "opensocial_app_id";
	protected static $XOAUTH_PUBLIC_KEY = "xoauth_signature_publickey";
	protected static $ALLOWED_PARAM_NAME = "[-:\\w]+";
	
	//protected final TimeSource clock = new TimeSource();
	
	/**
	 * Authentication token for the user and gadget making the request.
	 */
	protected $authToken;
	
	/**
	 * Private key we pass to the OAuth RSA_SHA1 algorithm.This can be a
	 * PrivateKey object, or a PEM formatted private key, or a DER encoded byte
	 * array for the private key.(No, really, they accept any of them.)
	 */
	protected $privateKeyObject;
	
	/**
	 * The name of the key, included in the fetch to help with key rotation.
	 */
	protected $keyName;

	/**
	 * Constructor based on signing with the given PrivateKey object.
	 *
	 * @param authToken verified gadget security token
	 * @param keyName name of the key to include in the request
	 * @param privateKey the key to use for the signing
	 */
	public static function makeFromPrivateKey($next, $authToken, $keyName, $privateKey)
	{
		return new SigningFetcher($next, $authToken, $keyName, $privateKey);
	}

	/**
	 * Constructor based on signing with the given PrivateKey object.
	 *
	 * @param authToken verified gadget security token
	 * @param keyName name of the key to include in the request
	 * @param privateKey base64 encoded private key
	 */
	public static function makeFromB64PrivateKey($next, $authToken, $keyName, $privateKey)
	{
		return new SigningFetcher($next, $authToken, $keyName, $privateKey);
	}

	/**
	 * Constructor based on signing with the given PrivateKey object.
	 *
	 * @param authToken verified gadget security token
	 * @param keyName name of the key to include in the request
	 * @param privateKey DER encoded private key
	 */
	public static function makeFromPrivateKeyBytes($next, $authToken, $keyName, $privateKey)
	{
		return new SigningFetcher($next, $authToken, $keyName, $privateKey);
	}

	protected function __construct($next, $authToken, $keyName, $privateKeyObject)
	{
		parent::setNextFetcher($next);
		$this->authToken = $authToken;
		$this->keyName = $keyName;
		$this->privateKeyObject = $privateKeyObject;
	}

	public function fetchRequest($request)
	{
		return $this->getNextFetcher()->fetchRequest($request);
	}

	public function fetch($url, $method)
	{
		$signed = $this->signRequest($url, $method);
		return $this->getNextFetcher()->fetchRequest($signed);
	}

	private function signRequest($url, $method)
	{
		try {
			// Parse the request into parameters for OAuth signing, stripping out
			// any OAuth or OpenSocial parameters injected by the client
			///////////////////////////////////////////////
			require 'src/common/Zend/Uri.php';
			$uri = Zend_Uri::factory($url);
			$resource = $uri->getUri();
			$queryParams = $this->sanitize($_GET);
			$postParams = $this->sanitize($_POST);
			$msgParams = array();
			$msgParams = array_merge($msgParams, $queryParams);
			$msgParams = array_merge($msgParams, $postParams);
			
			// TODO: is this ok?
			//$msgParams = array();
			$this->addOpenSocialParams($msgParams);		
			$this->addOAuthParams($msgParams);
			
			// Build and sign the OAuthMessage; note that the resource here has
			// no query string, the parameters are all in msgParams
			//$message  = new OAuthMessage($method, $resource, $msgParams);
	
			////////////////////////////////////////////////    
			$consumer = new OAuthConsumer(NULL, NULL, NULL);
			$consumer->setProperty(OAuthSignatureMethod_RSA_SHA1::$PRIVATE_KEY, $this->privateKeyObject);
			$signatureMethod = new OAuthSignatureMethod_RSA_SHA1();
			
			$req_req = OAuthRequest::from_consumer_and_token($consumer, NULL, $method, $resource, $msgParams);
			$req_req->sign_request($signatureMethod, $consumer, NULL);
			
			// Rebuild the query string, including all of the parameters we added.
			// We have to be careful not to copy POST parameters into the query.
			// If post and query parameters share a name, they end up being removed
			// from the query.
			$forPost = array();
			foreach ($postParams as $key => $param) {
				$forPost[$key] = $param;
			}
			$newQuery = array();
			foreach ($req_req->get_parameters() as $key => $param) {
				if (! isset($forPost[$key])) {
					$newQuery[$key] = $param;
				}
			}
			
			// Careful here; the OAuth form encoding scheme is slightly different than
			// the normal form encoding scheme, so we have to use the OAuth library
			// formEncode method.
			$uri->setQuery($newQuery);
			return new RemoteContentRequest($uri->getUri());
		} catch (Exception $e) {
			throw new GadgetException($e);
		}
	}

	private function addOpenSocialParams(&$msgParams)
	{
		$owner = $this->authToken->getOwnerId();
		if ($owner != null) {
			$msgParams[SigningFetcher::$OPENSOCIAL_OWNERID] = $owner;
		}
		$viewer = $this->authToken->getViewerId();
		if ($viewer != null) {
			$msgParams[SigningFetcher::$OPENSOCIAL_VIEWERID] = $viewer;
		}
		$app = $this->authToken->getAppId();
		if ($app != null) {
			$msgParams[SigningFetcher::$OPENSOCIAL_APPID] = $app;
		}
	}

	private function addOAuthParams(&$msgParams)
	{
		$msgParams[OAuth::$OAUTH_TOKEN] = '';
		$domain = $this->authToken->getDomain();
		if ($domain != null) {
			$msgParams[OAuth::$OAUTH_CONSUMER_KEY] = 'partuza.chabotc.com'; //$domain;
		}
		if ($this->keyName != null) {
			$msgParams[SigningFetcher::$XOAUTH_PUBLIC_KEY] = $this->keyName;
		}
		$nonce = OAuthRequest::generate_nonce();
		$msgParams[OAuth::$OAUTH_NONCE] = $nonce;
		$timestamp = time();
		$msgParams[OAuth::$OAUTH_TIMESTAMP] = $timestamp;
		$msgParams[OAuth::$OAUTH_SIGNATURE_METHOD] = OAuth::$RSA_SHA1;
	}

	/**
	 * Strip out any owner or viewer id passed by the client.
	 */
	private function sanitize($params)
	{
		$list = array();
		foreach ($params as $key => $p) {
			if ($this->allowParam($key)) {
				$list[$key] = $p;
			}
		}
		return $list;
	}

	private function allowParam($paramName)
	{
		$canonParamName = strtolower($paramName);
		return (! (substr($canonParamName, 0, 5) == "oauth" || substr($canonParamName, 0, 6) == "xoauth" || substr($canonParamName, 0, 9) == "opensocial")) && ereg(SigningFetcher::$ALLOWED_PARAM_NAME, $canonParamName);
	}
}
