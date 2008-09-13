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
require 'src/social/model/Activity.php';
require 'src/social/model/Address.php';
require 'src/social/model/ApiCollection.php';
require 'src/social/model/BodyType.php';
require 'src/social/model/Email.php';
require 'src/social/model/Enum.php';
require 'src/social/model/IdSpec.php';
require 'src/social/model/MediaItem.php';
require 'src/social/model/Message.php';
require 'src/social/model/Name.php';
require 'src/social/model/Organization.php';
require 'src/social/model/Person.php';
require 'src/social/model/Phone.php';
require 'src/social/model/Url.php';
require 'src/social/spi/GroupId.php';
require 'src/social/spi/UserId.php';
require 'src/social/spi/RestfulCollection.php';
require 'src/social/spi/ActivitiesService.php';
require 'src/social/spi/AppDataService.php';
require 'src/social/spi/PeopleService.php';
require 'src/social/service/RestRequestItem.php';
require 'src/social/service/DataRequestHandler.php';
require 'src/social/service/ActivitiesHandler.php';
require 'src/social/service/AppDataHandler.php';
require 'src/social/service/PeopleHandler.php';
require 'src/social/service/ResponseItem.php';
require 'src/social/converters/OutputConverter.php';
require 'src/social/converters/OutputAtomConverter.php';
require 'src/social/converters/OutputJsonConverter.php';
require 'src/social/converters/OutputXmlConverter.php';
require 'src/social/converters/InputConverter.php';
require 'src/social/converters/InputAtomConverter.php';
require 'src/social/converters/InputJsonConverter.php';
require 'src/social/converters/InputXmlConverter.php';
require 'src/social/sample/JsonDbOpensocialService.php';

class RestException extends Exception {}

/*
 * Internal error code representations, these get translated into http codes in the outputError() function
 */
define('NOT_FOUND', "notFound");
define('NOT_IMPLEMENTED', "notImplemented");
define('UNAUTHORIZED', "unauthorized");
define('FORBIDDEN', "forbidden");
define('BAD_REQUEST', "badRequest");
define('INTERNAL_ERROR', "internalError");

//FIXME Delete should respond with a 204 No Content to indicate success


class RestServlet extends HttpServlet {
	
	// The json Batch Route is used by the gadgets 
	private static $JSON_BATCH_ROUTE = "jsonBatch";
	// The Batch Proxy route is used the one defined in the RESTful API specification
	private static $BATCH_PROXY_ROUTE = "batchProxy";

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

	public function doPost($method = 'POST')
	{
		try {
			$this->setNoCache(true);
			// if oauth, create a token from it's values instead of one based on $_get['st']/$_post['st']
			// NOTE : if no token is provided an anonymous one is created (owner = viewer = appId = modId = 0)
			// keep this in mind when creating your data services.. 
			$token = $this->getSecurityToken();
			$outputFormat = $this->getOutputFormat();
			switch ($outputFormat) {
				case 'json':
					$this->setContentType('application/json');
					$outputConverter = new OutputJsonConverter();
					break;
				case 'atom':
					$this->setContentType('application/atom+xml');
					$outputConverter = new OutputAtomConverter();
					break;
				case 'xml':
					$this->setContentType('application/xml');
					$outputConverter = new OutputXmlConverter();
					break;
				default:
					$this->outputError(new ResponseItem(NOT_IMPLEMENTED, "Invalid output format"));
					break;
			}
			if ($this->isJsonBatchUrl()) {
				// custom json batch format used by the gadgets
				$responses = $this->handleJsonBatchRequest($token);
				$outputConverter->outputJsonBatch($responses, $token);
			} else {
				// single rest request
				$response = $this->handleRequest($token, $method);
				$outputConverter->outputResponse($response['response'], $response['request']);
			}
		} catch (Exception $e) {
			header("HTTP/1.0 500 Internal Server Error");
			echo "<html><body><h1>500 Internal Server Error</h1>";
			echo "Message: " . $e->getMessage() . "<br />\n";
			if (Config::get('debug')) {
				echo "<pre>\n";
				print_r($e);
				echo "\n</pre>";
			}
			echo "</body></html>";
		}
	}

	private function handleRequest($token, $method)
	{
		$params = $this->getListParams();
		$requestItem = new RestRequestItem();
		$url = $this->getUrl();
		$requestType = $this->getRouteFromParameter($url);
		$requestFormat = $this->getRequestFormat();
		$requestParam = $this->getRequestParams($requestType, $requestFormat);
		$requestItem->createRequestItem($url, $token, $method, $params, $requestParam);
		$responseItem = $this->getResponseItem($requestItem);
		return array('request' => $requestItem, 'response' => $responseItem);
	}

	private function handleJsonBatchRequest($token)
	{
		// we support both a raw http post (without application/x-www-form-urlencoded headers) like java does
		// and a more php / curl safe version of a form post with 'request' as the post field that holds the request json data
		if (isset($GLOBALS['HTTP_RAW_POST_DATA']) || isset($_POST['request'])) {
			$requests = $this->getRequestParams('jsonbatch');
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

	private function getResponseItem(RestRequestItem $requestItem)
	{
		$path = $this->getRouteFromParameter($requestItem->getUrl());
		$class = false;
		switch ($path) {
			case 'people':
				$class = 'PeopleHandler';
				break;
			case 'activities':
				$class = 'ActivitiesHandler';
				break;
			case 'appdata':
				$class = 'AppDataHandler';
				break;
			case 'messages':
				$class = 'MessagesHandler';
				break;
			//TODO add 'groups' and 'messages' here
			default:
				$response = new ResponseItem(NOT_IMPLEMENTED, "{$path} is not implemented");
				break;
		}
		if ($class && class_exists($class, true)) {
			$class = new $class(null);
			$response = $class->handleMethod($requestItem);
		}
		if ($response->getError() != null && ! $this->isJsonBatchUrl() && ! $this->isBatchProxyUrl()) {
			// Can't use http error codes in batch mode, instead we return the error code in the response item
			$this->outputError($response);
		}
		return $response;
	}

	private function decodeRequests($requestParam, $requestType, $format = 'json')
	{
		if (empty($requestParam)) {
			return null;
		}
		switch ($format) {
			case 'json':
				$inputConverter = new InputJsonConverter();
				break;
			case 'atom':
				$inputConverter = new InputAtomConverter();
				break;
			case 'xml':
				$inputConverter = new InputXmlConverter();
				break;
			default:
				throw new Exception("Invalid or unsupported input format");
				break;
		}
		switch ($requestType) {
			case 'people':
				$ret = $inputConverter->convertPeople($requestParam);
				break;
			case 'activities':
				$ret = $inputConverter->convertActivities($requestParam);
				break;
			case 'messages':
				$ret = $inputConverter->convertMessages($requestParam);
				break;
			case 'appdata':
				$ret = $inputConverter->convertAppData($requestParam);
				break;
			case 'jsonbatch':
				// this type is only used by the internal json batch format
				if ($format != 'json') {
					throw new Exception("the json batch only supports the json input format");
				}
				$ret = $inputConverter->convertJsonBatch($requestParam);
				break;
			//TODO add 'groups' and 'messages' here
			default:
				throw new Exception("Unsupported REST call");
				break;
		}
		return $ret;
	}

	private function getRequestParams($requestType, $requestFormat = 'json')
	{
		$post = in_array('request', $_POST) != null ? $_POST['request'] : null;
		$requestParam = isset($GLOBALS['HTTP_RAW_POST_DATA']) ? $GLOBALS['HTTP_RAW_POST_DATA'] : $post;
		if (get_magic_quotes_gpc()) {
			$requestParam = stripslashes($requestParam);
		}
		return $this->decodeRequests($requestParam, $requestType, $requestFormat);
	}

	private function getRouteFromParameter($pathInfo)
	{
		$pathInfo = substr($pathInfo, 1);
		$indexOfNextPathSeparator = strpos($pathInfo, "/");
		return $indexOfNextPathSeparator !== false ? substr($pathInfo, 0, $indexOfNextPathSeparator) : $pathInfo;
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
			// no security token, continue anonymously, remeber to check
			// for private profiles etc in your code so their not publicly
			// accessable to anoymous users! Anonymous == owner = viewer = appId = modId = 0
			$gadgetSigner = Config::get('security_token');
			// create token with 0 values, no gadget url, no domain and 0 duration
			return new $gadgetSigner(null, 0, 0, 0, 0, '', '', 0);
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
		$output = ! empty($_POST['format']) ? $_POST['format'] : (! empty($_GET['format']) ? $_GET['format'] : 'json');
		return strtolower(trim($output));
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

	private function getRequestFormat()
	{
		if (isset($_SERVER['CONTENT_TYPE'])) {
			switch ($_SERVER['CONTENT_TYPE']) {
				case 'application/atom+xml':
					return 'atom';
				case 'application/json':
					return 'json';
				default:
					throw new Exception("Invalid request content type");
			}
		}
		// if no Content-Type header is set, we assume json
		return 'json';
	}

	private function getUrl()
	{
		return substr($_SERVER["REQUEST_URI"], strlen(Config::get('web_prefix') . '/social/rest'));
	}

	public function isJsonBatchUrl()
	{
		return strrpos($_SERVER["REQUEST_URI"], RestServlet::$JSON_BATCH_ROUTE) !== false;
	}

	public function isBatchProxyUrl()
	{
		return strrpos($_SERVER["REQUEST_URI"], RestServlet::$BATCH_PROXY_ROUTE) !== false;
	}
}
