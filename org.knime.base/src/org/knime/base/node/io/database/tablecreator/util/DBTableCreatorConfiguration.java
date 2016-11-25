/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 15, 2016 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.tablecreator.DBColumn;
import org.knime.core.node.port.database.tablecreator.DBKey;

/**
 * A configuration class to store the settings of DBTable
 *
 * @author Budi Yanto, KNIME.com
 */
public class DBTableCreatorConfiguration {

    /** Default schema **/
    public static String DEF_SCHEMA = "";

    /** Default table name **/
    public static String DEF_TABLE_NAME = "newtable";

    /** Default temporary table value **/
    public static boolean DEF_IS_TEMP_TABLE = false;

    /** Default if not exists value **/
    public static boolean DEF_IF_NOT_EXISTS = false;

    /** Default use dynamic settings value **/
    public static boolean DEF_USE_DYNAMIC_SETTINGS = false;

    /** Configuration key for the schema **/
    public static final String CFG_SCHEMA = "Schema";

    /** Configuration key for the table name **/
    public static final String CFG_TABLE_NAME = "TableName";

    /** Configuration key for the temporary table **/
    public static final String CFG_TEMP_TABLE = "TempTable";

    /** Configuration key for if not exists **/
    public static final String CFG_IF_NOT_EXISTS = "IfNotExists";

    /** Configuration key for use dynamic settings **/
    public static final String CFG_USE_DYNAMIC_SETTINGS = "UseDynamicSettings";

    /** Configuration key for additional SQL statement **/
    public static final String CFG_ADDITIONAL_OPTIONS = "AdditionalOptions";

    /** Configuration key for the column definitions settings **/
    public static final String CFG_COLUMNS_SETTINGS = "Columns";

    /** Configuration key for the key definitions settings **/
    public static final String CFG_KEYS_SETTINGS = "Keys";

    /** Configuration key for the column name based type mapping **/
    public static final String CFG_NAME_BASED_TYPE_MAPPING = "NameBasedTypeMapping";

    /** Configuration key for the knime type based mapping **/
    public static final String CFG_KNIME_BASED_TYPE_MAPPING = "KnimeBasedTypeMapping";

    /** Configuration key for the column name based keys mapping **/
    public static final String CFG_NAME_BASED_KEYS = "NameBasedKeys";

    /** SettingsModel for the schema */
    private final SettingsModelString m_schemaSettingsModel = new SettingsModelString(CFG_SCHEMA, DEF_SCHEMA);

    /** SettingsModel for the table name */
    private final SettingsModelString m_tableNameSettingsModel = new SettingsModelString(CFG_TABLE_NAME, "");

    /** SettingsModel for the temp table */
    private final SettingsModelBoolean m_tempTableSettingsModel = new SettingsModelBoolean(CFG_TEMP_TABLE, DEF_IS_TEMP_TABLE);

    /** SettingsModel for the ifNotExisting */
    private final SettingsModelBoolean m_ifNotExistsSettingsModel = new SettingsModelBoolean(CFG_IF_NOT_EXISTS, DEF_IF_NOT_EXISTS);

    /** SettingsModel for the useDynamicSettings */
    private final SettingsModelBoolean m_useDynamicSettingsModel = new SettingsModelBoolean(CFG_USE_DYNAMIC_SETTINGS, DEF_USE_DYNAMIC_SETTINGS);

    /** SettingsModel for the additional SQL statement */
    private final SettingsModelString m_additionalOptionsModel = new SettingsModelString(CFG_ADDITIONAL_OPTIONS, "");

    private final Map<String, List<RowElement>> m_tableMap = new HashMap<>();

    private final Map<String, SQLTypeCellEditor> m_sqlCellEditorMap = new HashMap<>();

    private DataTableSpec m_tableSpec;

    /**
     * Creates a new instance of DBTableCreatorConfiguration
     */
    public DBTableCreatorConfiguration() {
        m_tableMap.put(CFG_COLUMNS_SETTINGS, new ArrayList<RowElement>());
        m_tableMap.put(CFG_KEYS_SETTINGS, new ArrayList<RowElement>());
        m_tableMap.put(CFG_NAME_BASED_TYPE_MAPPING, new ArrayList<RowElement>());
        m_tableMap.put(CFG_KNIME_BASED_TYPE_MAPPING, new ArrayList<RowElement>());
        m_tableMap.put(CFG_NAME_BASED_KEYS, new ArrayList<RowElement>());

        m_schemaSettingsModel.setEnabled(!m_tempTableSettingsModel.getBooleanValue());
    }

    /**
     * @return <code>SettingsModelString</code> for schema
     */
    public SettingsModelString getSchemaSettingsModel() {
        return m_schemaSettingsModel;
    }

    /**
     * @return <code>SettingsModelString</code> for table name
     */
    public SettingsModelString getTableNameSettingsModel() {
        return m_tableNameSettingsModel;
    }

    /**
     * @return <code>SettingsModelBoolean</code> for temporary table
     */
    public SettingsModelBoolean getTempTableSettingsModel() {
        return m_tempTableSettingsModel;
    }

    /**
     * @return <code>SettingsModelBoolean</code> for ifNotExists
     */
    public SettingsModelBoolean getIfNotExistsSettingsModel() {
        return m_ifNotExistsSettingsModel;
    }

    /**
     * @return <code>SettingsModelBoolean</code> for useDynamicSettings
     */
    public SettingsModelBoolean getUseDynamicSettingsModel() {
        return m_useDynamicSettingsModel;
    }

    /**
     * Returns the schema
     *
     * @return the schema
     */
    public String getSchema() {
        return m_schemaSettingsModel.getStringValue();
    }

    /**
     * Sets the schema
     *
     * @param schema the schema to set
     */
    public void setSchema(final String schema) {
        m_schemaSettingsModel.setStringValue(schema);
    }

    /**
     * Returns the table name
     *
     * @return the table name
     */
    public String getTableName() {
        return m_tableNameSettingsModel.getStringValue();
    }

    /**
     * Sets the table name
     *
     * @param tableName the table name to set
     */
    public void setTableName(final String tableName) {
        m_tableNameSettingsModel.setStringValue(tableName);
    }

    /**
     * Returns true if the table is a temporary table, otherwise returns false
     *
     * @return true if the table is a temporary table, otherwise false
     */
    public boolean isTempTable() {
        return m_tempTableSettingsModel.getBooleanValue();
    }

    /**
     * Sets to true if the table is a temporary table, otherwise set to false
     *
     * @param isTempTable true if the table is a temporary table, otherwise false
     */
    public void setTempTable(final boolean isTempTable) {
        m_tempTableSettingsModel.setBooleanValue(isTempTable);
    }

    /**
     * @return true if the "ifNotExists" flag should be used, otherwise false
     */
    public boolean ifNotExists() {
        return m_ifNotExistsSettingsModel.getBooleanValue();
    }

    /**
     * @return true if the the dynamic settings should be used, otherwise false
     */
    public boolean useDynamicSettings() {
        return m_useDynamicSettingsModel.getBooleanValue();
    }

    /**
     * @return the additional options
     */
    public String getAdditionalOptions() {
        return m_additionalOptionsModel.getStringValue();
    }

    /**
     * Sets the additional options
     * @param options the additional options to set
     */
    public void setAdditionalOptions(final String options) {
        m_additionalOptionsModel.setStringValue(options);
    }

    /**
     * Returns the DataTableSpec instance
     *
     * @return the DataTableSpec instance. Can be <code>null</code>
     */
    public DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    /**
     * Returns the columns
     *
     * @return the columns
     */
    public List<DBColumn> getColumns() {
        List<DBColumn> columns = new ArrayList<>();
        for (RowElement el : getRowElements(CFG_COLUMNS_SETTINGS)) {
            ColumnElement col = (ColumnElement)el;
            columns.add(col.getDBColumn());
        }
        return columns;
    }

    /**
     * Returns the keys
     *
     * @return the keys
     */
    public List<DBKey> getKeys() {
        List<DBKey> keys = new ArrayList<>();
        for (RowElement el : getRowElements(CFG_KEYS_SETTINGS)) {
            KeyElement elem = (KeyElement)el;
            keys.add(elem.getDBKey());
        }
        return keys;
    }

    /**
     * Returns the list of RowElements retrieved from the map using the specified key
     *
     * @param cfgKey key used to retrieve the RowElements
     * @return the list of RowElements retrieved from the map
     */
    public List<RowElement> getRowElements(final String cfgKey) {
        List<RowElement> elems = m_tableMap.get(cfgKey);
        if (elems == null) {
            elems = new ArrayList<>();
            m_tableMap.put(cfgKey, elems);
        }
        return elems;
    }

    /**
     * Returns the SQLTypeCellEditor retrieved from the map using the specified key
     *
     * @param editorKey key used to retrieve the SQLTypeCellEditor
     * @param relatedColumn related column for the SQLTypeCellEditor
     * @return the SQLTypeCellEditor retrieved from the map
     */
    public SQLTypeCellEditor getSqlTypeCellEditor(final String editorKey, final int relatedColumn) {
        SQLTypeCellEditor editor = m_sqlCellEditorMap.get(editorKey);
        if (editor == null) {
            editor = new SQLTypeCellEditor(relatedColumn);
            m_sqlCellEditorMap.put(editorKey, editor);
        }
        return editor;
    }

    /**
     * Sets the {@link DataTableSpec}
     * @param spec
     */
    public void setTableSpec(final DataTableSpec spec) {
        m_tableSpec = spec;
    }

    /**
     * @param spec the input {@link DataTableSpec}
     * @return <code>true</code> if the given {@link DataTableSpec} is a new one, otherwise <code>false</code>
     */
    public boolean isNewTableSpec(final DataTableSpec spec) {
        if((spec == null) || (m_tableSpec != null && m_tableSpec.equalStructure(spec))) {
            return false;
        }

        return true;
    }

    /**
     * Load settings for NodeModel
     *
     * @param settings NodeSettingsRO instance to load from
     * @throws InvalidSettingsException
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }

        loadSettingsForSettingsModel(settings);
        loadSettingsForRowElements(CFG_NAME_BASED_TYPE_MAPPING, settings);
        loadSettingsForRowElements(CFG_KNIME_BASED_TYPE_MAPPING, settings);
        loadSettingsForRowElements(CFG_NAME_BASED_KEYS, settings);
        loadSettingsForRowElements(CFG_COLUMNS_SETTINGS, settings);
        loadSettingsForRowElements(CFG_KEYS_SETTINGS, settings);
    }

    /**
     * Load settings for NodeDialog
     *
     * @param settings NodeSettingsRO instance to load from
     * @param specs PortObjectSpec array to load from
     * @throws NotConfigurableException
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);

            final String dbIdentifier = ((DatabaseConnectionPortObjectSpec)specs[0]).getDatabaseIdentifier();
            loadSettingsForSqlEditor(dbIdentifier);

            m_tableSpec = (DataTableSpec) specs[1];

            if(m_tableSpec != null && (useDynamicSettings() || getColumns().isEmpty())) {
                loadColumnSettingsFromTableSpec(m_tableSpec);
                if(useDynamicSettings()) {
                    updateKeysWithDynamicSettings();
                }
            } else {
                loadSettingsForRowElements(CFG_COLUMNS_SETTINGS, settings);
                loadSettingsForRowElements(CFG_KEYS_SETTINGS, settings);
            }
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
    }

    /**
     * A helper method to load settings for all SettingsModel instances
     *
     * @param settings NodeSettingsRO instance to load from
     * @throws InvalidSettingsException
     */
    private void loadSettingsForSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_tableNameSettingsModel.loadSettingsFrom(settings);
        m_tempTableSettingsModel.loadSettingsFrom(settings);
        m_schemaSettingsModel.setEnabled(!m_tempTableSettingsModel.getBooleanValue());
        m_schemaSettingsModel.loadSettingsFrom(settings);
        m_ifNotExistsSettingsModel.loadSettingsFrom(settings);
        m_useDynamicSettingsModel.loadSettingsFrom(settings);
        m_additionalOptionsModel.loadSettingsFrom(settings);
        if (StringUtils.isBlank(getTableName())) {
            setTableName(DEF_TABLE_NAME);
        }
    }

    /**
     * A helper method to load settings for all RowElements
     *
     * @param cfgKey key used to retrieve the RowElement list from the map
     * @param settings NodeSettingsRO instance to load from
     * @throws InvalidSettingsException
     */
    private void loadSettingsForRowElements(final String cfgKey, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final NodeSettingsRO root = settings.getNodeSettings(cfgKey);
        List<RowElement> elements = m_tableMap.get(cfgKey);
        elements.clear();
        for (String settingsKey : root.keySet()) {
            final NodeSettingsRO cfg = root.getNodeSettings(settingsKey);
            final RowElement elem = createRowElement(cfgKey, cfg);
            if (elem != null) {
                elements.add(elem);
            }
        }
    }

    /**
     * A helper method to create a new instance of RowElement from NodeSettingsRO instance
     *
     * @param cfgKey key to determine which kind of RowElement to create
     * @param settings NodeSettingsRO instance used to create a new RowElement
     * @return a new instance of RowElement
     */
    private RowElement createRowElement(final String cfgKey, final NodeSettingsRO settings) {
        switch (cfgKey) {
            case CFG_COLUMNS_SETTINGS:
                return new ColumnElement(settings);
            case CFG_KEYS_SETTINGS:
                KeyElement elem = new KeyElement(settings);
                Set<ColumnElement> columns = new HashSet<>();
                for (DBColumn dbCol : elem.getColumns()) {
                    boolean isFound = false;
                    for (RowElement el : getRowElements(CFG_COLUMNS_SETTINGS)) {
                        ColumnElement colElem = (ColumnElement)el;
                        if (dbCol.getName().equalsIgnoreCase(colElem.getName())) {
                            columns.add(colElem);
                            isFound = true;
                            break;
                        }
                    }
                    if (!isFound) {
                        throw new IllegalArgumentException(String.format("Column '%s' is undefined", dbCol.getName()));
                    }
                }
                elem.setColumnElements(columns);
                return elem;
            case CFG_NAME_BASED_TYPE_MAPPING:
                return new NameBasedMappingElement(settings);
            case CFG_KNIME_BASED_TYPE_MAPPING:
                return new KNIMEBasedMappingElement(settings);
            case CFG_NAME_BASED_KEYS:
                return new NameBasedKeysElement(settings);
            default:
                return null;
        }
    }

    /**
     * Validate settings
     * @param settings settings to validate
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException{
        m_schemaSettingsModel.validateSettings(settings);
        m_tableNameSettingsModel.validateSettings(settings);
        m_tempTableSettingsModel.validateSettings(settings);
        m_ifNotExistsSettingsModel.validateSettings(settings);
        m_useDynamicSettingsModel.validateSettings(settings);
        for(String key : m_tableMap.keySet()){
            settings.getNodeSettings(key);
        }
    }

    /**
     * Saves settings for NodeModel
     *
     * @param settings NodeSettingsWO instance to save settings to
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        m_schemaSettingsModel.saveSettingsTo(settings);
        m_tableNameSettingsModel.saveSettingsTo(settings);
        m_tempTableSettingsModel.saveSettingsTo(settings);
        m_ifNotExistsSettingsModel.saveSettingsTo(settings);
        m_useDynamicSettingsModel.saveSettingsTo(settings);
        m_additionalOptionsModel.saveSettingsTo(settings);
        for (Entry<String, List<RowElement>> entry : m_tableMap.entrySet()) {
            final NodeSettingsWO root = settings.addNodeSettings(entry.getKey());
            int idx = 0;
            for (RowElement elem : entry.getValue()) {
                final NodeSettingsWO cfg = root.addNodeSettings(elem.getPrefix() + idx++);
                elem.saveSettingsTo(cfg);
            }
        }
    }

    /**
     * Saves settings for NodeDialog
     *
     * @param settings NodeSettingsWO instance to save settings to
     * @throws InvalidSettingsException
     */
    public void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
        saveSettingsForSqlEditor();
    }

    /**
     * A helper method to save settings of SQLTypeCellEditor to StringHistory
     */
    private void saveSettingsForSqlEditor() {
        for (Entry<String, SQLTypeCellEditor> entry : m_sqlCellEditorMap.entrySet()) {
            entry.getValue().saveSettings();
        }
    }

    /**
     * A helper method to load settings of SQLTypeCellEditor from StringHistory
     *
     * @param identifier identifier used to get the corresponding StringHistory
     */
    private void loadSettingsForSqlEditor(final String identifier) {
        for (Entry<String, SQLTypeCellEditor> entry : m_sqlCellEditorMap.entrySet()) {
            entry.getValue().loadSettings(entry.getKey() + "_" + identifier);
        }
    }

    /**
     * Load column settings from table spec
     *
     * @param spec DataTableSpec to load from
     */
    public void loadColumnSettingsFromTableSpec(final DataTableSpec spec) {
        if(spec == null) {
            throw new IllegalArgumentException("spec cannot be null");
        }

        final List<RowElement> colElems = getRowElements(CFG_COLUMNS_SETTINGS);
        colElems.clear();

        final List<RowElement> nameBasedMappingElems = getRowElements(CFG_NAME_BASED_TYPE_MAPPING);
        final List<RowElement> knimeBasedMappingElems = getRowElements(CFG_KNIME_BASED_TYPE_MAPPING);
        for (int colIdx = 0; colIdx < spec.getNumColumns(); colIdx++) {
            final DataColumnSpec colSpec = spec.getColumnSpec(colIdx);
            final String name = colSpec.getName();
            boolean isMatched = false;
            String type = null;
            boolean notNull = false;

            if(useDynamicSettings()) {
                for (final RowElement el : nameBasedMappingElems) {
                    final NameBasedMappingElement elem = (NameBasedMappingElement)el;
                    final Pattern pattern =
                        PatternUtil.compile(elem.getNamePattern(), elem.isRegex(), Pattern.CASE_INSENSITIVE);
                    final Matcher matcher = pattern.matcher(name);
                    if (matcher.matches()) {
                        type = elem.getSqlType();
                        notNull = elem.isNotNull();
                        isMatched = true;
                        break;
                    }
                }

                if (!isMatched) { // No match on name-based mapping, try to look at Knime-based mapping
                    for (final RowElement el : knimeBasedMappingElems) {
                        final KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)el;
                        final DataType colType = colSpec.getType();
                        final DataType elementType = elem.getKnimeType();
                        if (elementType.equals(colType) || elementType.isASuperTypeOf(colType)) {
                            type = elem.getSqlType();
                            notNull = elem.isNotNull();
                            isMatched = true;
                            break;
                        }
                    }
                }
            }

            if (!isMatched) { // No match on name-based and knime-based mapping, use default value
                type = DBUtil.getDefaultSQLType(colSpec.getType());
            }

            final ColumnElement elem = new ColumnElement(name, type, notNull);
            colElems.add(elem);
        }

    }

    /**
     * A helper method to update the keys with dynamic settings if available
     */
    public void updateKeysWithDynamicSettings() {
        final List<RowElement> keyElems = getRowElements(CFG_KEYS_SETTINGS);
        keyElems.clear();
        final List<RowElement> nameBasedKeyElems = getRowElements(CFG_NAME_BASED_KEYS);
        if (!nameBasedKeyElems.isEmpty()) {
            final List<RowElement> colElems = getRowElements(CFG_COLUMNS_SETTINGS);
            for (final RowElement el : nameBasedKeyElems) {
                final NameBasedKeysElement nameBasedKeyElem = (NameBasedKeysElement)el;
                // Looking for columns that match the search pattern
                final Set<ColumnElement> columns = new HashSet<>();
                final String searchPattern = nameBasedKeyElem.getNamePattern();
                final Pattern pattern =
                    PatternUtil.compile(searchPattern, nameBasedKeyElem.isRegex(), Pattern.CASE_INSENSITIVE);
                for (final RowElement re : colElems) {
                    final ColumnElement colElem = (ColumnElement)re;
                    final Matcher m = pattern.matcher(colElem.getName());
                    if (m.matches()) {
                        columns.add(colElem);
                    }
                }

                // Add a new key if there is at least one column that matches the search pattern
                if (!columns.isEmpty()) {
                    final String keyName = nameBasedKeyElem.getKeyName();
                    final boolean primaryKey = nameBasedKeyElem.isPrimaryKey();
                    final KeyElement newElem = new KeyElement(keyName, columns, primaryKey);
                    keyElems.add(newElem);
                }
            }
        }
    }

    /**
     * Return the prefix of RowElement
     *
     * @param cfgKey key to identify the kind of RowElement
     * @return the prefix of RowElement
     */
    static String getPrefix(final String cfgKey) {
        switch (cfgKey) {
            case CFG_COLUMNS_SETTINGS:
                return ColumnElement.DEFAULT_PREFIX;
            case CFG_KEYS_SETTINGS:
                return KeyElement.DEFAULT_PREFIX;
            case CFG_NAME_BASED_TYPE_MAPPING:
                return NameBasedMappingElement.DEFAULT_PREFIX;
            case CFG_KNIME_BASED_TYPE_MAPPING:
                return KNIMEBasedMappingElement.DEFAULT_PREFIX;
            case CFG_NAME_BASED_KEYS:
                return NameBasedKeysElement.DEFAULT_PREFIX;
            default:
                return null;
        }
    }

}
