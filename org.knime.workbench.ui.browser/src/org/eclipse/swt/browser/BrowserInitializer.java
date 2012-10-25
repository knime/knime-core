/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *   25.10.2012 (meinl): created
 */
package org.eclipse.swt.browser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BrowserInitializer {
    private static final String PROPERTY_DEFAULTTYPE = "org.eclipse.swt.browser.DefaultType";

    private static final String XUL = "org.eclipse.swt.browser.XULRunnerPath";

    private static final NodeLogger logger = NodeLogger.getLogger(BrowserInitializer.class);

    private static final Pattern VERSION_PATTERN = Pattern.compile("Milestone=(\\d+)\\.(\\d+)");

    static {
        try {
            if (System.getProperty(PROPERTY_DEFAULTTYPE) == null) {
                Class<?> webkitClass = Class.forName("org.eclipse.swt.browser.WebKit");
                Method m = webkitClass.getDeclaredMethod("isInstalled");
                Boolean compatibleWebkitInstalled = (Boolean)m.invoke(null);

                if (compatibleWebkitInstalled) {
                    System.setProperty(PROPERTY_DEFAULTTYPE, "webkit");
                    logger.debug("Using WebKit as browser");
                } else if (System.getProperty("XUL") == null) {
                    File xulRunner = findCompatibleXulrunner();
                    if (xulRunner != null) {
                        System.setProperty(XUL, xulRunner.getAbsolutePath());
                        System.setProperty(PROPERTY_DEFAULTTYPE, "mozilla");
                        logger.info("Using xulrunner at '" + xulRunner.getAbsolutePath()
                                + "' as internal web browser. If you want to change this," + " add '-D" + XUL
                                + "=...' to knime.ini");
                    } else {
                        // this is for safety, because incompatible version lead to hard crashes
                        System.setProperty(XUL, "/dev/null");

                        String msg =
                                "No compatible web browser found. Node descriptions and online help will not work."
                                        + "Please see http://tech.knime.org/faq#q6 for details";
                        logger.warn(msg);
                        System.err.println(msg);
                    }
                }
            }
        } catch (Exception ex) {
            // this is for safety, because incompatible xulrunner versions lead to hard crashes
            System.setProperty(XUL, "/dev/null");

            logger.error("Error while determining web browser: " + ex.getMessage(), ex);
        }
    }

    /**
     * Tries to find a compatible xulrunner in the system or in our bundle.
     *
     * @return the path to the xulrunner library or <code>null</code> if no compatible xulrunner was found
     * @throws IOException if an I/O erro occurs while checking xulrunner version
     */
    static File findCompatibleXulrunner() throws IOException {
        File libDir;
        if ("amd64".equals(System.getProperty("os.arch"))) {
            libDir = new File("/usr/lib64");
            if (!libDir.isDirectory()) {
                libDir = new File("/usr/lib");
            }
        } else {
            libDir = new File("/usr/lib32");
            if (!libDir.isDirectory()) {
                libDir = new File("/usr/lib");
            }
        }
        File[] xulLocations = libDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory()
                        && (pathname.getName().startsWith("xulrunner") || pathname.getName().startsWith("firefox")
                                || pathname.getName().startsWith("seamonkey") || pathname.getName()
                                .startsWith("mozilla"));
            }
        });
        if (xulLocations == null) {
            xulLocations = new File[0];
        }

        File xul19Location = null;
        File xul18Location = null;

        for (File dir : xulLocations) {
            File libXPCom = new File(dir, "libxpcom.so");
            if (!libXPCom.exists()) {
                continue;
            }

            File platformIni = new File(dir, "platform.ini");
            if (!platformIni.exists()) {
                continue;
            }

            BufferedReader in = new BufferedReader(new FileReader(platformIni));
            String line;
            while ((line = in.readLine()) != null) {
                Matcher m = VERSION_PATTERN.matcher(line);
                if (m.find()) {
                    int major = Integer.parseInt(m.group(1));
                    int minor = Integer.parseInt(m.group(2));

                    if (major == 1) {
                        if (minor > 8) {
                            xul19Location = dir;
                        } else if (minor == 8) {
                            xul18Location = dir;
                        }
                    }
                    break;
                }
            }
            in.close();
        }

        if (xul19Location != null) {
            return xul19Location;
        } else if (xul18Location != null) {
            return xul18Location;
        } else {
            return getInternalXULRunner();
        }
    }

    private static File getInternalXULRunner() {
        Bundle bundle = Platform.getBundle("org.knime.xulrunner.linux." + Platform.getOSArch());
        if (bundle != null) {
            URL resourceUrl = bundle.getResource("lib");
            if (resourceUrl != null) {
                try {
                    URL fileUrl = FileLocator.toFileURL(resourceUrl);
                    File file = new File(fileUrl.getPath());
                    return file;
                } catch (Exception e) {
                    Bundle myself = FrameworkUtil.getBundle(BrowserInitializer.class);

                    Platform.getLog(myself).log(new Status(IStatus.ERROR, myself.getSymbolicName(),
                                                        "Could not resolve xulrunner plugin properly", e));
                }
            }
        }
        return null;
    }
}
