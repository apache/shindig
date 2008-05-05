/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/
package org.apache.shindig.social.abdera;

import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.gadgets.GadgetToken;

import com.google.inject.Inject;

import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * All "people" requests are processed here.
 *
 */
@SuppressWarnings("unchecked")
public class PeopleServiceAdapter extends RestServerCollectionAdapter {
  private static Logger logger =
    Logger.getLogger(PeopleServiceAdapter.class.getName());
  private PeopleService handler;
  
  // TODO get these from the config files like in feedserver
  private static final String TITLE = "People Collection title";
  private static final String AUTHOR = "TODO";
  
  @Inject
  public PeopleServiceAdapter(PeopleService handler) {
    this.handler = handler;
  }
  
  /**
   * Handles the following URLs
   *       /people/{uid}/@all
   */
  @Override
  public ResponseContext getFeed(RequestContext request) {
    // get the params from the request
    String[] paramNames = request.getTarget().getParameterNames();
    String uid = request.getTarget().getParameter(paramNames[0]);

    // TODO(doll): Fix the service apis to add a concept of arbitrary friends
    // Consider whether @all really makes sense...
    List<Person> listOfObj = null;

    return returnFeed(request, TITLE, AUTHOR, (List)listOfObj);
  }

  
  /**
   * Handles the following URLs
   *       /people/{uid}/@all/{pid}
   *       /people/{uid}/@self
   */
  @Override
  public ResponseContext getEntry(RequestContext request) {

      // TODO: Replace this with the real thing
    GadgetToken dummyToken = new GadgetToken() {
      public String toSerialForm() {
        return "";
      }

      public String getOwnerId() {
        return "";
      }

      public String getViewerId() {
        return "";
      }

      public String getAppId() {
        return "";
      }

      public String getDomain() {
        return "";
      }

      public String getAppUrl() {
        return "";
      }

      public long getModuleId() {
        return 0;
      }
    };

    // get the params from the request
    String[] paramNames = request.getTarget().getParameterNames();
    
    /* figure out which URL is passed in
     *     /people/{uid}/@all/{pid}
     *     /people/{uid}/@self
     *  To do that, see if we have both uid, pid params passed in
     *  OR just uid param.
     *  TODO better way is to have different methods to be called by abdera
     */
    String uid = request.getTarget().getParameter("uid");
    String pid = request.getTarget().getParameter("pid");
    Person person = (null == pid)
        ? handler.getPerson(uid, dummyToken).getResponse()
        : handler.getPerson(pid, dummyToken).getResponse();

    // TODO: how is entry id determined. check.
    String entryId = request.getUri().toString();
    Date updated = (person != null) ? person.getUpdated() : null;
    logger.fine("updated = " + updated);
    return returnEntry(request, person, entryId, updated);
  }
}
