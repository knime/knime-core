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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 21, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.distances;

import org.knime.base.node.mine.sota.logic.SotaFuzzyMath;
import org.knime.base.node.mine.sota.logic.SotaTreeCell;
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
    private Distances() {
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

            if (SotaUtil.isNumberType(type1) && SotaUtil.isNumberType(type2)
                    && !fuzzy) {
                distance += Math.pow((((DoubleValue)row1.getCell(i))
                        .getDoubleValue() - ((DoubleValue)row2.getCell(i))
                        .getDoubleValue()), power);
            } else if (SotaUtil.isFuzzyIntervalType(type1)
                    && SotaUtil.isFuzzyIntervalType(type2) && fuzzy) {
                distance += Math.pow((SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i)) - SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                .getCell(i))), power);
            }
        }
        return Math.pow(distance, (double)1 / (double)power);
    }
    
    /**
     * Calculates the Minkowski distance between a regular <code>DataRow</code>
     * and a <code>SotaTreeCell</code>. If fuzzy is set true only columns with 
     * cells containing numbers are used to compute the distance. If the number
     * of columns, which are used to compute the distance, contained in the 
     * given <code>DataRow</code> is different to the number of cells contained
     * in the given <code>SotaTreeCell</code>, only the first <i>n</i> columns
     * of the <code>DataRow</code> or <i>n</i> cells of the 
     * <code>SotaTreeCell</code> are used to compute the distance. The rest is
     * simply ignored.
     * The given power specifies the distance kind, i.e. if power is set to 2 
     * the euclidean distance will be computed.
     * 
     * @param power The power to use.
     * @param row The row to compute the distance.
     * @param cell The cell to compute the distance.
     * @param fuzzy If true only fuzzy data is taken into account, if 
     * <code>false</code> only number data.
     * 
     * @return Minkowski distance between the two rows.
     */    
    public static double getMinkowskiDistance(final int power, 
            final DataRow row, final SotaTreeCell cell, final boolean fuzzy) {
        int col = 0;
        double distance = 0;
        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                if (col < cell.getData().length) {
                    distance += Math.pow(
                            (cell.getData()[col].getValue() - ((DoubleValue)row
                                    .getCell(i)).getDoubleValue()), power);
                    col++;
                }
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                if (col < cell.getData().length) {
                    distance += Math.pow(cell.getData()[col].getValue() 
                            - SotaFuzzyMath.getCenterOfCoreRegion(
                                    (FuzzyIntervalValue)row.getCell(i)), power);
                    col++;
                }
            }
        }
        return Math.pow(distance, (double)1 / (double)power);
    }
    
    /**
     * Returns the euclidean distance between a given <code>DataRow</code>
     * and <code>SotaTreeCell</code> using the Minkowski distance with 
     * power 2.
     * @see Distances#getMinkowskiDistance(int, DataRow, DataRow, boolean)
     * 
     * @param row row to compute the distance
     * @param cell cell to compute the distance
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the euclidian distance between given row and cell
     */
    public static double getEuclideanDistance(final DataRow row,
            final SotaTreeCell cell, final boolean fuzzy) {
        return Distances.getMinkowskiDistance(2, row, cell, fuzzy);
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
     * Returns the manhattan distance between a given <code>DataRow</code>
     * and <code>SotaTreeCell</code> using the Minkowski distance with 
     * power 1.
     * @see Distances#getMinkowskiDistance(int, DataRow, DataRow, boolean)
     * 
     * @param row row to compute the distance
     * @param cell cell to compute the distance
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the euclidian distance between given row and cell
     */
    public static double getManhattanDistance(final DataRow row,
            final SotaTreeCell cell, final boolean fuzzy) {
        return Distances.getMinkowskiDistance(1, row, cell, fuzzy);
    }

    /**
     * Calculates the manhattan distance between two <code>DataRow</code>s
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

                vectorMultRes += ((DoubleValue)row1.getCell(i))
                        .getDoubleValue()
                        * ((DoubleValue)row2.getCell(i)).getDoubleValue();

                vector1Length += Math.pow(((DoubleValue)row1.getCell(i))
                        .getDoubleValue(), 2);

                vector2Length += Math.pow(((DoubleValue)row2.getCell(i))
                        .getDoubleValue(), 2);

            } else if (SotaUtil.isFuzzyIntervalType(type1)
                    && SotaUtil.isFuzzyIntervalType(type2) && fuzzy) {

                vectorMultRes += SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i))
                        * SotaFuzzyMath
                                .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                        .getCell(i));

                vector1Length += Math.pow(SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i)), 2);

                vector2Length += Math.pow(SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                .getCell(i)), 2);
            }
        }

        vector1Length = Math.sqrt(vector1Length);
        vector2Length = Math.sqrt(vector2Length);
        distance = vectorMultRes / (vector1Length * vector2Length);

        distance = offset - distance;
        return distance;
    }

    /**
     * Returns the cosinus distance between the cells values and the number
     * cells of the given row with a given offset.
     * 
     * @param row row to compute the cosinus distance of
     * @param cell cell to compute the cosinus distance of
     * @param offset offset to substract cosinus distance from
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the cosinus distance between given row and cell
     */
    public static double getCosinusDistance(final DataRow row,
            final SotaTreeCell cell, final double offset, final boolean fuzzy) {
        int col = 0;
        double distance = 0;
        double vectorMultRes = 0;
        double vectorLength = 0;
        double cellLength = 0;
        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                if (col < cell.getData().length) {
                    vectorMultRes += cell.getData()[col].getValue()
                            * ((DoubleValue)row.getCell(i)).getDoubleValue();

                    vectorLength += Math.pow(((DoubleValue)row.getCell(i))
                            .getDoubleValue(), 2);

                    cellLength += Math.pow(cell.getData()[col].getValue(), 2);

                    col++;
                }
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                if (col < cell.getData().length) {
                    vectorMultRes += cell.getData()[col].getValue() 
                        * SotaFuzzyMath.getCenterOfCoreRegion(
                                (FuzzyIntervalValue)row.getCell(i));

                    vectorLength += Math.pow(SotaFuzzyMath
                            .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                    .getCell(i)), 2);

                    cellLength += Math.pow(cell.getData()[col].getValue(), 2);

                    col++;
                }
            }
        }

        vectorLength = Math.sqrt(vectorLength);
        cellLength = Math.sqrt(cellLength);
        distance = vectorMultRes / (vectorLength * cellLength);

        distance = offset - distance;
        return distance;
    }

    /**
     * Returns the coefficient of correlation distance between the cells values
     * and the number cells of the given row with a given offset.
     * 
     * @param row row to compute the coefficient of correlation
     * @param cell cell to compute the coefficient of correlation
     * @param offset offset to substract coefficient of correlation from
     * @param abs flags if correlations distance should be used absolute
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the coefficient of correlation between given row and cel
     */
    public static double getCorrelationDistance(final DataRow row,
            final SotaTreeCell cell, final double offset, final boolean abs,
            final boolean fuzzy) {
        int col = 0;
        double dist = 0;
        double meanRow = Distances.getMean(row, fuzzy);
        double meanCell = Distances.getMean(cell);
        double devRow = Distances.getStandardDeviation(row, fuzzy);
        double devCell = Distances.getStandardDeviation(cell);

        if (devRow == 0 || devCell == 0) {
            return (offset - 0);
        }

        int count = 0;

        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                if (col < cell.getData().length) {
                    dist += (cell.getData()[col].getValue() - meanCell)
                            * (((DoubleValue)row.getCell(i)).getDoubleValue() 
                                    - meanRow);
                    col++;
                    count++;
                }
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                if (col < cell.getData().length) {
                    dist += (cell.getData()[col].getValue() - meanCell)
                            * (SotaFuzzyMath.getCenterOfCoreRegion(
                                    (FuzzyIntervalValue)row.getCell(i)) 
                                    - meanRow);
                    col++;
                    count++;
                }
            }
        }

        dist = offset - (dist / (count * devRow * devCell));
        if (abs) {
            dist = Math.abs(dist);
        }

        return dist;
    }

    /**
     * Returns the coefficient of correlation distance between the rows with a
     * given offset.
     * 
     * @param row1 first row to compute the coefficient of correlation
     * @param row2 second rell to compute the coefficient of correlation
     * @param offset offset to substract coefficient of correlation from
     * @param abs flags if correlations distance should be used absolute
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the coefficient of correlation between given rows
     */
    public static double getCorrelationDistance(final DataRow row1,
            final DataRow row2, final double offset, final boolean abs,
            final boolean fuzzy) {
        double dist = 0;
        double meanRow1 = Distances.getMean(row1, fuzzy);
        double meanRow2 = Distances.getMean(row2, fuzzy);
        double devRow1 = Distances.getStandardDeviation(row1, fuzzy);
        double devRow2 = Distances.getStandardDeviation(row2, fuzzy);

        if (devRow1 == 0 || devRow2 == 0) {
            return (offset - 0);
        }

        int count = 0;

        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType type = row1.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                dist += (((DoubleValue)row1.getCell(i)).getDoubleValue() 
                        - meanRow1) * (((DoubleValue)row2.getCell(i))
                                .getDoubleValue() - meanRow2);
                count++;
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                dist += (SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i)) - meanRow1)
                        * (SotaFuzzyMath
                                .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                        .getCell(i)) - meanRow2);
                count++;
            }
        }

        dist = offset - (dist / (count * devRow1 * devRow2));
        if (abs) {
            dist = Math.abs(dist);
        }

        return dist;
    }

    /**
     * Returns the standard deviation of the given row.
     * 
     * @param row the row to compute the standard deviation of.
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the standard deviation of the given row
     */
    public static double getStandardDeviation(final DataRow row,
            final boolean fuzzy) {
        double dev = 0;
        int count = 0;
        double mean = Distances.getMean(row, fuzzy);

        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                dev += Math.pow((((DoubleValue)row.getCell(i)).getDoubleValue() 
                        - mean), 2);
                count++;
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                dev += Math.pow((SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                .getCell(i)) - mean), 2);
                count++;
            }
        }
        return Math.sqrt((dev / (count - 1)));
    }

    /**
     * Returns the standard deviation of the given cell.
     * 
     * @param cell the SotaTreeCell to compute the standard deviation of
     * @return the standard deviation of the given cell
     */
    public static double getStandardDeviation(final SotaTreeCell cell) {
        double dev = 0;
        int count = 0;
        double mean = Distances.getMean(cell);

        for (int i = 0; i < cell.getData().length; i++) {
            dev += Math.pow((cell.getData()[i].getValue() - mean), 2);
            count++;
        }
        return Math.sqrt((dev / (count - 1)));
    }

    /**
     * Returns the mean value of the given row.
     * 
     * @param row row to get the mean value of
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the mean value of the given row
     */
    public static double getMean(final DataRow row, final boolean fuzzy) {
        double mean = 0;
        int count = 0;

        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                mean += ((DoubleValue)row.getCell(i)).getDoubleValue();
                count++;
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                mean += SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                .getCell(i));
                count++;
            }
        }
        return (mean / count);
    }

    /**
     * Returns the mean value of the given cell.
     * 
     * @param cell SotaTreeCell to get the mean value of
     * @return the mean value of the given cell
     */
    public static double getMean(final SotaTreeCell cell) {
        double mean = 0;
        int count = 0;

        for (int i = 0; i < cell.getData().length; i++) {
            mean += cell.getData()[i].getValue();
            count++;
        }
        return (mean / count);
    }
}
