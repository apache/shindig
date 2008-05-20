/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social.abdera.json;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.bidi.BidiHelper;
import org.apache.abdera.ext.html.HtmlHelper;
import org.apache.abdera.ext.thread.InReplyTo;
import org.apache.abdera.ext.thread.ThreadHelper;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.text.Bidi.Direction;
import org.apache.abdera.model.Base;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Control;
import org.apache.abdera.model.Div;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Generator;
import org.apache.abdera.model.Link;
import org.apache.abdera.model.Person;
import org.apache.abdera.model.Source;
import org.apache.abdera.model.Text;
import org.apache.abdera.model.TextValue;
import org.apache.abdera.xpath.XPath;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/*
 * TODO: This file is copied and modified from Abdera code as we needed
 * functionality different from the Abdera Json writer code base.
 * This file definitely needs cleanup and heavy refactoring
 */
@SuppressWarnings("unchecked")
public class JSONUtil {

  public static void toJson(
    Base base,
    Writer writer)
      throws IOException {
    JSONStream jstream = new JSONStream(writer);
    if (base instanceof Document) {
      toJson((Document)base,jstream);
    } else if (base instanceof Element) {
      toJson((Element)base,jstream);
    }
    writer.flush();
  }

  private static boolean isSameAsParentBase(
    Element element) {
      IRI parentbase = null;
      if (element.getParentElement() != null) {
        parentbase = element instanceof Document ?
          element.getBaseUri() :
          element.getResolvedBaseUri();
      }
      IRI base = element.getResolvedBaseUri();

      if (parentbase == null && base != null) {
        return false;
      }
      if (parentbase == null && base == null) {
        return true;
      }
      return parentbase.equals(element.getResolvedBaseUri());
  }

  private static void writeText (
      Element element,
      JSONStream jstream)
        throws IOException {
    Text text = (Text)element;
    Text.Type texttype = text.getTextType();
    if (texttype.equals(Text.Type.TEXT) &&
        !needToWriteLanguageFields(text)) {
      jstream.writeQuoted(text.getValue());
    } else {
      jstream.startObject();
      jstream.writeField("attributes");
      jstream.startObject();
      jstream.writeField("type",texttype.name().toLowerCase());
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      jstream.endObject();
      jstream.writeField("children");
      switch(text.getTextType()) {
        case TEXT:
          jstream.startArray();
          jstream.writeQuoted(text.getValue());
          jstream.endArray();
          break;
        case HTML:
          Div div = HtmlHelper.parse(text.getValue());
          writeElementValue(div, jstream);
          break;
        case XHTML:
          writeElementValue(text.getValueElement(), jstream);
          break;
      }
      jstream.endObject();
    }
  }

  private static void toJson(
    Element element,
    JSONStream jstream)
      throws IOException {

    if (element instanceof Text) {
      writeText(element, jstream);
      return;
    }

    if (element instanceof Content) {
      Content content = (Content)element;
      Content.Type contenttype = content.getContentType();
      if (contenttype.equals(Content.Type.TEXT) &&
          !needToWriteLanguageFields(content)) {
        String buf = content.getValue();
        if (buf.length() > 2) {
          jstream.writeValue(buf.substring(1, buf.length() - 1));
        }
      }
      return;
    }

    if (element instanceof Categories) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Categories categories = (Categories) element;
      jstream.writeField("fixed", categories.isFixed()?"true":"false");
      jstream.writeField("scheme", categories.getScheme());
      writeList("categories",categories.getCategories(),jstream);
      writeExtensions((ExtensibleElement)element,jstream);
      jstream.endObject();
      return;
    }

    if (element instanceof Category) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Category category = (Category) element;
      jstream.writeField("term", category.getTerm());
      jstream.writeField("scheme", category.getScheme());
      jstream.writeField("label", category.getLabel());
      writeExtensions((ExtensibleElement)element,jstream);
      jstream.endObject();
      return;
    }

    if (element instanceof Collection) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Collection collection = (Collection)element;
      jstream.writeField("href", collection.getResolvedHref());
      writeElement("title",collection.getTitleElement(),jstream);
      String[] accepts = collection.getAccept();
      if (accepts != null || accepts.length > 0) {
        jstream.writeField("accept");
        jstream.startArray();
        for (int n = 0; n < accepts.length; n++) {
          jstream.writeQuoted(accepts[n]);
          if (n < accepts.length - 1) {
            jstream.writeSeparator();
          }
        }
        jstream.endArray();
      }
      List<Categories> cats = collection.getCategories();
      if (cats.size() > 0) {
        writeList("categories",collection.getCategories(),jstream);
      }
      writeExtensions((ExtensibleElement)element,jstream);
      jstream.endObject();
      return;
    }

    if (element instanceof Control) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Control control = (Control)element;
      jstream.writeField("draft", control.isDraft()?"true":"false");
      writeExtensions((ExtensibleElement)element,jstream);
      jstream.endObject();
      return;
    }

    if (element instanceof Entry) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Entry entry = (Entry)element;
      jstream.writeField("id", entry.getId());
      writeElement("title", entry.getTitleElement(),jstream);
      writeElement("summary", entry.getSummaryElement(),jstream);
      writeElement("rights", entry.getRightsElement(),jstream);
      writeElement("content", entry.getContentElement(),jstream);
      jstream.writeField("updated", entry.getUpdated());
      jstream.writeField("published", entry.getPublished());
      jstream.writeField("edited", entry.getEdited());
      writeElement("source", entry.getSource(),jstream);
      writeList("authors",entry.getAuthors(),jstream);
      writeList("contributors",entry.getContributors(),jstream);
      writeList("links",entry.getLinks(),jstream);
      writeList("categories",entry.getCategories(),jstream);
      writeList("inreplyto",ThreadHelper.getInReplyTos(entry),jstream);
      writeElement("control", entry.getControl(), jstream);
      //writeExtensions((ExtensibleElement)element,jstream);
      jstream.endObject();
      return;
    }

    if (element instanceof Generator) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Generator generator = (Generator)element;
      jstream.writeField("version", generator.getVersion());
      jstream.writeField("uri", generator.getResolvedUri());
      jstream.writeField("value", generator.getText());
      jstream.endObject();
      return;
    }

    if (element instanceof Link) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Link link = (Link)element;
      jstream.writeField("href", link.getResolvedHref());
      jstream.writeField("rel", link.getRel());
      jstream.writeField("title", link.getTitle());
      jstream.writeField("type", link.getMimeType());
      jstream.writeField("hreflang", link.getHrefLang());
      if (link.getLength() > -1) {
        jstream.writeField("length", link.getLength());
      }
      writeExtensions((ExtensibleElement)element,jstream);
      jstream.endObject();
      return;
    }

    if (element instanceof Person) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      Person person = (Person)element;
      jstream.writeField("name",person.getName());
      if (person.getEmail() != null) {
        jstream.writeField("email",person.getEmail());
      }
      if (person.getUri() != null) {
        jstream.writeField("uri",person.getUriElement().getResolvedValue());
      }
      writeExtensions((ExtensibleElement)element,jstream);
      jstream.endObject();
      return;
    }

    if (element instanceof Source) {
      //jstream.startObject();
      writeLanguageFields(element, jstream);
//      if (!isSameAsParentBase(element)) {
//        jstream.writeField("xml:base", element.getResolvedBaseUri());
//      }
      Source source = (Source)element;
//      jstream.writeField("id", source.getId());
//      writeElement("title", source.getTitleElement(),jstream);
//      writeElement("subtitle", source.getSubtitleElement(),jstream);
//      writeElement("rights", source.getRightsElement(),jstream);
//      jstream.writeField("updated", source.getUpdated());
//      writeElement("generator", source.getGenerator(),jstream);
//      if (source.getIconElement() != null) {
//        jstream.writeField("icon", source.getIconElement().getResolvedValue());
//      }
//      if (source.getLogoElement() != null) {
//        jstream.writeField("logo", source.getLogoElement().getResolvedValue());
//      }
//      writeList("authors",source.getAuthors(),jstream);
//      writeList("contributors",source.getContributors(),jstream);
//      writeList("links",source.getLinks(),jstream);
//      writeList("categories",source.getCategories(),jstream);
//      if (FeedPagingHelper.isComplete(source)) {
//        jstream.writeField("complete",true);
//      }
//      if (FeedPagingHelper.isArchive(source)) {
//        jstream.writeField("archive",true);
//      }
      if (source instanceof Feed) {
        //writeList("entries",((Feed)source).getEntries(),jstream);
        writeList(null,((Feed)source).getEntries(),jstream);
      }
      //writeExtensions((ExtensibleElement)element,jstream);
      //jstream.endObject();
      return;
    }

    if (element instanceof InReplyTo) {
      jstream.startObject();
      writeLanguageFields(element, jstream);
      if (!isSameAsParentBase(element)) {
        jstream.writeField("xml:base", element.getResolvedBaseUri());
      }
      InReplyTo irt = (InReplyTo)element;
      jstream.writeField("ref",irt.getRef());
      jstream.writeField("href",irt.getResolvedHref());
      jstream.writeField("type",irt.getMimeType());
      jstream.writeField("source",irt.getResolvedSource());
      jstream.endObject();
    }
  }

  private static void writeElementValue(
    Element element,
    JSONStream jstream)
      throws IOException {
    writeElementChildren(element, jstream);
  }

  private static String getName(QName qname) {
    String prefix = qname.getPrefix();
    String name = qname.getLocalPart();
    return prefix != null && !"".equals(prefix) ?
      prefix + ":" + name :
      name;
  }

  private static void writeElement(
    Element child,
    QName parentqname,
    JSONStream jstream)
      throws IOException {
    QName childqname = child.getQName();
    String prefix = childqname.getPrefix();
    jstream.startObject();
    jstream.writeField("name", getName(childqname));
    jstream.writeField("attributes");
    List<QName> attributes = child.getAttributes();
    jstream.startObject();
    if (!isSameNamespace(childqname, parentqname)) {
      if (prefix != null && !"".equals(prefix)) {
        jstream.writeField("xmlns:" + prefix);
      } else {
        jstream.writeField("xmlns");
      }
      jstream.writeQuoted(childqname.getNamespaceURI());
    }
    if (!isSameAsParentBase(child)) {
      jstream.writeField("xml:base",child.getResolvedBaseUri());
    }
    writeLanguageFields(child, jstream);
    for (QName attr : attributes) {
      String name = getName(attr);
      jstream.writeField(name);
      if ("".equals(attr.getPrefix())  ||
          "xml".equals(attr.getPrefix())) {
        String val = child.getAttributeValue(attr);
        if (val != null &&
            ("href".equalsIgnoreCase(name) ||
             "src".equalsIgnoreCase(name) ||
             "action".equalsIgnoreCase(name))) {
         IRI base = child.getResolvedBaseUri();
         if (base != null) {
          val = base.resolve(val).toASCIIString();
        }
        }
        jstream.writeQuoted(val);
      } else {
        jstream.startObject();
        jstream.writeField("attributes");
        jstream.startObject();
        jstream.writeField("xmlns:" + attr.getPrefix());
        jstream.writeQuoted(attr.getNamespaceURI());
        jstream.endObject();
        jstream.writeField("value");
        jstream.writeQuoted(child.getAttributeValue(attr));
        jstream.endObject();
      }
    }
    jstream.endObject();
    jstream.writeField("children");
    writeElementChildren(child,jstream);
    jstream.endObject();
  }

  private static void writeElementChildren(
    Element element,
    JSONStream jstream)
      throws IOException {
    jstream.startArray();
    Object[] children = getChildren(element);
    QName parentqname = element.getQName();
    for (int n = 0; n < children.length; n++) {
      Object child = children[n];
      if (child instanceof Element) {
        writeElement((Element)child, parentqname, jstream);
        if (n < children.length-1) {
          jstream.writeSeparator();
        }
      } else if (child instanceof TextValue) {
        TextValue textvalue = (TextValue) child;
        String value = textvalue.getText();
        if (!element.getMustPreserveWhitespace()) {
          if (!value.matches("\\s*")) {
            jstream.writeQuoted(value.trim());
            if (n < children.length-1) {
              jstream.writeSeparator();
            }
          }
        } else {
          jstream.writeQuoted(value);
          if (n < children.length-1) {
            jstream.writeSeparator();
          }
        }
      }
    }
    jstream.endArray();
  }

  private static void writeExtensions(
    ExtensibleElement element,
    JSONStream jstream)
      throws IOException {
    writeExtensions(element,jstream,true);
  }

  private static void writeExtensions(
    ExtensibleElement element,
    JSONStream jstream,
    boolean startsep)
      throws IOException {
    List<QName> attributes = element.getExtensionAttributes();
    writeList("extensions",element.getExtensions(),jstream);
    if (attributes.size() > 0) {
      jstream.writeField("attributes");

      jstream.startObject();
      for (int n = 0; n < attributes.size(); n++) {
        QName qname = attributes.get(n);
        jstream.writeField(getName(qname));
        if ("".equals(qname.getPrefix())  ||
            "xml".equals(qname.getPrefix())) {
          jstream.writeQuoted(element.getAttributeValue(qname));
        } else {
          jstream.startObject();
          jstream.writeField("attributes");
          jstream.startObject();
          jstream.writeField("xmlns:" + qname.getPrefix());
          jstream.writeQuoted(qname.getNamespaceURI());
          jstream.endObject();
          jstream.writeField("value");
          jstream.writeQuoted(element.getAttributeValue(qname));
          jstream.endObject();
        }
      }
      jstream.endObject();
    }
  }

  private static boolean needToWriteLanguageFields(Element element) {
    return
      needToWriteLang(element) ||
      needToWriteDir(element);
  }

  private static boolean needToWriteLang(Element element) {
    String parentlang = null;
    if (element.getParentElement() != null) {
      Base parent = element.getParentElement();
      parentlang = parent instanceof Document ?
        ((Document)parent).getLanguage() :
        ((Element)parent).getLanguage();
    }
    String lang = element.getLanguage();
    return parentlang == null && lang != null ||
           lang != null && parentlang != null && !parentlang.equalsIgnoreCase(lang);
  }

  private static boolean needToWriteDir(Element element) {
    Direction parentdir = Direction.UNSPECIFIED;
    Direction dir = BidiHelper.getDirection(element);
    if (element.getParentElement() != null) {
      Base parent = element.getParentElement();
      if (parent instanceof Element) {
        parentdir = BidiHelper.getDirection((Element)parent);
      }
    }
    return dir != Direction.UNSPECIFIED && !dir.equals(parentdir);
  }

  private static void writeLanguageFields(
    Element element,
    JSONStream jstream)
      throws IOException {
    if (needToWriteLang(element)) {
      String lang = element.getLanguage();
      jstream.writeField("lang",lang);
    }
    if (needToWriteDir(element)) {
      Direction dir = BidiHelper.getDirection(element);
      jstream.writeField("dir", dir.name().toLowerCase());
    }
  }

  private static void writeElement(
    String name,
    Element element,
    JSONStream jstream)
      throws IOException {
    if (element != null) {
      if (name.equalsIgnoreCase("feed") ||
          name.equalsIgnoreCase("entry")) {
        // skip writing this name
      } else if (name.equalsIgnoreCase("content")) {
        jstream.skipWritingFieldName();
      } else {
        jstream.writeField(name);
      }
      toJson(element,jstream);
    }
  }

  private static boolean writeList(
    String name,
    List list,
    JSONStream jstream)
      throws IOException {
    if (list == null || list.size() == 0) {
      return false;
    }
    jstream.writeField(name);
    jstream.startArray();
    for (int n = 0; n < list.size(); n++) {
      Element el = (Element)list.get(n);
      if (!(el instanceof InReplyTo) &&
          !(el instanceof Control)) {
        toJson(el,jstream);
        if (n < list.size()-1) {
          jstream.writeSeparator();
        }
      }
    }
    jstream.endArray();
    return true;
  }

  private static void toJson(
    Document document,
    JSONStream jstream)
      throws IOException {

    Element root = document.getRoot();
    if (root != null &&
        root.getQName().getLocalPart().equalsIgnoreCase("entry")) {
      writeElement("entry", document.getRoot(), jstream);
      return;
    }

    jstream.startObject();
    jstream.writeField("base", document.getBaseUri());
    jstream.writeField("content-type", document.getContentType());
    jstream.writeField("etag", document.getEntityTag());
    jstream.writeField("language", document.getLanguage());
    jstream.writeField("slug", document.getSlug());
    jstream.writeField("last-modified", document.getLastModified());

    if (root != null) {
      String rootname = root.getQName().getLocalPart();
      writeElement(rootname,document.getRoot(),jstream);
    }
    jstream.endObject();
  }


  private static Object[] getChildren(
    Element element) {
      Abdera abdera = element.getFactory().getAbdera();
      XPath xpath = abdera.getXPath();
      List<Object> nodes = xpath.selectNodes("node()", element);
      return nodes.toArray(new Object[nodes.size()]);
  }

  private static boolean isSameNamespace(
    QName q1,
    QName q2) {
      if (q1 == null && q2 != null) {
        return false;
      }
      if (q1 != null && q2 == null) {
        return false;
      }
      String p1 = q1 == null ? "" :
        q1.getPrefix() != null ? q1.getPrefix() : "";
      String p2 = q2 == null ? "" :
        q2.getPrefix() != null ? q2.getPrefix() : "";
      String n1 = q1 == null ? "" :
        q1.getNamespaceURI() != null ? q1.getNamespaceURI() : "";
      String n2 = q2 == null ? "" :
        q2.getNamespaceURI() != null ? q2.getNamespaceURI() : "";
      return n1.equals(n2) && p1.equals(p2);
  }

}
