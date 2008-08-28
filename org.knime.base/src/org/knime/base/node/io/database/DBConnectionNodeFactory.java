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
 */
package org.knime.base.node.io.database;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DBConnectionNodeFactory 
        extends GenericNodeFactory<DBConnectionNodeModel> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DBConnectionNodeModel createNodeModel() {
        return new DBConnectionNodeModel(
                new PortType[]{DatabasePortObject.TYPE}, 
                new PortType[]{BufferedDataTable.TYPE});
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
    public GenericNodeView<DBConnectionNodeModel> createNodeView(
            final int viewIndex,
            final DBConnectionNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeDialogPane createNodeDialogPane() {
        return null;
    }
}
