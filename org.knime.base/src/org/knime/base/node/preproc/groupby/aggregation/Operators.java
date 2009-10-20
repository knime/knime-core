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
import java.util.HashMap;
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
     * Returns the concatenation of all different values per group and the
     * number of cells per distinct value.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class UniqueConcatenateWithCountOperator extends AggregationOperator {

        private final DataType m_type = StringCell.TYPE;

        private final Map<String, MutableInteger> m_vals;

        /**Constructor for class Concatenate.
         * @param maxUniqueValues the maximum number of unique values
         */
        public UniqueConcatenateWithCountOperator(final int maxUniqueValues) {
            super("Unique concatenate with count", false, true, false,
                    maxUniqueValues);
            try {
                m_vals = new HashMap<String, MutableInteger>(maxUniqueValues);
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
            return new UniqueConcatenateWithCountOperator(maxUniqueValues);
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
            final MutableInteger counter = m_vals.get(val);
            if (counter != null) {
                counter.inc();
                return false;
            }
            //check if the map contains more values than allowed
            //before adding a new value
            if (m_vals.size() >= getMaxUniqueValues()) {
                return true;
            }
            m_vals.put(val, new MutableInteger(1));
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            final StringBuilder buf = new StringBuilder();
            final Set<Entry<String, MutableInteger>> entrySet =
                m_vals.entrySet();
            boolean first = true;
            for (final Entry<String, MutableInteger> entry : entrySet) {
                if (first) {
                    first = false;
                } else {
                    buf.append(AggregationOperator.CONCATENATOR);
                }
                buf.append(entry.getKey());
                buf.append('(');
                buf.append(Integer.toString(entry.getValue().intValue()));
                buf.append(')');
            }
            return new StringCell(buf.toString());
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
