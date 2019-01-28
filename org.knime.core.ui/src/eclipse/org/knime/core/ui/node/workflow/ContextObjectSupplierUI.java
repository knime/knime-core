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
 */
package org.knime.core.ui.node.workflow;

import java.util.Optional;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerParent;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeContext.ContextObjectSupplier;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.CoreUIPlugin;
import org.knime.core.ui.UI;
import org.knime.core.ui.wrapper.NodeContainerWrapper;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.core.ui.wrapper.Wrapper;

/**
 * Context object supplier that either takes a {@link NodeContainer} or a {@link NodeContainerUI} and converts it into
 * {@link NodeContainer}, {@link NodeContainerUI}, {@link WorkflowManager}, or {@link WorkflowManagerUI}.
 *
 * Is registered with {@link NodeContext} in {@link CoreUIPlugin#start(org.osgi.framework.BundleContext)}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class ContextObjectSupplierUI implements ContextObjectSupplier, UI {

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Optional<C> getObjOfClass(final Class<C> contextObjClass, final Object srcObj) {
        if (srcObj instanceof NodeContainer) {
            return getObjOfClass(contextObjClass, (NodeContainer)srcObj);
        } else if (srcObj instanceof NodeContainerUI) {
            return getObjOfClass(contextObjClass, (NodeContainerUI)srcObj);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static <C> Optional<C> getObjOfClass(final Class<C> contextObjClass, final NodeContainer srcObj) {
        //order of checking important
        if (WorkflowManagerUI.class.isAssignableFrom(contextObjClass)) {
            return Optional.of((C)getRootParent(NodeContainerWrapper.wrap(srcObj)));
        } else if (NodeContainerUI.class.isAssignableFrom(contextObjClass)) {
            return Optional.of((C)NodeContainerWrapper.wrap(srcObj));
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static <C> Optional<C> getObjOfClass(final Class<C> contextObjClass, final NodeContainerUI srcObj) {
        //order of checking important here
        if (WorkflowManagerUI.class.isAssignableFrom(contextObjClass)) {
            return Optional.of((C)getRootParent(srcObj));
        } else if (NodeContainerUI.class.isAssignableFrom(contextObjClass)) {
            return Optional.of((C)srcObj);
        } else if (WorkflowManager.class.isAssignableFrom(contextObjClass)) {
            return (Optional<C>)Wrapper.unwrapWFMOptional(getRootParent(srcObj));
        } else if (NodeContainer.class.isAssignableFrom(contextObjClass)) {
            return (Optional<C>)Wrapper.unwrapNCOptional(srcObj);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("cast")
    private static WorkflowManagerUI getRootParent(final NodeContainerUI nc) {
        if (nc == null) {
            return null;
        }

        if (Wrapper.wraps(nc, NodeContainer.class)) {
            return WorkflowManagerWrapper.wrap(getRootParent(Wrapper.unwrapNC(nc)));
        }

        // find the actual workflow and not the metanode the container may be in
        WorkflowManagerUI parent = nc instanceof WorkflowManagerUI ? (WorkflowManagerUI)nc : nc.getParent();
        while (!(parent instanceof WorkflowManagerUI && parent.isProject())) {
            assert parent != null : "Parent item can't be null as a project parent is expected";
            parent = parent.getParent();
        }
        return parent;
    }

    private static WorkflowManager getRootParent(final NodeContainer nc) {
        // find the actual workflow and not the metanode the container may be in
        NodeContainerParent parent = nc instanceof WorkflowManager ? (WorkflowManager)nc : nc.getDirectNCParent();

        while (!(parent instanceof WorkflowManager && ((WorkflowManager)parent).isProject())) {
            assert parent != null : "Parent item can't be null as a project parent is expected";
            parent = parent.getDirectNCParent();
        }
        return (WorkflowManager)parent;
    }
}
