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

/**
 * Represents the request items that come from the restful request.
 */
class RestRequestItem {
	// Common OpenSocial RESTful fields
	public static $APP_ID = "appId";
	public static $USER_ID = "userId";
	public static $GROUP_ID = "groupId";
	public static $START_INDEX = "startIndex";
	public static $COUNT = "count";
	public static $ORDER_BY = "orderBy";
	public static $NETWORK_DISTANCE = "networkDistance";
	public static $FILTER_BY = "filterBy";
	public static $FIELDS = "fields";
	
	// OpenSocial defaults
	public static $DEFAULT_START_INDEX = 0;
	public static $DEFAULT_COUNT = 20;
	
	public static $APP_SUBSTITUTION_TOKEN = "@app";
	
	private $url;
	private $method;
	private $parameters = array();
	private $token;
	private $postData;

	public function createRequestItemWithRequest($request, $token)
	{
		$this->url = $request['url'];
		$this->parameters = $this->createParameterMap($request['url']);
		$this->token = $token;
		$this->method = $request['method'];
		if (isset($request['postData'])) {
			$this->postData = $request['postData'];
		}
	}

	public function createRequestItem($url, $token, $method, $params, $postData)
	{
		$this->url = $url;
		$this->parameters = $params;
		$this->token = $token;
		$this->method = $method;
		$this->postData = $postData;
	}

	private function createParameterMap($uri)
	{
		// get the rest params
		if ($uri == null) {
			$uri = substr($_SERVER["REQUEST_URI"], strlen(Config::get('web_prefix') . '/social/rest') + 1);
		} else {
			$uri = substr($uri, 1);
		}
		if (($pos = strpos($uri, '?')) !== false) {
			$uri = substr($uri, 0, $pos);
		}
		return explode('/', $uri);
	}

	/*
	 * Takes any url params out of the url and puts them into the param map.
	 * Usually the servlet request code does this for us but the batch request calls have to do it
	 * by hand.
	 */
	private function putUrlParamsIntoParameters()
	{
		$fullUrl = $this->url;
		$queryParamIndex = strpos($fullUrl, "?");
		if ($queryParamIndex > 0) {
			$this->url = substr($fullUrl, 0, $queryParamIndex);
			$queryParams = substr($fullUrl, $queryParamIndex + 1);
			$params = explode("&", $queryParams);
			foreach ($params as $param) {
				$paramPieces = explode("=", $param, 2);
				$this->parameters[$paramPieces[0]] = count($paramPieces) == 2 ? $paramPieces[1] : "";
			}
		}
	}

	/**
	 * This could definitely be cleaner..
	 * TODO: Come up with a cleaner way to handle all of this code.
	 *
	 * @param urlTemplate The template the url follows
	 */
	public function parseUrlWithTemplate($urlTemplate)
	{
		$this->putUrlParamsIntoParameters();
		$actualUrl = explode("/", $this->url);
		$expectedUrl = explode("/", $urlTemplate);
		for ($i = 0; $i < count($actualUrl); $i ++) {
			$actualPart = $actualUrl[$i];
			$expectedPart = $expectedUrl[$i];
			if (strpos($expectedPart, "{") !== false) {
				$this->parameters[substr($expectedPart, 1, strlen($expectedPart) - 2)] = $actualPart;
			}
		}
	}

	public function getAppId()
	{
		if (isset($this->parameters[self::$APP_ID]) && $this->parameters[self::$APP_ID] == self::$APP_SUBSTITUTION_TOKEN) {
			return $this->token->getAppId();
		} elseif (isset($this->parameters[self::$APP_ID])) {
			return $this->parameters[self::$APP_ID];
		} else {
			return 0;
		}
	}

	public function getUser()
	{
		return isset($this->parameters[self::$USER_ID]) ? UserId::fromJson($this->parameters[self::$USER_ID]) : false;
	}

	public function getGroup()
	{
		return isset($this->parameters[self::$GROUP_ID]) ? GroupId::fromJson($this->parameters[self::$GROUP_ID]) : false;
	}

	public function getStartIndex()
	{
		if (! empty($this->parameters[self::$START_INDEX])) {
			return $this->parameters[self::$START_INDEX];
		} else {
			return self::$DEFAULT_START_INDEX;
		}
	}

	public function getCount()
	{
		if (! empty($this->parameters[self::$COUNT])) {
			return $this->parameters[self::$COUNT];
		} else {
			return self::$DEFAULT_COUNT;
		}
	}

	public function getOrderBy()
	{
		if (! empty($this->parameters[self::$ORDER_BY])) {
			return $this->parameters[self::$ORDER_BY];
		} else {
			return PeopleOptions::$sortOrder;
		}
	}

	public function getNetworkDistance()
	{
		if (! empty($this->parameters[self::$NETWORK_DISTANCE])) {
			return $this->parameters[self::$NETWORK_DISTANCE];
		} else {
			return false;
		}
	}

	public function getFilterBy()
	{
		if (! empty($this->parameters[self::$FILTER_BY])) {
			return $this->parameters[self::$FILTER_BY];
		} else {
			return PeopleOptions::$filterType;
		}
	}

	public function getFields()
	{
		return $this->getFieldsWithDefaultValue(array());
	}

	public function getFieldsWithDefaultValue(Array $defaultValue)
	{
		if (! empty($this->parameters[self::$FIELDS])) {
			$paramValue = $this->parameters[self::$FIELDS];
			return explode(',', $paramValue);
		} else {
			return $defaultValue;
		}
	}

	public function getPostData()
	{
		return $this->postData;
	}

	public function getUrl()
	{
		return $this->url;
	}

	public function setUrl($url)
	{
		$this->url = $url;
	}

	public function getMethod()
	{
		return $this->method;
	}

	public function setMethod($method)
	{
		$this->method = $method;
	}

	public function getParameters()
	{
		return $this->parameters;
	}

	public function setParameters($parameters)
	{
		$this->parameters = $parameters;
	}

	public function getToken()
	{
		return $this->token;
	}

	public function setToken($token)
	{
		$this->token = $token;
	}
}
