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
 *   Jun 12, 2012 (wiswedel): created
 */
package org.knime.core.node.streamable;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeModel;

/**
 * Performs the execution of a node in streamed/distributed mode. Objects are
 * created in a node model. There is only one instance of an operator if the
 * node model requires all inputs to be non-distributable. There are several
 * instances of a streamable operator if the input is distributed, whereby these
 * instances are possibly created in different JVMs (when run in a compute
 * cluster/cloud).
 *
 * @see org.knime.core.node.NodeModel#createStreamableOperator(PartitionInfo,
 *      org.knime.core.node.port.PortObjectSpec[])
 * @since 2.6
 * @noextend Pending API.
 * @noimplement Pending API.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public interface StreamableOperator {

    /**
     * Performs the execution on the input and (possibly) produces the output.
     * The array elements of the <code>inputs</code> argument must be cast to
     * specific classes. The class depends on the {@link InputPortRole} of the
     * corresponding port (as described by {@link NodeModel#getInputPortRoles()}.
     * If it is streamable (which can only be true for data ports), then the
     * object can be cast to {@link RowInput}. If it's non streamable, then it's
     * an instance of {@link PortObjectInput}.
     *
     * <p>The <code>outputs</code> argument contains non-null output handles
     * in either of the two cases: (i) none of the inputs is non-distributable
     * (only one instance of a streamable operator that generates the final
     * output) or (ii) the corresponding output is distributable
     * ({@link NodeModel#getOutputPortRoles()}. Non-null output handles need
     * to be filled by this method.
     *
     * @param inputs The input handles.
     * @param outputs The output handles (may contain null, see above).
     * @param ctx The context for cancelation, progress reporting.
     * @throws Exception Any exception to indicate an error, cancelation.
     */
    public void execute(final PortInput[] inputs, final PortOutput[] outputs,
            final ExecutionContext ctx) throws Exception;

    /** Create the internals calculated during eecution that need to be merged
     * and feeded back to the node that controls the execution. Only distributed
     * streamable operator should have internals (and only if they need to
     * pass information to the node, e.g. parts of the output object or view
     * internals or warning messages).
     *
     * @return The internals that are transferred back to the client. Internals
     * of different operators are merged using a {@link MergeOperator}.
     */
    public StreamableOperatorInternals getInternals();

}
