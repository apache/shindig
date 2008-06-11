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

/**
 * @fileoverview
 *
 * Unittests for URL template functions of gadgets.views.
 */

function UrlTemplateTest(name) {
  TestCase.call(this, name);
}

UrlTemplateTest.inherits(TestCase);

UrlTemplateTest.prototype.setUp = function() {
};

UrlTemplateTest.prototype.tearDown = function() {
};

UrlTemplateTest.prototype.batchTest = function(testcases) {
  for (var i = 0; i < testcases.length; ++i) {
    var testcase = testcases[i];
    var urlTemplate = testcase[0];
    var environment = testcase[1];
    var expected = testcase[2];

    if (typeof expected === 'string') {
      this.assertEquals(expected, gadgets.views.bind(urlTemplate, environment));
    } else {
      var fallenThrough = false;
      try {
        gadgets.views.bind(urlTemplate, environment);
        fallenThrough = true;
      } catch (e) {
        this.assertEquals(expected.message, e.message);
      }
      this.assertFalse(fallenThrough);
    }
  }
};

UrlTemplateTest.prototype.testVariableSubstitution = function() {
  this.batchTest([
    [
      'http://host/path/{open}{social}{0.8}{d-_-b}',
      {
        'open': 'O',
        'social': 'S',
        '0.8': 'v0.8',
        'd-_-b': '!'
      },
      'http://host/path/OSv0.8!'
    ],

    [
      'http://host/path/{undefined_value}/suffix',
      {
        'value': 'undefined'
      },
      'http://host/path//suffix'
    ],

    [
      'http://host/path/{recurring}{recurring}{recurring}',
      {
        'recurring': '.'
      },
      'http://host/path/...'
    ],

    [
      'http://host/path/{invalid definition!!!}',
      {
        'value': 'defined'
      },
      new Error('Invalid syntax : {invalid definition!!!}')
    ]

  ]);
};

