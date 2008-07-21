/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.base.node.preproc.pivot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.knime.base.data.statistics.StatisticsTable;
import org.knime.base.node.preproc.groupby.AggregationMethod;
import org.knime.base.node.preproc.groupby.GroupByNodeFactory;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettings;

/**
 * This test ensures that the calculation of the mean is equal 
 * (and of course correct) for the following nodes:
 * <ul>
 * <li>GroupBy</li>
 * <li>Pivot</li>
 * <li>Statistics View</li>
 * </ul>
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MeanCaluclation extends TestCase {

    public void testSimpleMean() throws Exception {
        // create data table spec
        
        DataColumnSpecCreator colSpecCreator1 = new DataColumnSpecCreator(
                "Col1", StringCell.TYPE);
        DataColumnSpecCreator colSpecCreator2 = new DataColumnSpecCreator(
                "Col2", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator3 = new DataColumnSpecCreator(
                "Col3", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator4  = new DataColumnSpecCreator(
                "Col4", DoubleCell.TYPE);
        DataTableSpec spec = new DataTableSpec(
                colSpecCreator1.createSpec(), 
                colSpecCreator2.createSpec(),
                colSpecCreator3.createSpec(),
                colSpecCreator4.createSpec());
        
        DataContainer container = new DataContainer(spec);
        container.addRowToTable(
                new DefaultRow("Row1", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(2), 
                        new DoubleCell(3)));
        container.addRowToTable(
                new DefaultRow("Row2", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(4), 
                        new DoubleCell(6)));
        container.addRowToTable(
                new DefaultRow("Row3", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(6), 
                        new DoubleCell(3)));
        container.addRowToTable(
                new DefaultRow("Row4", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(8), 
                        new DoubleCell(4.5)));
        container.addRowToTable(
                new DefaultRow("Row5", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(10), 
                        new DoubleCell(1.5)));
        container.close();
        // col1 = 1
        // col2 = 6
        // col3 = 3.6
        double[] reference = new double[] {Double.NaN, 1.0, 6.0, 3.6};
        
        performChecks(container.getTable(), reference);
    }
    
    public void testMeanWithMissingValues() throws Exception {
        // create data table spec
        
        DataColumnSpecCreator colSpecCreator1 = new DataColumnSpecCreator(
                "Col1", StringCell.TYPE);
        DataColumnSpecCreator colSpecCreator2 = new DataColumnSpecCreator(
                "Col2", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator3 = new DataColumnSpecCreator(
                "Col3", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator4  = new DataColumnSpecCreator(
                "Col4", DoubleCell.TYPE);
        DataTableSpec spec = new DataTableSpec(
                colSpecCreator1.createSpec(), 
                colSpecCreator2.createSpec(),
                colSpecCreator3.createSpec(),
                colSpecCreator4.createSpec());
        
        DataContainer container = new DataContainer(spec);
        container.addRowToTable(
                new DefaultRow("Row1", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        DataType.getMissingCell(), 
                        new DoubleCell(3)));
        container.addRowToTable(
                new DefaultRow("Row2", 
                        new StringCell("A"),
                        DataType.getMissingCell(),
                        new DoubleCell(4), 
                        DataType.getMissingCell()));
        container.addRowToTable(
                new DefaultRow("Row3", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(6), 
                        DataType.getMissingCell()));
        container.addRowToTable(
                new DefaultRow("Row4", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(8), 
                        new DoubleCell(4.5)));
        container.addRowToTable(
                new DefaultRow("Row5", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(10), 
                        new DoubleCell(1.5)));
        container.close();
        // col1 = 1
        // col2 = 7
        // col3 = 3
        double[] reference = new double[] {Double.NaN, 1.0, 7.0, 3.0};
        
        performChecks(container.getTable(), reference);
    }

    
    public void testMeanWithEvenMoreMissingValues() 
        throws Exception {
        // create data table spec
        DataColumnSpecCreator colSpecCreator1 = new DataColumnSpecCreator(
                "Col1", StringCell.TYPE);
        DataColumnSpecCreator colSpecCreator2 = new DataColumnSpecCreator(
                "Col2", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator3 = new DataColumnSpecCreator(
                "Col3", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator4  = new DataColumnSpecCreator(
                "Col4", DoubleCell.TYPE);
        DataTableSpec spec = new DataTableSpec(
                colSpecCreator1.createSpec(), 
                colSpecCreator2.createSpec(),
                colSpecCreator3.createSpec(),
                colSpecCreator4.createSpec());
        
        DataContainer container = new DataContainer(spec);
        container.addRowToTable(
                new DefaultRow("Row1", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        DataType.getMissingCell(), 
                        DataType.getMissingCell()));
        container.addRowToTable(
                new DefaultRow("Row2", 
                        new StringCell("A"),
                        DataType.getMissingCell(),
                        new DoubleCell(4), 
                        DataType.getMissingCell()));
        container.addRowToTable(
                new DefaultRow("Row3", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(6), 
                        DataType.getMissingCell()));
        container.addRowToTable(
                new DefaultRow("Row4", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(8), 
                        DataType.getMissingCell()));
        container.addRowToTable(
                new DefaultRow("Row5", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(10), 
                        DataType.getMissingCell()));
        container.close();
        // col1 = 1
        // col2 = 7
        // col3 = 3
        double[] reference = new double[] {Double.NaN, 1.0, 7.0, Double.NaN};
        DataTable table = container.getTable();
        // statistics table
        StatisticsTable statsTable = new StatisticsTable(
                table, new ExecutionMonitor());
        double[] means = statsTable.getMean();
        assertTrue(Arrays.equals(reference, means));
        // group by
        Node groupByNode = new Node(new GroupByNodeFactory());
        ExecutionContext exec = new ExecutionContext(
                new DefaultNodeProgressMonitor(),
                groupByNode);
        BufferedDataTable bdt = exec.createBufferedDataTable(
                table, exec);
        List<String>colNames = new ArrayList<String>();
        colNames.add("Col1"); 
        GroupByTable groupTable = new GroupByTable(bdt, colNames, 
                AggregationMethod.MEAN, AggregationMethod.FIRST,
                10000, true, false, false, true, exec);
        for (DataRow row : groupTable.getBufferedTable()) {
            assertEquals(reference[1], 
                    ((DoubleValue)row.getCell(1)).getDoubleValue());
            assertEquals(reference[2], 
                    ((DoubleValue)row.getCell(2)).getDoubleValue());
            assertTrue(row.getCell(3).isMissing());
        }
        // pivoting
        PivotNodeModel pivotModel = new PivotNodeModel();
        NodeSettings settings = new NodeSettings("test");
        pivotModel.saveSettingsTo(settings);
        settings.addString("group_column", "Col1");
        settings.addString("pivot_column", "Col1");
        settings.addString("aggregation_column", "Col3");
        settings.addString("make_aggregation", "Enable aggregation");
        settings.addString("aggregation_method", "MEAN");
        pivotModel.validateSettings(settings);
        pivotModel.loadValidatedSettingsFrom(settings);
        pivotModel.configure(new DataTableSpec[] {bdt.getDataTableSpec()});
        BufferedDataTable[] out = pivotModel.execute(
                new BufferedDataTable[] {bdt}, exec);
        for (DataRow row : out[0]) {
            assertEquals(reference[2], 
                    ((DoubleValue)row.getCell(0)).getDoubleValue());
        }
        settings.addString("aggregation_column", "Col4");
        pivotModel.validateSettings(settings);
        pivotModel.loadValidatedSettingsFrom(settings);
        pivotModel.configure(new DataTableSpec[] {bdt.getDataTableSpec()});
        out = pivotModel.execute(
                new BufferedDataTable[] {bdt}, exec);
        for (DataRow row : out[0]) {
            assertTrue(row.getCell(0).isMissing());
        }
        settings.addString("aggregation_column", "Col2");
        pivotModel.validateSettings(settings);
        pivotModel.loadValidatedSettingsFrom(settings);
        pivotModel.configure(new DataTableSpec[] {bdt.getDataTableSpec()});
        out = pivotModel.execute(
                new BufferedDataTable[] {bdt}, exec);
        for (DataRow row : out[0]) {
            assertEquals(reference[1], 
                    ((DoubleValue)row.getCell(0)).getDoubleValue());
        }
    }
    
    public void testWithNegativeValues() throws Exception {
        // create data table spec
        DataColumnSpecCreator colSpecCreator1 = new DataColumnSpecCreator(
                "Col1", StringCell.TYPE);
        DataColumnSpecCreator colSpecCreator2 = new DataColumnSpecCreator(
                "Col2", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator3 = new DataColumnSpecCreator(
                "Col3", DoubleCell.TYPE);
        DataColumnSpecCreator colSpecCreator4  = new DataColumnSpecCreator(
                "Col4", DoubleCell.TYPE);
        DataTableSpec spec = new DataTableSpec(
                colSpecCreator1.createSpec(), 
                colSpecCreator2.createSpec(),
                colSpecCreator3.createSpec(),
                colSpecCreator4.createSpec());
        
        DataContainer container = new DataContainer(spec);
        container.addRowToTable(
                new DefaultRow("Row1", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(-4), 
                        new DoubleCell(-4)));
        container.addRowToTable(
                new DefaultRow("Row2", 
                        new StringCell("A"),
                        DataType.getMissingCell(),
                        new DoubleCell(4), 
                        new DoubleCell(4)));
        container.addRowToTable(
                new DefaultRow("Row3", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(-4), 
                        new DoubleCell(-4)));
        container.addRowToTable(
                new DefaultRow("Row4", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(4), 
                        new DoubleCell(4)));
        container.addRowToTable(
                new DefaultRow("Row5", 
                        new StringCell("A"),
                        new DoubleCell(1), 
                        new DoubleCell(4), 
                        DataType.getMissingCell()));
        container.close();
        // col1 = 1
        // col2 = 7
        // col3 = 3
        double[] reference = new double[] {Double.NaN, 1.0, 0.8, 0.0};

        performChecks(container.getTable(), reference);
    }
    
    private void performChecks(DataTable table, double[] reference) 
        throws Exception {
        // statistics table
        StatisticsTable statsTable = new StatisticsTable(
                table, new ExecutionMonitor());
        double[] means = statsTable.getMean();
        assertTrue(Arrays.equals(reference, means));
        // group by
        Node groupByNode = new Node(new GroupByNodeFactory());
        ExecutionContext exec = new ExecutionContext(
                new DefaultNodeProgressMonitor(),
                groupByNode);
        BufferedDataTable bdt = exec.createBufferedDataTable(
                table, exec);
        List<String>colNames = new ArrayList<String>();
        colNames.add("Col1"); 
        GroupByTable groupTable = new GroupByTable(bdt, colNames, 
                AggregationMethod.MEAN, AggregationMethod.FIRST,
                10000, true, false, false, true, exec);
        for (DataRow row : groupTable.getBufferedTable()) {
            assertEquals(reference[1], 
                    ((DoubleValue)row.getCell(1)).getDoubleValue());
            assertEquals(reference[2], 
                    ((DoubleValue)row.getCell(2)).getDoubleValue());
            assertEquals(reference[3], 
                    ((DoubleValue)row.getCell(3)).getDoubleValue());
        }
        // pivoting
        PivotNodeModel pivotModel = new PivotNodeModel();
        NodeSettings settings = new NodeSettings("test");
        pivotModel.saveSettingsTo(settings);
        settings.addString("group_column", "Col1");
        settings.addString("pivot_column", "Col1");
        settings.addString("aggregation_column", "Col3");
        settings.addString("make_aggregation", "Enable aggregation");
        settings.addString("aggregation_method", "MEAN");
        pivotModel.validateSettings(settings);
        pivotModel.loadValidatedSettingsFrom(settings);
        pivotModel.configure(new DataTableSpec[] {bdt.getDataTableSpec()});
        BufferedDataTable[] out = pivotModel.execute(
                new BufferedDataTable[] {bdt}, exec);
        for (DataRow row : out[0]) {
            assertEquals(reference[2], 
                    ((DoubleValue)row.getCell(0)).getDoubleValue());
        }
        settings.addString("aggregation_column", "Col4");
        pivotModel.validateSettings(settings);
        pivotModel.loadValidatedSettingsFrom(settings);
        pivotModel.configure(new DataTableSpec[] {bdt.getDataTableSpec()});
        out = pivotModel.execute(
                new BufferedDataTable[] {bdt}, exec);
        for (DataRow row : out[0]) {
            assertEquals(reference[3], 
                    ((DoubleValue)row.getCell(0)).getDoubleValue());
        }
        settings.addString("aggregation_column", "Col2");
        pivotModel.validateSettings(settings);
        pivotModel.loadValidatedSettingsFrom(settings);
        pivotModel.configure(new DataTableSpec[] {bdt.getDataTableSpec()});
        out = pivotModel.execute(
                new BufferedDataTable[] {bdt}, exec);
        for (DataRow row : out[0]) {
            assertEquals(reference[1], 
                    ((DoubleValue)row.getCell(0)).getDoubleValue());
        }
    }
    
}
