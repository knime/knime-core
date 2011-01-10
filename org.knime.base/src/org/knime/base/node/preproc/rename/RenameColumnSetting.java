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
        boolean useToString = 
            newVal.equals(StringValue.class) 
                // columns with only missing values (and corresponding col-spec)
                // need to handled separately, bug #1939
                && (DataType.getMissingCell().getType().equals(oldType)
                    || !oldType.isCompatible(StringValue.class));
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
