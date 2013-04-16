/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
package org.knime.base.node.preproc.filter.rowref;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * The dialog pane for the Reference Row Filter node which offers an
 * include and exclude option.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class RowFilterRefNodeDialogPane extends DefaultNodeSettingsPane {

    /** Include rows. */
    static final String INCLUDE = "Include rows from reference table";
    /** Exclude rows. */
    static final String EXCLUDE = "Exclude rows from reference table";

    /**
     * Creates a new dialog pane with a radio button group to shows between
     * include or exclude mode.
     */
    public RowFilterRefNodeDialogPane() {
        @SuppressWarnings("unchecked")
        final DataValueColumnFilter colFilter =
            new DataValueColumnFilter(DataValue.class);
        final DialogComponent dataTableCol =
            new DialogComponentColumnNameSelection(
                    createDataTableColModel(), "Data table column: ", 0,
                    false, colFilter);
        final DialogComponent referenceTableCol =
            new DialogComponentColumnNameSelection(
                    createReferenceTableColModel(), "Reference table column: ",
                    1, false, colFilter);
        final DialogComponentButtonGroup group = new DialogComponentButtonGroup(
                createInExcludeModel(), true, INCLUDE,
                new String[]{INCLUDE, EXCLUDE});
        group.setToolTipText("Include or exclude rows in first table "
                + "according to the second reference table.");
        createNewGroup(" Reference columns ");
        addDialogComponent(dataTableCol);
        addDialogComponent(referenceTableCol);
        closeCurrentGroup();
        addDialogComponent(group);
    }

    /**
     * @return setting model for include/exclude row IDs
     */
    static SettingsModelString createInExcludeModel() {
        return new SettingsModelString("inexclude", INCLUDE);
    }

    /**
     * @return setting model for for the column of the table to filter
     */
    static SettingsModelColumnName createDataTableColModel() {
        final SettingsModelColumnName col =
            new SettingsModelColumnName("dataTableColumn", null);
        col.setSelection(null, true);
        return col;
    }

    /**
     * @return setting model for the column of the reference table
     */
    static SettingsModelColumnName createReferenceTableColModel() {
        final SettingsModelColumnName col =
            new SettingsModelColumnName("referenceTableColumn", null);
        col.setSelection(null, true);
        return col;
    }
}
