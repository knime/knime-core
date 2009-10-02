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
package org.knime.timeseries.node.preset;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

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
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * Sets the time or date to a default (user entered) one by those 
 * {@link DateAndTimeValue}s, which lack the referring fields.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimePresetNodeModel extends NodeModel {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            TimePresetNodeModel.class);

    private final SettingsModelCalendar m_presetCalendar 
        = TimePresetNodeDialog.createCalendarModel();
    
    private final SettingsModelString m_selectedCol 
        = TimePresetNodeDialog.createColumnSelectionModel();
    
    private final SettingsModelBoolean m_replaceMissingValues
        = TimePresetNodeDialog.createReplaceMissingValuesModel();
    
    /**
     * One in-port with time values and one out-port with the preset values. 
     */
    public TimePresetNodeModel() {
        super(1, 1);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // does input spec contain a date and time col?
        DataTableSpec inSpec = inSpecs[0];
        if (!inSpec.containsCompatibleType(DateAndTimeValue.class)) {
            throw new InvalidSettingsException(
                    "Input table must contain at least one time column!");
        }
        String selectedColName = m_selectedCol.getStringValue(); 
        if (selectedColName != null && !selectedColName.isEmpty()) {
            // already set -> search for column name in input table
            if (!inSpec.containsName(selectedColName)) {
                throw new InvalidSettingsException("Column " + selectedColName
                        + " not found in input table!");
            } else {
                // check if it is of correct type
                DataColumnSpec colSpec = inSpec.getColumnSpec(selectedColName); 
                if (!colSpec.getType().isCompatible(DateAndTimeValue.class)) {
                    throw new InvalidSettingsException(
                            "Selected column (" + selectedColName 
                            + ") must contain date or time!");
                }
            }
        } else {
            // not yet set -> auto-configure: choose first time column
            for (DataColumnSpec colSpec : inSpec) {
                if (colSpec.getType().isCompatible(DateAndTimeValue.class)) {
                    String colName = colSpec.getName();
                    m_selectedCol.setStringValue(colName);
                    setWarningMessage("Auto-configure: selected " + colName);
                    // take the first compatible column
                    break;
                }
            }
        }
        // output spec is same as input spec: 
        // values in time column are "enriched"
        return inSpecs;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable input = inData[0];
        DataTableSpec spec = input.getDataTableSpec();
        String selectedColName = m_selectedCol.getStringValue();
        final int colIdx = spec.findColumnIndex(selectedColName);
        if (colIdx < 0) {
            throw new IllegalArgumentException("Column " + selectedColName
                    + " not found in input table!");
        }
        DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(
                spec.getColumnSpec(selectedColName));
        /*
         * Reset the domain here. Had problems when time was there and a date is
         * added, then the time without the date will stay the lower bound of 
         * the domain.
         */
        colSpecCreator.setDomain(null);
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        final Calendar preset = m_presetCalendar.getCalendar();
        rearranger.replace(new SingleCellFactory(colSpecCreator.createSpec()) {

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell dc = row.getCell(colIdx);
                if (dc.isMissing()) {
                    if (m_replaceMissingValues.getBooleanValue()) {
                        return new DateAndTimeCell(preset.getTimeInMillis(),
                                m_presetCalendar.useDate(), 
                                m_presetCalendar.useTime(), 
                                m_presetCalendar.useMilliseconds());
                    }
                    return DataType.getMissingCell();
                }
                if (dc.getType().isCompatible(DateAndTimeValue.class)) {
                    DateAndTimeValue v = (DateAndTimeValue)dc;
                    Calendar existing = v.getUTCCalendarClone();
                    // look at the date
                    if (m_presetCalendar.useDate() && !v.hasDate()) {
                        // set it
                        existing.set(Calendar.YEAR, preset.get(Calendar.YEAR));
                        existing.set(Calendar.MONTH, 
                                preset.get(Calendar.MONTH));
                        existing.set(Calendar.DAY_OF_MONTH, 
                                preset.get(Calendar.DAY_OF_MONTH));
                    }
                    if (m_presetCalendar.useTime() && !v.hasTime()) {
                        // set it
                        existing.set(Calendar.HOUR_OF_DAY, 
                                preset.get(Calendar.HOUR_OF_DAY));
                        existing.set(Calendar.MINUTE, 
                                preset.get(Calendar.MINUTE));
                        existing.set(Calendar.SECOND, 
                                preset.get(Calendar.SECOND));
                        if (m_presetCalendar.useMilliseconds()) {
                            existing.set(Calendar.MILLISECOND, 
                                    preset.get(Calendar.MILLISECOND));
                        }
                    }
                    return new DateAndTimeCell(existing.getTimeInMillis(),
                            m_presetCalendar.useDate() || v.hasDate(),
                            m_presetCalendar.useTime() || v.hasTime(), 
                            m_presetCalendar.useMilliseconds()
                            || v.hasMillis());
                }
                LOGGER.error("Unsupported type "  + dc.getType() 
                        + " found in row " + row.getKey() + "!");
                return DataType.getMissingCell();
            }
            
        }, colIdx);
        BufferedDataTable out = exec.createColumnRearrangeTable(
                input, rearranger, exec);
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
        m_presetCalendar.loadSettingsFrom(settings);
        m_selectedCol.loadSettingsFrom(settings);
        m_replaceMissingValues.loadSettingsFrom(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_presetCalendar.saveSettingsTo(settings);
        m_selectedCol.saveSettingsTo(settings);
        m_replaceMissingValues.saveSettingsTo(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_presetCalendar.validateSettings(settings);
        m_selectedCol.validateSettings(settings);
        m_replaceMissingValues.validateSettings(settings);
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals

    }

}
