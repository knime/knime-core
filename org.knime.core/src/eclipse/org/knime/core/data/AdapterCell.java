/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on 01.11.2012 by meinl
 */
package org.knime.core.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.collection.CellCollection;
import org.knime.core.data.collection.DefaultBlobSupportDataCellIterator;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.BufferedTableBackend;
import org.knime.core.node.workflow.WorkflowTableBackendSettings;

/**
 * Abstract implementation of an adapter cell. An adapter cells allows to store several representation of an entity
 * (e.g. a molecule) into one cell. The representation is identified by its value class (see {@link DataValue}) and is
 * itself a data cell. Initially an adapter is created with one data cell (for possibly more value classes) using the
 * public constructor. Additional representation can be added by creating a copy of the adapter cell and adding a new
 * cell with {@link #cloneAndAddAdapter(DataCell, Class...)}. Access to the contents is possible with the methods from
 * the {@link AdapterValue} interface.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.7
 */
@SuppressWarnings({"serial"})
public abstract class AdapterCell extends DataCell implements Cloneable, RWAdapterValue, CellCollection {
    private static final int MAX_ADAPTERS = 127;

    /**
     * (De)serializer for adapter cells. If you subclass the AdapterCell you must also subclass this serializer and
     * override at least {@link #deserialize(DataCellDataInput)} in order to return a new cell object.
     *
     * @author Thorsten Meinl, University of Konstanz
     * @param <T> any subclass of adapter cell that this serializer is responsible for
     */
    protected abstract static class AdapterCellSerializer<T extends AdapterCell> implements DataCellSerializer<T> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final T cell, final DataCellDataOutput output) throws IOException {
            List<Class<? extends DataValue>> values =
                    new ArrayList<Class<? extends DataValue>>(((AdapterCell) cell).m_adapterMap.size());
            // temporary map which gets reduced for each data cell written
            @SuppressWarnings("unchecked")
            Map<Class<? extends DataValue>, DataCell> temp =
                    (Map<Class<? extends DataValue>, DataCell>)((AdapterCell) cell).m_adapterMap.clone();

            // this may seem inefficient, but we assume that there are really only a few adapters per cell
            for (Map.Entry<Class<? extends DataValue>, DataCell> e1 : ((AdapterCell) cell).m_adapterMap.entrySet()) {
                for (Map.Entry<Class<? extends DataValue>, DataCell> e2 : temp.entrySet()) {
                    if ((e2.getValue() == e1.getValue()) && !e2.getKey().isAssignableFrom(AdapterCell.class)) {
                        // only add value class if contents are equal AND
                        // if the value class is not a generic value class implemented by the AdapterCell
                        // e.g. DataValue or RWAdapterValue
                        values.add(e2.getKey());
                    }
                }

                if (values.size() > 0) {
                    assert values.size() <= MAX_ADAPTERS : "More than " + MAX_ADAPTERS + " adapters in cell";
                    // write number of value class names that follow
                    output.writeByte(values.size());
                    for (Class<? extends DataValue> cl : values) {
                        // write value class names
                        output.writeUTF(cl.getName());
                        temp.remove(cl);
                    }
                    // write the data cell itself
                    output.writeDataCell(e1.getValue());
                    values.clear();
                }
            }
            output.writeByte(0); // end of adapters for this cell
        }
    }

    private BlobWrapperHashMap m_adapterMap;

    /**
     * This constructor should only be used by the de-serializer. It de-serializes a cell according to
     * {@link AdapterCellSerializer#serialize(AdapterCell, DataCellDataOutput)}.
     *
     * @param input a data cell input provided by a data cell serializer
     * @throws IOException if an error during de-serialization occurs
     */
    protected AdapterCell(final DataCellDataInput input) throws IOException {
        int valueCount = input.readByte();
        if (valueCount == 0) {
            throw new IllegalStateException("Empty adpater cells are not allowed");
        }

        m_adapterMap = new BlobWrapperHashMap();

        List<Class<? extends DataValue>> values = new ArrayList<>(Math.max(valueCount, 4));
        while (valueCount > 0) {
            // first read names of values classes
            for (int i = 0; i < valueCount; i++) {
                String className = input.readUTF();
                values.add(DataTypeRegistry.getInstance().getValueClass(className).orElseThrow(() -> new IOException(
                    "Error while reading adapter values, value class " + className + " is unknown")));
            }
            DataCell cell = input.readDataCell();
            for (Class<? extends DataValue> cl : values) {
                m_adapterMap.put(cl, cell);
            }
            values.clear();

            valueCount = input.readByte();
        }
    }

    /**
     * Used by extensions to fill adapter map from an existing AdapterValue. Rest is equivalent to
     * {@link AdapterCell#AdapterCell(DataCell, Class...)}.
     *
     * @param valueCell any data cell
     * @param predefinedAdapters Existing adapters.
     * @param valueClasses the value classes that this adapter cell should be adaptable to
     */
    protected AdapterCell(final DataCell valueCell, final AdapterValue predefinedAdapters,
                          final Class<? extends DataValue>... valueClasses) {
        m_adapterMap = new BlobWrapperHashMap();
        if (valueClasses.length == 0) {
            for (Class<? extends DataValue> v : valueCell.getType().getValueClasses()) {
                if (!v.isAssignableFrom(AdapterCell.class)) {
                    // don't add adapters for generic value interfaces such as DataValue or RWAdapterValue
                    // they are handled directly by this cell implementation
                    m_adapterMap.put(v, valueCell);
                }
            }
        } else {
            for (Class<? extends DataValue> v : valueClasses) {
                if (!valueCell.isMissing() && !v.isAssignableFrom(valueCell.getClass())) {
                    throw new IllegalArgumentException("A " + valueCell.getClass().getSimpleName()
                                                       + " is not compatible with " + v.getSimpleName());
                }
                m_adapterMap.put(v, valueCell);
            }
        }
        if (predefinedAdapters != null) {
            m_adapterMap.putAll(predefinedAdapters.getAdapterMap());
        }
    }

    /**
     * Creates a new adapter cell that contains a single data cell. This value classes that this data cell can represent
     * are given in the second argument. The adapter cell is then adaptable to all this values.
     *
     * @param valueCell any data cell
     * @param valueClasses the value classes that this adapter cell should be adaptable to
     */
    public AdapterCell(final DataCell valueCell, final Class<? extends DataValue>... valueClasses) {
        this(valueCell, null, valueClasses);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <V extends DataValue> V getAdapter(final Class<V> valueClass) {
        if (valueClass.isAssignableFrom(getClass())) {
            return valueClass.cast(this);
        }
        DataCell c = lookupFromAdapterMap(valueClass);
        if (c.isMissing()) {
            return null;
        }
        return valueClass.cast(c);
    }

    /** Allows sub classes to do a lookup in the internal adapter map. Used by derived classes to get a representation
     * that is not only an adapter by also implemented by the class.
     * @param valueClass The class to lookup.
     * @param <V> ...
     * @return The non-null value (might be missing!)
     * @throws IllegalArgumentException If no adapter is in the map.
     */
    protected final <V> DataCell lookupFromAdapterMap(final Class<V> valueClass) {
        DataCell c = m_adapterMap.get(valueClass);
        if (c == null) {
            throw new IllegalArgumentException("No adapter for " + valueClass);
        }
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public <V extends DataValue> boolean isAdaptable(final Class<V> valueClass) {
        return valueClass.isAssignableFrom(getClass()) || m_adapterMap.containsKey(valueClass);
    }

    /** {@inheritDoc} */
    @Override
    public AdapterCell cloneAndAddAdapter(final DataCell valueCell, final Class<? extends DataValue>... valueClasses) {
        if (m_adapterMap.size() >= MAX_ADAPTERS) {
            throw new IllegalArgumentException("At most " + MAX_ADAPTERS + " adapters are supported per cell");
        }
        AdapterCell clone = clone();
        clone.m_adapterMap = new BlobWrapperHashMap(m_adapterMap);
        for (Class<? extends DataValue> v : valueClasses) {
            if (!valueCell.isMissing() && !v.isAssignableFrom(valueCell.getClass())) {
                throw new IllegalArgumentException("A " + valueCell.getClass().getSimpleName()
                        + " is not compatible with" + v.getSimpleName());
            }
            clone.m_adapterMap.put(v, valueCell);
        }
        return clone;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        assert (m_adapterMap.size() > 0) : "Empty adapter not allowed";
        StringBuilder buf = new StringBuilder();
        for (Class<? extends DataValue> cl : m_adapterMap.keySet()) {
            buf.append(cl.getSimpleName()).append(", ");
        }
        buf.delete(buf.length() - 2, buf.length());

        return "Adapter for " + buf.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected AdapterCell clone() {
        try {
            return (AdapterCell)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Unexpected state as class implements Cloneable");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V extends DataValue> MissingValue getAdapterError(final Class<V> valueClass) {
        DataCell c = lookupFromAdapterMap(valueClass);
        if (c instanceof MissingValue) {
            return (MissingValue)c;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Class<? extends DataValue>, DataCell> getAdapterMap() {
        return Collections.unmodifiableMap(m_adapterMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsBlobWrapperCells() {
        for (DataCell c : m_adapterMap.values()) {
            if (c instanceof BlobWrapperDataCell) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<DataCell> iterator() {
        Iterator iterator = m_adapterMap.entrySet().iterator();
        return new DefaultBlobSupportDataCellIterator(iterator);
    }

    private static final class BlobWrapperHashMap extends LinkedHashMap<Class<? extends DataValue>, DataCell> {

        BlobWrapperHashMap() {
            super(4);
        }

        BlobWrapperHashMap(final BlobWrapperHashMap m) {
            super(m);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell get(final Object key) {
            DataCell c = super.get(key);
            if (c instanceof BlobWrapperDataCell) {
                return ((BlobWrapperDataCell)c).getCell();
            }
            return c;
        }

        DataCell getUnwrapped(final Object key) {
            return super.get(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell put(final Class<? extends DataValue> key, final DataCell c) {
            DataCell wrapperCell;
            if (c instanceof BlobWrapperDataCell) {
                wrapperCell = unwrapIfNeeded((BlobWrapperDataCell)c);
            } else if (c instanceof BlobDataCell) {
                wrapperCell = wrapIfNeeded((BlobDataCell)c);
            } else {
                wrapperCell = c;
            }
            return super.put(key, wrapperCell);
        }

        /*
         * The following three methods are required to avoid creation of BlobWrapperDataCells in backends other than the default backend.
         */
        private static DataCell wrapIfNeeded(final BlobDataCell c) {
            return isLegacyBackend() ? new BlobWrapperDataCell(c) : c;
        }

        private static DataCell unwrapIfNeeded(final BlobWrapperDataCell c) {
            return isLegacyBackend() ? c : c.getCell();
        }

        private static boolean isLegacyBackend() {
            return WorkflowTableBackendSettings.getTableBackendForCurrentContext() instanceof BufferedTableBackend;
        }
    }

}
