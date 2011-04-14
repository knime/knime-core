/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
package org.knime.core.node.port.pmml;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
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
    public static final String MINING_SCHEMA = "MiningSchema";

    /** Constant for the LocalTransformations tag. */
    public static final String LOCAL_TRANS = "LocalTransformations";

    private final DataTableSpec m_dataTableSpec;

    private final List<String> m_learningFields;

    private final List<String> m_targetFields;

    private List<DataColumnSpec> m_learningCols;

    private List<DataColumnSpec> m_targetCols;

    private static PortObjectSpecSerializer<PMMLPortObjectSpec> serializer;

    private static final TreeSet<String> SUPPORTED_PMML_VERSIONS;

    static {
            SUPPORTED_PMML_VERSIONS = new TreeSet<String>();
            SUPPORTED_PMML_VERSIONS.add(PMMLPortObject.PMML_V3_1);
            SUPPORTED_PMML_VERSIONS.add(PMMLPortObject.PMML_V3_2);
            SUPPORTED_PMML_VERSIONS.add(PMMLPortObject.PMML_V4_0);
    };

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
     * PMMLPortObjectSpec should only be created by
     * {@link PMMLPortObjectSpecCreator}.
     *
     * @param dataDictionary {@link DataTableSpec} describing the training data
     * @param learningCols columns used for learning of the model
     * @param targetCols columns to be predicted
     */
    PMMLPortObjectSpec(final DataTableSpec dataDictionary,
            final List<String> learningCols, final List<String> targetCols) {
        m_dataTableSpec = dataDictionary;
        if (learningCols == null) {
            m_learningFields = new LinkedList<String>();
        } else {
            m_learningFields = learningCols;
        }
        if (targetCols == null) {
            m_targetFields = new LinkedList<String>();
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
    public List<String> getLearningFields() {
        return m_learningFields;
    }

    /**
     *
     * @return those columns used for learning of the model
     */
    public List<DataColumnSpec> getLearningCols() {
        if (m_learningCols != null) {
            return m_learningCols;
        }
        List<DataColumnSpec> learningCols = new LinkedList<DataColumnSpec>();
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
     * @return by the model predicted columns
     */
    public List<String> getTargetFields() {
        return m_targetFields;
    }

    /**
     *
     * @return those columns used for learning of the model
     */
    public List<DataColumnSpec> getTargetCols() {
        if (m_targetCols != null) {
            return m_targetCols;
        }
        List<DataColumnSpec> targetCols = new LinkedList<DataColumnSpec>();
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
     * Convenience method to write a PMML DataDictionary based on the data table
     * spec.
     *
     * @param spec the spec to be converted into a PMML DataDictionary
     * @param handler th econtent handler to write to
     * @param pmmlVersion The version to write,
     * e.g. {@link PMMLPortObject#PMML_V3_1}. This method fails if the version
     * is not supported.
     * @throws SAXException if something goes wrong during writing
     */
    static void writeDataDictionary(final DataTableSpec spec,
            final TransformerHandler handler, final String pmmlVersion)
            throws SAXException {
        if (!SUPPORTED_PMML_VERSIONS.contains(pmmlVersion)) {
            throw new SAXException("PMML model seems to be of an "
                    + "unsupported version. Only PMML versions "
                    + SUPPORTED_PMML_VERSIONS
                    + " are supported. Found " + pmmlVersion);
        }
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "numberOfFields", CDATA, ""
                + spec.getNumColumns());
        handler.startElement(null, null, DATA_DICT, attr);
        // DataFields
        attr = new AttributesImpl();
        for (DataColumnSpec colSpec : spec) {
            // name
            attr.addAttribute(null, null, "name", CDATA, colSpec.getName());
            // optype
            String opType = "";
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                opType = "continuous";
            } else if (colSpec.getType().isCompatible(NominalValue.class)) {
                opType = "categorical";
            } else {
                throw new SAXException("Type " + colSpec.getType()
                        + " is not supported"
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
                    attr2.addAttribute(null, null, "value", CDATA, possVal
                            .toString());
                    handler.startElement(null, null, VALUE, attr2);
                    handler.endElement(null, null, VALUE);
                }
            } else if (colSpec.getType().isCompatible(DoubleValue.class)
                    && colSpec.getDomain().hasBounds()) {
                // Interval
                AttributesImpl attr2 = new AttributesImpl();
                attr2.addAttribute(null, null, "closure", CDATA,
                                "closedClosed");
                attr2.addAttribute(null, null, "leftMargin", CDATA, ""
                        + colSpec.getDomain().getLowerBound());
                attr2.addAttribute(null, null, "rightMargin", CDATA, ""
                        + colSpec.getDomain().getUpperBound());
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
     * {@link PMMLPortObjectSpec}. Since the MiningSchema is inside the model
     * tag of the PMML file, implementing classes have to take open their model
     * tag, then call this method, write their model content and close the model
     * tag.
     *
     * @param portSpec based upon this port object spec the mining schema is
     *            written
     * @param handler transformation handler to write to
     * {@link PMMLPortObject#PMML_V3_1}. This method fails if the version is
     * unsupported.
     * @throws SAXException if something goes wrong
     */
    public static void writeMiningSchema(final PMMLPortObjectSpec portSpec,
            final TransformerHandler handler)
            throws SAXException {
        // start MiningSchema
        handler.startElement(null, null, MINING_SCHEMA, null);
        // active columns = learning fields
        for (String colSpec : portSpec.getLearningFields()) {
            AttributesImpl atts = new AttributesImpl();
            // don't write usageType = active (is default)
            atts.addAttribute(null, null, "name", CDATA, colSpec);
            atts.addAttribute(null, null, "invalidValueTreatment", CDATA,
                    "asIs");
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
            atts.addAttribute(null, null, "invalidValueTreatment", CDATA,
                    "asIs");
            handler.startElement(null, null, MINING_FIELD, atts);
            handler.endElement(null, null, MINING_FIELD);
        }
        handler.endElement(null, null, MINING_SCHEMA);
    }

    private static final String DTS_KEY = "DataTableSpec";

    private static final String DTS_FILE = "DataTableSpec.xml";

    private static final String MINING_SCHEMA_KEY = "MiningSchema";

    private static final String MINING_SCHEMA_FILE = "MiningSchema.xml";

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
        miningSchema.addStringArray(LEARNING_KEY, m_learningFields
                .toArray(new String[0]));
        miningSchema.addStringArray(TARGET_KEY, m_targetFields
                .toArray(new String[0]));

        out.putNextEntry(new ZipEntry(MINING_SCHEMA_FILE));
        miningSchema.saveToXML(noCloseOut);
        out.close();
    }

    /**
     *
     * @param in stream reading the relevant files
     * @return a completely loaded port object spec with {@link DataTableSpec},
     *         and the sets of learning, ignored and target columns.
     * @throws IOException if something goes wrong
     * @throws InvalidSettingsException if something goes wrong
     */
    public static PMMLPortObjectSpec loadFrom(
            final PortObjectSpecZipInputStream in) throws IOException,
            InvalidSettingsException {
        NonClosableInputStream noCloseIn = new NonClosableInputStream(in);
        // the data table spec
        in.getNextEntry();
        // TODO: sanitycheck if name is the same
        NodeSettingsRO settings = NodeSettings.loadFromXML(noCloseIn);
        DataTableSpec dataTableSpec = DataTableSpec.load(settings);
        // the mining schema
        in.getNextEntry();
        // TODO: sanity check if names are consistent
        NodeSettingsRO miningSchemaSettings =
                NodeSettings.loadFromXML(noCloseIn);
        List<String> learningCols = new LinkedList<String>();
        for (String colName
                : miningSchemaSettings.getStringArray(LEARNING_KEY)) {
            DataColumnSpec colSpec = dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                throw new InvalidSettingsException("Column " + colName
                        + " is not in DataTableSpec");
            }
            learningCols.add(colName);
        }
        List<String> targetCols = new LinkedList<String>();
        for (String colName : miningSchemaSettings.getStringArray(TARGET_KEY)) {
            DataColumnSpec colSpec = dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                throw new InvalidSettingsException("Column " + colName
                        + " is not in DataTableSpec");
            }
            targetCols.add(colName);
        }
        return new PMMLPortObjectSpec(dataTableSpec, learningCols, targetCols);
    }

    /**
     *
     * @param handler the handler to write to
     * @param pmmlVersion The PMML version to write, e.g.
     * {@link PMMLPortObject#PMML_V3_1}. This method fails if the version is
     * unsupported.
     * @throws SAXException if something goes wrong
     */
    static void writeHeader(final TransformerHandler handler,
            final String pmmlVersion) throws SAXException {
        if (!SUPPORTED_PMML_VERSIONS.contains(pmmlVersion)) {
            throw new SAXException("PMML model seems to be of an "
                    + "unsupported version. Only PMML versions "
                    + SUPPORTED_PMML_VERSIONS
                    + " are supported. Found " + pmmlVersion);
        }
        AttributesImpl atts = new AttributesImpl();
        String owner = System.getProperty("user.name");
        if (owner == null || owner.isEmpty()) {
            owner = "KNIME";
        }
        atts.addAttribute(null, null, "copyright", CDATA, owner);
        handler.startElement(null, null, "Header", atts);
        atts = new AttributesImpl();
        atts.addAttribute(null, null, "name", CDATA, "KNIME");
        atts.addAttribute(null, null, "version", CDATA, KNIMEConstants.MAJOR
                + "." + KNIMEConstants.MINOR + "." + KNIMEConstants.REV);
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
        return new JComponent[]{new DataTableSpecView(getDataTableSpec())};
    }
}
