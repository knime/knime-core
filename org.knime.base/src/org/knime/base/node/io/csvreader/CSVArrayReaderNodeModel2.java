/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 4, 2009 (wiswedel): created
 */
package org.knime.base.node.io.csvreader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.vector.doublevector.DoubleVectorCellFactory;
import org.knime.core.data.vector.stringvector.StringVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.core.util.tokenizer.SettingsStatus;

/**
 * Model for CSV Reader node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class CSVArrayReaderNodeModel2 extends NodeModel {

    private CSVReaderConfig m_config;

    /** No input, one output. */
    CSVArrayReaderNodeModel2() {
        super(0, 1);
    }


    /**
     * @param context the node creation context
     */
    CSVArrayReaderNodeModel2(final NodeCreationContext context) {
        this();
        m_config = new CSVReaderConfig();
        m_config.setLocation(context.getUrl().toString());
    }


    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_config == null) {
            throw new InvalidSettingsException("No settings available");
        }

        String warning = CheckUtils.checkSourceFile(m_config.getLocation());
        if (warning != null) {
            setWarningMessage(warning);
        }

        return new DataTableSpec[] {null};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // prepare the settings for the file analyzer
        FileReaderNodeSettings settings = new FileReaderNodeSettings();

        CheckUtils.checkSourceFile(m_config.getLocation());
        URL url = FileUtil.toURL(m_config.getLocation());
        settings.setDataFileLocationAndUpdateTableName(url);

        String colDel = m_config.getColDelimiter();
        if (colDel != null && !colDel.isEmpty()) {
            settings.addDelimiterPattern(colDel, /*combine multiple*/true, false, false);
        }
        settings.setDelimiterUserSet(true);

        String rowDel = m_config.getRowDelimiter();
        if (rowDel != null && !rowDel.isEmpty()) {
            settings.addRowDelimiter(rowDel, true);
        }
        String quote = m_config.getQuoteString();
        if (quote != null && !quote.isEmpty()) {
            settings.addQuotePattern(quote, quote);
        }
        settings.setQuoteUserSet(true);

        String commentStart = m_config.getCommentStart();
        if (commentStart != null && !commentStart.isEmpty()) {
            settings.addSingleLineCommentPattern(commentStart, false, false);
        }
        settings.setCommentUserSet(true);

        boolean hasColHeader = m_config.hasColHeader();
        settings.setFileHasColumnHeaders(hasColHeader);
        settings.setFileHasColumnHeadersUserSet(true);

        boolean hasRowHeader = m_config.hasRowHeader();
        settings.setFileHasRowHeaders(hasRowHeader);
        settings.setFileHasRowHeadersUserSet(true);
        if (!hasRowHeader) {
            settings.setRowHeaderPrefix("Row");
        }

        settings.setWhiteSpaceUserSet(true);

        boolean supportShortLines = m_config.isSupportShortLines();
        settings.setSupportShortLines(supportShortLines);

        int skipFirstLinesCount = m_config.getSkipFirstLinesCount();
        settings.setSkipFirstLines(skipFirstLinesCount);

        long limitRowsCount = m_config.getLimitRowsCount();
        settings.setMaximumNumberOfRowsToRead(limitRowsCount);

        settings.setCharsetName(m_config.getCharSetName());
        exec.setMessage("Analyzing file");

        Vector<ColProperty> cols = new Vector<ColProperty>();
        // one col with double vector cell
        ColProperty colProp1 = new ColProperty();
        colProp1.setColumnSpec(new DataColumnSpecCreator("Values", DoubleVectorCellFactory.TYPE).createSpec());
        colProp1.setMissingValuePattern(null);
        colProp1.setUserSettings(true);
        cols.add(colProp1);
        ColProperty colProp2 = new ColProperty();
        colProp2.setColumnSpec(new DataColumnSpecCreator("Labels", StringVectorCellFactory.TYPE).createSpec());
        colProp2.setUserSettings(true);
        colProp2.setMissingValuePattern(null);
        cols.add(colProp2);
        settings.setColumnProperties(cols);
        settings.setNumberOfColumns(cols.size());

        SettingsStatus status = settings.getStatusOfSettings();
        if (status.getNumOfErrors() > 0) {
            throw new Exception(status.getAllErrorMessages(20));
        }

        exec.setMessage("Buffering file");

        DataTableSpec spec = settings.createDataTableSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        BufferedFileReader reader = settings.createNewInputReader();
        String line = null;
        long lineCnt = 0;
        int dblCnt = -1;
        int strCnt = -1;
        long fileSize = reader.getFileSize();
        ArrayList<String> colHdrs = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            lineCnt++;
            exec.setProgress(reader.getNumberOfBytesRead() / (double)fileSize);
            if (line.trim().isEmpty()) {
                // ignore empty lines
                continue;
            }
            String[] split = line.split(colDel);
            int splitLength = split.length;
            int idx = 0;

            // first line might be special
            if (hasColHeader && lineCnt == 1) {
                setColHdrs(split, colHdrs);
                continue;
            }

            String rowID = "Row" + lineCnt;
            if (hasRowHeader) {
                rowID = split[idx++];
            }

            // read double values
            int dblSize = (dblCnt < 0) ? 50 : dblCnt;
            ArrayList<Double> doubleVals = new ArrayList<Double>(dblSize);
            while (idx < splitLength) {
//                if (split[idx].isEmpty()) {
//                    idx++; // ignore consecutive tabs
//                    continue;
//                }
                if (split[idx].trim().isEmpty()) {
                    doubleVals.add(0.0d);
                } else {
                    try {
                        doubleVals.add(Double.parseDouble(split[idx].trim()));
                    } catch (NumberFormatException nfe) {
                        break;
                    }
                }
                idx++;
            }
            if (dblCnt < doubleVals.size()) {
                dblCnt = doubleVals.size();
//            } else {
//                if (doubleVals.size() != dblCnt) {
//                    throw new Exception("Unexpected number of double values (got " + doubleVals.size() + " expected "
//                        + dblCnt + ") in line " + reader.getCurrentLineNumber());
//                }
            }

            // read string values
            int strSize = (strCnt < 0) ? 50 : strCnt;
            ArrayList<String> strVals = new ArrayList<String>(strSize);
            while (idx < splitLength) {
//                if (split[idx].isEmpty()) {
//                    idx++; // ignore consecutive tabs
//                    continue;
//                }
                strVals.add(split[idx].trim());
                idx++;
            }
            if (strCnt < strVals.size()) {
                strCnt = strVals.size();
//            } else {
//                if (strVals.size() != strCnt) {
//                    throw new Exception("Unexpected number of strin values (got " + strVals.size() + " expected "
//                        + strCnt + ") in line " + reader.getCurrentLineNumber());
//                }
            }

            DataCell[] cells = new DataCell[2];
            cells[0] = DoubleVectorCellFactory.createCell(doubleVals.stream().mapToDouble(d->d).toArray());
            cells[1] = StringVectorCellFactory.createCell(strVals.toArray(new String[strVals.size()]));
            container.addRowToTable(new DefaultRow(rowID, cells));
        }

        container.close();
        // add column names to table spec (split in dbl cols and string cols)
        String[] valNames = new String[dblCnt];
        String[] labelNames = new String[strCnt];
        for (int i = 0; i < dblCnt; i++) {
            if (i < colHdrs.size()) {
                valNames[i] = colHdrs.get(i);
            } else {
                valNames[i] = "Val_" + i;
            }
        }
        for (int j = 0; j < strCnt; j++) {
            if (j + dblCnt < colHdrs.size()) {
                labelNames[j] = colHdrs.get(j + dblCnt);
            } else {
                labelNames[j] = "Label_" + j;
            }
        }
        // add to double vector column
        DataColumnSpecCreator dblCreator = new DataColumnSpecCreator(spec.getColumnSpec(0));
        dblCreator.setElementNames(valNames);
        // add to string vector column
        DataColumnSpecCreator strCreator = new DataColumnSpecCreator(spec.getColumnSpec(1));
        strCreator.setElementNames(labelNames);
        DataTableSpec newSpec = new DataTableSpec(dblCreator.createSpec(), strCreator.createSpec());

        BufferedDataTable finalTable = exec.createSpecReplacerTable(container.getTable(), newSpec);
        return new BufferedDataTable[] {finalTable};
    }

    private void setColHdrs(final String[] names, final ArrayList<String> hdrs) {
        for (String h : names) {
            if (!h.trim().isEmpty()) {
                hdrs.add(h.trim());
            }
        }
    }

//    private long getNumOfCols(final FileReaderSettings settings, final ExecutionContext exec) throws Exception {
//        BufferedFileReader reader = settings.createNewInputReader();
//        Tokenizer tokenizer = new Tokenizer(reader);
//        tokenizer.setSettings(settings);
//        String token;
//        if (settings.getFileHasColumnHeaders()) {
//            // read the first line
//            while ((token = tokenizer.nextToken()) != null) {
//                exec.checkCanceled();
//                if (settings.isRowDelimiter(token, tokenizer.lastTokenWasQuoted())) {
//                    break;
//                }
//            }
//            CheckUtils.checkState(token != null, "File has no data (first line is read as column header).");
//        }
//
//        if (settings.getFileHasRowHeaders()) {
//            // swallow row id
//            token = tokenizer.nextToken();
//        }
//        long numOfCols = 0;
//        while ((token = tokenizer.nextToken()) != null) {
//            exec.checkCanceled();
//            numOfCols++;
//        }
//        return numOfCols;
//    }
//
//
    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        new CSVReaderConfig().loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        CSVReaderConfig config = new CSVReaderConfig();
        config.loadSettingsInModel(settings);
        m_config = config;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            m_config.saveSettingsTo(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

}
