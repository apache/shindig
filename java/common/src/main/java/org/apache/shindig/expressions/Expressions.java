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

import org.json.JSONArray;

import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.el.ArrayELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.odysseus.el.ExpressionFactoryImpl;

/**
 * A facade to the expressions functionality.
 */
@Singleton
public class Expressions {
  private final ExpressionFactory factory;
  private final ELContext parseContext;
  private final ELResolver defaultELResolver;
  private final Functions functions;

  /**
   * Convenience constructor that doesn't require any Functions.
   */
  public Expressions() {
    this(null);
  }
  
  @Inject
  public Expressions(Functions functions) {
    this.functions = functions;
    factory = newExpressionFactory();
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
    if (type == JSONArray.class) {
      // TODO: the coming version of JUEL offers support for custom type converters.  Use it!
      return new CustomCoerce(factory.createValueExpression(parseContext, expression, String.class),
          type);
    }
    return factory.createValueExpression(parseContext, expression, type);
  }
  
  public ValueExpression constant(Object value, Class<?> type) {
    return factory.createValueExpression(value, type);
  }
  
  private ExpressionFactory newExpressionFactory() {
    Properties properties = new Properties();
    // TODO: configure cache size?
    return new ExpressionFactoryImpl(properties);
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
   * ELContext implementation.  SimpleContext from JUEL would be
   * sufficient if not for:
   * https://sourceforge.net/tracker2/?func=detail&aid=2590830&group_id=165179&atid=834616
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
  
  /** 
   * Class providing custom type coercion for getValue() where needed.
   * This will be obsolete with JUEL 2.1.1.
   */
  static private class CustomCoerce extends ValueExpression {

    private final ValueExpression base;
    private final Class<?> type;

    public CustomCoerce(ValueExpression base, Class<?> type) {
      this.base = base;
      this.type = type;
    }

    @Override
    public Class<?> getExpectedType() {
      return type;
    }

    @Override
    public Class<?> getType(ELContext context) {
      return type;
    }

    @Override
    public Object getValue(ELContext context) {
      Object value = base.getValue(context);
      if (value == null) {
        return null;
      }
      
      if (type == JSONArray.class) {
        JSONArray array = new JSONArray();
        StringTokenizer tokenizer = new StringTokenizer(value.toString(), ",");
        while (tokenizer.hasMoreTokens()) {
          array.put(tokenizer.nextToken());
        }

        return array;
      } else {
        throw new ELException("Can't coerce to type " + type.getName());
      }
    }

    @Override
    public boolean isReadOnly(ELContext context) {
      return true;
    }

    @Override
    public void setValue(ELContext context, Object value) {
      throw new PropertyNotWritableException();
    }

    @Override
    public String getExpressionString() {
      return base.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
      return base.isLiteralText();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      
      if (!(o instanceof CustomCoerce)) {
        return false;
      }
      
      CustomCoerce that = (CustomCoerce) o;
      return that.base.equals(this.base) && that.type.equals(this.type);
    }

    @Override
    public int hashCode() {
      return base.hashCode();
    }
    
  }
}
