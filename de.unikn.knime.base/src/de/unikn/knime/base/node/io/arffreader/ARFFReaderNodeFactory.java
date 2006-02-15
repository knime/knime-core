/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 *   11.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffreader;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class ARFFReaderNodeFactory extends NodeFactory {

    private final String m_fileURL;
    
    /**
     * will deliver a model with no default file set.
     */
    public ARFFReaderNodeFactory() {
        m_fileURL = null;
    }

    /**
     * this factory will create a model with the file set as default file.
     * @param fileURL a valid URL to the default ARFF file.
     */
    public ARFFReaderNodeFactory(final String fileURL) {
        m_fileURL = fileURL;
    }
    
    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    public NodeDialogPane createNodeDialogPane() {
        return new ARFFReaderNodeDialog();
    }
    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeModel()
     */
    public NodeModel createNodeModel() {
        if (m_fileURL == null) {
            return new ARFFReaderNodeModel();
        } else {
            return new ARFFReaderNodeModel(m_fileURL);
        }
    }
    /**
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    public NodeView createNodeView(final int viewIndex, 
            final NodeModel nodeModel) {
        assert false;
        return null;
    }
    /**
     * @see de.unikn.knime.core.node.NodeFactory#getNodeName()
     */
    public String getNodeName() {
        return "ARFF Reader";
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
