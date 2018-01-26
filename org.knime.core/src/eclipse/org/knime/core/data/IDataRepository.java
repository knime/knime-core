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
 *   Jan 24, 2018 (wiswedel): created
 */
package org.knime.core.data;

import java.util.Optional;
import java.util.UUID;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;

/**
 * Repository of tables and file stores that live in a workflow. Used to resolve blobs, file stores and referenced
 * tables.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference This interface is not intended to be referenced by clients.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public interface IDataRepository {

    /**
     * Adds a table to the global repository, optional operation.
     *
     * @param key The id under which to add the table - must not previously exist.
     * @param table The table itself.
     */
    void addTable(int key, ContainerTable table);

    /**
     * Get the table for the given id, if present.
     *
     * @param key The lookup key
     * @return The table.
     */
    Optional<ContainerTable> getTable(int key);

    /**
     * Remove the table for the given id, optional operation.
     *
     * @param key The lookup key
     * @return The removed table, if present in the repository.
     */
    Optional<ContainerTable> removeTable(Integer key);

    /**
     * Add a handler to this repo, optional operation.
     *
     * @param handler Non-null handler to add.
     * @throws UnsupportedOperationException If not to be called on this instance.
     */
    void addFileStoreHandler(IWriteFileStoreHandler handler);

    /**
     * Removes the given non-null handler from the repository, optional operation.
     *
     * @param handler to remove
     * @throws UnsupportedOperationException If not to be called on this instance.
     */
    void removeFileStoreHandler(IWriteFileStoreHandler handler);

    /**
     * Get handler to ID (which is part of a saved data stream)
     *
     * @param storeHandlerUUID The handler ID.
     * @return The handler for the id, never null.
     */
    IFileStoreHandler getHandler(UUID storeHandlerUUID);

    /**
     * Get handler to ID (which is part of a saved data stream). Throws exception when ID is unknown, returns never
     * <code>null</code>.
     *
     * @param storeHandlerUUID The handler ID.
     * @return The handler for the id, never null.
     */
    IFileStoreHandler getHandlerNotNull(UUID storeHandlerUUID);

    /**
     * Used for debuggging/error reporting in case an invalid handler is accessed.
     */
    void printValidFileStoreHandlersToLogDebug();

}