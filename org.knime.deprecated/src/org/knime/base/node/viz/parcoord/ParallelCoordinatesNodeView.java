/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.base.node.viz.parcoord;

import java.awt.Color;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * View showing the Parallel Coordinates panel.
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class ParallelCoordinatesNodeView extends NodeView {

    /**
     * Comment for <code>m_panel</code> the panel.
     */
    private ParallelCoordinatesViewPanel m_panel;

    /**
     * @param nodeModel the ParallelCoordinatesNodeModel
     */
    ParallelCoordinatesNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        m_panel = new ParallelCoordinatesViewPanel();
        this.setComponent(m_panel);
        m_panel.setBackground(Color.WHITE);
        //m_panel.createMenu(this.getJMenuBar());
        this.getJMenuBar().add(m_panel.createHiLiteMenu());
        this.getJMenuBar().add(m_panel.createViewTypeMenu());
        this.getJMenuBar().add(m_panel.createCoordinateOrderMenu());
        modelChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        final ParallelCoordinatesNodeModel parModel 
            = (ParallelCoordinatesNodeModel)getNodeModel();
        if (parModel.getViewContent() != null) {
            m_panel.setNewModel(parModel.getViewContent(), parModel
                    .getInHiLiteHandler(0), null);

            //setting view type to ALL_VISIBLE
            m_panel.setViewType(ParallelCoordinatesViewPanel.ALL_VISIBLE);
        } else {
            m_panel.setNewModel(null, null, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        HiLiteHandler hdl = super.getNodeModel().getInHiLiteHandler(0);
        if (hdl != null) {
            hdl.removeHiLiteListener(m_panel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }
}
