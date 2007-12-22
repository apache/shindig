<?php 
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
?><?php require_once 'config.php' ?>
<?php
// require_once NF_APP_BASE . '/lib/CryptoHelper.php';
require_once 'crypto.php';

function errorPage($code, $message) {
    header("HTTP/1.1 $code Error");
    echo $message;
}

// need to require libcurl stuff!
function do_curl_request($url, $postcontents = null) {
    error_log("Fetching $url");
    $curl = curl_init();
    curl_setopt($curl,CURLOPT_URL,$url);
    if ($postcontents) {
	    curl_setopt($curl,CURLOPT_POST,true);
		curl_setopt($curl,CURLOPT_POSTFIELDS,$postcontents);
    }
    else {
        curl_setopt($curl,CURLOPT_GET,true);
    }
    curl_setopt($curl,CURLOPT_USERAGENT,"Mozilla/4.0 (Compatible; Shindig Remote API)");
    curl_setopt($curl, CURLOPT_TIMEOUT, 30);
	curl_setopt($curl, CURLOPT_MAXREDIRS, 5);
    ob_start();
    	$result = curl_exec($curl);

        $errno = null;
        if ($result == false) {
            $errno = curl_errno($curl);
            error_log("Error fetching $url : $errno");
        }
        
    	$data = ob_get_contents();
    	ob_end_clean();
    	
    	$retcode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    curl_close($curl);
    return array('code' => $retcode, 'data' => $data, 'errno' => $errno);
}

// $appOrigin = $_GET['origin'];
// if (preg_match('@[\W]@', $appOrigin)) {
//     errorPage(400, 'Bogus origin param: ' . $appOrigin);
//     return;
// }

// $request = do_curl_request('http://' . $appOrigin  . '/gadgets/index/publicKey');
// if ($request['errno']) {
//     errorPage(500, 'Error fetching content');
//     return;
// }
$publicKey =  array("key" => CryptoHelper::appPublicKey(),
                    "modulo" => CryptoHelper::appModulo());

$xark = $_GET['xark'];
$decryptedData = CryptoHelper::decrypt($publicKey['key'], $publicKey['modulo'], $xark);
if (!is_array($decryptedData)) {
    errorPage(400, 'Bogus xark param ' . $decryptedData);
    return;
}

if (time() > $decryptedData['expires']) {
    errorPage(403, 'Gadget session has expired.  Please reload the page.');
    return;
}

$iframeParentLocation = $_GET['location'];

$url = $decryptedData['url'];
$gadgetUrl = $url;
$embedOwnerName = $decryptedData['owner'];
if (isset($decryptedData['viewer']) && $decryptedData['viewer'] !== '') { 
    $loggedInUser = $decryptedData['viewer'];
}


error_log('gadgetWrapper: ----------STARTING CALL-------------------------------------');
error_log('gadgetWrapper: url = ' . $url);
error_log('gadgetWrapper: referer = ' . $_SERVER['HTTP_REFERER']);


error_log('gadgetWrapper: starting ext request: ' . time());
$request = do_curl_request($url);
if ($request['errno']) {
    errorPage(500, 'Error fetching gadget XML from ' . $url);
    return;
}
$xml = $request['data'];
$retcode = $request['code'];
error_log('gadgetWrapper: ext request DONE: ' . time());


//$xml = simplexml_load_file($url) or die("gadget not loading");
//$xml = file_get_contents($url) or die("gadget not loading");

class Simple_Parser
{
    var $parser;
    var $error_code;
    var $error_string;
    var $current_line;
    var $current_column;
    var $data = array();
    var $datas = array();
   
    function parse($data)
    {
        $this->parser = xml_parser_create('UTF-8');
        xml_set_object($this->parser, $this);
        xml_parser_set_option($this->parser, XML_OPTION_SKIP_WHITE, 1);
        xml_set_element_handler($this->parser, 'tag_open', 'tag_close');
        xml_set_character_data_handler($this->parser, 'cdata');
        if (!xml_parse($this->parser, $data))
        {
            $this->data = array();
            $this->error_code = xml_get_error_code($this->parser);
            $this->error_string = xml_error_string($this->error_code);
            $this->current_line = xml_get_current_line_number($this->parser);
            $this->current_column = xml_get_current_column_number($this->parser);
        }
        else
        {
            $this->data = $this->data['child'];
        }
        xml_parser_free($this->parser);
    }

    function tag_open($parser, $tag, $attribs)
    {
        $this->data['child'][$tag][] = array('data' => '', 'attribs' => $attribs, 'child' => array());
        $this->datas[] =& $this->data;
        $this->data =& $this->data['child'][$tag][count($this->data['child'][$tag])-1];
    }

    function cdata($parser, $cdata)
    {
        $this->data['data'] .= $cdata;
    }

    function tag_close($parser, $tag)
    {
        $this->data =& $this->datas[count($this->datas)-1];
        array_pop($this->datas);
    }
}

$xml_parser = new Simple_Parser;
$xml_parser->parse($xml);
//print_r($xml_parser->data['MODULE'][0]['child']['CONTENT'][0]['data']);

$gadgetType = $xml_parser->data['MODULE'][0]['child']['CONTENT'][0][attribs]['TYPE'];
$gadgetHeight = $xml_parser->data['MODULE'][0]['child']['MODULEPREFS'][0][attribs]['HEIGHT'];
$moduleName = $xml_parser->data['MODULE'][0]['child']['MODULEPREFS'][0][attribs]['TITLE'];
//$moduleName = implode(' ', $moduleName);
//error_log('mod name=' . $moduleName);
if (!$gadgetHeight) {
	if ($_GET['mode'] == 'canvas') {
		$gadgetHeight = 400;
	}
	else {//profile view
		$gadgetHeight = 200;
	}
}

$user = $embedOwnerName;
$app = $moduleName;
$appUrlMd5 = base64_encode(md5($url));

function getPersonData($user, $appUrlMd5, $xark, $appOrigin) {
    // $postcontents = array();
    // $postcontents['user'] = $user;
    // $postcontents['op'] = 'get-app-data';
    // $postcontents['app'] = $appUrlMd5;
    // $postcontents['xark'] = $xark;
    // $postcontents['origin'] = $appOrigin;
    //     
    //     $request = do_curl_request('http://' . $appOrigin . XN_AtomHelper::$DOMAIN_SUFFIX . '/gadgets/index/backendApi', $postcontents);
    // return $request['data'];
    return "({})";
}

$preloadedUserData = getPersonData($embedOwnerName, $appUrlMd5, $xark, $appOrigin);	
if (isset($loggedInUser)) {
	$preloadedViewerData = getPersonData($loggedInUser, $appUrlMd5, $xark, $appOrigin);	
}
else {
	$preloadedViewerData = '{}';
}

error_log('gadgetWrapper: data preload (owner): '.$preloadedUserData);	
error_log('gadgetWrapper: data preload (viewer): '.$preloadedViewerData);	

function getObjKey($user, $app)
{
	return $user . '-' . $app;
}

function getGadgetAppDataQuery($user, $app)
{
	global $appOrigin;
	$objkey = getObjKey($user, $app);
	$contentQuery = XN_Query::create('Content');
	$contentQuery->filter('owner->relativeUrl', '=', $appOrigin);
	$contentQuery->filter('type', '=', 'GadgetAppData');
	$contentQuery->filter('title', '=', $objkey);
	error_log($contentQuery->debugHtml());
	error_log('apporigin = ' . $appOrigin);
	return $contentQuery;
}
////-----------------HARDWIRED FOR ILIKE/DEMO		

if ($gadgetType == 'url') {
	$gadgetUrl = $xml_parser->data['MODULE'][0]['child']['CONTENT'][0][attribs]['HREF'];	
?>
<iframe frameborder="0" width="340" style="padding: 0; margin: 0;" height="<?php echo $gadgetHeight; ?>" 
src="<?php echo $gadgetUrl; ?>">
</iframe>
<?php
}
else { //it's an HTML gadget
	$moduleContent = $xml_parser->data['MODULE'][0]['child']['CONTENT'][0]['data'];
?>

<script type="text/javascript" src="/js/hash.js"></script>
<script type="text/javascript" src="/js/json.js"></script>
<script type="text/javascript" src="/js/jquery.js"></script>
<script type="text/javascript" src="/js/People.js"></script>
<script type="text/javascript" src="/js/ShindigContainer.js"></script>
<script type="text/javascript" src="http://o.aolcdn.com/dojo/0.4.2/dojo.js"></script>

<script type="text/javascript">
window.magic_shindig = 'Shindig';

shindig = {};
shindig._ = shindig._ || {};
shindig._.os = {
	'xark': '<?php echo $xark; ?>',
	'origin': '<?php echo $appOrigin; ?>'
}

if(!window.console){
	var console = {
        init:function(){},
		hide:function(){},
		show:function(){},
		log:function(o){},
		clear:function(){},
		addLoadEvent:function(func){}
	};
}

var onloadHandler = null;
var completedLoading = false;
var preloadedUserData = <?php echo $preloadedUserData ? $preloadedUserData : null ?>;
var preloadedViewerData = <?php echo $preloadedViewerData ? $preloadedViewerData : null ?>;
var hashAppName = '<?php echo $appUrlMd5 ?>';   

function _IG_RegisterOnloadHandler(fun) {
	onloadHandler = fun;
}                                                      

opensocial.registerOnloadHandler = function(fun) {
	onloadHandler = fun;
};

function _gel(data) {
	return document.getElementById(data);
}

function _IG_FetchContent(urlx, callback, refresh) {
	opensocial.fetchContent(urlx, callback, refresh);
}

opensocial.fetchContent = function(urlx, callback, refresh) {
	console.log('proxy call URL='+urlx);
		dojo.io.bind({url: "/proxy.php?op=proxy&url="+escape(urlx),
				 load: function(type,data,evt) { console.log('Received data on IG_Fetch'); callback(data); }, 
				 mimetype: "text/plain" }	    
		);
}

_IG_Tabs = function(module_id, opt_selected_tab) {}
_IG_Tabs.prototype.alignTabs = function(location, space) {}
_IG_Tabs.prototype.addTab = function(tabName, opt_domId, opt_callback) {}
_IG_Tabs.prototype.addDynamicTab = function(tabName, callback) {}
_IG_Tabs.prototype.setSelectedTab = function(tabIndex) {}
_IG_Tabs.prototype.moveTab = function(tabIndex1, tabIndex2) {}
_IG_Tabs.prototype.numTabs = function() {}
_IG_Tabs.prototype.currentTab = function() {};


function _IG_Analytics(a,b) {}; 

__MODULE_ID__ = 10;

 _IG_Prefs = function() {
console.log('preloaded data = ');
console.log(preloadedUserData);
console.log('END preloaded data = ');
 	
 	this.data = preloadedUserData || new Array();
 	
 }
 
 _IG_Prefs.prototype.set = function(key, value) {
 	if (xnGetViewer().getId() != xnGetOwner().getId()) {
 		//don't allow writes if the person logged in is not the owner.
 		return;
 	}
 	onSuccess = function(evaldObj) {

 		console.log('success!');
 	};
 	onError = function(context, msg) {
 		console.log('error = ' + msg);
 	};
 	var handlers = {
 		success: onSuccess,
 		failure: onError
 	};
 	var userid = xnGetOwner().getId();
 	//var form = new Array();
 	content = {'user':  userid, 'op':'update-app-data', 'app': opensocial.Container.get().getAppName(), 'key': key, 'value': value, 'xark': shindig._.os.xark, 'origin': shindig._.os.origin};
 	console.log('updatedata = '+content);
 	console.log(content);
 	shindig.api.post("/gadgets/index/api", content, handlers);

 	this.data[key] = value;
 	
 }

 _IG_Prefs.prototype.getString = function(key) {
 	return this.data[key];
 }

 
 _IG_Prefs.prototype.getBool = function(key) {
 	return Boolean(this.data[key]);
 }                                  

 function  _IG_AdjustIFrameHeight()
 {
	opensocial.adjustIFrameHeight();
 }

 opensocial.adjustIFrameHeight = function()
 {
     <?php if ($iframeParentLocation && strpos($iframeParentLocation, '\\') === FALSE && strpos($iframeParentLocation, '\'') === FALSE) { ?>
         var preferredHeight = document.getElementById('__os_gadget_body').scrollHeight;
         if (preferredHeight > <?php echo $gadgetHeight ? $gadgetHeight : 200; ?>) {
             parent.location = '<?php echo $iframeParentLocation; ?>#__os_iframe_height_' + preferredHeight + '__';
         }
    <?php } ?>
 }
 
<?php

function getFriendsJSON($screenName)
{
	$request = do_curl_request("http://" . $_SERVER['HTTP_HOST'] . "/xn/rest/1.0/profile:" . $screenName . "/contact(relationship='friend'&onNing='true')?begin=0&end=25&xn_auth=no");
    $jsonText = $request['data'];
	return $jsonText ? $jsonText : '{}';
}

$user = 'brianm'; //isset($loggedInUser) ? XN_Profile::load($loggedInUser) : XN_Profile::current();
$owner = null;
$userFriends = '{}';
$ownerFriends = '{}';

$viewerIsAnonymous = true;
$viewerIsOwner = false;
error_log('gadgetWrapper: embed owner X='.$embedOwnerName);
if (isset($loggedInUser)) {
	//load friends for this user
	//
error_log('gadgetWrapper: user is logged in ');
	$userFriends = getFriendsJSON($user->screenName);

	$viewerIsAnonymous = false;
	if ($embedOwnerName == $user->screenName) {
		$owner = $user;
		$viewerIsOwner = true;
		$ownerFriends = $userFriends;
	}
}

if ($owner == null) {
	//if we got here either we are not logged in, or the viewer != owner, so we load the owner 
	$owner = 'brianm'; //XN_Profile::load($embedOwnerName);
	$ownerFriends = getFriendsJSON($embedOwnerName);
}

error_log('gadgetWrapper: done getting remote data for ' . $url);
?>
        		//todo load the app data
var xnOwner = new opensocial.ShindigPerson('brianm', 
                                        'Brian McCallister', 
                                        '', 
                                        'http://api.ning.com/files/iLsNX-7w48WXQ7TwAtd6*9TmG2Ajf3mdiGnNZp7ZIvQ_/8464126.bin?width=32&height=32', 
                                        '', 
                                        new Array(),
                                        '');
var xnOwnerFriends = '{}'; //<?php  // echo $ownerFriends ?>;
var xnViewerFriends = '{}'; //<?php // echo $userFriends ?>;

<?php
if ($viewerIsAnonymous) {
?>
var xnViewer = new opensocial.ShindigPerson('xn_anonymous', 'Anonymous', '', '', '', new Array(),'');
<?php
}
else {
	if ($viewerIsOwner) {
?>
var xnViewer = xnOwner;		
<?php
	}
	else {
?>
var xnViewer = new opensocial.ShindigPerson('<%= $user->screenName %>', '<%= $user->fullName %>', '', '<%= $user->thumbnailUrl(32,32); %>', '', new Array(),'');	
<?php
	}
}
?>


function xnGetViewer() {
	
	return xnViewer;
}

function xnGetOwner() {
	
	return xnOwner;
}

function xnGetViewerFriends() {
	
	return xnViewerFriends;
}

function xnGetOwnerFriends() {
	
	return xnOwnerFriends;
}

opensocial.Container.get().init(hashAppName);

</script>
<div id="__os_gadget_body">
<?php 

$moduleContent = str_replace('<script src="http://sandbox.orkut.com/js/gen/People.js"></script>', '', $moduleContent);
$moduleContent = str_replace('<script type="text/javascript" src="http://sandbox.orkut.com/js/gen/People.js"></script>', '', $moduleContent);
//$moduleContent = str_replace('params += "&f=" + person.getId();', 'params += "&f=" + person.obj_.getId();', $moduleContent);
$moduleContent = str_replace('http://sandbox.orkut.com/Application.aspx?appId=918178148210', 
									'/gadgets/index/canvas?user=' . $owner->screenName . '&feedUrl=' . urlencode($gadgetUrl) . ((!$moduleName || $moduleName == '' ? '' : ('&title=' . urlencode($moduleName)))), $moduleContent);

//echo 'TEST: <a target="_top" href="' . '/gadgets/index/canvas?user=' . $owner->screenName . '&feedUrl=' . urlencode($gadgetUrl) . ((!$moduleName || $moduleName == '' ? '' : ('&title=' . urlencode($moduleName)))) . '">test canvas link</a><br/>';
print $moduleContent ?>

<script type="text/javascript">
//call the onload handlers
	if (onloadHandler != null) {
		onloadHandler();
	}
</script>

<?php
	
}
?>
</div>
