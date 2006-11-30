/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
package org.knime.core.node.defaultnodesettings;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Provides a component for column filtering. This component for the default
 * dialog allows to enter a list of columns to include from the set of available
 * columns.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnFilter extends DialogComponent {

    /* the index of the port to take the table (spec) from. */
    private final int m_inPortIndex;

    private ColumnFilterPanel m_columnFilter;

    /**
     * Creates a new filter column panel with three components which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The settings model will hold the names of the columns
     * to be in- or excluded.
     * 
     * @param model a string array model that stores the value
     * @param inPortIndex the index of the port whose table is filtered.
     * 
     */
    @SuppressWarnings("unchecked")
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex) {
        this(model, inPortIndex, DataValue.class);
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter. The allowed types filters out every column which is not
     * compatible with the allowed type.
     * 
     * @param model a string array model that stores the value
     * @param inPortIndex the index of the port whose table is filtered.
     * @param allowedTypes filter for the columns all column not compatible with
     *            any of the allowed types are not displayed.
     */
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex,
            final Class<? extends DataValue>... allowedTypes) {
        super(model);

        m_inPortIndex = inPortIndex;

        m_columnFilter = new ColumnFilterPanel(allowedTypes);
        super.add(m_columnFilter);

        // when the user input changes we need to update the model.
        m_columnFilter.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateModel();
            }
        });
        // update the components, when the value in the model changes
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #updateComponent()
     */
    @Override
    void updateComponent() {
        // update component only if content is out of sync
        boolean update = false;
        SettingsModelFilterString filterModel =
                (SettingsModelFilterString)getModel();
        Set<String> compIncl = m_columnFilter.getIncludedColumnSet();
        Set<String> compExcl = m_columnFilter.getExcludedColumnSet();
        List<String> modelIncl = filterModel.getIncludeList();
        List<String> modelExcl = filterModel.getExcludeList();
        for (String s : compIncl) {
            if (!modelIncl.contains(s)) {
                update = true;
                break;
            }
        }
        if (!update) {
            for (String s : compExcl) {
                if (!modelExcl.contains(s)) {
                    update = true;
                    break;
                }
            }
        }
        if (update) {
            m_columnFilter.update(getLastTableSpec(m_inPortIndex), true,
                    filterModel.getExcludeList());
        }
    }

    /**
     * transfers the settings from the component into the settings model.
     */
    private void updateModel() {
        Set<String> inclList = m_columnFilter.getIncludedColumnSet();
        Set<String> exclList = m_columnFilter.getExcludedColumnSet();

        SettingsModelFilterString filterModel =
                (SettingsModelFilterString)getModel();
        filterModel.setIncludeList(inclList);
        filterModel.setExcludeList(exclList);
    }

    /**
     * We store the values from the panel in the model now.
     * 
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        // just in case we didn't get notified about the last change...
        updateModel();
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // currently we open the dialog even with spec - causing the panel
        // to be empty.
    }

    /**
     * @see DialogComponent #setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        recSetEnabledContainer(m_columnFilter, enabled);
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
