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
 * Convert Xml representations to the internal data structure representation
 */
class InputXmlConverter extends InputConverter {

	public function convertPeople($requestParam)
	{
		throw new Exception("Operation not supported");
	}

	public function convertActivities($requestParam)
	{
		$activity = array();
		$xml = simplexml_load_string($requestParam, 'SimpleXMLElement', LIBXML_NOCDATA);
		if (! isset($xml->title)) {
			throw new Exception("Mallformed activity xml");
		}
		// remember to either type cast to (string) or trim() the string so we don't get 
		// SimpleXMLString types in the internal data representation. I often prefer
		// using trim() since it cleans up the data too
		$activity['id'] = isset($xml->id) ? trim($xml->id) : '';
		$activity['title'] = trim($xml->title);
		$activity['body'] = isset($xml->summary) ? trim($xml->summary) : '';
		$activity['streamTitle'] = isset($xml->activity->streamTitle) ? trim($xml->activity->streamTitle) : '';
		$activity['streamId'] = isset($xml->activity->streamId) ? trim($xml->activity->streamId) : '';
		$activity['updated'] = isset($xml->updated) ? trim($xml->updated) : '';
		if (isset($xml->activity->mediaItems)) {
			$activity['mediaItems'] = array();
			foreach ($xml->activity->mediaItems->MediaItem as $mediaItem) {
				$item = array();
				if (! isset($mediaItem->type) || ! isset($mediaItem->mimeType) || ! isset($mediaItem->url)) {
					throw new Exception("Invalid media item in activity xml");
				}
				$item['type'] = trim($mediaItem->type);
				$item['mimeType'] = trim($mediaItem->mimeType);
				$item['url'] = trim($mediaItem->url);
				$activity['mediaItems'][] = $item;
			}
		}
		return $activity;
	}

	public function convertAppData($requestParam)
	{
		$xml = simplexml_load_string($requestParam, 'SimpleXMLElement', LIBXML_NOCDATA);
		if (! isset($xml->entry)) {
			throw new Exception("Mallformed AppData xml");
		}
		$data = array();
		foreach ($xml->entry as $entry) {
			$key = trim($entry->key);
			$val = isset($entry->value) ? trim($entry->value) : null;
			$data[$key] = $val;
		}
		return $data;
	}

	public function convertMessages($requestParam)
	{
		$xml = simplexml_load_string($requestParam, 'SimpleXMLElement', LIBXML_NOCDATA);
		$message = array();
		if (! isset($xml->title) || ! isset($xml->body)) {
			throw new Exception("Invalid message structure");
		}
		$message['id'] = isset($xml->id) ? trim($xml->id) : null;
		$message['title'] = trim($xml->title);
		$message['body'] = trim($xml->body);
		// retrieve recipients by looking at the osapi name space
		$xml = simplexml_load_string($requestParam, 'SimpleXMLElement', LIBXML_NOCDATA, "http://opensocial.org/2008/opensocialapi");
		if (! isset($xml->recipient)) {
			throw new Exception("Invalid message structure");
		}
		$message['recipients'] = array();
		foreach ($xml->recipient as $recipient) {
			$message['recipients'][] = trim($recipient);
		}
		return $message;
	}
}
