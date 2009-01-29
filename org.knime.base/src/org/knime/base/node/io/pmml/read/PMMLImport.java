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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
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

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLImport {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLImport.class);
    
    private static final Map<String, String>NS_MAP 
    = new HashMap<String, String>();
    
    private static final 
    Map<String, Class<? extends PMMLPortObject>>REGISTRY 
        = new HashMap<String, Class<? extends PMMLPortObject>>();
    
    static {
        NS_MAP.put("3.0", "http://www.dmg.org/PMML-3_0");
        NS_MAP.put("3.1", "http://www.dmg.org/PMML-3_1");
        NS_MAP.put("3.2", "http://www.dmg.org/PMML-3_2");

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
                    .getConfigurationElementsFor("org.knime.base.pmmlports")) {
            String modelType = element.getAttribute("modeltype");
            try {
                Class<? extends PMMLPortObject> clazz 
                    = (Class<?extends PMMLPortObject>)Class.forName(element
                            .getAttribute("PMMLPortObject"));
                if (REGISTRY.get(modelType) == null) {
                    // add class
                    REGISTRY.put(modelType, clazz);
                } // else already registered -> first come first serve
            } catch (InvalidRegistryObjectException e) {
                throw new IllegalArgumentException(e);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            for (String key : REGISTRY.keySet()) {
                LOGGER.debug("model type: " + key);
                    LOGGER.debug("port object: " 
                            + REGISTRY.get(key).getSimpleName());
                
            }
        }
    }
    
    private File m_file;
    private ErrorHandler m_errorHandler = new LoggingErrorHandler();
    private PMMLModelType m_modelType = PMMLModelType.None;
    private String m_version;
    private boolean m_hasNamespace;
    
    private PMMLPortObjectSpec m_portObjectSpec;
    private PMMLPortObject m_portObject;
    
    private PMMLImport() {
        
    }
    
    /**
     * Reads the passed file, checks the PMML version, the PMML model type,
     * and namespace and also validates the file against the schema, then 
     * creates the {@link PMMLPortObjectSpec} and {@link PMMLPortObject} from
     * the content of the file.
     * 
     * @param file containing the PMML model
     * @param errorHandler error handler used during parsing
     * @throws SAXException if something goes wrong (wrong version, 
     *  unsupported model type or invalid file)
     */
    public PMMLImport(final File file, final ErrorHandler errorHandler) 
        throws SAXException {
        if (file == null) {
            throw new IllegalArgumentException(
                    "File must not be null!");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException(
                    "File " + file.getName() + " does not exists.");
        }
        m_file = file;
        m_errorHandler = errorHandler;
        // initial file parsing: 
        // extract model type, version, and if namespace is available
        preCollectInformation();
        initializePortObjectClass();
        validate();
        if (!m_hasNamespace) {
            // add namespace in order to have the parser set the default values
            try {
                m_file = addNamespace(m_file);
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
        m_portObjectSpec = parseSpec();
        m_portObject = parseModel(m_portObjectSpec);
        if (!m_hasNamespace) {
            // if the file had no namespace a new file with namespace added was
            // created in temp dir -> now we can delete it;
            m_file.delete();
        }
    
    }
    
    /**
     * @see PMMLImport#PMMLImport(File, ErrorHandler) with default error 
     *  handler: {@link LoggingErrorHandler}.
     * 
     * @param file containing the PMML model
     * @throws SAXException if something goes wrong
     */
    public PMMLImport(final File file) throws SAXException {
        this(file, new LoggingErrorHandler());
    }
    
    
    /**
     * 
     * @param file the file containing the PMML model
     * @return true if the type of model is supported by the current platform
     *  configuration
     * @throws SAXException if something goes wrong
     */
    public static boolean isModelSupported(final File file) 
        throws SAXException {
        PMMLImport importer = new PMMLImport();
        importer.m_file = file;
        importer.preCollectInformation();
        return REGISTRY.get(importer.m_modelType.name()) != null;
    }
    
    /**
     * 
     * @return the parsed port object spec (data dictionary and mining schema)
     */
    public PMMLPortObjectSpec getPortObjectSpec() {
        return m_portObjectSpec;
    }
    
    /**
     * 
     * @return the parsed PMML model
     */
    public PMMLPortObject getPortObject() {
        return m_portObject;
    }
    
    private void preCollectInformation() throws SAXException {
        try {
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
        parser.parse(m_file, masterHandler);
        m_version = masterHandler.getVersion();
        m_modelType = modelTypeHdl.getModelType();
        m_hasNamespace = modelTypeHdl.hasNamespace();
        if (!m_version.startsWith("3")) {
            throw new SAXException(
            "PMML model seems to be of a not supported version. " 
                    + "Only PMML versions 3.0, 3.1, 3.2 are supported. "
                    + "Found " + m_version);
        }
        } catch (IOException io) {
            throw new SAXException(io);
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }
    
    private void initializePortObjectClass() {
        Class<? extends PMMLPortObject> portObject = REGISTRY.get(
                m_modelType.name());
        if (portObject == null) {
            throw new IllegalArgumentException("Model type: " + m_modelType
                    + " not supported yet.");
        } else {
            try {
            Class<? extends PMMLPortObject> clazz =
                    (Class<? extends PMMLPortObject>)Class
                            .forName(portObject.getName());
            m_portObject = clazz.newInstance();
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException(
                        "PortObject for model " + m_modelType 
                        + " could not be initialized!");
            } catch (IllegalAccessException iae) {
                throw new IllegalArgumentException(
                        "PortObject for model " + m_modelType 
                        + " could not be initialized!");                
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(
                        "PortObject for model " + m_modelType 
                        + " could not be initialized!");
            }
        }
    }

    private void validate() throws SAXException {
        try {
        LOGGER.debug("Validating PMML file " + m_file.getName()
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
        FileInputStream fis = new FileInputStream(m_file);
        XFilter filter = new XFilter(m_version);
        filter.setParent(fac.newSAXParser().getXMLReader());
        // use validator here
        Validator validator = schema.newValidator();
        // register error handler
        validator.setErrorHandler(m_errorHandler);
        validator.validate(new SAXSource(filter, new InputSource(fis)));
        } catch (IOException io) {
            throw new SAXException(io);
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }
    
    private PMMLPortObjectSpec parseSpec() throws SAXException {
        try {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser = fac.newSAXParser();
        PMMLMasterContentHandler masterHandler = new PMMLMasterContentHandler();
        DataDictionaryContentHandler ddHandler =
                new DataDictionaryContentHandler();
        masterHandler.addContentHandler(DataDictionaryContentHandler.ID,
                ddHandler);
        MiningSchemaContentHandler miningSchemaHdl =
                new MiningSchemaContentHandler();
        masterHandler.addContentHandler(MiningSchemaContentHandler.ID,
                miningSchemaHdl);
        parser.parse(m_file, masterHandler);
        PMMLPortObjectSpecCreator creator =
            new PMMLPortObjectSpecCreator(ddHandler.getDataTableSpec());
        creator.setIgnoredColsNames(miningSchemaHdl.getIgnoredFields());
        creator.setLearningColsNames(miningSchemaHdl.getLearningFields());
        creator.setTargetColsNames(miningSchemaHdl.getTargetFields());
        return creator.createSpec();
        } catch (IOException e) {
            throw new SAXException(e);
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        } catch (InvalidSettingsException ise) {
            throw new SAXException(ise);
        }
    }
    
    private File addNamespace(final File file) throws IOException {
        LOGGER.debug("adding namespace");
        File f = File.createTempFile("ns_added", ".xml");
        f.deleteOnExit();
        BufferedReader reader = new BufferedReader(
                new FileReader(file));
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
        return f;
    }
    
    private PMMLPortObject parseModel(final PMMLPortObjectSpec portObjectSpec) 
        throws SAXException {
        try {
            m_portObject.loadFrom(portObjectSpec, new FileInputStream(m_file), 
                    m_version);
        } catch (FileNotFoundException e) {
            throw new SAXException(e);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException(e);
        }
        return m_portObject;
    }
    
    private InputStream getSchemaInputStream(final String path) {
        ClassLoader loader = PMMLPortObject.class.getClassLoader();
        String packagePath =
                PMMLPortObject.class.getPackage().getName().replace('.', '/');
        return loader.getResourceAsStream(
                packagePath + path);
    }
    
}
