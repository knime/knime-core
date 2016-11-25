package org.knime.base.node.rules.engine.totable;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the "Rules to Table" and the "Decision Tree to Rules" Node.
 * Converts PMML RuleSets (with <tt>firstHit</tt>) to table containing the rules.
 *
 * @author Gabor Bakos
 */
public class RulesToTableNodeDialog extends AbstractRulesToTableNodeDialog<RulesToTableSettings> {

    private SettingsModelBoolean m_scoreTableRecordCount;
    private SettingsModelBoolean m_scoreTableProbability;
    /**
     * New pane for configuring the RulesToTable node.
     */
    public RulesToTableNodeDialog() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createScoreDistribution(final RulesToTableSettings settings) {
        createNewGroup("Score distribution");
        setHorizontalPlacement(true);
        m_scoreTableRecordCount = settings.getScoreTableRecordCount();
        final SettingsModelString scoreTableRecordCountPrefix = settings.getScoreTableRecordCountPrefix();
        addDialogComponent(new DialogComponentBoolean(m_scoreTableRecordCount, "Provide score distibution record count in table with column name prefix:"));
        addDialogComponent(new DialogComponentString(scoreTableRecordCountPrefix, ""));
        setHorizontalPlacement(false);
        setHorizontalPlacement(true);
        m_scoreTableProbability = settings.getScoreTableProbability();
        final SettingsModelString scoreTableProbabilityPrefix = settings.getScoreTableProbabilityPrefix();
        addDialogComponent(new DialogComponentBoolean(m_scoreTableProbability, "Provide score distibution probability in table with column name prefix:"));
        addDialogComponent(new DialogComponentString(scoreTableProbabilityPrefix, ""));
        m_scoreTableRecordCount.addChangeListener(c -> scoreTableRecordCountPrefix.setEnabled(m_scoreTableRecordCount.isEnabled() && m_scoreTableRecordCount.getBooleanValue()));
        m_scoreTableProbability.addChangeListener(c -> scoreTableProbabilityPrefix.setEnabled(m_scoreTableProbability.isEnabled() && m_scoreTableProbability.getBooleanValue()));
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        //Necessary for proper init on first open:
        boolean oldRecCount = m_scoreTableRecordCount.getBooleanValue();
        m_scoreTableRecordCount.setBooleanValue(!oldRecCount);
        m_scoreTableRecordCount.setBooleanValue(oldRecCount);
        boolean oldProbability = m_scoreTableProbability.getBooleanValue();
        m_scoreTableProbability.setBooleanValue(!oldProbability);
        m_scoreTableProbability.setBooleanValue(oldProbability);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RulesToTableSettings createSettings() {
        return new RulesToTableSettings();
    }
}
