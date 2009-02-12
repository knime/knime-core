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
 *   January 13, 2007 (rosaria): created from String2Smileys
 */
package org.knime.timeseries.node.stringtotimestamp;

//import java.util.LinkedList;
//import java.util.List;

//import javax.swing.JCheckBox;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
//import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This dialog lets the user choose the column that contains the string values
 * that should be converted into Smiles values. 
 * 
 * @author Rosaria Silipo
 */
public class String2DateDialog extends DefaultNodeSettingsPane {

  //  private final JCheckBox m_AppendColumn = new JCheckBox();

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    
    public String2DateDialog() {
        
        SettingsModelString columnName =
                new SettingsModelString(String2DateNodeModel.CFG_COLUMN_NAME,
                        null);

        DialogComponent columnChooser =
                new DialogComponentColumnNameSelection(columnName,
                        "Column containing strings to be converted: ", 0, StringValue.class);
        addDialogComponent(columnChooser);
        
/*        LinkedList ll = new LinkedList();
        List<String> listAllowedDateFormats = ll;
        listAllowedDateFormats.add("yyyy-MM-dd;HH:mm:ss.S");
        listAllowedDateFormats.add("yyyy-MM-dd;HH:mm:ss");
        listAllowedDateFormats.add("yyyy-MM-dd HH:mm:ss.S");
        
        SettingsModelString dateFormat =
            new SettingsModelString(String2DateNodeModel.CFG_DATE_FORMAT,
                    "yyyy-MM-dd;HH:mm:ss.S");

        DialogComponent dateFormatChooser =
            new DialogComponentStringSelection(dateFormat,
                    "TimeStamp format: ", listAllowedDateFormats);
        addDialogComponent(dateFormatChooser);
        */
        
        //org.knime.core.node.defaultnodesettings.SettingsModelStringArray
        //org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString
        //org.knime.core.node.defaultnodesettings.SettingsModelString
        
        SettingsModelString editedDateFormat =
            new SettingsModelString(String2DateNodeModel.CFG_EDITED_DATE_FORMAT,
                    "yyyy-MM-dd;HH:mm:ss.S");
       DialogComponent editString = 
            new DialogComponentString(editedDateFormat, 
                    "Edit TimeStamp format: ");
        addDialogComponent(editString);
       
        // The code of the String2Smilyes node does not have a checkbox 
        // to replace or add the transformed column.
        // Either we move to DefaultDialogPane (It looks a bit like 
        // overkilling) or we add the checkbox into the 
        // DialogComponentColumnNameSelection.
        
    }
}
