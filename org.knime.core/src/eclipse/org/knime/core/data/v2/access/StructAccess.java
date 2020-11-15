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
 *   Oct 9, 2020 (dietzc): created
 */
package org.knime.core.data.v2.access;

/**
 * Struct access to underlying data structures.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class StructAccess {

    private StructAccess() {
    }

    /**
     * StructAccessSpec defines the inner types of a struct.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class StructAccessSpec implements AccessSpec<StructReadAccess, StructWriteAccess> {

        private final AccessSpec<?, ?>[] m_inner;

        /**
         * @param inner AccessSpecs of the struct.
         */
        public StructAccessSpec(final AccessSpec<?, ?>... inner) {
            m_inner = inner;
        }

        /**
         * @return the inner specs
         */
        public AccessSpec<?, ?>[] getInnerSpecs() {
            return m_inner;
        }

        @Override
        public <V> V accept(final AccessSpecMapper<V> v) {
            return v.visit(this);
        }
    }

    /**
     * Read access to a struct.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface StructReadAccess extends ReadAccess {

        /**
         * Get {@link ReadAccess} to an inner {@link ReadAccess}.
         *
         * @param <R> type
         * @param index of inner type
         * @return {@link ReadAccess} to inner types of the struct
         */
        <R extends ReadAccess> R getInnerReadAccessAt(int index);
    }

    /**
     * Write access to a struct.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface StructWriteAccess extends WriteAccess {

        /**
         * Get {@link WriteAccess} to an inner {@link WriteAccess}.
         *
         * @param <W> type
         * @param index of inner type
         * @return {@link WriteAccess} to inner types of the struct
         */
        <W extends WriteAccess> W getWriteAccessAt(int index);
    }
}
