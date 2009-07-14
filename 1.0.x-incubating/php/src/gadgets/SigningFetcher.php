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
  protected static $ALLOWED_PARAM_NAME = '^[-_[:alnum:]]+$';

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
  public static function makeFromPrivateKey($next, $authToken, $keyName, $privateKey) {
    return new SigningFetcher($next, $authToken, $keyName, $privateKey);
  }

  /**
   * Constructor based on signing with the given PrivateKey object.
   *
   * @param authToken verified gadget security token
   * @param keyName name of the key to include in the request
   * @param privateKey base64 encoded private key
   */
  public static function makeFromB64PrivateKey($next, $authToken, $keyName, $privateKey) {
    return new SigningFetcher($next, $authToken, $keyName, $privateKey);
  }

  /**
   * Constructor based on signing with the given PrivateKey object.
   *
   * @param authToken verified gadget security token
   * @param keyName name of the key to include in the request
   * @param privateKey DER encoded private key
   */
  public static function makeFromPrivateKeyBytes($next, $authToken, $keyName, $privateKey) {
    return new SigningFetcher($next, $authToken, $keyName, $privateKey);
  }

  protected function __construct($next, $authToken, $keyName, $privateKeyObject) {
    parent::setNextFetcher($next);
    $this->authToken = $authToken;
    $this->keyName = $keyName;
    $this->privateKeyObject = $privateKeyObject;
  }

  public function fetchRequest($request) {
    return $this->getNextFetcher()->fetchRequest($request);
  }

  public function fetch($url, $method) {
    $signed = $this->signRequest($url, $method);
    return $this->getNextFetcher()->fetchRequest($signed);
  }

  public function multiFetchRequest(Array $requests) {
    return $this->getNextFetcher()->multiFetchRequest($requests);
  }

  public function signRequest($url, $method) {
    try {
      // Parse the request into parameters for OAuth signing, stripping out
      // any OAuth or OpenSocial parameters injected by the client
      $parsedUri = parse_url($url);
      $resource = $url;
      $queryParams = $this->sanitize($_GET);
      $postParams = $this->sanitize($_POST);
      // The data that is supposed to be posted to the target page is contained in the postData field
      // in the $_POST to the Shindig proxy server
      // Here we parse it and put it into the $postDataParams array which then is merged into the postParams
      // to be used for the GET/POST request and the building of the signature
      $postDataParams = array();
      if (isset($_POST['postData']) && count($postDataParts = split('&', urldecode($_POST['postData']))) > 0) {
        foreach ($postDataParts as $postDataPart) {
          $position = strpos($postDataPart, '=');
          $key = substr($postDataPart, 0, $position);
          $value = substr($postDataPart, $position + 1);
          $postDataParams[$key] = $value;
        }
      }
      $postParams = array_merge($postParams, $this->sanitize($postDataParams));
      $msgParams = array();
      $msgParams = array_merge($msgParams, $queryParams);
      $msgParams = array_merge($msgParams, $postParams);
      $this->addOpenSocialParams($msgParams);
      $this->addOAuthParams($msgParams);
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
      $postData = false;
      if ($method == 'POST') {
        foreach ($postParams as $key => $param) {
          $forPost[$key] = $param;
          if ($postData === false) {
            $postData = array();
          }
          $postData[] = OAuthUtil::urlencodeRFC3986($key) . "=" . OAuthUtil::urlencodeRFC3986($param);
        }
        if ($postData !== false) {
          $postData = implode("&", $postData);
        }
      }
      $newQuery = '';
      foreach ($req_req->get_parameters() as $key => $param) {
        if (! isset($forPost[$key])) {
          $newQuery .= urlencode($key) . '=' . urlencode($param) . '&';
        }
      }
      // and stick on the original query params too
      if (isset($parsedUri['query']) && ! empty($parsedUri['query'])) {
        $oldQuery = array();
        parse_str($parsedUri['query'], $oldQuery);
        foreach ($oldQuery as $key => $val) {
          $newQuery .= urlencode($key) . '=' . urlencode($val) . '&';
        }
      }
      // Careful here; the OAuth form encoding scheme is slightly different than
      // the normal form encoding scheme, so we have to use the OAuth library
      // formEncode method.
      $url = $parsedUri['scheme'] . '://' . $parsedUri['host'] . (isset($parsedUri['port']) ? ':' . $parsedUri['port'] : '') . $parsedUri['path'] . '?' . $newQuery;
      // The headers are transmitted in the POST-data array in the field 'headers'
      // if no post should be made, the value should be false for this parameter
      $postHeaders = ((isset($_POST['headers']) && $method == 'POST') ? urldecode(str_replace("&", "\n", str_replace("=", ": ", $_POST['headers']))) : false);
      return new RemoteContentRequest($url, $postHeaders, $postData);
    } catch (Exception $e) {
      throw new GadgetException($e);
    }
  }

  private function addOpenSocialParams(&$msgParams) {
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

  private function addOAuthParams(&$msgParams) {
    $msgParams[OAuth::$OAUTH_TOKEN] = '';
    $domain = $this->authToken->getDomain();
    if ($domain != null) {
      $msgParams[OAuth::$OAUTH_CONSUMER_KEY] = $domain;
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
  private function sanitize($params) {
    $list = array();
    foreach ($params as $key => $p) {
      if ($this->allowParam($key)) {
        $list[$key] = $p;
      }
    }
    return $list;
  }

  private function allowParam($paramName) {
    $canonParamName = strtolower($paramName);
    // Exclude the fields which are only used to tell the proxy what to do
    // and the fields which should be added by signing the request later on
    if ($canonParamName == "output" || $canonParamName == "httpmethod" || $canonParamName == "authz" || $canonParamName == "st" || $canonParamName == "headers" || $canonParamName == "url" || $canonParamName == "contenttype" || $canonParamName == "postdata" || $canonParamName == "numentries" || $canonParamName == "getsummaries" || $canonParamName == "signowner" || $canonParamName == "signviewer" || $canonParamName == "gadget" || $canonParamName == "bypassspeccache" || substr($canonParamName, 0, 5) == "oauth" || substr($canonParamName, 0, 6) == "xoauth" || substr($canonParamName, 0, 9) == "opensocial") {
      return false;
    }
    // make a last sanity check on the key of the data by using a regular expression
    return ereg(SigningFetcher::$ALLOWED_PARAM_NAME, $canonParamName);
  }
}
