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
 *   14.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.node.util.ColumnComboBoxRenderer;

/**
 * The SortItem is a JPanel with a JComboBox and two JRadioButtons.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class SortItem extends JPanel {
    private static final long serialVersionUID = 1662885117523675833L;

    /*
     * The unique ID
     */
    private final int m_id;

    /*
     * Values for the JComboBox
     */
    private final Vector<?> m_combovalues;

    /*
     * The JComboBox
     */
    private final JComboBox m_combo;

    /*
     * Ascending
     */
    private static final String ASC = "Ascending";

    /*
     * Descending
     */
    private static final String DESC = "Descending";

    /*
     * JRadioButton for ascending order
     */
    private final JRadioButton m_ascRB = new JRadioButton(ASC);

    /*
     * JRadioButton for descending order
     */
    private final JRadioButton m_descRB = new JRadioButton(DESC);

    /**
     * Constructs a new JPanel that consists of a JComboBox which lets the user
     * choose the columns to sort and two JRadioButtons to choose the sort order
     * (ascending/descending).
     *
     * @param id the unique ID of the SortItem
     * @param values the columns that the user can choose from
     * @param columnName the name of the column
     * @param sortOrder the sort
     */
    SortItem(final int id, final Vector<DataColumnSpec> values,
            final String columnName, final boolean sortOrder) {
        this(id, values, new DataColumnSpecCreator(columnName,
            DataType.getType(DataCell.class)).createSpec(), sortOrder);
        m_combo.setSelectedIndex(-1);
        ColumnComboBoxRenderer renderer =
            (ColumnComboBoxRenderer)m_combo.getRenderer();
        renderer.setDefaultValue(columnName);
    }

    /**
     * Constructs a new JPanel that consists of a JComboBox which lets the user
     * choose the columns to sort and two JRadioButtons to choose the sort order
     * (ascending/descending).
     *
     * @param id the unique ID of the SortItem
     * @param values the columns that the user can choose from
     * @param selected the selected column
     * @param sortOrder the sort
     */
    SortItem(final int id, final Vector<DataColumnSpec> values,
            final DataColumnSpec selected, final boolean sortOrder) {
        m_id = id;
        m_combovalues = values;


        super.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        m_combo = new JComboBox(m_combovalues);
        ColumnComboBoxRenderer renderer =
            new ColumnComboBoxRenderer();
        renderer.attachTo(m_combo);
        m_combo.setLightWeightPopupEnabled(false);
        m_combo.setSelectedItem(selected);
        m_combo.setMaximumSize(new Dimension(800, 30));

        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.Y_AXIS));
        comboPanel.add(m_combo);
        comboPanel.add(Box.createVerticalGlue());


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        group.add(m_ascRB);
        group.add(m_descRB);
        buttonPanel.add(m_ascRB);
        buttonPanel.add(m_descRB);
        buttonPanel.add(Box.createVerticalGlue());
        String bordertext = (id == 0) ? "Sort by:" : "Next by:";
        Border outsideborder = BorderFactory.createTitledBorder(bordertext);
        Border insideborder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border cborder = BorderFactory.createCompoundBorder(outsideborder,
                insideborder);
        setBorder(cborder);
        if (sortOrder) {
            m_ascRB.setSelected(true);
        } else {
            m_descRB.setSelected(true);
        }

        super.add(comboPanel);
        super.add(Box.createHorizontalStrut(20));
        super.add(buttonPanel);
    }

    /**
     * Each SortItem has a unique ID.
     *
     * @return the ID of the SortItem
     */
    public int getID() {
        return m_id;
    }

    /**
     * The Sortorder of this SortItem.
     *
     * @return <code>true</code> for ascending, <code>false</code> for
     *         descending
     */

    public boolean getSortOrder() {
        if (m_ascRB.isSelected()) {
            return true;
        }
        return false;
    }


    /**
     * The column that is selected in the JComboBox.
     *
     * @return the selected column
     */
    public DataColumnSpec getSelectedColumn() {
        return (DataColumnSpec) m_combo.getSelectedItem();
    }

    /**
     * Returns true when a valid selection is done for the column field.
     *
     * @return if a selection is done by the user.
     */
    boolean isColumnSelected() {
        return m_combo.getSelectedIndex() >= 0;
    }

    /**
     * Get the text in the column field that is displayed to the user.
     * @return the text of the column field displayed to the user.
     */
    String getColumnText() {
        if (isColumnSelected()) {
            return getSelectedColumn().getName();
        } else {
            ColumnComboBoxRenderer renderer =
                (ColumnComboBoxRenderer)m_combo.getRenderer();
            return renderer.getDefaultValue();
        }
    }
}
