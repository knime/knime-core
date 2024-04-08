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
 */
package org.knime.core.data.v2;

import java.io.Closeable;

/**
 * {@link RowRead} implementation allowing for iterative read access to a data storage.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @apiNote API still experimental. It might change in future releases of KNIME Analytics Platform.
 *
 * @noreference This interface is not intended to be referenced by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface RowCursor extends Closeable {

    /**
     * Creates an empty row cursor of rows with a given number of columns.
     *
     * @param numColumns number of columns, as returned by {@link RowCursor#getNumColumns()}
     * @return empty row cursor
     * @since 5.3
     */
    static RowCursor empty(final int numColumns) {
        return new RowCursor() {

            @Override
            public int getNumColumns() {
                return numColumns;
            }

            @Override
            public RowRead forward() {
                return null;
            }

            @Override
            public void close() { // NOSONAR
            }

            @Override
            public boolean canForward() {
                return false;
            }
        };
    }

    /**
     * Forwards the cursor by one to the next element. This next element is not guaranteed to be a new instance.
     *
     * @return the element at the current cursor position or null if no new element available
     */
    RowRead forward();


    /**
     * @return {@code true} if more elements are available, otherwise {@code false}
     */
    boolean canForward();

    /**
     * @return number of columns in the data row
     */
    int getNumColumns();

    /**
     * Closes this resource, relinquishing any underlying resources. This method is invoked automatically on objects
     * managed by the try-with-resources statement. This method is idempotent, i.e., it can be called repeatedly without
     * side effects.<br>
     *
     * Potential IOException will be logged.
     */
    @Override
    void close();
}
