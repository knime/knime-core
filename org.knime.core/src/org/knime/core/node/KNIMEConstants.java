/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 *
 * History
 *   10.08.2005 (bernd): created
 */
package org.knime.core.node;

import java.io.File;
import java.util.Locale;

import javax.swing.ImageIcon;

import org.knime.core.internal.CorePlugin;
import org.knime.core.internal.KNIMEPath;
import org.knime.core.util.ThreadPool;

/**
 * Class that hold static values about the KNIME platform. This includes,
 * among others, the welcome message and an icon.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class KNIMEConstants {
    /** KNIME's major release number. */
    public static final int MAJOR;
    /** KNIME's minor release number. */
    public static final int MINOR;
    /** KNIME's revision number. */
    public static final int REV;
    /** KNIME's build id. */
    public static final String BUILD;
    /** Workflow file version. */
    public static final String VERSION;

    /** The build date, is set automatically by the build scripts. */
    public static final String BUILD_DATE = "November 25, 2009";

    /** Java property name that is used to identify whether KNIME is started
     * in expert mode or not (e.g. whether to show loop nodes or not).
     * This field is also used for the preference pages.
     * <p>Values of this field must be either "true" or "false". */
    public static final String PROPERTY_EXPERT_MODE = "knime.expert.mode";

    /** Java property name to specify the default max thread count variable
     * (can be set via preference page). */
    public static final String PROPERTY_MAX_THREAD_COUNT =
        "org.knime.core.maxThreads";

    /** Java property name to specify the default temp directory for
     * KNIME temp files (such as data files). This can be changed in the
     * preference pages and is by default the same as the java.io.tmpdir */
    public static final String PROPERTY_TEMP_DIR = "knime.tmpdir";

    /** Java property to disable the asynchronous writing of KNIME tables. By
     * default, each table container writing to disk performs the write
     * operation in a dedicated (potentially re-used) thread. Setting this field
     * to true will instruct KNIME to always write synchronously, which in some
     * cases may be slower. (Asynchronous I/O became default with v2.1.) */
    public static final String PROPERTY_SYNCHRONOUS_IO = "knime.synchronous.io";

    /** Java property to enable/disable table stream compression. Compression
     * results in smaller temp-file sizes but also (sometimes significant)
     * longer runtime. The default is {@value
     * org.knime.core.data.container.DataContainer#DEF_GZIP_COMPRESSION}.
     * <p><strong>Warning:</strong> Changing this property will result in KNIME
     * not being able to read workflows written previously (with a
     * different compression property). */
    public static final String PROPERTY_TABLE_GZIP_COMPRESSION =
        "knime.compress.io";

    /**
     * The name of the system property whose value is - if set - used as knime
     * home directory. If no (or an invalid) value is set, ~user/knime will be
     * used instead. To set the knime home dir from the command line, use
     * -Dknime.home=&lt;absolutePathToNewHomeDir&gt;.
     */
    public static final String KNIME_HOME_PROPERTYNAME = "knime.home";

    /**
     * Java property used to set the timeout in seconds trying to establish a
     * connection to a database.
     */
    public static final String KNIME_DATABASE_LOGIN_TIMEOUT =
            "knime.database.timeout";

    /**
     * Java property used to adjust the fetch size for retrieving data from
     * a database.
     */
    public static final String KNIME_DATABASE_FETCHSIZE =
        "knime.database.fetchsize";

    /** Java property, which allows one to change the default
     * log file size. Values must be integer, possibly succeeded by "m" or "k"
     * to denote that the given value is in mega or kilo byte. */
    public static final String PROPERTY_MAX_LOGFILESIZE = "knime.logfile.maxsize";



    /** KNIME home directory. */
    private static File knimeHomeDir;

    /** KNIME temp directory. */
    private static File knimeTempDir;

    /**
     * <i>Welcome to KNIME Konstanz Information Miner</i>.
     */
    public static final String WELCOME_MESSAGE;


    static {
        String versionString;
        if (CorePlugin.getInstance() != null) {
            versionString = CorePlugin.getInstance().getBundle().getHeaders()
                .get("Bundle-Version").toString();
        } else {
            System.err.println(
                    "Can't locate CorePlugin, not an OSGi framework?");
            versionString = "1.0.0.000000";
        }
        VERSION = versionString;
        String[] parts = VERSION.split("\\.");
        MAJOR = Integer.parseInt(parts[0]);
        MINOR = Integer.parseInt(parts[1]);
        REV = Integer.parseInt(parts[2]);
        BUILD = parts[3];


        String line1 =
                "***  Welcome to KNIME v" + VERSION
                        + " - the Konstanz Information Miner  ***";
        String line2 =
                "Copyright, 2003 - 2010, Uni Konstanz and "
                        + "KNIME GmbH, Germany";
        line2 =
                "***"
                        + "        ".substring(0, (int)Math.floor((line1
                                .length()
                                - line2.length() - 6) / 2))
                        + line2
                        + "        ".substring(0, (int)Math.ceil((line1
                                .length()
                                - line2.length() - 6) / 2)) + "***";

        String stars =
                "***************************************************"
                        + "*******************************************";

        String s =
                stars.substring(0, line1.length()) + "\n" + line1 + "\n"
                        + line2 + "\n" + stars.substring(0, line1.length())
                        + "\n";
        WELCOME_MESSAGE = s;
    }

    /** Path to the <i>knime.png</i> icon. */
    private static final String KNIME_ICON_PATH =
            KNIMEConstants.class.getPackage().getName().replace('.', '/')
                    + "/knime.png";

    /** Icon 16 times 16 pixel. */
    public static final ImageIcon KNIME16X16;

    /** Load icon. */
    static {
        File knimeHome = KNIMEPath.getKNIMEHomeDirPath();
        knimeHomeDir = knimeHome;
        ImageIcon icon;
        try {
            ClassLoader loader = KNIMEConstants.class.getClassLoader();
            icon = new ImageIcon(loader.getResource(KNIME_ICON_PATH));
        } catch (Exception e) {
            icon = null;
        }
        KNIME16X16 = icon;
        // we prefer to have all gui-related locals being set to us-standard
        try {
            Locale.setDefault(Locale.US);
        } catch (Exception e) {
            // do nothing.
        }

        int maxThreads = Runtime.getRuntime().availableProcessors() + 2;
        String maxThreadsString =
            System.getProperty("org.knime.core.maxThreads");
        try {
            if (maxThreadsString != null && maxThreadsString.length() > 0) {
                int val = Integer.parseInt(maxThreadsString);
                if (val <= 0) {
                    throw new NumberFormatException("Not positive");
                }
                maxThreads = val;
            }
        } catch (NumberFormatException nfe) {
            // no NodeLogger available yet!
            System.err.println("Unable to parse system property "
                    + "\"org.knime.core.maxThreads\" (\"" + maxThreadsString
                    + "\") as number: " + nfe.getMessage());
        }
        GLOBAL_THREAD_POOL = new ThreadPool(maxThreads);
        boolean flag;
        try {
            assert false;
            flag = false;
        } catch (AssertionError ae) {
            flag = true;
        }
        ASSERTIONS_ENABLED = flag;
        String tempDirPath = System.getProperty(PROPERTY_TEMP_DIR);
        if (tempDirPath != null) {
            File f = new File(tempDirPath);
            if (!(f.isDirectory() && f.canWrite())) {
                String error = "Unable to set temp path to \""
                        + tempDirPath + "\": no directory or not writable";
                System.err.println(error);
                throw new InternalError(error);
            } else {
                setKNIMETempDir(f);
            }
        } else {
            knimeTempDir = new File(System.getProperty("java.io.tmpdir"));
        }
    }

    /** The global thread pool from which all threads should be taken. */
    public static final ThreadPool GLOBAL_THREAD_POOL;

    /** Global flag indicating whether assertions are enabled or disabled. */
    public static final boolean ASSERTIONS_ENABLED;

    /**
     * The directory where knime will put log files and configuration files. If
     * started in eclipse, this is usually ${workspace_path}/.metadata/knime.
     * Otherwise it's in the current working directory. This variable does not
     * have a trailing file separator character.
     *
     * @return The KNIME home dir.
     */
    public static final String getKNIMEHomeDir() {
        return knimeHomeDir.getAbsolutePath();
    }

    /** Location for KNIME related temp files such as data container files. This
     * is by default System.getProperty("java.io.tmpdir") but can be overwritten
     * in the command line or the preference page. The
     * @return The path to the temp directory (trailing slashes omitted).
     */
    public static final String getKNIMETempDir() {
        return knimeTempDir.getAbsolutePath();
    }

    /** Set a new location for the KNIME temp directory. Client should not
     * be required to use this method. It has public scope so that bootstrap
     * classes can initialize this properly.
     * @param dir the new location to set
     * @throws NullPointerException If the argument is null
     * @throws IllegalArgumentException If the argument is not a directory
     * or not writable.
     */
    public static final void setKNIMETempDir(final File dir) {
        if (dir == null) {
            throw new NullPointerException("Directory must not be null");
        }
        if (!(dir.isDirectory() && dir.canWrite())) {
            throw new IllegalArgumentException("Can't set temp directory to \""
                    + dir.getAbsolutePath()
                    + "\": not a directory or not writable");
        }
        System.setProperty("java.io.tmpdir", dir.getAbsolutePath());
        knimeTempDir = dir;
    }

    /**
     * Hides public constructor.
     */
    private KNIMEConstants() {
    }
}
