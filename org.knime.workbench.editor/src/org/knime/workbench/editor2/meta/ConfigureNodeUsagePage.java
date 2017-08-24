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
 *   24 Aug 2017 (albrecht): created
 */
package org.knime.workbench.editor2.meta;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class ConfigureNodeUsagePage extends WizardPage {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ConfigureNodeUsagePage.class);
    private static final String DESCRIPTION = "Specifiy in what way the contained nodes are allowed to be used.";

    private WorkflowManager m_wfmManager;
    private SubNodeContainer m_subNode;

    private Map<NodeID, NodeModel> m_viewNodes;
    private Map<NodeID, Button> m_wizardUsageMap;
    private Map<NodeID, Button> m_dialogUsageMap;
    private Map<NodeID, Button> m_interfaceUsageMap;

    /**
     * @param pageName
     */
    protected ConfigureNodeUsagePage(final String pageName) {
        super(pageName);
        setTitle(pageName);
        setDescription(DESCRIPTION);

        m_viewNodes = new LinkedHashMap<NodeID, NodeModel>();
        m_wizardUsageMap = new LinkedHashMap<NodeID, Button>();
        m_dialogUsageMap = new LinkedHashMap<NodeID, Button>();
        m_interfaceUsageMap = new LinkedHashMap<NodeID, Button>();
    }

    /**
     * This page initializes from the sub node.
     *
     * @param subNode the sub node to initialize the port lists from (and that is to be reconfigured)
     */
    void setNodes(final SubNodeContainer subNode, final Map<NodeID, NodeModel> viewNodes) {
        m_subNode = subNode;
        m_viewNodes = viewNodes;
        m_wfmManager = m_subNode.getWorkflowManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        if (m_viewNodes.size() > 0) {
            createNodeGrid(composite);
        } else {
            //TODO display message
        }
        setControl(composite);
    }

    private void createNodeGrid(final Composite parent) {
        ScrolledComposite scrollPane = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        scrollPane.setExpandHorizontal(true);
        scrollPane.setExpandVertical(true);
        Composite composite = new Composite(scrollPane, SWT.NONE);
        scrollPane.setContent(composite);
        scrollPane.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        composite.setLayout(new GridLayout(4, false));
        composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        //titles
        new Composite(composite, SWT.NONE); /* Placeholder */
        Label wizardLabel = new Label(composite, SWT.LEFT);
        FontData fontData = wizardLabel.getFont().getFontData()[0];
        Font boldFont = new Font(Display.getCurrent(),
            new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
        wizardLabel.setText("WebPortal");
        wizardLabel.setFont(boldFont);
        Label dialogLabel = new Label(composite, SWT.LEFT);
        dialogLabel.setText("Dialog");
        dialogLabel.setFont(boldFont);
        Label interfaceLabel = new Label(composite, SWT.LEFT);
        interfaceLabel.setText("Interface");
        interfaceLabel.setFont(boldFont);

        //select all checkboxes
        Label selectAllLabel = new Label(composite, SWT.LEFT);
        selectAllLabel.setText("Enable/Disable");
        Button selectAllWizard = new Button(composite, SWT.CHECK);
        Button selectAllDialog = new Button(composite, SWT.CHECK);
        Button selectAllInterface = new Button(composite, SWT.CHECK);

        //individual nodes
        for (Entry<NodeID, NodeModel> entry : m_viewNodes.entrySet()) {
            NodeID id = entry.getKey();
            NodeContainer nodeContainer = m_viewNodes.containsKey(id) ? m_wfmManager.getNodeContainer(id) : null;
            createNodeLabelComposite(composite, id, nodeContainer);

            NodeModel model = entry.getValue();
            if (model instanceof WizardNode) {
                m_wizardUsageMap.put(id, new Button(composite, SWT.CHECK));
            } else {
                new Composite(composite, SWT.NONE); /* Placeholder */
            }
            if (model instanceof DialogNode) {
                m_dialogUsageMap.put(id, new Button(composite, SWT.CHECK));
            } else {
                new Composite(composite, SWT.NONE); /* Placeholder */
            }
            if (model instanceof InputNode || model instanceof OutputNode) {
                m_interfaceUsageMap.put(id, new Button(composite, SWT.CHECK));
            } else {
                new Composite(composite, SWT.NONE); /* Placeholder */
            }
        }
    }

    private Composite createNodeLabelComposite(final Composite parent, final NodeID nodeID, final NodeContainer nodeContainer) {

        Composite labelComposite = new Composite(parent, SWT.NONE);
        labelComposite.setLayout(new GridLayout(2, false));
        labelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        Label iconLabel = new Label(labelComposite, SWT.CENTER);
        iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        if (nodeContainer == null) {
            iconLabel.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png"));
            iconLabel.setToolTipText("This node does not exist. \nIt is suggested to delete it from the layout.");
        } else {
            try (InputStream iconURLStream = FileLocator.resolve(nodeContainer.getIcon()).openStream()) {
                iconLabel.setImage(new Image(Display.getCurrent(), iconURLStream));
            } catch (IOException e) { /* do nothing */ }
        }

        Label nodeLabel = new Label(labelComposite, SWT.LEFT);
        nodeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        String nodeName;
        String annotation = null;
        if (nodeContainer == null) {
            nodeName = "[MISSING]";
            FontData font = nodeLabel.getFont().getFontData()[0];
            nodeLabel.setFont(new Font(parent.getDisplay(), new FontData(font.getName(), font.getHeight(), SWT.ITALIC)));
            nodeLabel.setToolTipText("This node does not exist. \nIt is suggested to delete it from the layout.");
        } else {
            nodeName = nodeContainer.getName();
            annotation = nodeContainer.getNodeAnnotation().getText();
            if (annotation.length() > 42) {
                nodeLabel.setToolTipText(annotation);
            }
            annotation = StringUtils.abbreviateMiddle(annotation, " [...] ", 50).replaceAll("[\n|\r]", "");
        }
        String nodeLabelText = nodeName + "\nID: " + nodeID.getIndex();
        if (StringUtils.isNoneBlank(annotation)) {
            nodeLabelText += "\n" + annotation;
        }
        nodeLabel.setText(nodeLabelText);
        return labelComposite;
    }

}
