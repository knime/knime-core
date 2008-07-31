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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.tableview;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;


/**
 * Renderer to be used to display the column header of a table. It will show
 * an icon on the left and the name of the column on the right. The icon is
 * given by the type's <code>getIcon()</code> method
 * 
 * @see org.knime.core.data.DataType#getIcon() 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ColumnHeaderRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -2356486759304444805L;
    
    private boolean m_showIcon = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, 
            final Object value, final boolean isSelected, 
            final boolean hasFocus, final int row, final int column) {
        // set look and feel of a header
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                setFont(header.getFont());
            }
        }
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        Icon icon = null;
        Object newValue = value;
        if (value instanceof DataColumnSpec) {
            DataType columnType = ((DataColumnSpec)value).getType();
            newValue =  ((DataColumnSpec)value).getName();
            icon = columnType.getIcon();
        }
        if (isShowIcon()) {
            setIcon(icon);
        } else {
            setIcon(null);
        }
        setToolTipText(newValue != null ? newValue.toString() : null);
        setValue(newValue);
        return this;
    }

    /**
     * @return the showIcon
     */
    public boolean isShowIcon() {
        return m_showIcon;
    }

    /**
     * @param showIcon the showIcon to set
     */
    public void setShowIcon(final boolean showIcon) {
        m_showIcon = showIcon;
    }
    
}
