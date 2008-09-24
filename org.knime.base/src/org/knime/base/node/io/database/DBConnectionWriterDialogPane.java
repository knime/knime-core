/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 */
package org.knime.base.node.io.database;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog pane of the database connection writer.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBConnectionWriterDialogPane extends DefaultNodeSettingsPane {
    
    
    /**
     * Creates new dialog.
     */
    DBConnectionWriterDialogPane() {
        super.addDialogComponent(new DialogComponentString(
                createTableNameModel(), "Table name"));
    }
    
    /**
     * Create model for table name.
     * @return string model for table name
     */
    static final SettingsModelString createTableNameModel() {
        return new SettingsModelString("table_name", "");
    }
    
}

