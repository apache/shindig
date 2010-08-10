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
 */
package org.apache.shindig.common.uri;

import com.google.common.collect.Maps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for UriBuilder
 */
public class UriBuilderTest {

  @Test
  public void allPartsUsed() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .setQuery("hello=world")
        .setFragment("foo");
    assertEquals("http://apache.org/shindig?hello=world#foo", builder.toString());
  }

  @Test
  public void noSchemeUsed() {
    UriBuilder builder = new UriBuilder()
        .setAuthority("apache.org")
        .setPath("/shindig")
        .setQuery("hello=world")
        .setFragment("foo");
    assertEquals("//apache.org/shindig?hello=world#foo", builder.toString());
  }

  @Test
  public void noAuthorityUsed() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setPath("/shindig")
        .setQuery("hello=world")
        .setFragment("foo");
    assertEquals("http:/shindig?hello=world#foo", builder.toString());
  }

  @Test
  public void noPathUsed() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setQuery("hello=world")
        .setFragment("foo");
    assertEquals("http://apache.org?hello=world#foo", builder.toString());
  }

  @Test
  public void noQueryUsed() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .setFragment("foo");
    assertEquals("http://apache.org/shindig#foo", builder.toString());
  }

  @Test
  public void noFragmentUsed() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .setQuery("hello=world");
    assertEquals("http://apache.org/shindig?hello=world", builder.toString());
  }

  @Test
  public void hostRelativePaths() {
    UriBuilder builder = new UriBuilder()
        .setPath("/shindig")
        .setQuery("hello=world")
        .setFragment("foo");
    assertEquals("/shindig?hello=world#foo", builder.toString());
  }

  @Test
  public void relativePaths() {
    UriBuilder builder = new UriBuilder()
        .setPath("foo")
        .setQuery("hello=world")
        .setFragment("foo");
    assertEquals("foo?hello=world#foo", builder.toString());
  }

  @Test
  public void noPathNoHostNoAuthority() {
    UriBuilder builder = new UriBuilder()
        .setQuery("hello=world")
        .setFragment("foo");
    assertEquals("?hello=world#foo", builder.toString());
  }

  @Test
  public void justSchemeAndAuthority() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org");
    assertEquals("http://apache.org", builder.toString());
  }

  @Test
  public void justPath() {
    UriBuilder builder = new UriBuilder()
        .setPath("/shindig");
    assertEquals("/shindig", builder.toString());
  }

  @Test
  public void justAuthorityAndPath() {
    UriBuilder builder = new UriBuilder()
        .setAuthority("apache.org")
        .setPath("/shindig");
    assertEquals("//apache.org/shindig", builder.toString());
  }

  @Test
  public void justQuery() {
    UriBuilder builder = new UriBuilder()
        .setQuery("hello=world");
    assertEquals("?hello=world", builder.toString());
  }

  @Test
  public void justFragment() {
    UriBuilder builder = new UriBuilder()
        .setFragment("foo");
    assertEquals("#foo", builder.toString());
  }

  @Test
  public void addSingleQueryParameter() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .addQueryParameter("hello", "world")
        .setFragment("foo");
    assertEquals("http://apache.org/shindig?hello=world#foo", builder.toString());
  }

  @Test
  public void addTwoQueryParameters() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .addQueryParameter("hello", "world")
        .addQueryParameter("foo", "bar")
        .setFragment("foo");
    assertEquals("http://apache.org/shindig?hello=world&foo=bar#foo", builder.toString());
  }

  @Test
  public void addIdenticalParameters() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .addQueryParameter("hello", "world")
        .addQueryParameter("hello", "goodbye")
        .setFragment("foo");
    assertEquals("http://apache.org/shindig?hello=world&hello=goodbye#foo", builder.toString());
  }

  @Test
  public void addBatchParameters() {
    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("foo", "bar");
    params.put("hello", "world");
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .addQueryParameters(params)
        .setFragment("foo");
    assertEquals("http://apache.org/shindig?foo=bar&hello=world#foo", builder.toString());
  }

  @Test
  public void queryStringIsUnescaped() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .setQuery("hello+world=world%26bar");
    assertEquals("world&bar", builder.getQueryParameter("hello world"));
  }

  @Test
  public void queryParamsAreEscaped() {
    UriBuilder builder = new UriBuilder()
        .setScheme("http")
        .setAuthority("apache.org")
        .setPath("/shindig")
        .addQueryParameter("hello world", "foo&bar")
        .setFragment("foo");
    assertEquals("http://apache.org/shindig?hello+world=foo%26bar#foo", builder.toString());
    assertEquals("hello+world=foo%26bar", builder.getQuery());
  }

  @Test
  public void parse() {
    UriBuilder builder = UriBuilder.parse("http://apache.org/shindig?foo=bar%26baz&foo=three#blah");

    assertEquals("http", builder.getScheme());
    assertEquals("apache.org", builder.getAuthority());
    assertEquals("/shindig", builder.getPath());
    assertEquals("foo=bar%26baz&foo=three", builder.getQuery());
    assertEquals("blah", builder.getFragment());

    assertEquals("bar&baz", builder.getQueryParameter("foo"));

    List<String> values = Arrays.asList("bar&baz", "three");
    assertEquals(values, builder.getQueryParameters("foo"));
  }

  @Test
  public void constructFromUriAndBack() {
    Uri uri = Uri.parse("http://apache.org/foo/bar?foo=bar#foo");
    UriBuilder builder = new UriBuilder(uri);

    assertEquals(uri, builder.toUri());
  }

  @Test
  public void constructFromUriAndModify() {
    Uri uri = Uri.parse("http://apache.org/foo/bar?foo=bar#foo");
    UriBuilder builder = new UriBuilder(uri);

    builder.setAuthority("example.org");
    builder.addQueryParameter("bar", "foo");

    assertEquals("http://example.org/foo/bar?foo=bar&bar=foo#foo", builder.toString());
  }

  @Test
  public void equalsAndHashCodeOk() {
    UriBuilder uri = UriBuilder.parse("http://example.org/foo/bar/baz?blah=blah#boo");
    Map<String, List<String>> params = UriBuilder.splitParameters("blah=blah");
    UriBuilder uri2 = new UriBuilder(Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo"));

    assertTrue(uri.equals(uri2));
    assertTrue(uri2.equals(uri));

    assertTrue(uri.equals(uri));

    assertFalse(uri.equals(null));
    assertFalse(uri.equals("http://example.org/foo/bar/baz?blah=blah#boo"));
    assertFalse(uri.equals(Uri.parse("http://example.org/foo/bar/baz?blah=blah#boo")));

    assertTrue(uri.hashCode() == uri2.hashCode());
  }
}
