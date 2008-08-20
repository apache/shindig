/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shindig.gadgets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.http.BasicHttpCache;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpRequest;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.OAuth.Parameter;
import net.oauth.signature.RSA_SHA1;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the signed fetch code.
 */
public class SigningFetcherTest {
  private static final String PRIVATE_KEY_TEXT =
    "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALRiMLAh9iimur8V" +
    "A7qVvdqxevEuUkW4K+2KdMXmnQbG9Aa7k7eBjK1S+0LYmVjPKlJGNXHDGuy5Fw/d" +
    "7rjVJ0BLB+ubPK8iA/Tw3hLQgXMRRGRXXCn8ikfuQfjUS1uZSatdLB81mydBETlJ" +
    "hI6GH4twrbDJCR2Bwy/XWXgqgGRzAgMBAAECgYBYWVtleUzavkbrPjy0T5FMou8H" +
    "X9u2AC2ry8vD/l7cqedtwMPp9k7TubgNFo+NGvKsl2ynyprOZR1xjQ7WgrgVB+mm" +
    "uScOM/5HVceFuGRDhYTCObE+y1kxRloNYXnx3ei1zbeYLPCHdhxRYW7T0qcynNmw" +
    "rn05/KO2RLjgQNalsQJBANeA3Q4Nugqy4QBUCEC09SqylT2K9FrrItqL2QKc9v0Z" +
    "zO2uwllCbg0dwpVuYPYXYvikNHHg+aCWF+VXsb9rpPsCQQDWR9TT4ORdzoj+Nccn" +
    "qkMsDmzt0EfNaAOwHOmVJ2RVBspPcxt5iN4HI7HNeG6U5YsFBb+/GZbgfBT3kpNG" +
    "WPTpAkBI+gFhjfJvRw38n3g/+UeAkwMI2TJQS4n8+hid0uus3/zOjDySH3XHCUno" +
    "cn1xOJAyZODBo47E+67R4jV1/gzbAkEAklJaspRPXP877NssM5nAZMU0/O/NGCZ+" +
    "3jPgDUno6WbJn5cqm8MqWhW1xGkImgRk+fkDBquiq4gPiT898jusgQJAd5Zrr6Q8" +
    "AO/0isr/3aa6O6NLQxISLKcPDk2NOccAfS/xOtfOz4sJYM3+Bs4Io9+dZGSDCA54" +
    "Lw03eHTNQghS0A==";

  private static final String CERTIFICATE_TEXT =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIBpjCCAQ+gAwIBAgIBATANBgkqhkiG9w0BAQUFADAZMRcwFQYDVQQDDA5UZXN0\n" +
    "IFByaW5jaXBhbDAeFw03MDAxMDEwODAwMDBaFw0zODEyMzEwODAwMDBaMBkxFzAV\n" +
    "BgNVBAMMDlRlc3QgUHJpbmNpcGFsMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n" +
    "gQC0YjCwIfYoprq/FQO6lb3asXrxLlJFuCvtinTF5p0GxvQGu5O3gYytUvtC2JlY\n" +
    "zypSRjVxwxrsuRcP3e641SdASwfrmzyvIgP08N4S0IFzEURkV1wp/IpH7kH41Etb\n" +
    "mUmrXSwfNZsnQRE5SYSOhh+LcK2wyQkdgcMv11l4KoBkcwIDAQABMA0GCSqGSIb3\n" +
    "DQEBBQUAA4GBAGZLPEuJ5SiJ2ryq+CmEGOXfvlTtEL2nuGtr9PewxkgnOjZpUy+d\n" +
    "4TvuXJbNQc8f4AMWL/tO9w0Fk80rWKp9ea8/df4qMq5qlFWlx6yOLQxumNOmECKb\n" +
    "WpkUQDIDJEoFUzKMVuJf4KO/FJ345+BNLGgbJ6WujreoM1X/gYfdnJ/J\n" +
    "-----END CERTIFICATE-----";

  private InterceptingContentFetcher interceptor;
  private HttpCache cache;
  private SigningFetcher signer;
  private BasicSecurityToken authToken;
  private OAuthAccessor accessor;
  private OAuthValidator messageValidator;

  @Before
  public void setUp() throws Exception {
    cache = new BasicHttpCache(10);
    interceptor = new InterceptingContentFetcher();
    authToken = new BasicSecurityToken("o", "v", "a", "d", "u", "m");
    signer = SigningFetcher.makeFromB64PrivateKey(cache,
        interceptor, authToken, "foo", PRIVATE_KEY_TEXT);
    OAuthConsumer consumer = new OAuthConsumer(null, null, null, null);
    consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, CERTIFICATE_TEXT);
    accessor = new OAuthAccessor(consumer);
    messageValidator = new SimpleOAuthValidator();
  }

  private HttpRequest makeHttpRequest(String method, String url) {
    return makeHttpRequest(method, url, null);
  }

  private HttpRequest makeHttpRequest(String method, String url, byte[] body) {
    return new HttpRequest(Uri.parse(url))
    .setMethod(method)
    .setPostBody(body);
  }

  private HttpRequest signAndInspect(HttpRequest orig) throws Exception {
    signer.fetch(orig);
    assertSignatureOK(interceptor.interceptedRequest);
    return interceptor.interceptedRequest;
  }

  @Test
  public void testParametersSet() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
    assertTrue(contains(queryParams, "opensocial_app_id", "a"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "d"));
    assertTrue(contains(queryParams, "xoauth_signature_publickey", "foo"));
  }

  @Test
  public void testNoSignViewer() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    unsigned.setSignViewer(false);
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertFalse(contains(queryParams, "opensocial_viewer_id", "v"));
  }

  @Test
  public void testNoSignOwner() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    unsigned.setSignOwner(false);
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertFalse(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
  }

  @Test
  public void testCacheHit() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    signAndInspect(unsigned);

    HttpRequest unsigned2 = makeHttpRequest("GET", "http://test", null);
    interceptor.interceptedRequest = null;
    signer.fetch(unsigned2);
    assertNull(interceptor.interceptedRequest);
  }

  @Test
  public void testCacheMiss_noOwner() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    unsigned.setSignOwner(false);
    signAndInspect(unsigned);

    HttpRequest unsigned2 = makeHttpRequest("GET", "http://test", null);
    interceptor.interceptedRequest = null;
    signer.fetch(unsigned2);
    assertNotNull(interceptor.interceptedRequest);
  }

  @Test
  public void testCacheHit_ownerOnly() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    unsigned.setSignViewer(false);
    signAndInspect(unsigned);

    HttpRequest unsigned2 = makeHttpRequest("GET", "http://test", null);
    unsigned2.setSignViewer(false);
    interceptor.interceptedRequest = null;
    signer.fetch(unsigned2);
    assertNull(interceptor.interceptedRequest);
  }

  @Test
  public void testCacheMiss_bypassCache() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    unsigned.setSignViewer(false);
    signAndInspect(unsigned);

    HttpRequest unsigned2 = makeHttpRequest("GET", "http://test", null);
    unsigned2.setIgnoreCache(true);
    unsigned2.setSignViewer(false);
    interceptor.interceptedRequest = null;
    signer.fetch(unsigned2);
    assertNotNull(interceptor.interceptedRequest);
  }

  @Test(expected = RequestSigningException.class)
  public void testTrickyParametersInQuery() throws Exception {
    String tricky = "%6fpensocial_owner_id=gotcha";
    HttpRequest unsigned = makeHttpRequest("GET", "http://test?" + tricky, null);
    signAndInspect(unsigned);
  }

  @Test(expected = RequestSigningException.class)
  public void testTrickyParametersInBody() throws Exception {
    String tricky = "%6fpensocial_owner_id=gotcha";
    HttpRequest unsigned = makeHttpRequest("POST", "http://test", tricky.getBytes());
    signAndInspect(unsigned);
  }

  @Test
  public void testGetNoQuery() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test", null);
    signAndInspect(unsigned);
  }

  @Test
  public void testGetWithQuery() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test?a=b", null);
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertTrue(contains(queryParams, "a", "b"));
  }

  @Test
  public void testGetWithQueryMultiParam() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test?a=b&a=c");
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertTrue(contains(queryParams, "a", "b"));
    assertTrue(contains(queryParams, "a", "c"));
  }

  @Test
  public void testValidParameterCharacters() throws Exception {
    String weird = "~!@$*()-_[]:,./";
    HttpRequest unsigned = makeHttpRequest("GET", "http://test?" + weird + "=foo");
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertTrue(contains(queryParams, weird, "foo"));
  }

  @Test
  public void testPostNoQueryNoData() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test");
    signAndInspect(unsigned);
  }

  @Test
  public void testPostWithQueryNoData() throws Exception {
    HttpRequest unsigned = makeHttpRequest("GET", "http://test?name=value");
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertTrue(contains(queryParams, "name", "value"));
  }

  @Test
  public void testPostNoQueryWithData() throws Exception {
    HttpRequest unsigned = makeHttpRequest("POST", "http://test", "name=value".getBytes());
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertFalse(contains(queryParams, "name", "value"));
  }

  @Test
  public void testPostWithQueryWithData() throws Exception {
    HttpRequest unsigned = makeHttpRequest(
        "POST", "http://test?queryName=queryValue", "name=value".getBytes());
    HttpRequest out = signAndInspect(unsigned);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getUri().getQuery());
    assertTrue(contains(queryParams, "queryName", "queryValue"));
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOpenSocialParamsFromQuery() throws Exception {
    HttpRequest unsigned = makeHttpRequest("POST", "http://test?opensocial_foo=bar");
    signAndInspect(unsigned);
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOAuthParamsFromQuery() throws Exception {
    HttpRequest unsigned = makeHttpRequest(
        "POST", "http://test?oauth_foo=bar", "name=value".getBytes());
    signAndInspect(unsigned);
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOpenSocialParamsFromBody() throws Exception {
    HttpRequest unsigned = makeHttpRequest("POST", "http://test", "opensocial_foo=bar".getBytes());
    signAndInspect(unsigned);
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOAuthParamsFromBody() throws Exception {
    HttpRequest unsigned = makeHttpRequest("POST", "http://test", "oauth_foo=bar".getBytes());
    signAndInspect(unsigned);
  }

  private void assertSignatureOK(HttpRequest req) throws Exception {
    UriBuilder url = new UriBuilder(req.getUri());
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(url.getQuery());
    url.setQuery(null);

    String body = req.getPostBodyAsString();
    if (body.length() == 0) {
      body = null;
    }
    List<OAuth.Parameter> postParams = OAuth.decodeForm(body);

    ArrayList<OAuth.Parameter> msgParams = new ArrayList<OAuth.Parameter>();
    msgParams.addAll(queryParams);
    msgParams.addAll(postParams);

    OAuthMessage message = new OAuthMessage(req.getMethod(), url.toString(), msgParams);

    // Throws on failure
    message.validateMessage(accessor, messageValidator);
  }

  // Checks whether the given parameter list contains the specified
  // key/value pair
  private boolean contains(List<Parameter> params, String key, String value) {
    for (Parameter p : params) {
      if (p.getKey().equals(key) && p.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
