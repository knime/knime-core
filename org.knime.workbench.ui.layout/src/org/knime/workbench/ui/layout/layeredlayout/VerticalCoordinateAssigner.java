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
package org.knime.workbench.ui.layout.layeredlayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.knime.workbench.ui.layout.Graph;
import org.knime.workbench.ui.layout.Graph.Edge;
import org.knime.workbench.ui.layout.Graph.Node;

/**
 * handles vertical assignment of coordinates within layers, see
 * "Brandes, Köpf, 2001: Fast and simple horizontal coordinate assignment"
 * 
 * @author mader, University of Konstanz
 */
public class VerticalCoordinateAssigner {

    private static final Double delta = 1.0;

    private Graph m_g;

    private ArrayList<ArrayList<Node>> m_layers;

    private HashMap<Node, Integer> m_pos = new HashMap<Graph.Node, Integer>();

    private HashMap<Node, Node> m_pred = new HashMap<Graph.Node, Graph.Node>();

    private HashMap<Edge, Boolean> m_innerSegment =
            new HashMap<Graph.Edge, Boolean>();

    private HashMap<Edge, Boolean> m_marked =
            new HashMap<Graph.Edge, Boolean>();

    private HashMap<Node, Node> m_align = new HashMap<Graph.Node, Graph.Node>();

    private HashMap<Node, Node> m_root = new HashMap<Graph.Node, Graph.Node>();

    private HashMap<Node, Node> m_sink = new HashMap<Graph.Node, Graph.Node>();

    private HashMap<Node, Double> m_shift = new HashMap<Graph.Node, Double>();

    private HashMap<Node, Double> m_y = new HashMap<Graph.Node, Double>();

    private HashMap<Node, Double> m_yLT = new HashMap<Graph.Node, Double>();

    private HashMap<Node, Double> m_yLB = new HashMap<Graph.Node, Double>();

    private HashMap<Node, Double> m_yRT = new HashMap<Graph.Node, Double>();

    private HashMap<Node, Double> m_yRB = new HashMap<Graph.Node, Double>();

    public VerticalCoordinateAssigner(Graph g,
            ArrayList<ArrayList<Node>> layers, ArrayList<Node> dummyNodes,
            ArrayList<Edge> dummyEdges) {
        m_g = g;
        m_layers = layers;
        // initialize pos and pred
        for (int i = 0; i < m_layers.size(); i++) {
            ArrayList<Node> layer = m_layers.get(i);
            for (int pos = 0; pos < layer.size(); pos++) {
                m_pos.put(layer.get(pos), pos);
                if (pos == 0)
                    m_pred.put(layer.get(pos), null);
                else
                    m_pred.put(layer.get(pos), layer.get(pos - 1));
            }
        }
        // initialize edge maps
        for (Edge e : m_g.edges()) {
            m_marked.put(e, false);
            // initialize inner segments, corrected later
            m_innerSegment.put(e, false);
        }
        // initialize node maps
        for (Node n : m_g.nodes()) {
            m_align.put(n, n);
            m_root.put(n, n);
            m_sink.put(n, n);
            m_shift.put(n, Double.POSITIVE_INFINITY);
            m_y.put(n, Double.NaN);
        }
        // determine inner segments
        for (Edge e : dummyEdges) {
            if (dummyNodes.contains(e.source())
                    && dummyNodes.contains(e.target()))
                m_innerSegment.put(e, true);
        }
    }

    public void run() {

        // preprocessing
        markConflicts();
        // run alignment and compaction 4 times, once for each directional
        // choice
        initNodeMaps();
        horizontalAlignmentLeftTopmost();
        verticalCompaction();
        storeCoordinates(m_yLT);
        initNodeMaps();
        horizontalAlignmentLeftBottommost();
        verticalCompaction();
        storeCoordinates(m_yLB);
        initNodeMaps();
        horizontalAlignmentRightTopmost();
        verticalCompaction();
        storeCoordinates(m_yRT);
        initNodeMaps();
        horizontalAlignmentRightBottommost();
        verticalCompaction();
        storeCoordinates(m_yRB);
        initNodeMaps();
        // balance between 4 alignments
        balance();
        // set final coordinates
        for (Node n : m_g.nodes()) {
            m_g.setY(n, m_y.get(n));
        }

    }

    private void storeCoordinates(HashMap<Node, Double> y) {
        for (Node n : m_g.nodes()) {
            y.put(n, m_y.get(n));
        }
    }

    private void initNodeMaps() {
        for (Node n : m_g.nodes()) {
            m_align.put(n, n);
            m_root.put(n, n);
            m_sink.put(n, n);
            m_shift.put(n, Double.POSITIVE_INFINITY);
            m_y.put(n, Double.NaN);
        }
    }

    private void balance() {
        // align to smallest height layout
        double[] height = new double[4];
        height[0] = getHeight(m_yLT);
        height[1] = getHeight(m_yLB);
        height[2] = getHeight(m_yRT);
        height[3] = getHeight(m_yRB);

        // ....

        // average median
        for (Node n : m_g.nodes()) {
            double[] y = new double[4];
            y[0] = m_yLT.get(n);
            y[1] = m_yLB.get(n);
            y[2] = m_yRT.get(n);
            y[3] = m_yRB.get(n);
            Arrays.sort(y);
            m_y.put(n, (y[1] + y[2]) / 2);
        }

    }

    private double getHeight(HashMap<Node, Double> y) {
        double max = 0;
        double min = Double.POSITIVE_INFINITY;
        for (Node n : m_g.nodes()) {
            max = Math.max(max, y.get(n));
            min = Math.min(min, y.get(n));
        }
        return max - min;
    }

    private void verticalCompaction() {
        for (Node v : m_g.nodes()) {
            if (m_root.get(v) == v)
                placeBlock(v);
        }
        for (Node v : m_g.nodes()) {
            double y = m_y.get(m_root.get(v)).doubleValue();
            m_y.put(v, y);
            double shift = m_shift.get(m_sink.get(m_root.get(v))).doubleValue();
            if (shift < Double.POSITIVE_INFINITY) {
                m_y.put(v, y + shift);
            }
        }

    }

    private void placeBlock(Node v) {
        if (m_y.get(v).equals(Double.NaN)) {
            m_y.put(v, 0.0);
            Node w = v;
            do {
                if (m_pos.get(w) > 0) {
                    Node u = m_root.get(m_pred.get(w));
                    placeBlock(u);
                    if (m_sink.get(v) == v)
                        m_sink.put(v, m_sink.get(u));
                    if (m_sink.get(v) != m_sink.get(u)) {
                        double shiftSinkU =
                                Math.min(m_shift.get(m_sink.get(u)), m_y.get(v)
                                        - m_y.get(u) - delta);
                        m_shift.put(m_sink.get(u), shiftSinkU);
                    } else {
                        m_y.put(v, Math.max(m_y.get(v), m_y.get(u) + delta));
                    }
                }
                w = m_align.get(w);
            } while (w != v);
        }

    }

    private void horizontalAlignmentLeftTopmost() {
        for (int i = 0; i < m_layers.size(); i++) {
            int r = -1;
            for (int k = 0; k < m_layers.get(i).size(); k++) {
                Node vk = m_layers.get(i).get(k);
                ArrayList<Node> neighbors = getNeighbors(vk, true);
                if (!neighbors.isEmpty()) {
                    int d = neighbors.size();
                    int m1 = (int)Math.floor((d + 1) / 2.0) - 1;
                    int m2 = (int)Math.ceil((d + 1) / 2.0) - 1;
                    for (int m = m1; m <= m2; m++) {
                        if (m_align.get(vk) == vk) {
                            Node um = neighbors.get(m);
                            if (!m_marked.get(um.getEdge(vk))
                                    && r < m_pos.get(um)) {
                                m_align.put(um, vk);
                                m_root.put(vk, m_root.get(um));
                                m_align.put(vk, m_root.get(vk));
                                r = m_pos.get(um);
                            }
                        }
                    }
                }

            }
        }
    }

    private void horizontalAlignmentLeftBottommost() {
        for (int i = 0; i < m_layers.size(); i++) {
            int r = m_layers.size();
            for (int k = m_layers.get(i).size() - 1; k >= 0; k--) {
                Node vk = m_layers.get(i).get(k);
                ArrayList<Node> neighbors = getNeighbors(vk, true);
                if (!neighbors.isEmpty()) {
                    int d = neighbors.size();
                    int m1 = (int)Math.floor((d + 1) / 2.0) - 1;
                    int m2 = (int)Math.ceil((d + 1) / 2.0) - 1;
                    for (int m = m2; m >= m1; m--) {
                        if (m_align.get(vk) == vk) {
                            Node um = neighbors.get(m);
                            if (!m_marked.get(um.getEdge(vk))
                                    && r > m_pos.get(um)) {
                                m_align.put(um, vk);
                                m_root.put(vk, m_root.get(um));
                                m_align.put(vk, m_root.get(vk));
                                r = m_pos.get(um);
                            }
                        }
                    }
                }

            }
        }
    }

    private void horizontalAlignmentRightTopmost() {
        for (int i = m_layers.size() - 1; i >= 0; i--) {
            int r = -1;
            for (int k = 0; k < m_layers.get(i).size(); k++) {
                Node vk = m_layers.get(i).get(k);
                ArrayList<Node> neighbors = getNeighbors(vk, false);
                if (!neighbors.isEmpty()) {
                    int d = neighbors.size();
                    int m1 = (int)Math.floor((d + 1) / 2.0) - 1;
                    int m2 = (int)Math.ceil((d + 1) / 2.0) - 1;
                    for (int m = m1; m <= m2; m++) {
                        if (m_align.get(vk) == vk) {
                            Node um = neighbors.get(m);
                            if (!m_marked.get(um.getEdge(vk))
                                    && r < m_pos.get(um)) {
                                m_align.put(um, vk);
                                m_root.put(vk, m_root.get(um));
                                m_align.put(vk, m_root.get(vk));
                                r = m_pos.get(um);
                            }
                        }
                    }
                }

            }
        }
    }

    private void horizontalAlignmentRightBottommost() {
        for (int i = m_layers.size() - 1; i >= 0; i--) {
            int r = m_layers.size();
            for (int k = m_layers.get(i).size() - 1; k >= 0; k--) {
                Node vk = m_layers.get(i).get(k);
                ArrayList<Node> neighbors = getNeighbors(vk, false);
                if (!neighbors.isEmpty()) {
                    int d = neighbors.size();
                    int m1 = (int)Math.floor((d + 1) / 2.0) - 1;
                    int m2 = (int)Math.ceil((d + 1) / 2.0) - 1;
                    for (int m = m2; m >= m1; m--) {
                        if (m_align.get(vk) == vk) {
                            Node um = neighbors.get(m);
                            if (!m_marked.get(um.getEdge(vk))
                                    && r > m_pos.get(um)) {
                                m_align.put(um, vk);
                                m_root.put(vk, m_root.get(um));
                                m_align.put(vk, m_root.get(vk));
                                r = m_pos.get(um);
                            }
                        }
                    }
                }

            }
        }
    }

    private ArrayList<Node> getNeighbors(Node n, boolean left) {
        ArrayList<Node> neighbors = new ArrayList<Graph.Node>();
        Iterable<Edge> incidentEdges;
        if (left)
            incidentEdges = m_g.inEdges(n);
        else
            incidentEdges = m_g.outEdges(n);
        for (Edge e : incidentEdges)
            neighbors.add(e.opposite(n));
        // sort by order in layer
        Collections.sort(neighbors, new Util.NodeByYComparator(m_g));
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
