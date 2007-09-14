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
package org.knime.base.node.viz.pie.node.fixed;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.PieDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.util.PieColumnFilter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;


/**
 * The NodeModel class of the interactive histogram plotter.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class PieNodeModel extends NodeModel {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PieNodeModel.class);

    /**Default number of rows to use.*/
    protected static final int DEFAULT_NO_OF_ROWS = 2500;
    /**Settings name for the take all rows select box.*/
    protected static final String CFGKEY_ALL_ROWS = "allRows";
    /**Settings name of the number of rows.*/
    protected static final String CFGKEY_NO_OF_ROWS = "noOfRows";
    /**Used to store the attribute column name in the settings.*/
    protected static final String CFGKEY_PIE_COLNAME = "PieColName";
    /**Settings name of the aggregation method.*/
    protected static final String CFGKEY_AGGR_METHOD = "aggrMethod";
    /**Settings name of the aggregation column name.*/
    protected static final String CFGKEY_AGGR_COLNAME = "aggrColumn";
    /**This column filter should be used in all x column select boxes.*/
    public static final ColumnFilter PIE_COLUMN_FILTER =
        PieColumnFilter.getInstance();

    private final SettingsModelIntegerBounded m_noOfRows;

    private final SettingsModelBoolean m_allRows;

    private final SettingsModelString m_pieColumn;

    private final SettingsModelString m_aggrMethod;

    private final SettingsModelString m_aggrColumn;

    private PieDataModel m_model;

    /**
     * The constructor.
     */
    protected PieNodeModel() {
        super(1, 0); // one input, no outputs
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
//        setAutoExecutable(true);
        m_noOfRows = new SettingsModelIntegerBounded(
                PieNodeModel.CFGKEY_NO_OF_ROWS,
                PieNodeModel.DEFAULT_NO_OF_ROWS, 0,
                Integer.MAX_VALUE);
        m_allRows = new SettingsModelBoolean(
                PieNodeModel.CFGKEY_ALL_ROWS, false);
        m_allRows.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfRows.setEnabled(!m_allRows.getBooleanValue());
            }
        });
        m_pieColumn = new SettingsModelString(
                PieNodeModel.CFGKEY_PIE_COLNAME, "");
        m_aggrColumn = new SettingsModelString(
                PieNodeModel.CFGKEY_AGGR_COLNAME, null);
        m_aggrColumn.setEnabled(!AggregationMethod.COUNT.equals(
                AggregationMethod.getDefaultMethod()));
        m_aggrMethod = new SettingsModelString(PieNodeModel.CFGKEY_AGGR_METHOD,
                AggregationMethod.getDefaultMethod().name());
        m_aggrMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final AggregationMethod method =
                    AggregationMethod.getMethod4Command(
                            m_aggrMethod.getStringValue());
                m_aggrColumn.setEnabled(
                        !AggregationMethod.COUNT.equals(method));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_allRows.saveSettingsTo(settings);
        m_noOfRows.saveSettingsTo(settings);
        m_pieColumn.saveSettingsTo(settings);
        m_aggrMethod.saveSettingsTo(settings);
        m_aggrColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_allRows.validateSettings(settings);
        m_noOfRows.validateSettings(settings);
        m_pieColumn.validateSettings(settings);
        m_aggrMethod.validateSettings(settings);
        m_aggrColumn.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_allRows.loadSettingsFrom(settings);
        m_noOfRows.loadSettingsFrom(settings);
        m_pieColumn.loadSettingsFrom(settings);
        m_aggrMethod.loadSettingsFrom(settings);
        m_aggrColumn.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        final DataTableSpec spec = inSpecs[0];
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification must not be null");
        }
        final DataColumnSpec pieCol =
            spec.getColumnSpec(m_pieColumn.getStringValue());
        if (pieCol == null) {
            throw new InvalidSettingsException(
                    "No column spec found for column with name: " + pieCol);
        }
        final AggregationMethod method =
            AggregationMethod.getMethod4Command(m_aggrMethod.getStringValue());
        if (method == null) {
            throw new InvalidSettingsException("No valid aggregation method");
        }
        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        LOGGER.debug("Entering execute(inData, exec) of class PieNodeModel.");
        final BufferedDataTable dataTable = inData[0];
        if (dataTable == null) {
            throw new IllegalArgumentException("No data found");
        }
        if (dataTable.getRowCount() < 1) {
            setWarningMessage("Data table contains no rows");
            return new BufferedDataTable[0];
        }
        final DataTableSpec spec = dataTable.getDataTableSpec();
        final DataColumnSpec pieCol =
            spec.getColumnSpec(m_pieColumn.getStringValue());
        if (pieCol == null) {
            throw new IllegalArgumentException(
                    "No column spec found for column with name: " + pieCol);
        }
        final String aggrCol = m_aggrColumn.getStringValue();
        final int aggrColIdx = spec.findColumnIndex(aggrCol);
        final int pieColIdx = spec.findColumnIndex(pieCol.getName());
        m_model = new PieDataModel(pieCol, true);
        for (final DataRow row : dataTable) {
            final Color rowColor = spec.getRowColor(row).getColor(false, false);
            final DataCell pieCell = row.getCell(pieColIdx);
            final DataCell aggrCell;
            if (aggrColIdx >= 0) {
                aggrCell = row.getCell(aggrColIdx);
            } else {
                aggrCell = null;
            }
            m_model.addDataRow(row.getKey().getId(), rowColor, pieCell,
                    aggrCell);
            exec.checkCanceled();
        }
        return new BufferedDataTable[0];
    }

    /**
     * @return the {@link PieVizModel}
     */
    public PieVizModel getVizModel() {
        if (m_model == null) {
            return null;
        }
        final AggregationMethod method =
            AggregationMethod.getMethod4Command(m_aggrMethod.getStringValue());
        final PieVizModel vizModel = new PieVizModel(m_model);
        vizModel.setAggregationMethod(method);
        return vizModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_model = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TK_TODO Auto-generated method stub

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TK_TODO Auto-generated method stub

    }
}
