<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\servlet\ResourcesFilesServlet;
use apache\shindig\gadgets\servlet\ContentFilesServlet;
use apache\shindig\common\Config;

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

class MockResourcesFilesServlet extends ResourcesFilesServlet
{
    public $noHeaders = true;
    public $uri;

    protected function getRequestUri() {
      return $this->uri;
    }
}

class MockContentFilesServlet extends ContentFilesServlet
{
    public $noHeaders = true;
    public $uri;

    protected function getRequestUri() {
      return $this->uri;
    }
}

class FilesServletTest extends \PHPUnit_Framework_TestCase
{
    public function testResources() {
        $servlet = new MockResourcesFilesServlet();
        $servlet->uri = 'com/google/caja/plugin/domita-minified.js';
        ob_start();
        $servlet->doGet();
        $servletContent = ob_get_clean();
        $fileContent = file_get_contents(Config::get('resources_path') . $servlet->uri);
        $this->assertEquals($fileContent, $servletContent);
    }

    public function testContentHtml() {
        $servlet = new MockContentFilesServlet();
        $servlet->uri = 'container/rpc_relay.html';
        ob_start();
        $servlet->doGet();
        $servletContent = ob_get_clean();
        $fileContent = file_get_contents(Config::get('javascript_path') . $servlet->uri);
        $this->assertEquals($fileContent, $servletContent);
    }

    public function testContentCss() {
        $servlet = new MockContentFilesServlet();
        $servlet->uri = 'container/gadgets.css';
        ob_start();
        $servlet->doGet();
        $servletContent = ob_get_clean();
        $fileContent = file_get_contents(Config::get('javascript_path') . $servlet->uri);
        $this->assertEquals($fileContent, $servletContent);
    }

    public function testContentFlash() {
        $servlet = new MockContentFilesServlet();
        $servlet->uri = 'container/Bridge.swf';
        ob_start();
        $servlet->doGet();
        $servletContent = ob_get_clean();
        $fileContent = file_get_contents(Config::get('javascript_path') . $servlet->uri);
        $this->assertEquals($fileContent, $servletContent);
    }

    public function testContentGif() {
        $servlet = new MockContentFilesServlet();
        $servlet->uri = 'images/new.gif';
        ob_start();
        $servlet->doGet();
        $servletContent = ob_get_clean();
        $fileContent = file_get_contents(Config::get('javascript_path') . $servlet->uri);
        $this->assertEquals($fileContent, $servletContent);
    }

    public function testContentPng() {
        $servlet = new MockContentFilesServlet();
        $servlet->uri = 'images/icon.png';
        ob_start();
        $servlet->doGet();
        $servletContent = ob_get_clean();
        $fileContent = file_get_contents(Config::get('javascript_path') . $servlet->uri);
        $this->assertEquals($fileContent, $servletContent);
    }
}

