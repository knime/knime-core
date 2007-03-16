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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class SettingsModelColorNameColumns extends SettingsModel {
    
    private static final String VALUE_SEPARATOR = "@";

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
            String[] stringVals = createStringValues(m_value);
            // use the current value, if no value is stored in the settings
            setStringArrayValue(settings.getStringArray(m_configName, 
                    stringVals));
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
    
    private String[] createStringValues(final ColorNameColumn[] value) {
        if (value == null) {
            return null;
        }
        String[] vals = new String[value.length];
        StringBuilder buf = new StringBuilder();
        for (int i = 0, length = value.length; i < length; i++) {
            final ColorNameColumn column = value[i];
            final Color color = column.getColor();
            buf.append(color.getRGB());
            buf.append(VALUE_SEPARATOR);
            buf.append(column.getColumnName());
            vals[i] = buf.toString();
            buf.setLength(0);
        }
        return vals;
    }
    
    private ColorNameColumn[] parseStringValues(final String[] value) {
        if (value == null) {
            return null;
        }
        List<ColorNameColumn> vals = 
            new ArrayList<ColorNameColumn>(value.length);
        final StringBuilder nameBuf = new StringBuilder();
        for (int i = 0, length = value.length; i < length; i++) {
            final String string = value[i];
            if (string == null) {
                continue;
            }
            StringTokenizer tok = 
                new StringTokenizer(string, VALUE_SEPARATOR);
            int valueCounter = 0;
            String colorString = null;
            try {
                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken();
                    if (valueCounter == 0) {
                        colorString = token;
                    } else {
                        nameBuf.append(token);
                    }
                    valueCounter++;
                }
                final Color color = new Color(Integer.parseInt(colorString));
                vals.add(new ColorNameColumn(color, nameBuf.toString()));
                nameBuf.setLength(0);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Error while parsing internal number: " 
                        + e.getMessage());
            }
        }
        if (vals.size() < 1) {
            return null;
        }
        return vals.toArray(new ColorNameColumn[vals.size()]);
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
            setStringArrayValue(settings.getStringArray(m_configName));
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
        settings.addStringArray(m_configName, getStringArrayValue());
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
    
    /**
     * @return the (a copy of the) current value stored.
     */
    public String[] getStringArrayValue() {
        return createStringValues(m_value);
    }

    /**
     * set the value stored to (a copy of) the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setStringArrayValue(final String[] newValue) {
        boolean same;
        final ColorNameColumn[] newVals = parseStringValues(newValue);
        if (newVals == null) {
            same = (m_value == null);
        } else {
            if ((m_value == null) || (m_value.length != newVals.length)) {
                same = false;
            } else {
                List<ColorNameColumn> current = Arrays.asList(m_value);
                same = true;
                for (ColorNameColumn s : newVals) {
                    if (!current.contains(s)) {
                        same = false;
                        break;
                    }
                }
            }
        }
        if (newVals == null) {
            m_value = null;
        } else {
            m_value = new ColorNameColumn[newVals.length];
            System.arraycopy(newVals, 0, m_value, 0, newVals.length);
        }
        
        if (!same) {
            notifyChangeListeners();
        }
    }
    
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
