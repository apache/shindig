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
package org.apache.shindig.protocol.conversion.xstream;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.shindig.protocol.model.ExtendableBeanImpl;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Serializes an ExtendableBeanImpl type as a Map instead of a POJO.
 *
 * Note: For an ExtendableBean field within a POJO to be properly serialized to
 * XML, the field must still be hardcoded into the POJO, even if the POJO itself
 * is an ExtendableBean. This is because POJOs are still serialized to XML
 * normally to preserve custom formatting (hence why this class ONLY converts
 * ExtendableBeanImpl). If xstream does not expect the ExtendableBean field, it
 * will not serialize it. This does not, however, affect JSON serialization,
 * which always works without predefining the field when extending
 * ExtendableBean.
 */
public class ExtendableBeanConverter implements Converter {

	@SuppressWarnings("rawtypes")
	public boolean canConvert(Class clazz) {
		return clazz.equals(ExtendableBeanImpl.class);
	}

	@SuppressWarnings("rawtypes")
	public void marshal(Object value, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		AbstractMap map = (AbstractMap) value;
		for (Object obj : map.entrySet()) {
			Entry entry = (Entry) obj;
			writer.startNode(entry.getKey().toString());
			if (entry.getValue() instanceof String) {
				writer.setValue(entry.getValue().toString());
			} else if (entry.getValue() instanceof Map) {
				marshal(entry.getValue(), writer, context);
			} else {
				context.convertAnother(entry.getValue());
			}
			writer.endNode();
		}
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		return null; // XML POST not supported
	}
}
