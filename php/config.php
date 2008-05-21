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
 * I really detest such config files to be honest, why put configuration in a web document! 
 * But since PHP lacks a propper way to set application configurations, and any other method 
 * would be horribly slow (db, xml, ini files etc), so ... here's our config.php
 */
$shindigConfig = array(
	// Show debug backtrace? Set this to false on anything that resembles a production env
	'debug' => false,

	// The base prefix under which the our url's live, if its the root set this to ''
	// don't forget to update your .htaccess to reflect this, as well as your container 
	// javascript like: gadget.setServerBase('/someBaseUrl/');
	'web_prefix' => '',

	// Max age of a security token, defaults to one hour
	'st_max_age' => 60 * 60,

	// Security token keys
	'token_cipher_key' => 'INSECURE_DEFAULT_KEY',
	'token_hmac_key' => 'INSECURE_DEFAULT_KEY',

	// Compresses features javascript? can save upto 50% page size
	'compress_javascript' => true,

	// Configurable CSS rules that are injected to the gadget page, 
	// be careful when adjusting these not to break most gadget's layouts :)
	'gadget_css' => 'body,td,div,span,p{font-family:arial,sans-serif;} a {color:#0000cc;}a:visited {color:#551a8b;}a:active {color:#ff0000;}body{margin: 0px;padding: 0px;background-color:white;}',
	//'gadget_css' => 'body,td,div,span,p{font-family:arial,sans-serif;} body {background-color:#ffffff; font-family: arial, sans-serif; padding: 0px; margin: 0px;  font-size: 12px; color: #000000;}a, a:visited {color: #3366CC;text-decoration: none; }a:hover {color: #3366CC; text-decoration: underline;} input, select { border: 1px solid #bdc7d8;font-size: 11px;padding: 3px;}',
	
	// The html / javascript samples use a plain text demo token,
	// set this to false on anything resembling a real site
	'allow_plaintext_token' => true,
	
	// P3P (Platform for Privacy Preferences) header for allowing cross domain cookies.
	// Setting this to an empty string: '' means no P3P header will be send
	'P3P' => 'CP="CAO PSA OUR"',

	// location of the features directory on disk. The default setting assumes you did a 
	// full checkout of the shindig project, and not just the php part.
	// Otherwise also checkout the features, config and javascript directories and set
	// these to their locations
	'features_path' => realpath(dirname(__FILE__)) . '/../features/',
	'container_path' => realpath(dirname(__FILE__)) . '/../config/',
	'javascript_path' => realpath(dirname(__FILE__)) . '/../javascript/', 
	'container_config' => realpath(dirname(__FILE__)) . '/../config/container.js',

	// The data handlers for the social data, this is a list of class names
	// seperated by a , For example:
	//'handlers' => 'PartuzaHandler',
	// if the value is empty, the defaults used in the example above will be used.
	'handlers' => '',

	'focedJsLibs' => '',
	
	// Configurable classes to use, this way we provide extensibility for what 
	// backends the gadget server uses for its logic functionality. 
	'blacklist_class' => 'BasicGadgetBlacklist',
	'remote_content' => 'BasicRemoteContent',
	'security_token_signer' => 'BasicSecurityTokenDecoder',
	'security_token' => 'BasicSecurityToken',
	'data_cache' => 'CacheFile', 
	
	// gadget server specific settings
	'userpref_param_prefix' => 'up_',
	'libs_param_name' => 'libs',
	// location  of the javascript handler (include the full path), default this is /gadgets/js
	'default_js_prefix' => '/gadgets/js/',
	// location of the gadget iframe renderer, default this is /gadgets/ifr?
	'default_iframe_prefix' => '/gadgets/ifr?', 
	
	// if your using memcached, these values are used for locating the server
	// if your not using memcached, ignore these values
	'cache_host' => 'localhost',
	'cache_port' => 11211, 
	
	// global cache age policy and location
	'cache_time' => 24 * 60 * 60,
	'cache_root' => '/tmp/shindig', 

	// OAuth private key Path
	'private_key_file' => realpath(dirname(__FILE__)) . '/certs/private.key',
	// file path to public RSA cert
	'public_key_file' => realpath(dirname(__FILE__)) . '/certs/public.crt',
	// Phrase to decrypt private key. Leave empty if unencrypted
	'private_key_phrase' => 'partuza',
	
	// In some cases we need to know the site root (for features forinstance)
	'base_path' => realpath(dirname(__FILE__))
);

class ConfigException extends Exception {}

/**
 * Abstracts how to retrieve configuration values so we can replace the
 * not so pretty $shindigConfig array some day.
 */
class Config {
	static function get($key)
	{
		global $shindigConfig;
		if (isset($shindigConfig[$key])) {
			return $shindigConfig[$key];
		} else {
			throw new ConfigException("Invalid Config Key");
		}
	}

	static function set($key, $val)
	{
		global $shindigConfig;
			if (isset($shindigConfig[$key])) {
			$shindigConfig[$key] = $val;
		} else {
			throw new ConfigException("Invalid Config Key");
		}
	}
}
