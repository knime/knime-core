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
package org.knime.base.node.io.pmml.read;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.knime.base.node.io.pmml.write.PMMLWriterNodeModel;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.DataDictionaryContentHandler;
import org.knime.core.node.port.pmml.ExtractModelTypeHandler;
import org.knime.core.node.port.pmml.MiningSchemaContentHandler;
import org.knime.core.node.port.pmml.PMMLMasterContentHandler;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLReaderNodeModel extends GenericNodeModel {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLReaderNodeModel.class);

    private SettingsModelString m_file 
        = PMMLReaderNodeDialog.createFileChooserModel();
    
    private SettingsModelString m_portObjectClassName 
        = PMMLReaderNodeDialog.createPortObjectSelectionModel();
        

    private PMMLModelType m_type;
    
    private PMMLPortObjectSpec m_spec;
    
    /**
     * 
     */
    public PMMLReaderNodeModel() {
        super(new PortType[] {}, 
                new PortType[] {PMMLWriterNodeModel.PMML_PORTTYPE});
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        try {
            // read the data dictionary and the mining schema and create a 
            // PMMLPortObjectSpec
            m_spec = dataDictionaryToDataTableSpec();

            LOGGER.debug("model type: " + m_type.name());
            
            return new PortObjectSpec[] {m_spec};
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        }
    }

    private PMMLPortObjectSpec dataDictionaryToDataTableSpec() 
        throws ParserConfigurationException, SAXException {
        File f = new File(m_file.getStringValue());
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser = fac.newSAXParser();
        PMMLMasterContentHandler masterHandler = new PMMLMasterContentHandler();
        DataDictionaryContentHandler ddHandler
                = new DataDictionaryContentHandler();
        masterHandler.addContentHandler(DataDictionaryContentHandler.ID, 
                ddHandler);
        ExtractModelTypeHandler modelTypeHdl = new ExtractModelTypeHandler();
        masterHandler.addContentHandler(ExtractModelTypeHandler.ID, 
                modelTypeHdl);
        MiningSchemaContentHandler miningSchemaHdl 
            = new MiningSchemaContentHandler();
        masterHandler.addContentHandler(MiningSchemaContentHandler.ID, 
                miningSchemaHdl);
        try {
            parser.parse(f, masterHandler);
        } catch (Exception e) {
            LOGGER.error("Error parsing file" + m_file.getStringValue(), e);
        }
        m_type = modelTypeHdl.getModelType();
        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                ddHandler.getDataTableSpec());
        creator.setIgnoredColsNames(miningSchemaHdl.getIgnoredFields());
        creator.setLearningColsNames(miningSchemaHdl.getLearningFields());
        creator.setTargetColsNames(miningSchemaHdl.getTargetFields());
        m_spec = creator.createSpec();
        return m_spec;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, 
            final ExecutionContext exec)
            throws Exception {
        // TODO: this instantiation has to be done differently 
        // (without the necessity of a default contructor)
        // retrieve selected PortObject class -> instantiate and load it
        LOGGER.debug("class name: " + m_portObjectClassName.getStringValue());
        Class<? extends PMMLPortObject> clazz 
            = (Class<?extends PMMLPortObject>)Class.forName(
                    m_portObjectClassName.getStringValue());
        PMMLPortObject portObject = clazz.newInstance();
        m_spec = dataDictionaryToDataTableSpec();
        portObject.loadFrom(m_spec, new FileInputStream(
                new File(m_file.getStringValue())));
        return new PortObject[] {portObject};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_file.loadSettingsFrom(settings);
        m_portObjectClassName.loadSettingsFrom(settings);
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
        m_portObjectClassName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_file.validateSettings(settings);
        m_portObjectClassName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
    }
}
