/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   28.07.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.learner2;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.view.DecTreeGraphView;
import org.knime.base.node.mine.decisiontree2.view.graph.CollapseBranchAction;
import org.knime.base.node.mine.decisiontree2.view.graph.ExpandBranchAction;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 *
 * The graph view displayed the view of the Decision Tree to Image node.
 *
 * @author Heiko HOfer
 * 
 * @since 2.6
 */
final class DecTreeLearnerGraphView2 extends
        NodeView<DecisionTreeLearnerNodeModel2> implements HiLiteListener {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecTreeLearnerGraphView2.class);
    private DecTreeGraphView m_graph;

    private HiLiteHandler m_hiLiteHdl;

    private JMenu m_hiLiteMenu;

    private JPopupMenu m_popup;

    /**
     * Default constructor, taking the model as argument.
     *
     * @param model the underlying NodeModel
     */
    public DecTreeLearnerGraphView2(final DecisionTreeLearnerNodeModel2 model) {
        super(model);
        DecisionTreeNode root =
                null != model.getDecisionTree() ? model.getDecisionTree()
                        .getRootNode() : null;
        String colorColumn =
            null != model.getDecisionTree() ? model.getDecisionTree()
                    .getColorColumn() : null;
        m_graph = new DecTreeGraphView(root, colorColumn);
        JPanel p = new JPanel(new GridLayout());
        p.setBackground(ColorAttr.BACKGROUND);
        p.add(m_graph.getView());
        p.setBorder(new EmptyBorder(5, 5, 5, 5));
        JScrollPane treeView = new JScrollPane(p);
        Dimension prefSize = treeView.getPreferredSize();
        treeView.setPreferredSize(
                new Dimension(Math.min(prefSize.width, 800), prefSize.height));

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(1.0);
        splitPane.setLeftComponent(treeView);
        splitPane.setRightComponent(createRightPanel());

        setComponent(splitPane);

        // retrieve HiLiteHandler from Input port
        m_hiLiteHdl = model.getInHiLiteHandler(
                DecisionTreeLearnerNodeModel2.DATA_INPORT);
        // and add menu entries for HiLite-ing
        m_hiLiteMenu = this.createHiLiteMenu();
        this.getJMenuBar().add(m_hiLiteMenu);
        m_hiLiteMenu.setEnabled(m_hiLiteHdl != null);
        m_hiLiteHdl.addHiLiteListener(this);
        // add menu entries for tree operations
        this.getJMenuBar().add(createTreeMenu());

        m_popup = new JPopupMenu();
        JMenuItem hiliteMenu = createHiliteItem();
        hiliteMenu.setText("HiLite Branch");
        m_popup.add(hiliteMenu);
        JMenuItem unHiliteMenu = createUnHiliteItem();
        unHiliteMenu.setText("UnHiLite Branch");
        m_popup.add(unHiliteMenu);
        m_popup.add(createClearHiliteItem());
        m_popup.add(new JSeparator());
        m_popup.add(new ExpandBranchAction<DecisionTreeNode>(m_graph));
        m_popup.add(new CollapseBranchAction<DecisionTreeNode>(m_graph));

        recreateHiLite();

        m_graph.getView().addMouseListener(new MouseAdapter() {
            private void showPopup(final MouseEvent e) {
                DecisionTreeNode node = m_graph.nodeAtPoint(e.getPoint());
                if (null != node) {
                    m_popup.show(m_graph.getView(), e.getX(), e.getY());
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Container getExportComponent() {
        return m_graph.getView();
    }

    /* Create the Panel with the outline view and the controls */
    private JPanel createRightPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.white);
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 6, 4, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;

        p.add(m_graph.createOutlineView(), c);

        c.weighty = 0;
        c.gridy++;
        p.add(new JLabel("Zoom:"), c);


        c.gridy++;
        final Map<Object, Float> scaleFactors =
                new LinkedHashMap<Object, Float>();
        scaleFactors.put("140.0%", 140f);
        scaleFactors.put("120.0%", 120f);
        scaleFactors.put("100.0%", 100f);
        scaleFactors.put("80.0%", 80f);
        scaleFactors.put("60.0%", 60f);

        final JComboBox scaleFactorComboBox = new JComboBox(
                scaleFactors.keySet().toArray());
        scaleFactorComboBox.setEditable(true);
        scaleFactorComboBox.setSelectedItem("100.0%");
        scaleFactorComboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                Object selected = scaleFactorComboBox.getSelectedItem();
                Float scaleFactor = scaleFactors.get(selected);
                if (null == scaleFactor) {
                    String str = ((String)selected).trim();
                    if (str.endsWith("%")) {
                        scaleFactor = Float.parseFloat(
                                str.substring(0, str.length() - 1));
                    } else {
                        scaleFactor = Float.parseFloat(str);
                    }
                }
                if (scaleFactor < 10) {
                    LOGGER.error("A zoom which is lower than 10% "
                            + "is not supported");
                    scaleFactor = 10f;
                }
                if (scaleFactor > 500) {
                    LOGGER.error("A zoom which is greater than 500% "
                            + "is not supported");
                    scaleFactor = 500f;
                }
                String sf = Float.toString(scaleFactor) + "%";
                scaleFactorComboBox.setSelectedItem(sf);
                scaleFactor = scaleFactor / 100f;
                m_graph.setScaleFactor(scaleFactor);
                getComponent().validate();
                getComponent().repaint();
            }
        });
        p.add(scaleFactorComboBox, c);
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        DecisionTreeLearnerNodeModel2 model = this.getNodeModel();
        if (model != null) {
            m_hiLiteHdl.removeHiLiteListener(this);
            DecisionTree dt = model.getDecisionTree();
            if (dt != null) {
                m_graph.setColorColumn(
                        model.getDecisionTree().getColorColumn());
                m_graph.setRootNode(dt.getRootNode());

                // retrieve HiLiteHandler from Input port
                m_hiLiteHdl = model.getInHiLiteHandler(
                                DecisionTreeLearnerNodeModel2.DATA_INPORT);
                // and adjust menu entries for HiLite-ing
                m_hiLiteMenu.setEnabled(m_hiLiteHdl != null);
                m_hiLiteHdl.addHiLiteListener(this);
                recreateHiLite();
            } else {
                m_graph.setColorColumn(null);
                m_graph.setRootNode(null);
            }
        }
    }

    private void updateHiLite(final boolean state) {
        DecisionTreeNode selected = m_graph.getSelected();
        Set<RowKey> covPat = new HashSet<RowKey>();
        covPat.addAll(selected.coveredPattern());
        if (state) {
            m_hiLiteHdl.fireHiLiteEvent(covPat);
        } else {
            m_hiLiteHdl.fireUnHiLiteEvent(covPat);
        }
    }

    /**
     * Create menu to control hiliting.
     *
     * @return A new JMenu with hiliting buttons
     */
    private JMenu createHiLiteMenu() {
        final JMenu result = new JMenu(HiLiteHandler.HILITE);
        result.setMnemonic('H');

        result.add(createHiliteItem());
        result.add(createUnHiliteItem());
        result.add(createClearHiliteItem());

        return result;
    }

    /**
     * Create menu to control tree.
     *
     * @return A new JMenu with tree operation buttons
     */
    private JMenu createTreeMenu() {
        final JMenu result = new JMenu("Tree");
        result.setMnemonic('T');

        Action expand = new ExpandBranchAction<DecisionTreeNode>(m_graph);
        expand.putValue(Action.NAME, "Expand Selected Branch");
        Action collapse = new CollapseBranchAction<DecisionTreeNode>(m_graph);
        collapse.putValue(Action.NAME, "Collapse Selected Branch");
        result.add(expand);
        result.add(collapse);

        return result;
    }

    private JMenuItem createHiliteItem() {
        JMenuItem item =
            new JMenuItem(HiLiteHandler.HILITE_SELECTED + " Branch");
        item.setMnemonic('S');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                updateHiLite(true);
            }
        });
        return item;
    }

    private JMenuItem createUnHiliteItem() {
        JMenuItem item = new JMenuItem(
                HiLiteHandler.UNHILITE_SELECTED + " Branch");
        item.setMnemonic('U');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                updateHiLite(false);
            }
        });
        return item;
    }

    private JMenuItem createClearHiliteItem() {
        JMenuItem item = new JMenuItem(HiLiteHandler.CLEAR_HILITE);
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                //m_graph.clearHilite();
                m_hiLiteHdl.fireClearHiLiteEvent();
            }
        });
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        if (!event.isEmpty()) {
            recreateHiLite();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        if (!event.isEmpty()) {
            recreateHiLite();
        }
    }

    private void recreateHiLite() {
        Set<RowKey> hilited = m_hiLiteHdl.getHiLitKeys();
        Set<DecisionTreeNode> toHilite = new HashSet<DecisionTreeNode>();
        DecisionTreeNode root = m_graph.getRootNode();

        List<DecisionTreeNode> toProcess = new LinkedList<DecisionTreeNode>();
        if (null != root) {
            toProcess.add(0, root);
        }
        // Traverse the tree breadth first
        while (!toProcess.isEmpty()) {
            DecisionTreeNode curr = toProcess.remove(0);
            // bug 2695: if not all pattern are selected for hilting, the
            // view will automatically hilite all branches that does not
            // cover any pattern
            if (curr.coveredPattern().isEmpty()) {
                continue;
            }
            if (hilited.containsAll(curr.coveredPattern())) {
                // hilite subtree starting from curr
                toHilite.addAll(getSubtree(curr));
            } else {
                for (int i = 0; i < curr.getChildCount(); i++) {
                    toProcess.add(0, curr.getChildAt(i));
                }
            }
        }
        m_graph.hiLite(toHilite);
    }

    private List<DecisionTreeNode> getSubtree(final DecisionTreeNode node) {
        List<DecisionTreeNode> subTree = new ArrayList<DecisionTreeNode>();
        List<DecisionTreeNode> toProcess = new LinkedList<DecisionTreeNode>();
        toProcess.add(0, node);
        // Traverse the tree breadth first
        while (!toProcess.isEmpty()) {
            DecisionTreeNode curr = toProcess.remove(0);
            subTree.add(curr);
            for (int i = 0; i < curr.getChildCount(); i++) {
                toProcess.add(0, curr.getChildAt(i));
            }
        }
        return subTree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteAll(final KeyEvent event) {
        m_graph.clearHilite();
    }
}
