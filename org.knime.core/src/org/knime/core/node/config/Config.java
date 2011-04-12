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
 */
package org.knime.core.node.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.ComplexNumberCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.data.def.FuzzyNumberCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.eclipseUtil.GlobalObjectInputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config.DataCellEntry.BooleanCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.ComplexNumberCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.DateAndTimeCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.DoubleCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.FuzzyIntervalCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.FuzzyNumberCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.IntCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.LongCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.MissingCellEntry;
import org.knime.core.node.config.Config.DataCellEntry.StringCellEntry;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.XMLConfig;
import org.xml.sax.SAXException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Supports a mechanism to save settings by their type and a key. Furthermore,
 * it provides a method to recursively add new sub <code>Config</code> objects
 * to this Config object, which then results in a tree-like structure.
 * <p>
 * This class inherits all types from its super class ConfigBase and in addition
 * DataCell, DataType, RowKey, and Config objects. For these supported elements,
 * methods to add either a single or an  array or retrieve them back by throwing
 * an <code>InvalidSettingsException</code> or passing a default valid in
 * advance have been implemented.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class Config extends ConfigBase
        implements ConfigRO, ConfigWO {

    private static final long serialVersionUID = -1823858289784818403L;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Config.class);

    private static final String CFG_ARRAY_SIZE = "array-size";
    private static final String CFG_IS_NULL    = "is_null";
    private static final String CFG_DATA_CELL  = "datacell";
    private static final String CFG_DATA_CELL_SER = "datacell_serialized";

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
         * <code>BooleanCell</code> entry.
         */
        public static final class BooleanCellEntry implements DataCellEntry {
            /**
             * <code>BooleanCell.class</code>.
             */
            public static final Class<BooleanCell> CLASS = BooleanCell.class;
            /**
             * {@inheritDoc}
             */
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addBoolean(CLASS.getSimpleName(),
                        ((BooleanCell) cell).getBooleanValue());
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell createCell(final ConfigRO config)
            throws InvalidSettingsException {
                boolean b = config.getBoolean(CLASS.getSimpleName());
                return b ? BooleanCell.TRUE : BooleanCell.FALSE;
            }
        };

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
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addString(CLASS.getSimpleName(),
                        ((StringCell) cell).getStringValue());
            }
            /**
             * {@inheritDoc}
             */
            @Override
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
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addDouble(CLASS.getSimpleName(),
                        ((DoubleCell) cell).getDoubleValue());
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell createCell(final ConfigRO config)
                    throws InvalidSettingsException {
                return new DoubleCell(config.getDouble(CLASS.getSimpleName()));
            }
        };

        /**
         * <code>LongCell</code> entry.
         */
        public static final class LongCellEntry implements DataCellEntry {
            /**
             * <code>LongCell.class</code>.
             */
            public static final Class<LongCell> CLASS = LongCell.class;
            /**
             * {@inheritDoc}
             */
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addLong(CLASS.getSimpleName(),
                        ((LongCell) cell).getLongValue());
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell createCell(final ConfigRO config)
            throws InvalidSettingsException {
                return new LongCell(config.getLong(CLASS.getSimpleName()));
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
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                config.addInt(CLASS.getSimpleName(),
                        ((IntCell) cell).getIntValue());
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell createCell(final ConfigRO config)
                    throws InvalidSettingsException {
                return new IntCell(config.getInt(CLASS.getSimpleName()));
            }
        };

        /**
         * <code>DateAndTimeCell</code> entry.
         */
        public static final class DateAndTimeCellEntry implements DataCellEntry {
            /**
             * <code>DateAndTimeCell.class</code>.
             */
            public static final Class<DateAndTimeCell> CLASS =
                DateAndTimeCell.class;
            /**
             * {@inheritDoc}
             */
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                ((DateAndTimeCell)cell).save(config);
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell createCell(final ConfigRO config)
            throws InvalidSettingsException {
                return DateAndTimeCell.load(config);
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
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                // nothing to save here
            }
            /**
             * {@inheritDoc}
             */
            @Override
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
            @Override
            public void saveToConfig(final DataCell cell, final Config config) {
                ComplexNumberCell ocell = (ComplexNumberCell) cell;
                config.addDouble(CFG_REAL, ocell.getRealValue());
                config.addDouble(CFG_IMAG, ocell.getImaginaryValue());
            }
            /**
             * {@inheritDoc}
             */
            @Override
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
            @Override
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
            @Override
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
            @Override
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
            @Override
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
        DATACELL_MAP.put(BooleanCellEntry.CLASS.getName(),
                new BooleanCellEntry());
        DATACELL_MAP.put(StringCellEntry.CLASS.getName(),
                new StringCellEntry());
        DATACELL_MAP.put(DoubleCellEntry.CLASS.getName(),
                new DoubleCellEntry());
        DATACELL_MAP.put(IntCellEntry.CLASS.getName(), new IntCellEntry());
        DATACELL_MAP.put(LongCellEntry.CLASS.getName(), new LongCellEntry());
        DATACELL_MAP.put(DateAndTimeCellEntry.CLASS.getName(),
                new DateAndTimeCellEntry());
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
        super(key);
    }

    /**
     * Creates a new Config of this type.
     *
     * @param key The new Config's key.
     * @return A new instance of this Config.
     */
    @Override
    public abstract Config getInstance(final String key);

    /**
     * Creates a new Config with the given key and returns it.
     *
     * @param key An identifier.
     * @return A new Config object.
     */
    @Override
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
    @Override
    public final Config getConfig(final String key)
            throws InvalidSettingsException {
        Object o = get(key);
        if (o == null || !(o instanceof Config)) {
            throw new InvalidSettingsException(
                    "Config for key \"" + key + "\" not found.");
        }
        return (Config) o;
    }

    /**
     * Adds this DataCell object to the Config by the given key. The cell can be
     * null.
     *
     * @param key The key.
     * @param cell The DataCell to add.
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
     * Return a DataCell which can be null, or the default value if the key is
     * not available.
     *
     * @param key The key.
     * @param def The default value, returned id the key is not available.
     * @return A DataCell object.
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
     * Adds the given Config entry to this Config.
     *
     * @param entry The Config entry to add.
     */
    @Override
    public void addEntry(final AbstractConfigEntry entry) {
        put(entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.getKey();
    }

    /* --- write and read from file --- */

    /**
     * Creates new Config from the given file using the serialized object
     * stream.
     *
     * @param ois Read Config from this stream.
     * @return The new Config.
     * @throws IOException Problem opening the file or content is not a Config.
     */
    protected static Config readFromFile(
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
     * Reads Config from XML into a new Config object. The stream will be closed
     * by this call.
     *
     * @param config Depending on the readRoot, we write into this Config and
     *            return it.
     * @param in The stream to read XML Config from.
     * @return A new Config filled with the content read from XML.
     * @throws IOException If the Config could not be load from stream.
     */
    protected static Config loadFromXML(final Config config,
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
    @Override
    public void load(final InputStream is) throws IOException {
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

}
