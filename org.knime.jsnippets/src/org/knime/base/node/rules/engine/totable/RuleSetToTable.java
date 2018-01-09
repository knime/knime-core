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
 *   30 May 2015 (Gabor): created
 */
package org.knime.base.node.rules.engine.totable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.base.node.rules.engine.pmml.PMMLRuleTranslator;
import org.knime.base.node.rules.engine.pmml.PMMLRuleTranslator.Rule;
import org.knime.base.node.rules.engine.pmml.PMMLRuleTranslator.ScoreProbabilityAndRecordCount;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.pmml.PMMLModelType;
import org.knime.core.util.UniqueNameGenerator;

/**
 * A class to convert a RuleSet to a normal {@link DataTable}.
 *
 * @author Gabor Bakos
 */
public class RuleSetToTable {
    /**
     * The number of correct column name.
     */
    public static final String NUMBER_OF_CORRECT = "Number of correct";

    /**
     * The record count column name.
     */
    public static final String RECORD_COUNT = "Record count";

    /**
     * The weight column name.
     */
    public static final String WEIGHT = "Weight";

    /**
     * The confidence column name.
     */
    public static final String CONFIDENCE = "Confidence";

    /**
     * The rule column name.
     */
    public static final String RULE = "Rule";

    /**
     * The outcome column name.
     */
    public static final String OUTCOME = "Outcome";

    /**
     * The condition column name.
     */
    public static final String CONDITION = "Condition";

    /** The settings/configuration to use. */
    private RulesToTableSettings m_settings;

    /**
     * Constructs the transformator class.
     *
     * @param settings The configuration.
     */
    public RuleSetToTable(final RulesToTableSettings settings) {
        this.m_settings = settings;
    }

    /**
     * Helper method to generate the output spec.
     *
     * @param spec The input {@link PMMLPortObject}.
     * @return The output table's spec.
     */
    public DataTableSpec configure(final PMMLPortObjectSpec spec) {
        List<DataColumnSpec> targetCols = spec.getTargetCols();
        CheckUtils.checkState(targetCols.size() == 1,
            "Only a single output column is supported, but got: " + targetCols.size() + "\nThese: " + targetCols);
        if ((m_settings.getScoreTableRecordCount().isEnabled()
            && m_settings.getScoreTableRecordCount().getBooleanValue())
            || (m_settings.getScoreTableProbability().isEnabled()
                && m_settings.getScoreTableProbability().getBooleanValue())) {
            //We have no information about the score values.
            return null;
        }
        List<DataColumnSpec> specs = baseOutputColumns();
        return new DataTableSpecCreator().addColumns(specs.toArray(new DataColumnSpec[0])).createSpec();
    }

    /**
     * @return The rule output columns.
     */
    private List<DataColumnSpec> baseOutputColumns() {
        List<DataColumnSpec> specs = new ArrayList<>();
        if (m_settings.getSplitRules().getBooleanValue()) {
            specs.add(new DataColumnSpecCreator(CONDITION, StringCell.TYPE).createSpec());
            //TODO in case we should infer type, we cannot, which case should we return null for the whole table?
            specs.add(new DataColumnSpecCreator(OUTCOME, StringCell.TYPE).createSpec());
        } else {
            specs.add(new DataColumnSpecCreator(RULE, StringCell.TYPE).createSpec());
        }
        if (m_settings.getConfidenceAndWeight().getBooleanValue()) {
            specs.add(new DataColumnSpecCreator(CONFIDENCE, DoubleCell.TYPE).createSpec());
            specs.add(new DataColumnSpecCreator(WEIGHT, DoubleCell.TYPE).createSpec());
        }
        if (m_settings.getProvideStatistics().getBooleanValue()) {
            specs.add(new DataColumnSpecCreator(RECORD_COUNT, DoubleCell.TYPE).createSpec());
            specs.add(new DataColumnSpecCreator(NUMBER_OF_CORRECT, DoubleCell.TYPE).createSpec());
        }
        return specs;
    }

    /**
     * Performs the conversion.
     *
     * @param exec An {@link ExecutionContext}.
     * @param pmmlPo The input {@link PMMLPortObject}.
     * @return The created {@link BufferedDataTable}.
     * @throws CanceledExecutionException Execition was cancelled.
     * @throws InvalidSettingsException No or more than one RuleSet model is in the PMML input.
     */
    public BufferedDataTable execute(final ExecutionContext exec, final PMMLPortObject pmmlPo)
        throws CanceledExecutionException, InvalidSettingsException {
        // TODO should the defaults (confidence) be properties on columns?
        // TODO should the rule selection method be an output flow variable?
        if (pmmlPo.getPMMLValue().getModels(PMMLModelType.RuleSetModel).size() != 1) {
            throw new InvalidSettingsException("Only a single RuleSet model is supported.");
        }

        PMMLRuleTranslator ruleTranslator = new PMMLRuleTranslator();
        pmmlPo.initializeModelTranslator(ruleTranslator);
        List<Rule> rules = ruleTranslator.getRules();

        final DataTableSpec confSpec = configure(pmmlPo.getSpec());
        final List<String> scoreValues = new ArrayList<>();
        final DataTableSpec properSpec = confSpec != null ? confSpec : properSpec(rules, scoreValues);
        BufferedDataContainer container = exec.createDataContainer(properSpec);
        List<DataColumnSpec> targetCols = pmmlPo.getSpec().getTargetCols();
        DataType outcomeType = targetCols.get(0).getType();

        long idx = 0L;
        int rulesSize = rules.size();
        Map<String, DataType> types = new LinkedHashMap<>();
        for (DataColumnSpec col : pmmlPo.getSpec().getLearningCols()) {
            types.put(col.getName(), col.getType());
        }
        for (Rule rule : rules) {
            exec.checkCanceled();
            exec.setProgress(1.0 * idx++ / rulesSize);
            container.addRowToTable(
                new DefaultRow(RowKey.createRowKey(idx), createRow(rule, outcomeType, types, scoreValues)));
        }
        container.close();
        return container.getTable();
    }

    /**
     * @param rules The rules with score distribution.
     * @param scoreValues The score values.
     * @return Spec with columns from score distribution.
     */
    private DataTableSpec properSpec(final List<Rule> rules, final List<String> scoreValues) {
        final List<DataColumnSpec> specs = baseOutputColumns();
        final Set<String> specSet = specs.stream().map(s -> s.getName()).collect(Collectors.toSet());
        specs.addAll(scoreOutputColumns(rules, scoreValues, new UniqueNameGenerator(specSet)));
        return new DataTableSpecCreator().addColumns(specs.toArray(new DataColumnSpec[0])).createSpec();
    }

    /**
     * @param rules The rules with score distribution.
     * @param scoreValues The score values.
     * @param nameGenerator The unique name generator.
     * @return Spec columns from score distribution.
     */
    private Collection<? extends DataColumnSpec> scoreOutputColumns(final List<Rule> rules,
        final List<String> scoreValues, final UniqueNameGenerator nameGenerator) {
        List<String> values = rules.stream().flatMap(r -> r.getScoreDistribution().keySet().stream()).distinct()
            .collect(Collectors.toList());
        scoreValues.addAll(values);
        List<DataColumnSpec> newSpecs = new ArrayList<>(values.size());
        if (m_settings.getScoreTableRecordCount().isEnabled()
            && m_settings.getScoreTableRecordCount().getBooleanValue()) {
            for (final String value : values) {
                newSpecs.add(new DataColumnSpecCreator(
                    nameGenerator.newName(m_settings.getScoreTableRecordCountPrefix().getStringValue() + value),
                    DoubleCell.TYPE).createSpec());
            }
        }
        if (m_settings.getScoreTableProbability().isEnabled()
            && m_settings.getScoreTableProbability().getBooleanValue()) {
            for (final String value : values) {
                newSpecs.add(new DataColumnSpecCreator(
                    nameGenerator.newName(m_settings.getScoreTableProbabilityPrefix().getStringValue() + value),
                    DoubleCell.TYPE).createSpec());
            }
        }
        return newSpecs;
    }

    /**
     * Creates a row, {@link DataCell} values based on {@code rule} and the other parameters.
     *
     * @param rule A PMML {@link Rule}.
     * @param outcomeType The expected outcome.
     * @param types The types of the input column.
     * @return The cells for the {@code rule}.
     */
    private DataCell[] createRow(final Rule rule, final DataType outcomeType, final Map<String, DataType> types,
        final List<String> scoreValues) {
        List<DataCell> ret = new ArrayList<>();
        boolean usePrecedence = !m_settings.getAdditionalParentheses().getBooleanValue();
        if (m_settings.getSplitRules().getBooleanValue()) {
            ret.add(new StringCell(convertToString(rule.getCondition(), usePrecedence, types)));
            ret.add(convertToExpectedType(rule.getOutcome(), outcomeType));
        } else {
            ret.add(new StringCell(convertToString(rule.getCondition(), usePrecedence, types) + " => "
                + toString(convertToExpectedType(rule.getOutcome(), outcomeType))));
        }
        if (m_settings.getConfidenceAndWeight().getBooleanValue()) {
            ret.add(toCell(rule.getConfidence()));
            ret.add(toCell(rule.getWeight()));
        }
        if (m_settings.getProvideStatistics().getBooleanValue()) {
            ret.add(toCell(rule.getRecordCount()));
            ret.add(toCell(rule.getNbCorrect()));
        }
        final Map<String, ScoreProbabilityAndRecordCount> scoreDistribution = rule.getScoreDistribution();
        if (m_settings.getScoreTableRecordCount().isEnabled()
            && m_settings.getScoreTableRecordCount().getBooleanValue()) {
            for (final String value : scoreValues) {
                if (scoreDistribution.containsKey(value)) {
                    ret.add(new DoubleCell(scoreDistribution.get(value).getRecordCount()));
                } else {
                    ret.add(DataType.getMissingCell());
                }
            }
        }
        if (m_settings.getScoreTableProbability().isEnabled()
            && m_settings.getScoreTableProbability().getBooleanValue()) {
            for (final String value : scoreValues) {
                if (scoreDistribution.containsKey(value)) {
                    final BigDecimal probability = scoreDistribution.get(value).getProbability();
                    ret.add(
                        probability == null ? DataType.getMissingCell() : new DoubleCell(probability.doubleValue()));
                } else {
                    ret.add(DataType.getMissingCell());
                }
            }
        }
        return ret.toArray(new DataCell[ret.size()]);
    }

    /**
     * Converts a {@link DataCell} to {@link String} for rules.
     *
     * @param cell A {@link DataCell}.
     * @return The value of {@code cell} as a {@link String}, properly escaped.
     * @throws InvalidSettingsException Missing cells are not supported.
     * @deprecated No longer used.
     * @see org.knime.base.node.rules.engine.twoports.RuleEngine2PortsSimpleSettings#asStringFailForMissing(DataCell)
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public static String toStringFailForMissing(final DataCell cell) throws InvalidSettingsException {
        if (cell.isMissing()) {
            throw new InvalidSettingsException("Missing cell");
        }
        return toString(cell);
    }

    /**
     * Converts a {@link DataCell} to {@link String} for rules.
     *
     * @param cell A {@link DataCell}.
     * @return The value of {@code cell} as a {@link String}, properly escaped.
     */
    public static String toString(final DataCell cell) {
        if (cell.isMissing()) {
            return "\"?\"";
        }
        if (cell instanceof StringValue) {
            StringValue sv = (StringValue)cell;
            String s = sv.getStringValue();
            return escapedText(s);
        }
        if (cell instanceof BooleanValue) {
            return Boolean.toString(((BooleanValue)cell).getBooleanValue()).toUpperCase();
        }
        if (cell instanceof DoubleValue) {
            return cell.toString();
        }
        return escapedText(cell.toString());
    }

    /**
     * @param s A {@link String}.
     * @return Properly escaped for rules.
     */
    private static String escapedText(final String s) {
        return s.contains("\"") ? "/" + (s.replace("/", "\\/")) + "/" : ('"' + s + '"');
    }

    /**
     * @param val A {@link Double} value.
     * @return A {@link DoubleCell} or missing of {@code val} is {@code null}.
     */
    private DataCell toCell(final Double val) {
        return val == null ? DataType.getMissingCell() : new DoubleCell(val.doubleValue());
    }

    /**
     * Converts a {@link String} ({@code outcome}) to the expected {@code outcomeType}.
     *
     * @param outcome {@link String} to convert.
     * @param outcomeType The expected outcome type.
     * @return The converted {@link DataCell}.
     */
    private static DataCell convertToExpectedType(final String outcome, final DataType outcomeType) {
        if (outcomeType == null || outcomeType.isCompatible(StringValue.class)) {
            return new StringCell(outcome);
        }
        if (outcomeType.isCompatible(BooleanValue.class)) {
            return BooleanCellFactory.create(outcome);
        }
        if (outcomeType.isCompatible(IntValue.class)) {
            return new IntCell(Integer.parseInt(outcome));
        }
        if (outcomeType.isCompatible(LongValue.class)) {
            return new LongCell(Long.parseLong(outcome));
        }
        if (outcomeType.isCompatible(DoubleValue.class)) {
            return new DoubleCell(Double.parseDouble(outcome));
        }
        return new StringCell(outcome);
    }

    /**
     * Converts {@code condition} to a {@link String}.
     *
     * @param condition A {@link PMMLPredicate}.
     * @param usePrecedence Should we simplify the condition?
     * @param types The type of input columns.
     * @return Converted {@code condition}.
     */
    static String convertToString(final PMMLPredicate condition, final boolean usePrecedence,
        final Map<String, DataType> types) {
        return convertToStringPrecedence(condition, usePrecedence, null, types);
    }

    /**
     * @param condition A {@link PMMLPredicate}.
     * @param usePrecedence Should we simplify the condition?
     * @param parentOperator The parent operator's (logical connective) type, used for precedence, can be {@code null}.
     * @param types The type of input columns.
     * @return Converted {@code condition} with precedence.
     */
    private static String convertToStringPrecedence(final PMMLPredicate condition, final boolean usePrecedence,
        final PMMLBooleanOperator parentOperator, final Map<String, DataType> types) {
        if (condition instanceof PMMLTruePredicate) {
            return "TRUE";
        }
        if (condition instanceof PMMLFalsePredicate) {
            return "FALSE";
        }
        if (condition instanceof PMMLSimplePredicate) {
            PMMLSimplePredicate sp = (PMMLSimplePredicate)condition;
            DataType dataType = types.get(sp.getSplitAttribute());
            switch (sp.getOperator()) {
                case EQUAL://intentional fall-through
                case GREATER_OR_EQUAL://intentional fall-through
                case GREATER_THAN://intentional fall-through
                case LESS_OR_EQUAL://intentional fall-through
                case LESS_THAN:
                    return dollars(sp.getSplitAttribute()) + " " + sp.getOperator().getSymbol() + " "
                        + asComparisonValue(sp.getThreshold(), dataType);
                case NOT_EQUAL:
                    return "NOT " + dollars(sp.getSplitAttribute()) + " = "
                        + asComparisonValue(sp.getThreshold(), dataType);
                case IS_MISSING:
                    return "MISSING " + dollars(sp.getSplitAttribute());
                case IS_NOT_MISSING:
                    return "NOT MISSING " + dollars(sp.getSplitAttribute());
                default:
                    throw new UnsupportedOperationException("Unknown operator: " + sp.getOperator());
            }
        }
        if (condition instanceof PMMLSimpleSetPredicate) {
            PMMLSimpleSetPredicate ssp = (PMMLSimpleSetPredicate)condition;
            DataType dataType = types.get(ssp.getSplitAttribute());
            switch (ssp.getSetOperator()) {
                case IS_IN:
                    return dollars(ssp.getSplitAttribute()) + " IN (" + asComparisonValues(ssp.getValues(), dataType)
                        + ")";
                case IS_NOT_IN:
                    return "NOT " + dollars(ssp.getSplitAttribute()) + " IN ("
                        + asComparisonValues(ssp.getValues(), dataType) + ")";
                default:
                    throw new UnsupportedOperationException("Unknown operator: " + ssp.getOperator());
            }
        }
        if (condition instanceof PMMLCompoundPredicate) {
            PMMLCompoundPredicate cp = (PMMLCompoundPredicate)condition;
            List<PMMLPredicate> predicates = cp.getPredicates();
            switch (cp.getBooleanOperator()) {
                case AND:
                    //never parentheses, parent XOR, OR, AND, nothing: all fine, SURROGATE is not supported
                    return parentheses(!usePrecedence, join(PMMLBooleanOperator.AND, cp, types, usePrecedence));
                case OR:
                    //if not nothing or OR, we have to use parentheses
                    return parentheses(
                        !usePrecedence || (predicates.size() > 1 && parentOperator != null
                            && parentOperator != PMMLBooleanOperator.OR),
                        join(PMMLBooleanOperator.OR, cp, types, usePrecedence));
                case XOR:
                    //if not nothing or XOR or OR, we have to use parentheses, so when it is an AND
                    return parentheses(
                        !usePrecedence || (predicates.size() > 1 && parentOperator == PMMLBooleanOperator.AND),
                        join(PMMLBooleanOperator.XOR, cp, types, usePrecedence));
                case SURROGATE: {
                    CheckUtils.checkState(predicates.size() > 1,
                        "At least two arguments are required for SURROGATE, but got only: " + predicates.size()
                            + "\nValues: " + predicates);

                    return handleSurrogate(cp, predicates, usePrecedence, parentOperator, types);
                }
                default:
                    throw new UnsupportedOperationException("Unknown operator: " + cp.getOperator());
            }
        }
        throw new IllegalArgumentException("Unknown predicate type: " + condition + " (" + condition.getClass());
    }

    /**
     * (This is a recursive method.)
     *
     * @param cp A SURROGATE {@link PMMLCompoundPredicate}.
     * @param predicates The predicates to be converted.
     * @param usePrecedence Should we simplify the condition?
     * @param parentOperator The parent operator's (logical connective) type, used for precedence, can be {@code null}.
     * @param types The type of input columns.
     * @return Converted {@code cp}.
     * @throws IllegalStateException If cannot be transformed.
     */
    private static String handleSurrogate(final PMMLCompoundPredicate cp, final List<PMMLPredicate> predicates,
        final boolean usePrecedence, final PMMLBooleanOperator parentOperator, final Map<String, DataType> types) {
        //surrogate(a, b) = if not missing(a) then rel(a) else b = ((NOT MISSING a) AND rel(a)) OR ((MISSING a) AND b)
        //surrogate(a, surrogate(b, c)) = if not missing(a) then rel(a) else if not missing(b) then rel(b) else c =
        //((NOT MISSING a) AND rel(a)) OR ((MISSING a) AND (((NOT MISSING b) AND rel(b)) OR ((MISSING b) AND rel(c))))
        PMMLPredicate first = predicates.get(0);
        List<PMMLPredicate> rest = predicates.subList(1, predicates.size());
        if (predicates.size() == 1) {
            return convertToStringPrecedence(first, usePrecedence, PMMLBooleanOperator.AND, types);
        }
        CheckUtils.checkState(
            first instanceof PMMLTruePredicate || first instanceof PMMLFalsePredicate
                || first instanceof PMMLSimplePredicate || first instanceof PMMLSimpleSetPredicate,
            "Compound predicates are not supported by the SURROGATE transformation: " + first + " in\n" + cp);
        if (first instanceof PMMLFalsePredicate || first instanceof PMMLTruePredicate) {
            return convertToString(first, usePrecedence, types);
        }
        if (first instanceof PMMLSimplePredicate || first instanceof PMMLSimpleSetPredicate) {
            return parentheses(!usePrecedence || (parentOperator != null && parentOperator != PMMLBooleanOperator.OR),
                parentheses(!usePrecedence/*OR is outside of this AND*/,
                    "NOT MISSING " + dollars(first.getSplitAttribute()) + " AND "
                        + convertToStringPrecedence(first, usePrecedence, PMMLBooleanOperator.AND, types))
                    + " OR "
                    + parentheses(!usePrecedence/*OR is outside of this AND*/,
                        "MISSING " + dollars(first.getSplitAttribute()) + " AND "
                            + handleSurrogate(cp, rest, usePrecedence, PMMLBooleanOperator.AND, types)));
        }
        throw new IllegalStateException(
            "Compound predicates are not supported at this position: " + first + " in\n" + cp);
    }

    /**
     * @param parentheses A boolean.
     * @param string A {@link String}.
     * @return Adds parentheses aroung {@code string} if {@code parentheses}.
     */
    private static String parentheses(final boolean parentheses, final String string) {
        if (parentheses) {
            return "(" + string + ")";
        }
        return string;
    }

    /**
     * Joins the compound predicate's representation from its subpredicates.
     *
     * @param op The operator's type.
     * @param cp The operator (NOT SURROGATE!)
     * @param types The types of the input columns.
     * @param usePrecedence Simplify the rules?
     * @return The joined arguments.
     * @throws UnsupportedOperationException if {@code cp} is SURROGATE
     * @throws IllegalStateException if cannot be transformed.
     */
    private static String join(final PMMLBooleanOperator op, final PMMLCompoundPredicate cp,
        final Map<String, DataType> types, final boolean usePrecedence) {
        StringBuilder sb = new StringBuilder();
        List<PMMLPredicate> predicates = cp.getPredicates();
        switch (predicates.size()) {
            case 0:
                switch (op) {
                    case AND:
                        return "TRUE";
                    case OR:
                        return "FALSE";
                    case XOR:
                        throw new IllegalStateException("Cannot have XOR without children!");
                    case SURROGATE:
                        throw new IllegalStateException("Cannot have SURROGATE without children!");
                    default:
                        throw new UnsupportedOperationException(
                            "Not supported PMML logical connective: " + op + " in\n" + cp);
                }
            case 1:
                return convertToStringPrecedence(predicates.get(0), usePrecedence, null, types);
            default: {
                int i = 0;
                for (PMMLPredicate predicate : predicates) {
                    ++i;
                    sb.append(convertToStringPrecedence(predicate, usePrecedence, cp.getBooleanOperator(), types));
                    if (i != predicates.size()) {
                        switch (cp.getBooleanOperator()) {
                            case AND:
                                sb.append(" AND ");
                                break;
                            case OR:
                                sb.append(" OR ");
                                break;
                            case XOR:
                                sb.append(" XOR ");
                                break;
                            case SURROGATE:
                                //#handleSurrogate
                                throw new UnsupportedOperationException("SURROGATEs are not supported: " + cp);
                            default:
                                throw new UnsupportedOperationException(
                                    "Unknown operator: " + cp.getBooleanOperator() + " in\n" + cp);
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Multiple comparison values (for {@code IN}).
     *
     * @param values The values to convert.
     * @param dataType The expected data type.
     * @return The joined values.
     * @see #asComparisonValue(String, DataType)
     */
    private static String asComparisonValues(final Set<String> values, final DataType dataType) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(asComparisonValue(value, dataType)).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - ", ".length());
        }
        return sb.toString();
    }

    /**
     * Converts {@code value} with expected {@code type} to {@link String} to be used in a comparison.
     *
     * @param value The {@link String} to convert.
     * @param dataType The expected {@link DataType}.
     * @return The converted {@code value}.
     */
    private static String asComparisonValue(final String value, final DataType dataType) {
        return toString(dataType.isCompatible(DoubleValue.class) ? convertToExpectedType(value, DoubleCell.TYPE)
            : convertToExpectedType(value, dataType));
    }

    /**
     * @param attr A {@link String}.
     * @return {@code attr} surrounded with {@code $} signs. (To represent column reference.)
     */
    private static String dollars(final String attr) {
        //No escaping
        return "$" + attr + "$";
    }
}
