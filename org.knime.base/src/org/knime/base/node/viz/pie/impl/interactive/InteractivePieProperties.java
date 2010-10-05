/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 *
 * History
 *    21.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.impl.interactive;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.border.Border;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieVizModel;
import org.knime.base.node.viz.pie.impl.PieProperties;
import org.knime.base.node.viz.pie.node.PieNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * The interactive implementation of the {@link PieProperties} panel which
 * allows the changing of the pie and aggregation column.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieProperties
    extends PieProperties<InteractivePieVizModel> {

    private static final long serialVersionUID = -4836394731936769733L;

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(InteractivePieProperties.class);

//    private static final String AGGREGATION_COLUMN_DISABLED_TOOLTIP =
//        "Not available for aggregation method count";
//
//    private static final String AGGREGATION_COLUMN_ENABLED_TOOLTIP =
//        "Select the column used for aggregation";

    private final ColumnSelectionPanel m_pieCol;

    private final ColumnSelectionPanel m_aggrCol;

    /**Constructor for class FixedPieProperties.
     * @param vizModel the visualization model to initialize all swing
     * components
     */
    public InteractivePieProperties(final InteractivePieVizModel vizModel) {
        super(vizModel);
        // the column select boxes for the X axis
        m_pieCol = new ColumnSelectionPanel((Border)null,
                PieNodeModel.PIE_COLUMN_FILTER);
        m_pieCol.setBackground(this.getBackground());

        m_aggrCol = new ColumnSelectionPanel((Border)null,
                PieNodeModel.AGGREGATION_COLUMN_FILTER, true);
        m_aggrCol.setBackground(this.getBackground());
        m_aggrCol.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                enableAggrMethodGroup(getSelectedAggrColumn() != null);
            }
        });
        updatePanel(vizModel);
        onSelectAggrMethod(vizModel.getAggregationMethod());
        super.addColumnTab(m_pieCol, m_aggrCol);
    }

    /**
     * @param listener adds the listener to the x column select box.
     */
    protected void addPieColumnChangeListener(final ActionListener listener) {
            m_pieCol.addActionListener(listener);
    }

    /**
     * @param listener adds the listener to the x column select box.
     */
    protected void addAggrColumnChangeListener(final ActionListener listener) {
            m_aggrCol.addActionListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectAggrMethod(final AggregationMethod aggrMethod) {
//        if (AggregationMethod.COUNT.equals(aggrMethod)) {
//            m_aggrCol.setEnabled(false);
//            m_aggrCol.setToolTipText(
//                    AGGREGATION_COLUMN_DISABLED_TOOLTIP);
//        } else {
//            m_aggrCol.setEnabled(true);
//            m_aggrCol.setToolTipText(
//                    AGGREGATION_COLUMN_ENABLED_TOOLTIP);
//        }
    }

    /**
     * @return the name of the selected pie column
     */
    public String getSelectedPieColumn() {
        return m_pieCol.getSelectedColumn();
    }

    /**
     * @return the name of the selected aggregation column
     */
    protected String getSelectedAggrColumn() {
        return m_aggrCol.getSelectedColumn();
    }

    /**
     * This method is called when the user has changed the pie or aggregation
     * column.
     * @param vizModel the actual visualization model
     */
    @Override
    protected void updatePanelInternal(final InteractivePieVizModel vizModel) {
        final DataTableSpec tableSpec = vizModel.getTableSpec();
        updateColBox(m_pieCol, tableSpec, vizModel.getPieColumnName());
        updateColBox(m_aggrCol, tableSpec, vizModel.getAggregationColumnName());
    }

    private static void updateColBox(final ColumnSelectionPanel box,
            final DataTableSpec spec, final String selection) {
        if (box == null) {
            return;
        }
        try {
            //...update the column select box
            box.update(spec, selection);
        } catch (final NotConfigurableException e) {
            LOGGER.warn("Exception updating columns in properties panel: "
                    + e.getMessage());
        }
    }
}
