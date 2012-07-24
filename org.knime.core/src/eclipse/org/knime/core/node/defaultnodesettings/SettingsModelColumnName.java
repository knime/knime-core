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
        if ((configName == null) || "".equals(configName)) {
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
