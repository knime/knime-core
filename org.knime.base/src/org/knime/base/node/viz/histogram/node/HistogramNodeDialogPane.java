/*
 * ------------------------------------------------------------------
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import java.awt.Dimension;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.util.AggregationColumnDialogComponent;
import org.knime.base.node.viz.histogram.util.SettingsModelColorNameColumns;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog of the {@link HistogramNodeModel} where the user can
 * define the x column and the number of rows. 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeDialogPane extends DefaultNodeSettingsPane {

    private static final String ALL_ROWS_LABEL = "Display all rows";

    private static final String NO_OF_ROWS_LABEL = "No. of rows to display:";

    private static final String X_COL_SEL_LABEL = "X column:";

    private static final String AGGR_COL_SEL_LABEL = "Aggregation column:";

    private final SettingsModelIntegerBounded m_noOfRowsModel;

    private final SettingsModelBoolean m_allRowsModel;
    /**
     * Constructor for class HistogramNodeDialogPane.
     * 
     */
    @SuppressWarnings("unchecked")
    protected HistogramNodeDialogPane() {
        super();
        createNewGroup("Rows to display:");
        m_noOfRowsModel = new SettingsModelIntegerBounded(
                AbstractHistogramNodeModel.CFGKEY_NO_OF_ROWS,
                AbstractHistogramNodeModel.DEFAULT_NO_OF_ROWS, 1,
                Integer.MAX_VALUE);
        final DialogComponentNumber noOfRowsComp = 
            new DialogComponentNumber(m_noOfRowsModel,
                NO_OF_ROWS_LABEL, 1);
        m_allRowsModel = new SettingsModelBoolean(
                AbstractHistogramNodeModel.CFGKEY_ALL_ROWS, false);
        m_allRowsModel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfRowsModel.setEnabled(!m_allRowsModel.getBooleanValue());
            }
        });
        final DialogComponentBoolean allRowsComp = 
            new DialogComponentBoolean(m_allRowsModel, ALL_ROWS_LABEL);
        addDialogComponent(allRowsComp);
        addDialogComponent(noOfRowsComp);

        createNewGroup("Column selection:");
        //the x column select box
        addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(
                        AbstractHistogramNodeModel.CFGKEY_X_COLNAME, ""),
                HistogramNodeDialogPane.X_COL_SEL_LABEL, 0, true, 
                AbstractHistogramPlotter.X_COLUMN_FILTER));

        //the aggregation column select box
//        addDialogComponent(new DialogComponentColumnNameSelection(
//                ,
//                AGGR_COL_SEL_LABEL, 0, false, 
//                AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER));
        final AggregationColumnDialogComponent aggrCols = 
            new AggregationColumnDialogComponent(AGGR_COL_SEL_LABEL,
                    new SettingsModelColorNameColumns(
                        AbstractHistogramNodeModel.CFGKEY_AGGR_COLNAME, null),
                        new Dimension(150, 155), 
                AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER);
        addDialogComponent(aggrCols);
    }
}
