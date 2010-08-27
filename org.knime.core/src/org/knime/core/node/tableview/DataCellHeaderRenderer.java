/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * 
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.tableview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/** 
 * Class to render a <code>DataCell</code> in a row header of a
 * <code>JTable</code>. The layout of the component being returned is the same
 * as a TableHeader in a <code>JTable</code>, i.e. it uses the look and feel
 * of a table header. This renderer component allows to encode the hilite
 * status of the row being displayed: Hilited rows have a different
 * background color than non-hilited rows. This implementation also allows
 * to encode the color information for a data cell (i.e. from the 
 * <code>ColorHandler</code>) in a small icon.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class DataCellHeaderRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -6837071446890050246L;

    /** 
     * Factory method to create a new instance of this class.
     * 
     * @return a new <code>DataCellHeaderRenderer</code>
     */
    public static final DataCellHeaderRenderer newInstance() { 
        return new DataCellHeaderRenderer();
    } // newInstance()

    
    // user should use newInstance instead 
    private DataCellHeaderRenderer() {
        setTableHeaderLaF();
        showIcon(true);
    } // DataCellHeaderRenderer()
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        StringBuilder b = new StringBuilder("\"");
        b.append(value);
        b.append("\" (");
        b.append(row + 1);
        b.append("/");
        b.append(table.getRowCount());
        TableModel mdl = table.getModel();
        if (mdl instanceof TableRowHeaderModel) {
            if (!((TableRowHeaderModel)mdl).isRowCountFinal()) {
                b.append("+");
            }
        }
        b.append(")");
        setToolTipText(b.toString());
        return super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setBounds(
            final int x, final int y, final int width, final int height) {
        ColorIcon icon = (ColorIcon)getIcon();
        if (icon != null) {
            icon.setIconHeight(height);
            icon.setIconWidth(Math.min(15, Math.max(1, (int)(0.15 * width))));
        }
        super.setBounds(x, y, width, height);
    }
    
    
    /** 
     * Set the color information for the next cell to be rendered. This method
     * should be called right before the 
     * {@link DefaultTableCellRenderer#getTableCellRendererComponent(
     * javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)} 
     * method is called. It sets the color info of the row (or key) that is
     * rendered. In a table, e.g., you should override the
     * {@link javax.swing.JTable#prepareRenderer(
     * javax.swing.table.TableCellRenderer, int, int)} method and call this
     * method first before calling <code>super.prepareRenderer</code>.
     * 
     * @param color the color information.
     * @see javax.swing.JTable#prepareRenderer(
     *      javax.swing.table.TableCellRenderer, int, int)
     */
    public void setColor(final Color color) {
        ColorIcon icon = (ColorIcon)getIcon();
        if (icon != null) {
            icon.setColor(color);
        }
    } // setColor(Color)
    
    
    /** 
     * Enable/Disable the color information output.
     * 
     * @param isShowIcon <code>true</code> for show icon, <code>false</code>
     * otherwise
     */
    public void showIcon(final boolean isShowIcon) {
        if (isShowIcon() == isShowIcon) {
            return;
        }
        setIcon(isShowIcon ? new ColorIcon() : null);
    } // showIcon(boolean)
    
    
    /** 
     * Is the icon with the color info shown?
     * 
     * @return <code>true</code> if it's there, <code>false</code> otherwise
     */
    public boolean isShowIcon() {
        return getIcon() != null;
    }
        
    
    /** 
     * Catches look and feel changes and updates the layout of the renderer.
     * This renderer simulates the table header look and feel.
     * 
     * @see javax.swing.JComponent#updateUI()
     */
    @Override
    public void updateUI() {
        super.updateUI();
        setTableHeaderLaF();
    } // updateUI()
    
    
    /** 
     * Called when look and feel changes. Sets border and fore- and background
     * color according the TableHeader property.
     */ 
    private void setTableHeaderLaF() {
        setForeground(UIManager.getColor("TableHeader.foreground"));
        setBackground(UIManager.getColor("TableHeader.background"));
        setFont(UIManager.getFont("TableHeader.font"));
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
    } // setTableHeaderLaF()
    
    
    /**
     * Private icon that is shown in front of the value to encode the hilite
     * status. This icon is a simple bubble. The code is mainly copied from
     * {@link javax.swing.plaf.basic.BasicIconFactory} and altered accordingly.
     */
    private static class ColorIcon implements Icon {
        private int m_height = 8;
        private int m_width = 8;
        private Color m_color = Color.WHITE;

        /**
         * {@inheritDoc}
         */
        public int getIconHeight() {
            return m_height;
        }
        
        /**
         * {@inheritDoc}
         */
        public int getIconWidth() {
            return m_width;
        }
        
        /**
         * @param height new height to set.
         */
        public void setIconHeight(final int height) {
            m_height = height;
        }
        
        /**
         * @param width new width to set.
         */
        public void setIconWidth(final int width) {
            m_width = width;
        }
        
        /** 
         * Setting a color the icon should have. Used to encode the hilite 
         * status.
         * 
         * @param color New color for the icon.
         */
        public void setColor(final Color color) {
            m_color = color;
        }

        /**
         * {@inheritDoc}
         */
        public void paintIcon(
            final Component c, final Graphics g, final int x, final int y) {
            g.setColor(m_color);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
        }
    } // end class ColorIcon
}
