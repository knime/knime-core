/*
 * ------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.product.p2.actions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Custom p2 action extracting a tar (.gz) archive.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 3.2
 */
public class ExtractTarGz extends ProvisioningAction {

    private static final Bundle bundle = FrameworkUtil.getBundle(ExtractTarGz.class);

    private static final String SOURCE_ARCHIVE = "source";
    private static final String TARGET_DIR = "targetDir";

    private final static ILog logger = Platform.getLog(bundle);

    @Override
    public IStatus execute(final Map<String, Object> parameters) {
        try {
            String source = (String)parameters.get(SOURCE_ARCHIVE);
            if (source == null) {
                return new Status(IStatus.ERROR, bundle.getSymbolicName(),
                    String.format("No '%s' attribute specified", SOURCE_ARCHIVE));
            }
            logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "Source file: " + source));

            String targetDir = (String)parameters.get(TARGET_DIR);
            if (targetDir == null) {
                return new Status(IStatus.ERROR, bundle.getSymbolicName(),
                    String.format("No '%s' attribute specified", TARGET_DIR));
            }
            logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "Target directory: " + targetDir));

            IProfile profile = (IProfile) parameters.get("profile");
            File installFolder = new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER));
            File destDir = new File(installFolder, targetDir);
            if (destDir.exists()) {
                return new Status(IStatus.ERROR, bundle.getSymbolicName(),
                    String.format("Target directory '%s' already exists", targetDir));
            }

            URL url = new URL(source);
            InputStream in;
            try (InputStream fileInputStream = new BufferedInputStream(url.openStream())) {
                if (StringUtils.endsWithIgnoreCase(source, ".tar.gz")
                        || StringUtils.endsWithIgnoreCase(source, ".tgz")) {
                    in = new GzipCompressorInputStream(fileInputStream);
                } else if (StringUtils.endsWithIgnoreCase(source, ".tar.bz2")) {
                    in = new BZip2CompressorInputStream(fileInputStream);
                } else if (StringUtils.endsWithIgnoreCase(source, ".tar.xz")) {
                    in = new XZCompressorInputStream(fileInputStream);
                } else {
                    in = fileInputStream;
                }
                untar(in, destDir);
            }
            return Status.OK_STATUS;
        } catch (Throwable e) {
            return new Status(IStatus.ERROR, bundle.getSymbolicName(), e.getMessage(), e);
        }
    }

    private static void untar(final InputStream in, final File destDir) throws IOException {
        try (TarArchiveInputStream tarInS = new TarArchiveInputStream(in)) {
            TarArchiveEntry entry;
            while ((entry = tarInS.getNextTarEntry()) != null) {
                String name = entry.getName();
                File destFile = new File(destDir, name);
                if (entry.isSymbolicLink()) {
                    Files.createSymbolicLink(destFile.toPath(), Paths.get(name));
                } else {
                    try (FileOutputStream out = new FileOutputStream(destFile)) {
                        long size = entry.getSize();
                        IOUtils.copyLarge(tarInS, out, 0, size);
                    }
                    chmod(destFile, entry.getMode());
                }
            }
        }
    }

    private static void chmod(final File file, final int mode) {
        file.setExecutable((mode & 0111) != 0, (mode & 0110) == 0);
        file.setWritable((mode & 0222) != 0, (mode & 0220) == 0);
        file.setReadable((mode & 0444) != 0, (mode & 0440) == 0);
    }

    @Override
    public IStatus undo(final Map<String, Object> parameters) {
        return Status.OK_STATUS;
    }
}
