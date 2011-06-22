/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.quickform;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Abstract configuration, contains fields for label, description and weight.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractQuickFormConfiguration
    <V extends AbstractQuickFormValueInConfiguration> {

    private String m_label = "Label";
    private String m_description;
    private String m_variableName;
    private int m_weight;
    private V m_valueConfiguration;

    /** @return the label */
    public String getLabel() {
        return m_label;
    }
    /** @param label the label to set
     * @throws InvalidSettingsException If label is empty or null.  */
    public void setLabel(final String label) throws InvalidSettingsException {
        if (label == null || label.length() == 0) {
            throw new InvalidSettingsException("No label provided");
        }
        m_label = label;
    }
    /** @param weight the weight to set */
    public void setWeight(final int weight) {
        m_weight = weight;
    }
    /** @return the weight */
    public int getWeight() {
        return m_weight;
    }
    /** @return the description */
    public String getDescription() {
        return m_description;
    }
    /** @param description the description to set */
    public void setDescription(final String description) {
        m_description = description;
    }

    /** @return the variableName */
    public String getVariableName() {
        return m_variableName;
    }

    /** @param variableName the variableName to set */
    public void setVariableName(final String variableName) {
        m_variableName = variableName;
    }

    /** @param reference the valueConfiguration to copy from, not null. */
    public void copyValueConfigurationFrom(final V reference)
        throws InvalidSettingsException {
        V valueConfig = createValueConfiguration();
        NodeSettings settings = new NodeSettings("copy");
        reference.saveValue(settings);
        valueConfig.loadValueInModel(settings);
        m_valueConfiguration = valueConfig;
    }

    /** @return the valueConfiguration */
    public V getValueConfiguration() {
        if (m_valueConfiguration == null) {
            m_valueConfiguration = createValueConfiguration();
        }
        return m_valueConfiguration;
    }

    /** Save config to argument.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("label", m_label);
        settings.addString("description", m_description);
        settings.addString("variableName", m_variableName);
        settings.addInt("weight", m_weight);
        NodeSettingsWO valueSet = settings.addNodeSettings("value");
        m_valueConfiguration.saveValue(valueSet);
    }

    /** Load config in model.
     * @param settings To load from.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    public void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_label = settings.getString("label");
        if (m_label == null) {
            throw new InvalidSettingsException("Label must not be null");
        }
        m_description = settings.getString("description");
        m_variableName =
            verifyFlowVariableName(settings.getString("variableName"));
        m_weight = settings.getInt("weight");
        NodeSettingsRO valueSet = settings.getNodeSettings("value");
        V valueConfiguration = createValueConfiguration();
        valueConfiguration.loadValueInModel(valueSet);
        m_valueConfiguration = valueConfiguration;
    }

    /** Load settings in dialog, init defaults if that fails.
     * @param settings To load from.
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings) {
        m_label = settings.getString("label", "Label");
        if (m_label == null) {
            m_label = "Label";
        }
        m_description = settings.getString("description", "Enter Description");
        m_variableName = settings.getString("variableName", "new variable");
        m_weight = settings.getInt("weight", 1);
        try {
            verifyFlowVariableName(m_variableName);
        } catch (InvalidSettingsException ise) {
            m_variableName = "new variable";
        }
        V valueConfiguration = createValueConfiguration();
        NodeSettingsRO valueSet;
        try {
            valueSet = settings.getNodeSettings("value");
        } catch (InvalidSettingsException ise) {
            valueSet = new NodeSettings("empty");
        }
        valueConfiguration.loadValueInDialog(valueSet);
        m_valueConfiguration = valueConfiguration;
    }

    private static String verifyFlowVariableName(
            final String name) throws InvalidSettingsException {
        try {
            FlowVariable.Scope.Flow.verifyName(name);
            return name;
        } catch (Exception e) {
            throw new InvalidSettingsException("Invalid variable name \""
                    + name + "\": " + e.getMessage(), e);
        }
    }

    /**
     * Create and return a controller that shows the label and description
     * together with the actual value represented by this quickform
     * configuration.
     * @return a controller that layouts this quickform
     */
    public abstract QuickFormConfigurationPanel<V> createController();

    public abstract V createValueConfiguration();

}
