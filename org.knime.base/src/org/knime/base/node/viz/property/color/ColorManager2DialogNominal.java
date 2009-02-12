/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * A dialog panel used to set color for nominal values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ColorManager2DialogNominal extends JPanel {
    
    /** Keeps mapping from data cell name to color. */
    private final Map<String, Map<DataCell, ColorAttr>> m_map;

    /** Keeps the all possible column values. */
    private final JList m_columnValues;
    
    /** list model for column values. */
    private final DefaultListModel m_columnModel;
    
    private int m_alpha = 255;

    /**
     * Creates an empty nominal dialog.
     */
    ColorManager2DialogNominal() {
        super(new GridLayout());
        
        // map for key to color mapping
        m_map = new LinkedHashMap<String, Map<DataCell, ColorAttr>>();

        // create list for possible column values
        m_columnModel = new DefaultListModel();
        m_columnValues = new JList(m_columnModel);
        m_columnValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_columnValues.setCellRenderer(new ColorManager2IconRenderer());
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
        Map<DataCell, ColorAttr> map = m_map.get(column);
        boolean flag;
        if (map == null) {
            m_columnModel.removeAllElements();
            m_columnValues.setEnabled(false);
            flag = false;
        } else {
            m_columnValues.setEnabled(true);
            for (DataCell cell : map.keySet()) {
                assert cell != null;
                ColorAttr color = map.get(cell);
                assert color != null;
                m_columnModel.addElement(
                        new ColorManager2Icon(cell, color.getColor()));
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
    void update(final String column, final ColorAttr color) {
        Object o = m_columnValues.getSelectedValue();
        if (o != null) {
            Map<DataCell, ColorAttr> map = m_map.get(column);
            ColorManager2Icon icon = (ColorManager2Icon)o;
            map.put(icon.getCell(), color);
            icon.setColor(color.getColor());
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
        if (set != null && !set.isEmpty()) {
            m_map.put(column, createColorMapping(set));
        }
    }

    /**
     * Create default color mapping for the given set of possible 
     * <code>DataCell</code> values.
     * @param set possible values
     * @return a map of possible value to color
     */
    static final Map<DataCell, ColorAttr> createColorMapping(
            final Set<DataCell> set) {
        if (set == null) {
            return Collections.EMPTY_MAP;
        }
        Map<DataCell, ColorAttr> map = new LinkedHashMap<DataCell, ColorAttr>();
        int idx = 0;
        for (DataCell cell : set) {
            // use Color, half saturated, half bright for base color
            Color color = Color.getColor(null, 
                Color.HSBtoRGB((float) idx++ / (float) set.size(), 1.0f, 1.0f));
            map.put(cell, ColorAttr.getInstance(color));
        }
        return map;
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
                ColorManager2Icon icon = (ColorManager2Icon)m_columnModel
                        .getElementAt(i);
                vals[i] = icon.getCell();
                Color c = icon.getColor();
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), m_alpha);
                settings.addInt(vals[i].toString(), c.getRGB());
            }
            settings.addDataCellArray(ColorManager2NodeModel.VALUES, vals);
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
                ColorManager2NodeModel.VALUES, (DataCell[])null);
        if (vals == null) {
            return;
        }
        Map<DataCell, ColorAttr> map = m_map.get(column);
        if (map == null) {
            return;
        }
        for (int i = 0; i < vals.length; i++) {
            if (map.containsKey(vals[i])) {
                Color dftColor = map.get(vals[i]).getColor();
                int c = settings.getInt(vals[i].toString(), dftColor.getRGB());
                Color color = new Color(c, true);
                m_alpha = color.getAlpha();
                color = new Color(color.getRGB(), false);
                map.put(vals[i], ColorAttr.getInstance(color));
            }
        }
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
