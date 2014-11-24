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
 *   23.09.2014 (Christian Albrecht, KNIME.com AG, Zurich, Switzerland): created
 */
package org.knime.core.node.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.InteractiveViewDelegate;
import org.knime.core.node.interactive.ReexecutionCallback;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @param <T> requires a {@link NodeModel} implementing {@link WizardNode} as well
 * @param <REP> the {@link WebViewContent} implementation used as view representation
 * @param <VAL> the {@link WebViewContent} implementation used as view value
 * @since 2.11
 */
public abstract class AbstractWizardNodeView<T extends NodeModel & WizardNode<REP, VAL>,
    REP extends WebViewContent, VAL extends WebViewContent> extends AbstractNodeView<T>
    implements InteractiveView<T, REP, VAL> {

    private static final String EXT_POINT_ID = "org.knime.core.WizardNodeView";

    private final InteractiveViewDelegate<VAL> m_delegate;

    /**
     * @param nodeModel
     */
    protected AbstractWizardNodeView(final T nodeModel) {
        super(nodeModel);
        m_delegate = new InteractiveViewDelegate<VAL>();
    }

    @Override
    public void setWorkflowManagerAndNodeID(final WorkflowManager wfm, final NodeID id) {
        m_delegate.setWorkflowManagerAndNodeID(wfm, id);
    }

    @Override
    public boolean canReExecute() {
        return m_delegate.canReExecute();
    }

    /**
     * @since 2.10
     */
    @Override
    public void triggerReExecution(final VAL value, final boolean useAsNewDefault, final ReexecutionCallback callback) {
        m_delegate.triggerReExecution(value, useAsNewDefault, callback);
    }

    /**
     * @return The current html file object.
     */
    protected File getViewSource() {
        String viewPath = getNodeModel().getViewHTMLPath();
        if (viewPath != null && !viewPath.isEmpty()) {
            return new File(viewPath);
        }
        return null;
    }

     /**
     * {@inheritDoc}
     */
    @Override
    protected void callCloseView() {
        closeView();
    }

    /**
     * @return The node views creator instance.
     */
    protected WizardViewCreator<REP, VAL> getViewCreator() {
        return getNodeModel().getViewCreator();
    }

    /**
     * Called on view close.
     */
    protected abstract void closeView();

    /**
     * Queries extension point for additional {@link AbstractWizardNodeView} implementations.
     * @return A list with all registered view implementations.
     */
    @SuppressWarnings("unchecked")
    public static List<WizardNodeViewExtension> getAllWizardNodeViews() {
        List<WizardNodeViewExtension> viewExtensionList = new ArrayList<WizardNodeViewExtension>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement viewElement : elements) {
                String viewClassName = viewElement.getAttribute("viewClass");
                String viewName = viewElement.getAttribute("name");
                String viewDesc = viewElement.getAttribute("description");
                Class<AbstractWizardNodeView<?, ?, ?>> viewClass;
                try {
                    viewClass = (Class<AbstractWizardNodeView<?, ?, ?>>)Class.forName(viewClassName);
                    viewExtensionList.add(new WizardNodeViewExtension(viewClass, viewName, viewDesc));
                } catch (ClassNotFoundException ex) {
                    NodeLogger.getLogger(AbstractWizardNodeView.class).coding(
                        "Could not find implementation for " + viewClassName, ex);
                }
            }
        }
        return viewExtensionList;
    }

    /**
     * Implementation of a WizardNodeView from extension point.
     *
     * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
     */
    public static class WizardNodeViewExtension {

        private Class<AbstractWizardNodeView<?, ?, ?>> m_viewClass;
        private String m_viewName;
        private String m_viewDescription;

        /**
         * Creates a new WizardNodeViewExtension.
         *
         * @param viewClass the class holding the view implementation
         * @param viewName the name of the view
         * @param viewDescription the optional description of the view
         */
        public WizardNodeViewExtension(final Class<AbstractWizardNodeView<?, ?, ?>> viewClass, final String viewName,
            final String viewDescription) {
            m_viewClass = viewClass;
            m_viewName = viewName;
            m_viewDescription = viewDescription;
        }

        /**
         * @return the viewClass
         */
        public Class<AbstractWizardNodeView<? extends NodeModel, ? extends WebViewContent, ? extends WebViewContent>>
            getViewClass() {
            return m_viewClass;
        }

        /**
         * @return the viewName
         */
        public String getViewName() {
            return m_viewName;
        }

        /**
         * @return the viewDescription
         */
        public String getViewDescription() {
            return m_viewDescription;
        }
    }

}
