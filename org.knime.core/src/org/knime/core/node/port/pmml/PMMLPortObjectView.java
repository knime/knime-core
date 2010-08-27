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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
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
