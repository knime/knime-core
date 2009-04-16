/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * KNIME.com, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.04.2009 (ohl): created
 */
package org.knime.core.node.workflow;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JTabbedPane;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 *
 * @author ohl, KNIME.com GmbH Zurich, Switzerland
 */
public class NodeExecutionJobManagerView extends
        NodeView<NodeModel> {

    final List<NodeExecutionJobManagerViewPanel> m_panels =
            new LinkedList<NodeExecutionJobManagerViewPanel>();

    NodeExecutionJobManagerView(final NodeExecutionJobManager jobManager,
            final NodeContainer nc,
            final NodeExecutionJobManagerBlankModel model) {
        super(model);

        // it's up to the job manager to decide when to show something
        setShowNODATALabel(false);

        // add all panels of the job manager
        JTabbedPane tabs = new JTabbedPane();
        for (int i = 0; i < jobManager.getNumberOfViews(); i++) {
            NodeExecutionJobManagerViewPanel p = jobManager.getViewPanel(i, nc);
            String title = jobManager.getViewPanelName(i, nc);
            tabs.add(title, p);
            m_panels.add(p);
        }

        setComponent(tabs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        for (NodeExecutionJobManagerViewPanel p : m_panels) {
            p.onClose();
        }
        m_panels.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        for (NodeExecutionJobManagerViewPanel p : m_panels) {
            p.onOpen();
        }
    }

    void reset() {
        for (NodeExecutionJobManagerViewPanel p : m_panels) {
            p.reset();
        }
    }
}
