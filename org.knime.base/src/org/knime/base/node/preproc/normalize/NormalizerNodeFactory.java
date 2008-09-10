/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 * 
 * History
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize;

import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;

/**
 * Factory class for the Normalize Node.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class NormalizerNodeFactory 
extends GenericNodeFactory<NormalizerNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeDialogPane createNodeDialogPane() {
        return new NormalizerNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NormalizerNodeModel createNodeModel() {
        return new NormalizerNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeView<NormalizerNodeModel> createNodeView(
            final int viewIndex, final NormalizerNodeModel nodeModel) {
        return null;
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
    public boolean hasDialog() {
        return true;
    }
}
