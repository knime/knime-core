/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   14 Oct 2021 (Steffen Fissler, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2.value;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Set;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.BoundedValue;
import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.FuzzyNumberValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.collection.SetDataValue;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.collection.SparseListDataValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.model.PortObjectCell;
import org.knime.core.data.model.PortObjectValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.data.vector.stringvector.StringVectorValue;
import org.knime.core.node.port.PortObject;

/**
 * The read and write values defined by the interfaces in this utility class are equivalent to the corresponding
 * ListCells (or might be seen as gateways to them). <br>
 * This class contains all interfaces for reading and writing values of the following: <br>
 * BooleanList <br>
 * BooleanSet <br>
 * BooleanSparseList <br>
 * Boolean <br>
 * DoubleList <br>
 * DoubleSet <br>
 * DoubleSparseList <br>
 * Double <br>
 * IntList <br>
 * IntSet <br>
 * IntSparseList <br>
 * Int <br>
 * List <br>
 * LongList <br>
 * LongSet <br>
 * LongSparceList <br>
 * Long <br>
 * PortObject <br>
 * Set <br>
 * SparseList <br>
 * StringList <br>
 * StringSet <br>
 * StringSparcelist <br>
 * String
 * <p>
 *
 * @author Steffen Fissler, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ValueInterfaces {

    private ValueInterfaces() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link BooleanCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface BooleanListReadValue extends ListReadValue {

        /**
         * @param index the index in the list
         * @return the boolean value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        boolean getBoolean(int index);

        /**
         * @return the list as a boolean array
         * @throws IllegalStateException if the value at one index is missing
         */
        boolean[] getBooleanArray();

        /**
         * @return an iterator over the boolean list
         * @throws IllegalStateException if the value at one index is missing
         */
        Iterator<Boolean> booleanIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link BooleanCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface BooleanListWriteValue extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of boolean values
         */
        void setValue(boolean[] values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link BooleanCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface BooleanSetReadValue extends SetReadValue {

        /**
         * @param value a boolean value
         * @return true if the set contains the value
         */
        boolean contains(boolean value);

        /**
         * @return a {@link Set} containing the {@link Boolean} values
         */
        Set<Boolean> getBooleanSet();

        /**
         * @return an iterator of the boolean set
         * @throws IllegalStateException if the set contains a missing value
         */
        Iterator<Boolean> booleanIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link BooleanCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface BooleanSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of boolean values
         */
        void setBooleanCollectionValue(Collection<Boolean> values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link BooleanCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface BooleanSparseListReadValue extends SparseListReadValue, BooleanListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link BooleanCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface BooleanSparseListWriteValue extends SparseListWriteValue {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(boolean[] values, boolean defaultElement);
    }

    /**
     * {@link ReadValue} equivalent to {@link BooleanCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface BooleanReadValue extends //
        BooleanValue, //
        DoubleValue, //
        BoundedValue, //
        LongValue, //
        IntValue, //
        NominalValue, //
        ComplexNumberValue, //
        FuzzyNumberValue, //
        FuzzyIntervalValue, //
        ReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link BooleanCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface BooleanWriteValue extends WriteValue<BooleanValue> {

        /**
         * @param value the boolean to set.
         */
        void setBooleanValue(boolean value);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link DoubleCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface DoubleListReadValue extends ListReadValue, DoubleVectorValue {

        /**
         * @param index the index in the list
         * @return the double value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        double getDouble(int index);

        /**
         * @return the list as a double array
         * @throws IllegalStateException if the value at one index is missing
         */
        double[] getDoubleArray();

        /**
         * @return an iterator over the double list
         * @throws IllegalStateException if the value at one index is missing
         */
        PrimitiveIterator.OfDouble doubleIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link DoubleCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface DoubleListWriteValue extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of double values
         */
        void setValue(double[] values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link DoubleCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface DoubleSetReadValue extends SetReadValue {

        /**
         * @param value a double value
         * @return true if the set contains the value
         */
        boolean contains(double value);

        /**
         * @return a {@link Set} containing the {@link Double} values
         */
        Set<Double> getDoubleSet();

        /**
         * @return an iterator of the double set
         * @throws IllegalStateException if the set contains a missing value
         */
        PrimitiveIterator.OfDouble doubleIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link DoubleCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface DoubleSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of double values
         */
        void setDoubleCollectionValue(Collection<Double> values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link DoubleCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface DoubleSparseListReadValue extends SparseListReadValue, DoubleListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link DoubleCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface DoubleSparseListWriteValue extends SparseListWriteValue {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(double[] values, double defaultElement);
    }

    /**
     * {@link ReadValue} equivalent to {@link DoubleCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface DoubleReadValue extends //
        DoubleValue, //
        BoundedValue, //
        ReadValue, //
        ComplexNumberValue, //
        FuzzyNumberValue, //
        FuzzyIntervalValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link DoubleCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface DoubleWriteValue extends WriteValue<DoubleValue> {

        /**
         * @param value the double to set
         */
        void setDoubleValue(double value);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link IntCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface IntListReadValue extends ListReadValue {

        /**
         * @param index the index in the list
         * @return the integer value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        int getInt(int index);

        /**
         * @return the list as a integer array
         * @throws IllegalStateException if the value at one index is missing
         */
        int[] getIntArray();

        /**
         * @return an iterator over the integer list
         * @throws IllegalStateException if the value at one index is missing
         */
        PrimitiveIterator.OfInt intIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link IntCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface IntListWriteValue extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of int values
         */
        void setValue(int[] values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link IntCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface IntSetReadValue extends SetReadValue {

        /**
         * @param value a double value
         * @return true if the set contains the value
         */
        boolean contains(int value);

        /**
         * @return a {@link Set} containing the {@link Integer} values
         */
        Set<Integer> getIntSet();

        /**
         * @return an iterator of the double set
         * @throws IllegalStateException if the set contains a missing value
         */
        PrimitiveIterator.OfInt intIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link IntCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface IntSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of double values
         */
        void setIntCollectionValue(Collection<Integer> values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link IntCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface IntSparseListReadValue extends SparseListReadValue, IntListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link IntCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface IntSparseListWriteValue extends SparseListWriteValue {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(int[] values, int defaultElement);
    }

    /**
     * {@link ReadValue} equivalent to {@link IntCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface IntReadValue extends //
        ReadValue, //
        IntValue, //
        DoubleValue, //
        ComplexNumberValue, //
        FuzzyNumberValue, //
        FuzzyIntervalValue, //
        BoundedValue, //
        LongValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link IntCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface IntWriteValue extends WriteValue<IntValue> {

        /**
         * @param value the int value to set
         */
        void setIntValue(int value);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface ListReadValue extends ReadValue, ListDataValue {

        /**
         * @param index the index in the list
         * @return if the value at this index is missing
         */
        boolean isMissing(int index);
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface ListWriteValue extends WriteValue<ListDataValue> {

        /**
         * @param values the values to set
         */
        void setValue(List<DataValue> values);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link LongCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface LongListReadValue extends ListReadValue {

        /**
         * @param index the index in the list
         * @return the long value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        long getLong(int index);

        /**
         * @return the list as a long array
         * @throws IllegalStateException if the value at one index is missing
         */
        long[] getLongArray();

        /**
         * @return an iterator over the long list
         * @throws IllegalStateException if the value at one index is missing
         */
        PrimitiveIterator.OfLong longIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link LongCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface LongListWriteValue extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of int values
         */
        void setValue(long[] values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link LongCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface LongSetReadValue extends SetReadValue {

        /**
         * @param value a long value
         * @return true if the set contains the value
         */
        boolean contains(long value);

        /**
         * @return a {@link Set} containing the {@link Long} values
         */
        Set<Long> getLongSet();

        /**
         * @return an iterator of the long set
         * @throws IllegalStateException if the set contains a missing value
         */
        PrimitiveIterator.OfLong longIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link LongCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface LongSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of long values
         */
        void setLongCollectionValue(Collection<Long> values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link LongCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface LongSparseListReadValue extends SparseListReadValue, LongListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link LongCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface LongSparseListWriteValue extends SparseListWriteValue {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(long[] values, long defaultElement);
    }

    /**
     * {@link ReadValue} equivalent to {@link LongCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface LongReadValue extends //
        LongValue, //
        DoubleValue, //
        BoundedValue, //
        ReadValue, //
        ComplexNumberValue, //
        FuzzyNumberValue, //
        FuzzyIntervalValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link LongCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface LongWriteValue extends WriteValue<LongValue> {

        /**
         * @param value the long value to set
         */
        void setLongValue(long value);
    }

    /**
     * {@link ReadValue} equivalent to {@link PortObjectCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface PortObjectReadValue extends ReadValue, PortObjectValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link PortObjectCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface PortObjectWriteValue extends WriteValue<PortObjectValue> {

        /**
         * @param portObject the {@link PortObject} to set
         */
        void setPortObject(PortObject portObject);
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface SetReadValue extends ReadValue, SetDataValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface SetWriteValue extends WriteValue<SetDataValue> {

        /**
         * @param values the values to set
         */
        void setValue(Collection<DataValue> values);
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface SparseListReadValue extends ReadValue, SparseListDataValue {

        /**
         * @param index the index in the list
         * @return if the value at this index is missing
         */
        boolean isMissing(int index);
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell}.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface SparseListWriteValue extends WriteValue<SparseListDataValue> {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(List<DataValue> values, DataValue defaultElement);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link StringCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface StringListReadValue extends ListReadValue, StringVectorValue {

        /**
         * @param index at which to obtain the returned String
         * @return the String at <b>index</b>
         */
        String getString(int index);

        /**
         *
         * @return the content of the list as array
         */
        String[] getStringArray();

        /**
         * @return an {@link Iterator} over the Strings in the list
         */
        Iterator<String> stringIterator();

    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link StringCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface StringListWriteValue extends ListWriteValue {

        /**
         * @param values to set
         */
        void setValue(String[] values);

    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with elements of type T.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface StringSetReadValue extends SetReadValue {

        /**
         * @param value an object value
         * @return true if the set contains the value
         */
        boolean contains(String value);

        /**
         * @return a {@link Set} containing the object values
         */
        Set<String> getStringSet();

        /**
         * @return an iterator of the object set
         * @throws IllegalStateException if the set contains a missing value
         */
        Iterator<String> stringIterator();

    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with elements of type T.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface StringSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of String values
         */
        void setStringCollectionValue(Collection<String> values);

    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link StringCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface StringSparseListReadValue extends SparseListReadValue, StringListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link StringCell} elements.
     *
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static interface StringSparseListWriteValue extends SparseListWriteValue {

        /**
         * Set the value.
         *
         * @param values
         * @param defaultElement
         */
        void setValue(String[] values, String defaultElement);

    }

    /**
     * {@link ReadValue} equivalent to {@link StringCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface StringReadValue extends //
        StringValue, //
        NominalValue, //
        ReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link StringCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.5
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This class is not intended to be referenced by clients.
     */
    public interface StringWriteValue extends WriteValue<StringValue> {
        /**
         * @param value the string value to set
         */
        void setStringValue(String value);
    }

}
