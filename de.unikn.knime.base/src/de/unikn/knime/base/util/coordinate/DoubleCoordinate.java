/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   01.02.2006 (sieb): created
 */
package de.unikn.knime.base.util.coordinate;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DoubleValue;

/**
 * This class represents a numeric coordinate defined by a given
 * {@link de.unikn.knime.core.data.DataColumnSpec}. The class provides
 * functionality for extension of coordinates beyond the domain length.
 * Furthermore the label ticks can be determined dependant on given properties.
 * All these sizes are normalized (0-1) but there are also methods to convert
 * them to absolut values given an absolut maximum length.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
 class DoubleCoordinate extends NumericCoordinate {

    /**
     * The default value for <code>m_coordPrefix</code>.
     */
    private static final double DEFAULT_COORDINATE_PREFIX = 0;

    /**
     * The default value for <code>m_coordPostfix</code>.
     */
    private static final double DEFAULT_COORDINATE_POSTFIX = 0;

    /**
     * The default value for <code>m_coordPrefix</code>.
     */
    private static final double DEFAULT_ABSOLUTE_TICK_DIST = 30;

    /**
     * Default number of rounding digits.
     */
    private static final int DEFAULT_DOMAIN_LABLE_LENGTH = 6;

    /**
     * An optional prefix (extension) of the coordinate beyond the domain
     * length. A value larger or equal 0 is expected and is assumed to be a
     * percent value.
     */
    private double m_coordPrefix;

    /**
     * An optional postfix (extension) of the coordinate beyond the domain
     * length. A value larger or equal 0 is expected and is assumed to be a
     * percent value.
     */
    private double m_coordPostfix;

    /**
     * The abolute tick distance is used when absolute values according to the
     * placement of a tick are requested. The absolute tick distance is
     * dependant on the usage.
     */
    private double m_absoluteTickDistance;

    /**
     * The tick policies available for the creation of ticks.
     */
    private NumericTickPolicy m_tickPolicy;

    /**
     * The maximum lenght of a numeric domain lable.
     */
    private int m_maxDomainLableLenght;

    /**
     * The min domain value to use.
     */
    private double m_minDomainValue;

    /**
     * The max domain value to use.
     */
    private double m_maxDomainValue;

    /**
     * The domain range defined by <code>m_minDomainValue</code> and
     * <code>m_maxDomainValue</code>.
     */
    private double m_domainRange;

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
                NumericTickPolicy.LABLE_WITH_ROUNDED_NUMBERS,
                DEFAULT_DOMAIN_LABLE_LENGTH);
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
     * @param maxDomainLableLenght the number digits after the decimal dot for
     *            rounding accuracy
     */
    DoubleCoordinate(final DataColumnSpec dataColumnSpec,
            final double coordinatePrefix, final double coordinatePostfix,
            final double absoluteTickDistance,
            final NumericTickPolicy tickPolicy, 
            final int maxDomainLableLenght) {
        super(dataColumnSpec);

        // check the column type first it must be compatible to a double
        // value as it is the most general on
        DataType type = dataColumnSpec.getType();

        if (!type.isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException("The type of the given column "
                    + "is not applicable to a numveric coordinate: "
                    + type.toString());
        }

        // check and set the domain ragne
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
            m_minDomainValue = ((DoubleValue)lowerBound).getDoubleValue();
        }

        DataCell upperBound = domain.getUpperBound();
        if (upperBound == null) {

            // if there is no upper bound a coordinate makes no sense
            throw new IllegalArgumentException(
                    "The upper bound of the set column spec is null. "
                            + "Coordinate can not be created.");
        } else {
            m_maxDomainValue = ((DoubleValue)upperBound).getDoubleValue();
        }

        updateDomainRange();

        // check the rounding accuracy
        if (maxDomainLableLenght < 0) {
            throw new IllegalArgumentException("The number of digits after "
                    + "the decimal point must be larger than 0");
        }
        m_maxDomainLableLenght = maxDomainLableLenght;

        // the pre- and postfix must be larger or equal to zero
        if (coordinatePostfix < 0 || coordinatePrefix < 0) {
            throw new IllegalArgumentException("The pre-(" + coordinatePrefix
                    + ") and postfix (" + coordinatePostfix + ")extension "
                    + "must be equal or larger than zero.");
        }
        m_coordPrefix = coordinatePrefix;
        m_coordPostfix = coordinatePostfix;

        // the tick distance must be larger than zero
        if (absoluteTickDistance <= 0) {
            throw new IllegalArgumentException(
                    "The tick distance must be larger than zero: "
                            + absoluteTickDistance);
        }
        m_absoluteTickDistance = absoluteTickDistance;

        m_tickPolicy = tickPolicy;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#getTickPositions(double, boolean)
     */
    @Override
    public CoordinateMapping[] getTickPositions(final double absolutLength,
            final boolean naturalMapping) {

        // the mapping to create and return
        DoubleCoordinateMapping[] mapping = null;

        double absoluteTickDistance = m_absoluteTickDistance;
        // round the absolute tick distance if integer equivalent mapping
        // values are requested
        if (naturalMapping) {

            absoluteTickDistance = Math.round(absoluteTickDistance);
        }

        // compute the start and end value for this reason the domain has
        // to be extended by postfix and prefix
        double startOfDomain = m_minDomainValue;
        double endOfDomain = m_maxDomainValue;
        double domainLength = m_domainRange;

        // update the start- and end point and the range
        startOfDomain = startOfDomain - domainLength * m_coordPrefix;
        endOfDomain = endOfDomain + domainLength * m_coordPostfix;
        domainLength = endOfDomain - startOfDomain;

        // do a special treatment if the domain range is zero
        if (domainLength == 0.0) {
            // just one mapping is created in the middle of the available
            // absolute length

            double domainValue = startOfDomain;
            double mappingValue = Math.round(absolutLength / 2);
            mapping = new DoubleCoordinateMapping[1];
            mapping[0] = new DoubleCoordinateMapping(
                    formatNumber(domainValue), domainValue, mappingValue);
            return mapping;
        }

        // compute the number of ticks for the given absolute lenth
        int numberTicks = (int)(absolutLength / absoluteTickDistance);

        /**
         * implement tick policy <code>LABLE_WITH_ROUNDED_NUMBERS</code>
         * 
         * @see NumericTickPolicy#LABLE_WITH_ROUNDED_NUMBERS
         */
        if (m_tickPolicy == NumericTickPolicy.LABLE_WITH_ROUNDED_NUMBERS) {

            // to determine a usefull range the domain range for
            // the tick distance is determined
            double domainTickStep = domainLength / numberTicks;

            // adjust the domain tick step to a usefull rounding
            int exponent = 0;
            if (domainTickStep > 10) {
                // if the domain tick step is above 10
                while (domainTickStep > 10) {
                    domainTickStep = domainTickStep / 10.0;

                    // count the number of decimal comman shifts
                    exponent++;
                }

                // now there is a value between <= 10 and > 1
                // this value is ceiled and than transformed back to
                // the former decimal correspondence
                domainTickStep = Math.ceil(domainTickStep);
                domainTickStep = domainTickStep * Math.pow(10, exponent);
            } else if (domainTickStep < 1.0) {
                // if the domain tick step is below 1.0
                while (domainTickStep < 1.0) {
                    domainTickStep = domainTickStep * 10.0;

                    // count the number of decimal comman shifts
                    exponent--;
                }

                // now there is a value between <= 10 and > 1
                // this value is ceiled and than transformed back to
                // the former decimal correspondence
                domainTickStep = Math.ceil(domainTickStep);
                domainTickStep = domainTickStep * Math.pow(10, exponent);
            } else {
                // just ceil in this case
                domainTickStep = Math.ceil(domainTickStep);
            }

            // adjust the starting domain value according to the
            // rounding done before for the domain tick distance

            // transform for ceiling
            startOfDomain = startOfDomain * Math.pow(10, -exponent);

            // ceil
            startOfDomain = Math.ceil(startOfDomain);

            // transform back
            startOfDomain = startOfDomain * Math.pow(10, exponent);

            // no create the tick mapping
            ArrayList<DoubleCoordinateMapping> mappingArray = 
                new ArrayList<DoubleCoordinateMapping>();
            for (int i = 0; 
                startOfDomain + i * domainTickStep <= m_maxDomainValue; i++) {

                double domainValue = startOfDomain + i * domainTickStep;
                double mappingValue = (domainValue - m_minDomainValue)
                        / m_domainRange * absolutLength;

                // if natural mappings (natural numbers) are needed
                if (naturalMapping) {
                    mappingValue = Math.round(mappingValue);
                }

                mappingArray.add(new DoubleCoordinateMapping(
                        formatNumber(domainValue), domainValue, mappingValue));
            }

            // convert the array list to an mapping array
            mapping = new DoubleCoordinateMapping[mappingArray.size()];

            mapping = mappingArray.toArray(mapping);
        }

        /**
         * implement tick policy
         * <code>START_WITH_FIRST_END_WITH_LAST_DOMAINE_VALUE</code>
         * 
         * @see NumericTickPolicy#START_WITH_FIRST_END_WITH_LAST_DOMAINE_VALUE
         */
        if (m_tickPolicy 
                == 
              NumericTickPolicy.START_WITH_FIRST_END_WITH_LAST_DOMAINE_VALUE) {

            // add one for the last tick
            numberTicks++;

            // create the mappings for each tick
            mapping = new DoubleCoordinateMapping[numberTicks];
            for (int i = 0; i < numberTicks - 1; i++) {

                double mappingValue = absoluteTickDistance * i;

                // start of domain is the offset which is not needed for the
                // absolut mapping value starting at zero
                double domainValue = startOfDomain + mappingValue
                        / absolutLength * domainLength;
                mapping[i] = new DoubleCoordinateMapping(
                        formatNumber(domainValue), domainValue, mappingValue);
            }

            // the last tick is added manually
            mapping[numberTicks - 1] = new DoubleCoordinateMapping(
                    formatNumber(endOfDomain), endOfDomain, absolutLength);
        }

        return mapping;
    }

    /**
     * Formats a number according to the maximum length allowed for a domain
     * lable. This constraint is set from the higher level according to the
     * application.
     * 
     * @param number the number to format
     * @return the formated number as string
     */
    private String formatNumber(final double number) {

        // get the largest absolute domain value
        double lowerBound = m_minDomainValue;
        double upperBound = m_maxDomainValue;
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

        // the maximum prefix as number allowed given max lable length
        // this is the power of 10 to the allowed length
        double maxNumber = Math.pow(10, m_maxDomainLableLenght);

        int prefixLength = numberAsString.length();
        String formatPattern = null;
        // if the prefix length is longer than the max domain lable lenght
        // the pattern is switched to scientific notation
        if (maxAbsValue >= maxNumber) {

            StringBuffer afterDecimalDigits = new StringBuffer();
            // -4 corresponds to the mandatory first position, the decimal point
            // the E for the exponent and the exponent value (maybe it is larger
            // but this is rather a fuzzy formating)
            for (int i = 0; i < m_maxDomainLableLenght - 4; i++) {

                afterDecimalDigits.append("#");
            }

            // append the format string parts
            formatPattern = "0." + afterDecimalDigits + "E0";

        } else {

            // calculate the remaining digits for the decimal postfix
            // -1 is the decimal point
            int postfixLength = m_maxDomainLableLenght - prefixLength - 1;

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
        DecimalFormat format = (DecimalFormat)NumberFormat
                .getInstance(new Locale("en"));
        format.applyPattern(formatPattern);
        return format.format(number);
    }

    /**
     * Precomputes the domain range from the min and max domain value.
     */
    private void updateDomainRange() {
        m_domainRange = Math.abs(m_maxDomainValue - m_minDomainValue);
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * calculateMappedValue(de.unikn.knime.core.data.DataCell, double, boolean)
     */
    @Override
    public double calculateMappedValue(final DataCell domainValueCell,
            final double absolutLength, final boolean naturalMapping) {

        double domainValue = ((DoubleValue)domainValueCell).getDoubleValue();

        // check if the domain value is usefull
        if (Double.isNaN(domainValue) || Double.isInfinite(domainValue)) {
            return -1.0;
        }

        // if min max is equal return the middle of the absolute length
        if (m_minDomainValue == m_maxDomainValue) {

            return Math.round(absolutLength / 2);
        }

        // calculate the mapping of the domain value
        double mappedValue = (domainValue - m_minDomainValue)
                * (absolutLength / m_domainRange);

        // if a natural mapping (natural numbers) are requested
        // the value is rounded
        if (naturalMapping) {
            mappedValue = Math.round(mappedValue);
        }
        return mappedValue;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * getMaxDomainValue()
     */
    public double getMaxDomainValue() {
        return m_maxDomainValue;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * getMinDomainValue()
     */
    public double getMinDomainValue() {
        return m_minDomainValue;
    }
    
    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * isMinDomainValueSet()
     */
    public boolean isMinDomainValueSet() {
        if (m_minDomainValue != Double.NaN) {
            return true;
        }

        return false;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * isMaxDomainValueSet()
     */
    public boolean isMaxDomainValueSet() {
        if (m_maxDomainValue != Double.NaN) {
            return true;
        }

        return false;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * setMaxDomainValue(double)
     */
    public void setMaxDomainValue(final double maxDomainValue) {
        m_maxDomainValue = maxDomainValue;
        updateDomainRange();
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * setMinDomainValue(double)
     */
    public void setMinDomainValue(final double minDomainValue) {
        m_minDomainValue = minDomainValue;
        updateDomainRange();
    }
}
