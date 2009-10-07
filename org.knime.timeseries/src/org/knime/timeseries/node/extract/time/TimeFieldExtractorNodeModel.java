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
 *   24.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract.time;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.node.extract.AbstractFieldExtractorNodeDialog;
import org.knime.timeseries.node.extract.AbstractTimeExtractorCellFactory;
import org.knime.timeseries.node.extract.AbstractTimeExtractorIntCellFactory;
import org.knime.timeseries.node.extract.SingleCellFactoryCompound;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimeFieldExtractorNodeModel extends NodeModel {
    

    
    private final SettingsModelString m_selectedColumn 
        = TimeFieldExtractorNodeDialog.createColumnSelectionModel();

    // hour
    private final SettingsModelBoolean m_useHour = TimeFieldExtractorNodeDialog
        .createUseTimeFieldModel(TimeFieldExtractorNodeDialog.HOUR); 
    private final SettingsModelString m_hourColName = 
        TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
            TimeFieldExtractorNodeDialog.HOUR);
    // minute
    private final SettingsModelBoolean m_useMinute = 
        TimeFieldExtractorNodeDialog.createUseTimeFieldModel(
                TimeFieldExtractorNodeDialog.MINUTE); 
    private final SettingsModelString m_minuteColName = 
    TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
            TimeFieldExtractorNodeDialog.MINUTE);
    // second
    private final SettingsModelBoolean m_useSecond = 
        TimeFieldExtractorNodeDialog.createUseTimeFieldModel(
                TimeFieldExtractorNodeDialog.SECOND); 
    private final SettingsModelString m_secondColName = 
        TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
            TimeFieldExtractorNodeDialog.SECOND);
    // millis
    private final SettingsModelBoolean m_useMillis = 
        TimeFieldExtractorNodeDialog.createUseTimeFieldModel(
                TimeFieldExtractorNodeDialog.MILLISECOND); 
    private final SettingsModelString m_milliColName = 
    TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
            TimeFieldExtractorNodeDialog.MILLISECOND);
    
    /**
     * One in port containing {@link DateAndTimeValue}s, one out port with the 
     * extracted time fields appended.
     */
    public TimeFieldExtractorNodeModel() {
        super(1, 1);
        // add listener to the models
        TimeFieldExtractorNodeDialog.addListener(m_useHour, m_hourColName);
        TimeFieldExtractorNodeDialog.addListener(m_useMinute, m_minuteColName);
        TimeFieldExtractorNodeDialog.addListener(m_useSecond, m_secondColName);
        TimeFieldExtractorNodeDialog.addListener(m_useMillis, m_milliColName);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        // check input spec:
        // contains timestamp?
        if (!inSpec.containsCompatibleType(DateAndTimeValue.class)) {
            throw new InvalidSettingsException(
                    "No timestamp found in input table!");
        }
        // currently selected column still there?
        String selectedColName = m_selectedColumn.getStringValue(); 
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
                    m_selectedColumn.setStringValue(colName);
                    setWarningMessage("Auto-configure: selected " + colName);
                    break;
                }
            }
        }
        // create outputspec
        ColumnRearranger colRearranger = createColumnRearranger(inSpec)
            .getColumnRearranger();
        return new DataTableSpec[] {colRearranger.createSpec()};
    }
    

    
    private SingleCellFactoryCompound createColumnRearranger(
            final DataTableSpec inSpec) {
        final int colIdx = inSpec.findColumnIndex(
                m_selectedColumn.getStringValue());
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        List<AbstractTimeExtractorCellFactory> cellFactories 
            = new ArrayList<AbstractTimeExtractorCellFactory>();
        // ************************* TIME fields factories *******************/
        // hour
        AbstractTimeExtractorCellFactory hourFactory = null; 
        if (m_useHour.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_hourColName.getStringValue());
            hourFactory = new AbstractTimeExtractorIntCellFactory(
                    colName, colIdx, true) {
                @Override
                protected int extractTimeField(
                        final DateAndTimeValue value) {
                    return value.getHourOfDay();
                }
            };    
            rearranger.append(hourFactory);
            cellFactories.add(hourFactory);
        }
        // minute 
        AbstractTimeExtractorCellFactory minuteFactory = null;
        if (m_useMinute.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_minuteColName.getStringValue());
            minuteFactory = new AbstractTimeExtractorIntCellFactory(
                    colName, colIdx, true) {
                @Override
                protected int extractTimeField(
                        final DateAndTimeValue value) {
                    return value.getMinute();
                }
            };
            rearranger.append(minuteFactory);
            cellFactories.add(minuteFactory);
        }
        // second
        AbstractTimeExtractorCellFactory secondFactory = null;
        if (m_useSecond.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_secondColName.getStringValue());
            secondFactory = new AbstractTimeExtractorIntCellFactory(
                    colName, colIdx, true) {
                @Override
                protected int extractTimeField(
                        final DateAndTimeValue value) {
                    return value.getSecond();
                }
            };
            rearranger.append(secondFactory);
            cellFactories.add(secondFactory);
        }
        // millisecond
        AbstractTimeExtractorCellFactory milliFactory = null;
        if (m_useMillis.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_milliColName.getStringValue());
            milliFactory = new AbstractTimeExtractorIntCellFactory(
                    colName, colIdx, true) {
                // here we also have to check if the value has millis  
                @Override
                protected int extractTimeField(
                        final DateAndTimeValue value) {
                    return value.getMillis();
                }
                
                @Override
                public DataCell getCell(final DataRow row) {
                    DataCell cell = row.getCell(colIdx);
                    if (cell.isMissing()) {
                        return DataType.getMissingCell();
                    }
                    DateAndTimeValue value = (DateAndTimeValue)cell;
                    if (value.hasMillis()) {
                        producedValidValue();
                        return new IntCell(extractTimeField(value));
                    }
                    // no date set
                    increaseMissingValueCount();
                    return DataType.getMissingCell(); 
                }
            };
            rearranger.append(milliFactory);
            cellFactories.add(milliFactory);
        }
        return new SingleCellFactoryCompound(rearranger, cellFactories);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (!checkSelection()) {
            setWarningMessage(AbstractFieldExtractorNodeDialog
                    .NOTHING_SELECTED_MESSAGE);
        }
        SingleCellFactoryCompound compound = createColumnRearranger(
                inData[0].getDataTableSpec());
        ColumnRearranger rearranger = compound.getColumnRearranger();
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0],
                rearranger, exec);
        int nrMissingValues = 0;
        for (AbstractTimeExtractorCellFactory cellFactory 
                : compound.getUsedCellFactories()) {
            nrMissingValues += cellFactory.getNumberMissingValues();
        }
        if (nrMissingValues > 0) {
            setWarningMessage("Produced " + nrMissingValues 
                    + " missing values due to missing time"
                    + " information in input date/time!");
        }
        
        return new BufferedDataTable[] {out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }
    
    /**
     * 
     * @return <code>true</code> if at least one field is selected, 
     * <code>false</code> otherwise
     */
    private boolean checkSelection() {
        boolean atLeastOneSelected = false;
        atLeastOneSelected |= m_useHour.getBooleanValue();
        atLeastOneSelected |= m_useMinute.getBooleanValue();
        atLeastOneSelected |= m_useSecond.getBooleanValue();
        atLeastOneSelected |= m_useMillis.getBooleanValue();
        return atLeastOneSelected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColumn.saveSettingsTo(settings);
        // hour
        m_hourColName.saveSettingsTo(settings);
        m_useHour.saveSettingsTo(settings);
        // minute
        m_minuteColName.saveSettingsTo(settings);
        m_useMinute.saveSettingsTo(settings);
        // second
        m_secondColName.saveSettingsTo(settings);
        m_useSecond.saveSettingsTo(settings);
        // and the milliseconds
        m_milliColName.saveSettingsTo(settings);
        m_useMillis.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // basic checks 
        m_selectedColumn.validateSettings(settings);
        // hour
        m_useHour.validateSettings(settings);
        m_hourColName.validateSettings(settings);
        // minute
        m_useMinute.validateSettings(settings);
        m_minuteColName.validateSettings(settings);
        // second
        m_useSecond.validateSettings(settings);
        m_secondColName.validateSettings(settings);
        // and the milliseconds
        m_useMillis.validateSettings(settings);
        m_milliColName.validateSettings(settings);
        // now ensure that the column name is not null and not empty
        boolean atLeastOneChecked = false;
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useHour, m_hourColName);
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useMinute, m_minuteColName);
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useSecond, m_secondColName);
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useMillis, m_milliColName);
        // all unchecked?
        if (!atLeastOneChecked) {
            setWarningMessage(AbstractFieldExtractorNodeDialog
                    .NOTHING_SELECTED_MESSAGE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColumn.loadSettingsFrom(settings);
        // hour
        m_useHour.loadSettingsFrom(settings);
        m_hourColName.loadSettingsFrom(settings);
        // minute
        m_useMinute.loadSettingsFrom(settings);
        m_minuteColName.loadSettingsFrom(settings);
        // second
        m_useSecond.loadSettingsFrom(settings);
        m_secondColName.loadSettingsFrom(settings);
        // and the millisecond
        m_useMillis.loadSettingsFrom(settings);
        m_milliColName.loadSettingsFrom(settings);
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

