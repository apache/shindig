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

import java.net.URL;

/**
 * Interface for generating signed requests and handling responses.
 * 
 * Most of the functionality here isn't used at the moment, just enough to
 * implement signed fetch.  The status and approval bits will be used for 
 * full OAuth.
 */
public interface RequestSigner {

  /**
   * Whether the signing request succeeded.
   */
  public enum Status {
    OK,
    APPROVAL_NEEDED,
  }
  
  /**
   * Signs a URL and post body, returning a modified URL including the
   * signature.  The post body is not modified.
   * 
   * @param method how the request will be sent
   * @param resource the URL to sign
   * @param postBody the body of the request (may be null)
   * 
   * @return a signed URL
   * @throws GadgetException if an error occurs.
   */
  public URL signRequest(String method, URL resource, String postBody)
  throws GadgetException;
  
  /**
   * @return the status associated with the last signing request.  May return
   * APPROVAL_NEEDED if user interaction is required.
   */
  public Status getSigningStatus();

  /**
   * @return the URL the user needs to visit to approve the access request.
   */
  public String getApprovalUrl();
}
