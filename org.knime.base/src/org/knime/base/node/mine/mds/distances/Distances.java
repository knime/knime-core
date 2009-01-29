/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 21, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds.distances;

import org.knime.base.node.mine.mds.DataPoint;
import org.knime.base.node.mine.sota.logic.SotaFuzzyMath;
import org.knime.base.node.mine.sota.logic.SotaUtil;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class Distances {
    private Distances() { }

    /**
     * Calculates the Minkowski distance between two <code>DataPoint</code>s. 
     * The given power specifies the distance kind, i.e. if power 
     * is set to 2 the euclidean distance will be computed.
     * 
     * @param power The power to use.
     * @param point1 The first point
     * @param point2 The second point
     * 
     * @return Minkowski distance between the two points.
     */
    public static double getMinkowskiDistance(final int power, 
            final DataPoint point1, final DataPoint point2) {
        double distance = 0;
        for (int i = 0; i < point1.size(); i++) {
            distance += Math.pow(Math.abs(
                    point1.getElementAt(i) - point2.getElementAt(i)), 
                    power);
        }
        return Math.pow(distance, (double)1 / (double)power);
    }    
    
    /**
     * Calculates the Minkowski distance between two rows. If fuzzy is set true
     * only columns with cells containing numbers are used to compute the 
     * distance. The given power specifies the distance kind, i.e. if power 
     * is set to 2 the euclidean distance will be computed.
     * 
     * @param power The power to use.
     * @param row1 The first row
     * @param row2 The second row
     * @param fuzzy If true only fuzzy data is taken into account, if 
     * <code>false</code> only number data.
     * 
     * @return Minkowski distance between the two rows.
     */
    public static double getMinkowskiDistance(final int power, 
            final DataRow row1, final DataRow row2, final boolean fuzzy) {
        double distance = 0;
        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType type1 = row1.getCell(i).getType();
            DataType type2 = row2.getCell(i).getType();

            if (row1.getCell(i).isMissing() || row2.getCell(i).isMissing()) {
                continue;
            }
            
            if (SotaUtil.isNumberType(type1) && SotaUtil.isNumberType(type2)
                    && !fuzzy) {
                distance += Math.pow(Math.abs(
                        ((DoubleValue)row1.getCell(i)).getDoubleValue() 
                        - ((DoubleValue)row2.getCell(i)).getDoubleValue())
                        , power);
            } else if (SotaUtil.isFuzzyIntervalType(type1)
                    && SotaUtil.isFuzzyIntervalType(type2) && fuzzy) {
                distance += Math.pow(Math.abs(
                        SotaFuzzyMath.getCenterOfCoreRegion(
                                (FuzzyIntervalValue)row1.getCell(i)) 
                        - SotaFuzzyMath.getCenterOfCoreRegion(
                                (FuzzyIntervalValue)row2.getCell(i))),
                                power);
            }
        }
        return Math.pow(distance, (double)1 / (double)power);
    }

    /**
     * Calculates the Minkowski distance between two rows no matter if they
     * contain fuzzy or number values. If they contain fuzzy values, the center
     * of gravity is used as number value, if they contain number values
     * the number is used as value.
     * The given power specifies the distance kind, i.e. if power 
     * is set to 2 the euclidean distance will be computed.
     * 
     * @param power The power to use.
     * @param row1 The first row
     * @param row2 The second row
     * @return Minkowski distance between the two rows.
     */    
    public static double getMinkowskiDistance(final int power, 
            final DataRow row1, final DataRow row2) {
        double distance = 0;
        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType type1 = row1.getCell(i).getType();
            DataType type2 = row2.getCell(i).getType();

            if (row1.getCell(i).isMissing() || row2.getCell(i).isMissing()) {
                continue;
            }
            
            double value1 = 0;
            double value2 = 0;
            
            if (SotaUtil.isNumberType(type1)) {
                value1 = ((DoubleValue)row1.getCell(i)).getDoubleValue();
            } else if (SotaUtil.isFuzzyIntervalType(type1)) {
                value1 = SotaFuzzyMath.getCenterOfCoreRegion(
                        (FuzzyIntervalValue)row1.getCell(i)); 
            }
            
            if (SotaUtil.isNumberType(type2)) {
                value2 = ((DoubleValue)row2.getCell(i)).getDoubleValue();
            } else if (SotaUtil.isFuzzyIntervalType(type2)) {
                value2 = SotaFuzzyMath.getCenterOfCoreRegion(
                        (FuzzyIntervalValue)row2.getCell(i)); 
            }
            
            distance += Math.pow(Math.abs(value1 - value2), power);
        }
        return Math.pow(distance, (double)1 / (double)power);
    }    
    
    
    
    /**
     * Calculates the euclidean distance between two <code>DataRow</code>s
     * using the Minkowski distance with power 2.
     * @see Distances#getMinkowskiDistance(int, DataRow, DataRow, boolean)
     * 
     * @param row1 the first row
     * @param row2 the second row
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return distance between the two rows
     */
    public static double getEuclideanDistance(final DataRow row1,
            final DataRow row2, final boolean fuzzy) {
        return Distances.getMinkowskiDistance(2, row1, row2, fuzzy);
    }
    
    /**
     * Calculates the euclidean distance between two <code>DataRow</code>s
     * using the Minkowski distance with power 2.
     * @see Distances#getMinkowskiDistance(int, DataRow, DataRow)
     * 
     * @param row1 the first row
     * @param row2 the second row
     * @return distance between the two rows
     */
    public static double getEuclideanDistance(final DataRow row1,
            final DataRow row2) {
        return Distances.getMinkowskiDistance(2, row1, row2);
    }    
    
    /**
     * Calculates the euclidean distance between two <code>DataPoints</code>s
     * using the Minkowski distance with power 2.
     * @see Distances#getMinkowskiDistance(int, DataPoint, DataPoint)
     * 
     * @param point1 The first point
     * @param point2 The second point
     * @return distance between the two rows
     */
    public static double getEuclideanDistance(final DataPoint point1,
            final DataPoint point2) {
        return Distances.getMinkowskiDistance(2, point1, point2);
    }    

    /**
     * Calculates the Manhattan distance between two <code>DataRow</code>s
     * using the Minkowski distance with power 1.
     * @see Distances#getMinkowskiDistance(int, DataRow, DataRow, boolean)
     * 
     * @param row1 the first row
     * @param row2 the second row
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return distance between the two rows
     */
    public static double getManhattanDistance(final DataRow row1,
            final DataRow row2, final boolean fuzzy) {
        return Distances.getMinkowskiDistance(1, row1, row2, fuzzy);
    }
    
    /**
     * Calculates the Manhattan distance between two <code>DataRow</code>s
     * using the Minkowski distance with power 1.
     * @see Distances#getMinkowskiDistance(int, DataRow, DataRow)
     * 
     * @param row1 the first row
     * @param row2 the second row
     * @return distance between the two rows
     */
    public static double getManhattanDistance(final DataRow row1,
            final DataRow row2) {
        return Distances.getMinkowskiDistance(1, row1, row2);
    }
    
    /**
     * Calculates the Manhattan distance between two <code>DataPoints</code>s
     * using the Minkowski distance with power 1.
     * @see Distances#getMinkowskiDistance(int, DataPoint, DataPoint)
     * 
     * @param point1 The first point
     * @param point2 The second point
     * @return distance between the two rows
     */
    public static double getManhattanDistance(final DataPoint point1,
            final DataPoint point2) {
        return Distances.getMinkowskiDistance(1, point1, point2);
    }
    
    
    /**
     * Computes the cosinus distance between the given two rows, with given
     * offset.
     * 
     * @param row1 first row to compute the cosinus distance of
     * @param row2 second row to compute the cosinus distance of
     * @param offset offset to substract cosinus distance from
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the cosinus distance between the given two rows
     */
    public static double getCosinusDistance(final DataRow row1,
            final DataRow row2, final double offset, final boolean fuzzy) {
        double distance = 0;
        double vectorMultRes = 0;
        double vector1Length = 0;
        double vector2Length = 0;
        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType type1 = row1.getCell(i).getType();
            DataType type2 = row2.getCell(i).getType();

            if (SotaUtil.isNumberType(type1) && SotaUtil.isNumberType(type2)
                    && !fuzzy) {

                vectorMultRes += ((DoubleValue)row1.getCell(i)).getDoubleValue()
                        * ((DoubleValue)row2.getCell(i)).getDoubleValue();

                vector1Length += Math.pow(((DoubleValue)row1.getCell(i))
                        .getDoubleValue(), 2);

                vector2Length += Math.pow(((DoubleValue)row2.getCell(i))
                        .getDoubleValue(), 2);

            } else if (SotaUtil.isFuzzyIntervalType(type1)
                    && SotaUtil.isFuzzyIntervalType(type2) && fuzzy) {

                vectorMultRes += SotaFuzzyMath.getCenterOfCoreRegion(
                        (FuzzyIntervalValue)row1.getCell(i))
                        * SotaFuzzyMath.getCenterOfCoreRegion(
                                (FuzzyIntervalValue)row2.getCell(i));

                vector1Length += Math.pow(SotaFuzzyMath.getCenterOfCoreRegion(
                        (FuzzyIntervalValue)row1.getCell(i)), 2);

                vector2Length += Math.pow(SotaFuzzyMath.getCenterOfCoreRegion(
                        (FuzzyIntervalValue)row2.getCell(i)), 2);
            }
        }

        vector1Length = Math.sqrt(vector1Length);
        vector2Length = Math.sqrt(vector2Length);
        distance = vectorMultRes / (vector1Length * vector2Length);

        if (offset != 0) {
            distance = offset - distance;
        }
        return distance;
    }
    
    
    /**
     * Computes the cosinus distance between the given two 
     * <code>DataPoint</code>s, with given offset.
     * 
     * @param point1 first point to compute the cosinus distance of
     * @param point2 second point to compute the cosinus distance of
     * @param offset offset to substract cosinus distance from

     * @return the cosinus distance between the given two rows
     */
    public static double getCosinusDistance(final DataPoint point1,
            final DataPoint point2, final double offset) {
        double distance = 0;
        double vectorMultRes = 0;
        double vector1Length = 0;
        double vector2Length = 0;
        
        for (int i = 0; i < point1.size(); i++) {
            vectorMultRes += point1.getElementAt(i) * point2.getElementAt(i);
            vector1Length += Math.pow(point1.getElementAt(i), 2);
            vector2Length += Math.pow(point2.getElementAt(i), 2);
        }

        vector1Length = Math.sqrt(vector1Length);
        vector2Length = Math.sqrt(vector2Length);
        distance = vectorMultRes / (vector1Length * vector2Length);

        if (offset != 0) {
            distance = offset - distance;
        }
        return distance;
    }    
}
