/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.port.pmml;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses XML and returns a model of the parsed tree that can be displayed 
 * with a {@link JTree}. 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class XMLTreeCreator extends DefaultHandler {
    
    
    private DefaultMutableTreeNode m_currentNode;

    private StringBuffer m_buffer = new StringBuffer();
    
    /**
     * 
     * @return the tree representing the parsed XML tree 
     */
    public TreeNode getTreeNode() {
        return m_currentNode.getRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // TODO: somewhere display the content between elements!
        m_buffer.append(ch, start, length);
        super.characters(ch, start, length);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName, 
            final String name)
            throws SAXException {
        if (m_currentNode.getParent() != null) {
            if (!m_buffer.toString().trim().isEmpty()) {
                m_currentNode.add(new DefaultMutableTreeNode(
                        m_buffer.toString()));
                m_buffer = new StringBuffer();
            }
            m_currentNode = (DefaultMutableTreeNode)m_currentNode.getParent();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName, 
            final String name, final Attributes attributes) 
        throws SAXException {
        StringBuilder builder = new StringBuilder(name);
        for (int i = 0; i < attributes.getLength(); i++) {
            builder.append(" " + attributes.getQName(i) 
                    + "=\"" + attributes.getValue(i) + "\"");
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                builder.toString());
        if (m_currentNode != null) {
            m_currentNode.add(node);
        }
        m_currentNode = node;
        m_buffer = new StringBuffer();
    }

    

}
