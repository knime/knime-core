/*
 * --------------------------------------------------------------------
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
 * --------------------------------------------------------------------
 * 
 * History
 *   03.07.2007 (cebron): created
 *   01.09.2009 (adae): expanded
 */
package org.knime.base.node.preproc.double2int;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * Dialog for the double to integer Node.
 * Lets the user choose the columns to use. And the type of rounding.
 * 
 * @author cebron, University of Konstanz
 * @author adae, University of Konstanz
 */
public class DoubleToIntNodeDialog extends DefaultNodeSettingsPane {
    
    /**
     * Constructor.
     */
    public DoubleToIntNodeDialog() {
    	ColumnFilter filter = new ColumnFilter() {
    		/**
    		 * @return true, if the given column type is compatible with double 
    		 *         but not with int values
    	     */
    		@Override
    		public boolean includeColumn(final DataColumnSpec cspec) {
    			final DataType type = cspec.getType();
    			return (type.isCompatible(DoubleValue.class)
    					&& !type.isCompatible(IntValue.class));
    		}
    		/** {@inheritDoc} */
    		@Override
    		public String allFilteredMsg() {
    			return "No double-type columns available.";
    		}
    	};
        addDialogComponent(new DialogComponentButtonGroup(getCalcTypeModel(),
                false, "Rounding type", DoubleToIntNodeModel.CFG_ROUND,
                DoubleToIntNodeModel.CFG_FLOOR, DoubleToIntNodeModel.CFG_CEIL));
        addDialogComponent(new DialogComponentColumnFilter(
                new SettingsModelFilterString(
                        DoubleToIntNodeModel.CFG_INCLUDED_COLUMNS), 0, filter));
        
    }

    /**
     * @return the model for the rounding type.
     */
    public static SettingsModelString getCalcTypeModel() {
        return new SettingsModelString(DoubleToIntNodeModel.CFG_TYPE_OF_ROUND, 
                    DoubleToIntNodeModel.CFG_ROUND);
    }
}
