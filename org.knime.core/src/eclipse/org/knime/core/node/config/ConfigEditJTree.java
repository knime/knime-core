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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreeModel;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.util.RetriggerableDelayedRunnable;

/**
 * A tree implementation that allows one to overwrite certain node settings
 * using flow variables.
 *
 * <p>This class is not meant for public use.
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class ConfigEditJTree extends JTree {
    /**
     * The minimum visible width the tree would like to have.
     * @since 4.2
     */
    public static final int MINIMUM_ROW_WIDTH
                        = ConfigEditTreeNodePanel.MINIMUM_LABEL_WIDTH
                            + ConfigEditTreeNodePanel.COMBOBOX_WIDTH
                            + 100                   // text label
                            + (3 * (9 * 2));        // inset slop

    // Part of implementing AP-11595 featured, for a moment in time, an attempt to fill the width; that functionality
    //      is enabled or disabled by this flag.
    static final boolean ROW_SHOULD_FILL_WIDTH = true;

    /** Fallback model. */
    private static final ConfigEditTreeModel EMPTY_MODEL = ConfigEditTreeModel.create(new NodeSettings("empty"));


    /** To get the available variables from. */
    private FlowObjectStack m_flowObjectStack;

    /** The maximum width of all rendered key labels */
    private HashMap<Integer, Integer> m_maxLabelWidthPathDepthMap;

    private final int m_childIndentSum;

    // the visible width of this tree (the parent viewport's width)
    private int m_visibleWidth = -1;

    private final AtomicBoolean m_instantiatedTriggerDueToMissedTrain;
    private RetriggerableDelayedRunnable m_modelRefreshTrigger;

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
        final BasicTreeUI treeUI = (BasicTreeUI)getUI();
        m_childIndentSum = (treeUI.getLeftChildIndent() + treeUI.getRightChildIndent());
        final ConfigEditTreeRenderer renderer = new ConfigEditTreeRenderer(this);
        setCellRenderer(renderer);
        setCellEditor(new ConfigEditTreeEditor(this, renderer));
        setRowHeight(renderer.getPreferredSize().height);
        setEditable(true);
        setToolTipText("config tree"); // enable tooltip

        m_instantiatedTriggerDueToMissedTrain = new AtomicBoolean(false);
    }

    /**
     * This should be called providing the tree's parent viewport's width.
     * @param w the width of the parent viewport
     * @since 4.2
     */
    public void setViewportWidth(final int w) {
        m_visibleWidth = w - m_childIndentSum;

        synchronized (m_instantiatedTriggerDueToMissedTrain) {
            final boolean createTrigger;
            if (m_modelRefreshTrigger != null) {
                createTrigger = !m_modelRefreshTrigger.retrigger();
                if (createTrigger) {
                    m_instantiatedTriggerDueToMissedTrain.set(true);
                }
            } else {
                createTrigger = true;
            }

            if (createTrigger) {
                final Runnable r = () -> {
                    SwingUtilities.invokeLater(() -> {
                        getModel().forceModelRefresh(ConfigEditJTree.this);

                        synchronized (m_instantiatedTriggerDueToMissedTrain) {
                            if (!m_instantiatedTriggerDueToMissedTrain.getAndSet(false)) {
                                m_modelRefreshTrigger = null;
                            }
                        }
                    });
                };
                m_modelRefreshTrigger = new RetriggerableDelayedRunnable(r, 200);
                KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(m_modelRefreshTrigger);
            }
        }
    }

    int getVisibleWidth() {
        return m_visibleWidth;
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

        generateWidthDepthMap((ConfigEditTreeModel)newModel);

        super.setModel(newModel);
    }

    /** Expand the tree. */
    public void expandAll() {
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }

    private void generateWidthDepthMap(final ConfigEditTreeModel newTreeModel) {
        final BufferedImage image = new BufferedImage(800, 40, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = image.createGraphics();
        final JLabel donorLabel = new JLabel();

        if (m_maxLabelWidthPathDepthMap == null) {
            m_maxLabelWidthPathDepthMap = new HashMap<>();
        } else {
            m_maxLabelWidthPathDepthMap.clear();
        }

        generateWidthDepthForChild(newTreeModel, newTreeModel.getRoot(), donorLabel.getFont(), donorLabel.getInsets(), g2d);

        g2d.dispose();
    }

    private void generateWidthDepthForChild(final ConfigEditTreeModel newTreeModel,
                                            final ConfigEditTreeModel.ConfigEditTreeNode child,
                                            final Font font, final Insets insets, final Graphics2D g2d) {
        final String wholeLabelText = child.getConfigEntry().getKey();
        final String displayText = ConfigEditTreeNodePanel.displayTextForString(wholeLabelText);
        final FontMetrics fm = g2d.getFontMetrics(font);
        final int textWidth = (int)(fm.stringWidth(displayText) * 1.05) + insets.left + insets.right;
        final Integer depth = Integer.valueOf(newTreeModel.getPathToRoot(child).length);
        final Integer maxWidth = m_maxLabelWidthPathDepthMap.get(depth);

        if ((maxWidth == null) || (textWidth > maxWidth.intValue())) {
            m_maxLabelWidthPathDepthMap.put(depth, Integer.valueOf(textWidth));
        }

        final int childrenCount = child.getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            generateWidthDepthForChild(newTreeModel, (ConfigEditTreeModel.ConfigEditTreeNode)child.getChildAt(i),
                                       font, insets, g2d);
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
