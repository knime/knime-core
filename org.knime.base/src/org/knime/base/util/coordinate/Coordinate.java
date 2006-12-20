/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   02.02.2006 (sieb): created
 */
package org.knime.base.util.coordinate;

import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;


/**
 * The abstract class for all coordinate classes. A concrete coordinate depends
 * on whether it is nominal or numeric, etc. All coordinates have an underlying
 * {@link org.knime.core.data.DataColumnSpec}. Ticks have to be created
 * and mapped to their domain values.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class Coordinate {

    /**
     * The underlying data column spec of this coordinate.
     */
    private DataColumnSpec m_columnSpec;

    /**
     * Factory method to create a coordinate for a given column spec. The type
     * of the column is determined and dependent on that the corresponding
     * coordinate is created.
     * 
     * @param dataColumnSpec the column spec to create the coordinate from
     * @return the created coordinate, <code>null</code> if not possible
     */
    public static Coordinate createCoordinate(
            final DataColumnSpec dataColumnSpec) {
        // check the column type first it must be compatible to a double
        // value to be a numeric coordinate
        if (dataColumnSpec == null) {
            return null;
        }
        DataType type = dataColumnSpec.getType();
            if (type.isCompatible(IntValue.class)) {
             return new IntegerCoordinate(dataColumnSpec);
        } else
        if (type.isCompatible(DoubleValue.class)) {
            return new DoubleCoordinate(dataColumnSpec);
        } else {
            Set<DataCell> possibleValues = dataColumnSpec.getDomain()
                .getValues();
            if (possibleValues != null && possibleValues.size() > 0) {
                return new NominalCoordinate(dataColumnSpec);
            }
        }

        // else return null
        return null;
    }

    /**
     * Creates a coordinate from a data column spec.
     * 
     * @param dataColumnSpec the underlying column spec to set
     */
    Coordinate(final DataColumnSpec dataColumnSpec) {
        if (dataColumnSpec == null) {
            throw new IllegalArgumentException("Column specification shouldn't"
                    + " be null.");
        }
        m_columnSpec = dataColumnSpec;
    }

    /**
     * @return the underlying column spec of this coordinate
     */
    DataColumnSpec getDataColumnSpec() {
        return m_columnSpec;
    }

    /**
     * Returns an array with the position of all ticks and their corresponding
     * domain values given an absolute length.
     * 
     * @param absolutLength the absolute length the domain is mapped on
     * @param naturalMapping if <code>true</code> the mapping values are
     *            rounded to the next integer equivalent
     * 
     * @return the mapping of tick positions and corresponding domain values
     */
    public abstract CoordinateMapping[] getTickPositions(
            final double absolutLength, final boolean naturalMapping);

    /**
     * Returns the mapping of a domain value for this coordinate axis. The
     * mapping is done according to the given absolute length.
     * <p>
     * The value is not the position on the screen. Since the java coordinate
     * system is upside down simply subtract the returned value from the screen 
     * height to calculate the screen position.
     * 
     * @param domainValueCell the data cell with the domain value to map
     * @param absolutLength the absolute length on which the domain value is
     *            mapped on
     * @param naturalMapping if true the return value will be a double but with
     *            zeros after the decimal dot
     * 
     * @return the mapped value
     */
    public abstract double calculateMappedValue(final DataCell domainValueCell,
            final double absolutLength, final boolean naturalMapping);

    /**
     * Whether this coordinate is a nominal one. Nominal coordinates must be
     * treated differently in some cases, i.e. when rendering in a scatterplott
     * nominal values are very likely to be drawn above each other which
     * requires jittering.
     * 
     * @return <code>true</code>, if this coordinate is a nominal one
     */
    public abstract boolean isNominal();

    /**
     * Returns the range according to the mapping in which no values can have
     * values. This distance will not occur in floating point numbers. For
     * nominal values it is most likely to occur. For discrete values like
     * integers, it will happen when the integer range is smaller than the
     * available pixels.
     * 
     * @param absoluteLength the absolute length available for this coordinate
     * 
     * @return the unused mapping range per domain value
     */
    public abstract double getUnusedDistBetweenTicks(double absoluteLength);
}
