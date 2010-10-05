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
 * ------------------------------------------------------------------------
 *
 * History
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.generator;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * Generates equidistant times.
 * 
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 *
 */
public class DateGeneratorNodeModel extends NodeModel {

    private final SettingsModelCalendar m_from = DateGeneratorNodeDialog
        .createStartingPointModel();
    private final SettingsModelCalendar m_to = DateGeneratorNodeDialog
        .createEndPointModel();
    private final SettingsModelInteger m_noOfRows = DateGeneratorNodeDialog
        .createNumberOfRowsModel();
    
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
        validateDates(m_from, m_to);
        return new DataTableSpec[] {
                createOutSpec()
        };
    }

    private DataTableSpec createOutSpec() {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(
                "Date and time", DateAndTimeCell.TYPE);
        return new DataTableSpec(creator.createSpec());
    }
    
    private static long calculateOffset(final Calendar from, final Calendar to, 
            final int noOfRows) {
        if (noOfRows <= 1) {
            return 0;
        }
        // if offset is smaller than a second milliseconds 
        // might be of interest
        return (to.getTimeInMillis() - from.getTimeInMillis())
                / (noOfRows - 1);
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
        Calendar to = m_to.getCalendar();
        boolean useDate = m_from.useDate() || m_to.useDate();
        boolean useTime = m_from.useTime() || m_to.useTime();
        boolean useMillis = m_from.useMilliseconds() || m_to.useMilliseconds();
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
        BufferedDataContainer container = exec.createDataContainer(
                createOutSpec());
        int nrRows = m_noOfRows.getIntValue(); 
        long offset = calculateOffset(from, to, nrRows);
        long currentTime = from.getTimeInMillis();
        Calendar test = DateAndTimeCell.getUTCCalendar();
        test.setTimeInMillis(currentTime);
        int currentRow = 0;
        for (int i = 0; i < nrRows; i++) {
            // zero based row key as FileReader
            RowKey key = new RowKey("Row" + i);
            DateAndTimeCell cell = new DateAndTimeCell(currentTime, 
                    useDate, useTime, useMillis);
            container.addRowToTable(new DefaultRow(key, cell));
            currentTime += offset;
            currentRow++;
            exec.setProgress(currentRow / (double)nrRows, "Generating row #" 
                    + currentRow);
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
        SettingsModelInteger noRowsModel = m_noOfRows
            .createCloneWithValidatedValue(settings);
        SettingsModelCalendar from = m_from.createCloneWithValidatedValue(
                settings);
        SettingsModelCalendar to = m_to.createCloneWithValidatedValue(settings);
        validateDates(from, to);
        int noRows = noRowsModel.getIntValue();
        long offset = calculateOffset(from.getCalendar(), to.getCalendar(), 
                noRows);
        // if no of row = 1 we simply return the start date
        if (offset <= 0 && noRows > 1) {
            throw new InvalidSettingsException(
                    "Number of rows too large for entered time period! " 
                    + "Steps are smaller than a millisecond. " 
                    + "Please reduce number of rows.");
        }
    }

    private static void validateDates(final SettingsModelCalendar start, 
            final SettingsModelCalendar end)
        throws InvalidSettingsException {
        // check for !useDate and !useTime
        if (!start.useDate() && !start.useTime()) {
            throw new InvalidSettingsException(
                    "Timestamp must consists of date or time!");
        }
        if (!end.useDate() && !end.useTime()) {
            throw new InvalidSettingsException(
                    "Timestamp must consists of date or time!");
        }
        
        if (end.getCalendar().before(start.getCalendar())
                || end.getCalendar().getTimeInMillis() == start.getCalendar()
                .getTimeInMillis()) {
            throw new InvalidSettingsException("End point "
                    + end.toString()
                    + " must be after starting point "
                    + start.toString());
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
