<?
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
	
	public function __construct($type, $content)
	{
		$this->type = $type;
		$this->content = $content;
	}
	
	public function getType()
	{
		return $this->type;
	}
	
	public function getContent()
	{
		return $this->content;
	}
	
	public function toString()
	{
		if ($this->type == 'URL') {
			return "<script src=\"" . $this->content . "\"></script>";
		} else {
			return "<script><!--\n" . $this->content . "\n--></script>";
		}
	}
	
	static function create($type, $content)
	{
		if ($type == 'FILE' || $type == 'RESOURCE') {
			$content = JsLibrary::loadData($content, $type);
		}
		return new JsLibrary($type, $content);
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
		if (!file_exists($fileName)) {
			throw new Exception("JsLibrary file missing: $fileName");
		}
		if (!is_file($fileName)) {
			throw new Exception("JsLibrary file is not a file: $fileName");
		}
		if (!is_readable($fileName)) {
			throw new Exception("JsLibrary file not readable: $fileName");
		}
		if (!($content = @file_get_contents($fileName))) {
			throw new Exception("JsLibrary error reading file: $fileName");
		}
		return $content;
	}
}