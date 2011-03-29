/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.12.2010 (hofer): created
 */
package org.knime.base.node.io.xml.reader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.NamespaceContext;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.io.LimitedXPathMatcher;
import org.knime.core.data.xml.io.XMLCellReader;
import org.knime.core.data.xml.io.XMLCellReaderFactory;
import org.knime.core.data.xml.util.DefaultNamespaceContext;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model for the XML Reader node.
 *
 * @author Heiko Hofer
 */
public class XMLReaderNodeModel extends NodeModel {
    private final XMLReaderNodeSettings m_settings;

    /**
     * Creates a new model with no input port and one output port.
     */
    public XMLReaderNodeModel() {
        super(0, 1);
        m_settings = new XMLReaderNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings.getFileURL() == null) {
            throw new InvalidSettingsException("No input file selected");
        }
        String loc = m_settings.getFileURL();
        if (loc == null || loc.length() == 0) {
            throw new InvalidSettingsException("No location provided");
        }

        if (loc.startsWith("file:/") || !loc.matches("^[a-zA-Z]+:/.*")) {
            File file = null;
            if (loc.startsWith("file:/")) {
                URL url;
                try {
                    url = new URL(loc);
                } catch (MalformedURLException ex) {
                    throw new InvalidSettingsException("Invalid URL: " + loc,
                            ex);
                }
                try {
                    // can handle file:///c:/Documents%20and%20Settings/...
                    file = new File(url.toURI());
                } catch (Exception e) {
                    // can handle file:///c:/Documents and Settings/...
                    file = new File(url.getPath());
                }
            } else {
                file = new File(loc);
            }

            if (!file.exists()) {
                throw new InvalidSettingsException("File '"
                        + file.getAbsolutePath() + "' does not exist");
            }
            if (!file.isFile()) {
                throw new InvalidSettingsException("'" + file.getAbsolutePath()
                        + "' is a directory");
            }
        }

        if (m_settings.getUseXPathFilter()) {
            // Check for empty prefix
            String[] prefix = m_settings.getNsPrefixes();
            String[] namespaces = m_settings.getNamespaces();
            for (int i = 0; i < prefix.length; i++) {
                if (prefix[i].isEmpty()) {
                    throw new InvalidSettingsException("An empty prefix for "
                            + "namespaces are not allowe in XPath. "
                            + "Please define a valid prefix.");
                }
            }
            // DefaultNamespaceContext does some sanity checking
            NamespaceContext nsContext =
                new DefaultNamespaceContext(prefix, namespaces);
            // LimitedXPathMatcher DefaultNamespaceContext does some
            // sanity checking
            new LimitedXPathMatcher(m_settings.getXpath(), nsContext);
        }

        DataTableSpec spec = createOutSpec();
        return new DataTableSpec[]{spec};
    }

    private DataTableSpec createOutSpec() {
        DataColumnSpecCreator colSpecCreator =
                new DataColumnSpecCreator("XML", XMLCell.TYPE);
        DataTableSpec spec = new DataTableSpec(colSpecCreator.createSpec());
        return spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        InputStream in = null;
        try {
            in = openInputStream();
            XMLCellReader reader = null;
            if (m_settings.getUseXPathFilter()) {
                String[] prefix = m_settings.getNsPrefixes();
                String[] namespaces = m_settings.getNamespaces();
                NamespaceContext nsContext =
                    new DefaultNamespaceContext(prefix, namespaces);
                LimitedXPathMatcher xpathMatcher =
                    new LimitedXPathMatcher(m_settings.getXpath(), nsContext);
                reader = XMLCellReaderFactory.createXPathXMLCellReader(in,
                        xpathMatcher);
            } else {
                reader = XMLCellReaderFactory.createXMLCellReader(in);
            }
            int rowCount = 0;
            DataContainer cont = exec.createDataContainer(createOutSpec());
            DataCell cell = reader.readXML();
            while(null != cell) {
                // TODO Check noding conventions for row ID naming
                DataRow row = new DefaultRow("Row " + rowCount, cell);
                cont.addRowToTable(row);
                rowCount++;
                cell = reader.readXML();
            }
            cont.close();
            DataTable table = cont.getTable();
            //DataTable table = parseXML(in, exec);

            BufferedDataTable out = exec.createBufferedDataTable(table, exec);
            return new BufferedDataTable[]{out};
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

    private InputStream openInputStream() throws IOException,
            InvalidSettingsException {
        String loc = m_settings.getFileURL();
        if (loc == null || loc.length() == 0) {
            throw new InvalidSettingsException("No location provided");
        }
        if (loc.matches("^[a-zA-Z]+:/.*")) { // URL style
            URL url;
            try {
                url = new URL(loc);
            } catch (MalformedURLException ex) {
                throw new InvalidSettingsException("Invalid URL: " + loc, ex);
            }
            return url.openStream();
        } else {
            File file = new File(loc);
            if (!file.exists()) {
                throw new InvalidSettingsException("No such file: " + loc);
            }
            return new BufferedInputStream(new FileInputStream(file));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new XMLReaderNodeSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }
}
