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
 */
package org.knime.base.node.preproc.missingval;

import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * An object that holds some the properties how to handle missing values in an
 * individual column (called individual) or in columns of one type (called
 * meta). This object holds all properties that can be set in one single
 * component in the missing value dialog, i.e.
 * <ul>
 * <li>name of column (or null for meta-column)</li>
 * <li>type {String, Double, Int, Others}</li>
 * <li>method how to handle</li>
 * <li>fixed replacement (if method is "replace by fixed value"),</li>
 * </ul>
 * where name and type are read only.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class ColSetting {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MissingValueHandlingNodeModel.class);

    /** Type of column: anything but String, Double, Int. */
    public static final int TYPE_UNKNOWN = -1;

    /** Type of column: Double. */
    public static final int TYPE_DOUBLE = 0;

    /** Type of column: Int. */
    public static final int TYPE_INT = 1;

    /** Type of column: String. */
    public static final int TYPE_STRING = 2;

    /** Method: Do nothing, leave untouched, available for all types. */
    public static final int METHOD_NO_HANDLING = -1;

    /** Method: Remove row from data set, available for all types. */
    public static final int METHOD_IGNORE_ROWS = 0;

    /** Method: Replace by fixed value, available for Double, Int, String. */
    public static final int METHOD_FIX_VAL = 1;

    /** Method: Replace by mean, available for Double and Int (rounded). */
    public static final int METHOD_MEAN = 2;

    /** Method: Replace by min in column, available for Double and Int. */
    public static final int METHOD_MIN = 3;

    /** Method: Replace by max in column, available for Double and Int. */
    public static final int METHOD_MAX = 4;

    /** Method: Replace by most frequent value, 
     * available for String. */
    public static final int METHOD_MOST_FREQUENT = 5;
    
    /** NodeSettings key: write method. */
    protected static final String CFG_METHOD = "miss_method";

    /** NodeSettings key: write column name (only for individual columns). */
    protected static final String CFG_COLNAME = "col_names";

    /** NodeSettings key: write column type. */
    protected static final String CFG_TYPE = "col_types";

    /** NodeSettings key: write fixed value replacement (if any). */
    protected static final String CFG_FIXVAL = "fix_val";

    /** NodeSettings branch identifier for meta settings. */
    protected static final String CFG_META = "meta_colsetting";

    /**
     * NodeSettings branch identifier for individual settings.
     */
    protected static final String CFG_INDIVIDUAL = "individual_colsetting";

    /**
     * NodeSettings branch identifier for meta setting, String
     * (Individual columns have their name as identifier).
     */
    protected static final String CFG_META_STRING = "meta_string";

    /**
     * NodeSettings branch identifier for meta setting, Double.
     */
    protected static final String CFG_META_DOUBLE = "meta_double";

    /**
     * NodeSettings branch identifier for meta setting, Int.
     */
    protected static final String CFG_META_INT = "meta_int";

    /**
     * NodeSettings branch identifier for meta setting, Other.
     */
    protected static final String CFG_META_OTHER = "meta_other";

    /** Name of the column or null if meta column. */
    private String m_name;

    /** Type of the column, e.g. TYPE_INT. */
    private int m_type;

    /** Method how to handle missing values, e.g. METHOD_MIN. */
    private int m_method;

    /** Fixed value replacement, if any. */
    private DataCell m_fixCell;

    /** Private constructor, used by the load method. */
    private ColSetting() {
    }

    /**
     * Constructor for meta column setting.
     * 
     * @param type the type of the meta column
     */
    public ColSetting(final int type) {
        m_name = null;
        switch (type) {
        case TYPE_UNKNOWN:
        case TYPE_DOUBLE:
        case TYPE_INT:
        case TYPE_STRING:
            break;
        default:
            throw new IllegalArgumentException("No such type: " + type);
        }
        m_type = type;
        setMethod(METHOD_NO_HANDLING);
    }

    /**
     * Constructor for individual column.
     * 
     * @param spec the spec to the column
     */
    public ColSetting(final DataColumnSpec spec) {
        m_name = spec.getName();
        DataType type = spec.getType();
        // NOTE: It's important here to check first for double since
        // DoubleCell.TYPE is a super type of IntCell.TYPE.
        // In other words: If type is DoubleCell.TYPE
        // the type.isOneSuperTypeOf(IntCell.TYPE) is true
        // (that is the second check below)
        if (type.isASuperTypeOf(DoubleCell.TYPE)) {
            m_type = TYPE_DOUBLE;
        } else if (type.isASuperTypeOf(IntCell.TYPE)) {
            m_type = TYPE_INT;
        } else if (type.isASuperTypeOf(StringCell.TYPE)) {
            m_type = TYPE_STRING;
        } else {
            m_type = TYPE_UNKNOWN;
        }
        setMethod(METHOD_NO_HANDLING);
    }

    /**
     * @return the method
     */
    public int getMethod() {
        return m_method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(final int method) {
        m_method = method;
    }

    /**
     * @return the replace
     */
    public DataCell getFixCell() {
        return m_fixCell;
    }

    /**
     * @param newFix the replace to set
     */
    public void setFixCell(final DataCell newFix) {
        m_fixCell = newFix;
    }

    /**
     * @return returns the type
     */
    public int getType() {
        return m_type;
    }

    /**
     * @return returns the name or <code>null</code> if
     *         {@link #isMetaConfig()} returns <code>true</code>
     */
    public String getName() {
        return m_name;
    }

    /**
     * Is this config a meta-config?
     * 
     * @return <code>true</code> if it is
     */
    public boolean isMetaConfig() {
        return getName() == null;
    }

    /**
     * Loads settings from a NodeSettings object, used in
     * {@link org.knime.core.node.NodeModel}.
     * 
     * @param settings the (sub-) config to load from
     * @throws InvalidSettingsException if any setting is missing
     */
    protected void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // may be null to indicate meta config
        String name = null;
        if (settings.containsKey(CFG_COLNAME)) {
            name = settings.getString(CFG_COLNAME);
        }
        int method = settings.getInt(CFG_METHOD);
        int type = settings.getInt(CFG_TYPE);
        DataCell fixVal = null;
        switch (method) {
        case ColSetting.METHOD_NO_HANDLING:
        case ColSetting.METHOD_IGNORE_ROWS:
        case ColSetting.METHOD_MEAN:
        case ColSetting.METHOD_MIN:
        case ColSetting.METHOD_MAX:
        case ColSetting.METHOD_MOST_FREQUENT:
            break;
        case ColSetting.METHOD_FIX_VAL:
            DataType superType;
            String errorType;
            switch (type) {
            case ColSetting.TYPE_DOUBLE:
                fixVal = settings.getDataCell(CFG_FIXVAL);
                superType = DoubleCell.TYPE;
                errorType = "Type Double";
                break;
            case ColSetting.TYPE_INT:
                fixVal = settings.getDataCell(CFG_FIXVAL);
                superType = IntCell.TYPE;
                errorType = "Type Int";
                break;
            case ColSetting.TYPE_STRING:
                superType = StringCell.TYPE;
                fixVal = settings.getDataCell(CFG_FIXVAL);
                errorType = "Type String";
                break;
            default:
                throw new InvalidSettingsException(
                        "Unable to define fix value for unknown type");
            }
            if (fixVal == null) {
                throw new InvalidSettingsException(
                        "No replacement value for column: "
                                + (isMetaConfig() ? "meta" : m_name.toString())
                                + "(" + errorType + ")");
            }
            if (!superType.isASuperTypeOf(fixVal.getType())) {
                throw new InvalidSettingsException(
                        "Wrong type of replacement value for column: "
                                + (isMetaConfig() ? "meta" : m_name.toString())
                                + "(" + errorType + "): " + fixVal.getType());
            }
            break;
        default:
            throw new InvalidSettingsException("Unknown method: " + method);
        }
        m_name = name;
        m_method = method;
        m_type = type;
        m_fixCell = fixVal;
    }

    /**
     * Save settings to config object.
     * 
     * @param settings to save to
     */
    protected void saveSettings(final NodeSettingsWO settings) {
        if (!isMetaConfig()) {
            settings.addString(CFG_COLNAME, m_name);
        }
        settings.addInt(CFG_METHOD, m_method);
        settings.addInt(CFG_TYPE, m_type);
        if (m_method == METHOD_FIX_VAL) {
            assert (m_fixCell != null);
            switch (m_type) {
            case TYPE_DOUBLE:
                settings.addDataCell(CFG_FIXVAL, m_fixCell);
                break;
            case TYPE_INT:
                settings.addDataCell(CFG_FIXVAL, m_fixCell);
                break;
            case TYPE_STRING:
                settings.addDataCell(CFG_FIXVAL, m_fixCell);
                break;
            default:
                throw new RuntimeException("Cannot use fixed value "
                        + "for unknown type. (Column name '" + m_name + "')");
            }
        }
    }

    /**
     * Helper that load meta settings from a config object, used in NodeModel.
     * 
     * @param settings to load from
     * @return meta settings
     * @throws InvalidSettingsException if errors occur
     */
    protected static ColSetting[] loadMetaColSettings(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO subConfig = settings.getNodeSettings(CFG_META);
        Map<String, ColSetting> map = loadSubConfigs(subConfig);
        return map.values().toArray(new ColSetting[0]);
    }

    /**
     * Helper that loads meta settings from a config object, used in NodeDialog.
     * 
     * @param settings to load from
     * @param spec To be used for default init
     * @return meta settings
     */
    protected static ColSetting[] loadMetaColSettings(
            final NodeSettingsRO settings, final DataTableSpec spec) {
        LinkedHashMap<String, ColSetting> defaultsHash = 
            new LinkedHashMap<String, ColSetting>();
        if (spec.containsCompatibleType(IntValue.class)) {
            defaultsHash.put(CFG_META_INT, new ColSetting(TYPE_INT));
        }
        if (spec.containsCompatibleType(DoubleValue.class)) {
            defaultsHash.put(CFG_META_DOUBLE, new ColSetting(TYPE_DOUBLE));
        }
        if (spec.containsCompatibleType(StringValue.class)) {
            defaultsHash.put(CFG_META_STRING, new ColSetting(TYPE_STRING));
        }
        defaultsHash.put(CFG_META_OTHER, new ColSetting(TYPE_UNKNOWN));
        // lets see if the CFG_META branch is in the settings object
        if (settings.containsKey(CFG_META)) {
            NodeSettingsRO subConfig;
            try {
                subConfig = settings.getNodeSettings(CFG_META);
                Map<String, ColSetting> subDefaults = loadSubConfigs(subConfig);
                defaultsHash.putAll(subDefaults);
            } catch (InvalidSettingsException ise) {
                LOGGER.debug("Error loading subconfig: " + CFG_META, ise);
            }

        }
        return defaultsHash.values().toArray(new ColSetting[0]);
    }

    /**
     * Helper that load individual settings from a config object, used in
     * NodeModel.
     * 
     * @param settings to load from
     * @return individual settings
     * @throws InvalidSettingsException if errors occur
     */
    protected static ColSetting[] loadIndividualColSettings(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO subConfig = settings.getNodeSettings(CFG_INDIVIDUAL);
        Map<String, ColSetting> map = loadSubConfigs(subConfig);
        return map.values().toArray(new ColSetting[0]);
    }

    /**
     * Helper that individual settings from a config object, used in NodeDialog.
     * 
     * @param settings to load from
     * @param spec ignored, used here to differ from method that is used by
     *            {@link org.knime.core.node.NodeModel}
     * @return individual settings
     */
    protected static ColSetting[] loadIndividualColSettings(
            final NodeSettingsRO settings, final DataTableSpec spec) {
        assert (spec == spec); // avoid checkstyle complain
        Map<String, ColSetting> individHash = 
            new LinkedHashMap<String, ColSetting>();
        if (settings.containsKey(CFG_INDIVIDUAL)) {
            NodeSettingsRO subConfig;
            try {
                subConfig = settings.getNodeSettings(CFG_INDIVIDUAL);
                Map<String, ColSetting> subDefaults = loadSubConfigs(subConfig);
                individHash.putAll(subDefaults);
            } catch (InvalidSettingsException ise) {
                LOGGER.debug("Error loading subconfig: " + CFG_INDIVIDUAL, ise);
            }
        }
        return individHash.values().toArray(new ColSetting[0]);
    }

    /*
     * Get ColSetting objects in a map, mapping name to ColSetting.
     */
    private static Map<String, ColSetting> loadSubConfigs(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        LinkedHashMap<String, ColSetting> result = 
            new LinkedHashMap<String, ColSetting>();
        for (String key : settings.keySet()) { // TODO CONFIG
            NodeSettingsRO subConfig = settings.getNodeSettings(key);
            ColSetting local = new ColSetting();
            try {
                local.loadSettings(subConfig);
            } catch (InvalidSettingsException ise) {
                throw new InvalidSettingsException(
                        "Unable to load sub config for key '" + key + "'", ise);
            }
            result.put(key, local);
        }
        return result;
    }

    /**
     * Saves the individual settings to a config object.
     * 
     * @param colSettings the settings to write, may include meta settings
     *            (ignored)
     * @param settings to write to
     */
    protected static void saveIndividualsColSettings(
            final ColSetting[] colSettings, final NodeSettingsWO settings) {
        NodeSettingsWO individuals = settings.addNodeSettings(CFG_INDIVIDUAL);
        for (int i = 0; i < colSettings.length; i++) {
            if (colSettings[i].isMetaConfig()) {
                continue;
            }
            String id = colSettings[i].getName().toString();
            NodeSettingsWO subConfig = individuals.addNodeSettings(id);
            colSettings[i].saveSettings(subConfig);
        }
    }

    /**
     * Saves the meta settings to a config object.
     * 
     * @param colSettings the settings to write, may include individual settings
     *            (ignored)
     * @param settings to write to
     */
    protected static void saveMetaColSettings(final ColSetting[] colSettings,
            final NodeSettingsWO settings) {
        NodeSettingsWO defaults = settings.addNodeSettings(CFG_META);
        for (int i = 0; i < colSettings.length; i++) {
            if (!colSettings[i].isMetaConfig()) {
                continue;
            }
            int type = colSettings[i].getType();
            String id;
            switch (type) {
            case TYPE_STRING:
                id = CFG_META_STRING;
                break;
            case TYPE_INT:
                id = CFG_META_INT;
                break;
            case TYPE_DOUBLE:
                id = CFG_META_DOUBLE;
                break;
            case TYPE_UNKNOWN:
                id = CFG_META_OTHER;
                break;
            default:
                assert false;
                id = CFG_META_OTHER;
            }
            NodeSettingsWO subConfig = defaults.addNodeSettings(id);
            colSettings[i].saveSettings(subConfig);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(isMetaConfig() ? "META" : m_name.toString());
        buffer.append(":");
        switch (m_type) {
        case TYPE_STRING:
            buffer.append(CFG_META_STRING);
            break;
        case TYPE_INT:
            buffer.append(CFG_META_INT);
            break;
        case TYPE_DOUBLE:
            buffer.append(CFG_META_DOUBLE);
            break;
        case TYPE_UNKNOWN:
            buffer.append(CFG_META_OTHER);
            break;
        default:
            throw new RuntimeException();
        }
        buffer.append(":");
        switch (m_method) {
        case ColSetting.METHOD_NO_HANDLING:
            buffer.append("no handling");
            break;
        case ColSetting.METHOD_IGNORE_ROWS:
            buffer.append("ignore");
            break;
        case ColSetting.METHOD_MEAN:
            buffer.append("mean");
            break;
        case ColSetting.METHOD_MOST_FREQUENT:
            buffer.append("most frequent");
            break;
        case ColSetting.METHOD_MIN:
            buffer.append("min");
            break;
        case ColSetting.METHOD_MAX:
            buffer.append("max");
            break;
        case ColSetting.METHOD_FIX_VAL:
            buffer.append("fix:");
            buffer.append(m_fixCell);
            break;
        default:
            throw new RuntimeException();
        }
        buffer.append("]");
        return buffer.toString();
    }
}
