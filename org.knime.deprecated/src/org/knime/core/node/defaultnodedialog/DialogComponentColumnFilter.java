/* 
 * -------------------------------------------------------------------
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
