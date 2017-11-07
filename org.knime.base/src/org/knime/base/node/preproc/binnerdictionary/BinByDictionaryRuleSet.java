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
 * Created on Mar 23, 2013 by wiswedel
 */
package org.knime.base.node.preproc.binnerdictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValueComparator;

/**
 * Contains all rules, implements the search.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class BinByDictionaryRuleSet {

    private final List<Rule> m_rules;
    private final boolean m_isBinarySearch;
    private boolean m_isClosed;
    private final DataValueComparator m_lowerBoundComp;
    private final boolean m_lowerBoundInclusive;
    private final DataValueComparator m_upperBoundComp;
    private final boolean m_upperBoundInclusive;

    /**
     * @param lowerBoundComp comparator from DataColumnSpec
     * @param lowerBoundInclusive checkbox as per configuration
     * @param upperBoundComp ...
     * @param upperBoundInclusive ...
     * @param binarySearch ...
     */
    BinByDictionaryRuleSet(final DataValueComparator lowerBoundComp, final boolean lowerBoundInclusive,
        final DataValueComparator upperBoundComp, final boolean upperBoundInclusive, final boolean binarySearch) {
        m_lowerBoundComp = lowerBoundComp;
        m_lowerBoundInclusive = lowerBoundInclusive;
        m_upperBoundComp = upperBoundComp;
        m_upperBoundInclusive = upperBoundInclusive;
        m_rules = new ArrayList<BinByDictionaryRuleSet.Rule>();
        m_isBinarySearch = binarySearch;
    }

    /** @return number of rules. */
    int getSize() {
        return m_rules.size();
    }

    /** Search, maybe linear maybe binary.
     * @param value to search
     * @return matching cell or null if not found.
     */
    DataCell search(final DataCell value) {
        if (m_isBinarySearch) {
            return binarySearch(value);
        } else {
            return linearSearch(value);
        }
    }

    private DataCell binarySearch(final DataCell value) {
        int low = 0;
        int high = m_rules.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Rule midRule = m_rules.get(mid);
            int compare = midRule.compareTo(value);
            if (compare > 0) {
                low = mid + 1;
            } else if (compare < 0) {
                high = mid - 1;
            } else {
                return midRule.getLabel();
            }
        }
        return null;
    }

    /** Linear search over all elements.
     * @param value Query
     * @return Result or null if no rules matches.
     */
    private DataCell linearSearch(final DataCell value) {
        for (Rule r : m_rules) {
            if (r.matches(value)) {
                return r.getLabel();
            }
        }
        return null;
    }

    /** Called after all rules are added.
     */
    void close() {
        m_isClosed = true;
        if (m_isBinarySearch) {
            Collections.sort(m_rules);
        }
    }

    /** Add a rule to the set.
     * @param lowerBound lower bound from table (possibly missing)
     * @param upperBound ...
     * @param label cell from label column
     */
    void addRule(final DataCell lowerBound, final DataCell upperBound, final DataCell label) {
        if (m_isClosed) {
            throw new IllegalStateException("Closed");
        }
        if (m_isBinarySearch && (lowerBound.isMissing() || upperBound.isMissing())) {
            throw new IllegalStateException("Bounds must not be missing when using binary search - "
                + "select \"linear search\" in dialog");
        }
        m_rules.add(new Rule(lowerBound, upperBound, label));
    }

    private static int compare(final DataValueComparator comparator,
                              final DataCell c1, final DataCell c2, final boolean missingAsLarger) {
        boolean c1Missing = c1.isMissing();
        boolean c2Missing = c2.isMissing();
        if (c1Missing && c2Missing) {
            return 0;
        } else if (c1Missing) {
            return missingAsLarger ? +1 : -1;
        } else if (c2Missing) {
            return missingAsLarger ? -1 : +1;
        }
        return comparator.compare(c1, c2);
    }

    private final class Rule implements Comparable<Rule> {

        private final DataCell m_lowerBoundValue;
        private final DataCell m_upperBoundValue;
        private final DataCell m_label;

        Rule(final DataCell lowerBoundValue, final DataCell upperBoundValue, final DataCell label) {
            m_lowerBoundValue = lowerBoundValue;
            m_upperBoundValue = upperBoundValue;
            m_label = label;
        }

        boolean matches(final DataCell c) {
            if (m_lowerBoundComp != null && !m_lowerBoundValue.isMissing()) {
                int compare = m_lowerBoundComp.compare(m_lowerBoundValue, c);
                if (m_lowerBoundInclusive && compare > 0) {
                    return false;
                } else if (!m_lowerBoundInclusive && compare >= 0) {
                    return false;
                }
            }
            if (m_upperBoundComp != null && !m_upperBoundValue.isMissing()) {
                int compare = m_upperBoundComp.compare(m_upperBoundValue, c);
                if (m_upperBoundInclusive && compare < 0) {
                    return false;
                } else if (!m_upperBoundInclusive && compare <= 0) {
                    return false;
                }
            }
            return true;
        }

        /** @return the label */
        DataCell getLabel() {
            return m_label;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Lower: \"" + m_lowerBoundValue + "\"; Upper: \"" + m_upperBoundValue + "\"; Label: \"" + m_label
                    + "\"";
        }

        /** Compares cell to range.
         * -1 if cell is smaller than lower bound
         * +1 if cell is larger than upper bound
         * 0 if cell is within range
         * @param cell
         * @return
         */
        int compareTo(final DataCell cell) {
            if (m_lowerBoundComp != null) {
                int lowCom = compare(m_lowerBoundComp, m_lowerBoundValue, cell, false);
                boolean isLargerThanLowerBound = m_lowerBoundInclusive ? lowCom <= 0 : lowCom < 0;
                if (!isLargerThanLowerBound) {
                    return -1; // cell is smaller than lower bound
                }
            }
            if (m_upperBoundComp != null) {
                int upCom = compare(m_upperBoundComp, m_upperBoundValue, cell, true);
                boolean isSmallerThanUpperBound = m_upperBoundInclusive ? upCom >= 0 : upCom > 0;
                if (!isSmallerThanUpperBound) {
                    return 1;
                }
            }
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final Rule o) {
            if (m_lowerBoundComp != null) {
                int lowCom = compare(m_lowerBoundComp, m_lowerBoundValue, o.m_lowerBoundValue, false);
                if (lowCom != 0) {
                    return lowCom;
                }
            }
            if (m_upperBoundComp != null) {
                return compare(m_upperBoundComp, m_upperBoundValue, o.m_upperBoundValue, true);
            }
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((m_label == null) ? 0 : m_label.hashCode());
            result = prime * result + ((m_lowerBoundValue == null) ? 0 : m_lowerBoundValue.hashCode());
            result = prime * result + ((m_upperBoundValue == null) ? 0 : m_upperBoundValue.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Rule other = (Rule)obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (m_label == null) {
                if (other.m_label != null) {
                    return false;
                }
            } else if (!m_label.equals(other.m_label)) {
                return false;
            }
            if (m_lowerBoundValue == null) {
                if (other.m_lowerBoundValue != null) {
                    return false;
                }
            } else if (!m_lowerBoundValue.equals(other.m_lowerBoundValue)) {
                return false;
            }
            if (m_upperBoundValue == null) {
                if (other.m_upperBoundValue != null) {
                    return false;
                }
            } else if (!m_upperBoundValue.equals(other.m_upperBoundValue)) {
                return false;
            }
            return true;
        }

        private BinByDictionaryRuleSet getOuterType() {
            return BinByDictionaryRuleSet.this;
        }
    }
}
