/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.wizard.AbstractWizardNodeView;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult.SingleInteractiveWebViewResult;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WizardNodeView;

/**
 * Action to open an interactive web view of a node.
 *
 * @author Bernd Wiswedel, KNIME.com AG, Zurich, Switzerland
 * @since 3.3
 */
public final class OpenInteractiveWebViewAction extends Action {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenInteractiveWebViewAction.class);

    private final SingleInteractiveWebViewResult m_webViewForNode;
    private final NodeContainer m_nodeContainer;


    /**
     * New action to open an interactive node view.
     *
     * @param nodeContainer The NC for the view, might not the NodeContainer for the model contained in the view arg
     * @param webViewForNode The view for the node (note, for {@link org.knime.core.node.workflow.SubNodeContainer}
     *        this is the content of a contained node.
     */
    public OpenInteractiveWebViewAction(final NodeContainer nodeContainer,
        final SingleInteractiveWebViewResult webViewForNode) {
        m_nodeContainer = CheckUtils.checkArgumentNotNull(nodeContainer);
        m_webViewForNode = CheckUtils.checkArgumentNotNull(webViewForNode);
    }

    @Override
    public boolean isEnabled() {
        return m_nodeContainer.getNodeContainerState().isExecuted();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openInteractiveView.png");
    }

    @Override
    public String getToolTipText() {
        return "Opens interactive node view: " + m_webViewForNode.getViewName();
    }

    @Override
    public String getText() {
        return "Interactive View: " + m_webViewForNode.getViewName();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void run() {
        LOGGER.debug("Open Interactive Web Node View " + m_nodeContainer.getName());
        NativeNodeContainer nativeNC = m_webViewForNode.getNativeNodeContainer();
        try {
            AbstractWizardNodeView view = null;
            NodeContext.pushContext(nativeNC);
            try {
                NodeModel nodeModel = nativeNC.getNodeModel();
                view = getConfiguredWizardNodeView(nodeModel);
            } finally {
                NodeContext.removeLastContext();
            }
            view.setWorkflowManagerAndNodeID(nativeNC.getParent(), nativeNC.getID());
            final String title = m_webViewForNode.getViewName();
            Node.invokeOpenView(view, title, OpenViewAction.getAppBoundsAsAWTRec());
        } catch (Throwable t) {
            final MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Interactive View cannot be opened");
            mb.setMessage("The interactive view cannot be opened for the following reason:\n" + t.getMessage());
            mb.open();
            LOGGER.error("The interactive view for node '" + nativeNC.getNameWithID() + "' has thrown a '"
                    + t.getClass().getSimpleName() + "'. That is most likely an implementation error.", t);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private AbstractWizardNodeView getConfiguredWizardNodeView(final NodeModel nodeModel) {
        //TODO uncomment for 3.1, make view interchangeable
        //TODO get preference key
        /*String viewID = "org.knime.ext.chromedriver.ChromeWizardNodeView";
        try {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IConfigurationElement[] configurationElements =
                registry.getConfigurationElementsFor("org.knime.core.WizardNodeView");
            for (IConfigurationElement element : configurationElements) {
                if (viewID.equals(element.getAttribute("viewClass"))) {
                    try {
                        return (AbstractWizardNodeView)element.createExecutableExtension("viewClass");
                    } catch (Throwable e) {
                        LOGGER.error("Can't load view class for " + element.getAttribute("name")
                            + ". Switching to default. - " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.error(
                "JS view set in preferences (" + viewID + ") can't be loaded. Switching to default. - "
                    + e.getMessage(), e);
        }*/
        return new WizardNodeView(nodeModel);
    }

    @Override
    public String getId() {
        return "knime.open.interactive.web.view.action";
    }
}
