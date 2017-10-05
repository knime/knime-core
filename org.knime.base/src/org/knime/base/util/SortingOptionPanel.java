/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on 2013.07.19. by Gabor Bakos
 */
package org.knime.base.util;

import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A dialog panel for sorting options.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public class SortingOptionPanel extends JPanel {
    private static final long serialVersionUID = -7815684557613853264L;

    /** The key for sorting options. */
    public static final String DEFAULT_KEY_SORTING_STRATEGY = "sortingStrategy";

    /** The key to reverse order. */
    public static final String DEFAULT_KEY_SORTING_REVERSED = "sortingReversed";

    @SuppressWarnings("rawtypes")
    private final JComboBox m_sortingStrategy = new JComboBox();

    private final JCheckBox m_reverse = new JCheckBox("Reverse order");

    /**
     * Constructs {@link SortingOptionPanel}.
     */
    public SortingOptionPanel() {
        FlowLayout layout = new FlowLayout(FlowLayout.LEADING);
        layout.setHgap(4);
        setLayout(layout);
        for (SortingStrategy ss : SortingStrategy.values()) {
            addSortingStrategy(ss);
        }
        add(new JLabel("Sorting strategy: "));
        add(m_sortingStrategy);
        m_sortingStrategy.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                updateControls();
            }
        });
        add(m_reverse);
    }

    /**
     * @param strategy The {@link SortingStrategy} to add to the possible sorting strategies.
     */
    @SuppressWarnings("unchecked")
    private void addSortingStrategy(final SortingStrategy strategy) {
        m_sortingStrategy.addItem(strategy);
    }

    /**
     * @param reverseOrder The new value for reverse order (when the result of comparison is the opposite.)
     */
    public void setReverseOrder(final boolean reverseOrder) {
        m_reverse.getModel().setSelected(reverseOrder);
    }

    /**
     * @return The value of reverse order.
     */
    public boolean isReverseOrder() {
        return m_reverse.getModel().isSelected();
    }

    /**
     * Selects the specified {@link SortingStrategy}.
     *
     * @param strategy The {@link SortingStrategy} to select in the combobox. (Not {@code null}.)
     */
    public void setSortingStrategy(final SortingStrategy strategy) {
        m_sortingStrategy.setSelectedItem(strategy);
    }

    /**
     * @return The currently selected {@link SortingStrategy}.
     */
    public SortingStrategy getStrategy() {
        return (SortingStrategy)m_sortingStrategy.getSelectedItem();
    }

    /**
     * Initializes the values based on the settings without prefixing the keys.
     *
     * @param settings The {@link NodeSettingsRO} containing the keys.
     * @throws InvalidSettingsException Problem loading the settings, or wrong values.
     * @see #load(NodeSettingsRO, String)
     */
    public void loadDefault(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings, "");
    }

    /**
     * Initializes the values based on the settings with the specified {@code prefix} of the keys.
     *
     * @param settings The {@link NodeSettingsRO} containing the keys.
     * @param prefix The prefix for the keys.
     * @throws InvalidSettingsException Problem loading the settings, or wrong values.
     * @see #DEFAULT_KEY_SORTING_REVERSED
     * @see #DEFAULT_KEY_SORTING_STRATEGY
     */
    //TODO extract to a model.
    public void load(final NodeSettingsRO settings, final String prefix) throws InvalidSettingsException {
        setSortingStrategy(SortingStrategy.values()[settings.getInt(prefix + DEFAULT_KEY_SORTING_STRATEGY,
            SortingStrategy.Lexical.ordinal())]);
        setReverseOrder(settings.getBoolean(prefix + DEFAULT_KEY_SORTING_REVERSED, false));
        updateControls();
    }

    /**
     * Sets the options available from the combobox for the strategies.
     *
     * @param sortingStrategies The possible sorting strategies. (Non-empty array.)
     */
    public void setPossibleSortingStrategies(final SortingStrategy... sortingStrategies) {
        if (sortingStrategies.length == 0) {
            throw new IllegalArgumentException("No strategies set.");
        }
        m_sortingStrategy.removeAllItems();
        for (SortingStrategy sortingStrategy : sortingStrategies) {
            addSortingStrategy(sortingStrategy);
        }
        if (getStrategy() == null || !Arrays.asList(sortingStrategies).contains(getStrategy())) {
            m_sortingStrategy.setSelectedIndex(0);
        }
        updateControls();
    }

    /**
     * Updates controls based on their selections.
     */
    public void updateControls() {
        //Do nothing for now.
    }

    /**
     * Save the current configuration to {@code settings} with empty prefix.
     *
     * @param settings The {@link NodeSettingsWO} where the settings will be stored.
     * @see #save(NodeSettingsWO, String)
     */
    public void saveDefault(final NodeSettingsWO settings) {
        save(settings, "");
    }

    /**
     * Save the current configuration to {@code settings} with empty prefix.
     *
     * @param settings The {@link NodeSettingsWO} where the settings will be stored.
     * @param prefix The prefix for the configuration keys.
     * @see #DEFAULT_KEY_SORTING_REVERSED
     * @see #DEFAULT_KEY_SORTING_STRATEGY
     */
    //TODO extract to a model.
    public void save(final NodeSettingsWO settings, final String prefix) {
        settings.addBoolean(prefix + DEFAULT_KEY_SORTING_REVERSED, isReverseOrder());
        settings.addInt(prefix + DEFAULT_KEY_SORTING_STRATEGY, getStrategy().ordinal());
    }
}
