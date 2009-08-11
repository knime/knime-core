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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.date.DateCell;
import org.knime.core.data.date.DateTimeCell;
import org.knime.core.data.date.TimeCell;
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
    
    private DataType m_type;

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
        validateDates(m_from.getCalendar(), m_to.getCalendar());
        return new DataTableSpec[] {
                createOutSpec()
        };
    }

    private DataTableSpec createOutSpec() {
        String name;
        // check data type
        // 1. only date?
        if (!m_from.useTime() && !m_to.useTime()) {
            // only date
            m_type = DateCell.TYPE;
            name = "Date";
        } else if (!m_from.useDate() && !m_to.useDate()) {
            m_type = TimeCell.TYPE;
            name = "Time";
        } else {
            m_type = DateTimeCell.TYPE;
            name = "DateTime";
        }
        DataColumnSpecCreator creator = new DataColumnSpecCreator(
                name, m_type);
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
        if (m_type.equals(TimeCell.TYPE)) {
            // "reset" the date fields 
            from = TimeCell.resetDateFields(from);
            to = TimeCell.resetDateFields(to);
        } else if (m_type.equals(DateCell.TYPE)) {
            from = DateCell.resetTimeFields(from);
            to = DateCell.resetTimeFields(to);    
        }
        long offset = (m_to.getCalendar().getTimeInMillis()
                - m_from.getCalendar().getTimeInMillis())
                / m_noOfRows.getIntValue();
        BufferedDataContainer container = exec.createDataContainer(
                createOutSpec());
        long currentTime = m_from.getCalendar().getTimeInMillis();
        for (int i = 0; i < m_noOfRows.getIntValue(); i++) {
            // zero based row key as FileReader
            RowKey key = new RowKey("Row" + i);
            DataCell cell;
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(currentTime);
            if (m_type.equals(TimeCell.TYPE)) {
                cell = new TimeCell(c);
            } else if (m_type.equals(DateCell.TYPE)) {
                cell = new DateCell(c);
            } else {
                cell = new DateTimeCell(c);
            }
            container.addRowToTable(new DefaultRow(key, cell));
            currentTime += offset;
        }
        container.close();
        return new BufferedDataTable[] {
                exec.createBufferedDataTable(container.getTable(), exec)
        };
    }

//    private Calendar resetDateFields(final Calendar calendar) {
//        calendar.clear(Calendar.YEAR);
//        calendar.clear(Calendar.MONTH);
//        calendar.clear(Calendar.DAY_OF_MONTH);
//        return calendar;
//    }
//    
//    private void resetTimeFields(final Calendar calendar) {
//        // reset the time to 12 o clock
//        calendar.clear(Calendar.HOUR_OF_DAY);
//        calendar.clear(Calendar.MINUTE);
//        calendar.clear(Calendar.SECOND);
//        // ignore millisecond
//    }

    /**
     * {@inheritDoc}
     */

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
        Calendar from = ((SettingsModelCalendar)m_from
                    .createCloneWithValidatedValue(settings)).getCalendar();
        Calendar to = ((SettingsModelCalendar)m_to
                    .createCloneWithValidatedValue(settings)).getCalendar();
        validateDates(from, to);
    }

    private void validateDates(final Calendar start, final Calendar end)
        throws InvalidSettingsException {
        if (end.before(start)
                || end.getTimeInMillis() == start.getTimeInMillis()) {
            throw new InvalidSettingsException("End point "
                    + end.getTime().toString()
                    + " must be after starting point "
                    + start.getTime().toString());
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
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

}
