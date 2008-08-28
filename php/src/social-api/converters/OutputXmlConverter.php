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
 * Format = xml output converter, for format definition see:
 * http://docs.google.com/View?docid=dcc2jvzt_37hdzwkmf8
 */
class OutputXmlConverter extends OutputConverter {
	private static $xmlVersion = '1.0';
	private static $charSet = 'UTF-8';
	private static $formatOutput = true;
	
	// this maps the REST url to the xml tags
	private static $entryTypes = array('people' => 'person', 'appdata' => 'appdata', 
			'activities' => 'activity', 'messages' => 'messages');
	private $doc;

	function outputResponse(ResponseItem $responseItem, RestRequestItem $requestItem)
	{
		$doc = $this->createXmlDoc();
		$requestType = $this->getRequestType($requestItem);
		$data = $responseItem->getResponse();
		
		// Check to see if this is a single entry, or a collection, and construct either an xml 
		// feed (collection) or an entry (single)		
		if ($responseItem->getResponse() instanceof RestFulCollection) {
			$totalResults = $responseItem->getResponse()->getTotalResults();
			$itemsPerPage = $requestItem->getCount();
			$startIndex = $requestItem->getStartIndex();
			
			// The root Feed element
			$entry = $this->addNode($doc, 'response', '');
			
			// Required Xml fields
			$this->addNode($entry, 'startIndex', $startIndex);
			$this->addNode($entry, 'itemsPerPage', $itemsPerPage);
			$this->addNode($entry, 'totalResults', $totalResults);
			$responses = $responseItem->getResponse()->getEntry();
			foreach ($responses as $response) {
				// recursively add responseItem data to the xml structure
				$this->addData($entry, $requestType, $response);
			}
		} else {
			// Single entry = Xml:Entry	
			$entry = $this->addNode($doc, 'response', '');
			// addData loops through the responseItem data recursively creating a matching XML structure
			$this->addData($entry, $requestType, $data);
		}
		$xml = $doc->saveXML();
		echo $xml;
	}

	function outputBatch(Array $responses, SecurityToken $token)
	{
		$this->boundryHeaders();
		foreach ($responses as $response) {
			$request = $response['request'];
			$response = $response['response'];
			// output buffering supports multiple levels of it.. it's a nice feature to abuse :)
			ob_start();
			$this->outputResponse($response, $request);
			$part = ob_get_contents();
			ob_end_clean();
			$this->outputPart($part, $response->getError());
		}
	}

	/**
	 * Easy shortcut for creating & appending XML nodes
	 *
	 * @param DOMElement $node node to append the new child node too
	 * @param string $name name of the new element
	 * @param string $value value of the element, if empty no text node is created
	 * @param array $attributes optional array of attributes, false by default. If set attributes are added to the node using the key => val pairs
	 * @param string $nameSpace optional namespace to use when creating node
	 * @return DOMElement node
	 */
	private function addNode($node, $name, $value = '', $attributes = false)
	{
		$childNode = $node->appendChild($this->doc->createElement($name));
		if (! empty($value) || $value == '0') {
			$childNode->appendChild($this->doc->createTextNode($value));
		}
		if ($attributes && is_array($attributes)) {
			foreach ($attributes as $attrName => $attrVal) {
				$childNodeAttr = $childNode->appendChild($this->doc->createAttribute($attrName));
				if (! empty($attrVal)) {
					$childNodeAttr->appendChild($this->doc->createTextNode($attrVal));
				}
			}
		}
		return $childNode;
	}

	/**
	 * Creates the root document using our xml version & charset
	 *
	 * @return DOMDocument
	 */
	private function createXmlDoc()
	{
		$this->doc = new DOMDocument(self::$xmlVersion, self::$charSet);
		$this->doc->formatOutput = self::$formatOutput;
		return $this->doc;
	}

	/**
	 * Extracts the Xml entity name from the request url
	 *
	 * @param RequestItem $requestItem the request item
	 * @return string the request type
	 */
	private function getRequestType($requestItem)
	{
		// map the Request URL to the content type to use  
		$params = $requestItem->getParameters();
		if (! is_array($params) || empty(self::$entryTypes[$params[0]])) {
			throw new Exception("Unsupported request type");
		}
		return self::$entryTypes[$params[0]];
	}

	/**
	 * Recursive function that maps an data array or object to it's xml represantation 
	 *
	 * @param DOMDocument $doc the root document
	 * @param DOMElement $element the element to append the new node(s) to
	 * @param string $name the name of the to be created node
	 * @param array or object $data the data to map to xml
	 * @param string $nameSpace if specified, the node is created using this namespace
	 * @return DOMElement returns newly created element
	 */
	private function addData(DOMElement $element, $name, $data)
	{
		$newElement = $element->appendChild($this->doc->createElement($name));
		if (is_array($data)) {
			foreach ($data as $key => $val) {
				if (is_array($val) || is_object($val)) {
					// prevent invalid names.. try to guess a good one :)
					if (is_numeric($key)) {
						$key = is_object($val) ? get_class($val) : $key = $name;
					}
					$this->addData($newElement, $key, $val);
				} else {
					if (is_numeric($key)) {
						$key = is_object($val) ? get_class($val) : $key = $name;
					}
					$elm = $newElement->appendChild($this->doc->createElement($key));
					$elm->appendChild($this->doc->createTextNode($val));
				}
			}
		} elseif (is_object($data)) {
			if ($data instanceof Enum) {
				// enums are output as : <NAME key="$key">$displayValue</NAME> 
				$keyEntry = $newElement->appendChild($this->doc->createAttribute('key'));
				$keyEntry->appendChild($this->doc->createTextNode($data->key));
				$newElement->appendChild($this->doc->createTextNode($data->getDisplayValue()));
			
			} else {
				$vars = get_object_vars($data);
				foreach ($vars as $key => $val) {
					if (is_array($val) || is_object($val)) {
						// prevent invalid names.. try to guess a good one :)
						if (is_numeric($key)) {
							$key = is_object($val) ? get_class($val) : $key = $name;
						}
						$this->addData($newElement, $key, $val);
					} else {
						$elm = $newElement->appendChild($this->doc->createElement($key));
						$elm->appendChild($this->doc->createTextNode($val));
					}
				}
			}
		} else {
			$newElement->appendChild($this->doc->createTextNode($data));
		}
		return $newElement;
	}
}
