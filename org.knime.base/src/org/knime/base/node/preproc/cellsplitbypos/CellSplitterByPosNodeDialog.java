/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Dec 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplitbypos;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

/**
 * Dialog for the CellSplitterByPos node.
 * 
 * @author ohl, University of Konstanz
 */
public class CellSplitterByPosNodeDialog extends DefaultNodeSettingsPane {

    /**
     * The constructor. You guessed so.
     */
    @SuppressWarnings("unchecked")
    CellSplitterByPosNodeDialog() {

        createNewGroup("Splits");

        addDialogComponent(new DialogComponentString(
                CellSplitterByPosNodeModel.createSplitPointSettingsModel(),
                "Split indices, comma separated:"));

        addDialogComponent(new DialogComponentString(
                CellSplitterByPosNodeModel.createColNameSettingsModel(),
                "New column names, comma separated:"));

        createNewGroup("Target Column");

        addDialogComponent(new DialogComponentColumnNameSelection(
                CellSplitterByPosNodeModel.createColSelectSettingsModel(),
                "Column to split:", 0, true, StringValue.class));
        closeCurrentGroup();

    }
}
