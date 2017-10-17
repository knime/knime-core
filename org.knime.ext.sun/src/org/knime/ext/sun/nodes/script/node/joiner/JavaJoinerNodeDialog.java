/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   13 Jan 2017 (stefano): created
 */
package org.knime.ext.sun.nodes.script.node.joiner;

import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.multitable.MultiSpecHandler;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingCustomizer;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingJarListPanel;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingPanel;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingPanel.DialogFlowVariableProvider;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingSettings;

/**
 *
 * @author stefano
 */
public class JavaJoinerNodeDialog extends NodeDialogPane {

    private final MultiTableJavaScriptingJarListPanel m_jarPanel;
    private final MultiTableJavaScriptingPanel m_mainPanel;
    private final MultiTableJavaScriptingCustomizer m_customizer;

    /** Inits GUI.
     * @param customizer The customizer to enable/disable certain fields */
    public JavaJoinerNodeDialog(final MultiTableJavaScriptingCustomizer customizer) {
        m_customizer = customizer;
        m_jarPanel = new MultiTableJavaScriptingJarListPanel();
        m_mainPanel = new MultiTableJavaScriptingPanel(customizer,
                new DialogFlowVariableProvider() {
            @Override
            public Map<String, FlowVariable> getAvailableFlowVariables() {
                return JavaJoinerNodeDialog.this.getAvailableFlowVariables();
            }
        });
        addTab("Java Snippet", m_mainPanel, false);
        addTab("Additional Libraries", m_jarPanel, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        MultiTableJavaScriptingSettings s = m_customizer.createSettings();
        s.loadSettingsInDialog(settings, MultiSpecHandler.createJointSpec(specs[0], specs[1]));
        m_mainPanel.loadSettingsFrom(s, MultiSpecHandler.createJointSpec(specs[0], specs[1]));
        m_jarPanel.loadSettingsFrom(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        MultiTableJavaScriptingSettings s = m_customizer.createSettings();
        // first save jar panel so that settings contain additional
        // libraries that may be required for test compilation
        m_jarPanel.saveSettingsTo(s);
        m_mainPanel.saveSettingsTo(s);
        s.saveSettingsTo(settings);
    }
}
