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
 *    13.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.node;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.base.node.viz.histogram.util.NoDomainColumnFilter;
import org.knime.base.node.viz.histogram.util.SettingsModelColorNameColumns;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ColumnFilter;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger
        .getLogger(AbstractHistogramNodeModel.class);
    /**Default number of rows to use.*/
    protected static final int DEFAULT_NO_OF_ROWS = 2500;
    /**Settings name for the take all rows select box.*/
    protected static final String CFGKEY_ALL_ROWS = "allRows";
    /**Settings name of the number of rows.*/
    protected static final String CFGKEY_NO_OF_ROWS = "noOfRows";
    /**Used to store the attribute column name in the settings.*/
    protected static final String CFGKEY_X_COLNAME = "HistogramXColName";
    /**Settings name of the aggregation column name.*/
    protected static final String CFGKEY_AGGR_COLNAME = "aggrColumn";
    /**The name of the file which holds the table specification.*/
    private static final String CFG_TABLESPEC_FILE = "histoTableSpec";
    /**The root tag of the table specification.*/
    private static final String CFG_TABLESPEC_TAG = "tableSpec";

    /**The name of the directory which holds the optional data of the
     * different histogram implementations.*/
    public static final String CFG_DATA_DIR_NAME = "histoData";

    private DataTableSpec m_tableSpec;
    private DataColumnSpec m_xColSpec;
    private int m_xColIdx;
    private Collection<ColorColumn> m_aggrCols;

    private final SettingsModelIntegerBounded m_noOfRows =
        new SettingsModelIntegerBounded(
                CFGKEY_NO_OF_ROWS, DEFAULT_NO_OF_ROWS, 0, Integer.MAX_VALUE);
    private final SettingsModelBoolean m_allRows = new SettingsModelBoolean(
                CFGKEY_ALL_ROWS, false);
    /** The name of the x column. */
    private final SettingsModelString m_xColName = new SettingsModelString(
                CFGKEY_X_COLNAME, "");
    private final SettingsModelColorNameColumns m_aggrColName =
        new SettingsModelColorNameColumns(CFGKEY_AGGR_COLNAME, null);

    /**Constructor for class AbstractHistogramNodeModel.
     */
    public AbstractHistogramNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[0]);
        m_allRows.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfRows.setEnabled(!m_allRows.getBooleanValue());
            }
        });
        m_noOfRows.setEnabled(!m_allRows.getBooleanValue());

    }

    /**
     * @param allRows set the default value of the all rows select box.
     */
    protected void setAllRowsDefault(final boolean allRows) {
        m_allRows.setBooleanValue(allRows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        final SettingsModelBoolean model =
            m_allRows.createCloneWithValidatedValue(settings);
        if (!model.getBooleanValue()) {
            //read the spinner value only if the user hasn't selected to
            //retrieve all values
            m_noOfRows.validateSettings(settings);
            final SettingsModelInteger copy =
                m_noOfRows.createCloneWithValidatedValue(settings);
            if (copy.getIntValue() > Integer.MAX_VALUE
                    || copy.getIntValue() <= 0) {
                throw new InvalidSettingsException(
                        "No of rows must be greater zero");
            }
        }
        try {
            m_xColName.validateSettings(settings);
        } catch (final Throwable e) {
            //It's an older node which hasn't stored the aggregation column
            final String xCol = settings.getString("xColumn");
            if (xCol == null || xCol.length() < 1) {
                throw new InvalidSettingsException("Invalid binning column");
            }
        }
        try {
            m_aggrColName.validateSettings(settings);
        } catch (final Throwable e) {
            //It's an older node which hasn't stored the aggregation column
            LOGGER.debug("Exception while validating settings: "
                    + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        try {
            m_allRows.loadSettingsFrom(settings);
            m_noOfRows.loadSettingsFrom(settings);
        } catch (final Exception e) {
            // In case of older nodes the row number is not available
            m_allRows.setBooleanValue(false);
            m_noOfRows.setIntValue(DEFAULT_NO_OF_ROWS);
        }
        try {
            m_xColName.loadSettingsFrom(settings);
        } catch (final Throwable e) {
            //It's an older node which had a different name for the x column
            final String xCol = settings.getString("xColumn");
            m_xColName.setStringValue(xCol);
            LOGGER.debug("Old histogram settings found");
        }
        try {
            m_aggrColName.loadSettingsFrom(settings);
        } catch (final Throwable e) {
            //It's an older node which hasn't stored the aggregation column
            m_aggrColName.setColorNameColumns((ColorColumn)null);
            LOGGER.debug("Exception while loading settings use default values");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_allRows.saveSettingsTo(settings);
        m_noOfRows.saveSettingsTo(settings);
        m_xColName.saveSettingsTo(settings);
        m_aggrColName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        try {
            final File settingsFile =
                new File(nodeInternDir, CFG_TABLESPEC_FILE);
            final FileInputStream settingsIS =
                new FileInputStream(settingsFile);
            final ConfigRO settings = NodeSettings.loadFromXML(settingsIS);
            m_tableSpec = DataTableSpec.load(settings);
            final File histoDataDir =
                new File(nodeInternDir, CFG_DATA_DIR_NAME);
            loadXCol();
            loadAggrColumns();
            //load the data of the implementation
            loadHistogramInternals(histoDataDir, exec);
        } catch (final FileNotFoundException e) {
            LOGGER.debug("Previous implementations haven't stored the data");
            m_tableSpec = null;
        } catch (final Exception e) {
            LOGGER.debug("Error while loading table specification: "
                    + e.getMessage());
            m_tableSpec = null;
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Called from the {@link #loadInternals(File, ExecutionMonitor)} method
     * to let the histogram implementation load own internal data.
     * @param dataDir the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress message
     * @throws Exception if an exception occurs
     */
    protected abstract void loadHistogramInternals(final File dataDir,
            final ExecutionMonitor exec) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        if (m_tableSpec == null) {
            return;
        }
        try {
            final File settingsFile =
                new File(nodeInternDir, CFG_TABLESPEC_FILE);
            final FileOutputStream settingsOS =
                new FileOutputStream(settingsFile);
            final NodeSettings settings = new NodeSettings(CFG_TABLESPEC_TAG);
            m_tableSpec.save(settings);
            settings.saveToXML(settingsOS);
            if (!new File(nodeInternDir, CFG_DATA_DIR_NAME).mkdir()) {
                throw new Exception("Unable to create internal data directory");
            }
            final File histoDataDir =
                new File(nodeInternDir, CFG_DATA_DIR_NAME);
            //save the data of the implementation
            saveHistogramInternals(histoDataDir, exec);
        } catch (final Exception e) {
            LOGGER.warn("Error while saving table specification: "
                    + e.getMessage());
        }
    }
    /**
     * Called from the {@link #saveInternals(File, ExecutionMonitor)} method
     * to let the histogram implementation save own internal data.
     * @param dataDir the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress message
     * @throws Exception if an exception occurs
     */
    protected abstract void saveHistogramInternals(final File dataDir,
            final ExecutionMonitor exec) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_tableSpec = null;
        m_xColSpec = null;
        m_xColIdx = -1;
        m_aggrCols = null;
    }

    /**
     * This method creates a new {@link AbstractHistogramVizModel} each time
     * it is called.
     *
     * @return the histogram viz model or <code>null</code> if not
     * all information are available yet
     */
    protected abstract AbstractHistogramVizModel getHistogramVizModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
    throws InvalidSettingsException {
        if (inSpecs == null || inSpecs[0] == null) {
            throw new InvalidSettingsException(
                    "No input specification available.");
        }
        final DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];
        if (tableSpec == null || tableSpec.getNumColumns() < 1) {
            throw new InvalidSettingsException(
                    "Input table should have at least 1 column.");
        }

        if (!tableSpec.containsName(m_xColName.getStringValue())) {
            // if the input table has only two columns where only one column
            // is numerical select these two columns as default columns
            // if both are numeric we don't know which one the user wants as
            // aggregation column and which one as x column
            final ColumnFilter xFilter =
                AbstractHistogramPlotter.X_COLUMN_FILTER;
            final ColumnFilter aggrFilter =
                AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER;
            if (tableSpec.getNumColumns() == 1) {
                final DataColumnSpec columnSpec0 = tableSpec.getColumnSpec(0);
                if (xFilter.includeColumn(columnSpec0)) {
                    m_xColName.setStringValue(columnSpec0.getName());
                } else {
                    throw new InvalidSettingsException(
                        "No column compatible with this node. Column needs to "
                        + "be nominal or numeric and must contain a valid "
                        + "domain. In order to compute the domain of a column "
                        + "use the DomainCalculator or ColumnFilter node.");
                }
            } else if (tableSpec.getNumColumns() == 2) {
                final DataColumnSpec columnSpec0 = tableSpec.getColumnSpec(0);
                final DataColumnSpec columnSpec1 = tableSpec.getColumnSpec(1);
                final DataType type0 = columnSpec0.getType();
                final DataType type1 = columnSpec1.getType();

                if (type0.isCompatible(StringValue.class)
                        && type1.isCompatible(DoubleValue.class)
                        && xFilter.includeColumn(columnSpec0)
                        && aggrFilter.includeColumn(columnSpec1)) {
                    m_xColName.setStringValue(tableSpec.getColumnSpec(0)
                            .getName());
                    m_aggrColName.setColorNameColumns(
                            new ColorColumn(Color.lightGray,
                                    tableSpec.getColumnSpec(1).getName()));
                } else if (type0.isCompatible(DoubleValue.class)
                        && type1.isCompatible(StringValue.class)
                        && xFilter.includeColumn(columnSpec1)
                        && aggrFilter.includeColumn(columnSpec0)) {
                    m_xColName.setStringValue(tableSpec.getColumnSpec(1)
                            .getName());
                    m_aggrColName.setColorNameColumns(
                            new ColorColumn(Color.lightGray,
                                    tableSpec.getColumnSpec(0).getName()));
                } else {
                    throw new InvalidSettingsException(
                            "Please define the binning column.");
                }
            } else {
                throw new InvalidSettingsException(
                        "Please define the binning column.");
            }
        }
        //check if the aggregation columns are available
        for (final ColorColumn col : m_aggrColName.getColorNameColumns()) {
            DataColumnSpec cSpec = tableSpec.getColumnSpec(col.getColumnName());
            if (cSpec == null) {
                throw new InvalidSettingsException("Aggregation column '"
                        + col.getColumnName() + "' not found in input table");
            } else if (!cSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Aggregation column '"
                        + col.getColumnName() + "' is not numeric " 
                        + "(not double compatible)");
            }
        }
        m_xColSpec = tableSpec.getColumnSpec(m_xColName.getStringValue());
        //check if the table contains value which don't have a valid domain
        //and display a warning that they are ignored
        final ColumnFilter filter = NoDomainColumnFilter.getInstance();
        final int numColumns = tableSpec.getNumColumns();
        final List<DataColumnSpec> invalidCols =
            new ArrayList<DataColumnSpec>(numColumns);
        for (int i = 0; i < numColumns; i++) {
            final DataColumnSpec columnSpec = tableSpec.getColumnSpec(i);
            if (!filter.includeColumn(columnSpec)) {
                invalidCols.add(columnSpec);
            }
        }
        if (invalidCols.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            if (invalidCols.size() == 1) {
                buf.append("Column ");
                buf.append(invalidCols.get(0).getName());
                buf.append(" contains no valid domain an will be ignored.");
            } else {
                buf.append(invalidCols.size());
                buf.append(" columns without a valid domain will be ignored.");
            }
            buf.append(" In order to calculate the domain use the"
                    + " Nominal Values or Domain Calculator node.");
            setWarningMessage(buf.toString());
        }
        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        LOGGER.debug("Entering execute(inData, exec) of class "
                + "FixedColumnHistogramNodeModel.");
        if (inData == null || inData[0] == null) {
            throw new Exception("No data table available!");
        }
        // create the data object
        final BufferedDataTable table = (BufferedDataTable)inData[0];
        m_tableSpec = table.getDataTableSpec();
        if (m_tableSpec == null) {
            throw new NullPointerException(
                    "Table specification must not be null");
        }
        final int maxNoOfRows = table.getRowCount();
        if (maxNoOfRows < 1) {
            setWarningMessage("Empty data table found.");
        }
        loadXCol();
        loadAggrColumns();
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
        createHistogramModel(exec, selectedNoOfRows, table);
        LOGGER.debug("Exiting execute(inData, exec) of class "
                + "FixedColumnHistogramNodeModel.");
        return new BufferedDataTable[0];
    }

    /**
     *
     */
    private void loadXCol() {
        final String xCol = m_xColName.getStringValue();
        m_xColSpec = m_tableSpec.getColumnSpec(xCol);
        if (m_xColSpec == null) {
            throw new IllegalArgumentException("Binning column not found");
        }
        m_xColIdx = m_tableSpec.findColumnIndex(xCol);
        if (m_xColIdx < 0) {
            throw new IllegalArgumentException(
                    "Binning column index not found");
        }
    }

    /**
     *
     */
    private void loadAggrColumns() {
        final ColorColumn[] aggrCols = m_aggrColName.getColorNameColumns();
        if (aggrCols == null) {
            //the user hasn't selected an aggregation column
            //thats fine since it is optional
            m_aggrCols = null;
        } else {
            m_aggrCols = new ArrayList<ColorColumn>(aggrCols.length);
            for (final ColorColumn column : aggrCols) {
                final String columnName = column.getColumnName();
                final int aggrColIdx = m_tableSpec.findColumnIndex(columnName);
                if (aggrColIdx < 0) {
                    throw new IllegalArgumentException(
                            "Selected aggregation column not found.");
                }
                final ColorColumn aggrColumn =
                    new ColorColumn(column.getColor(), columnName);
                m_aggrCols.add(aggrColumn);
            }
        }
    }

    /**
     * This method should use the given information to create the internal
     * histogram data model.
     * @param exec the {@link ExecutionContext} for progress information
     * @param noOfRows the number of rows to display
     * @param table the {@link DataTable} which contains the rows
     * @throws CanceledExecutionException if the user has canceled the
     * node execution
     */
    protected abstract void createHistogramModel(final ExecutionContext exec,
            final int noOfRows, final BufferedDataTable table)
    throws CanceledExecutionException;

    /**
     * @return the {@link DataTableSpec} of the input table
     */
    protected DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    /**
     * @return the aggregation columns to use or <code>null</code> if
     * the user hasn't selected a aggregation column
     */
    protected Collection<ColorColumn> getAggrColumns() {
        return m_aggrCols;
    }

    /**
     * @return the name of the selected x column or null if none is selected
     */
    protected String getSelectedXColumnName() {
        final String value = m_xColName.getStringValue();
        if (value == null || value.trim().length() < 1) {
            return null;
        }
        return value;
    }

    /**
     * @param name the new selected x column name
     */
    protected void setSelectedXColumnName(final String name) {
        if (name == null) {
            throw new NullPointerException("Name must not be null");
        }
        m_xColName.setStringValue(name);
    }

    /**
     * @param aggrCols the new selected aggregation column
     */
    protected void setSelectedAggrColumns(final ColorColumn... aggrCols) {
        if (aggrCols == null) {
            throw new NullPointerException(
                    "Aggregation columns must not be null or empty");
        }
        m_aggrColName.setColorNameColumns(aggrCols);
    }

    /**
     * @return the {@link DataColumnSpec} of the selected x column
     */
    protected DataColumnSpec getXColSpec() {
        return m_xColSpec;
    }

    /**
     * @return the index of the selected x column in the given
     * {@link DataTableSpec}
     */
    protected int getXColIdx() {
        return m_xColIdx;
    }
}
