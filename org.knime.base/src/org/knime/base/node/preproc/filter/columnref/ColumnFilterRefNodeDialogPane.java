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
 *   07.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.columnref;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnFilterRefNodeDialogPane extends DefaultNodeSettingsPane {
    
    /** Include columns. */
    static final String INCLUDE = "Include columns from reference table";
    /** Exclude columns. */
    static final String EXCLUDE = "Exclude columns from reference table";

    /**
     * 
     */
    public ColumnFilterRefNodeDialogPane() {
        DialogComponentButtonGroup group = new DialogComponentButtonGroup(
                createInExcludeModel(), 
                null, true,
                INCLUDE, new String[]{INCLUDE, EXCLUDE});
        group.setToolTipText("Include or exclude columns in first table "
                + "according to the second reference table.");
        addDialogComponent(group);
        addDialogComponent(new DialogComponentBoolean(createTypeModel(), 
                "Ensure compatibility of column types"));
    }
    
    /**
     * @return model for include/exclude button group
     */
    static SettingsModelString createInExcludeModel() {
        return new SettingsModelString("inexclude", INCLUDE);
    }
    
    /**
     * @return type compatibility model
     */
    static SettingsModelBoolean createTypeModel() {
        return new SettingsModelBoolean("type_compatibility", false);
    }
}
