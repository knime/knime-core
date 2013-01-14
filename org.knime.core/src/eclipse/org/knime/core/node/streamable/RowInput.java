/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 12, 2012 (wiswedel): created
 */
package org.knime.core.node.streamable;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;

/**
 * A streamable data input.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public abstract class RowInput extends PortInput {

    /**
     * The table spec of the input.
     *
     * @return The table spec of the input table/stream.
     */
    public abstract DataTableSpec getDataTableSpec();

    /**
     * Get the next row from the input stream. The call may block if upstream
     * nodes are still in the process of generating data. Client code could look
     * like this:
     *
     * <pre>
     * DataRow row;
     * while ((row = rowInput.poll()) != null) {
     *     // do something with row
     * }
     * </pre>
     *
     * @return The next row or null if the end of the stream has been reached.
     * @throws InterruptedException If canceled.
     */
    public abstract DataRow poll() throws InterruptedException;

    /** Indicates that no more input is needed. Upstream nodes may stop
     * generating data (unless there are other consumers). */
    public abstract void close();

}
