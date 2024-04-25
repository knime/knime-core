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
 *   9 Nov 2022 (manuelhotz): created
 */
package org.knime.core.data;

import java.util.Comparator;

import org.knime.core.data.v2.RowRead;

/**
 * Strategies for treating missing values when comparing or sorting data cells.
 * <ul>
 *   <li>{@link #LEAST} treats missing values as smaller than non-missing values.</li>
 *   <li>{@link #GREATEST} treats missing values as larger than non-missing values.</li>
 * </ul>
 * Comparators for data cells that compare missing cells as smaller or larger than any non-missing cell.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public enum MissingValueHandling {

    /** Compare missing values as <b>smaller</b> than any non-missing value. Two missing values are treated as equal. */
    LEAST() {
        @Override
        public int compareMissings(final boolean leftMissing, final boolean rightMissing) {
            return !rightMissing ? -1 : (leftMissing ? 0 : 1); // NOSONAR
        }
    },

    /** Compare missing values as <b>larger</b> than any non-missing value. Two missing values are treated as equal. */
    GREATEST() {
        @Override
        int compareMissings(final boolean leftMissing, final boolean rightMissing) {
            return !rightMissing ? 1 : (leftMissing ? 0 : -1); // NOSONAR
        }
    };

    abstract int compareMissings(boolean leftMissing, boolean rightMissing);

    /**
     * Adapts a {@link DataValue} comparator to compare {@link RowRead}s, handling missing values according to this
     * enumeration entry's strategy.
     *
     * @param index column index
     * @param valueComparator wrapped comparator
     * @return resulting comparator on {@link RowRead}s
     */
    public Comparator<RowRead> compareColumnWithMissing(final int index,
            final Comparator<? extends DataValue> valueComparator) {
        return (left, right) -> {
            final var leftMissing = left.isMissing(index);
            final var rightMissing = right.isMissing(index);
            if (leftMissing || rightMissing) {
                return compareMissings(leftMissing, rightMissing);
            }
            return valueComparator.compare(left.getValue(index), right.getValue(index));
        };
    }

    /**
     * Wraps a data cell comparator to handle missing values according to this enumeration entry's strategy.
     *
     * @param cellComparator wrapped comparator, only called with non-missing {@link DataCell}s
     * @return resulting comparator on {@link DataCell}s
     */
    public <T extends DataCell> Comparator<T> applyMissingOrdering(final Comparator<T> cellComparator) {
        return (left, right) -> {
            final var leftMissing = left.isMissing();
            final var rightMissing = right.isMissing();
            if (leftMissing || rightMissing) {
                return compareMissings(leftMissing, rightMissing);
            }
            return cellComparator.compare(left, right);
        };
    }
}
