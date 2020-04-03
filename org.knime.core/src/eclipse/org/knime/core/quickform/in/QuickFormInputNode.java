/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.quickform.in;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.dialog.MetaNodeDialogNode;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.quickform.AbstractQuickFormConfiguration;
import org.knime.core.quickform.AbstractQuickFormValueInConfiguration;
import org.knime.core.quickform.QuickFormNode;
import org.knime.core.util.node.quickform.in.AbstractQuickFormInElement;

/**
 * Implemented by {@linkplain org.knime.core.node.NodeModel NodeModel}
 * derivatives that represent quick form input elements.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.6
 *
 * @noimplement Not yet stable API.
 * @deprecated Use {@link DialogNode} or {@link InputNode} instead
 */
@Deprecated
public interface QuickFormInputNode extends QuickFormNode, MetaNodeDialogNode {

    /** Filter used in {@link WorkflowManager#findNextWaitingWorkflowManager(Class, NodeModelFilter)}.
     * It only includes not hidden qf nodes.
     * @since 2.7
     */
    public static final NodeModelFilter<QuickFormInputNode> NOT_HIDDEN_FILTER =
            new NodeModelFilter<QuickFormInputNode>() {

        @Override
        public boolean include(final QuickFormInputNode nodeModel) {
            return !nodeModel.hideInWizard();
        }
    };

    /** Get the quick form element controlling the relevant portions to this
     * node. The returned value can be remotely modified and later loaded into
     * this instance using the
     * {@link #loadFromQuickFormElement(AbstractQuickFormInElement)} method.
     * @return The form element to this node.
     */
    public AbstractQuickFormInElement getQuickFormElement();

    /** Get handle on current (filled) configuration or null.
     * @return The config to use.
     */
    public AbstractQuickFormConfiguration
        <? extends AbstractQuickFormValueInConfiguration> getConfiguration();

    /** Loads values from the argument form element.
     * @param formElement To load from.
     * @throws InvalidSettingsException If the argument is not of expected
     * type or has invalid values.
     */
    public void loadFromQuickFormElement(
            final AbstractQuickFormInElement formElement)
            throws InvalidSettingsException;

}
