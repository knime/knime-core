/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Nov 25, 2006 (wiswedel): created
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 * Abstract implementation of a {@link DataValueRenderer} that renders more
 * complex scenes than just ordinary text. This abstract already supplies
 * implementations of the <code>getTableCellRendererComponent</code> and
 * <code>getListCellRenderer</code> methods. Subclasses usually override
 * <ul>
 * <li>{@link #setValue(Object)}: Keep the object to render as field</li>
 * <li>{@link JComponent#paintComponent(java.awt.Graphics)}: Render the object
 * to a graphics object, if the rendering is implemented in an external panel
 * implementation, use {@link javax.swing.SwingUtilities#paintComponent(
 * java.awt.Graphics, Component, java.awt.Container, int, int, int, int)}
 * <li>{@link #getDescription()}: Return a more meaningful description than
 * just &quot;Default&quot;
 * </ul>
 *
 * <p>
 * <strong><a name="override">Implementation Note:</a></strong>
 * This class inherits from <code>JComponent</code> but is not supposed
 * to be put into a component hierarchy. It should only be used for rendering.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractPainterDataValueRenderer
    extends JComponent implements DataValueRenderer {

    /** Variables copied from DefaultTableCellRenderer. */
    private static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
    private static final Border SAFE_NO_FOCUS_BORDER =
        new EmptyBorder(1, 1, 1, 1);

    // We need a place to store the color the JLabel should be returned
    // to after its foreground and background colors have been set
    // to the selection background color.
    private Color m_unselectedForeground;
    private Color m_unselectedBackground;

    private static Border getNoFocusBorder() {
        if (System.getSecurityManager() != null) {
            return SAFE_NO_FOCUS_BORDER;
        } else {
            return noFocusBorder;
        }
    }

    /**
     * Creates a default table cell renderer.
     */
    public AbstractPainterDataValueRenderer() {
        setOpaque(true);
        setBorder(getNoFocusBorder());
    }

    /**
     * @return &quot;Default&quot;
     * @see DataValueRenderer#getDescription()
     */
    @Override
    public String getDescription() {
        return "Default";
    }

    /**
     * Returns always <code>true</code>.
     * @see DataValueRenderer#accepts(DataColumnSpec)
     */
    @Override
    public boolean accepts(final DataColumnSpec spec) {
        return true;
    }

    /**
     * Sets the object to be rendered next. This method is invoked from
     * the {@link #getRendererComponent(Object)},
     * {@link #getTableCellRendererComponent(
     * JTable, Object, boolean, boolean, int, int)}, and
     * {@link #getListCellRendererComponent(
     * JList, Object, int, boolean, boolean)} methods.
     *
     * @param value The value to render. This is most of the time a DataValue
     * that the derived class is supposed to render. However, it may also
     * be <code>null</code>, a missing data cell or any other object.
     */
    protected abstract void setValue(Object value);

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getRendererComponent(final Object val) {
        setValue(val);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        if (isSelected) {
            super.setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            super.setForeground((m_unselectedForeground != null)
                    ? m_unselectedForeground : table.getForeground());
            super.setBackground((m_unselectedBackground != null)
                    ? m_unselectedBackground : table.getBackground());
        }
        setFont(table.getFont());
        if (hasFocus) {
            Border border = null;
            if (isSelected) {
                border = UIManager.getBorder(
                        "Table.focusSelectedCellHighlightBorder");
            }
            if (border == null) {
                border = UIManager.getBorder("Table.focusCellHighlightBorder");
            }
            setBorder(border);
            if (!isSelected && table.isCellEditable(row, column)) {
                Color col;
                col = UIManager.getColor("Table.focusCellForeground");
                if (col != null) {
                    super.setForeground(col);
                }
                col = UIManager.getColor("Table.focusCellBackground");
                if (col != null) {
                    super.setBackground(col);
                }
            }
        } else {
            setBorder(getNoFocusBorder());
        }
        try {
            setValue(value);
        } catch (Exception e) {
            String valS = value != null ? value.toString() : "<null>";
            if (valS.length() > 20) {
                valS = valS.substring(0, 20).concat("...");
            }
            StringBuilder b = new StringBuilder("Failed setting new value (\"");
            b.append(valS).append("\"");
            if (value != null) {
                b.append(" - class ").append(value.getClass().getSimpleName());
            }
            b.append(")");
            NodeLogger.getLogger(getClass()).coding(b.toString(), e);
            setValue(DataType.getMissingCell());
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        setComponentOrientation(list.getComponentOrientation());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setValue(value);
        setEnabled(list.isEnabled());
        setFont(list.getFont());

        Border border = null;
        if (cellHasFocus) {
            if (isSelected) {
                border = UIManager.getBorder(
                        "List.focusSelectedCellHighlightBorder");
            }
            if (border == null) {
                border = UIManager.getBorder("List.focusCellHighlightBorder");
            }
        } else {
            border = getNoFocusBorder();
        }
        setBorder(border);
        return this;
    }

    /**
     * Overrides <code>JComponent.setForeground</code> to assign
     * the unselected-foreground color to the specified color.
     *
     * @param c set the foreground color to this value
     */
    @Override
    public void setForeground(final Color c) {
        super.setForeground(c);
        m_unselectedForeground = c;
    }

    /**
     * Overrides <code>JComponent.setBackground</code> to assign
     * the unselected-background color to the specified color.
     *
     * @param c set the background color to this value
     */
    @Override
    public void setBackground(final Color c) {
        super.setBackground(c);
        m_unselectedBackground = c;
    }


    /**
     * Notification from the <code>UIManager</code> that the look and feel
     * [L&F] has changed.
     * Replaces the current UI object with the latest version from the
     * <code>UIManager</code>.
     *
     * @see JComponent#updateUI
     */
    @Override
    public void updateUI() {
        super.updateUI();
        setForeground(null);
        setBackground(null);
    }

    /*
     * The following methods were copied from
     * javax.swing.table.DefaultTableCellRenderer.
     */
    /*
     * The following methods are overridden as a performance measure to
     * to prune code-paths are often called in the case of renders
     * but which we know are unnecessary.  Great care should be taken
     * when writing your own renderer to weigh the benefits and
     * drawbacks of overriding methods like these.
     */

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     * @see JComponent#isOpaque()
     */
    @Override
    public boolean isOpaque() {
    Color back = getBackground();
    Component p = getParent();
    if (p != null) {
        p = p.getParent();
    }
    // p should now be the JTable.
    boolean colorMatch = (back != null) && (p != null)
        && back.equals(p.getBackground()) && p.isOpaque();
    return !colorMatch && super.isOpaque();
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     * @see JComponent#invalidate()
     */
    @Override
    public void invalidate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     * @see JComponent#validate()
     */
    @Override
    public void validate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     * @see JComponent#revalidate()
     */
    @Override
    public void revalidate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     * @see JComponent#repaint(long, int, int, int, int)
     */
    @Override
    public void repaint(final long tm, final int x, final int y,
            final int width, final int height) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     * @see JComponent#repaint(Rectangle)
     */
    @Override
    public void repaint(final Rectangle r) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     * @see JComponent#repaint()
     */
    @Override
    public void repaint() {
    }

}
