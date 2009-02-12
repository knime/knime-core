/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
