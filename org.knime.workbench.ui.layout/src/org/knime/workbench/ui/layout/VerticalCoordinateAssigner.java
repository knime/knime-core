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
 * Created: 30.03.2011
 * Author: mader
 */
package org.knime.workbench.ui.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.knime.workbench.ui.layout.Graph.Edge;
import org.knime.workbench.ui.layout.Graph.Node;

/**
 * handles vertical assignment of coordinates within layers, see
 * "Brandes, Köpf, 2001: Fast and simple horizontal coordinate assignment"
 * 
 * @author mader, University of Konstanz
 */
public class VerticalCoordinateAssigner {

    private Graph m_g;

    private ArrayList<ArrayList<Node>> m_layers;

    private HashMap<Edge, Boolean> m_innerSegment;

    private HashMap<Edge, Boolean> m_marked;

    private HashMap<Node, Node> m_align;

    private HashMap<Node, Node> m_root;

    public VerticalCoordinateAssigner(Graph g,
            ArrayList<ArrayList<Node>> layers, ArrayList<Node> dummyNodes,
            ArrayList<Edge> dummyEdges) {
        m_g = g;
        m_layers = layers;
        m_innerSegment = new HashMap<Graph.Edge, Boolean>();
        m_marked = new HashMap<Graph.Edge, Boolean>();
        m_align = new HashMap<Graph.Node, Graph.Node>();
        m_root = new HashMap<Graph.Node, Graph.Node>();
        for (Edge e : m_g.edges()) {
            m_marked.put(e, false);
            // initialize inner segments, corrected later
            m_innerSegment.put(e, false);
        }
        for (Node n : m_g.nodes()) {
            m_align.put(n, n);
            m_root.put(n, n);
        }
        // determine inner segments
        for (Edge e : dummyEdges) {
            if (dummyNodes.contains(e.source())
                    && dummyNodes.contains(e.target()))
                m_innerSegment.put(e, true);
        }
    }

    public void run() {
        markConflicts();
        horizontalAlignment();
        verticalCompaction();
    }

    private void verticalCompaction() {
        // TODO Auto-generated method stub

    }

    private void horizontalAlignment() {
        for (int i = 0; i < m_layers.size(); i++) {
            int r = 0;
            for (int k = 0; k < m_layers.get(i).size(); k++) {
                Node v_k = m_layers.get(i).get(k);
                ArrayList<Node> leftNeighbors = getLeftNeighbors(v_k);
                if (!leftNeighbors.isEmpty()) {
                    int d = leftNeighbors.size();
                    int m1 = (int)Math.floor((d + 1) / 2.0) - 1;
                    int m2 = (int)Math.ceil((d + 1) / 2.0) - 1;
                    for (int m = m1; m <= m2; m++) {
                        Node u_m = m_layers.get(i - 1).get(m);
                        if (!m_marked.get(u_m.getEdge(v_k))
                                && r < m_layers.get(i - 1).indexOf(u_m)) {
                            m_align.put(u_m, v_k);
                            m_root.put(v_k, m_root.get(u_m));
                            m_align.put(v_k, m_root.get(v_k));
                            r = m_layers.get(i - 1).indexOf(u_m);
                        }
                    }
                }

            }
        }

    }

    private ArrayList<Node> getLeftNeighbors(Node n) {
        ArrayList<Node> neighbors = new ArrayList<Graph.Node>();
        for (Edge e : m_g.inEdges(n))
            neighbors.add(e.source());
        // sort by order in layer
        Collections.sort(neighbors, new Comparator<Node>() {

            @Override
            public int compare(Node o1, Node o2) {
                return new Double(m_g.getY(o1)).compareTo(new Double(m_g
                        .getY(o2)));
            }
        });
        return neighbors;
    }

    private void markConflicts() {
        if (m_layers.size() < 4)
            // no conflicts possible since there cannot be any inner segments
            return;
        // inner segments can not occur between first and second layer, and
        // next-to-last and last layer
        for (int i = 1; i < m_layers.size() - 2; i++) {
            int k0 = 0;
            int l = 0;
            for (int l1 = 0; l1 < m_layers.get(i + 1).size(); l1++) {
                Node v_l1 = m_layers.get(i + 1).get(l1);
                Edge innerSegment = getInnerSegmentIncidentTo(v_l1);
                if (l1 == m_layers.get(i + 1).size() - 1
                        || innerSegment != null) {
                    int k1 = m_layers.get(i).size() - 1;
                    if (innerSegment != null)
                        k1 =
                                m_layers.get(i).indexOf(
                                        innerSegment.opposite(v_l1));
                    while (l <= l1) {
                        Node v_l = m_layers.get(i + 1).get(l);
                        for (Edge e : m_g.inEdges(v_l)) {
                            Node v_k = e.opposite(v_l);
                            int k = m_layers.get(i).indexOf(v_k);
                            if (k < k0 || k > k1)
                                m_marked.put(e, true);
                        }
                        l++;
                    }
                    k0 = k1;
                }
            }
        }

    }

    private Edge getInnerSegmentIncidentTo(Node node) {
        for (Edge e : m_g.inEdges(node))
            // if node is incident to inner segment this will be the only
            // incoming edge
            if (m_innerSegment.get(e))
                return e;
        return null;
    }
}
