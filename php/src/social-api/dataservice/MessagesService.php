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

interface MessagesService {

	/**  $message is an array containing the following fields:
	 *  [id] => {msgid}
	 *  [title] => You have an invitation from Joe
	 *  [body] => Click <a href="http://app.example.org/invites/{msgid}">here</a> to review your invitation.
	 *  [recipients] => Array
	 *      (
	 *          [0] => example.org:AD38B3886625AAF
	 *          [1] => example.org:997638BAA6F25AD
	 *      )
	 */
	public function createMessage($userId, $message, SecurityToken $token);
}
