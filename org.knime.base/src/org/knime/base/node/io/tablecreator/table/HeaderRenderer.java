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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   04.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/**
 * The renderer for the spreadsheets row and column header.
 *
 * @author Heiko Hofer
 */
abstract class HeaderRenderer extends JLabel implements
        TableCellRenderer {
    private static final long serialVersionUID = 1563599810171319482L;

    private int m_fontStyle;

    private Color m_foreground;

    private Color m_background;

    private Color m_selForeground;

    private Color m_selBackground;

    private Color m_notInOutputForeground;

    private Color m_notInOutputBackground;

    private Color m_notInOutputSelForeground;

    private Color m_notInOutputSelBackground;

    private boolean m_showOutputTable;

    /**
     * Create new instance.
     */
    public HeaderRenderer() {
        m_fontStyle = Font.PLAIN;
    }

    /**
     * Returns true when given cell is in the output table.
     *
     * @param row the row
     * @param column the column
     * @return true when given cell is in the output table
     */
    abstract boolean isInOutputTable(final int row, final int column);

    /**
     * Define whether the output table should be highlighted.
     *
     * @param show true when the output table should be highlighted.
     */
    public void showOutputTable(final boolean show) {
        m_showOutputTable = show;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean selected, final boolean focused,
            final int row, final int column) {
        if (m_showOutputTable && !isInOutputTable(row, column)) {
            if (selected) {
                setForeground(m_notInOutputSelForeground);
                setBackground(m_notInOutputSelBackground);
            } else {
                setForeground(m_notInOutputForeground);
                setBackground(m_notInOutputBackground);
            }
        } else {
            if (selected) {
                setForeground(m_selForeground);
                setBackground(m_selBackground);
            } else {
                setForeground(m_foreground);
                setBackground(m_background);
            }
        }

        setText(value.toString());

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUI() {
        super.updateUI();
        applyDefaults();
    }

    /**
     * Override this in sub class to change colors.
     */
    protected void applyDefaults() {
        setOpaque(true);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        m_foreground = UIManager.getColor("TableHeader.foreground");
        m_background = UIManager.getColor("TableHeader.background");
        m_selForeground = UIManager.getColor("Table.selectionForeground");
        m_selBackground = UIManager.getColor("Table.selectionBackground");
        m_notInOutputForeground = Color.gray;
        m_notInOutputBackground = Color.lightGray;
        m_notInOutputSelForeground = Color.darkGray;
        m_notInOutputSelBackground = m_selBackground.darker();
        m_showOutputTable = false;
        setHorizontalAlignment(CENTER);
        m_fontStyle = Font.PLAIN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(final Graphics g) {
        Font prevFont = g.getFont();
        g.setFont(prevFont.deriveFont(m_fontStyle));
        super.paintComponent(g);
        g.setFont(prevFont);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidate() {
        // do nothing, overridden for performance reasons
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revalidate() {
        // do nothing, overridden for performance reasons
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        // do nothing, overridden for performance reasons
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void repaint(final int x, final int y, final int width,
            final int height) {
        // do nothing, overridden for performance reasons
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void repaint(final Rectangle r) {
        // do nothing, overridden for performance reasons
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void repaint() {
        // do nothing, overridden for performance reasons
    }

}

