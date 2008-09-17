/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
