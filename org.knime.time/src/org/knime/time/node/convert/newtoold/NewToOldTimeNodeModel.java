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
 * -------------------------------------------------------------------
 *
 */
package org.knime.time.node.convert.newtoold;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.util.UniqueNameGenerator;

/**
 * The {@link NodeModel} implementation of the node which converts new to old date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class NewToOldTimeNodeModel extends NodeModel {

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final String TIME_ZONE_OPT1 = "Add the offset of the time zone to the time";

    static final String TIME_ZONE_OPT2 = "Drop time zone information";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_timeZoneSelect = createStringModel();

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", LocalDateTimeValue.class, ZonedDateTimeValue.class,
            LocalDateValue.class, LocalTimeValue.class);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createReplaceAppendStringBool() {
        return new SettingsModelString("replace_or_append", OPTION_REPLACE);
    }

    /**
     * @param replaceOrAppendModel model for the replace/append button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createSuffixModel(final SettingsModelString replaceOrAppendModel) {
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(old Date&Time)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createStringModel() {
        return new SettingsModelString("time_zone_select", TIME_ZONE_OPT1);
    }

    /** One in, one out. */
    NewToOldTimeNodeModel() {
        super(1, 1);
    }

    /**
     * @param inSpec Current input spec
     * @return The CR describing the output
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        final int[] includeIndeces =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();

        int i = 0;
        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                final DataColumnSpecCreator dataColumnSpecCreator =
                    new DataColumnSpecCreator(includedCol, DateAndTimeCell.TYPE);
                final ConvertTimeCellFactory cellFac =
                    new ConvertTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i++]);
                rearranger.replace(cellFac, includedCol);
            } else {
                final DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec)
                    .newColumn(includedCol + m_suffix.getStringValue(), DateAndTimeCell.TYPE);
                final ConvertTimeCellFactory cellFac = new ConvertTimeCellFactory(dataColSpec, includeIndeces[i++]);
                rearranger.append(cellFac);
            }
        }
        return rearranger;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
        throws Exception {
        final ColumnRearranger columnRearranger = createColumnRearranger(inObjects[0].getDataTableSpec());
        final BufferedDataTable out = exec.createColumnRearrangeTable(inObjects[0], columnRearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public StreamableOperatorInternals saveInternals() {
                return null;
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];

                final DataTableSpec inSpec = in.getDataTableSpec();
                String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
                final int[] includeIndeces =
                    Arrays.stream(includeList).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();

                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    DataCell[] datacells = new DataCell[includeIndeces.length];
                    for (int i = 0; i < includeIndeces.length; i++) {
                        if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                            final DataColumnSpecCreator dataColumnSpecCreator =
                                new DataColumnSpecCreator(includeList[i], DateAndTimeCell.TYPE);
                            final ConvertTimeCellFactory cellFac =
                                new ConvertTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i]);
                            datacells[i] = cellFac.getCells(row)[0];
                        } else {
                            final DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec)
                                .newColumn(includeList[i] + m_suffix.getStringValue(), DateAndTimeCell.TYPE);
                            final ConvertTimeCellFactory cellFac =
                                new ConvertTimeCellFactory(dataColSpec, includeIndeces[i]);
                            datacells[i] = cellFac.getCells(row)[0];
                        }
                    }
                    if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                        out.push(new ReplacedColumnsDataRow(row, datacells, includeIndeces));
                    } else {
                        out.push(new AppendedColumnRow(row, datacells));
                    }
                }
                in.close();
                out.close();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final ColumnRearranger columnRearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{columnRearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_timeZoneSelect.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_timeZoneSelect.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_timeZoneSelect.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    private final class ConvertTimeCellFactory extends SingleCellFactory {

        private final int m_colIndex;

        /**
         * @param inSpec spec of the column after computation
         * @param colIndex index of the column to work on
         */
        public ConvertTimeCellFactory(final DataColumnSpec inSpec, final int colIndex) {
            super(inSpec);
            m_colIndex = colIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            if (cell instanceof LocalDateTimeValue) {
                final LocalDateTime ldt = ((LocalDateTimeCell)cell).getLocalDateTime();
                if (ldt.getNano() == 0) {
                    return new DateAndTimeCell(ldt.getYear(), ldt.getMonthValue() - 1, ldt.getDayOfMonth(),
                        ldt.getHour(), ldt.getMinute(), ldt.getSecond());
                } else {
                    return new DateAndTimeCell(ldt.getYear(), ldt.getMonthValue() - 1, ldt.getDayOfMonth(),
                        ldt.getHour(), ldt.getMinute(), ldt.getSecond(),
                        (int)TimeUnit.NANOSECONDS.toMillis(ldt.getNano()));
                }
            } else if (cell instanceof ZonedDateTimeValue) {
                LocalDateTime ldt = null;
                if (m_timeZoneSelect.getStringValue().equals(TIME_ZONE_OPT1)) {
                    final ZonedDateTime zdt = ((ZonedDateTimeCell)cell).getZonedDateTime();
                    final LocalDateTime ldtUTC = LocalDateTime.of(zdt.getYear(), zdt.getMonth(), zdt.getDayOfMonth(),
                        zdt.getHour(), zdt.getMinute(), zdt.getSecond(), zdt.getNano());
                    ldt = LocalDateTime.ofInstant(ldtUTC.toInstant(ZoneOffset.UTC), zdt.getZone());
                } else {
                    ldt = ((ZonedDateTimeCell)cell).getZonedDateTime().toLocalDateTime();
                }
                if (ldt.getNano() == 0) {
                    return new DateAndTimeCell(ldt.getYear(), ldt.getMonthValue() - 1, ldt.getDayOfMonth(),
                        ldt.getHour(), ldt.getMinute(), ldt.getSecond());
                } else {
                    return new DateAndTimeCell(ldt.getYear(), ldt.getMonthValue() - 1, ldt.getDayOfMonth(),
                        ldt.getHour(), ldt.getMinute(), ldt.getSecond(),
                        (int)TimeUnit.NANOSECONDS.toMillis(ldt.getNano()));
                }
            } else if (cell instanceof LocalTimeValue) {
                final LocalTime lt = ((LocalTimeCell)cell).getLocalTime();
                if (lt.getNano() == 0) {
                    return new DateAndTimeCell(lt.getHour(), lt.getMinute(), lt.getSecond(), -1);
                } else {
                    return new DateAndTimeCell(lt.getHour(), lt.getMinute(), lt.getSecond(),
                        (int)TimeUnit.NANOSECONDS.toMillis(lt.getNano()));
                }
            } else if (cell instanceof LocalDateValue) {
                final LocalDate ld = ((LocalDateCell)cell).getLocalDate();
                return new DateAndTimeCell(ld.getYear(), ld.getMonthValue() - 1, ld.getDayOfMonth());
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }
    }
}
