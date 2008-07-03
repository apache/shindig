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
require 'src/common/HttpServlet.php';
require 'src/common/SecurityTokenDecoder.php';
require 'src/common/SecurityToken.php';
require 'src/common/BlobCrypter.php';
require 'src/common/Crypto.php';
require 'src/socialdata/opensocial/model/Activity.php';
require 'src/socialdata/opensocial/model/Address.php';
require 'src/socialdata/opensocial/model/ApiCollection.php';
require 'src/socialdata/opensocial/model/BodyType.php';
require 'src/socialdata/opensocial/model/Email.php';
require 'src/socialdata/opensocial/model/Enum.php';
require 'src/socialdata/opensocial/model/idSpec.php';
require 'src/socialdata/opensocial/model/MediaItem.php';
require 'src/socialdata/opensocial/model/Message.php';
require 'src/socialdata/opensocial/model/Name.php';
require 'src/socialdata/opensocial/model/Organization.php';
require 'src/socialdata/opensocial/model/Person.php';
require 'src/socialdata/opensocial/model/Phone.php';
require 'src/socialdata/opensocial/model/Url.php';
require 'src/socialrest/GroupId.php';
require 'src/socialrest/UserId.php';
require 'src/socialrest/ResponseItem.php';
require 'src/socialrest/RestfulCollection.php';
require 'src/socialrest/http/RestRequestItem.php';

/*
 * See:
 * http://sites.google.com/a/opensocial.org/opensocial/Technical-Resources/opensocial-specification----implementation-version-08/restful-api-specification
 * OpenSocial uses standard HTTP methods: GET to retrieve, PUT to update in place, POST to create new, and DELETE to remove.
 * POST is special; it operates on collections and creates new activities, persons, or app data within those collections,
 * and returns the base URI for the created resource in the Location: header, per AtomPub semantics.
 *
 * Error status is returned by HTTP error code, with the error message in the html's body
 */

//NOTE TO SELF: delete should respond with a 204 No Content to indicate success?


/*
 * Internal error code representations, these get translated into http codes in the outputError() function
 */
define('NOT_FOUND', "notFound");
define('NOT_IMPLEMENTED', "notImplemented");
define('UNAUTHORIZED', "unauthorized");
define('FORBIDDEN', "forbidden");
define('BAD_REQUEST', "badRequest");
define('INTERNAL_ERROR', "internalError");

class RestException extends Exception {}

class RestServlet extends HttpServlet {
	
	private static $JSON_BATCH_ROUTE = "jsonBatch";

	public function doPost($method = 'POST')
	{
		$this->setNoCache(true);
		$this->noHeaders = true;
		// use security token, for now this is required
		// (later oauth should also be a way to specify this info)
		$token = $this->getSecurityToken();
		$req = null;
		if ($this->isBatchUrl()) {
			$req = $this->handleBatchRequest($token);
			echo json_encode(array("responses" => $req, "error" => false));
		} else {
			$responseItem = $this->handleSingleRequest($token, $method);
			echo json_encode($responseItem);
		}
	}

	private function handleSingleRequest($token, $method)
	{
		$params = $this->getListParams();
		$requestItem = new RestRequestItem();
		$url = $this->getUrl();
		$requestParam = $this->getRequestParams();
		$requestItem->createRequestItem($url, $token, $method, $params, $requestParam);
		$responseItem = $this->getResponseItem($requestItem);
		return $responseItem;
	}

	private function getRouteFromParameter($pathInfo)
	{
		$pathInfo = substr($pathInfo, 1);
		$indexOfNextPathSeparator = strpos($pathInfo, "/");
		return $indexOfNextPathSeparator != - 1 ? substr($pathInfo, 0, $indexOfNextPathSeparator) : $pathInfo;
	}

	private function getResponseItem(RestRequestItem $requestItem)
	{
		$path = $this->getRouteFromParameter($requestItem->getUrl());
		$class = false;
		switch ($path) {
			case 'people':
				$class = 'PersonHandler';
				break;
			case 'activities':
				$class = 'ActivityHandler';
				break;
			case 'appdata':
				$class = 'AppDataHandler';
				break;
			//TODO add 'groups' here
			default:
				$response = new ResponseItem(NOT_IMPLEMENTED, "{$path} is not implemented");
				break;
		}
		if ($class && class_exists($class, true)) {
			$class = new $class(null);
			$response = $class->handleMethod($requestItem);
		}
		if ($this->getOutputFormat() == 'json') {
			if ($this->isBatchUrl()) {
				return $response;
			} else {
				//If the method exists, its an ResponseItem Error
				if ($response->getError() != null) {
					$this->outputError($response);
				}
				return $response->getResponse();
			}
		} else {	// output atom format
		}
	}

	private function getRequestParams()
	{
		$post = in_array('request', $_POST) != null ? $_POST['request'] : null;
		$requestParam = isset($GLOBALS['HTTP_RAW_POST_DATA']) ? $GLOBALS['HTTP_RAW_POST_DATA'] : $post;
		if (get_magic_quotes_gpc()) {
			$requestParam = stripslashes($requestParam);
		}
		$requests = json_decode($requestParam);
		if ($requests == (isset($GLOBALS['HTTP_RAW_POST_DATA']) ? $GLOBALS['HTTP_RAW_POST_DATA'] : $post)) {
			return new ResponseItem(BAD_REQUEST, "Malformed json string");
		}
		return $requests;
	}

	private function handleBatchRequest($token)
	{
		// we support both a raw http post (without application/x-www-form-urlencoded headers) like java does
		// and a more php / curl safe version of a form post with 'request' as the post field that holds the request json data
		if (isset($GLOBALS['HTTP_RAW_POST_DATA']) || isset($_POST['request'])) {
			$requests = $this->getRequestParams();
			$responses = array();
			foreach ($requests as $key => $value) {
				$requestItem = new RestRequestItem();
				$requestItem->createRequestItemWithRequest($value, $token);
				$responses[$key] = $this->getResponseItem($requestItem);
			}
			return $responses;
		} else {
			throw new Exception("No post data set");
		}
	}

	public function doGet()
	{
		$this->doPost('GET');
	}

	public function doPut()
	{
		$this->doPost('PUT');
	}

	public function doDelete()
	{
		$this->doPost('DELETE');
	}

	private function outputError(ResponseItem $response)
	{
		$errorMessage = $response->getErrorMessage();
		switch ($response->getError()) {
			case BAD_REQUEST:
				$code = '400 Bad Request';
				break;
			case UNAUTHORIZED:
				$code = '401 Unauthorized';
				break;
			case FORBIDDEN:
				$code = '403 Forbidden';
				break;
			case FORBIDDEN:
				$code = '404 Not Found';
				break;
			case NOT_IMPLEMENTED:
				$code = '501 Not Implemented';
				break;
			case INTERNAL_ERROR:
			default:
				$code = '500 Internal Server Error';
				break;
		
		}
		header("HTTP/1.0 $code", true);
		echo "$code - $errorMessage";
		die();
	}

	private function getSecurityToken()
	{
		$token = isset($_GET['st']) ? $_GET['st'] : '';
		if (empty($token)) {
			throw new RestException("Missing security token");
		}
		if (count(explode(':', $token)) != 6) {
			$token = urldecode(base64_decode($token));
		}
		$gadgetSigner = Config::get('security_token_signer');
		$gadgetSigner = new $gadgetSigner();
		return $gadgetSigner->createToken($token);
	}

	private function getOutputFormat()
	{
		return isset($_POST['format']) && $_POST['format'] == 'atom' ? 'atom' : 'json';
	}

	private function getListParams()
	{
		// get the rest params
		$uri = substr($_SERVER["REQUEST_URI"], strlen(Config::get('web_prefix') . '/social/rest') + 1);
		if (($pos = strpos($uri, '?')) !== false) {
			$uri = substr($uri, 0, $pos);
		}
		$restParams = explode('/', $uri);
		return $restParams;
	}

	private function getUrl()
	{
		return substr($_SERVER["REQUEST_URI"], strlen(Config::get('web_prefix') . '/social/rest'));
	}

	public function isBatchUrl()
	{
		return strrpos($_SERVER["REQUEST_URI"], RestServlet::$JSON_BATCH_ROUTE) > 0;
	}
}
