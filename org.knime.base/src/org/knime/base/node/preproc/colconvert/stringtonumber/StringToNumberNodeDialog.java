 /* This source code, its documentation and all appendant files
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
 * 
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert.stringtonumber;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * Dialog for the String to Number Node.
 * Lets the user choose the columns to use.
 * 
 * @author cebron, University of Konstanz
 */
public class StringToNumberNodeDialog extends DefaultNodeSettingsPane {
    
    /**
     * Constructor.
     *
     */
    @SuppressWarnings("unchecked")
    public StringToNumberNodeDialog() {
        addDialogComponent(new DialogComponentColumnFilter(
                new SettingsModelFilterString(
                        StringToNumberNodeModel.CFG_INCLUDED_COLUMNS), 0,
                new Class[]{StringValue.class}));
    }
}
