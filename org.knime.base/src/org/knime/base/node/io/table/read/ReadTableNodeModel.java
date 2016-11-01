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
 * -------------------------------------------------------------------
 *
 * History
 *   May 19, 2006 (wiswedel): created
 */
package org.knime.base.node.io.table.read;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;


/**
 * NodeMode for table that reads the file as written from the
 * Write table node.
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ReadTableNodeModel.class);

    /** Identifier for the node settings object. */
    static final String CFG_FILENAME = "filename";

    /** The extension of the files to store, \".knime\". */
    public static final String PREFERRED_FILE_EXTENSION = ".table";

    private final SettingsModelString m_fileName = new SettingsModelString(CFG_FILENAME, null);
    private final SettingsModelBoolean m_limitCheckerModel = createLimitCheckerModel();
    private final SettingsModelInteger m_limitSpinnerModel = createLimitSpinnerModel(m_limitCheckerModel);

    /**
     * Creates new model with no inputs, one output.
     */
    public ReadTableNodeModel() {
        super(0, 1);
    }

    /** @param limitCheckerModel to register for enable/disable.
     * @return */
    static final SettingsModelInteger createLimitSpinnerModel(final SettingsModelBoolean limitCheckerModel) {
        final SettingsModelInteger result = new SettingsModelInteger("limitRowsCount", 100000);
        limitCheckerModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                result.setEnabled(limitCheckerModel.getBooleanValue());
            }
        });
        result.setEnabled(limitCheckerModel.getBooleanValue());
        return result;
    }

    /** @return */
    static final SettingsModelBoolean createLimitCheckerModel() {
        return new SettingsModelBoolean("limitRows", false);
    }

    /**
     * Called by the node factory if the node is instantiated due to a file
     * drop.
     *
     * @param context the node creation context
     */
    public ReadTableNodeModel(final NodeCreationContext context) {
        this();
        m_fileName.setStringValue(context.getUrl().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_fileName.getStringValue() != null) {
            m_fileName.saveSettingsTo(settings);
            m_limitCheckerModel.saveSettingsTo(settings);
            m_limitSpinnerModel.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
        try {
            m_limitCheckerModel.loadSettingsFrom(settings);
            m_limitSpinnerModel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_limitCheckerModel.setBooleanValue(false);
            m_limitSpinnerModel.setIntValue(100000);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
        final ExecutionContext exec) throws Exception {
        exec.setMessage("Extracting temporary table");
        ContainerTable table = extractTable(exec.createSubExecutionContext(0.4));
        exec.setMessage("Reading into final format");
        BufferedDataTableRowOutput c = new BufferedDataTableRowOutput(
            exec.createDataContainer(table.getDataTableSpec(), true));
        execute(table, c, exec.createSubExecutionContext(0.6));
        return new BufferedDataTable[]{c.getDataTable()};
    }

    void execute(final ContainerTable table, final RowOutput output, final ExecutionContext exec) throws Exception {
        long limit = m_limitCheckerModel.getBooleanValue() ? m_limitSpinnerModel.getIntValue() : Long.MAX_VALUE;
        final long rowCount = Math.min(limit, table.size());
        long row = 0L;
        for (RowIterator it = table.iterator(); it.hasNext() && row < limit; row++) {
            final DataRow next = it.next();
            final long rowFinal = row;
            exec.setProgress(row / (double)rowCount,
                () -> String.format("Row %,d/%,d (%s)", rowFinal + 1, rowCount, next.getKey()));
            exec.checkCanceled();
            output.push(next);
        }
        output.close();
    }

    /**
     * @param exec
     * @return
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private ContainerTable extractTable(final ExecutionContext exec) throws IOException, InvalidSettingsException {
        try (InputStream inputStream = openInputStream()) {
            InputStream in = inputStream; // possibly re-assigned
            long sizeInBytes;
            String loc = m_fileName.getStringValue();
            try {
                try {
                    URL url = new URL(loc);
                    sizeInBytes = FileUtil.getFileFromURL(url).length();
                } catch (MalformedURLException mue) {
                    File file = new File(loc);
                    if (file.exists()) {
                        sizeInBytes = file.length();
                    } else {
                        sizeInBytes = 0L;
                    }
                }
            } catch (Exception e) {
                // ignore, no progress
                sizeInBytes = 0L;
            }
            final long sizeFinal = sizeInBytes;
            if (sizeFinal > 0) {
                CountingInputStream bcs = new CountingInputStream(in) {
                    @Override
                    protected synchronized void afterRead(final int n) {
                        super.afterRead(n);
                        final long byteCount = getByteCount();
                        exec.setProgress((double)byteCount / sizeFinal,
                            () -> FileUtils.byteCountToDisplaySize(byteCount));
                        try {
                            exec.checkCanceled();
                        } catch (CanceledExecutionException e) {
                            throw new RuntimeException("canceled");
                        }
                    }
                };
                in = bcs;
            }
            return DataContainer.readFromStream(in);
        } finally {
            exec.setProgress(1.0);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String warning = CheckUtils.checkSourceFile(m_fileName.getStringValue());
        if (warning != null) {
            setWarningMessage(warning);
        }
        InputStream in = null;
        try {
            in = openInputStream();
            DataTableSpec spec = peekDataTableSpec(in);
            if (spec == null) { // if written with 1.3.x and before
                in.close();
                in = openInputStream();
                LOGGER.debug("Table spec is not first entry in input file, "
                        + "need to deflate entire file");
                DataTable outTable = DataContainer.readFromStream(in);
                spec = outTable.getDataTableSpec();
            }
            return new DataTableSpec[]{spec};
        } catch (IOException ioe) {
            String message = ioe.getMessage();
            if (message == null) {
                message = "Unable to read spec from file, "
                    + "no detailed message available.";
            }
            throw new InvalidSettingsException(message);

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    /** Opens the zip file and checks whether the first entry is the spec. If
     * so, the spec is parsed and returned. Otherwise null is returned.
     *
     * <p> This method is used to fix bug #1141: Dialog closes very slowly.
     * @param in Input stream
     * @return The spec or null (null will be returned when the file was
     * written with a version prior 2.0)
     * @throws IOException If that fails for any reason.
     */
    private DataTableSpec peekDataTableSpec(final InputStream in)
        throws IOException {
        // must not use ZipFile here as it is known to have memory problems
        // on large files, see e.g.
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5077277
        try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in))) {
            ZipEntry entry = zipIn.getNextEntry();
            // hardcoded constants here as we do not want additional
            // functionality to DataContainer ... at least not yet.
            if ("spec.xml".equals(entry != null ? entry.getName() : "")) {
                NodeSettingsRO settings = NodeSettings.loadFromXML(new NonClosableInputStream.Zip(zipIn));
                try {
                    NodeSettingsRO specSettings = settings.getNodeSettings("table.spec");
                    return DataTableSpec.load(specSettings);
                } catch (InvalidSettingsException ise) {
                    IOException ioe = new IOException("Unable to read spec from file");
                    ioe.initCause(ise);
                    throw ioe;
                }
            } else {
                return null;
            }
        }
    }

    private InputStream openInputStream()
        throws IOException, InvalidSettingsException {
        String loc = m_fileName.getStringValue();
        return FileUtil.openInputStream(loc);
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[] {OutputPortRole.DISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs,
                final ExecutionContext exec) throws Exception {
                exec.setMessage("Extract temporary table");
                ContainerTable table = extractTable(exec.createSubExecutionContext(0.4));
                exec.setMessage("Streaming Output");
                RowOutput output = (RowOutput)outputs[0];
                execute(table, output, exec.createSubExecutionContext(0.6));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not internals to save
    }
}
