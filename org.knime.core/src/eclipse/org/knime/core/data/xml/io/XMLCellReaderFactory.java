/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   09.03.2011 (hofer): created
 */
package org.knime.core.data.xml.io;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.node.KNIMEConstants;

/**
 * Factory class for {@link XMLCellReader}.
 *
 * @author Heiko Hofer
 */
public class XMLCellReaderFactory {
	/**
	 * Creates a {@link XMLCellReader} to read a single cell from given
	 * {@link InputStream}
	 *
	 * @param is the xml document
	 * @return {@link XMLCellReader} to read a single cell from given
	 * {@link InputStream}
	 * @throws ParserConfigurationException when the factory object for
	 * DOMs could not be created.
	 */
	public static XMLCellReader createXMLCellReader(final InputStream is)
			throws ParserConfigurationException {
		return new XMLDOMCellReader(is);
	}

   /**
     * Creates a {@link XMLCellReader} to read a single cell from given
     * {@link Reader}
     *
     * @param reader a reader for the xml document
     * @return {@link XMLCellReader} to read a single cell from given
     * {@link InputStream}
     * @throws ParserConfigurationException when the factory object for
     * DOMs could not be created.
     * @since 2.8
     */
    public static XMLCellReader createXMLCellReader(final Reader reader)
            throws ParserConfigurationException {
        return new XMLDOMCellReader(reader);
    }


	/**
	 * Creates a {@link XMLCellReader} to read nodes matching the given limited
	 * XPath. Every node is read in a single DataCell whereas namespaces,
	 * xml:base, xml:space and xml:lang definitions are retained.
	 *
	 * WARNING: This could read external
	 * XML entities depending on {@link KNIMEConstants#PROPERTY_XML_DISABLE_EXT_ENTITIES}
	 *
	 * @param is the xml document
	 * @param xpathMatcher Only nodes that match are read
	 * @return {@link XMLCellReader} to read nodes matching the given limited
	 * XPath. Every node is read in a single DataCell.
	 * @throws ParserConfigurationException when the factory object for
	 * DOMs could not be created.
	 * @throws XMLStreamException when parser could not be configured
	 */
	public static XMLCellReader createXPathXMLCellReader(final InputStream is,
			final LimitedXPathMatcher xpathMatcher)
			throws ParserConfigurationException, XMLStreamException {
		return new XMLXpathCellReader(is, xpathMatcher);
	}

	/**
     * Creates a {@link XMLCellReader} to read nodes matching the given limited
     * XPath. Every node is read in a single DataCell whereas namespaces,
     * xml:base, xml:space and xml:lang definitions are retained. This completely
     * disables reading external entities in XML.
     *
     * @param is the xml document
     * @param xpathMatcher Only nodes that match are read
     * @return {@link XMLCellReader} to read nodes matching the given limited
     * XPath. Every node is read in a single DataCell.
     * @throws ParserConfigurationException when the factory object for
     * DOMs could not be created.
     * @throws XMLStreamException when parser could not be configured
     * @since 4.5
     */
    public static XMLCellReader createXPathXMLCellReader2(final InputStream is,
            final LimitedXPathMatcher xpathMatcher)
            throws ParserConfigurationException, XMLStreamException {
        return new XMLXpathCellReader(is, xpathMatcher,true);
    }
}
