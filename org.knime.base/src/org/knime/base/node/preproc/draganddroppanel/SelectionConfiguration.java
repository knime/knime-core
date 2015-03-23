/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel;

import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;

import org.knime.base.node.preproc.draganddroppanel.droppanes.DropPaneConfig;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.util.Pair;

/**
 * Class which holds a all input columns and all configuration panels.
 *
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
 */
public abstract class SelectionConfiguration {
    /**
     * List model which holds the input columns. All columns in this list will not be modified.
     */
    protected DefaultListModel<String> m_inputListModel = null;


    /**
     * The indices of the added configuration panels.
     */
    protected List<Integer> m_dropPaneIndices;

    /**
     * Mapping from dropPaneIndices to configuration panel configurations.
     */
    protected HashMap<Integer, DropPaneConfig> m_panelList;

    /**
     * Index of the next added configuration panel.
     * This member will always get incremented.
     */
    protected int m_index = 0;

    /**
     * The configuration dialog factory.
     */
    protected ConfigurationDialogFactory m_fac;


    /**
     * The input column filter.
     */
    protected InputFilter<DataColumnSpec> m_filter;

    /**
     * A new SelectionConfiguration which filters the input columns.
     *
     * @param filter to filter all input columns.
     * @param fac the factory to create a new instance of the configuration dialog for each configuration panel
     */
    public SelectionConfiguration(final InputFilter<DataColumnSpec> filter, final ConfigurationDialogFactory fac ) {
        m_filter = filter;
        m_fac = fac;
        m_inputListModel = new DefaultListModel<String>();
        m_panelList = new HashMap<Integer, DropPaneConfig>();
    }

    /**
     * A new SelectionConfiguration which does not filter the input columns.
     *
     * @param fac the factory to create a new instance of the configuration dialog for each configuration panel
     */
    public SelectionConfiguration(final ConfigurationDialogFactory fac) {
        m_filter = null;
        m_fac = fac;
        m_inputListModel = new DefaultListModel<String>();
        m_panelList = new HashMap<Integer, DropPaneConfig>();
    }

    /**
     * Add a new configuration panel to the drop panel.
     *
     * @param columnName the column name which will be dropped
     * @return the index of the new added configuration panel
     */
    public abstract int drop(final String columnName);

    /**
     * @return the model of the input list
     */
    public ListModel<String> getInputListModel() {
        return m_inputListModel;
    }

    /**
     * @param columnName the column name to add to the input list
     */
    public void addElement(final String columnName) {
        m_inputListModel.addElement(columnName);
    }

    /**
     * Clear input list and panel list.
     */
    public void clear() {
        m_inputListModel.clear();
        m_panelList.clear();
    }

    /**
     * @param i set the index.
     * Note: this index should always be incremented.
     */
    public void setIndex(final int i) {
        m_index = i;
    }

    /**
     * @param settings
     * @param specs
     * @throws NotConfigurableException
     */
    public abstract void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException;

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    public abstract void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * @param settings asdf
     * @throws InvalidSettingsException asdf
     */
    public abstract void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    public abstract void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * @param settings
     */
    public abstract void saveSettings(final NodeSettingsWO settings);


    /**
     * @return
     */
    public HashMap<Integer, DropPaneConfig> getData() {
        return m_panelList;
    }

    /**
     * @param spec
     * @return
     */
    public abstract List<Pair<String, PaneConfigurationDialog>> configure(final DataTableSpec spec);

    /**
     * @param index
     */
    public void removePanel(final int index) {
        m_panelList.remove(index);
    }

}
