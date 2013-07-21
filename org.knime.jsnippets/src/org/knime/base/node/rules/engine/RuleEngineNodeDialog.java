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
 * History
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules.engine;

import org.fife.rsta.ac.LanguageSupportFactory;
import org.knime.base.node.rules.engine.manipulator.RuleManipulatorProvider;
import org.knime.base.node.util.JavaScriptingCompletionProvider;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Rule Engine node dialog, but also usable for rule engine filter/splitter.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Gabor Bakos
 * @since 2.8
 */
public class RuleEngineNodeDialog extends NodeDialogPane {
    static {
        new RuleParser.RuleLanguageSupport();
        LanguageSupportFactory.get().addLanguageSupport(RuleParser.SYNTAX_STYLE_RULE,
                                                        RuleParser.RuleLanguageSupport.class.getName());
    }

    /** Default default label. */
    static final String DEFAULT_LABEL = "default";

    /** Default name for the newly appended column. */
    static final String NEW_COL_NAME = "prediction";

    /** The default text for the rule editor. */
    static final String RULE_LABEL = "Enter condition...\n";

    private final boolean m_hasOutputColumn;

    private final boolean m_hasDefaultOutcome;

    private final boolean m_warnOnColRefsInStrings;

    private final String m_inclusionLabel;

    private final String m_exclusionLabel;

    private RulePanel m_rulePanel;

    /**
     * Constructs the default {@link RuleEngineNodeDialog}.
     */
    public RuleEngineNodeDialog() {
        this(true, true, true, null, null);
    }

    /**
     * Constructs a rule engine filter dialog with the {@code inclusion} and {@code exclusion} labels.
     *
     * @param inclusion Label for the option: match goes to the first output port.
     * @param exclusion Label for the option: match goes to the second (probably not existing) port.
     */
    public RuleEngineNodeDialog(final String inclusion, final String exclusion) {
        this(false, false, false, inclusion, exclusion);
    }

    /**
     * Constructs a {@link RuleEngineNodeDialog}.
     *
     * @param hasOutputColumn Whether there should be a control for output column name.
     * @param hasDefaultOutcome Whether there should be an option for default outcome.
     * @param warnOnColRefsInStrings Whether to warn if there are column references in the outcome strings.
     * @param inclusion The label for the inclusion (only when no output column and no default outcome is specified).
     * @param exclusion The label for the exclusion (only when no output column and no default outcome is specified).
     */
    RuleEngineNodeDialog(final boolean hasOutputColumn, final boolean hasDefaultOutcome,
                         final boolean warnOnColRefsInStrings, final String inclusion, final String exclusion) {
        this.m_hasOutputColumn = hasOutputColumn;
        this.m_hasDefaultOutcome = hasDefaultOutcome;
        this.m_warnOnColRefsInStrings = warnOnColRefsInStrings;
        if (hasOutputColumn != hasDefaultOutcome) {
            throw new UnsupportedOperationException(
                    "Please review the code when you want output column without default outcome!");
        }
        m_inclusionLabel = inclusion;
        m_exclusionLabel = exclusion;
        initializeComponent();
    }

    private void initializeComponent() {
        addTab("Rule Editor", m_rulePanel = createRulePanel());
    }

    /**
     * @return The {@link RulePanel}.
     */
    private RulePanel createRulePanel() {
        return new RulePanel(m_hasOutputColumn, m_hasDefaultOutcome, m_warnOnColRefsInStrings, m_inclusionLabel,
                m_exclusionLabel, RuleManipulatorProvider.getProvider(), new JavaScriptingCompletionProvider()) {
            private static final long serialVersionUID = 5022739491267638254L;

            @Override
            protected FlowVariableModel createFlowVariableModel() {
                return RuleEngineNodeDialog.this.createFlowVariableModel(RuleEngineSettings.CFG_DEFAULT_LABEL,
                                                                         Type.STRING);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        m_rulePanel.loadSettingsFrom(settings, specs, getAvailableFlowVariables());
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
