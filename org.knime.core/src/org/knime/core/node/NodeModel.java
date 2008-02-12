/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   17.01.2006(sieb, ohl): reviewed
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;


/**
 * Class implements the general model of a node which gives access to the
 * <code>DataTable</code>,<code>HiLiteHandler</code>, and
 * <code>DataTableSpec</code> of all outputs.
 * <p>
 * The <code>NodeModel</code> should contain the node's "model", i.e., what
 * ever is stored, contained, done in this node - it's the "meat" of this node.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeModel extends GenericNodeModel {

    private final int m_nrDataInPorts;
    private final int m_nrDataOutPorts;
    private final int m_nrModelInPorts;
    private final int m_nrModelOutPorts;

    /**
     * Creates a new model with the given number of in- and outputs.
     *
     * @param nrDataIns Number of data inputs.
     * @param nrDataOuts Number of data outputs.
     *
     * @see NodeModel#NodeModel(int, int, int, int)
     */
    protected NodeModel(final int nrDataIns, final int nrDataOuts) {
        this(nrDataIns, nrDataOuts, 0, 0);
    }

    private static PortType[] createArrayOfDataAndModelTypes(
            final int nrDataPorts, final int nrModelPorts) {
        PortType[] pTypes = new PortType[nrDataPorts + nrModelPorts];
        for (int i = 0; i < nrDataPorts; i++) {
            pTypes[i] = BufferedDataTable.TYPE;
        }
        for (int i = nrDataPorts; i < nrDataPorts + nrModelPorts; i++) {
            pTypes[i] = ModelContent.TYPE;
        }
        return pTypes;
    }

    /** Old-style constructor creating a NodeModel that also has model ports.
     *
     * DO NOT USE ANYMORE, USE @see GenericNodeModel INSTEAD
     *
     * @param nrDataIns
     * @param nrDataOuts
     * @param nrModelIns
     * @param nrModelOuts
     */
    @Deprecated
    protected NodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrModelIns, final int nrModelOuts) {
        super(createArrayOfDataAndModelTypes(nrDataIns, nrModelIns),
              createArrayOfDataAndModelTypes(nrDataOuts, nrModelOuts));
        m_nrDataInPorts = nrDataIns;
        m_nrDataOutPorts = nrDataOuts;
        m_nrModelInPorts = nrModelIns;
        m_nrModelOutPorts = nrModelOuts;
    }

    /**
     *
     * @param inSpecs
     * @return
     * @throws InvalidSettingsException
     */
    protected abstract DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
    throws InvalidSettingsException {
        // convert all PortObjectSpecs corresponding to data ports to
        // DataTableSpecs
        DataTableSpec[] inTableSpecs =
             new DataTableSpec[m_nrDataInPorts];
        for (int i = 0; i < m_nrDataInPorts; i++) {
            inTableSpecs[i] = (DataTableSpec)(inSpecs[i]);
        }
        for (int i = m_nrDataInPorts;
        i < m_nrDataInPorts + m_nrModelInPorts; i++) {
            int mdlIndex = i - m_nrDataInPorts;
            if (inSpecs[i] instanceof MeanLittleWrapper) {
                MeanLittleWrapper mlw = (MeanLittleWrapper)inSpecs[i];
                ModelContentRO mdl = mlw.m_hiddenModel;
                loadModelContent(mdlIndex, mdl);
            }
        }
        // call old-style configure
        DataTableSpec[] outTableSpecs = configure(inTableSpecs);
        // copy output specs and put dummy model-out specs in result array
        PortObjectSpec[] returnObjectSpecs =
            new PortObjectSpec[m_nrDataOutPorts + m_nrModelOutPorts];
        for (int i = 0; outTableSpecs != null && i < m_nrDataOutPorts; i++) {
            returnObjectSpecs[i] = outTableSpecs[i];
        }
        m_localOutModels = new MeanLittleWrapper[m_nrModelOutPorts];
        for (int i = m_nrDataOutPorts;
             i < m_nrDataOutPorts + m_nrModelOutPorts; i++) {
            ModelContent thisMdl = new ModelContent("ModelContent");
            saveModelContent(i - m_nrDataOutPorts, thisMdl);
            m_localOutModels[i - m_nrDataOutPorts] = new MeanLittleWrapper(thisMdl);
            returnObjectSpecs[i] = new MeanLittleWrapper(thisMdl);
        }
        return returnObjectSpecs;
    }

    /////////////////////////////////////
    // The following is a hack to allow usage of ModelContent object already
    // during configure! (old v1.x model ports!)
    //
    // hide model content in a modern style PortObjectSpec
    private class MeanLittleWrapper implements PortObjectSpec {
        MeanLittleWrapper(final ModelContentRO mdl) {
            m_hiddenModel = mdl;
        }
        ModelContentRO m_hiddenModel;
    }
    //
    // allow to replace model content generated during execute in the modern
    // style PortObjectSpec (Schweinerei! changes spec under the model's ass)
    private MeanLittleWrapper[] m_localOutModels;
    //
    // end of evil hack.
    ///////////////////////////////////////////
    
    protected abstract BufferedDataTable[] execute(
            final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception;

    @Override
    protected final PortObject[] execute(
            final PortObject[] inData, final ExecutionContext exec)
            throws Exception {
        // convert all PortObjects to DataTables
        BufferedDataTable[] inTables =
            new BufferedDataTable[m_nrDataInPorts];
        for (int i = 0; i < m_nrDataInPorts; i++) {
            inTables[i] = (BufferedDataTable)(inData[i]);
        }
        // load remaining Model Objects into old style NodeModel
        for (int i = m_nrDataInPorts;
             i < m_nrDataInPorts + m_nrModelInPorts; i++) {
            int mdlIndex = i - m_nrDataInPorts;
            assert (inData[i] instanceof ModelContentRO);
            ModelContentRO mdl = (ModelContent)inData[i];
            loadModelContent(mdlIndex, mdl);
        }
        // finally call old style execute
        BufferedDataTable[] outTables = execute(inTables, exec);
        // retrieve models from old style NodeModel
        PortObject[] returnObjects =
            new PortObject[m_nrDataOutPorts + m_nrModelOutPorts];
        for (int i = 0; i < m_nrDataOutPorts; i++) {
            returnObjects[i] = outTables[i];
        }
        for (int i = m_nrDataOutPorts;
             i < m_nrDataOutPorts + m_nrModelOutPorts; i++) {
            int mdlIndex = i - m_nrDataInPorts;
            ModelContent thisMdl = new ModelContent("ModelContent");
            saveModelContent(mdlIndex, thisMdl);
            returnObjects[i] = thisMdl;
            m_localOutModels[mdlIndex].m_hiddenModel = thisMdl;
        }
        // and return the assembled data+models
        return returnObjects;
    }

    ///////////////////// DEPRECATED STARTS HERE /////////////////////

    /**
     * Override this method if <code>ModelContent</code> input(s) have
     * been set. This method is then called for each ModelContent input to
     * load the <code>ModelContent</code> after the previous node has been
     * executed successfully or is reset.
     *
     * <p>This implementation throws a InvalidSettingsException as it should
     * not have been called: If a derived NodeModel defines a model input, it
     * must override this method.
     *
     * @param index The input index, starting from 0.
     * @param predParams The ModelContent to load, which can be null to
     *            indicate that no ModelContent model is available.
     * @throws InvalidSettingsException If the predictive parameters could not
     *             be loaded.
     */
    @Deprecated
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        assert predParams == predParams;
        throw new InvalidSettingsException(
                "loadModelContent() not overridden: " + index);
    }

    /**
     * Override this method if <code>ModelContent</code> output(s) have
     * been set. This method is then called for each
     * <code>ModelContent</code> output to save the
     * <code>ModelContent</code> after this node has been successfully
     * executed.
     *
     * <p>This implementation throws a InvalidSettingsException as it should
     * not have been called: If a derived NodeModel defines a model output, it
     * must override this method.

     * @param index The output index, starting from 0.
     * @param predParams The ModelContent to save to.
     * @throws InvalidSettingsException If the model could not be saved.
     */
    @Deprecated
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        assert predParams == predParams;
        throw new InvalidSettingsException(
                "saveModelContent() not overridden: " + index);
    }

}

