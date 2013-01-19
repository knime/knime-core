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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.flowcontrol.breakpoint;

import java.util.Arrays;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

    private DialogComponentStringSelection m_variableName;

    /**
     *
     */
    public BreakpointNodeDialog() {
        final SettingsModelBoolean smb = createEnableModel(); 
        DialogComponentBoolean enable = new DialogComponentBoolean(
                               smb, "Breakpoint Enabled");
        addDialogComponent(enable);
        final SettingsModelString sms = createChoiceModel();
        final DialogComponentButtonGroup choices
                      = new DialogComponentButtonGroup(sms,
                               false, "Breakpoint active for:", EMTPYTABLE,
                               ACTIVEBRANCH, INACTIVEBRANCH, VARIABLEMATCH);
        addDialogComponent(choices);
        m_variableName = new DialogComponentStringSelection(
                    createVarNameModel(),
                    "Select Variable: ",
                    new String[]{"no variables available"});
        m_variableName.getModel().setEnabled(false);
        final DialogComponentString varvalue
                  = new DialogComponentString(createVarValueModel(),
                    "Enter Variable Value: ");
        // the choice control enable-status of the variable entry fields.
        sms.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                boolean useVar = VARIABLEMATCH.equals(sms.getStringValue());
                m_variableName.getModel().setEnabled(useVar);
                varvalue.getModel().setEnabled(useVar);
            }
        });
        // the enable button controls enable status of everything!
        // (but needs to keep in mind the variable choice settings)
        smb.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (smb.getBooleanValue()) {
                    choices.getModel().setEnabled(true);
                    boolean useVar = VARIABLEMATCH.equals(sms.getStringValue());
                    m_variableName.getModel().setEnabled(useVar);
                    varvalue.getModel().setEnabled(useVar);
                } else {
                    choices.getModel().setEnabled(false);
                    m_variableName.getModel().setEnabled(false);
                    varvalue.getModel().setEnabled(false);
                }
            }
        });
        varvalue.getModel().setEnabled(false);
        addDialogComponent(m_variableName);
        addDialogComponent(varvalue);
    }

    /** {@inheritDoc} */
    @Override
    public void onOpen() {
        Set<String> availableVars = this.getAvailableFlowVariables().keySet();
        if (availableVars.size() < 1) {
            m_variableName.replaceListItems(
                    Arrays.asList(new String[]{"no variables available"}),
                    null);
            m_variableName.getModel().setEnabled(false);
        } else {
            m_variableName.replaceListItems(availableVars, null);
        }
        super.onOpen();
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
