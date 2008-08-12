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
package org.knime.base.node.io.pmml.read;

import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLReaderNodeFactory 
    extends GenericNodeFactory<PMMLReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected GenericNodeDialogPane createNodeDialogPane() {
        return new PMMLReaderNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLReaderNodeModel createNodeModel() {
        return new PMMLReaderNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeView<PMMLReaderNodeModel> createNodeView(
            final int viewIndex,
            final PMMLReaderNodeModel nodeModel) {
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
