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

import com.google.common.annotations.VisibleForTesting;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.expressions.juel.JuelProvider;
import org.apache.shindig.expressions.juel.JuelTypeConverter;

import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * A facade to the expressions functionality.
 */
@Singleton
public class Expressions {

  private final ExpressionFactory factory;
  private final ELContext parseContext;
  private final ELResolver defaultELResolver;
  private final Functions functions;
  private final ELTypeConverter typeConverter;

  /**
   * Returns an instance of Expressions that doesn't require
   * any functions or perform any caching.  Use only for testing.
   */
  @VisibleForTesting
  public static Expressions forTesting(Functions functions) {
    return new Expressions(functions, null, new JuelTypeConverter(), new JuelProvider());
  }

  /**
   * Returns an instance of Expressions that doesn't require
   * any functions or perform any caching.  Use only for testing.
   */
  @VisibleForTesting
  public static Expressions forTesting() {
    return new Expressions(null, null, new JuelTypeConverter(), new JuelProvider());
  }

  @Inject
  public Expressions(Functions functions, CacheProvider cacheProvider,
      ELTypeConverter typeConverter, ExpressionProvider expProvider) {
    this.functions = functions;
    this.typeConverter = typeConverter;
    factory = newExpressionFactory(expProvider, cacheProvider);
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
    boolean shouldConvert = typeConverter.isPostConvertible(type);
    if (shouldConvert) {
      return new ValueExpressionWrapper(factory.createValueExpression(
          parseContext, expression, Object.class), typeConverter, type);
    }
    else {
      return factory.createValueExpression(parseContext, expression, type);
    }
  }

  public ValueExpression constant(Object value, Class<?> type) {
    boolean shouldConvert = typeConverter.isPostConvertible(type);
    if (shouldConvert) {
      return new ValueExpressionWrapper(factory.createValueExpression(value, Object.class), typeConverter, type);
    }
    else {
      return factory.createValueExpression(value, type);
    }

  }


  private ExpressionFactory newExpressionFactory(
      ExpressionProvider expProvider, CacheProvider cacheProvider) {
    return expProvider.newExpressionFactory(cacheProvider, typeConverter);
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

  private static class ValueExpressionWrapper extends ValueExpression {

    private static final long serialVersionUID = 2135607228206570229L;
    private ValueExpression expression = null;
    private Class<?> type = null;
    private ELTypeConverter converter = null;

    public ValueExpressionWrapper(ValueExpression ve,
        ELTypeConverter converter, Class<?> type) {
      expression = ve;
      this.type = type;
      this.converter = converter;
    }

    @Override
    public Object getValue(ELContext context) throws NullPointerException,
        PropertyNotFoundException, ELException {
        return converter.convert(expression.getValue(context), type);
    }

    @Override
    public Class<?> getExpectedType() {
      return expression.getExpectedType();
    }

    @Override
    public Class<?> getType(ELContext context) throws NullPointerException,
        PropertyNotFoundException, ELException {
      return expression.getType(context);
    }

    @Override
    public boolean isReadOnly(ELContext context) throws NullPointerException,
        PropertyNotFoundException, ELException {
      return expression.isReadOnly(context);
    }

    @Override
    public void setValue(ELContext context, Object value)
        throws NullPointerException, PropertyNotFoundException,
        PropertyNotWritableException, ELException {
      expression.setValue(context, value);

    }

    @Override
    public boolean equals(Object obj) {
      return expression.equals(obj);
    }

    @Override
    public String getExpressionString() {
      return expression.getExpressionString();
    }

    @Override
    public int hashCode() {
      return expression.hashCode();
    }

    @Override
    public boolean isLiteralText() {
      return expression.isLiteralText();
    }
  }

}
