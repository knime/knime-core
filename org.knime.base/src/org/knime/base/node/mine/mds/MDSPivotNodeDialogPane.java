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
 *   22.12.2006 (gabriel): created
 */
package org.knime.base.node.mine.mds;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MDSPivotNodeDialogPane extends DefaultNodeSettingsPane {
    
    /**
     * Creates new dialog pane for MDS pivot.
     */
    public MDSPivotNodeDialogPane() {
        final SettingsModelIntegerBounded pivotModel =
            MDSPivotNodeModel.createPivotElements();
        pivotModel.setEnabled(true);
        final SettingsModelBoolean pivotCheckbox = 
            MDSPivotNodeModel.createPivotCheckbox();
        final DialogComponentNumber pivotComponent = 
            new DialogComponentNumber(pivotModel, "#Rows used as pivots", 1);
        pivotCheckbox.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                pivotModel.setEnabled(pivotCheckbox.getBooleanValue());
            } 
        });
        super.createNewGroup(" Lower dimension ");
        addDialogComponent(new DialogComponentNumber(
                MDSPivotNodeModel.createLowerDimension(), 
                "Lower dimension", 1));
        super.createNewGroup(" Pivot elements ");
        addDialogComponent(new DialogComponentBoolean(pivotCheckbox, 
                "Define #pivots"));
        addDialogComponent(pivotComponent);
        super.createNewGroup(" Column selection ");
        addDialogComponent(new DialogComponentColumnFilter(
                MDSPivotNodeModel.createColumnFilter(), 0, DoubleValue.class));
    }
    
    

}
