/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ------------------------------------------------------------------------
 * 
 * History
 *   05.10.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract.date;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.date.DateAndTimeValue;
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
import org.knime.timeseries.node.extract.AbstractTimeExtractorStringCellFactory;
import org.knime.timeseries.node.extract.SingleCellFactoryCompound;
import org.knime.timeseries.node.extract.time.TimeFieldExtractorNodeDialog;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateFieldExtractorNodeModel extends NodeModel {

    private final SettingsModelString m_selectedColumn 
        = DateFieldExtractorNodeDialog.createColumnSelectionModel();

    // year
    private final SettingsModelBoolean m_useYear = DateFieldExtractorNodeDialog
        .createUseTimeFieldModel(DateFieldExtractorNodeDialog.YEAR); 
    private final SettingsModelString m_yearColName = 
        DateFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
                DateFieldExtractorNodeDialog.YEAR);
    // quarter
    private final SettingsModelBoolean m_useQuarter 
        = DateFieldExtractorNodeDialog.createUseTimeFieldModel(
                DateFieldExtractorNodeDialog.QUARTER);
    private final SettingsModelString m_quarterColName 
        = DateFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
                DateFieldExtractorNodeDialog.QUARTER); 
    // month
    private final SettingsModelBoolean m_useMonth = DateFieldExtractorNodeDialog
        .createUseTimeFieldModel(DateFieldExtractorNodeDialog.MONTH); 
    private final SettingsModelString m_monthColName = 
        DateFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
                DateFieldExtractorNodeDialog.MONTH);
    private final SettingsModelString m_monthRepresentation
        = DateFieldExtractorNodeDialog.createRepresentationModelFor(
                DateFieldExtractorNodeDialog.MONTH);
    // day of week
    private final SettingsModelBoolean m_useDayOfWeek 
        = DateFieldExtractorNodeDialog.createUseTimeFieldModel(
                DateFieldExtractorNodeDialog.DAY_OF_WEEK);
    private final SettingsModelString m_dayOfWeekColName 
        = DateFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
                DateFieldExtractorNodeDialog.DAY_OF_WEEK);
    private final SettingsModelString m_dayOfWeekRepresentationModel 
        = DateFieldExtractorNodeDialog.createRepresentationModelFor(
                DateFieldExtractorNodeDialog.DAY_OF_WEEK);
    // day of month
    private final SettingsModelBoolean m_useDay = TimeFieldExtractorNodeDialog
        .createUseTimeFieldModel(DateFieldExtractorNodeDialog.DAY_OF_MONTH); 
    private final SettingsModelString m_dayColName = 
    DateFieldExtractorNodeDialog.createTimeFieldColumnNameModel(
            DateFieldExtractorNodeDialog.DAY_OF_MONTH);    
    
    
    /**
     * One in port containing {@link DateAndTimeValue}s, one out port with the 
     * extracted date fields appended.
     */
    public DateFieldExtractorNodeModel() {
        super(1, 1);
        DateFieldExtractorNodeDialog.addListener(m_useYear, m_yearColName);
        DateFieldExtractorNodeDialog.addListener(m_useMonth, m_monthColName);
        DateFieldExtractorNodeDialog.addListener(m_useDay, m_dayColName);
        DateFieldExtractorNodeDialog.addListener(m_useDayOfWeek, 
                m_dayOfWeekColName);
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
                    + " missing values due to missing date"
                    + " information in input date/time!");
        }
        return new BufferedDataTable[] {out};
    }

    private SingleCellFactoryCompound createColumnRearranger(
            final DataTableSpec inSpec) {
        final int colIdx = inSpec.findColumnIndex(
                m_selectedColumn.getStringValue());
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        List<AbstractTimeExtractorCellFactory>cellFactories 
            = new ArrayList<AbstractTimeExtractorCellFactory>();
        /* ************************* DATE fields factories *******************/
        // year
        if (m_useYear.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_yearColName.getStringValue());
            AbstractTimeExtractorCellFactory yearFactory 
                = new AbstractTimeExtractorIntCellFactory(
                    colName, colIdx, false) {
                @Override
                protected int extractTimeField(
                        final DateAndTimeValue value) {
                    return value.getYear();
                }
            };
            rearranger.append(yearFactory);
            cellFactories.add(yearFactory);
        }
        // quarter
        if (m_useQuarter.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_quarterColName.getStringValue());
            AbstractTimeExtractorCellFactory quarterFactory 
                = new AbstractTimeExtractorIntCellFactory(
                    colName, colIdx, false) {
                @Override
                protected int extractTimeField(
                        final DateAndTimeValue value) {
                    int month = value.getMonth();
                    // calculate the quarter under the assumption that 
                    // the month returned by DateAndTimeCell is 0-based
                    // hence we add 1
                    return (int)Math.ceil(month / 3) + 1;
                }
            };
            rearranger.append(quarterFactory);
            cellFactories.add(quarterFactory);
        }
        // month
        AbstractTimeExtractorCellFactory monthFactory = null;
        if (m_useMonth.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_monthColName.getStringValue());
            if (m_monthRepresentation.getStringValue().equals(
                    TimeFieldExtractorNodeDialog.AS_INT)) {
                monthFactory = new AbstractTimeExtractorIntCellFactory(
                        colName, colIdx, false) {
                    @Override
                    protected int extractTimeField(
                            final DateAndTimeValue value) {
                        // add 1 in order to start with 1, in contrast 
                        // to java.util.Calendar#MONTH
                        return value.getMonth() + 1;
                    }
                };
            } else {
                // extract the display name of the month
                monthFactory = new AbstractTimeExtractorStringCellFactory(
                        colName, colIdx, false) {

                    @Override
                    protected String extractTimeField(
                            final DateAndTimeValue value) {
                        return value.getUTCCalendarClone()
                            .getDisplayName(Calendar.MONTH, 
                                    Calendar.LONG, Locale.ENGLISH);
                    }
                };
            }
            rearranger.append(monthFactory);
            cellFactories.add(monthFactory);
        }
        // day
        if (m_useDay.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_dayColName.getStringValue());
            AbstractTimeExtractorCellFactory dayFactory 
                = new AbstractTimeExtractorIntCellFactory(
                    colName, colIdx, false) {
                @Override
                protected int extractTimeField(
                        final DateAndTimeValue value) {
                    return value.getDayOfMonth();
                }
            };
            rearranger.append(dayFactory);
            cellFactories.add(dayFactory);
        }
        // day of week
        AbstractTimeExtractorCellFactory dayOfWeekFactory = null; 
        if (m_useDayOfWeek.getBooleanValue()) {
            String colName = DataTableSpec.getUniqueColumnName(inSpec, 
                    m_dayOfWeekColName.getStringValue());
            if (m_dayOfWeekRepresentationModel.getStringValue().equals(
                    TimeFieldExtractorNodeDialog.AS_INT)) {
                dayOfWeekFactory = new AbstractTimeExtractorIntCellFactory(
                        colName, colIdx, false) {
                            @Override
                            protected int extractTimeField(
                                    final DateAndTimeValue value) {
                                return value.getUTCCalendarClone().get(
                                        Calendar.DAY_OF_WEEK);
                            }
                };
            } else {
                dayOfWeekFactory = new AbstractTimeExtractorStringCellFactory(
                        colName, colIdx, false) {
                    @Override
                    protected String extractTimeField(
                            final DateAndTimeValue value) {
                        return value.getUTCCalendarClone()
                            .getDisplayName(Calendar.DAY_OF_WEEK, 
                                    Calendar.LONG, Locale.ENGLISH);
                    }
                };
                // extract the display name of the day of week
            }
            rearranger.append(dayOfWeekFactory);
            cellFactories.add(dayOfWeekFactory);
        }
        return new SingleCellFactoryCompound(rearranger, cellFactories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }
    
    private boolean checkSelection() {
        boolean atLeastOneSelected = false;
        atLeastOneSelected |= m_useYear.getBooleanValue();
        atLeastOneSelected |= m_useQuarter.getBooleanValue();
        atLeastOneSelected |= m_useMonth.getBooleanValue();
        atLeastOneSelected |= m_useDay.getBooleanValue();
        atLeastOneSelected |= m_useDayOfWeek.getBooleanValue();
        return atLeastOneSelected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_selectedColumn.loadSettingsFrom(settings);
        // year
        m_useYear.loadSettingsFrom(settings);
        m_yearColName.loadSettingsFrom(settings);
        // quarter 
        m_useQuarter.loadSettingsFrom(settings);
        m_quarterColName.loadSettingsFrom(settings);
        // month 
        m_useMonth.loadSettingsFrom(settings);
        m_monthColName.loadSettingsFrom(settings);
        m_monthRepresentation.loadSettingsFrom(settings);
        // day
        m_useDay.loadSettingsFrom(settings);
        m_dayColName.loadSettingsFrom(settings);
        // day of week
        m_useDayOfWeek.loadSettingsFrom(settings);
        m_dayOfWeekColName.loadSettingsFrom(settings);
        m_dayOfWeekRepresentationModel.loadSettingsFrom(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_selectedColumn.validateSettings(settings);
        m_monthRepresentation.validateSettings(settings);
        SettingsModelString monthRepModel = m_monthRepresentation
            .createCloneWithValidatedValue(settings);
        String monthRep = monthRepModel.getStringValue();
        if (!monthRep.equals(TimeFieldExtractorNodeDialog.AS_INT)
                && !monthRep.equals(
                        TimeFieldExtractorNodeDialog.AS_STRING)) {
            throw new InvalidSettingsException(
                    "Month representation must be one of "
                    + TimeFieldExtractorNodeDialog.AS_INT
                    + ", " + TimeFieldExtractorNodeDialog.AS_STRING);
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
        // day of week
        m_useDayOfWeek.validateSettings(settings);
        m_dayOfWeekColName.validateSettings(settings);
        m_dayOfWeekRepresentationModel.validateSettings(settings);
        boolean atLeastOneChecked = false;
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useYear, m_yearColName);
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useQuarter, m_quarterColName);
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useMonth, m_monthColName);
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useDay, m_dayColName);
        atLeastOneChecked |= AbstractFieldExtractorNodeDialog
            .validateColumnName(settings, m_useDayOfWeek, m_dayOfWeekColName);
        // all unchecked?
        if (!atLeastOneChecked) {
            setWarningMessage("No time field selected. " 
                    + "Output table will be same as input table!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColumn.saveSettingsTo(settings);
        // year
        m_yearColName.saveSettingsTo(settings);
        m_useYear.saveSettingsTo(settings);
        // quarter
        m_quarterColName.saveSettingsTo(settings);
        m_useQuarter.saveSettingsTo(settings);
        // month
        m_monthColName.saveSettingsTo(settings);
        m_useMonth.saveSettingsTo(settings);
        m_monthRepresentation.saveSettingsTo(settings);
        // day
        m_dayColName.saveSettingsTo(settings);
        m_useDay.saveSettingsTo(settings);
        // day of week 
        m_dayOfWeekColName.saveSettingsTo(settings);
        m_useDayOfWeek.saveSettingsTo(settings);
        m_dayOfWeekRepresentationModel.saveSettingsTo(settings);
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
