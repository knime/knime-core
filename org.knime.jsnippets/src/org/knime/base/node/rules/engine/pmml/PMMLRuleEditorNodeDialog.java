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
 * Created on 2013.08.11. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.RulePanel;
import org.knime.base.node.rules.engine.rsyntax.AbstractRuleParser;
import org.knime.base.node.rules.engine.rsyntax.PMMLRuleParser;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the "PMML41RuleEditor" Node. Edits PMML RuleSets.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Gabor Bakos
 */
public class PMMLRuleEditorNodeDialog extends NodeDialogPane {
    static {
        new org.knime.base.node.rules.engine.rsyntax.PMMLRuleParser.PMMLRuleLanguageSupport();
    }

    /** Default default label. */
    static final String DEFAULT_LABEL = "default";

    /** Default name for the newly appended column. */
    static final String NEW_COL_NAME = "prediction";

    /** The default text for the rule editor. */
    public static final String RULE_LABEL = "// enter ordered set of rules, e.g.:\n"
        + "// $double column name$ > 5.0 => \"large\"\n" + "// $string column name$ = \"blue\" => \"small and blue\"\n"
        + "// TRUE => \"default outcome\"\n";

    private RulePanel m_rulePanel;

    /**
     * New pane for configuring the PMML41RuleEditor node.
     */
    protected PMMLRuleEditorNodeDialog() {
        initializeComponent();
//        this(true, false, true);
    }

    private void initializeComponent() {
        addTab("Rule Editor", m_rulePanel = createRulePanel());
    }

    /**
     * @return The {@link RulePanel}.
     */
    private RulePanel createRulePanel() {
        return new RulePanel(RuleNodeSettings.PMMLRule) {
            private static final long serialVersionUID = 1989431706527387707L;

            /**
             * {@inheritDoc}
             */
            @Override
            protected AbstractRuleParser createParser(final boolean warnOnColRefsInStrings,
                final RuleNodeSettings nodeType) {
                return new PMMLRuleParser();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        //final DataTableSpec[] specsMod = new DataTableSpec[]{PMMLRuleEditorNodeModel.computeSpecs(specs)};
        m_rulePanel.loadSettingsFrom(settings, new DataTableSpec[] {(DataTableSpec)specs[0]}, getAvailableFlowVariables());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_rulePanel.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        // Important to not close on esc, because after that the JSyntaxTextArea do not work properly.
        return false;
    }
}
