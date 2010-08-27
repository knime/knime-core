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
