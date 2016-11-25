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
 *   04.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.view.classification;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.knime.base.node.mine.decisiontree2.view.CollapsiblePanel;
import org.knime.base.node.mine.decisiontree2.view.graph.HierarchicalGraphView;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeColumnCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.node.view.RandomForestAbstractTreeNodeWidget;
import org.knime.core.data.property.ColorAttr;

/**
 *
 * @author Adrian Nembach
 */
public class RandomForestClassificationTreeNodeWidget extends RandomForestAbstractTreeNodeWidget {

    private boolean m_displayTable;
    private boolean m_displayChart;
    private boolean m_tableCollapsed;
    private boolean m_chartCollapsed;
    private CollapsiblePanel m_table;
    private CollapsiblePanel m_chart;
    private String m_colorColumn;

    /**
     * @param graph
     * @param treeNode
     */
    public RandomForestClassificationTreeNodeWidget(final HierarchicalGraphView<AbstractTreeNode> graph,
        final TreeNodeClassification treeNode, final boolean displayTable, final boolean displayChart, final boolean tableCollapsed, final boolean chartCollapsed) {
        super(graph, treeNode);
        m_displayTable = displayTable;
        m_displayChart = displayChart;
        m_tableCollapsed = tableCollapsed;
        m_chartCollapsed = chartCollapsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent createComponent() {
        float scale = getScaleFactor();
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;

        JPanel nodePanel = createNodeLabelPanel(scale);
        nodePanel.setOpaque(false);
        p.add(nodePanel, c);

     // add table
        if (m_displayTable) {
            c.gridy++;
            JPanel tablePanel = createTablePanel(scale);
            tablePanel.setOpaque(false);
            m_table = new CollapsiblePanel("Table:", tablePanel, scale);
            m_table.setOpaque(false);
            m_table.setCollapsed(m_tableCollapsed);
            p.add(m_table, c);
        }

        return p;
    }

    @Override
    public void setScaleFactor(final float scale) {
        // remember if table is collapsed
        if (null != m_table) {
            m_tableCollapsed = m_table.isCollapsed();
        }
        // remember if chart is collapsed
        if (null != m_chart) {
            m_chartCollapsed = m_chart.isCollapsed();
        }
        super.setScaleFactor(scale);
    }


    private JPanel createTablePanel(final float scale) {
        TreeNodeClassification node = (TreeNodeClassification)getUserObject();
        final float[] targetDistribution = node.getTargetDistribution();
        double totalClassCount = 0.0;
        for (double classCount : targetDistribution) {
            totalClassCount += classCount;
        }
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        int gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        int bw = Math.round(1 * scale);
        c.insets = new Insets(bw, bw, bw, bw);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        p.add(scaledLabel("Category", scale), c);
        c.gridx++;
        p.add(scaledLabel("% ", scale, SwingConstants.RIGHT), c);
        c.gridx++;
        p.add(scaledLabel("n ", scale, SwingConstants.RIGHT), c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(new MyJSeparator(), c);
        c.gridwidth = 1;

        int majorityClassIndex = node.getMajorityClassIndex();

        NominalValueRepresentation[] classNomVals = node.getTargetMetaData().getValues();

        List<Double> classFreqList = new ArrayList<Double>();
        for (int i = 0; i < targetDistribution.length; i++) {
            JLabel classLabel = scaledLabel(classNomVals[i].getNominalValue(), scale);
            c.gridy++;
            c.gridx = 0;
            p.add(classLabel, c);
            c.gridx++;
            double classFreq = targetDistribution[i] / totalClassCount;
            classFreqList.add(classFreq);
            p.add(scaledLabel(convertPercentage(classFreq), scale,
                    SwingConstants.RIGHT), c);
            c.gridx++;
            final Float classCountValue = targetDistribution[i];
            p.add(scaledLabel(convertCount(classCountValue), scale,
                    SwingConstants.RIGHT), c);
            if (i == majorityClassIndex) {
                c.gridx = 0;
                JComponent comp = new JPanel();
                comp.setMinimumSize(classLabel.getPreferredSize());
                comp.setPreferredSize(classLabel.getPreferredSize());
                comp.setBackground(new Color(225, 225, 225));
                c.gridwidth = gridwidth;
                p.add(comp, c);
                c.gridwidth = 1;
            }
        }
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = gridwidth;
        p.add(new MyJSeparator(), c);
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        p.add(scaledLabel("Total", scale), c);
        c.gridx++;
        double nominator = 0.0;
        TreeNodeClassification root = (TreeNodeClassification)getGraphView().getRootNode();
        if (root != null) {
            final float[] rootTargetDistribution = root.getTargetDistribution();
            double rootTotalClassCount = 0.0;
            for (double classCount : rootTargetDistribution) {
                rootTotalClassCount += classCount;
            }
            nominator = rootTotalClassCount;
        } else {
            nominator = totalClassCount;
        }
        double coverage = totalClassCount / nominator;
        p.add(scaledLabel(convertPercentage(coverage), scale, SwingConstants.RIGHT), c);
        c.gridx++;
        p.add(scaledLabel(convertCount(totalClassCount), scale,
                SwingConstants.RIGHT), c);
        return p;
    }


    /** The panel at the top displaying the node label.
     * @param scale
     * @return A label, e.g. "Iris-versicolor (45/46)" */
    private JPanel createNodeLabelPanel(final float scale) {
        int gap = Math.round(5 * scale);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, gap, gap));
        StringBuilder label = new StringBuilder();

        TreeNodeClassification node = (TreeNodeClassification)getUserObject();
        String majorityClass = node.getMajorityClassName();
        final float[] targetDistribution = node.getTargetDistribution();
        double majorityClassCount = targetDistribution[node.getMajorityClassIndex()];
        boolean displayTargetDistribution = false;
        double totalClassCount = 0;
        for (double classCount : targetDistribution) {
            totalClassCount += classCount;
            if (!displayTargetDistribution && classCount != 0.0 && classCount != 1.0) {
                displayTargetDistribution = true;
            }
        }

        label.append(majorityClass);
        // display target distribution only if available (not 1/1)
        if (displayTargetDistribution) {
            label.append(" (").append(convertCount(majorityClassCount));
            label.append("/").append(convertCount(totalClassCount));
            label.append(")");
        }

        p.add(scaledLabel(label.toString(), scale));
        return p;
    }

    private JLabel scaledLabel(final String label, final float scale) {
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(l.getFont().getSize() * scale));
        return l;
    }

    private JLabel scaledLabel(final String label, final float scale,
        final int horizontalAlignment) {
    JLabel l = new JLabel(label, horizontalAlignment);
    l.setFont(l.getFont().deriveFont(l.getFont().getSize() * scale));
    return l;
}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectorLabelAbove() {
        TreeNodeClassification node = (TreeNodeClassification)getUserObject();
        if (node != null && !node.getSignature().equals(TreeNodeSignature.ROOT_SIGNATURE)) {
            TreeNodeCondition condition = node.getCondition();
            if (condition instanceof TreeNodeSurrogateCondition) {
                condition = ((TreeNodeSurrogateCondition)condition).getFirstCondition();
            }
            if (condition instanceof TreeNodeColumnCondition) {
                TreeNodeColumnCondition colCondition = (TreeNodeColumnCondition) condition;
                return colCondition.toString().substring(colCondition.getAttributeName().length() + 1);
            }
            return condition.toString();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectorLabelBelow() {
        TreeNodeClassification node = (TreeNodeClassification)getUserObject();
        if (node.getNrChildren() != 0) {
            TreeNodeClassification child = node.getChild(0);
            TreeNodeCondition childCondition = child.getCondition();
            if (childCondition instanceof TreeNodeColumnCondition) {
                return ((TreeNodeColumnCondition)childCondition).getAttributeName();
            } else if (childCondition instanceof TreeNodeSurrogateCondition) {
                TreeNodeSurrogateCondition surrogateCondition = (TreeNodeSurrogateCondition) childCondition;
                TreeNodeCondition headCondition = surrogateCondition.getFirstCondition();
                if (headCondition instanceof TreeNodeColumnCondition) {
                    return ((TreeNodeColumnCondition)headCondition).getAttributeName();
                }
            }
        }
        return null;
    }


    private static final DecimalFormat DECIMAL_FORMAT_ONE = initFormat("0.0");
    private static final DecimalFormat DECIMAL_FORMAT = initFormat("0");

    private static DecimalFormat initFormat(final String pattern) {
        DecimalFormat df = new DecimalFormat(pattern);
        df.setGroupingUsed(true);
        df.setGroupingSize(3);
        return df;
    }

    private static String convertCount(final double value) {
        // show integer as integer (without decimal places)
        if (Double.compare(Math.ceil(value), value) == 0) {
            return DECIMAL_FORMAT.format(value);
        }
        return DECIMAL_FORMAT_ONE.format(value);
    }

    private static String convertPercentage(final double value) {
        return DECIMAL_FORMAT_ONE.format(100.0 * value);
    }


    private static class MyJSeparator extends JComponent {
        private static final long serialVersionUID = -5611048590057773103L;

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2, 2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            Paint origPaint = g2.getPaint();
            g2.setPaint(ColorAttr.BORDER);
            Dimension s = getSize();
            g2.drawLine(0, s.height / 2, s.width, s.height / 2);
            g2.setPaint(origPaint);
        }
    }
}
