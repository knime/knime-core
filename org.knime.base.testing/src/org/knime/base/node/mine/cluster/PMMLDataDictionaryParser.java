/* This source code, its documentation and all appendant files
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
 */
package org.knime.base.node.mine.cluster;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.pmml.DataDictionaryContentHandler;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLDataDictionaryParser extends TestCase {


    private SAXParser m_parser;
    private DataDictionaryContentHandler m_handler;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SAXParserFactory fac = SAXParserFactory.newInstance();
        m_parser =  fac.newSAXParser();
        m_handler = new DataDictionaryContentHandler();
    }


    public void testOwnClusterModel() throws Exception {
        InputStream is = getClass().getResourceAsStream(
                "files/iris_kMeans.pmml");
        m_parser.parse(is, m_handler);
        DataTableSpec spec = m_handler.getDataTableSpec();
        assertEquals(5, spec.getNumColumns());
        assertEquals(0, spec.findColumnIndex("sepal length"));
        assertEquals(1, spec.findColumnIndex("sepal width"));
        assertEquals(2, spec.findColumnIndex("petal length"));
        assertEquals(3, spec.findColumnIndex("petal width"));
        assertEquals(4, spec.findColumnIndex("class"));
        DataColumnSpec sepalLength = spec.getColumnSpec(0);
        assertEquals("sepal length", sepalLength.getName());
        assertEquals(4.3, ((DoubleValue)sepalLength.getDomain()
                .getLowerBound()).getDoubleValue());
        assertEquals(7.9, ((DoubleValue)sepalLength.getDomain()
                .getUpperBound()).getDoubleValue());

        DataColumnSpec sepalWidth = spec.getColumnSpec(1);
        assertEquals("sepal width", sepalWidth.getName());
        assertEquals(2.0, ((DoubleValue)sepalWidth.getDomain()
                .getLowerBound()).getDoubleValue());
        assertEquals(4.4, ((DoubleValue)sepalWidth.getDomain()
                .getUpperBound()).getDoubleValue());

        DataColumnSpec petalLength = spec.getColumnSpec(2);
        assertEquals("petal length", petalLength.getName());
        assertEquals(1.0, ((DoubleValue)petalLength.getDomain()
                .getLowerBound()).getDoubleValue());
        assertEquals(6.9, ((DoubleValue)petalLength.getDomain()
                .getUpperBound()).getDoubleValue());


        DataColumnSpec petalWidth = spec.getColumnSpec(3);
        assertEquals("petal width", petalWidth.getName());
        assertEquals(0.1, ((DoubleValue)petalWidth.getDomain()
                .getLowerBound()).getDoubleValue());
        assertEquals(2.5, ((DoubleValue)petalWidth.getDomain()
                .getUpperBound()).getDoubleValue());

        DataColumnSpec clazz = spec.getColumnSpec(4);
        assertEquals("class", clazz.getName());
        assertEquals(3, clazz.getDomain()
                .getValues().size());
        assertTrue(clazz.getDomain().getValues().contains(
                new StringCell("Iris-setosa")));
        assertTrue(clazz.getDomain().getValues().contains(
                new StringCell("Iris-versicolor")));
        assertTrue(clazz.getDomain().getValues().contains(
                new StringCell("Iris-virginica")));
        }

    public void testAssociationruleModelFile() throws Exception {
        InputStream is = getClass().getResourceAsStream(
                "files/association_rules_model.xml");
        m_parser.parse(is, m_handler);
        DataTableSpec spec = m_handler.getDataTableSpec();
        assertEquals(0, spec.findColumnIndex("cardid"));
        assertEquals(1, spec.findColumnIndex("Product"));
        assertEquals(StringCell.TYPE, spec.getColumnSpec("cardid").getType());
        assertEquals(StringCell.TYPE, spec.getColumnSpec("Product").getType());
    }

    public void testCenterBasedClusteringModelFile() throws Exception {
        InputStream is = getClass().getResourceAsStream(
                "files/CenterbasedClustering.xml");
        m_parser.parse(is, m_handler);
        DataTableSpec spec = m_handler.getDataTableSpec();
        // column names
        assertEquals(0, spec.findColumnIndex("PETALLEN"));
        assertEquals(1, spec.findColumnIndex("PETALWID"));
        assertEquals(2, spec.findColumnIndex("SEPALLEN"));
        assertEquals(3, spec.findColumnIndex("SEPALWID"));
        assertEquals(4, spec.findColumnIndex("SPECIES"));
        // column types
        assertEquals(DoubleCell.TYPE, spec.getColumnSpec(0).getType());
        assertEquals(DoubleCell.TYPE, spec.getColumnSpec(1).getType());
        assertEquals(DoubleCell.TYPE, spec.getColumnSpec(2).getType());
        assertEquals(DoubleCell.TYPE, spec.getColumnSpec(3).getType());
        assertEquals(StringCell.TYPE, spec.getColumnSpec(4).getType());
        // lower and upper bound (col 0)
        assertTrue(spec.getColumnSpec(0).getDomain().hasBounds());
        assertEquals(((DoubleValue)spec.getColumnSpec(0).getDomain()
                .getLowerBound()).getDoubleValue(), 10.0);
        assertEquals(((DoubleValue)spec.getColumnSpec(0).getDomain()
                .getUpperBound()).getDoubleValue(), 69.0);
        // lower and upper bound (col 1)
        assertTrue(spec.getColumnSpec(1).getDomain().hasBounds());
        assertEquals(((DoubleValue)spec.getColumnSpec(1).getDomain()
                .getLowerBound()).getDoubleValue(), 1.0);
        assertEquals(((DoubleValue)spec.getColumnSpec(1).getDomain()
                .getUpperBound()).getDoubleValue(), 25.0);
        // lower and upper bound (col 2)
        assertTrue(spec.getColumnSpec(2).getDomain().hasBounds());
        assertEquals(((DoubleValue)spec.getColumnSpec(2).getDomain()
                .getLowerBound()).getDoubleValue(), 43.0);
        assertEquals(((DoubleValue)spec.getColumnSpec(2).getDomain()
                .getUpperBound()).getDoubleValue(), 79.0);
        // lower and upper bound (col 3)
        assertTrue(spec.getColumnSpec(3).getDomain().hasBounds());
        assertEquals(((DoubleValue)spec.getColumnSpec(3).getDomain()
                .getLowerBound()).getDoubleValue(), 20.0);
        assertEquals(((DoubleValue)spec.getColumnSpec(3).getDomain()
                .getUpperBound()).getDoubleValue(), 44.0);
        // values for (col 4)
        assertTrue(spec.getColumnSpec(4).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(4).getDomain().getValues().contains(
                new StringCell("virginica")));
        assertTrue(spec.getColumnSpec(4).getDomain().getValues().contains(
                new StringCell("versicolor")));
        assertTrue(spec.getColumnSpec(4).getDomain().getValues().contains(
                new StringCell("setosa")));
    }

    public void testClusModelFile() throws Exception {
        InputStream is= getClass().getResourceAsStream(
                "files/ClusModel.xml");
        m_parser.parse(is, m_handler);
        // names
        DataTableSpec spec = m_handler.getDataTableSpec();
        assertEquals(spec.findColumnIndex("INT_0_1"), 0);
        assertEquals(spec.findColumnIndex("CONST1"), 1);
        assertEquals(spec.findColumnIndex("TARGET_0_1"), 2);
        assertEquals(spec.findColumnIndex("SGL_VAL"), 3);
        assertEquals(spec.findColumnIndex("BI_VAL"), 4);
        assertEquals(spec.findColumnIndex("TRI_VAL"), 5);
        assertEquals(spec.findColumnIndex("TRAIL_BLANKS"), 6);
        assertEquals(spec.findColumnIndex("LEAD_BLANKS"), 7);
        assertEquals(spec.findColumnIndex("EMPTY_STR"), 8);
        assertEquals(spec.findColumnIndex("CAP_STR"), 9);
        assertEquals(spec.findColumnIndex("TARGET_BI"), 10);
        assertEquals(spec.findColumnIndex("TARGET_TRI"), 11);
        assertEquals(spec.findColumnIndex("INT100"), 12);
        assertEquals(spec.findColumnIndex("NORM_0_1"), 13);
        assertEquals(spec.findColumnIndex("NORM100"), 14);
        assertEquals(spec.findColumnIndex("TARGET_INT100"), 15);
        assertEquals(spec.findColumnIndex("TARGET_CONT"), 16);
        // types
        assertEquals(spec.getColumnSpec(0).getType(),
                DoubleCell.TYPE);
        assertEquals(spec.getColumnSpec(1).getType(),
                DoubleCell.TYPE);
        assertEquals(spec.getColumnSpec(2).getType(),
                DoubleCell.TYPE);
        assertEquals(spec.getColumnSpec(3).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(4).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(5).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(6).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(7).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(8).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(9).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(10).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(11).getType(),
                StringCell.TYPE);
        assertEquals(spec.getColumnSpec(12).getType(),
                DoubleCell.TYPE);
        assertEquals(spec.getColumnSpec(13).getType(),
                DoubleCell.TYPE);
        assertEquals(spec.getColumnSpec(14).getType(),
                DoubleCell.TYPE);
        assertEquals(spec.getColumnSpec(15).getType(),
                DoubleCell.TYPE);
        assertEquals(spec.getColumnSpec(16).getType(),
                DoubleCell.TYPE);
        // values and bounds
        assertTrue(spec.getColumnSpec(0).getDomain().hasBounds());
        assertEquals(((DoubleValue)spec.getColumnSpec(0).getDomain()
                .getLowerBound()).getDoubleValue(), 0.0);
        assertEquals(((DoubleValue)spec.getColumnSpec(0).getDomain()
                .getUpperBound()).getDoubleValue(), 1.0);

        assertTrue(spec.getColumnSpec(1).getDomain().hasBounds());
        assertEquals(((DoubleValue)spec.getColumnSpec(1).getDomain()
                .getLowerBound()).getDoubleValue(), 1.0);
        assertEquals(((DoubleValue)spec.getColumnSpec(1).getDomain()
                .getUpperBound()).getDoubleValue(), 1.0);

        assertTrue(spec.getColumnSpec(2).getDomain().hasBounds());
        assertEquals(0.0, ((DoubleValue)spec.getColumnSpec(2).getDomain()
                .getLowerBound()).getDoubleValue());
        assertEquals(((DoubleValue)spec.getColumnSpec(2).getDomain()
                .getUpperBound()).getDoubleValue(), 1.0);

        assertTrue(spec.getColumnSpec(3).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(3).getDomain().getValues().contains(
                new StringCell("A")));
        assertEquals(spec.getColumnSpec(3).getDomain().getValues().size(), 1);

        assertTrue(spec.getColumnSpec(4).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(4).getDomain().getValues().contains(
                new StringCell("A")));
        assertTrue(spec.getColumnSpec(4).getDomain().getValues().contains(
                new StringCell("B")));
        assertEquals(spec.getColumnSpec(4).getDomain().getValues().size(), 2);

        assertTrue(spec.getColumnSpec(5).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(5).getDomain().getValues().contains(
                new StringCell("A")));
        assertTrue(spec.getColumnSpec(5).getDomain().getValues().contains(
                new StringCell("B")));
        assertTrue(spec.getColumnSpec(5).getDomain().getValues().contains(
                new StringCell("C")));
        assertEquals(spec.getColumnSpec(5).getDomain().getValues().size(), 3);

        assertTrue(spec.getColumnSpec(6).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(6).getDomain().getValues().contains(
                new StringCell("A")));
        assertEquals(spec.getColumnSpec(6).getDomain().getValues().size(), 1);

        assertTrue(spec.getColumnSpec(7).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(7).getDomain().getValues().contains(
                new StringCell("A")));
        assertTrue(spec.getColumnSpec(7).getDomain().getValues().contains(
                new StringCell(" A")));
        assertTrue(spec.getColumnSpec(7).getDomain().getValues().contains(
                new StringCell("  A")));
        assertEquals(spec.getColumnSpec(7).getDomain().getValues().size(), 3);

        assertTrue(spec.getColumnSpec(8).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(8).getDomain().getValues().contains(
                new StringCell("")));
        assertTrue(spec.getColumnSpec(8).getDomain().getValues().contains(
                new StringCell("A")));
        assertTrue(spec.getColumnSpec(8).getDomain().getValues().contains(
                new StringCell("B")));
        assertTrue(spec.getColumnSpec(8).getDomain().getValues().contains(
                new StringCell("C")));
        assertEquals(spec.getColumnSpec(8).getDomain().getValues().size(), 4);

        assertTrue(spec.getColumnSpec(9).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(9).getDomain().getValues().contains(
                new StringCell("A")));
        assertTrue(spec.getColumnSpec(9).getDomain().getValues().contains(
                new StringCell("a")));
        assertEquals(spec.getColumnSpec(9).getDomain().getValues().size(), 2);

        assertTrue(spec.getColumnSpec(10).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(10).getDomain().getValues().contains(
                new StringCell("b")));
        assertTrue(spec.getColumnSpec(10).getDomain().getValues().contains(
                new StringCell("a")));
        assertEquals(spec.getColumnSpec(10).getDomain().getValues().size(), 2);

        assertTrue(spec.getColumnSpec(11).getDomain().hasValues());
        assertTrue(spec.getColumnSpec(11).getDomain().getValues().contains(
                new StringCell("c")));
        assertTrue(spec.getColumnSpec(11).getDomain().getValues().contains(
                new StringCell("b")));
        assertTrue(spec.getColumnSpec(11).getDomain().getValues().contains(
                new StringCell("a")));
        assertEquals(spec.getColumnSpec(11).getDomain().getValues().size(), 3);

    }

}
