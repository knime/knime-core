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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

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
import org.knime.core.node.port.pmml.XFilter;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLReaderNodeModel extends GenericNodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLReaderNodeModel.class);
    
    private SettingsModelString m_file =
            PMMLReaderNodeDialog.createFileChooserModel();

    private String m_portObjectClassName = null;

    private PMMLModelType m_type;
    
    private boolean m_hasNamespace;

    private PMMLPortObjectSpec m_spec;

    private String m_version;
    
    private static final Map<String, String>NS_MAP 
        = new HashMap<String, String>();
    
    static {
        NS_MAP.put("3.0", "http://www.dmg.org/PMML-3_0");
        NS_MAP.put("3.1", "http://www.dmg.org/PMML-3_1");
        NS_MAP.put("3.2", "http://www.dmg.org/PMML-3_2");
    }

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
            try {
                m_spec = dataDictionaryToDataTableSpec(
                        new File(m_file.getStringValue()));
                if (m_version.startsWith("3.")) {
                    validateSchema();
                } else {
                    throw new InvalidSettingsException(
                            "Only PMML versions 3.0, 3.1, 3.2 are supported.");
                }
            } catch (SAXException e) {
                LOGGER.error("PMML file is not valid", e);
                setWarningMessage(
                        "File seems to be not a vaild PMML file.");
                throw new InvalidSettingsException(e);
            } catch (IOException io) {
                throw new InvalidSettingsException(io);
            } catch (ParserConfigurationException pce) {
                throw new InvalidSettingsException(pce);
            } 
            LOGGER.debug("model type: " + m_type.name());
            return new PortObjectSpec[]{m_spec};
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
    
    private InputStream getSchemaInputStream(final String path) {
        ClassLoader loader = PMMLPortObject.class.getClassLoader();
        String packagePath =
                PMMLPortObject.class.getPackage().getName().replace('.', '/');
        return loader.getResourceAsStream(
                packagePath + path);
    }
    
    
    private void validateSchema() throws SAXException, IOException, 
        ParserConfigurationException {
        LOGGER.debug("Validating PMML file " + m_file.getStringValue() 
                + ". Version = " + m_version);
        SchemaFactory schemaFac =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        SAXParserFactory fac = SAXParserFactory.newInstance();
        fac.setNamespaceAware(true);
        
        Schema schema = null;
        if (m_version == null) {
            throw new SAXException(
                    "Input file is not a valid PMML file. "
                            + "Attribute \"version\" is missing");
        }
        String schemaLocation = PMMLPortObject.getLocalSchemaLocation(
                m_version);
        if (schemaLocation == null) {
            throw new SAXException(
                    "Version " + m_version + " is not supported!");
        }
        schema =
            schemaFac
                    .newSchema(new SAXSource(new InputSource(
                            getSchemaInputStream(schemaLocation))));
        File f = new File(m_file.getStringValue());
        FileInputStream fis = new FileInputStream(f);
        XFilter filter = new XFilter(m_version);
        filter.setParent(fac.newSAXParser().getXMLReader());
        // use validator here
        Validator validator = schema.newValidator();
        // register error handler
        validator.setErrorHandler(new LoggingErrorHandler());
        validator.validate(new SAXSource(filter, new InputSource(fis)));

    }

    private PMMLPortObjectSpec dataDictionaryToDataTableSpec(final File file)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser = fac.newSAXParser();
        PMMLMasterContentHandler masterHandler = new PMMLMasterContentHandler();
        DataDictionaryContentHandler ddHandler =
                new DataDictionaryContentHandler();
        masterHandler.addContentHandler(DataDictionaryContentHandler.ID,
                ddHandler);
        ExtractModelTypeHandler modelTypeHdl = new ExtractModelTypeHandler();
        masterHandler.addContentHandler(ExtractModelTypeHandler.ID,
                modelTypeHdl);
        MiningSchemaContentHandler miningSchemaHdl =
                new MiningSchemaContentHandler();
        masterHandler.addContentHandler(MiningSchemaContentHandler.ID,
                miningSchemaHdl);
        parser.parse(file, masterHandler);
        m_version = masterHandler.getVersion();
        m_type = modelTypeHdl.getModelType();
        m_hasNamespace = modelTypeHdl.hasNamespace();
        PMMLPortObjectSpecCreator creator =
            new PMMLPortObjectSpecCreator(ddHandler.getDataTableSpec());
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
            final ExecutionContext exec) throws Exception {
        // retrieve selected PortObject class -> instantiate and load it
        LOGGER.debug("class name: " + m_portObjectClassName);
        Class<? extends PMMLPortObject> clazz =
                (Class<? extends PMMLPortObject>)Class
                        .forName(m_portObjectClassName);
        PMMLPortObject portObject = clazz.newInstance();
        // TODO: create the PMMLPortObjectSpec while parsing the file 
        // now the dataDictionaryToDataTableSpec parses the whole file
        // TODO: check if namespace is available in configure
        if (!m_hasNamespace) {
            LOGGER.debug("adding namespace");
            File f = File.createTempFile("ns_added", ".xml");
            f.deleteOnExit();
            BufferedReader reader = new BufferedReader(
                    new FileReader(m_file.getStringValue()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            String line = reader.readLine();
            boolean tagOpen = false; 
            while (line != null) {
                if (line.startsWith("<PMML")) {
                    tagOpen = true;
                }
                if (tagOpen && line.endsWith(">")) {
                    tagOpen = false;
                    // add namespace declaration
                    line = "<PMML version=\"" + m_version 
                    + "\" xmlns=\"" + NS_MAP.get(m_version) + "\" xmlns:xsi=" 
                    + "\"http://www.w3.org/2001/XMLSchema-instance\">";
                    LOGGER.debug(line);
                }
                if (!tagOpen) {
                    writer.write(line + "\n");
                }
                line = reader.readLine();
            }
            reader.close();
            writer.close();
            m_spec = dataDictionaryToDataTableSpec(f);
            portObject.loadFrom(m_spec, new FileInputStream(f), m_version);
            f.delete();
        } else {
            m_spec = dataDictionaryToDataTableSpec(
                    new File(m_file.getStringValue()));
            portObject.loadFrom(m_spec, 
                    new FileInputStream(m_file.getStringValue()), m_version);
        }
        return new PortObject[]{portObject};
    }
    
    


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_file.loadSettingsFrom(settings);
        m_portObjectClassName = settings.getString(
                PMMLReaderNodeDialog.PORT_OBJECT_KEY);
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
        settings.addString(PMMLReaderNodeDialog.PORT_OBJECT_KEY,
                m_portObjectClassName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_file.validateSettings(settings);
        String portObjClassName = settings.getString(
                PMMLReaderNodeDialog.PORT_OBJECT_KEY);
        if (portObjClassName == null || portObjClassName.isEmpty()) {
            throw new InvalidSettingsException(
                    "Model not supported!");
        }
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
    
    /**
     * Error handler that throws an exception if an error occurs.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public static class LoggingErrorHandler implements ErrorHandler {
        
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public void error(final SAXParseException saxe) 
            throws SAXException {
            LOGGER.error("Invalid PMML file: ", saxe);
            throw saxe;
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public void fatalError(final SAXParseException saxe) 
            throws SAXException {
            LOGGER.fatal("Invalid PMML file: ", saxe);
            throw saxe;
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public void warning(final SAXParseException saxe) 
            throws SAXException {
            LOGGER.warn("Invalid PMML file: ", saxe);
        }

    }
}
