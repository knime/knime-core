/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
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
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        // TODO Auto-generated method stub

    }

}
