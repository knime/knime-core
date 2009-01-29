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
 *   Jun 8, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.FuzzyIntervalValue;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class SotaFuzzyMath {
    private SotaFuzzyMath() {
    }

    /**
     * Computes the maximal distance between the center of the core region and
     * the end of the core region, by deviding difference of MaxCore and MinCore
     * by 2, and returns it.
     * 
     * @param val the Fuzzy Cell Value to compute center of core region for
     * @return the center of the given FuzzyCells core region
     */
    public static double getMaxCoreDistanceToCenter(
            final FuzzyIntervalValue val) {
        return (val.getMaxCore() - val.getMinCore()) / 2;
    }

    /**
     * Computes the center of the FuzzyCells core region, by adding MaxCore and
     * MinCore and dividing the result by 2.
     * 
     * @param val the Fuzzy Cell to compute center of gravity for
     * @return the center of gravity of a core region
     */
    public static double getCenterOfCoreRegion(final FuzzyIntervalValue val) {
        if (val != null) {
            return (val.getMaxCore() + val.getMinCore()) / 2;
        }
        return 0;
    }

    /**
     * Approximates dilatation of Core region, by using Pythagoras. 
     * Dilatation d = (sum(ai))^(1/2), with ai = (Cmax - Cmin) / 2. 
     * If cell-array length is less or equal 0, than -1 is returned.
     * 
     * @param vals array of cells of N-dimensional Fuzzy Set to approximate core
     *            dilatation
     * @return core dilatation of given FuzzyIntervalCells. If cell-array length
     *         is less or equal 0, than -1 is returned.
     */
    public static double getMaxCoreDilatation(final FuzzyIntervalValue[] vals) {
        double d = -1;
        if (vals.length > 0) {
            d = 0;
            for (int i = 0; i < vals.length; i++) {
                d += Math.pow(
                        SotaFuzzyMath.getMaxCoreDistanceToCenter(vals[i]), 2);
            }
            d = Math.sqrt(d);
        }
        return d;
    }

    /**
     * Approximates dilatation of Core region, by using Pythagoras. 
     * Dilatation d = (sum(ai))^(1/2), with ai = (Cmax - Cmin) / 2. 
     * -1 is returned if the given DataRow contains no FuzzyIntervalCells.
     * 
     * @param cells row which contains FuzzyIntervalCells
     * @param spec spec of the row, to see which cells are FuzzyIntervalCells
     * @return core dilatation of given FuzzyIntervalCells. If the row contains
     *         no FuzzyIntervalCells -1 is returned.
     */
    public static double getMaxCoreDilatation(final DataRow cells,
            final DataTableSpec spec) {
        double d = -1;
        if (cells.getNumCells() > 0) {
            d = 0;
            for (int i = 0; i < cells.getNumCells(); i++) {
                DataType type = spec.getColumnSpec(i).getType();
                if (type.isCompatible(FuzzyIntervalValue.class)) {
                    if (!(cells.getCell(i).isMissing())) {
                        d += Math.pow(SotaFuzzyMath.getMaxCoreDistanceToCenter(
                                (FuzzyIntervalValue)cells.getCell(i)), 2);
                    }
                }
            }
            d = Math.sqrt(d);
        }
        return d;
    }

    // /**
    // * Calculates the distance between the core regions of two
    // * FuzzyIntervalCells. If the number of FuzzyIntervalCells of the given
    // * DataRows is not equal -1 is returned. To calculate the distance the
    // * method computes for each cell of the given cells the center of its core
    // * region, this results in two vectors. Than the euclidean distance
    // * between these vectors is calculated and returned.
    // * @param cells1 The first DataRow which contains FuzzyIntervalCells
    // * @param cells2 The second DataRow which contains FuzzyIntervalCells
    // * @param spec DataTableSpec of DataRows, to see which cells are
    // * FuzzyIntervalCells
    // * @return The euclidean distance btwn the core regions of set of cells,
    // * if the number of FuzzyIntervalCells is greater than 0.
    // * Else -1 is returned.
    // */
    // public static double getCoreDistance(final DataRow cells1,
    // final DataRow cells2, final DataTableSpec spec) {
    // double distance = -1;
    //        
    // // Check is given DataRow have the same length
    // // If not return -1;
    // if (cells1.getNumCells() != cells2.getNumCells()) {
    // return distance;
    // }
    //        
    // double[] centerOfCoreRegionVector1 =
    // SotaFuzzyMath.getCenterOfAllCoreRegions(cells1, spec);
    // double[] centerOfCoreRegionVector2 =
    // SotaFuzzyMath.getCenterOfAllCoreRegions(cells2, spec);
    //        
    // if (centerOfCoreRegionVector1.length
    // == centerOfCoreRegionVector2.length) {
    // // Calculate distance between vectors of centers of core regions
    // distance = MDSEuclideanAlgorithm.getDistance(
    // centerOfCoreRegionVector1, centerOfCoreRegionVector2);
    // }
    //        
    // return distance;
    // }

    /**
     * Computes the center vector of all core regions of the given FuzzyCells as
     * a double array. If the row contains no FuzzyCell <code>null</code> is
     * returned.
     * 
     * @param cells FuzzyCells to compute the center of the core regions
     * @param spec DataTableSpec of rows, to see which cells are
     *            FuzzyIntervalCells
     * @return the vector of the center of all core regions of the given
     *         FuzzyCells as a double array. If row contains no FuzzyCells
     *         <code>null</code> is returned.
     */
    public static double[] getCenterOfAllCoreRegions(final DataRow cells,
            final DataTableSpec spec) {

        int fuzzyCellCount = SotaFuzzyMath.getNumberOfFuzzyCells(cells, spec);

        // Check if there are more than 0 FuzzyIntervalCells.
        // If not return null.
        if (fuzzyCellCount <= 0) {
            return null;
        }

        double[] centerOfCoreRegionVector = new double[fuzzyCellCount];

        // Store centers of core regions of each FuzzyIntervalCell
        for (int i = 0; i < cells.getNumCells(); i++) {
            DataType type = spec.getColumnSpec(i).getType();
            if (type.isCompatible(FuzzyIntervalValue.class)) {
                centerOfCoreRegionVector[i] = SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)cells
                                .getCell(i));
            }
        }
        return centerOfCoreRegionVector;
    }

    /**
     * Counts the number of FuzzyIntervalValues of given row and returns it.
     * 
     * @param cells DataRow to count number of FuzzyIntervalValues
     * @param spec DataTableSpec of given row to get information about Types of
     *            cell in row
     * @return the number of FuzzyIntervalValues of given row
     */
    public static int getNumberOfFuzzyCells(final DataRow cells,
            final DataTableSpec spec) {
        // Count the number of FuzzyIntervalCells
        int fuzzyCellCount = 0;
        for (int i = 0; i < cells.getNumCells(); i++) {
            DataType type = spec.getColumnSpec(i).getType();
            if (type.isCompatible(FuzzyIntervalValue.class)) {
                fuzzyCellCount++;
            }
        }
        return fuzzyCellCount;
    }

    /**
     * Computes the core dilatation of a core region to another core region.
     * 
     * @param cells1 core region to compute dilataion for
     * @param cells2 core region which indicates the direction
     * @param spec DataTableSpec of row to get information about types of
     *            DataCells
     * @return dilatation of core region of cells1, with respect to the
     *         direction indicated by cells2
     */
    public static double getCoreDilatationToOtherCore(final DataRow cells1,
            final DataRow cells2, final DataTableSpec spec) {

        double dilatation = -1;
        int fuzzyCellCount1 = SotaFuzzyMath.getNumberOfFuzzyCells(cells1, spec);
        int fuzzyCellCount2 = SotaFuzzyMath.getNumberOfFuzzyCells(cells2, spec);

        // Check if number of FuzzyIntervalValues of given Cells is equal and
        // greater 0.
        // If not return -1.
        if (fuzzyCellCount1 != fuzzyCellCount2 && fuzzyCellCount1 <= 0) {
            return dilatation;
        }
        dilatation = 0;
        double alternativeDilatation = 0;

        double[] fuzzyCells1CoreRegion = SotaFuzzyMath
                .getCenterOfAllCoreRegions(cells1, spec);

        // Through all dimensions
        int currentFuzzyCell = 0;
        for (int d = 0; d < cells1.getNumCells(); d++) {
            DataType type = spec.getColumnSpec(d).getType();
            if (type.isCompatible(FuzzyIntervalValue.class)) {
                FuzzyIntervalValue fuzzyCell1 = (FuzzyIntervalValue)cells1
                        .getCell(d);
                FuzzyIntervalValue fuzzyCell2 = (FuzzyIntervalValue)cells2
                        .getCell(d);

                //
                // / Do they differ in that dimension "d" ?
                //
                // Cell1 is left to Cell2
                if (fuzzyCell2.getMinCore() > fuzzyCell1.getMaxCore()) {
                    dilatation += Math.pow(Math
                            .abs(fuzzyCells1CoreRegion[currentFuzzyCell]
                                    - fuzzyCell1.getMaxCore()), 2);
                } else if (fuzzyCell1.getMinCore() > fuzzyCell2.getMaxCore()) {
                    // Cell1 is right to Cell2
                    dilatation += Math.pow(Math
                            .abs(fuzzyCells1CoreRegion[currentFuzzyCell]
                                    - fuzzyCell1.getMinCore()), 2);
                } else if (fuzzyCell2.getMinCore() <= fuzzyCell1.getMaxCore()
                        && fuzzyCell2.getMinCore() >= fuzzyCell1.getMinCore()) {
                    //
                    // / If they overlap in any dimension
                    //
                    // Cell1 is left to Cell2 with overlap on the right
                    alternativeDilatation += Math.pow(Math
                            .abs(fuzzyCells1CoreRegion[currentFuzzyCell]
                                    - fuzzyCell1.getMaxCore()), 2);
                } else if (fuzzyCell1.getMinCore() <= fuzzyCell2.getMaxCore()
                        && fuzzyCell1.getMinCore() >= fuzzyCell2.getMinCore()) {
                    // Cell1 is right to Cell2 with overlap on the left
                    alternativeDilatation += Math.pow(Math
                            .abs(fuzzyCells1CoreRegion[currentFuzzyCell]
                                    - fuzzyCell1.getMinCore()), 2);
                } else if (fuzzyCell2.getMaxCore() > fuzzyCell1.getMaxCore()
                        && fuzzyCell2.getMinCore() < fuzzyCell1.getMinCore()) {
                    // Cell1 is in Cell2
                    alternativeDilatation += Math.pow(Math
                            .abs(fuzzyCells1CoreRegion[currentFuzzyCell]
                                    - fuzzyCell1.getMaxCore()), 2);
                } else if (fuzzyCell1.getMaxCore() > fuzzyCell2.getMaxCore()
                        && fuzzyCell1.getMinCore() < fuzzyCell2.getMinCore()) {
                    // Cell2 is in Cell1
                    alternativeDilatation += Math.pow(Math
                            .abs(fuzzyCells1CoreRegion[currentFuzzyCell]
                                    - fuzzyCell1.getMaxCore()), 2);
                }

                currentFuzzyCell++;
            }
        }

        if (dilatation > 0) {
            return Math.sqrt(dilatation);
        }
        return Math.sqrt(alternativeDilatation);
    }
}
