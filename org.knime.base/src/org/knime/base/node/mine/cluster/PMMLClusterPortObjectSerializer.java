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

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSerializer;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;

/**
 * Loads and saves the {@link PMMLClusterPortObject} ot valid PMML by simply
 * forwarding to the referring {@link PMMLClusterPortObject#loadFrom(File)} 
 * and {@link PMMLClusterPortObject#save(File)} methods of the  
 * {@link PMMLClusterPortObject}.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLClusterPortObjectSerializer 
    extends PMMLPortObjectSerializer<PMMLClusterPortObject> {
    
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLClusterPortObjectSerializer.class);
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected PMMLClusterPortObject loadPortObject(final File directory,
            final PortObjectSpec spec, 
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        PMMLClusterHandler hdl = new PMMLClusterHandler();
        super.addPMMLContentHandler("clusterModel", 
                hdl);
        hdl = (PMMLClusterHandler)super.getPMMLContentHandler(
                "clusterModel");
        int[] clusterCoverage = null;
        if (hdl.getClusterCoverage() != null) {
            clusterCoverage = hdl.getClusterCoverage();
        }
        int nrOfClusters = hdl.getNrOfClusters();
        double[][] prototypes = hdl.getPrototypes();
        String[] labels = hdl.getLabels();
        
        double[] mins = hdl.getMins();
        double[] maxs = hdl.getMaxs();
        //dataDictionaryToDataTableSpec(f);
        LOGGER.info("loaded cluster port object");
        LOGGER.debug("number of clusters: " + nrOfClusters);
        PMMLClusterPortObject o = new PMMLClusterPortObject(
                prototypes, nrOfClusters, mins, maxs,
                (PMMLPortObjectSpec)spec);
        o.setClusterLabels(labels);
        if (clusterCoverage != null) {
            o.setClusterCoverage(clusterCoverage);
        }
        try {
            o.loadFrom(new File(directory, "pmml.xml"));
        } catch (Exception e) {
            throw new IOException(e);
        }
        return o;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void savePortObject(final PMMLClusterPortObject portObject,
            final File directory, final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        try {
            portObject.save(new File(directory, "pmml.xml"));
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (TransformerConfigurationException e) {
            throw new IOException(e);
        }
    }
}
