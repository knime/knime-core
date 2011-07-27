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
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.dmg.pmml40.ApplicationDocument.Application;
import org.dmg.pmml40.AssociationModelDocument.AssociationModel;
import org.dmg.pmml40.ClusteringModelDocument.ClusteringModel;
import org.dmg.pmml40.DataDictionaryDocument.DataDictionary;
import org.dmg.pmml40.DataFieldDocument.DataField;
import org.dmg.pmml40.DerivedFieldDocument.DerivedField;
import org.dmg.pmml40.ExtensionDocument;
import org.dmg.pmml40.GeneralRegressionModelDocument.GeneralRegressionModel;
import org.dmg.pmml40.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml40.MiningFieldDocument.MiningField;
import org.dmg.pmml40.MiningModelDocument.MiningModel;
import org.dmg.pmml40.MiningSchemaDocument.MiningSchema;
import org.dmg.pmml40.NaiveBayesModelDocument.NaiveBayesModel;
import org.dmg.pmml40.NeuralNetworkDocument.NeuralNetwork;
import org.dmg.pmml40.PMMLDocument;
import org.dmg.pmml40.PMMLDocument.PMML;
import org.dmg.pmml40.RegressionModelDocument.RegressionModel;
import org.dmg.pmml40.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml40.SequenceModelDocument.SequenceModel;
import org.dmg.pmml40.SupportVectorMachineModelDocument.SupportVectorMachineModel;
import org.dmg.pmml40.TextModelDocument.TextModel;
import org.dmg.pmml40.TimeSeriesModelDocument.TimeSeriesModel;
import org.dmg.pmml40.TransformationDictionaryDocument.TransformationDictionary;
import org.dmg.pmml40.TreeModelDocument.TreeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.xml.PMMLCellFactory;
import org.knime.core.data.xml.PMMLValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.pmml.PMMLFormatter;
import org.knime.core.pmml.PMMLModelType;
import org.knime.core.pmml.PMMLUtils;
import org.knime.core.pmml.PMMLValidator;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;


/**
 *
 * @author Fabian Dill, University of Konstanz
 * @author Dominik Morent, KNIME.com GmbH, Zurich, Switzerland
 */
public final class PMMLPortObject implements PortObject {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PMMLPortObject.class);

    /** Convenience accessor for the port type. */
    public static final PortType TYPE = new PortType(PMMLPortObject.class);

    /** Constant for CDATA. */
    public static final String CDATA = "CDATA";
    /** Constant for DataDictionary. */
    public static final String DATA_DICT = "DataDictionary";
    /** Constant for DataField. */
    public static final String DATA_FIELD = "DataField";
    /** Constant for PMML Element. */
    public static final String PMML_ELEMENT = "PMML";
    /** Constant for Extension element. */
    public static final String EXTENSION_ELEMENT = "Extension";

    /** Constant for Value. */
    protected static final String VALUE = "Value";
    /** Constant for the LocalTransformations tag. */
    protected static final String LOCAL_TRANS = "LocalTransformations";


    /** Version array for KNIME v.2.3.3. */
    public static final Integer[] KNIME_V_2_3_3 = new Integer[]{2, 3, 3};

    /** Version array for KNIME v.2.4. */
    public static final Integer[] KNIME_V_2_4 = new Integer[]{2, 4};

    /** Constant for version 3.0.*/
    public static final String PMML_V3_0 = "3.0";
    /** Constant for version 3.1.*/
    public static final String PMML_V3_1 = "3.1";
    /** Constant for version 3.2.*/
    public static final String PMML_V3_2 = "3.2";
    /** Constant for version 4.0.*/
    public static final String PMML_V4_0 = "4.0";

    /** Static initialization of all expressions needed for XPath.*/
    private static final String NAMESPACE_DECLARATION =
            "declare namespace pmml='http://www.dmg.org/PMML-4_0'; ";
    private static final String PATH_END = "']";
    private static final String FIELD = "field";
    private static final String NAME = "name";
    private static final String LABEL = "label";
    private static final String PREDICTOR_NAME = "predictorName";
    private static final String TREE_PATH =
            "./pmml:TreeModel/descendant::pmml:Node/descendant::*[@field='";
    private static final String CLUSTERING_PATH =
            "./pmml:ClusteringModel/pmml:ClusteringField[@field='";
    private static final String NN_PATH =
            "./pmml:NeuralNetwork/pmml:NeuralInputs/pmml:NeuralInput"
            + "/pmml:DerivedField/descendant::*[@field='";
    private static final String SVM_PATH =
            "./pmml:SupportVectorMachineModel/pmml:VectorDictionary/"
            + "pmml:VectorFields/pmml:FieldRef[@field='";
    private static final String REGRESSION_PATH_1 =
           "./pmml:RegressionModel/pmml:RegressionTable/descendant::*[@field='";
    private static final String REGRESSION_PATH_2 =
            "./pmml:RegressionModel/pmml:RegressionTable/descendant::*[@name='";
    private static final String GR_PATH_1 =
            "./pmml:GeneralRegressionModel/*/pmml:Predictor[@name='";
    private static final String GR_PATH_2 =
            "./pmml:GeneralRegressionModel/pmml:ParameterList/"
            + "pmml:Parameter[@label='";
    private static final String GR_PATH_3 =
            "./pmml:GeneralRegressionModel/pmml:PPMatrix/"
            + "pmml:PPCell[@predictorName='";
    /* ------------------------------------------------------ */


    private static final Map<String, String> VERSION_NAMESPACE_MAP
            = new HashMap<String, String>();

    private PMMLDocument m_pmmlDoc;

    static {
        VERSION_NAMESPACE_MAP.put(PMML_V3_0, "http://www.dmg.org/PMML-3_0");
        VERSION_NAMESPACE_MAP.put(PMML_V3_1, "http://www.dmg.org/PMML-3_1");
        VERSION_NAMESPACE_MAP.put(PMML_V3_2, "http://www.dmg.org/PMML-3_2");
        VERSION_NAMESPACE_MAP.put(PMML_V4_0, "http://www.dmg.org/PMML-4_0");
    }


    private PMMLPortObjectSpec m_spec;

    private static PMMLPortObjectSerializer serializer;

    /**
     * Static serializer as demanded from {@link PortObject} framework.
     * @return serializer for PMML (reads and writes PMML files)
     */
    public static final PortObjectSerializer<PMMLPortObject>
            getPortObjectSerializer() {
        if (serializer == null) {
            serializer = new PMMLPortObjectSerializer();
        }
        return serializer;
    }


    /**
     * Default constructor necessary for loading. Derived classes also
     * <em>must</em> provide a default constructor, otherwise loading will fail.
     * Calling this constructor is discouraged. It is only available for
     * internal calls.
     */
    PMMLPortObject() {
        /** Has to be initialized by calling the loadFrom(...) method. */
    }

    /**
     * Creates a new PMML port object. Models can be added later by calling
     * {@link #addModelTranslater(PMMLTranslator)}.
     * @param spec the referring {@link PMMLPortObjectSpec}
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec) {
        this(spec, (PMMLPortObject)null, null);
    }

    /**
     * Creates a new PMML port object baed on the spec and the PMML document
     * after it has been validated.
     *
     * @param spec the {@link PMMLPortObjectSpec}
     * @param pmmlDoc a PMML document
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec,
            final PMMLDocument pmmlDoc) {
        m_spec = spec;
        m_pmmlDoc = pmmlDoc;
        if (!m_pmmlDoc.validate()) {
            throw new IllegalArgumentException(
                    "The passed PMML document is not valid.");
        }
    }

    /**
     * Creates a new PMML port based on the {@link PMMLPortObjectSpec} and the
     * {@link PMMLPortObject}. If port is null it has the same effect as calling
     * {@link #PMMLPortObject(PMMLPortObjectSpec)}.
     *
     * @param spec the referring {@link PMMLPortObjectSpec}
     * @param port the existing PMML port
     * @see #PMMLPortObject(PMMLPortObjectSpec)
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec,
            final PMMLPortObject port) {
        this(spec, port, null);
    }

    /**
     * Creates a new PMML port based on the {@link PMMLPortObjectSpec} and the
     * {@link PMMLPortObject}. If port is null the inData is
     * used for initialization, otherwise inData is ignored.
     *
     * @param spec the referring {@link PMMLPortObjectSpec}
     * @param port the existing PMML port
     * @param inData the incoming data table spec
     * @see #PMMLPortObject(PMMLPortObjectSpec)
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec,
            final PMMLPortObject port, final DataTableSpec inData) {
        m_spec = spec;
        if (port != null) {
            parse(port.getPMMLValue().getDocument());
        } else if (inData != null) {
            initializePMMLDocument(inData);
        } else {
            initializePMMLDocument(spec.getDataTableSpec());
        }
    }

    /**
     * @param doc
     */
    private void parse(final Document doc) {
        try {
            m_pmmlDoc = PMMLDocument.Factory.parse(doc);
            // no validation needed here as the input is already validated
        } catch (XmlException e) {
            throw new IllegalArgumentException("An error occured while "
                    + "parsing the PMML document.", e);
        }
    }

    /* Just added temporary for models still using SAX. Will be removed soon.*/
    /**
     * @param spec the port object spec
     * @param handler the pmml content handler that adds the model content
     */
    @Deprecated
    public PMMLPortObject(final PMMLPortObjectSpec spec,
            final PMMLContentHandler handler) {
        this(spec);
        try {
            addPMMLModelFromHandler(handler);
        } catch (SAXException e) {
            throw new RuntimeException("Could not add model to PMML port "
                    + "object.", e);
        }
    }

    /**
     * Appends the pmml model of the content handler by invoking its
     * {@link PMMLContentHandler#addPMMLModel(DocumentFragment,
     * PMMLPortObjectSpec)} method.
     * Only {@link PMMLModelType} elements can be added.
     *
     * @param model the model fragment to add
     * @throws SAXException if the pmml model could not be added
     */
    @Deprecated
    public void addPMMLModelFromHandler(final PMMLContentHandler handler)
            throws SAXException {
        XmlObject model = null;
        try {
            model = XmlObject.Factory.parse(
                    handler.getPMMLModelFragment(m_spec));
        } catch (Exception e) {
            throw new SAXException(e);
        }
        PMML pmmlXml = m_pmmlDoc.getPMML();
        XmlCursor pmmlCursor = pmmlXml.newCursor();
        pmmlCursor.toEndToken();
        XmlCursor modelCursor = model.newCursor();
        modelCursor.toFirstChild();
        modelCursor.copyXml(pmmlCursor);
        modelCursor.dispose();
        pmmlCursor.dispose();
    }

    /*=======================================================================*/

    /**
     * Writes the port object to valid PMML.
     *
     * @param out stream which reads the PMML file
     * @throws IOException if the file cannot be written to the stream
     */
    public final void save(final OutputStream out) throws IOException {
        PMMLFormatter.save(m_pmmlDoc, out);
        out.close();
    }

   /**
     * Creates a pmml document from scratch (still without a model) and stores
     * it as the PMMLValue of this class.
 * @param inData the data table spec
     */
    private void initializePMMLDocument(final DataTableSpec inData) {
        m_pmmlDoc = PMMLDocument.Factory.newInstance(
                PMMLFormatter.getOptions());
        PMML pmml = m_pmmlDoc.addNewPMML();
        pmml.setVersion(PMML_V4_0);
        PMMLPortObjectSpec.writeHeader(pmml);
        new PMMLDataDictionaryTranslator().exportTo(m_pmmlDoc, inData);
        /** No model is written yet. It has to be added by the responsible
         PMMLTranslator. */
    }

    /**
     * @param translator the model translator to be initialized
     */
    public void initializeModelTranslator(final PMMLTranslator translator) {
        translator.initializeFrom(m_pmmlDoc);
    }

    /**
     * Adds the model of the content translater to the PMML document.
     * @param modelTranslator the model translator containing the model to be
     *      added
     */
    public void addModelTranslater(final PMMLTranslator modelTranslator) {
        SchemaType type = modelTranslator.exportTo(m_pmmlDoc, m_spec);
        moveDerivedFields(type);

        /* Remove mining fields from mining schema that where created as a
         * derived field. In KNIME the origin of columns is not distinguished
         * and all columns are added to the mining schema. But in PMML this
         * results in duplicate entries. Those columns should only appear once
         * as derived field in the transformation dictionary or local
         * transformations. */
        Set<String> derivedFields = new HashSet<String>();
        for (DerivedField derivedField : getDerivedFields()) {
            derivedFields.add(derivedField.getName());
        }
        MiningSchema miningSchema = PMMLUtils.getFirstMiningSchema(m_pmmlDoc,
                type);
        if (miningSchema == null) {
            LOGGER.info("No mining schema found.");
            return;
        }
        MiningField[] miningFieldArray = miningSchema.getMiningFieldArray();
        List<MiningField> miningFields = new ArrayList<MiningField>(
                Arrays.asList(miningFieldArray));
        for (MiningField miningField : miningFieldArray) {
            if (derivedFields.contains(miningField.getName())) {
                miningFields.remove(miningField);
            }
        }
        miningSchema.setMiningFieldArray(miningFields.toArray(
                new MiningField[0]));
    }


    /** Moves the content of the transformation dictionary to local
     * transformations of the model if a model exists. */
    public void moveGlobalTransformationsToModel() {
        PMML pmml = m_pmmlDoc.getPMML();
        TransformationDictionary transDict
                = pmml.getTransformationDictionary();
        if (transDict == null || transDict.getDerivedFieldArray() == null
                || transDict.getDerivedFieldArray().length == 0) {
            // nothing to be moved
            return;
        }
        DerivedField[] globalDerivedFields = transDict.getDerivedFieldArray();
        LocalTransformations localTrans = null;
        if (pmml.getTreeModelArray().length > 0) {
            TreeModel model = pmml.getTreeModelArray(0);
            localTrans = model.getLocalTransformations();
            if (localTrans == null) {
                localTrans = model.addNewLocalTransformations();
            }
        } else if (pmml.getClusteringModelArray().length > 0) {
            ClusteringModel model = pmml.getClusteringModelArray(0);
            localTrans = model.getLocalTransformations();
            if (localTrans == null) {
                localTrans = model.addNewLocalTransformations();
            }
        } else if (pmml.getNeuralNetworkArray().length > 0) {
            NeuralNetwork model = pmml.getNeuralNetworkArray(0);
            localTrans = model.getLocalTransformations();
            if (localTrans == null) {
                localTrans = model.addNewLocalTransformations();
            }
        } else if (pmml.getSupportVectorMachineModelArray().length > 0) {
            SupportVectorMachineModel model
                    = pmml.getSupportVectorMachineModelArray(0);
            localTrans = model.getLocalTransformations();
            if (localTrans == null) {
                localTrans = model.addNewLocalTransformations();
            }
        } else if (pmml.getRegressionModelArray().length > 0) {
            RegressionModel model = pmml.getRegressionModelArray(0);
            localTrans = model.getLocalTransformations();
            if (localTrans == null) {
                localTrans = model.addNewLocalTransformations();
            }
        } else if (pmml.getGeneralRegressionModelArray().length > 0) {
            GeneralRegressionModel model
                    = pmml.getGeneralRegressionModelArray(0);
            localTrans = model.getLocalTransformations();
            if (localTrans == null) {
                localTrans = model.addNewLocalTransformations();
            }
        }
        if (localTrans != null) {
            DerivedField[] derivedFields = appendDerivedFields(
                    localTrans.getDerivedFieldArray(), globalDerivedFields);
            localTrans.setDerivedFieldArray(derivedFields);
            // remove derived fields from TransformationDictionary
            transDict.setDerivedFieldArray(new DerivedField[0]);
        } // else do nothing as no model exists yet
    }

    /* Moves the content of the transformation dictionary to local
     * transformations. */
    private void moveDerivedFields(final SchemaType type) {
        PMML pmml = m_pmmlDoc.getPMML();

        TransformationDictionary transDict
                = pmml.getTransformationDictionary();
        if (transDict == null) { // nothing to be moved
            return;
        }
        LocalTransformations localTrans
                = LocalTransformations.Factory.newInstance();
        localTrans.setDerivedFieldArray(transDict.getDerivedFieldArray());
        localTrans.setExtensionArray(transDict.getExtensionArray());

        /*
         * Unfortunately the PMML models have no common base class. Therefore a
         * cast to the specific type is necessary for being able to add the
         * mining schema.
         */
        boolean known = true;
        if (AssociationModel.type.equals(type)) {
            AssociationModel model = pmml.getAssociationModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (ClusteringModel.type.equals(type)) {
            ClusteringModel model = pmml.getClusteringModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (GeneralRegressionModel.type.equals(type)) {
            GeneralRegressionModel model
                    = pmml.getGeneralRegressionModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (MiningModel.type.equals(type)) {
            MiningModel model = pmml.getMiningModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (NaiveBayesModel.type.equals(type)) {
            NaiveBayesModel model = pmml.getNaiveBayesModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (NeuralNetwork.type.equals(type)) {
            NeuralNetwork model = pmml.getNeuralNetworkArray(0);
            model.setLocalTransformations(localTrans);
        } else if (RegressionModel.type.equals(type)) {
            RegressionModel model = pmml.getRegressionModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (RuleSetModel.type.equals(type)) {
            RuleSetModel model = pmml.getRuleSetModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (SequenceModel.type.equals(type)) {
            SequenceModel model = pmml.getSequenceModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (SupportVectorMachineModel.type.equals(type)) {
            SupportVectorMachineModel model
                    = pmml.getSupportVectorMachineModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (TextModel.type.equals(type)) {
            TextModel model = pmml.getTextModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (TimeSeriesModel.type.equals(type)) {
            TimeSeriesModel model = pmml.getTimeSeriesModelArray(0);
            model.setLocalTransformations(localTrans);
        } else if (TreeModel.type.equals(type)) {
            TreeModel model = pmml.getTreeModelArray(0);
            model.setLocalTransformations(localTrans);
        } else {
            LOGGER.error("Could not move TransformationDictionary to "
                    + "unsupported model of type \"" + type + "\".");
            known = false;
        }
        if (known) {
            // remove derived fields from TransformationDictionary
            transDict.setDerivedFieldArray(new DerivedField[0]);
            transDict.setExtensionArray(new ExtensionDocument.Extension[0]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        StringBuffer sb = new StringBuffer();
        sb.append("PMML document with version ");
        sb.append(m_pmmlDoc.getPMML().getVersion());
        sb.append(" and models: ");
        boolean first = true;
        for (PMMLModelType modelType
                : PMMLUtils.getNumberOfModels(m_pmmlDoc).keySet()) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(modelType);
        }
        return sb.toString();
    }

    /**
     * This method should no longer be used. The version parameter is ignored.
     * Use {@link #loadFrom(PMMLPortObjectSpec, InputStream)} instead.
    *
    * @param spec the referring spec of this object
    * @param in the input stream to write to
    * @param version the version (3.0 - 3.2)
    * @throws SAXException if something goes wrong during writing
    * @throws ParserConfigurationException if the parser cannot be instantiated
    * @throws IOException if the file cannot be found
    */
    @Deprecated
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream in,
            @SuppressWarnings("unused") final String version)
            throws IOException, ParserConfigurationException, SAXException {
        // Version is ignored and only maintained due to compatibility reasons
        try {
            loadFrom(spec, in);
        } catch (XmlException e) {
            throw new SAXException(e);
        }
    }

    /**
     * Initializes the pmml port object based on the xml input stream.
     * @param spec the referring spec of this object
     * @param is the pmml input stream
     * @throws IOException if the file cannot be found
     * @throws XmlException if something goes wrong during reading
     */
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream is)
            throws IOException, XmlException {
        // disallow close in the factory -- we had indeterministic behavior
        // where close was called more than once (which should be OK) but as
        // the argument input stream is a NonClosableZipInput, which delegates
        // close to closeEntry(), we have to make sure that close is only
        // called once.
        XmlObject xmlDoc = XmlObject.Factory.parse(
                new NonClosableInputStream(is));
        is.close();
        if (xmlDoc instanceof PMMLDocument) {
            m_pmmlDoc = (PMMLDocument)xmlDoc;
        } else {
            /* Try to recover when reading a PMML 3.x document that
             * was produced by KNIME by just replacing the PMML version and
             * namespace. */
            if (PMMLUtils.isOldKNIMEPMML(xmlDoc)) {
                try {
                    String updatedPMML
                            = PMMLUtils.getUpdatedVersionAndNamespace(xmlDoc);
                    /* Parse the modified document and assign it to a
                     * PMMLDocument.*/
                    m_pmmlDoc = PMMLDocument.Factory.parse(updatedPMML);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Parsing of PMML v 3.x document failed.", e);
                }
                LOGGER.info("KNIME produced PMML 3.x  converted to PMML 4.0.");
            } else {
                throw new RuntimeException(
                        "Parsing of PMML v 3.x document failed.");
            }
        }
        m_spec = spec;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public PMMLPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[] {new PMMLPortObjectView(this)};
    }

    /**
     * Adds global transformations to the PMML document. Only DerivedField
     * elements are supported so far. If no global transformations are set so
     * far the dictionary is set as new transformation dictionary, otherwise
     * all contained transformations are appended to the existing one.
     *
     * @param dictionary the transformation dictionary that contains the
     *      transformations to be added
     */
    public void addGlobalTransformations(
            final TransformationDictionary dictionary) {
        // add the transformations to the TransformationDictionary
        if (dictionary.getDefineFunctionArray().length > 0) {
            throw new IllegalArgumentException("DefineFunctions are not "
                    + "supported so far. Only derived fields are allowed.");
        }

        TransformationDictionary dict
            = m_pmmlDoc.getPMML().getTransformationDictionary();
        if (dict == null) {
            m_pmmlDoc.getPMML().setTransformationDictionary(dictionary);
            dict = m_pmmlDoc.getPMML().getTransformationDictionary();
        } else {
            // append the transformations to the existing dictionary
            DerivedField[] existingFields = dict.getDerivedFieldArray();
            DerivedField[] result =
                    appendDerivedFields(existingFields,
                            dictionary.getDerivedFieldArray());
            dict.setDerivedFieldArray(result);
        }
        DerivedField[] df = dict.getDerivedFieldArray();
        List<String> colNames = new ArrayList<String>(df.length);

        Set<String> dfNames = new HashSet<String>();
        for (int i = 0; i < df.length; i++) {
            String derivedName = df[i].getName();
           if (dfNames.contains(derivedName)) {
                throw new IllegalArgumentException("Derived field name \""
                        + derivedName + "\" is not unique.");
           }
           dfNames.add(derivedName);
           String displayName = df[i].getDisplayName();
           colNames.add(displayName == null ? derivedName : displayName);
        }

        /* Remove data fields from data dictionary that where created as a
         * derived field. In KNIME the origin of columns is not distinguished
         * and all columns are added to the data dictionary. But in PMML this
         * results in duplicate entries. Those columns should only appear once
         * as derived field in the transformation dictionary or local
         * transformations. */
        DataDictionary dataDict = m_pmmlDoc.getPMML().getDataDictionary();
        DataField[] dataFieldArray = dataDict.getDataFieldArray();
        List<DataField> dataFields = new ArrayList<DataField>(Arrays.asList(
                dataFieldArray));
        for (DataField dataField : dataFieldArray) {
            if (dfNames.contains(dataField.getName())) {
                dataFields.remove(dataField);
            }
        }
        dataDict.setDataFieldArray(dataFields.toArray(new DataField[0]));
        // update the number of fields
        dataDict.setNumberOfFields(BigInteger.valueOf(dataFields.size()));

        // -------------------------------------------------
        // update field names in the model if applicable
        DerivedFieldMapper dfm = new DerivedFieldMapper(df);
        Map<String, String> derivedFieldMap = dfm.getDerivedFieldMap();
        /* Use XPATH to update field names in the model and move the derived
         * fields to local transformations. */
        PMML pmml = m_pmmlDoc.getPMML();
        if (pmml.getTreeModelArray().length > 0) {
            fixAttributeAtPath(pmml, TREE_PATH, FIELD, derivedFieldMap);
        } else if (pmml.getClusteringModelArray().length > 0) {
            fixAttributeAtPath(pmml, CLUSTERING_PATH, FIELD, derivedFieldMap);
        } else if (pmml.getNeuralNetworkArray().length > 0) {
            fixAttributeAtPath(pmml, NN_PATH, FIELD, derivedFieldMap);
        } else if (pmml.getSupportVectorMachineModelArray().length > 0) {
            fixAttributeAtPath(pmml, SVM_PATH, FIELD, derivedFieldMap);
        } else if (pmml.getRegressionModelArray().length > 0) {
            fixAttributeAtPath(pmml, REGRESSION_PATH_1, FIELD, derivedFieldMap);
            fixAttributeAtPath(pmml, REGRESSION_PATH_2, NAME, derivedFieldMap);
        } else if (pmml.getGeneralRegressionModelArray().length > 0) {
            fixAttributeAtPath(pmml, GR_PATH_1, NAME, derivedFieldMap);
            fixAttributeAtPath(pmml, GR_PATH_2, LABEL, derivedFieldMap);
            fixAttributeAtPath(pmml, GR_PATH_3, PREDICTOR_NAME,
                    derivedFieldMap);
        } // else do nothing as no model exists yet
        // --------------------------------------------------

        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(this,
                m_spec.getDataTableSpec());
        creator.addPreprocColNames(colNames);
        m_spec = creator.createSpec();
    }



    /**
     * @param existingFields the existing field array
     * @param newFields the field array to be appended
     * @return the combined field array
     */
    private DerivedField[] appendDerivedFields(
            final DerivedField[] existingFields,
            final DerivedField[] newFields) {
        if (existingFields == null) {
            return Arrays.copyOf(newFields, newFields.length);
        }
        DerivedField[] result = Arrays.copyOf(existingFields,
                existingFields.length + newFields.length);
        System.arraycopy(newFields, 0, result, existingFields.length,
                newFields.length);
        return result;
    }


    /**
     * @param pmml
     *            the PMML file we want to update
     * @param xpath
     *            the Xpath where we want to perform the update
     * @param attribute
     *            string the attribute we want to update
     * @param derivedFieldMap
     *            a map containing the derived field names for column names
     */
    private void fixAttributeAtPath(final PMML pmml, final String xpath,
            final String attribute, final Map<String, String> derivedFieldMap) {
        for (Map.Entry<String, String> entry : derivedFieldMap.entrySet()) {
            String colName = entry.getKey();
            String mappedName = entry.getValue();
            String fullPath
                    = NAMESPACE_DECLARATION + xpath + colName + PATH_END;
            try {
                XmlObject[] xmlDescendants = pmml.selectPath(fullPath);
                for (XmlObject xo : xmlDescendants) {
                    XmlCursor xmlCursor = xo.newCursor();
                    if (!xmlCursor.isStart()) {
                        throw new RuntimeException(
                             "Could not add transformations to the PMML file.");
                    }
                    xmlCursor
                            .setAttributeText(new QName(attribute), mappedName);
                    xmlCursor.dispose();
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not add transformations to the PMML file.", e);
            }
        }
    }

    /**
     * @return the derived fields defined in the transformation dictionary or
     *      an empty array if no derived fields are defined.
     */
    public DerivedField[] getDerivedFields() {
        return DerivedFieldMapper.getDerivedFields(m_pmmlDoc.getPMML());
    }

    /**
     * Returns the PMML value.
     *
     * @return the pmml value
     */
    public PMMLValue getPMMLValue() {
        try {
            return (PMMLValue)PMMLCellFactory.create(m_pmmlDoc.toString());
        } catch (Exception e) {
            throw new RuntimeException("Could not create PMML value.", e);
        }
    }

    /**
     * Validates that this PMMLPortObject contains a valid PMML document.
     * @throws IllegalStateException if this PMMLPortObject does not contain a
     *      valid PMML document
     */
    public void validate() throws IllegalStateException {
        Map<String, String> errors = PMMLValidator.validatePMML(m_pmmlDoc);
        if (!errors.isEmpty()) {
            StringBuffer sb = new StringBuffer("Invalid PMML document found. "
                    + "Errors: ");
            for (Map.Entry<String, String> entry : errors.entrySet()) {
                String location = entry.getKey();
                String errorMsg = entry.getValue();
                sb.append(location);
                sb.append(": ");
                sb.append(errorMsg);
                sb.append("\n");
            }
            String msg = sb.toString();
            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * @param pmml the PMML document
     * @param version the KNIME version to check
     * @return true, if the passed PMML document was produced with KNIME in a
     *      version older than the passed version.
     */
    public static boolean isKnimeProducedAndOlderThanVersion(
            final PMML pmml, final Integer[] version) {
        Application application = pmml.getHeader().getApplication();
        if (pmml.getHeader() == null
                || !PMMLPortObjectSpec.KNIME.equals(application.getName())) {
            return false;
        }

        return isOlderThanVersion(version, application.getVersion());
    }


    /**
     * @param version  the KNIME version to check against
     * @param appVersion the version string to be checked
     */
    private static boolean isOlderThanVersion(final Integer[] version,
            final String appVersion) {
        if (appVersion != null) {
            try {
                StringTokenizer token = new StringTokenizer(
                        appVersion, ".");
                for (int v  : version) {
                    if (!token.hasMoreTokens()) {
                        /* The parsed version is less specific and therefore
                         * older. */
                        return true;
                    }
                    int parsedRev = Integer.parseInt(token.nextToken());
                    if (parsedRev > v) {
                        return false;
                    } else if (parsedRev < v) {
                        return true;
                    } /* else we have the same version so far and
                        continue */
                }
            } catch (NumberFormatException e) {
               /* An invalid version string is not older. */
            }
        }
        return false;
    }

}
