/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   22.12.2006 (gabriel): created
 */
package org.knime.base.node.mine.mds;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Dialog for the MDS Pivot node.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MDSPivotNodeDialogPane extends DefaultNodeSettingsPane {
    
    /**
     * Creates new dialog pane for MDS Pivot node.
     */
    @SuppressWarnings("unchecked")
    public MDSPivotNodeDialogPane() {
        final SettingsModelIntegerBounded pivotModel =
            MDSPivotNodeModel.createPivotElements();
        pivotModel.setEnabled(true);
        final SettingsModelBoolean pivotCheckbox = 
            MDSPivotNodeModel.createPivotCheckbox();
        final DialogComponentNumber pivotComponent = 
            new DialogComponentNumber(pivotModel, 
                    "No. of pivot rows used for MDS: ", 1);
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
                "Define no. of pivots"));
        addDialogComponent(pivotComponent);
        super.createNewGroup(" Column selection ");
        addDialogComponent(new DialogComponentColumnFilter(
                MDSPivotNodeModel.createColumnFilter(), 0, DoubleValue.class));
        super.createNewGroup("");
        addDialogComponent(new DialogComponentBoolean(
                MDSPivotNodeModel.createAppendColumns(), 
                " Append all column not used for MDS "));
    }
    
    /**
     * Additional check when dialogs opens.
     * @throws NotConfigurableException if spec is null or does contain at least
     *         two double-value column
     * {@inheritDoc}
     */ 
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        if (specs[0] == null) {
            throw new NotConfigurableException(
                    "No data provided at input port.");
        }
        int numCols = 0;
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            if (specs[0].getColumnSpec(i).getType().isCompatible(
                    DoubleValue.class)) {
                numCols++;
            }
        }
        if (numCols < 2) {
            throw new NotConfigurableException("Input spec must contain at "
                    + "least 2 double-value columns.");
        }
    }
    
    

}
