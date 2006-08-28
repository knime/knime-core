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
 *   14.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

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
    private static final String ASC = new String("Ascending");

    /*
     * Descending
     */
    private static final String DESC = new String("Descending");

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
     * @param selected the selected column
     * @param sortOrder the sort
     */
    SortItem(final int id, final Vector<?> values, final Object selected,
            final boolean sortOrder) {
        m_id = id;
        m_combovalues = values;

        GridLayout gl = new GridLayout(2, 2);
        super.setLayout(gl);
        m_combo = new JComboBox(m_combovalues);
        m_combo.setLightWeightPopupEnabled(false);
        m_combo.setSelectedItem(selected);
        m_combo.setMaximumSize(new Dimension(150, 25));
        m_combo.setPreferredSize(new Dimension(150, 25));
        
        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.Y_AXIS));
        comboPanel.add(Box.createGlue());
        comboPanel.add(m_combo);
        comboPanel.add(Box.createGlue());
        
        
        
        ButtonGroup group = new ButtonGroup();
        group.add(m_ascRB);
        group.add(m_descRB);
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
        super.add(m_ascRB);
        super.add(new JLabel());
        super.add(m_descRB);
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
    public Object getSelectedColumn() {
        return m_combo.getSelectedItem();
    }
}
