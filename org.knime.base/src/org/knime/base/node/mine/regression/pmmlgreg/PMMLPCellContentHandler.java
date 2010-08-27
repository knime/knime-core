/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   27.04.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;

import java.util.List;
import java.util.Stack;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author Heiko Hofer
 */
final class PMMLPCellContentHandler  extends PMMLContentHandler {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PMMLPCellContentHandler.class);

    private Stack<PMMLContentHandler> m_contentHandlerStack;
    private List<PMMLPCell> m_pCellList;
    private PMMLPCell m_pCell;

    /**
     * @param contentHandlerStack stack of content handlers
     * @param atts the attributes of a PMMLPCell
     * @param pCellList the list is used to store the read PMMLPCell
     */
    public PMMLPCellContentHandler(
            final Stack<PMMLContentHandler> contentHandlerStack,
            final Attributes atts,
            final List<PMMLPCell> pCellList) {
        m_contentHandlerStack = contentHandlerStack;
        m_pCellList = pCellList;

        double beta = Double.valueOf(atts.getValue("beta"));
        if (null != atts.getValue("df")) {
            int df = Integer.valueOf(atts.getValue("df"));
            m_pCell = new PMMLPCell(
                    atts.getValue("parameterName"),
                    beta,
                    df,
                    atts.getValue("targetCategory")
                    );
        } else {
            m_pCell = new PMMLPCell(
                    atts.getValue("parameterName"),
                    beta,
                    atts.getValue("targetCategory")
                    );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name)
            throws SAXException {
        if ("Extension".equals(name)) {
            // sub element, ignore
        } else if ("PCell".equals(name)) {
            PMMLContentHandler me = m_contentHandlerStack.pop();
            assert me == this : "Invalid content handler stack";
            m_pCellList.add(m_pCell);
        } else {
            throw new SAXException("Invalid end of element: " + name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri,
            final String localName, final String name,
            final Attributes atts) throws SAXException {
        if ("Extension".equals(name)) {
            LOGGER.debug("Skipping unknown extension in Parameter: " + name);
        } else {
            throw new SAXException("Unknown xml element in Parameter: "
                    + name);
        }
    }

}
