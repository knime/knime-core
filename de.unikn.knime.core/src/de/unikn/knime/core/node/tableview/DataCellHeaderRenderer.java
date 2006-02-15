/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node.tableview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/** 
 * Class to render a <code>DataCell</code> in a row header of a
 * <code>JTable</code>. The layout of the component being returned is the same
 * as a TableHeader in a <code>JTable</code>, i.e. it uses the look and feel
 * of a table header. This renderer component allows to encode the highlight
 * status of the row being displayed: Highlighted rows have a different
 * background color than non-highlighted rows. This implementation also allows
 * to encode the color information for a data cell (i.e. from the 
 * <code>ColorHandler</code>) in an small icon.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class DataCellHeaderRenderer extends DefaultTableCellRenderer {
    
    /** 
     * Factory method to create a new instance of this class.
     * @return a new <code>DataCellHeaderRenderer</code>
     */
    public static final DataCellHeaderRenderer newInstance() { 
        return new DataCellHeaderRenderer();
    } // newInstance()
    
    // user should use INSTANCE instead 
    private DataCellHeaderRenderer() {
        setTableHeaderLaF();
        showIcon(true);
    } // DataCellHeaderRenderer()
    
    /**
     * @see java.awt.Component#setBounds(int, int, int, int)
     */
    public void setBounds(
            final int x, final int y, final int width, final int height) {
        ColorIcon icon = (ColorIcon)getIcon();
        if (icon != null) {
            icon.setIconHeigth(height);
            icon.setIconWidth(Math.max(1, (int)(0.15 * width)));
        }
        super.setBounds(x, y, width, height);
    }
    /** 
     * Set the color information for the next cell to be rendered. This method
     * should be called right before the 
     * <code>getTableCellRendererComponent</code> method is called. It sets the
     * color info of the row (or key) that is rendered. 
     * In a table, e.g., you should override the <code>prepareRenderer</code> 
     * method and call this method first before calling 
     * <code>super.prepareRenderer</code>.
     * @param color The color information.
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
     * @param isShowIcon true for show icon.
     */
    public void showIcon(final boolean isShowIcon) {
        if (isShowIcon() == isShowIcon) {
            return;
        }
        setIcon(isShowIcon ? new ColorIcon() : null);
    } // showIcon(boolean)
    
    /** 
     * Is the icon with the color info shown?
     * @return <code>true</code> Yep, it's there.
     */
    public boolean isShowIcon() {
        return getIcon() != null;
    }
        
    /** 
     * Catches look and feel changes and updates the layout of the renderer.
     * This renderer simulates the table header look and feel.
     * @see javax.swing.JComponent#updateUI()
     */
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
    
    /** Private icon that is shown in front of the value to encode the highlight
     * status. This icon is a simple bubble. The code is mainly copied from 
     * javax.swing.plaf.basic.BasicIconFactory and altered accordingly.
     */
    private static class ColorIcon implements Icon {

        private int m_height = 8;
        private int m_width = 8;
        private Color m_color = Color.WHITE;

        /**
         * @see javax.swing.Icon#getIconHeight()
         */
        public int getIconHeight() {
            return m_height;
        }
        
        /**
         * @see javax.swing.Icon#getIconWidth()
         */
        public int getIconWidth() {
            return m_width;
        }
        
        /**
         * @param height New height to set.
         */
        public void setIconHeigth(final int height) {
            m_height = height;
        }
        
        /**
         * @param width New width to set.
         */
        public void setIconWidth(final int width) {
            m_width = width;
        }
        /** 
         * Setting a color the icon should have. Used to encode highlight 
         * status.
         * @param color New color for the icon.
         */
        public void setColor(final Color color) {
            m_color = color;
        }

        /**
         * @see javax.swing.Icon#paintIcon(
         *      java.awt.Component, java.awt.Graphics, int, int)
         */
        public void paintIcon(
            final Component c, final Graphics g, final int x, final int y) {
            g.setColor(m_color);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
        }
    } // end class ColorIcon
}
