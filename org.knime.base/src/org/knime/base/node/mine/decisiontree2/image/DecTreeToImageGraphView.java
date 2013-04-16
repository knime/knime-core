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
 *   14.11.2011 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.image;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.mine.decisiontree2.image.DecTreeToImageNodeSettings.Scaling;
import org.knime.base.node.mine.decisiontree2.image.DecTreeToImageNodeSettings.UnfoldMethod;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplit;
import org.knime.base.node.mine.decisiontree2.view.DecTreeGraphView;
import org.knime.base.node.mine.decisiontree2.view.DecTreeNodeWidget;
import org.knime.base.node.mine.decisiontree2.view.graph.NodeWidget;
import org.knime.base.node.mine.decisiontree2.view.graph.NodeWidgetFactory;

/**
 * The graph view of the Decision Tree to Image node.
 *
 * @author Heiko Hofer
 */
public class DecTreeToImageGraphView extends DecTreeGraphView {
    private DecTreeToImageNodeWidgetFactory m_factory;
    private DecTreeToImageNodeSettings m_settings;

    /**
     * @param root
     *            The root node of the tree
     * @param colorColumn
     *            The target column of a color handler used to produce the color
     *            retrieved from root.coveredColors(). This parameter can be
     *            null.
     * @param settings the settings object of the Decision Tree to Image node
     */
    public DecTreeToImageGraphView(final DecisionTreeNode root,
            final String colorColumn,
            final DecTreeToImageNodeSettings settings) {
        super(null, colorColumn);
        m_settings = settings;
        if (m_settings.getScaling().equals(Scaling.fixed)) {
            setScaleFactor(m_settings.getScaleFactor());
        }
        setRootNode(root);
        getVisible().clear();
        if (m_settings.getUnfoldMethod().equals(UnfoldMethod.level)) {
            // set all nodes visible to the given level;
            int maxDepth = m_settings.getUnfoldToLevel();
            int depth = 0;
            Set<DecisionTreeNode> parents = new HashSet<DecisionTreeNode>();
            parents.add(root);
            while (!parents.isEmpty()) {
                // set all parents visible
                for (DecisionTreeNode p : parents) {
                    getVisible().put(p, new Rectangle());
                }
                Set<DecisionTreeNode> children = new HashSet<DecisionTreeNode>();
                if (depth < maxDepth) {
                    // collect children
                    for (DecisionTreeNode p : parents) {
                        for (int i = 0; i < p.getChildCount(); i++) {
                            children.add(p.getChildAt(i));
                        }

                    }
                }
                depth++;
                parents.clear();
                parents = children;
            }
        } else {
            // m_settings.getUnfoldMethod().equals(UnfoldMethod.totalCoverage)
            // set all nodes visible with total coverage greater to this
            double minCoverage = m_settings.getUnfoldWithCoverage();
            Set<DecisionTreeNode> parents = new HashSet<DecisionTreeNode>();
            parents.add(root);
            double total = root.getEntireClassCount();
            while (!parents.isEmpty()) {
                // set all parents visible
                for (DecisionTreeNode p : parents) {
                    getVisible().put(p, new Rectangle());
                }
                Set<DecisionTreeNode> children = new HashSet<DecisionTreeNode>();
                // collect children
                for (DecisionTreeNode p : parents) {
                    double coverage = p.getEntireClassCount() / total;
                    if (coverage >= minCoverage) {
                        for (int i = 0; i < p.getChildCount(); i++) {
                            children.add(p.getChildAt(i));
                        }
                    }
                }
                parents.clear();
                parents = children;
            }
        }

        layoutGraph();

        int c = 0;
        float s = 1f;
        float sfit = -1;
        // In special cases more than one scaling iterations is needed, since
        // the graph does not scale linearly with the scale factor. There are
        // elements that do not scale (e.g. borders or spaces).
        while (c < 3 && needsScaling()) {
            s = getOptimalScaleFactor() * s;
            setScaleFactor(s);
            layoutGraph();
            if (getView().getPreferredSize().width < m_settings.getWidth()
              && getView().getPreferredSize().height < m_settings.getHeight()) {
                sfit = s;
            }
            c++;
        }
        // In corner cases the last scaled factor causes the graph to be to big
        // for the image, used the last scale factor where the grapht fits to
        // the image
        if (sfit != s && -1 != sfit) {
            setScaleFactor(sfit);
            layoutGraph();

        }
    }

    private boolean needsScaling() {
        if (!m_settings.getScaling().equals(Scaling.fixed)) {
            int width = getView().getPreferredSize().width;
            // the 10 is a magic number, its reason is, that now every
            // graph has a 5 Pixel empty Border.
            float dx = width - m_settings.getWidth();
            int height = getView().getPreferredSize().height;
            float dy = height - m_settings.getHeight();

            if (m_settings.getScaling().equals(Scaling.fit)) {
                if (dx < 0 && dy < 0) {
                    return dx <= -3 && dy <= -3;
                } else {
                    return dx >= 3 || dy >= 3;
                }
            } else { // m_settings.getScaling().equals(Scaling.shrink)
                return dx >= 3 || dy >= 3;
            }
        } else {
            return false;
        }

    }

    private float getOptimalScaleFactor() {
        if (!m_settings.getScaling().equals(Scaling.fixed)) {

            int width = getView().getPreferredSize().width;
            // the 10 is a magic number, its reason is, that now every
            // graph has a 5 Pixel empty Border.
            float sx = (m_settings.getWidth()) / (float) width;
            int height = getView().getPreferredSize().height;
            float sy = (m_settings.getHeight()) / (float) height;
            float s = Math.min(sx, sy);
            if (m_settings.getScaling().equals(Scaling.fit)
                || (m_settings.getScaling().equals(Scaling.shrink) && s < 1)) {
                return s;
            } else {
                return 1f;
            }
        } else {
            return 1f;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeWidgetFactory<DecisionTreeNode> getNodeWidgetFactory() {
        if (m_factory == null) {
            m_factory = new DecTreeToImageNodeWidgetFactory(this, m_settings);
        }
        return m_factory;
    }

    /**
     * Graph node factory for the Decision Tree To Image node.
     *
     * @author Heiko Hofer
     */
    public static class DecTreeToImageNodeWidgetFactory implements
            NodeWidgetFactory<DecisionTreeNode> {

        private DecTreeGraphView m_graph;
        private DecTreeToImageNodeSettings m_settings;

        /**
         * Creates a new instance.
         *
         * @param graph
         *            the graph
         * @param settings the settings object of the Decision Tree to Image
         * node
         */
        public DecTreeToImageNodeWidgetFactory(final DecTreeGraphView graph,
                final DecTreeToImageNodeSettings settings) {
            m_graph = graph;
            m_settings = settings;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NodeWidget<DecisionTreeNode> createGraphNode(
                final DecisionTreeNode object) {
            return new DecTreeNodeWidget(m_graph, object,
                    m_graph.getColorColumn(), false, false,
                    m_settings.getDisplayTable(),
                    m_settings.getDisplayChart());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<DecisionTreeNode> getChildren(
                final DecisionTreeNode object) {
            if (!object.isLeaf()) {
                return Arrays.asList(((DecisionTreeNodeSplit) object)
                        .getChildren());
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLeaf(final DecisionTreeNode object) {
            return object.isLeaf();
        }

    }

}
