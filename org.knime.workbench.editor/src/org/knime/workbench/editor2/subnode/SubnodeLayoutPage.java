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
 *   25.03.2014 (Christian Albrecht, KNIME.com AG, Zurich, Switzerland): created
 */
package org.knime.workbench.editor2.subnode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.wizard.WizardNodeLayoutInfo;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
public class SubnodeLayoutPage extends WizardPage {

    private List<NodeID> m_viewNodes = new ArrayList<NodeID>();
    private WorkflowManager m_wfManager;
    private SubNodeContainer m_subNodeContainer;

    private final Map<Integer, WizardNodeLayoutInfo> m_layoutMap;

    /**
     * @param pageName
     */
    protected SubnodeLayoutPage(final String pageName) {
        super(pageName);
        setDescription("Please define the grid positions and additional layout data of the view nodes.");
        m_layoutMap = new HashMap<Integer, WizardNodeLayoutInfo>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        ScrolledComposite scrollPane = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        scrollPane.setExpandHorizontal(true);
        scrollPane.setExpandVertical(true);
        Composite composite = new Composite(scrollPane, SWT.NONE);
        scrollPane.setContent(composite);
        scrollPane.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        composite.setLayout(new GridLayout(4, false));
        composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        Label titleLabel = new Label(composite, SWT.LEFT);
        FontData fontData = titleLabel.getFont().getFontData()[0];
        Font boldFont = new Font(Display.getCurrent(),
            new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
        titleLabel.setText("Node");
        titleLabel.setFont(boldFont);
        Label xLabel = new Label(composite, SWT.LEFT);
        xLabel.setText("X");
        xLabel.setFont(boldFont);
        Label yLabel = new Label(composite, SWT.LEFT);
        yLabel.setText("Y");
        yLabel.setFont(boldFont);
        Label paddingLabel = new Label(composite, SWT.LEFT);
        paddingLabel.setText("Padding");
        paddingLabel.setFont(boldFont);
        for (final NodeID nodeID : m_viewNodes) {
            NodeContainer nodeContainer = m_wfManager.getNodeContainer(nodeID);
            WizardNodeLayoutInfo layoutInfo = null;
            if (m_subNodeContainer != null && m_subNodeContainer.getLayoutInfo() != null) {
                layoutInfo = m_subNodeContainer.getLayoutInfo().get(nodeID.getIndex());
                if (layoutInfo != null) {
                    m_layoutMap.put(nodeID.getIndex(), layoutInfo);
                }
            }

            Composite labelComposite = new Composite(composite, SWT.NONE);
            labelComposite.setLayout(new GridLayout(2, false));
            labelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

            Label iconLabel = new Label(labelComposite, SWT.CENTER);
            iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            try (InputStream iconURLStream = FileLocator.resolve(nodeContainer.getIcon()).openStream()) {
                iconLabel.setImage(new Image(Display.getCurrent(), iconURLStream));
            } catch (IOException e) { /* do nothing */ }

            Label nodeLabel = new Label(labelComposite, SWT.LEFT);
            nodeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            String nodeName = nodeContainer.getName();
            String annotation = nodeContainer.getNodeAnnotation().getText();
            nodeLabel.setText(nodeName + "\nID: " + nodeID.getIndex() + "\n" + annotation);

            GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            gridData.widthHint = 80;
            final Text xText = new Text(composite, SWT.SINGLE | SWT.BORDER);
            xText.setLayoutData(gridData);
            xText.setText(layoutInfo == null ? "" : layoutInfo.getX());
            xText.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent e) {
                    editX(nodeID, xText);
                }
            });

            final Text yText = new Text(composite, SWT.SINGLE | SWT.BORDER);
            yText.setLayoutData(gridData);
            yText.setText(layoutInfo == null ? "" : layoutInfo.getY());
            yText.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent e) {
                   editY(nodeID, yText);
                }
            });

            final Text paddingText = new Text(composite, SWT.SINGLE | SWT.BORDER);
            paddingText.setLayoutData(gridData);
            paddingText.setText(layoutInfo == null || layoutInfo.getPadding() == null ? "" : layoutInfo.getPadding());
            paddingText.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent e) {
                    editPadding(nodeID, paddingText);
                }
            });
        }
        scrollPane.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        setControl(scrollPane);
    }

    private void editX(final NodeID nodeId, final Text xText) {
        WizardNodeLayoutInfo layoutInfo = getOrCreateLayoutInfo(nodeId);
        layoutInfo.setX(xText.getText());
        updateLayoutInfo(nodeId, layoutInfo);
    }

    private void editY(final NodeID nodeId, final Text yText) {
        WizardNodeLayoutInfo layoutInfo = getOrCreateLayoutInfo(nodeId);
        layoutInfo.setY(yText.getText());
        updateLayoutInfo(nodeId, layoutInfo);
    }

    private void editPadding(final NodeID nodeId, final Text paddingText) {
        WizardNodeLayoutInfo layoutInfo = getOrCreateLayoutInfo(nodeId);
        layoutInfo.setPadding(paddingText.getText());
        updateLayoutInfo(nodeId, layoutInfo);
    }

    private WizardNodeLayoutInfo getOrCreateLayoutInfo(final NodeID nodeID) {
        WizardNodeLayoutInfo layoutInfo = m_layoutMap.get(nodeID.getIndex());
        if (layoutInfo == null) {
            layoutInfo = new WizardNodeLayoutInfo();
        }
        return layoutInfo;
    }

    private void updateLayoutInfo(final NodeID nodeId, final WizardNodeLayoutInfo layoutInfo) {
        if (isEmptyInfo(layoutInfo)) {
            m_layoutMap.remove(nodeId);
        } else {
            m_layoutMap.put(nodeId.getIndex(), layoutInfo);
        }
    }

    /**
     * @param layoutInfo
     * @return
     */
    private boolean isEmptyInfo(final WizardNodeLayoutInfo layoutInfo) {
        String x = layoutInfo.getX();
        String y = layoutInfo.getY();
        String padding = layoutInfo.getPadding();
        boolean isEmpty = (x == null || x.trim().isEmpty());
        isEmpty &= (y == null || y.trim().isEmpty());
        isEmpty &= (padding == null || padding.trim().isEmpty());
        return isEmpty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPageComplete() {
        return isLayoutMapValid();
    }

    /**
     * @return
     */
    public boolean isLayoutMapValid() {
        for (Entry<Integer, WizardNodeLayoutInfo> layoutEntry : m_layoutMap.entrySet()) {
            WizardNodeLayoutInfo layoutInfo = layoutEntry.getValue();
            if (isEmptyInfo(layoutInfo)) {
                m_layoutMap.remove(layoutEntry.getKey());
                continue;
            }
            if (!isCoordinatesSet(layoutInfo.getX(), layoutInfo.getY())) {
                return false;
            }
            if (!isCoordinateDepthEqual(layoutInfo.getX(), layoutInfo.getY())) {
                return false;
            }
            if (!isPaddingParseable(layoutInfo.getPadding())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param x
     * @param y
     * @return
     */
    private boolean isCoordinatesSet(final String x, final String y) {
        return (x != null && !x.trim().isEmpty()) && (y != null && !y.trim().isEmpty());
    }

    /**
     * @param x
     * @param y
     * @return
     */
    private boolean isCoordinateDepthEqual(final String x, final String y) {
        if (x == null || y == null) {
            return false;
        }
        String[] xParts = x.split(":");
        String[] yParts = y.split(":");
        return xParts.length == yParts.length;
    }

    /**
     * @param padding
     * @return
     */
    private boolean isPaddingParseable(final String padding) {
        if (padding == null || padding.trim().isEmpty()) {
            return true;
        }
        String[] paddingParts = padding.trim().split("\\s+");
        if (paddingParts.length < 1 || paddingParts.length > 4) {
            return false;
        }
        for (String paddingPart : paddingParts) {
            if (!paddingPart.matches("^auto$|^[+-]?[0-9]+\\.?([0-9]+)?(px|em|ex|%|in|cm|mm|pt|pc)?$")) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param manager
     * @param subnodeContainer
     * @param viewNodes
     */
    public void setNodes(final WorkflowManager manager, final SubNodeContainer subnodeContainer,
            final List<NodeID> viewNodes) {
        m_wfManager = manager;
        m_subNodeContainer = subnodeContainer;
        m_viewNodes = viewNodes;
    }

    /**
     * @return the layoutMap
     */
    public Map<Integer, WizardNodeLayoutInfo> getLayoutMap() {
        Map<Integer, WizardNodeLayoutInfo> layoutMap = new HashMap<Integer, WizardNodeLayoutInfo>();
        for (Entry<Integer, WizardNodeLayoutInfo> layoutEntry : m_layoutMap.entrySet()) {
            int nodeIndex = layoutEntry.getKey();
            layoutMap.put(nodeIndex, layoutEntry.getValue());
        }
        return layoutMap;
    }

}
