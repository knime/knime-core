/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Hashtable;
import java.util.Optional;

import org.junit.After;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.testing.util.URIToFileResolveTestUtil;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Makes sure that files are resolved conditionally via
 * {@link URIToFileResolve}, i.e. such that components are only downloaded if
 * new than the local one.
 * 
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP11813_EfficientTemplateUpdateCheck extends WorkflowTestCase {

    private URIToFileResolve m_resolveOrg;

    /**
     * Essentially tests that
     * {@link WorkflowManager#checkUpdateMetaNodeLink(NodeID, WorkflowLoadHelper)}
     * eventually uses
     * {@link URIToFileResolve#resolveToLocalOrTempFileConditional(java.net.URI, org.eclipse.core.runtime.IProgressMonitor, java.time.ZonedDateTime)}
     * to resolve URIs to files.
     * 
     * @throws Exception
     */
    @Test
    public void testCheckUpdateMetaNodeLink() throws Exception {
        loadAndSetWorkflow();
        WorkflowManager wfm = getManager();
        NodeID compId = wfm.getID().createChild(0);

        URIToFileResolve resolveMock = Mockito.mock(URIToFileResolve.class);
		m_resolveOrg = URIToFileResolveTestUtil.replaceURIToFileResolveService(resolveMock);

        // case if there is no (mocked) update
        when(resolveMock.resolveToLocalOrTempFileConditional(any(), any(), any())).thenReturn(Optional.empty());
        assertFalse(wfm.checkUpdateMetaNodeLink(compId, null));
        verify(resolveMock).resolveToFile(any());
        verify(resolveMock, never()).resolveToLocalOrTempFile(any());
        verify(resolveMock).resolveToLocalOrTempFileConditional(any(), any(), any());

        // case there is a (mocked) update
        File componentDir = new File(getDefaultWorkflowDirectory().getAbsolutePath() + "_Component");
        when(resolveMock.resolveToLocalOrTempFileConditional(any(), any(), any()))
                .thenReturn(Optional.of(componentDir));
        assertTrue(wfm.checkUpdateMetaNodeLink(compId, new WorkflowLoadHelper(true, true,
                WorkflowContextV2.forTemporaryWorkflow(componentDir.toPath(), null))));
        verify(resolveMock, times(2)).resolveToLocalOrTempFileConditional(any(), any(), any());

        // ensures that the file is _not_ resolved conditionally when
        // calling wfm.updateMetaNodeLink
        Mockito.reset(resolveMock);
        when(resolveMock.resolveToFile(any())).thenReturn(null);
        when(resolveMock.resolveToLocalOrTempFile(any())).thenReturn(null);
        wfm.updateMetaNodeLink(compId, new ExecutionMonitor(), null);
        verify(resolveMock).resolveToFile(any());
        verify(resolveMock, never()).resolveToLocalOrTempFile(any());
        verify(resolveMock).resolveToLocalOrTempFileConditional(any(), any(), any());

    }

	@After
	public void setOriginalURIToFileResolve() {
		URIToFileResolveTestUtil.replaceURIToFileResolveService(m_resolveOrg);
	}

}
