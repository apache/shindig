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
 * Abstract class for the Output conversion of the RESTful API
 *
 */
abstract class OutputConverter {
	private $boundry;
	
	abstract function outputResponse(ResponseItem $responseItem, RestRequestItem $requestItem);
	abstract function outputBatch(Array $responses, SecurityToken $token);
	
	/**
	 * Output the multipart/mixed headers and returns the boundry token used
	 *
	 */
	public function boundryHeaders()
	{
		$this->boundry = '--batch-'.md5(rand(0,32000));
		header("HTTP/1.1 200 OK", true);
		header("Content-Type: multipart/mixed; boundary=$this->boundry", true);
	}
	
	public function outputPart($part, $code)
	{
		$boundryHeader = "{$this->boundry}\r\n".
				"Content-Type: application/http;version=1.1\r\n".
				"Content-Transfer-Encoding: binary\r\n\r\n";
		echo $boundryHeader;
		switch ($code) {
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
				$code = '200 OK';
				break;
		}
		echo "$code\r\n\r\n";
		echo $part."\n";
	}
}
