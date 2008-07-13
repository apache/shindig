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
 * Format = atom output converter, for format definition see:
 * http://www.opensocial.org/Technical-Resources/opensocial-specification----implementation-version-08/restful-api-specification
 */
class OutputAtomConverter extends OutputConverter {
	private static $nameSpace = 'http://www.w3.org/2005/Atom';
	private static $osNameSpace = 'http://ns.opensocial.org/2008/opensocial';
	private static $xmlVersion = '1.0';
	private static $charSet = 'UTF-8';
	private static $formatOutput = true;
	// this maps the REST url to the atom content type
	private static $entryTypes = array(
		'people' => 'person', 'appdata' => 'appdata', 'activities' => 'activity'
	);

	function outputResponse(ResponseItem $responseItem, RestRequestItem $requestItem)
	{
		$doc = new DOMDocument(self::$xmlVersion, self::$charSet);
		$doc->formatOutput = self::$formatOutput;
		$data = $responseItem->getResponse();
		$params = $requestItem->getParameters();
		// map the Request URL to the content type to use  
		if (empty(self::$entryTypes[$params[0]])) {
			throw new Exception("Unsupported request type");
		}
		$requestType = self::$entryTypes[$params[0]];
		// Check to see if this is a single entry, or a collection, and construct either an atom 
		// feed (collection) or an entry (single)
		
		if ($responseItem->getResponse() instanceof RestFulCollection) {
			$entry = $doc->appendChild($doc->createElementNS(self::$nameSpace, "feed"));
			
			// osearch fields and next link
			$total = $entry->appendChild($doc->createElement('osearch:totalResults'));
			$total->appendChild($doc->createTextNode($responseItem->getResponse()->getTotalResults()));
			$startIndex = $entry->appendChild($doc->createElement('osearch:startIndex'));
			$startIndex->appendChild($doc->createTextNode($requestItem->getStartIndex()));
			$itemsPerPage = $entry->appendChild($doc->createElement('osearch:itemsPerPage'));
			$itemsPerPage->appendChild($doc->createTextNode($requestItem->getCount()));
			
			// fabricate a next link based on our current url if this is a pageable collection
			if (($requestItem->getStartIndex() + $requestItem->getCount()) < $responseItem->getResponse()->getTotalResults()) {
				$nextStartIndex = $requestItem->getStartIndex() + $requestItem->getCount();
				if (($uri = $_SERVER['REQUEST_URI']) === false) {
					throw new Exception("Could not parse URI : {$_SERVER['REQUEST_URI']}");
				}
				$uri = parse_url($uri);
				if (isset($uri['query'])) {
					parse_str($uri['query'], $params);
				} else {
					$params = array();
				}
				$params[RestRequestItem::$START_INDEX] = $nextStartIndex;
				$params[RestRequestItem::$COUNT] = $requestItem->getCount();
				foreach ($params as $paramKey => $paramVal) {
					$outParams[] = $paramKey . '=' . $paramVal;
				}
				$outParams = '?' . implode('&', $outParams);
				$nextUri = 'http://' . $_SERVER['HTTP_HOST'] . $uri['path'] . $outParams;
				// <link rel="next" href="http://api.example.org/..." />
				$link = $entry->appendChild($doc->createElement('link'));
				$linkRel = $link->appendChild($doc->createAttribute('rel'));
				$linkRel->appendChild($doc->createTextNode('next'));
				$linkHref = $link->appendChild($doc->createAttribute('href'));
				$linkHref->appendChild($doc->createTextNode($nextUri));
			}
			
			// Atom fields
			$title = $entry->appendChild($doc->createElement('title'));
			$author = $entry->appendChild($doc->createElement('author'));
			$updated = $entry->appendChild($doc->createElement('updated'));
			$updated->appendChild($doc->createTextNode(date(DATE_ATOM)));
			$id = $entry->appendChild($doc->createElement('id'));
			$id->appendChild($doc->createTextNode('urn:guid:' . $requestItem->getToken()->getDomain() . ':' . htmlentities($requestItem->getUser()->getUserId($requestItem->getToken(), ENT_NOQUOTES, 'UTF-8'))));
			
			// Add response entries to feed
			$responses = $responseItem->getResponse()->getEntry();
			foreach ($responses as $response) {
				$feedEntry = $entry->appendChild($doc->createElement("entry"));
				$type = $feedEntry->appendChild($doc->createElement('content'));
				if ($response instanceof Activity) {
					// Special hoisting rules for activities
					$updated = $feedEntry->appendChild($doc->createElement('updated'));
					$updated->appendChild($doc->createTextNode(date(DATE_ATOM, $response->postedTime)));
					$id = $feedEntry->appendChild($doc->createElement('id'));
					//FIXME these should get proper URL's in the ID and link:
					// <link rel="self" type="application/atom+xml" href="http://api.example.org/activity/feeds/.../af3778"/>
					$id->appendChild($doc->createTextNode('urn:guid:activity:' . htmlentities($response->id, ENT_NOQUOTES, 'UTF-8')));
					$summary = $feedEntry->appendChild($doc->createElement('summary'));
					$summary->appendChild($doc->createTextNode(htmlentities($response->body, ENT_NOQUOTES, 'UTF-8')));
					$title = $feedEntry->appendChild($doc->createElement('title'));
					$title->appendChild($doc->createTextNode(htmlentities($response->title, ENT_NOQUOTES, 'UTF-8')));
					unset($response->id);
					unset($response->title);
					unset($response->body);
					unset($response->postedTime);
				}				
				$content = $this->addData($doc, $type, $requestType, $response, self::$osNameSpace);
				$contentType = $type->appendChild($doc->createAttribute('type'));
				$contentType->appendChild($doc->createTextNode('application/xml'));
				
				// Attempt to have a real ID field, otherwise we fall back on the idSpec id
				$idField = is_object($response) && isset($response->id) ? $response->id : (is_array($response) && isset($response['id']) ? $response['id'] : $requestItem->getUser()->getUserId($requestItem->getToken()));
				
				// Author node
				$author = $feedEntry->appendChild($doc->createElement('author'));
				$authorUrl = $author->appendChild($doc->createElement('uri'));
				$authorUrl->appendChild($doc->createTextNode('urn:guid:' . htmlentities($idField, ENT_NOQUOTES, 'UTF-8')));
				// Updated node, only if it's not an activity (special case)
				if ($response instanceof Activity) {
					$title = $feedEntry->appendChild($doc->createElement('title'));
					$updated = $feedEntry->appendChild($doc->createElement('updated'));
					$updated->appendChild($doc->createTextNode(date(DATE_ATOM)));
					$id = $feedEntry->appendChild($doc->createElement('id'));
					$id->appendChild($doc->createTextNode('urn:guid:' . htmlentities($idField, ENT_NOQUOTES, 'UTF-8')));
				}
			}
		} else {
			// Single entry = Atom:Entry
			$entry = $doc->appendChild($doc->createElementNS(self::$nameSpace, "entry"));
			$type = $entry->appendChild($doc->createElement('content'));
			
			// addData loops through the responseItem data recursively creating a matching XML structure
			// and appends the nodes to the $type element
			$content = $this->addData($doc, $type, $requestType, $data, self::$osNameSpace);
			$contentType = $type->appendChild($doc->createAttribute('type'));
			$contentType->appendChild($doc->createTextNode('application/xml'));
			
			// Atom fields
			$title = $entry->appendChild($doc->createElement('title'));
			$author = $entry->appendChild($doc->createElement('author'));
			$authorUri = $author->appendChild($doc->createElement('uri'));
			$authorUri->appendChild($doc->createTextNode(htmlentities($requestItem->getUser()->getUserId($requestItem->getToken(), ENT_NOQUOTES, 'UTF-8'))));
			$updated = $entry->appendChild($doc->createElement('updated'));
			$updated->appendChild($doc->createTextNode(date(DATE_ATOM)));
			$id = $entry->appendChild($doc->createElement('id'));
			$id->appendChild($doc->createTextNode('urn:guid:' . htmlentities($requestItem->getUser()->getUserId($requestItem->getToken()), ENT_NOQUOTES, 'UTF-8')));
		}
		echo $doc->saveXML();
	
	}

	function outputBatch(Array $responses, SecurityToken $token)
	{
		//TODO once we support spec compliance batching, this needs to be added too
	}

	private function addData(DOMDocument $doc, DOMElement $element, $name, $data, $nameSpace = false)
	{
		if ($nameSpace) {
			$newElement = $element->appendChild($doc->createElementNS($nameSpace, $name));
		} else {
			$newElement = $element->appendChild($doc->createElement($name));
		}
		if (is_array($data)) {
			foreach ($data as $key => $val) {
				if (is_array($val) || is_object($val)) {
					// prevent invalid names.. try to guess a good one :)
					if (is_numeric($key)) {
						$key = is_object($val) ? get_class($val) : $key = $name;
					}
					$this->addData($doc, $newElement, $key, $val);
				} else {
					$elm = $newElement->appendChild($doc->createElement($key));
					$elm->appendChild($doc->createTextNode(htmlentities($val, ENT_NOQUOTES, 'UTF-8')));
				}
			}
		} elseif (is_object($data)) {
			if ($data instanceof Enum) {
				// enums are output as : <NAME key="$key">$displayValue</NAME> 
				$keyEntry = $newElement->appendChild($doc->createAttribute('key'));
				$keyEntry->appendChild($doc->createTextNode(htmlentities($data->key, ENT_NOQUOTES, 'UTF-8')));
				$newElement->appendChild($doc->createTextNode(htmlentities($data->getDisplayValue(), ENT_NOQUOTES, 'UTF-8')));
			
			} else {
				$vars = get_object_vars($data);
				foreach ($vars as $key => $val) {
					if (is_array($val) || is_object($val)) {
						// prevent invalid names.. try to guess a good one :)
						if (is_numeric($key)) {
							$key = is_object($val) ? get_class($val) : $key = $name;
						}
						$this->addData($doc, $newElement, $key, $val);
					} else {
						$elm = $newElement->appendChild($doc->createElement($key));
						$elm->appendChild($doc->createTextNode(htmlentities($val, ENT_NOQUOTES, 'UTF-8')));
					}
				}
			}
		} else {
			$newElement->appendChild($doc->createTextNode(htmlentities($data, ENT_NOQUOTES, 'UTF-8')));
		}
		return $newElement;
	}
}
