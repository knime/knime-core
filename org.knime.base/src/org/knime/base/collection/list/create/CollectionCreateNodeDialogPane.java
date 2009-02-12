/* ------------------------------------------------------------------
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
 *   Aug 11, 2008 (wiswedel): created
 */
package org.knime.base.collection.list.create;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class CollectionCreateNodeDialogPane extends DefaultNodeSettingsPane {

    /**
     *
     */
    public CollectionCreateNodeDialogPane() {
        SettingsModelFilterString m =
                CollectionCreateNodeModel.createSettingsModel();
        DialogComponent dc = new DialogComponentColumnFilter(m, 0);
        addDialogComponent(dc);

        createNewGroup("Collection type");
        SettingsModelBoolean t =
                CollectionCreateNodeModel.createSettingsModelSetOrList();
        DialogComponentBoolean type =
                new DialogComponentBoolean(t,
                        "Create a collection of type 'set' "
                        + "(doesn't store duplicate values)");
        addDialogComponent(type);
        closeCurrentGroup();

        createNewGroup("Output table structure");
        SettingsModelBoolean remCols =
            CollectionCreateNodeModel.createSettingsModelRemoveCols();
        DialogComponentBoolean remove =
            new DialogComponentBoolean(remCols,
                    "Remove aggregated column from table");
        addDialogComponent(remove);
        SettingsModelString colName =
            CollectionCreateNodeModel.createSettingsModelColumnName();
        DialogComponentString col = new DialogComponentString(colName,
                "Enter the name of the new column:", true, 25);
        addDialogComponent(col);
    }
}
