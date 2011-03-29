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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class implements a simple graph data structure using an adjacency-list
 * representation. Allows basic graph operations (like creation, insertion and
 * removal of nodes and edges), provides basic information (empty-status, number
 * of contained nodes / edges, test whether a node / edge is contained), and
 * iterators to access all nodes and edges in the graph.
 * 
 * Nodes are automatically labeled from "1" to "number of nodes created". This
 * numbering is only used for String representation of nodes and edges, and
 * should not be used for any other operations!
 * 
 * 
 * @author Martin Mader, University Konstanz
 * 
 */
public class Graph {

	/**
	 * list of nodes in this graph
	 */
	private List<Node> nodes;
	/**
	 * list of edges in this graph
	 */
	private List<Edge> edges;
	/**
	 * used to label the nodes (increased by one whenever a new node is created)
	 */
	private int nodeIndex = 0;

	/**
	 * constructor initializing an empty graph
	 */
	public Graph() {
		nodes = new ArrayList<Node>();
		edges = new ArrayList<Edge>();
	}

	/**
	 * @return true if the graph is empty (i.e., does not contain any nodes) or
	 *         false otherwise
	 */
	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	/**
	 * test whether an edge specified by its source and target node is contained
	 * in the graph.
	 * 
	 * @param source
	 * @param target
	 * @return true if edge (source, target) is contained in the graph, false
	 *         otherwise
	 */
	public boolean containsEdge(Node source, Node target) {
		return source.getEdge(target) != null;
	}

	/**
	 * test whether the given node is contained in the graph.
	 * 
	 * @param node
	 * @return true if node is contained, false otherwise
	 */
	public boolean containsNode(Node node) {
		return nodes.contains(node);
	}

	/**
	 * creates a new node.
	 * 
	 * @return the newly created node or null if the node could not be inserted
	 *         to the internal list of nodes
	 */
	public Node createNode(String label) {
		nodeIndex++; // increase label counter
		Node node = new Node(nodeIndex, label);
		return nodes.add(node) ? node : null;
	}

	public Node createNode(String label, double x, double y) {
		nodeIndex++; // increase label counter
		Node node = new Node(nodeIndex, label, x, y);
		return nodes.add(node) ? node : null;
	}

	/**
	 * creates an edge between given source and target nodes.
	 * 
	 * @param source
	 * @param target
	 * @return the newly created edge, or null if the edge already exists or
	 *         could not be inserted to the internal edge list
	 */
	public Edge createEdge(Node source, Node target) {
		// if the two nodes are already adjacent
		if (source.getEdge(target) != null)
			return null;
		// create edge and insert it affected nodes' incidence-lists and to this
		// graphs' edge-list
		Edge edge = new Edge(source, target);
		source.addEdge(edge);
		target.addEdge(edge);
		return edges.add(edge) ? edge : null;
	}

	public Edge reinsert(Edge e) {
		Node source = e.source();
		Node target = e.target();
		// if the two nodes are already adjacent
		if (source.getEdge(target) != null)
			return null;
		// create edge and insert it affected nodes' incidence-lists and to this
		// graphs' edge-list
		source.addEdge(e);
		target.addEdge(e);
		return edges.add(e) ? e : null;
	}

	/**
	 * @return an iterable for the nodes contained in this graph (in order of
	 *         creation)
	 */
	public Iterable<Node> nodes() {
		return new Iterable<Graph.Node>() {

			@Override
			public Iterator<Node> iterator() {
				return nodes.iterator();
			}
		};
	}

	/**
	 * @return an iterable for the edges contained in this graph (in order of
	 *         creation)
	 */
	public Iterable<Edge> edges() {
		return new Iterable<Graph.Edge>() {

			@Override
			public Iterator<Edge> iterator() {
				return edges.iterator();
			}
		};
	}

	public Iterable<Edge> edges(final Node n) {
		return new Iterable<Graph.Edge>() {

			@Override
			public Iterator<Edge> iterator() {
				return n.edges();
			}
		};
	}

	public Iterable<Edge> inEdges(final Node n) {
		return new Iterable<Graph.Edge>() {

			@Override
			public Iterator<Edge> iterator() {
				return n.inEdges();
			}
		};
	}

	public Iterable<Edge> outEdges(final Node n) {
		return new Iterable<Graph.Edge>() {

			@Override
			public Iterator<Edge> iterator() {
				return n.outEdges();
			}
		};
	}

	/**
	 * @return the number of edges contained in this graph
	 */
	public int m() {
		return edges.size();
	}

	/**
	 * @return the number of nodes contained in this graph
	 */
	public int n() {
		return nodes.size();
	}

	/**
	 * removes a given edge from the graph
	 * 
	 * @param edge
	 * @return the removed edge, or null if the edge could not be removed from
	 *         the internal edge list
	 */
	public Edge removeEdge(Edge edge) {
		Node source = edge.source();
		Node target = edge.target();
		// remove edge from edge-list and from the incidence-lists of its two
		// nodes
		source.removeEdge(edge);
		target.removeEdge(edge);
		return edges.remove(edge) ? edge : null;
	}

	/**
	 * removes a given node from the graph
	 * 
	 * @param node
	 * @return the removed node, or null if the node could not be removed from
	 *         the internal node list
	 */
	public Node removeNode(Node node) {
		// remove all incident edges from neighbors' incidence-lists
		for (Iterator<Edge> it = node.edges(); it.hasNext();) {
			Edge edge = it.next();
			edge.opposite(node).removeEdge(edge);
			edges.remove(edge);
		}
		// remove node from graph's node-list
		return nodes.remove(node) ? node : null;
	}

	/**
	 * return the x-coordinate of a given node n.
	 * 
	 * @param n
	 *            a node
	 * @return n's x-coordinate
	 */
	public double getX(Node n) {
		return n.x;
	}

	/**
	 * return the y-coordinate of a given node n.
	 * 
	 * @param n
	 *            a node
	 * @return n's y-coordinate
	 */
	public double getY(Node n) {
		return n.y;
	}

	/**
	 * set the x-coordinate of a node
	 * 
	 * @param n
	 * @param x
	 */
	public void setX(Node n, double x) {
		n.x = x;
	}

	/**
	 * set the y-coordinate of a node
	 * 
	 * @param n
	 * @param y
	 */
	public void setY(Node n, double y) {
		n.y = y;
	}

	/**
	 * set the coordinates of a given node n.
	 * 
	 * @param n
	 *            a node
	 * @param x
	 *            the x-coordinate
	 * @param y
	 *            the y-coordinate
	 */
	public void setCoordinates(Node n, double x, double y) {
		n.x = x;
		n.y = y;
	}

	/**
	 * for each edge, remove all bendpoints that have the same y-coordinate as
	 * their predecessor and successor.
	 */
	public void cleanBends() {
		for (Edge e : edges()) {
			ArrayList<Point2D> bends = bends(e);
			if (bends.size() > 0) {
				// traverse bends in reverse order (to be able to remove them
				// from array list
				double lastY = e.target().y;
				double curY, nextY;

				for (int i = bends.size() - 1; i >= 0; i--) {
					curY = bends.get(i).getY();
					if (i == 0)
						nextY = e.source().y;
					else
						nextY = bends.get(i - 1).getY();

					if (curY == lastY && curY == nextY) {
						bends.remove(i);
					} else {
						lastY = curY;
					}
				}
				e.bends = bends;
			}
		}
	}

	/**
	 * create a map storing values for each node in the graph.
	 * 
	 * @return a node map containing <code>null</code> for each node
	 */
	public Map<Node, Object> createNodeMap() {
		HashMap<Node, Object> map = new HashMap<Graph.Node, Object>(n());
		for (Node n : nodes)
			map.put(n, null);
		return map;
	}

	/**
	 * create a map storing an {@link Integer} for each node in the graph.
	 * 
	 * @return a node map containing <code>null</code> for each node
	 */
	public Map<Node, Integer> createIntNodeMap() {
		HashMap<Node, Integer> map = new HashMap<Graph.Node, Integer>(n());
		for (Node n : nodes)
			map.put(n, null);
		return map;
	}

	/**
	 * return the list of bend-points of a given edge
	 * 
	 * @param e
	 * @return
	 */
	public ArrayList<Point2D> bends(Edge e) {
		return e.bends;
	}

	/**
	 * add a bend-point to the given edge
	 * 
	 * @param e
	 * @param x
	 * @param y
	 */
	public void addBend(Edge e, double x, double y) {
		e.bends.add(new Point2D.Double(x, y));
	}

	@Override
	public String toString() {
		return "Nodes: " + Arrays.toString(nodes.toArray()) + "\nEdges: "
				+ Arrays.toString(edges.toArray());
	}

	/**
	 * implements a simple node data structure, using an incidence-list storing
	 * incident edges.
	 * 
	 * @author Martin Mader, University Konstanz
	 * 
	 */
	public class Node {

		/**
		 * the internal list of edges incident to this node.
		 */
		private List<Edge> edges;
		private List<Edge> inEdges;
		private List<Edge> outEdges;
		private int index = -1;
		/**
		 * this nodes' label
		 */
		private String label = "";
		private double x;
		private double y;

		/**
		 * constructor creating a node with empty incidence-list. Will only be
		 * called by {@link AbstractGraph}.
		 * 
		 * @param label
		 *            this nodes' label
		 */
		private Node(int index, String label) {
			this.index = index;
			this.label = label;
			edges = new ArrayList<Edge>();
			inEdges = new ArrayList<Edge>();
			outEdges = new ArrayList<Edge>();
		}

		private Node(int index, String label, double x, double y) {
			this(index, label);
			this.x = x;
			this.y = y;
		}

		public int index() {
			return index;
		}

		/**
		 * @return the degree of this node
		 */
		public int degree() {
			return edges.size();
		}

		public int inDegree() {
			return inEdges.size();
		}

		public int outDegree() {
			return outEdges.size();
		}

		/**
		 * @return an iterator for all incident edges (in order of creation)
		 */
		public Iterator<Edge> edges() {
			return edges.iterator();
		}

		/**
		 * @return an iterator for all incoming edges (in order of creation)
		 */
		public Iterator<Edge> inEdges() {
			return inEdges.iterator();
		}

		/**
		 * @return an iterator for all incident edges (in order of creation)
		 */
		public Iterator<Edge> outEdges() {
			return outEdges.iterator();
		}

		/**
		 * returns the edge connecting this node with a given node
		 * 
		 * @param node
		 * @return the (first) edge connecting this node with the given node, or
		 *         null if no such edge exists
		 */
		public Edge getEdge(Node node) {
			for (Iterator<Edge> it = edges(); it.hasNext();) {
				Edge edge = it.next();
				if (edge.source() == node || edge.target() == node)
					return edge;
			}
			return null;
		}

		/**
		 * adds the given edge to this nodes' incidence-lists.
		 * 
		 * @param edge
		 * @return the added edge
		 */
		private Edge addEdge(Edge edge) {
			boolean ok = true;
			ok = ok && edges.add(edge);
			if (this == edge.source())
				ok = ok && outEdges.add(edge);
			else if (this == edge.target())
				ok = ok && inEdges.add(edge);
			return ok ? edge : null;
		}

		/**
		 * removes the given edge from this nodes' incidence lists.
		 * 
		 * @param edge
		 * @return the removed edge
		 */
		private Edge removeEdge(Edge edge) {
			boolean ok = true;
			ok = ok && edges.remove(edge);
			if (this == edge.source())
				ok = ok && outEdges.remove(edge);
			else if (this == edge.target())
				ok = ok && inEdges.remove(edge);
			return ok ? edge : null;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	/**
	 * implements a simple edge data structure for directed edges
	 * 
	 * @author Martin Mader, University Konstanz
	 * 
	 */
	public class Edge {

		/**
		 * this edges' source node
		 */
		private Node source;
		/**
		 * this edges' target node
		 */
		private Node target;
		/**
		 * this edges' bend points
		 */
		private ArrayList<Point2D> bends = new ArrayList<Point2D>();

		/**
		 * creates an edge (source, target). Will only be called by
		 * {@link AbstractGraph}.
		 * 
		 * @param source
		 * @param target
		 */
		private Edge(Node source, Node target) {
			this.source = source;
			this.target = target;
		}

		/**
		 * @return this edges' source node
		 */
		public Node source() {
			return source;
		}

		/**
		 * @return this edges' target node
		 */
		public Node target() {
			return target;
		}

		/**
		 * returns the opposite node w.r.t. the given node of this edge
		 * 
		 * @param node
		 * @return the opposite of the given node, or null if the given node is
		 *         not part of this edge
		 */
		public Node opposite(Node node) {
			// if the given node is this edges' source, return target node and
			// vice versa
			if (this.source == node)
				return this.target;
			else if (this.target == node)
				return this.source;
			// given node is not part of this edge
			else
				return null;
		}

		@Override
		public String toString() {
			return "(" + source + "," + target + ")";
		}
	}
}
