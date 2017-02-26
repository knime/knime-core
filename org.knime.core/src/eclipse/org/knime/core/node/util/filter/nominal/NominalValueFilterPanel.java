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
 *   Feb 8, 2017 (ferry): created
 */
package org.knime.core.node.util.filter.nominal;

import java.util.ArrayList;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JToggleButton;

import org.knime.core.data.DataCell;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterPanel;

/**
 * A dialog component to filter nominal values. Mainly for visualization, configuration is done in
 * {@link NominalValueFilterConfiguration}.
 *
 * @author Ferry Abt, KNIME.com AG, Zurich, Switzerland
 * @since 3.3
 */
@SuppressWarnings("serial")
public class NominalValueFilterPanel extends NameFilterPanel<String> {

    private JToggleButton m_includeMissing;

    /**
     * Creates a basic panel that allows the filtering of nominal values.
     *
     * @since 3.3
     */
    public NominalValueFilterPanel() {
        this(false);
    }

    /**
     * Creates a basic panel that allows the filtering of nominal values.
     *
     * @param showSelectionListsOnly {@code true} hides the <i>Enforce in-/exclusion</i> buttons
     * @since 3.3
     */
    public NominalValueFilterPanel(final boolean showSelectionListsOnly) {
        super(showSelectionListsOnly, null, "Value(s)");
    }

    @Override
    protected JToggleButton getAdditionalButton() {
        m_includeMissing = new JToggleButton("Incl. Missing");
        m_includeMissing.setToolTipText("Include Missing Values");
        return m_includeMissing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultListCellRenderer getListCellRenderer() {
        return new DefaultListCellRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getTforName(final String name) {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getNameForT(final String t) {
        return t;
    }

    /**
     * Updates this filter panel by removing all current selections from the include and exclude list. The exclude list
     * will contains all values from the given domain afterwards.
     *
     * @see NameFilterPanel#loadConfiguration(NameFilterConfiguration, String[])
     *
     * @param config to be loaded from
     * @param domain
     */
    public void loadConfiguration(final NameFilterConfiguration config, final Set<DataCell> domain) {
        ArrayList<String> names = new ArrayList<>();
        //get array of domain values
        if (domain != null) {
            for (DataCell dc : domain) {
                names.add(dc.toString());
            }
        }
        super.loadConfiguration(config, names.toArray(new String[names.size()]));
        if (m_includeMissing != null && config instanceof NominalValueFilterConfiguration) {
            m_includeMissing.setSelected(((NominalValueFilterConfiguration)config).isIncludeMissing());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveConfiguration(final NameFilterConfiguration config) {
        if (config instanceof NominalValueFilterConfiguration) {
            ((NominalValueFilterConfiguration)config).setIncludeMissing(m_includeMissing.isSelected());
        }
        super.saveConfiguration(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_includeMissing.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NominalValuePatternFilterPanel getPatternFilterPanel(final InputFilter<String> filter) {
        return new NominalValuePatternFilterPanel(this, filter);
    }

}
