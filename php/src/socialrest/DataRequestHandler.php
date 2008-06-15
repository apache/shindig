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

abstract class DataRequestHandler {
	
	public function handleMethod($method, $params, $token)
	{
		if ($method == 'POST') {
			$response = $this->handlePost($params, $token);
		} elseif ($method == 'GET') {
			$response = $this->handleGet($params, $token);
		} elseif ($method == 'DELETE') {
			$response = $this->handleDelete($params, $token);
		} elseif ($method == 'PUT') {
			$response = $this->handlePut($params, $token);
		} else {
			$response = new ResponseItem(BAD_REQUEST, "Unserviced Http method type", null);
		}
		return $response;
	}
	
	static public function getAppId($appId, SecurityToken $token)
	{
	    if ($appId == '@app') {
			return $token->getAppId();
		} else {
			return $appId;
		}
	}
	
	static public function convertToObject($string)
	{
		//TODO should detect if it's atom/xml or json here really. assuming json for now
		$decoded = json_decode($string);
		if ($decoded == $string) {
			throw new Exception("Invalid JSON syntax");
		}
		return $decoded;
	}

	abstract public function handleDelete($params, $token);

	abstract public function handleGet($params, $token);

	abstract public function handlePost($params, $token);

	abstract public function handlePut($params, $token);
}