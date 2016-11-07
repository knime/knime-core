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
 *   Oct 21, 2016 (simon): created
 */
package org.knime.time.node.convert.datetimetostring;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.LocaleUtils;
import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
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
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.UniqueNameGenerator;

/**
 * The node model of the node which converts the new date&time types to strings.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class DateTimeToStringNodeModel extends NodeModel {

    static final String FORMAT_HISTORY_KEY = "string_to_date_formats";

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_format = createFormatModel();

    private final SettingsModelString m_locale = createLocaleModel();

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", StringValue.class);
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
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(String)");
        replaceOrAppendModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)) {
                    suffixModel.setEnabled(true);
                } else {
                    suffixModel.setEnabled(false);
                }
            }
        });

        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createFormatModel() {
        return new SettingsModelString("date_format", "yyyy-MM-dd;HH:mm:ss.S");
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createCancelOnFailModel() {
        return new SettingsModelBoolean("cancel_on_fail", true);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createLocaleModel() {
        return new SettingsModelString("locale", Locale.getDefault().toString());
    }

    /**
     * @return a set of all predefined formats plus the formats added by the user
     */
    static Collection<String> createPredefinedFormats() {
        // unique values
        Set<String> formats = new LinkedHashSet<String>();
        formats.add("yyyy-MM-dd;HH:mm:ss.S");
        formats.add("dd.MM.yyyy;HH:mm:ss.S");
        formats.add("yyyy-MM-dd HH:mm:ss.S");
        formats.add("dd.MM.yyyy HH:mm:ss.S");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS");
        formats.add("yyyy-MM-dd;HH:mm:ssVV");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV'['zzzz']'");
        formats.add("yyyy/dd/MM");
        formats.add("dd.MM.yyyy");
        formats.add("yyyy-MM-dd");
        formats.add("HH:mm:ss");
        // check also the StringHistory....
        final String[] userFormats = StringHistory.getInstance(FORMAT_HISTORY_KEY).getHistory();
        for (String userFormat : userFormats) {
            formats.add(userFormat);
        }
        return formats;
    }

    /**
     */
    protected DateTimeToStringNodeModel() {
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
        final ColumnRearranger columnRearranger = createColumnRearranger(inData[0].getDataTableSpec());
        final BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], columnRearranger, exec);
        return new BufferedDataTable[]{out};
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
                    new DataColumnSpecCreator(includedCol, StringCell.TYPE);
                final TimeToStringCellFactory cellFac =
                    new TimeToStringCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i++]);
                rearranger.replace(cellFac, includedCol);
            } else {
                final DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(inSpec).newColumn(includedCol + m_suffix.getStringValue(), StringCell.TYPE);
                final TimeToStringCellFactory cellFac = new TimeToStringCellFactory(dataColSpec, includeIndeces[i++]);
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
                            final DataColumnSpecCreator dataColumnSpecCreator =
                                new DataColumnSpecCreator(includeList[i], StringCell.TYPE);
                            final TimeToStringCellFactory cellFac =
                                new TimeToStringCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i]);
                            datacells[i] = cellFac.getCell(row);
                        } else {
                            final DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec)
                                .newColumn(includeList[i] + m_suffix.getStringValue(), StringCell.TYPE);
                            final TimeToStringCellFactory cellFac =
                                new TimeToStringCellFactory(dataColSpec, includeIndeces[i]);
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
        m_format.saveSettingsTo(settings);
        m_locale.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_format.validateSettings(settings);
        m_locale.validateSettings(settings);

        try {
            LocaleUtils.toLocale(m_locale.getStringValue());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(
                "Unsupported locale in setting (" + m_locale.getStringValue() + "): " + ex.getMessage(), ex);
        }

        final SettingsModelString formatClone = m_format.createCloneWithValidatedValue(settings);
        final String format = formatClone.getStringValue();
        if (format == null || format.length() == 0) {
            throw new InvalidSettingsException("Format must not be empty!");
        }
        try {
            DateTimeFormatter.ofPattern(formatClone.getStringValue());
        } catch (Exception e) {
            String msg = "Invalid date format: \"" + format + "\".";
            final String errMsg = e.getMessage();
            if (errMsg != null && !errMsg.isEmpty()) {
                msg += " Reason: " + errMsg;
            }
            throw new InvalidSettingsException(msg, e);
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
        m_format.loadSettingsFrom(settings);
        m_locale.loadSettingsFrom(settings);
        final String dateformat = m_format.getStringValue();
        // if it is not a predefined one -> store it
        if (!createPredefinedFormats().contains(dateformat)) {
            StringHistory.getInstance(FORMAT_HISTORY_KEY).add(dateformat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    final class TimeToStringCellFactory extends SingleCellFactory {

        private final int m_colIndex;

        /**
         * @param inSpec spec of the column after computation
         * @param colIndex index of the column to work on
         */
        public TimeToStringCellFactory(final DataColumnSpec inSpec, final int colIndex) {
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
            try {
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(m_format.getStringValue(),
                    LocaleUtils.toLocale(m_locale.getStringValue()));
                final DataType type = cell.getType();
                if (type.equals(LocalDateCellFactory.TYPE)) {
                    final String result = ((LocalDateCell)cell).getLocalDate().format(formatter);
                    return StringCellFactory.create(result);
                } else if (type.equals(LocalTimeCellFactory.TYPE)) {
                    final String result = ((LocalTimeCell)cell).getLocalTime().format(formatter);
                    return StringCellFactory.create(result);
                } else if (type.equals(LocalDateTimeCellFactory.TYPE)) {
                    final String result = ((LocalDateTimeCell)cell).getLocalDateTime().format(formatter);
                    return StringCellFactory.create(result);
                } else if (type.equals(ZonedDateTimeCellFactory.TYPE)) {
                    final String result = ((ZonedDateTimeCell)cell).getZonedDateTime().format(formatter);
                    return StringCellFactory.create(result);
                }
            } catch (UnsupportedTemporalTypeException e) {
                return new MissingCell(e.getMessage());
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }
    }
}
