/* Created on Jun 13, 2006 4:22:14 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateView extends NodeView {
    private final TableView m_table;
    
    /**
     * Creates a new view for the cross validation node.
     * 
     * @param nodeModel the cross validation node model
     */
    public XValidateView(final NodeModel nodeModel) {
        super(nodeModel);
        m_table = new TableView();
        m_table.setShowColorInfo(false);
        setComponent(m_table);
    }

    /**
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        m_table.setDataTable(((XValidateModel) getNodeModel())
                .getConfusionMatrix());
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        // nothing to do
    }
}
