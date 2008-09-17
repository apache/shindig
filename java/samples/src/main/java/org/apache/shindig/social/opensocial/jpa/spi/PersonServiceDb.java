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
package org.apache.shindig.social.opensocial.jpa.spi;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.jpa.PersonDb;
import org.apache.shindig.social.opensocial.jpa.api.FilterCapability;
import org.apache.shindig.social.opensocial.jpa.api.FilterSpecification;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.inject.Inject;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 */
public class PersonServiceDb implements PersonService {

  private EntityManager entiyManager;

  /**
   * 
   */
  @Inject
  public PersonServiceDb(EntityManager entityManager) {
    this.entiyManager = entityManager;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.shindig.social.opensocial.spi.PersonService#getPeople(java.util.Set,
   *      org.apache.shindig.social.opensocial.spi.GroupId,
   *      org.apache.shindig.social.opensocial.spi.CollectionOptions, java.util.Set,
   *      org.apache.shindig.auth.SecurityToken)
   */
  public Future<RestfulCollection<Person>> getPeople(final Set<UserId> userIds,
      final GroupId groupId, final CollectionOptions collectionOptions, final Set<String> fields,
      final SecurityToken token) throws SocialSpiException {
    // for each user id get the filtered userid using the token and then, get the users identified
    // by the group id, the final set is filtered
    // using the collectionOptions and return the fields requested.

    // not dealing with the collection options at the moment, and not the fields because they are
    // either lazy or at no extra costs.

    return new Future<RestfulCollection<Person>>() {

      private boolean cancel = false;

      public boolean cancel(boolean cancel) {
        this.cancel = this.cancel || cancel;
        return this.cancel;
      }

      public RestfulCollection<Person> get() throws InterruptedException, ExecutionException {

        // sanitize the list to get the uid's and remove duplicates
        HashMap<String, String> userIdMap = new HashMap<String, String>();
        List<String> paramList = new ArrayList<String>();
        for (UserId u : userIds) {
          try {
            String uid = u.getUserId(token);
            if (uid != null) {
              userIdMap.put(uid, uid);
              paramList.add(uid);
            }
          } catch (IllegalStateException istate) {
            // ignore the user id.
          }
        }
        // select the group Id as this will drive the query
        switch (groupId.getType()) {
        case all: {
          // select all contacts
          StringBuilder sb = new StringBuilder();
          sb.append(PersonDb.JPQL_FINDALLPERSON);
          addInClause(sb, "id", paramList.size());
          int filterPos = addFilterClause(sb, PersonDb.getFilterCapability(), collectionOptions,
              paramList.size() + 1);
          if (filterPos > 0) {
            paramList.add(collectionOptions.getFilterValue());
          }
          addOrderClause(sb, collectionOptions);

          List<Person> plist = getListQuery(sb.toString(), paramList, collectionOptions);
          return new RestfulCollection<Person>(plist);
        }
        case friends: {
          // select all friends
          StringBuilder sb = new StringBuilder();
          sb.append(PersonDb.JPQL_FINDPERSON_BY_FRIENDS);
          addInClause(sb, "id", paramList.size());
          int filterPos = addFilterClause(sb, PersonDb.getFilterCapability(), collectionOptions,
              paramList.size() + 1);
          if (filterPos > 0) {
            paramList.add(collectionOptions.getFilterValue());
          }
          addOrderClause(sb, collectionOptions);

          List<Person> plist = getListQuery(sb.toString(), paramList, collectionOptions);
          return new RestfulCollection<Person>(plist);
        }
        case groupId: {
          // select those in the group
          StringBuilder sb = new StringBuilder();
          sb.append(PersonDb.JPQL_FINDPERSON_BY_GROUP);
          List<Object> params = new ArrayList<Object>();
          params.add(groupId.getGroupId());
          params.addAll(paramList);

          addInClause(sb, "id", paramList.size());
          int filterPos = addFilterClause(sb, PersonDb.getFilterCapability(), collectionOptions,
              params.size() + 1);
          if (filterPos > 0) {
            params.add(collectionOptions.getFilterValue());
          }
          addOrderClause(sb, collectionOptions);

          List<Person> plist = getListQuery(sb.toString(), params, collectionOptions);
          return new RestfulCollection<Person>(plist);
        }
        case deleted:
          // ???
          break;
        case self: {
          // select self
          StringBuilder sb = new StringBuilder();
          sb.append(PersonDb.JPQL_FINDPERSON);
          addInClause(sb, "id", paramList.size());
          int filterPos = addFilterClause(sb, PersonDb.getFilterCapability(), collectionOptions,
              paramList.size() + 1);
          if (filterPos > 0) {
            paramList.add(collectionOptions.getFilterValue());
          }
          addOrderClause(sb, collectionOptions);

          List<Person> plist = getListQuery(sb.toString(), paramList, collectionOptions);
          return new RestfulCollection<Person>(plist);

        }

        }
        throw new SocialSpiException(ResponseError.BAD_REQUEST, "Group ID not recognized");
      }

      public RestfulCollection<Person> get(long arg0, TimeUnit arg1) throws InterruptedException,
          ExecutionException, TimeoutException {
        return get();
      }

      public boolean isCancelled() {
        return false;
      }

      public boolean isDone() {
        return true;
      }

    };

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.shindig.social.opensocial.spi.PersonService#getPerson(org.apache.shindig.social.opensocial.spi.UserId,
   *      java.util.Set, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Person> getPerson(UserId id, Set<String> fields, SecurityToken token)
      throws SocialSpiException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Perform a JPAQ, and return a typed list.
   * 
   * @param <T> The type of list
   * @param query the JPQL Query with positional parameters
   * @param parametersValues a list of parameters
   * @param collectionOptions the options used for paging.
   * @return a typed list of objects
   */
  protected <T> List<T> getListQuery(String query, List<?> parametersValues,
      CollectionOptions collectionOptions) {
    Query q = entiyManager.createQuery(query);
    int i = 1;
    for (Object p : parametersValues) {
      q.setParameter(i, p);
      i++;
    }
    q.setFirstResult(collectionOptions.getFirst());
    q.setMaxResults(collectionOptions.getFirst() + collectionOptions.getMax());
    return q.getResultList();
  }

  /**
   * Append an in clause to the query builder buffer, using positional parameters.
   * 
   * @param sb the query builder buffer
   * @param inField the infield name (assumes that this is bound to a p. object)
   * @param nfields the number of infields
   */
  private void addInClause(StringBuilder sb, String inField, int nfields) {
    sb.append("p.").append(inField).append(" in (");
    for (int i = 1; i <= nfields; i++) {
      sb.append(" ?").append(i).append(" ");
    }
    sb.append(")");
  }

  /**
   * Add a filter clause specified by the collection options.
   * 
   * @param sb the query builder buffer
   * @param collectionOptions the options
   * @param lastPos the last positional parameter that was used so far in the query
   * @return
   */
  private int addFilterClause(StringBuilder sb, FilterCapability filterable,
      CollectionOptions collectionOptions, int lastPos) {
    // this makes the filter value safe
    String filter = filterable.getFilterableProperty(collectionOptions.getFilter(),
        collectionOptions.getFilterOperation());
    String filterValue = collectionOptions.getFilterValue();
    int filterPos = 0;
    if (FilterSpecification.isValid(filter)) {
      if (FilterSpecification.isSpecial(filter)) {
        if ("hasApp".equals(filter)) {
        } else if ("topFriends".equals(filter)) {
        }
      } else {
        sb.append("p.").append(filter);
        switch (collectionOptions.getFilterOperation()) {
        case contains:
          filterPos = lastPos + 1;
          sb.append(" like ").append(" ?").append(filterPos);
          filterValue = "%" + filterValue + "%";
          break;
        case equals:
          filterPos = lastPos + 1;
          sb.append(" = ").append(" ?").append(filterPos);
          break;
        case present:
          sb.append(" is not null ");
          break;
        case startsWith:
          filterPos = lastPos + 1;
          sb.append(" like ").append(" ?").append(filterPos);
          filterValue = "%" + filterValue + "%";
          break;
        }
      }
    }
    return filterPos;
  }

  /**
   * Add an order clause to the query string.
   * 
   * @param sb the buffer for the query string
   * @param collectionOptions the options to use for the order.
   */
  private void addOrderClause(StringBuilder sb, CollectionOptions collectionOptions) {
    String sortBy = collectionOptions.getSortBy();
    if (sortBy != null && sortBy.length() > 0) {
      sb.append(" order by p.").append(sortBy);
      switch (collectionOptions.getSortOrder()) {
      case ascending:
        sb.append(" asc ");
        break;
      case descending:
        sb.append(" desc ");
        break;
      }
    }
  }
}
