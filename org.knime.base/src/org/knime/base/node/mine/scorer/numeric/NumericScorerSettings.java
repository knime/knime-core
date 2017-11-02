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
 *   Jun 1, 2016 (oole): created
 */
package org.knime.base.node.mine.scorer.numeric;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This class holds the settings for numeric scorers like NumericScorerNodeModel.
 *
 * @author Ole Ostergaard, KNIME.com
 * @since 3.2
 */
public class NumericScorerSettings {

    static final String CFGKEY_REFERENCE = "reference";


    static final String CFGKEY_PREDICTED = "predicted";


    static final String CFGKEY_OUTPUT = "output column";

    static final String DEFAULT_OUTPUT = "";

    static final String CFGKEY_OVERRIDE_OUTPUT = "override default output name";

    static final boolean DEFAULT_OVERRIDE_OUTPUT = false;

    /**
     * The default string for the predicted
     */
    public static final String DEFAULT_PREDICTED = "";

    /**
     * The default string for the reference
     */
    public static final String DEFAULT_REFERENCE = "";

    private final SettingsModelColumnName m_referenceModel = new SettingsModelColumnName(CFGKEY_REFERENCE, DEFAULT_REFERENCE);
    private final SettingsModelColumnName m_predictedModel = new SettingsModelColumnName(CFGKEY_PREDICTED, DEFAULT_PREDICTED);
    private final SettingsModelBoolean m_overrideModel = new SettingsModelBoolean(CFGKEY_OVERRIDE_OUTPUT, DEFAULT_OVERRIDE_OUTPUT);
    private final SettingsModelString m_outputModel = new SettingsModelString(CFGKEY_OUTPUT, DEFAULT_OUTPUT);
    private final SettingsModelBoolean m_flowVarModel = new SettingsModelBoolean("generate flow variables", false);
    private final SettingsModelString m_useNamePrefixModel = createFlowPrefixModel(m_flowVarModel);



    /**
     * Constructor.
     */
    public NumericScorerSettings() {
        m_overrideModel.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_outputModel.setEnabled(m_overrideModel.getBooleanValue());
            }
        });
    }

    /**
     * @param useNamePrefixModel TODO
     * @return A new {@link SettingsModelString} for the flow variable prefix
     */
    private SettingsModelString createFlowPrefixModel(final SettingsModelBoolean useNamePrefixModel) {
        final SettingsModelString result = new SettingsModelString("name prefix for flowvars", "");
        useNamePrefixModel.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void stateChanged(final ChangeEvent e) {
                result.setEnabled(useNamePrefixModel.getBooleanValue());
            }
        });
        result.setEnabled(useNamePrefixModel.getBooleanValue());
        return result;
    }


    /**
     * Returns whether flow variables should be pushed.
     *
     * @return whether the variables should be pushed as flow variables
     */
    public boolean doFlowVariables() {
        return m_flowVarModel.getBooleanValue();
    }

    /**
     * Get the flow variable prefix.
     *
     * @return the flow variable prefix
     */
    public String getFlowVariablePrefix() {
        return m_useNamePrefixModel.getStringValue();
    }

    /**
     *
     * @return whether the column name should be replaced.
     */
    public boolean doOverride() {
        return m_overrideModel.getBooleanValue();
    }

    /**
     * Get the output column name.
     *
     * @return the output column name
     */
    public String getOutputColumnName() {
        return m_outputModel.getStringValue();
    }

    /**
     * Set the output column name
     *
     * @param columnName the output column name to be set
     */
    public void setOutputColumnName(final String columnName) {
        m_outputModel.setStringValue(columnName);
    }

    /**
     * Get the prediction column name.
     *
     * @return the prediction column name
     */
    public String getPredictionColumnName() {
        return m_predictedModel.getColumnName();
    }

    /**
     * Get the reference column name.
     *
     * @return the reference column name
     */
    public String getReferenceColumnName() {
        return m_referenceModel.getColumnName();
    }


    /**
     *  Perform on open action for the components
     */
    public void onOpen() {
        //force update of the visual state (view model)
        String columnName = m_predictedModel.getColumnName();
        m_predictedModel.setSelection(null, false);
        m_predictedModel.setSelection(columnName, false);
        boolean b = m_overrideModel.getBooleanValue();
        m_overrideModel.setBooleanValue(!b);
        m_overrideModel.setBooleanValue(b);
    }

    /**
     * Saves all the setting models to the given {@link NodeSettingsWO}.
     *
     * @param settings the {@link NodeSettingsWO} to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings){
        m_referenceModel.saveSettingsTo(settings);
        m_predictedModel.saveSettingsTo(settings);
        m_overrideModel.saveSettingsTo(settings);
        m_outputModel.saveSettingsTo(settings);

        m_useNamePrefixModel.saveSettingsTo(settings);
        m_flowVarModel.saveSettingsTo(settings);
    }



    /**
     * Loads all the setting models from from the given {@link NodeSettingsRO}.
     *
     * @param settings the {@link NodeSettingsRO} to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_referenceModel.loadSettingsFrom(settings);
        m_predictedModel.loadSettingsFrom(settings);
        m_overrideModel.loadSettingsFrom(settings);
        m_outputModel.loadSettingsFrom(settings);

        // since 3.2
        if (settings.containsKey(m_useNamePrefixModel.getKey())) {
            m_useNamePrefixModel.loadSettingsFrom(settings);
        }
        if (settings.containsKey(m_flowVarModel.getConfigName())) {
            m_flowVarModel.loadSettingsFrom(settings);
        }
    }


    /**
     * Validates all the setting models from the given {@link NodeSettingsRO}.
     *
     * @param settings the {@link NodeSettingsRO} to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_referenceModel.validateSettings(settings);
        m_predictedModel.validateSettings(settings);
        m_overrideModel.validateSettings(settings);
        m_outputModel.validateSettings(settings);


        // new since 3.2
        if (settings.containsKey(m_useNamePrefixModel.getKey())) {
            m_useNamePrefixModel.validateSettings(settings);
        }
        if (settings.containsKey(m_flowVarModel.getConfigName())) {
            m_flowVarModel.validateSettings(settings);
        }
    }


    /**
     * Get the reference settings model.
     *
     * @return the referenceModel
     */
    public SettingsModelColumnName getReferenceModel() {
        return m_referenceModel;
    }

    /**
     * Get the predicted settings model.
     *
     * @return the predictedModel
     */
    public SettingsModelColumnName getPredictedModel() {
        return m_predictedModel;
    }

    /**
     * Get the override settings model.
     *
     * @return the overrideModel
     */
    public SettingsModelBoolean getOverrideModel() {
        return m_overrideModel;
    }

    /**
     * Get the output settings model.
     *
     * @return the outputModel
     */
    public SettingsModelString getOutputModel() {
        return m_outputModel;
    }

    /**
     * Get the flow variable settings model.
     *
     * @return the flowVarModel
     */
    public SettingsModelBoolean getFlowVarModel() {
        return m_flowVarModel;
    }

    /**
     * get the name prefix settings model.
     *
     * @return the useNamePrefixModel
     */
    public SettingsModelString getUseNamePrefixModel() {
        return m_useNamePrefixModel;
    }

}
