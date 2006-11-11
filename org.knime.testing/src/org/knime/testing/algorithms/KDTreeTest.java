/* Created on Nov 11, 2006 1:40:47 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
package org.knime.testing.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import junit.framework.TestCase;

import org.knime.dev.util.kdtree.KDTree;
import org.knime.dev.util.kdtree.KDTreeBuilder;
import org.knime.dev.util.kdtree.NearestNeighbour;

/**
 * This testcase checks if the k-d tree implementation is correct.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class KDTreeTest extends TestCase {
    private static class Helper implements Comparable<Helper> {
        private final double[] m_coords;

        private final double m_dist;

        private final int m_id;

        public Helper(final double[] coords, final int id, final double[] query) {
            m_coords = coords;
            m_id = id;
            m_dist = computeDist(query);
        }

        public double[] getCoords() {
            return m_coords;
        }

        public int getId() {
            return m_id;
        }

        public double computeDist(final double[] c2) {
            double sum = 0;
            for (int i = 0; i < m_coords.length; i++) {
                double dist = m_coords[i] - c2[i];
                sum += dist * dist;
            }

            return Math.sqrt(sum);
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Helper o) {
            return (int)Math.signum(this.m_dist - o.m_dist);
        }

        @Override
        public String toString() {
            return "(" + m_dist + ", " + m_id + ")";
        }

    }

    public void testSearch() {
        for (int i = 0; i < 100; i++) {
            final int size = (int)(Math.random() * 1000) + 1;
            final int dimensions = (int)(Math.random() * 50) + 1;
            final int neighbours = (int)(Math.random() * size) + 1;
            singleTest(size, dimensions, neighbours);
        }
    }

    private void singleTest(final int size, final int dimensions,
            final int neighbours) {
        KDTreeBuilder<Integer> builder = new KDTreeBuilder<Integer>(dimensions);

        final double[] query = new double[dimensions];

        for (int k = 0; k < query.length; k++) {
            query[k] = 100 * (Math.random() - 0.5);
        }

        ArrayList<Helper> points = new ArrayList<Helper>();
        for (int i = 0; i < size; i++) {
            final double[] coords = new double[dimensions];

            for (int k = 0; k < coords.length; k++) {
                coords[k] = 120 * (Math.random() - 0.5);
            }

            builder.addPattern(coords, i);
            points.add(new Helper(coords, i, query));
        }

        Collections.sort(points);

        KDTree<Integer> tree = builder.buildTree();
        List<NearestNeighbour<Integer>> results =
                tree.getKNearestNeighbours(query, neighbours);

        for (int i = 0; i < neighbours; i++) {
            // if (points.get(i).getId() != results.get(i).getData()) {
            // tree.getKNearestNeighbours(query, neighbours);
            // }
            assertEquals(points.get(i).getId(), results.get(i).getData()
                    .intValue());
        }
    }

    public void testSpeed() {
        int[] times = singleSpeedTest(2000, 120, 10, 10000);
        System.out.println("k-d time = " + times[0] + "ms, brute force time = "
                + times[1] + "ms");
    }

    private int[] singleSpeedTest(final int size, final int dimensions,
            final int neighbours, final int queries) {
        long bruteForceTime = 0, kdTime = 0;
        KDTreeBuilder<Integer> builder = new KDTreeBuilder<Integer>(dimensions);

        ArrayList<Helper> points = new ArrayList<Helper>();
        for (int i = 0; i < size; i++) {
            final double[] coords = new double[dimensions];

            for (int k = 0; k < coords.length; k++) {
                coords[k] = 120 * (Math.random() - 0.5);
            }

            builder.addPattern(coords, i);
            points.add(new Helper(coords, i, coords));
        }

        long t = System.currentTimeMillis();
        KDTree<Integer> tree = builder.buildTree();
        kdTime += System.currentTimeMillis() - t;

        for (int m = 0; m < queries; m++) {
            final double[] query = new double[dimensions];

            for (int k = 0; k < query.length; k++) {
                query[k] = 100 * (Math.random() - 0.5);
            }

            t = System.currentTimeMillis();
            PriorityQueue<Helper> pq =
                    new PriorityQueue<Helper>(size, new Comparator<Helper>() {
                        public int compare(Helper o1, Helper o2) {
                            return (int)Math.signum(o1.computeDist(query)
                                    - o2.computeDist(query));
                        }
                    });
            pq.addAll(points);
            for (int i = 0; i < neighbours; i++) {
                pq.poll();
            }
            bruteForceTime += System.currentTimeMillis() - t;

            t = System.currentTimeMillis();
            tree.getKNearestNeighbours(query, neighbours);
            kdTime += System.currentTimeMillis() - t;
//            System.out.println("Pruning = "
//                    + (100.0 * (tree.size() - tree.getVisitedNodes()) / tree
//                            .size()) + "%");
        }

        return new int[]{(int)kdTime, (int)bruteForceTime};
    }
}
