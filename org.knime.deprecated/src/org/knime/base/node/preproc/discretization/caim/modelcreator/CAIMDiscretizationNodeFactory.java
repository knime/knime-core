/*
 * @(#)$RCSfile$ 
 * $Revision: 4973 $ $Date: 2006-08-01 12:15:56 +0200 (Di, 01 Aug 2006) $ $
 * --------------------------------------------------------------------- *
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
 */
package org.knime.base.node.preproc.discretization.caim.modelcreator;

import org.knime.base.node.preproc.discretization.caim2.modelcreator.BinModelNodeView;
import org.knime.base.node.preproc.discretization.caim2.modelcreator.BinModelPlotter;
import org.knime.base.node.preproc.discretization.caim2.modelcreator.CAIMDiscretizationNodeDialog;
import org.knime.base.node.preproc.discretization.caim2.modelcreator.CAIMDiscretizationNodeModel;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeDialogPane;

/**
 * The Factory for the CAIM Discretizer.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class CAIMDiscretizationNodeFactory 
        extends GenericNodeFactory<CAIMDiscretizationNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public CAIMDiscretizationNodeModel createNodeModel() {
        return new CAIMDiscretizationNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeView<CAIMDiscretizationNodeModel> createNodeView(final int viewIndex, 
            final CAIMDiscretizationNodeModel nodeModel) {
        return new BinModelNodeView(nodeModel, 
                new BinModelPlotter());
    }
    
    /**
     * @return <b>true</b>.
     * @see org.knime.core.node.NodeFactory#hasDialog()
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
        return new CAIMDiscretizationNodeDialog();
    }

}
