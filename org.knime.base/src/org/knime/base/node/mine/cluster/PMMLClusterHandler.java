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
package org.knime.base.node.mine.cluster;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.knime.base.node.mine.cluster.PMMLClusterPortObject.ComparisonMeasure;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLClusterHandler extends PMMLContentHandler {
    
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
        UNSUPPORTED.add("Covariances");
        
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
                // initialize only when necessary
                if (m_linearNorms == null) {
                    m_linearNorms = new LinkedHashMap<String, LinearNorm>();
                }
                m_linearNorms.put(m_currentLinearNorm.getName(), 
                        m_currentLinearNorm);
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

}
