/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.core.gateway.v0.workflow.entity.impl;

import org.knime.core.gateway.v0.workflow.entity.WorkflowUIInfoEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowUIInfoEntBuilder;

import org.knime.core.gateway.entities.EntityBuilderFactory;
import org.knime.core.gateway.entities.EntityBuilderManager;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of the WorkflowUIInfoEnt-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultWorkflowUIInfoEnt implements WorkflowUIInfoEnt {

	private int m_GridX;
	private int m_GridY;
	private boolean m_SnapToGrid;
	private boolean m_ShowGrid;
	private double m_ZoomLevel;
	private boolean m_HasCurvedConnection;
	private int m_ConnectionLineWidtdh;

    /**
     * @param builder
     */
    DefaultWorkflowUIInfoEnt(final DefaultWorkflowUIInfoEntBuilder builder) {
		m_GridX = builder.m_GridX;
		m_GridY = builder.m_GridY;
		m_SnapToGrid = builder.m_SnapToGrid;
		m_ShowGrid = builder.m_ShowGrid;
		m_ZoomLevel = builder.m_ZoomLevel;
		m_HasCurvedConnection = builder.m_HasCurvedConnection;
		m_ConnectionLineWidtdh = builder.m_ConnectionLineWidtdh;
    }

	@Override
    public int getGridX() {
        return m_GridX;
    }
    
	@Override
    public int getGridY() {
        return m_GridY;
    }
    
	@Override
    public boolean getSnapToGrid() {
        return m_SnapToGrid;
    }
    
	@Override
    public boolean getShowGrid() {
        return m_ShowGrid;
    }
    
	@Override
    public double getZoomLevel() {
        return m_ZoomLevel;
    }
    
	@Override
    public boolean getHasCurvedConnection() {
        return m_HasCurvedConnection;
    }
    
	@Override
    public int getConnectionLineWidtdh() {
        return m_ConnectionLineWidtdh;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static DefaultWorkflowUIInfoEntBuilder builder() {
		return new DefaultWorkflowUIInfoEntBuilder();
	}
}
