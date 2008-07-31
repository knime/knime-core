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
 * History
 *   09.02.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * A dialog panel used to set color for nominal values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ColorManagerDialogNominal extends JPanel {
    /** Keeps mapping from data cell name to color. */
    private final LinkedHashMap<String, LinkedHashMap<DataCell, Color>> m_map;

    /** Keeps the all possible column values. */
    private final JList m_columnValues;

    private final DefaultListModel m_columnModel;

    /**
     * Creates an empty nominal dialog.
     */
    ColorManagerDialogNominal() {
        super(new GridLayout());

        // map for key to color mapping
        m_map = new LinkedHashMap<String, LinkedHashMap<DataCell, Color>>();

        // create list for possible column values
        m_columnModel = new DefaultListModel();
        m_columnValues = new JList(m_columnModel);
        m_columnValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_columnValues.setCellRenderer(new ColorManagerIconRenderer());
        super.add(new JScrollPane(m_columnValues));
    }

    /**
     * Called is a new column is selected. If the column is null every
     * 
     * @param column the new selected column
     * @return <code>true</code>, if the call caused any changes
     */
    boolean select(final String column) {
        m_columnModel.removeAllElements();
        Map<DataCell, Color> map = m_map.get(column);
        boolean flag;
        if (map == null) {
            m_columnModel.removeAllElements();
            m_columnValues.setEnabled(false);
            flag = false;
        } else {
            m_columnValues.setEnabled(true);
            for (DataCell cell : map.keySet()) {
                assert cell != null;
                Color color = map.get(cell);
                assert color != null;
                m_columnModel.addElement(new ColorManagerIcon(cell, color));
            }
            flag = true;
        }
        super.validate();
        super.repaint();
        return flag;
    }

    /**
     * Select new color for the selected attribute value of the the selected
     * column.
     * 
     * @param column the selected column
     * @param color the new color
     */
    void update(final String column, final Color color) {
        Object o = m_columnValues.getSelectedValue();
        if (o != null) {
            LinkedHashMap<DataCell, Color> map = m_map.get(column);
            ColorManagerIcon icon = (ColorManagerIcon)o;
            map.put(icon.getCell(), color);
            icon.setColor(color);
            super.validate();
            super.repaint();
        }
    }

    /**
     * Adds the given set of possible values to the internal structure by the
     * given column name.
     * 
     * @param column the column name
     * @param set the set of possible values for this column
     */
    void add(final String column, final Set<DataCell> set) {
        LinkedHashMap<DataCell, Color> map = 
            new LinkedHashMap<DataCell, Color>();
        if (set != null && !set.isEmpty()) {
            int idx = -1;
            for (DataCell cell : set) {
                map.put(cell, generateColor(++idx, set.size()));
            }
            m_map.put(column, map);
        }
    }

    private static Color generateColor(final int idx, final int size) {
        // use Color, half saturated, half bright for base color
        return Color.getColor(null, Color.HSBtoRGB((float)idx / (float)size,
                1.0f, 1.0f));
    }

    /**
     * Removes all elements for the internal map.
     */
    void removeAllElements() {
        m_map.clear();
        m_columnModel.removeAllElements();
        this.setEnabled(false);
    }

    /**
     * Save settings that are the current color settings.
     * 
     * @param settings to write to
     * @throws InvalidSettingsException if no nominal value are defined on the
     *             selected column
     */
    void saveSettings(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        int len = m_columnModel.getSize();
        if (len > 0) {
            DataCell[] vals = new DataCell[len];
            for (int i = 0; i < m_columnModel.getSize(); i++) {
                ColorManagerIcon icon = (ColorManagerIcon)m_columnModel
                        .getElementAt(i);
                vals[i] = icon.getCell();
                settings.addInt(vals[i].toString(), icon.getColor().getRGB());
            }
            settings.addDataCellArray(ColorNodeModel.VALUES, vals);
        } else {
            throw new InvalidSettingsException("Make sure the selected column "
                    + "has nominal values defined within the domain.");
        }
    }

    /**
     * Reads the color settings for the given column.
     * 
     * @param settings to read from
     * @param column the selected column
     */
    void loadSettings(final NodeSettingsRO settings, final String column) {
        if (column == null) {
            return;
        }
        DataCell[] vals = settings.getDataCellArray(
                ColorNodeModel.VALUES, (DataCell[])null);
        if (vals == null) {
            return;
        }
        LinkedHashMap<DataCell, Color> map = m_map.get(column);
        if (map == null) {
            return;
        }
        for (int i = 0; i < vals.length; i++) {
            if (map.containsKey(vals[i])) {
                Color dftColor = map.get(vals[i]);
                Color color = new Color(settings.getInt(vals[i].toString(),
                        dftColor.getRGB()));
                map.put(vals[i], color);
            }
        }
    }
}
