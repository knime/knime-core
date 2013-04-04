/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Random;
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
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;


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
    private static final String[] uriExtensions = new String[] {"", "csv", "xml", "html"};
    private static final String[] uriSchemes = new String[] {"http", "https", "file", "sftp"};
    private static final int[] intVals = new int[] {Integer.MAX_VALUE,
        Integer.MIN_VALUE, -1, 1, 0, 65, 123789043, -489546568};
    private static final long[] longVals = new long[] {Long.MAX_VALUE,
        Long.MIN_VALUE, -1, 1, 0, 6782346868234l, 4327897691234567123l,
        -3685468548523478l, -678546786868234l};
    private static final double[] doubleVals = new double[] {Double.NaN,
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_NORMAL,
        -Double.MAX_VALUE, -Double.MIN_VALUE, -Double.MIN_NORMAL};
    private static final Date[] dateVals = new Date[] {
        new Date(System.currentTimeMillis()),
        new Date(System.nanoTime()),
        new Date(0),
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

    private final SettingsModelInteger m_noOfRows = createNoOfRowsModel();
    private final SettingsModelInteger m_noOfListItems =
        createNoOfListItemsModel();
    private final SettingsModelInteger m_noOfSetItems =
        createNoOfSetItemsModel();
    private final SettingsModelInteger m_maxStringLength =
        createMaxStringLengthModel();
    private final SettingsModelInteger m_noOfAllMissingRows = createNoOfAllMissingRowsModel();

    private final static Random rnd = new Random();

    /**Constructor for class TestDataNodeModel.
     */
    protected TestDataNodeModel() {
        super(0, 1);
    }

    /**
     * @return the no of all missing rows
     */
    static SettingsModelInteger createNoOfAllMissingRowsModel() {
        return new SettingsModelIntegerBounded("noOfAllMissingRows", 0, 0, Integer.MAX_VALUE);
    }

    /**
     * @return the max string length model
     */
    static SettingsModelInteger createMaxStringLengthModel() {
        return new SettingsModelIntegerBounded("noStringLength", 350, 1,
                Integer.MAX_VALUE);
    }

    /**
     * @return the number of rows model
     */
    static SettingsModelInteger createNoOfRowsModel() {
        return new SettingsModelIntegerBounded("noOfRows", 100, 1,
                Integer.MAX_VALUE);
    }

    /**
     * @return the number of list items model
     */
    static SettingsModelInteger createNoOfListItemsModel() {
        return new SettingsModelIntegerBounded("noOfListItems", 50, 1,
                Integer.MAX_VALUE);
    }

    /**
     * @return the number of set items model
     */
    static SettingsModelInteger createNoOfSetItemsModel() {
        return new SettingsModelIntegerBounded("noOfSetItems", 10, 1,
                Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(@SuppressWarnings("unused") final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] {createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(@SuppressWarnings("unused") final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final DataTableSpec newSpec = createSpec();
        final BufferedDataContainer dc = exec.createDataContainer(newSpec);
        final int totalNoOfRows = m_noOfRows.getIntValue();
        final int noOfValueRows = totalNoOfRows - m_noOfAllMissingRows.getIntValue();
        final int noOfListItems = m_noOfListItems.getIntValue();
        final int noOfSetItems = m_noOfSetItems.getIntValue();
        for (int rowIdx = 0; rowIdx < noOfValueRows; rowIdx++) {
            exec.setProgress((double)rowIdx / totalNoOfRows, "Generating row "
                    + rowIdx + " of " + totalNoOfRows);
            exec.checkCanceled();
            final LinkedList<DataCell> cells = new LinkedList<DataCell>();
            cells.add(getStringVal(exec, rowIdx));
            cells.add(getStringListVal(exec, rowIdx, noOfListItems));
            cells.add(getStringSetVal(exec, rowIdx, noOfSetItems));

            cells.add(getIntVal(rowIdx));
            cells.add(getIntListVal(rowIdx, noOfListItems));
            cells.add(getIntSetVal(rowIdx, noOfSetItems));

            cells.add(getLongVal(rowIdx));
            cells.add(getLongListVal(rowIdx, noOfListItems));
            cells.add(getLongSetVal(rowIdx, noOfSetItems));

            cells.add(getDoubleVal(rowIdx));
            cells.add(getDoubleListVal(rowIdx, noOfListItems));
            cells.add(getDoubleSetVal(rowIdx, noOfSetItems));

            cells.add(getTimestampVal(rowIdx));
            cells.add(getTimestampListVal(rowIdx, noOfListItems));
            cells.add(getTimestampSetVal(rowIdx, noOfSetItems));

            cells.add(getBooleanVal(rowIdx));
            cells.add(getBooleanListVal(noOfListItems));
            cells.add(getBooleanSetVal(noOfSetItems));

            cells.add(getUriVal(rowIdx));
            cells.add(getUriListVal(rowIdx, noOfListItems));
            cells.add(getUriSetVal(rowIdx, noOfSetItems));

            cells.add(getMissingVal(rowIdx));
            cells.add(getMissingValListVal(rowIdx, noOfListItems));
            cells.add(getMissingValSetVal(rowIdx, noOfSetItems));

            cells.add(getStringVal(exec, rowIdx));
            cells.add(getDoubleVal(rowIdx));

            final DefaultRow row =
                new DefaultRow(RowKey.createRowKey(rowIdx), cells);
            dc.addRowToTable(row);
        }
        //add the all missing cells row last
        final DataCell[] allMissing = new DataCell[newSpec.getNumColumns()];
        Arrays.fill(allMissing, getMissingVal(0));
        for (int i = 0; i < m_noOfAllMissingRows.getIntValue(); i++) {
            final int rowIdx = noOfValueRows + i;
            exec.setProgress((double)rowIdx / totalNoOfRows, "Generating row "
                    + rowIdx + " of " + totalNoOfRows);
            final DefaultRow row =
                    new DefaultRow(RowKey.createRowKey(rowIdx), allMissing);
            dc.addRowToTable(row);
        }
        dc.close();
        return new BufferedDataTable[] {dc.getTable()};
    }

    private DataCell getBooleanListVal(final int noOf) {
        return CollectionCellFactory.createListCell(
                createBooleanCollection(noOf));
    }

    private DataCell getBooleanSetVal(final int noOf) {
        return CollectionCellFactory.createSetCell(
                createBooleanCollection(noOf));
    }

    private Collection<DataCell> createBooleanCollection(final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = 0; i < noOf; i++) {
            cells.add(getBooleanVal(i));
        }
        return cells;
    }

    /**
     * @param rowIdx
     * @return
     */
    private DataCell getBooleanVal(final int rowIdx) {
        if (rowIdx % 2 == 0) {
            return BooleanCell.TRUE;
        }
        return BooleanCell.FALSE;
    }


    private DataCell getUriVal(final int rowIdx){
        final String scheme = uriSchemes[rowIdx % uriSchemes.length];
        final String extension = uriExtensions[rowIdx % uriExtensions.length];
        final StringBuilder buf = new StringBuilder();
        buf.append(scheme);
        buf.append("://");
        if (!extension.isEmpty()) {
            buf.append("C:/");
        }
        for (int i = 0; i < 15; i++) {
            buf.append(String.valueOf((char) (rnd.nextInt(25) + 97)));
        }
        if (!extension.isEmpty()) {
            buf.append(".");
            buf.append(extension);
        }
        URI uri;
        try {
            uri = new URI(buf.toString());
        } catch (final URISyntaxException e) {
            return DataType.getMissingCell();
        }
        return new URIDataCell(new URIContent(uri, extension));
    }

    private DataCell getUriSetVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createSetCell(
                createUriCellCollection(rowIdx, i));
    }

    private DataCell getUriListVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createListCell(
                createUriCellCollection(rowIdx, i));
    }

    private Collection<DataCell> createUriCellCollection(final int start, final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = start; i < noOf + start; i++) {
            cells.add(getUriVal(i));
        }
        return cells;
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
        Date val;
        if (rowIdx >= dateVals.length) {
            long nextLong = rnd.nextLong();
            if (rowIdx % 2 == 0) {
                nextLong *= -1;
            }
            val = new Date(nextLong);
        } else {
            val = dateVals[rowIdx];
        }
        return new DateAndTimeCell(val.getTime(), true, true, true);
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
            final double random = Math.random();
            val = random;
            if (rowIdx % 2 == 0) {
                val *= -1;
            }
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
            val = rnd.nextInt();
            if (rowIdx % 2 == 0) {
                val *= -1;
            }
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

    private DataCell getLongVal(final int rowIdx) {
        long val;
        if (rowIdx >= longVals.length) {
            val = rnd.nextLong();
            if (rowIdx % 2 == 0) {
                val *= -1;
            }
        } else {
            val = longVals[rowIdx];
        }
        return new LongCell(val);
    }

    private DataCell getLongSetVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createSetCell(
                createLongCollection(rowIdx, i));
    }

    private DataCell getLongListVal(final int rowIdx, final int i) {
        return CollectionCellFactory.createListCell(
                createLongCollection(rowIdx, i));
    }

    private Collection<DataCell> createLongCollection(
            final int start, final int noOf) {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = start; i < noOf + start; i++) {
            cells.add(getLongVal(i));
        }
        return cells;
    }

    private DataCell getStringVal(final ExecutionContext exec, final int rowIdx)
        throws CanceledExecutionException {
        exec.checkCanceled();
        String val;
        if (rowIdx >= stringVals.length) {
            final StringBuffer sb = new StringBuffer();
            final int noOfChars = rnd.nextInt(m_maxStringLength.getIntValue());
            for (int i = noOfChars; i > 0; i--) {
                String string;
                if (rnd.nextInt(6) == 5) {
                    string = " ";
                } else {
                    exec.checkCanceled();
                    final int n = Math.min(12, Math.abs(i));
                    string = Long.toString(
                          Math.round(Math.random() * Math.pow(36, n)), 36);
                    if (rnd.nextBoolean()) {
                        string = string.toUpperCase();
                    }
                }
                sb.append(string);
            }
            val = sb.toString();
        } else {
            val = stringVals[rowIdx];
        }
        return new StringCell(val);
    }

    private DataCell getStringSetVal(final ExecutionContext exec,
            final int rowIdx, final int i) throws CanceledExecutionException {
        return CollectionCellFactory.createSetCell(
                createStringCellCollection(exec, rowIdx, i));
    }

    private DataCell getStringListVal(final ExecutionContext exec,
            final int rowIdx, final int i) throws CanceledExecutionException {
        return CollectionCellFactory.createListCell(
                createStringCellCollection(exec, rowIdx, i));
    }

    private Collection<DataCell> createStringCellCollection(
            final ExecutionContext exec, final int start, final int noOf)
            throws CanceledExecutionException {
        final Collection<DataCell> cells =
            new ArrayList<DataCell>(noOf);
        for (int i = start; i < noOf + start; i++) {
            cells.add(getStringVal(exec, i));
        }
        return cells;
    }

    private static DataTableSpec createSpec() {
        final LinkedList<DataColumnSpec> specs =
            new LinkedList<DataColumnSpec>();
        final DataColumnSpecCreator creator =
            new DataColumnSpecCreator("StringCol", StringCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("StringListCol");
        creator.setType(ListCell.getCollectionType(StringCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("StringSetCol");
        creator.setType(SetCell.getCollectionType(StringCell.TYPE));
        specs.add(creator.createSpec());

        creator.setName("IntCol");
        creator.setType(IntCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("IntListCol");
        creator.setType(ListCell.getCollectionType(IntCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("IntSetCol");
        creator.setType(SetCell.getCollectionType(IntCell.TYPE));
        specs.add(creator.createSpec());

        creator.setName("LongCol");
        creator.setType(LongCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("LongListCol");
        creator.setType(ListCell.getCollectionType(LongCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("LongSetCol");
        creator.setType(SetCell.getCollectionType(LongCell.TYPE));
        specs.add(creator.createSpec());

        creator.setName("DoubleCol");
        creator.setType(DoubleCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("DoubleListCol");
        creator.setType(ListCell.getCollectionType(DoubleCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("DoubleSetCol");
        creator.setType(SetCell.getCollectionType(DoubleCell.TYPE));
        specs.add(creator.createSpec());

        creator.setName("TimestampCol");
        creator.setType(DateAndTimeCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("TimestampListCol");
        creator.setType(ListCell.getCollectionType(DateAndTimeCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("TimestampSetCol");
        creator.setType(SetCell.getCollectionType(DateAndTimeCell.TYPE));
        specs.add(creator.createSpec());


        creator.setName("BooleanCol");
        creator.setType(BooleanCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("BooleanListCol");
        creator.setType(ListCell.getCollectionType(BooleanCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("BooleanSetCol");
        creator.setType(SetCell.getCollectionType(BooleanCell.TYPE));
        specs.add(creator.createSpec());


        creator.setName("UriCol");
        creator.setType(URIDataCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("UriListCol");
        creator.setType(ListCell.getCollectionType(URIDataCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("UriSetCol");
        creator.setType(SetCell.getCollectionType(URIDataCell.TYPE));
        specs.add(creator.createSpec());


        creator.setName("MissingValStringCol");
        creator.setType(StringCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("MissingValStringListCol");
        creator.setType(ListCell.getCollectionType(StringCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("MissingValStringSetCol");
        creator.setType(SetCell.getCollectionType(StringCell.TYPE));
        specs.add(creator.createSpec());
        creator.setName("LongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnNameLongStringColumnName"
                + "LongStringColumnNameLongStringColumnName");
        creator.setType(StringCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("LongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnNameLongDoubleColumnName"
                + "LongDoubleColumnNameLongDoubleColumnName");
        creator.setType(DoubleCell.TYPE);
        specs.add(creator.createSpec());

        return new DataTableSpec(specs.toArray(new DataColumnSpec[0]));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_noOfRows.loadSettingsFrom(settings);
        m_noOfListItems.loadSettingsFrom(settings);
        m_noOfSetItems.loadSettingsFrom(settings);
        m_maxStringLength.loadSettingsFrom(settings);
        try {
            m_noOfAllMissingRows.loadSettingsFrom(settings);
        } catch (final Exception e) {
            // new introduced in 2.8
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_noOfRows.saveSettingsTo(settings);
        m_noOfListItems.saveSettingsTo(settings);
        m_noOfSetItems.saveSettingsTo(settings);
        m_maxStringLength.saveSettingsTo(settings);
        m_noOfAllMissingRows.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_noOfRows.validateSettings(settings);
        final int totalNoOfRows = ((SettingsModelInteger)
                m_noOfRows.createCloneWithValidatedValue(settings)).getIntValue();
        m_noOfListItems.validateSettings(settings);
        m_noOfSetItems.validateSettings(settings);
        m_maxStringLength.validateSettings(settings);
        int noOfMissingRows = 0;
        try {
            noOfMissingRows = ((SettingsModelInteger)
                    m_noOfAllMissingRows.createCloneWithValidatedValue(settings)).getIntValue();
        } catch (final Exception e) {
            // new introduced in 2.8
        }
        if (totalNoOfRows < noOfMissingRows) {
            throw new InvalidSettingsException("Total number of rows is less than the number of all missing rows");
        }
    }
}
