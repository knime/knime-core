/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   14.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import junit.framework.TestCase;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DoubleType;
import de.unikn.knime.core.data.IntType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.StringType;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class ARFFTableTest extends TestCase {

    private static final String ARFF_IRISFULL = "% \n"
            + "% The lovely Iris data set - as we all know it\n"
            + "\n"
            + "@RELATION iris\n"
            + "\n"
            + "@ATTRIBUTE sepallength  REAL\n"
            + "@ATTRIBUTE sepalwidth   REAL\n"
            + "@ATTRIBUTE petallength  REAL\n"
            + "@ATTRIBUTE petalwidth   REAL\n"
            + "@ATTRIBUTE class  {Iris-setosa,Iris-versicolor,Iris-virginica}\n"
            + "\n" + "@DATA\n" + "5.1,3.5,1.4,0.2,Iris-setosa\n"
            + "4.9,3.0,1.4,0.2,Iris-setosa\n" + "4.7,3.2,1.3,0.2,Iris-setosa\n"
            + "4.6,3.1,1.5,0.2,Iris-setosa\n" + "5.0,3.6,1.4,0.2,Iris-setosa\n"
            + "7.0,3.2,4.7,1.4,Iris-versicolor\n"
            + "6.4,3.2,4.5,1.5,Iris-versicolor\n"
            + "6.9,3.1,4.9,1.5,Iris-versicolor\n"
            + "5.5,2.3,4.0,1.3,Iris-versicolor\n"
            + "6.5,2.8,4.6,1.5,Iris-versicolor\n"
            + "6.3,3.3,6.0,2.5,Iris-virginica\n"
            + "5.8,2.7,5.1,1.9,Iris-virginica\n"
            + "7.1,3.0,5.9,2.1,Iris-virginica\n"
            + "6.3,2.9,5.6,1.8,Iris-virginica\n"
            + "6.5,3.0,5.8,2.2,Iris-virginica\n";

    /**
     * ARFF stuff to test.
     */
    private static final String ARFF_FOO = "\n\n" + "@Relation Foo dataset\n\n"
            + "% this is a test\n\n" + "@attribute col1 numeric\n"
            + "@ATTRIBUTE COL1 NUMERIC\n" + "@attribute col2 real\n"
            + "@ATTRIBUTE COL2 REAL\n" + "@attribute col3 integer\n"
            + "@attribute col4 string\n"
            + "@attribute col5 date \"yyyy-MM-dd HH:mm:ss\"\n"
            + "@attribute col6 {foo, poo, \"loo loo\", 'moo moo', schluss}\n"
            + "@data\n\n\n"
            + "1, 2, 3.0, 4.5, 6, string67, '1966-08-03 10:14:12', foo";

    /**
     * eat this.
     */
    private static final String ARFF_FIES = "% Comment\n"
            + "% comment line 2\n" + "@attribute col1 string\n"
            + "@attribute col2 string\n" + "\n\n" + "@data\n\n" + "foo, poo\n"
            + "foo, ?\n" + "?, foo\n" + "%\n" + "%\n" + "\n";

    /**
     * tests the creatoion of a table spec from a nice ARFF file.
     * 
     * @throws IOException if.
     * @throws InvalidSettingsException when.
     */
    public void testCreateDataTableSpecFromARFFfileFOO() throws IOException,
            InvalidSettingsException {

        File tempFile = File.createTempFile("ARFFReaderUnitTest", "mini");
        tempFile.deleteOnExit();
        Writer out = new BufferedWriter(new FileWriter(tempFile));
        out.write(ARFF_FOO);
        out.close();
        try {
            DataTableSpec tSpec = ARFFTable.
                    createDataTableSpecFromARFFfile(tempFile.toURL(), null);
            assertEquals(tSpec.getNumColumns(), 8);
            assertEquals(tSpec.getColumnSpec(0).getName().toString(), "col1");
            assertEquals(tSpec.getColumnSpec(1).getName().toString(), "COL1");
            assertEquals(tSpec.getColumnSpec(2).getName().toString(), "col2");
            assertEquals(tSpec.getColumnSpec(3).getName().toString(), "COL2");
            assertEquals(tSpec.getColumnSpec(4).getName().toString(), "col3");
            assertEquals(tSpec.getColumnSpec(5).getName().toString(), "col4");
            assertEquals(tSpec.getColumnSpec(6).getName().toString(), "col5");
            assertEquals(tSpec.getColumnSpec(7).getName().toString(), "col6");
            assertEquals(tSpec.getColumnSpec(0).getType(),
                    DoubleType.DOUBLE_TYPE);
            assertEquals(tSpec.getColumnSpec(1).getType(),
                    DoubleType.DOUBLE_TYPE);
            assertEquals(tSpec.getColumnSpec(2).getType(),
                    DoubleType.DOUBLE_TYPE);
            assertEquals(tSpec.getColumnSpec(3).getType(),
                    DoubleType.DOUBLE_TYPE);
            assertEquals(tSpec.getColumnSpec(4).getType(), IntType.INT_TYPE);
            assertEquals(tSpec.getColumnSpec(5).getType(),
                    StringType.STRING_TYPE);
            assertEquals(tSpec.getColumnSpec(6).getType(),
                    StringType.STRING_TYPE);
            assertEquals(tSpec.getColumnSpec(7).getType(),
                    StringType.STRING_TYPE);
            assertNull(tSpec.getColumnSpec(0).getDomain().getValues());
            assertNull(tSpec.getColumnSpec(1).getDomain().getValues());
            assertNull(tSpec.getColumnSpec(2).getDomain().getValues());
            assertNull(tSpec.getColumnSpec(3).getDomain().getValues());
            assertNull(tSpec.getColumnSpec(4).getDomain().getValues());
            assertNull(tSpec.getColumnSpec(5).getDomain().getValues());
            assertNull(tSpec.getColumnSpec(6).getDomain().getValues());
            assertEquals(tSpec.getColumnSpec(7).getDomain().getValues().size(),
                    5);
        } catch (CanceledExecutionException cee) {
            // if you cancel during the test I will not fail!
        }
    }

    /**
     * test the creation of a table spec from the IRIS data in an ARFF file.
     * 
     * @throws IOException if it wants to.
     * @throws InvalidSettingsException if it feels like.
     */
    public void testCreateDataTableSpecFromARFFfileIRIS() throws IOException,
            InvalidSettingsException {

        File tempFile = File.createTempFile("ARFFReaderUnitTest", "mini");
        tempFile.deleteOnExit();
        Writer out = new BufferedWriter(new FileWriter(tempFile));
        out.write(ARFF_IRISFULL);
        out.close();
        try {
        DataTableSpec tSpec = ARFFTable.
                createDataTableSpecFromARFFfile(tempFile.toURL(), null);
        //  + "% The lovely Iris data set - as we all know it\n"
        //  + "\n"
        //  + "@RELATION iris\n"
        //  + "\n"
        //  + "@ATTRIBUTE sepallength REAL\n"
        //  + "@ATTRIBUTE sepalwidth REAL\n"
        //  + "@ATTRIBUTE petallength REAL\n"
        //  + "@ATTRIBUTE petalwidth REAL\n"
        //  + "@ATTRIBUTE class {Iris-setosa,Iris-versicolor,Iris-virginica}\n"
        //  + "\n"
        assertEquals(tSpec.getNumColumns(), 5);
        assertEquals(tSpec.getColumnSpec(0).getName().toString(), 
                "sepallength");
        assertEquals(tSpec.getColumnSpec(1).getName().toString(), 
                "sepalwidth");
        assertEquals(tSpec.getColumnSpec(2).getName().toString(), 
                "petallength");
        assertEquals(tSpec.getColumnSpec(3).getName().toString(), 
                "petalwidth");
        assertEquals(tSpec.getColumnSpec(4).getName().toString(), 
                "class");
        assertEquals(tSpec.getColumnSpec(0).getType(), DoubleType.DOUBLE_TYPE);
        assertEquals(tSpec.getColumnSpec(1).getType(), DoubleType.DOUBLE_TYPE);
        assertEquals(tSpec.getColumnSpec(2).getType(), DoubleType.DOUBLE_TYPE);
        assertEquals(tSpec.getColumnSpec(3).getType(), DoubleType.DOUBLE_TYPE);
        assertEquals(tSpec.getColumnSpec(4).getType(), StringType.STRING_TYPE);
        assertNull(tSpec.getColumnSpec(0).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(1).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(2).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(3).getDomain().getValues());
        assertEquals(tSpec.getColumnSpec(4).getDomain().getValues().size(), 3);
        } catch (CanceledExecutionException cee) {
            // no chance to end up here.
        }
    }

    /**
     * tests stuff like '?' the ARFF missing value, comment lines and empty
     * lines in the file.
     * 
     * @throws IOException some time.
     * @throws InvalidSettingsException sometimes.
     */
    public void testARFFTableMissVals() throws IOException,
            InvalidSettingsException {
        //        "% Comment\n"
        //        + "% comment line 2\n"
        //        + "@attribute col1 string\n"
        //        + "@attribute col2 string\n"
        //        + "\n\n"
        //        + "@data\n\n"
        //        + "foo, poo\n"
        //        + "foo, ?\n"
        //        + "?, foo\n"
        //        + "%\n"
        //        + "%\n"
        //        + "\n";

        File tempFile = File.createTempFile("ARFFReaderUnitTest", "table");
        tempFile.deleteOnExit();
        Writer out = new BufferedWriter(new FileWriter(tempFile));
        out.write(ARFF_FIES);
        out.close();
        try {
        ARFFTable table = new ARFFTable(tempFile.toURL(), ARFFTable.
                createDataTableSpecFromARFFfile(tempFile.toURL(), null), 
                                                    "Row");

        assertEquals(table.getDataTableSpec().getNumColumns(), 2);
        assertEquals(table.getDataTableSpec().getColumnSpec(0).getType(),
                StringType.STRING_TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(1).getType(),
                StringType.STRING_TYPE);
        assertNull(table.getDataTableSpec().getColumnSpec(0).getDomain().
                getValues());
        assertNull(table.getDataTableSpec().getColumnSpec(1).getDomain().
                getValues());
        DataRow row;
        RowIterator rIter = table.iterator();

        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row1");
        assertEquals(row.getCell(0).toString(), "foo");
        assertEquals(row.getCell(1).toString(), "poo");

        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row2");
        assertEquals(row.getCell(0).toString(), "foo");
        assertTrue(row.getCell(1).isMissing());

        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row3");
        assertTrue(row.getCell(0).isMissing());
        assertEquals(row.getCell(1).toString(), "foo");

        assertFalse(rIter.hasNext());
        } catch (CanceledExecutionException cee) {
            // no exec monitor, no cancel
        }

    }
}
