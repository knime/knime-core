/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.node.mine.sota.view.SotaDrawingPane;
import org.knime.base.node.mine.sota.view.SotaTreeViewPropsPanel;
import org.knime.core.node.NodeView;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaNodeView extends NodeView<SotaNodeModel> {
    private SotaDrawingPane m_pane;

    private SotaTreeViewPropsPanel m_panel;

    private JPanel m_outerPanel;

    /**
     * Constructor of SotaNodeView. Creates new instance of SotaNodeView with
     * given SotaNodeModel.
     * 
     * @param nodeModel the node model
     */
    public SotaNodeView(final SotaNodeModel nodeModel) {
        super(nodeModel);

        if (nodeModel.getSotaManager().getRoot() != null) {
            // get data model, init view
            initialize(nodeModel);
        }
    }

    private void initialize(final SotaNodeModel nodeModel) {
        m_pane =
                new SotaDrawingPane(nodeModel.getSotaManager().getRoot(),
                        nodeModel.getSotaManager().getInDataContainer(),
                        nodeModel.getSotaManager().getOriginalData(), nodeModel
                                .getSotaManager().isUseHierarchicalFuzzyData(),
                        nodeModel.getSotaManager().getMaxHierarchicalLevel());

        nodeModel.getInHiLiteHandler(0).addHiLiteListener(m_pane);
        m_pane.setHiliteHandler(nodeModel.getInHiLiteHandler(0));

        m_panel = new SotaTreeViewPropsPanel(m_pane);

        getJMenuBar().add(m_pane.createHiLiteMenu());
        getJMenuBar().add(m_panel.createZoomMenu());

        m_outerPanel = new JPanel();
        m_outerPanel.setLayout(new BoxLayout(m_outerPanel, BoxLayout.Y_AXIS));
        m_outerPanel.add(m_panel);
        super.setComponent(m_outerPanel);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        SotaNodeModel node = this.getNodeModel();

        if (m_pane != null) {
            m_pane.setRoot(node.getSotaManager().getRoot());
            m_pane.setData(node.getSotaManager().getInDataContainer());
            m_pane.setOriginalData(node.getSotaManager().getOriginalData());
            m_pane.setMaxHLevel(node.getSotaManager()
                    .getMaxHierarchicalLevel());

            m_pane.modelChanged(true);
            m_panel.modelChanged();
        } else  {
            initialize(node);
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateModel(final Object arg) {
        modelChanged();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }
}
