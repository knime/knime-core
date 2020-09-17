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
 *   Sep 10, 2020 (dietzc): created
 */
package org.knime.core.data;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Read input.
 *
 * @author Christian Dietz
 * @since 4.2.2
 *
 * @apiNote API still experimental. It might change in future releases of KNIME Analytics Platform.
 *
 * @noreference This interface is not intended to be referenced by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface RowAccess {
    /**
     * @return number of columns.
     */
    int getNumColumns();

    /**
     * Get a {@link DataValue} at a given position.
     *
     * @param <D> type of the {@link DataValue}
     * @param index the column index
     *
     * @return the {@link DataValue} at column index or <source>null</source> if {@link DataValue} is not available, for
     *         example if the column has been filtered out. In case {@link #isMissing(int)} returns
     *         <source>true</source> the returned instance is a {@link MissingValue}.
     *
     * @throws NoSuchElementException if the cursor is at an invalid position
     */
    <D extends DataValue> D getValue(int index);

    /**
     * If <code>true</<code> getValue will return `MissingValue` to get missing value cause.
     *
     * @param index column index
     * @return <code>true</code> if value at index is missing
     *
     * @throws NoSuchElementException if the cursor is at an invalid position
     */
    boolean isMissing(int index);

    /**
     * Returns the error message that explains why the cell is missing, or an empty optional if no such message has been
     * set. This method is not to be called for non-missing values.
     *
     * @param index column index
     * @return the error associated with a missing value at the given column index, if present
     *
     * @throws IllegalStateException if {@link #isMissing(int)} at the given column index does not return
     *             <code>true</code>
     * @throws NoSuchElementException if the cursor is at an invalid position
     *
     * @see MissingValue#getError()
     */
    Optional<String> getMissingValueError(final int index);

    /**
     * @return the {@link RowKeyValue}
     *
     * @throws NoSuchElementException if the cursor is at an invalid position
     */
    RowKeyValue getRowKeyValue();
}
