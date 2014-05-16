/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 28, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface used by embedded {@link WorkflowManager} instances to invoke actions on the parent item. The parent
 * item is a {@link WorkflowManager} for meta nodes or projects. It is a {@link SubNodeContainer} for the instance
 * that is used inside a sub node.
 *
 * <p>None of the methods are meant be public API.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
interface NodeContainerParent {


    /** See {@link NodeContainer#getDirectNCParent()}.
     * @return the direct node container parent.
     */
    public NodeContainerParent getDirectNCParent();

    /** If a contained node contained (independent of its connections). Generally true but subnodes disallow
     * node reset if there are downstream executing nodes.
     * @return that property.
     */
    public boolean canResetContainedNodes();

    /** @return the workflowMutex */
    public Object getWorkflowMutex();

    /** Is this workflow represents a linked meta node (locked for edit). This
     * flag is only a hint for the UI -- non of the add/remove operations will
     * read this flag.
     * @return Whether edit operations are not permitted. */
    public boolean isWriteProtected();

    /** Private routine which assembles a stack of workflow variables all
     * the way to the top of the workflow hierarchy.
     */
    public void pushWorkflowVariablesOnStack(final FlowObjectStack sos);

    /** Returns the argument string unless this workflow or any of the parent wfms is encrypted.
     * Then it appends ".encrypted" to the argument.
     * @param fileName Suggest file name
     * @return fileName, possibly appended with ".encrypted".
     * @noreference This method is not intended to be referenced by clients.
     */
    public abstract String getCipherFileName(final String fileName);

    /** @param out The output
     * @return see {@link WorkflowCipher#cipherOutput(WorkflowManager, OutputStream)}.
     * @throws IOException If fails
     * @noreference This method is not intended to be referenced by clients. */
    @SuppressWarnings("javadoc")
    public abstract OutputStream cipherOutput(final OutputStream out) throws IOException;

}