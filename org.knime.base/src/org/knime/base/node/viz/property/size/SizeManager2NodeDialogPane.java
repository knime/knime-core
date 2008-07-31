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
 *   30.11.2007 (gabriel): created
 */
package org.knime.base.node.viz.property.size;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.SizeModelDouble;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class SizeManager2NodeDialogPane extends DefaultNodeSettingsPane {
    
    /**
     * @return settings model for column selection.
     */
    static final SettingsModelString createColumnModel() {
        return new SettingsModelString("selected_column", null);
    }
    
    /**
     * 
     * @return settings model for scaling factor
     */
    static final SettingsModelDouble createFactorModel() {
        return new SettingsModelDoubleBounded("size_factor", 2, 1, 
                Integer.MAX_VALUE);
    }
    
    /**
     * 
     * @return settings model for mapping method
     */
    static final SettingsModelString createMappingModel() {
        return new SettingsModelString("size_mapping_method",
                SizeModelDouble.Mapping.LINEAR.name());
    }

    /**
     * Create a new size manager dialog.
     */
    @SuppressWarnings("unchecked")
    public SizeManager2NodeDialogPane() {
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColumnModel(), 
                "Column to use for size settings ", 0, 
                BoundedValue.class, DoubleValue.class));
        
        addDialogComponent(new DialogComponentNumber(
                createFactorModel(), "Scaling factor: ", 1));
        
        addDialogComponent(new DialogComponentStringSelection(
                createMappingModel(), "Select mapping method ",
                SizeModelDouble.Mapping.getStringValues()));
    }

}
