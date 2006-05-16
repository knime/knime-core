/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   17.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffwriter;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class ARFFWriterNodeFactory extends NodeFactory {

    private final String m_file;
    
    /**
     * new factory - no default file.
     */
    public ARFFWriterNodeFactory() {
        m_file = null;
    }
    
    /**
     * new ARFF factory with default output file.
     * @param defFile the default file to write to.
     */
    public ARFFWriterNodeFactory(final String defFile) {
        m_file = defFile;
    }
    
    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    public NodeDialogPane createNodeDialogPane() {
        return new ARFFWriterNodeDialog();
    }
    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeModel()
     */
    public NodeModel createNodeModel() {
        if (m_file == null) {
            return new ARFFWriterNodeModel();
        } else {
            return new ARFFWriterNodeModel(m_file);
        }
    }
    /**
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    public NodeView createNodeView(final int viewIndex, 
            final NodeModel nodeModel) {
        return null;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#getNrNodeViews()
     */
    public int getNrNodeViews() {
        return 0;
    }
    /**
     * @see de.unikn.knime.core.node.NodeFactory#hasDialog()
     */
    public boolean hasDialog() {
        return true;
    }
}
