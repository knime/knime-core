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
 * -------------------------------------------------------------------
 *
 * History
 *   16.11.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodesettings;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * Provides a component for column filtering. This component for the default
 * dialog allows to enter a list of columns to include from the set of available
 * columns.
 *
 * @author M. Berthold, University of Konstanz
 * @deprecated use {@link DialogComponentColumnFilter2} instead
 */
@Deprecated
public class DialogComponentColumnFilter extends DialogComponent {

    /** Index of the port to take the table (spec) from. */
    private final int m_inPortIndex;

    private final ColumnFilterPanel m_columnFilter;

    /** Table spec that was sent last into the filter component. */
    private DataTableSpec m_specInFilter;

    /**
     * Creates a new filter column panel with three components which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The settings model will hold the names of the columns
     * to be in- or excluded.
     *
     * @param model a string array model that stores the value
     * @param inPortIndex the index of the port whose table is filtered.
     *
     * @deprecated Use the constructor {@link #DialogComponentColumnFilter(
     * SettingsModelFilterString, int, boolean)} instead.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex) {
        this(model, inPortIndex, DataValue.class);
    }

    /**
     * Creates a new filter column panel with three components which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The settings model will hold the names of the columns
     * to be in- or excluded.
     *
     * @param model a string array model that stores the value
     * @param inPortIndex the index of the port whose table is filtered
     * @param showKeepAllBox if the keep-all check box should be visible
     *
     */
    @SuppressWarnings("unchecked")
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex, final boolean showKeepAllBox) {
        this(model, inPortIndex, showKeepAllBox, DataValue.class);
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
     * @deprecated Use the constructor {@link #DialogComponentColumnFilter(
     * SettingsModelFilterString, int, boolean, Class...)} instead
     */
    @Deprecated
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex,
            final Class<? extends DataValue>... allowedTypes) {
        this(model, inPortIndex, false,
                new ColumnFilterPanel.ValueClassFilter(allowedTypes));
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter. The allowed types filters out every column which is not
     * compatible with the allowed type.
     *
     * @param model a string array model that stores the value
     * @param inPortIndex the index of the port whose table is filtered
     * @param showKeepAllBox if the keep-all check box should be visible
     * @param allowedTypes filter for the columns all column not compatible with
     *            any of the allowed types are not displayed
     */
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex, final boolean showKeepAllBox,
            final Class<? extends DataValue>... allowedTypes) {
        this(model, inPortIndex, showKeepAllBox,
                new ColumnFilterPanel.ValueClassFilter(allowedTypes));
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
     * @param filter for the columns, all column not compatible with
     *            any of the allowed types are not displayed.
     *
     * @deprecated Use the constructor {@link #DialogComponentColumnFilter(
     * SettingsModelFilterString, int, boolean, ColumnFilter)} instead
     */
    @Deprecated
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex, final ColumnFilter filter) {
        this(model, inPortIndex, false, filter);
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter. The allowed types filters out every column which is not
     * compatible with the allowed type.
     *
     * @param model a string array model that stores the value
     * @param inPortIndex the index of the port whose table is filtered
     * @param showKeepAllBox if the keep-all check box should be visible
     * @param filter for the columns, all column not compatible with
     *            any of the allowed types are not displayed
     */
    public DialogComponentColumnFilter(final SettingsModelFilterString model,
            final int inPortIndex, final boolean showKeepAllBox,
            final ColumnFilter filter) {
        super(model);

        m_inPortIndex = inPortIndex;
        m_specInFilter = null;

        m_columnFilter = new ColumnFilterPanel(showKeepAllBox, filter);
        getComponentPanel().add(m_columnFilter);

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
        // to be in sync with the settings model (clear settings model)
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // update component only if content is out of sync
        final SettingsModelFilterString filterModel =
                (SettingsModelFilterString)getModel();
        final Set<String> compIncl = m_columnFilter.getIncludedColumnSet();
        final Set<String> compExcl = m_columnFilter.getExcludedColumnSet();
        final boolean compKeepAll = m_columnFilter.isKeepAllSelected();
        final Set<String> modelIncl =
            new LinkedHashSet<String>(filterModel.getIncludeList());
        final Set<String> modelExcl =
            new LinkedHashSet<String>(filterModel.getExcludeList());
        final boolean modelKeepAll = filterModel.isKeepAllSelected();

        boolean update =
                (compIncl.size() != modelIncl.size())
                        || (compExcl.size() != modelExcl.size()
                                || compKeepAll != modelKeepAll);

        if (!update) {
            // update if the current spec and the spec we last updated with
            // are different
            final PortObjectSpec currPOSpec = getLastTableSpec(m_inPortIndex);
            if (currPOSpec == null) {
                update = false;
            } else {
                if (!(currPOSpec instanceof DataTableSpec)) {
                    throw new RuntimeException("Wrong type of PortObject for"
                            + " ColumnFilterPanel, expecting DataTableSpec!");
                }
                final DataTableSpec currSpec = (DataTableSpec)currPOSpec;
                update = (!currSpec.equalStructure(m_specInFilter));
            }
        }
        if (!update) {
            // one way check, because size is equal
            update = !modelIncl.containsAll(compIncl);
        }
        if (!update) {
            // one way check, because size is equal
            update = !modelExcl.containsAll(compExcl);
        }
        if (update) {
            m_specInFilter = (DataTableSpec)getLastTableSpec(m_inPortIndex);
            if (m_specInFilter == null) {
                // the component doesn't take a null spec. Create an empty one
                m_specInFilter = new DataTableSpec();
            }
            m_columnFilter.update(m_specInFilter, true, filterModel
                    .getExcludeList());
            m_columnFilter.setKeepAllSelected(modelKeepAll);
        }

        // also update the enable status
       setEnabledComponents(filterModel.isEnabled());
    }

    /**
     * transfers the settings from the component into the settings model.
     */
    private void updateModel() {
        final boolean keepAll = m_columnFilter.isKeepAllSelected();
        final SettingsModelFilterString filterModel =
            (SettingsModelFilterString)getModel();
        final Set<String> inclList = m_columnFilter.getIncludedColumnSet();
        final Set<String> exclList = m_columnFilter.getExcludedColumnSet();

        filterModel.setNewValues(inclList, exclList, keepAll);
    }

    /**
     * We store the values from the panel in the model now.
     *
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // just in case we didn't get notified about the last change...
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // currently we open the dialog even with an empty spec - causing the
        // panel to be empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_columnFilter.setEnabled(enabled);
    }

    /**
     * @param allowedTypes filter for the columns all column not compatible with
     *            any of the allowed types are not displayed.
     */
    public void setAllowedTypes(
            final Class<? extends DataValue>... allowedTypes) {
        setColumnFilter(new DataValueColumnFilter(allowedTypes));
    }

    /**
     * @param filter for the columns, all column not compatible with
     *            any of the allowed types are not displayed
     */
    public void setColumnFilter(final ColumnFilter filter) {
        m_columnFilter.setColumnFilter(filter);
        //update the settings to be in sync with the panel
        updateModel();
    }

    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public void setIncludeTitle(final String title) {
        m_columnFilter.setIncludeTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public void setExcludeTitle(final String title) {
        m_columnFilter.setExcludeTitle(title);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_columnFilter.setToolTipText(text);
    }
}
