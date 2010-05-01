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

import org.apache.shindig.social.opensocial.spi.CollectionOptions;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

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
    sb.append(alias).append(".").append(inField).append(" in (");
    for (int i = firstField; i < (firstField + nfields); i++) {
      sb.append(" ?").append(i).append(" ");
    }
    sb.append(")");
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
    Query q = entityManager.createQuery(query);
    int i = 1;
    for (Object p : parametersValues) {
      q.setParameter(i, p);
      i++;
    }
    if (collectionOptions != null) {
      q.setFirstResult(collectionOptions.getFirst());
      q.setMaxResults(collectionOptions.getFirst() + collectionOptions.getMax());
    }
    return (List<T>) q.getResultList();
  }

}
