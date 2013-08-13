<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\servlet\GadgetRenderingServlet;

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

class GadgetRenderingServletTest extends \PHPUnit_Framework_TestCase {
    public function testCheckConstraints() {
        $servlet = new GadgetRenderingServlet();
        $servlet->noHeaders = true;

        $constraints = array('type' => 'HTML', 'href' => false);

        $view = array('type' => 'HTML', 'foo' => 'bar');
        $this->assertTrue($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'HTML', 'foo' => 'bar', 'href' => '');
        $this->assertTrue($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'HTML', 'foo' => 'bar', 'href' => 'blub');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'URL', 'foo' => 'bar', 'href' => 'blub');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'URL', 'foo' => 'bar');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));

        $constraints = array('type' => 'HTML', 'href' => true);

        $view = array('type' => 'HTML', 'foo' => 'bar');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'HTML', 'foo' => 'bar', 'href' => '');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'HTML', 'foo' => 'bar', 'href' => 'blub');
        $this->assertTrue($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'URL', 'foo' => 'bar', 'href' => 'blub');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'URL', 'foo' => 'bar');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));

        $constraints = array('type' => 'URL');

        $view = array('type' => 'HTML', 'foo' => 'bar');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'HTML', 'foo' => 'bar', 'href' => '');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'HTML', 'foo' => 'bar', 'href' => 'blub');
        $this->assertFalse($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'URL', 'foo' => 'bar', 'href' => 'blub');
        $this->assertTrue($servlet->checkConstraints($view, $constraints));
        $view = array('type' => 'URL', 'foo' => 'bar');
        $this->assertTrue($servlet->checkConstraints($view, $constraints));
    }
}
