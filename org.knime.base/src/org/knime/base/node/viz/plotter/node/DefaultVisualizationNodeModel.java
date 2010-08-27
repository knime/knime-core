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
        // generate list of excluded columns; issuing warning
        findCompatibleColumns(inSpecs[0], true);
        return new DataTableSpec[0];
    }

    private void findCompatibleColumns(final DataTableSpec inSpec, 
            final boolean warn) throws InvalidSettingsException {
        // list of excluded columns for visualization
        final List<Integer>excludedCols = new ArrayList<Integer>();
        // warning if view properties (color, size, and/or shape) are excluded
        boolean propMgrWarning = false;
        // warning if columns with too many/missing values are present
        boolean nominalWarning = false;
        // warning if columns with too missing bounds are present
        boolean numericWarning = false;
        for (int currCol = 0; currCol < inSpec.getNumColumns(); currCol++) {
            final DataColumnSpec colSpec = inSpec.getColumnSpec(currCol);
            // neither nominal nor numeric column
            if (!colSpec.getType().isCompatible(NominalValue.class) 
                    && !colSpec.getType().isCompatible(DoubleValue.class)) {
                excludedCols.add(currCol);
            }
            // for nominal columns check number of nominal values 
            if (colSpec.getType().isCompatible(NominalValue.class)) {
                if (colSpec.getDomain().hasValues() 
                        // bugfix 1299: made the "60" adjustable via the dialog
                        && colSpec.getDomain().getValues().size() 
                            > m_maxNominalValues.getIntValue()) {
                    excludedCols.add(currCol);
                    nominalWarning = true;
                } else if (!colSpec.getDomain().hasValues()) {
                    excludedCols.add(currCol);
                    nominalWarning = true;
                }
            }
            // for numeric columns check if the lower/upper bounds are available
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                if (!colSpec.getDomain().hasBounds()) {
                    excludedCols.add(currCol);
                    numericWarning = true;
                }
            }
            // bugfix 2124: report warning when color, size, and/or shape
            // handler(s) are excluded form visualization
            if (excludedCols.contains(currCol)
                && (colSpec.getColorHandler() != null
                 || colSpec.getSizeHandler() != null
                 || colSpec.getShapeHandler() != null)) {
                propMgrWarning = true;
            }
        }
        
        // format exclusion list indices to int[]
        m_excludedColumns = new int[excludedCols.size()];
        for (int i = 0; i < excludedCols.size(); i++) {
            m_excludedColumns[i] = excludedCols.get(i);
        }
        
        // check is all columns are excluded
        if (warn && inSpec.getNumColumns() <= excludedCols.size()) {
            throw new InvalidSettingsException(
                "No columns to visualize are available!"
                + " Please refer to the NodeDescription to find out why!");
        }
        
        // report detailed warning
    if (warn && excludedCols.size() > 0) {
        final StringBuilder warning = new StringBuilder();
        if (propMgrWarning) {
            warning.append("Some view properties are ignored "
               + " (defined on incompatible columns): ");
        } else {
            warning.append("Some columns are ignored: ");
        }
        if (nominalWarning) {
            warning.append("too many/missing nominal values");
            if (numericWarning) {
                warning.append(" and ");
            } else {
                warning.append(".");
            }
        }
        if (numericWarning) {
            warning.append("bounds missing.");
        }
        setWarningMessage(warning.toString());
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
        // generate list of excluded columns, suppressing warning
        findCompatibleColumns(inData[0].getDataTableSpec(), false);
        DataTable filter = new FilterColumnTable(inData[0], false, 
                getExcludedColumns());
        m_input = new DefaultDataArray(
                filter, 1, m_maxRows.getIntValue(), exec);
        if (m_maxRows.getIntValue() < inData[0].getRowCount()) {
            setWarningMessage("Only the first " 
                    + m_maxRows.getIntValue() + " rows are displayed.");
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
     * @return the excluded column indices
     */
    public int[] getExcludedColumns() {
        return m_excludedColumns;
    }

    /**
     * @return the last row index
     */
    public int getEndIndex() {
        return m_maxRows.getIntValue();
    }

}
