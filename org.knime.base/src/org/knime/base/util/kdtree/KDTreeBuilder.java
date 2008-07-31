/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.util.kdtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * This class is some kind of factory for the {@link KDTree}. Because an
 * optimized k-d tree needs to inspect all nodes upon building the node, the
 * builder first collects all patterns and then builds the tree.
 * 
 * @param <T> the type of the data stored inside the tree
 * @author Thorsten Meinl, University of Konstanz
 */
public class KDTreeBuilder<T> {
    private final int m_k;

    /** The default number of patterns inside a terminal node. */
    public static final int DEFAULT_BUCKET_SIZE = 4;

    private final ArrayList<TerminalNode<T>> m_nodes =
            new ArrayList<TerminalNode<T>>();

    private int m_processedPatterns;

    /**
     * Creates a new k-d tree builder.
     * 
     * @param k the dimensionality of the stored patterns
     */
    public KDTreeBuilder(final int k) {
        m_k = k;
    }

    /**
     * Adds a pattern that is later inserted into the tree.
     * 
     * @param pattern the pattern; the length of the array must be the same as
     *            the number specified when the builder was created
     * @param data (optional) data associated with the pattern
     */
    public void addPattern(final double[] pattern, final T data) {
        if (pattern.length != m_k) {
            throw new IllegalArgumentException(
                    "pattern is not of specified dimensionality " + m_k);
        }
        m_nodes.add(new TerminalNode<T>(pattern, data));
    }

    /**
     * Builds a k-d tree using all the patterns that have been added to the
     * builder so far.
     * 
     * @return an optimized k-d tree
     */
    public KDTree<T> buildTree() {
        return buildTree(DEFAULT_BUCKET_SIZE);
    }

    /**
     * Builds a k-d tree using all the patterns that have been added to the
     * builder so far.
     * 
     * @param bucketSize the number of patterns inside the terminal nodes
     * @param progMon an optional progress monitor, can be <code>null</code>
     * @return ann optimized k-d tree
     * @throws CanceledExecutionException if the execution has been canceled
     */
    public KDTree<T> buildTree(final int bucketSize,
            final ExecutionMonitor progMon) throws CanceledExecutionException {
        m_processedPatterns = 0;
        Node rootNode = buildTree(m_nodes, bucketSize, progMon);
        return new KDTree<T>(m_k, rootNode, m_nodes.size());
    }

    /**
     * Builds a k-d tree using all the patterns that have been added to the
     * builder so far.
     * 
     * @param progMon an optional progress monitor, can be <code>null</code>
     * @return an optimized k-d tree
     * @throws CanceledExecutionException if the execution has been canceled
     */
    public KDTree<T> buildTree(final ExecutionMonitor progMon)
            throws CanceledExecutionException {
        return buildTree(DEFAULT_BUCKET_SIZE, progMon);
    }

    /**
     * Builds a k-d tree using all the patterns that have been added to the
     * builder so far.
     * 
     * @param bucketSize the number of patterns inside the terminal nodes
     * @return ann optimized k-d tree
     */
    public KDTree<T> buildTree(final int bucketSize) {
        Node rootNode = buildTree(m_nodes, bucketSize);
        return new KDTree<T>(m_k, rootNode, m_nodes.size());
    }

    /**
     * Recursive method to build the tree.
     * 
     * @param nodes the list of nodes for which a (sub)tree should be built
     * @param bSize the number of patterns inside the terminal nodes
     * 
     * @return a k-d tree for the passed nodes
     */
    private Node buildTree(final List<TerminalNode<T>> nodes, final int bSize) {
        m_processedPatterns = 0;
        try {
            return buildTree(nodes, bSize, null);
        } catch (CanceledExecutionException ex) {
            // cannot happen because we don't have an execution monitor
        }
        return null;
    }

    /**
     * Recursive method to build the tree.
     * 
     * @param nodes the list of nodes for which a (sub)tree should be built
     * @param bSize the number of patterns inside the terminal nodes
     * 
     * @return a k-d tree for the passed nodes
     * @throws CanceledExecutionException if the execution has been canceled
     */
    private Node buildTree(final List<TerminalNode<T>> nodes, final int bSize,
            final ExecutionMonitor progMon) throws CanceledExecutionException {
        if (nodes.size() <= bSize) {
            m_processedPatterns += nodes.size();
            return new TerminalBucket<T>(nodes);
        } else if (nodes.size() == 0) {
            return null;
        }

        double maxSpread = -1;

        int maxSpreadKey = -1;
        for (int i = 0; i < m_k; i++) {
            double curSpread = computeSpread(nodes, i);
            if (curSpread > maxSpread) {
                maxSpread = curSpread;
                maxSpreadKey = i;
            }
        }

        final int temp = maxSpreadKey;
        // find the median and split the nodes
        Collections.sort(nodes, new Comparator<TerminalNode<T>>() {
            public int compare(final TerminalNode<T> o1,
                    final TerminalNode<T> o2) {
                return (int)Math.signum(o1.getPattern()[temp]
                        - o2.getPattern()[temp]);
            }
        });

        int mid = nodes.size() / 2;
        final double median = nodes.get(mid - 1).getPattern()[maxSpreadKey];

        List<TerminalNode<T>> left = nodes.subList(0, mid);
        List<TerminalNode<T>> right = nodes.subList(mid, nodes.size());

        Node leftNode = buildTree(left, bSize, progMon);
        Node rightNode = buildTree(right, bSize, progMon);

        if (progMon != null) {
            progMon.checkCanceled();
            progMon.setProgress(m_processedPatterns / (double)m_nodes.size(),
                    "Added " + m_processedPatterns + " patterns to the tree");
        }

        NonterminalNode newNode =
                new NonterminalNode(maxSpreadKey, median, leftNode, rightNode);
        return newNode;
    }

    /**
     * Computes the spread of the attribute inside the passed list of patterns.
     * 
     * @param nodes the patterns for which the attribute spread should be
     *            computed
     * @param key the current attribute index
     * @return the spread
     */
    private double computeSpread(final List<TerminalNode<T>> nodes,
            final int key) {
        double sum = 0;
        double squareSum = 0;

        for (TerminalNode<T> node : nodes) {
            sum += node.getPattern()[key];
            squareSum += node.getPattern()[key] * node.getPattern()[key];
        }

        sum /= nodes.size();

        final double variance = squareSum / nodes.size() - sum * sum;
        return variance;
    }
}
