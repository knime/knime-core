/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *    16.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class SettingsModelColorNameColumns extends SettingsModel {
    
//    private static final String VALUE_SEPARATOR = "@";

    private static final String CFG_COLOR_COLUMN_NAMES = "colorColumnNames";
    
    private ColorColumn[] m_value;

    private final String m_configName;

    /**
     * Creates a new object holding a string value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelColorNameColumns(final String configName,
            final ColorColumn[] defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_value = defaultValue;
        m_configName = configName;
    }
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelColorNameColumns createClone() {
        return new SettingsModelColorNameColumns(m_configName, m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_colorColumns";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, 
            final PortObjectSpec[] specs) {
        try {
            final ColorColumn[] columns = 
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
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) {
        saveSettingsForModel(settings);
    }
    
    private static void saveColorColumns(final String configName, 
            final NodeSettingsWO settings, final ColorColumn[] columns) {
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
        for (ColorColumn column : columns) {
            config.addInt(column.getColumnName(), 
                    column.getColor().getRGB());
        }
    }

    private static ColorColumn[] loadColorColumns(final String configName,
            final NodeSettingsRO settings) {
        try {
            final Config config = settings.getConfig(configName);
            final String[] columnNames = 
                config.getStringArray(CFG_COLOR_COLUMN_NAMES);
            if (columnNames == null) {
                return null;
            }
            final ColorColumn[] columns = 
                new ColorColumn[columnNames.length];
            for (int i = 0, length = columnNames.length; i < length; i++) {
                final String columnName = columnNames[i];
                final int rgb = config.getInt(columnName);
                columns[i] = new ColorColumn(new Color(rgb), columnName);
            }
            return columns;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        saveColorColumns(m_configName, settings, m_value);
    }

    /**
     * @return the (a copy of the) current value stored.
     */
    public ColorColumn[] getColorNameColumns() {
        if (m_value == null) {
            return null;
        }
        final ColorColumn[] result = new ColorColumn[m_value.length];
        System.arraycopy(m_value, 0, result, 0, m_value.length);
        return result;
    }
    
    /**
     * set the value stored to (a copy of) the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setColorNameColumns(final ColorColumn... newValue) {
        boolean same;
        if (newValue == null) {
            same = (m_value == null);
        } else {
            if ((m_value == null) || (m_value.length != newValue.length)) {
                same = false;
            } else {
                List<ColorColumn> current = Arrays.asList(m_value);
                same = true;
                for (ColorColumn s : newValue) {
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
            m_value = new ColorColumn[newValue.length];
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        settings.getStringArray(m_configName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prependChangeListener(final ChangeListener l) {
        super.prependChangeListener(l);
    }
}
