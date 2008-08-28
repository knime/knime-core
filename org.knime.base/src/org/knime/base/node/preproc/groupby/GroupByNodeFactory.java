/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 *  History
 *      28.06.2007 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.groupby;

import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory class of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class GroupByNodeFactory extends GenericNodeFactory<GroupByNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected GenericNodeDialogPane createNodeDialogPane() {
        return new GroupByNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupByNodeModel createNodeModel() {
        return new GroupByNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public NodeView createNodeView(final int viewIndex,
            final GroupByNodeModel nodeModel) {
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
