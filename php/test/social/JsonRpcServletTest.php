<?php
namespace apache\shindig\test\social;
use apache\shindig\social\servlet\JsonRpcServlet;

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

class JsonRpcServletTest extends \PHPUnit_Framework_TestCase {

    public function testParseRPCGetParameters()
    {
        $servlet = new JsonRpcServlet();
        $servlet->noHeaders = true;
        $parameters = 'oauth_token=abcdef&method=people.get&id=req&params.userId=@me&params.groupId=@self&field=1,2,3&fieldtwo(0).nested1=value1&fieldtwo(1).nested2.blub(0)=value2&fieldtwo(1).nested3=value3&f.a.c=foo&f.a.d=bar';

        $result = $servlet->parseGetRequest($parameters);

        $expected = array(
            'method' => 'people.get',
            'id' => 'req',
            'params' => array(
                'userId' => '@me',
                'groupId' => '@self',
            ),
            'field' => array(1,2,3),
            'fieldtwo' => array(
                0 => array(
                    'nested1' => 'value1',
                ),
                1 => array(
                    'nested2' => array(
                        'blub' => array(
                            0 => 'value2',
                        ),
                    ),
                    'nested3' => 'value3',
                ),
            ),
            'f' => array(
                'a' => array(
                    'c' => 'foo',
                    'd' => 'bar',
                )
            ),
            'oauth_token' => 'abcdef',
        );

        $this->assertEquals($expected, $result);
    }


    public function testParseRPCGetWithEmptyParameters()
    {
        $servlet = new JsonRpcServlet();
        $servlet->noHeaders = true;
        $result = $servlet->parseGetRequest('');

        $this->assertEquals(array(), $result);
    }
}
