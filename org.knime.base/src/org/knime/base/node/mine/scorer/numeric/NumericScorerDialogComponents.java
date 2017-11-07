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
 *   Jun 15, 2016 (oole): created
 */
package org.knime.base.node.mine.scorer.numeric;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This class holds the dialog components for the {@link NumericScorerNodeDialog}.
 *
 * @author Ole Ostergaard, KNIME.com
 * @since 3.2
 */
public class NumericScorerDialogComponents {

    private final NumericScorerSettings m_numericScorerSettings;

    private final DialogComponentColumnNameSelection m_referenceComponent;
    private final DialogComponentColumnNameSelection m_predictionComponent;
    private final DialogComponentBoolean m_overrideComponent;
    private final DialogComponentString m_outputComponent;
    private final DialogComponentBoolean m_flowVarComponent;
    private final DialogComponentString m_useNamePrefixComponent;

    private final DialogComponent[] m_components;

    /**
     * Constructor. Initializes the NumericScorer dialog components
     * @param settings the {@link NumericScorerSettings}
     */
    public NumericScorerDialogComponents(final NumericScorerSettings settings) {
        m_numericScorerSettings = settings;
        m_referenceComponent = new DialogComponentColumnNameSelection(m_numericScorerSettings.getReferenceModel(), "Reference column",  0, DoubleValue.class);
        m_predictionComponent = new DialogComponentColumnNameSelection(m_numericScorerSettings.getPredictedModel(), "Predicted column", 0, DoubleValue.class);
        m_overrideComponent = new DialogComponentBoolean(m_numericScorerSettings.getOverrideModel(), "Change column name");
        m_outputComponent = new DialogComponentString(m_numericScorerSettings.getOutputModel(), "Output column name");
        m_flowVarComponent = new DialogComponentBoolean(m_numericScorerSettings.getFlowVarModel(), "Output scores as flow variables");
        m_useNamePrefixComponent = new DialogComponentString(m_numericScorerSettings.getUseNamePrefixModel(), "Prefix of flow variables");
        m_components = new DialogComponent[] {m_referenceComponent,
            m_predictionComponent, m_overrideComponent, m_outputComponent, m_flowVarComponent, m_useNamePrefixComponent};

        m_predictionComponent.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!m_numericScorerSettings.doOverride()) {
                    m_numericScorerSettings.setOutputColumnName(m_numericScorerSettings.getPredictionColumnName());
                }
            }
        });
    }

    /**
     * Loads all the dialog component settings from the given {@link NodeSettingsRO} and {@link PortObjectSpec}s.
     *
     * @param settings the {@link NodeSettingsRO} to read from
     * @param specs input {@link PortObjectSpec}
     * @throws NotConfigurableException if the settings are invalid
     *
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        for (DialogComponent c : m_components) {
            c.loadSettingsFrom(settings, specs);
        }
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (DialogComponent c : m_components) {
            c.saveSettingsTo(settings);
        }
    }

    /**
     * Get the reference dialog component.
     *
     * @return the referenceComponent
     */
    public DialogComponentColumnNameSelection getReferenceComponent() {
        return m_referenceComponent;
    }

    /**
     * Get the prediction dialog component.
     *
     * @return the predictionComponent
     */
    public DialogComponentColumnNameSelection getPredictionComponent() {
        return m_predictionComponent;
    }

    /**
     * Get the override dialog component.
     *
     * @return the overrideComponent
     */
    public DialogComponentBoolean getOverrideComponent() {
        return m_overrideComponent;
    }

    /**
     * Get the output dialog component.
     *
     * @return the outputComponent
     */
    public DialogComponentString getOutputComponent() {
        return m_outputComponent;
    }

    /**
     * Get the flow variable dialog component.
     *
     * @return the flowVarComponent
     */
    public DialogComponentBoolean getFlowVarComponent() {
        return m_flowVarComponent;
    }

    /**
     * Get the name prefix dialog component.
     *
     * @return the useNamePrefixComponent
     */
    public DialogComponentString getUseNamePrefixComponent() {
        return m_useNamePrefixComponent;
    }

    /**
     * Return the component's underlying {@link NumericScorerSettings}.
     *
     * @return the {@link NumericScorerSettings} underlying the components
     */
    public NumericScorerSettings getSettings() {
        return m_numericScorerSettings;
    }
}
