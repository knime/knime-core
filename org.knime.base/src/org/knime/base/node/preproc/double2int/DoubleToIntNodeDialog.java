/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
