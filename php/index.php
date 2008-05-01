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
 */

/*
 * Written by Chris Chabot <chabotc@xs4all.nl> - http://www.chabotc.com
 * 
 * "It is not the strongest of the species that survives, nor the most intelligent that survives. 
 * It is the one that is the most adaptable to change" - Darwin
 * 
 * So in this php version of shindig we make like java and act like a servlet setup, and using
 * it's structures as our reference.
 * 
 * The .htaccess file redirects all requests that are neither an existing file or directory
 * to this index.php, and the $servletMap checks which class is associated with it, if a mapping
 * doesn't exist it display's a 404 error.
 * 
 * See config.php for the global settings and backend class selections.
 * 
 */

include_once ('config.php');

function __autoload($className)
{
	//FIXME this started out like a good idea when we just knew gadgets
	// and it was just 1 directory, now its pretty nuts and probably needs
	// replacing since it's getting dog slow this way
	
	// Check for the presense of this class in our all our directories.
	$fileName = $className.'.php';
	if (file_exists("src/common/$fileName")) {
		    require "src/common/$fileName";
		    
	} elseif (file_exists("src/gadgets/$fileName")) {
		          require "src/gadgets/$fileName";
		          
	} elseif (file_exists("src/gadgets/samplecontainer/$fileName")) {
		          require "src/gadgets/samplecontainer/$fileName";
		          
	} elseif (file_exists("src/gadgets/http/$fileName")) {
		          require "src/gadgets/http/$fileName";
		          
	} elseif (file_exists("src/socialdata/$fileName")) {
		          require "src/socialdata/$fileName";
		          
	} elseif (file_exists("src/socialdata/samplecontainer/$fileName")) {
		          require "src/socialdata/samplecontainer/$fileName";
		           
	} elseif (file_exists("src/socialdata/opensocial/$fileName")) {
		          require "src/socialdata/opensocial/$fileName";
		          
	} elseif (file_exists("src/socialdata/opensocial/model/$fileName")) {
		          require "src/socialdata/opensocial/model/$fileName";
		          
	} elseif (file_exists("src/socialdata/http/$fileName")) {
		          require "src/socialdata/http/$fileName";
	}
}

$servletMap = array(
	Config::get('web_prefix') . '/gadgets/files'    => 'FilesServlet',
	Config::get('web_prefix') . '/gadgets/js'       => 'JsServlet',
	Config::get('web_prefix') . '/gadgets/proxy'    => 'ProxyServlet',
	Config::get('web_prefix') . '/gadgets/ifr'      => 'GadgetRenderingServlet',
	Config::get('web_prefix') . '/gadgets/metadata' => 'JsonRpcServlet',
	Config::get('web_prefix') . '/social/data'      => 'GadgetDataServlet'
);

$servlet = false;
$uri = $_SERVER["REQUEST_URI"];
foreach ($servletMap as $url => $class) {
	if (substr($uri, 0, strlen($url)) == $url) {
		$servlet = $class;
		break;
	}
}
if ($servlet) {	
	$class = new $class();
	if ($_SERVER['REQUEST_METHOD'] == 'POST') {
		$class->doPost();
	} else {
		$class->doGet();
	}
} else {
	// Unhandled event, display simple 404 error
	header("HTTP/1.0 404 Not Found");
	echo "<html><body><h1>404 Not Found</h1></body></html>";
}
