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
 * -------------------------------------------------------------------
 * 
 * History
 *   16.11.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import java.awt.Component;
import java.awt.Container;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ColumnFilterPanel;


/**
 * Default component for dialogs allowing to select a subset of the available
 * columns.
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnFilter extends DialogComponent {

    /* name for XML config entry holing list of included columns */
    private final String m_configName;

    /* Store the excluded or the included cols into the settings? */
    private final boolean m_storeExcluded;

    /* delegate to the column filter panel */
    private final ColumnFilterPanel m_columnFilter;
    
    
    /**
     * Creates a new filter column panel with three components which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * 
     * @param configName name of entry in setting object
     * @param label description of this panel
     * @see #update(DataTableSpec, Set, boolean)
     */
    public DialogComponentColumnFilter(final String configName,
            final String label) {
        this(configName, label, true);
    } // FilterColumnNodeDialogPanel()

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * 
     * @param configName name of entry in setting object
     * @param label description of this panel
     * @param excluded true if the excluded columns should be stored into the
     *            spec, false otherwise.
     * @see #update(DataTableSpec, Set, boolean)
     */
    @SuppressWarnings("unchecked")
    public DialogComponentColumnFilter(final String configName,
            final String label, final boolean excluded) {
        this(configName, label, excluded, DataValue.class);
    }
    
    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter. The allowed types filters out every column which is not 
     * compatible with the allowed type.
     * 
     * @param configName the config name under which the values are stored in 
     * the config.
     * @param label explanation of the component
     * @param excluded true if the excluded columns should be stored in the 
     * config, false if the included columns should be stored.
     * @param allowedTypes filter for the columns all column not compatible 
     * with any of the allowed types are not displayed.
     */
    public DialogComponentColumnFilter(final String configName, 
            final String label, final boolean excluded, 
            final Class<? extends DataValue>... allowedTypes) {
        // the label is no longer added but is kept for backward compatibility
        assert label == label;
        m_storeExcluded = excluded;
        m_configName = configName;
        m_columnFilter = new ColumnFilterPanel(allowedTypes);
        super.add(m_columnFilter);        
    }
    
    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter. The excluded columns are stored in the config.
     * The allowed types filters out every column which is not compatible with 
     * the allowed type.
     * 
     * @param configName the config name under which the values are stored in 
     * the config.
     * @param label explanation of the component 
     * config, false if the included columns should be stored.
     * @param allowedTypes filter for the columns all column not compatible 
     * with any of the allowed types are not displayed.
     */
    public DialogComponentColumnFilter(final String configName,
            final String label, 
            final Class<? extends DataValue>... allowedTypes) {
        this(configName, label, true, allowedTypes);
    }
                                                         



    /**
     * Read contents of this dialog from config file (the list of excluded
     * columns only in this case - everything else can be reconstructed from
     * that).
     * 
     * @param settings config to read from
     * @param specs table specs for the inports
     * @throws InvalidSettingsException if something fails
     * @see DialogComponent#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        LinkedHashSet<String> excl = new LinkedHashSet<String>();
        try {
            String[] excludedCells = settings.getStringArray(m_configName);
            if (excludedCells != null) {
                for (String s : excludedCells) {
                    excl.add(s);
                }
            }
        } finally {
            this.update(specs[0], excl, m_storeExcluded);
        }
    }

    /**
     * Store contents of this dialog to config file (the list of excluded
     * columns only in this case - everything else can be reconstructed from
     * that).
     * 
     * @param settings config to write to
     * @see DialogComponent#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        List<String> colList;
        if (m_storeExcluded) {
            colList = this.getExcludedColumnList();
        } else {
            colList = this.getIncludedColumnList();
        }
        String[] colCells = new String[colList.size()];
        int i = 0;
        for (String cell : colList) {
            colCells[i] = cell;
            i++;
        }
        settings.addStringArray(m_configName, colCells);
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contain all column names
     * from the spec afterwards.
     * 
     * @param spec the spec to retrieve the column names from
     * @param excl the list of columns to exclude or include
     * @param exclude the flag if <code>excl</code> contains the columns to
     *            exclude otherwise include.
     */
    public void update(final DataTableSpec spec, final Set<String> excl,
            final boolean exclude) {
        assert (spec != null && excl != null);
        m_columnFilter.update(spec, exclude, excl);
        repaint();
    }

    /**
     * Returns all columns from the exclude list.
     * 
     * @return a list of all columns from the exclude list
     */
    public List<String> getExcludedColumnList() {
        List<String> list = new LinkedList<String>();
        list.addAll(m_columnFilter.getExcludedColumnSet());
        return list;
    }

    /**
     * Returns all columns from the include list.
     * 
     * @return a list of all columns from the include list
     */
    public List<String> getIncludedColumnList() {
        List<String> list = new LinkedList<String>();
        list.addAll(m_columnFilter.getIncludedColumnSet());
        return list;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        recSetEnabledContainer(this, enabled);
    }

    private void recSetEnabledContainer(final Container cont, final boolean b) {
        cont.setEnabled(b);
        for (Component c : cont.getComponents()) {
            if (c instanceof Container) {
                recSetEnabledContainer((Container)c, b);
            } else {
                c.setEnabled(b);
            }
        }
    }
}
