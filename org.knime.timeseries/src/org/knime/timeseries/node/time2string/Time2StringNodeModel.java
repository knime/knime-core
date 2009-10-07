/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   28.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.time2string;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.StringHistory;
import org.knime.timeseries.node.stringtotimestamp.String2DateDialog;

/**
 * Takes a column containing {@link DateAndTimeValue}s and converts them into 
 * strings by using a {@link SimpleDateFormat} which can be selected or entered 
 * in the dialog. 
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class Time2StringNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            Time2StringNodeModel.class);
    
    /** Suffix to append to existing column name as proposal for new col name. 
     */
    static final String COL_NAME_SUFFIX = "string";
    
    private final SettingsModelString m_selectedCol = String2DateDialog
        .createColumnSelectionModel();
    
    private final SettingsModelString m_newColName = String2DateDialog
        .createColumnNameModel();
    
    private final SettingsModelBoolean m_replaceCol = String2DateDialog
        .createReplaceModel();
    
    private final SettingsModelString m_pattern = String2DateDialog
        .createFormatModel();
    
    /**
     * One in port for the input table containing a time column, and one out
     * port with the time converted to string.  
     */
    public Time2StringNodeModel() {
        super(1, 1);
        String2DateDialog.addColSelectionListener(m_selectedCol, m_newColName, 
                COL_NAME_SUFFIX);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check if input has dateandtime column 
        DataTableSpec inSpec = inSpecs[0];
        if (!inSpec.containsCompatibleType(DateAndTimeValue.class)) {
            throw new InvalidSettingsException(
                    "Input table must contain at least timestamp column!");
        }
        // currently selected column still there?
        String selectedColName = m_selectedCol.getStringValue(); 
        if (selectedColName != null && !selectedColName.isEmpty()) {
            if (!inSpec.containsName(selectedColName)) {
                throw new InvalidSettingsException(
                        "Column " + selectedColName 
                        + " not found in input spec!");
            }
        } else {
            // no value set: auto-configure -> choose first timeseries
            for (DataColumnSpec colSpec : inSpec) {
                if (colSpec.getType().isCompatible(DateAndTimeValue.class)) {
                    String colName = colSpec.getName();
                    m_selectedCol.setStringValue(colName);
                    setWarningMessage("Auto-configure: selected " + colName);
                    break;
                }
            }
        }        
        // create output spec
        ColumnRearranger colRearranger = createColumnRearranger(inSpec);
        return new DataTableSpec[] {colRearranger.createSpec()};
    }
    
    private ColumnRearranger createColumnRearranger(
            final DataTableSpec inSpec) {
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // if replace -> use original column name
        final boolean replace = m_replaceCol.getBooleanValue();
        String colName = m_newColName.getStringValue();
        if (replace) {
            colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_selectedCol.getStringValue());
        }
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                colName, StringCell.TYPE);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(
                m_pattern.getStringValue());
        dateFormat.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);
        final int colIdx = inSpec.findColumnIndex(
                m_selectedCol.getStringValue());
        SingleCellFactory factory = new SingleCellFactory(
                specCreator.createSpec()) {
                    @Override
                    public DataCell getCell(final DataRow row) {
                        DataCell dc = row.getCell(colIdx);
                        if (dc.isMissing()) {
                            return DataType.getMissingCell();
                        }
                        if (dc.getType().isCompatible(DateAndTimeValue.class)) {
                            DateAndTimeValue v = (DateAndTimeValue)dc;
                            String result = dateFormat.format(
                                    v.getUTCCalendarClone().getTime());
                            return new StringCell(result);
                        }
                        LOGGER.error("Encountered unsupported data type: " 
                                + dc.getType() + " in row: " + row.getKey());
                        return DataType.getMissingCell();
                    }
        };
        if (!m_replaceCol.getBooleanValue()) {
            rearranger.append(factory);
        } else {
            rearranger.replace(factory, m_selectedCol.getStringValue());
        }
        return rearranger;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger rearranger = createColumnRearranger(
                inData[0].getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], 
                rearranger, exec);
        return new BufferedDataTable[] {out};
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_newColName.loadSettingsFrom(settings);
        m_replaceCol.loadSettingsFrom(settings);
        m_selectedCol.loadSettingsFrom(settings);
        m_pattern.loadSettingsFrom(settings);
        String dateFormat = m_pattern.getStringValue();
        // if it is not a predefined one -> store it
        if (!String2DateDialog.PREDEFINED_FORMATS.contains(dateFormat)) {
            StringHistory.getInstance(String2DateDialog.FORMAT_HISTORY_KEY).add(
                    dateFormat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_newColName.saveSettingsTo(settings);
        m_replaceCol.saveSettingsTo(settings);
        m_selectedCol.saveSettingsTo(settings);
        m_pattern.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_newColName.validateSettings(settings);
        m_replaceCol.validateSettings(settings);
        m_selectedCol.validateSettings(settings);
        SettingsModelBoolean replaceModelClone = m_replaceCol
            .createCloneWithValidatedValue(settings);
        // only if the original column is not replaced we need to check the new
        // column name
        boolean replace = replaceModelClone.getBooleanValue();
        if (replace) {
            return;
        }
        // check for valid and unique column name != null && !empty
        SettingsModelString colNameClone = m_newColName
            .createCloneWithValidatedValue(settings);
        String newColName = colNameClone.getStringValue();
        if (newColName == null || newColName.isEmpty()) {
            throw new InvalidSettingsException(
                    "New column name must not be empty!");
        }
        
        m_pattern.validateSettings(settings);
        SettingsModelString patternStringModel = m_pattern
            .createCloneWithValidatedValue(settings);
        String patternString = patternStringModel.getStringValue();
        // validate the pattern
        try {
            new SimpleDateFormat(patternString);
        } catch (Exception e) {
            throw new InvalidSettingsException("Pattern " + patternString 
                    + " is invalid!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }
    
}
