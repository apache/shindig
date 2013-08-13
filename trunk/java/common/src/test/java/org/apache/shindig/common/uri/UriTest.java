/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.common.uri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Ignore;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests for Uri.
 */
public class UriTest {
  @Test
  public void parseFull() {
    Uri uri = Uri.parse("http://apache.org/foo?a=b&a=c&b=d+e#blah");

    assertEquals("http", uri.getScheme());
    assertEquals("apache.org", uri.getAuthority());
    assertEquals("/foo", uri.getPath());
    assertEquals("a=b&a=c&b=d+e", uri.getQuery());
    Collection<String> params = Arrays.asList("b", "c");
    assertEquals(params, uri.getQueryParameters("a"));
    assertEquals("b", uri.getQueryParameter("a"));
    assertEquals("d e", uri.getQueryParameter("b"));
    assertEquals("blah", uri.getFragment());

    assertEquals("http://apache.org/foo?a=b&a=c&b=d+e#blah", uri.toString());
  }

  @Test
  public void parseNoScheme() {
    Uri uri = Uri.parse("//apache.org/foo?a=b&a=c&b=d+e#blah");

    assertNull(uri.getScheme());
    assertEquals("apache.org", uri.getAuthority());
    assertEquals("/foo", uri.getPath());
    assertEquals("a=b&a=c&b=d+e", uri.getQuery());
    Collection<String> params = Arrays.asList("b", "c");
    assertEquals(params, uri.getQueryParameters("a"));
    assertEquals("b", uri.getQueryParameter("a"));
    assertEquals("d e", uri.getQueryParameter("b"));
    assertEquals("blah", uri.getFragment());
  }

  @Test
  public void parseNoAuthority() {
    Uri uri = Uri.parse("http:/foo?a=b&a=c&b=d+e#blah");

    assertEquals("http", uri.getScheme());
    assertNull(uri.getAuthority());
    assertEquals("/foo", uri.getPath());
    assertEquals("a=b&a=c&b=d+e", uri.getQuery());
    Collection<String> params = Arrays.asList("b", "c");
    assertEquals(params, uri.getQueryParameters("a"));
    assertEquals("b", uri.getQueryParameter("a"));
    assertEquals("d e", uri.getQueryParameter("b"));
    assertEquals("blah", uri.getFragment());
  }

  @Test
  public void parseNoPath() {
    Uri uri = Uri.parse("http://apache.org?a=b&a=c&b=d+e#blah");

    assertEquals("http", uri.getScheme());
    assertEquals("apache.org", uri.getAuthority());
    // Path is never null.
    assertEquals("", uri.getPath());
    assertEquals("a=b&a=c&b=d+e", uri.getQuery());
    Collection<String> params = Arrays.asList("b", "c");
    assertEquals(params, uri.getQueryParameters("a"));
    assertEquals("b", uri.getQueryParameter("a"));
    assertEquals("d e", uri.getQueryParameter("b"));
    assertEquals("blah", uri.getFragment());
  }

  @Test
  public void parseNoQuery() {
    Uri uri = Uri.parse("http://apache.org/foo#blah");

    assertEquals("http", uri.getScheme());
    assertEquals("apache.org", uri.getAuthority());
    assertEquals("/foo", uri.getPath());
    assertNull(uri.getQuery());
    assertEquals(0, uri.getQueryParameters().size());
    assertNull(uri.getQueryParameter("foo"));
    assertEquals("blah", uri.getFragment());
  }

  @Test
  public void parseNoFragment() {
    Uri uri = Uri.parse("http://apache.org/foo?a=b&a=c&b=d+e");

    assertEquals("http", uri.getScheme());
    assertEquals("apache.org", uri.getAuthority());
    assertEquals("/foo", uri.getPath());
    assertEquals("a=b&a=c&b=d+e", uri.getQuery());
    assertNull(uri.getFragment());
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseInvalidHost() {
    Uri.parse("http://A&E%#%#%/foo?a=b#blah");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseInvalidScheme() {
    Uri.parse("----://apache.org/foo?a=b#blah");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseInvalidPath() {
    Uri.parse("http://apache.org/foo\\---(&%?a=b#blah");
  }

  @Test
  public void toJavaUri() {
    URI javaUri = URI.create("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri uri = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");

    assertEquals(javaUri, uri.toJavaUri());
  }

  @Test
  public void toJavaUriWithSpecialChars() {
    URI javaUri = URI.create("http://example.org/foo/bar/baz?blah=bl%25ah#boo");
    Uri uri = Uri.parse("http://example.org/foo/bar/baz?blah=bl%25ah#boo");

    assertEquals(javaUri, uri.toJavaUri());
  }

  @Test
  public void fromJavaUri() throws Exception {
    URI javaUri = URI.create("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri uri = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");

    assertEquals(uri, Uri.fromJavaUri(javaUri));
  }

  @Test
  public void resolveFragment() throws Exception {
    Uri base = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("#bar");

    assertEquals("http://example.org/foo/bar/baz?blah=blah#bar", base.resolve(other).toString());
  }

  @Test
  public void resolveQuery() throws Exception {
    Uri base = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("?hello=world");

    assertEquals("http://example.org/foo/bar/?hello=world", base.resolve(other).toString());
  }

  @Test
  public void resolvePathIncludesSubdirs() throws Exception {
    Uri base = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("fez/../huey/./dewey/../louis");

    assertEquals("http://example.org/foo/bar/huey/louis", base.resolve(other).toString());
  }

  // Ignore for now..
  @Ignore
  public void resolvePathSubdirsExtendsBeyondRoot() throws Exception {
    Uri base = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("../random/../../../../../home");

    assertEquals("http://example.org/home", base.resolve(other).toString());
  }

  @Test
  public void resolvePathRelative() throws Exception {
    Uri base = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("wee");

    assertEquals("http://example.org/foo/bar/wee", base.resolve(other).toString());
  }

  @Test
  public void resolvePathRelativeToNullPath() throws Exception {
    Uri base = new UriBuilder().setScheme("http").setAuthority("example.org").toUri();
    Uri other = Uri.parse("dir");

    assertEquals("http://example.org/dir", base.resolve(other).toString());
  }

  @Test
  public void resolvePathAbsolute() throws Exception {
    Uri base = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("/blah");

    assertEquals("http://example.org/blah", base.resolve(other).toString());
  }

  @Test
  public void resolveAuthority() throws Exception {
    Uri base = Uri.parse("https://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("//example.com/blah");

    assertEquals("https://example.com/blah", base.resolve(other).toString());
  }

  @Test
  public void resolveAbsolute() throws Exception {
    Uri base = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri other = Uri.parse("http://www.ietf.org/rfc/rfc2396.txt");

    assertEquals("http://www.ietf.org/rfc/rfc2396.txt", base.resolve(other).toString());
  }

  @Test
  public void absoluteUrlIsAbsolute() {
    assertTrue("Url with scheme not reported absolute.",
        Uri.parse("http://example.org/foo").isAbsolute());
  }

  @Test
  public void relativeUrlIsNotAbsolute() {
    assertFalse("Url without scheme reported absolute.",
        Uri.parse("//example.org/foo").isAbsolute());
  }

  @Test
  public void parseWithSpecialCharacters() {
    String original = "http://example.org/?foo%25pbar=baz+blah";

    assertEquals(original, Uri.parse(original).toString());
  }

  @Test
  public void equalsAndHashCodeOk() {
    Uri uri = Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Uri uri2 = new UriBuilder()
        .setScheme("http")
        .setAuthority("example.org")
        .setPath("/foo/bar/baz")
        .addQueryParameter("blah", "blah")
        .setFragment("boo")
        .toUri();

    assertEquals(uri, uri2);
    assertEquals(uri2, uri);

    assertEquals(uri.hashCode(), uri2.hashCode());
  }
}
