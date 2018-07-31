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
 *   2 Aug 2018 (Marc Bux, KNIME AG, Zurich, Switzerland): created
 */
package org.knime.core.data;

import java.io.IOException;

/**
 * Implementation of an unmaterialized cell that has not been read and therefore cannot be accessed or worked with. Long
 * term, we might introduce some kind of lazy loading of unmaterialized cells. For now, we simply throw an exception
 * when an unmaterialized cell is accessed in any way.
 *
 * @see DataTable#iterator(int...)
 * @see DataTable#iterator(String...)
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 * @since 3.7
 */
public final class UnmaterializedCell extends DataCell {

    private static final long serialVersionUID = 5706006742035041039L;

    private static final UnmaterializedCell INSTANCE = new UnmaterializedCell();

    /**
     * A cell representing a {@link DataCell} that has not been materialized. When accessed or (attempted to be) worked
     * with, it throws an {@link UnmaterializedDataCellException}.
     *
     * @return singleton of an unmaterialized cell
     */
    public static UnmaterializedCell getInstance() {
        return INSTANCE;
    }

    private UnmaterializedCell() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        /**
         * Users of KNIME should not get to see the String representation of unmaterialized cells. Still, we should not
         * throw an exception here, since it will make debugging difficult.
         */
        return "!";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        throw new UnmaterializedDataCellException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Factory for (de-)serializing an unmaterialized cell.
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class UnmaterializedSerializer implements DataCellSerializer<UnmaterializedCell> {

        /** {@inheritDoc} */
        @Override
        public void serialize(final UnmaterializedCell cell, final DataCellDataOutput output) throws IOException {
            throw new UnmaterializedDataCellException();
        }

        /** {@inheritDoc} */
        @Override
        public UnmaterializedCell deserialize(final DataCellDataInput input) throws IOException {
            return INSTANCE;
        }

    }

    /**
     * Thrown to indicate that a {@link DataCell} has not been materialized and cannot be accessed.
     */
    public static final class UnmaterializedDataCellException extends RuntimeException {

        private static final long serialVersionUID = 673504816092726049L;

        /**
         * Constructs an UnmaterializedDataCellException with no detail message.
         */
        public UnmaterializedDataCellException() {
            super("Data cell is not materialized and cannot be accessed.");
        }

    }

}
