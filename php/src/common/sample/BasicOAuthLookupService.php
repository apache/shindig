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
 * 
 */

/**
 * Basic implementation of OAuthLookupService using BasicOAuthDataStore.
 */
class BasicOAuthLookupService extends OAuthLookupService {

	public function thirdPartyHasAccessToUser($oauthRequest, $appUrl, $userId)
	{
		$appId = $this->getAppId($appUrl);
		return $this->hasValidSignature($oauthRequest, $appUrl, $appId) && $this->userHasAppInstalled($userId, $appId);
	}

	private function hasValidSignature($oauthRequest, $appUrl, $appId)
	{
		try {
			$server = new OAuthServer(new BasicOAuthDataStore());
			$server->add_signature_method(new OAuthSignatureMethod_HMAC_SHA1());
			$server->add_signature_method(new OAuthSignatureMethod_PLAINTEXT());
			list($consumer, $token) = $server->verify_request($oauthRequest);
			return true;
		} catch (OAuthException $e) {
			//FIXME seems some old debugging code? should clean this up if so
			echo "OAuthException: " . $e->getMessage();
		}
		return false;
	}

	private function userHasAppInstalled($apUrl, $appId)
	{
		return true; // a real implementation would look this up
	}

	public function getSecurityToken($appUrl, $userId)
	{
		return new OAuthSecurityToken($userId, $appUrl, $this->getAppId($appUrl), "samplecontainer");
	}

	private function getAppId($appUrl)
	{
		return 0; // a real implementation would look this up
	}
}
