/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   07.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.columnref;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog pane to filter column which offers options to include or
 * exclude column and two check column type compatibility.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnFilterRefNodeDialogPane extends DefaultNodeSettingsPane {
    
    /** Include columns. */
    static final String INCLUDE = "Include columns from reference table";
    /** Exclude columns. */
    static final String EXCLUDE = "Exclude columns from reference table";

    /**
     * Creates a new dialog pane with the option to include or exclude column
     * and to optionally check to column compatibility.
     */
    public ColumnFilterRefNodeDialogPane() {
        DialogComponentButtonGroup group = new DialogComponentButtonGroup(
                createInExcludeModel(), 
                true, INCLUDE, new String[]{INCLUDE, EXCLUDE});
        group.setToolTipText("Include or exclude columns in first table "
                + "according to the second reference table.");
        addDialogComponent(group);
        addDialogComponent(new DialogComponentBoolean(createTypeModel(), 
                "Ensure compatibility of column types"));
    }
    
    /**
     * @return settings model for include/exclude columns
     */
    static SettingsModelString createInExcludeModel() {
        return new SettingsModelString("inexclude", INCLUDE);
    }
    
    /**
     * @return settings model for column type compatibility
     */
    static SettingsModelBoolean createTypeModel() {
        return new SettingsModelBoolean("type_compatibility", false);
    }
}
