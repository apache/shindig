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

import junit.framework.Assert;
import junit.framework.TestCase;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuth.Parameter;
import net.oauth.signature.RSA_SHA1;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Tests the signed fetch code.
 */
public class SignedFetchRequestSignerTest extends TestCase {
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

  private SignedFetchRequestSigner signer;
  private BasicGadgetToken authToken;
  private OAuthAccessor accessor;
  
  @Override
  public void setUp() throws Exception {
    authToken = new BasicGadgetToken("o", "v", "a", "d");
    signer = new SignedFetchRequestSigner(authToken, "foo", PRIVATE_KEY_TEXT);
    OAuthConsumer consumer = new OAuthConsumer(null, null, null, null);
    consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, CERTIFICATE_TEXT);
    accessor = new OAuthAccessor(consumer);
  }
  
  public void testParametersSet() throws Exception {
    URL unsigned = new URL("http://test");
    URL out = signer.signRequest("GET", unsigned, null);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertTrue(contains(queryParams, "opensocial_ownerid", "o"));
    Assert.assertTrue(contains(queryParams, "opensocial_viewerid", "v"));
    Assert.assertTrue(contains(queryParams, "opensocial_appid", "a"));
    Assert.assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "d"));
    Assert.assertTrue(
        contains(queryParams, "xoauth_signature_publickey", "foo"));
  }
  
  public void testTrickyParametersInQuery() throws Exception {
    String tricky = "%6fpensocial_ownerid=gotcha";
    URL unsigned = new URL("http://test?" + tricky);
    URL out = signer.signRequest("GET", unsigned, null);
    Assert.assertFalse(out.getQuery().contains("gotcha"));
    assertSignatureOK("GET", out.toString(), null);
  }
  
  public void testTrickyParametersInBody() throws Exception {
    URL unsigned = new URL("http://test");
    String tricky = "%6fpensocial_ownerid=gotcha";
    URL out = signer.signRequest("POST", unsigned, tricky);
    assertSignatureInvalid("POST", out.toString(), tricky);       
  }
  
  public void testGetNoQuery() throws Exception {
    URL unsigned = new URL("http://test");
    URL out = signer.signRequest("GET", unsigned, null);
    assertSignatureOK("GET", out.toString(), null);
  }
  
  public void testGetWithQuery() throws Exception {
    URL unsigned = new URL("http://test?a=b");
    URL out = signer.signRequest("GET", unsigned, null);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertTrue(contains(queryParams, "a", "b"));
    assertSignatureOK("GET", out.toString(), null);
  }


  public void testGetWithQueryMultiParam() throws Exception {
    URL unsigned = new URL("http://test?a=b&a=c");
    URL out = signer.signRequest("GET", unsigned, null);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertTrue(contains(queryParams, "a", "b"));
    Assert.assertTrue(contains(queryParams, "a", "c"));
    assertSignatureOK("GET", out.toString(), null);
  }
  
  public void testPostNoQueryNoData() throws Exception {
    URL unsigned = new URL("http://test");
    URL out = signer.signRequest("POST", unsigned, null);
    assertSignatureOK("POST", out.toString(), null);    
  }
  
  public void testPostWithQueryNoData() throws Exception {
    URL unsigned =  new URL("http://test?name=value");
    URL out = signer.signRequest("POST", unsigned, null);
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertTrue(contains(queryParams, "name", "value"));
    assertSignatureOK("POST", out.toString(), null);    
  }
  
  public void testPostNoQueryWithData() throws Exception {
    URL unsigned =  new URL("http://test");
    URL out = signer.signRequest("POST", unsigned, "name=value");
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertFalse(contains(queryParams, "name", "value"));
    assertSignatureOK("POST", out.toString(), "name=value");    
  }
  
  public void testPostWithQueryWithData() throws Exception {
    URL unsigned =  new URL("http://test?queryName=queryValue");
    URL out = signer.signRequest("POST", unsigned, "name=value");
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertTrue(contains(queryParams, "queryName", "queryValue"));
    assertSignatureOK("POST", out.toString(), "name=value");    
  }
  
  public void testStripOpenSocialParamsFromQuery() throws Exception {
    URL unsigned =  new URL("http://test?opensocial_foo=bar");
    URL out = signer.signRequest("POST", unsigned, "name=value");
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertFalse(contains(queryParams, "opensocial_foo", "bar"));
    assertSignatureOK("POST", out.toString(), "name=value");       
  }
  
  public void testStripOAuthParamsFromQuery() throws Exception {
    URL unsigned =  new URL("http://test?oauth_foo=bar");
    URL out = signer.signRequest("POST", unsigned, "name=value");
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(out.getQuery());
    Assert.assertFalse(contains(queryParams, "oauth_foo", "bar"));
    assertSignatureOK("POST", out.toString(), "name=value");       
  }

  public void testStripOpenSocialParamsFromBody() throws Exception {
    URL unsigned =  new URL("http://test");
    URL out = signer.signRequest("POST", unsigned, "opensocial_foo=bar");
    assertSignatureInvalid("POST", out.toString(), "opensocial_foo=bar");       
  }
  
  public void testStripOAuthParamsFromBody() throws Exception {
    URL unsigned =  new URL("http://test");
    URL out = signer.signRequest("POST", unsigned, "oauth_foo=bar");
    assertSignatureInvalid("POST", out.toString(), "oauth_foo=bar");       
  }
  
  private void assertSignatureOK(String method, String urlStr, String body)
  throws Exception {
    URL url = new URL(urlStr);
    URL noQuery = new URL(url.getProtocol(), url.getHost(), url.getPort(),
        url.getPath());
    List<OAuth.Parameter> queryParams = OAuth.decodeForm(url.getQuery());
    List<OAuth.Parameter> postParams = OAuth.decodeForm(body);
    
    ArrayList<OAuth.Parameter> msgParams = new ArrayList<OAuth.Parameter>();
    msgParams.addAll(queryParams);
    msgParams.addAll(postParams);
    
    OAuthMessage message = new OAuthMessage(method, noQuery.toString(),
        msgParams);

    // Throws on failure
    message.validateSignature(accessor);
  }
  
  private void assertSignatureInvalid(String method, String urlStr, String body) {
    try {
      assertSignatureOK(method, urlStr, body);
      fail("Signature verification should have failed");
    } catch (Exception e) {
      // good
    }
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
