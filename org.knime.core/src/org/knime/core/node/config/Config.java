/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   19.01.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.tree.TreeNode;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.ComplexNumberCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.data.def.FuzzyNumberCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.eclipseUtil.GlobalObjectInputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config.DataCellEntry.ComplexNumberCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.DoubleCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.FuzzyIntervalCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.FuzzyNumberCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.IntCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.MissingCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.StringCellEntry;
import org.xml.sax.SAXException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

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
public abstract class Config extends AbstractConfigEntry 
        implements Serializable, ConfigRO, ConfigWO {

    private static final long serialVersionUID = -1823858289784818403L;
   
    private static final NodeLogger LOGGER = NodeLogger.getLogger(Config.class);

    private static final String CFG_ARRAY_SIZE = "array-size";
    private static final String CFG_IS_NULL    = "is_null";
    private static final String CFG_DATA_CELL  = "datacell";
    private static final String CFG_DATA_CELL_SER = "datacell_serialized";

    private final LinkedHashMap<String, AbstractConfigEntry> m_map;
    
    private void put(final AbstractConfigEntry e) {
        m_map.put(e.getKey(), e);
        e.setParent(this); // (tg)
    }

    /**
     * Interface for all registered <code>DataCell</code> objects.
     */
    interface DataCellEntry {
        /**
         * Save this <code>DataCell</code> to the given <code>Config</code>.
         * @param cell The <code>DataCell</code> to save.
         * @param config To this <code>Config</code>.
         */
        void saveToConfig(DataCell cell, Config config);
        /**
         * Create <code>DataCell</code> on given <code>Config</code>.
         * @param config Used to read <code>DataCell</code> from.
         * @return A new <code>DataCell</code> object.
         * @throws InvalidSettingsException If the cell could not be loaded.
         */
        DataCell createCell(ConfigRO config) throws InvalidSettingsException;
        
        /**
         * <code>StringCell</code> entry.
         */
        public static final class StringCellEntry implements DataCellEntry {
            /**
             * <code>StringCell.class</code>.
             */
            public static final Class<StringCell> CLASS = StringCell.class;
            /**
             * {@inheritDoc}
             */
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addString(CLASS.getSimpleName(), 
                        ((StringCell) cell).getStringValue());
            }
            /**
             * {@inheritDoc}
             */
            public DataCell createCell(final ConfigRO config) 
                    throws InvalidSettingsException {
                return new StringCell(config.getString(CLASS.getSimpleName()));
            }
        };
        
        /**
         * <code>DoubleCell</code> entry.
         */
        public static final class DoubleCellEntry implements DataCellEntry {
            /**
             * <code>DoubleCell.class</code>.
             */
            public static final Class<DoubleCell> CLASS = DoubleCell.class;
            /**
             * {@inheritDoc}
             */
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addDouble(CLASS.getSimpleName(), 
                        ((DoubleCell) cell).getDoubleValue());
            }
            /**
             * {@inheritDoc}
             */
            public DataCell createCell(final ConfigRO config) 
                    throws InvalidSettingsException {
                return new DoubleCell(config.getDouble(CLASS.getSimpleName()));
            }
        };
        
        /**
         * <code>IntCell</code> entry.
         */
        public static final class IntCellEntry implements DataCellEntry {
            /**
             * <code>IntCell.class</code>.
             */
            public static final Class<IntCell> CLASS = IntCell.class;
            /**
             * {@inheritDoc}
             */
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addInt(CLASS.getSimpleName(), 
                        ((IntCell) cell).getIntValue());
            }
            /**
             * {@inheritDoc}
             */
            public DataCell createCell(final ConfigRO config) 
                    throws InvalidSettingsException {
                return new IntCell(config.getInt(CLASS.getSimpleName()));
            }
        };
        
        /**
         * Entry for missing <code>DataCell</code>.
         */
        public static final class MissingCellEntry implements DataCellEntry {
            /**
             * <code>DataType.getMissingCell().getClass()</code>.
             */
            public static final Class<? extends DataCell> CLASS = 
                DataType.getMissingCell().getClass();
            /**
             * {@inheritDoc}
             */
            public void saveToConfig(final DataCell cell, final Config config) {
                // nothing to save here
            }
            /**
             * {@inheritDoc}
             */
            public DataCell createCell(final ConfigRO config) 
                    throws InvalidSettingsException {
                return DataType.getMissingCell();
            }
        };
        
        /**
         * <code>ComplexNumberCell</code> entry.
         */
        public static final class ComplexNumberCellEntry 
                implements DataCellEntry {
            private static final String CFG_REAL = "real";
            private static final String CFG_IMAG = "imaginary";
            /**
             * <code>ComplexNumberCell.class</code>.
             */
            public static final Class<ComplexNumberCell> CLASS = 
                ComplexNumberCell.class;
            /**
             * {@inheritDoc}
             */
            public void saveToConfig(final DataCell cell, final Config config) {
                ComplexNumberCell ocell = (ComplexNumberCell) cell;
                config.addDouble(CFG_REAL, ocell.getRealValue());
                config.addDouble(CFG_IMAG, ocell.getImaginaryValue());
            }
            /**
             * {@inheritDoc}
             */
            public DataCell createCell(final ConfigRO config) 
                    throws InvalidSettingsException {
                double r = config.getDouble(CFG_REAL);
                double i = config.getDouble(CFG_IMAG);
                return new ComplexNumberCell(r, i);
            }
        };
        
        /**
         * <code>FuzzyIntervalCell</code> entry.
         */
        public static final class FuzzyIntervalCellEntry 
                implements DataCellEntry {
            private static final String CFG_MIN_SUPP = "min_supp";
            private static final String CFG_MIN_CORE = "min_core";
            private static final String CFG_MAX_CORE = "max_core";
            private static final String CFG_MAX_SUPP = "max_supp";
            /** <code>FuzzyIntervalCell.class</code>. */
            public static final Class<FuzzyIntervalCell> CLASS = 
                FuzzyIntervalCell.class;
            /**
             * {@inheritDoc}
             */
            public void saveToConfig(final DataCell cell, final Config config) {
                FuzzyIntervalCell ocell = 
                    (FuzzyIntervalCell) cell;
                config.addDouble(CFG_MIN_SUPP, ocell.getMinSupport());
                config.addDouble(CFG_MIN_CORE, ocell.getMinCore());
                config.addDouble(CFG_MAX_CORE, ocell.getMaxCore());
                config.addDouble(CFG_MAX_SUPP, ocell.getMaxSupport());
            }
            /**
             * {@inheritDoc}
             */
            public DataCell createCell(final ConfigRO config) 
                    throws InvalidSettingsException {
                double minSupp = config.getDouble(CFG_MIN_SUPP);
                double minCore = config.getDouble(CFG_MIN_CORE);
                double maxCore = config.getDouble(CFG_MAX_CORE);
                double maxSupp = config.getDouble(CFG_MAX_SUPP);
                return new FuzzyIntervalCell(
                        minSupp, minCore, maxCore, maxSupp);
            }
        };
        
        /**
         * <code>FuzzyNumberCell</code> entry.
         */
        public static final class FuzzyNumberCellEntry 
                implements DataCellEntry {
            private static final String CFG_LEFT = "left";
            private static final String CFG_CORE = "core";
            private static final String CFG_RIGHT = "right";
            /** <code>FuzzyNumberCell.class</code>. */
            public static final Class<FuzzyNumberCell> CLASS = 
                FuzzyNumberCell.class;
            /**
             * {@inheritDoc}
             */
            public void saveToConfig(final DataCell cell, final Config config) {
                FuzzyNumberCell ocell = (FuzzyNumberCell) cell;
                config.addDouble(CFG_LEFT,  ocell.getMinSupport());
                config.addDouble(CFG_CORE,  ocell.getMinCore());
                assert ocell.getMinCore() == ocell.getMaxCore();
                config.addDouble(CFG_RIGHT, ocell.getMaxSupport());
            }
            /**
             * {@inheritDoc}
             */
            public DataCell createCell(final ConfigRO config) 
                    throws InvalidSettingsException {
                double left  = config.getDouble(CFG_LEFT);
                double core  = config.getDouble(CFG_CORE);
                double right = config.getDouble(CFG_RIGHT);
                return new FuzzyNumberCell(left, core, right);
            }
        };
    }
    
    /**
     * Keeps all registered <code>DataCell</code> objects which are mapped
     * to <code>DataCellEntry</code> values in order to save and load them.
     */
    private static final HashMap<String, DataCellEntry> DATACELL_MAP
        = new HashMap<String, DataCellEntry>();

    static {
        DATACELL_MAP.put(StringCellEntry.CLASS.getName(), 
                new StringCellEntry());
        DATACELL_MAP.put(DoubleCellEntry.CLASS.getName(),
                new DoubleCellEntry());
        DATACELL_MAP.put(IntCellEntry.CLASS.getName(), new IntCellEntry());
        DATACELL_MAP.put(MissingCellEntry.CLASS.getName(), 
                new MissingCellEntry());
        DATACELL_MAP.put(ComplexNumberCellEntry.CLASS.getName(), 
                new ComplexNumberCellEntry());
        DATACELL_MAP.put(FuzzyIntervalCellEntry.CLASS.getName(), 
                new FuzzyIntervalCellEntry());
        DATACELL_MAP.put(FuzzyNumberCellEntry.CLASS.getName(), 
                new FuzzyNumberCellEntry());
    }
    
    /**
     * Creates a new, empty config object with the given key.
     * 
     * @param key The key for this Config.
     */
    protected Config(final String key) {
        super(ConfigEntries.config, key);
        m_map = new LinkedHashMap<String, AbstractConfigEntry>(1, 0.8f);
    }

    /**
     * Creates a new Config of this type.
     * 
     * @param key The new Config's key.
     * @return A new instance of this Config.
     */
    protected abstract Config getInstance(final String key);

    /**
     * Creates a new Config with the given key and returns it.
     * 
     * @param key An identifier.
     * @return A new Config object.
     * @see #getInstance(String)
     */
    public final Config addConfig(final String key) {
        final Config config = getInstance(key);
        put(config);
        return config;
    }
    
    /**
     * Appends the given Config to this Config which has to directly derived 
     * from this class.
     * 
     * @param config The Config to append.
     * @throws NullPointerException If <code>config</code> is null. 
     * @throws IllegalArgumentException If <code>config</code> is not instance
     *         of this class.
     */
    protected final void addConfig(final Config config) {
        if (getClass() != config.getClass()) {
            throw new IllegalArgumentException("This " + getClass() 
                    + " is not equal to " + config.getClass());
        }
        put(config);
    }

    /**
     * Retrieves Config by key.
     * 
     * @param key The key.
     * @return A Config object.
     * @throws InvalidSettingsException If the key is not available.
     */
    public final Config getConfig(final String key) 
        throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null || !(o instanceof Config)) {
            throw new InvalidSettingsException(
                    "Config for key \"" + key + "\" not found.");
        }
        return (Config) o;
    }

    /**
     * Adds an int.
     * 
     * @param key The key.
     * @param value The int value.
     */
    public void addInt(final String key, final int value) {
        put(new ConfigIntEntry(key, value));
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
        if (o == null || !(o instanceof ConfigIntEntry)) {
            throw new InvalidSettingsException(
                    "Int for key \"" + key + "\" not found.");
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
        put(new ConfigDoubleEntry(key, value));
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
        if (o == null || !(o instanceof ConfigDoubleEntry)) {
            throw new InvalidSettingsException(
                    "Double for key \"" + key + "\" not found.");
        }
        return ((ConfigDoubleEntry)o).getDouble();
    }
    
    /**
     * Adds a float by the given key.
     * 
     * @param key The key.
     * @param value The float value to add.
     */
    public void addFloat(final String key, final float value) {
        put(new ConfigFloatEntry(key, value));
    }

    /**
     * Return float for key.
     * 
     * @param key The key.
     * @return A generic float.
     * @throws InvalidSettingsException If the key is not available.
     */
    public float getFloat(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigFloatEntry)) {
            throw new InvalidSettingsException(
                    "Float for key \"" + key + "\" not found.");
        }
        return ((ConfigFloatEntry)o).getFloat();
    }

    /**
     * Adds this char value to the Config by the given key.
     * 
     * @param key The key.
     * @param value The char to add.
     */
    public void addChar(final String key, final char value) {
        put(new ConfigCharEntry(key, value));
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
        if (o == null || !(o instanceof ConfigCharEntry)) {
            throw new InvalidSettingsException(
                    "Char for key \"" + key + "\" not found.");
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
        put(new ConfigShortEntry(key, value));
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
        if (o == null || !(o instanceof ConfigShortEntry)) {
            throw new InvalidSettingsException(
                    "Short for key \"" + key + "\" not found.");
        }
        return ((ConfigShortEntry)o).getShort();
    }
    
    /** 
     * Adds this long value to the Config by the given key.
     * 
     * @param key The key.
     * @param value The long to add.
     */
    public void addLong(final String key, final long value) {
        put(new ConfigLongEntry(key, value));
    }

    /**
     * Return long for key.
     * 
     * @param key The key.
     * @return A generic long.
     * @throws InvalidSettingsException If the key is not available.
     */
    public long getLong(final String key) throws InvalidSettingsException {
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigLongEntry)) {
            throw new InvalidSettingsException(
                    "Long for key \"" + key + "\" not found.");
        }
        return ((ConfigLongEntry)o).getLong();
    }
    

    /**
     * Adds this byte value to the Config by the given key.
     * 
     * @param key The key.
     * @param value The byte to add.
     */
    public void addByte(final String key, final byte value) {
        put(new ConfigByteEntry(key, value));
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
        if (o == null || !(o instanceof ConfigByteEntry)) {
            throw new InvalidSettingsException(
                    "Byte for key \"" + key + "\" not found.");
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
        put(new ConfigStringEntry(key, value));
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
        if (o == null || !(o instanceof ConfigStringEntry)) {
            throw new InvalidSettingsException(
                    "String for key \"" + key + "\" not found.");
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
        ConfigWO config = addConfig(key);
        if (cell == null) {
            config.addString(CFG_DATA_CELL, null);
        } else {
            String className = cell.getClass().getName();
            Object o = DATACELL_MAP.get(className);
            if (o != null) {
               config.addString(CFG_DATA_CELL, className);
               DataCellEntry e = (DataCellEntry) o;
               Config cellConfig = config.addConfig(className);
               e.saveToConfig(cell, cellConfig);
            } else { 
                try {
                    // serialize DataCell
                    config.addString(CFG_DATA_CELL, className);
                    config.addString(CFG_DATA_CELL_SER, 
                            Config.writeObject(cell));
                } catch (IOException ioe) {
                    LOGGER.warn("Could not write DataCell: " + cell);
                    LOGGER.debug("", ioe);
                }
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
        ConfigWO config = addConfig(key);
        if (type == null) {
            config.addBoolean(CFG_IS_NULL, true);
        } else {
            type.save(config);
            config.addBoolean(CFG_IS_NULL, false);
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
        ConfigRO config = getConfig(key);
        String className = config.getString(CFG_DATA_CELL);
        if (className == null) {
            return null;
        }

        
        Object o = null;
        if (className.startsWith("de.unikn.knime.")) {
            o = DATACELL_MAP.get(className.replace("de.unikn.knime.",
                    "org.knime."));
            // this may fail, e.g for de.unikn.knime.altanaexp, thus
            // the fallback is also tried
        }
        if (o == null) {
            o = DATACELL_MAP.get(className);
        }
        
         
        if (o != null) {
            Config cellConfig = config.getConfig(className);
            DataCellEntry e = (DataCellEntry) o;
            return e.createCell(cellConfig);
        } else {
            // deserialize DataCell
            try {
                String serString = config.getString(CFG_DATA_CELL_SER, null);
                if (serString == null) { // backward comp. to v1.0.0
                    return (DataCell)Config.readObject(null, className);
                } else {
                    return (DataCell)Config.readObject(className, serString);
                }
            } catch (IOException ioe) {
                LOGGER.warn("Could not read DataCell: " + className);
                LOGGER.debug("", ioe);
                return null;
            } catch (ClassNotFoundException cnfe) {
                LOGGER.warn("Could not read DataCell: " + className);
                LOGGER.debug("", cnfe);
                return null;
            }
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
        Config config = getConfig(key);
        boolean isNull = config.getBoolean(CFG_IS_NULL);
        if (isNull) {
            return null;
        }
        return DataType.load(config);
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

        if (this.m_map.size() != otherCfg.m_map.size()) {
           return false;
        }
        
        for (String myKey : this.m_map.keySet()) {
            // The other config must contain all keys we've stored.
            if (!otherCfg.m_map.containsKey(myKey)) {
                return false;
            }
            AbstractConfigEntry ce = this.m_map.get(myKey);
            AbstractConfigEntry otherCe = otherCfg.m_map.get(myKey);
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
     * Checks if this key for a particular type is in this Config.
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
        if (o == null || !(o instanceof ConfigBooleanEntry)) {
            throw new InvalidSettingsException(
                    "Boolean for key \"" + key + "\" not found.");
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
        put(new ConfigBooleanEntry(key, value));
    }

    /**
     * Return int for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic int.
     */
    public int getInt(final String key, final int def) {
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigIntEntry)) {
            return def;
        }
        return ((ConfigIntEntry)o).getInt();
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        int[] ret = new int[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getInt(Integer.toString(i));
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
    public int[] getIntArray(final String key, final int... def) {
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
    public void addIntArray(final String key, final int... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addInt(Integer.toString(i), values[i]);
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
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigDoubleEntry)) {
            return def;
        }
        return ((ConfigDoubleEntry)o).getDouble();
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        double[] ret = new double[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getDouble(Integer.toString(i));
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
    public double[] getDoubleArray(final String key, final double... def) {
        try {
            return getDoubleArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }
    
    /**
     * Return float for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic float.
     */
    public float getFloat(final String key, final float def) {
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigFloatEntry)) {
            return def;
        }
        return ((ConfigFloatEntry)o).getFloat();
    }

    /**
     * Return float array for key or the default value if not available.
     * 
     * @param key The key.
     * @return An array of float values.
     * @throws InvalidSettingsException If the key is not available.
     */
    public float[] getFloatArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        float[] ret = new float[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getFloat(Integer.toString(i));
        }
        return ret;
    }

    /**
     * Return float array which can be null for key, or the default array if
     * the key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A float array.
     */
    public float[] getFloatArray(final String key, final float... def) {
        try {
            return getFloatArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }

    /**
     * Adds this double array value to the Config by the given key. The array
     * can be null.
     * 
     * @param key The key.
     * @param values The double array to add.
     */
    public void addDoubleArray(final String key, final double... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addDouble(Integer.toString(i), values[i]);
            }
        }
    }
    
    /**
     * Adds this float array value to the Config by the given key. The array
     * can be null.
     * 
     * @param key The key.
     * @param values The float array to add.
     */
    public void addFloatArray(final String key, final float... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addFloat(Integer.toString(i), values[i]);
            }
        }
    }

    /**
     * Return char for key or the default value if not available.
     * 
     * @param key The key.
     * @param def Returned if no value available for the given key.
     * @return A generic char.
     */
    public char getChar(final String key, final char def) {
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigCharEntry)) {
            return def;
        }
        return ((ConfigCharEntry)o).getChar();
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        char[] ret = new char[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getChar(Integer.toString(i));
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
    public byte[] getByteArray(final String key, final byte... def) {
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        byte[] ret = new byte[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getByte(Integer.toString(i));
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
    public void addByteArray(final String key, final byte... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addByte(Integer.toString(i), values[i]);
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
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigByteEntry)) {
            return def;
        }
        return ((ConfigByteEntry)o).getByte();
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        short[] ret = new short[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getShort(Integer.toString(i));
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
    public short[] getShortArray(final String key, final short... def) {
        try {
            return getShortArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }
    
    /**
     * Return a long array which can be null for key, or the default value if
     * not available.
     * 
     * @param key The key.
     * @return A long array.
     * @throws InvalidSettingsException If the key is not available.
     */
    public long[] getLongArray(final String key)
            throws InvalidSettingsException {
        Config config = this.getConfig(key);
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        long[] ret = new long[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getLong(Integer.toString(i));
        }
        return ret;
    }

    /**
     * Return long array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A long array.
     */
    public long[] getLongArray(final String key, final long... def) {
        try {
            return getLongArray(key);
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
    public void addShortArray(final String key, final short... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addShort(Integer.toString(i), values[i]);
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
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigShortEntry)) {
            return def;
        }
        return ((ConfigShortEntry)o).getShort();
    }
    
    /**
     * Adds this long array to the Config by the given key.
     * 
     * @param key The key.
     * @param values The long arry to add.
     */
    public void addLongArray(final String key, final long... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addLong(Integer.toString(i), values[i]);
            }
        }
    }

    /**
     * Return long value for key or the default if the key is not available.
     * 
     * @param key The key.
     * @param def The default values returned if the key is not available.
     * @return A long value.
     */
    public long getLong(final String key, final long def) {
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigLongEntry)) {
            return def;
        }
        return ((ConfigLongEntry)o).getLong();
    }

    /**
     * Return char array which can be null for key, or the default array if the
     * key is not available.
     * 
     * @param key The key.
     * @param def The default array returned if the key is not available.
     * @return A char array.
     */
    public char[] getCharArray(final String key, final char... def) {
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
    public void addCharArray(final String key, final char... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addChar(Integer.toString(i), values[i]);
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
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigBooleanEntry)) {
            return def;
        }
        return ((ConfigBooleanEntry)o).getBoolean();
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        boolean[] ret = new boolean[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getBoolean(Integer.toString(i));
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
    public boolean[] getBooleanArray(final String key, final boolean... def) {
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
    public void addBooleanArray(final String key, final boolean... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addBoolean(Integer.toString(i), values[i]);
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
        Object o = m_map.get(key);
        if (o == null || !(o instanceof ConfigStringEntry)) {
            return def;
        }
        return ((ConfigStringEntry)o).getString();
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        String[] ret = new String[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getString(Integer.toString(i));
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
    public String[] getStringArray(final String key, final String... def) {
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
    public void addStringArray(final String key, final String... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addString(Integer.toString(i), values[i]);
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        DataCell[] ret = new DataCell[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getDataCell(Integer.toString(i));
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
    public DataCell[] getDataCellArray(final String key, 
            final DataCell... def) {
        try {
            return getDataCellArray(key);
        } catch (InvalidSettingsException ise) {
            return def;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public RowKey getRowKey(final String key) throws InvalidSettingsException {
        String rk = getString(key);
        return (rk == null ? null : new RowKey(rk));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowKey getRowKey(final String key, final RowKey def) {
        String rk;
        if (def == null) {
            rk = getString(key, null);
        } else {
            rk = getString(key, def.getString());
        }
        return (rk == null ? null : new RowKey(rk));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRowKey(final String key, final RowKey rowKey) {
        if (rowKey == null) {
            addString(key, null);
        } else {
           addString(key, rowKey.getString());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public RowKey[] getRowKeyArray(final String key) 
            throws InvalidSettingsException {
        String[] strs = getStringArray(key);
        return RowKey.toRowKeys(strs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowKey[] getRowKeyArray(final String key, final RowKey... def) {
        String[] strs;
        if (def == null) {
            strs = getStringArray(key, (String[]) null);
        } else {
            String[] defStrs = RowKey.toStrings(def); 
            strs = getStringArray(key, defStrs);
        }
        return (strs == null ? null : RowKey.toRowKeys(strs));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRowKeyArray(final String key, final RowKey... rowKey) {
        if (rowKey == null) {
            addStringArray(key, (String[]) null);
        } else {
           addStringArray(key, RowKey.toStrings(rowKey));
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
        int size = config.getInt(CFG_ARRAY_SIZE, -1);
        if (size == -1) {
            return null;
        }
        DataType[] ret = new DataType[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = config.getDataType(Integer.toString(i));
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
    public DataType[] getDataTypeArray(final String key, final DataType... v) {
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
    public void addDataCellArray(final String key, final DataCell... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addDataCell(Integer.toString(i), values[i]);
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
    public void addDataTypeArray(final String key, final DataType... values) {
        ConfigWO config = this.addConfig(key);
        if (values != null) {
            config.addInt(CFG_ARRAY_SIZE, values.length);
            for (int i = 0; i < values.length; i++) {
                config.addDataType(Integer.toString(i), values[i]);
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
        return m_map.get(key);
    }

    /**
     * Adds the given Config entry to this Config.
     * 
     * @param entry The Config entry to add.
     */
    void addEntry(final AbstractConfigEntry entry) {
        put(entry);
    }

    /**
     * {@inheritDoc}
     */
    public final Iterator<String> iterator() {
        return keySet().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toStringValue() {
        return super.getKey();
    }

    /**
     * Adds this and all children String representations to the given buffer.
     * @param buf The string buffer to which this Config's String all all 
     *        children String representation is added.
     */
    public final void toString(final StringBuffer buf) {
        toString(0, buf);
        buf.trimToSize();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.getKey();
    }
    
    private static final int TAB_SIZE = 2;
    
    private static final String SPACE = " ";
    private static final String KEYEQ = "key=";
    private static final String COMMA_TYPEEQ = ",type=";
    private static final String LINE_BREAK = "\n";
    private static final String DOT_LINE_BREAK = ":\n";
    private static final String ARROW_NULL = "->null";
    private static final String ARROW = "->";

    private void toString(final int indent, final StringBuffer sb) {
        assert (indent >= 0);
        sb.ensureCapacity(1000);
        for (String key : m_map.keySet()) {
            for (int t = 0; t < indent * TAB_SIZE; t++) {
                sb.append(SPACE);
            }
            AbstractConfigEntry e = getEntry(key);
            sb.append(KEYEQ);
            sb.append(key);
            sb.append(COMMA_TYPEEQ);
            sb.append(e.getType());
            if (e instanceof Config) {
                int myindent = indent;
                sb.append(DOT_LINE_BREAK);
                Config ms = (Config)e;
                ms.toString(++myindent, sb);
            } else {
                String value = e.toStringValue();
                if (value == null) {
                    sb.append(ARROW_NULL);
                } else {
                    sb.append(ARROW);
                    sb.append(value);
                }
                sb.append(LINE_BREAK);
            }
        }
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
     * Writes this Config to the given stream as XML. The stream will be closed
     * when finished.
     * 
     * @param os The stream to write into.
     * @throws IOException If this Config could be stored to the stream.
     */
    public final synchronized void saveToXML(final OutputStream os)
            throws IOException {
        if (os == null) {
            throw new NullPointerException();
        }
        XMLConfig.save(this, os);
    }

    /**
     * Reads Config from XML into a new Config object.
     * 
     * @param config Depending on the readRoot, we write into this Config and
     *            return it.
     * @param in The stream to read XML Config from.
     * @return A new Config filled with the content read from XML.
     * @throws IOException If the Config could not be load from stream.
     */
    protected static synchronized Config loadFromXML(final Config config,
            final InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        config.load(in);
        return config;
    }
    
    /**
     * Read config entries from an XML file into this object.
     * @param is The XML inputstream storing the configuration to read
     * @throws IOException If the stream could not be read.
     */
    protected void load(final InputStream is) throws IOException {
        try {
            XMLConfig.load(this, is);
        } catch (SAXException se) {
            IOException ioe = new IOException(se.getMessage());
            ioe.initCause(se);
            throw ioe;
        } catch (ParserConfigurationException pce) {
            IOException ioe = new IOException(pce.getMessage());
            ioe.initCause(pce);
            throw ioe;
        } finally {
            is.close();
        }
    }

    /* --- serialize objects --- */

    /**
     * List of never serialized objects (java.lang.Class), used to print
     * warning.
     */
    private static final Set<Class<?>> UNSUPPORTED = new HashSet<Class<?>>();

    /**
     * Serializes the given object to space-separated integer.
     * 
     * @param o Object to serialize.
     * @return The serialized String.
     * @throws IOException if an I/O error occurs during serializing the object
     */
    private static final String writeObject(final Object o) throws IOException {
        // print unsupported Object message
        if (o != null && !UNSUPPORTED.contains(o.getClass())) {
            UNSUPPORTED.add(o.getClass());
            LOGGER.debug("Class " + o.getClass() 
                    + " not yet supported in Config, serializing it.");
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
     * @throws IOException if an I/O error occurs during reading the object
     * @throws ClassNotFoundException if the class of the serialized object
     *  cannot be found. 
     */
    private static final Object readObject(final String className, 
            final String serString) 
            throws IOException, ClassNotFoundException {
        byte[] bytes = new BASE64Decoder().decodeBuffer(serString);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        GlobalObjectInputStream ois;
        if (className == null) {
            ois = new GlobalObjectInputStream(bais);
        } else {
            final Class<?> cl = GlobalClassCreator.createClass(className);
            if (cl == null) {
                throw new ClassNotFoundException("Could not find class: " + cl);
            }
            ois = new GlobalObjectInputStream(bais) {
                @Override
                protected Class<?> resolveClass(final ObjectStreamClass desc) 
                        throws IOException, ClassNotFoundException {
                    ClassLoader clLoader = cl.getClassLoader();
                    try {
                        return Class.forName(desc.getName(), true, clLoader);
                    } catch (ClassNotFoundException cnfe) {
                        // ignore and let super try to do it.
                    }
                    return super.resolveClass(desc);
                }  
            };
        }
        return ois.readObject();
    }
    
    

    /**
     * Makes a deep copy of this Config and all sub-configs.
     * 
     * @param dest the destination this Config object is copied to.
     */
    public void copyTo(final ConfigWO dest) {
        for (Map.Entry<String, AbstractConfigEntry> e : m_map.entrySet()) {
            AbstractConfigEntry ace = e.getValue();
            if (ace instanceof Config) {
                Config config = dest.addConfig(ace.getKey());
                ((Config) ace).copyTo(config);
            } else {
                ((Config) dest).addEntry(ace);
            }
        }
    }
    
    // tree node methods
    
    /**
     * The TreeNode for the given index.
     * @param childIndex The index to retrieve the TreeNode for.
     * @return The associated TreeNode.
     */
    @Override
    public TreeNode getChildAt(final int childIndex) {
        Iterator<String> it = m_map.keySet().iterator();
        for (int i = 0; i < childIndex; i++) {
            it.next();
        }
        TreeNode node = m_map.get(it.next());
        return node;
    }

    /**
     * @return The number of entries in this Config.
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    @Override
    public int getChildCount() {
        return m_map.size();
    }

    /**
     * Returns the index for a given TreeNode.
     * @param node The TreeNode to get the index for.
     * @return The index of the given node.
     * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
     */
    @Override
    public int getIndex(final TreeNode node) {
        int i = 0;
        for (Map.Entry<String, AbstractConfigEntry> e : m_map.entrySet()) {
            if (e.getValue().equals(node)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * @return true, only if the map is empty.
     * @see javax.swing.tree.TreeNode#isLeaf()
     */
    @Override
    public final boolean isLeaf() {
        return m_map.isEmpty();
    }
    
    /**
     * An enumeration of a values.
     * @return All elements of this Config.
     * @see javax.swing.tree.TreeNode#children()
     */
    @Override
    public final Enumeration<TreeNode> children() {
        return new Vector<TreeNode>(m_map.values()).elements();
    }
    
} // Config
