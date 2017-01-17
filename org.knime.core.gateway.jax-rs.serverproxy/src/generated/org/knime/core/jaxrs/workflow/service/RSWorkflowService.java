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
package org.knime.core.jaxrs.workflow.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.ws.rs.GET;

import org.knime.core.gateway.serverproxy.service.DefaultWorkflowService;
import org.knime.core.gateway.v0.workflow.entity.*;
import org.knime.core.gateway.v0.workflow.service.WorkflowService;
import org.knime.core.jaxrs.workflow.entity.*;

import java.util.List;
import java.util.stream.Collectors;


/**
 * RESTful service implementation of the {@link WorkflowService} - rest calls (i.e. rest resources) are essentially
 * delegated to the {@link DefaultWorkflowService} implementation.
 *
 * @author Martin Horn, University of Konstanz
 */
@Path("/WorkflowService")
public class RSWorkflowService {

    private DefaultWorkflowService m_service = new DefaultWorkflowService();


			
    @POST
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/getWorkflow")
    public Response getWorkflow(
 	 				final EntityID id		 	    ) {
    		 		 			WorkflowEnt res = m_service.getWorkflow(id);
		Object resConv;
			//convert entity via a wrapper to a json-serializable class
		resConv = new WorkflowEntToJson(res);
		        return Response.ok().entity(resConv).build();
        }
			
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/updateWorkflow")
    public Response updateWorkflow(
 	 				final WorkflowEnt wf		 	    ) {
    		 		 			m_service.updateWorkflow(wf);
		Object resConv;
				return Response.ok().build();
	    }

	@GET
	@Produces(MediaType.APPLICATION_JSON)
    @Path("/getAllWorkflows")
    public Response getAllWorkflows(
 	    ) {
    				List<EntityID> res = m_service.getAllWorkflows();
		Object resConv;
			//convert entity via a wrapper to a json-serializable class
		resConv = res.stream().map(e -> new EntityIDToJson(e)).collect(Collectors.toList()); 
		        return Response.ok().entity(resConv).build();
        }

}
