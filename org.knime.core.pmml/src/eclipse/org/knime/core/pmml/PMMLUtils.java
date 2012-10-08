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
 *
 * History
 *   May 19, 2011 (morent): created
 */

package org.knime.core.pmml;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.dmg.pmml.AssociationModelDocument.AssociationModel;
import org.dmg.pmml.ClusteringModelDocument.ClusteringModel;
import org.dmg.pmml.GeneralRegressionModelDocument.GeneralRegressionModel;
import org.dmg.pmml.MiningModelDocument.MiningModel;
import org.dmg.pmml.MiningSchemaDocument.MiningSchema;
import org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel;
import org.dmg.pmml.NeuralNetworkDocument.NeuralNetwork;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.RegressionModelDocument.RegressionModel;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.SequenceModelDocument.SequenceModel;
import org.dmg.pmml.SupportVectorMachineModelDocument.SupportVectorMachineModel;
import org.dmg.pmml.TextModelDocument.TextModel;
import org.dmg.pmml.TimeSeriesModelDocument.TimeSeriesModel;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class PMMLUtils {
    private static final String PMML_V3_NS_PREFIX = "http://www.dmg.org/PMML-3";
    private static final String PMML_V4_NS = "http://www.dmg.org/PMML-4_0";
    private static final String PMML_V41_NS = "http://www.dmg.org/PMML-4_1";

    private static final String PMML_V41 = "4.1";

    private PMMLUtils() {
        // hiding constructor of utility class
    }

    /**
     * Retrieves the mining schema of the first model of a specific type.
     *
     * @param pmmlDoc the PMML document to extract the mining schema from
     * @param type the type of the model
     * @return the mining schema of the first model of the given type or null if
     *         there is no model of the given type contained in the pmmlDoc
     */
    public static MiningSchema getFirstMiningSchema(final PMMLDocument pmmlDoc,
            final SchemaType type) {
        Map<PMMLModelType, Integer> models = getNumberOfModels(pmmlDoc);
        if (!models.containsKey(PMMLModelType.getType(type))) {
            return null;
        }
        PMML pmml = pmmlDoc.getPMML();
        /*
         * Unfortunately the PMML models have no common base class. Therefore a
         * cast to the specific type is necessary for being able to add the
         * mining schema.
         */
        if (AssociationModel.type.equals(type)) {
            AssociationModel model = pmml.getAssociationModelArray(0);
            return model.getMiningSchema();
        } else if (ClusteringModel.type.equals(type)) {
            ClusteringModel model = pmml.getClusteringModelArray(0);
            return model.getMiningSchema();
        } else if (GeneralRegressionModel.type.equals(type)) {
            GeneralRegressionModel model =
                    pmml.getGeneralRegressionModelArray(0);
            return model.getMiningSchema();
        } else if (MiningModel.type.equals(type)) {
            MiningModel model = pmml.getMiningModelArray(0);
            return model.getMiningSchema();
        } else if (NaiveBayesModel.type.equals(type)) {
            NaiveBayesModel model = pmml.getNaiveBayesModelArray(0);
            return model.getMiningSchema();
        } else if (NeuralNetwork.type.equals(type)) {
            NeuralNetwork model = pmml.getNeuralNetworkArray(0);
            return model.getMiningSchema();
        } else if (RegressionModel.type.equals(type)) {
            RegressionModel model = pmml.getRegressionModelArray(0);
            return model.getMiningSchema();
        } else if (RuleSetModel.type.equals(type)) {
            RuleSetModel model = pmml.getRuleSetModelArray(0);
            return model.getMiningSchema();
        } else if (SequenceModel.type.equals(type)) {
            SequenceModel model = pmml.getSequenceModelArray(0);
            return model.getMiningSchema();
        } else if (SupportVectorMachineModel.type.equals(type)) {
            SupportVectorMachineModel model =
                    pmml.getSupportVectorMachineModelArray(0);
            return model.getMiningSchema();
        } else if (TextModel.type.equals(type)) {
            TextModel model = pmml.getTextModelArray(0);
            return model.getMiningSchema();
        } else if (TimeSeriesModel.type.equals(type)) {
            TimeSeriesModel model = pmml.getTimeSeriesModelArray(0);
            return model.getMiningSchema();
        } else if (TreeModel.type.equals(type)) {
            TreeModel model = pmml.getTreeModelArray(0);
            return model.getMiningSchema();
        } else {
            return null;
        }
    }

    /**
     * @param pmmlDoc the PMML document to retrieve the model information for
     * @return a map containing the number of contained models for each PMML
     *         model type
     */
    public static Map<PMMLModelType, Integer> getNumberOfModels(
            final PMMLDocument pmmlDoc) {
        Map<PMMLModelType, Integer> numModels =
                new LinkedHashMap<PMMLModelType, Integer>();
        PMML pmml = pmmlDoc.getPMML();
        int num = pmml.sizeOfAssociationModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.AssociationModel, num);
        }
        num = pmml.sizeOfClusteringModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.ClusteringModel, num);
        }
        num = pmml.sizeOfGeneralRegressionModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.GeneralRegressionModel, num);
        }
        num = pmml.sizeOfMiningModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.MiningModel, num);
        }
        num = pmml.sizeOfNaiveBayesModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.NaiveBayesModel, num);
        }
        num = pmml.sizeOfNeuralNetworkArray();
        if (num > 0) {
            numModels.put(PMMLModelType.NeuralNetwork, num);
        }
        num = pmml.sizeOfRegressionModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.RegressionModel, num);
        }
        num = pmml.sizeOfRuleSetModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.RuleSetModel, num);
        }
        num = pmml.sizeOfSequenceModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.SequenceModel, num);
        }
        num = pmml.sizeOfSupportVectorMachineModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.SupportVectorMachineModel, num);
        }
        num = pmml.sizeOfTextModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.TextModel, num);
        }
        num = pmml.sizeOfTimeSeriesModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.TimeSeriesModel, num);
        }
        num = pmml.sizeOfTreeModelArray();
        if (num > 0) {
            numModels.put(PMMLModelType.TreeModel, num);
        }
        return numModels;
    }

    /**
     * Update the namespace and version of the document to PMML 4.1.
     *
     * @param document the document to update the namespace
     */
    public static void updatePmmlNamespaceAndVersion(final Document document) {
        Element root = document.getDocumentElement();
        String rootPrefix = root.getPrefix();
        fixNamespace(document, root, rootPrefix);
        NodeList nodeList = document.getElementsByTagName("PMML");
        Node pmmlNode = nodeList.item(0);
        if (pmmlNode == null) {
            throw new RuntimeException(
                    "Invalid PMML document without a PMML element encountered.");
        }
        Node version = pmmlNode.getAttributes().getNamedItem("version");
        version.setNodeValue(PMML_V41);
    }

    /**
     * Update the namespace and version of the document to PMML 4.1.
     *
     * @param xmlDoc the {@link XmlObject} to update the namespace
     * @return the updated PMML
     */
    public static String getUpdatedVersionAndNamespace(final XmlObject xmlDoc) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            String encoding = xmlDoc.documentProperties().getEncoding();
            if (encoding == null) {
                // use utf-8 as default encoding if none is set
                encoding = "UTF-8";
            }
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(xmlDoc.xmlText()
                            .getBytes(encoding));
            Document doc = builder.parse(inputStream);
            inputStream.close();
            updatePmmlNamespaceAndVersion(doc);
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            writer.flush();
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not update PMML document.", e);
        }
    }

    /**
     * Fixes the namespace of an element and all its descendants.
     *
     * @param document the document to update the namespace
     * @param element the node to start updating at
     * @param prefix the prefix used
     */
    private static void fixNamespace(final Document document,
            final Element element, final String prefix) {
        NamedNodeMap atts = element.getAttributes();
        for (int i = 0; i < atts.getLength(); i++) {
            Node att = atts.item(i);
            if ((att.getNodeName().equals("xmlns") || att.getNodeName()
                    .startsWith("xmlns:"))
                    && (att.getNodeValue().startsWith(PMML_V3_NS_PREFIX)
                            || att.getNodeValue().equals(PMML_V4_NS))) {
                att.setNodeValue(PMML_V41_NS);
            } else if (att.getNodeName().equals("xsi:schemaLocation")
                    && (att.getNodeValue().startsWith(PMML_V3_NS_PREFIX)
                            || att.getNodeValue().equals(PMML_V4_NS))) {
                element.removeAttribute(att.getNodeName());
            }
        }

        Node child = element.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                fixNamespace(document, (Element)child, prefix);
            }
            child = child.getNextSibling();
        }
    }

    /**
     * @param xmlDoc the {@link XmlObject} to verify
     * @return true if the xmlDoc is a 3.x or 4.0 PMML document produced by
     *      KNIME
     */
    public static boolean isOldKNIMEPMML(final XmlObject xmlDoc) {
        // Insert a cursor and move it to the PMML element.
        XmlCursor pmmlCursor = xmlDoc.newCursor();
        pmmlCursor.toFirstChild(); // the PMML element
        String xmlns = "";
        String version = "";
        while (!pmmlCursor.toNextToken().isNone() && pmmlCursor.isAnyAttr()) {
            String name = pmmlCursor.getName().getLocalPart();
            if (pmmlCursor.currentTokenType().isNamespace() && name.isEmpty()) {
                // the default namespace
                xmlns = pmmlCursor.getTextValue();
            } else if ("version".equals(name)) {
                // the version attribute
                version = pmmlCursor.getTextValue();
            }
        }
        pmmlCursor.dispose();

        String application = "";
        XmlCursor appCursor = xmlDoc.newCursor();
        String query =
                "declare namespace p='" + xmlns + "';" + "$this//p:Application";
        appCursor.selectPath(query);
        if (appCursor.hasNextSelection()) {
            appCursor.toNextSelection();
            application = appCursor.getAttributeText(new QName("name"));
        }
        appCursor.dispose();
        return (version.startsWith("3.") || "4.0".equals(version))
                && ("KNIME".equals(application) // learned by KNIME
                        || "Rattle/PMML".equals(application)); // learned in R
    }


//    /** Static initialization of all expressions needed for XPath.*/
//    private static final String NAMESPACE_DECLARATION =
//            "declare namespace pmml='http://www.dmg.org/PMML-4_0';";
//    private static final String PATH_END = "']";
//    private static final String FIELD = "field";
//    private static final String NAME = "name";
//    private static final String LABEL = "label";
//    private static final String PREDICTOR_NAME = "predictorName";
//    private static final String TREE_PATH =
//            "./pmml:TreeModel/descendant::pmml:Node/descendant::*[@field='";
//    private static final String CLUSTERING_PATH =
//            "./pmml:ClusteringModel/pmml:ClusteringField[@field='";
//    private static final String NN_PATH =
//            "./pmml:NeuralNetwork/pmml:NeuralInputs/pmml:NeuralInput"
//            + "/pmml:DerivedField/descendant::*[@field='";
//    private static final String SVM_PATH =
//            "./pmml:SupportVectorMachineModel/pmml:VectorDictionary/"
//            + "pmml:VectorFields/pmml:FieldRef[@field='";
//    private static final String REGRESSION_PATH_1 =
//           "./pmml:RegressionModel/pmml:RegressionTable/descendant::*[@field='";
//    private static final String REGRESSION_PATH_2 =
//            "./pmml:RegressionModel/pmml:RegressionTable/descendant::*[@name='";
//    private static final String GR_PATH_1 =
//            "./pmml:GeneralRegressionModel/*/pmml:Predictor[@name='";
//    private static final String GR_PATH_2 =
//            "./pmml:GeneralRegressionModel/pmml:ParameterList/"
//            + "pmml:Parameter[@label='";
//    private static final String GR_PATH_3 =
//            "./pmml:GeneralRegressionModel/pmml:PPMatrix/"
//            + "pmml:PPCell[@predictorName='";
//    /* ------------------------------------------------------ */
//
//    public static void fixPMMLModelReferences(final PMML pmml,
//            final List<String> colNames,
//            final Map<String, String> derivedNames) {
//        /* Use XPATH to update field names in the model. */
//            if (pmml.getTreeModelArray().length > 0) {
//                fixAttributeAtPath(pmml, TREE_PATH, FIELD, colNames,
//                        derivedNames);
//            } else if (pmml.getClusteringModelArray().length > 0) {
//                fixAttributeAtPath(pmml, CLUSTERING_PATH, FIELD, colNames,
//                        derivedNames);
//            } else if (pmml.getNeuralNetworkArray().length > 0) {
//                fixAttributeAtPath(pmml, NN_PATH, FIELD, colNames, derivedNames);
//            } else if (pmml.getSupportVectorMachineModelArray().length > 0) {
//                fixAttributeAtPath(pmml, SVM_PATH, FIELD, colNames,
//                        derivedNames);
//            } else if (pmml.getRegressionModelArray().length > 0) {
//                fixAttributeAtPath(pmml, REGRESSION_PATH_1, FIELD, colNames,
//                        derivedNames);
//                fixAttributeAtPath(pmml, REGRESSION_PATH_2, NAME, colNames,
//                        derivedNames);
//            } else if (pmml.getRegressionModelArray().length > 0) {
//                fixAttributeAtPath(pmml, GR_PATH_1, NAME, colNames,
//                        derivedNames);
//                fixAttributeAtPath(pmml, GR_PATH_2, LABEL, colNames,
//                        derivedNames);
//                fixAttributeAtPath(pmml, GR_PATH_3, PREDICTOR_NAME, colNames,
//                        derivedNames);
//            }
//    }
//
//    /**
//     * @param pmml
//     *            the PMML file we want to update
//     * @param xpath
//     *            the Xpath where we want to perform the update
//     * @param attribute
//     *            string the attribute we want to update
//     * @param the
//     *            list of column names we need to update
//     * @param dfm
//     *            a derivedFieldMapper
//     */
//    private static void fixAttributeAtPath(final PMML pmml, final String xpath,
//            final String attribute, final List<String> colNames,
//            final Map<String, String> dfm) {
//        for (String colName : colNames) {
//            String mappedName = dfm.get(colName);
//            String fullPath
//                    = NAMESPACE_DECLARATION + xpath + colName + PATH_END;
//            try {
//                XmlObject[] xmlDescendants = pmml.selectPath(fullPath);
//                for (XmlObject xo : xmlDescendants) {
//                    XmlCursor xmlCursor = xo.newCursor();
//                    if (!xmlCursor.isStart()) {
//                        throw new RuntimeException(
//                             "Could not add transformations to the PMML file.");
//                    }
//                    xmlCursor
//                            .setAttributeText(new QName(attribute), mappedName);
//                    xmlCursor.dispose();
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(
//                        "Could not add transformations to the PMML file.", e);
//            }
//        }
//    }
}
