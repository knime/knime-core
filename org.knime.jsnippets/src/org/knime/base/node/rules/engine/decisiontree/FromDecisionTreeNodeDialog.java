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
 *   9 May 2016 (Gabor Bakos): created
 */
package org.knime.base.node.rules.engine.decisiontree;

import org.knime.base.node.rules.engine.totable.AbstractRulesToTableNodeDialog;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog for the Decision tree to Ruleset node.
 *
 * @author Gabor Bakos
 * @since 3.2
 */
class FromDecisionTreeNodeDialog extends AbstractRulesToTableNodeDialog<FromDecisionTreeSettings> {

    private SettingsModelBoolean m_scorePmmlRecordCount;

    /**
     * Constructor
     */
    FromDecisionTreeNodeDialog() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createScoreDistribution(final FromDecisionTreeSettings settings) {
        createNewGroup("Score distribution");
        setHorizontalPlacement(true);
        //Record count
        m_scorePmmlRecordCount = settings.getScorePmmlRecordCount();
        final SettingsModelBoolean scoreTableRecordCount = settings.getScoreTableRecordCount();
        final SettingsModelString scoreTableRecordCountPrefix = settings.getScoreTableRecordCountPrefix();
        addDialogComponent(new DialogComponentBoolean(m_scorePmmlRecordCount, "Provide score distibution record count in PMML"));
        addDialogComponent(new DialogComponentBoolean(scoreTableRecordCount, "Provide score distibution record count in table with column name prefix:"));
        addDialogComponent(new DialogComponentString(scoreTableRecordCountPrefix, ""));
        setHorizontalPlacement(false);
        setHorizontalPlacement(true);
        //Probability
        final SettingsModelBoolean scorePmmlProbability = settings.getScorePmmlProbability();
        final SettingsModelBoolean scoreTableProbability = settings.getScoreTableProbability();
        final SettingsModelString scoreTableProbabilityPrefix = settings.getScoreTableProbabilityPrefix();
        addDialogComponent(new DialogComponentBoolean(scorePmmlProbability, "Provide score distibution probability in PMML"));
        addDialogComponent(new DialogComponentBoolean(scoreTableProbability, "Provide score distibution probability in table with column name prefix:"));
        addDialogComponent(new DialogComponentString(scoreTableProbabilityPrefix, ""));
        m_scorePmmlRecordCount.addChangeListener(c -> scoreTableRecordCount.setEnabled(m_scorePmmlRecordCount.getBooleanValue()));
        m_scorePmmlRecordCount.addChangeListener(c -> scorePmmlProbability.setEnabled(m_scorePmmlRecordCount.getBooleanValue()));
        scoreTableRecordCount.addChangeListener(c -> scoreTableRecordCountPrefix.setEnabled(scoreTableRecordCount.isEnabled() && scoreTableRecordCount.getBooleanValue()));
        scorePmmlProbability.addChangeListener(c -> scoreTableProbability.setEnabled(scorePmmlProbability.isEnabled() && scorePmmlProbability.getBooleanValue()));
        scoreTableProbability.addChangeListener(c -> scoreTableProbabilityPrefix.setEnabled(scoreTableProbability.isEnabled() && scoreTableProbability.getBooleanValue()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        //Necessary for proper init on first open:
        boolean old = m_scorePmmlRecordCount.getBooleanValue();
        m_scorePmmlRecordCount.setBooleanValue(!old);
        m_scorePmmlRecordCount.setBooleanValue(old);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FromDecisionTreeSettings createSettings() {
        return new FromDecisionTreeSettings();
    }
}
