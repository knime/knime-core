/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    16.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class SettingsModelColorNameColumns extends SettingsModel {
    
//    private static final String VALUE_SEPARATOR = "@";

    private static final String CFG_COLOR_COLUMN_NAMES = "colorColumnNames";
    
    private ColorNameColumn[] m_value;

    private final String m_configName;

    /**
     * Creates a new object holding a string value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelColorNameColumns(final String configName,
            final ColorNameColumn[] defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_value = defaultValue;
        m_configName = configName;
    }
    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel#createClone()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelColorNameColumns createClone() {
        return new SettingsModelColorNameColumns(m_configName, m_value);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #getConfigName()
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #getModelTypeID()
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_colorColumns";
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #loadSettingsForDialog(org.knime.core.node.NodeSettingsRO, 
     * org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) {
        try {
            final ColorNameColumn[] columns = 
                loadColorColumns(m_configName, settings);
            if (columns != null) {
                //only if the settings return a value use it
                setColorNameColumns(columns);
            }
        } catch (IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        } 
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #saveSettingsForDialog(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) {
        saveSettingsForModel(settings);
    }
    
    private static void saveColorColumns(final String configName, 
            final NodeSettingsWO settings, final ColorNameColumn[] columns) {
        final Config config = settings.addConfig(configName);
        if (columns == null || columns.length < 1) {
            config.addStringArray(CFG_COLOR_COLUMN_NAMES, new String[0]);
            return;
        }
        final String[] columnNames = new String[columns.length];
        for (int i = 0, length = columns.length; i < length; i++) {
            columnNames[i] = columns[i].getColumnName();
        }
        config.addStringArray(CFG_COLOR_COLUMN_NAMES, columnNames);
        for (ColorNameColumn column : columns) {
            config.addInt(column.getColumnName(), 
                    column.getColor().getRGB());
        }
    }

    private static ColorNameColumn[] loadColorColumns(final String configName,
            final NodeSettingsRO settings) {
        try {
            final Config config = settings.getConfig(configName);
            final String[] columnNames = 
                config.getStringArray(CFG_COLOR_COLUMN_NAMES);
            if (columnNames == null) {
                return null;
            }
            final ColorNameColumn[] columns = 
                new ColorNameColumn[columnNames.length];
            for (int i = 0, length = columnNames.length; i < length; i++) {
                final String columnName = columnNames[i];
                final int rgb = config.getInt(columnName);
                columns[i] = new ColorNameColumn(new Color(rgb), columnName);
            }
            return columns;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #loadSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setColorNameColumns(loadColorColumns(m_configName, settings));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #saveSettingsForModel(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        saveColorColumns(m_configName, settings, m_value);
    }

    /**
     * @return the (a copy of the) current value stored.
     */
    public ColorNameColumn[] getColorNameColumns() {
        if (m_value == null) {
            return null;
        }
        final ColorNameColumn[] result = new ColorNameColumn[m_value.length];
        System.arraycopy(m_value, 0, result, 0, m_value.length);
        return result;
    }
    
    /**
     * set the value stored to (a copy of) the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setColorNameColumns(final ColorNameColumn... newValue) {
        boolean same;
        if (newValue == null) {
            same = (m_value == null);
        } else {
            if ((m_value == null) || (m_value.length != newValue.length)) {
                same = false;
            } else {
                List<ColorNameColumn> current = Arrays.asList(m_value);
                same = true;
                for (ColorNameColumn s : newValue) {
                    if (!current.contains(s)) {
                        same = false;
                        break;
                    }
                }
            }
        }
        
        if (newValue == null) {
            m_value = null;
        } else {
            m_value = new ColorNameColumn[newValue.length];
            System.arraycopy(newValue, 0, m_value, 0, newValue.length);
        }
        
        if (!same) {
            notifyChangeListeners();
        }
    }
    
//    /**
//     * @return the (a copy of the) current value stored.
//     */
//    public String[] getStringArrayValue() {
//        return createStringValues(m_value);
//    }
//
//    /**
//     * set the value stored to (a copy of) the new value.
//     * 
//     * @param newValue the new value to store.
//     */
//    public void setStringArrayValue(final String[] newValue) {
//        boolean same;
//        final ColorNameColumn[] newVals = parseStringValues(newValue);
//        if (newVals == null) {
//            same = (m_value == null);
//        } else {
//            if ((m_value == null) || (m_value.length != newVals.length)) {
//                same = false;
//            } else {
//                List<ColorNameColumn> current = Arrays.asList(m_value);
//                same = true;
//                for (ColorNameColumn s : newVals) {
//                    if (!current.contains(s)) {
//                        same = false;
//                        break;
//                    }
//                }
//            }
//        }
//        if (newVals == null) {
//            m_value = null;
//        } else {
//            m_value = new ColorNameColumn[newVals.length];
//            System.arraycopy(newVals, 0, m_value, 0, newVals.length);
//        }
//        
//        if (!same) {
//            notifyChangeListeners();
//        }
//    }
    
    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #validateSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        settings.getStringArray(m_configName);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     * #prependChangeListener(javax.swing.event.ChangeListener)
     */
    @Override
    protected void prependChangeListener(final ChangeListener l) {
        super.prependChangeListener(l);
    }
}
