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
package org.knime.base.node.mine.cluster;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.dmg.pmml.ArrayType;
import org.dmg.pmml.ArrayType.Type;
import org.dmg.pmml.COMPAREFUNCTION;
import org.dmg.pmml.ClusterDocument;
import org.dmg.pmml.ClusteringFieldDocument;
import org.dmg.pmml.ClusteringFieldDocument.ClusteringField;
import org.dmg.pmml.ClusteringModelDocument;
import org.dmg.pmml.ClusteringModelDocument.ClusteringModel;
import org.dmg.pmml.ClusteringModelDocument.ClusteringModel.ModelClass;
import org.dmg.pmml.ComparisonMeasureDocument;
import org.dmg.pmml.ComparisonMeasureDocument.ComparisonMeasure.Kind;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * A Cluster translator class between KNIME and PMML.
 *
 * Modified from the original PMMLClusterHandler.java by Fabian Dill.
 *
 * @author wenlin, Zementis, Apr 2011
 *
 */
public class PMMLClusterTranslator implements PMMLTranslator {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLClusterTranslator.class);

    private static final String BACKSLASH = "\\";

    private static final String DOUBLE_QUOT = "\"";

    private static final String SPACE = " ";

    private static final String TAB = "\t";

    private ComparisonMeasure m_measure;
    private int m_nrOfClusters;
    private double[][] m_prototypes;
    private String[] m_labels;
    private int[] m_clusterCoverage;
    private Set<String> m_usedColumns = new LinkedHashSet<String>();

    /**
     * Constants indicating whether the squared euclidean or the euclidean
     * comparison measure should be used.
     */
    public enum ComparisonMeasure {
        /** Squared Euclidean distance. */
        squaredEuclidean,
        /** Euclidean distance. */
        euclidean
    }

    /*
     * Conformance: ComparisonMeasure -> euclidean although only
     * squaredEuclidean is in core
     */

    /**
     * Constructor. Pass the KNIME cluster information to the translator.
     *
     * @param measure
     *            the comparison measure
     * @param nrOfClusters
     *            number of clusters
     * @param prototypes
     *            the clusters
     * @param clusterCoverage
     *            the size of clusters
     * @param colSpecs
     *            the fields used as cluster coordinates
     */
    public PMMLClusterTranslator(final ComparisonMeasure measure,
            final int nrOfClusters, final double[][] prototypes,
            final int[] clusterCoverage, final Set<String> colSpecs) {
        m_measure = measure;
        m_nrOfClusters = nrOfClusters;
        m_prototypes = prototypes;
        m_clusterCoverage = clusterCoverage;

        m_usedColumns = new LinkedHashSet<String>();
        for (String dcs : colSpecs) {
            m_usedColumns.add(dcs);
        }

        m_labels = new String[m_nrOfClusters];
        for (int i = 0; i < m_nrOfClusters; i++) {
            m_labels[i] = "cluster_" + i;
        }
    }

    /**
     * Constructor.
     *
     * Create an empty translator, leaving required cluster model information
     * uninitialized.
     */
    public PMMLClusterTranslator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        PMML pmml = pmmlDoc.getPMML();
        DerivedFieldMapper mapper = new DerivedFieldMapper(pmmlDoc);
        ClusteringModelDocument.ClusteringModel pmmlClusteringModel =
            pmml.getClusteringModelArray(0);

        // ---------------------------------------------------
        // initialize ClusteringFields
        for (ClusteringField cf
                : pmmlClusteringModel.getClusteringFieldArray()) {
            m_usedColumns.add(mapper.getColumnName(cf.getField()));
            if (COMPAREFUNCTION.ABS_DIFF != cf.getCompareFunction()) {
                LOGGER.error("Comparison Function "
                        + cf.getCompareFunction().toString()
                        + " is not supported!");
                throw new IllegalArgumentException(
                        "Only the absolute difference (\"absDiff\") as "
                        + "compare function is supported!");
            }
        }

        // ---------------------------------------------------
        // initialize Clusters
        m_nrOfClusters = pmmlClusteringModel.sizeOfClusterArray();
        m_prototypes = new double[m_nrOfClusters][m_usedColumns.size()];
        m_labels = new String[m_nrOfClusters];

        m_clusterCoverage = new int[m_nrOfClusters];
        for (int i = 0; i < m_nrOfClusters; i++) {
            ClusterDocument.Cluster currentCluster =
                pmmlClusteringModel.getClusterArray(i);
            m_labels[i] = currentCluster.getName();
            // in KNIME learner: m_labels[i] = "cluster_" + i;
            ArrayType clusterArray = currentCluster.getArray();
            String content = clusterArray.newCursor().getTextValue();
            String[] stringValues;
            content = content.trim();

            if (content.contains(DOUBLE_QUOT)) {
                content = content.replace(BACKSLASH + DOUBLE_QUOT, TAB);
                /* TODO We need to take care of the cases with double quots,
                 * e.g ==> <Array n="3" type="string">"Cheval  Blanc" "TABTAB"
                 "Latour"</Array> */
                stringValues = content.split(DOUBLE_QUOT + SPACE);
                for (int j = 0; j < stringValues.length; j++) {
                    stringValues[j] = stringValues[j].replace(DOUBLE_QUOT, "");
                    stringValues[j] = stringValues[j].replace(TAB, DOUBLE_QUOT);
                    stringValues[j] = stringValues[j].trim();
                }
            } else {
                stringValues = content.split("\\s+");
            }
            for (int j = 0; j < m_usedColumns.size(); j++) {
                m_prototypes[i][j] = Double.valueOf(stringValues[j]);
            }

            if (currentCluster.isSetSize()) {
                m_clusterCoverage[i] = currentCluster.getSize().intValue();
            }
        }

        if (pmmlClusteringModel.isSetMissingValueWeights()) {
            ArrayType weights =
                pmmlClusteringModel.getMissingValueWeights().getArray();
            String content = weights.newCursor().getTextValue();
            String[] stringValues;
            Double[] weightValues;
            content = content.trim();

            if (content.contains(DOUBLE_QUOT)) {
                content = content.replace(BACKSLASH + DOUBLE_QUOT, TAB);
                /* TODO We need to take care of the cases with double quots,
                 * e.g ==> <Array n="3" type="string">"Cheval  Blanc" "TABTAB"
                 "Latour"</Array> */
                stringValues = content.split(DOUBLE_QUOT + SPACE);
                weightValues = new Double[stringValues.length];
                for (int j = 0; j < stringValues.length; j++) {
                    stringValues[j] = stringValues[j].replace(DOUBLE_QUOT, "");
                    stringValues[j] = stringValues[j].replace(TAB, DOUBLE_QUOT);
                    stringValues[j] = stringValues[j].trim();
                    weightValues[j] = Double.valueOf(stringValues[j]);
                    if (weightValues[j] == null
                            || weightValues[j].doubleValue() != 1.0) {
                        String msg = "Missing Value Weight not equals one"
                                    + " is not supported!";
                        LOGGER.error(msg);
                    }
                }
            } else {
                stringValues = content.split("\\s+");
            }

        }
        // ------------------------------------------
        // initialize m_usedColumns from ClusteringField
        ClusteringFieldDocument.ClusteringField[] clusteringFieldArray =
            pmmlClusteringModel.getClusteringFieldArray();
        for (ClusteringField cf : clusteringFieldArray) {
            m_usedColumns.add(mapper.getColumnName(cf.getField()));
        }

        // --------------------------------------------
        // initialize Comparison Measure
        ComparisonMeasureDocument.ComparisonMeasure pmmlComparisonMeasure =
            pmmlClusteringModel.getComparisonMeasure();
        if (pmmlComparisonMeasure.isSetSquaredEuclidean()) {
            m_measure = ComparisonMeasure.squaredEuclidean;
        } else if (pmmlComparisonMeasure.isSetEuclidean()) {
            m_measure = ComparisonMeasure.euclidean;
        } else {
            String measure =
                pmmlComparisonMeasure.getDomNode().getFirstChild()
                        .getNodeName();
            throw new IllegalArgumentException(
                    "\"" + ComparisonMeasure.euclidean + "\" and \""
                    + ComparisonMeasure.squaredEuclidean
                    + "\" are the only supported comparison "
                    + "measures! Found " + measure + ".");
        }

        if (Kind.SIMILARITY == pmmlComparisonMeasure.getKind()) {
            LOGGER.error("A Similarity Kind of Comparison Measure is not "
                    + "supported!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final PMMLPortObjectSpec spec) {
        DerivedFieldMapper mapper = new DerivedFieldMapper(pmmlDoc);

        PMML pmml = pmmlDoc.getPMML();
        ClusteringModelDocument.ClusteringModel clusteringModel =
            pmml.addNewClusteringModel();
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, clusteringModel);

        // ---------------------------------------------------
        // set clustering model attributes
        clusteringModel.setModelName("k-means");
        clusteringModel.setFunctionName(MININGFUNCTION.CLUSTERING);
        clusteringModel.setModelClass(ModelClass.CENTER_BASED);
        clusteringModel.setNumberOfClusters(BigInteger.valueOf(m_nrOfClusters));

        // ---------------------------------------------------
        // set comparison measure
        ComparisonMeasureDocument.ComparisonMeasure pmmlComparisonMeasure =
            clusteringModel.addNewComparisonMeasure();
        pmmlComparisonMeasure.setKind(Kind.DISTANCE);
        if (ComparisonMeasure.squaredEuclidean.equals(m_measure)) {
            pmmlComparisonMeasure.addNewSquaredEuclidean();
        } else {
            pmmlComparisonMeasure.addNewEuclidean();
        }

        // ---------------------------------------------------
        // set clustering fields
        for (String colName : m_usedColumns) {
            ClusteringFieldDocument.ClusteringField pmmlClusteringField =
                clusteringModel.addNewClusteringField();
            pmmlClusteringField.setField(mapper.getDerivedFieldName(colName));
            pmmlClusteringField.setCompareFunction(COMPAREFUNCTION.ABS_DIFF);
        }

        // ----------------------------------------------------
        // set clusters
        int i = 0;
        for (double[] prototype : m_prototypes) {
            ClusterDocument.Cluster pmmlCluster =
                clusteringModel.addNewCluster();

            String name = "cluster_" + i;
            pmmlCluster.setName(name);
            if (m_clusterCoverage != null
                    && m_clusterCoverage.length == m_prototypes.length) {
                pmmlCluster.setSize(BigInteger.valueOf(m_clusterCoverage[i]));
            }
            i++;

            ArrayType pmmlArray = pmmlCluster.addNewArray();
            pmmlArray.setN(BigInteger.valueOf(prototype.length));
            pmmlArray.setType(Type.REAL);

            StringBuffer buff = new StringBuffer();
            for (double d : prototype) {
                buff.append(d + " ");
            }
            XmlCursor xmlCursor = pmmlArray.newCursor();
            xmlCursor.setTextValue(buff.toString());
            xmlCursor.dispose();
        }
        return ClusteringModel.type;
    }

    /**
     * @return the measure
     */
    public ComparisonMeasure getComparisonMeasure() {
        return m_measure;
    }

    /**
     * @return the labels
     */
    public String[] getLabels() {
        return m_labels;
    }

    /**
     * @return the prototypes
     */
    public double[][] getPrototypes() {
        return m_prototypes;
    }

    /**
     * @return the usedColumns
     */
    public Set<String> getUsedColumns() {
        return m_usedColumns;
    }
}
