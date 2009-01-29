/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   02.02.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.distances;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class DistanceManagerFactory {

    /**
     * Flag for euclidean distance.
     */
    public static final String EUCLIDEAN_DIST = "Euclidean";

    /**
     * Flag for cosinus distance.
     */
    public static final String COS_DIST = "Cosinus";
    
    /**
     * Flag for korrelation distance.
     */
    public static final String MANHATTAN_DIST = "Manhattan";
    
    
    private DistanceManagerFactory() { }
    
    /**
     * Creates a new instance extending the <code>DistanceManager</code> 
     * interface. According to the kind of distance a particular 
     * <code>DistanceManager</code> is returned. If you want to compute for 
     * instance euclidean distances, then use
     * <code>DistanceManagerFactory.EUCLIDEAN_DIST</code> as distance parameter 
     * and the <code>EuclideanDistanceManager</code> is returned. If an unvalid
     * kind of distance is given null will be returned.
     * The fuzzy parameter specifies if the created <code>DistanceManager</code>
     * will compute distances between <code>FuzzyIntervalCell</code>s or
     * <code>DataCell</code>s containing numbers.
     * The offset parameter sepcifies a particular offset used i.e. by the
     * <code>CosinusDistanceManager</code>.
     * 
     * @param distance Specifies the concrete <code>DistanceManager</code>
     * implementation to be returned.
     * @param fuzzy If true the <code>DistanceManager</code> will compute
     * distances between <code>FuzzyIntervalCell</code>s.
     * @param offset A particular offset use by i.e. the
     * <code>CosinusDistanceManager</code> 
     * 
     * @return A particular <code>DistanceManager</code>, specified by the
     * distance parameter.
     */
    public static final DistanceManager createDistanceManager(
            final String distance, final boolean fuzzy, final double offset) {
        if (distance.equals(DistanceManagerFactory.EUCLIDEAN_DIST)) {
            return new EuclideanDistanceManager(fuzzy);
        } else if (distance.equals(DistanceManagerFactory.COS_DIST)) {
            return new CosinusDistanceManager(offset, fuzzy);
        } else if (distance.equals(DistanceManagerFactory.MANHATTAN_DIST)) {
            return new ManhattanDistanceManager(fuzzy);
        }
        return null;
    }
    
    /**
     * Creates a new instance extending the <code>DistanceManager</code> 
     * interface. According to the kind of distance a particular 
     * <code>DistanceManager</code> is returned. If you want to compute for 
     * instance euclidean distances, then use
     * <code>DistanceManagerFactory.EUCLIDEAN_DIST</code> as distance parameter 
     * and the <code>EuclideanDistanceManager</code> is returned. If an unvalid
     * kind of distance is given null will be returned.
     * The returned <code>DistanceManager</code> computes distances between
     * <code>DataCell</code>s containing numbers, not 
     * <code>FuzzyIntervalCell</code>s. The offset is set to 1 by default.
     * 
     * @param distance Specifies the concrete <code>DistanceManager</code>
     * implementation to be returned.
     * 
     * @return A particular <code>DistanceManager</code>, specified by the
     * distance parameter.
     */
    public static final DistanceManager createDistanceManager(
            final String distance) {
        return DistanceManagerFactory.createDistanceManager(distance, false, 1);
    }
    
    /**
     * Creates a new instance extending the <code>DistanceManager</code> 
     * interface. According to the kind of distance a particular 
     * <code>DistanceManager</code> is returned. If you want to compute for 
     * instance euclidean distances, then use
     * <code>DistanceManagerFactory.EUCLIDEAN_DIST</code> as distance parameter 
     * and the <code>EuclideanDistanceManager</code> is returned. If an unvalid
     * kind of distance is given null will be returned.
     * The fuzzy parameter specifies if the created <code>DistanceManager</code>
     * will compute distances between <code>FuzzyIntervalCell</code>s or
     * <code>DataCell</code>s containing numbers. 
     * The offset is set to 1 by default.
     * 
     * @param distance Specifies the concrete <code>DistanceManager</code>
     * implementation to be returned.
     * @param fuzzy If true the <code>DistanceManager</code> will compute
     * distances between <code>FuzzyIntervalCell</code>s.
     * 
     * @return A particular <code>DistanceManager</code>, specified by the
     * distance parameter.
     */    
    public static final DistanceManager createDistanceManager(
            final String distance, final boolean fuzzy) {
        return DistanceManagerFactory.createDistanceManager(distance, fuzzy, 1);
    }
}
