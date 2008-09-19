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
 * -------------------------------------------------------------------
 *
 * History
 *   27.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.predictor;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 *
 * @author Michael Berthold, University of Konstanz
 */
public class DecTreePredictorNodeFactory 
        extends NodeFactory<DecTreePredictorNodeModel> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DecTreePredictorNodeModel createNodeModel() {
        return new DecTreePredictorNodeModel();
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
    public NodeView<DecTreePredictorNodeModel> createNodeView(final int viewIndex,
            final DecTreePredictorNodeModel nodeModel) {
        return new DecTreePredictorNodeView(nodeModel);
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
        return new DefaultNodeSettingsPane() {
            {
                addDialogComponent(new DialogComponentNumber(
                        new SettingsModelIntegerBounded(
              /* config-name: */DecTreePredictorNodeModel.MAXCOVERED,
              /* default */50000,
                      /* min: */0,
                      /* max: */100000),
                   /* label: */"Maximum number of stored patterns "
                                + "for HiLite-ing: ", 100));
            }
        };
    }
}
