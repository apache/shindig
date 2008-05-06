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

class UrlGenerator {
	static function getIframeURL($gadget)
	{
		//TODO this
		// returns fake demo value to make the basic JsonRpc service work
		// note: put the URL last, else some browsers seem to get confused (reported by hi5)
		return "/gadgets/ifr?container=default&mid=1&v=db18c863f15d5d1e758a91f2a44881b4&lang=en&country=US&url=http%3A%2F%2Fwww.google.com%2Fig%2Fmodules%2Fhello.xml";
	}
}
