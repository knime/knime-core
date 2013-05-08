/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.generator;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * Generates equidistant times.
 *
 * @author Fabian Dill, KNIME.com AG, Zurich, Switzerland
 *
 */
public class DateGeneratorNodeModel extends NodeModel {

    private final SettingsModelCalendar m_from = DateGeneratorNodeDialog.createStartingPointModel();
    private final SettingsModelCalendar m_to = DateGeneratorNodeDialog.createEndPointModel();
    private final SettingsModelInteger m_noOfRows = DateGeneratorNodeDialog.createNumberOfRowsModel();

    private final SettingsModelBoolean m_useExecution = DateGeneratorNodeDialog.createUseCurrentForStart();

    /**
     *
     */
    public DateGeneratorNodeModel() {
        super(0, 1);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        validateDates(m_to);
        final Calendar from;
        if (m_useExecution.getBooleanValue()) {
            from = Calendar.getInstance(TimeZone.getDefault());
            from.setTimeInMillis(System.currentTimeMillis()
                                 + TimeZone.getDefault().getOffset(System.currentTimeMillis()));
            // no validation of from date necessary
        } else {
            from = m_from.getCalendar();
            validateDates(m_from);
        }
        int noRows = m_noOfRows.getIntValue();
        long offset = (long)calculateOffset(from, m_to.getCalendar(), noRows);
        // if no of row = 1 we simply return the start date
        if (Math.abs(offset) <= 0 && noRows > 1) {
           setWarningMessage(
                    "Number of rows too large for entered time period! "
                    + "All rows will contain the same time stamp.");
        }
        return new DataTableSpec[] {createOutSpec()};
    }

    private DataTableSpec createOutSpec() {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(
                "Date and time", DateAndTimeCell.TYPE);
        return new DataTableSpec(creator.createSpec());
    }

    private static double calculateOffset(final Calendar from, final Calendar to,
            final int noOfRows) {
        if (noOfRows <= 1) {
            return 0;
        }
        double toD = 1.0 * to.getTimeInMillis();
        double fromD = 1.0 * from.getTimeInMillis();
        // if offset is smaller than a second milliseconds
        // might be of interest
        return (toD - fromD) / (noOfRows - 1.0);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // prepare the calendars
        Calendar from = m_from.getCalendar();
        // new since 2.8. the start time is the current time.
        if (m_useExecution.getBooleanValue()) {
            from = Calendar.getInstance(TimeZone.getDefault());
            from.setTimeInMillis(System.currentTimeMillis()
                                 + TimeZone.getDefault().getOffset(System.currentTimeMillis()));
        }
        Calendar to = m_to.getCalendar();

        // if the use execution time is set, we ignore the settings for the from date
        boolean useDate = (m_from.useDate() && !m_useExecution.getBooleanValue()) || m_to.useDate();
        boolean useTime = (m_from.useTime() && !m_useExecution.getBooleanValue()) || m_to.useTime();
        boolean useMillis = (m_from.useMilliseconds() && !m_useExecution.getBooleanValue()) || m_to.useMilliseconds();
        if (useDate && !useTime) {
            DateAndTimeCell.resetTimeFields(from);
            DateAndTimeCell.resetTimeFields(to);
        } else if (useTime && !useDate) {
            DateAndTimeCell.resetDateFields(from);
            DateAndTimeCell.resetDateFields(to);
        }

        if (!useMillis) {
            from.clear(Calendar.MILLISECOND);
            to.clear(Calendar.MILLISECOND);
        }

        BufferedDataContainer container = exec.createDataContainer(createOutSpec());
        int nrRows = m_noOfRows.getIntValue();
        double offset = calculateOffset(from, to, nrRows);

        double currentTime = from.getTimeInMillis();
        for (int i = 0; i < nrRows; i++) {
            // zero based row key as FileReader
            RowKey key = new RowKey("Row" + i);
            DateAndTimeCell cell = new DateAndTimeCell((long)Math.ceil(currentTime), useDate, useTime, useMillis);
            container.addRowToTable(new DefaultRow(key, cell));
            currentTime += offset;
            exec.setProgress((i + 1) / (double)nrRows, "Generating row #" + (i + 1));
            exec.checkCanceled();
        }
        container.close();
        return new BufferedDataTable[] {
                exec.createBufferedDataTable(container.getTable(), exec)
        };
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
        m_from.saveSettingsTo(settings);
        m_to.saveSettingsTo(settings);
        m_noOfRows.saveSettingsTo(settings);
        m_useExecution.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_from.validateSettings(settings);
        m_to.validateSettings(settings);
        m_noOfRows.validateSettings(settings);
        // we only validate the true date if we do not use the execution time
        boolean checkFrom = true;
        try {
            SettingsModelBoolean useEx = m_useExecution.createCloneWithValidatedValue(settings);
            if (useEx.getBooleanValue()) {
                checkFrom = false;
            }

        } catch (Exception e) {
              //  Do nothing, backward compatibility
        }
        if (checkFrom) {
            SettingsModelCalendar from = m_from.createCloneWithValidatedValue(settings);
            validateDates(from);
        }
        SettingsModelCalendar to = m_to.createCloneWithValidatedValue(settings);
        validateDates(to);

    }

    private static void validateDates(final SettingsModelCalendar time)
        throws InvalidSettingsException {
        // check for !useDate and !useTime
        if (!time.useDate() && !time.useTime()) {
            throw new InvalidSettingsException("Timestamp must consists of date or time!");
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_from.loadSettingsFrom(settings);
        m_to.loadSettingsFrom(settings);
        m_noOfRows.loadSettingsFrom(settings);
        // new since 2.8.
        try {
            m_useExecution.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ie) {
            // set default value
            m_useExecution.setBooleanValue(false);

        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no view -> no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no view -> no internals
    }

}
