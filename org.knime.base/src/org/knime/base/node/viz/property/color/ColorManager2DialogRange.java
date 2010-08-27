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
 * -------------------------------------------------------------------
 * 
 * History
 *   09.02.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Dialog pane used to specify colors by minimum and maximum bounds.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorManager2DialogRange extends JPanel {
    private class DataCellColorEntry {
        private final DataCell m_cell;

        private Color m_color;

        /**
         * Create new cell and color entry.
         * 
         * @param cell the cell
         * @param color the color
         */
        DataCellColorEntry(final DataCell cell, final Color color) {
            assert color != null;
            m_color = color;
            m_cell = cell;

        }

        /**
         * @return the cell
         */
        DataCell getCell() {
            return m_cell;
        }

        /**
         * @param color the new color to set
         */
        void setColor(final Color color) {
            m_color = color;
        }

        /**
         * @return the color
         */
        Color getColor() {
            return m_color;
        }
    }

    /** Keeps mapping from data cell name to color. */
    private final LinkedHashMap<String, DataCellColorEntry[]> m_map;

    /** Keeps the all possible column values. */
    private final JList m_columnValues;

    private final DefaultListModel m_columnModel;

    private final ColorManager2RangeIcon m_rangeLabel;
    
    private int m_alpha = 255;

    /**
     * Creates a new empty dialog pane.
     */
    ColorManager2DialogRange() {
        super(new BorderLayout());
        m_rangeLabel = new ColorManager2RangeIcon();

        // map for key to color mapping
        m_map = new LinkedHashMap<String, DataCellColorEntry[]>();

        // create list for possible column values
        m_columnModel = new DefaultListModel();
        m_columnValues = new JList(m_columnModel);
        m_columnValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_columnValues.setCellRenderer(new ColorManager2IconRenderer());
        JPanel rangePanel = new JPanel(new GridLayout());
        rangePanel.add(new JScrollPane(m_columnValues));
        super.add(rangePanel, BorderLayout.CENTER);
        JPanel rangeBorder = new JPanel(new GridLayout());
        rangeBorder.setBorder(BorderFactory.createTitledBorder(" Preview "));
        rangeBorder.add(m_rangeLabel);
        super.add(rangeBorder, BorderLayout.SOUTH);

    }

    /**
     * Select new color for the selected attribute value of the the selected
     * column.
     * 
     * @param column the selected column
     * @param color the new color
     */
    void update(final String column, final Color color) {
        int idx = m_columnValues.getSelectedIndex();
        if (idx == 0 || idx == 1) {
            ColorManager2Icon icon = (ColorManager2Icon)m_columnValues
                    .getSelectedValue();
            icon.setColor(color);
            DataCell cell = icon.getCell();
            DataCellColorEntry ex = new DataCellColorEntry(cell, color);
            DataCellColorEntry[] e = m_map.get(column);
            assert e.length == 2;
            if (idx == 0) {
                m_map.put(column, new DataCellColorEntry[]{ex, e[1]});
                m_rangeLabel.setMinColor(color);
            }
            if (idx == 1) {
                m_map.put(column, new DataCellColorEntry[]{e[0], ex});
                m_rangeLabel.setMaxColor(color);
            }
            super.validate();
            super.repaint();
        }
    }

    /**
     * Called if the column selection has changed.
     * 
     * @param column the new selected column
     * @return <code>true</code>, if this call caused any changes
     */
    boolean select(final String column) {
        m_columnModel.removeAllElements();
        Object o = m_map.get(column);
        boolean flag;
        if (o == null) {
            m_columnModel.removeAllElements();
            m_columnValues.setEnabled(false);
            m_rangeLabel.setMinColor(ColorAttr.DEFAULT.getColor());
            m_rangeLabel.setMaxColor(ColorAttr.DEFAULT.getColor());
            flag = false;
        } else {
            m_columnValues.setEnabled(true);
            DataCellColorEntry[] e = (DataCellColorEntry[])o;
            m_columnModel.addElement(new ColorManager2Icon(e[0].getCell(),
                    "min=", e[0].getColor()));
            m_rangeLabel.setMinColor(e[0].getColor());
            m_columnModel.addElement(new ColorManager2Icon(e[1].getCell(),
                    "max=", e[1].getColor()));
            m_rangeLabel.setMaxColor(e[1].getColor());
            flag = true;
        }
        super.validate();
        super.repaint();
        return flag;
    }

    /**
     * Add new column with lower and upper bound.
     * 
     * @param column the column to add
     * @param low the lower bound
     * @param upp the upper bound
     */
    void add(final String column, final DataCell low, final DataCell upp) {
        DataCellColorEntry e1 = new DataCellColorEntry(low, Color.RED);
        DataCellColorEntry e2 = new DataCellColorEntry(upp, Color.GREEN);
        m_map.put(column, new DataCellColorEntry[]{e1, e2});
    }

    /**
     * Removes all elements.
     */
    void removeAllElements() {
        m_map.clear();
        m_columnModel.removeAllElements();
        this.setEnabled(false);
    }

    /**
     * Writes the color settings.
     * 
     * @param settings to write to
     */
    void saveSettings(final NodeSettingsWO settings) {
        assert m_columnModel.getSize() == 2;
        ColorManager2Icon i0 = (ColorManager2Icon)m_columnModel.getElementAt(0);
        Color c0 = new Color(i0.getColor().getRed(), i0.getColor().getGreen(), 
                i0.getColor().getBlue(), getAlpha());
        settings.addInt(ColorManager2NodeModel.MIN_COLOR, c0.getRGB());
        ColorManager2Icon i1 = (ColorManager2Icon)m_columnModel.getElementAt(1);
        Color c1 = new Color(i1.getColor().getRed(), i1.getColor().getGreen(), 
                i1.getColor().getBlue(), m_alpha);
        settings.addInt(ColorManager2NodeModel.MAX_COLOR, c1.getRGB());
    }

    /**
     * Reads color settings.
     * 
     * @param settings to read from
     * @param column the selected column
     */
    void loadSettings(final NodeSettingsRO settings, final String column) {
        if (column == null) {
            return;
        }
        int rgba0 = settings.getInt(ColorManager2NodeModel.MIN_COLOR,
                Color.RED.getRGB());
        Color c0 = new Color(rgba0, true);
        m_alpha = c0.getAlpha();
        int rgba1 = settings.getInt(ColorManager2NodeModel.MAX_COLOR,
                Color.GREEN.getRGB());
        Color c1 = new Color(rgba1, true);
        assert (m_alpha == c1.getAlpha());
        DataCellColorEntry[] ex = m_map.get(column);
        if (ex == null) {
            return;
        }
        ex[0].setColor(new Color(c0.getRGB(), false));
        ex[1].setColor(new Color(c1.getRGB(), false));
    }
    
    /**
     * @return intermediate alpha value as read from the current settings
     */
    final int getAlpha() {
        return m_alpha;
    }
    
    /**
     * @param alpha the new alpha value as set by the alpha color panel
     */
    final void setAlpha(final int alpha) {
        m_alpha = alpha;
    }
    
}
