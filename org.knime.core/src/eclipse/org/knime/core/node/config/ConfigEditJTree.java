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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 29, 2008 (wiswedel): created
 */
package org.knime.core.node.config;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowObjectStack;

/**
 * A tree implementation that allows one to overwrite certain node settings
 * using flow variables.
 *
 * <p>This class is not meant for public use.
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class ConfigEditJTree extends JTree {
    // Part of implementing AP-11595 featured, for a moment in time, an attempt to fill the width; that functionality
    //      is enabled or disabled by this flag.
    static final boolean ROW_SHOULD_FILL_WIDTH = false;

    /** Fallback model. */
    private static final ConfigEditTreeModel EMPTY_MODEL = ConfigEditTreeModel.create(new NodeSettings("empty"));


    /** To get the available variables from. */
    private FlowObjectStack m_flowObjectStack;

    /** The maximum width of all rendered key labels */
    private final HashMap<Integer, Integer> m_maxLabelWidthPathDepthMap;
    /** A holder for the currently running, if any, model forced refresh timer */
    private final List<Runnable> m_forcedModelRefreshTimer = new ArrayList<>();

    /** Constructor for empty tree. */
    public ConfigEditJTree() {
        this(EMPTY_MODEL);
    }

    /**
     * Shows given tree model.
     *
     * @param model The model to show.
     */
    public ConfigEditJTree(final ConfigEditTreeModel model) {
        super(model);
        setRootVisible(false);
        setShowsRootHandles(true);
        final ConfigEditTreeRenderer renderer = new ConfigEditTreeRenderer(this);
        setCellRenderer(renderer);
        setCellEditor(new ConfigEditTreeEditor(this, renderer));
        setRowHeight(renderer.getPreferredSize().height);
        setEditable(true);
        setToolTipText("config tree"); // enable tooltip
        m_maxLabelWidthPathDepthMap = new HashMap<>();
    }

    /**
     * Overwritten to fail on model implementations which are not of class {@link ConfigEditTreeModel}.
     *
     * {@inheritDoc}
     */
    @Override
    public void setModel(final TreeModel newModel) {
        if (!(newModel instanceof ConfigEditTreeModel)) {
            throw new IllegalArgumentException("Argument must be of class " + ConfigEditTreeModel.class.getSimpleName());
        }

        super.setModel(newModel);
    }

    /** Expand the tree. */
    public void expandAll() {
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }

    /**
     * This method will only ever be called from EDT during pai.
     *
     * @param depth the tree depth of the row with the label
     * @param width the width of the {@code JLabel} component that has been rendered in
     *            {@link ConfigEditTreeRenderer#paintComponent(java.awt.Graphics)}
     */
    void renderedKeyLabelAtDepthWithWidth(final int depth, final int width) {
        final Integer key = Integer.valueOf(depth);
        final Integer labelWidth = m_maxLabelWidthPathDepthMap.get(key);
        final boolean needsForcedRefresh = (labelWidth == null) || (width > labelWidth.intValue());

        if (needsForcedRefresh) {
            m_maxLabelWidthPathDepthMap.put(key, Integer.valueOf(width));

            synchronized (m_forcedModelRefreshTimer) {
                if (m_forcedModelRefreshTimer.size() == 0) {
                    final ForcedModelRefreshTrigger trigger = new ForcedModelRefreshTrigger();
                    m_forcedModelRefreshTimer.add(trigger);
                    (new Thread(trigger)).start();
                } else {
                    ((ForcedModelRefreshTrigger)m_forcedModelRefreshTimer.get(0)).retriggerTimer();
                }
            }
        }
    }

    /**
     * @param depth the tree depth of the row with the label
     * @return the width which a key label being rendered should be set to, or -1 if the max width hasn't been defined
     */
    int labelWidthToEnforceForDepth(final int depth) {
        final Integer key = Integer.valueOf(depth);
        final Integer labelWidth = m_maxLabelWidthPathDepthMap.get(key);

        return (labelWidth != null) ? labelWidth.intValue() : -1;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigEditTreeModel getModel() {
        return (ConfigEditTreeModel)super.getModel();
    }

    /** @param foStack the flow object stack to set */
    public void setFlowObjectStack(final FlowObjectStack foStack) {
        m_flowObjectStack = foStack;
    }

    /** @return the flow object Stack */
    public FlowObjectStack getFlowObjectStack() {
        return m_flowObjectStack;
    }


    /*
     * This forces the model to be told that all of its nodes have changed, which in turn repaints them. The reason
     *  a simple {@link #repaint()} invocation fails is that if a row's content is out of the viewport, but at
     *  whose paint-time, its clip rectangle was computed to be what is now too small (because a row after it
     *  had a pixel-wider label string and so the subject row's label size grew) the repainter will deem the
     *  out-of-viewport-bounds not dirtied.)
     */
    private class ForcedModelRefreshTrigger implements Runnable {
        private final AtomicBoolean m_retrigger;

        private ForcedModelRefreshTrigger() {
            m_retrigger = new AtomicBoolean(false);
        }

        private void retriggerTimer() {
            m_retrigger.set(true);
        }

        @Override
        public void run() {
            boolean sleep = true;

            while (sleep) {
                try {
                    Thread.sleep(80);
                } catch (final Exception e) { } // NOPMD

                sleep = m_retrigger.getAndSet(false);
            }

            getModel().forceModelRefresh(ConfigEditJTree.this);

            synchronized (m_forcedModelRefreshTimer) {
                m_forcedModelRefreshTimer.remove(0);
            }
        }
    }

    /**
     * Public testing method that displays a simple tree with no flow variable stack, though.
     *
     * @param args command line args, ignored here.
     */
    public static void main(final String[] args) {
        NodeSettings settings = new NodeSettings("Demo");
        settings.addString("String_Demo", "This is a demo string");
        settings.addInt("Int_Demo", 32);
        settings.addDoubleArray("DoubleArray_Demo", new double[]{3.2, 97.4});
        NodeSettingsWO sub = settings.addNodeSettings("SubElement_Demo");
        sub.addString("String_Demo", "Yet another string");
        sub.addString("String_Demo2", "One more");
        JFrame frame = new JFrame("Tree View Demo");
        Container content = frame.getContentPane();
        ConfigEditTreeModel treeModel = ConfigEditTreeModel.create(settings);
        ConfigEditJTree tree = new ConfigEditJTree(treeModel);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(tree), BorderLayout.CENTER);
        content.add(p);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
