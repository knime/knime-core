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
package org.knime.timeseries.node.extract;

import java.io.File;
import java.io.IOException;

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

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimeFieldExtractorNodeModel extends NodeModel {
    
    private final SettingsModelString m_selectedColumn 
        = TimeFieldExtractorNodeDialog.createColumnSelectionModel();
    
    private final SettingsModelString m_monthRepresentation
        = TimeFieldExtractorNodeDialog.createMonthRepresentationModel();
    
    // year
    private final SettingsModelBoolean m_useYear = TimeFieldExtractorNodeDialog
        .createUseTimeFieldModel(TimeFieldExtractorNodeDialog.YEAR); 
    private final SettingsModelString m_yearColName = 
        TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
                TimeFieldExtractorNodeDialog.YEAR);
    // quarter
    private final SettingsModelBoolean m_useQuarter 
        = TimeFieldExtractorNodeDialog.createUseTimeFieldModel(
                TimeFieldExtractorNodeDialog.QUARTER);
    private final SettingsModelString m_quarterColName 
        = TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
                TimeFieldExtractorNodeDialog.QUARTER); 
    // month
    private final SettingsModelBoolean m_useMonth = TimeFieldExtractorNodeDialog
        .createUseTimeFieldModel(TimeFieldExtractorNodeDialog.MONTH); 
    private final SettingsModelString m_monthColName = 
        TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
                TimeFieldExtractorNodeDialog.MONTH);
    // day
    private final SettingsModelBoolean m_useDay = TimeFieldExtractorNodeDialog
    .createUseTimeFieldModel(TimeFieldExtractorNodeDialog.DAY); 
    private final SettingsModelString m_dayColName = 
    TimeFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
            TimeFieldExtractorNodeDialog.DAY);
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
        TimeFieldExtractorNodeDialog.addListener(m_useYear, m_yearColName);
        TimeFieldExtractorNodeDialog.addListener(m_useMonth, m_monthColName);
        TimeFieldExtractorNodeDialog.addListener(m_useDay, m_dayColName);
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
        // check for unique column names
        checkUniqueColumnName(m_yearColName, inSpec);
        checkUniqueColumnName(m_quarterColName, inSpec);
        checkUniqueColumnName(m_monthColName, inSpec);
        checkUniqueColumnName(m_dayColName, inSpec);
        checkUniqueColumnName(m_hourColName, inSpec);
        checkUniqueColumnName(m_minuteColName, inSpec);
        checkUniqueColumnName(m_secondColName, inSpec);
        checkUniqueColumnName(m_milliColName, inSpec);
        // create outputspec
        ColumnRearranger colRearranger = createColumnRearranger(inSpec);
        return new DataTableSpec[] {colRearranger.createSpec()};
    }
    
    /**
     * Gets a unique column name in the given spec and sets it back into the 
     * model.
     * 
     * @param colNameModel column name model to check
     * @param tableSpec spec for which the name should be unique
     */
    private void checkUniqueColumnName(final SettingsModelString colNameModel,
            final DataTableSpec inSpec) {
        String colName = colNameModel.getStringValue();
        colName = DataTableSpec.getUniqueColumnName(inSpec, colName);
        colNameModel.setStringValue(colName);
    }
    
    private ColumnRearranger createColumnRearranger(
            final DataTableSpec inSpec) {
        final int colIdx = inSpec.findColumnIndex(
                m_selectedColumn.getStringValue());
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        /* ************************* DATE fields factories *******************/
        // year
        if (m_useYear.getBooleanValue()) {
            rearranger.append(new AbstractTimeExtractorCellFactory(
                    m_yearColName.getStringValue(), colIdx, false) {
                        @Override
                        int extractTimeField(final DateAndTimeValue value) {
                            return value.getYear();
                        }
            });
        }
        // quarter
        if (m_useQuarter.getBooleanValue()) {
            rearranger.append(new AbstractTimeExtractorCellFactory(
                    m_quarterColName.getStringValue(), colIdx, false) {
                    @Override
                    int extractTimeField(final DateAndTimeValue value) {
                        int month = value.getMonth();
                        // calculate the quarter under the assumption that 
                        // the month returned by DateAndTimeCell is 0-based
                        // hence we add 1
                        return (int)Math.ceil(month / 3) + 1;
                    }
            });
        }
        // month
        if (m_useMonth.getBooleanValue()) {
            if (m_monthRepresentation.getStringValue().equals(
                    TimeFieldExtractorNodeDialog.MONTH_AS_INT)) {
                rearranger.append(new AbstractTimeExtractorCellFactory(
                        m_monthColName.getStringValue(), colIdx, false) {
                            @Override
                            int extractTimeField(final DateAndTimeValue value) {
                                // add 1 in order to start with 1, in contrast 
                                // to java.util.Calendar#MONTH
                                return value.getMonth() + 1;
                            }
                });
            } else {
                // extract the display name of the month
                rearranger.append(new MonthStringExtractorCellFactory(
                        m_monthColName.getStringValue(), colIdx));
            }
        }
        // day
        if (m_useDay.getBooleanValue()) {
            rearranger.append(new AbstractTimeExtractorCellFactory(
                    m_dayColName.getStringValue(), colIdx, false) {
                        @Override
                        int extractTimeField(final DateAndTimeValue value) {
                            return value.getDayOfMonth();
                        }
            });
        }
        // ************************* TIME fields factories *******************/
        // hour
        if (m_useHour.getBooleanValue()) {
            rearranger.append(new AbstractTimeExtractorCellFactory(
                    m_hourColName.getStringValue(), colIdx, true) {
                        @Override
                        int extractTimeField(final DateAndTimeValue value) {
                            return value.getHourOfDay();
                        }
            });
        }
        // minute
        if (m_useMinute.getBooleanValue()) {
            rearranger.append(new AbstractTimeExtractorCellFactory(
                    m_minuteColName.getStringValue(), colIdx, true) {
                        @Override
                        int extractTimeField(final DateAndTimeValue value) {
                            return value.getMinute();
                        }
            });
        }
        // second
        if (m_useSecond.getBooleanValue()) {
            rearranger.append(new AbstractTimeExtractorCellFactory(
                    m_secondColName.getStringValue(), colIdx, true) {
                        @Override
                        int extractTimeField(final DateAndTimeValue value) {
                            return value.getSecond();
                        }
            });
        }
        // millisecond
        if (m_useMillis.getBooleanValue()) {
            rearranger.append(new AbstractTimeExtractorCellFactory(
                    m_milliColName.getStringValue(), colIdx, true) {
                        // here we also have to check if the value  
                        @Override
                        int extractTimeField(final DateAndTimeValue value) {
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
                                return new IntCell(extractTimeField(value));
                            }
                            // no date set
                            return DataType.getMissingCell(); 
                        }
            });
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
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColumn.saveSettingsTo(settings);
        m_monthRepresentation.saveSettingsTo(settings);
        // year
        m_yearColName.saveSettingsTo(settings);
        m_useYear.saveSettingsTo(settings);
        // quarter
        m_quarterColName.saveSettingsTo(settings);
        m_useQuarter.saveSettingsTo(settings);
        // month
        m_monthColName.saveSettingsTo(settings);
        m_useMonth.saveSettingsTo(settings);
        // day
        m_dayColName.saveSettingsTo(settings);
        m_useDay.saveSettingsTo(settings);
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
        m_monthRepresentation.validateSettings(settings);
        SettingsModelString monthRepModel = m_monthRepresentation
            .createCloneWithValidatedValue(settings);
        String monthRep = monthRepModel.getStringValue();
        if (!monthRep.equals(TimeFieldExtractorNodeDialog.MONTH_AS_INT)
                && !monthRep.equals(
                        TimeFieldExtractorNodeDialog.MONTH_AS_STRING)) {
            throw new InvalidSettingsException(
                    "Month representation must be one of "
                    + TimeFieldExtractorNodeDialog.MONTH_AS_INT
                    + ", " + TimeFieldExtractorNodeDialog.MONTH_AS_STRING);
        }
        //year
        m_useYear.validateSettings(settings);
        m_yearColName.validateSettings(settings);
        // quarter 
        m_useQuarter.validateSettings(settings);
        m_quarterColName.validateSettings(settings);
        // month
        m_useMonth.validateSettings(settings);
        m_monthColName.validateSettings(settings);
        // day
        m_useDay.validateSettings(settings);
        m_dayColName.validateSettings(settings);
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
        atLeastOneChecked |= validateColumnName(settings, m_useYear, 
                m_yearColName);
        atLeastOneChecked |= validateColumnName(settings, m_useQuarter, 
                m_quarterColName);
        atLeastOneChecked |= validateColumnName(settings, m_useMonth, 
                m_monthColName);
        atLeastOneChecked |= validateColumnName(settings, m_useDay, 
                m_dayColName);
        atLeastOneChecked |= validateColumnName(settings, m_useHour, 
                m_hourColName);
        atLeastOneChecked |= validateColumnName(settings, m_useMinute, 
                m_minuteColName);
        atLeastOneChecked |= validateColumnName(settings, m_useSecond, 
                m_secondColName);
        atLeastOneChecked |= validateColumnName(settings, m_useMillis, 
                m_milliColName);
        // all unchecked?
        if (!atLeastOneChecked) {
            setWarningMessage("No time field selected. " 
                    + "Output table will be same as input table!");
        }
    }

    /**
     * 
     * @param settings settings to read from
     * @param enabledModel the check box model in order to validate only active
     * column name models
     * @param colNameModel the column name model for which the value should be 
     * validated
     * @return true if the name is enabled and valid, falsei f the name is not 
     * enabled 
     * @throws InvalidSettingsException if the string value of the column model
     * is either <code>null</code> or empty 
     */
    private boolean validateColumnName(final NodeSettingsRO settings,
            final SettingsModelBoolean enabledModel,
            final SettingsModelString colNameModel) 
    throws InvalidSettingsException {
        SettingsModelBoolean isEnabled = enabledModel
            .createCloneWithValidatedValue(settings);
        if (!isEnabled.getBooleanValue()) {
            return false;
        }
        SettingsModelString colNameClone = colNameModel
                .createCloneWithValidatedValue(settings);
        String colName = colNameClone.getStringValue();
        if (colName == null || colName.isEmpty()) {
            throw new InvalidSettingsException(
                    "A column name must not be empty!");
        }
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColumn.loadSettingsFrom(settings);
        m_monthRepresentation.loadSettingsFrom(settings);
        // year
        m_useYear.loadSettingsFrom(settings);
        m_yearColName.loadSettingsFrom(settings);
        // quarter 
        m_useQuarter.loadSettingsFrom(settings);
        m_quarterColName.loadSettingsFrom(settings);
        // month 
        m_useMonth.loadSettingsFrom(settings);
        m_monthColName.loadSettingsFrom(settings);
        // day
        m_useDay.loadSettingsFrom(settings);
        m_dayColName.loadSettingsFrom(settings);
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

