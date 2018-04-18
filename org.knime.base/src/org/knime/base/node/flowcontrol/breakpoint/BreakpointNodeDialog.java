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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.flowcontrol.breakpoint;

import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author M. Berthold, University of Konstanz
 */
public class BreakpointNodeDialog extends DefaultNodeSettingsPane {

    /** break on table with zero rows. */
    static final String EMTPYTABLE = "empty table";

    /** break on active branch. */
    static final String ACTIVEBRANCH = "active branch";

    /** break on inactive branch. */
    static final String INACTIVEBRANCH = "inactive branch";

    /** break on variable having given value. */
    static final String VARIABLEMATCH = "variable matches value";

    private final SettingsModelBoolean m_enableModel = createEnableModel();

    private final SettingsModelString m_choicesModel = createChoiceModel();

    private final SettingsModelString m_varNameModel = createVarNameModel();

    private final SettingsModelString m_varValueModel = createVarValueModel();

    private final DialogComponentStringSelection m_variableName;

    private boolean m_varsAvailable;

    private String m_varValueAtOpen;

    /**
     * Creates the dialog of the Breakpoint node.
     */
    public BreakpointNodeDialog() {
        final DialogComponentBoolean enable = new DialogComponentBoolean(m_enableModel, "Breakpoint Enabled");
        addDialogComponent(enable);
        final DialogComponentButtonGroup choices = new DialogComponentButtonGroup(m_choicesModel, false,
            "Breakpoint active for:", EMTPYTABLE, ACTIVEBRANCH, INACTIVEBRANCH, VARIABLEMATCH);
        addDialogComponent(choices);
        m_variableName =
            new DialogComponentStringSelection(m_varNameModel, "Select Variable: ", "no variables available");
        m_varNameModel.setEnabled(false);
        final DialogComponentString varvalue = new DialogComponentString(m_varValueModel, "Enter Variable Value: ");
        // the choice control enable-status of the variable entry fields.
        m_choicesModel.addChangeListener(e -> {
            final boolean useVar = VARIABLEMATCH.equals(m_choicesModel.getStringValue());
            m_varNameModel.setEnabled(useVar && m_varsAvailable);
            m_varValueModel.setEnabled(useVar && m_varsAvailable);
        });
        // the enable button controls enable status of everything!
        // (but needs to keep in mind the variable choice settings)
        m_enableModel.addChangeListener(e -> {
            if (m_enableModel.getBooleanValue()) {
                m_choicesModel.setEnabled(true);
                final boolean useVar = m_varsAvailable && VARIABLEMATCH.equals(m_choicesModel.getStringValue());
                m_varNameModel.setEnabled(useVar);
                m_varValueModel.setEnabled(useVar);
            } else {
                m_choicesModel.setEnabled(false);
                m_varNameModel.setEnabled(false);
                m_varValueModel.setEnabled(false);
            }
        });
        addDialogComponent(m_variableName);
        addDialogComponent(varvalue);
    }

    /** {@inheritDoc} */
    @Override
    public void onOpen() {
        final Set<String> availableVars = this.getAvailableFlowVariables().keySet();
        if (availableVars.isEmpty()) {
            m_varsAvailable = false;
            m_varNameModel.setEnabled(false);
            m_varValueModel.setEnabled(false);
        } else {
            m_varsAvailable = true;
            /**
             * because we replace the list items we need to store the original value to be able to restore it later,
             * this ensures the correct value is displayed if the selected flowvariable becomes available again.
             */
            m_varValueAtOpen = m_varNameModel.getStringValue();
            m_variableName.replaceListItems(availableVars, m_varValueAtOpen);
            m_choicesModel.setEnabled(m_enableModel.getBooleanValue());
            final boolean varsEnabled =
                m_enableModel.getBooleanValue() && VARIABLEMATCH.equals(m_choicesModel.getStringValue());
            m_varNameModel.setEnabled(varsEnabled);
            m_varValueModel.setEnabled(varsEnabled);
        }
        super.onOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_varNameModel.setStringValue(m_varValueAtOpen);
    }

    /**
     * @return settings model (choice) for node and dialog.
     */
    static SettingsModelString createChoiceModel() {
        return new SettingsModelString("BreakPoint", EMTPYTABLE);
    }

    /**
     * @return settings model (enable) for node and dialog.
     */
    static SettingsModelBoolean createEnableModel() {
        return new SettingsModelBoolean("Enabled", false);
    }

    /**
     * @return settings model (choice) for node and dialog.
     */
    static SettingsModelString createVarNameModel() {
        return new SettingsModelString("Variable Name", "");
    }

    /**
     * @return settings model (choice) for node and dialog.
     */
    static SettingsModelString createVarValueModel() {
        return new SettingsModelString("Variable Value", "0");
    }
}
