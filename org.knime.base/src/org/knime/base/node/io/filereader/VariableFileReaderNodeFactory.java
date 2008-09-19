/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   19.09.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.NodeDialogPane;

/**
 *
 * @author ohl, University of Konstanz
 */
public class VariableFileReaderNodeFactory extends
        NodeFactory<VariableFileReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new VariableFileReaderNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VariableFileReaderNodeModel createNodeModel() {
        return new VariableFileReaderNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<VariableFileReaderNodeModel> createNodeView(
            final int index, final VariableFileReaderNodeModel model) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
