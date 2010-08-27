/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.util.kdtree;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;

/**
 * This class is an implementation of a k-d tree as described in <div> Friedman,
 * Jerome H; Bentley, Jon Louis; Finkel, Raphael Ari: <i>An Algorithm for
 * Finding Best Matches in Logarithmic Expected Time</i>; ACM Transactions on
 * Mathematical Software; 1997, 3(3), pages 209-226 </div>
 *
 * For creating a k-d tree use the {@link KDTreeBuilder}.
 *
 * @param <T> the type of the data that is to be stored in the tree
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class KDTree<T> {
    private final int m_k, m_size;

    private final Node m_root;

    private int m_testedPatterns;

    /**
     * Creates a new optimized k-d tree. This constructor is called by the
     * {@link KDTreeBuilder}.
     *
     * @param k the number of dimensions of the patterns
     * @param rootNode the root node of the tree
     * @param size the size of the tree
     */
    KDTree(final int k, final Node rootNode, final int size) {
        m_k = k;
        m_root = rootNode;
        m_size = size;
    }

    /**
     * Returns the tree's size, i.e. the number of stored patterns. The number
     * of nodes inside the tree is approximately twice the size.
     *
     * @return the tree's size
     */
    public int size() {
        return m_size;
    }

    /**
     * Searches for the <code>k</code> nearest neighbours of the
     * <code>query</code> pattern. The returned list is sorted by the distance
     * to the query pattern in increasing order. The returned list may contain
     * more than <code>k</code> patterns if the patterns from <code>k</code> to
     * the end have equal distance to the query pattern.
     *
     *
     * @param query the query pattern, must have the same dimensionality as the
     *            patterns inside the tree
     * @param k the number of nearest neighbours to retrieve
     * @return a sorted list of the nearest neighbours
     */
    public List<NearestNeighbour<T>> getKNearestNeighbours(
            final double[] query, final int k) {
        if (query.length != m_k) {
            throw new IllegalArgumentException(
                    "The query vector has not length " + m_k);
        }
        if (k > m_size) {
            throw new IllegalArgumentException("The tree contains only "
                    + m_size + " elements, but " + k + " were requested");
        }

        PriorityQueue<NearestNeighbour<T>> pq =
                new PriorityQueue<NearestNeighbour<T>>(k);
        for (int i = 0; i < k; i++) {
            pq.add(new NearestNeighbour<T>(null, Double.MAX_VALUE));
        }

        double[] lowerBounds = new double[m_k];
        double[] upperBounds = new double[m_k];

        for (int i = 0; i < m_k; i++) {
            lowerBounds[i] = -Double.MAX_VALUE;
            upperBounds[i] = Double.MAX_VALUE;
        }

        m_testedPatterns = 0;
        search(m_root, query, pq, lowerBounds, upperBounds, false);
        LinkedList<NearestNeighbour<T>> results =
                new LinkedList<NearestNeighbour<T>>();

        while (pq.peek() != null) {
            NearestNeighbour<T> nn = pq.poll();
            nn.setDistance(Math.sqrt(nn.getDistance()));
            results.addFirst(nn);
        }

        // The final list may contain much more than k elements and even
        // elements farther away than the k-th. So we need to remove the
        // superflous elements.
        ListIterator<NearestNeighbour<T>> it = results.listIterator();
        // take the first k elements
        for (int i = 0; i < k; i++) {
            it.next();
        }
        if (it.hasNext()) {
            // take all following elements having the same distance as the k-th
            double lastDist = it.previous().getDistance();
            while (it.hasNext() && (it.next().getDistance() == lastDist)) {
                //
            }
            // remove all remaining elements
            if (it.hasNext()) {
                it.remove();
                while (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
        }


        // assert (results.size() == k);
        return results;
    }

    /**
     * Searches for all neighbours of the <code>query</code> pattern that are
     * not more than <code>maxDist</code> away from it. The returned list is
     * sorted by the distance to the query pattern in increasing order.
     *
     * @param query the query pattern, must have the same dimensionality as the
     *            patterns inside the tree
     * @param maxDist the maximum distance the patterns may have (exclusive)
     * @return a sorted list of the neighbours
     */
    public List<NearestNeighbour<T>> getMaxDistanceNeighbours(
            final double[] query, final double maxDist) {
        if (query.length != m_k) {
            throw new IllegalArgumentException(
                    "The query vector has not length " + m_k);
        }

        PriorityQueue<NearestNeighbour<T>> pq =
                new PriorityQueue<NearestNeighbour<T>>();
        pq.add(new NearestNeighbour<T>(null, maxDist * maxDist));

        double[] lowerBounds = new double[m_k];
        double[] upperBounds = new double[m_k];

        for (int i = 0; i < m_k; i++) {
            lowerBounds[i] = -Double.MAX_VALUE;
            upperBounds[i] = Double.MAX_VALUE;
        }

        m_testedPatterns = 0;
        search(m_root, query, pq, lowerBounds, upperBounds, true);
        LinkedList<NearestNeighbour<T>> results =
                new LinkedList<NearestNeighbour<T>>();

        while (pq.peek() != null) {
            NearestNeighbour<T> nn = pq.poll();
            nn.setDistance(Math.sqrt(nn.getDistance()));
            if (nn.getData() != null) {
                // the "border" pattern has null data and must not be included
                results.addFirst(nn);
            }
        }

        assert (results.getLast().getDistance() <= maxDist);
        return results;
    }

    /**
     * Adds a new nearest neighbour to the candidate list, of the passed
     * terminal node is nearer to the query pattern than the currently farthest
     * neighbour. This method can be used for two purposes: First during the
     * search for the k nearest neighbours of the query pattern. For this the
     * <code>maxDistanceMode</code> parameter must be set to <code>false</code>.
     * Second during a search for all patterns up to a maximum distance from the
     * query pattern, if <code>maxDistanceMode</code> is set to
     * <code>true</code>.
     *
     * @param tn the terminal node under consideration
     * @param pq the list of nearest neighbours
     * @param query the query pattern
     * @param maxDistanceMode <code>true</code> if all nodes up to a maximal
     *            distance should be added, <code>false</code> if the k nearest
     *            neighbours should be found
     *
     * @return <code>true</code> if a new nearest neighbour has been found,
     *         <code>false</code> otherwise
     */
    private boolean addNewNearestNeighbour(final TerminalNode<T> tn,
            final PriorityQueue<NearestNeighbour<T>> pq, final double[] query,
            final boolean maxDistanceMode) {
        m_testedPatterns++;
        double distance = tn.getDistance(query);

        double d = pq.peek().getDistance();

        if (d > distance) {
            NearestNeighbour<T> qr =
                    new NearestNeighbour<T>(tn.getData(), distance);
            pq.offer(qr);
            if (!maxDistanceMode) {
                pq.poll();
            }
            return true;
        } else if (d == distance) {
            NearestNeighbour<T> qr =
                    new NearestNeighbour<T>(tn.getData(), distance);
            pq.offer(qr);
            return true;
        }
        return false;
    }

    /**
     * Does the recursive search. This method can be used for two purposes:
     * First during the search for the k nearest neighbours of the query
     * pattern. For this the <code>maxDistanceMode</code> parameter must be set
     * to <code>false</code>. Second during a search for all patterns up to a
     * maximum distance from the query pattern, if <code>maxDistanceMode</code>
     * is set to <code>true</code>.
     *
     * @param node the current node under consideration
     * @param query the query pattern
     * @param pq the priority queue of the currently nearest neighbours
     * @param lowerBounds the lower bounds array
     * @param upperBounds the upper bounds array
     * @param maxDistanceMode <code>true</code> if all nodes up to a maximal
     *            distance should be added, <code>false</code> if the k nearest
     *            neighbours should be found
     *
     * @return <code>true</code> if the search can be aborted,
     *         <code>false</code> if it should be continued
     */
    private boolean search(final Node node, final double[] query,
            final PriorityQueue<NearestNeighbour<T>> pq,
            final double[] lowerBounds, final double[] upperBounds,
            final boolean maxDistanceMode) {
        if (node == null) {
            return false;
        }
        if (node instanceof TerminalBucket) {
            boolean newFound = false;
            for (TerminalNode<T> tn : ((TerminalBucket<T>)node)) {
                newFound |=
                        addNewNearestNeighbour(tn, pq, query, maxDistanceMode);
            }
            if (newFound
                    && ballWithinBounds(query, pq.peek().getDistance(),
                            lowerBounds, upperBounds)) {
                return true; // search is done
            }
            return false;
        }

        final NonterminalNode n = (NonterminalNode)node;

        final int keyIndex = n.getSplitAttribute();
        final double keyValue = n.getSplitValue();

        // recursive call on the closer child node
        if (query[keyIndex] <= keyValue) {
            final double temp = upperBounds[keyIndex];
            upperBounds[keyIndex] = keyValue;
            boolean finished =
                    search(n.getLeft(), query, pq, lowerBounds, upperBounds,
                            maxDistanceMode);
            upperBounds[keyIndex] = temp;
            if (finished) {
                return true;
            }
        } else {
            final double temp = lowerBounds[keyIndex];
            lowerBounds[keyIndex] = keyValue;
            boolean finished =
                    search(n.getRight(), query, pq, lowerBounds, upperBounds,
                            maxDistanceMode);
            lowerBounds[keyIndex] = temp;
            if (finished) {
                return true;
            }
        }

        // recursive call on the farther child node
        if (query[keyIndex] <= keyValue) {
            final double temp = lowerBounds[keyIndex];
            lowerBounds[keyIndex] = keyValue;

            if (boundsOverlapBall(query, pq.peek().getDistance(), lowerBounds,
                    upperBounds)) {
                search(n.getRight(), query, pq, lowerBounds, upperBounds,
                        maxDistanceMode);
            }
            lowerBounds[keyIndex] = temp;
        } else {
            final double temp = upperBounds[keyIndex];
            upperBounds[keyIndex] = keyValue;

            if (boundsOverlapBall(query, pq.peek().getDistance(), lowerBounds,
                    upperBounds)) {
                search(n.getLeft(), query, pq, lowerBounds, upperBounds,
                        maxDistanceMode);
            }

            upperBounds[keyIndex] = temp;
        }

        if (ballWithinBounds(query, pq.peek().getDistance(), lowerBounds,
                upperBounds)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the region determined by the lower and upper bounds of the
     * current non-terminal node overlaps with the ball around the query pattern
     * with radius equal to the distance of the currently farthest away nearest
     * pattern.
     *
     * @param query the query pattern
     * @param farthestDist the currently farthest distance of the nearest node
     * @param lowerBounds the lower bounds for the attributes of the nodes
     *            "below" the current node
     * @param upperBounds the upper bounds for the attributes of the nodes
     *            "below" the current node
     *
     * @return <code>true</code> if the ball and the region overlap,
     *         <code>false</code> otherwise
     */
    private boolean boundsOverlapBall(final double[] query,
            final double farthestDist, final double[] lowerBounds,
            final double[] upperBounds) {
        double sum = 0;

        for (int i = 0; i < m_k; i++) {
            if (query[i] < lowerBounds[i]) {
                double dist = query[i] - lowerBounds[i];
                sum += dist * dist;
                if (sum > farthestDist) {
                    return false;
                }
            } else if (query[i] > upperBounds[i]) {
                double dist = query[i] - upperBounds[i];
                sum += dist * dist;
                if (sum > farthestDist) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if the "ball" around the query with the radius equal to the
     * currently farthest away neighbour is entirely within the region covered
     * by the current non-terminal node under consideration. This region is
     * given by the upper and lower bounds arrays.
     *
     * @param query the query pattern
     * @param farthestDist the distance of the currently farthest away pattern
     *            in the list of nearest patterns
     * @param lowerBounds the lower bounds for the attributes of the nodes
     *            "below" the current node
     * @param upperBounds the upper bounds for the attributes of the nodes
     *            "below" the current node
     *
     * @return <code>true</code> if the ball is completely within the bounds,
     *         <code>false</code> otherwise
     */
    private boolean ballWithinBounds(final double[] query,
            final double farthestDist, final double[] lowerBounds,
            final double[] upperBounds) {
        for (int i = 0; i < m_k; i++) {
            double x = query[i] - lowerBounds[i];
            if (x * x <= farthestDist) {
                return false;
            }

            x = query[i] - upperBounds[i];
            if (x * x <= farthestDist) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the number of tested patterns during the last call to
     * {@link #getKNearestNeighbours(double[], int)}. The lower the number the
     * better the k-d tree could prune the search.
     *
     * @return the number of tested patterns
     */
    public int getTestedPatterns() {
        return m_testedPatterns;
    }
}
