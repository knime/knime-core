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
 */
package org.knime.core.data.container.filter.predicate;

/**
 * Abstract class for {@link FilterPredicate FilterPredicates} expressing binary logical operations.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public abstract class BinaryLogicalPredicate implements FilterPredicate {

    private final FilterPredicate m_left;

    private final FilterPredicate m_right;

    private BinaryLogicalPredicate(final FilterPredicate left, final FilterPredicate right) {
        m_left = left;
        m_right = right;
    }

    /**
     * Method for obtaining the left part of the binary logical operation expressed by this
     * {@link BinaryLogicalPredicate}.
     *
     * @return the left part of the binary logical operation
     */
    public FilterPredicate getLeft() {
        return m_left;
    }

    /**
     * Method for obtaining the right part of the binary logical operation expressed by this
     * {@link BinaryLogicalPredicate}.
     *
     * @return the right part of the binary logical operation
     */
    public FilterPredicate getRight() {
        return m_right;
    }

    /**
     * The {@link FilterPredicate FilterPredicates} expressing logical conjunction.
     */
    public static final class And extends BinaryLogicalPredicate {

        And(final FilterPredicate left, final FilterPredicate right) {
            super(left, right);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return "(" + getLeft() + " and " + getRight() + ")";
        }

    }

    /**
     * The {@link FilterPredicate FilterPredicates} expressing logical disjunction.
     */
    public static final class Or extends BinaryLogicalPredicate {

        Or(final FilterPredicate left, final FilterPredicate right) {
            super(left, right);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return "(" + getLeft() + " or " + getRight() + ")";
        }

    }

}
