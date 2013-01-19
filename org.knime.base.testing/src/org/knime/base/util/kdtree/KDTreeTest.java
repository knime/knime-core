/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
package org.knime.base.util.kdtree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import junit.framework.TestCase;

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
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final Helper o) {
            return (int)Math.signum(this.m_dist - o.m_dist);
        }

        @Override
        public String toString() {
            return "(" + m_dist + ", " + m_id + ")";
        }

    }

    private static class Helper2<T> {
        public final double[] data;

        public final T value;

        public Helper2(final double[] data, final T value) {
            this.data = data;
            this.value = value;
        }

        public double dist(final double[] query) {
            double sum = 0;
            for (int i = 0; i < data.length; i++) {
                double diff = data[i] - query[i];
                sum += diff * diff;
            }

            return Math.sqrt(sum);
        }
    }

    /**
     * Tests the search in various k-d trees.
     */
    public void testSearch() {
        for (int i = 0; i < 300; i++) {
            final int size = (int)(Math.random() * 1000) + 1;
            final int dimensions = (int)(Math.random() * 50) + 1;
            final int neighbours = (int)(Math.random() * size) + 1;
            final int bucketSize = (int)(Math.random() * 16) + 1;
            singleTest(size, dimensions, neighbours, bucketSize);
        }
    }

    private void singleTest(final int size, final int dimensions,
            final int neighbours, final int bucketSize) {
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

        KDTree<Integer> tree = builder.buildTree(bucketSize);
        List<NearestNeighbour<Integer>> results =
                tree.getKNearestNeighbours(query, neighbours);
        assertEquals(results.size(), neighbours);

        for (int i = 0; i < neighbours; i++) {
            // if (points.get(i).getId() != results.get(i).getData()) {
            // tree.getKNearestNeighbours(query, neighbours);
            // }
            assertEquals(points.get(i).getId(), results.get(i).getData()
                    .intValue());
        }
    }

    public static void singleSpeedTest(final int size, final int dimensions,
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
        KDTree<Integer> tree = builder.buildTree(16);
        kdTime += System.currentTimeMillis() - t;

        double averagePruning = 0;
        for (int m = 0; m < queries; m++) {
            final double[] query = new double[dimensions];

            for (int k = 0; k < query.length; k++) {
                query[k] = 100 * (Math.random() - 0.5);
            }

            t = System.currentTimeMillis();
            PriorityQueue<Helper> pq =
                    new PriorityQueue<Helper>(size, new Comparator<Helper>() {
                        @Override
                        public int compare(final Helper o1, final Helper o2) {
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
            averagePruning +=
                    (tree.size() - tree.getTestedPatterns())
                            / (double)tree.size();
        }

        System.out.println("Pruning = " + (100 * averagePruning / queries)
                + "%");
        System.out.println("k-d time = " + kdTime + "ms, brute force time = "
                + bruteForceTime + "ms");
    }

    public void testFromFile() throws IOException {
        BufferedReader in =
                new BufferedReader(new InputStreamReader(getClass()
                        .getClassLoader().getResourceAsStream(
                                KDTreeTest.class.getPackage().getName()
                                        .replace('.', '/')
                                        + "/KDTreeTest.csv")));

        KDTreeBuilder<Integer> builder = new KDTreeBuilder<Integer>(20);
        ArrayList<Helper2<Integer>> list = new ArrayList<Helper2<Integer>>();
        ArrayList<double[]> queries = new ArrayList<double[]>();

        String line;
        while ((line = in.readLine()) != null) {
            String[] parts = line.split(",");
            double[] data = new double[parts.length - 2];
            for (int i = 1; i < parts.length - 1; i++) {
                data[i - 1] = Double.parseDouble(parts[i]);
            }

            Integer v = new Integer(parts[0].replaceAll("\"", ""));

            builder.addPattern(data, v);
            list.add(new Helper2<Integer>(data, v));
            queries.add(data);
        }

        KDTree<Integer> tree = builder.buildTree();

        for (final double[] q : queries) {
            Collections.sort(list, new Comparator<Helper2<Integer>>() {
                @Override
                public int compare(final Helper2<Integer> o1, final Helper2<Integer> o2) {
                    return (int)Math.signum(o1.dist(q) - o2.dist(q));
                }
            });

            List<NearestNeighbour<Integer>> neighbours =
                    tree.getKNearestNeighbours(q, 3);
            for (int i = 0; i < neighbours.size(); i++) {
                Integer v1 = neighbours.get(i).getData();
                Integer v2 = list.get(i).value;

                assertEquals(v2, v1);
            }
        }

    }


    public void testFromFile2() throws IOException {
        BufferedReader in =
                new BufferedReader(new InputStreamReader(getClass()
                        .getClassLoader().getResourceAsStream(
                                KDTreeTest.class.getPackage().getName()
                                        .replace('.', '/')
                                        + "/knnprob.txt")));

        KDTreeBuilder<String> builder = new KDTreeBuilder<String>(19);
        ArrayList<Helper2<String>> list = new ArrayList<Helper2<String>>();
        ArrayList<double[]> queries = new ArrayList<double[]>();

        String line;
        while ((line = in.readLine()) != null) {
            String[] parts = line.split(",");
            double[] data = new double[parts.length - 1];
            for (int i = 1; i < parts.length; i++) {
                data[i - 1] = Double.parseDouble(parts[i]);
            }

            list.add(new Helper2<String>(data, parts[0]));
        }

        for (int i = 0; i < list.size() - 1; i++) {
            builder.addPattern(list.get(i).data, list.get(i).value);
        }
        queries.add(list.remove(list.size() - 1).data);


        KDTree<String> tree = builder.buildTree();

        for (final double[] q : queries) {
            Collections.sort(list, new Comparator<Helper2<String>>() {
                @Override
                public int compare(final Helper2<String> o1, final Helper2<String> o2) {
                    return (int)Math.signum(o1.dist(q) - o2.dist(q));
                }
            });

            List<NearestNeighbour<String>> neighbours =
                    tree.getKNearestNeighbours(q, 5);
            for (int i = 0; i < neighbours.size(); i++) {
                String v1 = neighbours.get(i).getData();
                String v2 = list.get(i).value;

                assertEquals(v2, v1);
            }
        }

    }


    /**
     * Well...
     *
     * @param args not used
     * @throws IOException bum
     */
    public static void main(final String[] args) throws IOException {
        for (int i = 0; i < 10; i++) {
            singleSpeedTest(1000, 10, 10, 10000);
        }
        KDTreeTest t = new KDTreeTest();
        t.testFromFile();
        t.testFromFile2();
    }
}
