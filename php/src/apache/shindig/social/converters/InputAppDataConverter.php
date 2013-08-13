<?php
namespace apache\shindig\social\converters;

/*
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

class InputAppDataConverter extends InputConverter
{
    public function convertAtom($requestParam) {
        $xml = InputBasicXmlConverter::loadString($requestParam);
        if (! isset($xml->content) || ! isset($xml->content->appdata)) {
          throw new \Exception("Mallformed AppData xml");
        }
        $data = array();
        foreach (get_object_vars($xml->content->appdata) as $key => $val) {
          $data[trim($key)] = trim($val);
        }
        return $data;
    }

    public function convertJson($requestParam) {
        $ret = json_decode($requestParam, true);
        if ($ret == $requestParam) {
          throw new \Exception("Mallformed app data json string");
        }
        return $ret;
    }

    public function convertXml($requestParam) {
        $xml = InputBasicXmlConverter::loadString($requestParam);
        if (! isset($xml->entry)) {
          throw new \Exception("Mallformed AppData xml");
        }
        $data = array();
        foreach ($xml->entry as $entry) {
          $key = trim($entry->key);
          $val = isset($entry->value) ? trim($entry->value) : null;
          $data[$key] = $val;
        }
        return $data;
    }
}
