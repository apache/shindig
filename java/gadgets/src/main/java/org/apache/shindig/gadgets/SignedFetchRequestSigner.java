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

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuth.Parameter;
import net.oauth.signature.RSA_SHA1;

import org.apache.shindig.util.Crypto;
import org.apache.shindig.util.TimeSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implements signed fetch based on the OAuth request signing algorithm.
 * 
 * Subclasses can override signMessage to use their own crypto if they don't
 * like the oauth.net code for some reason.
 * 
 * Instances of this class are only accessed by a single thread at a time,
 * but instances may be created by multiple threads.
 */
public class SignedFetchRequestSigner implements RequestSigner {

  protected static final String OPENSOCIAL_OWNERID = "opensocial_ownerid";

  protected static final String OPENSOCIAL_VIEWERID = "opensocial_viewerid";

  protected static final String OPENSOCIAL_APPID = "opensocial_appid";
  
  protected static final String XOAUTH_PUBLIC_KEY =
    "xoauth_signature_publickey";

  protected static final Pattern ALLOWED_PARAM_NAME = Pattern
      .compile("[\\w_\\-]+");

  protected final TimeSource clock = new TimeSource();

  /**
   * Authentication token for the user and gadget making the request.
   */
  protected GadgetToken authToken;

  /**
   * Private key we pass to the OAuth RSA_SHA1 algorithm.  This can be a 
   * PrivateKey object, or a PEM formatted private key, or a DER encoded byte
   * array for the private key.  (No, really, they accept any of them.)
   */
  protected Object privateKeyObject;

  /**
   * The name of the key, included in the fetch to help with key rotation.
   */
  protected String keyName;

  /**
   * Constructor for subclasses that don't want this code to use their
   * keys.
   */
  protected SignedFetchRequestSigner(GadgetToken authToken) {
    init(authToken, null, null);
  }

  /**
   * Constructor based on signing with the given PrivateKey object.
   * 
   * @param authToken verified gadget security token
   * @param keyName name of the key to include in the request
   * @param privateKey the key to use for the signing
   */
  public SignedFetchRequestSigner(GadgetToken authToken, String keyName,
      PrivateKey privateKey) {
    init(authToken, keyName, privateKey);
  }
  
  /**
   * Constructor based on signing with the given PrivateKey object.
   * 
   * @param authToken verified gadget security token
   * @param keyName name of the key to include in the request
   * @param privateKey base64 encoded private key
   */
  public SignedFetchRequestSigner(GadgetToken authToken, String keyName,
      String privateKey) {
    init(authToken, keyName, privateKey);
  }
 
  /**
   * Constructor based on signing with the given PrivateKey object.
   * 
   * @param authToken verified gadget security token
   * @param keyName name of the key to include in the request
   * @param privateKey DER encoded private key
   */
  public SignedFetchRequestSigner(GadgetToken authToken, String keyName, 
      byte[] privateKey) {
    init(authToken, keyName, privateKey);
  }
  
  protected void init(GadgetToken authToken, String keyName,
      Object privateKeyObject) {
    this.authToken = authToken;
    this.keyName = keyName;
    this.privateKeyObject = privateKeyObject;
  }

  /**
   * Signed fetch doesn't require approvals, always returns null.
   */
  public String getApprovalUrl() {
    return null;
  }

  /**
   * Signed fetch doesn't require approvals, always returns Status.OK
   */
  public Status getSigningStatus() {
    return Status.OK;
  }

  public URL signRequest(String method, URL resource, String postBody)
  throws GadgetException {
    try {
      // Parse the request into parameters for OAuth signing, stripping out
      // any OAuth or OpenSocial parameters injected by the client
      String query = resource.getQuery();
      resource = removeQuery(resource);
      List<OAuth.Parameter> queryParams = sanitize(OAuth.decodeForm(query));
      List<OAuth.Parameter> postParams = sanitize(OAuth.decodeForm(postBody));
      List<OAuth.Parameter> msgParams = new ArrayList<OAuth.Parameter>();
      msgParams.addAll(queryParams);
      msgParams.addAll(postParams);

      // Add the OpenSocial parameters
      addOpenSocialParams(msgParams);
      
      // Add the OAuth parameters
      addOAuthParams(msgParams);

      // Build and sign the OAuthMessage; note that the resource here has
      // no query string, the parameters are all in msgParams
      OAuthMessage message = new OAuthMessage(method, resource.toString(),
          msgParams);
      
      // Sign the message, this may jump into a subclass
      signMessage(message);

      // Rebuild the query string, including all of the parameters we added.
      // We have to be careful not to copy POST parameters into the query.
      // If post and query parameters share a name, they end up being removed
      // from the query.
      HashSet<String> forPost = new HashSet<String>();
      for (OAuth.Parameter param : postParams) {
        forPost.add(param.getKey());
      }
      List<Map.Entry<String, String>> newQuery =
        new ArrayList<Map.Entry<String, String>>();
      for (Map.Entry<String, String> param : message.getParameters()) {
        if (! forPost.contains(param.getKey())) {
          newQuery.add(param);
        }
      }
      String finalQuery = OAuth.formEncode(newQuery);
      return new URL(resource.getProtocol(), resource.getHost(),
          resource.getPort(), resource.getPath() + '?' + finalQuery);
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          e);
    }
  }

  private URL removeQuery(URL resource) throws MalformedURLException {
    return new URL(resource.getProtocol(), resource.getHost(),
        resource.getPort(), resource.getPath());
  }
  

  private void addOpenSocialParams(List<Parameter> msgParams) {
    String owner = authToken.getOwnerId();
    if (owner != null) {
      msgParams.add(new OAuth.Parameter(OPENSOCIAL_OWNERID, owner));
    }
    
    String viewer = authToken.getViewerId();
    if (viewer != null) {
      msgParams.add(new OAuth.Parameter(OPENSOCIAL_VIEWERID, viewer));
    }

    String app = authToken.getAppId();
    if (app != null) {
      msgParams.add(new OAuth.Parameter(OPENSOCIAL_APPID, app));
    }

  }
  
  private void addOAuthParams(List<Parameter> msgParams) {
    msgParams.add(new OAuth.Parameter(OAuth.OAUTH_TOKEN, ""));
    
    String domain = authToken.getDomain();
    if (domain != null) {
      msgParams.add(new OAuth.Parameter(OAuth.OAUTH_CONSUMER_KEY, domain));
    }
    
    if (keyName != null) {
      msgParams.add(new OAuth.Parameter(XOAUTH_PUBLIC_KEY, keyName));
    }

    String nonce = Long.toHexString(Crypto.rand.nextLong());
    msgParams.add(new OAuth.Parameter(OAuth.OAUTH_NONCE, nonce));
    
    String timestamp = Long.toString(clock.currentTimeMillis()/1000L);
    msgParams.add(new OAuth.Parameter(OAuth.OAUTH_TIMESTAMP, timestamp));

    msgParams.add(new OAuth.Parameter(OAuth.OAUTH_SIGNATURE_METHOD,
        OAuth.RSA_SHA1));
  }

  /**
   * Sign a message and append the oauth signature parameter to the message
   * object.
   * 
   * @param message the message to sign
   * 
   * @throws Exception because the OAuth libraries require it.
   */
  protected void signMessage(OAuthMessage message) throws Exception {
    OAuthConsumer consumer = new OAuthConsumer(null, null, null, null);
    consumer.setProperty(RSA_SHA1.PRIVATE_KEY, privateKeyObject);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    message.sign(accessor);
  }
  
  /**
   * Strip out any owner or viewer id passed by the client.
   */
  private List<Parameter> sanitize(List<Parameter> params) {
    ArrayList<Parameter> list = new ArrayList<Parameter>();
    for (Parameter p : params) {
      if (allowParam(p.getKey())) {
        list.add(p);
      }
    }
    return list;
  }

  private boolean allowParam(String paramName) {
    String canonParamName = paramName.toLowerCase();
    return (!(canonParamName.startsWith("oauth") ||
        canonParamName.startsWith("xoauth") ||
        canonParamName.startsWith("opensocial")) &&
        ALLOWED_PARAM_NAME.matcher(canonParamName).matches());
  }


}
