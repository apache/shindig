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
require 'src/socialdata/DataResponse.php';
require 'src/socialdata/GadgetDataHandler.php';
require 'src/common/SecurityTokenDecoder.php';
require 'src/common/SecurityToken.php';
require 'src/common/BlobCrypter.php';
require 'src/common/Crypto.php';
require 'src/socialdata/RequestItem.php';
require 'src/socialdata/ResponseItem.php';
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

// Response item error codes
define('NOT_IMPLEMENTED', "notImplemented");
define('UNAUTHORIZED', "unauthorized");
define('FORBIDDEN', "forbidden");
define('BAD_REQUEST', "badRequest");
define('INTERNAL_ERROR', "internalError");

class GadgetDataServlet extends HttpServlet {
	private $handlers = array();

	public function __construct()
	{
		parent::__construct();
		$handlers = Config::get('handlers');
		if (empty($handlers)) {
			$this->handlers[] = new OpenSocialDataHandler();
			$this->handlers[] = new StateFileDataHandler();
		} else {
			$handlers = explode(',', $handlers);
			foreach ($handlers as $handler) {
				$this->handlers[] = new $handler();
			}
		}
	}

	public function doPost()
	{
		try {
			$requestParam = isset($_POST['request']) ? $_POST['request'] : '';
			$token = isset($_POST['st']) ? $_POST['st'] : '';
			if (count(explode(':', $token)) != 6) {
				$token = urldecode(base64_decode($token));
			}
			// detect if magic quotes are on, and if so strip them from the request
			if (get_magic_quotes_gpc()) {
				$requestParam = stripslashes($requestParam);
			}
			$requests = json_decode($requestParam, true);
			if ($requests == $requestParam) {
				// oddly enough if the json_decode function can't parse the code,
				// it just returns the original string (instead of something usefull like 'null' or false :))
				throw new Exception("Invalid request JSON");
			}
			$response = new DataResponse($this->createResponse($requests, $token));
		} catch (Exception $e) {
			$response = new DataResponse(false, BAD_REQUEST);
		}
		echo json_encode($response);
	}

	private function createResponse($requests, $token)
	{
		if (empty($token)) {
			throw new Exception("INVALID_GADGET_TOKEN");
		}
		$gadgetSigner = Config::get('security_token_signer');
		$gadgetSigner = new $gadgetSigner();
		//FIXME currently don't have a propper token, impliment and re-enable this asap
		$securityToken = $gadgetSigner->createToken($token);
		$responseItems = array();
		foreach ($requests as $request) {
			$requestItem = new RequestItem($request['type'], $request, $securityToken);
			$response = new ResponseItem(NOT_IMPLEMENTED, $request['type'] . " has not been implemented yet.", array());
			foreach ($this->handlers as $handler) {
				if ($handler->shouldHandle($request['type'])) {
					$response = $handler->handleRequest($requestItem);
				}
			}
			$responseItems[] = $response;
		}
		return $responseItems;
	}

	public function doGet()
	{
		echo header("HTTP/1.0 400 Bad Request", true, 400);
		die("<h1>Bad Request</h1>");
	}
}
