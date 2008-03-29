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
 * Request signer interface that does nothing, for convenience of writing
 * code that may or may not need to sign requests.
 */
public class NullRequestSigner implements RequestSigner {

  public String getApprovalUrl() {
    return null;
  }

  public Status getSigningStatus() {
    return Status.OK;
  }

  @SuppressWarnings("unused")
  public URL signRequest(String method, URL resource, String postBody) throws GadgetException {
    return resource;
  }

}
