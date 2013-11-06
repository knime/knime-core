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
 */
package org.knime.core.node.util.filter.column;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterPanel;

/**
 * A panel to filter {@link DataColumnSpec}s.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 */
@SuppressWarnings("serial")
public class DataColumnSpecFilterPanel extends NameFilterPanel<DataColumnSpec> {

    private final InputFilter<DataColumnSpec> m_filter;

    private DataTableSpec m_spec;

    private TypeFilterPanelImpl m_typePanel;

    private JRadioButton m_typeButton;

    private Boolean m_typeFilterEnabled = null;

    /**
     * Create a new panel to filter {@link DataColumnSpec}s.
     */
    public DataColumnSpecFilterPanel() {
        super();
        m_filter = null;
        init();
    }

    /**
     * Create a new panel to filter {@link DataColumnSpec}s.
     *
     * @param showSelectionListsOnly if true, the panel shows no additional options like search box,
     *            force-include-option, etc.
     */
    public DataColumnSpecFilterPanel(final boolean showSelectionListsOnly) {
        super(showSelectionListsOnly);
        m_filter = null;
        init();
    }

    /**
     * Create a new panel to filter {@link DataColumnSpec}s. The given {@link DataValue}s specify the type of the
     * columns which are shown and can be included or excluded.
     *
     * @param filterValueClasses The {@link DataValue} of the columns to show.
     */
    public DataColumnSpecFilterPanel(final Class<? extends DataValue>... filterValueClasses) {
        super(false, new DataTypeColumnFilter(filterValueClasses));
        m_filter = new DataTypeColumnFilter(filterValueClasses);
        init();
    }

    /**
     * Create a new panel to filter {@link DataColumnSpec}s. The given {@link DataValue}s specify the type of the
     * columns which are shown and can be included or excluded.
     *
     * @param showSelectionListsOnly if true, the panel shows no additional options like search box,
     *            force-include-option, etc.
     * @param filterValueClasses The {@link DataValue} of the columns to show.
     */
    public DataColumnSpecFilterPanel(final boolean showSelectionListsOnly,
        final Class<? extends DataValue>... filterValueClasses) {
        super(showSelectionListsOnly, new DataTypeColumnFilter(filterValueClasses));
        m_filter = new DataTypeColumnFilter(filterValueClasses);
        init();
    }

    /**
     * Create a new panel to filter {@link DataColumnSpec}s. The given filter handles which columns are shown and can be
     * included or excluded and which not, based on the underlying type data type of the column.
     *
     * @param showSelectionListsOnly if true, the panel shows no additional options like search box,
     *            force-include-option, etc.
     * @param filter The filter specifying which columns are shown and which not.
     */
    public DataColumnSpecFilterPanel(final boolean showSelectionListsOnly, final InputFilter<DataColumnSpec> filter) {
        super(showSelectionListsOnly, filter);
        m_filter = filter;
        init();
    }

    /**
     * Create a new panel to filter {@link DataColumnSpec}s. The given filter handles which columns are shown and can be
     * included or excluded and which not, based on the underlying type data type of the column.
     *
     * @param filter The filter specifying which columns are shown and which not.
     */
    public DataColumnSpecFilterPanel(final InputFilter<DataColumnSpec> filter) {
        super(false, filter);
        m_filter = filter;
        init();
    }

    /**
     * Load configuration.
     *
     * @param config the configuration to read to settings from.
     * @param spec the {@link DataTableSpec} to validate the settings on
     */
    public void loadConfiguration(final DataColumnSpecFilterConfiguration config, final DataTableSpec spec) {
        m_spec = spec;
        m_typePanel.loadConfiguration(config.getTypeConfig(), spec);
        setTypeFilterEnabled(config.isTypeFilterEnabled());
        super.loadConfiguration(config, spec == null ? new String[0] : spec.getColumnNames());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveConfiguration(final NameFilterConfiguration config) {
        if (config instanceof DataColumnSpecFilterConfiguration) {
            m_typePanel.saveConfiguration(((DataColumnSpecFilterConfiguration)config).getTypeConfig());
        }
        super.saveConfiguration(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_typePanel.setEnabled(enabled);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    protected ListCellRenderer getListCellRenderer() {
        return new DataColumnSpecListCellRenderer();
    }

    /** {@inheritDoc} */
    @Override
    protected DataColumnSpec getTforName(final String name) {
        if (m_spec == null) {
            return null;
        }
        return m_spec.getColumnSpec(name);
    }

    /** {@inheritDoc} */
    @Override
    protected String getNameForT(final DataColumnSpec dcs) {
        return dcs.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPanel getFilterPanel(final String type) {
        if (TypeFilterConfigurationImpl.TYPE.equals(type)) {
            return m_typePanel;
        }
        return super.getFilterPanel(type);
    }

    private void init() {
        DataValueFilter filter = null;
        if (m_filter != null && m_filter instanceof DataTypeColumnFilter) {
            filter = new DataValueFilter(((DataTypeColumnFilter)m_filter).getFilterClasses());
        }
        m_typePanel = new TypeFilterPanelImpl(filter);
        m_typePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                fireFilteringChangedEvent();
            }
        });
        m_typeButton = createButtonToFilterPanel(TypeFilterConfigurationImpl.TYPE, "Type Selection");
    }

    /**
     * Enables or disables the type filter.
     *
     * @param enabled If the type filter should be enabled
     * @since 2.9
     */
    private void setTypeFilterEnabled(final boolean enabled) {
        if (m_typeFilterEnabled == null || !m_typeFilterEnabled.equals(enabled)) {
            if (enabled) {
                addType(m_typeButton, DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE);
            } else {
                removeType(m_typeButton);
            }
            m_typeFilterEnabled = enabled;
        }
    }

}
