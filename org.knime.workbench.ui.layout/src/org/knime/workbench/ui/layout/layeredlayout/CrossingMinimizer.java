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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created: 29.03.2011
 * Author: mader
 */
package org.knime.workbench.ui.layout.layeredlayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.knime.workbench.ui.layout.Graph;
import org.knime.workbench.ui.layout.Graph.Edge;
import org.knime.workbench.ui.layout.Graph.Node;

/**
 * reduces crossings of a given layering according to the average median
 * heuristic (see, e.g., Eades and Wormald, 1994; Mäkinen 1990)
 * 
 * @author Martin Mader, University of Konstanz
 */
public class CrossingMinimizer {
    private Graph m_g;

    private ArrayList<ArrayList<Node>> m_layers;

    public CrossingMinimizer(final Graph g,
            final ArrayList<ArrayList<Node>> layers) {
        m_g = g;
        m_layers = layers;
    }

    public void run() {
        if (m_layers.size() < 2) // nothing to do
            return;
        int oldCrossings = Integer.MAX_VALUE;
        int crossings = numberOfCrossings();
        do {
            // rightward sweep
            for (int i = 1; i < m_layers.size(); i++) {
                ArrayList<Node> curLayer = m_layers.get(i);
                ArrayList<Node> prevLayer = m_layers.get(i - 1);
                // set node to median position
                orderByMedian(curLayer, prevLayer);
            }
            // leftward sweep
            for (int i = m_layers.size() - 2; i >= 0; i--) {
                ArrayList<Node> curLayer = m_layers.get(i);
                ArrayList<Node> prevLayer = m_layers.get(i + 1);
                // set node to median position
                orderByMedian(curLayer, prevLayer);
            }
            // greedy switch
            greedySwitch();
            oldCrossings = crossings;
            crossings = numberOfCrossings();
        } while (crossings < oldCrossings);
    }

    

    /**
     * order the current layer by means of the average median of neighbors of
     * each node in the previous layer.
     * 
     * @param curLayer
     * @param prevLayer
     */
    private void orderByMedian(final ArrayList<Node> curLayer,
            final ArrayList<Node> prevLayer) {
        for (Node v : curLayer) {
            // get sorted list of neighbors in the previous layer
            ArrayList<Node> neighbors = getNeighbors(v, prevLayer);
            Collections.sort(neighbors, new Util.NodeByYComparator(m_g));
            // set median position, or average of two medians if degree is even
            int size = neighbors.size();
            int m1 = (int)Math.ceil(size / 2.0) - 1;
            int m2 = m1 + 1; // needed for even degree
            if (size > 0) {
                // median heuristic
                //m_g.setY(v, m_g.getY(neighbors.get(m1)));
                
                // average median heuristic
                if (size % 2 == 1) {
                    // odd degree
                    m_g.setY(v, m_g.getY(neighbors.get(m1)));
                } else {
                    // even degree
                    double y1 = m_g.getY(neighbors.get(m1));
                    double y2 = m_g.getY(neighbors.get(m2));
                    m_g.setY(v, (y1 + y2) / 2);
                }
            }
        }
        // sort current layer by medians
        Collections.sort(curLayer, new LayerSortComparator(prevLayer));
        // set corresponding y-coordinates
        double y = 0;
        for (Node n : curLayer) {
            m_g.setY(n, y);
            y++;
        }
    }
    
    private void greedySwitch() {
        for (int i = 1; i < m_layers.size(); i++) {
            ArrayList<Node> curLayer = m_layers.get(i);
            ArrayList<Node> prevLayer = m_layers.get(i - 1);
            int oldCross = Integer.MAX_VALUE;
            int cross = numberCrossingTwoLayer(curLayer, prevLayer);
            int crossTemp = cross;
            do {
                for (int j = 0; j < curLayer.size() - 1; j++) {
                    Node u = curLayer.get(j);
                    Node v = curLayer.get(j + 1);
                    switchNodes(u, v);
                    // if switch produces less crossings keep it
                    // i.e., if switch produces equal or more crossings, switch
                    // back
                    if (numberCrossingTwoLayer(curLayer, prevLayer) >= crossTemp)
                        switchNodes(u, v);
                    else
                        // is crossing number is reduced store new crossing
                        // number
                        crossTemp = numberCrossingTwoLayer(curLayer, prevLayer);
                }
                oldCross = cross;
                cross = numberCrossingTwoLayer(curLayer, prevLayer);
            } while (cross < oldCross);
        }
    }

    /**
     * switch y-coordinates of nodes u and v
     * 
     * @param u
     * @param v
     */
    private void switchNodes(Node u, Node v) {
        double temp = m_g.getY(u);
        m_g.setY(u, m_g.getY(v));
        m_g.setY(v, temp);
    }

    /**
     * counts the number of crossings in the whole layering.
     * @return
     */
    private int numberOfCrossings() {
        int cross = 0;
        for (int i = 1; i < m_layers.size(); i++) {
            ArrayList<Node> curLayer = m_layers.get(i);
            ArrayList<Node> prevLayer = m_layers.get(i - 1);
            cross += numberCrossingTwoLayer(curLayer, prevLayer);
        }
        return cross;
    }

    /**
     * counts the number of crossings between the two given layers. THIS IS
     * REALLY BRUTE FORCE AND CAN BE MADE MORE EFFICIENT! (e.g., modifying
     * mergesort to count inversions)
     * 
     * @param curLayer
     * @param prevLayer
     * @return
     */
    private int numberCrossingTwoLayer(ArrayList<Node> curLayer,
            ArrayList<Node> prevLayer) {
        int cross = 0;
        for (Node u1 : prevLayer) {
            for (Node v1 : getNeighbors(u1, curLayer)) {
                // inspect edge from prev layer to cur layer (u1,v1)
                // now check against all other edges ending at curLayer at
                // other nodes than u1 or v1
                for (Node v2 : curLayer) {
                    if (v1 != v2) {
                        for (Node u2 : getNeighbors(v2, prevLayer)) {
                            if (u2 != u1) {
                                // check if e1 and e2 are crossing
                                cross += checkCrossing(u1, v1, u2, v2);
                            }
                        }
                    }
                }
            }
        }
        // all crossings have been counted twice!
        return cross / 2;
    }

    /**
     * return the neighbors of a node n on the given layer
     * 
     * @param n
     * @param layer
     * @return
     */
    private ArrayList<Node> getNeighbors(final Node n,
            final ArrayList<Node> layer) {
        ArrayList<Node> neighbors = new ArrayList<Graph.Node>();
        for (Edge e : m_g.edges(n)) {
            Node m = e.opposite(n);
            if (layer.contains(m))
                neighbors.add(m);
        }
        return neighbors;
    }

    /**
     * check whether to edges (u1,v1) and (u2,v2) create a crossing
     * 
     * @param u1 node of first edge on first layer
     * @param v1 node of first edge on second layer
     * @param u2 node of second edge on first layer
     * @param v2 node of second edge on second layer
     * @return 1 if edges cross, 0 otherwise
     */
    private int checkCrossing(final Node u1, final Node v1, final Node u2,
            final Node v2) {
        if (m_g.getY(u1) < m_g.getY(u2) && m_g.getY(v1) > m_g.getY(v2))
            return 1;
        else if (m_g.getY(u1) > m_g.getY(u2) && m_g.getY(v1) < m_g.getY(v2))
            return 1;
        else
            return 0;
    }

    /**
     * used to compare two nodes on a layer, after each nodes y-coordinate is
     * set to the median of its neighbors. If two nodes have equal median,
     * preference is given to odd-degree nodes.
     * 
     * @author mader, University of Konstanz
     */
    public class LayerSortComparator implements
            Comparator<org.knime.workbench.ui.layout.Graph.Node> {

        ArrayList<Node> m_otherLayer;

        public LayerSortComparator(final ArrayList<Node> otherLayer) {
            m_otherLayer = otherLayer;
        }

        @Override
        public int compare(final Node o1, final Node o2) {
            if (m_g.getY(o1) < m_g.getY(o2))
                return -1;
            else if (m_g.getY(o1) > m_g.getY(o2))
                return 1;
            else {
                // both have same median
                // if o1 has odd degree choose this one
                if (getNeighbors(o1, m_otherLayer).size() % 2 != 0)
                    return -1;
                else if (getNeighbors(o2, m_otherLayer).size() % 2 != 0)
                    return 1;
                else
                    return 0;
            }
        }
    }
}