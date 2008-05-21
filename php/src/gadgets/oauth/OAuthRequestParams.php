<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


/**
 * Bundles information about a proxy request that requires OAuth
 */
class OAuthRequestParams {
	public static $SERVICE_PARAM = "oauthService";
	public static $TOKEN_PARAM = "oauthToken";
	public static $CLIENT_STATE_PARAM = "oauthState";
	private $serviceName;
	private $tokenName;
	private $origClientState;

	public function __construct()
	{
		$this->serviceName = $_REQUEST[OAuthRequestParams::$SERVICE_PARAM];
		$this->tokenName = $_REQUEST[OAuthRequestParams::$TOKEN_PARAM];
		$this->origClientState = $_REQUEST[OAuthRequestParams::$CLIENT_STATE_PARAM];
	}

	public function getServiceName()
	{
		return $this->serviceName;
	}

	public function getTokenName()
	{
		return $this->tokenName;
	}

	public function getOrigClientState()
	{
		return $this->origClientState;
	}
}
