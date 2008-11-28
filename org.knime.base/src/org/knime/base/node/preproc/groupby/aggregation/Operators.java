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
 *    18.07.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.aggregation;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.MutableInteger;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


/**
 * Contains all nominal aggregation operators.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public final class Operators {

    private static Operators instance;

    private Operators() {
        //avoid object creation
    }

    /**
     * Returns the only instance of this class.
     * @return the only instance
     */
    public static Operators getInstance() {
        if (instance == null) {
            instance = new Operators();
        }
        return instance;
    }



    /**
     * Returns the first element per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    class FirstOperator extends AggregationOperator {

        private DataCell m_firstCell = null;

        /**Constructor for class MinOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        FirstOperator(final int maxUniqueValues) {
            super("First", false, false, true, maxUniqueValues);
        }

        /**Constructor for class FirstOperator.
         * @param label user readable label
         * @param numerical <code>true</code> if the operator is only suitable
         * for numerical columns
         * @param usesLimit <code>true</code> if the method checks the number of
         * unique values limit.
         * @param keepColSpec <code>true</code> if the original column
         * specification should be kept if possible
         * @param maxUniqueValues the maximum number of unique values
         */
        public FirstOperator(final String label, final boolean numerical,
                final boolean usesLimit, final boolean keepColSpec,
                final int maxUniqueValues) {
            super(label, numerical, usesLimit, keepColSpec, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return origType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new FirstOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (m_firstCell == null) {
                m_firstCell = cell;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (m_firstCell == null) {
                return DataType.getMissingCell();
            }
            return m_firstCell;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_firstCell = null;
        }
    }

    /**
     * Returns the first value (ignores missing values) per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class FirstValueOperator extends FirstOperator {

        /**Constructor for class FirstValueOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        FirstValueOperator(final int maxUniqueValues) {
            super("First value", false, false, true, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new FirstValueOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            return super.computeInternal(cell);
        }
    }

    /**
     * Returns the last element per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    class LastOperator extends AggregationOperator {

        private DataCell m_lastCell = null;

        /**Constructor for class MinOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        LastOperator(final int maxUniqueValues) {
            super("Last", false, false, true, maxUniqueValues);
        }

        /**Constructor for class LastOperator.
         * @param label user readable label
         * @param numerical <code>true</code> if the operator is only suitable
         * for numerical columns
         * @param usesLimit <code>true</code> if the method checks the number of
         * unique values limit.
         * @param keepColSpec <code>true</code> if the original column
         * specification should be kept if possible
         * @param maxUniqueValues the maximum number of unique values
         */
        public LastOperator(final String label, final boolean numerical,
                final boolean usesLimit, final boolean keepColSpec,
                final int maxUniqueValues) {
            super(label, numerical,  usesLimit, keepColSpec, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return origType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new LastOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            m_lastCell = cell;
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (m_lastCell == null) {
                return DataType.getMissingCell();
            }
            return m_lastCell;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_lastCell = null;
        }
    }

    /**
     * Returns the last value (ignores missing values) per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    class LastValueOperator extends LastOperator {
        /**Constructor for class LastValueOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        LastValueOperator(final int maxUniqueValues) {
            super("Last value", false, false, true, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new LastValueOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            return super.computeInternal(cell);
        }
    }

    /**
     * Returns the most frequent entry per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class ModeOperator extends AggregationOperator {

        private final Map<DataCell, MutableInteger> m_valCounter;

        /**Constructor for class MinOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        ModeOperator(final int maxUniqueValues) {
            super("Mode", false, true, true, maxUniqueValues);
            try {
                m_valCounter =
                    new LinkedHashMap<DataCell, MutableInteger>(
                            maxUniqueValues);
            } catch (final OutOfMemoryError e) {
                throw new IllegalArgumentException(
                        "Maximum unique values number to big");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return origType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new ModeOperator(maxUniqueValues);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            MutableInteger counter = m_valCounter.get(cell);
            if (counter == null) {
                //check if the maps contains more values than allowed
                //before adding a new value
                if (m_valCounter.size() >= getMaxUniqueValues()) {
                    return true;
                }
                counter = new MutableInteger(0);
                m_valCounter.put(cell, counter);
            }
            counter.inc();
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (m_valCounter.size() < 1) {
                return DataType.getMissingCell();
            }
            //get the cell with the most counts
            final Set<Entry<DataCell, MutableInteger>> entries =
                m_valCounter.entrySet();
            int max = Integer.MIN_VALUE;
            DataCell result = null;
            for (final Entry<DataCell, MutableInteger> entry : entries) {
                if (entry.getValue().intValue() > max) {
                    max = entry.getValue().intValue();
                    result = entry.getKey();
                }
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_valCounter.clear();
        }
    }

    /**
     * Returns the all values concatenated per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class ConcatenateOperator extends AggregationOperator {

        private final DataType m_type = StringCell.TYPE;

        private final StringBuilder m_buf = new StringBuilder();

        private boolean m_first = true;

        /**Constructor for class Concatenate.
         * @param maxUniqueValues the maximum number of unique values
         */
        public ConcatenateOperator(final int maxUniqueValues) {
            super("Concatenate", false, false, false, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new ConcatenateOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            if (m_first) {
                m_first = false;
            } else {
                m_buf.append(AggregationOperator.CONCATENATOR);
            }
            m_buf.append(cell.toString());
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            return new StringCell(m_buf.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_buf.setLength(0);
            m_first = true;
        }
    }

    /**
     * Returns the concatenation of all different values per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class UniqueConcatenateOperator extends AggregationOperator {

        private final DataType m_type = StringCell.TYPE;

        private final Set<String> m_vals;

        private final StringBuilder m_buf = new StringBuilder();

        private boolean m_first = true;

        /**Constructor for class Concatenate.
         * @param maxUniqueValues the maximum number of unique values
         */
        public UniqueConcatenateOperator(final int maxUniqueValues) {
            super("Unique concatenate", false, true, false, maxUniqueValues);
            try {
                m_vals = new HashSet<String>(maxUniqueValues);
            } catch (final OutOfMemoryError e) {
                throw new IllegalArgumentException(
                        "Maximum unique values number to big");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new UniqueConcatenateOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            final String val = cell.toString();
            if (m_vals.contains(val)) {
                return false;
            }
            //check if the set contains more values than allowed
            //before adding a new value
            if (m_vals.size() >= getMaxUniqueValues()) {
                return true;
            }
            m_vals.add(val);
            if (m_first) {
                m_first = false;
            } else {
                m_buf.append(AggregationOperator.CONCATENATOR);
            }
            m_buf.append(val);
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            return new StringCell(m_buf.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_buf.setLength(0);
            m_first = true;
            m_vals.clear();
        }
    }

    /**
     * Returns the count of the unique values per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class UniqueCountOperator extends AggregationOperator {

        private final DataType m_type = IntCell.TYPE;

        private final Set<String> m_vals;

        /**Constructor for class Concatenate.
         * @param maxUniqueValues the maximum number of unique values
         */
        public UniqueCountOperator(final int maxUniqueValues) {
            super("Unique count", false, true, false, maxUniqueValues);
            try {
                m_vals = new HashSet<String>(maxUniqueValues);
            } catch (final OutOfMemoryError e) {
                throw new IllegalArgumentException(
                        "Maximum unique values number to big");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new UniqueCountOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            final String val = cell.toString();
            if (m_vals.contains(val)) {
                return false;
            }
            //check if the set contains more values than allowed
            //before adding a new value
            if (m_vals.size() >= getMaxUniqueValues()) {
                return true;
            }
            m_vals.add(val);
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            return new IntCell(m_vals.size());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_vals.clear();
        }
    }

    /**
     * Returns the count per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class CountOperator extends AggregationOperator {

        private final DataType m_type = IntCell.TYPE;

        private int m_counter = 0;

        /**Constructor for class CountOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        CountOperator(final int maxUniqueValues) {
            super("Count", false, false, false, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new CountOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            m_counter++;
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            return new IntCell(m_counter);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_counter = 0;
        }
    }

    /**
     * Returns all values as a {@link ListCell} per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class ListCellOperator extends AggregationOperator {

        private final Collection<DataCell> m_cells;

        /**Constructor for class Concatenate.
         * @param maxUniqueValues the maximum number of unique values
         */
        public ListCellOperator(final int maxUniqueValues) {
            super("List", false, false, false, maxUniqueValues);
            m_cells = new LinkedList<DataCell>();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return ListCell.getCollectionType(origType);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new ListCellOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (m_cells.size() >= getMaxUniqueValues()) {
                return true;
            }
            m_cells.add(cell);
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            return CollectionCellFactory.createListCell(m_cells);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_cells.clear();
        }
    }

    /**
     * Returns all values as a {@link SetCell} per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class SetCellOperator extends AggregationOperator {

        private final Set<DataCell> m_cells;

        /**Constructor for class Concatenate.
         * @param maxUniqueValues the maximum number of unique values
         */
        public SetCellOperator(final int maxUniqueValues) {
            super("Set", false, true, false, maxUniqueValues);
            try {
                m_cells = new HashSet<DataCell>(maxUniqueValues);
            } catch (final OutOfMemoryError e) {
                throw new IllegalArgumentException(
                        "Maximum unique values number to big");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return SetCell.getCollectionType(origType);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new SetCellOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (m_cells.size() >= getMaxUniqueValues()) {
                return true;
            }
            m_cells.add(cell);
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            return CollectionCellFactory.createSetCell(m_cells);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_cells.clear();
        }
    }
}
