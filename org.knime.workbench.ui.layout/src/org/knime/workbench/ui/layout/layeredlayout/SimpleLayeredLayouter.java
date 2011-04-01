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
 * Created: 28.03.2011
 * Author: mader
 */
package org.knime.workbench.ui.layout.layeredlayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.workbench.ui.layout.Graph;
import org.knime.workbench.ui.layout.Graph.Edge;
import org.knime.workbench.ui.layout.Graph.Node;

/**
 * computes a layered layout for a directed acyclic graph. Following the
 * Sugijama framework, the algorithm processes 3 stages: layer assingment,
 * crossing minimization, and coordinate assignment.
 * 
 * Techniques involved are covered in
 * "Bastert, Matuszewski: Layered Drawings of Digraphs, LNCS 2025, pp87-120, 2001"
 * ; except for coordinate assignment that is covered in "Brandes, Köpf: Fast
 * and simple horizontal coordinate assignment, 2001".
 * 
 * @author Martin Mader, University of Konstanz
 */
public class SimpleLayeredLayouter {

    public void doLayout(final Graph g) throws RuntimeException {

        // get layering of the graph
        Map<Node, Integer> nodeLayer = g.createIntNodeMap();
        ArrayList<ArrayList<Node>> layers = Layerer.assignLayers(g, nodeLayer);

        // add dummy vertices for edges spanning several layers
        ArrayList<Edge> hiddenEdges = new ArrayList<Graph.Edge>();
        ArrayList<Node> dummyNodes = new ArrayList<Graph.Node>();
        ArrayList<Edge> dummyEdges = new ArrayList<Graph.Edge>();
        HashMap<Edge, ArrayList<Node>> hiddenEdgeToDummyVertices =
                new HashMap<Graph.Edge, ArrayList<Node>>();
        for (Edge e : g.edges()) {
            int startLayer = nodeLayer.get(e.source()).intValue();
            int endLayer = nodeLayer.get(e.target()).intValue();
            int span = endLayer - startLayer;
            if (span > 1) {
                hiddenEdges.add(e);
            }
        }
        // cannot modify graph in for-loop above, since it would create
        // concurrent modification due to iterator
        for (Edge e : hiddenEdges) {
            // list for this edges dummy nodes
            ArrayList<Node> eDummyNodes = new ArrayList<Graph.Node>();
            int startLayer = nodeLayer.get(e.source()).intValue();
            int endLayer = nodeLayer.get(e.target()).intValue();
            int span = endLayer - startLayer;
            Node last = e.source();
            for (int i = 1; i < span; i++) {
                Node current =
                        g.createNode("bend " + e + ", " + i, startLayer + i,
                                g.getY(last));
                // add dummy to its layer
                nodeLayer.put(current, startLayer + i);
                layers.get(startLayer + i).add(current);
                // add dummy edge to graph
                Edge dEdge = g.createEdge(last, current);
                dummyEdges.add(dEdge);
                // add dummy vertex to the list of dummies for the original edge
                eDummyNodes.add(current);
                // proceed
                last = current;
            }
            // add last dummy edge
            g.createEdge(last, e.target());
            // store list of dummy nodes for original edge
            hiddenEdgeToDummyVertices.put(e, eDummyNodes);
            // add this edges dummy Nodes to the list of all dummy nodes
            dummyNodes.addAll(eDummyNodes);
        }

        // remove hidden edges
        for (Edge e : hiddenEdges) {
            g.removeEdge(e);
        }

        // set initial coordinates by layer
        int layer = 0;
        for (ArrayList<Node> currentLayer : layers) {
            // here the ordering is shuffled, could also be done several times
            // in the crossing minimization phase.
            // I.e., every execution of the algorithm potentially yields another
            // result!
            Collections.shuffle(currentLayer);
            // ordering could also be initialized by the current ordering from
            // y-coordinates.
            // Collections.sort(currentLayer, new Util.NodeByYComparator(g));

            // set coordinates from 0,1,...,size of layer
            int verticalCoord = 0;
            for (Node n : currentLayer) {
                g.setCoordinates(n, layer, verticalCoord);
                verticalCoord++;
            }
            layer++;
        }

        /* Do crossing minimization */
        CrossingMinimizer cm = new CrossingMinimizer(g, layers);
        cm.run();

        /* Do vertical placement */
        VerticalCoordinateAssigner vca =
                new VerticalCoordinateAssigner(g, layers, dummyNodes,
                        dummyEdges);
        vca.run();

        /*
         * Reinsert hidden edges with bendpoints, and remove dummy nodes and
         * edges
         */
        for (Edge hEdge : hiddenEdges) {
            Edge e = g.reinsert(hEdge);
            for (Node n : hiddenEdgeToDummyVertices.get(hEdge)) {
                g.addBend(e, g.getX(n), g.getY(n));
                g.removeNode(n); // also removes dummy edges!
            }
        }

        // clean up unnecessary bend-points
        g.cleanBends();
    }

}
