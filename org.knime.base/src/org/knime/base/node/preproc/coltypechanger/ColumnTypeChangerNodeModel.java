/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.base.node.preproc.coltypechanger;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * The column type changer node model which converts a string column to a numeric or date-type column iff all
 * column-entries could be converted.
 *
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
public class ColumnTypeChangerNodeModel extends NodeModel {

    private DataColumnSpecFilterConfiguration m_conf;

    private String m_dateFormat = "dd.MM.yy";

    /**
     * Creates a new node model with one in- and outport.
     */
    public ColumnTypeChangerNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        double progress = 0;
        final BufferedDataTable data = inData[0];
        final String[] incls = m_conf.applyTo(data.getDataTableSpec()).getIncludes();
        final DataType[] types = new DataType[incls.length];
        final double max = incls.length + data.getRowCount();

        if (data.getRowCount() > 0) {
            // empty table check

            for (DataRow row : data) {
                for (int i = 0; i < incls.length; i++) {
                    // guess for each cell in each column the best matching datatype
                    DataType newType = typeGuesser(row.getCell(data.getDataTableSpec().findColumnIndex(incls[i])));
                    if (types[i] != null) {
                        types[i] = setType(types[i], newType);
                    } else {
                        types[i] = newType;
                    }
                    exec.checkCanceled();
                }
                exec.checkCanceled();
                progress++;
                exec.setProgress(progress / max);
            }

            for (int i = 0; i < types.length; i++) {
                // if one column only contains missingCells than set column type to StringCell
                if (types[i].equals(DataType.getMissingCell().getType())) {
                    types[i] = StringCell.TYPE;
                }
            }

            ColumnRearranger arrange = new ColumnRearranger(data.getDataTableSpec());
            for (int i = 0; i < incls.length; i++) {
                final int colIdx = data.getDataTableSpec().findColumnIndex(incls[i]);
                final DataType type = types[i];

                if (!type.equals(StringCell.TYPE)) {
                    // convert only columns with a new type (not StringCell)

                    DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(incls[i], types[i]);
                    DataColumnSpec colSpec = colSpecCreator.createSpec();

                    if (type.equals(DateAndTimeCell.TYPE)) {
                        arrange.replace(createDateAndTimeConverter(colIdx, colSpec), colIdx);
                    } else if (type.equals(LongCell.TYPE)) {
                        arrange.replace(createLongConverter(colIdx, colSpec), colIdx);
                    } else {
                        arrange.replace(createNumberConverter(colIdx, type, colSpec), colIdx);
                    }

                }
                progress++;
                exec.setProgress(progress / max);
                exec.checkCanceled();
            }

            BufferedDataTable outTable = exec.createColumnRearrangeTable(data, arrange, exec);

            return new BufferedDataTable[]{outTable};
        } else {
            return inData;
        }
    }

    private SingleCellFactory
        createNumberConverter(final int colIdx, final DataType type, final DataColumnSpec colSpec) {
        return new SingleCellFactory(colSpec) {
            private final DataCellFactory m_fac = new DataCellFactory();

            @Override
            public DataCell getCell(final DataRow row) {

                DataCell cell = row.getCell(colIdx);
                if (!cell.isMissing()) {
                    String str = ((StringValue)cell).getStringValue();

                    // create String-, Int- or DoubleCell
                    return m_fac.createDataCellOfType(type, str);

                } else {
                    // create MissingCell
                    return DataType.getMissingCell();
                }
            }
        };
    }

    private SingleCellFactory createLongConverter(final int colIdx, final DataColumnSpec colSpec) {
        return new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {

                DataCell cell = row.getCell(colIdx);
                if (!cell.isMissing()) {
                    String str = ((StringValue)cell).getStringValue();
                    // create LongCell
                    return new LongCell(Long.parseLong(str));
                } else {
                    // create MissingCell
                    return DataType.getMissingCell();
                }
            }
        };
    }

    private SingleCellFactory createDateAndTimeConverter(final int colIdx, final DataColumnSpec colSpec) {
        return new SingleCellFactory(colSpec) {
            private final Calendar m_cal = Calendar.getInstance(TimeZone.getDefault());

            private final SimpleDateFormat m_format = new SimpleDateFormat(m_dateFormat);

            private final boolean m_hasDate;

            private final boolean m_hasTime;

            private final boolean m_hasMillis;

            {
                TimeZone timeZone = TimeZone.getTimeZone("UTC");
                m_format.setTimeZone(timeZone);
                m_cal.setTimeZone(timeZone);
                m_hasDate = m_dateFormat.contains("d");
                m_hasTime = m_dateFormat.contains("H");
                m_hasMillis = m_dateFormat.contains("S");
            }

            @Override
            public DataCell getCell(final DataRow row) {

                DataCell cell = row.getCell(colIdx);
                if (!cell.isMissing()) {
                    String str = ((StringValue)cell).getStringValue();

                    try {
                        m_cal.setTime(m_format.parse(str));
                        return new DateAndTimeCell(m_cal.getTimeInMillis(), m_hasDate, m_hasTime, m_hasMillis);
                    } catch (ParseException e) {
                        getLogger()
                            .warn("Date format parsing error in row: " + row.getKey() + ", column: " + colIdx, e);
                        return new MissingCell(e.getMessage());
                    }

                } else {
                    // create MissingCell
                    return DataType.getMissingCell();
                }
            }
        };
    }

    /**
     * @param curType currently stored DataType
     * @param newType possible new DataType
     */
    private DataType setType(final DataType curType, final DataType newType) {
        // if one of the types represents the missing cell type, we
        // return the other type.
        if (curType.equals(DataType.getMissingCell().getType())) {
            return newType;
        }
        if (newType.equals(DataType.getMissingCell().getType())) {
            return curType;
        }

        // handles also the equals case
        if (curType.isASuperTypeOf(newType)) {
            return curType;
        }
        if (newType.isASuperTypeOf(curType)) {
            return newType;
        }

        // if both are not super type, return default StringCell
        return StringCell.TYPE;

    }

    /**
     * try to parse all different numeric types and the date type based on the given format-pattern.
     *
     * @param cell
     * @return new DataType if string could be parsed else return old DataType
     */
    private DataType typeGuesser(final DataCell cell) {
        SimpleDateFormat date = new SimpleDateFormat(m_dateFormat);
        if (!cell.isMissing()) {
            String str = cell.toString();

            try {
                Integer.parseInt(str);
                return IntCell.TYPE;
            } catch (NumberFormatException eInt) {
                try {
                    Long.parseLong(str);
                    return LongCell.TYPE;
                } catch (NumberFormatException eLong) {
                    try {
                        Double.parseDouble(str);
                        return DoubleCell.TYPE;

                    } catch (NumberFormatException e) {
                        try {
                            date.parse(str);
                            return DateAndTimeCell.TYPE;
                        } catch (ParseException e1) {
                            return cell.getType();
                        }
                    }
                }
            }
        }
        return cell.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        if (m_conf == null) {
            m_conf = createDCSFilterConfiguration();
            // auto-configure
            m_conf.loadDefaults(inSpecs[0], true);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    /**
     * @param settings NodeSettings
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
     * @throws InvalidSettingsException invalid settings exception
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createDCSFilterConfiguration();
        conf.loadConfigurationInModel(settings);
        m_conf = conf;
        m_dateFormat = settings.getString("dateFormat");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_conf != null) {
            m_conf.saveConfiguration(settings);
        }
        settings.addString("dateFormat", m_dateFormat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createDCSFilterConfiguration();
        conf.loadConfigurationInModel(settings);
        String tmpDateFormat = settings.getString("dateFormat");
        try {
            new SimpleDateFormat(tmpDateFormat);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }

    /**
     * A new configuration to store the settings. Only Columns of Type String are available.
     *
     * @return ...
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column-filter", new InputFilter<DataColumnSpec>() {

            @Override
            public boolean include(final DataColumnSpec name) {
                return name.getType().equals(StringCell.TYPE);
            }
        }, NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
    }
}
