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
 * ----------------------------------------------------------------------------
 */
package org.knime.product.p2.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.knime.core.util.FileUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Custom p2 action extracting a tar (.gz) archive.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
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
            String source = readParameter(parameters, SOURCE_ARCHIVE);
            String targetDir = readParameter(parameters, TARGET_DIR);

            File destDir;
            Path targetDirPath = Paths.get(targetDir);
            if (targetDirPath.isAbsolute()) { // targetDir is absolute path or contains @artifact
                destDir = targetDirPath.toFile();
            } else { // targetDir is something like '.'
                IProfile profile = (IProfile) parameters.get("profile");
                File installFolder = new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER));
                destDir = new File(installFolder, targetDir);
            }

            try (InputStream fileInputStream = FileUtil.openInputStream(source)) {
                InputStream in;
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

    /**
     * @param parameters
     * @param s
     * @return
     * @throws CoreException
     */
    private static String readParameter(final Map<String, Object> parameters, final String s) throws CoreException {
        String result = (String)parameters.get(s);
        if (result == null) {
            throw new CoreException(new Status(IStatus.ERROR, bundle.getSymbolicName(),
                String.format("No '%s' attribute specified", s)));
        }
        logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(), "s: " + result));
        return resolveArtifactParam(result, parameters);
    }

    /** Resolves artifact key in argument. Inspired by org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util.
     * @param argument The string to 'fix'.
     * @param parameters parameter map.
     * @return the fixed argument string (unmodified if it does not contain the artifact keyword). */
    private static String resolveArtifactParam(final String argument,
        final Map<String, Object> parameters) throws CoreException {
        if (StringUtils.contains(argument, "@artifact")) {
            String artifactLocation = (String) parameters.get("artifact.location");
            if (artifactLocation != null) {
                return StringUtils.replace(argument, "@artifact", artifactLocation);
            }
            throw new CoreException(new Status(
                IStatus.ERROR, bundle.getSymbolicName(), "Unable to resolve @artifact location"));
        }
        return argument;
    }

    private static void untar(final InputStream in, final File destDir) throws IOException {
        try (TarArchiveInputStream tarInS = new TarArchiveInputStream(in)) {
            TarArchiveEntry entry;
            while ((entry = tarInS.getNextTarEntry()) != null) {
                String name = entry.getName();
                File destFile = new File(destDir, name);
                if (entry.isSymbolicLink()) {
                    Files.createSymbolicLink(destFile.toPath(), Paths.get(entry.getLinkName()));
                } else if (entry.isDirectory()) {
                    destFile.mkdirs();
                    chmod(destFile, entry.getMode());
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

    /** Set permission oa file according to unix file permission flags:
     * http://www.unix.com/tips-and-tutorials/19060-unix-file-permissions.html
     * @param file the file to change
     * @param mode the mode as per tar entry
     */
    private static void chmod(final File file, final int mode) {
        // java doesn't give us full control over permissions (e.g. no group)
        // the below will fail also for, e.g. -rw-???r-x ... but that seems artificial anyway
        file.setExecutable((mode & 0100) != 0, (mode & 0001) == 0);
        file.setWritable((mode & 0200) != 0, (mode & 0002) == 0);
        file.setReadable((mode & 0400) != 0, (mode & 0004) == 0);
    }

    @Override
    public IStatus undo(final Map<String, Object> parameters) {
        return Status.OK_STATUS;
    }

}
