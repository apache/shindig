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
package org.apache.shindig.gadgets.rewrite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rewriter that converts all url's to absolute.
 *
 * @since 2.0.0
 */
public class AbsolutePathReferenceRewriter extends DomWalker.Rewriter {
  private static final Logger LOG = Logger.getLogger(AbsolutePathReferenceRewriter.class.getName());

  private AbsolutePathReferenceVisitor.Tags[] tags = {AbsolutePathReferenceVisitor.Tags.RESOURCES};

  @Inject
  public AbsolutePathReferenceRewriter() {}

  @Inject(optional=true)
  public void setAbsolutePathTags(@Named("shindig.gadgets.rewriter.absolutePath.tags")
                                         String absolutePathTags) {
    if(LOG.isLoggable(Level.FINE)) {
      LOG.fine("Tags that should have the reference resolved to absolute path: " + absolutePathTags);
    }
    String[] tagsArray = absolutePathTags.split(",");
    List<AbsolutePathReferenceVisitor.Tags> tagsList = Lists.newArrayList();
    for(String tagValue : tagsArray) {
      try {
        AbsolutePathReferenceVisitor.Tags tag = AbsolutePathReferenceVisitor.Tags.valueOf(tagValue);
        if(!tagsList.contains(tag)) {
          tagsList.add(tag);
        }
      } catch (Exception ex) {
        LOG.warning("Invalid absolute path tag name : " + tagValue);
        continue;
      }
    }
    this.tags = tagsList.toArray(new AbsolutePathReferenceVisitor.Tags[tagsList.size()]);
  }

  @Override
  protected List<DomWalker.Visitor> makeVisitors(Gadget context, Uri gadgetUri) {
    return ImmutableList.<DomWalker.Visitor>of(new AbsolutePathReferenceVisitor(tags));
  }
}
