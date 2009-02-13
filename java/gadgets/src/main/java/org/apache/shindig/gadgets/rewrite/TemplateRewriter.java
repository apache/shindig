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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.templates.TemplateContext;
import org.apache.shindig.gadgets.templates.TemplateProcessor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.json.JSONObject;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This ContentRewriter uses a TemplateProcessor to replace os-template
 * tag contents of a gadget spec with their rendered equivalents.
 * 
 * Only templates without the @name and @tag attributes are processed 
 * automatically.
 */
public class TemplateRewriter implements ContentRewriter {

  public final static Set<String> TAGS = ImmutableSet.of("script");
  
  /**
   * Provider of the processor.  TemplateRewriters are stateless and multithreaded,
   * processors are not.
   */
  private final Provider<TemplateProcessor> processor;
  
  @Inject
  public TemplateRewriter(Provider<TemplateProcessor> processor) {
    this.processor = processor;
  }
  
  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    return null;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    Feature f = gadget.getSpec().getModulePrefs().getFeatures()
        .get("opensocial-templates");
    if (f != null) {
      return rewriteImpl(gadget, content);   
    }
    return null;
  }
  
  private RewriterResults rewriteImpl(Gadget gadget, MutableContent content) {
    List<Element> tagList =
      DomUtil.getElementsByTagNameCaseInsensitive(content.getDocument(), TAGS);
    final Map<String, JSONObject> pipelinedData = content.getPipelinedData();

    List<Element> templates = ImmutableList.copyOf(
        Iterables.filter(tagList, new Predicate<Element>() {
      public boolean apply(Element element) {
        String type = element.getAttribute("type");
        String name = element.getAttribute("name");
        String tag = element.getAttribute("tag");
        String require = element.getAttribute("require");
        // Templates with "tag" or "name" can't be processed;  templates
        // that require data that isn't available on the server can't
        // be processed either
        return "text/os-template".equals(type)
            && "".equals(name) 
            && "".equals(tag)
            && checkRequiredData(require, pipelinedData.keySet());
      }
    }));
    
    if (templates.isEmpty()) {
      return null;
    }
    
    TemplateContext templateContext = new TemplateContext(pipelinedData);
    GadgetELResolver globalGadgetVars = new GadgetELResolver(gadget.getContext());
    
    for (Element template : templates) {
      DocumentFragment result = processor.get().processTemplate(
          template, templateContext, globalGadgetVars);
      // Note: replaceNode errors when replacing Element with DocumentFragment
      template.getParentNode().insertBefore(result, template);
      // TODO: clients that need to update data that is initially available,
      // e.g. paging through friend lists, will still need the template
      template.getParentNode().removeChild(template);
    }
    
    // TODO: Deactivate the "os-templates" feature if all templates have
    // been rendered.
    // Note: This may not always be correct behavior since client may want
    // some purely client-side templating (such as from libraries)
    MutableContent.notifyEdit(content.getDocument());
    return RewriterResults.cacheableIndefinitely();
  }
  
  /**
   * Checks that all the required data is available at rewriting time.
   * @param requiredData A string of comma-separated data set names
   * @param availableData A map of available data sets
   * @return true if all required data sets are present, false otherwise
   */
  private static boolean checkRequiredData(String requiredData, Set<String> availableData) {
    if ("".equals(requiredData)) {
      return true;
    }
    StringTokenizer st = new StringTokenizer(requiredData, ",");
    while (st.hasMoreTokens()) {
      if (!availableData.contains(st.nextToken().trim())) {
        return false;
      }
    }
    return true;
  }
}
