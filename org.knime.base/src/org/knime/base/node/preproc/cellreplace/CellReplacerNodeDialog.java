/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.cellreplace;

import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.preproc.cellreplace.CellReplacerNodeModel.NoMatchPolicy;
import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "CellReplacer" Node. Replaces cells in a
 * column according to dictionary table (2nd input)
 *
 * @author Bernd Wiswedel
 */
public class CellReplacerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the CellReplacer node.
     */
    @SuppressWarnings("unchecked")
    protected CellReplacerNodeDialog() {
        SettingsModelString targetColModel =
                CellReplacerNodeModel.createTargetColModel();
        SettingsModelString noMatchPolicyModel =
                CellReplacerNodeModel.createNoMatchPolicyModel();
        SettingsModelColumnName dictInputColModel =
                CellReplacerNodeModel.createDictInputColModel();
        SettingsModelColumnName dictOutputColModel =
                CellReplacerNodeModel.createDictOutputColModel();
        SettingsModelBoolean appColumnModel =
            CellReplacerNodeModel.createAppendColumnModel();
        SettingsModelString appColumnNameModel =
            CellReplacerNodeModel.createAppendColumnNameModel(appColumnModel);

        DialogComponentColumnNameSelection targetColSelector =
                new DialogComponentColumnNameSelection(targetColModel,
                        "Target column", 0, DataValue.class);

        List<String> noMatchPols = new ArrayList<String>();
        for (NoMatchPolicy p : NoMatchPolicy.values()) {
            noMatchPols.add(p.toString());
        }
        DialogComponentButtonGroup noMatchButtonGroup =
                new DialogComponentButtonGroup(noMatchPolicyModel, false,
                        "If no element matches, use: ", noMatchPols
                                .toArray(new String[noMatchPols.size()]));

        DialogComponentColumnNameSelection dictInputColSelector =
                new DialogComponentColumnNameSelection(dictInputColModel,
                        "Input (Lookup)", 1, DataValue.class);

        DialogComponentColumnNameSelection dictOutputColSelector =
                new DialogComponentColumnNameSelection(dictOutputColModel,
                        "Output (Replacement)", 1, DataValue.class);

        DialogComponentBoolean appendColumnChecker =
                new DialogComponentBoolean(appColumnModel, "Append new column");

        DialogComponentString appendColumnNameField =
                new DialogComponentString(appColumnNameModel, "");

        createNewGroup("Input table");
        addDialogComponent(targetColSelector);
        closeCurrentGroup();

        createNewGroup("Dictionary table");
        addDialogComponent(dictInputColSelector);
        addDialogComponent(dictOutputColSelector);
        closeCurrentGroup();

        createNewGroup("Append/Replace Result Column");
        setHorizontalPlacement(true);
        addDialogComponent(appendColumnChecker);
        addDialogComponent(appendColumnNameField);
        closeCurrentGroup();

        setHorizontalPlacement(false);
        addDialogComponent(noMatchButtonGroup);
    }
}
