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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSerializer;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLClusterPortObject extends PMMLPortObject 
    implements PortObject {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLClusterPortObject.class);
    
    private Set<DataColumnSpec> m_usedColumns;
    private double[][]m_prototypes;
    private int m_nrOfClusters;
    private int[] m_clusterCoverage;
    private String[] m_labels;
    private double[] m_mins;
    private double[] m_maxs;
    
    /**
     * PMML Cluster port type.
     */
    public static final PortType TYPE 
        = new PortType(PMMLClusterPortObject.class);
    
    private static PortObjectSerializer<PMMLClusterPortObject>serializer;
    
    /**
     * Static serializer as demanded from {@link PortObject} framework.
     * @return serializer for PMML (reads and writes PMML files)
     */
    public static PortObjectSerializer<PMMLClusterPortObject> 
        getPortObjectSerializer() {
            if (serializer == null) {
                serializer 
                    = new PMMLPortObjectSerializer<PMMLClusterPortObject>();
            }
        return serializer;
    }
    
    /**
     * Default constructor necessary for loading.
     */
    public PMMLClusterPortObject() {
        
    }
    
    /**
     * 
     * @param prototypes the unnormalized prototypes of clusters
     * @param nrOfClusters number of clusters
     * @param mins minima of the used column domains
     * @param maxs maxima of the used column domains
     * @param portSpec the {@link PMMLPortObjectSpec} holding information of the
     *  PMML DataDictionary and the PMML MiningSchema
     */
    public PMMLClusterPortObject(
            final double[][] prototypes,
            final int nrOfClusters, 
            final double[] mins,
            final double[] maxs,
            final PMMLPortObjectSpec portSpec) {
        super(portSpec);
        m_nrOfClusters = nrOfClusters;
        m_usedColumns = getColumnSpecsFor(portSpec.getLearningFields(), 
                portSpec.getDataTableSpec());
        m_labels = new String[m_nrOfClusters];
        for (int i = 0; i < m_nrOfClusters; i++) {
            m_labels[i] = "cluster_" + i;
        }
        // PMMLCLusterPortObject holds only normalized prototypes
        m_prototypes = prototypes;
        for (int i = 0; i < prototypes.length; i++) {
            m_prototypes[i] = normalizePrototype(prototypes[i], 
                    mins[i], maxs[i]);
        }
    }
    
    
    private Set<DataColumnSpec> getColumnSpecsFor(final Set<String> colNames,
            final DataTableSpec tableSpec) {
        Set<DataColumnSpec> colSpecs = new LinkedHashSet<DataColumnSpec>();
        for (String colName : colNames) {
            DataColumnSpec colSpec = tableSpec.getColumnSpec(colName);
            if (colName == null) {
                throw new IllegalArgumentException(
                        "Column " + colName + " not found in data table spec!");
            }
            colSpecs.add(colSpec);
        }
        return colSpecs;
    }
    
    /**
     * 
     * @param labels cluster labels
     */
    public void setClusterLabels(final String[] labels) {
        m_labels = labels;
    }
    
    /**
     * 
     * @param clusterCoverage how many data points are in each cluster
     */
    public void setClusterCoverage(final int[] clusterCoverage) {
        m_clusterCoverage = clusterCoverage;
    }
    
    /**
     * 
     * @param mins domain minima of used columns
     */
    public void setMinima(final double[] mins) {
        m_mins = mins;
    }
    
    /**
     * 
     * @return minima for cloumn domains
     */
    public double[] getMinima() {
        return m_mins;
    }
    
    /**
     * 
     * @param maxs maxima for column domains
     */
    public void setMaxima(final double[] maxs) {
        m_maxs = maxs;
    }
    
    /**
     * 
     * @return maxima for used column domains
     */
    public double[] getMaxima() {
        return m_maxs;
    }
    

    /**
     * 
     * @return used columns
     */
    public Set<DataColumnSpec> getUsedColumns() {
        return m_usedColumns;
    }


    /**
     * 
     * @return <em>normalized</em> prototypes
     */
    public double[][] getPrototypes() {
        return m_prototypes;
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
     * @return number of covered data points per cluster
     */
    public int[] getClusterCoverage() {
        return m_clusterCoverage;
    }

    
    /**
     * 
     * @return cluster names
     */
    public String[] getLabels() {
        return m_labels;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writePMMLModel(final TransformerHandler handler) 
        throws SAXException {
        // create the cluster model 
        // with the attributes...
        AttributesImpl atts = new AttributesImpl();
        // modelName
        atts.addAttribute(null, null, "modelName", CDATA, "k-means");
        // functionName
        atts.addAttribute(null, null, "functionName", CDATA, "clustering");
        // modelClass
        atts.addAttribute(null, null, "modelClass", CDATA, "centerBased");
        // numberOfClusters
        atts.addAttribute(null, null, "numberOfClusters", CDATA, 
                "" + m_nrOfClusters);
        handler.startElement(null, null, "ClusteringModel", atts);
        if (m_usedColumns != null) {
            PMMLPortObjectSpec.writeMiningSchema(getSpec(), handler);
            addUsedDistanceMeasure(handler);
            addClusteringFields(handler, m_usedColumns);
            addCenterFields(handler, m_usedColumns);
            addClusters(handler, m_prototypes);
        }
        handler.endElement(null, null, "ClusteringModel");
    }
    
    /**
     * Writes the used distance measure - so far it is euclidean.
     * @param handler to write to
     * @throws SAXException if something goes wrong
     */
    protected void addUsedDistanceMeasure(final TransformerHandler handler) 
        throws SAXException {
        // add kind="distance" to ComparisonMeasure element
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "kind", CDATA, "distance");
        handler.startElement(null, null, "ComparisonMeasure", atts);
        // for now hard-coded squared euclidean
        handler.startElement(null, null, "squaredEuclidean", null);
        handler.endElement(null, null, "squaredEuclidean");
        handler.endElement(null, null, "ComparisonMeasure");
    }
    
    /**
     * Writes the clustering fields (name).
     *  
     * @param handler to write to
     * @param colSpecs column specs of used columns
     * @throws SAXException if something goes wrong
     */
    protected void addClusteringFields(final TransformerHandler handler,
            final Set<DataColumnSpec> colSpecs) throws SAXException {
        for (DataColumnSpec colSpec : colSpecs) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "field", CDATA, colSpec.getName());
            atts.addAttribute(null, null, "compareFunction", CDATA, "absDiff");
            handler.startElement(null, null, "ClusteringField", atts);
            handler.endElement(null, null, "ClusteringField");
        }
    }
    
    /**
     * Writes the center fields (name, minimum, and maximum).
     * 
     * @param handler to write to
     * @param colSpecs used columns in correct order
     * @throws SAXException if something goes wrong
     */
    protected void addCenterFields(final TransformerHandler handler,
            final Set<DataColumnSpec> colSpecs) throws SAXException {
        handler.startElement(null, null, "CenterFields", null);
        int i = 0;
        // for each column add
        for (DataColumnSpec colSpec : colSpecs) {
            // a derived field with name "normalized-[columnName]"
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "name", CDATA, 
                    "normalized-" + colSpec.getName());
            handler.startElement(null, null, "DerivedField", atts);
            // NormContinuous field="[columnName]"
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "field", CDATA, 
                        colSpec.getName());
                handler.startElement(null, null, "NormContinuous", atts);
                    // LinearNorm orig="[lowerBound]" norm="0"
                    atts = new AttributesImpl();
                    atts.addAttribute(null, null, "orig", CDATA, 
                            "" + m_mins[i]);
                    atts.addAttribute(null, null, "norm", CDATA, "0");
                    handler.startElement(null, null, "LinearNorm", atts);
                    handler.endElement(null, null, "LinearNorm");
                    // LinearNorm orig="[upperBound]" norm="1"
                    atts = new AttributesImpl();
                    atts.addAttribute(null, null, "orig", CDATA, 
                            "" + m_maxs[i]);
                    atts.addAttribute(null, null, "norm", CDATA, "1");
                    handler.startElement(null, null, "LinearNorm", atts);
                    handler.endElement(null, null, "LinearNorm");
                handler.endElement(null, null, "NormContinuous");
           handler.endElement(null, null, "DerivedField");
           i++;
        }
        handler.endElement(null, null, "CenterFields");
    }
    
    /**
     * Writes the actual cluster prototypes.
     * 
     * @param handler to write to
     * @param prototypes the normalized prototypes
     * @throws SAXException if something goes wrong
     */
    protected void addClusters(final TransformerHandler handler,
            final double[][] prototypes) throws SAXException {
        int i = 0;
        for (double[] prototype : prototypes) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "name", CDATA, "cluster_" + i);
            if (m_clusterCoverage != null 
                    && m_clusterCoverage.length == m_prototypes.length) {
            atts.addAttribute(null, null, "size", CDATA, 
                    "" + m_clusterCoverage[i]);
            }
            i++;
            handler.startElement(null, null, "Cluster", atts);
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "n", CDATA, 
                            "" + prototype.length);
                atts.addAttribute(null, null, "type", CDATA, "real");
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

    private double[] normalizePrototype(final double[] prototype, 
            final double min, final double max) {
        double[] normalized = new double[prototype.length];
        for (int i = 0; i < prototype.length; i++) {
            normalized[i] = (prototype[i] - min) / (max - min); 
        }
        return normalized;
    }
    
    
    /**
     * {@inheritDoc}
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    @Override
    public PMMLClusterPortObject loadFrom(final PMMLPortObjectSpec spec, 
            final InputStream in) 
        throws ParserConfigurationException, SAXException, IOException {
        PMMLClusterHandler hdl = new PMMLClusterHandler();
        super.addPMMLContentHandler("clusterModel", 
                hdl);
        super.loadFrom(spec, in);
        hdl = (PMMLClusterHandler)super.getPMMLContentHandler(
                "clusterModel");
        if (hdl.getClusterCoverage() != null) {
            m_clusterCoverage = hdl.getClusterCoverage();
        }
        m_nrOfClusters = hdl.getNrOfClusters();
        m_prototypes = hdl.getPrototypes();
        m_labels = hdl.getLabels();
        
        m_mins = hdl.getMins();
        m_maxs = hdl.getMaxs();
        m_usedColumns = getColumnSpecsFor(spec.getLearningFields(), 
                spec.getDataTableSpec()); 
        LOGGER.info("loaded cluster port object");
        LOGGER.debug("number of clusters: " + m_nrOfClusters);
        return this;
    }
}
