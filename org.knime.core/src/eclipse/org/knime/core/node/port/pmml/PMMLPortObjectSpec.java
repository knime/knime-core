/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
import javax.xml.transform.sax.TransformerHandler;

import org.dmg.pmml.ApplicationDocument.Application;
import org.dmg.pmml.HeaderDocument.Header;
import org.dmg.pmml.PMMLDocument.PMML;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
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

    /** The application name for PMML generated with KNIME. */
    public static final String KNIME = "KNIME";

    private final DataTableSpec m_dataTableSpec;

    private final List<String> m_learningFields;

    private final List<String> m_predictedFields;

    private final List<String> m_preprocFields;

    private List<DataColumnSpec> m_learningCols;

    private List<DataColumnSpec> m_predictedCols;

    private List<DataColumnSpec> m_preprocCols;

    private static PortObjectSpecSerializer<PMMLPortObjectSpec> serializer;

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
     * @param preprocFields all columns involved in preprocessing steps
     * @param learningFields columns used for learning of the model
     * @param targetFields columns to be predicted
     */
    PMMLPortObjectSpec(final DataTableSpec dataDictionary,
            final List<String> preprocFields, final List<String> learningFields,
            final List<String> targetFields) {
        m_dataTableSpec = dataDictionary;

        if (preprocFields == null) {
            m_preprocFields = new LinkedList<String>();
        } else {
            m_preprocFields = new LinkedList<String>(preprocFields);
        }
        if (learningFields == null) {
            m_learningFields = new LinkedList<String>();
        } else {
            m_learningFields = new LinkedList<String>(learningFields);
        }
        if (targetFields == null) {
            m_predictedFields = new LinkedList<String>();
        } else {
            m_predictedFields = new LinkedList<String>(targetFields);
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
        return Collections.unmodifiableList(m_learningFields);
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
        for (String learnCol : m_learningFields) {
            DataColumnSpec colspec = m_dataTableSpec.getColumnSpec(learnCol);
            assert colspec != null : "Learning column " + learnCol + " not "
                    + "found in DataTableSpec.";
            learningCols.add(colspec);
        }
        m_learningCols = learningCols;
        return Collections.unmodifiableList(m_learningCols);
    }

    /**
     *
     * @return by the model predicted columns
     */
    public List<String> getTargetFields() {
        return Collections.unmodifiableList(m_predictedFields);
    }

    /**
     *
     * @return those columns used for learning of the model
     */
    public List<DataColumnSpec> getTargetCols() {
        if (m_predictedCols != null) {
            return m_predictedCols;
        }
        List<DataColumnSpec> targetCols = new LinkedList<DataColumnSpec>();
        for (String targetCol : m_predictedFields) {
            DataColumnSpec colspec = m_dataTableSpec.getColumnSpec(targetCol);
            assert colspec != null : "Target column " + targetCol + " not "
                    + "found in DataTableSpec.";
            targetCols.add(colspec);
        }
        m_predictedCols = targetCols;
        return Collections.unmodifiableList(m_predictedCols);
    }

    /**
    * @return those columns involved in preprocessing steps
    */
   public List<String> getPreprocessingFields() {
       return Collections.unmodifiableList(m_preprocFields);
   }

   /**
    * @return those columns involved in preprocessing steps
    */
   public List<DataColumnSpec> getPreprocessingCols() {
       if (m_preprocCols != null) {
           return m_preprocCols;
       }
       List<DataColumnSpec> preprocCols = new LinkedList<DataColumnSpec>();
       for (String preproc : m_preprocFields) {
           DataColumnSpec colspec = m_dataTableSpec.getColumnSpec(preproc);
           assert colspec != null : "Preprocessing column " + preproc + " not "
                   + "found in DataTableSpec.";
           preprocCols.add(colspec);
       }
       m_preprocCols = preprocCols;
       return Collections.unmodifiableList(m_preprocCols);
   }

   /**
   *
   * @return those columns used for preprocessing and learning the model
   */
  public Set<String> getActiveFields() {
      LinkedHashSet<String> active = new LinkedHashSet<String>(
              m_learningFields);
      active.addAll(m_preprocFields);
      return Collections.unmodifiableSet(active);
  }

 /**
  *
  * @return those columns used for preprocessing and learning the model
  */
  public List<DataColumnSpec> getActiveCols() {
      Set<String> activeFields = getActiveFields();
      List<DataColumnSpec> activeCols = new LinkedList<DataColumnSpec>();
      for (String field : activeFields) {
        activeCols.add(m_dataTableSpec.getColumnSpec(field));
      }
      return Collections.unmodifiableList(activeCols);
  }


    // **************** Persistence methods*****************/

    /**
     * @param pmmlDoc the PMML document to write the header to
     */
    public static void writeHeader(final PMML pmmlDoc) {
        Header header = Header.Factory.newInstance();
        String owner = System.getProperty("user.name");
        if (owner == null || owner.isEmpty()) {
            owner = KNIME;
        }
        header.setCopyright(owner);

        Application application = Application.Factory.newInstance();
        application.setName(KNIME);
        application.setVersion(KNIMEConstants.MAJOR + "." + KNIMEConstants.MINOR
                + "." + KNIMEConstants.REV);
        header.setApplication(application);

        pmmlDoc.setHeader(header);
    }



    private static final String DTS_KEY = "DataTableSpec";

    private static final String DTS_FILE = "DataTableSpec.xml";

    private static final String MINING_SCHEMA_KEY = "MiningSchema";

    private static final String MINING_SCHEMA_FILE = "MiningSchema.xml";

    private static final String LEARNING_KEY = "learning";

    private static final String TARGET_KEY = "target";

    private static final String PREPROC_COL_FILE = "ActiveColumns.xml";
    private static final String PREPROC_KEY = "preprocessing";
    private static final String PREPROC_COL_KEY = "preprocColumns";

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
        miningSchema.addStringArray(TARGET_KEY, m_predictedFields
                .toArray(new String[0]));
        out.putNextEntry(new ZipEntry(MINING_SCHEMA_FILE));
        miningSchema.saveToXML(noCloseOut);
        out.putNextEntry(new ZipEntry(PREPROC_COL_FILE));
        NodeSettings preprocessing = new NodeSettings(PREPROC_KEY);
        preprocessing.addStringArray(PREPROC_COL_KEY,
                m_preprocFields.toArray(new String[0]));
        preprocessing.saveToXML(noCloseOut);
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

        // the preprocessing settings if existent
        ZipEntry preprocEntry = in.getNextEntry();
        List<String> activeCols = null;
        if (preprocEntry != null) {
            NodeSettingsRO preprocSettings = NodeSettings.loadFromXML(
                    noCloseIn);
            activeCols = new LinkedList<String>();
            for (String colName : preprocSettings.getStringArray(
                    PREPROC_COL_KEY)) {
                DataColumnSpec colSpec = dataTableSpec.getColumnSpec(colName);
                if (colSpec == null) {
                    throw new InvalidSettingsException("Column " + colName
                            + " is not in DataTableSpec");
                }
                activeCols.add(colName);
            }
        }
        return new PMMLPortObjectSpec(dataTableSpec, activeCols,
                learningCols, targetCols);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        DataTableSpec activeTableSpec = new DataTableSpec(
                getActiveCols().toArray(new DataColumnSpec[0]));
        return new JComponent[]{new DataTableSpecView(activeTableSpec)};
    }

/*__________________________________________________________________________*/
    /* TODO Remove after all has been switched to XMLBeans!
     * Just added temporary for models still using SAX. Will be removed soon.*/
    /** Constant for CDATA. */
    private static final String CDATA = "CDATA";

    /** Constant for MiningField tag. */
    private static final String MINING_FIELD = "MiningField";

    /** Constant for the MiningSchema tag. */
    private static final String MINING_SCHEMA = "MiningSchema";

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
    @Deprecated
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_dataTableSpec == null) ? 0
                : m_dataTableSpec.hashCode());
        result = prime * result + ((m_learningFields == null) ? 0
                : m_learningFields.hashCode());
        result = prime * result + ((m_predictedFields == null) ? 0
                : m_predictedFields.hashCode());
        result = prime * result + ((m_preprocFields == null) ? 0
                : m_preprocFields.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PMMLPortObjectSpec other = (PMMLPortObjectSpec)obj;
        if (m_dataTableSpec == null) {
            if (other.m_dataTableSpec != null) {
                return false;
            }
        } else if (!m_dataTableSpec.equalStructure(other.m_dataTableSpec)) {
            return false;
        }
        if (m_learningFields == null) {
            if (other.m_learningFields != null) {
                return false;
            }
        } else if (!m_learningFields.equals(other.m_learningFields)) {
            return false;
        }
        if (m_predictedFields == null) {
            if (other.m_predictedFields != null) {
                return false;
            }
        } else if (!m_predictedFields.equals(other.m_predictedFields)) {
            return false;
        }
        if (m_preprocFields == null) {
            if (other.m_preprocFields != null) {
                return false;
            }
        } else if (!m_preprocFields.equals(other.m_preprocFields)) {
            return false;
        }
        return true;
    }


}
