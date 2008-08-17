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
package org.knime.base.util.pmml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObject implements PortObject {
    
    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";
    /** Constant for DataDictionary. */
    protected static final String DATA_DICT = "DataDictionary";
    /** Constant for DataField. */
    protected static final String DATA_FIELD = "DataField";
    /** Constant for Value. */
    protected static final String VALUE = "Value";
    
    private TransformerHandler m_handler;
    private FileOutputStream m_fos;
    private File m_result;
    
    private DataTableSpec m_spec;
    
    private PMMLModelType m_modelType;
    
    private PMMLMasterContentHandler m_masterHandler;
    
    private static PortObjectSerializer<? extends PMMLPortObject>serializer;
    
    
    /**
     * Static serializer as demanded from {@link PortObject} framework.
     * @return serializer for PMML (reads and writes PMML files)
     */
    public static PortObjectSerializer<? extends PMMLPortObject> 
        getPortObjectSerializer() {
            if (serializer == null) {
                serializer = new PMMLPortObjectSerializer<PMMLPortObject>();
            }
        return serializer;
    }
    
    /**
     * Reads the data dictionary directly from the PMML file and converts it 
     * into a {@link DataTableSpec}. 
     * 
     * @param f the PMML file
     * @return the referring data table spec for the data dictionary
     * @throws ParserConfigurationException if the parser cannot be instantiated
     * @throws SAXException if something goes wrong during parsing
     * @throws IOException if the file cannot be found, or opened
     */
    public static DataTableSpec dataDictionaryToDataTableSpec(final File f)
            throws ParserConfigurationException, SAXException, IOException {
                SAXParserFactory fac = SAXParserFactory.newInstance();
                SAXParser parser = fac.newSAXParser();
                PMMLMasterContentHandler masterHandler 
                    = new PMMLMasterContentHandler();
                DataDictionaryContentHandler ddHandler =
                        new DataDictionaryContentHandler();
                masterHandler.addContentHandler(DataDictionaryContentHandler.ID,
                        ddHandler);
                ExtractModelTypeHandler modelTypeHdl 
                    = new ExtractModelTypeHandler();
                masterHandler.addContentHandler(ExtractModelTypeHandler.ID,
                        modelTypeHdl);
                parser.parse(f, masterHandler);
                return ddHandler.getDataTableSpec();
            }

    /**
     * Type of this port.
     */
    public static final PortType TYPE = new PortType(PMMLPortObject.class);
    
    
    /**
     * 
     */
    public PMMLPortObject() {
        m_masterHandler = new PMMLMasterContentHandler();
    }
    
    /**
     * Adds a content handler to the master content handler. The master content 
     * handler forwards all relevant events from PMML file parsing to all 
     * registered content handlers.
     *  
     * @param id to later on retrieve the registered content handler
     * @param defaultHandler specialized content handler interested in certain 
     * parts of the PMML file (ClusteringModel, TreeModel, etc.)
     * @return true if the handler was added, false if it is already registered
     */
    public boolean addPMMLContentHandler(final String id, 
            final PMMLContentHandler defaultHandler) {
        return m_masterHandler.addContentHandler(id, defaultHandler);
    }
    
    /**
     * 
     * @param id the id which was used for registration of the handler
     * @return the handler registered with this id or null if no handler with 
     *  this id can be found
     */
    public PMMLContentHandler getPMMLContentHandler(final String id) {
        return m_masterHandler.getDefaultHandler(id);
    }
    
    /**
     * 
     * @return the type of model
     * @see PMMLModelType
     */
    public PMMLModelType getModelType() {
        return m_modelType;
    }
    
    private void init(final File file) 
        throws TransformerConfigurationException, SAXException, 
        FileNotFoundException {
        SAXTransformerFactory fac = (SAXTransformerFactory)TransformerFactory
        .newInstance();
        m_handler = fac.newTransformerHandler();
        
        Transformer t = m_handler.getTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        
        m_result = file;        
        m_fos = new FileOutputStream(m_result);
        m_handler.setResult(new StreamResult(m_fos));
        
        // PMML root element, namespace declaration, etc.
        m_handler.startDocument();
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "version", CDATA, "3.1");
        attr.addAttribute(null, null, "xmlns", CDATA, 
            "http://www.dmg.org/PMML-3_1");
        attr.addAttribute(null, null, "xmlns:xsi", CDATA, 
            "http://www.w3.org/2001/XMLSchema-instance");
        m_handler.startElement(null, null, "PMML", attr);
    }
    
    /**
     * Convenience method to write a PMML DataDictionary based on the data 
     * table spec.
     * @param spec the spec to be converted into a PMML DataDictionary
     * @throws SAXException if something goes wrong during writing
     */
    protected void writeDataDictionary(final DataTableSpec spec) 
        throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "numberOfFields", CDATA, 
                "" + spec.getNumColumns());
        m_handler.startElement(null, null, DATA_DICT, attr);
            // DataFields
            attr = new AttributesImpl();
            for (DataColumnSpec colSpec : spec) {
                // name
                attr.addAttribute(null, null, "name", CDATA, 
                        colSpec.getName());
                // optype
                String opType = "";
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    opType = "continuous";
                } else if (colSpec.getType().isCompatible(NominalValue.class)) {
                    opType = "categorical";
                }
                attr.addAttribute(null, null, "optype", CDATA, opType);
                // data type
                String dataType = "";
                if (colSpec.getType().isCompatible(IntValue.class)) {
                    dataType = "integer";
                } else if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    dataType = "double";
                } else if (colSpec.getType().isCompatible(StringValue.class)) {
                    dataType = "string";
                }
                attr.addAttribute(null, null, "dataType", CDATA, dataType);
                m_handler.startElement(null, null, DATA_FIELD, attr);
                // Value
                if (colSpec.getType().isCompatible(NominalValue.class)
                        && colSpec.getDomain().hasValues()) {
                    for (DataCell possVal : colSpec.getDomain().getValues()) {
                        AttributesImpl attr2 = new AttributesImpl();
                        attr2.addAttribute(null, null, "value", CDATA, 
                                possVal.toString());
                        m_handler.startElement(null, null, VALUE, attr2);
                        m_handler.endElement(null, null, VALUE);
                    }
                } else if (colSpec.getType().isCompatible(DoubleValue.class)
                        && colSpec.getDomain().hasBounds()) {
                    // Interval
                    AttributesImpl attr2 = new AttributesImpl();
                    attr2.addAttribute(null, null, "closure", CDATA, 
                            "openOpen");
                    attr2.addAttribute(null, null, "leftMargin", CDATA, 
                            "" + colSpec.getDomain().getLowerBound());
                    attr2.addAttribute(null, null, "rightMargin", CDATA, 
                            "" + colSpec.getDomain().getUpperBound());
                    m_handler.startElement(null, null, "Interval", attr2);
                    m_handler.endElement(null, null, "Interval");
                }
                m_handler.endElement(null, null, DATA_FIELD);
            }
            m_handler.endElement(null, null, DATA_DICT);        
    }
    
    /**
     * Writes the port object to valid PMML. Subclasses should not override this
     * method but the {@link #writePMMLModel(TransformerHandler)} instead.
     *    
     * 
     * @param file directory which should contain the PMML file 
     * @return valid PMML file
     * @throws SAXException if something goes wrong during writing of PMML
     * @throws IOException if the file cannot be written to the directory
     * @throws TransformerConfigurationException if something goes wrong with 
     *  the transformation handler 
     */
    public File save(final File file) 
        throws SAXException, IOException, TransformerConfigurationException {
        init(file);
        writeDataDictionary((DataTableSpec)getSpec());
        writePMMLModel(m_handler);
        m_handler.endElement(null, null, "PMML");
        m_handler.endDocument();
        m_fos.close();
        return m_result;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return m_spec;
    }
    
    /**
     * 
     * @param spec generic spec object. Subclasses are free to use a different 
     * spec than the {@link DataTableSpec}.
     */
    public void setSpec(final PortObjectSpec spec) {
        m_spec = (DataTableSpec)spec;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return null;
    }
    
    /**
     * Loads the port object by reading the file and setting the member 
     * variables, subclasses must override this method in order to register a 
     * specific content handler to the master content handler.
     * @see #addPMMLContentHandler(String, PMMLContentHandler)
     *  
     * @param f the fire containing the PMML
     * @return a loaded PMML object with all fields initialized with the values 
     *  found in the PMML
     * @throws ParserConfigurationException if parser cannot be instantiated
     * @throws SAXException if something goes wrong during parsing
     * @throws IOException if the file cannot be found or read
     */
    public PMMLPortObject loadFrom(final File f) 
        throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser = fac.newSAXParser();
        m_masterHandler.addContentHandler(ExtractModelTypeHandler.ID, 
                new ExtractModelTypeHandler());
        m_masterHandler.addContentHandler(DataDictionaryContentHandler.ID,
                new DataDictionaryContentHandler());
        parser.parse(f, m_masterHandler);
        ExtractModelTypeHandler hdl = (ExtractModelTypeHandler)m_masterHandler
            .getDefaultHandler(ExtractModelTypeHandler.ID);
        m_spec = ((DataDictionaryContentHandler)m_masterHandler
                    .getDefaultHandler(DataDictionaryContentHandler.ID))
                    .getDataTableSpec();
        m_modelType = hdl.getModelType();
        return this;
        
    }

    /**
     * 
     * @param handler the handler responsible for writing the PMML
     * @throws SAXException if something goes wrong during writing the PMML
     */
    protected void writePMMLModel(final TransformerHandler handler)
        throws SAXException {
        
    }

}
