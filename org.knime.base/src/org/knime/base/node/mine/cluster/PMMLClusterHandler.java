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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLClusterHandler extends PMMLContentHandler {

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

    // number of clusters <ClusterModel numberOfClusters="">
    // prototypes <Cluster><Array>values </Array>
    // with name <Cluster name="...">
    // String[] usedColumns <ClusteringField field="">
    // int[] clusterCoverage -> <Cluster size="">

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLClusterHandler.class);

    /*
     * Conformance:
     * ComparisonMeasure -> euclidean although only squaredEuclidean is in core
     *
     * Compare function -> absDiff (absolute distance)
     */

    private ComparisonMeasure m_measure;

    private static final Set<String>UNSUPPORTED
        = new LinkedHashSet<String>();

    private static final Set<String>IGNORED
        = new LinkedHashSet<String>();

    private static final Set<String>KNOWN = new LinkedHashSet<String>();

    static {
        UNSUPPORTED.add("KohonenMap");
        UNSUPPORTED.add("LocalTransformations");

        IGNORED.add("Covariances");
        IGNORED.add("MissingValueWeights");

        KNOWN.add("PMML");
        KNOWN.add("Header");
        KNOWN.add("Application");
        KNOWN.add("DataDictionary");
        KNOWN.add("DataField");
        KNOWN.add("MiningSchema");
        KNOWN.add("Value");
        KNOWN.add("Interval");
    }

    private int m_nrOfClusters;
    private double[][] m_prototypes;
    private String[] m_labels;
    private int[] m_clusterCoverage;

   private Set<String> m_usedColumns = new LinkedHashSet<String>();

    private StringBuffer m_buffer;
    private int m_currentCluster = 0;

    private Stack<String>m_elementStack = new Stack<String>();

    private String m_lastDerivedField;

    private Map<String, LinearNorm>m_linearNorms;
    private LinearNorm m_currentLinearNorm;

    

    public PMMLClusterHandler(ComparisonMeasure measure, int nrOfClusters, 
    		double[][] prototypes,  int[] clusterCoverage, 
    		Set<String> colSpecs) {
    	  m_measure = measure;

    	    m_nrOfClusters = nrOfClusters;
    	   m_prototypes = prototypes;
    	 
    	   m_clusterCoverage = clusterCoverage;

    	    
    	    m_usedColumns = new LinkedHashSet<String>();
    	    for(String dcs : colSpecs){
    	    	m_usedColumns.add(dcs);
    	    }
            
            m_labels = new String[m_nrOfClusters];
            for (int i = 0; i < m_nrOfClusters; i++) {
                m_labels[i] = "cluster_" + i;
            }
            m_prototypes = prototypes;
            for (int i = 0; i < prototypes.length; i++) {
                m_prototypes[i] = prototypes[i];
            }
	}

    public PMMLClusterHandler() {
		// nothing to do.
	}

	/**
     *
     * @return number of clusters
     */
    public int getNrOfClusters() {
        return m_nrOfClusters;
    }

    /**
     *
     * @return the prototypes
     */
    public double[][] getPrototypes() {
        return m_prototypes;
    }

    /**
     *
     * @return labels of the cluster in same order as prototypes
     */
    public String[] getLabels() {
        return m_labels;
    }

    /**
     *
     * @return the number of items in the cluster
     */
    public int[] getClusterCoverage() {
        return m_clusterCoverage;
    }

    /**
     *
     * @return list of used columns
     */
    public Set<String> getUsedColumns() {
        return m_usedColumns;
    }

    /**
     *
     * @return the used comparison measure
     */
    public ComparisonMeasure getComparisonMeasure() {
        return m_measure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // necessary for inside ArrayElement
        if (m_buffer == null) {
            m_buffer = new StringBuffer();
        }
        m_buffer.append(ch, start, length);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        // clear buffer and stuff
        m_buffer = null;
        m_currentCluster = 0;
        m_lastDerivedField = null;

        if (m_linearNorms != null) {
            for (int clusterNr = 0; clusterNr < m_prototypes.length;
                clusterNr++) {
                int columnIndex = 0;
                // if there are some normalized columns - unnormalize them
                for (String col : m_usedColumns) {
                    LinearNorm linearNorm = m_linearNorms.get(col);
                    if (linearNorm != null) {
                        // normalize
                        LOGGER.warn(
                                "De-normalizing prototype value for column \""
                                + col + "\"");
                        m_prototypes[clusterNr][columnIndex] =
                                linearNorm.unnormalize(
                                        m_prototypes[clusterNr][columnIndex]);
                    } // else leave prototype as is
                    columnIndex++;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        // if Array -> read buffer out
        m_elementStack.pop();
        if (name.equals("Array") && m_elementStack.peek().equals("Cluster")) {
            String[] coords = m_buffer.toString().trim().split(" ");
            // TODO -> check if coord.length == m_usedColumns.length
            double[] protoCoords = new double[m_usedColumns.size()];
            for (int i = 0; i < m_usedColumns.size(); i++) {
                protoCoords[i] = Double.parseDouble(coords[i]);
            }
            m_prototypes[m_currentCluster] = protoCoords;
        }
        if (name.equals("Cluster")
                && m_elementStack.peek().equals("ClusteringModel")) {
            m_currentCluster++;
        }
        if (name.equals("NormContinuous")) {
            // initialize only when necessary
            if (m_linearNorms == null) {
                m_linearNorms = new LinkedHashMap<String, LinearNorm>();
            }
            // element NormContinuos put on map
            m_linearNorms.put(m_currentLinearNorm.getName(),
                    m_currentLinearNorm);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name,
            final Attributes atts) throws SAXException {
        // ensure to throw exception on method distribution/kohonen/etc.
        if (UNSUPPORTED.contains(name)) {
            throw new IllegalArgumentException(
                    "Element " + name + " is not supported!");
        }
        if (IGNORED.contains(name)) {
            LOGGER.warn("Element " + name + " is ignored.");
        }
        // if Array -> open buffer
        if (name.equals("Array") && m_elementStack.peek().equals("Cluster")) {
            m_buffer = new StringBuffer();
        } else if (name.equals("ClusteringModel")) {
            // only center-based is supported
            String modelClass = atts.getValue("modelClass");
            if (!modelClass.equals(
                    "centerBased")) {
                throw new IllegalArgumentException(
                        "Only centerBased clustering models are supported!"
                        + " Found modelClass " + modelClass);
            }
            // check if the compare function is euclidean
            // if ClusterModel -> retrieve number of clusters
            m_nrOfClusters = Integer.parseInt(
                    atts.getValue("numberOfClusters"));
            m_prototypes = new double[m_nrOfClusters][m_usedColumns.size()];
            m_labels = new String[m_nrOfClusters];
            m_clusterCoverage = new int[m_nrOfClusters];
        } else if (name.equals("Cluster")
                && m_elementStack.peek().equals("ClusteringModel")) {
            // Cluster -> retrieve name
            m_labels[m_currentCluster] = atts.getValue("name");
            if (atts.getValue("size") != null) {
                m_clusterCoverage[m_currentCluster] = Integer.parseInt(
                        atts.getValue("size"));
            }
        } else if (name.equals("ClusteringField")) {
            // if ClusteringField -> retrieve field
            // check if attribute usageType is available
            if (atts.getValue("usageType") != null) {
                // if yes check if it is "active"
                if (atts.getValue("usageType").equals("active")) {
                    m_usedColumns.add(atts.getValue("field"));
                }
            } else {
                m_usedColumns.add(atts.getValue("field"));
            }
            // some models do not have ClusteringField but MiningField
        } else if (name.equals("MiningField")) {
            // but then it must be of usageType=active
            if (atts.getValue("usageType") != null
                    && atts.getValue("usageType").equals("active")) {
                // since we have a set this should not mess up with the
                // clustering fields
                // (if both clustering and mining fields are defined)
                m_usedColumns.add(atts.getValue("name"));
            }
        } else if (name.equals("NormContinuous")) {
            String fieldName = atts.getValue("field");
            m_lastDerivedField = fieldName;
        } else if (name.equals("LinearNorm")) {
            if (m_currentLinearNorm == null) {
                m_currentLinearNorm = new LinearNorm(m_lastDerivedField);
            }
            if (!m_currentLinearNorm.getName().equals(m_lastDerivedField)) {
                m_currentLinearNorm = new LinearNorm(m_lastDerivedField);
            }
            double val = Double.parseDouble(atts.getValue("orig"));
            double norm = Double.parseDouble(atts.getValue("norm"));
            m_currentLinearNorm.addInterval(val, norm);
        } else if (name.equals("ComparisonMeasure")) {
            // only absolute difference as compare function is supported
            String compareFunction = atts.getValue("compareFunction");
            // set default values here - because we do not validate against
            // schema
            if (compareFunction == null) {
               compareFunction = "absDiff";
            }
            if (!compareFunction.equals("absDiff")) {
                throw new IllegalArgumentException(
                        "Only the absolute difference (\"absDiff\") as "
                        + "compare function is supported!");
            }
        } else if (!m_elementStack.isEmpty()
                && m_elementStack.peek().equals("ComparisonMeasure")) {
            if (!name.trim().equals(
                    ComparisonMeasure.euclidean.name())
                    && !name.trim().equals(
                            ComparisonMeasure.squaredEuclidean.name())) {
                throw new IllegalArgumentException(
                        "\"" + ComparisonMeasure.euclidean
                        + "\" and \""
                        + ComparisonMeasure.squaredEuclidean
                        + "\" are the only supported comparison "
                        + "measures! Found " + name + ".");
            } else {
                m_measure = ComparisonMeasure.valueOf(name);
            }

        } else if (!KNOWN.contains(name)) {
            LOGGER.warn("Skipping unknown element " + name);
        }
        m_elementStack.push(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getSupportedVersions() {
        TreeSet<String> versions = new TreeSet<String>();
        versions.add(PMMLPortObject.PMML_V3_0);
        versions.add(PMMLPortObject.PMML_V3_1);
        versions.add(PMMLPortObject.PMML_V3_2);
        return versions;
    }

   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void addPMMLModelContent(final TransformerHandler handler,
            final PMMLPortObjectSpec spec) throws SAXException {
        
        // create the cluster model
        // with the attributes...
        AttributesImpl atts = new AttributesImpl();
        // modelName
        atts.addAttribute(null, null, "modelName", 
        		PMMLPortObject.CDATA, "k-means");
        // functionName
        atts.addAttribute(null, null, "functionName", 
        		PMMLPortObject.CDATA, "clustering");
        // modelClass
        atts.addAttribute(null, null, "modelClass", 
        		PMMLPortObject.CDATA, "centerBased");
        // numberOfClusters
        atts.addAttribute(null, null, "numberOfClusters", PMMLPortObject.CDATA,
                "" + m_nrOfClusters);
        handler.startElement(null, null, "ClusteringModel", atts);
        if (m_usedColumns != null) {
            PMMLPortObjectSpec.writeMiningSchema(spec, handler);
            // TODO write the local transformtions
//            writeLocalTransformations(handler);
            addUsedDistanceMeasure(handler);
            addClusteringFields(handler);
            addClusters(handler);
        }
        handler.endElement(null, null, "ClusteringModel");
    }
    
    /**
     * Writes the used distance measure - so far it is euclidean.
     * @param handler to write to
     * @throws SAXException if something goes wrong
     */
    protected void addUsedDistanceMeasure(
    		final TransformerHandler handler)
        throws SAXException {
        // add kind="distance" to ComparisonMeasure element
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "kind", PMMLPortObject.CDATA, "distance");
        handler.startElement(null, null, "ComparisonMeasure", atts);
        // for now hard-coded squared euclidean
        handler.startElement(null, null,
                m_measure.name(), null);
        handler.endElement(null, null, m_measure.name());
        handler.endElement(null, null, "ComparisonMeasure");
    }

    /**
     * Writes the clustering fields (name).
     *
     * @param handler to write to
     * @throws SAXException if something goes wrong
     */
    protected void addClusteringFields(final TransformerHandler handler
    				) throws SAXException {
        for (String colName : m_usedColumns) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, 
            		"field", PMMLPortObject.CDATA, colName);
            atts.addAttribute(null, null, 
            		"compareFunction", PMMLPortObject.CDATA, "absDiff");
            handler.startElement(null, null, "ClusteringField", atts);
            handler.endElement(null, null, "ClusteringField");
        }
    }

    /**
     * Writes the actual cluster prototypes.
     *
     * @param handler to write to
     * @throws SAXException if something goes wrong
     */
    protected void addClusters(final TransformerHandler handler
            ) throws SAXException {
        int i = 0;
        for (double[] prototype : m_prototypes) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "name", 
            		PMMLPortObject.CDATA, "cluster_" + i);
            if (m_clusterCoverage != null
                    && m_clusterCoverage.length == m_prototypes.length) {
            atts.addAttribute(null, null, "size", PMMLPortObject.CDATA,
                    "" + m_clusterCoverage[i]);
            }
            i++;
            handler.startElement(null, null, "Cluster", atts);
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "n", PMMLPortObject.CDATA,
                            "" + prototype.length);
                atts.addAttribute(null, null, "type", 
                		PMMLPortObject.CDATA, "real");
                handler.startElement(null, null, "Array", atts);
                StringBuffer buff = new StringBuffer();
                for (double d : prototype) {
                    buff.append(d + " ");
                }
                char[] chars = buff.toString().toCharArray();
                handler.characters(chars, 0, chars.length);
                handler.endElement(null, null, "Array");
            handler.endElement(null, null, "Cluster");
        }
    }
}
