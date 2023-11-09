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
 *   18 Oct 2023 (carlwitt): created
 */
package org.knime.core.node.extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.workflowalizer.NodeMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * This class is responsible for persisting the {@link NodeSpecCache} to disk to reuse the information across restarts
 * of the Analytics Platform.
 *
 * <p>
 * Only the node factory extensions are persisted, not node set factory extensions. The reason is that node set factory
 * extensions contribute a dynamic set of node factories and the set can change without a change in the bundle version.
 * </p>
 * <p>
 * The cache data is stored in {@value #DATA_DIR} the data area of the OSGi bundles that contributes the cache. It is
 * partitioned across bundles - each bundles contributed nodes are stored in a separate file along with a fingerprint
 * file (contains the bundle and cache data schema version) and the actual cached data. An index of files is maintained
 * in {@value #INDEX_FILE}.
 * </p>
 * <p>
 * If at startup a different version of the bundle is found in {@value #VERSION_FILE_NAME}, the cache is considered
 * invalid and the data is computed again by instantiating each contributed node and extracting the metadata (port
 * types, vendor, etc.).
 * </p>
 * <p>
 * The data is stored in a file called {@value #CACHE_FILE_NAME} and contains the zipped, json-serialized metadata.
 * </p>
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Leonard Wörteler, KNIME AG, Zurich, Switzerland
 * @since 5.2
 */
final class DiskBasedNodeSpecCache {

    /** Loads node specs from disk and counts down the initialized latch. */
    private static final Thread loadThread = new Thread(DiskBasedNodeSpecCache::loadCache);

    /** Used to wait for the cache to be loaded. */
    private static final CountDownLatch initialized = new CountDownLatch(1);

    private static final String DATA_DIR = "nodespeccachedata";

    private static final String INDEX_FILE = "nodespeccachecontent.json";

    /**
     * The cache stores a file in the data area of each bundle contributing node factories or node set factories. It
     * simply contains
     */
    private static final String VERSION_FILE_NAME = "node-metadata-cache-version.txt";

    private static final String CACHE_FILE_NAME = "node-metadata-cache.json.gzip";

    private static boolean cannotWriteFilesWarningIssued;

    private static final Map<INodeFactoryExtension, List<NodeSpec>> specs = new HashMap<>();


    static {
        loadThread.setName("Node Repository Disk Loader");
        loadThread.start();
    }
//
//    /**
//     * @param cacheFormatVersion summarizes the format/schema of the metadata cache. This is compared to the
//     *            cacheFormatVersion that was written with the previous serialization. If the format versions disagree,
//     *            the cache is invalidated.
//     */
    private DiskBasedNodeSpecCache() {

    }

    private static void loadCache() {
        //
    }

    /**
     * @param bundle
     * @return whether the cache is up to date (was computed for the given bundle version and the current cache format
     *         version)
     */
    static boolean isCacheUsable(final Bundle bundle) {
        final var requiredFingerprint = new Fingerprint(bundle.getVersion());
        return cacheStateFilePath(bundle).flatMap(Fingerprint::read).filter(requiredFingerprint::equals).isPresent();
    }
    //
    //    /**
    //     * @param bundle
    //     * @param extensions all extensions in this bundle
    //     * @return null if no data is available on disk. Otherwise the metadata of all nodes contributed by the node factory
    //     *         extension point. If the bundle contains nodes contributed by the node set factory extension point, the
    //     *         metadata is not loaded but the extensions are added to the load result instead.
    //     */
    //    DiskLoadResult load(final Bundle bundle, final Set<INodeFactoryExtension> extensions) {
    //        if (!isCacheUsable(bundle)) {
    //            return null;
    //        }
    //
    //        // load nodes from disk
    //        var path = cacheDataFilePath(bundle).get();
    //        var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
    //
    //        try (var zis = new GZIPInputStream(Files.newInputStream(path))) {
    //            var cacheData = mapper.readValue(zis, NodeProperties[].class);
    //            // add node set factories to load result
    //            var nodeSetFactoryExtensions = extensions.stream()
    //                .flatMap(ext -> ClassUtils.castStream(NodeSetFactoryExtension.class, ext)).collect(Collectors.toSet());
    //            return new DiskLoadResult(cacheData, nodeSetFactoryExtensions);
    //        } catch (DatabindException ex) {
    //            NodeLogger.getLogger(NodeMetadataCachePersistor.class)
    //                .warn(() -> "Cannot deserialize node metadata cache in %s.".formatted(path), ex);
    //        } catch (IOException ex) {
    //            NodeLogger.getLogger(NodeMetadataCachePersistor.class)
    //                .warn(() -> "Cannot read node metadata cache in %s.".formatted(path), ex);
    //        }
    //        // TODO
    //        return null;
    //    }

    //    /**
    //     * Always writes to disk - assumes this is only called when needed.
    //     */
    //    void write(final Bundle bundle, final NodeProperties[] metadata) {
    //        try {
    //            Path dataFile = cacheDataFilePath(bundle)
    //                .orElseThrow(() -> new IOException("Cannot create data file path for bundle %s".formatted(bundle)));
    //            Files.createDirectories(dataFile.getParent());
    //            var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
    //            // use a zipped output stream to save space
    //            try (var zos = new java.util.zip.GZIPOutputStream(Files.newOutputStream(dataFile))) {
    //                mapper.writeValue(zos, metadata);
    //            }
    //            writeBundleCacheState(bundle);
    //            NodeLogger.getLogger(NodeMetadataCachePersistor.class)
    //                .debug(() -> "Written property of %s nodes to %s".formatted(metadata.length, dataFile));
    //        } catch (IOException ex) {
    //            if (!cannotWriteFilesWarningIssued) {
    //                NodeLogger.getLogger(NodeMetadataCachePersistor.class)
    //                    .warn("Cannot write node metadata cache to KNIME installation directory.", ex);
    //                cannotWriteFilesWarningIssued = true;
    //            }
    //        }
    //    }

    private void writeBundleCacheState(final Bundle bundle) throws IOException {
        var optPath = cacheStateFilePath(bundle);
        if (optPath.isEmpty()) {
            throw new IOException("Could not write node metadata cache state for bundle %s".formatted(bundle));
        }
        new Fingerprint(bundle.getVersion()).write(optPath.get());
    }

    private static Optional<Path> cacheStateFilePath(final Bundle bundle) {
        return Optional.ofNullable(bundle.getBundleContext())//
            .map(bc -> bc.getDataFile(VERSION_FILE_NAME)).map(File::toPath);
    }

    /** @return the path to the bundle data directory provided by the bundle context */
    private static Optional<Path> cacheDataFilePath(final Bundle bundle) {
        try {
            return Optional.ofNullable(bundle.getBundleContext())//
                .map(bc -> bc.getDataFile(CACHE_FILE_NAME)).map(File::toPath);
        } catch (IllegalStateException ex) {
            NodeLogger.getLogger(DiskBasedNodeSpecCache.class).warn("Bundle context for %s no longer valid", ex);
            return Optional.empty();
        }
    }

    /**
     * @param bundle
     * @param metadata
     * @param nodeSetMetadata
     * @return
     */
    public void write(final Bundle bundle, final Map<NodeFactoryExtension, NodeMetadata> metadata,
        final Map<NodeSetFactoryExtension, List<NodeMetadata>> nodeSetMetadata) {
        // TODO Auto-generated method stub
    }

    //  private static void writeBundleCacheData(final Bundle bundle, final List<NodeMetadata> bundleNodeMetadata)
    //  throws IOException {
    //  var optPath = cacheDataFilePath(bundle);
    //  if (optPath.isEmpty()) {
    //      throw new IOException("Could not write node metadata cache data for bundle %s".formatted(bundle));
    //  }
    //  // serialize to json
    //  var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
    //  final String serialized = mapper.writeValueAsString(bundleNodeMetadata);
    //
    //  Files.writeString(optPath.get(), serialized);
    //}

    //    /**
    //     * Persists the cache to disk. This method is called for each bundle with an unusable cache.
    //     */
    //    static void persistCache(final NodeMetadataCache cache) {
    //        cache.getAllByBundle().entrySet().stream().parallel().forEach(e -> {
    //            try {
    //                var bundle = e.getKey();
    //                var bundleNodeMetadata = e.getValue();
    //                writeBundleCacheData(bundle, bundleNodeMetadata);
    //                writeBundleCacheState(bundle);
    //            } catch (IOException ex) {
    //                // TODO log
    //            }
    //        });
    //    }

    /**
     * Used to determine whether cache content is fresh.
     *
     * @param cacheFormatVersion identifies the schema of the persisted data
     * @param version identifies the bundle version
     */
    private static record Fingerprint(String cacheFormatVersion, Version version) {

        private Fingerprint(final Version version) {
            this(NodeSpec.SERIALIZATION_VERSION, version);
        }

        /**
         * @param path to the file in the bundle data area
         * @return version for which the cached content is valid. Optional.empty if the file cannot be read.
         */
        static Optional<Fingerprint> read(final Path path) {
            try {
                final var lines = Files.readAllLines(path);
                if (lines.size() != 2) {
                    // invalid cache state file
                    return Optional.empty();
                }
                final var cacheFormatVersion = lines.get(0);
                final var version = Version.valueOf(lines.get(1));
                return Optional.of(new Fingerprint(cacheFormatVersion, version));
            } catch (NoSuchFileException ex) {
                // file not present is accepted that's why we return Optional
                NodeLogger.getLogger(DiskBasedNodeSpecCache.class).debug(() -> ex);
                return Optional.empty();
            } catch (IOException ex) {
                NodeLogger.getLogger(DiskBasedNodeSpecCache.class)
                    .warn(() -> "Cannot read node metadata cache state in %s.".formatted(path), ex);
                return Optional.empty();
            }
        }

        void write(final Path path) throws IOException {
            Files.writeString(path, "%s%n%s".formatted(cacheFormatVersion, version));
        }
    }

    /**
     * @param ext the extension to look for
     * @return the node specs for the given extension as cached on disk or empty if no data has been cached yet or the
     *         cached data is outdated (bundle version or node spec format have changed)
     */
    static Optional<List<NodeSpec>> get(final INodeFactoryExtension ext) {
        try {
            initialized.await();
            return Optional.ofNullable(specs.get(ext));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null; // NOSONAR nothing should be returned
        }
    }
}
