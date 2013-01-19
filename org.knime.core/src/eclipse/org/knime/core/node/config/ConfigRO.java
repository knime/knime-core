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
 *
 */
package org.knime.core.node.config;

import java.util.Iterator;

import javax.swing.tree.TreeNode;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;


/**
 * Interface implements only access functions for <code>Config</code> objects.
 * In addition, it implements the <code>TreeNode</code> and
 * <code>Iterable</code> interface.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public interface ConfigRO extends ConfigBaseRO, TreeNode, Iterable<String> {

    /**
     * Returns a <code>Config</code> for the given key.
     * @param key The identifier for the <code>Config</code>.
     * @return A new <code>Config</code> object.
     * @throws InvalidSettingsException If the Config could not be accessed.
     */
    public Config getConfig(String key) throws InvalidSettingsException;

    /**
     * Return DataCell for key.
     *
     * @param key The key.
     * @return A DataCell.
     * @throws InvalidSettingsException If the key is not available.
     */
    public DataCell getDataCell(final String key)
            throws InvalidSettingsException;

    /**
     * Return <code>RowKey</code> for key.
     *
     * @param key the identifier used to store the <code>RowKey</code> before
     * @return the store <code>RowKey</code>
     * @throws InvalidSettingsException if the key is not available
     */
    public RowKey getRowKey(final String key)
            throws InvalidSettingsException;

    /**
     * Return <code>RowKey</code> array for the given key.
     *
     * @param key the identifier used to store the <code>RowKey</code> array
     *        before
     * @return the store <code>RowKey</code> array
     * @throws InvalidSettingsException if the key is not available
     */
    public RowKey[] getRowKeyArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return DataType for key.
     *
     * @param key The key.
     * @return A DataType.
     * @throws InvalidSettingsException If the key is not available.
     */
    public DataType getDataType(final String key)
            throws InvalidSettingsException;

    /**
     * Return a DataCell which can be null, or the default value if the key is
     * not available.
     *
     * @param key The key.
     * @param def The default value, returned id the key is not available.
     * @return A DataCell object.
     */
    public DataCell getDataCell(final String key, final DataCell def);

    /**
     * Return a <code>RowKey</code> which can be null, or the default value if
     * the key is not available.
     *
     * @param key identifier used to store the <code>RowKey</code> before
     * @param def default value, returned if the key is not available
     * @return the stored <code>RowKey</code>
     */
    public RowKey getRowKey(final String key, final RowKey def);

    /**
     * Return a <code>RowKey</code> array which can be null, or the default
     * value if the key is not available.
     *
     * @param key identifier used to store the <code>RowKey</code> array before
     * @param def default value, returned if the key is not available
     * @return the stored <code>RowKey</code> array
     */
    public RowKey[] getRowKeyArray(final String key, final RowKey... def);

    /**
     * Return a DataType elements or null for key, or the default value if not
     * available.
     *
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A DataType object or null, or the def value. generic boolean.
     */
    public DataType getDataType(final String key, final DataType def);

    /**
     * Return DataCell array. The array an the elements can be null.
     *
     * @param key The key.
     * @return A DataCell array.
     * @throws InvalidSettingsException If the the key is not available.
     */
    public DataCell[] getDataCellArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return DataCell array which can be null for key, or the default array if
     * the key is not available.
     *
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A char array.
     */
    public DataCell[] getDataCellArray(final String key, final DataCell... def);

    /**
     * Returns an array of DataType objects which can be null.
     *
     * @param key The key.
     * @return An array of DataType objects.
     * @throws InvalidSettingsException The the object is not available for the
     *             given key.
     */
    public DataType[] getDataTypeArray(final String key)
            throws InvalidSettingsException;

    /**
     * Returns the array of DataType objects for the given key or if not
     * available the given array.
     *
     * @param key The key.
     * @param v The default array, returned if no entry available for the key.
     * @return An array of DataType objects.
     */
    public DataType[] getDataTypeArray(final String key, final DataType... v);

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> iterator();

}
