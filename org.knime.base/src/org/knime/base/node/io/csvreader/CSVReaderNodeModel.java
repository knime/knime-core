/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

import org.knime.base.node.io.filereader.FileAnalyzer;
import org.knime.base.node.io.filereader.FileReaderExecutionMonitor;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.workflow.NodeProgress;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.util.FileUtil;
import org.knime.core.util.tokenizer.SettingsStatus;

/**
 * Model for CSV Reader node.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
// extended in wide data plug-in
public class CSVReaderNodeModel extends NodeModel {

    private CSVReaderConfig m_config;

    /** No input, one output. */
    protected CSVReaderNodeModel() {
        super(0, 1);
    }


    /**
     * @param context the node creation context
     */
    CSVReaderNodeModel(final NodeCreationContext context) {
        this();
        m_config = new CSVReaderConfig();
        m_config.setLocation(context.getUrl().toString());
    }

    /** @return the config */
    protected CSVReaderConfig getConfig() {
        return m_config;
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

        FileTable fTable = createFileTable(exec);
        try {
            BufferedDataTable table = exec.createBufferedDataTable(fTable, exec.createSubExecutionContext(0.0));
            return new BufferedDataTable[] {table};
        } finally {
            // fix AP-6127
            fTable.dispose();
        }
    }

    protected FileTable createFileTable(final ExecutionContext exec) throws Exception {
        // prepare the settings for the file analyzer
        FileReaderNodeSettings settings = new FileReaderNodeSettings();

        CheckUtils.checkSourceFile(m_config.getLocation());
        URL url = FileUtil.toURL(m_config.getLocation());
        settings.setDataFileLocationAndUpdateTableName(url);

        String colDel = m_config.getColDelimiter();
        if (colDel != null && !colDel.isEmpty()) {
            settings.addDelimiterPattern(colDel, false, false, false);
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

        settings.setWhiteSpaceUserSet(true);

        boolean supportShortLines = m_config.isSupportShortLines();
        settings.setSupportShortLines(supportShortLines);

        int skipFirstLinesCount = m_config.getSkipFirstLinesCount();
        settings.setSkipFirstLines(skipFirstLinesCount);

        final long limitRowsCount = m_config.getLimitRowsCount();
        settings.setMaximumNumberOfRowsToRead(limitRowsCount);

        settings.setCharsetName(m_config.getCharSetName());
        settings.setCharsetUserSet(true);

        settings.setConnectTimeout(m_config.getConnectTimeout());

        final int limitAnalysisCount = m_config.getLimitAnalysisCount();
        final ExecutionMonitor analyseExec = exec.createSubProgress(0.5);
        final ExecutionContext readExec = exec.createSubExecutionContext(0.5);
        exec.setMessage("Analyzing file");
        if (limitAnalysisCount >= 0) {
            final FileReaderExecutionMonitor fileReaderExec = new FileReaderExecutionMonitor();
            fileReaderExec.getProgressMonitor().addProgressListener(new NodeProgressListener() {

                @Override
                public void progressChanged(final NodeProgressEvent pe) {
                    try {
                        //if the node was canceled, cancel (interrupt) the analysis
                        analyseExec.checkCanceled();
                        //otherwise update the node progress
                        NodeProgress nodeProgress = pe.getNodeProgress();
                        analyseExec.setProgress(nodeProgress.getProgress(), nodeProgress.getMessage());
                    } catch (CanceledExecutionException e) {
                        fileReaderExec.setExecuteInterrupted();
                    }
                }
            });
            fileReaderExec.setShortCutLines(limitAnalysisCount);
            fileReaderExec.setExecuteCanceled();
            settings = FileAnalyzer.analyze(settings, fileReaderExec);
        } else {
            settings = FileAnalyzer.analyze(settings, analyseExec);
        }
        SettingsStatus status = settings.getStatusOfSettings();
        if (status.getNumOfErrors() > 0) {
            throw new IllegalStateException(status.getErrorMessage(0));
        }
        final DataTableSpec tableSpec = settings.createDataTableSpec();
        if (tableSpec == null) {
            final SettingsStatus status2 = settings.getStatusOfSettings(true, null);
            if (status2.getNumOfErrors() > 0) {
                throw new IllegalStateException(status2.getErrorMessage(0));
            } else {
                throw new IllegalStateException("Unknown error during file analysis.");
            }
        }
        exec.setMessage("Buffering file");
        return new FileTable(tableSpec, settings, readExec);
    }

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
