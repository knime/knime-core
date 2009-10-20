/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
    
    private PortObjectSpec m_spec;
    
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
        } else {
            m_sotaRoot = null;
        }
        m_spec = spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        m_sotaRoot.saveTo(model, 0);
        model.addString(CFG_KEY_DIST, m_distance);
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
