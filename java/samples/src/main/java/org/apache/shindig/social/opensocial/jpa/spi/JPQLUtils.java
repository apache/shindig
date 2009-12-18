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

import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class JPQLUtils {

  /**
   * Append an in clause to the query builder buffer, using positional parameters.
   *
   * @param sb the query builder buffer
   * @param alias the alias to use for the property
   * @param inField the infield name
   * @param nfields the number of infields
   */
  public static int addInClause(StringBuilder sb, String alias, String inField, int firstField,
      int nfields) {
    sb.append(alias).append('.').append(inField).append(" in (");
    for (int i = firstField; i < (firstField + nfields); i++) {
      if (i != firstField) {
        sb.append(", ");
      }
      sb.append('?').append(i);
    }
    sb.append(')');
    return firstField + nfields;
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
  @SuppressWarnings("unchecked")
  public static <T> List<T> getListQuery(EntityManager entityManager, String query,
      List<?> parametersValues, CollectionOptions collectionOptions) {
    Query q = createQuery(entityManager, query, parametersValues);
    if (collectionOptions != null) {
      q.setFirstResult(collectionOptions.getFirst());
      q.setMaxResults(collectionOptions.getMax());
    }
    return (List<T>) q.getResultList();
  }


  /**
   * Performs a 'select count(*)' on the given query
   *
   * @param entityManager
   * @param query
   * @param parametersValues
   * @return
   */
  public static Long getTotalResults(EntityManager entityManager, String query,
      List<?> parametersValues) {
    int fromIndex = 0;
    String queryInUpperCase = query.toUpperCase();
    // If JPA query starts with FROM then fromIndex as 0 is correct,
    // otherwise find where FROM keyword starts in the query string and set the fromIndex.
    if (!queryInUpperCase.startsWith("FROM ")) {
      fromIndex = queryInUpperCase.indexOf(" FROM ");
      if (fromIndex == -1) {
        // Couldn't find the FROM keyword in the query
        throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid query [" + query + ']');
      }
    }
    query = "select count(*) " + query.substring(fromIndex, query.length());
    Query q = createQuery(entityManager, query, parametersValues);
    return (Long) q.getSingleResult();
  }

  /**
   * Create JPA Query
   *
   * @param entityManager
   * @param query
   * @param parametersValues
   * @return
   */
  private static Query createQuery(EntityManager entityManager, String query,
      List<?> parametersValues) {
    Query q = entityManager.createQuery(query);
    int i = 1;
    for (Object p : parametersValues) {
      q.setParameter(i, p);
      i++;
    }
    return q;
  }

}
