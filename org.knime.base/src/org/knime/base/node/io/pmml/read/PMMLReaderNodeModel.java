/* This source code, its documentation and all appendant files
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
