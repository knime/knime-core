/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   31.07.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.knime.core.data.DataCell;

/**
 * Implements a table that holds {@link DataRowWeighted}s in memory.
 * Additionally, this class maintains distribution information about the class
 * values and possible values of nominal attributes.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class InMemoryTable implements Iterable<DataRowWeighted> {

    /**
     * The initial size of the data row array.
     */
    private static final int INITIAL_ROWVECTOR_SIZE = 2000;

    /**
     * The weighted data rows of this table.
     */
    private DataRowWeighted[] m_rows;

    /**
     * The size of the row array.
     */
    private int m_size;

    /**
     * The value mapper for nominal attribute values. The index positions for
     * numeric values are left <code>null</code>.
     */
    private ValueMapper<DataCell>[] m_nominalAttributeValueMapper;

    /**
     * Keeps the attribute value - class frequencies as historgrams. Updated if
     * a new row is added. First dimension are the attributes
     */
    private NominalValueHistogram[] m_nominalValueClassFrequencyHisto;

    /**
     * For performance reasons this array holds the indices of the nominal
     * attributes. Thus, it is faster to access only the nominal attributes.
     */
    private int[] m_nominalAttributeIndices;

    /**
     * Remembers for each attribute if it should be considered during learning.
     * (E.g. after splitting a nominal attribute at each value, further splits
     * on this attribute are not possible in deeper levels)
     */
    private boolean[] m_considerAttribute;

    /**
     * The value mapper for the class attribute values. They are mapped to an
     * int value.
     */
    private ValueMapper<DataCell> m_classValueMapper;

    /**
     * The mapper for the attribute indices.
     */
    private ValueMapper<String> m_attributeNameMapper;

    /**
     * Keeps the class frequencies. Updated if a new row is added.
     */
    private double[] m_classFrequencyArray;

    /**
     * Used to determine if this data is pure enought according to its class
     * value distribution.
     */
    private double m_minNumberRowsPerNode;

    /**
     * Holds the sum of all weights of the rows.
     */
    private double m_sumOfWeights;

    /**
     * Creates an empty table that keeps all rows in memory. The
     * {@link ValueMapper} array must contain mappers only at array positions
     * where a nominal attribute is available. Numeric attributes do not need a
     * mapper. These array positions must contain <code>null</code>.
     *
     * @param nominalAttributeValueMapper the value mapper for the nominal
     *            attributes; the array must only contain mappers at positions
     *            where nominal values are available
     * @param classValueMapper the value mapper for the class attribute values
     *            that are also stored in a integer mapped manner
     * @param attributeNameMapper the value mapper for the attribute names
     * @param minNumberRowsPerNode the minimum number of nodes per leaf; used to
     *            determine whether this tables distribution of class values is
     *            pure enough
     */
    public InMemoryTable(
            final ValueMapper<DataCell>[] nominalAttributeValueMapper,
            final ValueMapper<DataCell> classValueMapper,
            final ValueMapper<String> attributeNameMapper,
            final double minNumberRowsPerNode) {
        m_nominalAttributeValueMapper = nominalAttributeValueMapper;
        m_classValueMapper = classValueMapper;
        m_attributeNameMapper = attributeNameMapper;
        m_minNumberRowsPerNode = minNumberRowsPerNode;
        // the initial size of the class frequency counter array is
        // 1 and will be increased if more class values are detected
        m_classFrequencyArray = new double[1];
        m_rows = new DataRowWeighted[INITIAL_ROWVECTOR_SIZE];
        m_size = 0;
        // create the historgram array for the nominal attributes
        m_nominalValueClassFrequencyHisto =
                new NominalValueHistogram[nominalAttributeValueMapper.length];
        int numNominalAttributes = 0;
        for (int i = 0; i < m_nominalValueClassFrequencyHisto.length; i++) {
            // only for nominal attributes
            if (nominalAttributeValueMapper[i] != null) {
                m_nominalValueClassFrequencyHisto[i] =
                        new NominalValueHistogram();
                numNominalAttributes++;
            }
        }
        // create the nominal attribute indices array
        m_nominalAttributeIndices = new int[numNominalAttributes];
        int k = 0;
        for (int i = 0; i < getNumAttributes(); i++) {
            if (isNominal(i)) {
                m_nominalAttributeIndices[k] = i;
                k++;
            }
        }
        // initialize the boolean array remembering whether an attribute
        // should be considered during learning
        // initially all attributes are condiered (i.e. true)
        m_considerAttribute = new boolean[attributeNameMapper.getNumMappings()];
        for (int i = 0; i < m_considerAttribute.length; i++) {
            m_considerAttribute[i] = true;
        }
    }

    /**
     * Creates an empty table from a given template table. The new empty table
     * receives the mappers, the remarks whether to consider certain
     * attributes, the minimum number rows per tree node and the nominal
     * attribute indices array.
     *
     * @param tableTemplate the table that is used as a template to create this
     *            new table
     */
    public InMemoryTable(final InMemoryTable tableTemplate) {
        m_nominalAttributeValueMapper =
                tableTemplate.m_nominalAttributeValueMapper;
        m_classValueMapper = tableTemplate.m_classValueMapper;
        m_attributeNameMapper = tableTemplate.m_attributeNameMapper;
        m_minNumberRowsPerNode = tableTemplate.m_minNumberRowsPerNode;
        // the initial size of the class frequency counter array is
        // the same as in the template but the values are initialized with 0
        m_classFrequencyArray =
                new double[tableTemplate.m_classFrequencyArray.length];
        m_rows = new DataRowWeighted[INITIAL_ROWVECTOR_SIZE];
        m_size = 0;
        // create the histogram array for the nominal attributes
        m_nominalValueClassFrequencyHisto =
                new NominalValueHistogram[m_nominalAttributeValueMapper.length];
        for (int i = 0; i < m_nominalValueClassFrequencyHisto.length; i++) {
            // only for nominal attributes
            if (m_nominalAttributeValueMapper[i] != null) {
                m_nominalValueClassFrequencyHisto[i] =
                        new NominalValueHistogram(
                            tableTemplate.m_nominalValueClassFrequencyHisto[i]);
            }
        }
        // create the nominal attribute indices array
        m_nominalAttributeIndices = tableTemplate.m_nominalAttributeIndices;

        // initialize the boolean array remembering whether an attribute
        // should be considered during learning
        m_considerAttribute = tableTemplate.m_considerAttribute;
    }

    /**
     * Returns true if the given attribute should be considered during learning,
     * false if not.
     *
     * @param attributeIndex the index of the attribute to get the considering
     *            information for
     *
     * @return true if the given attribute should be considered during learning,
     *         false if not
     */
    public boolean considerAttribute(final int attributeIndex) {
        return m_considerAttribute[attributeIndex];
    }

    /**
     * To set if an attribute should be considered during learning or not. NOTE:
     * this is just a hint for the algorithm (i.e. just a flag).
     *
     * @param attributeIndex the index of the attribute to set the considering
     *            information for
     * @param consider true - the attribute should be considered during
     *            learning, false - the attribute should not be considered
     */
    public void setConsiderAttribute(final int attributeIndex,
            final boolean consider) {
        m_considerAttribute[attributeIndex] = consider;
    }

    /**
     * Returns the name of the attribute specified by the given index.
     *
     * @param index the index of the attribute to get the name for
     * @return the name of the specified attribute
     */
    public String getAttributeName(final int index) {
        return m_attributeNameMapper.getMappedObject(index).toString();
    }

    /**
     * Whether the attribute at the given index position is nominal or not.
     *
     * @param index the attribute index position
     * @return true if the attribute at the given index position is nominal,
     *         false otherwise (i.e. the attribute is numeric)
     */
    public boolean isNominal(final int index) {
        return m_nominalAttributeValueMapper[index] != null;
    }

    /**
     * Frees the underlying data rows. Can be used to reduce the memory
     * requirements in case the data itself is not needed any more.
     */
    public void freeUnderlyingDataRows() {
        m_rows = null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataRowWeighted> iterator() {
        if (m_rows == null) {
            throw new RuntimeException("Data rows have been removed.");
        }
        return new InMemoryTableIterator();
    }

    private class InMemoryTableIterator implements Iterator<DataRowWeighted> {

        private int m_next = 0;

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return m_next < m_rows.length;
        }

        /**
         * {@inheritDoc}
         */
        public DataRowWeighted next() {
            return m_rows[m_next++];
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported.");
        }
    }

    /**
     * Adds a {@link DataRowWeighted}.
     *
     * @param row the row to add
     */
    public void addRow(final DataRowWeighted row) {

        // TODO:debug code
        if (row.getNumAttributes() != m_nominalAttributeValueMapper.length) {
            throw new RuntimeException(
                    "A data row must have the same length as the mapper array");
        }

        int classValue = row.getClassValue();
        // increment the class frequency counter
        // first check if the array is large enough
        if (classValue >= m_classFrequencyArray.length) {
            // if not, enlarge the array and copy the old values
            double[] enlargedArray = new double[classValue + 1];
            System.arraycopy(m_classFrequencyArray, 0, enlargedArray, 0,
                    m_classFrequencyArray.length);
            m_classFrequencyArray = enlargedArray;
        }
        double weight = row.getWeight();
        m_classFrequencyArray[classValue] += weight;
        m_sumOfWeights += weight;

        // count the nominal attribute values for the nominal histograms
        for (int index : m_nominalAttributeIndices) {
            m_nominalValueClassFrequencyHisto[index].increment((int)row
                    .getValue(index), row.getClassValue(), weight);

        }

        // add the row, before this, check the capacity of the array
        ensureCapacity(m_size + 1);
        m_rows[m_size] = row;
        m_size++;
    }

    /**
     * Returns the frequency of the majoriy class.
     *
     * @return the frequency of the majoriy class.
     */
    public double getMajorityClassCount() {
        double maxFrequency = 0.0;
        for (double frequency : m_classFrequencyArray) {
            if (maxFrequency < frequency) {
                maxFrequency = frequency;
            }
        }
        return maxFrequency;
    }

    /**
     * Returns the mapping value of the majority class.
     *
     * @return the mapping value of the majority class
     */
    public int getMajorityClass() {
        double maxFrequency = -1.0;
        int majorityClassMappingValue = -1;
        int i = 0;
        for (double frequency : m_classFrequencyArray) {
            if (maxFrequency < frequency) {
                maxFrequency = frequency;
                majorityClassMappingValue = i;
            }
            i++;
        }
        return majorityClassMappingValue;
    }

    /**
     * Returns the majority class value as {@link DataCell}.
     *
     * @return the majority class value as {@link DataCell}
     */
    public DataCell getMajorityClassAsCell() {
        return m_classValueMapper.getMappedObject(getMajorityClass());
    }

    /**
     * Determines if the data distribution (class value distribution) is pure
     * enough. The table is pure enough, if there are only rows of one class
     * value, or if the number of rows (sum of weights) is below twice the
     * threashold specified in the constructor.
     *
     * @return true, if the table is pure enough, false otherwise
     */
    public boolean isPureEnough() {
        if (getMajorityClassCount() == m_sumOfWeights
                || m_sumOfWeights <= 2 * m_minNumberRowsPerNode) {
            return true;
        }
        return false;
    }

    /**
     * Returns the class frequency array representing the class distribution of
     * this table.
     *
     * @return the class frequency array representing the class distribution of
     *         this table
     */
    public double[] getClassFrequencyArray() {
        return m_classFrequencyArray;
    }

    /**
     * Returns a copy of the class frequency array representing the class
     * distribution of this table. This is important if the returned array is
     * inteded to be manipulated!
     *
     * @return a copy of the class frequency array representing the class
     *         distribution of this table
     */
    public double[] getCopyOfClassFrequencyArray() {
        return Arrays.copyOf(m_classFrequencyArray,
                m_classFrequencyArray.length);
    }

    /**
     * Returns the class frequencies as a {@link LinkedHashMap}
     *
     * mapping class values ({@link DataCell}) to the frequency as doubles.
     *
     * @return the class frequencies as a {@link LinkedHashMap}
     *
     * mapping class values ({@link DataCell}) to the frequency as doubles
     */
    public LinkedHashMap<DataCell, Double> getClassFrequencies() {
        LinkedHashMap<DataCell, Double> resultMap = 
            new LinkedHashMap<DataCell, Double>();
        int i = 0;
        for (double frequency : m_classFrequencyArray) {
            resultMap.put(m_classValueMapper.getMappedObject(i), frequency);
            i++;
        }
        return resultMap;
    }

    /**
     * Returns the size of this table.
     *
     * @return the size of this table
     */
    public int getNumberDataRows() {
        return m_size;
    }

    /**
     * Returns the class value mapper of this table.
     *
     * @return the class value mapper of this table
     */
    public ValueMapper<DataCell> getClassValueMapper() {
        return m_classValueMapper;
    }

    /**
     * Returns the attribute value mapper of this table for the given attribute.
     *
     * @param attributeIndex the index for which to return the value mapper
     *
     * @return the attribute value mapper of this table for the given nominal
     *         attribute, <code>null</code> if the attribute is not nominal
     *         (i.e. numeric)
     */
    public ValueMapper<DataCell> getNominalAttributeValueMapper(
            final int attributeIndex) {
        return m_nominalAttributeValueMapper[attributeIndex];
    }

    /**
     * Returns the number of attributes (excluding the class attribute).
     *
     * @return the number of attributes (excluding the class attribute)
     */
    public int getNumAttributes() {
        return m_nominalAttributeValueMapper.length;
    }

    private void ensureCapacity(final int minCapacity) {
        int oldCapacity = m_rows.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            // minCapacity is usually close to size, so this is a win:
            m_rows = Arrays.copyOf(m_rows, newCapacity);
        }
    }

    /**
     * Sets the size of the underlying array to the number of elements in the
     * list.
     */
    public void pack() {
        int oldCapacity = m_rows.length;
        if (m_size < oldCapacity) {
            m_rows = Arrays.copyOf(m_rows, m_size);
        }
    }

    /**
     * Returns the sum of the weights of all rows.
     *
     * @return the sum of the weights of all rows
     */
    public double getSumOfWeights() {
        return m_sumOfWeights;
    }

    /**
     * Returns the number of nominal values for the given attribute.
     *
     * @param attributeIndex the nominal attribute index for which to get the
     *            number of nominal values
     * @return the number of nominal values for the given attribute; -1 if the
     *         attribute is not nominal
     */
    public int getNumNominalValues(final int attributeIndex) {
        ValueMapper<DataCell> mapper =
                m_nominalAttributeValueMapper[attributeIndex];
        if (mapper == null) {
            return -1;
        }
        return mapper.getNumMappings();
    }

    /**
     * Returns the value histogram for the given attribute index. If the
     * attribute is numeric, <code>null</code> is returned.
     *
     * @param attributeIndex the attribute index for which to return the
     *            histogram
     * @return the value histogram for the given attribute index; if the
     *         attribute is numeric, <code>null</code> is returned
     */
    public NominalValueHistogram getNominalValueHistogram(
            final int attributeIndex) {
        return m_nominalValueClassFrequencyHisto[attributeIndex];
    }

    /**
     * Returns the nominal values for the given attribute index. The value array
     * is ordered according to the integer mapping, i.e. the {@link DataCell}
     * mapped with integer 0 is placed first, and so on.
     *
     * @param attributeIndex the attribute index for which to return the nominal
     *            values; <code>null</code> if the attribute is not nomnial
     * @return the nominal values for the given attribute index. The value array
     *         is ordered according to the integer mapping
     */
    public DataCell[] getNominalValuesInMappingOrder(final int attributeIndex) {
        ValueMapper<DataCell> mapper =
                m_nominalAttributeValueMapper[attributeIndex];
        if (mapper == null) {
            return null;
        }
        Object[] values = mapper.getMappedObjectsInMappingOrder();
        DataCell[] asCells = new DataCell[values.length];
        for (int i = 0; i < values.length; i++) {
            asCells[i] = (DataCell)values[i];
        }
        return asCells;
    }

    /**
     * Sorts the data rows of this table in ascending order on the given
     * attribute index. The missing values are put at the end of the table.
     *
     * @param attributeIndex the index of the attribute on which to sort the
     *            data rows
     * @return the sum of weights of the missing value rows for each class
     *         value; corresponds to the class frequency array but only for the
     *         missing values
     */
    public double[] sortDataRows(final int attributeIndex) {
        // TODO:debug just for debug reasons, there is no problem in sorting
        // nominal attributes
        assert !isNominal(attributeIndex);

        double[] sumOfMissingValueWeights =
                new double[m_classFrequencyArray.length];

        // put all missing values (encoded as not a number - NaN) at the end
        // of the row array
        int left = 0;
        int right = m_size - 1;
        while (left < right) {
            if (Double.isNaN(m_rows[right].getValue(attributeIndex))) {
                sumOfMissingValueWeights[m_rows[right].getClassValue()] +=
                        m_rows[right].getWeight();
                right--;
            } else {
                if (Double.isNaN(m_rows[left].getValue(attributeIndex))) {
                    sumOfMissingValueWeights[m_rows[left].getClassValue()] +=
                            m_rows[left].getWeight();
                    DataRowWeighted temp = m_rows[left];
                    m_rows[left] = m_rows[right];
                    m_rows[right] = temp;
                    right--;
                }
                left++;
            }
        }

        // on the normal values perform quicksort
        quicksort(0, right, attributeIndex);

        return sumOfMissingValueWeights;
    }

    /**
     * Implements the basic quicksort algorithm.
     *
     * @param left the left index of the partition to sort
     * @param right the right index of the partition to sort
     * @param attributeIndex the attribute index to sort on
     */
    private void quicksort(final int left, final int right,
            final int attributeIndex) {
        if (left < right) {
            int midIndex = partition(left, right, attributeIndex);
            quicksort(left, midIndex, attributeIndex);
            quicksort(midIndex + 1, right, attributeIndex);
        }
    }

    /**
     * Partitions the instances according to a pivot element. The pivot element
     * is here the element in the middle of the data rows.
     *
     * @param left the left index of the partition
     * @param right the right index of the partition
     * @param attributeIndex the attribute index to compare on
     *
     * @return the index of the element in the middle
     */
    private int partition(int left, int right, final int attributeIndex) {
        double pivot = m_rows[(left + right) / 2].getValue(attributeIndex);
        while (left < right) {
            while ((m_rows[left].getValue(attributeIndex) < pivot)
                    && (left < right)) {
                left++;
            }
            while ((m_rows[right].getValue(attributeIndex) > pivot)
                    && (left < right)) {
                right--;
            }
            if (left < right) {
                DataRowWeighted temp = m_rows[left];
                m_rows[left] = m_rows[right];
                m_rows[right] = temp;
                left++;
                right--;
            }
        }
        if ((left == right)
                && (m_rows[right].getValue(attributeIndex) > pivot)) {
            right--;
        }
        return right;
    }
}
