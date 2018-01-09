/*
 * ------------------------------------------------------------------------
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
 * Created on 2013.08.17. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.SimpleRuleDocument.SimpleRule;
import org.knime.base.node.rules.engine.pmml.PMMLRuleTranslator.Rule;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.pmml.PMMLModelType;
import org.knime.core.util.Pair;
import org.w3c.dom.Node;

/**
 * This is the model implementation of PMMLRuleSetPredictor. Applies the rules to the input table.
 *
 * @author Gabor Bakos
 */
public class PMMLRuleSetPredictorNodeModel extends NodeModel {
    private static final int MODEL_INDEX = 0;

    private static final int DATA_INDEX = 1;

    static final String CFGKEY_OUTPUT_COLUMN = "output column";

    static final String DEFAULT_OUTPUT_COLUMN = "prediction";

    static final String CFGKEY_CONFIDENCE_COLUMN = "confidence column";

    static final String DEFAULT_CONFIDENCE_COLUN = "confidence";

    static final String CFGKEY_ADD_CONFIDENCE = "add confidence";

    static final boolean DEFAULT_ADD_CONFIDENCE = false;

    static final String CFGKEY_REPLACE_COLUMN = "column to replace";

    static final String DEFAULT_REPLACE_COLUMN = "";

    static final String CFGKEY_DO_REPLACE_COLUMN = "replace?";

    static final boolean DEFAULT_DO_REPLACE_COLUMN = false;

    private SettingsModelString m_outputColumn = new SettingsModelString(CFGKEY_OUTPUT_COLUMN, DEFAULT_OUTPUT_COLUMN);

    private SettingsModelBoolean m_addConfidence =
        new SettingsModelBoolean(CFGKEY_ADD_CONFIDENCE, DEFAULT_ADD_CONFIDENCE);

    private SettingsModelString m_confidenceColumn =
        new SettingsModelString(CFGKEY_CONFIDENCE_COLUMN, DEFAULT_CONFIDENCE_COLUN);

    private final SettingsModelString m_replaceColumn = createReplaceColumn();

    /**
     * @return A new {@link SettingsModelString} for the replaced column name.
     */
    static SettingsModelString createReplaceColumn() {
        return new SettingsModelString(CFGKEY_REPLACE_COLUMN, DEFAULT_REPLACE_COLUMN);
    }

    private final SettingsModelBoolean m_doReplaceColumn = createDoReplace();

    /**
     * @return A new {@link SettingsModelBoolean} whether replace a column, or add a new one.
     */
    static SettingsModelBoolean createDoReplace() {
        return new SettingsModelBoolean(CFGKEY_DO_REPLACE_COLUMN, DEFAULT_DO_REPLACE_COLUMN);
    }

    /**
     * Constructor for the node model.
     */
    protected PMMLRuleSetPredictorNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        PMMLPortObject obj = (PMMLPortObject)inData[MODEL_INDEX];
        BufferedDataTable data = (BufferedDataTable)inData[DATA_INDEX];
        ColumnRearranger rearranger = createColumnRearranger(obj, data.getSpec(), exec);
        BufferedDataTable table = exec.createColumnRearrangeTable(data, rearranger, exec);
        if (m_doReplaceColumn.getBooleanValue()) {
            DataTableSpec preSpec = table.getSpec();
            DataColumnSpec[] columns = new DataColumnSpec[preSpec.getNumColumns()];
            for (int i = columns.length; i-- > 0;) {
                columns[i] = preSpec.getColumnSpec(i);
            }
            int columnIndex = data.getSpec().findColumnIndex(m_replaceColumn.getStringValue());
            if (m_addConfidence.getBooleanValue()) {
                ColumnRearranger mover = new ColumnRearranger(table.getSpec());
                //Move confidence to the end
                mover.move(columnIndex, table.getSpec().getNumColumns());
                //Move the result to its place
                mover.move(table.getSpec().getNumColumns() - 2, columnIndex);
                table = exec.createColumnRearrangeTable(table, mover, exec);
            } else {
                DataColumnSpecCreator creator = new DataColumnSpecCreator(columns[columnIndex]);
                creator.setName(m_replaceColumn.getStringValue());
                columns[columnIndex] = creator.createSpec();
                DataTableSpec newSpec = new DataTableSpec(columns);
                table = exec.createSpecReplacerTable(table, newSpec);
            }
        }
        return new BufferedDataTable[]{table};
    }

    /**
     * Constructs the {@link ColumnRearranger}.
     *
     * @param obj The {@link PMMLPortObject} of the preprocessing model.
     * @param spec The {@link DataTableSpec} of the table.
     * @param exec The execution monitor to display messages.
     * @return The {@link ColumnRearranger} computing the result.
     * @throws InvalidSettingsException Problem with rules.
     */
    private ColumnRearranger createColumnRearranger(final PMMLPortObject obj, final DataTableSpec spec,
        final ExecutionMonitor exec) throws InvalidSettingsException {
        exec.setMessage("Loading model.");
        return createRearranger(obj, spec, m_doReplaceColumn.getBooleanValue(),
            m_doReplaceColumn.getBooleanValue() ? m_replaceColumn.getStringValue()
                : DataTableSpec.getUniqueColumnName(spec, m_outputColumn.getStringValue()),
            m_addConfidence.getBooleanValue(), m_confidenceColumn.getStringValue(),
            /*no validation column*/-1, /* no statistics computed, so concurrent processing is allowed*/
            true);
    }

    /**
     * Constructs the {@link ColumnRearranger} for computing the new columns.
     *
     * @param obj The {@link PMMLPortObject} of the preprocessing model.
     * @param spec The {@link DataTableSpec} of the table.
     * @param replaceColumn Should replace the {@code outputColumnName}?
     * @param outputColumnName The output column name (which might be an existing).
     * @param addConfidence Should add the confidence values to a column?
     * @param confidenceColumnName The name of the confidence column.
     * @param validationColumnIdx Index of the validation column, {@code -1} if not specified.
     * @return The {@link ColumnRearranger} computing the result.
     * @throws InvalidSettingsException Problem with rules.
     * @since 2.12
     * @noreference This method is not intended to be referenced by clients.
     */
    public static ColumnRearranger createRearranger(final PMMLPortObject obj, final DataTableSpec spec,
        final boolean replaceColumn, final String outputColumnName, final boolean addConfidence,
        final String confidenceColumnName, final int validationColumnIdx) throws InvalidSettingsException {
        return createRearranger(obj, spec, replaceColumn, outputColumnName, addConfidence, confidenceColumnName,
            validationColumnIdx, /*might compute statistics*/false);
    }

    /**
     * Constructs the {@link ColumnRearranger} for computing the new columns.
     *
     * @param obj The {@link PMMLPortObject} of the preprocessing model.
     * @param spec The {@link DataTableSpec} of the table.
     * @param replaceColumn Should replace the {@code outputColumnName}?
     * @param outputColumnName The output column name (which might be an existing).
     * @param addConfidence Should add the confidence values to a column?
     * @param confidenceColumnName The name of the confidence column.
     * @param validationColumnIdx Index of the validation column, {@code -1} if not specified.
     * @param processConcurrently Should be {@code false} when the statistics are to be computed.
     * @return The {@link ColumnRearranger} computing the result.
     * @throws InvalidSettingsException Problem with rules.
     */
    private static ColumnRearranger createRearranger(final PMMLPortObject obj, final DataTableSpec spec,
        final boolean replaceColumn, final String outputColumnName, final boolean addConfidence,
        final String confidenceColumnName, final int validationColumnIdx, final boolean processConcurrently)
        throws InvalidSettingsException {
        List<Node> models = obj.getPMMLValue().getModels(PMMLModelType.RuleSetModel);
        if (models.size() != 1) {
            throw new InvalidSettingsException("Expected exactly on RuleSetModel, but got: " + models.size());
        }
        final PMMLRuleTranslator translator = new PMMLRuleTranslator();
        obj.initializeModelTranslator(translator);
        if (!translator.isScorable()) {
            throw new UnsupportedOperationException("The model is not scorable.");
        }
        final List<PMMLRuleTranslator.Rule> rules = translator.getRules();
        ColumnRearranger ret = new ColumnRearranger(spec);
        final List<DataColumnSpec> targetCols = obj.getSpec().getTargetCols();
        final DataType dataType = targetCols.isEmpty() ? StringCell.TYPE : targetCols.get(0).getType();
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(outputColumnName, dataType);
        Set<DataCell> outcomes = new LinkedHashSet<>();
        for (Rule rule : rules) {
            DataCell outcome;
            if (dataType.equals(BooleanCell.TYPE)) {
                outcome = BooleanCellFactory.create(rule.getOutcome());
            } else if (dataType.equals(StringCell.TYPE)) {
                outcome = new StringCell(rule.getOutcome());
            } else if (dataType.equals(DoubleCell.TYPE)) {
                try {
                    outcome = new DoubleCell(Double.parseDouble(rule.getOutcome()));
                } catch (NumberFormatException e) {
                    //ignore
                    continue;
                }
            } else if (dataType.equals(IntCell.TYPE)) {
                try {
                    outcome = new IntCell(Integer.parseInt(rule.getOutcome()));
                } catch (NumberFormatException e) {
                    //ignore
                    continue;
                }
            } else if (dataType.equals(LongCell.TYPE)) {
                try {
                    outcome = new LongCell(Long.parseLong(rule.getOutcome()));
                } catch (NumberFormatException e) {
                    //ignore
                    continue;
                }
            } else {
                throw new UnsupportedOperationException("Unknown outcome type: " + dataType);
            }
            outcomes.add(outcome);
        }
        specCreator.setDomain(new DataColumnDomainCreator(outcomes).createDomain());
        DataColumnSpec colSpec = specCreator.createSpec();
        final RuleSelectionMethod ruleSelectionMethod = translator.getSelectionMethodList().get(0);
        final String defaultScore = translator.getDefaultScore();
        final Double defaultConfidence = translator.getDefaultConfidence();
        final DataColumnSpec[] specs;
        if (addConfidence) {
            specs = new DataColumnSpec[]{
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(ret.createSpec(), confidenceColumnName),
                    DoubleCell.TYPE).createSpec(),
                colSpec};
        } else {
            specs = new DataColumnSpec[]{colSpec};
        }
        final int oldColumnIndex = replaceColumn ? ret.indexOf(outputColumnName) : -1;
        ret.append(new AbstractCellFactory(processConcurrently, specs) {
            private final List<String> m_values;
            {
                Map<String, List<String>> dd = translator.getDataDictionary();
                m_values = dd.get(targetCols.get(0).getName());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell[] getCells(final DataRow row) {
                //See http://www.dmg.org/v4-1/RuleSet.html#Rule
                switch (ruleSelectionMethod.getCriterion().intValue()) {
                    case RuleSelectionMethod.Criterion.INT_FIRST_HIT: {
                        Pair<DataCell, Double> resultAndConfidence = selectFirstHit(row);
                        return toCells(resultAndConfidence);
                    }
                    case RuleSelectionMethod.Criterion.INT_WEIGHTED_MAX: {
                        Pair<DataCell, Double> resultAndConfidence = selectWeightedMax(row);
                        return toCells(resultAndConfidence);
                    }
                    case RuleSelectionMethod.Criterion.INT_WEIGHTED_SUM: {
                        Pair<DataCell, Double> resultAndConfidence = selectWeightedSum(row);
                        return toCells(resultAndConfidence);
                    }
                    default:
                        throw new UnsupportedOperationException(ruleSelectionMethod.getCriterion().toString());
                }
            }

            /**
             * Converts the pair to a {@link DataCell} array.
             *
             * @param resultAndConfidence The {@link Pair}.
             * @return The result and possibly the confidence.
             */
            private DataCell[] toCells(final Pair<DataCell, Double> resultAndConfidence) {
                if (!addConfidence) {
                    return new DataCell[]{resultAndConfidence.getFirst()};
                }
                if (resultAndConfidence.getSecond() == null) {
                    return new DataCell[]{DataType.getMissingCell(), resultAndConfidence.getFirst()};
                }
                return new DataCell[]{new DoubleCell(resultAndConfidence.getSecond()), resultAndConfidence.getFirst()};
            }

            /**
             * Computes the result and the confidence using the weighted sum method.
             *
             * @param row A {@link DataRow}
             * @return The result and the confidence.
             */
            private Pair<DataCell, Double> selectWeightedSum(final DataRow row) {
                final Map<String, Double> scoreToSumWeight = new LinkedHashMap<String, Double>();
                for (String val : m_values) {
                    scoreToSumWeight.put(val, 0.0);
                }
                int matchedRuleCount = 0;
                for (final PMMLRuleTranslator.Rule rule : rules) {
                    if (rule.getCondition().evaluate(row, spec) == Boolean.TRUE) {
                        ++matchedRuleCount;
                        Double sumWeight = scoreToSumWeight.get(rule.getOutcome());
                        if (sumWeight == null) {
                            throw new IllegalStateException(
                                "The score value: " + rule.getOutcome() + " is not in the data dictionary.");
                        }
                        final Double wRaw = rule.getWeight();
                        final double w = wRaw == null ? 0.0 : wRaw.doubleValue();
                        scoreToSumWeight.put(rule.getOutcome(), sumWeight + w);
                    }
                }
                double maxSumWeight = Double.NEGATIVE_INFINITY;
                String bestScore = null;
                for (Entry<String, Double> entry : scoreToSumWeight.entrySet()) {
                    final double d = entry.getValue().doubleValue();
                    if (d > maxSumWeight) {
                        maxSumWeight = d;
                        bestScore = entry.getKey();
                    }
                }
                if (bestScore == null || matchedRuleCount == 0) {
                    return pair(result(defaultScore), defaultConfidence);
                }
                return pair(result(bestScore), maxSumWeight / matchedRuleCount);
            }

            /**
             * Helper method to create {@link Pair}s.
             *
             * @param f The first element.
             * @param s The second element.
             * @return The new pair.
             */
            private <F, S> Pair<F, S> pair(final F f, final S s) {
                return new Pair<F, S>(f, s);
            }

            /**
             * Computes the result and the confidence using the weighted max method.
             *
             * @param row A {@link DataRow}
             * @return The result and the confidence.
             */
            private Pair<DataCell, Double> selectWeightedMax(final DataRow row) {
                double maxWeight = Double.NEGATIVE_INFINITY;
                PMMLRuleTranslator.Rule bestRule = null;
                for (final PMMLRuleTranslator.Rule rule : rules) {
                    if (rule.getCondition().evaluate(row, spec) == Boolean.TRUE) {
                        if (rule.getWeight() > maxWeight) {
                            maxWeight = rule.getWeight();
                            bestRule = rule;
                        }
                    }
                }
                if (bestRule == null) {
                    return pair(result(defaultScore), defaultConfidence);
                }
                bestRule.setRecordCount(bestRule.getRecordCount() + 1);
                DataCell result = result(bestRule);
                if (validationColumnIdx >= 0) {
                    if (row.getCell(validationColumnIdx).equals(result)) {
                        bestRule.setNbCorrect(bestRule.getNbCorrect() + 1);
                    }
                }
                Double confidence = bestRule.getConfidence();
                return pair(result, confidence == null ? defaultConfidence : confidence);
            }

            /**
             * Selects the outcome of the rule and converts it to the proper outcome type.
             *
             * @param rule A {@link Rule}.
             * @return The {@link DataCell} representing the result. (May be missing.)
             */
            private DataCell result(final PMMLRuleTranslator.Rule rule) {
                String outcome = rule.getOutcome();
                return result(outcome);
            }

            /**
             * Constructs the {@link DataCell} from its {@link String} representation ({@code outcome}) and its type.
             *
             * @param dataType The expected {@link DataType}
             * @param outcome The {@link String} representation.
             * @return The {@link DataCell}.
             */
            private DataCell result(final String outcome) {
                if (outcome == null) {
                    return DataType.getMissingCell();
                }
                try {
                    if (dataType.isCompatible(BooleanValue.class)) {
                        return BooleanCellFactory.create(outcome);
                    }
                    if (IntCell.TYPE.isASuperTypeOf(dataType)) {
                        return new IntCell(Integer.parseInt(outcome));
                    }
                    if (LongCell.TYPE.isASuperTypeOf(dataType)) {
                        return new LongCell(Long.parseLong(outcome));
                    }
                    if (DoubleCell.TYPE.isASuperTypeOf(dataType)) {
                        return new DoubleCell(Double.parseDouble(outcome));
                    }
                    return new StringCell(outcome);
                } catch (NumberFormatException e) {
                    return new MissingCell(outcome + "\n" + e.getMessage());
                }
            }

            /**
             * Selects the first rule that matches and computes the confidence and result for the {@code row}.
             *
             * @param row A {@link DataRow}.
             * @return The result and the confidence.
             */
            private Pair<DataCell, Double> selectFirstHit(final DataRow row) {
                for (final PMMLRuleTranslator.Rule rule : rules) {
                    Boolean eval = rule.getCondition().evaluate(row, spec);
                    if (eval == Boolean.TRUE) {
                        rule.setRecordCount(rule.getRecordCount() + 1);
                        DataCell result = result(rule);
                        if (validationColumnIdx >= 0) {
                            if (row.getCell(validationColumnIdx).equals(result)) {
                                rule.setNbCorrect(rule.getNbCorrect() + 1);
                            }
                        }
                        Double confidence = rule.getConfidence();
                        return pair(result, confidence == null ? defaultConfidence : confidence);
                    }
                }
                return pair(result(defaultScore), defaultConfidence);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void afterProcessing() {
                super.afterProcessing();
                obj.getPMMLValue();
                RuleSetModel ruleSet = translator.getOriginalRuleSetModel();
                assert rules.size() == ruleSet.getRuleSet().getSimpleRuleList().size()
                    + ruleSet.getRuleSet().getCompoundRuleList().size();
                if (ruleSet.getRuleSet().getSimpleRuleList().size() == rules.size()) {
                    for (int i = 0; i < rules.size(); ++i) {
                        Rule rule = rules.get(i);
                        final SimpleRule simpleRuleArray = ruleSet.getRuleSet().getSimpleRuleArray(i);
                        synchronized (simpleRuleArray) /*synchronized fixes AP-6766 */ {
                            simpleRuleArray.setRecordCount(rule.getRecordCount());
                            if (validationColumnIdx >= 0) {
                                simpleRuleArray.setNbCorrect(rule.getNbCorrect());
                            } else if (simpleRuleArray.isSetNbCorrect()) {
                                simpleRuleArray.unsetNbCorrect();
                            }
                        }
                    }
                }
            }
        });
        if (replaceColumn) {
            ret.remove(outputColumnName);
            ret.move(ret.getColumnCount() - 1 - (addConfidence ? 1 : 0), oldColumnIndex);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec original = (DataTableSpec)inSpecs[DATA_INDEX];
        ColumnRearranger rearranger = new ColumnRearranger(original);
        PMMLPortObjectSpec portObjectSpec = (PMMLPortObjectSpec)inSpecs[MODEL_INDEX];
        List<DataColumnSpec> activeColumnList = portObjectSpec.getActiveColumnList();
        List<DataColumnSpec> notFound = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec dataColumnSpec : activeColumnList) {
            if (original.containsName(dataColumnSpec.getName())) {
                DataColumnSpec origSpec = original.getColumnSpec(dataColumnSpec.getName());
                if (!origSpec.getType().equals(dataColumnSpec.getType())) {
                    notFound.add(dataColumnSpec);
                }
            } else {
                notFound.add(dataColumnSpec);
            }
        }
        if (!notFound.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                "Incompatible to the table, the following columns are not present, or have a wrong type:");
            for (DataColumnSpec dataColumnSpec : notFound) {
                sb.append("\n   ").append(dataColumnSpec);
            }
            throw new InvalidSettingsException(sb.toString());
        }
        List<DataColumnSpec> targetCols = portObjectSpec.getTargetCols();
        final DataType dataType = targetCols.isEmpty() ? StringCell.TYPE : targetCols.get(0).getType();
        DataColumnSpecCreator specCreator;
        if (m_doReplaceColumn.getBooleanValue()) {
            String col = m_replaceColumn.getStringValue();
            specCreator = new DataColumnSpecCreator(col, dataType);
        } else {
            specCreator = new DataColumnSpecCreator(
                DataTableSpec.getUniqueColumnName(original, m_outputColumn.getStringValue()), dataType);
        }
        SingleCellFactory dummy = new SingleCellFactory(specCreator.createSpec()) {
            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell getCell(final DataRow row) {
                throw new IllegalStateException();
            }
        };
        if (m_addConfidence.getBooleanValue()) {
            rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(
                DataTableSpec.getUniqueColumnName(rearranger.createSpec(), m_confidenceColumn.getStringValue()),
                DoubleCell.TYPE).createSpec()) {
                @Override
                public DataCell getCell(final DataRow row) {
                    throw new IllegalStateException();
                }
            });
        }
        if (m_doReplaceColumn.getBooleanValue()) {
            rearranger.replace(dummy, m_replaceColumn.getStringValue());
        } else {
            rearranger.append(dummy);
        }
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_outputColumn.saveSettingsTo(settings);
        m_addConfidence.saveSettingsTo(settings);
        m_confidenceColumn.saveSettingsTo(settings);
        m_replaceColumn.saveSettingsTo(settings);
        m_doReplaceColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_outputColumn.loadSettingsFrom(settings);
        m_addConfidence.loadSettingsFrom(settings);
        m_confidenceColumn.loadSettingsFrom(settings);
        try {
            m_replaceColumn.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_replaceColumn.setStringValue(DEFAULT_REPLACE_COLUMN);
        }
        try {
            m_doReplaceColumn.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_doReplaceColumn.setBooleanValue(DEFAULT_DO_REPLACE_COLUMN);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        SettingsModelString v = new SettingsModelString(CFGKEY_OUTPUT_COLUMN, DEFAULT_OUTPUT_COLUMN);
        v.loadSettingsFrom(settings);
        if (v.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Specify a non-empty column name as a result.");
        }
        //Nothing to validate for the add confidence setting.
        SettingsModelString u = new SettingsModelString(CFGKEY_CONFIDENCE_COLUMN, DEFAULT_CONFIDENCE_COLUN);
        u.loadSettingsFrom(settings);
        if (u.isEnabled() && u.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Specify a non-empty column name as a confidence.");
        }
        SettingsModelString r = createReplaceColumn();
        try {
            r.validateSettings(settings);
        } catch (InvalidSettingsException e) {
            m_replaceColumn.setStringValue(DEFAULT_REPLACE_COLUMN);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = super.getInputPortRoles();
        inputPortRoles[DATA_INDEX] = InputPortRole.DISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final OutputPortRole[] outputPortRoles = super.getOutputPortRoles();
        outputPortRoles[0] = OutputPortRole.DISTRIBUTED;
        return outputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec preSpec = (DataTableSpec)inSpecs[DATA_INDEX];
        return new StreamableOperator() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final PortObjectInput modelPort = (PortObjectInput)inputs[MODEL_INDEX];
                final ColumnRearranger rearranger = createRearranger((PMMLPortObject)modelPort.getPortObject(), preSpec,
                    m_doReplaceColumn.getBooleanValue(),
                    m_doReplaceColumn.getBooleanValue() ? m_replaceColumn.getStringValue()
                        : DataTableSpec.getUniqueColumnName(preSpec, m_outputColumn.getStringValue()),
                    m_addConfidence.getBooleanValue(), m_confidenceColumn.getStringValue(),
                    /*no validation column*/-1, /* no statistics computed, so concurrent processing is allowed*/
                    true);
                final DataTableSpec tableSpec = rearranger.createSpec();
                if (m_doReplaceColumn.getBooleanValue()) {
                    DataColumnSpec[] columns = new DataColumnSpec[preSpec.getNumColumns()];
                    for (int i = columns.length; i-- > 0;) {
                        columns[i] = preSpec.getColumnSpec(i);
                    }
                    int columnIndex = preSpec.findColumnIndex(m_replaceColumn.getStringValue());
                    if (m_addConfidence.getBooleanValue()) {
                        //Move confidence to the end
                        rearranger.move(columnIndex, tableSpec.getNumColumns());
                        //Move the result to its place
                        rearranger.move(tableSpec.getNumColumns() - 2, columnIndex);
                    }
                }
                final StreamableFunction function = rearranger.createStreamableFunction(DATA_INDEX, 0);
                function.runFinal(inputs, outputs, exec);
            }
        };
    }
}
