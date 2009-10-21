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
 */
package org.knime.timeseries.node.diff;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Appends the difference between two dates with a selected granularity (year,
 * quarter, month, week, day, hour, minute).
 * 
 * @author KNIME GmbH
 */
public class TimeDifferenceNodeModel extends NodeModel {

    // first date column
    private final SettingsModelString m_col1 = TimeDifferenceNodeDialog
            .createColmn1Model();

    // ... and the referring index
    private int m_col1Idx;

    // second date column
    private final SettingsModelString m_col2 = TimeDifferenceNodeDialog
            .createColumn2Model();

    // ... and the referring index
    private int m_col2Idx;

    // new column name
    private final SettingsModelString m_newColName = TimeDifferenceNodeDialog
            .createNewColNameModel();

    // selected granularity level
    private final SettingsModelString m_granularity = TimeDifferenceNodeDialog
            .createGranularityModel();

    // number of fraction digits for rounding
    private final SettingsModelInteger m_rounding = TimeDifferenceNodeDialog
            .createRoundingModel();

    /**
     * Constructor for the node model with one in and one out port.
     */
    protected TimeDifferenceNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // get the selected granularity level
        final Granularity g = Granularity.valueOf(m_granularity
                .getStringValue());
        // create rearranger
        ColumnRearranger rearranger = new ColumnRearranger(inData[0]
                .getDataTableSpec());
        // append the new column with single cell factory
        rearranger.append(new SingleCellFactory(createOutputColumnSpec(
                inData[0].getDataTableSpec(), m_newColName.getStringValue())) {
            /**
             * Value for the new column is based on the values of two column of
             * the row (first and second date column), the selected granularity,
             * and the fraction digits for rounding.
             * 
             * @param row the current row
             * @return the difference between the two date values with the given
             *         granularity and rounding
             */
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell1 = row.getCell(m_col1Idx);
                DataCell cell2 = row.getCell(m_col2Idx);
                if ((cell1.isMissing()) || (cell2.isMissing())) {
                    return DataType.getMissingCell();
                }
                long first = ((DateAndTimeValue)cell1).getUTCTimeInMillis();
                long last = ((DateAndTimeValue)cell2).getUTCTimeInMillis();
                double diffTime = (last - first) / g.getFactor();
                BigDecimal bd = new BigDecimal(diffTime);
                bd = bd.setScale(m_rounding.getIntValue(),
                        BigDecimal.ROUND_CEILING);
                return new DoubleCell(bd.doubleValue());
            }
        });
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0],
                rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check for at least two date columns and auto configure
        String firstColName = m_col1.getStringValue();
        String secondColName = m_col2.getStringValue();
        boolean autoConfigure = (firstColName == null || firstColName.isEmpty())
            && (secondColName == null || secondColName.isEmpty());
        if (autoConfigure) {
            // avoid NullPointer
            m_col1.setStringValue("");
            m_col2.setStringValue("");
        }
        DataTableSpec inSpec = inSpecs[0];
        int nrDateCols = 0;
        for (DataColumnSpec colSpec : inSpec) {
            if (colSpec.getType().isCompatible(DateAndTimeValue.class)) {
                nrDateCols++;
                if (nrDateCols >= 2 && !autoConfigure) {
                    break;
                }
                // if autoconfigure -> set first column
                if (autoConfigure && m_col1.getStringValue().isEmpty()) {
                    m_col1.setStringValue(colSpec.getName());
                } else if (autoConfigure 
                        // if autoconfigure=true first column was null/empty
                        // if not empty anymore autoconfigure second column
                        && !m_col1.getStringValue().isEmpty()) {
                    m_col2.setStringValue(colSpec.getName());
                    setWarningMessage("Auto-configure: first column = \""
                            + m_col1.getStringValue() + "\", second column = \""
                            + m_col2.getStringValue() + "\"");
                }
            }
        }
        if (nrDateCols < 2) {
            m_col1.setStringValue("");
            m_col2.setStringValue("");
            throw new InvalidSettingsException(
                    "Input must contain at least two date/time columns!");
        }
        m_col1Idx = inSpecs[0].findColumnIndex(m_col1.getStringValue());
        m_col2Idx = inSpecs[0].findColumnIndex(m_col2.getStringValue());
        // check for first date column in input spec
        if (m_col1Idx < 0) {
            throw new InvalidSettingsException("Column "
                    + m_col1.getStringValue() + " not found in input table");
        }
        // check for second date column in input spec
        if (m_col2Idx < 0) {
            throw new InvalidSettingsException("Column "
                    + m_col2.getStringValue() + " not found in input table");
        }
        // return new spec with appended column
        // (time and chosen new column name)
        return new DataTableSpec[]{new DataTableSpec(inSpecs[0],
                new DataTableSpec(createOutputColumnSpec(inSpecs[0],
                        m_newColName.getStringValue())))};
    }

    private DataColumnSpec createOutputColumnSpec(final DataTableSpec spec,
            final String newColName) {
        // get unique column name based on the entered column name
        m_newColName.setStringValue(DataTableSpec.getUniqueColumnName(spec,
                newColName));
        // create column spec with type date and new (now uniwue) column name
        DataColumnSpecCreator creator = new DataColumnSpecCreator(m_newColName
                .getStringValue(), DoubleCell.TYPE);
        return creator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_col1.saveSettingsTo(settings);
        m_col2.saveSettingsTo(settings);
        m_newColName.saveSettingsTo(settings);
        m_granularity.saveSettingsTo(settings);
        m_rounding.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col1.loadSettingsFrom(settings);
        m_col2.loadSettingsFrom(settings);
        m_newColName.loadSettingsFrom(settings);
        m_granularity.loadSettingsFrom(settings);
        m_rounding.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col1.validateSettings(settings);
        m_col2.validateSettings(settings);
        m_newColName.validateSettings(settings);
        m_granularity.validateSettings(settings);
        m_rounding.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing
    }

}
