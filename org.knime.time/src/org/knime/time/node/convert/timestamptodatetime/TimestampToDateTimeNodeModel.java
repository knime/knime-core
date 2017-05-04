/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 3, 2017 (clemens): created
 */
package org.knime.time.node.convert.timestamptodatetime;

import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
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
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.node.convert.DateTimeTypes;

/**
 * The node model of the node which converts unix timestamps to the new date&time types.
 *
 * @author Clemens von Schwerin, KNIME.com, Konstanz, Germany
 */
final class TimestampToDateTimeNodeModel extends NodeModel {

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final TimeUnit[] TIMEUNITS = { TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS };

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private String m_selectedType = DateTimeTypes.LOCAL_DATE_TIME.name();

    private TimeUnit m_selectedUnit = TIMEUNITS[0];

    private final SettingsModelString m_timezone = createTimezoneModel();

    private boolean m_hasValidatedConfiguration = false;

    static InputFilter<DataColumnSpec> filter = new InputFilter<DataColumnSpec>() {
        @Override
        public boolean include(final DataColumnSpec spec) {
            return spec.getType().getPreferredValueClass() == LongValue.class || spec.getType().getPreferredValueClass() == IntValue.class;
        }
    };

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColSelectModel() {

        return new SettingsModelColumnFilter2("col_select", filter, 0);
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
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(Date&Time)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createTimezoneModel()
    {
        return new SettingsModelString("timezone", "UTC");
    }

    /**
     * Chech if timezonestr is a valid timezone descriptor.
     * @param timezonestr - the string to check
     * @return true - valid timezone descriptor, false - otherwise
     */
    static boolean validateTimezone(final String timezonestr)
    {
        try {
            ZoneId.of(timezonestr);
            return true;
        } catch (DateTimeException e)
        {
            return false;
        }
    }


    /**
     * Sets the column selections to only include columns having an applicable DataType (Long or Integer).
     *
     * @param tableSpec the corresponding spec
     */
    private void setDefaultColumnSelection(final DataTableSpec tableSpec) {
        m_colSelect.loadDefaults(tableSpec, filter, true);
    }

    /**
     * one in, one out
     */
    protected TimestampToDateTimeNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (!m_hasValidatedConfiguration) {
            setDefaultColumnSelection(inSpecs[0]);
            throw new InvalidSettingsException("Node must be configured!");
        }
        final ColumnRearranger columnRearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{columnRearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final ColumnRearranger columnRearranger = createColumnRearranger(inData[0].getDataTableSpec());
        final BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], columnRearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * @param inSpec table input spec
     * @return the CR describing the output
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        final int[] includeIndeces =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
        int i = 0;
        final boolean isReplace = m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE);
        for (String includedCol : includeList) {
            if (isReplace) {
                final DataColumnSpecCreator dataColumnSpecCreator =
                    new DataColumnSpecCreator(includedCol, DateTimeTypes.valueOf(m_selectedType).getDataType());
                final TimestampToTimeCellFactory cellFac =
                    new TimestampToTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i++]);
                rearranger.replace(cellFac, includedCol);
            } else {
                final DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec).newColumn(
                    includedCol + m_suffix.getStringValue(), DateTimeTypes.valueOf(m_selectedType).getDataType());
                final TimestampToTimeCellFactory cellFac = new TimestampToTimeCellFactory(dataColSpec, includeIndeces[i++]);
                rearranger.append(cellFac);
            }
        }
        return rearranger;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];
                final DataTableSpec inSpec = in.getDataTableSpec();
                final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
                final int[] includeIndeces = Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes())
                    .mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
                final boolean isReplace = m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE);

                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    DataCell[] datacells = new DataCell[includeIndeces.length];
                    for (int i = 0; i < includeIndeces.length; i++) {
                        if (isReplace) {
                            final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(
                                includeList[i], DateTimeTypes.valueOf(m_selectedType).getDataType());
                            final TimestampToTimeCellFactory cellFac =
                                new TimestampToTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i]);
                            datacells[i] = cellFac.getCell(row);
                        } else {
                            final DataColumnSpec dataColSpec =
                                new UniqueNameGenerator(inSpec).newColumn(includeList[i] + m_suffix.getStringValue(),
                                    DateTimeTypes.valueOf(m_selectedType).getDataType());
                            final TimestampToTimeCellFactory cellFac =
                                new TimestampToTimeCellFactory(dataColSpec, includeIndeces[i]);
                            datacells[i] = cellFac.getCell(row);
                        }
                    }
                    if (isReplace) {
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
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        settings.addString("typeEnum", m_selectedType);
        settings.addString("unitEnum", m_selectedUnit.toString());
        m_timezone.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        settings.getString("typeEnum");
        settings.getString("unitEnum");
        m_timezone.validateSettings(settings);
        final SettingsModelString timezoneClone = m_timezone.createCloneWithValidatedValue(settings);
        final String timezonestr = timezoneClone.getStringValue();
        DateTimeTypes type = DateTimeTypes.valueOf(settings.getString("typeEnum"));
        if(type == DateTimeTypes.ZONED_DATE_TIME && !validateTimezone(timezonestr))
        {
            String msg = "Invalid timezone: " + m_timezone.getStringValue() + ".";
            throw new InvalidSettingsException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_selectedType = settings.getString("typeEnum");
        m_selectedUnit = TimeUnit.valueOf(settings.getString("unitEnum"));
        m_timezone.loadSettingsFrom(settings);
        m_hasValidatedConfiguration = true;
    }

    /**
     * This cell factory converts a single Int or Long cell to a Date&Time cell.
     */
    final class TimestampToTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        /**
         * @param inSpec spec of the column after computation
         * @param colIndex index of the column to work on
         */
        public TimestampToTimeCellFactory(final DataColumnSpec inSpec, final int colIndex) {
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
            final long input;
            //Interpret as LongValue (also works for Int columns)
            input = ((LongValue)cell).getLongValue();

            Instant instant;
            if(m_selectedUnit == TimeUnit.MILLISECONDS) {
                instant = Instant.ofEpochSecond(input / 1000L, (input % 1000L) * (1000L * 1000L));
            } else if(m_selectedUnit == TimeUnit.MICROSECONDS) {
                instant = Instant.ofEpochSecond(input / (1000L * 1000L), (input % (1000L * 1000L)) * 1000L);
            } else if(m_selectedUnit == TimeUnit.NANOSECONDS) {
                instant = Instant.ofEpochSecond(input / (1000L * 1000L * 1000L), input % (1000L * 1000L * 1000L));
            } else {
                throw new IllegalStateException("Unknown unit " + m_selectedUnit);
            }

            switch (DateTimeTypes.valueOf(m_selectedType)) {
                case LOCAL_DATE: {
                    final LocalDate ld = LocalDate.from(instant.atZone(ZoneId.of("UTC")));
                    return LocalDateCellFactory.create(ld);
                }
                case LOCAL_TIME: {
                    final LocalTime lt = LocalTime.from(instant.atZone(ZoneId.of("UTC")));
                    return LocalTimeCellFactory.create(lt);
                }
                case LOCAL_DATE_TIME: {
                    final LocalDateTime ldt = LocalDateTime.from(instant.atZone(ZoneId.of("UTC")));
                    return LocalDateTimeCellFactory.create(ldt);
                }
                case ZONED_DATE_TIME: {
                    try {
                        final ZoneId zone = ZoneId.of(m_timezone.getStringValue());
                        final ZonedDateTime zdt = ZonedDateTime.from(instant.atZone(zone));
                        return ZonedDateTimeCellFactory.create(zdt);
                    } catch(DateTimeException e) {
                        throw new IllegalArgumentException("Invalid timezone format: " + m_timezone.getStringValue());
                    }
                }
                default:
                    throw new IllegalStateException("Unhandled date&time type: " + m_selectedType);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }
}
