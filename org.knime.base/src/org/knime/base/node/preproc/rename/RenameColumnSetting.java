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
 *   Feb 1, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.rename;

import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Helper class that combines settings as to what should be happen with one
 * column. That is one object of this class is responsible for only one column!
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class RenameColumnSetting {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RenameColumnSetting.class);

    /** Config identifier for the original column name, used for sanity check. */
    public static final String CFG_OLD_COLNAME = "old_column_name";

    /** Config identifier for the new column name (if any). */
    public static final String CFG_NEW_COLNAME = "new_column_name";

    /**
     * Config identifier for the index of the new type. The index is calculated
     * on the order of the compatible types in constructPossibleTypes(DataType)
     */
    public static final String CFG_NEW_COLTYPE = "new_column_type";

    /** Original column name, never null. */
    private final String m_name;

    /**
     * Array with all types that the column can be casted to. May be null if
     * first constructor is used
     */
    private final Class<? extends DataValue>[] m_possibleValueClasses;

    /** New name of the column, may be null to retain the old name. */
    private String m_newColumnName;

    /**
     * Index of the new type, according to the order in m_possibleValueClasses.
     */
    private int m_newValueClassIndex;

    /**
     * Inits a settings object for a given column. This constructor is used in
     * the NodeModel' load and validate method (no DataTableSpec availabe).
     * 
     * @param name the column to get settings (e.g. compatible types) from
     * @throws NullPointerException if argument is <code>null</code>
     */
    RenameColumnSetting(final String name) {
        m_name = name;
        m_possibleValueClasses = null;
    }

    /**
     * Constructor being used in the NodeModel's configure method (for
     * validation) and in the NodeDialog (DataTableSpec used to init default
     * values like possible types).
     * 
     * @param column the column spec form which to get values
     */
    @SuppressWarnings("unchecked")
    // no generics in array definition
    RenameColumnSetting(final DataColumnSpec column) {
        m_name = column.getName();
        DataType oldType = column.getType();
        Set<Class<? extends DataValue>> possibleTypeSet = constructPossibleTypes(oldType);
        m_possibleValueClasses = possibleTypeSet.toArray(new Class[0]);
    }

    /**
     * The name of the new column, if any. May be <code>null</code> when no
     * new name was set.
     * 
     * @return the newColumnName
     */
    String getNewColumnName() {
        return m_newColumnName;
    }

    /**
     * Sets a new column name or <code>null</code>.
     * 
     * @param newColumnName the newColumnName to set
     */
    void setNewColumnName(final String newColumnName) {
        m_newColumnName = newColumnName;
    }

    /**
     * The index of the type to cast the column to.
     * 
     * @return the newType
     */
    int getNewValueClassIndex() {
        return m_newValueClassIndex;
    }

    /**
     * Set new type.
     * 
     * @param newType the newType to set
     */
    void setNewValueClassIndex(final int newType) {
        m_newValueClassIndex = newType;
    }

    /**
     * @return the old name
     */
    String getName() {
        return m_name;
    }

    /**
     * Result may be <code>null</code> when the first constructor was used
     * (based on a column name only).
     * 
     * @return the possibleTypes
     */
    Class<? extends DataValue>[] getPossibleValueClasses() {
        return m_possibleValueClasses;
    }

    /**
     * Loads settings from a settings object.
     * 
     * @param settings to load from
     */
    void loadSettingsFrom(final NodeSettingsRO settings) {
        String name;
        try {
            name = settings.getString(CFG_OLD_COLNAME);
        } catch (InvalidSettingsException ise) {
            // this method is called from the dialog which inits "this" first
            // and immediately calls this method, name should (must) match
            LOGGER.warn("Can't safely update settings for column \"" + m_name
                    + "\": No matching identifier.", ise);
            name = m_name;
        }
        if (!m_name.equals(name)) {
            LOGGER.warn("Can't update settings for column \"" + m_name
                    + "\": got NodeSettings for \"" + name + "\"");
        }
        String newName = settings.getString(CFG_NEW_COLNAME, null);
        setNewColumnName(newName);
        int newVal = settings.getInt(CFG_NEW_COLTYPE, 0);
        boolean off = newVal < 0;
        off |= m_possibleValueClasses != null
                && newVal >= m_possibleValueClasses.length;
        if (off) {
            LOGGER.debug("New type identifier for column \"" + m_name
                    + "\" out of range: " + newVal);
            newVal = 0;
        }
        setNewValueClassIndex(newVal);
    }

    /**
     * Save the current settings to a config.
     * 
     * @param settings to save to
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_OLD_COLNAME, m_name);
        settings.addString(CFG_NEW_COLNAME, m_newColumnName);
        settings.addInt(CFG_NEW_COLTYPE, m_newValueClassIndex);
    }

    /**
     * Called by configure in NodeModel to compute the new column spec.
     * 
     * @param inSpec the original input spec (names must match)
     * @return the new column spec
     * @throws InvalidSettingsException if that fails
     */
    DataColumnSpec configure(final DataColumnSpec inSpec)
            throws InvalidSettingsException {
        String name = inSpec.getName();
        DataType oldType = inSpec.getType();
        if (!name.equals(m_name)) {
            throw new InvalidSettingsException("Column names don't match: \""
                    + m_name + "\" vs. \"" + name + "\"");
        }
        Set<Class<? extends DataValue>> possibleTypeSet = constructPossibleTypes(inSpec
                .getType());
        // no generics in array definition
        @SuppressWarnings("unchecked")
        Class<? extends DataValue>[] possibleTypes = possibleTypeSet
                .toArray(new Class[possibleTypeSet.size()]);
        if (getNewValueClassIndex() >= possibleTypes.length) {
            throw new InvalidSettingsException("Invalid type index: "
                    + getNewValueClassIndex());
        }
        String newName = m_newColumnName == null ? m_name : m_newColumnName;
        Class<? extends DataValue> newVal = 
            possibleTypes[getNewValueClassIndex()];
        boolean useToString = newVal.equals(StringValue.class)
                && !oldType.isCompatible(StringValue.class);
        DataColumnDomain newDomain;
        DataType newType;
        if (useToString) {
            newDomain = null;
            newType = StringCell.TYPE;
        } else {
            newDomain = inSpec.getDomain();
            Class<? extends DataValue> oldP = oldType.getPreferredValueClass();
            if (oldP != null && oldP.equals(newVal)) {
                newType = oldType;
            } else {
                newType = DataType.cloneChangePreferredValue(oldType, newVal);
            }
        }
        DataColumnSpecCreator creator = new DataColumnSpecCreator(inSpec);
        creator.setName(newName);
        creator.setType(newType);
        creator.setDomain(newDomain);
        return creator.createSpec();
    }

    /**
     * Factory method used in NodeModel#validate and #loadSettingsFrom.
     * 
     * @param settings to load from
     * @return a new object of this class with the settings
     * @throws InvalidSettingsException if that fails
     */
    static RenameColumnSetting createFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String name = settings.getString(CFG_OLD_COLNAME);
        String newName = settings.getString(CFG_NEW_COLNAME);
        int newType = settings.getInt(CFG_NEW_COLTYPE, 0);
        if (newType < 0) {
            throw new InvalidSettingsException(
                    "New type identifier for column \"" + name
                            + "\" out of range: " + newType);
        }
        RenameColumnSetting result = new RenameColumnSetting(name);
        result.setNewColumnName(newName);
        result.setNewValueClassIndex(newType);
        return result;
    }

    /**
     * Construct a set with all types a given type can be cast to. It also
     * contains always <code>StringValue.class</code>.
     * 
     * @param type the type for which to determine all possible types
     * @return a set containing all compatible types, including
     *         <code>type</code>
     */
    static Set<Class<? extends DataValue>> constructPossibleTypes(
            final DataType type) {
        LinkedHashSet<Class<? extends DataValue>> possibleValues = new LinkedHashSet<Class<? extends DataValue>>(
                type.getValueClasses());
        // disallow DataValue.class
        if (possibleValues.size() > 1) {
            possibleValues.remove(DataValue.class);
        }
        // we can always cast to a string
        if (!possibleValues.contains(StringValue.class)) {
            possibleValues.add(StringValue.class);
        }
        return possibleValues;
    }
}
