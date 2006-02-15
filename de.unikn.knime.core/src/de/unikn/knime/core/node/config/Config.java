/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   19.01.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;

/**
 * Supports a mechanism to save settings by their type and a key. Furthermore,
 * it provides a method to recursively add new sub <code>Config</code> objects
 * to this Config object, which then results in a tree-like structure.
 * 
 * <p>
 * This class provides several types of settings which are int, double, char,
 * short, byte, boolean, java.lang.String, java.lang.Class, DataCell, and
 * Config. For these supported elements, methods to add either a single or an
 * array or retrieve them back by throwing an
 * <code>InvalidSettingsException</code> or passing a default valid in advance
 * have been implemented.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class Config extends AbstractConfigEntry implements
        Serializable, Iterable<String> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Config.class);

    private final LinkedHashMap<String, ConfigurableEntry> m_map;

    /**
     * Creates a new, empty config object with the given key.
     * 
     * @param key The key for this Config.
     */
    protected Config(final String key) {
        super(key, ConfigEntries.config);
        m_map = new LinkedHashMap<String, ConfigurableEntry>();
    }

    /**
     * Creates a new Config of this type.
     * 
     * @param key The new Config's key.
     * @return A new instance of this Config.
     */
    public abstract Config getInstance(final String key);

    /**
     * Creates a new Config with the given key and returns it.
     * 
     * @param key An identifier.
     * @return A new Config object.
     * @see #getInstance(String)
     */
    public Config addConfig(final String key) {
        final Config config = getInstance(key);
        m_map.put(key, config);
        return config;
    }

    /**
     * Retrieves Config by key.
     * 
     * @param key The key.
     * @return A Config object.
     * @throws InvalidSettingsException If the key is not available.
     */
    public Config getConfig(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for config not found.");
        }
        return (Config)o;
    }

    /**
     * Adds an int.
     * 
     * @param key The key.
     * @param value The int value.
     */
    public void addInt(final String key, final int value) {
        m_map.put(key, new ConfigIntEntry(key, value));
    }

    /**
     * Return int for key.
     * 
     * @param key The key.
     * @return A generic int.
     * @throws InvalidSettingsException If the key is not available.
     */
    public int getInt(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for int not found.");
        }
        return ((ConfigIntEntry)o).getInt();
    }

    /**
     * Adds a double by the given key.
     * 
     * @param key The key.
     * @param value The double value to add.
     */
    public void addDouble(final String key, final double value) {
        m_map.put(key, new ConfigDoubleEntry(key, value));
    }

    /**
     * Return double for key.
     * 
     * @param key The key.
     * @return A generic double.
     * @throws InvalidSettingsException If the key is not available.
     */
    public double getDouble(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for double not found.");
        }
        return ((ConfigDoubleEntry)o).getDouble();
    }

    /**
     * Adds this char value to the Config by the given key.
     * 
     * @param key The key.
     * @param value The char to add.
     */
    public void addChar(final String key, final char value) {
        m_map.put(key, new ConfigCharEntry(key, value));
    }

    /**
     * Return char for key.
     * 
     * @param key The key.
     * @return A generic char.
     * @throws InvalidSettingsException If the key is not available.
     */
    public char getChar(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for char not found.");
        }
        return ((ConfigCharEntry)o).getChar();
    }

    /**
     * Adds this short value to the Config by the given key.
     * 
     * @param key The key.
     * @param value The short to add.
     */
    public void addShort(final String key, final short value) {
        m_map.put(key, new ConfigShortEntry(key, value));
    }

    /**
     * Return short for key.
     * 
     * @param key The key.
     * @return A generic short.
     * @throws InvalidSettingsException If the key is not available.
     */
    public short getShort(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for short not found.");
        }
        return ((ConfigShortEntry)o).getShort();
    }

    /**
     * Adds this byte value to the Config by the given key.
     * 
     * @param key The key.
     * @param value The byte to add.
     */
    public void addByte(final String key, final byte value) {
        m_map.put(key, new ConfigByteEntry(key, value));
    }

    /**
     * Return byte for key.
     * 
     * @param key The key.
     * @return A generic byte.
     * @throws InvalidSettingsException If the key is not available.
     */
    public byte getByte(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for byte not found.");
        }
        return ((ConfigByteEntry)o).getByte();
    }

    /**
     * Adds this String object to the Config by the given key. The String can be
     * null.
     * 
     * @param key The key.
     * @param value The boolean to add.
     */
    public void addString(final String key, final String value) {
        m_map.put(key, new ConfigStringEntry(key, value));
    }

    /**
     * Return String for key.
     * 
     * @param key The key.
     * @return A String object.
     * @throws InvalidSettingsException If the key is not available.
     */
    public String getString(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for String not found.");
        }
        return ((ConfigStringEntry)o).getString();
    }

    /**
     * Adds this DataCell object to the Config by the given key. The cell can be
     * null.
     * 
     * @param key The key.
     * @param cell The DataCell to add.
     */
    public void addDataCell(final String key, final DataCell cell) {
        Config config = addConfig(key);
        if (cell == null) {
            config.addString("datacell", null);
        } else {
            String simpleClass = cell.getClass().getSimpleName();
            ConfigDataCellEntries[] values = ConfigDataCellEntries.values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].name().equals(simpleClass)) {
                    config.addString("datacell", simpleClass);
                    config
                            .addString(simpleClass, values[i]
                                    .toStringValue(cell));
                    return;
                }
            }
            try {
                // if no enum entry found, serialize DataCell
                config.addString("datacell", Config.writeObject(cell));
            } catch (IOException ioe) {
                LOGGER.warn("Could not write DataCell: " + cell);
                LOGGER.debug("", ioe);
            }
        }
    }

    /**
     * Adds this DataType object value to the Config by the given key. The type
     * can be null.
     * 
     * @param key The key.
     * @param type The DataType object to add.
     */
    public void addDataType(final String key, final DataType type) {
        Config config = addConfig(key);
        if (type == null) {
            config.addString("datatype", null);
        } else {
            String simpleClass = type.getClass().getSimpleName();
            ConfigDataTypeEntries[] values = ConfigDataTypeEntries.values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].name().equals(simpleClass)) {
                    config.addString("datatype", simpleClass);
                    return;
                }
            }
            // if no enum entry found, serialize DataType
            try {
                config.addString("datatype", Config.writeObject(type));
            } catch (IOException ioe) {
                LOGGER.warn("Could not write DataType: " + type);
                LOGGER.debug("", ioe);
            }
        }
    }

    /**
     * Return DataCell for key.
     * 
     * @param key The key.
     * @return A DataCell.
     * @throws InvalidSettingsException If the key is not available.
     */
    public DataCell getDataCell(final String key)
            throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key 
                    + " for DataCell not found.");
        }
        Config config = getConfig(key);
        String cell = config.getString("datacell");
        if (cell == null) {
            return null;
        }
        ConfigDataCellEntries[] values = ConfigDataCellEntries.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].name().equals(cell)) {
                String val = config.getString(values[i].name());
                return ConfigDataCellEntries.valueOf(cell).createDataCell(val);
            }
        }
        // if no enum entry found, deserialize DataCell
        try {
            return (DataCell)Config.readObject(cell);
        } catch (IOException ioe) {
            LOGGER.warn("Could not read DataCell: " + cell);
            LOGGER.debug("", ioe);
            return null;
        } catch (ClassNotFoundException cnfe) {
            LOGGER.warn("Could not read DataCell: " + cell);
            LOGGER.debug("", cnfe);
            return null;
        }
    }

    /**
     * Return DataType for key.
     * 
     * @param key The key.
     * @return A DataType.
     * @throws InvalidSettingsException If the key is not available.
     */
    public DataType getDataType(final String key)
            throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key 
                    + " for DataType not found.");
        }
        Config config = getConfig(key);
        String type = config.getString("datatype");
        if (type == null) {
            return null;
        }
        ConfigDataTypeEntries[] values = ConfigDataTypeEntries.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].name().equals(type)) {
                return ConfigDataTypeEntries.valueOf(type).createDataType();
            }
        }
        // if no enum entry found, deserialize DataType
        try {
            return (DataType)Config.readObject(type);
        } catch (IOException ioe) {
            LOGGER.warn("Could not read DataType: " + type);
            LOGGER.debug("", ioe);
            return null;
        } catch (ClassNotFoundException cnfe) {
            LOGGER.warn("Could not read DataType: " + type);
            LOGGER.debug("", cnfe);
            return null;
        }
    }

    /**
     * Returns an unmodifiable Set of keys in this Config.
     * 
     * @return A Set of keys.
     */
    public Set<String> keySet() {
        return Collections.unmodifiableSet(m_map.keySet());
    }

    /**
     * @param otherConfig The other Config to check.
     * @return true if both Config objects store identical entries.
     */
    @Override
    public boolean hasIdenticalValue(final AbstractConfigEntry otherConfig) {
        
        // this should be save as the super ensures identical classes
        Config otherCfg = (Config)otherConfig;
        
        for (String myKey : this.m_map.keySet()) {
            // The other config must contain all keys we've stored.
            if (!otherCfg.m_map.containsKey(myKey)) {
                return false;
            }
            ConfigurableEntry ce = this.m_map.get(myKey);
            ConfigurableEntry otherCe = otherCfg.m_map.get(myKey);
            if (ce == null) {
                if (otherCe != null) {
                    return false;
                }
            } else {
                // and must map an identical value with it.
                if (!ce.isIdentical(otherCe)) {
                    return false;
                }
            }
        }
        
        return true;

    }

    /**
     * Checks if this key for a particluar type is in this Config.
     * 
     * @param key The key.
     * @return <b>true</b> if available, <b>false</b> if key is
     *         <code>null</code> or not available.
     */
    public boolean containsKey(final String key) {
        return m_map.containsKey(key);
    }

    /**
     * Return boolean for key.
     * 
     * @param key The key.
     * @return A generic boolean.
     * @throws InvalidSettingsException If the key is not available.
     */
    public boolean getBoolean(final String key) 
            throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null) {
            throw new InvalidSettingsException(key + " for boolean not found.");
        }
        return ((ConfigBooleanEntry)o).getBoolean();
    }

    /**
     * Adds this boolean value to the Config by the given key.
     * 
     * @param key The key.
     * @param value The boolean to add.
     */
    public void addBoolean(final String key, final boolean value) {
        m_map.put(key, new ConfigBooleanEntry(key, value));
    }

    /**
     * Return int for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic int.
     */
    public int getInt(final String key, final int def) {
        try {
            return getInt(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return int array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @return An int array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public int[] getIntArray(final String key) throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        int[] ret = new int[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getInt("" + i);
        }
        return ret;
    }

    /**
     * Return int array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return An int array.
     */
    public int[] getIntArray(final String key, final int[] def) {
        try {
            return getIntArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Adds this int array to the Config by the given key.
     * 
     * @param key The key.
     * @param values The int array to add.
     */
    public void addIntArray(final String key, final int[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addInt("" + i, values[i]);
            }
        }
    }

    /**
     * Return double for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic double.
     */
    public double getDouble(final String key, final double def) {
        try {
            return getDouble(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return double array for key or the default value if not available.
     * 
     * @param key The key.
     * @return An array of double values.
     * @throws InvalidSettingsException If the key is not available.
     */
    public double[] getDoubleArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        double[] ret = new double[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getDouble("" + i);
        }
        return ret;
    }

    /**
     * Return double array which can be null for key, or the default array if
     * the key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A double array.
     */
    public double[] getDoubleArray(final String key, final double[] def) {
        try {
            return getDoubleArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Adds this double array value to the Config by the given key. The array
     * can be null-
     * 
     * @param key The key.
     * @param values The double array to add.
     */
    public void addDoubleArray(final String key, final double[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addDouble("" + i, values[i]);
            }
        }
    }

    /**
     * Returnchar for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic char.
     */
    public char getChar(final String key, final char def) {
        try {
            return getChar(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return char array which can be null for key.
     * 
     * @param key The key.
     * @return A char array.
     * @throws InvalidSettingsException If the the key is not available.
     */
    public char[] getCharArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        char[] ret = new char[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getChar("" + i);
        }
        return ret;
    }

    /**
     * Return byte array which can be null for key, or the default value if not
     * available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A byte array.
     */
    public byte[] getByteArray(final String key, final byte[] def) {
        try {
            return getByteArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return byte array which can be null for key.
     * 
     * @param key The key.
     * @return A byte array.
     * @throws InvalidSettingsException If the the key is not available.
     */
    public byte[] getByteArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        byte[] ret = new byte[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getByte("" + i);
        }
        return ret;
    }

    /**
     * Adds this byte array to the Config by the given key. The array can be
     * null.
     * 
     * @param key The key.
     * @param values The byte array to add.
     */
    public void addByteArray(final String key, final byte[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addByte("" + i, values[i]);
            }
        }
    }

    /**
     * Return byte for key.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic byte.
     */
    public byte getByte(final String key, final byte def) {
        try {
            return getByte(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return a short array which can be null for key, or the default value if
     * not available.
     * 
     * @param key The key.
     * @return A short array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public short[] getShortArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        short[] ret = new short[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getShort("" + i);
        }
        return ret;
    }

    /**
     * Return short array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A short array.
     */
    public short[] getShortArray(final String key, final short[] def) {
        try {
            return getShortArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Adds this short array to the Config by the given key.
     * 
     * @param key The key.
     * @param values The short to add.
     */
    public void addShortArray(final String key, final short[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addShort("" + i, values[i]);
            }
        }
    }

    /**
     * Return short value for key or the default if the key is not available.
     * 
     * @param key The key.
     * @param def The default values returned if the key is not available.
     * @return A short value.
     */
    public short getShort(final String key, final short def) {
        try {
            return getShort(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return char array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A char array.
     */
    public char[] getCharArray(final String key, final char[] def) {
        try {
            return getCharArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Adds this char array to the Config by the given key.
     * 
     * @param key The key.
     * @param values The char array to add.
     */
    public void addCharArray(final String key, final char[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addChar("" + i, values[i]);
            }
        }
    }

    /**
     * Return boolean for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic boolean.
     */
    public boolean getBoolean(final String key, final boolean def) {
        try {
            return getBoolean(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return a boolean array for key which can be null.
     * 
     * @param key The key.
     * @return A boolean or null.
     * @throws InvalidSettingsException If the key is not available.
     */
    public boolean[] getBooleanArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        boolean[] ret = new boolean[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getBoolean("" + i);
        }
        return ret;
    }

    /**
     * Return a boolean array which can be null for key, or the default value if
     * not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A boolean array.
     */
    public boolean[] getBooleanArray(final String key, final boolean[] def) {
        try {
            return getBooleanArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Adds this boolean values to the Config by the given key. The array can be
     * null.
     * 
     * @param key The key.
     * @param values The boolean array to add.
     */
    public void addBooleanArray(final String key, final boolean[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addBoolean("" + i, values[i]);
            }
        }
    }

    /**
     * Return String object which can be null, or the default array if the key
     * is not available.
     * 
     * @param key The key.
     * @param def The default String returned if the key is not available.
     * @return A String.
     */
    public String getString(final String key, final String def) {
        try {
            return getString(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return String array which can be null for key.
     * 
     * @param key The key.
     * @return A String array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public String[] getStringArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        String[] ret = new String[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getString("" + i);
        }
        return ret;
    }

    /**
     * Return String array which can be null for key, or the default array if
     * the key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A String array.
     */
    public String[] getStringArray(final String key, final String[] def) {
        try {
            return getStringArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Adds this array of String object to the Config by the given key. The
     * array and the elements can be null.
     * 
     * @param key The key.
     * @param values The String array to add.
     */
    public void addStringArray(final String key, final String[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addString("" + i, values[i]);
            }
        }
    }

    /**
     * Return a DataCell which can be null, or the default value if the key is
     * not available.
     * 
     * @param key The key.
     * @param def The default value, returned id the key is not available.
     * @return A DataCell object.
     */
    public DataCell getDataCell(final String key, final DataCell def) {
        try {
            return getDataCell(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return a DataType elements or null for key, or the default value if not
     * available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A DataType object or null, or the def value. generic boolean.
     */
    public DataType getDataType(final String key, final DataType def) {
        try {
            return getDataType(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Return DataCell array. The array an the elements can be null.
     * 
     * @param key The key.
     * @return A DataCell array.
     * @throws InvalidSettingsException If the the key is not available.
     */
    public DataCell[] getDataCellArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        DataCell[] ret = new DataCell[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getDataCell("" + i);
        }
        return ret;
    }

    /**
     * Return DataCell array which can be null for key, or the default array if
     * the key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A char array.
     */
    public DataCell[] getDataCellArray(final String key, final DataCell[] def) {
        try {
            return getDataCellArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Returns an array of DataType objects which can be null.
     * 
     * @param key The key.
     * @return An array of DataType objects.
     * @throws InvalidSettingsException The the object is not available for the
     *             given key.
     */
    public DataType[] getDataTypeArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt("array-size", -1);
        if (size == -1) {
            return null;
        }
        DataType[] ret = new DataType[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getDataType("" + i);
        }
        return ret;
    }

    /**
     * Returns the array of DataType objects for the given key or if not
     * available the given array.
     * 
     * @param key The key.
     * @param v The default array, returned if no entry available for the key.
     * @return An array of DataType objects.
     */
    public DataType[] getDataTypeArray(final String key, final DataType[] v) {
        try {
            return getDataTypeArray(key);
        } catch (InvalidSettingsException ise) {
            return v;
        }
    }

    /**
     * Adds an array of DataCell objects to this Config. The array and all
     * elements can be null.
     * 
     * @param key The key.
     * @param values The data cells, elements can be null.
     */
    public void addDataCellArray(final String key, final DataCell[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addDataCell("" + i, values[i]);
            }
        }
    }

    /**
     * Adds an array of DataType objects to this Config. The array and all
     * elements can be null.
     * 
     * @param key The key.
     * @param values The data types, elements can be null.
     */
    public void addDataTypeArray(final String key, final DataType[] values) {
        Config config = this.addConfig(key);
        if (values != null) {
            config.addInt("array-size", values.length);
            for (int i = 0; i < values.length; i++) {
                config.addDataType("" + i, values[i]);
            }
        }
    }

    /**
     * Returns Config entry for a key.
     * 
     * @param key The key.
     * @return The Config entry for the key.
     */
    AbstractConfigEntry getEntry(final String key) {
        return (AbstractConfigEntry)m_map.get(key);
    }

    /**
     * Adds the given Config entry to this Config.
     * 
     * @param key The key.
     * @param entry The Config entry to add.
     */
    void addEntry(final String key, final ConfigurableEntry entry) {
        m_map.put(key, entry);
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public final Iterator<String> iterator() {
        return keySet().iterator();
    }

    /**
     * @see de.unikn.knime.core.node.config.ConfigurableEntry#toStringValue()
     */
    public final String toStringValue() {
        return toString();
    }

    /**
     * Returns a small summary of this config as String.
     * 
     * @return String representation.
     */
    public final String toString() {
        return toString(0, new StringBuffer());
    }

    private static final int TAB_SIZE = "\t".length();

    private String toString(final int indent, final StringBuffer sb) {
        assert (indent >= 0);
        for (String key : keySet()) {
            for (int t = 0; t < indent * TAB_SIZE; t++) {
                sb.append(" ");
            }
            AbstractConfigEntry e = getEntry(key);
            sb.append("key=" + key + ",type=" + e.getType());
            if (e instanceof Config) {
                int myindent = indent;
                sb.append(":\n");
                Config ms = (Config)e;
                ms.toString(++myindent, sb);
            } else {
                String value = ((ConfigurableEntry)e).toStringValue();
                if (value == null) {
                    sb.append("->null\n");
                } else {
                    sb.append("->" + value);
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /* --- write and read from file --- */

    /**
     * Writes this Config into the given stream.
     * 
     * @param oos Write Config to this stream.
     * @throws IOException If the file can not be accessed.
     */
    public final synchronized void writeToFile(final ObjectOutputStream oos)
            throws IOException {
        oos.writeObject(this);
        oos.close();
    }

    /**
     * Creates new Config from the given file using the serialized object
     * stream.
     * 
     * @param ois Read Config from this stream.
     * @return The new Config.
     * @throws IOException Problem opening the file or content is not a Config.
     */
    protected static synchronized Config readFromFile(
            final ObjectInputStream ois) throws IOException {
        try {
            Config config = (Config)ois.readObject();
            ois.close();
            return config;
        } catch (ClassNotFoundException cnfe) {
            IOException e = new IOException(cnfe.getMessage());
            e.initCause(cnfe);
            throw e;
        }
    }

    /**
     * Writes this Config to the given stream as XML.
     * 
     * @param os The stream to write into.
     * @throws IOException If this Config could be stored to the stream.
     */
    public synchronized void saveToXML(final OutputStream os)
            throws IOException {
        if (os == null) {
            throw new NullPointerException();
        }
        XMLConfig.save(this, os);
    }

    /**
     * Reads Config from XML into the given arg.
     * 
     * @param config Write Config into this object.
     * @param in The stream to read XML Config from.
     * @throws IOException If the Config could not be load from stream.
     */
    protected static synchronized void loadFromXML(final Config config,
            final InputStream in) throws IOException {
        Config.loadFromXML(config, in, false);
    }

    /**
     * Reads Config from XML into a new Config object.
     * 
     * @param config Depending on the readRoot, we write into this Config and
     *            return it.
     * @param in The stream to read XML Config from.
     * @param readRoot If the root XML element should be used to init the root
     *            Config.
     * @return A new Config filled with the content read from XML.
     * @throws IOException If the Config could not be load from stream.
     */
    protected static synchronized Config loadFromXML(final Config config,
            final InputStream in, final boolean readRoot) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        return XMLConfig.load(config, in, readRoot);
    }

    /* --- serialize objects --- */

    /**
     * List of never serialized objects (java.lang.Class), used to print
     * warning.
     */
    private static final Set<Class> UNSUPPORTED = new HashSet<Class>();

    /**
     * Serializes the given object to space-separated integer.
     * 
     * @param o Object to serialize.
     * @return The serialized String.
     */
    private static String writeObject(final Object o) throws IOException {
        // print unsupported Object message
        if (o != null && !UNSUPPORTED.contains(o.getClass())) {
            UNSUPPORTED.add(o.getClass());
            LOGGER.warn(o.getClass() + " not yet supported in Config.");
        }
        // serialize object
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return new BASE64Encoder().encode(baos.toByteArray());
    }

    /**
     * Reads and creates a new Object from the given serialized object stream.
     * 
     * @param string The serialized object's stream.
     * @return A new instance of this object.
     */
    private static Object readObject(final String string) throws IOException,
            ClassNotFoundException {
        byte[] bytes = new BASE64Decoder().decodeBuffer(string);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

} // Config
