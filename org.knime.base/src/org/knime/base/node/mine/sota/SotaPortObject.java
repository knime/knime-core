/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   16.09.2008 (thiel): created
 */
package org.knime.base.node.mine.sota;

import org.knime.base.node.mine.sota.logic.SotaManager;
import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPortObject extends AbstractSimplePortObject {

    /**
     * The configuration key for the usage of hierarchical fuzzy data.
     */
    static final String CFG_KEY_USE_FUZZY_HIERARCHY = "FuzzyHierarchy";
    
    /**
     * The configuration key for the maximal fuzzy hierarchy level.
     */
    static final String CFG_KEY_MAX_FUZZY_LEVEL = "MaxFuzzyLevel";
    
    /**
     * The configuration key for the size of the in data container.
     */
    static final String CFG_KEY_INDATA_SIZE = "InDataContainerSize";
    
    /**
     * The configuration key for the size of the original data container.
     */
    static final String CFG_KEY_ORIGDATA_SIZE = "OrigDataContainerSize";

    /**
     * The configuration key for the distance to use.
     */
    static final String CFG_KEY_DIST = "Distance";     

    /**
     * The configuration key for the object spec.
     */
    static final String CFG_KEY_SPEC = "Sota-PortObjectSpec";
    
    /**
     * The configuration key for the index of the class column.
     */
    static final String CFG_KEY_CLASSCOL_INDEX = "ClassColIndex";
    
    
    private SotaManager m_sota;
    
    private SotaTreeCell m_sotaRoot;
    
    private String m_distance;
    
    private SotaPortObjectSpec m_spec;
    
    /**
     * Creates empty instance of <code>SotaPortObject</code>.
     */
    public SotaPortObject() { }
    
    /**
     * Creates new instance of <code>SotaPortObject</code> with given 
     * <code>SotaManager</code>, <code>DataTableSpec</code> and index of the 
     * class column to store.
     * 
     * @param sota The <code>SotaManager</code> to store.
     * @param spec The data table spec to store.
     * @param indexOfClassCol The index of the class column.
     */
    public SotaPortObject(final SotaManager sota, final DataTableSpec spec,
            final int indexOfClassCol) {
        this();
        setSota(sota);
        m_spec = new SotaPortObjectSpec(spec, indexOfClassCol);
    }   

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        if (model != null) {
            // Load tree
            m_sotaRoot = new SotaTreeCell(0, false);
            try {
                m_sotaRoot.loadFrom(model, 0, null, false);
            } catch (InvalidSettingsException e) {
                InvalidSettingsException ioe = new InvalidSettingsException(
                        "Could not load tree cells, due to invalid settings in "
                        + "model content !");
                ioe.initCause(e);
                throw ioe;
            }
            
            m_distance = model.getString(CFG_KEY_DIST);
        }
        m_spec = (SotaPortObjectSpec) spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        if (m_sota != null) {
            // Save tree
            m_sota.getRoot().saveTo(model, 0);

            // Save settings
            model.addBoolean(CFG_KEY_USE_FUZZY_HIERARCHY, m_sota
                    .isUseHierarchicalFuzzyData());
            model.addInt(CFG_KEY_MAX_FUZZY_LEVEL, m_sota
                    .getMaxHierarchicalLevel());
            model.addInt(CFG_KEY_INDATA_SIZE, m_sota.getInDataContainer()
                    .size());
            model.addInt(CFG_KEY_ORIGDATA_SIZE, m_sota.getOriginalData()
                    .size());

            model.addString(CFG_KEY_DIST, m_sota.getDistance());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return "Sota Port";
    }

    /**
     * Sets the given <code>SotaManager</code>.
     * 
     * @param sota The <code>SotaManager</code> to set.
     */
    public void setSota(final SotaManager sota) {
        if (sota != null) {
            m_sota = sota;
            m_sotaRoot = m_sota.getRoot();
            m_distance = m_sota.getDistance();
        }
    }
    
    /**
     * @return the sota
     */
    public SotaManager getSota() {
        return m_sota;
    }

    /**
     * @return the sotaRoot
     */
    public SotaTreeCell getSotaRoot() {
        return m_sotaRoot;
    }

    /**
     * @return the distance
     */
    public String getDistance() {
        return m_distance;
    }

}
