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
 *   Oct 9, 2020 (benjamin): created
 */
package org.knime.core.data.v2.access;

import org.knime.core.data.DataValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;

/**
 * Definitions of Access for to Lists.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class ListAccess {

    private ListAccess() {
    }

    /**
     * Specification of a {@link ListReadAccess} and {@link ListWriteAccess}.
     *
     * @param <R> the type of the {@link ReadAccess} for the list elements
     * @param <W> the type of the {@link WriteAccess} for the list elements
     */
    public static final class ListAccessSpec<R extends ReadAccess, W extends WriteAccess>
        implements AccessSpec<ListReadAccess, ListWriteAccess> {

        private final ValueFactory<R, W> m_inner;

        /**
         * @param inner the {@link ValueFactory} for the list elements
         */
        public ListAccessSpec(final ValueFactory<R, W> inner) {
            m_inner = inner;
        }

        /**
         * @return the inner {@link ValueFactory} to create values for the elements
         */
        public ValueFactory<R, W> getInnerValueFactory() {
            return m_inner;
        }

        /**
         * @return the {@link AccessSpec} of the list elements
         */
        public AccessSpec<R, W> getInnerSpecs() {
            return m_inner.getSpec();
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
        }
    }

    /**
     * {@link ReadAccess} to a list.
     *
     * @since 4.3
     */
    public interface ListReadAccess extends ReadAccess {

        /**
         * Get the {@link ReadValue} at the given index in the list. Note that this object should only be used until
         * this method is called again. Implementations are allowed to reuse the object when this method is called
         * again.
         *
         * @param <R> the type of the {@link ReadValue}
         * @param index the index in the list
         * @return the {@link ReadValue} at the given index
         */
        <R extends ReadValue> R getReadValue(int index);

        /**
         * @param index the index in the list
         * @return <code>true</code> if the value at this index is missing
         */
        boolean isMissing(int index);

        /**
         * @return the size of the list
         */
        int size();
    }

    /**
     * {@link WriteAccess} to a list.
     *
     * @since 4.3
     */
    public interface ListWriteAccess extends WriteAccess {

        /**
         * Get the {@link WriteValue} at the given index in the list. Call this only after starting a new list with
         * {@link #create(int)}.
         *
         * Note that this object should only be used until this method is called again. Implementations are allowed to
         * reuse the object when this method is called again.
         *
         * @param <D> the type of the {@link DataValue} written by the {@link WriteValue} of type W
         * @param <W> the type of the {@link WriteValue}
         * @param index the index in the list
         * @return the {@link WriteValue} at the given index
         */
        <D extends DataValue, W extends WriteValue<D>> W getWriteValue(int index);

        /**
         * Create a new list with the given size. Call this before accessing the elements.
         *
         * @param size the size of the list
         */
        void create(int size);
    }
}
