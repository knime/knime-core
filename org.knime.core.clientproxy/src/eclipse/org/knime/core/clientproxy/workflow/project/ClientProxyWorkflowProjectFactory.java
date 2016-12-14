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
 * History
 *   Nov 29, 2016 (hornm): created
 */
package org.knime.core.clientproxy.workflow.project;

import static org.knime.core.gateway.entities.EntityBuilderManager.builder;
import static org.knime.core.gateway.services.ServiceManager.service;

import java.util.Arrays;

import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.project.WorkflowGroup;
import org.knime.core.api.node.workflow.project.WorkflowProject;
import org.knime.core.api.node.workflow.project.WorkflowProjectFactory;
import org.knime.core.clientproxy.workflow.WorkflowManager;
import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.EntityIDBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.TestEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.XYEntBuilder;
import org.knime.core.gateway.v0.workflow.service.TestService;
import org.knime.core.gateway.v0.workflow.service.WorkflowService;

/**
 * Mainly for testing and prototyping purposes.
 *
 * @author Martin Horn, University of Konstanz
 */
public class ClientProxyWorkflowProjectFactory implements WorkflowProjectFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IWorkflowManager openProject(final WorkflowProject id) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowGroup getProjectTree() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWorkflowManager wrap(final IWorkflowManager wfm) {
        //server has already been started with the bundle activation
        //but here: make sure that the "server" knows the workflow

        //just for testing / proof-of-concept
        XYEnt xy = builder(XYEntBuilder.class).setX(10).setY(25).build();
        service(TestService.class).test(builder(TestEntBuilder.class).setxy(xy).setother("hello1234").setxylist(Arrays.asList(xy)).build());

        //return 'client' workflow manager
        //download 'workflow' from server'
        EntityID workflowId =
                builder(EntityIDBuilder.class).setID(wfm.getID().toString()).setType("WorkflowEnt").build();
        return new WorkflowManager(service(WorkflowService.class).getWorkflow(workflowId));
    }

}
