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
package org.knime.workbench.ui.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.workbench.ui.layout.Graph.Edge;
import org.knime.workbench.ui.layout.Graph.Node;

/**
 * assigns layers to the nodes of a graph by topological sorting
 * 
 * @author mader, University of Konstanz
 */
public class Layerer {
	
	private static final int SCALE = 120;

	/**
	 * Perform an improved topological sort to assign layers in x-direction.
	 * 
	 * @param g
	 *            the graph to be layered
	 * @param nodeLayer
	 *            a map storing the layer of each node
	 * @return the list of layers, each layer containing a list of nodes
	 */
	static ArrayList<List<Node>> assignLayers(Graph g, Map<Node, Integer> nodeLayer) {
		// initialize residual degrees, and find first sources
		ArrayList<List<Node>> layers = new ArrayList<List<Node>>();
		Map<Node, Integer> residualDegree = g.createIntNodeMap();
		ArrayList<Node> sources = new ArrayList<Node>();
		for (Node n : g.nodes()) {
			residualDegree.put(n, n.inDegree());
			if (n.inDegree() == 0)
				sources.add(n);
		}

		// process each layer:
		int layer = 0;
		while (!sources.isEmpty()) {
			ArrayList<Node> nextSources = new ArrayList<Node>();
			// put all of the current sources on the current layer
			layers.add(sources);
			for (Node n : sources) {
				g.setCoordinates(n, layer * SCALE, g.getY(n));
				nodeLayer.put(n, layer);
				// reduce residual degree of neighbours
				for (Edge e : g.outEdges(n)) {
					Node t = e.target();
					int deg = residualDegree.get(t).intValue() - 1;
					residualDegree.put(t, deg);
					// neighbours can become new sources. if they do, add them
					// to the sources that will be placed on the next layer.
					if (deg == 0)
						nextSources.add(t);
				}
			}
			// advance to the next layer
			sources = nextSources;
			layer++;
		}
		return layers;
	}
}
