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
 *    16.07.2009 (Tobias Koetter): created
 */

package org.knime.testing.node.datagenerator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class TestDataNodeModel extends NodeModel {

    private static final String[] stringVals = new String[] {
        "semicolon;semicolon", "backslash\\bakslash", "quotes\"quotes",
        "carriage return \r carriage return", "new line \n new line",
        "carriage return and new line \r\n carriage return and new line",
        "tab\ttab", "comma,comma", "single quote'single quote",
        "    ", ""};
    private static final int[] intVals = new int[] {Integer.MAX_VALUE,
        Integer.MIN_VALUE, -1, 1, 0};
    private static final double[] doubleVals = new double[] {Double.NaN,
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_NORMAL,
        -Double.MAX_VALUE, -Double.MIN_VALUE, -Double.MIN_NORMAL};
    private static final Date[] dateVals = new Date[] {
        new Date(System.currentTimeMillis()), new Date(System.nanoTime()),
        new GregorianCalendar(1600, 1, 1).getTime(),
        new GregorianCalendar(0, 1, 1).getTime(),
        new GregorianCalendar(4000, 1, 1).getTime(),
        new GregorianCalendar(1600, 12, 31).getTime(),
        new GregorianCalendar(0, 12, 31).getTime(),
        new GregorianCalendar(4000, 12, 31).getTime(),
        new GregorianCalendar(1600, 1, 1, 1, 1, 1).getTime(),
        new GregorianCalendar(0, 1, 1, 1, 1, 1).getTime(),
        new GregorianCalendar(4000, 1, 1, 1, 1, 1).getTime(),
        new GregorianCalendar(1600, 1, 1, 24, 59, 59).getTime(),
        new GregorianCalendar(0, 1, 1, 24, 59, 59).getTime(),
        new GregorianCalendar(4000, 1, 1, 24, 59, 59).getTime()};

    private static final int noOfCols = 17;


    /**Constructor for class TestDataNodeModel.
     */
    protected TestDataNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] {createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataContainer dc =
            exec.createDataContainer(createSpec());
        final int noOfRows = Math.max(Math.max(Math.max(stringVals.length,
                intVals.length), doubleVals.length), dateVals.length);
        final int noOfListItems = noOfRows * 2;
        final int noOfSetItems = noOfRows;
        for (int rowIdx = 0; rowIdx < noOfRows; rowIdx++) {
            final DataCell[] cells = new DataCell[noOfCols];
            int i = 0;
            cells[i++] = getStringVal(rowIdx);
            cells[i++] = getStringListVal(rowIdx, noOfListItems);
            cells[i++] = getStringSetVal(rowIdx, noOfSetItems);

            cells[i++] = getIntVal(rowIdx);
            cells[i++] = getIntListVal(rowIdx, noOfListItems);
            cells[i++] = getIntSetVal(rowIdx, noOfSetItems);

            cells[i++] = getDoubleVal(rowIdx);
            cells[i++] = getDoubleListVal(rowIdx, noOfListItems);
            cells[i++] = getDoubleSetVal(rowIdx, noOfSetItems);

            cells[i++] = getTimestampVal(rowIdx);
            cells[i++] = getTimestampListVal(rowIdx, noOfListItems);
            cells[i++] = getTimestampSetVal(rowIdx, noOfSetItems);


            cells[i++] = getMissingVal(rowIdx);
            cells[i++] = getMissingValListVal(rowIdx, noOfListItems);
            cells[i++] = getMissingValSetVal(rowIdx, noOfSetItems);

            cells[i++] = getStringVal(rowIdx);
            cells[i++] = getDoubleVal(rowIdx);

            final DefaultRow row =
                new DefaultRow(RowKey.createRowKey(rowIdx), cells);
            dc.addRowToTable(row);
        }
        dc.close();
        return new BufferedDataTable[] {dc.getTable()};
    }

    private DataCell getMissingVal(
            @SuppressWarnings("unused") final int rowIdx) {
        return DataType.getMissingCell();
    }

    private DataCell getMissingValSetVal(
            @SuppressWarnings("unused") final int rowIdx, final int i) {
        return CollectionCellFactory.createSetCell(
                createMissingCellCollection(i));
    }

    private DataCell getMissingValListVal(
            @SuppressWarnings("unused") final int rowIdx, final int i) {
        return CollectionCellFactory.createListCell(
                createMissingCellCollection(i));
    }

    private Collection<DataCell> createMissingCellCollection(final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = 0; i < noOf; i++) {
            cells.add(getMissingVal(i));
        }
        return cells;
    }

    private DataCell getTimestampVal(final int rowIdx) {
        return new DateAndTimeCell(
                dateVals[rowIdx % dateVals.length].getTime(), true, true, true);
    }

    private DataCell getTimestampSetVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createSetCell(
                createTimestampCellCollection(rowIdx, i));
    }

    private DataCell getTimestampListVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createListCell(
                createTimestampCellCollection(rowIdx, i));
    }

    private Collection<DataCell> createTimestampCellCollection(
            final int start, final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = start; i < noOf + start; i++) {
            cells.add(getTimestampVal(i));
        }
        return cells;
    }

    private DataCell getDoubleVal(final int rowIdx) {
        double val;
        if (rowIdx >= doubleVals.length) {
            val = Math.random();
        } else {
            val = doubleVals[rowIdx];
        }
        return new DoubleCell(val);
    }

    private DataCell getDoubleSetVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createSetCell(
                createDoubleCollection(rowIdx, i));
    }

    private DataCell getDoubleListVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createListCell(
                createDoubleCollection(rowIdx, i));
    }

    private Collection<DataCell> createDoubleCollection(
            final int start, final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = start; i < noOf + start; i++) {
            cells.add(getDoubleVal(i));
        }
        return cells;
    }

    private DataCell getIntVal(final int rowIdx) {
        int val;
        if (rowIdx >= intVals.length) {
            val = (int)Math.round(Math.random() * 10000);
        } else {
            val = intVals[rowIdx];
        }
        return new IntCell(val);
    }

    private DataCell getIntSetVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createSetCell(
                createIntCollection(rowIdx, i));
    }

    private DataCell getIntListVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createListCell(
                createIntCollection(rowIdx, i));
    }

    private Collection<DataCell> createIntCollection(
            final int start, final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = start; i < noOf + start; i++) {
            cells.add(getIntVal(i));
        }
        return cells;
    }

    private DataCell getStringVal(final int rowIdx) {
        return new StringCell(stringVals[rowIdx % stringVals.length]);
    }

    private DataCell getStringSetVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createSetCell(
                createStringCellCollection(rowIdx, i));
    }

    private DataCell getStringListVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createListCell(
                createStringCellCollection(rowIdx, i));
    }

    private Collection<DataCell> createStringCellCollection(final int start,
            final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = start; i < noOf + start; i++) {
            cells.add(getStringVal(i));
        }
        return cells;
    }

    private static DataTableSpec createSpec() {
        final DataColumnSpec[] specs = new DataColumnSpec[noOfCols];
        int i = 0;
        final DataColumnSpecCreator creator =
            new DataColumnSpecCreator("StringCol", StringCell.TYPE);
        specs[i++] = creator.createSpec();
        creator.setName("StringListCol");
        creator.setType(ListCell.getCollectionType(StringCell.TYPE));
        specs[i++] = creator.createSpec();
        creator.setName("StringSetCol");
        creator.setType(SetCell.getCollectionType(StringCell.TYPE));
        specs[i++] = creator.createSpec();

        creator.setName("IntCol");
        creator.setType(IntCell.TYPE);
        specs[i++] = creator.createSpec();
        creator.setName("IntListCol");
        creator.setType(ListCell.getCollectionType(IntCell.TYPE));
        specs[i++] = creator.createSpec();
        creator.setName("IntSetCol");
        creator.setType(SetCell.getCollectionType(IntCell.TYPE));
        specs[i++] = creator.createSpec();

        creator.setName("DoubleCol");
        creator.setType(DoubleCell.TYPE);
        specs[i++] = creator.createSpec();
        creator.setName("DoubleListCol");
        creator.setType(ListCell.getCollectionType(DoubleCell.TYPE));
        specs[i++] = creator.createSpec();
        creator.setName("DoubleSetCol");
        creator.setType(SetCell.getCollectionType(DoubleCell.TYPE));
        specs[i++] = creator.createSpec();

        creator.setName("TimestampCol");
        creator.setType(DateAndTimeCell.TYPE);
        specs[i++] = creator.createSpec();
        creator.setName("TimestampListCol");
        creator.setType(ListCell.getCollectionType(DateAndTimeCell.TYPE));
        specs[i++] = creator.createSpec();
        creator.setName("TimestampSetCol");
        creator.setType(SetCell.getCollectionType(DateAndTimeCell.TYPE));
        specs[i++] = creator.createSpec();

        creator.setName("MissingValStringCol");
        creator.setType(StringCell.TYPE);
        specs[i++] = creator.createSpec();
        creator.setName("MissingValStringListCol");
        creator.setType(ListCell.getCollectionType(StringCell.TYPE));
        specs[i++] = creator.createSpec();
        creator.setName("MissingValStringSetCol");
        creator.setType(SetCell.getCollectionType(StringCell.TYPE));
        specs[i++] = creator.createSpec();
        creator.setName("LongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnName");
        creator.setType(StringCell.TYPE);
        specs[i++] = creator.createSpec();
        creator.setName("LongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnName");
        creator.setType(DoubleCell.TYPE);
        specs[i++] = creator.createSpec();

        return new DataTableSpec(specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do
    }
}
