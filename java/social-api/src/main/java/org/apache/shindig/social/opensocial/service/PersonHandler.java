/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.common.ContainerConfigException;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.core.util.ContainerConf;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PersonHandler extends DataRequestHandler {
  private final static Logger logger = Logger.getLogger(PersonHandler.class.getName());
  private final PersonService personService;

  private static final String PEOPLE_PATH = "/people/{userId}+/{groupId}/{personId}+";
  private static final String PEOPLE_SUP_FIELDS_REGEX = "/people/@supportedFields";
  private static Pattern peoplePatternSupFields = Pattern.compile(PEOPLE_SUP_FIELDS_REGEX);
  private Object personFields = null;
  
  @Inject
  public PersonHandler(PersonService personService, ContainerConf containerConf) {
    super(containerConf);
    this.personService = personService;
  }

  @Override
  protected Future<?> handleDelete(RequestItem request) throws SocialSpiException {
    throw new SocialSpiException(ResponseError.BAD_REQUEST, "You can't delete people.");
  }

  @Override
  protected Future<?> handlePut(RequestItem request) throws SocialSpiException {
    throw new SocialSpiException(ResponseError.NOT_IMPLEMENTED, "You can't update right now.");
  }

  @Override
  protected Future<?> handlePost(RequestItem request) throws SocialSpiException {
    throw new SocialSpiException(ResponseError.NOT_IMPLEMENTED, "You can't add people right now.");
  }

  /**
   * Allowed end-points /people/{userId}+/{groupId} /people/{userId}/{groupId}/{optionalPersonId}+
   *
   * examples: /people/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
   */
  @Override
  protected Future<?> handleGet(RequestItem request) throws SocialSpiException {
    request.applyUrlTemplate(PEOPLE_PATH);
    if (request instanceof RestfulRequestItem) {
  	  if (isValidSupportedFieldsRestCall(request)) { 
  	    if (personFields == null) {
  		  logger.fine("personFieldsList is null");
  		  try {
		    personFields = this.containerConf.getPersonFields();
		  } catch (ContainerConfigException e) {
            throw new SocialSpiException(ResponseError.INTERNAL_ERROR,
			  "Error trying to Supported Person Fields from container.js", e);
		  }
  		}
  	    return ImmediateFuture.newInstance(personFields);
  	  } 
    }
    GroupId groupId = request.getGroup();
    Set<String> optionalPersonId = Sets.newLinkedHashSet(request.getListParameter("personId"));
    Set<String> fields = request.getFields(Person.Field.DEFAULT_FIELDS);
    Set<UserId> userIds = request.getUsers();

    // Preconditions
    Preconditions.requireNotEmpty(userIds, "No userId specified");
    if (userIds.size() > 1 && !optionalPersonId.isEmpty()) {
      throw new IllegalArgumentException("Cannot fetch personIds for multiple userIds");
    }

    CollectionOptions options = new CollectionOptions(request);

    if (userIds.size() == 1) {
      if (optionalPersonId.isEmpty()) {
        if (groupId.getType() == GroupId.Type.self) {
          return personService.getPerson(userIds.iterator().next(), fields, request.getToken());
        } else {
          return personService.getPeople(userIds, groupId, options, fields, request.getToken());
        }
      } else if (optionalPersonId.size() == 1) {
        // TODO: Add some crazy concept to handle the userId?
        return personService.getPerson(new UserId(UserId.Type.userId,
            optionalPersonId.iterator().next()),
            fields, request.getToken());
      } else {
        Set<UserId> personIds = Sets.newLinkedHashSet();
        for (String pid : optionalPersonId) {
          personIds.add(new UserId(UserId.Type.userId, pid));
        }
        // Every other case is a collection response of optional person ids
        return personService.getPeople(personIds, new GroupId(GroupId.Type.self, null),
            options, fields, request.getToken());
      }
    }

    // Every other case is a collection response.
    return personService.getPeople(userIds, groupId, options, fields, request.getToken());
  }
  
  private boolean isValidSupportedFieldsRestCall(RequestItem request) {
    String url = ((RestfulRequestItem)request).getUrl();
	Matcher supFieldsMatcher =peoplePatternSupFields.matcher(url);
	boolean isValidSupFieldsUrl = supFieldsMatcher.matches();
	return isValidSupFieldsUrl;
  }
}
