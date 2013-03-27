/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
