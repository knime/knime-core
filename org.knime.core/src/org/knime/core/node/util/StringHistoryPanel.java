/*
 * ------------------------------------------------------------------
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
 *   Dec 17, 2005 (wiswedel): created
 */
package org.knime.core.node.util;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;



/**
 * Panel that contains an editable Combo Box to choose or edit a string. The
 * string being selected are memorized by means of a {@link StringHistory}.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class StringHistoryPanel extends JPanel {

    private final JComboBox m_textBox;

    private final String m_historyID;

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     * @param historyID Identifier for the string history, 
     *        see {@link StringHistory}.
     */
    public StringHistoryPanel(final String historyID) {
        if (historyID == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_historyID = historyID;
        m_textBox = new JComboBox(new DefaultComboBoxModel());
        m_textBox.setEditable(true);
        m_textBox.setRenderer(new MyComboBoxRenderer());
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(m_textBox);
        updateHistory();
    }
    
    /**
     * Calls the respective method on the underlying text box.
     * @param s The reference object
     * @see JComboBox#setPrototypeDisplayValue(Object)
     */
    public final void setPrototypeDisplayValue(final Object s) {
        m_textBox.setPrototypeDisplayValue(s);
    }

    /**
     * Get currently selected entry.
     * 
     * @return the current entry
     * @see javax.swing.JComboBox#getSelectedItem()
     */
    public String getSelectedString() {
        return m_textBox.getEditor().getItem().toString();
    }

    /**
     * Set the default entry.  
     * 
     * @param entry the entry to choose.
     * @see javax.swing.JComboBox#setSelectedItem(java.lang.Object)
     */
    public void setSelectedString(final String entry) {
        m_textBox.setSelectedItem(entry);
    }
    
    /** Adds the currently selected element to the string history. */
    public void commitSelectedToHistory() {
        StringHistory history = StringHistory.getInstance(m_historyID);
        history.add(getSelectedString());
    }

    /** Updates the elements in the combo box, reads the file history. */
    public void updateHistory() {
        StringHistory history = StringHistory.getInstance(m_historyID);
        String[] allVals = history.getHistory();
        DefaultComboBoxModel comboModel = 
            (DefaultComboBoxModel)m_textBox.getModel();
        comboModel.removeAllElements();
        for (String s : allVals) {
            comboModel.addElement(s);
        }
        // changing the model will also change the minimum size to be
        // quite big. We have tooltips, we don't need that
        Dimension newMin = new Dimension(0, getPreferredSize().height);
        setMinimumSize(newMin);
    }

    /** renderer that also supports to show customized tooltip. */
    private static class MyComboBoxRenderer extends BasicComboBoxRenderer {
        /**
         * @see BasicComboBoxRenderer#getListCellRendererComponent(
         *      javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            if (index > -1) {
                list.setToolTipText(value.toString());
            }
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }
    }
}
