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
 */
package org.knime.core.node.config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;


/**
 * Interface implements only access functions for <code>Config</code> objects.
 * In addition, it implements the <code>TreeNode</code> and 
 * <code>Iterable</code> interface.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface ConfigRO extends TreeNode, Iterable<String> {
    
    /**
     * Returns a <code>Config</code> for the given key.
     * @param key The identifier for the <code>Config</code>.
     * @return A new <code>Config</code> object.
     * @throws InvalidSettingsException If the Config could not be accessed.
     */
    public Config getConfig(String key) throws InvalidSettingsException;
    
    /**
     * Saves this <code>Config</code> into the given stream in XML format.
     * @param os The stream to write into.
     * @throws IOException If an io exception occurs during writing.
     */
    public void saveToXML(final OutputStream os) throws IOException;
    
    /**
     * Copies this <code>Config</code> into a write-only one.
     * @param config The <code>Config</code> to write this to.
     */
    public void copyTo(ConfigWO config);
    
    /**
     * @return The identifier for this <code>Config</code>.
     */
    public String getKey();
    
    /**
     * Return int for key.
     * 
     * @param key The key.
     * @return A generic int.
     * @throws InvalidSettingsException If the key is not available.
     */
    public int getInt(final String key) throws InvalidSettingsException;

    /**
     * Return double for key.
     * 
     * @param key The key.
     * @return A generic double.
     * @throws InvalidSettingsException If the key is not available.
     */
    public double getDouble(final String key) throws InvalidSettingsException;

    /**
     * Return char for key.
     * 
     * @param key The key.
     * @return A generic char.
     * @throws InvalidSettingsException If the key is not available.
     */
    public char getChar(final String key) throws InvalidSettingsException;

    /**
     * Return short for key.
     * 
     * @param key The key.
     * @return A generic short.
     * @throws InvalidSettingsException If the key is not available.
     */
    public short getShort(final String key) throws InvalidSettingsException;

    /**
     * Return long for key.
     * 
     * @param key The key.
     * @return A generic long.
     * @throws InvalidSettingsException If the key is not available.
     */
    public long getLong(final String key) throws InvalidSettingsException;

    /**
     * Return byte for key.
     * 
     * @param key The key.
     * @return A generic byte.
     * @throws InvalidSettingsException If the key is not available.
     */
    public byte getByte(final String key) throws InvalidSettingsException;

    /**
     * Return String for key.
     * 
     * @param key The key.
     * @return A String object.
     * @throws InvalidSettingsException If the key is not available.
     */
    public String getString(final String key) throws InvalidSettingsException;

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
     * Returns an unmodifiable Set of keys in this Config.
     * 
     * @return A Set of keys.
     */
    public Set<String> keySet();

    /**
     * Checks if this key for a particular type is in this Config.
     * 
     * @param key The key.
     * @return <b>true</b> if available, <b>false</b> if key is
     *         <code>null</code> or not available.
     */
    public boolean containsKey(final String key);

    /**
     * Return boolean for key.
     * 
     * @param key The key.
     * @return A generic boolean.
     * @throws InvalidSettingsException If the key is not available.
     */
    public boolean getBoolean(final String key) throws InvalidSettingsException;

    /**
     * Return int for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic int.
     */
    public int getInt(final String key, final int def);

    /**
     * Return int array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @return An int array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public int[] getIntArray(final String key) throws InvalidSettingsException;

    /**
     * Return int array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return An int array.
     */
    public int[] getIntArray(final String key, final int... def);

    /**
     * Return double for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic double.
     */
    public double getDouble(final String key, final double def);

    /**
     * Return double array for key or the default value if not available.
     * 
     * @param key The key.
     * @return An array of double values.
     * @throws InvalidSettingsException If the key is not available.
     */
    public double[] getDoubleArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return double array which can be null for key, or the default array if
     * the key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A double array.
     */
    public double[] getDoubleArray(final String key, final double... def);
    
    /**
     * Return float for key.
     * 
     * @param key The key.
     * @return A generic float.
     * @throws InvalidSettingsException If the key is not available.
     */
    public float getFloat(final String key) throws InvalidSettingsException;

    /**
     * Return float for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic float.
     */
    public float getFloat(final String key, final float def);

    /**
     * Return float array for key or the default value if not available.
     * 
     * @param key The key.
     * @return An array of float values.
     * @throws InvalidSettingsException If the key is not available.
     */
    public float[] getFloatArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return float array which can be null for key, or the default array if
     * the key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A float array.
     */
    public float[] getFloatArray(final String key, final float... def);

    /**
     * Return char for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic char.
     */
    public char getChar(final String key, final char def);

    /**
     * Return char array which can be null for key.
     * 
     * @param key The key.
     * @return A char array.
     * @throws InvalidSettingsException If the the key is not available.
     */
    public char[] getCharArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return byte array which can be null for key, or the default value if not
     * available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A byte array.
     */
    public byte[] getByteArray(final String key, final byte... def);

    /**
     * Return byte array which can be null for key.
     * 
     * @param key The key.
     * @return A byte array.
     * @throws InvalidSettingsException If the the key is not available.
     */
    public byte[] getByteArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return byte for key.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic byte.
     */
    public byte getByte(final String key, final byte def);

    /**
     * Return a short array which can be null for key, or the default value if
     * not available.
     * 
     * @param key The key.
     * @return A short array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public short[] getShortArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return short array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A short array.
     */
    public short[] getShortArray(final String key, final short... def);

    /**
     * Return a long array which can be null for key, or the default value if
     * not available.
     * 
     * @param key The key.
     * @return A long array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public long[] getLongArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return long array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A long array.
     */
    public long[] getLongArray(final String key, final long... def);

    /**
     * Return short value for key or the default if the key is not available.
     * 
     * @param key The key.
     * @param def The default values returned if the key is not available.
     * @return A short value.
     */
    public short getShort(final String key, final short def);

    /**
     * Return long value for key or the default if the key is not available.
     * 
     * @param key The key.
     * @param def The default values returned if the key is not available.
     * @return A long value.
     */
    public long getLong(final String key, final long def);

    /**
     * Return char array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A char array.
     */
    public char[] getCharArray(final String key, final char... def);

    /**
     * Return boolean for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic boolean.
     */
    public boolean getBoolean(final String key, final boolean def);

    /**
     * Return a boolean array for key which can be null.
     * 
     * @param key The key.
     * @return A boolean or null.
     * @throws InvalidSettingsException If the key is not available.
     */
    public boolean[] getBooleanArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return a boolean array which can be null for key, or the default value if
     * not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A boolean array.
     */
    public boolean[] getBooleanArray(final String key, final boolean... def);

    /**
     * Return String object which can be null, or the default array if the key
     * is not available.
     * 
     * @param key The key.
     * @param def The default String returned if the key is not available.
     * @return A String.
     */
    public String getString(final String key, final String def);

    /**
     * Return String array which can be null for key.
     * 
     * @param key The key.
     * @return A String array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public String[] getStringArray(final String key)
            throws InvalidSettingsException;

    /**
     * Return String array which can be null for key, or the default array if
     * the key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A String array.
     */
    public String[] getStringArray(final String key, final String... def);

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
    public Iterator<String> iterator();

}
