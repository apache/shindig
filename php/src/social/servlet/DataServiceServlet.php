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

class DataServiceServlet extends ApiServlet {
	
	protected static $FORMAT_PARAM = "format";
	protected static $ATOM_FORMAT = "atom";
	protected static $XML_FORMAT = "atom";
	
	public static $PEOPLE_ROUTE = "people";
	public static $ACTIVITY_ROUTE = "activities";
	public static $APPDATA_ROUTE = "appdata";
	public static $MESSAGE_ROUTE = "message";

	public function doGet()
	{
		$this->doPost();
	}

	public function doPut()
	{
		$this->doPost();
	}

	public function doDelete()
	{
		$this->doPost();
	}

	public function doPost()
	{
		try {
			$token = $this->getSecurityToken();
			if ($token == null) {
				$this->sendSecurityError();
				return;
			}
			$converter = $this->getConverterForRequest();
			$this->handleSingleRequest($token, $converter);
		} catch (Exception $e) {
			echo "<b>Exception: " . $e->getMessage() . "</b><br><pre>\n";
			echo $e->getTraceAsString();
			echo "</pre>";
		}
	}

	public function sendError(ResponseItem $responseItem)
	{
		$errorMessage = $responseItem->getErrorMessage();
		switch ($responseItem->getError()) {
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

	/**
	 * Handler for non-batch requests
	 */
	private function handleSingleRequest(SecurityToken $token, $converter)
	{
		$servletRequest = array(
				'url' => substr($_SERVER["REQUEST_URI"], strlen(Config::get('web_prefix') . '/social/rest')));
		$requestItem = RestRequestItem::createWithRequest($servletRequest, $token, $converter);
		$responseItem = $this->getResponseItem($this->handleRequestItem($requestItem));
		if ($responseItem->getError() == null) {
			//FIXME does this code have to be here? bah, breaks our converters :)
			/*if (! ($responseItem instanceof DataCollection) && ! ($responseItem instanceof RestfulCollection)) {
				$responseItem = array("entry" => $responseItem);
			}*/
			$converter->outputResponse($responseItem, $requestItem);
		} else {
			$this->sendError($responseItem);
		}
	}

	private function getConverterForRequest()
	{
		$outputFormat = strtolower(trim(! empty($_POST[self::$FORMAT_PARAM]) ? $_POST[self::$FORMAT_PARAM] : (! empty($_GET[self::$FORMAT_PARAM]) ? $_GET[self::$FORMAT_PARAM] : 'json')));
		switch ($outputFormat) {
			case 'xml':
				return new OutputXmlConverter();
			case 'atom':
				return new OutputAtomConverter();
			case 'json':
				return new OutputJsonConverter();
			default:
				throw new Exception("Unknown format param: $outputFormat");
		}
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

	private function getRouteFromParameter($pathInfo)
	{
		$pathInfo = substr($pathInfo, 1);
		$indexOfNextPathSeparator = strpos($pathInfo, "/");
		return $indexOfNextPathSeparator !== false ? substr($pathInfo, 0, $indexOfNextPathSeparator) : $pathInfo;
	}
}
