package org.knime.base.node.rules.engine.totable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod;
import org.knime.base.node.rules.engine.pmml.PMMLRuleTranslator;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * This is the model implementation of Rules to Table. Converts PMML RuleSets (with <tt>firstHit</tt>) to table containing
 * the rules.
 *
 * @author Gabor Bakos
 * @see RuleSetToTable
 * @see RulesToTableSettings
 */
class RulesToTableNodeModel extends NodeModel {
    private RulesToTableSettings m_settings = new RulesToTableSettings();

    /**
     * Constructor for the node model.
     */
    protected RulesToTableNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        PMMLPortObject ruleSetPo = (PMMLPortObject)inData[0];
        PMMLRuleTranslator translator = new PMMLRuleTranslator();
        BufferedDataTable table = new RuleSetToTable(m_settings).execute(exec, ruleSetPo);
        ruleSetPo.initializeModelTranslator(translator);
        List<RuleSelectionMethod> selectionMethodList = translator.getSelectionMethodList();
        if (selectionMethodList.size() != 1
            || (selectionMethodList.size() == 1 && !Objects.equals(selectionMethodList.get(0).getCriterion(),
                RuleSelectionMethod.Criterion.FIRST_HIT))) {
            setWarningMessage("Only a single 'firstHit' rule selection method is supported properly, all others cannot be represented properly. Rule selection methods: "
                + selectionMethodList);
        }
        return new BufferedDataTable[]{table};
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
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{new RuleSetToTable(m_settings).configure((PMMLPortObjectSpec)inSpecs[0])};
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
        m_settings.loadSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RulesToTableSettings().loadSettingsForModel(settings);
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
}
