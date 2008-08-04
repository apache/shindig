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
 * see
 * http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Enum.html
 *
 * Base class for all Enum objects. This class allows containers to use constants
 * for fields that have a common set of values.
 *
 */

abstract class Enum {
	public $displayValue;
	public $key;
	public $values = array();

	public function __construct($key, $displayValue = '')
	{
		//FIXME should add enum restriction checking to this
		if (! isset($this->values[$key])) {
			throw new Exception("Invalid Enum key");
		}
		$this->key = $key;
		$this->displayValue = ! empty($displayValue) ? $displayValue : $this->values[$key];
		unset($this->values);
	}

	public function getDisplayValue()
	{
		return $this->displayValue;
	}

	public function setDisplayValue($displayValue)
	{
		$this->displayValue = $displayValue;
	}

	public function toString()
	{
		return $this->jsonString;
	}
}

/**
 * public Enum for opensocial.Enum.Drinker
 */
class EnumDrinker extends Enum {
	public $values = array('HEAVILY' => "Heavily", 'NO' => "No", 'OCCASIONALLY' => "Occasionally", 
			'QUIT' => "Quit", 'QUITTING' => "Quitting", 'REGULARLY' => "Regularly", 
			'SOCIALLY' => "Socially", 'YES' => "Yes");
}

/**
 * public Enum for opensocial.Enum.Gender
 */
class EnumGender extends Enum {
	public $values = array('FEMALE' => "Female", 'MALE' => "Male");
}

/**
 * public Enum for opensocial.Enum.Smoker
 */
class EnumSmoker extends Enum {
	public $values = array('HEAVILY' => "Heavily", 'NO' => "No", 'OCCASIONALLY' => "Ocasionally", 
			'QUIT' => "Quit", 'QUITTING' => "Quitting", 'REGULARLY' => "Regularly", 
			'SOCIALLY' => "Socially", 'YES' => "Yes");
}

/**
 * public Enum for opensocial.Enum.Presence
 */
class EnumPresence extends Enum {
	public $values = array('AWAY' => "Away", 'CHAT' => "Chat", 'DND' => "Do Not Disturb", 
			'OFFLINE' => "Offline", 'ONLINE' => "Online", 'XA' => "Extended Away");
}
