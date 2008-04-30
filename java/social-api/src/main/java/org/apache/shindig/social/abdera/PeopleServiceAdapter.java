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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * All "people" requests are processed here.
 *
 * @author vnori@google.com (Vasu Nori)
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

  public PeopleServiceAdapter() {
    // TODO needs cleanup once injection from AbderaServlet works..
    Injector injector = null;
    try {
       injector = Guice.createInjector(new RestGuiceModule());
    } catch (Exception e) {
      logger.severe("injector exception: " + e.getMessage());
    }
    handler = injector.getInstance(PeopleService.class);
  }

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

  @Override
  public ResponseContext getEntry(RequestContext request) {
    // get the params from the request
    String[] paramNames = request.getTarget().getParameterNames();

    //   this method is called with 2 params for /people/{uid}/@all/{pid}
    // and with 1 param for /people/{uid}/@self
    // TODO have 2 different Abdera Handlers for the 2 different urls.
    Person person;
    switch (paramNames.length) {
      case 1:
        String uid = request.getTarget().getParameter(paramNames[0]);
        // TODO: Pass in the gadget token
        person = handler.getPerson(uid, null).getResponse();
        break;
      case 2:
        uid = request.getTarget().getParameter(paramNames[0]);
        String pid = request.getTarget().getParameter(paramNames[1]);
        // TODO: pass in the gadget token with the uid parameter set. We don't
        // have different views of people from an aribtrary ids point of view.
        // Rather, the token is how permissions are done.
        person = handler.getPerson(pid, null).getResponse();
        break;
      default:
        return ProviderHelper.notsupported(request, "more than 2 params?");
    }
    // TODO: how is entry id determined. check.
    String entryId = request.getUri().toString();
    Date updated = (person != null) ? person.getUpdated() : null;
    logger.info("updated = " + updated);
    return returnEntry(request, person, entryId, updated);
  }
}
