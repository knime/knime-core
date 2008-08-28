/*
 * -------------------------------------------------------------------
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
 * History
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.node;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.util.PieColumnFilter;
import org.knime.base.node.viz.pie.util.TooManySectionsException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The abstract pie chart implementation of the{@link NodeModel} class.
 * @author Tobias Koetter, University of Konstanz
 * @param <D> the {@link PieVizModel} implementation
 */
public abstract class PieNodeModel<D extends PieVizModel>
extends GenericNodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PieNodeModel.class);

    /**The name of the directory which holds the optional data of the
     * different histogram implementations.*/
    public static final String CFG_DATA_DIR_NAME = "pieData";

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

    /**This column filter should be used in all x column select boxes.*/
    public static final ColumnFilter AGGREGATION_COLUMN_FILTER =
            new DataValueColumnFilter(DoubleValue.class);

    private final SettingsModelIntegerBounded m_noOfRows;

    private final SettingsModelBoolean m_allRows;

    private final SettingsModelString m_pieColumn;

    private final SettingsModelString m_aggrMethod;

    private final SettingsModelString m_aggrColumn;

    /**Constructor for class PieNodeModel.
     */
    public PieNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[0]);
        m_noOfRows = new SettingsModelIntegerBounded(CFGKEY_NO_OF_ROWS,
                        DEFAULT_NO_OF_ROWS, 0, Integer.MAX_VALUE);
        m_allRows = new SettingsModelBoolean(CFGKEY_ALL_ROWS, false);
        m_allRows.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                m_noOfRows.setEnabled(!m_allRows.getBooleanValue());
            }
        });
        m_pieColumn = new SettingsModelString(CFGKEY_PIE_COLNAME, "");
        m_aggrColumn = new SettingsModelString(CFGKEY_AGGR_COLNAME, null);
        m_aggrMethod = new SettingsModelString(CFGKEY_AGGR_METHOD,
                        AggregationMethod.getDefaultMethod().name());
        m_aggrMethod.setEnabled(m_aggrColumn.getStringValue() != null);
        m_aggrColumn.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                boolean enable = m_aggrColumn.getStringValue() != null;
                m_aggrMethod.setEnabled(enable);
                if (!enable) {
                    m_aggrMethod.setStringValue(
                            AggregationMethod.COUNT.getActionCommand());
                }
            }
        });
//        m_aggrColumn.setEnabled(!AggregationMethod.COUNT
//                .equals(AggregationMethod.getDefaultMethod()));
//        m_aggrMethod.addChangeListener(new ChangeListener() {
//
//            public void stateChanged(final ChangeEvent e) {
//                final AggregationMethod method =
//                        AggregationMethod.getMethod4Command(m_aggrMethod
//                                .getStringValue());
//                m_aggrColumn
//                        .setEnabled(!AggregationMethod.COUNT.equals(method));
//            }
//        });
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
        final String aggrMethod =
                ((SettingsModelString)m_aggrMethod
                        .createCloneWithValidatedValue(settings))
                        .getStringValue();
        final AggregationMethod method =
                AggregationMethod.getMethod4Command(aggrMethod);
        m_aggrColumn.validateSettings(settings);
        if (!AggregationMethod.COUNT.equals(method)) {
            final String value =
                    ((SettingsModelString)m_aggrColumn
                            .createCloneWithValidatedValue(settings))
                            .getStringValue();
            if (value == null) {
                throw new InvalidSettingsException(
                        "No aggregation column only valid for method count");
            }
        }
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
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec spec = (DataTableSpec)inSpecs[0];
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification must not be null");
        }
        final String colName = m_pieColumn.getStringValue();
        if (colName == null || colName.length() < 1) {
            throw new InvalidSettingsException("Please select the pie column");
        }
        final DataColumnSpec pieCol = spec.getColumnSpec(colName);
        if (pieCol == null) {
            throw new InvalidSettingsException(
                    "Specified column name '" + colName
                    + "' not found in input table");
        }
        if (!PieColumnFilter.validDomain(pieCol)) {
            throw new InvalidSettingsException("No valid pie column selected.");
        }
        final AggregationMethod method =
                AggregationMethod.getMethod4Command(
                        m_aggrMethod.getStringValue());
        if (method == null) {
            throw new InvalidSettingsException("No valid aggregation method");
        }
        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable dataTable = (BufferedDataTable)inData[0];
        if (dataTable == null) {
            throw new IllegalArgumentException("No data found");
        }
        final int maxNoOfRows = dataTable.getRowCount();
        if (maxNoOfRows < 1) {
            setWarningMessage("Data table contains no rows");
            return new BufferedDataTable[0];
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
                    "No column spec found for pie column");
        }

        final String aggrColName = m_aggrColumn.getStringValue();
        final DataColumnSpec aggrCol;

        if (aggrColName == null) {
            aggrCol = null;
        } else {
            aggrCol = spec.getColumnSpec(aggrColName);
        }
        int selectedNoOfRows;
        if (m_allRows.getBooleanValue()) {
            //set the actual number of rows in the selected number of rows
            //object since the user wants to display all rows
            //            m_noOfRows.setIntValue(maxNoOfRows);
            selectedNoOfRows = maxNoOfRows;
        } else {
            selectedNoOfRows = m_noOfRows.getIntValue();
        }
        //final int noOfRows = inData[0].getRowCount();
        if ((selectedNoOfRows) < maxNoOfRows) {
            setWarningMessage("Only the first " + selectedNoOfRows + " of "
                    + maxNoOfRows + " rows are displayed.");
        } else if (selectedNoOfRows > maxNoOfRows) {
            selectedNoOfRows = maxNoOfRows;
        }
        createModel(exec, pieCol, aggrCol, dataTable,
                selectedNoOfRows, containsColorHandler(dataTable));
        exec.setMessage("Vaidating model");
        final long startTime = System.currentTimeMillis();
        //validate the data model by trying to get the viz model
        if (getVizModelInternal() == null) {
            setWarningMessage("No visualization model available");
        }
        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time to validate model: " + durationTime + " ms");
        exec.setProgress(1.0, "Pie chart finished...");
        return new BufferedDataTable[0];
    }

    /**
     * Called prior the {@link #addDataRow(DataRow, Color, DataCell, DataCell)}
     * method to allow the implementing class the specific model creation.
     * @param exec the {@link ExecutionMonitor}
     * @param pieColSpec the {@link DataColumnSpec} of the selected pie column
     * @param aggrColSpec the {@link DataColumnSpec} of the selected
     * aggregation column
     * @param dataTable the {@link DataTableSpec}
     * @param noOfRows the expected number of rows
     * @param containsColorHandler <code>true</code> if a color handler is set
     * @throws CanceledExecutionException if the progress was canceled
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    protected abstract void createModel(ExecutionContext exec,
            final DataColumnSpec pieColSpec,
            final DataColumnSpec aggrColSpec,
            BufferedDataTable dataTable, final int noOfRows,
            final boolean containsColorHandler)
    throws CanceledExecutionException, TooManySectionsException;

    private static boolean containsColorHandler(
            final BufferedDataTable dataTable) {
        if (dataTable == null) {
            return false;
        }
        final DataTableSpec spec = dataTable.getDataTableSpec();
        for (final DataColumnSpec colSpec : spec) {
            if (colSpec.getColorHandler() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the {@link PieVizModel}. Could be null.
     */
    public D getVizModel() {
        D vizModel = null;
        try {
             vizModel = getVizModelInternal();
        } catch (final TooManySectionsException e) {
            setWarningMessage(e.getMessage());
            LOGGER.error(e.getMessage());
        }
        if (vizModel == null) {
            return null;
        }
        final AggregationMethod method =
            AggregationMethod.getMethod4Command(m_aggrMethod.getStringValue());
        vizModel.setAggregationMethod(method);
        return vizModel;
    }

    /**
     * @param name the name of the pie column
     */
    protected void setPieColumnName(final String name) {
        if (name == null) {
            throw new NullPointerException("Pie column name must not be null");
        }
        m_pieColumn.setStringValue(name);
    }

    /**
     * @return the selected pie column name
     */
    protected String getPieColumnName() {
        return m_pieColumn.getStringValue();
    }

    /**
     * @param name the name of the aggregation column
     */
    protected void setAggregationColumnName(final String name) {
        if (name == null) {
            throw new NullPointerException(
                    "Aggregation column name must not be null");
        }
        m_aggrColumn.setStringValue(name);
    }

    /**
     * @return the optional selected aggregation column name
     */
    protected String getAggregationColumnName() {
        return m_aggrColumn.getStringValue();
    }

    /**
     * @return the {@link PieVizModel}. Could be null.
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    protected abstract D getVizModelInternal() throws TooManySectionsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        resetPieData();
    }

    /**
     * Resets the implementation internal data.
     */
    protected abstract void resetPieData();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        try {
            final File dataDir = new File(nodeInternDir, CFG_DATA_DIR_NAME);
            loadPieInternals(dataDir, exec);
        } catch (final CanceledExecutionException e) {
            throw e;
        } catch (final Exception e) {
            LOGGER.debug("Error while loading internals: " + e.getMessage());
        }
    }

    /**
     * Loads the implementation internals.
     * @param dataDir the directory load load from
     * @param exec the {@link ExecutionMonitor}
     * @throws CanceledExecutionException if action was canceled
     */
    protected abstract void loadPieInternals(final File dataDir,
            final ExecutionMonitor exec) throws CanceledExecutionException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        try {
            if (!new File(nodeInternDir, CFG_DATA_DIR_NAME).mkdir()) {
                throw new Exception("Unable to create internal data directory");
            }
            final File dataDir = new File(nodeInternDir, CFG_DATA_DIR_NAME);
            savePieInternals(dataDir, exec);
        } catch (final CanceledExecutionException e) {
            throw e;
        } catch (final Exception e) {
            LOGGER.warn("Error while saving saving internals: "
                    + e.getMessage());
            throw new IOException(e);
        }
    }

    /**
     * Saves the implementation internal data.
     * @param dataDir the directory to save to
     * @param exec the {@link ExecutionMonitor}
     * @throws IOException file exception
     * @throws CanceledExecutionException action was canceled
     */
    protected abstract void savePieInternals(final File dataDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException;
}
