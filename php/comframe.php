<php?
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
<html>
<head></head>
<body style="border:0;padding:0;margin:0">
<?php
    require_once 'crypto.php';
    $ONE_HOUR = (60 * 60);
    $values = array();
    $values['url'] =  MODULE_URL;
    $values['owner'] = OWNER;
    $values['expires'] = time() + $ONE_HOUR;
    $values['viewer'] = VIEWER;
    $xark = CryptoHelper::encrypt(CryptoHelper::appPrivateKey(), 
                                  CryptoHelper::appModulo(), $values);

    $mode = $_GET['mode'] == 'canvas' ? 'canvas' : 'profile';
?>
<iframe id="gadgetFrame" name="gadgetFrame" scrolling="auto" frameborder="0"  style="border:0;padding:0;margin:0;width:100%;height:100%;overflow:auto"
src="http://<?php echo PROXY_HOST ?>/container.php?xark=<?php echo $xark ?>&amp;mode=<?php echo $mode; ?>&amp;origin=<?php echo SITE_HOST ?>&amp;location=<?php echo urlencode("http://" . SITE_HOST . "/comframe.php"); ?>">
</iframe>
