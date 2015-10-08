package org.knime.base.node.rules.engine.twoports;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod;
import org.dmg.pmml.RuleSetDocument.RuleSet;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.SimpleRuleDocument.SimpleRule;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.rules.engine.BaseRuleParser.ParseState;
import org.knime.base.node.rules.engine.Condition.MatchOutcome.MatchState;
import org.knime.base.node.rules.engine.Expression;
import org.knime.base.node.rules.engine.Rule;
import org.knime.base.node.rules.engine.Rule.Outcome;
import org.knime.base.node.rules.engine.RuleEngineNodeModel;
import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.RuleSupport;
import org.knime.base.node.rules.engine.Util;
import org.knime.base.node.rules.engine.VariableProvider;
import org.knime.base.node.rules.engine.pmml.PMMLRuleParser;
import org.knime.base.node.rules.engine.pmml.PMMLRuleSetPredictorNodeModel;
import org.knime.base.node.rules.engine.pmml.PMMLRuleTranslator;
import org.knime.base.node.rules.engine.totable.RuleSetToTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;

/**
 * This is the model implementation of Rule Engine (Dictionary). Applies the rules from the second input port to the first
 * datatable.
 *
 * @author Gabor Bakos
 */
public class RuleEngine2PortsNodeModel extends NodeModel implements FlowVariableProvider {
    /** Index of the data port/optional flow variable port for the Rule * (Dictionary) nodes. */
    static final int DATA_PORT = 0,
            /** Index of the rules port for the Rule * (Dictionary) nodes. */
            RULE_PORT = 1;

    private final RuleEngine2PortsSettings m_settings = new RuleEngine2PortsSettings();

    private int m_rowCount;

    private PMMLPortObject m_copy;

    /**
     * Constructor for the node model.
     */
    protected RuleEngine2PortsNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE_OPTIONAL});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataTable data = (BufferedDataTable)inData[DATA_PORT];
        m_rowCount = data.getRowCount();
        try {
            Pair<ColumnRearranger, PortObject> rearrangerPair =
                createColumnRearranger((DataTableSpec)inData[DATA_PORT].getSpec(), (DataTable)inData[RULE_PORT]);
            BufferedDataTable predictedTable = exec.createColumnRearrangeTable(data, rearrangerPair.getFirst(), exec);
            PortObject second = rearrangerPair.getSecond();
            if (m_settings.isPMMLRuleSet()) {
                if (m_settings.isProvideStatistics()) {
                    PMMLPortObject po = (PMMLPortObject)rearrangerPair.getSecond();
                    PMMLPortObject pmmlPortObject = new PMMLPortObject(m_copy.getSpec(), po);
                    //Remove extra model.
                    pmmlPortObject.addModelTranslater(new PMMLTranslator() {
                        @Override
                        public void initializeFrom(final PMMLDocument pmmlDoc) {
                        }

                        @Override
                        public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
                            pmmlDoc.getPMML().removeRuleSetModel(1);
                            return null;
                        }
                    });
                    second = pmmlPortObject;
                } else {
                    second = m_copy;
                }
            }
            return new PortObject[]{predictedTable, second};
        } finally {
            m_rowCount = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_copy = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        String warning = autoGuessRuleColumnName(inSpecs, m_settings);
        if (warning != null) {
            setWarningMessage(warning);
        }
        m_rowCount = -1;
        return new PortObjectSpec[]{null//unfortunately cannot guess output type of column without reading the rules table
            /*createColumnRearranger((DataTableSpec)inSpecs[DATA_PORT], emptyDataTable((DataTableSpec)inSpecs[RULE_PORT]))
                .getFirst().createSpec()*/, null};
    }

    /**
     * Tries to autoguess the rule column name. In case it fails because of ambiguity it returns a warning message, otherwise it fails with {@link InvalidSettingsException}.
     *
     * @param inSpecs The input specs.
     * @param settings The {@link RuleEngine2PortsSimpleSettings} for the node model.
     * @return The warning message (or {@code null} if everything is fine).
     * @throws InvalidSettingsException Cannot find String-valued column for rules.
     */
    static String autoGuessRuleColumnName(final PortObjectSpec[] inSpecs,
        final RuleEngine2PortsSimpleSettings settings) throws InvalidSettingsException {
        // check spec with selected column
        String ruleColumn = settings.getRuleColumn();
        DataTableSpec ruleSpec = (DataTableSpec)inSpecs[RULE_PORT];
        if (ruleSpec == null) {
            throw new InvalidSettingsException("Rule table specification is not available.");
        }
        DataColumnSpec columnSpec = ruleSpec.getColumnSpec(ruleColumn);
        boolean isValid = columnSpec != null && columnSpec.getType().isCompatible(StringValue.class);
        CheckUtils.checkSetting(ruleColumn == null || isValid, "Rule column \"" + ruleColumn
            + "\" not found or incompatible");
        if (ruleColumn == null) { // auto-guessing
            assert !isValid : "No class column set but valid configuration";
            // if no useful column is selected guess one
            // get the first useful one starting at the end of the table
            for (int i = ruleSpec.getNumColumns(); i-- > 0;) {
                if (ruleSpec.getColumnSpec(i).getType().isCompatible(StringValue.class)) {
                    settings.setRuleColumn(ruleSpec.getColumnSpec(i).getName());
                    return "Guessing target column: \"" + settings.getRuleColumn() + "\".";
                }
            }
            CheckUtils.checkSetting(false, "Rules table contains no String column for rules.");
        }
        return null;
    }

//    /**
//     * @param inSpec The output {@link DataTableSpec}.
//     * @return A DataTable without rows.
//     */
//    private static DataTable emptyDataTable(final DataTableSpec inSpec) {
//        return new DataTable() {
//            @Override
//            public RowIterator iterator() {
//                return new DefaultRowIterator();
//            }
//
//            @Override
//            public DataTableSpec getDataTableSpec() {
//                return inSpec;
//            }
//        };
//    }
//
    /**
     * Creates a {@link ColumnRearranger} for the prediction and also the {@link PMMLPortObject} for the PMML model if possible.
     *
     * @param spec The input data table spec.
     * @param rules The rules data table spec.
     * @return The {@link ColumnRearranger} and a possibly {@code null} {@link PortObject}.
     * @throws InvalidSettingsException Wrong rule in the selected column.
     */
    private Pair<ColumnRearranger, PortObject> createColumnRearranger(final DataTableSpec spec, final DataTable rules)
        throws InvalidSettingsException {
        Map<String, FlowVariable> flowVars = getAvailableInputFlowVariables();
        PortObject po;
        int ruleIdx = rules.getDataTableSpec().findColumnIndex(m_settings.getRuleColumn()), outcomeIdx =
            rules.getDataTableSpec().findColumnIndex(m_settings.getOutcomeColumn()), confidenceIdx =
            rules.getDataTableSpec().findColumnIndex(m_settings.getRuleConfidenceColumn()), weightIdx =
            rules.getDataTableSpec().findColumnIndex(m_settings.getRuleWeightColumn()), validationIdx =
            spec.findColumnIndex(m_settings.getValidateColumn());
        String appendColumn = m_settings.getAppendColumn();
        if (appendColumn == null || appendColumn.isEmpty()) {
            appendColumn = RuleEngine2PortsSettings.DEFAULT_APPEND_COLUMN;
        }
        String outputColumnName =
            m_settings.isReplaceColumn() ? m_settings.getReplaceColumn() : DataTableSpec.getUniqueColumnName(spec,
                appendColumn);
        ColumnRearranger ret;
        if (m_settings.isPMMLRuleSet()) {
            PMMLDocument doc = PMMLDocument.Factory.newInstance();
            final PMML pmmlObj = doc.addNewPMML();
            RuleSetModel ruleSetModel = pmmlObj.addNewRuleSetModel();
            RuleSet ruleSet = ruleSetModel.addNewRuleSet();

            List<DataType> outcomeTypes = new ArrayList<>();
            PMMLRuleParser parser = new PMMLRuleParser(spec, flowVars);
            int lineNo = 0;
            for (DataRow dataRow : rules) {
                ++lineNo;
                DataCell rule = dataRow.getCell(ruleIdx);
                CheckUtils.checkSetting(!rule.isMissing(), "Missing rule in row: " + dataRow.getKey());
                if (rule instanceof StringValue) {
                    StringValue ruleText = (StringValue)rule;
                    String r = ruleText.getStringValue().replaceAll("[\r\n]+", " ");
                    if (RuleSupport.isComment(r)) {
                        continue;
                    }
                    if (outcomeIdx >= 0) {
                        r += " => " + RuleSetToTable.toStringFailForMissing(dataRow.getCell(outcomeIdx));
                    }
                    ParseState state = new ParseState(r);
                    try {
                        PMMLPredicate condition = parser.parseBooleanExpression(state);
                        SimpleRule simpleRule = ruleSet.addNewSimpleRule();
                        setCondition(simpleRule, condition);
                        state.skipWS();
                        state.consumeText("=>");
                        state.skipWS();
                        Expression outcome = parser.parseOutcomeOperand(state, null);
                        simpleRule.setScore(outcome.toString());
                        if (confidenceIdx >= 0) {
                            DataCell confidenceCell = dataRow.getCell(confidenceIdx);
                            if (!confidenceCell.isMissing()) {
                                if (confidenceCell instanceof DoubleValue) {
                                    DoubleValue dv = (DoubleValue)confidenceCell;
                                    double confidence = dv.getDoubleValue();
                                    simpleRule.setConfidence(confidence);
                                }
                            }
                        }
                        if (weightIdx >= 0) {
                            DataCell weightCell = dataRow.getCell(weightIdx);
                            boolean missing = true;
                            if (!weightCell.isMissing()) {
                                if (weightCell instanceof DoubleValue) {
                                    DoubleValue dv = (DoubleValue)weightCell;
                                    double weight = dv.getDoubleValue();
                                    simpleRule.setWeight(weight);
                                    missing = false;
                                }
                            }
                            if (missing && m_settings.isHasDefaultWeight()) {
                                simpleRule.setWeight(m_settings.getDefaultWeight());
                            }
                        }
                        CheckUtils.checkSetting(outcome.isConstant(), "Outcome is not constant in line " + lineNo
                            + " (" + dataRow.getKey() + ") for rule: " + rule);
                        outcomeTypes.add(outcome.getOutputType());
                    } catch (ParseException e) {
                        ParseException error = Util.addContext(e, r, lineNo);
                        throw new InvalidSettingsException("Wrong rule in line: " + dataRow.getKey() + "\n"
                            + error.getMessage(), error);
                    }
                } else {
                    CheckUtils.checkSetting(false, "Wrong type (" + rule.getType() + ") of rule: " + rule
                        + "\nin row: " + dataRow.getKey());
                }
            }
            ColumnRearranger dummy = new ColumnRearranger(spec);
            if (!m_settings.isReplaceColumn()) {
                dummy.append(new SingleCellFactory(new DataColumnSpecCreator(outputColumnName, RuleEngineNodeModel
                    .computeOutputType(outcomeTypes, computeOutcomeType(rules), true)).createSpec()) {
                    @Override
                    public DataCell getCell(final DataRow row) {
                        return null;
                    }
                });
            }
            PMMLPortObject pmml = createPMMLPortObject(doc, ruleSetModel, ruleSet, parser, dummy.createSpec());
            po = pmml;
            m_copy = copy(pmml);
            String predictionConfidenceColumn = m_settings.getPredictionConfidenceColumn();
            if (predictionConfidenceColumn == null || predictionConfidenceColumn.isEmpty()) {
                predictionConfidenceColumn = RuleEngine2PortsSettings.DEFAULT_PREDICTION_CONFIDENCE_COLUMN;
            }
            ret =
                PMMLRuleSetPredictorNodeModel.createRearranger(pmml, spec, m_settings.isReplaceColumn(),
                    outputColumnName, m_settings.isComputeConfidence(),
                    DataTableSpec.getUniqueColumnName(dummy.createSpec(), predictionConfidenceColumn), validationIdx);
        } else {
            RuleFactory factory = RuleFactory.getInstance(RuleNodeSettings.RuleEngine).cloned();
            po = InactiveBranchPortObject.INSTANCE;
            ret = new ColumnRearranger(spec);
            factory.disableMissingComparisons();
            factory.disableNaNComparisons();
            final List<Rule> ruleList = new ArrayList<>();
            int lineNo = 0;
            for (DataRow dataRow : rules) {
                lineNo++;
                DataCell cell = dataRow.getCell(ruleIdx);
                CheckUtils.checkSetting(!cell.isMissing(), "Missing rule in row: " + dataRow.getKey());
                if (cell instanceof StringValue) {
                    StringValue sv = (StringValue)cell;
                    String ruleText = sv.getStringValue();
                    if (outcomeIdx >= 0) {
                        try {
                            ruleText += " => " + RuleSetToTable.toStringFailForMissing(dataRow.getCell(outcomeIdx));
                        } catch (InvalidSettingsException e) {
                            if (RuleSupport.isComment(ruleText)) {
                                ruleText += " => ?";
                            } else {
                                throw e;
                            }
                        }
                    }
                    try {
                        Rule rule = factory.parse(ruleText, spec, flowVars);
                        if (rule.getCondition().isEnabled()) {
                            ruleList.add(rule);
                        }
                    } catch (ParseException e) {
                        ParseException error = Util.addContext(e, ruleText, lineNo);
                        throw new InvalidSettingsException("Wrong rule in line: " + dataRow.getKey() + "\n"
                            + error.getMessage(), error);
                    }
                } else {
                    CheckUtils.checkSetting(false, "Wrong type (" + cell.getType() + ") of rule: " + cell
                        + "\nin row: " + dataRow.getKey());
                }
            }
            //unfortunately we cannot compute the domain and limits of the output column.
            final DataType outType = RuleEngineNodeModel.computeOutputType(ruleList, computeOutcomeType(rules), RuleNodeSettings.RuleEngine);
            VariableProvider.SingleCellFactoryProto fac = new VariableProvider.SingleCellFactoryProto(new DataColumnSpecCreator(outputColumnName, outType)
                .createSpec()) {
                @Override
                public DataCell getCell(final DataRow row) {
                    for (Rule r : ruleList) {
                        if (r.getCondition().matches(row, this).getOutcome() == MatchState.matchedAndStop) {
                            Outcome outcome2 = r.getOutcome();
                            //                        r.getSideEffect().perform(row, this);
                            final DataCell cell = (DataCell)outcome2.getComputedResult(row, this);
                            if (outType.equals(StringCell.TYPE) && !cell.isMissing()
                                && !cell.getType().equals(StringCell.TYPE)) {
                                return new StringCell(cell.toString());
                            } else {
                                return cell;
                            }
                        }
                    }

                    return DataType.getMissingCell();
                }

                @Override
                public Object readVariable(final String name, final Class<?> type) {
                    return RuleEngine2PortsNodeModel.this.readVariable(name, type);
                }

                @Override
                public int getRowCount() {
                    return RuleEngine2PortsNodeModel.this.getRowCount();
                }
            };
            if (m_settings.isReplaceColumn()) {
                ret.replace(fac, outputColumnName);
            } else {
                ret.append(fac);
            }
        }
        return Pair.create(ret, po);
    }

    /**
     * @param rules The table containing the rules.
     * @return The default outcome type based on the selected column for outcomes.
     */
    private DataType computeOutcomeType(final DataTable rules) {
        final DataColumnSpec spec = rules.getDataTableSpec().getColumnSpec(m_settings.getOutcomeColumn());
        return spec == null ? StringCell.TYPE : spec.getType();
    }

    /**
     * Creates the {@link PMMLPortObject} based on {@code doc}, {@code ruleSetModel} and {@code ruleSet}.
     *
     * @param doc A {@link PMMLDocument}.
     * @param ruleSetModel The {@link RuleSetModel}.
     * @param ruleSet The {@link RuleSet}.
     * @param parser The {@link PMMLRuleParser} to collect used columns.
     * @param outputSpec The expected output table specification.
     * @return The computed {@link PMMLPortObject} with PMML RuleSet.
     */
    private PMMLPortObject createPMMLPortObject(final PMMLDocument doc, final RuleSetModel ruleSetModel,
        final RuleSet ruleSet, final PMMLRuleParser parser, final DataTableSpec outputSpec) {
        PMMLPortObject pmml = new PMMLPortObject(createPMMLPortObjectSpec(outputSpec, parser.getUsedColumns()));
        //
        //            if (inData[1] != null) {
        //                PMMLPortObject po = (PMMLPortObject)inData[1];
        //                TransformationDictionary dict = TransformationDictionary.Factory.newInstance();
        //                dict.setDerivedFieldArray(po.getDerivedFields());
        //                ret.addGlobalTransformations(dict);
        //            }
        fillPMMLPortObject(doc, ruleSetModel, ruleSet, pmml);
        return pmml;
    }

    /**
     * Fills the parts missing using the parameters.
     *
     * @param doc A {@link PMMLDocument}.
     * @param ruleSetModel The {@link RuleSetModel}.
     * @param ruleSet The {@link RuleSet}.
     * @param pmml The output {@link PMMLPortObject}.
     */
    private void fillPMMLPortObject(final PMMLDocument doc, final RuleSetModel ruleSetModel, final RuleSet ruleSet,
        final PMMLPortObject pmml) {
        ruleSetModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        if (m_settings.isHasDefaultConfidence()) {
            ruleSet.setDefaultConfidence(m_settings.getDefaultConfidence());
        }
        if (m_settings.isHasDefaultScore()) {
            ruleSet.setDefaultScore(m_settings.getDefaultScore());
        }
        if (m_settings.isProvideStatistics()) {
            ruleSet.setRecordCount(m_rowCount);
        }
        RuleSelectionMethod rsm = ruleSet.addNewRuleSelectionMethod();
        rsm.setCriterion(m_settings.getRuleSelectionMethod().asCriterion());
        fillUsingDoc(doc, ruleSetModel, pmml);
    }

    /**
     * Fills the results obtained from {@code doc}.
     *
     * @param doc A {@link PMMLDocument}.
     * @param ruleSetModel The {@link RuleSetModel}.
     * @param pmml The {@link PMMLPortObject}.
     */
    private void fillUsingDoc(final PMMLDocument doc, final RuleSetModel ruleSetModel, final PMMLPortObject pmml) {
        PMMLRuleTranslator modelTranslator = new PMMLRuleTranslator(m_settings.isProvideStatistics());
        PMMLMiningSchemaTranslator.writeMiningSchema(pmml.getSpec(), ruleSetModel);
        PMMLDataDictionaryTranslator ddTranslator = new PMMLDataDictionaryTranslator();
        ddTranslator.exportTo(doc, pmml.getSpec());
        modelTranslator.initializeFrom(doc);
        pmml.addModelTranslater(modelTranslator);
        pmml.validate();
    }

    /**
     * Creates a copy of a {@link PMMLPortObject}.
     *
     * @param pmml A {@link PMMLPortObject}.
     * @return The deep copy of {@code pmml}.
     * @throws IllegalStateException Hopefully never.
     */
    private PMMLPortObject copy(final PMMLPortObject pmml) {
        final PMMLPortObject ret = new PMMLPortObject(pmml.getSpec());
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            pmml.save(baos);
            try (final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                ret.loadFrom(pmml.getSpec(), bais);
            } catch (XmlException e) {
                throw new IllegalStateException(e);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return ret;
    }

    /**
     * Initializes the xml {@link SimpleRule} based on the {@code expression}. After this call, the predicate will be
     * set.
     *
     * @param simpleRule An xml {@link SimpleRule}.
     * @param expression A condition to set.
     */
    private void setCondition(final SimpleRule simpleRule, final PMMLPredicate expression) {
        PMMLRuleTranslator translator = new PMMLRuleTranslator();
        translator.setPredicate(simpleRule, expression);
    }

    /**
     * Initializes the {@link PMMLPortObjectSpec} based on the model, input and the used column.
     *
     * @param modelSpec The preprocessing model, can be {@code null}.
     * @param spec The input table spec.
     * @param usedColumns The columns used by the rules.
     * @return The {@link PMMLPortObjectSpec} filled with proper data for configuration.
     */
    private PMMLPortObjectSpec createPMMLPortObjectSpec(final DataTableSpec spec, final List<String> usedColumns) {
        // this assumes that the new column is always the last column in the spec; which is the case if
        // #createRearranger uses ColumnRearranger.append.
        String targetCol =
            m_settings.isReplaceColumn() ? m_settings.getReplaceColumn() : spec.getColumnSpec(spec.getNumColumns() - 1)
                .getName();
        Set<String> set = new LinkedHashSet<String>(usedColumns);
        List<String> learnCols = new LinkedList<String>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec columnSpec = spec.getColumnSpec(i);
            String col = columnSpec.getName();
            if (!col.equals(targetCol)
                && set.contains(col)
                && (columnSpec.getType().isCompatible(DoubleValue.class) || columnSpec.getType().isCompatible(
                    NominalValue.class)
                    && (/*!m_skipColumns.getBooleanValue() ||*/columnSpec.getDomain().hasValues()))) {
                learnCols.add(spec.getColumnSpec(i).getName());
            }
        }
        PMMLPortObjectSpecCreator pmmlSpecCreator = new PMMLPortObjectSpecCreator(spec);
        pmmlSpecCreator.setLearningColsNames(learnCols);
        pmmlSpecCreator.setTargetColName(targetCol);
        return pmmlSpecCreator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RuleEngine2PortsSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }

    /** {@inheritDoc} */
    @Override
    public Object readVariable(final String name, final Class<?> type) {
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return peekFlowVariableInt(name);
        } else if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return peekFlowVariableDouble(name);
        } else if (String.class.equals(type)) {
            return peekFlowVariableString(name);
        } else {
            throw new IllegalArgumentException("Invalid variable class: " + type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_rowCount;
    }
}
