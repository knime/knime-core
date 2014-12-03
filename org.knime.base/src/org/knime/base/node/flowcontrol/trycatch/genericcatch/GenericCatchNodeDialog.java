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
 *   02.12.2014 (adae): created
 */
package org.knime.base.node.flowcontrol.trycatch.genericcatch;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog for all catch nodes.
 *
 * @author Iris Adae, University of Konstanz
 * @since 2.11.1
 */
public class GenericCatchNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Default constructor.
     */
    public GenericCatchNodeDialog() {
        final SettingsModelBoolean alwaysPopulate = getAlwaysPopulate();
        addDialogComponent(new DialogComponentBoolean(alwaysPopulate, "Always populate error variable"));
        final SettingsModelString defaultVariable = getDefaultVariable();
        addDialogComponent(new DialogComponentString(defaultVariable, "Default for \"FailingNode\" variable:"));
        final SettingsModelString defaultMessage = getDefaultMessage();
        addDialogComponent(new DialogComponentString(defaultMessage,
            "Default for \"FailingNodeMessage\" variable:"));
        final SettingsModelString defaultStackTrace = getDefaultStackTrace();
        addDialogComponent(new DialogComponentString(defaultStackTrace,
                "Default for \"FailingNodeStackTrace\" variable:"));

        alwaysPopulate.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                    activateComponents(alwaysPopulate.getBooleanValue());
            }

            private void activateComponents(final boolean doactivate) {
                defaultMessage.setEnabled(doactivate);
                defaultVariable.setEnabled(doactivate);
                defaultStackTrace.setEnabled(doactivate);
            }
        });
    }

    /**
     * @return the SM for the default text message if the node is failing.
     */
    static SettingsModelString getDefaultMessage() {
        return new SettingsModelString("CFG_DEFAULT_TEXT_MESSAGE", "none");
    }

    /**
     * @return the SM for the default variable if the is failing
     */
    static SettingsModelString getDefaultVariable() {
        return new SettingsModelString("CFG_DEFAULT_TEXT_VARIABLE", "none");
    }

    /**
     * @return the SM for always populating the model. (if true, the flow variables will always be shown)
     */
    static SettingsModelBoolean getAlwaysPopulate() {
        return new SettingsModelBoolean("CFG_ALWAYS_POPULATE", false);
    }

    /**
     * @return the SM for the default variable if the is failing
     */
    static SettingsModelString getDefaultStackTrace() {
        return new SettingsModelString("CFG_DEFAULT_STACK_TRACE", "none");
    }
}
