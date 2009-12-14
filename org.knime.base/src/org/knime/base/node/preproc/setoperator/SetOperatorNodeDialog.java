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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *    22.11.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SetOperatorNodeDialog extends DefaultNodeSettingsPane {


    private final SettingsModelColumnName m_col1 =
        new SettingsModelColumnName(SetOperatorNodeModel.CFG_COL1, null);
    private final SettingsModelColumnName m_col2 =
        new SettingsModelColumnName(SetOperatorNodeModel.CFG_COL2, null);

    private final SettingsModelString m_setOp;

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(SetOperatorNodeModel.CFG_ENABLE_HILITE, false);

    private final SettingsModelBoolean m_sortInMemory;

    private final SettingsModelBoolean m_skipMissing;


    /**Constructor for claprivaterNodeDialog.
     *
     */
    public SetOperatorNodeDialog() {
        m_setOp = new SettingsModelString(SetOperatorNodeModel.CFG_OP,
                SetOperation.getDefault().getName());
        m_sortInMemory = new SettingsModelBoolean(
                SetOperatorNodeModel.CFG_SORT_IN_MEMORY, false);
        m_skipMissing = new SettingsModelBoolean(
                SetOperatorNodeModel.CFG_SKIP_MISSING, true);
        final DialogComponent col1 =
                new DialogComponentColumnNameSelection(m_col1, "First set:", 0,
                        DataValue.class);
        final DialogComponent col2 =
            new DialogComponentColumnNameSelection(m_col2, "Second set:", 1,
                    DataValue.class);
        final DialogComponentStringSelection ops =
                new DialogComponentStringSelection(m_setOp, "Operation",
                        SetOperation.values());
        final DialogComponent enableHilite = new DialogComponentBoolean(
                m_enableHilite, "Enable hiliting");
        final DialogComponent sortInMemory = new DialogComponentBoolean(
                m_sortInMemory, "Sort in memory");
        final DialogComponent skipMissing = new DialogComponentBoolean(
                m_skipMissing, "Skip missing values");
        addDialogComponent(col1);
        addDialogComponent(ops);
        addDialogComponent(col2);
        setHorizontalPlacement(true);
        addDialogComponent(enableHilite);
        addDialogComponent(skipMissing);
        addDialogComponent(sortInMemory);
    }
}
