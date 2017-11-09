/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Holds all information necessary to initialize the missing value handlers for all columns of a table.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class MVSettings {

    private static final String COL_SETTINGS_CFG = "columnSettings";

    private static final String DT_SETTINGS_CFG = "dataTypeSettings";

    private List<MVColumnSettings> m_columnSettings;

    private HashMap<String, MVIndividualSettings> m_generalSettings;

    /**
     * Default constructor for MVSettings.
     */
    public MVSettings() {
        m_columnSettings = new ArrayList<MVColumnSettings>();
        m_generalSettings = new HashMap<String, MVIndividualSettings>();
    }

    /**
     * Constructor for MVSettings where each column in the spec is configured
     * with a do nothing missing cell handler.
     * @param spec the DataTableSpec the settings are for.
     */
    public MVSettings(final DataTableSpec spec) {
        this();

        HashSet<DataType> types = new HashSet<DataType>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            types.add(spec.getColumnSpec(i).getType());
        }
        for (DataType t : types) {
            setSettingsForDataType(t, new MVIndividualSettings(getHandlerFactoryManager()));
        }
    }

    /**
     * @return a list of settings objects for groups of columns
     */
    public List<MVColumnSettings> getColumnSettings() {
        return m_columnSettings;
    }

    /**
     * Sets missing value handling settings for a data type.
     * @param dt the data type
     * @param settings the settings
     */
    public void setSettingsForDataType(final DataType dt, final MVIndividualSettings settings) {
        m_generalSettings.put(getTypeKey(dt), settings);
    }

    /**
     * Returns the missing value handling settings for a column.
     * @param col the column the setting should be retrieved for
     * @return the settings
     * @throws InvalidSettingsException when the set handler for a column is not valid for the column's data type
     */
    public MVIndividualSettings getSettingsForColumn(final DataColumnSpec col) throws InvalidSettingsException {
        for (MVColumnSettings colSetting : m_columnSettings) {
            for (String colName : colSetting.getColumns()) {
                if (colName.equals(col.getName())) {
                    // Check if there is a misconfiguration
                    if (!colSetting.getSettings().getFactory().isApplicable(col.getType())) {
                        throw new InvalidSettingsException("The selected missing value handler ("
                                            + colSetting.getSettings().getFactory().getDisplayName()
                                            + ") for column " + col.getName() + " cannot handle columns of type "
                                            + col.getType().toString());
                    }
                    return colSetting.getSettings();
                }
            }
        }
        // Nothing has been found: Fall back to data type setting
        return m_generalSettings.get(getTypeKey(col.getType()));
    }

    /**
     * Returns the missing value handling settings for a data type.
     * @param dt the data type
     * @return the settings
     */
    public MVIndividualSettings getSettingsForDataType(final DataType dt) {
        return m_generalSettings.get(getTypeKey(dt));
    }

    /**
     * Saves the settings.
     * @param settings the settings to save to
     */
    public void saveToSettings(final NodeSettingsWO settings) {
        NodeSettingsWO colSettings = settings.addNodeSettings(COL_SETTINGS_CFG);
        int i = 0;
        for (MVColumnSettings cols : m_columnSettings) {
            NodeSettingsWO colSetting = colSettings.addNodeSettings((Integer.toString(i++)));
            cols.saveSettings(colSetting);
        }
        NodeSettingsWO dtSettings = settings.addNodeSettings(DT_SETTINGS_CFG);
        for (Entry<String, MVIndividualSettings> entry : m_generalSettings.entrySet()) {
            NodeSettingsWO dtSetting = dtSettings.addNodeSettings(entry.getKey());
            entry.getValue().saveSettings(dtSetting);
        }
    }

    /**
     * Loads the settings from a read only node settings object.
     * @param settings the settings
     * @param repair if true, missing factories are replaced by the do nothing factory, else an exception is thrown
     * @return the missing value handling settings
     * @throws InvalidSettingsException when the settings cannot be retrieved
     */
    public String loadSettings(final NodeSettingsRO settings, final boolean repair) throws InvalidSettingsException {
        if (!settings.containsKey(COL_SETTINGS_CFG) || !settings.containsKey(DT_SETTINGS_CFG)) {
            return null;
        }
        StringBuffer warning = new StringBuffer();
        m_columnSettings.clear();
        m_generalSettings.clear();
        NodeSettingsRO colSettings = settings.getNodeSettings(COL_SETTINGS_CFG);
        for (String key : colSettings.keySet()) {
            MVColumnSettings colSet = new MVColumnSettings(getHandlerFactoryManager());
            String w = colSet.loadSettings(colSettings.getNodeSettings(key), repair);
            if (w != null) {
                if (warning.length() > 0) {
                    warning.append("\n");
                }
                warning.append(w);
            }
            this.m_columnSettings.add(colSet);
        }

        NodeSettingsRO dtSettings = settings.getNodeSettings(DT_SETTINGS_CFG);
        for (String key : dtSettings.keySet()) {
            MVIndividualSettings dtSetting = new MVIndividualSettings(getHandlerFactoryManager());
            String w = dtSetting.loadSettings(dtSettings.getNodeSettings(key), repair);
            if (w != null) {
                if (warning.length() > 0) {
                    warning.append("\n");
                }
                warning.append(w);
            }
            this.m_generalSettings.put(key, dtSetting);
        }
        if (warning.length() == 0) {
            return null;
        } else {
            return warning.toString();
        }
    }

    /**
     * Adds do nothing handlers for all types that have no setting yet.
     * @param dataTableSpec the spec for which to configure
     */
    public void configure(final DataTableSpec dataTableSpec) {
        for (int i = 0; i < dataTableSpec.getNumColumns(); i++) {
            DataType type = dataTableSpec.getColumnSpec(i).getType();
            if (!m_generalSettings.containsKey(getTypeKey(type))) {
                setSettingsForDataType(type, new MVIndividualSettings(getHandlerFactoryManager()));
            }
        }
    }

    private String getTypeKey(final DataType type) {
        // If the cell class is not available, we use all data values
        // This is a fix for bug 6253
        if (type.getCellClass() != null) {
            return type.getCellClass().getCanonicalName();
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("Non-Native ");
            List<Class<? extends DataValue>> valueClasses = type.getValueClasses();
            for (int i = 0; i < valueClasses.size(); i++) {
                if (i > 0) {
                    sb.append(";");
                }
                Class<? extends DataValue> c = valueClasses.get(i);
                sb.append(c.getName());
            }
            return sb.toString();
        }
    }

    /** @return manager keeping the missing value handler factories */
    protected MissingCellHandlerFactoryManager getHandlerFactoryManager() {
        return MissingCellHandlerFactoryManager.getInstance();
    }
}
