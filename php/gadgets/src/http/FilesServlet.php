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

class FilesServlet extends HttpServlet {
	
	public function doGet()
	{
		global $config;
		$file = str_replace($config['web_prefix'] . '/files/', '', $_SERVER["REQUEST_URI"]);
		$file = $config['javascript_path'] . $file;
		// make sure that the real path name is actually in the javascript_path, so people can't abuse this to read
		// your private data from disk .. otherwise this would be a huge privacy and security issue 
		if (substr(realpath($file), 0, strlen(realpath($config['javascript_path']))) != realpath($config['javascript_path'])) {
			header("HTTP/1.0 400 Bad Request", true);
			echo "<html><body><h1>400 - Bad Request</h1></body></html>";
			die();
		}
		// if the file doesn't exist or can't be read, give a 404 error
		if (!file_exists($file) || !is_readable($file) || !is_file($file)) {
			header("HTTP/1.0 404 Not Found", true);
			echo "<html><body><h1>404 - Not Found</h1></body></html>";
			die();
		}
		readfile($file);
	}
}
