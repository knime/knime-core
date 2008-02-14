/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   21.09.2007 (gabriel): created
 */
package org.knime.base.node.io.database;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DBQueryNodeDialogPane extends DefaultNodeSettingsPane {
    
    /**
     * Create query dialog with text box to enter table name.
     */
    public DBQueryNodeDialogPane() {
        super.addDialogComponent(new DialogComponentMultiLineString(
                createQueryModel(), "SQL query")); 
    }
    
    /**
     * Create model to enter SQL statement on input database view.
     * @return a new model to enter SQL statement
     */
    static final SettingsModelString createQueryModel() {
        return new SettingsModelString("SQL_query", "SELECT * FROM "
                + DBQueryNodeModel.VIEW_PLACE_HOLDER);
    }
}
