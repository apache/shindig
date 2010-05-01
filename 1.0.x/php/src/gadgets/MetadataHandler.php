<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

class MetadataHandler {

  public function process($requests) {
    $response = array();
    foreach ($requests->gadgets as $gadget) {
      try {
        $gadgetUrl = $gadget->url;
        $gadgetModuleId = $gadget->moduleId;
        $context = new MetadataGadgetContext($requests->context, $gadgetUrl);
        $gadgetServer = new GadgetServer();
        $gadget = $gadgetServer->processGadget($context);
        $response[] = $this->makeResponse($gadget, $gadgetModuleId, $gadgetUrl, $context);
      } catch (Exception $e) {
        $response[] = array('errors' => array($e->getMessage()), 
            'moduleId' => $gadgetModuleId, 'url' => $gadgetUrl);
      }
    }
    return $response;
  }

  private function makeResponse($gadget, $gadgetModuleId, $gadgetUrl, $context) {
    $response = array();
    $prefs = array();
    foreach ($gadget->getUserPrefs() as $pref) {
      $prefs[$pref->getName()] = array('displayName' => $pref->getDisplayName(), 
          'type' => $pref->getDataType(), 'default' => $pref->getDefaultValue(), 
          'enumValues' => $pref->getEnumValues(), 
          'required' => $pref->isRequired());
    }
    $features = array();
    foreach ($gadget->getRequires() as $feature) {
      $features[] = $feature->getName();
    }
    $views = array();
    foreach ($gadget->getViews() as $view) {
      // we want to include all information, except for the content
      unset($view->content);
      $views[$view->getName()] = $view;
    }
    $links = array();
    foreach ($gadget->links as $link) {
      $links[] = $link;
    }
    $icons = array();
    foreach ($gadget->getIcons() as $icon) {
      $icons[] = $icon;
    }
    $oauth = array();
    $oauthspec = $gadget->getOAuthSpec();
    if (! empty($oauthspec)) {
      foreach ($oauthspec->getServices() as $oauthservice) {
        $oauth[$oauthservice->getName()] = array(
            "request" => $oauthservice->getRequestUrl(), 
            "access" => $oauthservice->getAccessUrl(), 
            "authorization" => $oauthservice->getAuthorizationUrl());
      }
    }
    $response['author'] = $gadget->getAuthor();
    $response['authorEmail'] = $gadget->getAuthorEmail();
    $response['description'] = $gadget->getDescription();
    $response['directoryTitle'] = $gadget->getDirectoryTitle();
    $response['features'] = $features;
    $response['screenshot'] = $gadget->getScreenShot();
    $response['thumbnail'] = $gadget->getThumbnail();
    $response['title'] = $gadget->getTitle();
    $response['titleUrl'] = $gadget->getTitleUrl();
    $response['authorAffiliation'] = $gadget->getAuthorAffiliation();
    $response['authorLocation'] = $gadget->getAuthorLocation();
    $response['authorPhoto'] = $gadget->getAuthorPhoto();
    $response['authorAboutme'] = $gadget->getAuthorAboutme();
    $response['authorQuote'] = $gadget->getAuthorQuote();
    $response['authorLink'] = $gadget->getAuthorLink();
    $response['showInDirectory'] = $gadget->getShowInDirectory();
    $response['showStats'] = $gadget->getShowStats();
    $response['width'] = $gadget->getWidth();
    $response['height'] = $gadget->getHeight();
    $response['categories'] = Array($gadget->getCategory(), $gadget->getCategory2());
    $response['singleton'] = $gadget->getSingleton();
    $response['scaling'] = $gadget->getScaling();
    $response['scrolling'] = $gadget->getScrolling();
    $response['links'] = $links;
    $response['views'] = $views;
    $response['icons'] = $icons;
    $response['moduleId'] = $gadgetModuleId;
    $response['url'] = $gadgetUrl;
    $response['iframeUrl'] = UrlGenerator::getIframeURL($gadget, $context);
    $response['userPrefs'] = $prefs;
    $response['oauth'] = $oauth;
    return $response;
  }
}
