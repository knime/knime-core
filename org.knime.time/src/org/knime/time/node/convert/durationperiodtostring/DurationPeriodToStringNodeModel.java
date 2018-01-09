/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Feb 6, 2017 (simon): created
 */
package org.knime.time.node.convert.durationperiodtostring;

import java.time.Duration;
import java.time.Period;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.util.DurationPeriodFormatUtils;

/**
 * The node model of the node which converts period or duration cells to string cells.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DurationPeriodToStringNodeModel extends SimpleStreamableFunctionNodeModel {

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final String FORMAT_ISO = "ISO-8601 representation";

    static final String FORMAT_SHORT = "Short representation";

    static final String FORMAT_LONG = "Long representation";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_format = createFormatModel();

    private boolean m_hasValidatedConfiguration = false;

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", DurationValue.class, PeriodValue.class);
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
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(String)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createFormatModel() {
        return new SettingsModelString("format", FORMAT_ISO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        if (!m_hasValidatedConfiguration) {
            m_colSelect.loadDefaults(spec);
        }
        final ColumnRearranger rearranger = new ColumnRearranger(spec);
        final String[] includeList = m_colSelect.applyTo(spec).getIncludes();
        final int[] includeIndices =
            Arrays.stream(m_colSelect.applyTo(spec).getIncludes()).mapToInt(s -> spec.findColumnIndex(s)).toArray();
        int i = 0;

        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(includedCol, StringCell.TYPE);
                DurationPeriodToStringCellFactory cellFac =
                    new DurationPeriodToStringCellFactory(dataColumnSpecCreator.createSpec(), includeIndices[i++]);
                rearranger.replace(cellFac, includedCol);
            } else {
                DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(spec).newColumn(includedCol + m_suffix.getStringValue(), StringCell.TYPE);
                DurationPeriodToStringCellFactory cellFac =
                    new DurationPeriodToStringCellFactory(dataColSpec, includeIndices[i++]);
                rearranger.append(cellFac);
            }
        }
        return rearranger;
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
        m_hasValidatedConfiguration = true;
    }

    private final class DurationPeriodToStringCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        public DurationPeriodToStringCellFactory(final DataColumnSpec newColSpec, final int colIndex) {
            super(newColSpec);
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
            final String format = m_format.getStringValue();
            if (cell instanceof DurationValue) {
                final Duration duration = ((DurationValue)cell).getDuration();
                if (format.equals(FORMAT_ISO)) {
                    return StringCellFactory.create(duration.toString());
                }
                if (format.equals(FORMAT_LONG)) {
                    return StringCellFactory.create(DurationPeriodFormatUtils.formatDurationLong(duration));
                }
                if (format.equals(FORMAT_SHORT)) {
                    return StringCellFactory.create(DurationPeriodFormatUtils.formatDurationShort(duration));
                }
                throw new IllegalStateException("Unexpected format: " + format);
            }
            if (cell instanceof PeriodValue) {
                final Period period = ((PeriodValue)cell).getPeriod();
                if (format.equals(FORMAT_ISO)) {
                    return StringCellFactory.create(period.toString());
                }
                if (format.equals(FORMAT_LONG)) {
                    return StringCellFactory.create(DurationPeriodFormatUtils.formatPeriodLong(period));
                }
                if (format.equals(FORMAT_SHORT)) {
                    return StringCellFactory.create(DurationPeriodFormatUtils.formatPeriodShort(period));
                }
                throw new IllegalStateException("Unexpected format: " + format);
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }
    }
}
