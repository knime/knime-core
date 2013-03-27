/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
