/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
    
    /** Access to combo box component. This method allows one to change
     * the font being used, for instance.
     * @return The used combo box component.
     */
    public JComboBox getComboBox() {
        return m_textBox;
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
