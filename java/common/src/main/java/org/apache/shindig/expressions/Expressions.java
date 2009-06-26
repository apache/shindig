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
package org.apache.shindig.expressions;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.NullCache;

import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.misc.TypeConverter;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeCache;
import de.odysseus.el.tree.TreeStore;
import de.odysseus.el.tree.impl.Builder;

/**
 * A facade to the expressions functionality.
 */
@Singleton
public class Expressions {
  private static final String EXPRESSION_CACHE = "expressions";
  
  private final ExpressionFactory factory;
  private final ELContext parseContext;
  private final ELResolver defaultELResolver;
  private final Functions functions;
  private final TypeConverter typeConverter;

  /** 
   * Returns an instance of Expressions that doesn't require
   * any functions or perform any caching.  Use only for testing.
   */
  public static Expressions forTesting() {
    return new Expressions(null, null, new ShindigTypeConverter());
  }
  
  @Inject
  public Expressions(Functions functions, CacheProvider cacheProvider,
      ShindigTypeConverter typeConverter) {
    this.functions = functions;
    this.typeConverter = typeConverter;
    factory = newExpressionFactory(cacheProvider);
    // Stub context with no FunctionMapper, used only to parse expressions
    parseContext = new Context(null);
    defaultELResolver = createDefaultELResolver();
  }

  /**
   * Creates an ELContext.
   * @param customResolvers resolvers to be added to the chain
   */
  public ELContext newELContext(ELResolver... customResolvers) {
    CompositeELResolver composite = new CompositeELResolver();
    for (ELResolver customResolver : customResolvers) {
      composite.add(customResolver);
    }

    composite.add(defaultELResolver);
    return new Context(composite);
  }

  /**
   * Parse a value expression.
   * @param expression the string expression.  This may be a literal
   *     without any expressions.
   * @param type the desired coercion type.
   * @return a ValueExpression corresponding to the expression
   */
  public ValueExpression parse(String expression, Class<?> type) {
    return factory.createValueExpression(parseContext, expression, type);
  }
  
  public ValueExpression constant(Object value, Class<?> type) {
    return factory.createValueExpression(value, type);
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
  
  
  private ExpressionFactory newExpressionFactory(CacheProvider cacheProvider) {
    TreeStore store = new TreeStore(new Builder(), createTreeCache(cacheProvider));
    return new ExpressionFactoryImpl(store, typeConverter);
  }
  
  /**
   * @return a default ELResolver with functionality needed by all
   * expression evaluation.
   */
  private ELResolver createDefaultELResolver() {
    CompositeELResolver resolver = new CompositeELResolver();
    // Resolvers, in the order they will be most commonly accessed.
    // Moving JsonELResolver to the end makes JSON property resolution twice
    // as slow, so this is quite important.
    resolver.add(new JsonELResolver());
    resolver.add(new MapELResolver());
    resolver.add(new ListELResolver());
    resolver.add(new ArrayELResolver());
    // TODO: bean el resolver?
    
    return resolver;
  }

  /**
   * ELContext implementation, like SimpleContext but using an injected
   * FunctionMapper.
   */
  private class Context extends ELContext {
    private final ELResolver resolver;
    private VariableMapper variables;

    public Context(ELResolver resolver) {
      this.resolver = resolver;
    }
    
    @Override
    public ELResolver getELResolver() {
      return resolver;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
      return functions;
    }

    @Override
    public VariableMapper getVariableMapper() {
      if (variables == null) {
        variables = new Variables();
      }
      
      return variables;
    }
    
  }
  
  static private class Variables extends VariableMapper {
    private final Map<String, ValueExpression> variables = Maps.newHashMap();
    @Override
    public ValueExpression resolveVariable(String var) {
      return variables.get(var);
    }

    @Override
    public ValueExpression setVariable(String var, ValueExpression expression) {
      return variables.put(var, expression);
    }
    
  }
}
