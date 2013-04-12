/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * Created on Apr 11, 2013 by wiswedel
 */
package org.knime.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Creator for {@link DataTableSpec}. This class can be used to set different fields (columns, name, properties).
 *
 * <p>Node implementations can use the constructors of {@link DataTableSpec} directly. This class is merely for clients,
 * which are interested in setting {@linkplain DataTableSpec#getProperties() properties} on a table spec.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.8
 */
public final class DataTableSpecCreator {

    private List<DataColumnSpec> m_columnSpecs = new ArrayList<DataColumnSpec>();
    private Map<String, Integer> m_columnSpecMap = new HashMap<String, Integer>();

    private String m_name;
    private Map<String, String> m_properties = new LinkedHashMap<String, String>();

    /** Adds argument columns to list of already added columns.
     * @param columns to add
     * @return this
     * @throws IllegalArgumentException If duplicate is encountered
     * @throws NullPointerException If argument or any element in it is null.
     */
    public DataTableSpecCreator addColumns(final DataColumnSpec... columns) {
        for (int i = 0; i < columns.length; i++) {
            String colName = columns[i].getName();
            Integer oldIndex = m_columnSpecMap.get(colName);
            if (m_columnSpecMap.containsKey(colName)) {
                throw new IllegalArgumentException("Column \"" + colName + "\" already contained at index " + oldIndex);
            }
            m_columnSpecs.add(columns[i]);
            m_columnSpecMap.put(colName, m_columnSpecs.size() - 1);
        }
        return this;
    }

    /** Analogous to {@link #addColumns(DataColumnSpec...)} using the columns from the argument spec.
     * @param fromSpec ...
     * @return this
     */
    public DataTableSpecCreator addColumns(final DataTableSpec fromSpec) {
        addColumns(fromSpec.getColumnSpecs());
        return this;
    }

    /** Sets name on output table spec.
     * @param name New name (may be null - then using a default).
     * @return this
     * @see DataTableSpec#getName()
     */
    public DataTableSpecCreator setName(final String name) {
        m_name = name;
        return this;
    }

    /** Adds all properties from the argument to the prop list.
     * @param properties to add
     * @return this
     * @throws IllegalArgumentException If any key is null or an empty string.
     * @throws NullPointerException If arg is null.
     */
    public DataTableSpecCreator putProperties(final Map<String, String> properties) {
        if (properties.containsKey(null) || properties.containsKey("")) {
            throw new IllegalArgumentException("Invalid property key (null or empty string)");
        }
        m_properties.putAll(properties);
        return this;
    }

    /** Adds one key/value pair.
     * @param key ...
     * @param value ...
     * @return this
     * @throws IllegalArgumentException if key is null or an empty string.
     * @return
     */
    public DataTableSpecCreator putProperty(final String key, final String value) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("Invalid property key (null or empty string)");
        }
        m_properties.put(key, value);
        return this;
    }

    /** Takes all settings and creates the table spec.
     * @return A new table spec.
     */
    public DataTableSpec createSpec() {
        return new DataTableSpec(m_name, m_columnSpecs.toArray(new DataColumnSpec[m_columnSpecs.size()]), m_properties);
    }

}
