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

import java.util.Properties;
import java.util.StringTokenizer;

import javax.el.ArrayELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;

/**
 * A facade to the expressions functionality.
 */
@Singleton
public class Expressions {
  private static final Expressions sharedInstance = new Expressions();
  
  private final ExpressionFactory factory;
  private final ELContext parseContext;
  private final ELResolver defaultELResolver;

  /**
   * Return a shared instance.
   * TODO: inject Expressions into the gadget spec code and get rid of
   * this singleton.
   */
  public static Expressions sharedInstance() {
    return sharedInstance;
  }
  
  @Inject
  public Expressions() {
    factory = newExpressionFactory();
    // Stub context with no FunctionMapper, used only to parse expressions
    parseContext = new SimpleContext();
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
    return new SimpleContext(composite);
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
