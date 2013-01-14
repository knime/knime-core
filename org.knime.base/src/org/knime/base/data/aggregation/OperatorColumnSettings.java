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
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation;

import org.knime.core.data.DataColumnSpec;

import java.util.HashMap;
import java.util.Map;


/**
 * Contains the operator specific settings for a specific column such as
 * if missing values should be considered during aggregation.
 * The informations might be provided by the user in the node dialog.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class OperatorColumnSettings {

    /**Default include missing values {@link OperatorColumnSettings} object
     * used in operator templates.*/
    public static final OperatorColumnSettings DEFAULT_INCL_MISSING =
        new OperatorColumnSettings(true, null);

    /**Default exclude missing values {@link OperatorColumnSettings} object
     * used in operator templates.*/
    public static final OperatorColumnSettings DEFAULT_EXCL_MISSING =
        new OperatorColumnSettings(false, null);

    /**If missing values should be considered during calculation.*/
    private boolean m_inclMissingCells;

    /**The original {@link DataColumnSpec} of the column to aggregate.*/
    private final DataColumnSpec m_origColSpec;

    /**This key value map allows the storing of arbitrary objects associated
     * with a unique key which are accessible in the
     * <code>AggregationMethod</code> implementations.
     */
    private final Map<String, Object> m_keyValueMap =
        new HashMap<String, Object>();

    /**Constructor for class OperatorSeetings.
     *
     * @param origColSpec the {@link DataColumnSpec} from the column to
     * aggregate
     * @param inclMissingCells <code>true</code> if missing values should
     * be considered during aggregation <code>false</code> if missing values
     * should be omitted
     */
    public OperatorColumnSettings(final boolean inclMissingCells,
            final DataColumnSpec origColSpec) {
        m_origColSpec = origColSpec;
        m_inclMissingCells = inclMissingCells;
    }

    /**
     * @param inclMissingCells <code>true</code> if missing cells should be
     * considered during aggregation
     */
    public void setInclMissing(final boolean inclMissingCells) {
        m_inclMissingCells = inclMissingCells;
    }

    /**
     * @return <code>true</code> if missing cells are considered during
     * aggregation
     */
    public boolean inclMissingCells() {
        return m_inclMissingCells;
    }

    /**
     * @return the original {@link DataColumnSpec} of the column to aggregate
     * <b>Notice:</b>The original column spec is <code>null</code> when
     * registering the operator during plugin initialization phase.
     * When the operator is used the method always returns the original column
     * specification.
     */
    public DataColumnSpec getOriginalColSpec() {
        return m_origColSpec;
    }

    /**
     * Allows the adding of arbitrary objects with a given <tt>key</tt>.
     * @param key the <tt>key</tt> to use. Must not be <code>null</code>.
     * @param value the value to store. Must not be <code>null</code>.
     * @return the previous value associated with <tt>key</tt>, or
     *         <code>null</code> if there was no mapping for <tt>key</tt>.
     * @since 2.6
     */
    public Object addValue(final String key, final Object value) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        if (value == null) {
            throw new NullPointerException("value must not be null");
        }
        return m_keyValueMap.put(key, value);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     * @since 2.6
     */
    public Object getValue(final String key) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        return m_keyValueMap.get(key);
    }
}
