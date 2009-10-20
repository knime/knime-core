/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   02.02.2007 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds.distances;


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
     * <code>DistanceManager</code> is returned. If you want to compute i.e. 
     * euclidean distances, then use
     * <code>DistanceManagerFactory.EUCLIDEAN_DIST</code> as distance parameter 
     * and the <code>EuclideanDistanceManager</code> is returned. If an unvalid
     * kind of distance is given null will be returned.
     * The fuzzy parameter specifies if the created <code>DistanceManager</code>
     * will compute distances between <code>FuzzyIntervalCell</code>s or
     * <code>DataCell</code>s containing numbers.
     * The offset parameter specifies a particular offset used i.e. by the
     * <code>CosinusDistanceManager</code>.
     * 
     * @param distance Specifies the concrete <code>DistanceManager</code>
     * implementation to be returned.
     * @param fuzzy If true the <code>DistanceManager</code> will compute
     * distances between <code>FuzzyIntervalCell</code>s.
     * @param offset A particular offset use by i.e. the
     * <code>CosinusDistanceManager</code> 
     * @param ignoreType If set <code>true</code> the type 
     * (fuzzy or number) will be ignored. When dealing with fuzzy values the 
     * center of gravity is used, otherwise the numerical value.
     * 
     * @return A particular <code>DistanceManager</code>, specified by the
     * distance parameter.
     */
    public static final DistanceManager createDistanceManager(
            final String distance, final boolean fuzzy, final double offset,
            final boolean ignoreType) {
        if (distance.equals(DistanceManagerFactory.EUCLIDEAN_DIST)) {
            EuclideanDistanceManager dm = new EuclideanDistanceManager(fuzzy);
            dm.setIgnoreType(ignoreType);
            return dm;
        } else if (distance.equals(DistanceManagerFactory.COS_DIST)) {
            return new CosinusDistanceManager(offset, fuzzy);
        } else if (distance.equals(DistanceManagerFactory.MANHATTAN_DIST)) {
            ManhattanDistanceManager dm = new ManhattanDistanceManager(fuzzy);
            dm.setIgnoreType(ignoreType);
            return dm;
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
     * The types will not be ignored by default.
     * 
     * @param distance Specifies the concrete <code>DistanceManager</code>
     * implementation to be returned.
     * 
     * @return A particular <code>DistanceManager</code>, specified by the
     * distance parameter.
     */
    public static final DistanceManager createDistanceManager(
            final String distance) {
        return DistanceManagerFactory.createDistanceManager(distance, false, 1,
                false);
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
        return DistanceManagerFactory.createDistanceManager(distance, fuzzy, 1, 
                false);
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
     * @param ignoreCase If <code>true</code>The type (fuzzy or number) 
     * will be ignored. When dealing with fuzzy values the 
     * center of gravity is used, otherwise the numerical value. 
     * 
     * @return A particular <code>DistanceManager</code>, specified by the
     * distance parameter.
     */    
    public static final DistanceManager createDistanceManager(
            final String distance, final boolean fuzzy, 
            final boolean ignoreCase) {
        return DistanceManagerFactory.createDistanceManager(distance, fuzzy, 
                1, ignoreCase);
    }    
}
