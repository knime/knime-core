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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.tableview;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.WeakHashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.tableview.TableSortOrder.TableSortKey;


/**
 * Renderer to be used to display the column header of a table. It will show
 * an icon on the left and the name of the column on the right. The icon is
 * given by the type's <code>getIcon()</code> method. If the column is sorted,
 * the icon will be compound icon consisting of the type icon and and
 * arrow icon indicating the sort order.
 *
 * @see org.knime.core.data.DataType#getIcon()
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ColumnHeaderRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = -2356486759304444805L;

    private boolean m_showIcon = true;

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
        Icon typeIcon = null;
        Icon sortIcon = null;
        Object newValue = value;
        if (isShowIcon() && value instanceof DataColumnSpec) {
            DataType columnType = ((DataColumnSpec)value).getType();
            newValue =  ((DataColumnSpec)value).getName();
            typeIcon = columnType.getIcon();
        }
        sortIcon = getSortIcon(table, column);
        if (typeIcon == null && sortIcon == null) {
            setIcon(null);
        } else {
            setIcon(new CompoundIcon(typeIcon, sortIcon));
        }
        setToolTipText(newValue != null ? newValue.toString() : null);
        setValue(newValue);
        return this;
    }

    /**
     * @param table
     * @param column
     * @param sortIcon
     * @return */
    private Icon getSortIcon(final JTable table, final int column) {
        if (table == null) {
            return null;
        }
        TableModel model = table.getModel();
        int colIndexInModel = -1;
        TableSortOrder sortOrder = null;
        if (model instanceof TableContentModel) {
            TableContentModel cntModel = (TableContentModel)model;
            sortOrder = cntModel.getTableSortOrder();
            colIndexInModel = table.convertColumnIndexToModel(column);
        } else if (model instanceof TableRowHeaderModel) {
            TableRowHeaderModel rowHeaderModel = (TableRowHeaderModel)model;
            TableContentInterface cntIface = rowHeaderModel.getTableContent();
            if (cntIface instanceof TableContentModel) {
                TableContentModel cntModel = (TableContentModel)cntIface;
                sortOrder = cntModel.getTableSortOrder();
                colIndexInModel = -1;
            }
        }
        TableSortKey sortKey;
        if (sortOrder == null) {
            sortKey = TableSortKey.NONE;
        } else {
            sortKey = sortOrder.getSortKeyForColumn(colIndexInModel);
        }
        switch (sortKey) {
        case PRIMARY_ASCENDING:
            Icon sortIcon = UIManager.getIcon("Table.ascendingSortIcon");
            return getLarge16x16SortIcon(sortIcon);
        case SECONDARY_ASCENDING:
            return UIManager.getIcon("Table.ascendingSortIcon");
        case PRIMARY_DESCENDING:
            sortIcon = UIManager.getIcon("Table.descendingSortIcon");
            return getLarge16x16SortIcon(sortIcon);
        case SECONDARY_DESCENDING:
            return UIManager.getIcon("Table.descendingSortIcon");
        default:
            return null;
        }
    }

    /** Maps the table ascending/descending sort icon to a slightly upscaled
     * image (scaling is done on demand). */
    static final WeakHashMap<Icon, ImageIcon> SORT_ICON_MAP =
        new WeakHashMap<Icon, ImageIcon>();

    /**
     * Get the image underlying the icon in 16x16 format. The argument icon
     * is the java system icon used to represent sort orderings.
     * @param icon The icon or null.;
     * @return The upscaled image icon (or null if that's not possible).
     */
    private ImageIcon getLarge16x16SortIcon(final Icon icon) {
        ImageIcon result = SORT_ICON_MAP.get(icon);
        if (result != null) {
            return result;
        } else {
            Image image = getImageFromIcon(icon);
            Image large = image.getScaledInstance(16, -1, Image.SCALE_SMOOTH);
            ImageIcon largeIcon = new ImageIcon(large, "scaled_image");
            SORT_ICON_MAP.put(icon, largeIcon);
            return largeIcon;
        }
    }
    
    private Image getImageFromIcon(final Icon icon) {
        if (icon instanceof ImageIcon) {
            ImageIcon imageIcon = (ImageIcon)icon;
            return imageIcon.getImage();
        } else {
            BufferedImage bimage = new BufferedImage(icon.getIconWidth(), 
                    icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();

            // Create an image that supports arbitrary levels of transparency
            bimage = gc.createCompatibleImage(icon.getIconWidth(), 
                    icon.getIconHeight(), Transparency.TRANSLUCENT);
            icon.paintIcon(this, bimage.createGraphics(), 0, 0);
            bimage.flush();
            return bimage;
        }
    }

    /** Merges two icons (type icon & sort icon). */
    private static final class CompoundIcon implements Icon {

        private final Icon m_leftIcon;
        private final Icon m_rightIcon;

        /**
         * @param leftIcon
         * @param rightIcon */
        private CompoundIcon(final Icon leftIcon, final Icon rightIcon) {
            m_leftIcon = leftIcon;
            m_rightIcon = rightIcon;
        }

        /** {@inheritDoc} */
        @Override
        public void paintIcon(final Component c, final Graphics g,
                final int x, final int y) {
            if (m_leftIcon == null && m_rightIcon == null) {
                // nothing to paint
            } else if (m_leftIcon == null) {
                m_rightIcon.paintIcon(c, g, x, y);
            } else if (m_rightIcon == null) {
                m_leftIcon.paintIcon(c, g, x, y);
            } else {
                int leftHeight = m_leftIcon.getIconHeight();
                int rightHeight = m_rightIcon.getIconHeight();
                int maxHeight = Math.max(leftHeight, rightHeight);
                int leftWidth = m_leftIcon.getIconWidth();
                if (leftHeight == maxHeight) {
                    m_leftIcon.paintIcon(c, g, x, y);
                } else {
                    m_leftIcon.paintIcon(c, g, x,
                            y + (maxHeight - leftHeight) / 2);
                }
                if (rightHeight == maxHeight) {
                    m_rightIcon.paintIcon(c, g, leftWidth + 1, y);
                } else {
                    m_rightIcon.paintIcon(c, g, leftWidth + 1,
                            y + (maxHeight - rightHeight) / 2);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getIconWidth() {
            if (m_leftIcon == null && m_rightIcon == null) {
                return 0;
            } else if (m_leftIcon == null) {
                return m_rightIcon.getIconWidth();
            } else if (m_rightIcon == null) {
                return m_leftIcon.getIconWidth();
            } else {
                return m_leftIcon.getIconWidth()
                + m_rightIcon.getIconWidth() + 1;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getIconHeight() {
            if (m_leftIcon == null && m_rightIcon == null) {
                return 0;
            } else if (m_leftIcon == null) {
                return m_rightIcon.getIconHeight();
            } else if (m_rightIcon == null) {
                return m_leftIcon.getIconHeight();
            } else {
                return Math.max(m_leftIcon.getIconHeight(),
                        m_rightIcon.getIconHeight());
            }
        }
    }

}
