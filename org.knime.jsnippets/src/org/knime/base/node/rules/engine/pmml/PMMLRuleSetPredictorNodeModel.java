/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.08.17. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
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
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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
import org.knime.core.pmml.PMMLModelType;
import org.knime.core.util.Pair;
import org.w3c.dom.Node;

/**
 * This is the model implementation of PMMLRuleSetPredictor. Applies the rules to the input table.
 *
 * @author Gabor Bakos
 */
public class PMMLRuleSetPredictorNodeModel extends NodeModel {
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

    private SettingsModelBoolean m_addConfidence = new SettingsModelBoolean(CFGKEY_ADD_CONFIDENCE,
        DEFAULT_ADD_CONFIDENCE);

    private SettingsModelString m_confidenceColumn = new SettingsModelString(CFGKEY_CONFIDENCE_COLUMN,
        DEFAULT_CONFIDENCE_COLUN);

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
        super(new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        PMMLPortObject obj = (PMMLPortObject)inData[1];
        ColumnRearranger rearranger = createColumnRearranger(obj, (DataTableSpec)inData[0].getSpec(), exec);
        BufferedDataTable table = exec.createColumnRearrangeTable((BufferedDataTable)inData[0], rearranger, exec);
        if (m_doReplaceColumn.getBooleanValue()) {
            DataTableSpec preSpec = table.getSpec();
            DataColumnSpec[] columns = new DataColumnSpec[preSpec.getNumColumns()];
            for (int i = columns.length; i-->0;) {
                columns[i] = preSpec.getColumnSpec(i);
            }
            int columnIndex = ((BufferedDataTable)inData[0]).getSpec().findColumnIndex(m_replaceColumn.getStringValue());
            DataColumnSpecCreator creator = new DataColumnSpecCreator(columns[columnIndex]);
            creator.setName(m_replaceColumn.getStringValue());
            columns[columnIndex] = creator.createSpec();
            DataTableSpec newSpec = new DataTableSpec(columns);
            table = exec.createSpecReplacerTable(table, newSpec);
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
        List<Node> models = obj.getPMMLValue().getModels(PMMLModelType.RuleSetModel);
        if (models.size() != 1) {
            throw new InvalidSettingsException("Expected exactly on RuleSetModel, but got: " + models.size());
        }
        exec.setMessage("Loading model.");
        final PMMLRuleTranslator translator = new PMMLRuleTranslator();
        obj.addModelTranslater(translator);
        obj.initializeModelTranslator(translator);
        if (!translator.isScorable()) {
            throw new UnsupportedOperationException("The model is not scorable.");
        }
        final List<PMMLRuleTranslator.Rule> rules = translator.getRules();
        ColumnRearranger ret = new ColumnRearranger(spec);
        final List<DataColumnSpec> targetCols = obj.getSpec().getTargetCols();
        //TODO Maybe this check is not necessary, we do not use this info besides the type.
        if (targetCols.size() != 1) {
            throw new InvalidSettingsException("Exactly on output column is required, but got: " + targetCols);
        }
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(targetCols.get(0));
        if (m_doReplaceColumn.getBooleanValue()) {
            specCreator.setName(DataTableSpec.getUniqueColumnName(spec, m_replaceColumn.getStringValue()));
        } else {
            specCreator.setName(m_outputColumn.getStringValue());
        }
        DataColumnSpec colSpec = specCreator.createSpec();
        final DataType dataType = colSpec.getType();
        final RuleSelectionMethod ruleSelectionMethod = translator.getSelectionMethodList().get(0);
        final String defaultScore = translator.getDefaultScore();
        final Double defaultConfidence = translator.getDefaultConfidence();
        final DataColumnSpec[] specs;
        if (m_addConfidence.getBooleanValue()) {
            specs =
                new DataColumnSpec[]{
                    new DataColumnSpecCreator(m_confidenceColumn.getStringValue(), DoubleCell.TYPE).createSpec(),
                    colSpec};
        } else {
            specs = new DataColumnSpec[]{colSpec};
        }
        final int oldColumnIndex =
            m_doReplaceColumn.getBooleanValue() ? ret.indexOf(m_replaceColumn.getStringValue()) : -1;
        ret.append(new AbstractCellFactory(true, specs) {
            List<String> values;
            {
                Map<String, List<String>> dd = translator.getDataDictionary();
                values = dd.get(targetCols.get(0).getName());
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
                if (!m_addConfidence.getBooleanValue()) {
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
                for (String val : values) {
                    scoreToSumWeight.put(val, 0.0);
                }
                int matchedRuleCount = 0;
                for (final PMMLRuleTranslator.Rule rule : rules) {
                    if (rule.getCondition().evaluate(row, spec) == Boolean.TRUE) {
                        ++matchedRuleCount;
                        Double sumWeight = scoreToSumWeight.get(rule.getOutcome());
                        if (sumWeight == null) {
                            throw new IllegalStateException("The score value: " + rule.getOutcome()
                                + " is not in the data dictionary.");
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
                Double confidence = bestRule.getConfidence();
                return pair(result(bestRule), confidence == null ? defaultConfidence : confidence);
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
                        return BooleanCell.get(Boolean.parseBoolean(outcome));
                    }
                    if (IntCell.TYPE.isASuperTypeOf(dataType)) {
                        return new IntCell(Integer.parseInt(outcome));
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
                        Double confidence = rule.getConfidence();
                        return pair(result(rule), confidence == null ? defaultConfidence : confidence);
                    }
                }
                return pair(result(defaultScore), defaultConfidence);
            }
        });
        if (m_doReplaceColumn.getBooleanValue()) {
            ret.move(ret.getColumnCount() - 1 - (m_addConfidence.getBooleanValue() ? 1 : 0), oldColumnIndex);
            ret.remove(m_replaceColumn.getStringValue());
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
        DataTableSpec original = (DataTableSpec)inSpecs[0];
        ColumnRearranger rearranger = new ColumnRearranger(original);
        PMMLPortObjectSpec portObjectSpec = (PMMLPortObjectSpec)inSpecs[1];
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
            StringBuilder sb =
                new StringBuilder(
                    "Incompatible to the table, the following columns are not present, or have a wrong type:");
            for (DataColumnSpec dataColumnSpec : notFound) {
                sb.append("\n   ").append(dataColumnSpec);
            }
            throw new InvalidSettingsException(sb.toString());
        }
        List<DataColumnSpec> targetCols = portObjectSpec.getTargetCols();
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(targetCols.get(0));
        if (m_doReplaceColumn.getBooleanValue()) {
            specCreator.setName(m_replaceColumn.getStringValue());
        } else {
            specCreator.setName(m_outputColumn.getStringValue());
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
            rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(m_confidenceColumn.getStringValue(),
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
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

}
