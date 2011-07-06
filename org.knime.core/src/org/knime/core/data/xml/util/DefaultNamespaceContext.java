/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   09.03.2011 (hofer): created
 */
package org.knime.core.data.xml.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * A default implementation of {@link NamespaceContext}. The JDK does not
 * provide a default implementation.
 * 
 * @author Heiko Hofer
 */
public class DefaultNamespaceContext implements NamespaceContext {
	private final Map<String, String> m_namespaces;
	private final Map<String, List<String>> m_nsRevers;

	/**
	 * The context with the mapping prefixes[i] matches namespaces[i]
	 * 
	 * @param prefixes the namespace prefixes
	 * @param namespaces the namespaces
	 */
	public DefaultNamespaceContext(final String[] prefixes,
			final String[] namespaces) {
		m_namespaces = new HashMap<String, String>();
		for (int i = 0; i < prefixes.length; i++) {
			if (m_namespaces.containsKey(prefixes[i])) {
				throw new IllegalArgumentException("Duplicated "
						+ "namespace prefix.");
			}
			m_namespaces.put(prefixes[i], namespaces[i]);
		}
		// Ensure if "xml" is bound to the correct namespace
		if (m_namespaces.containsKey(XMLConstants.XML_NS_PREFIX)) {
			if (!m_namespaces.get(XMLConstants.XML_NS_PREFIX).equals(
					XMLConstants.XML_NS_URI)) {
				throw new IllegalArgumentException("The prefix "
						+ XMLConstants.XML_NS_PREFIX + "can only be "
						+ "bound to the namespace " + XMLConstants.XML_NS_URI);
			}
		} else {
			m_namespaces.put(XMLConstants.XML_NS_PREFIX,
					XMLConstants.XML_NS_URI);
		}
		// Ensure if "xmlns" is bound to the correct namespace
		if (m_namespaces.containsKey(XMLConstants.XMLNS_ATTRIBUTE)) {
			if (!m_namespaces.get(XMLConstants.XMLNS_ATTRIBUTE).equals(
					XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
				throw new IllegalArgumentException("The prefix "
						+ XMLConstants.XMLNS_ATTRIBUTE + "can only be "
						+ "bound to the namespace "
						+ XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
			}
		} else {
			m_namespaces.put(XMLConstants.XMLNS_ATTRIBUTE,
					XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
		}
		// create reverse mapping
		m_nsRevers = new HashMap<String, List<String>>();
		for (Entry<String, String> entry : m_namespaces.entrySet()) {
			String ns = entry.getValue();
			String prefix = entry.getKey();
			if (m_nsRevers.containsKey(ns)) {
				List<String> list = m_nsRevers.get(ns);
				list.add(prefix);
			} else {
				List<String> list = new ArrayList<String>();
				list.add(prefix);
				m_nsRevers.put(ns, list);
			}
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNamespaceURI(final String prefix) {
		if (prefix == null)
			throw new IllegalArgumentException("Null prefix");
		if (m_namespaces.containsKey(prefix)) {
			return m_namespaces.get(prefix);
		} else {
			return XMLConstants.NULL_NS_URI;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPrefix(final String uri) {
		if (uri == null)
			throw new IllegalArgumentException("Null uri");
		if (m_nsRevers.containsKey(uri)) {
			return m_nsRevers.get(uri).get(0);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Iterator getPrefixes(final String uri) {
		if (uri == null)
			throw new IllegalArgumentException("Null uri");
		if (m_nsRevers.containsKey(uri)) {
			return m_nsRevers.get(uri).iterator();
		} else {
			return Collections.EMPTY_LIST.iterator();
		}
	}
}
