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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   10.11.2011 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.view.DecTreeGraphView;
import org.knime.base.node.mine.decisiontree2.view.graph.CollapseBranchAction;
import org.knime.base.node.mine.decisiontree2.view.graph.ExpandBranchAction;
import org.knime.core.data.image.ImageContent;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * The view of the Decision Tree to Image node.
 *
 * @author Heiko Hofer
 */
final class DecTreeToImageView extends
        NodeView<DecTreeToImageNodeModel> implements HiLiteListener {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecTreeToImageView.class);

    private DecTreeGraphView m_graph;
    private JPopupMenu m_popup;


    /**
     * Default constructor, taking the model as argument.
     *
     * @param model
     *            the underlying NodeModel
     */
    public DecTreeToImageView(final DecTreeToImageNodeModel model) {
        super(model);
        JTabbedPane tabbedPane = new JTabbedPane();
        // add Tab with static view
        JScrollPane jsp = new JScrollPane(new ImageComponent(model));
        tabbedPane.addTab(model.getImageDescription(), jsp);



        DecisionTreeNode root = null != model.getDecisionTree() ? model
                .getDecisionTree().getRootNode() : null;
        String colorColumn = null != model.getDecisionTree() ? model
                .getDecisionTree().getColorColumn() : null;
        m_graph = new DecTreeGraphView(root, colorColumn);
        JPanel p = new JPanel(new GridLayout());
        p.setBackground(ColorAttr.BACKGROUND);
        p.add(m_graph.getView());
        p.setBorder(new EmptyBorder(5, 5, 5, 5));
        JScrollPane treeView = new JScrollPane(p);
        Dimension prefSize = treeView.getPreferredSize();
        treeView.setPreferredSize(new Dimension(Math.min(prefSize.width, 800),
                prefSize.height));

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(1.0);
        splitPane.setLeftComponent(treeView);
        splitPane.setRightComponent(createRightPanel());

        // add second tab with dynamic view
        tabbedPane.addTab("Decision Tree View", splitPane);

        setComponent(tabbedPane);

        // add menu entries for tree operations
        this.getJMenuBar().add(createTreeMenu());

        m_popup = new JPopupMenu();
        m_popup.add(new ExpandBranchAction<DecisionTreeNode>(m_graph));
        m_popup.add(new CollapseBranchAction<DecisionTreeNode>(m_graph));

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
        final Map<Object, Float> scaleFactors = new LinkedHashMap<Object, Float>();
        scaleFactors.put("140.0%", 140f);
        scaleFactors.put("120.0%", 120f);
        scaleFactors.put("100.0%", 100f);
        scaleFactors.put("80.0%", 80f);
        scaleFactors.put("60.0%", 60f);

        final JComboBox scaleFactorComboBox = new JComboBox(scaleFactors
                .keySet().toArray());
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
        DecTreeToImageNodeModel model = this.getNodeModel();
        if (model != null) {
            DecisionTree dt = model.getDecisionTree();
            if (dt != null) {
                m_graph.setColorColumn(model.getDecisionTree().getColorColumn());
                m_graph.setRootNode(dt.getRootNode());
            } else {
                m_graph.setColorColumn(null);
                m_graph.setRootNode(null);
            }
        }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        // no data input
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        // no data input
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteAll(final KeyEvent event) {
        // no data input
    }

    /**
     * A component drawing a single image.
     *
     * @author Heiko Hofer
     */
    @SuppressWarnings("serial")
	private static class ImageComponent extends JComponent {
        DecTreeToImageNodeModel m_model;

        /**
         * @param model
         */
        public ImageComponent(final DecTreeToImageNodeModel model) {
            m_model = model;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            ImageContent image = m_model.getImage();
            if (null != image) {
                Rectangle r = getBounds();
                Color orig = g.getColor();
                g.setColor(ColorAttr.BACKGROUND);
                g.fillRect(r.x, r.y, r.width, r.height);
                Dimension dim = image.getPreferredSize();
                image.paint((Graphics2D)g, dim.width, dim.height);
                g.setColor(orig);
            }
        }

        /**
         *; {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize() {
            ImageContent image = m_model.getImage();
            if (null != image) {
                return image.getPreferredSize();
            } else {
                return new Dimension(20,20);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isOpaque() {
            return super.isOpaque();
        }


    }
}
