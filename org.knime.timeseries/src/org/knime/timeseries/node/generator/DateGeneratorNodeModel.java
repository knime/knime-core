/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
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
 * @author Fabian Dill
 *
 */
public class DateGeneratorNodeModel extends NodeModel {

    private final SettingsModelCalendar m_from = DateGeneratorNodeDialog
        .createStartingPointModel();
    private final SettingsModelCalendar m_to = DateGeneratorNodeDialog
        .createEndPointModel();
    private final SettingsModelInteger m_noOfRows = DateGeneratorNodeDialog
        .createNumberOfRowsModel();
    
    private boolean m_useDate;
    
    private boolean m_useTime;

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
                "Timestamp", DateAndTimeCell.TYPE);
        return new DataTableSpec(creator.createSpec());
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
        if (m_useDate && !m_useTime) {
            DateAndTimeCell.resetTimeFields(from);
            DateAndTimeCell.resetTimeFields(to);
        } else if (m_useTime && !m_useDate) {
            DateAndTimeCell.resetDateFields(from);
            DateAndTimeCell.resetDateFields(to);
        }
        long offset = (to.getTimeInMillis() - from.getTimeInMillis())
                / m_noOfRows.getIntValue();
        BufferedDataContainer container = exec.createDataContainer(
                createOutSpec());
        // offset is shorter than a day -> so we need the time fields
        boolean needsTimeFields = offset < 86400000; // <- one day
        // if offset is smaller than a minute milliseconds might be of interest
        boolean needsMillis = offset < 60000;
        long currentTime = from.getTimeInMillis();
        Calendar test = DateAndTimeCell.getUTCCalendar();
        test.setTimeInMillis(currentTime);
        for (int i = 0; i < m_noOfRows.getIntValue(); i++) {
            // zero based row key as FileReader
            RowKey key = new RowKey("Row" + i);
            DateAndTimeCell cell = new DateAndTimeCell(currentTime, 
                    m_useDate, m_useTime || needsTimeFields, needsMillis);
            container.addRowToTable(new DefaultRow(key, cell));
            currentTime += offset;
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
        // TODO Auto-generated method stub

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
        SettingsModelCalendar from = m_from.createCloneWithValidatedValue(
                settings);
        SettingsModelCalendar to = m_to.createCloneWithValidatedValue(settings);
        // check for !useDate and !useTime
        if (!from.useDate() && !from.useTime()) {
            throw new InvalidSettingsException(
                    "Timestamp must consists of date or time!");
        }
        if (!to.useDate() && !to.useTime()) {
            throw new InvalidSettingsException(
                    "Timestamp must consists of date or time!");
        }
        validateDates(from, to);
    }

    private void validateDates(final SettingsModelCalendar start, 
            final SettingsModelCalendar end)
        throws InvalidSettingsException {
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
        m_useDate = false;
        m_useTime = false;
        // check data type
        // 1. only date?
        if (m_from.useDate() || m_to.useDate()) {
            // only date
            m_useDate = true;
        } 
        if (m_from.useTime() || m_to.useTime()) {
            // only time
            m_useTime = true;
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
