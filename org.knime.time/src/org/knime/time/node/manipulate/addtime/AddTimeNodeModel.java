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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.addtime;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
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
import org.knime.core.util.UniqueNameGenerator;

/**
 * The node model of the node which adds a time to a date cell.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
class AddTimeNodeModel extends org.knime.core.node.NodeModel {

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelInteger m_hour = createHourModel();

    private final SettingsModelInteger m_minute = createMinuteModel();

    private final SettingsModelInteger m_second = createSecondModel();

    private final SettingsModelInteger m_nano = createNanoModel();

    private final SettingsModelBoolean m_addZone = createZoneModelBool();

    private final SettingsModelString m_timeZone = createTimeZoneSelectModel(m_addZone);

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", LocalDateValue.class);
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createReplaceAppendStringBool() {
        return new SettingsModelString("replace_or_append", OPTION_REPLACE);
    }

    /**
     * @param replaceOrAppendModel model for the replace/append button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createSuffixModel(final SettingsModelString replaceOrAppendModel) {
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(with time)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the integer model, used in both dialog and model. */
    public static SettingsModelIntegerBounded createHourModel() {
        return new SettingsModelIntegerBounded("hour_int", LocalTime.now().getHour(), 0, 23);
    }

    /** @return the integer model, used in both dialog and model. */
    public static SettingsModelIntegerBounded createMinuteModel() {
        return new SettingsModelIntegerBounded("minute_int", LocalTime.now().getMinute(), 0, 59);
    }

    /** @return the integer model, used in both dialog and model. */
    public static SettingsModelIntegerBounded createSecondModel() {
        return new SettingsModelIntegerBounded("second_int", LocalTime.now().getSecond(), 0, 59);
    }

    /** @return the integer model, used in both dialog and model. */
    public static SettingsModelIntegerBounded createNanoModel() {
        return new SettingsModelIntegerBounded("nano_int", 0, 0, 999_999_999);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createZoneModelBool() {
        return new SettingsModelBoolean("zone_bool", false);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createTimeZoneSelectModel(final SettingsModelBoolean zoneModelBool) {
        final SettingsModelString zoneSelectModel =
            new SettingsModelString("time_zone_select", ZoneId.systemDefault().getId());
        zoneSelectModel.setEnabled(false);
        zoneModelBool.addChangeListener(e -> zoneSelectModel.setEnabled(zoneModelBool.getBooleanValue()));
        return zoneSelectModel;
    }

    /**
     * one in, one out
     */
    protected AddTimeNodeModel() {
        super(1, 1);
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
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        ColumnRearranger columnRearranger = createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], columnRearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * @param inSpec table input spec
     * @return the CR describing the output
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        int[] includeIndices =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
        int i = 0;
        DataType dataType;
        if (m_addZone.getBooleanValue()) {
            dataType = ZonedDateTimeCellFactory.TYPE;
        } else {
            dataType = LocalDateTimeCellFactory.TYPE;
        }

        ZoneId zone = ZoneId.of(m_timeZone.getStringValue());

        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(includedCol, dataType);
                AddTimeCellFactory cellFac =
                    new AddTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndices[i++], zone);
                rearranger.replace(cellFac, includedCol);
            } else {
                DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(inSpec).newColumn(includedCol + m_suffix.getStringValue(), dataType);
                AddTimeCellFactory cellFac = new AddTimeCellFactory(dataColSpec, includeIndices[i++], zone);
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
                String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
                int[] includeIndeces = Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes())
                    .mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
                boolean isReplace = m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE);
                DataType dataType;
                if (m_addZone.getBooleanValue()) {
                    dataType = ZonedDateTimeCellFactory.TYPE;
                } else {
                    dataType = LocalDateTimeCellFactory.TYPE;
                }

                ZoneId zone = ZoneId.of(m_timeZone.getStringValue());

                AddTimeCellFactory[] cellFacs = new AddTimeCellFactory[includeIndeces.length];
                if (isReplace) {
                    for (int i = 0; i < includeIndeces.length; i++) {
                        DataColumnSpecCreator dataColumnSpecCreator =
                            new DataColumnSpecCreator(includeList[i], dataType);
                        cellFacs[i] =
                            new AddTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i], zone);
                    }
                } else {
                    for (int i = 0; i < includeIndeces.length; i++) {
                        DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec)
                            .newColumn(includeList[i] + m_suffix.getStringValue(), dataType);
                        cellFacs[i] = new AddTimeCellFactory(dataColSpec, includeIndeces[i], zone);
                    }
                }

                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    DataCell[] datacells = new DataCell[includeIndeces.length];
                    for (int i = 0; i < includeIndeces.length; i++) {
                        if (isReplace) {
                            datacells[i] = cellFacs[i].getCell(row);
                        } else {
                            datacells[i] = cellFacs[i].getCell(row);
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
        m_hour.saveSettingsTo(settings);
        m_minute.saveSettingsTo(settings);
        m_second.saveSettingsTo(settings);
        m_nano.saveSettingsTo(settings);
        m_addZone.saveSettingsTo(settings);
        m_timeZone.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_hour.validateSettings(settings);
        m_minute.validateSettings(settings);
        m_second.validateSettings(settings);
        m_nano.validateSettings(settings);
        m_addZone.validateSettings(settings);
        m_timeZone.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_hour.loadSettingsFrom(settings);
        m_minute.loadSettingsFrom(settings);
        m_second.loadSettingsFrom(settings);
        m_nano.loadSettingsFrom(settings);
        m_addZone.loadSettingsFrom(settings);
        m_timeZone.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    private final class AddTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final ZoneId m_zone;

        AddTimeCellFactory(final DataColumnSpec inSpec, final int colIndex, final ZoneId zone) {
            super(inSpec);
            m_colIndex = colIndex;
            m_zone = zone;
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
            final LocalDateCell localDateCell = (LocalDateCell)cell;
            final LocalTime localTime = LocalTime.of(m_hour.getIntValue(), m_minute.getIntValue(),
                m_second.getIntValue(), m_nano.getIntValue());
            if (m_addZone.getBooleanValue()) {
                return ZonedDateTimeCellFactory
                    .create(ZonedDateTime.of(LocalDateTime.of(localDateCell.getLocalDate(), localTime), m_zone));
            } else {
                return LocalDateTimeCellFactory.create(LocalDateTime.of(localDateCell.getLocalDate(), localTime));
            }
        }
    }
}
