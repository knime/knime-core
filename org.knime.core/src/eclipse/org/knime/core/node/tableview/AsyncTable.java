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
 *   Aug 28, 2018 (hornm): created
 */
package org.knime.core.node.tableview;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A table that needs some time to load it's rows (asynchronous).
 *
 * No API, only intended for UI.
 *
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 3.7
 */
public interface AsyncTable {

    /**
     * Registers a callback that gets called when new rows get available (and their loading is finished).
     *
     * @param rowsAvailableCallback consumer that receives the from- and to-index of the rows that became available. Can
     *            be <code>null</code>, e.g., to unregister the callback.
     */
    void setRowsAvailableCallback(BiConsumer<Long, Long> rowsAvailableCallback);

    /**
     * Registers a callback that gets called when the end of the table has been reached and/or the row count is known.
     *
     * @param rowCountKnownCallback consumer that receives the new row count. Can be <code>null</code>, e.g., to
     *            unregister the callback.
     */
    void setRowCountKnownCallback(Consumer<Long> rowCountKnownCallback);

    /**
     * Cancel all loading processes if there are still some running.
     */
    void cancel();
}
