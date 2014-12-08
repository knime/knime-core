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
 *   14.04.2014 (koetter): created
 */
package org.knime.timeseries.node.movagg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.dialogutil.pattern.PatternAggregator;
import org.knime.base.data.aggregation.dialogutil.type.DataTypeAggregator;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.GroupByNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *  Node model of the Moving Aggregation node.
 *
 *  @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 *  @since 2.10
 */
public class MovingAggregationNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MovingAggregationNodeModel.class);

    /**Configuration key for the data type based aggregation methods.*/
    static final String CFG_DATA_TYPE_AGGREGATORS = "dataTypeAggregators";

    /**Configuration key for the pattern based aggregation methods.*/
    static final String CFG_PATTERN_AGGREGATORS = "patternAggregators";

    private final SettingsModelInteger m_winLength = createWindowLengthModel();

    private final SettingsModelBoolean m_handleMissings = createHandleMissingsModel();

    private final SettingsModelInteger m_maxUniqueVals = createMaxUniqueValModel();

    private final SettingsModelString m_valueDelimiter = createValueDelimiterModel();

    private final SettingsModelBoolean m_removeRetainedCols = createRemoveRetainedColsModel();

    private final SettingsModelBoolean m_removeAggregationCols = createRemoveAggregationColsModel();

    private final SettingsModelString m_columnNamePolicy = createColNamePolicyModel();

    private final SettingsModelBoolean m_cumulativeComputing = createCumulativeComputingModel();

    private final SettingsModelString m_windowType = createWindowTypeModel();

    private final List<ColumnAggregator> m_columnAggregators = new LinkedList<>();

    private final Collection<DataTypeAggregator> m_dataTypeAggregators = new LinkedList<>();

    private final Collection<PatternAggregator> m_patternAggregators = new LinkedList<>();

    private List<ColumnAggregator> m_columnAggregators2Use = new LinkedList<>();


    /**
     * Constructor.
     */
    public MovingAggregationNodeModel() {
        super(1, 1);
    }

    /**
     * @return the {@link WindowType} model
     */
    static SettingsModelString createWindowTypeModel() {
        return new SettingsModelString("windowType", WindowType.getDefault().getActionCommand());
    }

    /**
     * @return the perform cumulative computing model
     */
    static SettingsModelBoolean createCumulativeComputingModel() {
        return new SettingsModelBoolean("cumulativeComputing", false);
    }

    /**
     * @return the default value delimiter model
     */
    static SettingsModelString createValueDelimiterModel() {
        return new SettingsModelString("valueDelimiter", GlobalSettings.STANDARD_DELIMITER);
    }

    /**
     *
     * @return the model for the window length
     */
    static SettingsModelIntegerBounded createWindowLengthModel() {
        return new SettingsModelIntegerBounded("windowLength", 21, 2, Integer.MAX_VALUE);
    }

    /**
     * @return the model for handling missing values
     */
    static SettingsModelBoolean createHandleMissingsModel() {
        return new SettingsModelBoolean("handleMissings", false);
    }

    /**
     * @return the remove aggregation column model
     */
    static SettingsModelBoolean createRemoveAggregationColsModel() {
        return new SettingsModelBoolean("removeAggregationColumns", false);
    }

    /**
     * @return the remove aggregation column model
     */
    static SettingsModelBoolean createRemoveRetainedColsModel() {
        return new SettingsModelBoolean("removeRetainedColumns", false);
    }

    /**
     * @return the maximum unique value model
     */
    static SettingsModelInteger createMaxUniqueValModel() {
        return new SettingsModelIntegerBounded("maxUniqueVals", 10000, 1, Integer.MAX_VALUE);
    }

    /**
     * @return aggregation column name policy
     */
    static SettingsModelString createColNamePolicyModel() {
        return new SettingsModelString("columnNamePolicy", ColumnNamePolicy.getDefault().getLabel());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs == null || inSpecs.length != 1)  {
            throw new InvalidSettingsException("No input table specification available");
        }
        final DataTableSpec inputSpec = inSpecs[0];
        m_columnAggregators2Use.clear();
        final ArrayList<ColumnAggregator> invalidColAggrs = new ArrayList<>(1);
        m_columnAggregators2Use.addAll(GroupByNodeModel.getAggregators(inputSpec, Collections.EMPTY_LIST,
            m_columnAggregators, m_patternAggregators, m_dataTypeAggregators, invalidColAggrs));
        if (m_columnAggregators2Use.isEmpty()) {
            setWarningMessage("No aggregation column defined");
        }
        if (!invalidColAggrs.isEmpty()) {
            setWarningMessage(invalidColAggrs.size() + " invalid aggregation column(s) found.");
        }
        LOGGER.debug(m_columnAggregators2Use);
        final MovingAggregationTableFactory tableFactory =
                createTableFactory(FileStoreFactory.createNotInWorkflowFileStoreFactory(), inputSpec);
        return new DataTableSpec[] {tableFactory.createResultSpec()};
    }

    private MovingAggregationTableFactory createTableFactory(final FileStoreFactory fsf, final DataTableSpec spec) {
        final GlobalSettings globalSettings = new GlobalSettings(fsf, Collections.<String> emptyList(),
            m_maxUniqueVals.getIntValue(), m_valueDelimiter.getJavaUnescapedStringValue(), spec, 0);
        final ColumnNamePolicy colNamePolicy = ColumnNamePolicy.getPolicy4Label(m_columnNamePolicy.getStringValue());
        final MovingAggregationTableFactory tableFactory =  new MovingAggregationTableFactory(spec, globalSettings,
            colNamePolicy, m_columnAggregators2Use, m_cumulativeComputing.getBooleanValue(),
            WindowType.getType(m_windowType.getStringValue()), m_winLength.getIntValue(),
            m_handleMissings.getBooleanValue(), m_removeAggregationCols.getBooleanValue(),
            m_removeRetainedCols.getBooleanValue());
        return tableFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        if (inData == null || inData.length != 1) {
            throw new InvalidSettingsException("No input table available");
        }
        final BufferedDataTable table = inData[0];
        if (table.getRowCount() == 0) {
            setWarningMessage("Empty input table found");
        } else if (!m_cumulativeComputing.getBooleanValue() && table.getRowCount() < m_winLength.getIntValue()) {
            throw new InvalidSettingsException(
                "Window length is larger than the number of rows of the input table");
        }
        final DataTableSpec spec = table.getDataTableSpec();
        final MovingAggregationTableFactory tableFactory =
                createTableFactory(FileStoreFactory.createWorkflowFileStoreFactory(exec), spec);
        BufferedDataTable resultTable = tableFactory.createTable(exec, table);
        return new BufferedDataTable[] {resultTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_winLength.saveSettingsTo(settings);
        m_windowType.saveSettingsTo(settings);
        m_cumulativeComputing.saveSettingsTo(settings);
        m_handleMissings.saveSettingsTo(settings);
        m_removeRetainedCols.saveSettingsTo(settings);
        m_removeAggregationCols.saveSettingsTo(settings);
        m_maxUniqueVals.saveSettingsTo(settings);
        m_valueDelimiter.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        ColumnAggregator.saveColumnAggregators(settings, m_columnAggregators);
        PatternAggregator.saveAggregators(settings, CFG_PATTERN_AGGREGATORS, m_patternAggregators);
        DataTypeAggregator.saveAggregators(settings, CFG_DATA_TYPE_AGGREGATORS, m_dataTypeAggregators);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_winLength.validateSettings(settings);
        m_windowType.validateSettings(settings);
        m_cumulativeComputing.validateSettings(settings);
        m_handleMissings.validateSettings(settings);
        m_removeRetainedCols.validateSettings(settings);
        m_maxUniqueVals.validateSettings(settings);
        m_valueDelimiter.validateSettings(settings);
        final List<ColumnAggregator> aggregators = ColumnAggregator.loadColumnAggregators(settings);
        final List<DataTypeAggregator> typeAggregators = new LinkedList<>();
        final List<PatternAggregator> regexAggregators = new LinkedList<>();
        try {
            regexAggregators.addAll(PatternAggregator.loadAggregators(settings, CFG_PATTERN_AGGREGATORS));
            typeAggregators.addAll(DataTypeAggregator.loadAggregators(settings, CFG_DATA_TYPE_AGGREGATORS));
        } catch (InvalidSettingsException e) {
            // introduced in 2.11
        }
        if (aggregators.isEmpty() && regexAggregators.isEmpty() && typeAggregators.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one aggregation option");
        }
        final String policyLabel =
                ((SettingsModelString) m_columnNamePolicy.createCloneWithValidatedValue(settings)).getStringValue();
        final ColumnNamePolicy namePolicy = ColumnNamePolicy.getPolicy4Label(policyLabel);
        try {
            GroupByNodeModel.checkDuplicateAggregators(namePolicy, aggregators);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
        final boolean removeAggrCols = ((SettingsModelBoolean)
                m_removeAggregationCols.createCloneWithValidatedValue(settings)).getBooleanValue();
        if (ColumnNamePolicy.KEEP_ORIGINAL_NAME.equals(namePolicy) && !removeAggrCols) {
            throw new InvalidSettingsException("'" + ColumnNamePolicy.KEEP_ORIGINAL_NAME.getLabel()
                + "' option only valid if aggregation columns are filtered");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_winLength.loadSettingsFrom(settings);
        m_windowType.loadSettingsFrom(settings);
        m_cumulativeComputing.loadSettingsFrom(settings);
        m_handleMissings.loadSettingsFrom(settings);
        m_removeRetainedCols.loadSettingsFrom(settings);
        m_removeAggregationCols.loadSettingsFrom(settings);
        m_maxUniqueVals.loadSettingsFrom(settings);
        m_valueDelimiter.loadSettingsFrom(settings);
        m_columnNamePolicy.loadSettingsFrom(settings);
        m_columnAggregators.clear();
        m_columnAggregators.addAll(ColumnAggregator.loadColumnAggregators(settings));
        m_patternAggregators.clear();
        m_dataTypeAggregators.clear();
        try {
            m_patternAggregators.addAll(PatternAggregator.loadAggregators(settings, CFG_PATTERN_AGGREGATORS));
            m_dataTypeAggregators.addAll(DataTypeAggregator.loadAggregators(settings, CFG_DATA_TYPE_AGGREGATORS));
        } catch (final InvalidSettingsException e) {
            // introduced in KNIME 2.11
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        for (final ColumnAggregator colAggr : m_columnAggregators) {
            colAggr.reset();
        }
        m_columnAggregators2Use.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //nothing to do
    }
}
