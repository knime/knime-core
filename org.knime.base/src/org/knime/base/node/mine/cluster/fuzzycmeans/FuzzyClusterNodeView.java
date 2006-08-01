/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   24.03.2005 (cebron): created
 */
package org.knime.base.node.mine.cluster.fuzzycmeans;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;


/**
 * The FuzzyClusterNodeView provides the user with information about the quality
 * of the clustering.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class FuzzyClusterNodeView extends NodeView {
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
     * 
     * @see FuzzyClusterNodeView#modelChanged()
     */
    FuzzyClusterNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        m_model = (FuzzyClusterNodeModel)nodeModel;
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
     * @see NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        if (m_model != null && m_model.getClusterCentres() != null
                && m_model.getWithinClusterVariations() != null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("<html>\n");
            buffer.append("<body>\n");
            buffer.append("<h1>Cluster Centers</h1>");
            buffer.append(printClusterCenters());
            buffer.append("<br>");
            buffer.append("<h1>WithinClusterVariations:</h1>");
            double[] withinclustervars = m_model.getWithinClusterVariations();
            for (int c = 0; c < m_model.getClusterCentres().length; c++) {
                buffer.append("Cluster " + c + ": " + withinclustervars[c]
                        + "<br>");
            }
            buffer.append("<h1>BetweenClusterVariation:</h1>");
            buffer.append(m_model.getBetweenClusterVariation());
            buffer.append("</body>\n");
            buffer.append("</html>\n");
            m_output.setText(buffer.toString());
        }
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        // empty
    }

    /**
     * @see NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        // empty
    }

    /*
     * Prints the cluster centers from the FuzzyClusterNodeModel
     */
    private String printClusterCenters() {
        String temp = new String();
        if (m_model.getClusterCentres() != null) {
            double[][] clustercenters = m_model.getClusterCentres();
            for (int i = 0; i < clustercenters.length; i++) {
                temp += "<h4>Cluster " + i + ":</h4><br>";
                if (m_model.noiseClustering() && i == clustercenters.length - 1) {
                    temp += "Noise Cluster";
                } else {
                    for (int j = 0; j < clustercenters[i].length; j++) {
                        temp += clustercenters[i][j];
                        if (j != clustercenters[i].length - 1) {
                            temp += ",";
                        }
                    }
                }
                temp += "<br>";
            }
        }
        return temp;
    }
}
