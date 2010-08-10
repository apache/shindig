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
 * Unittests for the shindig.uri library.
 */

function ShindigUriTest(name) {
  TestCase.call(this, name);
}

ShindigUriTest.inherits(TestCase);

ShindigUriTest.prototype.testParseFullUri = function() {
  var str = "http://www.example.com/my/path?qk1=qv1&qk2=qv2#fk1=fv1&fk2=fv2";
  var uri = shindig.uri(str);

  this.assertEquals("http", uri.getSchema());
  this.assertEquals("www.example.com", uri.getAuthority());
  this.assertEquals("http://www.example.com", uri.getOrigin());
  this.assertEquals("/my/path", uri.getPath());
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());
  this.assertEquals("qv1", uri.getQP("qk1"));
  this.assertEquals("qv2", uri.getQP("qk2"));
  this.assertEquals("fv1", uri.getFP("fk1"));
  this.assertEquals("fv2", uri.getFP("fk2"));

  // Check query and fragment again to ensure ordering doesn't matter.
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());

  // Re-serialize all.
  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParseQuerylessUri = function() {
  var str = "http://www.example.com/my/path#fk1=fv1&fk2=fv2";
  var uri = shindig.uri(str);

  this.assertEquals("http", uri.getSchema());
  this.assertEquals("www.example.com", uri.getAuthority());
  this.assertEquals("http://www.example.com", uri.getOrigin());
  this.assertEquals("/my/path", uri.getPath());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());
  this.assertEquals("fv1", uri.getFP("fk1"));
  this.assertEquals("fv2", uri.getFP("fk2"));

  // Check query and fragment again to ensure ordering doesn't matter.
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());

  // Re-serialize all.
  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParseFragmentlessUri = function() {
  var str = "http://www.example.com/my/path?qk1=qv1&qk2=qv2";
  var uri = shindig.uri(str);

  this.assertEquals("http", uri.getSchema());
  this.assertEquals("www.example.com", uri.getAuthority());
  this.assertEquals("http://www.example.com", uri.getOrigin());
  this.assertEquals("/my/path", uri.getPath());
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("qv1", uri.getQP("qk1"));
  this.assertEquals("qv2", uri.getQP("qk2"));

  // Check query and fragment again to ensure ordering doesn't matter.
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());

  // Re-serialize all.
  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParseSchemalessUri = function() {
  var str = "//www.example.com/my/path?qk1=qv1&qk2=qv2#fk1=fv1&fk2=fv2";
  var uri = shindig.uri(str);

  this.assertEquals("", uri.getSchema());
  this.assertEquals("www.example.com", uri.getAuthority());
  this.assertEquals("//www.example.com", uri.getOrigin());
  this.assertEquals("/my/path", uri.getPath());
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());
  this.assertEquals("qv1", uri.getQP("qk1"));
  this.assertEquals("qv2", uri.getQP("qk2"));
  this.assertEquals("fv1", uri.getFP("fk1"));
  this.assertEquals("fv2", uri.getFP("fk2"));

  // Check query and fragment again to ensure ordering doesn't matter.
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());

  // Re-serialize all.
  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParseAuthoritylessUri = function() {
  var str = "/my/path?qk1=qv1&qk2=qv2#fk1=fv1&fk2=fv2";
  var uri = shindig.uri(str);

  this.assertEquals("", uri.getSchema());
  this.assertEquals("", uri.getAuthority());
  this.assertEquals("", uri.getOrigin());
  this.assertEquals("/my/path", uri.getPath());
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());
  this.assertEquals("qv1", uri.getQP("qk1"));
  this.assertEquals("qv2", uri.getQP("qk2"));
  this.assertEquals("fv1", uri.getFP("fk1"));
  this.assertEquals("fv2", uri.getFP("fk2"));

  // Check query and fragment again to ensure ordering doesn't matter.
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());

  // Re-serialize all.
  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParsePathlessUri = function() {
  var str = "http://www.example.com?qk1=qv1&qk2=qv2#fk1=fv1&fk2=fv2";
  var uri = shindig.uri(str);

  this.assertEquals("http", uri.getSchema());
  this.assertEquals("www.example.com", uri.getAuthority());
  this.assertEquals("http://www.example.com", uri.getOrigin());
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());
  this.assertEquals("qv1", uri.getQP("qk1"));
  this.assertEquals("qv2", uri.getQP("qk2"));
  this.assertEquals("fv1", uri.getFP("fk1"));
  this.assertEquals("fv2", uri.getFP("fk2"));

  // Check query and fragment again to ensure ordering doesn't matter.
  this.assertEquals("qk1=qv1&qk2=qv2", uri.getQuery());
  this.assertEquals("fk1=fv1&fk2=fv2", uri.getFragment());

  // Re-serialize all.
  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParsePathOnly = function() {
  var str = "/my/path";
  var uri = shindig.uri(str);

  this.assertEquals("", uri.getSchema());
  this.assertEquals("", uri.getAuthority());
  this.assertEquals("", uri.getOrigin());
  this.assertEquals("/my/path", uri.getPath());

  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParseQueryOnly = function() {
  var str = "?foo=bar&baz=bak&boo=hiss";
  var uri = shindig.uri(str);

  this.assertEquals("bar", uri.getQP("foo"));
  this.assertEquals("bak", uri.getQP("baz"));
  this.assertEquals("hiss", uri.getQP("boo"));

  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParseFragmentOnly = function() {
  var str = "#foo=bar&baz=bak";
  var uri = shindig.uri(str);

  this.assertEquals("bar", uri.getFP("foo"));
  this.assertEquals("bak", uri.getFP("baz"));

  this.assertEquals(str, uri.toString());
};

ShindigUriTest.prototype.testParseWithContextualOddities = function() {
  var uri = shindig.uri("//www.example.com/my//path?#");

  this.assertEquals("", uri.getSchema());
  this.assertEquals("www.example.com", uri.getAuthority());
  this.assertEquals("/my//path", uri.getPath());
  this.assertEquals("", uri.getQuery());
  this.assertEquals("", uri.getFragment());

  this.assertEquals("//www.example.com/my//path", uri.toString());
};

ShindigUriTest.prototype.testParseQueryNullAndMissing = function() {
  var uri = shindig.uri("?&one=two&three&&four=five");
  this.assertEquals("two", uri.getQP("one"));
  this.assertTrue(null === uri.getQP("three"));
  this.assertTrue(undefined === uri.getQP("nonexistent"));
  this.assertEquals("five", uri.getQP("four"));
};

ShindigUriTest.prototype.testParseFragmentNullAndMissing = function() {
  var uri = shindig.uri("#&one=two&three&&four=five");
  this.assertEquals("two", uri.getFP("one"));
  this.assertTrue(null === uri.getFP("three"));
  this.assertTrue(undefined === uri.getQP("nonexistent"));
  this.assertEquals("five", uri.getFP("four"));
};

ShindigUriTest.prototype.testBuildFullUri = function() {
  var uri = shindig.uri().setSchema("http")
                         .setAuthority("www.example.com")
                         .setPath("/my/path")
                         .setQuery("?one=two&three=four")
                         .setFragment("#five=six");
  this.assertEquals("http://www.example.com/my/path?one=two&three=four#five=six", uri.toString());
};

ShindigUriTest.prototype.testBuildSchemalessUri = function() {
  var uri = shindig.uri().setAuthority("www.example.com")
                         .setPath("/my/path")
                         .setQuery("?one=two&three=four")
                         .setFragment("#five=six");
  this.assertEquals("//www.example.com/my/path?one=two&three=four#five=six", uri.toString());
};

ShindigUriTest.prototype.testBuildAuthoritylessUri = function() {
  var uri = shindig.uri().setPath("/my/path")
                         .setQuery("?one=two&three=four")
                         .setFragment("#five=six");
  this.assertEquals("/my/path?one=two&three=four#five=six", uri.toString());
};

ShindigUriTest.prototype.testBuildPathlessUri = function() {
  var uri = shindig.uri().setSchema("http")
                         .setAuthority("www.example.com")
                         .setQuery("?one=two&three=four")
                         .setFragment("#five=six");
  this.assertEquals("http://www.example.com?one=two&three=four#five=six", uri.toString());
};

ShindigUriTest.prototype.testBuildQuerylessUri = function() {
  var uri = shindig.uri().setSchema("http")
                         .setAuthority("www.example.com")
                         .setPath("/my/path")
                         .setFragment("#five=six");
  this.assertEquals("http://www.example.com/my/path#five=six", uri.toString());
};

ShindigUriTest.prototype.testBuildFragmentlessUri = function() {
  var uri = shindig.uri().setSchema("http")
                         .setAuthority("www.example.com")
                         .setPath("/my/path")
                         .setQuery("?one=two&three=four");
  this.assertEquals("http://www.example.com/my/path?one=two&three=four", uri.toString());
};

ShindigUriTest.prototype.testBuildPath = function() {
  var uri = shindig.uri().setPath("/my/path");
  this.assertEquals("/my/path", uri.toString());
};

ShindigUriTest.prototype.testBuildAuthority = function() {
  var uri = shindig.uri().setAuthority("www.example.com");
  this.assertEquals("//www.example.com", uri.toString());
};

ShindigUriTest.prototype.testBuildDirectQuery = function() {
  var uri = shindig.uri().setQuery("one=two&three&&four=five");
  this.assertEquals("two", uri.getQP("one"));
  this.assertTrue(null === uri.getQP("three"));
  this.assertTrue(undefined === uri.getQP("nonexistent"));
  this.assertEquals("five", uri.getQP("four"));
};

ShindigUriTest.prototype.testBuildDirectFragment = function() {
  var uri = shindig.uri().setFragment("one=two&three&&four=five");
  this.assertEquals("two", uri.getFP("one"));
  this.assertTrue(null === uri.getFP("three"));
  this.assertTrue(undefined === uri.getQP("nonexistent"));
  this.assertEquals("five", uri.getFP("four"));
};

ShindigUriTest.prototype.testBuildQuery = function() {
  var uri = shindig.uri().setQuery("one=two&three=four");
  this.assertEquals("two", uri.getQP("one"));
  this.assertEquals("four", uri.getQP("three"));
  uri.setQP("three", "five");
  this.assertEquals("two", uri.getQP("one"));
  this.assertEquals("five", uri.getQP("three"));
  uri.setQP({ one: "one", two: "two", three: "three" });
  this.assertEquals("one", uri.getQP("one"));
  this.assertEquals("two", uri.getQP("two"));
  this.assertEquals("three", uri.getQP("three"));
  uri.setQP("two", null);
  this.assertEquals("?one=one&three=three&two", uri.toString());
};

ShindigUriTest.prototype.testBuildFragment = function() {
  var uri = shindig.uri().setFragment("one=two&three=four");
  this.assertEquals("two", uri.getFP("one"));
  this.assertEquals("four", uri.getFP("three"));
  uri.setFP("three", "five");
  this.assertEquals("two", uri.getFP("one"));
  this.assertEquals("five", uri.getFP("three"));
  uri.setFP({ one: "one", two: "two", three: "three" });
  this.assertEquals("one", uri.getFP("one"));
  this.assertEquals("two", uri.getFP("two"));
  this.assertEquals("three", uri.getFP("three"));
  uri.setFP("two", null);
  this.assertEquals("#one=one&three=three&two", uri.toString());
};

ShindigUriTest.prototype.testReplaceExistingQuery = function() {
  var uri = shindig.uri().setQuery("one=two")
                         .setFragment("three=four")
                         .setExistingP("one", "111")
                         .setExistingP("three", "333")
                         .setExistingP("xxx", "yyy");
  this.assertEquals("?one=111#three=333", uri.toString());
};

ShindigUriTest.prototype.testBuildWithOverrides = function() {
  var uri =
      shindig.uri("http://www.example.com/my/path?one=two&baz#three=four")
        .setAuthority("www.foo.com")
        .setQP("one", "five")
        .setFragment("foo=bar");
  this.assertEquals(null, uri.getQP("baz"));
  this.assertEquals(uri.toString(), "http://www.foo.com/my/path?one=five&baz#foo=bar", uri.toString());
};
