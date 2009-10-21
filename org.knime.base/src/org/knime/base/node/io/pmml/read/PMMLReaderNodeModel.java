/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.pmml.read;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLReaderNodeModel extends NodeModel {
    
    private SettingsModelString m_file =
            PMMLReaderNodeDialog.createFileChooserModel();

    private PMMLImport m_importer;
    
    /**
     * 
     */
    public PMMLReaderNodeModel() {
        super(new PortType[]{}, new PortType[]{
                new PortType(PMMLPortObject.class)});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
            // read the data dictionary and the mining schema and create a
            // PMMLPortObjectSpec
            if (m_file.getStringValue() == null 
                    || m_file.getStringValue().isEmpty()) {
                throw new InvalidSettingsException(
                        "Please select a PMML file!");
            }
            File file = new File(m_file.getStringValue());
            if (!file.exists() || !file.canRead()) {
                throw new InvalidSettingsException("Can't access PMML file \""
                        + file + "\".");
            }
            try {
                m_importer = new PMMLImport(file);
            } catch (SAXException e) {
                setWarningMessage(
                    "File \"" + file + "\" is not a valid PMML file:\n" 
                        + e.getMessage());
                throw new InvalidSettingsException(e);
            }  
            return new PortObjectSpec[]{m_importer.getPortObjectSpec()};
    }

    /*
    private void validate() throws Exception {
        InputStream xsltStream = getSchemaInputStream(
                "/schemata/pmml.xslt");
        TransformerFactory transFac = TransformerFactory.newInstance();
        StreamSource ss = new StreamSource(xsltStream);
        
        Transformer transformer = transFac.newTransformer(
                ss);
//        XFilter filter = new XFilter();
//        filter.setParent(parser.getXMLReader());
//        InputSource fileSource = new InputSource(
//                new FileInputStream(new File(m_file.getStringValue())));
//        SAXSource saxSrc = new SAXSource(filter, fileSource);
//        TransformerFactory.newInstance().newTransformer().transform(
//          saxSrc, result);
        
        StreamResult result = new StreamResult(System.out);
//
      SAXParserFactory saxFac = SAXParserFactory.newInstance();
      saxFac.setValidating(false);
      saxFac.setNamespaceAware(true);
      SAXParser parser = saxFac.newSAXParser();
        
        SAXSource saxSrc = new SAXSource(parser.getXMLReader(),
                new InputSource(new FileInputStream(
                new File(m_file.getStringValue()))));
        transformer.transform(saxSrc, result);
        
    }
    */
    
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        // retrieve selected PortObject class -> instantiate and load it
        if (m_importer == null) {
            m_importer = new PMMLImport(new File(m_file.getStringValue()));
        }
        return new PortObject[]{m_importer.getPortObject()};
    }
    
    


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_file.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_file.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_file.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
}
