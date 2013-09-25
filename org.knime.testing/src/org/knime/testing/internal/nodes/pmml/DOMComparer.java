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
 */
package org.knime.testing.internal.nodes.pmml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides methods for comparing XML nodes.
 * @author Alexander Fillbrunn
 *
 */
public final class DOMComparer {

	// Regex for numbers used to determine whether an attribute contains a number
	private final static String NUMBER_REGEX = "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$";

    private DOMComparer() {
    }

    /**
     * Checks if two nodes are equal.
     * @param node1 the first xml node
     * @param node2 the second xml node
     * @return A comparison result tree that says whether nodes are equal or not
     */
    public static CompareResult areNodesEqual(final Node node1, final Node node2) {
        return areNodesEqual(node1, node2, null);
    }

    /**
     * Checks if two nodes are equal and uses a XMLComparerListener to determine
     * which nodes to check and which to ignore.
     * @param node1 the first xml node
     * @param node2 the second xml node
     * @param l the listener that determines whether a node is checked or not
     * @return A comparison result tree that says whether nodes are equal or not
     */
    public static CompareResult areNodesEqual(final Node node1, final Node node2, final DOMComparerFilter l) {
        return areNodesEqual(node1, node2, l, 0);
    }

    private static CompareResult areNodesEqual(final Node node1, final Node node2, final DOMComparerFilter l,
            final int depth) {

        // Different names means different nodes
        if (!node1.getNodeName().equals(node2.getNodeName())) {
            return new CompareResult(node1, depth, false);
        }

        // Check attributes
        NamedNodeMap attributes1 = node1.getAttributes();
        NamedNodeMap attributes2 = node2.getAttributes();

        if (attributes1.getLength() != attributes2.getLength()) {
        	return new CompareResult(node1, depth, false);
        } else {
        	for (int i = 0; i < attributes1.getLength(); i++) {
        		Node attr = attributes1.item(i);
        		String val1 = attr.getNodeValue();
        		String val2 = attributes2.getNamedItem(attr.getNodeName()).getNodeValue();

        		if (val1.equals(val2)) {
        			continue;
        		} else {
        			// We have to take care of cases where an attribute can be 1.0 in one document and 1 in the other.
        			// There could also be cases such as 1.000 and 1.0, so string comparison is not that easy.
        			// BigDecimal is used in order to avoid floating point errors
        			if (val1.matches(NUMBER_REGEX) && val2.matches(NUMBER_REGEX)) {
        				java.math.BigDecimal bd1 = null;
        				java.math.BigDecimal bd2 = null;
        				try {
        					// BigDecimal needs a capital E for the exponent
        					bd1 = new BigDecimal(val1.replace('e', 'E'));
        					bd2 = new BigDecimal(val2.replace('e', 'E'));
        				} catch(NumberFormatException e) {
        					// Cannot be parsed to a number, strings are not the same, so it is a mismatch
        					// Should not happen because of the regular expression
        					return new CompareResult(node1, depth, false);
        				}

        				// Scale is the number of digits after the decimal point. It is also compared in the
        				// equals() method, so we set it to the  maximum on both numbers
        				int maxScale = Math.max(bd1.scale(), bd2.scale());
        				bd1 = bd1.setScale(maxScale);
        				bd2 = bd2.setScale(maxScale);
        				if (!bd1.equals(bd2)) {
        					return new CompareResult(node1, depth, false);
        				}
        			} else {
            			return new CompareResult(node1, depth, false);
            		}
        		}
        	}
        }

        NodeList children1 = node1.getChildNodes();
        NodeList children2 = node2.getChildNodes();

        // Lists holding all nodes of which no counterpart in the other xml has been found yet
        ArrayList<Node> c1 = new ArrayList<Node>();
        ArrayList<Node> c2 = new ArrayList<Node>();

        for (int i = 0; i < children1.getLength(); i++) {
            Node n = children1.item(i);
            if (l == null || l.isCheckedNode(n)) {
                c1.add(n);
            }
        }

        for (int i = 0; i < children2.getLength(); i++) {
            Node n = children2.item(i);
            if (l == null || l.isCheckedNode(n)) {
                c2.add(n);
            }
        }

        // If different number of children, elements are not equal
        if (c1.size() != c2.size()) {
            return new CompareResult(node1, depth, false);
        }

        CompareResult r = new CompareResult(node1, depth, true);
        ArrayList<Node> successNodes = new ArrayList<Node>();

        // Recursively compare nodes with each other and remove them from lists if they are equal
        for (int i = c1.size() - 1; i >= 0; i--) {
            for (int j = c2.size() - 1; j >= 0; j--) {
                Node n1 = c1.get(i);
                Node n2 = c2.get(j);
                CompareResult res = areNodesEqual(n1, n2, l, depth + 1);
                r.addChild(res);
                res.setParent(r);
                if (res.isSuccess()) {
                    successNodes.add(n1);
                    c1.remove(i);
                    c2.remove(j);
                    break;
                }
            }
        }
        // Partial success if for some nodes a match has been found
        r.setPartialSuccess(c1.size() < children1.getLength());
        r.setSuccess(c1.size() == 0);

        // Remove unnecessary results of comparisons
        for (int i = r.getChildren().size() - 1; i >= 0; i--) {
            CompareResult k = r.getChildren().get(i);
            // If this comparison was not successful but there exists one, then delete this one
            if (!k.isSuccess()) {
                if (successNodes.contains(k.getNode())) {
                    r.getChildren().remove(i);
                } else {
                    for (int j = r.getChildren().size() - 1; j >= 0; j--) {
                        // If multiple unsuccessful comparison results exist for a node,
                        // delete all but one
                        if (j != i && r.getChildren().get(j).getNode().equals(k.getNode())) {
                            r.getChildren().remove(i);
                            break;
                        }
                    }
                }
            }
        }

        return r;
    }

    /**
     * Result of comparing two xml nodes.
     * @author Alexander Fillbrunn
     *
     */
    public static final class CompareResult {

        private int m_level;
        private Node m_node;
        private boolean m_success;
        private boolean m_partialSuccess = false;
        private List<CompareResult> m_children;
        private CompareResult m_parent = null;

        private CompareResult(final Node n, final int level, final boolean success) {
            m_level = level;
            m_node = n;
            m_success = success;
            m_partialSuccess = success;
            m_children = new ArrayList<CompareResult>();
        }

        private void addChild(final CompareResult r) {
            m_children.add(r);
        }

        private void setParent(final CompareResult p) {
            m_parent = p;
        }

        private void setPartialSuccess(final boolean s) {
            m_partialSuccess = s;
        }

        private void setSuccess(final boolean s) {
            m_success = s;
        }

        /**
         * Returns the children of this CompareResult.
         * Children are all results of comparisons of child elements of this CompareResult's node with nodes
         * in the second document that are children of the corresponding parent node.
         * @return a list of CompareResults of comparisons of the children of this result's node
         */
        public List<CompareResult> getChildren() {
            return m_children;
        }

        /**
         * The level at which this CompareResult's node is located in the XML.
         * @return the level/depth
         */
        public int getLevel() {
            return m_level;
        }

        /**
         * The node that was compared with a node of the second document.
         * @return the node
         */
        public Node getNode() {
            return m_node;
        }

        /**
         * Returns whether the comparison was successful or not.
         * @return true if the node is equal to a corresponding node in the second node, false otherwise.
         */
        public boolean isSuccess() {
            return m_success;
        }

        /**
         * Returns whether some of the descendant nodes of this node were equal
         * to corresponding nodes in the second document.
         * @return true if at least some descendant elements have a matching counterpart in the second document,
         * false otherwise.
         */
        public boolean isPartialSuccess() {
            return m_partialSuccess;
        }


        /**
         * The parent CompareResult, which compares the parent XML node of this result's node with a matching
         * counterpart in the second document.
         * @return the parent CompareResult
         */
        public CompareResult getParent() {
            return m_parent;
        }
    }
}
