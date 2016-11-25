package org.knime.base.node.rules.engine.decisiontree;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate.BooleanOperator;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod.Criterion;
import org.dmg.pmml.RuleSetDocument.RuleSet;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.ScoreDistributionDocument.ScoreDistribution;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate.Operator.Enum;
import org.dmg.pmml.SimpleRuleDocument.SimpleRule;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLDecisionTreeTranslator;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLPredicateTranslator;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitPMML;
import org.knime.base.node.rules.engine.totable.RuleSetToTable;
import org.knime.core.data.DataCell;
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
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * This is the model implementation of Decision Tree to Rules.
 * Converts a decision tree model to PMML <a href="http://www.dmg.org/v4-2-1/RuleSet.html">RuleSet</a> model.
 *
 * @author Gabor Bakos
 */
class FromDecisionTreeNodeModel extends NodeModel {
    private final FromDecisionTreeSettings m_rulesToTable = new FromDecisionTreeSettings();

    /**
     * Constructor for the node model.
     */
    protected FromDecisionTreeNodeModel() {
        super(new PortType[] {PMMLPortObject.TYPE}, new PortType[] {PMMLPortObject.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     * @throws CanceledExecutionException Execution cancelled.
     * @throws InvalidSettingsException No or more than one RuleSet model is in the PMML input.
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        PMMLPortObject decTreeModel = (PMMLPortObject)inData[0];
        PMMLDecisionTreeTranslator treeTranslator = new PMMLDecisionTreeTranslator();
        decTreeModel.initializeModelTranslator(treeTranslator);
        DecisionTree decisionTree = treeTranslator.getDecisionTree();
        decisionTree.getRootNode();
        PMMLPortObject ruleSetModel = new PMMLPortObject(decTreeModel.getSpec());
        PMMLDocument document = PMMLDocument.Factory.newInstance();
        PMML pmml = document.addNewPMML();
        PMMLPortObjectSpec.writeHeader(pmml);
        pmml.setVersion(PMMLPortObject.PMML_V4_2);
        new PMMLDataDictionaryTranslator().exportTo(document, decTreeModel.getSpec());
        RuleSetModel newRuleSetModel = pmml.addNewRuleSetModel();
        PMMLMiningSchemaTranslator.writeMiningSchema(decTreeModel.getSpec(), newRuleSetModel);
        newRuleSetModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        newRuleSetModel.setAlgorithmName("RuleSet");
        RuleSet ruleSet = newRuleSetModel.addNewRuleSet();
        ruleSet.addNewRuleSelectionMethod().setCriterion(Criterion.FIRST_HIT);
        addRules(ruleSet, new ArrayList<DecisionTreeNode>(), decisionTree.getRootNode());
        // TODO: Return a BufferedDataTable for each output port
        PMMLPortObject pmmlPortObject = new PMMLPortObject(ruleSetModel.getSpec(), document);
        return new PortObject[]{pmmlPortObject, new RuleSetToTable(m_rulesToTable).execute(exec, pmmlPortObject)};
    }

    /**
     * Adds the rules to {@code rs} (recursively on each leaf).
     *
     * @param rs The output {@link RuleSet}.
     * @param parents The parent stack.
     * @param node The actual node.
     */
    private void addRules(final RuleSet rs, final List<DecisionTreeNode> parents, final DecisionTreeNode node) {
        if (node.isLeaf()) {
            SimpleRule rule = rs.addNewSimpleRule();
            if (m_rulesToTable.getScorePmmlRecordCount().getBooleanValue()) {
                //This increases the PMML quite significantly
                BigDecimal sum = BigDecimal.ZERO;
                final MathContext mc = new MathContext(7, RoundingMode.HALF_EVEN);
                final boolean computeProbability = m_rulesToTable.getScorePmmlProbability().getBooleanValue();
                if (computeProbability) {
                    sum = new BigDecimal(node.getClassCounts().entrySet().stream().mapToDouble(e -> e.getValue().doubleValue()).sum(), mc);
                }
                for (final Entry<DataCell, Double> entry: node.getClassCounts().entrySet()) {
                    final ScoreDistribution scoreDistrib = rule.addNewScoreDistribution();
                    scoreDistrib.setValue(entry.getKey().toString());
                    scoreDistrib.setRecordCount(entry.getValue());
                    if (computeProbability) {
                        if (Double.compare(entry.getValue().doubleValue(), 0.0) == 0) {
                            scoreDistrib.setProbability(new BigDecimal(0.0));
                        } else {
                            scoreDistrib.setProbability(new BigDecimal(entry.getValue().doubleValue(), mc).divide(sum, mc));
                        }
                    }
                }
            }
            CompoundPredicate and = rule.addNewCompoundPredicate();
            and.setBooleanOperator(BooleanOperator.AND);
            DecisionTreeNode n = node;
            do {
                PMMLPredicate pmmlPredicate = ((DecisionTreeNodeSplitPMML)n.getParent()).getSplitPred()[n.getParent().getIndex(n)];
                if (pmmlPredicate instanceof PMMLSimplePredicate) {
                    PMMLSimplePredicate simple = (PMMLSimplePredicate)pmmlPredicate;
                    SimplePredicate predicate = and.addNewSimplePredicate();
                    copy(predicate, simple);
                } else if (pmmlPredicate instanceof PMMLCompoundPredicate) {
                    PMMLCompoundPredicate compound = (PMMLCompoundPredicate)pmmlPredicate;
                    CompoundPredicate predicate = and.addNewCompoundPredicate();
                    copy(predicate, compound);
                } else if (pmmlPredicate instanceof PMMLSimpleSetPredicate) {
                    PMMLSimpleSetPredicate simpleSet = (PMMLSimpleSetPredicate)pmmlPredicate;
                    copy(and.addNewSimpleSetPredicate(), simpleSet);
                } else if (pmmlPredicate instanceof PMMLTruePredicate) {
                    and.addNewTrue();
                } else if (pmmlPredicate instanceof PMMLFalsePredicate) {
                    and.addNewFalse();
                }
                n = n.getParent();
            } while (n.getParent() != null);
            //Simple fix for the case when a single condition was used.
            while (and.getFalseList().size() + and.getCompoundPredicateList().size()
                + and.getSimplePredicateList().size() + and.getSimpleSetPredicateList().size()
                + and.getTrueList().size() < 2) {
                and.addNewTrue();
            }
            if (m_rulesToTable.getProvideStatistics().getBooleanValue()) {
                rule.setNbCorrect(node.getOwnClassCount());
                rule.setRecordCount(node.getEntireClassCount());
            }
            rule.setScore(node.getMajorityClass().toString());
        } else {
            parents.add(node);
            for (int i = 0; i< node.getChildCount(); ++i) {
                addRules(rs, parents, node.getChildAt(i));
            }
            parents.remove(node);
        }
    }

    /**
     * Copies the {@code ssp} to {@code simpleSet}.
     *
     * @param ssp A PMML xml object for {@link SimpleSetPredicate}
     * @param simpleSet A KNIME domain object for {@link PMMLSimpleSetPredicate}.
     */
    private void copy(final SimpleSetPredicate ssp, final PMMLSimpleSetPredicate simpleSet) {
        PMMLPredicateTranslator.initSimpleSetPred(simpleSet, ssp);
    }

    /**
     * Copies the {@code predicate} to {@code simple}.
     *
     * @param predicate A PMML xml object for {@link SimplePredicate}
     * @param simple A KNIME domain object for {@link PMMLSimplePredicate}.
     */
    private void copy(final SimplePredicate predicate, final PMMLSimplePredicate simple) {
        setOperator(predicate, simple);
        predicate.setField(simple.getSplitAttribute());
        predicate.setValue(simple.getThreshold());
    }

    /**
     * Copies the {@code predicate} to {@code compound}.
     *
     * @param predicate A PMML xml object for {@link CompoundPredicate}
     * @param compund A KNIME domain object for {@link PMMLCompoundPredicate}.
     */
    private void copy(final CompoundPredicate predicate, final PMMLCompoundPredicate compound) {
        PMMLPredicateTranslator.exportTo(compound, predicate);
        predicate.setBooleanOperator(PMMLPredicateTranslator.getOperator(compound.getBooleanOperator()));
        for (PMMLPredicate rawPredicate: compound.getPredicates()) {
            if (rawPredicate instanceof PMMLSimplePredicate) {
                PMMLSimplePredicate sp = (PMMLSimplePredicate)rawPredicate;
                copy(predicate.addNewSimplePredicate(), sp);
            } else if (rawPredicate instanceof PMMLCompoundPredicate) {
                PMMLCompoundPredicate cp = (PMMLCompoundPredicate)rawPredicate;
                copy(predicate.addNewCompoundPredicate(), cp);
            } else if (rawPredicate instanceof PMMLSimpleSetPredicate) {
                PMMLSimpleSetPredicate ssp = (PMMLSimpleSetPredicate)rawPredicate;
                copy(predicate.addNewSimpleSetPredicate(), ssp);
            } else if (rawPredicate instanceof PMMLTruePredicate) {
                predicate.addNewTrue();
            } else if (rawPredicate instanceof PMMLFalsePredicate) {
                predicate.addNewFalse();
            }
        }
    }

    /**
     * Sets the operator of {@code pred} based on the properties of {@code simple.}
     *
     * @param pred An xml {@link SimplePredicate}.
     * @param simple A {@link PMMLSimplePredicate}.
     */
    private void setOperator(final SimplePredicate pred, final PMMLSimplePredicate simple) {
        PMMLOperator x = simple.getOperator();
        Enum e = PMMLPredicateTranslator.getOperator(x);
        if (e == null) {
            throw new UnsupportedOperationException("Unknown operator: " + x);
        }
        pred.setOperator(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PMMLPortObjectSpec spec = (PMMLPortObjectSpec)inSpecs[0];
        return new PortObjectSpec[]{new PMMLPortObjectSpecCreator(spec).createSpec(), new RuleSetToTable(m_rulesToTable).configure(spec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rulesToTable.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_rulesToTable.loadSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new FromDecisionTreeSettings().loadSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //No internal state
    }
}

