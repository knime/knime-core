/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * History
 *   16.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.urltofilepathvariable;

import java.util.Map;

import org.knime.base.node.preproc.urltofilepathvariable.defaultnodesettings.DialogComponentFlowVariableNameSelection;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Creates the dialog of the url to file path node and provides static methods
 * which create the necessary settings models.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class UrlToFilePathVaribaleNodeDialog extends DefaultNodeSettingsPane {
  
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(UrlToFilePathVaribaleNodeDialog.class);    
    
    /**
     * Creates and returns the settings model, storing the selected variable.
     * 
     * @return The settings model with the selected column.
     */
    static final SettingsModelString getFlowVariableModel() {
        return new SettingsModelString(
                UrlToFilePathVariableConfigKeys.VARIABLE_NAME, 
                UrlToFilePathVariableNodeModel.DEF_VARNAME);
    }

    /**
     * Creates and returns the settings model, storing the "fail on invalid
     * syntax" flag.
     * 
     * @return The settings model, storing the "fail on invalid
     * syntax" flag.
     */
    static final SettingsModelBoolean getFailOnInvalidSyntaxModel() {
        return new SettingsModelBoolean(
                UrlToFilePathVariableConfigKeys.FAIL_ON_INVALID_SYNTAX,
                UrlToFilePathVariableNodeModel.DEF_FAIL_ON_INVALID_SYNTAX);
    }
   
    /**
     * Creates and returns the settings model, storing the "fail on invalid
     * location" flag.
     * 
     * @return The settings model, storing the "fail on invalid
     * location" flag.
     */
    static final SettingsModelBoolean getFailOnInvalidLocationModel() {
        return new SettingsModelBoolean(
                UrlToFilePathVariableConfigKeys.FAIL_ON_INVALID_LOCATION,
                UrlToFilePathVariableNodeModel.DEF_FAIL_ON_INVALID_LOCATION);
    }

    /**
     * Creates and returns the settings model, storing the "add prefix to
     * variable" flag.
     * 
     * @return The settings model, storing the "add prefix to variable" flag.
     */
    static final SettingsModelBoolean getAddPrefixToVariableModel() {
        return new SettingsModelBoolean(
                UrlToFilePathVariableConfigKeys.ADD_PREFIX_TO_VAR,
                UrlToFilePathVariableNodeModel.DEF_ADD_PREFIX_TO_VARIABLE);
    }    
    
    private DialogComponentFlowVariableNameSelection m_flowVarSelection;
    private SettingsModelString m_flowVarModel;
    
    /**
     * Creates new instance of <code>UrlToFilePathVaribaleNodeDialog</code>.
     */
    UrlToFilePathVaribaleNodeDialog() {
        
        // VARIABLE SELECTION
        createNewGroup("Variables");
        Map<String, FlowVariable> flowVars = getAvailableFlowVariables();
        
        m_flowVarModel = getFlowVariableModel();
        m_flowVarSelection = new DialogComponentFlowVariableNameSelection(
                m_flowVarModel, "Flow variable containing URLs", 
                flowVars.values(), FlowVariable.Type.STRING);
        
        addDialogComponent(m_flowVarSelection);
        
        addDialogComponent(new DialogComponentBoolean(
                getAddPrefixToVariableModel(), 
                "Add prefix to variable identifiers"));
        closeCurrentGroup();
        
        // FAILING
        createNewGroup("Failing behavior");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(
                getFailOnInvalidSyntaxModel(), 
                "Fail if URL has invalid syntax"));
        
        addDialogComponent(new DialogComponentBoolean(
                getFailOnInvalidLocationModel(), 
                "Fail if file does not exist"));
        closeCurrentGroup();
    }
    
    /**
     * List of available string flow variables must be updated since it could
     * have changed.
     * 
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        Map<String, FlowVariable> flowVars = getAvailableFlowVariables();
        
        // check for selected value
        String flowVar = "";
        try {
            flowVar = ((SettingsModelString)m_flowVarModel
                            .createCloneWithValidatedValue(settings))
                            .getStringValue();
        } catch (InvalidSettingsException e) {
            LOGGER.debug("Settings model could not be cloned with given " 
                    + "settings!");
        } finally {
            m_flowVarSelection.replaceListItems(flowVars.values(), flowVar);
        }
    }
}
