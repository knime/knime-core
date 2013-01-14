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
 *   24.03.2005 (cebron): created
 */
package org.knime.base.node.mine.cluster.fuzzycmeans;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.NodeView;

/**
 * The FuzzyClusterNodeView provides the user with information about the quality
 * of the clustering.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class FuzzyClusterNodeView extends
        NodeView<FuzzyClusterNodeModel> {
    /*
     * The underlying FuzzyClusterNodeModel
     */
    private FuzzyClusterNodeModel m_model;

    /*
     * Output is printed in a JTExtArea
     */
    private JEditorPane m_output;

    /**
     * Creates a new view.
     * @param nodeModel the underlying NodeModel
     * @see FuzzyClusterNodeView#modelChanged()
     */
    FuzzyClusterNodeView(final FuzzyClusterNodeModel nodeModel) {
        super(nodeModel);
        m_model = nodeModel;
        m_output = new JEditorPane("text/html", "");
        m_output.setEditable(false);
        JScrollPane scroller = new JScrollPane(m_output);
        setComponent(scroller);
    }

    /**
     * Updates the view with the following values.
     * <ul>
     * <li>Cluster centers</li>
     * <li>Within-Cluster Variation</li>
     * <li>Between Cluster Variation</li>
     * <li>Clustering Quality (BCV / WCV)</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        if (m_model != null && m_model.getClusterCentres() != null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("<html>\n");
            buffer.append("<body>\n");
            buffer.append("<h1>Cluster Centers</h1>");
            buffer.append(printClusterCenters());
            buffer.append("<br>");

            double[] withinclustervars = m_model.getWithinClusterVariations();
            if (withinclustervars != null && withinclustervars.length > 0) {
                buffer.append("<h1>WithinClusterVariations:</h1>");
                for (int c = 0; c < m_model.getClusterCentres().length; c++) {
                    buffer.append("Cluster " + c + ": " + withinclustervars[c]
                            + "<br>");
                }
            }
            double betweenclusterVar = m_model.getBetweenClusterVariation();
            if (!Double.isNaN(betweenclusterVar)) {
                buffer.append("<h1>BetweenClusterVariation:</h1>");
                buffer.append(betweenclusterVar);
                buffer.append("<br>");
            }
            double partCoefficient = m_model.getPartitionCoefficient();
            if (!Double.isNaN(partCoefficient)) {
                buffer.append("<h1>Partition Coefficient:</h1>");
                buffer.append(partCoefficient);
                buffer.append("<br>");
            }
            double partEntropy = m_model.getPartitionEntropy();
            if (!Double.isNaN(partEntropy)) {
                buffer.append("<h1>Partition Entropy:</h1>");
                buffer.append(partEntropy);
                buffer.append("<br>");
            }

            double xiebeni = m_model.getXieBeniIndex();
            if (!Double.isNaN(xiebeni)) {
                buffer.append("<h1>XieBeni Index:</h1>");
                buffer.append(xiebeni);
                buffer.append("<br>");
            }

            double[] fHyperVols = m_model.getFuzzyHyperVolumes();
            if (fHyperVols != null) {
                buffer.append("<h1>Fuzzy Hypervolumes:</h1>");
                for (int c = 0; c < fHyperVols.length; c++) {
                    buffer.append("Cluster " + c + ": " + fHyperVols[c]
                            + "<br>");
                }
            }

            buffer.append("</body>\n");
            buffer.append("</html>\n");
            m_output.setText(buffer.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // empty
    }

    /*
     * Prints the cluster centers from the FuzzyClusterNodeModel
     */
    private String printClusterCenters() {
        StringBuilder temp = new StringBuilder();
        if (m_model.getClusterCentres() != null) {
            double[][] clustercenters = m_model.getClusterCentres();
            for (int i = 0; i < clustercenters.length; i++) {
                temp.append("<h4>Cluster " + i + ":</h4><br>");
                if (m_model.noiseClustering()
                        && i == clustercenters.length - 1) {
                    temp.append("Noise Cluster");
                } else {
                    for (int j = 0; j < clustercenters[i].length; j++) {
                        temp.append(clustercenters[i][j]);
                        if (j != clustercenters[i].length - 1) {
                            temp.append(",");
                        }
                    }
                }
                temp.append("<br>");
            }
        }
        return temp.toString();
    }
}
