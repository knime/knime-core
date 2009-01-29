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
 *   01.02.2006 (sieb): created
 */
package org.knime.base.util.coordinate;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;

/**
 * This class represents a numeric coordinate defined by a given
 * {@link org.knime.core.data.DataColumnSpec}. The class provides functionality
 * for extension of coordinates beyond the domain length. Furthermore the label
 * ticks can be determined dependent on given properties. All these sizes are
 * normalized (0-1) but there are also methods to convert them to absolute
 * values given an absolute maximum length.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Stephan Sellien, University of Konstanz
 */
public class DoubleCoordinate extends NumericCoordinate {

    /**
     * The default value for <code>m_coordPrefix</code>.
     */
    private static final double DEFAULT_COORDINATE_PREFIX = 0;

    /**
     * The default value for <code>m_coordPostfix</code>.
     */
    private static final double DEFAULT_COORDINATE_POSTFIX = 0;

    /**
     * Default number of rounding digits.
     */
    private static final int DEFAULT_DOMAIN_LABEL_LENGTH = 6;

    /**
     * Default tick policy.
     */
    private static final String DEFAULT_TICK_POLICY = "Ascending";

    /**
     * An optional prefix (extension) of the coordinate beyond the domain
     * length. A value larger or equal 0 is expected and is assumed to be a
     * percent value.
     */
    // private double m_coordPrefix;
    /**
     * An optional postfix (extension) of the coordinate beyond the domain
     * length. A value larger or equal 0 is expected and is assumed to be a
     * percent value.
     */
    // private double m_coordPostfix;
    /**
     * The absolute tick distance is used when absolute values according to the
     * placement of a tick are requested. The absolute tick distance is
     * dependent on the usage.
     */
    private double m_absoluteTickDistance;

    /**
     * The maximum length of a numeric domain label.
     */
    private int m_maxDomainLabelLength;

    /**
     * Constructs a Coordinate according to the given column spec and predefined
     * default values for the <code>coordinatePrefix</code>,
     * <code>coordinatePostfix</code> and the
     * <code>absoluteTickDistance</code>.
     *
     * @param dataColumnSpec the column spec to create this coordinate from
     */
    protected DoubleCoordinate(final DataColumnSpec dataColumnSpec) {
        this(dataColumnSpec, DEFAULT_COORDINATE_PREFIX,
                DEFAULT_COORDINATE_POSTFIX, DEFAULT_ABSOLUTE_TICK_DIST,
                DEFAULT_TICK_POLICY, DEFAULT_DOMAIN_LABEL_LENGTH);
    }

    /**
     * Constructs a coordinate from the given parameters.
     *
     * @param dataColumnSpec the column spec to create this coordinate from
     * @param coordinatePrefix the prefix to append before the domain size. A
     *            value larger or equal 0 is expected and is assumed to be a
     *            percent value.
     * @param coordinatePostfix the postfix to append after the end of the
     *            domain. A value larger or equal 0 is expected and is assumed
     *            to be a percent value.
     * @param absoluteTickDistance the distance used to calculate absolute tick
     *            positions when given an maximum length
     * @param tickPolicy the policy to determine the position of the ticks for
     *            this coordinate
     * @param maxDomainLabelLength the number digits after the decimal dot for
     *            rounding accuracy
     */
    DoubleCoordinate(final DataColumnSpec dataColumnSpec,
            final double coordinatePrefix, final double coordinatePostfix,
            final double absoluteTickDistance, final String tickPolicy,
            final int maxDomainLabelLength) {
        super(dataColumnSpec);

        // check the column type first it must be compatible to a double
        // value as it is the most general on
        DataType type = dataColumnSpec.getType();

        if (!type.isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException("The type of the given column "
                    + "is not applicable to a numeric coordinate: "
                    + type.toString());
        }

        // check and set the domain range
        DataColumnDomain domain = getDataColumnSpec().getDomain();
        if (domain == null) {

            // if there is no domain a coordinate makes no sense
            throw new IllegalArgumentException(
                    "The domain of the set column spec is null. "
                            + "Coordinate can not be created.");
        }

        DataCell lowerBound = domain.getLowerBound();
        if (lowerBound == null) {

            // if there is no lower bound a coordinate makes no sense
            throw new IllegalArgumentException(
                    "The lower bound of the set column spec is null. "
                            + "Coordinate can not be created.");
        } else {
            setMinDomainValue(((DoubleValue)lowerBound).getDoubleValue());
        }

        DataCell upperBound = domain.getUpperBound();
        if (upperBound == null) {

            // if there is no upper bound a coordinate makes no sense
            throw new IllegalArgumentException(
                    "The upper bound of the set column spec is null. "
                            + "Coordinate can not be created.");
        } else {
            setMaxDomainValue(((DoubleValue)upperBound).getDoubleValue());
        }

        // check the rounding accuracy
        if (maxDomainLabelLength < 0) {
            throw new IllegalArgumentException("The number of digits after "
                    + "the decimal point must be larger than 0");
        }
        m_maxDomainLabelLength = maxDomainLabelLength;

        // the pre- and postfix must be larger or equal to zero
        if (coordinatePostfix < 0 || coordinatePrefix < 0) {
            throw new IllegalArgumentException("The pre-(" + coordinatePrefix
                    + ") and postfix (" + coordinatePostfix + ")extension "
                    + "must be equal or larger than zero.");
        }
        // m_coordPrefix = coordinatePrefix;
        // m_coordPostfix = coordinatePostfix;

        // the tick distance must be larger than zero
        if (absoluteTickDistance <= 0) {
            throw new IllegalArgumentException(
                    "The tick distance must be larger than zero: "
                            + absoluteTickDistance);
        }
        m_absoluteTickDistance = absoluteTickDistance;

        setPolicy(getPolicyStategy(tickPolicy));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CoordinateMapping[] getTickPositionsInternal(
            final double absoluteLength) {
        PolicyStrategy strategy = getCurrentPolicy();
        if (strategy != null) {
            strategy.setValues(getDesiredValues());
            CoordinateMapping[] coordMapping =
                    strategy.getTickPositions(absoluteLength,
                            getMinDomainValue(), getMaxDomainValue(),
                            m_absoluteTickDistance, getNegativeInfinity(),
                            getPositiveInfinity());
            return coordMapping;
        }

        double middle = (getMaxDomainValue() - getMinDomainValue()) / 2;
        return new CoordinateMapping[]{new DoubleCoordinateMapping(
                formatNumber(middle), middle, (int)absoluteLength / 2)};
    }

    /**
     * Formats a number according to the maximum length allowed for a domain
     * label. This constraint is set from the higher level according to the
     * application.
     *
     * @param number the number to format
     * @return the formated number as string
     */
    public String formatNumber(final double number) {

        // get the largest absolute domain value
        double lowerBound = getMinDomainValue();
        double upperBound = getMaxDomainValue();
        double maxAbsLowerValue = Math.abs(lowerBound);
        double maxAbsUpperValue = Math.abs(upperBound);

        double maxAbsValue = 0;
        if (maxAbsLowerValue < maxAbsUpperValue) {
            maxAbsValue = maxAbsUpperValue;
        } else {
            maxAbsValue = maxAbsLowerValue;
        }
        // convert to string
        String numberAsString = Double.toString(maxAbsValue);
        // cut off decimal postfix
        int decimalPointPos = numberAsString.indexOf(".");
        if (decimalPointPos >= 0) {
            numberAsString = numberAsString.substring(0, decimalPointPos);
        }

        // the maximum prefix as number allowed given max label length
        // this is the power of 10 to the allowed length
        double maxNumber = Math.pow(10, m_maxDomainLabelLength);

        int prefixLength = numberAsString.length();
        String formatPattern = null;
        // if the prefix length is longer than the max domain label length
        // the pattern is switched to scientific notation
        if (maxAbsValue >= maxNumber) {

            StringBuffer afterDecimalDigits = new StringBuffer();
            // -4 corresponds to the mandatory first position, the decimal point
            // the E for the exponent and the exponent value (maybe it is larger
            // but this is rather a fuzzy formating)
            for (int i = 0; i < m_maxDomainLabelLength - 4; i++) {

                afterDecimalDigits.append("#");
            }

            // append the format string parts
            formatPattern = "0." + afterDecimalDigits + "E0";

        } else {

            // calculate the remaining digits for the decimal postfix
            // -1 is the decimal point
            int postfixLength = m_maxDomainLabelLength - prefixLength - 1;

            // create the postfix pattern according to the number postfix digits
            // each # represents a postfix digit
            StringBuffer postfixPatter = new StringBuffer();
            for (int i = 0; i < postfixLength; i++) {

                // the first digit appends the decimal point and a
                // "0" which forces the digit to print even if it is a zero
                if (i == 0) {
                    postfixPatter.append(".0");
                } else {
                    postfixPatter.append("#");
                }
            }

            formatPattern = "0" + postfixPatter.toString();
        }

        // format the number according to the pattern
        DecimalFormat format =
                (DecimalFormat)NumberFormat.getInstance(new Locale("en"));
        format.applyPattern(formatPattern);
        return format.format(number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double calculateMappedValueInternal(
            final DataCell domainValueCell, final double absoluteLength) {

        PolicyStrategy strategy = getCurrentPolicy();
        if (strategy != null) {
            return strategy.calculateMappedValue(domainValueCell,
                    absoluteLength, getMinDomainValue(), getMaxDomainValue(),
                    getNegativeInfinity(), getPositiveInfinity());
        }
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMinDomainValueSet() {
        if (getMinDomainValue() != Double.NaN) {
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMaxDomainValueSet() {
        if (getMaxDomainValue() != Double.NaN) {
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxDomainValue(final double maxDomainValue) {
        super.setMaxDomainValue(maxDomainValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMinDomainValue(final double minDomainValue) {
        super.setMinDomainValue(minDomainValue);
    }
}
