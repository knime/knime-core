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
package org.knime.core.node.port.pmml;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.workflow.DataTableSpecView;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectSpec implements PortObjectSpec {
    
    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";
    /** Constant for DataDictionary. */
    protected static final String DATA_DICT = "DataDictionary";
    /** Constant for DataField. */
    protected static final String DATA_FIELD = "DataField";
    /** Constant for Value. */
    protected static final String VALUE = "Value";
    /** Constant for MiningField tag. */
    protected static final String MINING_FIELD = "MiningField";
    /** Constant for the MiningSchema tag. */
    protected static final String MINING_SCHEMA = "MiningSchema";
    
    private final DataTableSpec m_dataTableSpec;
    
    private final Set<String> m_learningFields;

    private final Set<String> m_ignoredFields;

    private final Set<String> m_targetFields;

    private Set<DataColumnSpec> m_learningCols;
    
    private Set<DataColumnSpec> m_ignoredCols;
    
    private Set<DataColumnSpec> m_targetCols;
    
    private static PortObjectSpecSerializer<PMMLPortObjectSpec>serializer;
    
    /**
     * 
     * @see PortObjectSpec
     * @return the serializer
     */
    public static PortObjectSpecSerializer<PMMLPortObjectSpec> 
        getPortObjectSpecSerializer() {
        if (serializer == null) {
            serializer = new PMMLPortObjectSpecSerializer();
        }
        return serializer;
    }
    
    /**
     * 
     * @param dataDictionary {@link DataTableSpec} describing the training data
     * @param learningCols columns used for learning of the model
     * @param ignoredCols columns ignored while learning the model
     * @param targetCols columns to be predicted
     */
    public PMMLPortObjectSpec(final DataTableSpec dataDictionary, 
            final Set<String> learningCols, 
            final Set<String> ignoredCols, 
            final Set<String> targetCols) {
        m_dataTableSpec = dataDictionary;
        if (learningCols == null) {
            m_learningFields = new LinkedHashSet<String>();
        } else {
            m_learningFields = learningCols;
        }
        if (ignoredCols == null) {
            m_ignoredFields = new LinkedHashSet<String>();
        } else {            
            m_ignoredFields = ignoredCols;
        }
        if (targetCols == null) {
            m_targetFields = new LinkedHashSet<String>();
        } else {
            m_targetFields = targetCols;
        }
    }
    
    /**
     * 
     * @return the {@link DataTableSpec} describing the training data  
     */
    public DataTableSpec getDataTableSpec() {
        return m_dataTableSpec;
    }
    
    /**
     * 
     * @return those columns used for learning of the model
     */
    public Set<String> getLearningFields() {
        return m_learningFields;
    }

    /**
     *
     * @return those columns used for learning of the model
     */
    public Set<DataColumnSpec> getLearningCols() {
        if (m_learningCols != null) {
        return m_learningCols;
    }
        Set<DataColumnSpec> learningCols = new LinkedHashSet<DataColumnSpec>();
        for (String learncol : m_learningFields) {
            DataColumnSpec colspec = m_dataTableSpec.getColumnSpec(learncol);
            assert colspec != null : "Learning column " + learncol + " not "
                    + "found in DataTableSpec.";
            learningCols.add(colspec);
        }
        m_learningCols = learningCols;
        return m_learningCols;
    }
    
    /**
     * 
     * @return those columns ignored while learning the model
     */
    public Set<String> getIgnoredFields() {
        return m_ignoredFields;
    }

    /**
    *
    * @return those columns used for learning of the model
    */
   public Set<DataColumnSpec> getIgnoredCols() {
       if (m_ignoredCols != null) {
           return m_ignoredCols;
       }
       Set<DataColumnSpec> ignoredCols = new LinkedHashSet<DataColumnSpec>();
       for (String ignoredcol : m_ignoredFields) {
           DataColumnSpec colspec = m_dataTableSpec.getColumnSpec(ignoredcol);
           assert colspec != null : "Ignored column " + ignoredcol + " not "
                   + "found in DataTableSpec.";
           ignoredCols.add(colspec);
       }
       m_ignoredCols = ignoredCols;
        return m_ignoredCols;
    }

    /**
     * 
     * @return by the model predicted columns 
     */
    public Set<String> getTargetFields() {
        return m_targetFields;
    }

    /**
    *
    * @return those columns used for learning of the model
    */
   public Set<DataColumnSpec> getTargetCols() {
       if (m_targetCols != null) {
        return m_targetCols;
    }
       Set<DataColumnSpec> targetCols = new LinkedHashSet<DataColumnSpec>();
       for (String targetCol : m_targetFields) {
           DataColumnSpec colspec = m_dataTableSpec.getColumnSpec(targetCol);
           assert colspec != null : "Target column " + targetCol + " not "
                   + "found in DataTableSpec.";
           targetCols.add(colspec);
       }
       m_targetCols = targetCols;
       return m_targetCols;
   }

    
    
    // **************** Persistence methods*****************/
    
    /**
     * Convenience method to write a PMML DataDictionary based on the data 
     * table spec.
     * @param spec the spec to be converted into a PMML DataDictionary
     * @param handler th econtent handler to write to
     * @throws SAXException if something goes wrong during writing
     */
    public static void writeDataDictionary(final DataTableSpec spec, 
            final TransformerHandler handler) 
        throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "numberOfFields", CDATA, 
                "" + spec.getNumColumns());
        handler.startElement(null, null, DATA_DICT, attr);
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
                } else {
                    throw new SAXException(
                            "Type " + colSpec.getType() + " is not supported" 
                            + " by PMML. Allowed types are only all " 
                            + "double-compatible and all nominal value " 
                            + "compatible types.");
                }
                attr.addAttribute(null, null, "optype", CDATA, opType);
                // data type
                String dataType = getDataType(colSpec);

                attr.addAttribute(null, null, "dataType", CDATA, dataType);
                handler.startElement(null, null, DATA_FIELD, attr);
                // Value
                if (colSpec.getType().isCompatible(NominalValue.class)
                        && colSpec.getDomain().hasValues()) {
                    for (DataCell possVal : colSpec.getDomain().getValues()) {
                        AttributesImpl attr2 = new AttributesImpl();
                        attr2.addAttribute(null, null, "value", CDATA, 
                                possVal.toString());
                        handler.startElement(null, null, VALUE, attr2);
                        handler.endElement(null, null, VALUE);
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
                    handler.startElement(null, null, "Interval", attr2);
                    handler.endElement(null, null, "Interval");
                }
                handler.endElement(null, null, DATA_FIELD);
            }
            handler.endElement(null, null, DATA_DICT);        
    }
    
    /**
     * 
     * @param colSpec the column spec to get the PMML data type attribute from
     * @return the PMML data type for the {@link DataColumnSpec}
     */
    public static String getDataType(final DataColumnSpec colSpec) {
        String dataType = "unknown";
        if (colSpec.getType().isCompatible(IntValue.class)) {
            dataType = "integer";
        } else if (colSpec.getType().isCompatible(DoubleValue.class)) {
            dataType = "double";
        } else if (colSpec.getType().isCompatible(StringValue.class)) {
            dataType = "string";
        }
        return dataType;
    }
    
    /**
     * Writes the MiningSchema based upon the fields of the passed 
     *  {@link PMMLPortObjectSpec}. Since the MiningSchema is inside the model
     *  tag of the PMML file, implementing classes have to take open their model
     *  tag, then call this method, write their model content andclose the model
     *  tag.
     *  
     * @param portSpec based upon this port object spec the mining schema is 
     *      written
     * @param handler transformartion handler to write to
     * @throws SAXException if something goes wrong 
     */
    public static void writeMiningSchema(final PMMLPortObjectSpec portSpec,
            final TransformerHandler handler) throws SAXException {
        // start MiningSchema
        handler.startElement(null, null, MINING_SCHEMA, null);
        // active columns = learning fields
        for (String colSpec : portSpec.getLearningFields()) {
            AttributesImpl atts = new AttributesImpl();
            // don't write usageType = active (is default)
            atts.addAttribute(null, null, "name", CDATA, colSpec);
            handler.startElement(null, null, MINING_FIELD, atts);
            handler.endElement(null, null, MINING_FIELD);
        }
        // ignored columns
        for (String colSpec : portSpec.getIgnoredFields()) {
            AttributesImpl atts = new AttributesImpl();
            // name
            atts.addAttribute(null, null, "name", CDATA, colSpec);
            // usageType = active
            atts.addAttribute(null, null, "usageType", CDATA, "supplementary");
            handler.startElement(null, null, MINING_FIELD, atts);
            handler.endElement(null, null, MINING_FIELD);
        }
        // target columns = predicted
        for (String colSpec : portSpec.getTargetFields()) {
            AttributesImpl atts = new AttributesImpl();
            // name
            atts.addAttribute(null, null, "name", CDATA, colSpec);
            // usageType = active
            atts.addAttribute(null, null, "usageType", CDATA, "predicted");
            handler.startElement(null, null, MINING_FIELD, atts);
            handler.endElement(null, null, MINING_FIELD);
        }
        handler.endElement(null, null, MINING_SCHEMA);
    }
    
    /*
    public static PMMLPortObjectSpec loadFrom(final File specFile) 
        throws IOException {
        try {
            SAXParserFactory fac = SAXParserFactory.newInstance();
            SAXParser parser = fac.newSAXParser();
            DataDictionaryContentHandler dataDictHdl =
                    new DataDictionaryContentHandler();
            MiningSchemaContentHandler miningSchemaHdl =
                    new MiningSchemaContentHandler();
            PMMLMasterContentHandler masterHandler =
                    new PMMLMasterContentHandler();
            masterHandler.addContentHandler(DataDictionaryContentHandler.ID,
                    dataDictHdl);
            masterHandler.addContentHandler(MiningSchemaContentHandler.ID,
                    miningSchemaHdl);
            parser.parse(specFile, masterHandler);
            DataTableSpec dataTableSpec = dataDictHdl.getDataTableSpec();
            PMMLPortObjectSpecCreator creator =
                    new PMMLPortObjectSpecCreator(dataTableSpec);
            creator.setIgnoredCols(miningSchemaHdl.getIgnoredFields());
            creator.setLearningCols(miningSchemaHdl.getLearningFields());
            creator.setTargetCols(miningSchemaHdl.getTargetFields());
            return creator.createSpec();
        } catch (SAXException saxe) {
            throw new IOException(saxe);
        } catch (ParserConfigurationException pce) {
            throw new IOException(pce);
        }
    }
    */
    
    private static final String DTS_KEY = "DataTableSpec";
    private static final String DTS_FILE = "DataTableSpec.xml";
    private static final String MINING_SCHEMA_KEY = "MiningSchema";
    private static final String MINING_SCHEMA_FILE = "MiningSchema.xml";
    private static final String IGNORED_KEY = "ignored";
    private static final String LEARNING_KEY = "learning";
    private static final String TARGET_KEY = "target";
    
    /**
     * 
     * @param out zipped stream to write the entries to
     * @throws IOException if something goes wrong
     */
    public void saveTo(final PortObjectSpecZipOutputStream out) 
        throws IOException {
        NodeSettings settings = new NodeSettings(DTS_KEY);
        m_dataTableSpec.save(settings);
        NonClosableOutputStream noCloseOut = new NonClosableOutputStream(out);
        out.putNextEntry(new ZipEntry(DTS_FILE));
        settings.saveToXML(noCloseOut);

        NodeSettings miningSchema = new NodeSettings(MINING_SCHEMA_KEY);
        miningSchema.addStringArray(IGNORED_KEY, 
                m_ignoredFields.toArray(new String[0]));
        miningSchema.addStringArray(LEARNING_KEY, 
                m_learningFields.toArray(new String[0]));
        miningSchema.addStringArray(TARGET_KEY, 
                m_targetFields.toArray(new String[0]));
        
        out.putNextEntry(new ZipEntry(MINING_SCHEMA_FILE));
        miningSchema.saveToXML(noCloseOut);
        out.close();
    }
    
    /**
     * 
     * @param in stream reading the relevant files
     * @return a completely loaded port object spec with {@link DataTableSpec},
     *  and the sets of learning, ignored and target columns. 
     * @throws IOException if something goes wrong
     * @throws InvalidSettingsException if something goes wrong
     */
    public static PMMLPortObjectSpec loadFrom(
            final PortObjectSpecZipInputStream in) 
        throws IOException, InvalidSettingsException {
        NonClosableInputStream noCloseIn = new NonClosableInputStream(in);
        // the data table spec 
        in.getNextEntry();
        // TODO: sanitycheck if name is the same
        NodeSettingsRO settings = NodeSettings.loadFromXML(noCloseIn);
        DataTableSpec dataTableSpec = DataTableSpec.load(settings);
        // the mining schema
        in.getNextEntry();
        // TODO: sanity check if names are consistent
        NodeSettingsRO miningSchemaSettings = NodeSettings.loadFromXML(
                noCloseIn);
        Set<String>ignoredCols = new LinkedHashSet<String>();
        for (String colName : miningSchemaSettings.getStringArray(
                IGNORED_KEY)) {
            DataColumnSpec colSpec = dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                throw new InvalidSettingsException("Column " 
                        + colName + " is not in DataTableSpec");
            }
            ignoredCols.add(colName);
        }
        Set<String>learningCols = new LinkedHashSet<String>();
        for (String colName : miningSchemaSettings.getStringArray(
                LEARNING_KEY)) {
            DataColumnSpec colSpec = dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                throw new InvalidSettingsException("Column " 
                        + colName + " is not in DataTableSpec");
            }
            learningCols.add(colName);
        }
        Set<String>targetCols = new LinkedHashSet<String>();
        for (String colName : miningSchemaSettings.getStringArray(
                TARGET_KEY)) {
            DataColumnSpec colSpec = dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                throw new InvalidSettingsException("Column " 
                        + colName + " is not in DataTableSpec");                
            }
            targetCols.add(colName);
        }
        return new PMMLPortObjectSpec(dataTableSpec, 
                learningCols, ignoredCols, targetCols);
    }

    /**
     * 
     * @param handler the handler to write to
     * @throws SAXException if something goes wrong
     */
    public static void writeHeader(final TransformerHandler handler) 
        throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        String owner = System.getProperty("user.name");
        if (owner == null || owner.isEmpty()) {
            owner = "KNIME";
        }
        atts.addAttribute(null, null, "copyright", CDATA, owner);
        handler.startElement(null, null, "Header", atts);
        atts = new AttributesImpl();
        atts.addAttribute(null, null, "name", CDATA, "KNIME");
        atts.addAttribute(null, null, "version", CDATA, 
                KNIMEConstants.MAJOR + "." + KNIMEConstants.MINOR);
        handler.startElement(null, null, "Application", atts);
        handler.endElement(null, null, "Application");
        handler.endElement(null, null, "Header");
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[] {new DataTableSpecView(getDataTableSpec())};
    }
    
}
