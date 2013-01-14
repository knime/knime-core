/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
 * History
 *   May 19, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableView;

/**
 * 
 * @author ritmeier, University of Konstanz
 */
public class DiffNodeView extends NodeView {

    private TableContentModel m_tableModel;
    private TableView m_tableView;

    /**
     * @param nodeModel
     */
    public DiffNodeView(final DiffNodeModel nodeModel) {
        super(nodeModel);
        if (nodeModel == null) {
            throw new NullPointerException("Model must not be null.");
        }
        m_tableModel = new TableContentModel(nodeModel.getDiffTable());
        m_tableView = new TableView(m_tableModel);
        setComponent(m_tableView);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // TODO Auto-generated method stub

    }

}
