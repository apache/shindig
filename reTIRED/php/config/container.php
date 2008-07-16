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

/*
 * Put any site specific configuration in a config/local.php file, this way
 * your configuration won't be lost when upgrading shindig.
 * 
 * in local.php you only have to specificy the fields you want to overwrite
 * with other values, for example on a production system you would probably have:
 * $shindigConfig = array(
 * 	'debug' => false,
 * 	'allow_plaintext_token' => false,
 * 	'token_cipher_key' => 'MySecretKey',
 * 	'token_hmac_key' => 'MyOtherSecret',
 * 	'private_key_phrase' => 'MyCertificatePassword',
 * 	'people_service' => 'MyPeopleService',
 * 	'activity_service' => 'MyActivitiesService',
 * 	'app_data_service' => 'MyAppDataService'
 * );
 *  
 */

$shindigConfig = array(
	// Show debug backtrace's. Disable this on a production site
	'debug' => true,
	// Allow plain text security tokens, this is only here to allow the sample files to work. Disable on a production site
	'allow_plaintext_token' => true,
	// Compress the inlined javascript, saves upto 50% of the document size
	'compress_javascript' => true, 

	// The URL Prefix under which shindig lives ie if you have http://myhost.com/shindig/php set web_prefix to /shindig/php
	'web_prefix' => '', 
	// If you changed the web prefix, add the prefix to these too
	'default_js_prefix' => '/gadgets/js/', 
	'default_iframe_prefix' => '/gadgets/ifr?', 
	
	// The encryption keys for encrypting the security token, and the expiration of it. Make sure these match the keys used in your container/site
	'token_cipher_key' => 'INSECURE_DEFAULT_KEY',
	'token_hmac_key' => 'INSECURE_DEFAULT_KEY', 
	'token_max_age' => 60 * 60, 
	 
	// Ability to customize the style thats injected into the gadget document. Don't forget to put the link/etc colors in shindig/config/container.js too!
	'gadget_css' => 'body,td,div,span,p{font-family:arial,sans-serif;} a {color:#0000cc;}a:visited {color:#551a8b;}a:active {color:#ff0000;}body{margin: 0px;padding: 0px;background-color:white;}',
	
	// P3P privacy policy to use for the iframe document
	'P3P' => 'CP="CAO PSA OUR"', 
	
	// The locations of the various required components on disk. If you did a normal svn checkout there's no need to change these
	'base_path' => realpath(dirname(__FILE__).'/..').'/',
	'features_path' => realpath(dirname(__FILE__) . '/../../features').'/',
	'container_path' => realpath(dirname(__FILE__) . '/../../config').'/',
	'javascript_path' => realpath(dirname(__FILE__) . '/../../javascript').'/',
	'container_config' => realpath(dirname(__FILE__) . '/../../config').'/container.js',

	// The OAuth SSL certificates to use, and the pass phrase for the private key  
	'private_key_file' => realpath(dirname(__FILE__) . '/../certs').'/private.key', 
	'public_key_file' => realpath(dirname(__FILE__) . '/../certs').'/public.crt', 
	'private_key_phrase' => 'partuza', 

	// Force these libraries to be external (included through <script src="..."> tags), this way they could be cached by the browser
	'focedJsLibs' => '', 

	// Configurable classes. Change these to the class name to use, and make sure the auto-loader can find them
	'blacklist_class' => 'BasicGadgetBlacklist', 
	'remote_content' => 'BasicRemoteContent', 
	'security_token_signer' => 'BasicSecurityTokenDecoder', 
	'security_token' => 'BasicSecurityToken', 
	// Caching back-end to use. Shindig ships with CacheFile and CacheMemcache out of the box
	'data_cache' => 'CacheFile',
	// Old-style wire format data handler, this is being depreciated 
	'handlers' => '',
	// New RESTful API data service classes to use
	'people_service' => 'BasicPeopleService',
	'activity_service' => 'BasicActivitiesService',
	'app_data_service' => 'BasicAppDataService',
	// Also scan these directories when looking for <Class>.php files. You can include multiple paths by seperating them with a , 
	'extension_class_paths' => '',
	
	'userpref_param_prefix' => 'up_',
	'libs_param_name' => 'libs', 

	// If you use CacheMemcache as caching backend, change these to the memcache server settings
	'cache_host' => 'localhost',
	'cache_port' => 11211, 	
	'cache_time' => 24 * 60 * 60,
	// If you use CacheFile as caching backend, this is the directory where it stores the temporary files
	'cache_root' => '/tmp/shindig', 

	// If your development server is behind a proxy, enter the proxy details here in 'proxy.host.com:port' format.
	'proxy' => ''
);
