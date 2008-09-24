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

import java.awt.BorderLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.knime.core.node.NodeView;
import org.knime.core.node.NodeLogger;
import org.xml.sax.InputSource;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectView extends JComponent {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLPortObjectView.class);
    
    private final PMMLPortObject m_portObject;
    
    private final Object m_lock = new Object();
    
    private final JTree m_tree;
    
    /**
     * Displays the XML tree of the PMML.
     * 
     * @param portObject the object to display
     */
    public PMMLPortObjectView(final PMMLPortObject portObject) {
        setLayout(new BorderLayout());
        setBackground(NodeView.COLOR_BACKGROUND);
        m_portObject = portObject;
        if (portObject.getModelType() == null) {            
            setName("Unknown PMML model");
        } else {
            setName("PMML: " + portObject.getModelType().name());
        }
        m_tree = new JTree();
        create();
    }

    private void create() {
        // serialize port object
            synchronized (m_lock) {
                try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                m_portObject.save(out);
                SAXParserFactory saxFac = SAXParserFactory.newInstance();
                SAXParser parser = saxFac.newSAXParser();
                XMLTreeCreator treeCreator = new XMLTreeCreator();
                parser.parse(new InputSource(new ByteArrayInputStream(
                        out.toByteArray())), treeCreator);
                m_tree.setModel(new DefaultTreeModel(
                        treeCreator.getTreeNode()));
                add(new JScrollPane(m_tree));
                revalidate();
//                JTree tree = new JTree(treeCreator.getTreeNode());
//                add(tree);
            } catch (Exception e) {
                // log and return a "error during saving" component
                LOGGER.error("PMML contains errors", e);
                PMMLPortObjectView.this.add(
                        new JLabel("PMML contains errors: " + e.getMessage()));
            }
        }
    }
    

}
