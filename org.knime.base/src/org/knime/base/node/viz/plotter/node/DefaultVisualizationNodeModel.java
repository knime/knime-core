/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   20.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Implementation of a {@link org.knime.core.node.NodeModel} which provides all
 * functionality that is needed for a default plotter implementation. That is:
 * converting the incoming data of inport 0 into a 
 * {@link org.knime.base.node.util.DataArray} with the maximum number of rows 
 * specified in the {@link DefaultVisualizationNodeDialog}, loading and saving 
 * this {@link org.knime.base.node.util.DataArray} in the <code>load</code>- and
 * <code>saveInternals</code> and providing it via the
 * {@link #getDataArray(int)} method of the 
 * {@link org.knime.base.node.viz.plotter.DataProvider} interface.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultVisualizationNodeModel extends NodeModel implements 
    DataProvider {
    
    private DataArray m_input;
    
    /** Config key for the last displayed row. */
    public static final String CFG_END = "end";
    
    /** Config key for dis- or enabling antialiasing. */
    public static final String CFG_ANTIALIAS = "antialias";
    
    private static final String FILE_NAME = "internals";
    
    /** Config key for the maximal number of nominal values. */
    // bugfix 1299
    public static final String CFG_MAX_NOMINAL = "max_nominal_values";
    
    /** Per default columns with nominal values more than this value are 
     * ignored. 
     */
    // bugfix 1299
    static final int DEFAULT_NR_NOMINAL_VALUES = 60;
    
    private int[] m_excludedColumns;
    
    private final SettingsModelIntegerBounded m_maxNominalValues 
        = createMaxNominalValuesModel();
    
    private final SettingsModelIntegerBounded m_maxRows 
        = createLastDisplayedRowModel(END);
    /**
     * 
     * @return the settings model for max nominal values
     */
    // bugfix 1299
    static SettingsModelIntegerBounded createMaxNominalValuesModel() {
        return new SettingsModelIntegerBounded(
                CFG_MAX_NOMINAL, DEFAULT_NR_NOMINAL_VALUES, 
                1, Integer.MAX_VALUE);
    }
    
    /** @param end The last row index to display.
     * @return settings model for the max row count property. 
     * */
    static SettingsModelIntegerBounded createLastDisplayedRowModel(
            final int end) {
        return new SettingsModelIntegerBounded(
                CFG_END, end, 1, Integer.MAX_VALUE);
    }
    
    /**
     * Creates a {@link org.knime.core.node.NodeModel} with one data inport and 
     * no outport.
     */
    public DefaultVisualizationNodeModel() {
        super(1, 0);
    }
    
    /**
     * Constructor for extending classes to define an arbitrary number of
     * in- and outports.
     * 
     * @param nrInports number of data inports
     * @param nrOutports number of data outports
     */
    public DefaultVisualizationNodeModel(final int nrInports, 
            final int nrOutports) {
        super(nrInports, nrOutports);
    }


    /**
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        return m_input;
    }



    /**
     * All nominal columns without possible values or with more than 60
     * possible values are ignored. A warning is set if so.
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        findCompatibleColumns(inSpecs[0], true);
        return new DataTableSpec[0];
    }

    private void findCompatibleColumns(final DataTableSpec inSpec, 
            final boolean warn)
            throws InvalidSettingsException {
        // first filter out those nominal columns with
        // possible values == null
        // or possibleValues.size() > 60
        List<Integer>excludedCols = new ArrayList<Integer>();
        int currColIdx = 0;
        for (DataColumnSpec colSpec : inSpec) {
            // nominal value
            if (!colSpec.getType().isCompatible(NominalValue.class) 
                    && !colSpec.getType().isCompatible(DoubleValue.class)) {
                    excludedCols.add(currColIdx);
            }
            if (colSpec.getType().isCompatible(NominalValue.class)) {
                if (colSpec.getDomain().hasValues() 
                        // bugfix 1299 
                        // made the "60" adjustable via the dialog
                        && colSpec.getDomain().getValues().size() 
                            > m_maxNominalValues.getIntValue()) {
                    excludedCols.add(currColIdx);
                } else if (!colSpec.getDomain().hasValues()) {
                    excludedCols.add(currColIdx);
                }
            }
            // for numeric columns check if the lower and upper bounds are 
            // available
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                if (!colSpec.getDomain().hasBounds()) {
                    excludedCols.add(currColIdx);
                }
            }
            currColIdx++;
        }
        m_excludedColumns = new int[excludedCols.size()];
        for (int i = 0; i < excludedCols.size(); i++) {
            m_excludedColumns[i] = excludedCols.get(i);
        }
        if (warn && excludedCols.size() > 0) {
            setWarningMessage("Some columns are ignored! Not compatible " 
                    + "with DoubleValue or NominalValue or no or too many" 
                    + " possible values or no lower and upper bound provided.");
        }
        // check for empty table
        if (warn && inSpec.getNumColumns() - excludedCols.size() <= 0) {
            throw new InvalidSettingsException(
                "No columns to visualize are available!"
                    + " Please refer to the NodeDescription to find out why!");
        }
    }
    
    

    /**
     * Converts the input data at inport 0 into a 
     * {@link org.knime.base.node.util.DataArray} with maximum number of rows as
     * defined in the {@link DefaultVisualizationNodeDialog}. Thereby nominal 
     * columns are irgnored whose possible values are null or more than 60.
     * 
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // first filter out those nominal columns with
        // possible values == null
        // or possibleValues.size() > m_maxNominalValues
        findCompatibleColumns(inData[0].getDataTableSpec(), false);
        DataTable filter = new FilterColumnTable(inData[0], false, 
                getExcludedColumns());
        m_input = new DefaultDataArray(
                filter, 1, m_maxRows.getIntValue(), exec);
        if (m_maxRows.getIntValue() < inData[0].getRowCount()) {
            setWarningMessage("Only the rows from 0 to " 
                    + m_maxRows.getIntValue() + " are displayed.");
        }
        return new BufferedDataTable[0];
    }

    /**
     * Loads the converted {@link org.knime.base.node.util.DataArray}.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File f = new File(nodeInternDir, FILE_NAME);
        ContainerTable table = DataContainer.readFromZip(f);
        m_input = new DefaultDataArray(table, 1, m_maxRows.getIntValue(), exec);
    }

    /**
     * Loads the maximum number of rows.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_maxRows.loadSettingsFrom(settings);
        try {
            m_maxNominalValues.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // take default value
            m_maxNominalValues.setIntValue(DEFAULT_NR_NOMINAL_VALUES);
        }
    }

    /**
     * Sets the input data <code>null</code>.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_excludedColumns = null;
        m_input = null;
    }

    /**
     * Saves the converted {@link org.knime.base.node.util.DataArray}.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File f = new File(nodeInternDir, FILE_NAME);
        DataContainer.writeToZip(m_input, f, exec);
    }

    /**
     * Saves the maximum number of rows.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_maxRows.saveSettingsTo(settings);
        m_maxNominalValues.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_maxRows.validateSettings(settings);
        try {
            settings.getBoolean(CFG_ANTIALIAS);
            // do not validate m_maxNominalValues (backward compatibility)
        } catch (InvalidSettingsException ise) {
            // removed this from dialog
            // if not present set it to false in loadValidatedSettings
        }
    }

    /**
     * @return the excludedColumns
     */
    public int[] getExcludedColumns() {
        return m_excludedColumns;
    }

    /**
     * @return the last
     */
    public int getEndIndex() {
        return m_maxRows.getIntValue();
    }

}
