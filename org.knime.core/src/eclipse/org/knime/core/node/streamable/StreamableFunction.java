/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Mar 9, 2012 (wiswedel): created
 */
package org.knime.core.node.streamable;

import org.knime.core.data.DataRow;
import org.knime.core.node.ExecutionContext;

/** Simplified streamable operator that represents a function. A function is
 * a calculation on a row that outputs a row (no data is filtered or newly
 * created).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 * @noextend See {@link StreamableOperator}.
 */
public abstract class StreamableFunction extends StreamableOperator {

    /** {@inheritDoc} */
    @Override
    public void runFinal(final PortInput[] inputs, final PortOutput[] outputs,
            final ExecutionContext ctx) throws Exception {
        RowInput rowInput = ((RowInput)inputs[0]);
        RowOutput rowOutput = ((RowOutput)outputs[0]);
        DataRow inputRow;
        long index = 0;
        while ((inputRow = rowInput.poll()) != null) {
            rowOutput.push(compute(inputRow));
            ctx.setMessage(String.format("Row %d (\"%s\"))",
                    ++index, inputRow.getKey()));
        }
        rowInput.close();
        rowOutput.close();
        finish();
    }

    /** Single row computation.
     * @param input The input row.
     * @return The computed output row.
     * @throws Exception if that fails. */
    public abstract DataRow compute(final DataRow input) throws Exception;

    /** Called after all rows have been (successfully) processed.  */
    public void finish() {
        // no op
    }

}
