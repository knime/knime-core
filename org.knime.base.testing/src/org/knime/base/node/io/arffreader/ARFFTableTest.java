/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   14.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Peter Ohl, University of Konstanz
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

    // same thing but the nominal value list is not separated by a space
    // (Weka reads that!)
    private static final String ARFF_IRISFULL_BAR = "% \n"
        + "% The lovely Iris data set - as we all know it\n"
        + "\n"
        + "@RELATION iris\n"
        + "\n"
        + "@ATTRIBUTE sepallength  REAL\n"
        + "@ATTRIBUTE sepalwidth   REAL\n"
        + "@ATTRIBUTE petallength  REAL\n"
        + "@ATTRIBUTE petalwidth   REAL\n"
        + "@ATTRIBUTE class{Iris-setosa,Iris-versicolor,Iris-virginica}\n"
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

    // same thing but with attributes followed by a comment
    private static final String ARFF_IRISFULL_CMT = "% \n"
        + "% The lovely Iris data set - as we all know it\n"
        + "\n"
        + "@RELATION iris\n"
        + "\n"
        + "@ATTRIBUTE sepallength  REAL % !comment\n"
        + "@ATTRIBUTE sepalwidth   REAL\n"
        + "@ATTRIBUTE petallength  REAL % =Type0\n"
        + "@ATTRIBUTE petalwidth   REAL\n"
        + "@ATTRIBUTE class{Iris-setosa,Iris-versicolor,Iris-virginica}\n"
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
                    createDataTableSpecFromARFFfile(tempFile.toURI().toURL(), null);
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
                    DoubleCell.TYPE);
            assertEquals(tSpec.getColumnSpec(1).getType(),
                    DoubleCell.TYPE);
            assertEquals(tSpec.getColumnSpec(2).getType(),
                    DoubleCell.TYPE);
            assertEquals(tSpec.getColumnSpec(3).getType(),
                    DoubleCell.TYPE);
            assertEquals(tSpec.getColumnSpec(4).getType(), IntCell.TYPE);
            assertEquals(tSpec.getColumnSpec(5).getType(),
                    StringCell.TYPE);
            assertEquals(tSpec.getColumnSpec(6).getType(),
                    StringCell.TYPE);
            assertEquals(tSpec.getColumnSpec(7).getType(),
                    StringCell.TYPE);
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
                createDataTableSpecFromARFFfile(tempFile.toURI().toURL(), null);
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
        assertEquals(tSpec.getColumnSpec(0).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(1).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(2).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(3).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(4).getType(), StringCell.TYPE);
        assertNull(tSpec.getColumnSpec(0).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(1).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(2).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(3).getDomain().getValues());
        assertEquals(tSpec.getColumnSpec(4).getDomain().getValues().size(), 3);
        Set<DataCell> vals = tSpec.getColumnSpec(4).getDomain().getValues();
        assertTrue(vals.contains(new StringCell("Iris-setosa")));
        assertTrue(vals.contains(new StringCell("Iris-versicolor")));
        assertTrue(vals.contains(new StringCell("Iris-virginica")));
        } catch (CanceledExecutionException cee) {
            // no chance to end up here.
        }
    }

    /**
     * test the creation of a table spec from the IRIS data in an ARFF file.
     * (With the nominal value list not separated with a space)
     * @throws IOException if it wants to.
     * @throws InvalidSettingsException if it feels like.
     */
    public void testCreateDataTableSpecFromARFFfileIRIS_BAR()
            throws IOException, InvalidSettingsException {

        File tempFile = File.createTempFile("ARFFReaderUnitTest", "mini");
        tempFile.deleteOnExit();
        Writer out = new BufferedWriter(new FileWriter(tempFile));
        out.write(ARFF_IRISFULL_BAR);
        out.close();
        try {
        DataTableSpec tSpec = ARFFTable.
                createDataTableSpecFromARFFfile(tempFile.toURI().toURL(), null);
        //  + "% The lovely Iris data set - as we all know it\n"
        //  + "\n"
        //  + "@RELATION iris\n"
        //  + "\n"
        //  + "@ATTRIBUTE sepallength REAL\n"
        //  + "@ATTRIBUTE sepalwidth REAL\n"
        //  + "@ATTRIBUTE petallength REAL\n"
        //  + "@ATTRIBUTE petalwidth REAL\n"
        //  + "@ATTRIBUTE class{Iris-setosa,Iris-versicolor,Iris-virginica}\n"
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
        assertEquals(tSpec.getColumnSpec(0).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(1).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(2).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(3).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(4).getType(), StringCell.TYPE);
        assertNull(tSpec.getColumnSpec(0).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(1).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(2).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(3).getDomain().getValues());
        assertEquals(tSpec.getColumnSpec(4).getDomain().getValues().size(), 3);
        Set<DataCell> vals = tSpec.getColumnSpec(4).getDomain().getValues();
        assertTrue(vals.contains(new StringCell("Iris-setosa")));
        assertTrue(vals.contains(new StringCell("Iris-versicolor")));
        assertTrue(vals.contains(new StringCell("Iris-virginica")));
        } catch (CanceledExecutionException cee) {
            // no chance to end up here.
        }
    }

    /**
     * test the creation of a table spec from the IRIS data in an ARFF file.
     *
     * @throws IOException if it wants to.
     * @throws InvalidSettingsException if it feels like.
     */
    public void testCreateDataTableSpecFromARFFfileIRISCMT() throws IOException,
            InvalidSettingsException {

        File tempFile = File.createTempFile("ARFFReaderUnitTest", "mini");
        tempFile.deleteOnExit();
        Writer out = new BufferedWriter(new FileWriter(tempFile));
        out.write(ARFF_IRISFULL_CMT);
        out.close();
        try {
        DataTableSpec tSpec = ARFFTable.
                createDataTableSpecFromARFFfile(tempFile.toURI().toURL(), null);
        //  + "% The lovely Iris data set - as we all know it\n"
        //  + "\n"
        //  + "@RELATION iris\n"
        //  + "\n"
        //  + "@ATTRIBUTE sepallength  REAL % !comment\n"
        //  + "@ATTRIBUTE sepalwidth   REAL\n"
        //  + "@ATTRIBUTE petallength  REAL % =Type0\n"
        //  + "@ATTRIBUTE petalwidth   REAL\n"
        //  + "@ATTRIBUTE class{Iris-setosa,Iris-versicolor,Iris-virginica}\n"
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
        assertEquals(tSpec.getColumnSpec(0).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(1).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(2).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(3).getType(), DoubleCell.TYPE);
        assertEquals(tSpec.getColumnSpec(4).getType(), StringCell.TYPE);
        assertNull(tSpec.getColumnSpec(0).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(1).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(2).getDomain().getValues());
        assertNull(tSpec.getColumnSpec(3).getDomain().getValues());
        assertEquals(tSpec.getColumnSpec(4).getDomain().getValues().size(), 3);
        Set<DataCell> vals = tSpec.getColumnSpec(4).getDomain().getValues();
        assertTrue(vals.contains(new StringCell("Iris-setosa")));
        assertTrue(vals.contains(new StringCell("Iris-versicolor")));
        assertTrue(vals.contains(new StringCell("Iris-virginica")));
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
        ARFFTable table = new ARFFTable(tempFile.toURI().toURL(), ARFFTable.
                createDataTableSpecFromARFFfile(tempFile.toURI().toURL(), null),
                                                    "Row");

        assertEquals(table.getDataTableSpec().getNumColumns(), 2);
        assertEquals(table.getDataTableSpec().getColumnSpec(0).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(1).getType(),
                StringCell.TYPE);
        assertNull(table.getDataTableSpec().getColumnSpec(0).getDomain().
                getValues());
        assertNull(table.getDataTableSpec().getColumnSpec(1).getDomain().
                getValues());
        DataRow row;
        RowIterator rIter = table.iterator();

        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row0");
        assertEquals(row.getCell(0).toString(), "foo");
        assertEquals(row.getCell(1).toString(), "poo");

        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row1");
        assertEquals(row.getCell(0).toString(), "foo");
        assertTrue(row.getCell(1).isMissing());

        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row2");
        assertTrue(row.getCell(0).isMissing());
        assertEquals(row.getCell(1).toString(), "foo");

        assertFalse(rIter.hasNext());
        } catch (CanceledExecutionException cee) {
            // no exec monitor, no cancel
        }

    }

    /**
     * Customer file. Weka is able to read it. We failed on the missing space
     * in the last "@attribute" line.
     *
     * @throws IOException some time.
     * @throws InvalidSettingsException sometimes.
     */
    public void testARFFwithMissingSpace() throws IOException,
            InvalidSettingsException {
        final String missingSpace =
            "@relation kredit_bereinigt\n"
            + "\n"
            + "@attribute REPAYMENT_PROBLEM {0,1}\n" /* Col 0*/
            + "@attribute RSV {0,1}\n"/* Col 1*/
            + "@attribute GENDER {0,1}\n"/* Col 2*/
            + "@attribute AGE real\n"/* Col 3*/
            + "@attribute PHONE {0,1}\n"/* Col 4*/
            + "@attribute NUMBER_CHILDREN real\n"/* Col 5*/
            + "@attribute ADDRESS_CHANGED {0,1}\n"/* Col 6*/
            + "@attribute GUARANTOR {0,1}\n"/* Col 7*/
            + "@attribute JOB_DURATION real\n"/* Col 8*/
            + "@attribute INCOME real\n"/* Col 9*/
            + "@attribute DISP_INCOME real\n"/* Col 10*/
            + "@attribute RENTAL_FEE real\n"/* Col 11*/
            + "@attribute CAR {0,1}\n"/* Col 12*/
            + "@attribute OTHER_CONTRACTS {0,1}\n"/* Col 13*/
            + "@attribute OTHER_LOANS {0,1}\n"/* Col 14*/
            + "@attribute EXPENSE real\n"/* Col 15*/
            + "@attribute SAVINGS {0,1}\n"/* Col 16*/
            /* following line is missing a space */
            + "@attribute STOCK{0,1}\n"/* Col 17*/
            + "\n"
            + "@data\n"
            + "1,1,1,27,0,1,1,1,2,2900,1335,330,1,0,0,1565,1,0\n"
            + "1,1,0,28,1,0,1,0,20,2000,1100,150,0,1,0,900,1,0\n";

        File tempFile = File.createTempFile("ARFFReaderUnitTest", "missSpace");
        tempFile.deleteOnExit();
        Writer out = new BufferedWriter(new FileWriter(tempFile));
        out.write(missingSpace);
        out.close();
        try {
        ARFFTable table = new ARFFTable(tempFile.toURI().toURL(), ARFFTable.
                createDataTableSpecFromARFFfile(tempFile.toURI().toURL(), null),
                                                    "Row");

        assertEquals(table.getDataTableSpec().getNumColumns(), 18);

        assertEquals(table.getDataTableSpec().getColumnSpec(0).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(1).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(2).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(3).getType(),
                DoubleCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(4).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(5).getType(),
                DoubleCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(6).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(7).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(8).getType(),
                DoubleCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(9).getType(),
                DoubleCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(10).getType(),
                DoubleCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(11).getType(),
                DoubleCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(12).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(13).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(14).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(15).getType(),
                DoubleCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(16).getType(),
                StringCell.TYPE);
        assertEquals(table.getDataTableSpec().getColumnSpec(17).getType(),
                StringCell.TYPE);


/*
        + "1,1,1,27,0,1,1,1,2,2900,1335,330,1,0,0,1565,1,0\n"
*/
        DataRow row;
        RowIterator rIter = table.iterator();

        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row0");
        assertEquals(row.getCell(0).toString(), "1");
        assertEquals(row.getCell(1).toString(), "1");
        assertEquals(row.getCell(2).toString(), "1");
        assertEquals(row.getCell(3).toString(), "27.0");
        assertEquals(row.getCell(4).toString(), "0");
        assertEquals(row.getCell(5).toString(), "1.0");
        assertEquals(row.getCell(6).toString(), "1");
        assertEquals(row.getCell(7).toString(), "1");
        assertEquals(row.getCell(8).toString(), "2.0");
        assertEquals(row.getCell(9).toString(), "2900.0");
        assertEquals(row.getCell(10).toString(), "1335.0");
        assertEquals(row.getCell(11).toString(), "330.0");
        assertEquals(row.getCell(12).toString(), "1");
        assertEquals(row.getCell(13).toString(), "0");
        assertEquals(row.getCell(14).toString(), "0");
        assertEquals(row.getCell(15).toString(), "1565.0");
        assertEquals(row.getCell(16).toString(), "1");
        assertEquals(row.getCell(17).toString(), "0");

/*
 *         + "1,1,0,28,1,0,1,0,20,2000,1100,150,0,1,0,900,1,0\n";
 */
        assertTrue(rIter.hasNext());
        row = rIter.next();
        assertEquals(row.getKey().toString(), "Row1");
        assertEquals(row.getCell(0).toString(), "1");
        assertEquals(row.getCell(1).toString(), "1");
        assertEquals(row.getCell(2).toString(), "0");
        assertEquals(row.getCell(3).toString(), "28.0");
        assertEquals(row.getCell(4).toString(), "1");
        assertEquals(row.getCell(5).toString(), "0.0");
        assertEquals(row.getCell(6).toString(), "1");
        assertEquals(row.getCell(7).toString(), "0");
        assertEquals(row.getCell(8).toString(), "20.0");
        assertEquals(row.getCell(9).toString(), "2000.0");
        assertEquals(row.getCell(10).toString(), "1100.0");
        assertEquals(row.getCell(11).toString(), "150.0");
        assertEquals(row.getCell(12).toString(), "0");
        assertEquals(row.getCell(13).toString(), "1");
        assertEquals(row.getCell(14).toString(), "0");
        assertEquals(row.getCell(15).toString(), "900.0");
        assertEquals(row.getCell(16).toString(), "1");
        assertEquals(row.getCell(17).toString(), "0");

        assertFalse(rIter.hasNext());

        } catch (CanceledExecutionException cee) {
            // no exec monitor, no cancel
        }



    }
}
