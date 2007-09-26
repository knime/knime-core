/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
import org.knime.core.node.util.ColumnSelectionComboxBox;

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

    private final ColumnSelectionComboxBox m_pieCol;

    private final ColumnSelectionComboxBox m_aggrCol;

    /**Constructor for class FixedPieProperties.
     * @param vizModel the visualization model to initialize all swing
     * components
     */
    public InteractivePieProperties(final InteractivePieVizModel vizModel) {
        super(vizModel);
        // the column select boxes for the X axis
        m_pieCol = new ColumnSelectionComboxBox((Border)null,
                PieNodeModel.PIE_COLUMN_FILTER);
        m_pieCol.setBackground(this.getBackground());

        m_aggrCol = new ColumnSelectionComboxBox((Border)null,
                PieNodeModel.AGGREGATION_COLUMN_FILTER);
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
     * The columns settings panel which contains the x column
     * and aggregation column selection box.
     *
     * @return the columns selection panel
     */
//    private Box createColumnSettingsBox() {
////the x column box
//        final Box xColumnBox = Box.createHorizontalBox();
////        xColumnBox.setBorder(BorderFactory
////                .createEtchedBorder(EtchedBorder.RAISED));
//        final JLabel xColLabelLabel =
//            new JLabel(PieProperties.PIE_COLUMN_LABEL);
//        xColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//        xColumnBox.add(xColLabelLabel);
//        xColumnBox.add(Box.createHorizontalGlue());
//        xColumnBox.add(m_pieCol);
//        xColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
////the aggregation column box
//        final Box aggrColumnBox = Box.createHorizontalBox();
////      xColumnBox.setBorder(BorderFactory
////              .createEtchedBorder(EtchedBorder.RAISED));
//      final JLabel aggrColLabelLabel =
//          new JLabel(PieProperties.AGGREGATION_COLUMN_LABEL);
//      aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//      aggrColumnBox.add(aggrColLabelLabel);
//      aggrColumnBox.add(Box.createHorizontalGlue());
//      aggrColumnBox.add(m_aggrCol);
//      aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
////        final Box aggrColumnBox = Box.createHorizontalBox();
////        aggrColumnBox.add(m_aggrCol);
////        aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//
////the box which surround both column selection boxes
//        final Box columnsBox = Box.createVerticalBox();
//        columnsBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
//        columnsBox.add(Box.createVerticalGlue());
//        columnsBox.add(xColumnBox);
//        columnsBox.add(Box.createVerticalGlue());
//        columnsBox.add(aggrColumnBox);
//        columnsBox.add(Box.createVerticalGlue());
//        return columnsBox;
//    }

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

    private static void updateColBox(final ColumnSelectionComboxBox box,
            final DataTableSpec spec, final String selection) {
        if (box == null) {
            return;
        }
        try {
            //remove all action listener to avoid unnecessary calls...
            final ActionListener[] listeners = box.getActionListeners();
            for (final ActionListener listener : listeners) {
                box.removeActionListener(listener);
            }
            //...update the column select box
            box.update(spec, selection);
            //...and add the removed listeners
            for (final ActionListener listener : listeners) {
                box.addActionListener(listener);
            }
        } catch (final NotConfigurableException e) {
            LOGGER.warn("Exception updating columns in properties panel: "
                    + e.getMessage());
        }
    }
}
