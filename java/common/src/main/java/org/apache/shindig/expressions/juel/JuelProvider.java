/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.expressions.juel;

import javax.el.ExpressionFactory;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.misc.TypeConverter;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeCache;
import de.odysseus.el.tree.TreeStore;
import de.odysseus.el.tree.impl.Builder;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.NullCache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.expressions.ELTypeConverter;
import org.apache.shindig.expressions.ExpressionProvider;

/**
 * A provider for a Juel based Expression Implementation
 * @since 2.0.0
 */
public class JuelProvider implements ExpressionProvider {

  private static final String EXPRESSION_CACHE = "expressions";

  /**
   * Any provided JUEL converter must implement both JUEL TypeConverter impl and ELTypeConverter
   */
  public ExpressionFactory newExpressionFactory(CacheProvider cacheProvider,
      ELTypeConverter converter) {
    TreeStore store = new TreeStore(new Builder(),
        createTreeCache(cacheProvider));
    return new ExpressionFactoryImpl(store, (TypeConverter) converter);
  }

  /**
   * Create a JUEL cache of expressions.
   */
  private TreeCache createTreeCache(CacheProvider cacheProvider) {
    Cache<String, Tree> treeCache;
    if (cacheProvider == null) {
      treeCache = new NullCache<String, Tree>();
    } else {
      treeCache = cacheProvider.createCache(EXPRESSION_CACHE);
    }

    final Cache<String, Tree> resolvedTreeCache = treeCache;
    return new TreeCache() {
      public Tree get(String expression) {
        return resolvedTreeCache.getElement(expression);
      }

      public void put(String expression, Tree tree) {
        resolvedTreeCache.addElement(expression, tree);
      }
    };
  }

}
