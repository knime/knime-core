/*
 *
 *
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
 *
 * History
 *   12.07.2006 (gabriel): created
 */
package org.knime.core.node.config;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.node.config.base.ConfigBaseWO;

/**
 * Write-only interface for <code>Config</code> objects providing only access
 * functions.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public interface ConfigWO extends ConfigBaseWO {

    /**
     * Creates and adds a new <code>Config</code> to this <code>Config</code>
     * by the given <i>key</i>.
     * @param key The key for the config to add.
     * @return A new <code>Config</code> object.
     */
    Config addConfig(String key);

    /**
     * Adds this DataCell object to the Config by the given key. The cell can be
     * null.
     *
     * @param key The key.
     * @param cell The DataCell to add.
     */
    public void addDataCell(final String key, final DataCell cell);

    /**
     * Adds this <code>RowKey</code> object to this Config by the given key.
     * The row key can be null.
     *
     * @param key identifier used to store and the load the <code>RowKey</code>
     * @param rowKey value to store
     */
    public void addRowKey(final String key, final RowKey rowKey);

    /**
     * Adds this <code>RowKey</code> array to this Config by the given key.
     * The row key array can be null.
     *
     * @param key identifier used to store and the load the <code>RowKey</code>
     *        array
     * @param rowKey array to store
     */
    public void addRowKeyArray(final String key, final RowKey... rowKey);

    /**
     * Adds this DataType object value to the Config by the given key. The type
     * can be null.
     *
     * @param key The key.
     * @param type The DataType object to add.
     */
    public void addDataType(final String key, final DataType type);

    /**
     * Adds an array of DataCell objects to this Config. The array and all
     * elements can be null.
     *
     * @param key The key.
     * @param values The data cells, elements can be null.
     */
    public void addDataCellArray(final String key, final DataCell... values);

    /**
     * Adds an array of DataType objects to this Config. The array and all
     * elements can be null.
     *
     * @param key The key.
     * @param values The data types, elements can be null.
     */
    public void addDataTypeArray(final String key, final DataType... values);

}
