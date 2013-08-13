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

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.shindig.gadgets.templates.tags.RepeatTagHandler;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ValueExpression;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Implements a DOM-based OS templates compiler.
 * Supports:
 *   - ${...} style expressions in content and attributes
 *   - @if attribute
 *   - @repeat attribute
 * TODO:
 *   - Handle built-in/custom tags
 */
public class DefaultTemplateProcessor implements TemplateProcessor {

  //class name for logging purpose
  private static final String classname = DefaultTemplateProcessor.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);


  public static final String PROPERTY_INDEX = "Index";
  public static final String PROPERTY_COUNT = "Count";

  public static final String ATTRIBUTE_IF = "if";
  public static final String ATTRIBUTE_INDEX = "index";
  public static final String ATTRIBUTE_REPEAT = "repeat";
  public static final String ATTRIBUTE_VAR = "var";
  public static final String ATTRIBUTE_CUR = "cur";

  /**
   * Set of attributes in HTML 4 that are boolean, and may only be set
   * to that value, and should be omitted to indicate "false".
   */
  private static final Set<String> HTML4_BOOLEAN_ATTRIBUTES =
    ImmutableSet.of("checked", "compact", "declare", "defer", "disabled", "ismap",
        "multiple", "nohref", "noresize", "noshade", "nowrap", "readonly", "selected");

  private static final Set<String> ONCREATE_ATTRIBUTES =
    ImmutableSet.of("oncreate", "x-oncreate");

  private final Expressions expressions;
  // Reused buffer for creating template output
  private final StringBuilder outputBuffer;

  private TagRegistry registry;
  private TemplateContext templateContext;
  private ELContext elContext;

  private int uniqueIdCounter = 0;

  @Inject
  public DefaultTemplateProcessor(Expressions expressions) {
    this.expressions = expressions;
    outputBuffer = new StringBuilder();
  }

  /**
   * Process an entire template.
   *
   * @param template the DOM template, typically a script element
   * @param templateContext a template context providing top-level
   *     variables
   * @param globals ELResolver providing global variables other
   *     than those in the templateContext
   * @return a document fragment with the resolved content
   */
  public DocumentFragment processTemplate(Element template,
      TemplateContext templateContext, ELResolver globals, TagRegistry registry) {

    this.registry = registry;
    this.templateContext = templateContext;
    this.elContext = expressions.newELContext(globals,
        new GadgetELResolver(templateContext.getGadget().getContext()),
        new TemplateELResolver(templateContext),
        new ElementELResolver());

    DocumentFragment result = template.getOwnerDocument().createDocumentFragment();
    processChildNodes(result, template);
    return result;
  }

  /** Process the children of an element or document. */
  public void processChildNodes(Node result, Node source) {
    NodeList nodes = source.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      processNode(result, nodes.item(i));
    }
  }

  public TemplateContext getTemplateContext() {
    return templateContext;
  }

  /**
   * Process a node.
   *
   * @param result the target node where results should be inserted
   * @param source the source node of the template being processed
   */
  private void processNode(Node result, Node source) {
    switch (source.getNodeType()) {
      case Node.TEXT_NODE:
        processText(result, source.getTextContent());
        break;
      case Node.ELEMENT_NODE:
        processElement(result, (Element) source);
        break;
      case Node.DOCUMENT_NODE:
        processChildNodes(result, source);
        break;
    }
  }

  /**
   * Process text content by including non-expression content verbatim and
   * escaping expression content.

   * @param result the target node where results should be inserted
   * @param textContent the text content being processed
   */
  private void processText(Node result, String textContent) {
    Document ownerDocument = result.getOwnerDocument();

    int start = 0;
    int current = 0;
    while (current < textContent.length()) {
      current = textContent.indexOf("${", current);
      // No expressions, we're done
      if (current < 0) {
        break;
      }

      // An escaped expression "\${"
      if (current > 0 && textContent.charAt(current - 1) == '\\') {
        // Drop the \ by outputting everything before it, and moving past
        // the ${
        if (current - 1 > start) {
          String staticText = textContent.substring(start, current - 1);
          result.appendChild(ownerDocument.createTextNode(staticText));
        }
        //EL syntax is supported in gadget rendering(https://reviews.apache.org/r/8184),
        //so keep the \ into expression result, to make sure "\${" will not be evaluated in gadget rendering as expected
        start = current -1 ;
        current = current + 2;
        continue;
      }

      // Not a real expression, we're done
      int expressionEnd = textContent.indexOf('}', current + 2);
      if (expressionEnd < 0) {
        break;
      }

      // Append the existing static text, if any
      if (current > start) {
        result.appendChild(ownerDocument.createTextNode(textContent.substring(start, current)));
      }

      // Isolate the expression, parse and evaluate
      String expression = textContent.substring(current, expressionEnd + 1);
      String value = evaluate(expression, String.class, "");

      if (!"".equals(value)) {
        // And now escape
        outputBuffer.setLength(0);
        try {
          HtmlSerialization.printEscapedText(value, outputBuffer);
        } catch (IOException e) {
          // Can't happen writing to StringBuilder
          throw new RuntimeException(e);
        }

        result.appendChild(ownerDocument.createTextNode(outputBuffer.toString()));
      }

      // And continue with the next expression
      current = start = expressionEnd + 1;
    }

    // Add any static text left over
    if (start < textContent.length()) {
      result.appendChild(ownerDocument.createTextNode(textContent.substring(start)));
    }
  }

  /**
   * Process repeater state, if needed, on an element.
   */
  private void processElement(final Node result, final Element element) {
    Attr repeat = element.getAttributeNode(ATTRIBUTE_REPEAT);
    if (repeat != null) {
      Iterable<?> dataList = evaluate(repeat.getValue(), Iterable.class, null);
      processRepeat(result, element, dataList, new Runnable() {
        public void run() {
          processElementInner(result, element);
        }
      });
    } else {
      processElementInner(result, element);
    }
  }

  /**
   * @param result
   * @param element
   * @param dataList
   */
  public void processRepeat(Node result, Element element, Iterable<?> dataList,
      Runnable onEachLoop) {
    if (dataList == null) {
      return;
    }

    // Compute list size
    int size = Iterables.size(dataList);

    if (size > 0) {
      // Save the initial EL state
      Map<String, ? extends Object> oldContext = templateContext.getContext();
      Object oldCur = templateContext.getCur();
      ValueExpression oldVarExpression = null;

      // Set the new Context variable.  Copy the old context to preserve
      // any existing "index" variable
      Map<String, Object> loopData = Maps.newHashMap(oldContext);
      loopData.put(PROPERTY_COUNT, size);
      templateContext.setContext(loopData);

      // TODO: This means that any loop with @var doesn't make the loop
      // variable available in the default expression context.
      // Update the specification to make this explicit.
      Attr varAttr = element.getAttributeNode(ATTRIBUTE_VAR);
      if (varAttr == null) {
        oldCur = templateContext.getCur();
      } else {
        oldVarExpression = elContext.getVariableMapper().resolveVariable(varAttr.getValue());
      }

      Attr indexVarAttr = element.getAttributeNode(ATTRIBUTE_INDEX);
      String indexVar = indexVarAttr == null ? PROPERTY_INDEX : indexVarAttr.getValue();

      int index = 0;
      for (Object data : dataList) {
        loopData.put(indexVar, index++);

        // Set up context for rendering inner node
        templateContext.setCur(data);
        if (varAttr != null) {
          ValueExpression varExpression = expressions.constant(data, Object.class);
          elContext.getVariableMapper().setVariable(varAttr.getValue(), varExpression);
        }

        onEachLoop.run();

      }

      // Restore EL state
      if (varAttr == null) {
        templateContext.setCur(oldCur);
      } else {
        elContext.getVariableMapper().setVariable(varAttr.getValue(), oldVarExpression);
      }

      templateContext.setContext(oldContext);
    }
  }

  /**
   * Process conditionals and non-repeat attributes on an element
   */
  private void processElementInner(Node result, Element element) {
    TagHandler handler = registry.getHandlerFor(element);

    // An ugly special-case:  <os:Repeat> will re-evaluate the "if" attribute
    // (as it should) for each loop of the repeat.  Don't evaluate it here.
    if (!(handler instanceof RepeatTagHandler)) {
      Attr ifAttribute = element.getAttributeNode(ATTRIBUTE_IF);
      if (ifAttribute != null) {
        if (!evaluate(ifAttribute.getValue(), Boolean.class, false)) {
          return;
        }
      }
    }

    // TODO: the spec is silent on order of evaluation of "cur" relative
    // to "if" and "repeat"
    Attr curAttribute = element.getAttributeNode(ATTRIBUTE_CUR);
    Object oldCur = templateContext.getCur();
    if (curAttribute != null) {
      templateContext.setCur(evaluate(curAttribute.getValue(), Object.class, null));
    }

    if (handler != null) {
      handler.process(result, element, this);
    } else {
      // Be careful cloning nodes! If a target node belongs to a different document than the
      // template node then use importNode rather than cloneNode as that avoids side-effects
      // in UserDataHandlers where the cloned template node would belong to its original
      // document before being adopted by the target document.
      Element resultNode;
      if (element.getOwnerDocument() != result.getOwnerDocument()) {
        resultNode = (Element)result.getOwnerDocument().importNode(element, false);
      } else {
        resultNode = (Element)element.cloneNode(false);
      }

      clearSpecialAttributes(resultNode);
      Node additionalNode = processAttributes(resultNode);

      processChildNodes(resultNode, element);
      result.appendChild(resultNode);

      if (additionalNode != null) {
        result.appendChild(additionalNode);
      }
    }

    if (curAttribute != null) {
      templateContext.setCur(oldCur);
    }
  }

  private void clearSpecialAttributes(Element element) {
    element.removeAttribute(ATTRIBUTE_IF);
    element.removeAttribute(ATTRIBUTE_REPEAT);
    element.removeAttribute(ATTRIBUTE_INDEX);
    element.removeAttribute(ATTRIBUTE_VAR);
    element.removeAttribute(ATTRIBUTE_CUR);
  }

  /**
   * Process expressions on attributes.
   * @param element The Element to process attributes on
   * @return Node to attach after this Element, or null
   */
  private Node processAttributes(Element element) {
    NamedNodeMap attributes = element.getAttributes();
    Node additionalNode = null;

    // Mutations to perform after iterating (if needed)
    List<Attr> attrsToRemove = null;
    String newId = null;

    for (int i = 0; i < attributes.getLength(); i++) {
      boolean removeThisAttribute = false;

      Attr attribute = (Attr) attributes.item(i);
      // Boolean attributes: evaluate as a boolean.  If true, set the value to the
      // name of the attribute, e.g. selected="selected".  If false, remove the attribute
      // altogether.  The check here has some limitations for efficiency:  it assumes the
      // attribute is lowercase, and doesn't bother to check whether the boolean attribute
      // actually exists on the referred element (but HTML has no attrs that are sometimes
      // boolean and sometimes not)
      if (element.getNamespaceURI() == null &&
          HTML4_BOOLEAN_ATTRIBUTES.contains(attribute.getName())) {
        if (Boolean.TRUE.equals(evaluate(attribute.getValue(), Boolean.class, Boolean.FALSE))) {
          attribute.setNodeValue(attribute.getName());
        } else {
          removeThisAttribute = true;
        }
      } else if (ONCREATE_ATTRIBUTES.contains(attribute.getName())) {
        String id = element.getAttribute("id");
        if (id.length() == 0) {
          newId = id = getUniqueId();
        }

        additionalNode = buildOnCreateScript(
            evaluate(attribute.getValue(), String.class, null), id, element.getOwnerDocument());
        removeThisAttribute = true;
      } else {
        attribute.setNodeValue(evaluate(attribute.getValue(), String.class, null));
      }

      // Because NamedNodeMaps are live, removing them interferes with iteration.
      // Remove the attributes in a later pass
      if (removeThisAttribute) {
        if (attrsToRemove == null) {
          attrsToRemove = Lists.newArrayListWithCapacity(attributes.getLength());
        }

        attrsToRemove.add(attribute);
      }
    }

    // Now that iteration is complete, perform mutations
    if (attrsToRemove != null) {
      for (Attr attr : attrsToRemove) {
        element.removeAttributeNode(attr);
      }
    }

    if (newId != null) {
      element.setAttribute("id", newId);
    }

    return additionalNode;
  }

  /**
   * Inserts an inline script element that executes a snippet of Javascript
   * code after the element is emitted.
   * <p>
   * The approach used involves using Javascript to find the previous sibling
   * node and apply the code to it - this avoids decorating nodes with IDs, an
   * approach that could potentially clash with existing element IDs that could
   * be non-unique.
   * <p>
   * The resulting script element is subject to sanitization.
   * <p>
   * @param code Javascript code to execute
   * @param id Element ID which should be used
   * @param document document for creating elements
   *
   * TODO: Move boilerplate code for finding the right node out to a function
   * to reduce code size.
   */
  private Node buildOnCreateScript(String code, String id, Document document) {
    Element script = document.createElement("script");
    script.setAttribute("type", "text/javascript");
    StringBuilder builder = new StringBuilder();
    builder.append("(function(){");
    builder.append(code);
    builder.append("}).apply(document.getElementById('");
    builder.append(id);
    builder.append("'));");
    script.setTextContent(builder.toString());
    return script;
  }

  /**
   *  Evaluates an expression within the scope of this processor's context.
   *  @param expression The String expression
   *  @param type Expected result type
   *  @param defaultValue Default value to return in case of error
   */
  public <T> T evaluate(String expression, Class<T> type, T defaultValue) {
    try {
      ValueExpression expr = expressions.parse(expression, type);
      // Workaround for inability of Jasper-EL resolvers to access VariableMapper
      elContext.putContext(TemplateContext.class, elContext);
      Object result = expr.getValue(elContext);
      return type.cast(result);
    } catch (ELException e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.logp(Level.WARNING, classname, "evaluate", MessageKeys.EL_FAILURE,
        		  new Object[] {getTemplateContext().getGadget().getContext().getUrl(), e.getMessage()});
      }
      return defaultValue;
    }
  }

  private String getUniqueId() {
    return "ostid" + (uniqueIdCounter++);
  }
}
