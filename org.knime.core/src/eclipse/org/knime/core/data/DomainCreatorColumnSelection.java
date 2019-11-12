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
 * Created on 20.03.2014 by hofer
 */
package org.knime.core.data;

import java.util.function.Predicate;

/**
 * Defines which columns are effected by the DataTableDomainCreator.
 *
 * @author Heiko Hofer
 * @since 2.10
 */
public interface DomainCreatorColumnSelection {

    /**
     * Whether to create or recreate the domain for the specific column.
     *
     * @param colSpec the column to create or recreate the domain for
     * @return <code>true</code> when the domain of the given column should be create or recreated, <code>false</code>
     *         if it should not be (re)created
     */
    boolean createDomain(DataColumnSpec colSpec);

    /**
     * Whether to drop the domain for the specific column.
     *
     * @param colSpec the column to drop the domain for
     * @return <code>true</code> when the domain of the given column should be dropped, <code>false</code> if it should
     *         not be dropped
     */
    boolean dropDomain(DataColumnSpec colSpec);

    /**
     * Convenience factory method for simple {@link DomainCreatorColumnSelection} instances. Creates the
     * {@link DomainCreatorColumnSelection} by composing {@link Predicate createPredicate} and {@link Predicate
     * dropPredicate}.
     * @param dropDomainPredicate called by the returned object whenever its
     *            {@link DomainCreatorColumnSelection#dropDomain(DataColumnSpec)} method is called
     * @param createDomainPredicate called by the returned object whenever its
     *            {@link DomainCreatorColumnSelection#createDomain(DataColumnSpec)} method is called
     *
     * @return a {@link DomainCreatorColumnSelection} whose behavior is specified by the provided predicates
     * @since 4.1
     */
    static DomainCreatorColumnSelection create(final Predicate<DataColumnSpec> dropDomainPredicate,
        final Predicate<DataColumnSpec> createDomainPredicate) {
        return new DomainCreatorColumnSelection() {

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return createDomainPredicate.test(colSpec);
            }

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return dropDomainPredicate.test(colSpec);
            }

        };
    }
}
