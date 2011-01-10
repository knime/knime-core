/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.data.util;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;

import junit.framework.TestCase;

/**
 * Unit test for <code>ObjectToDataCellConverter</code>.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ObjectToDataCellConverterTest extends TestCase {
    
    /** Instance to test on. */
    @SuppressWarnings("deprecation")
    private final ObjectToDataCellConverter m_converter = 
        new ObjectToDataCellConverter();
    
    /** Main class to start unit test from commandline.
     * @param args ignored
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(ObjectToDataCellConverterTest.class);
    }

    /**
     * Class under test for DataCell createDataCell(Object).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellObject() {
        int i = 5;
        DataCell c = m_converter.createDataCell(new Integer(i));
        assert ((IntValue)c).getIntValue() == i;
        c = m_converter.createDataCell(new Byte((byte)i));
        assert ((IntValue)c).getIntValue() == i;
        float d = 8.0f;
        c = m_converter.createDataCell(new Double(d));
        assert ((DoubleValue)c).getDoubleValue() == d;
        c = m_converter.createDataCell(new Float(d));
        assert ((DoubleValue)c).getDoubleValue() == d;
        String s = "Test String";
        c = m_converter.createDataCell(s);
        assert ((StringValue)c).getStringValue().equals(s);
        c = m_converter.createDataCell(null);
        assert c.isMissing();
        try {
            c = m_converter.createDataCell(new Object());
            assert false;
        } catch (IllegalArgumentException iae) {
            assert true; // avoid complaints by checkstyle!
        }
    } // testCreateDataCellObject()

    /**
     * Class under test for DataCell createDataCell(double).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCelldouble() {
        double d = 8.0;
        DataCell c = m_converter.createDataCell(d);
        assert ((DoubleValue)c).getDoubleValue() == d;
    } // testCreateDataCelldouble()

    /**
     * Class under test for DataCell createDataCell(float).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellfloat() {
        float f = 8.0f;
        DataCell c = m_converter.createDataCell(f);
        assert ((DoubleValue)c).getDoubleValue() == f;
    } // testCreateDataCellfloat()

    /**
     * Class under test for DataCell createDataCell(int).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellint() {
        int i = 12;
        DataCell c = m_converter.createDataCell(i);
        assert ((IntValue)c).getIntValue() == i;
    } // testCreateDataCellint()

    /**
     * Class under test for DataCell createDataCell(byte).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellbyte() {
        byte b = 13;
        DataCell c = m_converter.createDataCell(b);
        assert ((IntValue)c).getIntValue() == b;
    } // testCreateDataCellbyte()

    /**
     * Class under test for DataCell createDataCell(boolean).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellboolean() {
        boolean b = true;
        DataCell c = m_converter.createDataCell(b);
        assert ((IntValue)c).getIntValue() == 1;
        b = false;
        c = m_converter.createDataCell(b);
        assert ((IntValue)c).getIntValue() == 0;
    } // testCreateDataCellboolean()

}
