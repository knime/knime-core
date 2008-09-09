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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
    
    
    private static final Set<String>notSupportedElements 
        = new LinkedHashSet<String>();
    
    static {
        notSupportedElements.add("KohonenMap");
        notSupportedElements.add("Covariances");
        notSupportedElements.add(" ");
    }
    
    private int m_nrOfClusters;
    private double[][] m_prototypes;
    private String[] m_labels;
    private int[] m_clusterCoverage;
    private Set<String> m_usedColumns = new LinkedHashSet<String>();
    
    private StringBuffer m_buffer;
    private int m_currentCluster = 0;
    
    private Stack<String>m_elementStack = new Stack<String>(); 
    
    private Map<String, double[]>m_normValues = new HashMap<String, double[]>();
    private String m_lastDerivedField;
    
    private double[] m_mins;
    private double[] m_maxs;
    
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
        m_mins = new double[m_usedColumns.size()];
        m_maxs = new double[m_usedColumns.size()];
        int i = 0;
        for (String col : m_usedColumns) {
            double[] vals = m_normValues.get(col);
            if (vals != null) {
                // only columns with left and right margin 
                // or columns of type double and values 
                // have min and max values
                m_mins[i] = vals[0];
                m_maxs[i] = vals[1];
            } else {
                // fields can not be normalized
                // then we put Double.NaN in order to signalize the predictor
                // that unnormalization is not necessary
                m_mins[i] = Double.NaN;
                m_maxs[i] = Double.NaN;             
            }
            i++;
        }
        m_normValues.clear();
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
        //TODO: -> ensure to throw exception on method distribution/kohonen/etc.
        
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
            m_lastDerivedField = atts.getValue("field");
        } else if (name.equals("LinearNorm")) {
            double val = Double.parseDouble(atts.getValue("orig"));
            boolean min = Double.parseDouble(atts.getValue("norm")) == 0;
            double[] vals = m_normValues.get(m_lastDerivedField);
            if (vals == null) {
                vals = new double[2];
                vals[0] = Double.NaN;
                vals[1] = Double.NaN;
            }
            if (min) {
                vals[0] = val;
            } else {
                vals[1] = val;
            }
            m_normValues.put(m_lastDerivedField, vals);
        }
        m_elementStack.push(name);
    }

    /**
     * 
     * @return the minima for each derived fields
     */
    public double[] getMins() {
        return m_mins;
    }

    /**
     * 
     * @return the maxima of each derived field
     */
    public double[] getMaxs() {
        return m_maxs;
    }



}
