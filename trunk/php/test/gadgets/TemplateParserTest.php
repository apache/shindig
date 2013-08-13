<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\templates\TemplateLibrary;
use apache\shindig\gadgets\GadgetContext;
use apache\shindig\gadgets\templates\TemplateParser;

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

class TemplateParserTest extends \PHPUnit_Framework_TestCase {
  public function testOsVar() {
    $viewNode = '<?xml version="1.0" encoding="UTF-8" ?>
        <script xmlns:os="http://ns.opensocial.org/2008/markup" type="text/os-template">
          <os:Var key="counter" value="1" />
          <os:Var key="counter2" value="${counter + 1}" />
          <os:Var key="array" value="[1,3,5,7]" />
          <os:Var key="object">
            {"key" : "value"}
          </os:Var>
        </script>';

    $dataContext = array();
    $doc = new \DomDocument();
    $doc->loadXml($viewNode);
    $contentBlocks = $doc->getElementsByTagName('script');
    $library = new TemplateLibrary(new GadgetContext('GADGET'));
    $parser = new TemplateParser();
    $tags = array();
    foreach ($contentBlocks as $content) {
      $tags[] = $parser->process($content, $dataContext, $library);
    }
    $this->assertEquals(1, count($tags));

    $dataContext = $parser->getDataContext();

    $this->assertEquals(1, $dataContext['Top']['counter']);
    $this->assertEquals(2, $dataContext['Top']['counter2']);
    $this->assertEquals(array(1,3,5,7), $dataContext['Top']['array']);
    $this->assertEquals(array('key' => 'value'), $dataContext['Top']['object']);
  }
}