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
 *    16.01.2008 (Tobias Koetter): created
 */

package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;


/**
 * Extends the {@link SettingsModelString} to provide the {@link #useRowID()}
 * method to check if the RowID should be used instead of a column.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SettingsModelColumnName extends SettingsModelString {

    private static final String CFG_COLUMN_NAME = "columnName";

    private static final String CFG_ROWID = "useRowID";

    private final String m_configName;

    private boolean m_useRowID = false;

    /**Creates a new object holding a column name if the useRowID method
     * returns <code>false</code>. If the useRowID method returns
     * <code>true</code> the user has selected the RowID.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelColumnName(final String configName,
            final String defaultValue) {
        super(CFG_COLUMN_NAME, defaultValue);
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_configName = configName;
    }

    private SettingsModelColumnName(final String configName,
            final String defaultValue, final boolean useRowID) {
        super(CFG_COLUMN_NAME, defaultValue);
        m_configName = configName;
        m_useRowID = useRowID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelColumnName createClone() {
        return new SettingsModelColumnName(getConfigName(), getStringValue(),
                m_useRowID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * @param columnName the selected column name
     * @param useRowID <code>true</code> if the RowID should be used instead
     * of a column
     */
    public void setSelection(final String columnName, final boolean useRowID) {
       final boolean useRowIDChanged = setUseRowID(useRowID);
        final boolean changed = updateStringValue(columnName);
        if (useRowIDChanged && !changed) {
            //If the column name has changed
            //the notifyChangeListener method is called from the
            //updateStringValue method by calling the setStringValue method
            notifyChangeListeners();
        }
    }

    /**
     * @param useRowID <code>true</code> if the RowID should be used
     * @return <code>true</code> if the new value is different from the previous
     * value
     */
    private boolean setUseRowID(final boolean useRowID) {
        final boolean changed = m_useRowID != useRowID;
            m_useRowID = useRowID;
        return changed;
    }

    /**
     * @param value the value to set
     * @return <code>true</code> if the argument is different from the previous
     * value
     */
    private boolean updateStringValue(final String value) {
        boolean changed = false;
        final String oldValue = getStringValue();
        if (value == null) {
            changed = (oldValue != null);
        } else {
            changed = !value.equals(oldValue);
        }
        setStringValue(value);
        return changed;
    }


    /**
     * @return <code>true</code> if the RowID should be used instead of
     * a column
     */
    public boolean useRowID() {
        return m_useRowID;
    }

    /**
     * @return the name of the column or <code>null</code> if the none column
     * or the RowID column was selected. To check if the RowID column was
     * selected  call the {@link #useRowID()} method.
     */
    public String getColumnName() {
        return getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final NodeSettingsRO subSettings =
            settings.getNodeSettings(m_configName);
        subSettings.getBoolean(CFG_ROWID);
        super.validateSettingsForModel(subSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        final NodeSettingsWO subSettings =
            settings.addNodeSettings(m_configName);
        subSettings.addBoolean(CFG_ROWID, m_useRowID);
        super.saveSettingsForDialog(subSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final NodeSettingsWO subSettings =
            settings.addNodeSettings(m_configName);
        subSettings.addBoolean(CFG_ROWID, m_useRowID);
        super.saveSettingsForModel(subSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        final boolean oldUseRowID = m_useRowID;
        try {
            final NodeSettingsRO subSettings =
                settings.getNodeSettings(m_configName);
            final boolean useRowId = subSettings.getBoolean(CFG_ROWID);
            final boolean rowIDChanged = setUseRowID(useRowId);
            final String oldStringVal = super.getStringValue();
            super.loadSettingsForDialog(subSettings, specs);
            final String newStringVal = super.getStringValue();
            boolean stringValChanged;
            if (oldStringVal == null) {
                stringValChanged = newStringVal != null;
            } else {
                stringValChanged = !oldStringVal.equals(newStringVal);
            }
            if (rowIDChanged && !stringValChanged) {
                //If the column name has changed
                //the notifyChangeListener method is called from the
                //updateStringValue method by calling the setStringValue method
                notifyChangeListeners();
            }
        } catch (final InvalidSettingsException iae) {
            // if the argument is not accepted: keep the old value.
            m_useRowID = oldUseRowID;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no default value, throw an exception instead
        final NodeSettingsRO subSettings =
            settings.getNodeSettings(m_configName);
        final boolean useRowId = subSettings.getBoolean(CFG_ROWID);
        final boolean rowIDChanged = setUseRowID(useRowId);
        final String oldStringVal = super.getStringValue();
        super.loadSettingsForModel(subSettings);
        final String newStringVal = super.getStringValue();
        boolean stringValChanged;
        if (oldStringVal == null) {
            stringValChanged = newStringVal != null;
        } else {
            stringValChanged = !oldStringVal.equals(newStringVal);
        }
        if (rowIDChanged && !stringValChanged) {
            //If the column name has changed
            //the notifyChangeListener method is called from the
            //updateStringValue method by calling the setStringValue method
            notifyChangeListeners();
        }
    }
}
