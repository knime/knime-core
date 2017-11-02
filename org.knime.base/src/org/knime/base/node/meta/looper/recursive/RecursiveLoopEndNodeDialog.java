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
 *
 */
package org.knime.base.node.meta.looper.recursive;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog for the recursive loop end.
 *
 * @author Iris Adae
 */
public class RecursiveLoopEndNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Create new dialog.
     */
    public RecursiveLoopEndNodeDialog() {
        createNewGroup("End settings");

        addDialogComponent(new DialogComponentNumber(createNumOfRowsModel(), "Minimal number of rows :", 1, 10));
        addDialogComponent(new DialogComponentNumber(
                                createIterationsModel(), "Maximal number of iterations :", 10, 10));
        addDialogComponent(new DialogComponentStringSelection(createEndLoop(), "End loop :", "true", "false"));
        closeCurrentGroup();


        createNewGroup("Data settings");
        addDialogComponent(new DialogComponentBoolean(createOnlyLastModel(), "Collect data from last iteration only"));
        addDialogComponent(new DialogComponentBoolean(createAddIterationColumn(), "Add iteration column"));
        closeCurrentGroup();
    }


    /**
     * @return the SM for adding the iteration column
     */
    static SettingsModelBoolean createAddIterationColumn() {
        return new SettingsModelBoolean("CFG_AddIterationColumn", false);
    }


    /**
     * @return the SM for ending the loop
     */
    static SettingsModelString createEndLoop() {
        return new SettingsModelString("CFG_End_Loop", "false");
    }


    /**
     *
     * @return the settings model for the maximal number of iterations.
     */
    static  SettingsModelIntegerBounded createIterationsModel() {
        return new SettingsModelIntegerBounded("CFG_MaxNrIterations", 100, 1, Integer.MAX_VALUE);
    }

    /**
     *
     * @return the settings model for the minimal number of rows.
     */
    static  SettingsModelInteger createNumOfRowsModel() {
        return new SettingsModelInteger("CFG_MinNrOfRows", 1);
    }

    /**
     * @return the settings model that contains the only last result flag
     */
    static SettingsModelBoolean createOnlyLastModel() {
        return new SettingsModelBoolean("CFG_OnlyLastData", false);
    }


}
