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
 * ---------------------------------------------------------------------
 *
 * Created: 28.03.2011
 * Author: mader
 */
package org.knime.workbench.ui.layout.layeredlayout;

import java.util.ArrayList;
import java.util.Map;

import org.knime.workbench.ui.layout.Graph;
import org.knime.workbench.ui.layout.Graph.Edge;
import org.knime.workbench.ui.layout.Graph.Node;

/**
 * assigns layers to the nodes of a graph by topological sorting.
 * 
 * @author mader, University of Konstanz
 */
public class Layerer {

    /**
     * Perform a topological sort to assign layers to nodes. If there are fixed
     * nodes they will be placed on the first or last layer, depending on
     * whether they are sources or sinks.
     * 
     * @param g the graph to be layered
     * @param nodeLayer a map storing the layer of each node
     * @param fixedSources a list of sources to fix on the first layer, or null
     *            if none exist
     * @param fixedSinks a list of sinks to fix on the last layer, or null if
     *            none exist
     * @return the list of layers, each layer containing an array list of nodes
     */
    static ArrayList<ArrayList<Node>> assignLayers(final Graph g,
            final Map<Node, Integer> nodeLayer,
            final ArrayList<Node> fixedSources, final ArrayList<Node> fixedSinks) {

        // initialize residual degrees, and find first sources
        ArrayList<ArrayList<Node>> layers = new ArrayList<ArrayList<Node>>();
        Map<Node, Integer> residualDegree = g.createIntNodeMap();
        ArrayList<Node> sources = new ArrayList<Node>();
        for (Node n : g.nodes()) {
            residualDegree.put(n, n.inDegree());
            if (n.inDegree() == 0) {
                sources.add(n);
            }
        }

        // process each layer:
        int layer = 0;
        // handle fixed sources, if any
        if (fixedSources != null) {
            layers.add(fixedSources);
            for (Node n : fixedSources) {
                sources.remove(n);
                nodeLayer.put(n, layer);
                // check if any of the outgoing neighbors becomes a source
                updateSources(g, n, sources, residualDegree);
            }
            layer++;
        }

        // handle regular nodes
        while (!sources.isEmpty()) {
            ArrayList<Node> nextSources = new ArrayList<Node>();
            // put all of the current sources on the current layer
            layers.add(sources);
            for (Node n : sources) {
                nodeLayer.put(n, layer);
                updateSources(g, n, nextSources, residualDegree);
            }
            // advance to the next layer
            sources = nextSources;
            layer++;
        }

        // handle fixed sinks by putting them on the last layer
        if (fixedSinks != null) {
            // check if there are non-fixed sinks on the current last layer
            boolean lastLayerValid = true;
            int lastlayer = layers.size() - 1;

            for (Node n : layers.get(lastlayer)) {
                if (!fixedSinks.contains(n)) {
                    lastLayerValid = false;
                }
            }

            // if last layer does only contain fixed sinks, then move all fixed
            // sinks to this layer, otherwise introduce new last layer
            if (!lastLayerValid) {
                lastlayer++;
                layers.add(new ArrayList<Graph.Node>());
            }
            for (Node n : fixedSinks) {
                layers.get(nodeLayer.get(n)).remove(n);
                nodeLayer.put(n, lastlayer);
                layers.get(lastlayer).add(n);
            }
        }
        return layers;
    }

    /**
     * check the outgoing edges of a given node n for becoming a new source
     * after n is processed.
     * 
     * @param g
     * @param n
     * @param sources
     * @param residualDegree
     */
    private static void updateSources(final Graph g, Node n,
            ArrayList<Node> sources, Map<Node, Integer> residualDegree) {
        for (Edge e : g.outEdges(n)) {
            Node t = e.target();
            int newDegree = residualDegree.get(t).intValue() - 1;
            residualDegree.put(t, newDegree);
            if (newDegree == 0)
                sources.add(t);
        }
    }
}
