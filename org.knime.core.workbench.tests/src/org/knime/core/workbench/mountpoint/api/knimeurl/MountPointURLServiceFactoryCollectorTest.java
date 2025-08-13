/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 13, 2025 (wiswedel): created
 */
package org.knime.core.workbench.mountpoint.api.knimeurl;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPoint;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;

/**
 *
 * @author wiswedel
 */
class MountPointURLServiceFactoryCollectorTest {

    private IPath m_tempFilePath;
    private IPath m_relativeTempFilePath;

    @BeforeEach
    void setUp() throws IOException {
        String fileName = "test_%s_%s.txt".formatted(getClass().getSimpleName(), System.currentTimeMillis());
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath workspaceRoot = workspace.getRoot().getLocation();
        IPath tempFilePath;
        String fileNameTemplate = "test_%s%s.txt";
        String uniquifier = StringUtils.EMPTY;
        do {
            fileName = fileNameTemplate.formatted(getClass().getSimpleName(), uniquifier);
            uniquifier = "_" + System.currentTimeMillis();
            tempFilePath = workspaceRoot.append(fileName);
        } while (!tempFilePath.toFile().createNewFile());
        try (var writer = Files.newBufferedWriter(tempFilePath.toFile().toPath(), StandardCharsets.UTF_8)) {
            writer.write("Hello World!");
        }
        m_tempFilePath = tempFilePath;
        m_relativeTempFilePath = tempFilePath.makeRelativeTo(workspaceRoot);
    }

    @Test
    final void testCollectFromLocalWorkspace() throws IOException {
        MountPointURLServiceFactoryCollector collector = MountPointURLServiceFactoryCollector.getInstance();
        Optional<MountPointURLServiceFactory> localServiceFactory =
            collector.getMountPointURLServiceFactory("org.knime.workbench.explorer.workspace");
        assertTrue(localServiceFactory.isPresent(), "Local workspace contribution is present");
        // resolve a local temporary file
        MountPointURLServiceFactory localService = localServiceFactory.get();
        Optional<WorkbenchMountPoint> mountPoint = WorkbenchMountTable.getMountPoint("LOCAL");
        assertTrue(mountPoint.isPresent(), "Local workspace must be mounted");
        MountPointURLService localMountPointURLService =
            localService.createMountPointURLService(mountPoint.get().getState());
        URLConnection urlConnection = localMountPointURLService.newURLConnection(m_relativeTempFilePath, null);

        try (var inputStream = urlConnection.getInputStream()) {
            assertTrue(inputStream.available() >= 0, "Input stream is available");
            final List<String> lines = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
            assertEquals(1, lines.size(), "Expected single line");
            assertEquals("Hello World!", lines.get(0), "Content in file");
        }
    }

    @AfterEach
    void tearDown() {
        if (m_tempFilePath != null && m_tempFilePath.toFile().exists()) {
            boolean deleted = m_tempFilePath.toFile().delete();
            assertTrue(deleted, "Temporary file should be deleted: " + m_tempFilePath);
        }
    }

}
