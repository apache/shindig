<?
/*
 * I really detest such config files to be honest, why put configuration in a web document! 
 * But since PHP lacks a propper way to set application configurations, and any other method 
 * would be horribly slow (db, xml, ini files etc), so ... here's our config.php
 */

$config = array(
	// The base prefix under which the gadget url's live, if its the root set this to ''
	// don't forget to update your .htaccess to reflect this, as well as your container 
	// javascript like: gadget.setServerBase('/gadgets/');
	'web_prefix' => '/gadgets',

	// location of the features directory on disk. The default setting assumes you did a 
	// full checkout of the shindig project, and not just the php part.
	// Otherwise also checkout the features, config and javascript directories and set
	// these to their locations
	'features_path' =>  realpath(dirname(__FILE__)).'/../../features/',
	'container_path' =>  realpath(dirname(__FILE__)).'/../../config/',
	'javascript_path' =>  realpath(dirname(__FILE__)).'/../../javascript/',

	// If you want to use the yuicompressor (http://developer.yahoo.com/yui/compressor/) to minify your javascript
	// set this to the yuicompressor-?.jar's file path
	//NOTE: not used yet, will be in the next patch
	'compressor_path' => realpath(dirname(__FILE__)).'/bin/yuicompressor-2.3.5.jar',

	// Configurable classes to use, this way we provide extensibility for what 
	// backends the gadget server uses for its logic functionality. 
	'blacklist_class' => 'BasicGadgetBlacklist',
	'remote_content' => 'BasicRemoteContent',
	'gadget_signer' => 'BasicGadgetSigner',
	'gadget_token' => 'BasicGadgetToken',
	'data_cache' => 'CacheFile',

	// gadget server specific settings
	'userpref_param_prefix'=> 'up_',
	'libs_param_name'=> 'libs',
	'default_js_prefix'=> '/js/',
	'default_iframe_prefix'=> 'ifr?',
	
	// show debugging output ?
	'debug'=> true,

	// if your using memcached, these values are used for locating the server
	// if your not using memcached, ignore these values
	'cache_host' => 'localhost',
	'cache_port'   => 11211,
	
	// global cache age policy and location
	'cache_time'=> 24*60*60,
	'cache_root'=> '/tmp/shindig',
	
	// In some cases we need to know the site root (for features forinstance)
	'base_path'=> realpath(dirname(__FILE__))
);
