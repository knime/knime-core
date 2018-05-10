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
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script;

import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingCustomizer;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingJarListPanel;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingPanel;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingPanel.DialogFlowVariableProvider;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JavaScriptingNodeDialog extends NodeDialogPane {

    private final JavaScriptingJarListPanel m_jarPanel;
    private final JavaScriptingPanel m_mainPanel;
    private final JavaScriptingCustomizer m_customizer;

    /** Inits GUI.
     * @param customizer The customizer to enable/disable certain fields */
    public JavaScriptingNodeDialog(final JavaScriptingCustomizer customizer) {
        m_customizer = customizer;
        m_jarPanel = new JavaScriptingJarListPanel();
        m_mainPanel = new JavaScriptingPanel(customizer,
                new DialogFlowVariableProvider() {
            @Override
            public Map<String, FlowVariable> getAvailableFlowVariables() {
                return JavaScriptingNodeDialog.this.getAvailableFlowVariables();
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
        JavaScriptingSettings s = m_customizer.createSettings();
        s.loadSettingsInDialog(settings, specs[0]);
        m_mainPanel.loadSettingsFrom(s, specs[0]);
        m_jarPanel.loadSettingsFrom(s);
        s.discard();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        JavaScriptingSettings s = m_customizer.createSettings();
        // first save jar panel so that settings contain additional
        // libraries that may be required for test compilation
        m_jarPanel.saveSettingsTo(s);
        m_mainPanel.saveSettingsTo(s);
        s.saveSettingsTo(settings);
        s.discard();
    }
}
