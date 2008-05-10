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

class JsLibrary {
	private $types = array('FILE', 'RESOURCE', 'URL', 'INLINE');
	private $type;
	private $content;
	private $featureName; // used to track what feature this belongs to
	private $loaded = false;
	
	public function __construct($type, $content, $featureName = '')
	{
		$this->featureName = $featureName;
		$this->type = $type;
		$this->content = $content;
	}

	public function getType()
	{
		return $this->type;
	}
	
	public function readfile()
	{
		// hack to bypass having the script in memory and dump it directly to stdout
		if (!$this->loaded && $this->type == 'FILE') {
			readfile($this->content);
			echo "\n";
		} else {
			// a call to getContent was already made so we have it in memory, just echo it then
			echo $this->content;
		}
	}

	public function getContent()
	{
		if (!$this->loaded && $this->type == 'FILE') {
			$this->content = JsLibrary::loadData($this->content, $this->type);
			$this->loaded = true;
		}
		return $this->content;
	}

	public function getFeatureName()
	{
		return $this->featureName;
	}

	public function toString()
	{
		if ($this->type == 'URL') {
			return "<script src=\"" . $this->getContent() . "\"></script>";
		} else {
			return "<script><!--\n" . $this->getContent() . "\n--></script>";
		}
	}

	static function create($type, $content)
	{
		$feature = '';
		if ($type == 'FILE') {
			$feature = dirname($content);
			if (substr($feature, strlen($feature) - 1, 1) == '/') {
				// strip tailing /, if any, so that the following strrpos works in any situation
				$feature = substr($feature, 0, strlen($feature) - 1);
			}
			$feature = substr($feature, strrpos($feature, '/') + 1);
		}
		return new JsLibrary($type, $content, $feature);
	}

	static private function loadData($name, $type)
	{
		// we don't really do 'resources', so limiting this to files only
		if ($type == 'FILE') {
			return JsLibrary::loadFile($name);
		}
		return null;
	}

	static private function loadFile($fileName)
	{
		if (empty($fileName)) {
			return '';
		}
		if (! file_exists($fileName)) {
			throw new Exception("JsLibrary file missing: $fileName");
		}
		if (! is_file($fileName)) {
			throw new Exception("JsLibrary file is not a file: $fileName");
		}
		if (! is_readable($fileName)) {
			throw new Exception("JsLibrary file not readable: $fileName");
		}
		if (! ($content = @file_get_contents($fileName))) {
			throw new Exception("JsLibrary error reading file: $fileName");
		}
		return $content;
	}
}