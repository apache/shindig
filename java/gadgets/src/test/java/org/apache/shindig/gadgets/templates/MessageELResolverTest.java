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
package org.apache.shindig.gadgets.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.junit.Before;
import org.junit.Test;

import javax.el.ELContext;
import javax.el.ELException;

public class MessageELResolverTest {
  static private final String MESSAGE_BUNDLE =
    "<messagebundle>" +
      "<msg name='hello'>world</msg>" +
      "<msg name='number'>${1+1}</msg>" +
      "<msg name='concat'>${Msg.hello} ${Msg.number}</msg>" +
      "<msg name='multiLevel'>${Msg.concat} ${Msg.concat}</msg>" +
      // Self-recursive EL, should fail
      "<msg name='recurse'>${Msg.recurse}</msg>" +
      // Mutually recursive EL, should fail
      "<msg name='mutual1'>${Msg.mutual2}</msg>" +
      "<msg name='mutual2'>${Msg.mutual1}</msg>" +
    "</messagebundle>";
  private MessageBundle messageBundle;
  private Expressions expressions;
  private ELContext context;

  @Before
  public void setUp() throws Exception {
    messageBundle = new MessageBundle(XmlUtil.parse(MESSAGE_BUNDLE));
    expressions = Expressions.forTesting();
    context = expressions.newELContext(new MessageELResolver(expressions, messageBundle));
  }

  @Test
  public void basicExpression() {
    assertEquals("world", expressions.parse("${Msg.hello}", String.class).getValue(context));
  }

  @Test
  public void nullForMissingProperty() {
    assertNull(expressions.parse("${Msg.notThere}", Object.class).getValue(context));
  }

  @Test
  public void innerEvaluation() {
    assertEquals(2, expressions.parse("${Msg.number}", Integer.class).getValue(context));
  }

  @Test
  public void recursiveEvaluation() {
    assertEquals("world 2", expressions.parse("${Msg.concat}", String.class).getValue(context));
  }

  @Test
  public void multiLevelRecursiveEvaluation() {
    assertEquals("world 2 world 2", expressions.parse("${Msg.multiLevel}", String.class).getValue(context));
  }

  @Test(expected = ELException.class)
  public void failsInsteadOfInfiniteRecursion() {
    expressions.parse("${Msg.recurse}", String.class).getValue(context);
  }

  @Test(expected = ELException.class)
  public void failsInsteadOfMutualInfiniteRecursion() {
    expressions.parse("${Msg.mutual1}", String.class).getValue(context);
  }
}
