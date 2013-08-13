<?php
namespace apache\shindig\test\common;
use apache\shindig\common\OpenSocialVersion;

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

/**
 * OpenSocialVersion test case.
 */
class OpenSocialVersionTest extends \PHPUnit_Framework_TestCase {
    
    public function testTwoVersionsAreEqual() {
        $version1 = new OpenSocialVersion('1.1.0');
        $version2 = new OpenSocialVersion('1.1.0');
        
        $this->assertTrue($version1->isEquivalent($version2));
    }
    
    public function testToString() {
        $this->assertEquals('1.2.3', (string) (new OpenSocialVersion('1.2.3')));
    }
    
    public function testTwoVersionsAreNotEqual() {
        $version1 = new OpenSocialVersion('1.1.0');
        $version2 = new OpenSocialVersion('1.2.0');
        $version3 = new OpenSocialVersion('2.1.0');
        $version4 = new OpenSocialVersion('1.1.2');
        
        $this->assertFalse($version1->isEquivalent($version2));
       // $this->assertFalse($version1->isEquivalent($version3));
       // $this->assertFalse($version1->isEquivalent($version4));
    }
    
    public function testVersionIsEqualOrGreater() {
        $version1 = new OpenSocialVersion('1.1.0');
        $version2 = new OpenSocialVersion('1.1.0');
        $version3 = new OpenSocialVersion('1.1.1');
        $version4 = new OpenSocialVersion('1.2.0');
        $version5 = new OpenSocialVersion('2.2.0');
        
        $this->assertTrue($version2->isEqualOrGreaterThan($version1));
        $this->assertTrue($version3->isEqualOrGreaterThan($version1));
        $this->assertTrue($version4->isEqualOrGreaterThan($version1));
        $this->assertTrue($version5->isEqualOrGreaterThan($version1));
    }
    
    public function testVersionIsNotEqualOrGreater() {
        $version1 = new OpenSocialVersion('1.1.1');
        $version2 = new OpenSocialVersion('1.0.9');
        $version3 = new OpenSocialVersion('1.1.0');
        $version4 = new OpenSocialVersion('0.2.0');
        $version5 = new OpenSocialVersion('0.9.9');
        
        $this->assertFalse($version2->isEqualOrGreaterThan($version1));
        $this->assertFalse($version3->isEqualOrGreaterThan($version1));
        $this->assertFalse($version4->isEqualOrGreaterThan($version1));
        $this->assertFalse($version5->isEqualOrGreaterThan($version1));
    }
    
    public function testEmptyOpenSocialVersion() {
        $version1 = new OpenSocialVersion('2.0.0');
        $version2 = new OpenSocialVersion();
        
        $this->assertTrue($version2->isEqualOrGreaterThan($version1));
    }
}