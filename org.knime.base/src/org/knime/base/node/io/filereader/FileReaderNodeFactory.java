/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.io.filereader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * @author Peter Ohl, University of Konstanz
 */
public class FileReaderNodeFactory extends NodeFactory {

    private String m_defaultXMLFile;

    /**
     * @param defXMLFileName this string will be set as default path to a XML
     *            file containing settings for the dialog. Won't be supported in
     *            the future anymore.
     */
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        throw new IllegalStateException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new FileReaderNodeDialog();
    }
}
