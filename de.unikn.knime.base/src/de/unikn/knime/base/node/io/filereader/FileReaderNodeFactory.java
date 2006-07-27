/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.io.filereader;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * @author Peter Ohl, University of Konstanz
 */
public class FileReaderNodeFactory extends NodeFactory {

    private String m_defaultXMLFile;

    /**
     * @param defXMLFileName this string will be set as default path to a XML
     *            file containing settings for the dialog. Won't be supported in
     *            the future anymore.
     * @deprecated use the standard constructor instead
     */
    @Deprecated
    public FileReaderNodeFactory(final String defXMLFileName) {
        m_defaultXMLFile = defXMLFileName;
    }

    /**
     * Default constructor.
     */
    public FileReaderNodeFactory() {
        m_defaultXMLFile = null;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        if (m_defaultXMLFile == null) {
            return new FileReaderNodeModel();
        } else {
            return new FileReaderNodeModel(m_defaultXMLFile);
        }
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeView(int,NodeModel)
     */
    @Override
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        throw new IllegalStateException();
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new FileReaderNodeDialog();
    }
}
