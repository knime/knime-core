/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Nov 7, 2016 (wiswedel): created
 */
package org.knime.core.node.workflow.action;

import java.util.ArrayList;

import org.knime.core.node.workflow.NativeNodeContainer;

/**
 * Return value of {@link org.knime.core.node.workflow.NodeContainer#getInteractiveWebViews()}. It combines all
 * the required fields/information that are required for the editor to offer the context menu and open the views.
 *
 * <p>The list is abstracted to a result object as collecting the views requires locking the workflow/subnode, which
 * might be expensive.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzland
 * @since 3.3
 */
public final class InteractiveWebViewsResult {

    private final SingleInteractiveWebViewResult[] m_singleViewResults;

    InteractiveWebViewsResult(final SingleInteractiveWebViewResult[] singleViewResults) {
        m_singleViewResults = singleViewResults;
    }

    /** @return number of views. */
    public int size() {
        return m_singleViewResults.length;
    }

    /** Get individual view info.
     * @param index Index of interest
     * @return The view result.
     */
    public SingleInteractiveWebViewResult get(final int index) {
        return m_singleViewResults[index];
    }

    /** Represents a single web view, currently a NodeModel and a name. */
    public static final class SingleInteractiveWebViewResult {

        /** The view itself is currently in UI code (org.knime.workbench.editor2.WizardNodeView) as it relies on the
         * SWT browser and eclipse code. */
        private final NativeNodeContainer m_model;
        private final String m_viewName;

        /**
         * @param model
         * @param viewName
         */
        SingleInteractiveWebViewResult(final NativeNodeContainer model, final String viewName) {
            m_model = model;
            m_viewName = viewName;
        }

        /** @return the node for the view. */
        public NativeNodeContainer getNativeNodeContainer() {
            return m_model;
        }

        /** @return the viewName */
        public String getViewName() {
            return m_viewName;
        }

    }

    /** @return a new builder to which node/views can be incrementally added. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /** Builder for a {@link InteractiveWebViewsResult}. */
    public static final class Builder {
        private final ArrayList<SingleInteractiveWebViewResult> m_viewResults = new ArrayList<>();

        Builder() {
        }

        /** Add the interactive view that is provided by a given node. Assumes the node does provide a view.
         * @param nc The NC providing a interactive view.
         * @return this */
        public Builder add(final NativeNodeContainer nc) {
            return add(nc, nc.getInteractiveViewName());
        }

        /** Add the interactive view that is provided by a given node. Assumes the node does provide a view.
         * @param nc The NC providing a interactive view.
         * @param name the name as shown in the context menu.
         * @return this */
        public Builder add(final NativeNodeContainer nc, final String name) {
            m_viewResults.add(new SingleInteractiveWebViewResult(nc, name));
            return this;
        }

        /** @return a new result object. */
        public InteractiveWebViewsResult build() {
            return new InteractiveWebViewsResult(
                m_viewResults.toArray(new SingleInteractiveWebViewResult[m_viewResults.size()]));
        }
    }

}
