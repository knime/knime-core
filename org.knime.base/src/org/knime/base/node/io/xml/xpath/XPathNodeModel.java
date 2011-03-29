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
 *   17.12.2010 (hofer): created
 */
package org.knime.base.node.io.xml.xpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.knime.base.node.io.xml.xpath.XPathNodeSettings.XPathOutput;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is the model for the XPath node. It takes an XML column from the
 * input table and performs a XPath query on every cell.
 *
 * @author Heiko Hofer
 */
public class XPathNodeModel extends NodeModel {

    private XPathNodeSettings m_settings;
    private XPathExpression m_xpathExpr;

    /**
     * Creates a new model with no input port and one output port.
     */
    public XPathNodeModel() {
        super(1, 1);
        m_settings = new XPathNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // validate settings for the XML column
        if (null == m_settings.getInputColumn()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(XMLValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_settings.setInputColumn(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_settings.setInputColumn(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
                // TODO point to node for converting Data Table to XML
                throw new InvalidSettingsException("No XML "
                        + "column in input table.");
            }
        }
        // validate new column name
        if (null == m_settings.getNewColumn()) {
            if (null != m_settings.getInputColumn()) {
                // auto-configure
                String newName =
                        DataTableSpec.getUniqueColumnName(inSpecs[0],
                                m_settings.getInputColumn() + " (xpath)");
                m_settings.setNewColumn(newName);
            } else {
                m_settings.setNewColumn("XPath query result");
            }
        }
        if (null == m_settings.getReturnType()) {
            throw new InvalidSettingsException("No return type defined.");
        }
        m_xpathExpr = null;
        if (null != m_settings.getXpathQuery()) {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            xpath.setNamespaceContext(new XPathNamespaceContext(
                    m_settings.getNsPrefixes(),
                    m_settings.getNamespaces()));
            try {
                m_xpathExpr = xpath.compile(m_settings.getXpathQuery());
            } catch (XPathExpressionException e) {
                throw new InvalidSettingsException(
                        "XPath query cannot be parsed.", e);
            }
        } else {
            throw new InvalidSettingsException("No XPath query defined.");
        }

        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        ColumnRearranger rearranger = createColumnRearranger(inSpec);
        BufferedDataTable outTable =
                exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{outTable};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
            throws InvalidSettingsException {
        // check user settings against input spec here
        String xmlColumn = m_settings.getInputColumn();
        final int xmlIndex = spec.findColumnIndex(xmlColumn);
        if (xmlIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + xmlColumn);
        }
        String newName = m_settings.getNewColumn();
        if ((spec.containsName(newName) && !newName.equals(xmlColumn))
                || (spec.containsName(newName) && newName.equals(xmlColumn)
                        && !m_settings.getRemoveInputColumn())) {
            throw new InvalidSettingsException("Cannot create column "
                    + newName + "since it is already in the input.");
        }

        ColumnRearranger colRearranger = new ColumnRearranger(spec);
        DataType newCellType = null;
        final XPathOutput returnType = m_settings.getReturnType();
        if (returnType.equals(XPathOutput.Boolean)) {
            newCellType = BooleanCell.TYPE;
        } else if (returnType.equals(XPathOutput.Number)) {
            newCellType = DoubleCell.TYPE;
        } else if (returnType.equals(XPathOutput.String)) {
            newCellType = StringCell.TYPE;
        } else if (returnType.equals(XPathOutput.Node)) {
            newCellType = XMLCell.TYPE;
        } else if (returnType.equals(XPathOutput.NodeSet)) {
            newCellType = DataType.getType(ListCell.class, XMLCell.TYPE);
        }

        DataColumnSpecCreator appendSpec =
                new DataColumnSpecCreator(newName, newCellType);
        colRearranger.append(new SingleCellFactory(appendSpec.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell xmlCell = row.getCell(xmlIndex);
                if (xmlCell.isMissing()) {
                    return DataType.getMissingCell();
                }
                XMLValue xmlValue = (XMLValue)xmlCell;
                DataCell newCell = null;
                try {
                    if (returnType.equals(XPathOutput.Boolean)) {
                        Object result = m_xpathExpr.evaluate(
                                xmlValue.getDocument(), XPathConstants.BOOLEAN);
                        Boolean value = (Boolean) result;
                        if (value.booleanValue()) {
                            newCell = BooleanCell.TRUE;
                        } else {
                            newCell = BooleanCell.FALSE;
                        }
                    } else if (returnType.equals(XPathOutput.Number)) {
                        Object result = m_xpathExpr.evaluate(
                                xmlValue.getDocument(), XPathConstants.NUMBER);
                        Double value = (Double) result;
                        newCell = new DoubleCell(value.doubleValue());
                    } else if (returnType.equals(XPathOutput.String)) {
                        Object result = m_xpathExpr.evaluate(
                                xmlValue.getDocument(), XPathConstants.STRING);
                        String value = (String) result;
                        newCell = new StringCell(value);
                    } else if (returnType.equals(XPathOutput.Node)) {
                        Object result = m_xpathExpr.evaluate(
                                xmlValue.getDocument(), XPathConstants.NODE);
                        Node value = (Node) result;
                        DocumentBuilderFactory domFactory =
                            DocumentBuilderFactory.newInstance();
                        domFactory.setNamespaceAware(true);
                        DocumentBuilder docBuilder = domFactory
                                                      .newDocumentBuilder();
                        Document doc = docBuilder.newDocument();
                        Node node = doc.importNode(value, true);
                        doc.appendChild(node);
                        newCell = XMLCellFactory.create(doc);
                    } else if (returnType.equals(XPathOutput.NodeSet)) {
                        Object result = m_xpathExpr.evaluate(
                                xmlValue.getDocument(), XPathConstants.NODESET);

                        NodeList nodes = (NodeList) result;
                        List<DataCell> cells = new ArrayList<DataCell>();
                        DocumentBuilderFactory domFactory =
                            DocumentBuilderFactory.newInstance();
                        domFactory.setNamespaceAware(true);
                        DocumentBuilder docBuilder = domFactory
                                                        .newDocumentBuilder();
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Node value = nodes.item(i);
                            Document doc = docBuilder.newDocument();
                            Node node = doc.importNode(value, true);
                            doc.appendChild(node);
                            cells.add(XMLCellFactory.create(doc));
                        }
                        newCell = CollectionCellFactory.createListCell(cells);
                    }
                } catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
                return newCell;
            }
        });
        if (m_settings.getRemoveInputColumn()) {
            colRearranger.remove(xmlIndex);
        }
        return colRearranger;
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
        new XPathNodeSettings().loadSettingsModel(settings);
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

    private static class XPathNamespaceContext implements NamespaceContext {
        private Map<String, String> m_namespaces;

        public XPathNamespaceContext(final String[] prefixes,
                final String[] namespaces) {
            m_namespaces = new HashMap<String, String>();
            for (int i = 0; i < prefixes.length; i++) {
                if (prefixes[i].isEmpty()) {
                    throw new IllegalArgumentException("There are empty "
                            + "namespace prefixes. Please provide a "
                            + "prefix for every namespace.");
                }
                m_namespaces.put(prefixes[i], namespaces[i]);
            }
        }

        @Override
        public String getNamespaceURI(final String prefix) {
            if (prefix == null) throw new NullPointerException("Null prefix");
            if ("xml".equals(prefix)) {
                return XMLConstants.XML_NS_URI;
            } else if (m_namespaces.containsKey(prefix)) {
                return m_namespaces.get(prefix);
            } else {
                return XMLConstants.NULL_NS_URI;
            }
        }


        @Override
        public String getPrefix(final String uri) {
            // This method isn't necessary for XPath processing.
            throw new UnsupportedOperationException();
        }


        @Override
        public Iterator getPrefixes(final String uri) {
            // This method isn't necessary for XPath processing.
            throw new UnsupportedOperationException();
        }

    }
}
