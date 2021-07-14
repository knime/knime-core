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
 * -------------------------------------------------------------------
 *
 * History
 *   10.08.2005 (bernd): created
 */
package org.knime.core.node;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.internal.ConfigurationAreaChecker;
import org.knime.core.internal.KNIMEPath;
import org.knime.core.util.ThreadPool;
import org.osgi.framework.Bundle;

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

    /** The build date, is set automatically by the build scripts.*/
    public static final String BUILD_DATE;

    /**
     * Java property name that is used to identify whether KNIME is started in expert mode or not. Note, with KNIME v2.4
     * this field became obsolete and is not used anymore, including all variable specific features/nodes.
     * <p>
     * Values of this field must be either "true" or "false".
     */
    public static final String PROPERTY_EXPERT_MODE = "knime.expert.mode";

    /**
     * Java property name to specify the default max thread count variable (can be set via preference page).
     */
    public static final String PROPERTY_MAX_THREAD_COUNT = "org.knime.core.maxThreads";

    /**
     * Java property name to specify the default temp directory for KNIME temp files (such as data files). This can be
     * changed in the preference pages and is by default the same as the java.io.tmpdir
     */
    public static final String PROPERTY_TEMP_DIR = "knime.tmpdir";

    /**
     * Java property to disable the nonsequential handling of rows for KNIME tables. By default, each table container
     * processes its rows asynchronously in a number of (potentially re-used) threads. Setting this field to true will
     * instruct KNIME to always handle rows sequentially and synchronously, which in some cases may be slower.
     * Asynchronous I/O became default with v2.1. Note that independent of this setting the writing of rows to disk
     * always happens sequentially, and, depending on the {@link #PROPERTY_TABLE_CACHE} setting, potentially
     * asynchronously. If not specified the default is obtained from {@link DataContainerSettings#getDefault()}.
     */
    public static final String PROPERTY_SYNCHRONOUS_IO = "knime.synchronous.io";

    /**
     * Java property to customize cache / batch size for non-sequential and asynchronous handling of rows (see
     * {@link #PROPERTY_SYNCHRONOUS_IO}). It specifies the amount of data rows that are handled by a single container
     * thread. The larger the buffer, the smaller the synchronization overhead but the larger the memory requirements.
     * This property has no effect if rows are handled sequentially. If not specified the default is obtained from
     * {@link DataContainerSettings#getDefault()}.
     */
    public static final String PROPERTY_ASYNC_WRITE_CACHE_SIZE = "knime.async.io.cachesize";

    /**
     * The number of nominal values kept in the domain when adding rows to a table. This is only the default and may be
     * overruled by individual node implementations. If not specified the default is obtained from
     * {@link DataContainerSettings#getDefault()}.
     *
     * @since 2.10
     */
    public static final String PROPERTY_DOMAIN_MAX_POSSIBLE_VALUES = "knime.domain.valuecount";

    /**
     * Java property name to set a different threshold for the number of cells to be held in main memory (if memory
     * setting is "Keep only small tables in memory"). If not specified the default is obtained from
     * {@link DataContainerSettings#getDefault()}.
     *
     * @since 2.6
     */
    public static final String PROPERTY_CELLS_IN_MEMORY = "org.knime.container.cellsinmemory";


    /**
     * Defines the maximum number of threads that are shared between instances of {@link DataContainer DataContainers}.
     * The default value is set to the number of available CPU's, see {@link Runtime#availableProcessors()} and
     * {@link DataContainerSettings}.
     *
     * @since 4.0
     *
     */
    public static final String PROPERTY_MAX_THREADS_TOTAL = "org.knime.container.threads.total";

    /**
     * Defines the maximum number of threads that can be used by a single instance of {@link DataContainer
     * DataContainer}. The value has to be smaller than {link {@link #PROPERTY_MAX_THREADS_TOTAL}. The default value is
     * set to the number of available CPU's, see {@link Runtime#availableProcessors()} and
     * {@link DataContainerSettings}.
     *
     * @since 4.0
     *
     */
    public static final String PROPERTY_MAX_THREADS_INSTANCE = "org.knime.container.threads.instance";

    /** Java property name to specify the minimum free disc space in MB that needs to be available. If less is
     * available, no further table files &amp; blobs will be created (resulting in an exception). Default is
     * {@value org.knime.core.data.container.Buffer#DEF_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB} MB.
     * @since 2.8
     */
    public static final String PROPERTY_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB = "org.knime.container.minspace.temp";

    /**
     * Java property name to specify the strategy for keeping tables in memory and writing tables to disk. Current
     * options are {@code LRU} and {@code SMALL}. If {@code LRU} is selected, tables of any size will be cached and
     * dropped from the cache only if (a) they have not been used recently or (b) memory becomes scarce. Flushing to
     * disk then happens as specified in {@link KNIMEConstants#PROPERTY_SYNCHRONOUS_IO}. If {@code SMALL} is selected,
     * tables are cached as in earlier versions of KNIME (&lt;v3.8). Specifically, only "small" tables (i.e., tables
     * with a maximum of {@link KNIMEConstants#PROPERTY_CELLS_IN_MEMORY} cells) are kept in memory, unless the node
     * generating the table was specifically configured to flush tables to disk or keep tables in memory. Flushing of
     * tables to disk then happens synchronously. The default is {@code LRU}.
     *
     * @since 4.0
     */
    public static final String PROPERTY_TABLE_CACHE = "knime.table.cache";

    /**
     * Java property to discourage KNIME from triggering a full stop-the-world garbage collection. Note that (a)
     * individual nodes are allowed to disregard this setting and (b) the garbage collector may independently decide
     * that a full stop-the-world garbage collection is warranted.
     *
     * @since 4.1
     */
    public static final String PROPERTY_DISCOURAGE_GC = "knime.discourage.gc";

    /**
     * Java property to enable/disable table stream compression. Compression results in smaller temp-file sizes but also
     * (sometimes significant) longer runtime. By default {@code Gzip} is used.
     * <p>
     * <strong>Warning:</strong> Changing this property will result in KNIME not being able to read workflows written
     * previously (with a different compression property).
     * @since 4.0
     */
    public static final String PROPERTY_TABLE_COMPRESSION = "knime.compress.io";

    /**
     * @see #PROPERTY_TABLE_COMPRESSION
     * @deprecated replaced by {@link #PROPERTY_TABLE_COMPRESSION}
     */
    @Deprecated
    public static final String PROPERTY_TABLE_GZIP_COMPRESSION = PROPERTY_TABLE_COMPRESSION;

    /** Java property to enable/disable row ID duplicate checks on tables.
     * Tables in KNIME are supposed to have unique IDs, whereby the uniqueness
     * is asserted using a duplicate checker. This property will disable this
     * check.
     * <p><strong>Warning:</strong> This property should not be changed by
     * the user. */
    public static final String PROPERTY_DISABLE_ROWID_DUPLICATE_CHECK =
        "knime.disable.rowid.duplicatecheck";

    /** Java property to enable/disable workflow locks. As of KNIME v2.4
     * workflows will be locked when opened; this property will disable the
     * locking (allowing multiple instances to have the same workflow open).
     * <p><strong>Warning:</strong> This property should not be changed by
     * the user.
     * @since v2.4 */
    public static final String PROPERTY_DISABLE_VM_FILE_LOCK =
        "knime.disable.vmfilelock";

    /** Java property to add a context menu entry on metanodes to allow the
     * user to lock the workflow. This feature is likely to be a KNIME.com
     * extension and is in beta stage - the action will eventually be moved
     * to a KNIME.com plugin but is currently contained in
     * org.knime.workbench.editor (though hidden unless this property is
     * specified).
     *
     * <br>
     * This flag only affects the KNIME client.
     * @since v2.5 */
    public static final String PROPERTY_SHOW_METANODE_LOCK_ACTION =
        "knime.showaction.metanodelock";

    /**
     * Java property to disable external entity resolution in DTDs. This property is sometimes set in server
     * installations with increased security requirements. Setting this property to <code>true</code> will disable
     * reading external entities in XML files.
     *
     * @since 3.4
     */
    // see AP-6752
    public static final String PROPERTY_XML_DISABLE_EXT_ENTITIES = "knime.xml.disable_external_entities";

    /** For KNIME's R extension: Run the R process in debug mode and print debug messages to the logging facilities.
     * Value is true or false (default).
     * @since 3.2*/
    public static final String PROPERTY_R_RSERVE_DEBUG = "org.knime.r.rserve.debug";

    /**
     * Java property do en-/disable the workaround for the dialog deadlocks
     * under MacOSX (see http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=3151).
     *
     * @since 2.5
     * @deprecated This property is not used any more.
     */
    @Deprecated
    public static final String PROPERTY_MACOSX_DIALOG_WORKAROUND =
        "knime.macosx.dialogworkaround";

    /**
     * The name of the system property whose value is - if set - used as knime
     * home directory. If no (or an invalid) value is set, ~user/knime will be
     * used instead. To set the knime home dir from the command line, use
     * -Dknime.home=&lt;absolutePathToNewHomeDir&gt;.
     */
    public static final String PROPERTY_KNIME_HOME = "knime.home";

    /** @deprecated Use {@link #PROPERTY_KNIME_HOME} instead. */
    @Deprecated
    // obsolete as of v2.3
    public static final String KNIME_HOME_PROPERTYNAME = PROPERTY_KNIME_HOME;

    /** Java property used to set the timeout in seconds trying to establish a
     * connection to a database.
     *
     * @deprecated Set the timeout via the database preferences.
     */
    @Deprecated
    public static final String PROPERTY_DATABASE_LOGIN_TIMEOUT =
            "knime.database.timeout";

    /** @deprecated Use #PROPERTY_DATABASE_LOGIN_TIMEOUT instead. */
    @Deprecated
    // obsolete as of v2.3
    public static final String KNIME_DATABASE_LOGIN_TIMEOUT =
        PROPERTY_DATABASE_LOGIN_TIMEOUT;

    /** Java property used to adjust the fetch size for retrieving data from a database. */
    public static final String PROPERTY_DATABASE_FETCHSIZE = "knime.database.fetchsize";

    /** Java property used to adjust the batch write size for writing data into a database.
     * @since 2.6 */
    public static final String PROPERTY_DATABASE_BATCH_WRITE_SIZE = "knime.database.batch_write_size";

    /** Java property to switch on/off the database connection access (applies only for the same database connection).
     * Default is true, that is all database accesses are synchronized based on single connection; false means off,
     * that is, the access is not synchronized and may lead to database errors.
     * @since 2.8 */
    public static final String PROPERTY_DATABASE_CONCURRENCY = "knime.database.enable.concurrency";

    /** If <code>true</code>, nodes using passwords as part of their configuration (e.g. DB connection or SendEmail) will
     * not store the password as part of the workflow on disc. Instead a null value is stored, which will cause the
     * node's configuration to be incorrect (but valid) after the workflow is restored from disc.
     * @since 4.3
     */
    // added with AP-15442
    public static final String PROPERTY_WEAK_PASSWORDS_IN_SETTINGS_FORBIDDEN = "knime.settings.passwords.forbidden";

    /** @deprecated Use #PROPERTY_DATABASE_FETCHSIZE instead. */
    @Deprecated
    // obsolete as of v2.3
    public static final String KNIME_DATABASE_FETCHSIZE =
        PROPERTY_DATABASE_FETCHSIZE;

    /** Java property, which allows one to change the default
     * log file size. Values must be integer, possibly succeeded by "m" or "k"
     * to denote that the given value is in mega or kilo byte. */
    public static final String PROPERTY_MAX_LOGFILESIZE =
        "knime.logfile.maxsize";

    /** Java property that allows to disable the live update in the node
         repository search. */
   public static final String PROPERTY_REPOSITORY_NON_INSTANT_SEARCH =
       "knime.repository.non-instant-search";

   /** Java property for the location of the license directory. */
   public static final String PROPERTY_LICENSE_DIRECTORY =
       "com.knime.licensedir";

   /** Java property used to set the timeout in millisecond trying to connect
    * or read data from an URL (e.g. http, ftp, ...)
    *
    * @since 2.6
    */
   public static final String PROPERTY_URL_TIMEOUT = "knime.url.timeout";

    /**
     * Java property which allows to skip automatic Log4J configuration when
     * KNIME starts. The value should be <code>true</code> or <code>false</code>
     * (which is the default).
     *
     * @since 2.6
     */
   public static final String PROPERTY_DISABLE_LOG4J_CONFIG =
       "knime.log4j.config.disabled";

   /**
    * Java property for doing all dialog operations automatically in the
    * AWT event dispatch thread.
    *
    * @since 2.6
    */
   public static final String PROPERTY_DIALOG_IN_EDT = "knime.core.dialog.edt";

   /** enables display of icons in different sizes (if available).
    * @since 3.0 */
   public static final String PROPERTY_HIGH_DPI_SUPPORT = "knime.highdpi.support";

    /**
     * The minimum refresh interval in ms, e.g. to refresh the node progress or the state of the remote job view.
     *
     * @since 3.6
     */
    public static final int MIN_GUI_REFRESH_INTERVAL = 250;

    /**
     * Connection timeout in ms when the workflow manager is connected to a server in order to retrieve the workflow
     * information.
     *
     * @since 3.6
     */
    public static final int WORKFLOW_EDITOR_CONNECTION_TIMEOUT = getWorkflowEditorConnectionTimeout();

    /* Try reading the timeout from the system properties, otherwise return default value. */
    private static int getWorkflowEditorConnectionTimeout() {
        String prop = System.getProperty("org.knime.ui.workflow_editor.timeout");
        if (prop != null) {
            try {
                return Integer.parseInt(prop);
            } catch (NumberFormatException e) {
                NodeLogger.getLogger(KNIMEConstants.class)
                    .warn("Couldn't parse value for system property 'org.knime.ui.workflow_editor.timeout'");
            }
        }
        return 1500;
    }

    /** KNIME home directory. */
    private static File knimeHomeDir;

    /** KNIME temp directory. */
    private static Path knimeTempDir;

    /**
     * <i>Welcome to KNIME</i>.
     */
    public static final String WELCOME_MESSAGE;


    static {
        BUILD_DATE = "Nightly build";
        String versionString;
        Bundle coreBundle = OSGIHelper.getBundle("org.knime.core");
        if (coreBundle != null) {
            versionString = coreBundle.getHeaders().get("Bundle-Version")
                .toString();
        } else {
            System.err.println("Can't locate CorePlugin, not an OSGi framework?");
            versionString = "1.0.0.000000";
        }
        VERSION = versionString;
        String[] parts = VERSION.split("\\.");
        MAJOR = Integer.parseInt(parts[0]);
        MINOR = Integer.parseInt(parts[1]);
        REV = Integer.parseInt(parts[2]);
        BUILD = parts[3];
        String stars = "**********************************************************************************************";
        String spaces = "                                    ";

        String line1 = "***         Welcome to KNIME Analytics Platform v" + VERSION + "       ***";
        String line2 = "Copyright by KNIME AG, Zurich, Switzerland";
        line2 =
            "***" + spaces.substring(0, (line1.length() - line2.length() - 6) / 2) + line2
                + spaces.substring(0, (int)Math.ceil((line1.length() - line2.length() - 6) / 2.0)) + "***";

        String s =
            stars.substring(0, line1.length()) + "\n" + line1 + "\n" + line2 + "\n"
                + stars.substring(0, line1.length()) + "\n";
        WELCOME_MESSAGE = s;
    }

    private static Optional<ImageIcon> KNIME16x16_ICON = null;

    /**
     * Icon 16 times 16 pixel.
     *
     * @return the image icon, or an empty optional if in headless-mode or the loading failed
     *
     * @since 4.0
     */
    public static Optional<ImageIcon> getKNIMEIcon16X16() {
        if (KNIME16x16_ICON == null) {
            if (!Boolean.getBoolean("java.awt.headless")) {
                final AtomicReference<ImageIcon> icon = new AtomicReference<ImageIcon>();
                try {
                    ClassLoader loader = KNIMEConstants.class.getClassLoader();
                    URL iconURL = loader.getResource(KNIME_ICON_PATH);
                    if (!SwingUtilities.isEventDispatchThread()) {
                        //make sure to instantiate the icon in the AWT event dispatch thread
                        SwingUtilities.invokeAndWait(() -> icon.set(new ImageIcon(iconURL)));
                    } else {
                        icon.set(new ImageIcon(iconURL));
                    }
                } catch (Throwable e) {
                    icon.set(null);
                }
                KNIME16x16_ICON = Optional.ofNullable(icon.get());
            } else {
                KNIME16x16_ICON = Optional.empty();
            }
            KNIME16X16 = KNIME16x16_ICON.orElseGet(null);
        }
        return KNIME16x16_ICON;
    }

    /** Path to the <i>knime.png</i> icon. */
    private static final String KNIME_ICON_PATH =
            KNIMEConstants.class.getPackage().getName().replace('.', '/')
                    + "/knime.png";

    /**
     * Icon 16 times 16 pixel or <code>null</code> if in headless-mode.
     *
     * @deprecated Use {@link KNIMEConstants#getKNIMEIcon16X16()} instead.
     */
    @Deprecated
    public static ImageIcon KNIME16X16 = null;

    static {
        File knimeHome = KNIMEPath.getKNIMEHomeDirPath();
        knimeHomeDir = knimeHome;

        int maxThreads = Runtime.getRuntime().availableProcessors() + 2;
        String maxThreadsString =
            System.getProperty(PROPERTY_MAX_THREAD_COUNT);
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
            try {
                knimeTempDir = Paths.get(System.getProperty("java.io.tmpdir"));
                knimeTempDir = knimeTempDir.toRealPath();
            } catch (IOException ex) {
                System.err.println("Could not canonicalize temp directory '" + knimeTempDir.toAbsolutePath() + "': "
                    + ex.getMessage());
            }
        }
        ConfigurationAreaChecker.scheduleIntegrityCheck();
    }

    /**
     * The standard file extension for KNIME imports/exports
     * @since 3.2
     */
    public static final String KNIME_ARCHIVE_FILE_EXTENSION = "knar";
    /**
     * The standard file extension for KNIME workflows
     * @since 3.2
     */
    public static final String KNIME_WORKFLOW_FILE_EXTENSION = "knwf";
    /**
     * The standard file extension for the KNIME protocol
     * @since 3.2
     */
    public static final String KNIME_PROTOCOL_FILE_EXTENSION = "knimeURL";


    /** The global thread pool from which all threads should be taken. */
    public static final ThreadPool GLOBAL_THREAD_POOL;

    /** Global flag indicating whether assertions are enabled or disabled. */
    public static final boolean ASSERTIONS_ENABLED;

    private static String knimeID;

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
        return knimeTempDir.toAbsolutePath().toString();
    }

    /**
     * Location for KNIME related temp files such as data container files. This
     * is by default System.getProperty("java.io.tmpdir") but can be overwritten
     * in the command line or the preference page. The
     *
     * @return The path to the temp directory
     * @since 3.1
     */
    public static Path getKNIMETempPath() {
        return knimeTempDir;
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
        try {
            knimeTempDir = dir.toPath().toRealPath();
        } catch (IOException ex) {
            NodeLogger.getLogger(KNIMEConstants.class)
                .debug("Could not canonicalize temp directory '" + dir.getAbsolutePath() + "': " + ex.getMessage(), ex);
            knimeTempDir = dir.toPath();
        }
    }

    /**
     * Returns the hostname or null, if it couldn't be determined.
     * @return the hostname or null, if it couldn't be determined.
     */
    public static final String getHostname() {
        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            return localMachine.getHostName();
        } catch (Exception uhe) {
            return null;
        }
    }

    /**
     * Returns the unique ID of this KNIME installation. The ID is saved in the configuration area when KNIME is started
     * for the first time and then read from there.
     *
     * @return a unique ID for this KNIME installation
     * @since 2.10
     */
    public static synchronized String getKNIMEInstanceID() {
        if (knimeID == null) {
            assignUniqueID();
        }
        return knimeID;
    }

    private static void assignUniqueID() {
        Optional<Path> configLocationPath = ConfigurationAreaChecker.getConfigurationLocationPath();
        if (configLocationPath.isPresent()) {
            Path uniqueId = configLocationPath.get().resolve("org.knime.core").resolve("knime-id");

            if (!Files.exists(uniqueId)) {
                try {
                    Files.createDirectories(uniqueId.getParent());

                    knimeID = "01-" + createUniqeID();
                    try (OutputStream os = Files.newOutputStream(uniqueId)) {
                        os.write(knimeID.toString().getBytes("UTF-8"));
                    } catch (IOException ex) {
                        NodeLogger.getLogger(KNIMEConstants.class).error(
                            "Could not write KNIME id to '" + uniqueId.toAbsolutePath() + "': " + ex.getMessage(),
                            ex);
                    }
                } catch (IOException ex) {
                    NodeLogger.getLogger(KNIMEConstants.class).error(
                        "Could not create configuration directory '" + uniqueId.getParent().toAbsolutePath()
                        + "': " + ex.getMessage(), ex);
                }
            } else if (Files.isReadable(uniqueId)) {
                try (InputStream is = Files.newInputStream(uniqueId)) {
                    byte[] buf = new byte[256];
                    int read = is.read(buf);
                    knimeID = new String(buf, 0, read, Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    NodeLogger.getLogger(KNIMEConstants.class).error(
                        "Could not read KNIME id from '" + uniqueId.toAbsolutePath() + "': " + ex.getMessage(), ex);
                }
            }
        }
        if (knimeID == null) {
            knimeID = "00-" + createUniqeID();
        }
    }

    private static String createUniqeID() {
        SecureRandom rand = new SecureRandom();
        byte[] uid = new byte[8];
        rand.nextBytes(uid);
        return new String(Hex.encodeHex(uid));
    }

    /**
     * Returns whether this is a nightly build or not.
     *
     * @return <code>true</code> if this is a nightly build, <code>false</code> otherwise
     * @since 3.4
     */
    public static boolean isNightlyBuild() {
        return BUILD_DATE.contains("Nightly");
    }


    /**
     * Returns details about the current operating system, such as the Linux distribution name.
     *
     * @return details about the OS variant
     * @since 3.6
     */
    public static String getOSVariant() {
        String os = Platform.getOS();
        try {
            if (Platform.OS_LINUX.equals(os)) {
                Path lsbRelease = Paths.get("/usr/bin/lsb_release");
                if (Files.isExecutable(lsbRelease)) {
                    ProcessBuilder pb = new ProcessBuilder("lsb_release", "-d");
                    Process p = pb.start();
                    try (InputStream is = p.getInputStream()) {
                        Properties props = new Properties();
                        props.load(is);
                        os = props.getProperty("Description", os).trim();
                    }
                } else {
                    Path redhatRelease = Paths.get("/etc/redhat-release");
                    if (Files.isReadable(redhatRelease)) {
                        try (BufferedReader in = Files.newBufferedReader(redhatRelease)) {
                            os = in.readLine();
                        }
                    }
                }
            } else if (Platform.OS_MACOSX.equals(os)) {
                os = System.getProperty("os.name") + " " + System.getProperty("os.version");
            } else if (Platform.OS_WIN32.equals(os)) {
                os = System.getProperty("os.name");
            }
        } catch (IOException ex) {
            NodeLogger.getLogger(KNIMEConstants.class)
                .info("I/O error while determining operating system: " + ex.getMessage(), ex);
        }

        return os;
    }

    /**
     * Hides public constructor.
     */
    private KNIMEConstants() {
    }
}
