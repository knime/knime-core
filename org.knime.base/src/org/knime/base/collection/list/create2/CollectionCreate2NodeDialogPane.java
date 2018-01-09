/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 11, 2008 (wiswedel): created
 */
package org.knime.base.collection.list.create2;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class CollectionCreate2NodeDialogPane extends DefaultNodeSettingsPane {

    /**
     * Needed to check the settings while saving.
     */
    private SettingsModelColumnFilter2 m_columnFilterModel;

    /**
     * Needed to apply the settings.
     */
    private DataTableSpec m_inSpec;

    /**
     *
     */
    public CollectionCreate2NodeDialogPane() {
        m_columnFilterModel =
                CollectionCreate2NodeModel.createSettingsModel();
        DialogComponent dc = new DialogComponentColumnFilter2(m_columnFilterModel, 0);
        addDialogComponent(dc);

        createNewGroup("Collection type");
        SettingsModelBoolean t =
                CollectionCreate2NodeModel.createSettingsModelSetOrList();
        DialogComponentBoolean type =
                new DialogComponentBoolean(t,
                        "Create a collection of type 'set' "
                        + "(doesn't store duplicate values)");
        addDialogComponent(type);

        SettingsModelBoolean ignoreMissingModel =
            CollectionCreate2NodeModel.createSettingsModelIgnoreMissing();
        DialogComponentBoolean ignoreMissing = new DialogComponentBoolean(
                ignoreMissingModel, "ignore missing values");
        addDialogComponent(ignoreMissing);

        closeCurrentGroup();

        createNewGroup("Output table structure");
        SettingsModelBoolean remCols =
            CollectionCreate2NodeModel.createSettingsModelRemoveCols();
        DialogComponentBoolean remove =
            new DialogComponentBoolean(remCols,
                    "Remove aggregated columns from table");
        addDialogComponent(remove);
        SettingsModelString colName =
            CollectionCreate2NodeModel.createSettingsModelColumnName();
        DialogComponentString col = new DialogComponentString(colName,
                "Enter the name of the new column:", true, 25);
        addDialogComponent(col);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        // Check if current settings applied to current specs have at least one include
        if (m_columnFilterModel.applyTo(m_inSpec).getIncludes().length < 1) {
            throw new InvalidSettingsException("At least one column needs to be selected");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        m_inSpec = specs[0];
    }

}
