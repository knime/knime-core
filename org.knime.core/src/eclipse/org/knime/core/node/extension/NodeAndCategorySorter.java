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
 * ---------------------------------------------------------------------
 *
 * Created on 15.11.2013 by thor
 */
package org.knime.core.node.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.extension.NodeAndCategorySorter.NodeOrCategory;

/**
 * This utility class sorts the children of a category (or the repository root) according to the following order:
 * <ol>
 * <li>All KNIME categories</li>
 * <li>All 3rd-party categories</li>
 * <li>All KNIME nodes</li>
 * <li>All 3rd-party nodes</li>
 * </ol>
 *
 * Within each of the four groups the following ordering criteria apply:
 * <ol>
 * <li>"after" relationships</li>
 * <li>lexicographical order of the name</li>
 * </ol>
 *
 * The after-IDs correspond to the IDs of categories or nodes. If no after-ID is specified this category/node is sorted
 * to the front. If the magic id <tt>_last_</tt> is specified, it is sorted to the back.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 *
 * @param <T> the node or category type
 * @since 2.9
 * @noreference This class is not intended to be referenced by clients.
 */
public final class NodeAndCategorySorter<T extends NodeOrCategory<T>> {

    private static final String LAST_ID = "_last_";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeAndCategorySorter.class);

    // all KNIME plug-ins and the free Marvin extension
    private static final Pattern KNIME_PATTERN =
        Pattern.compile("^(?:(?:org|com)\\.knime\\..+|jp\\.co\\.infocom\\.cheminfo\\.marvin)");

    private NodeAndCategorySorter() {
        // utility class
    }

    /**
     * Helper method to sort a stream of {@link CategoryExtension}s.
     *
     * @param extensions
     * @return a sorted stream
     */
    public static Stream<CategoryExtension> sortCategoryExtensions(final Stream<CategoryExtension> extensions) {
        return sortNodesAndCategories(extensions.map(CategoryExtensionInternal::new).collect(Collectors.toList()))
            .stream().map(s -> s.m_ext);
    }

    /**
     * Helper method to a list of {@link NodeOrCategory}-items.
     *
     * @param items the list to sort
     * @return the new, sorted, list
     */
    public static <T extends NodeOrCategory<T>> List<T> sortNodesAndCategories(final List<T> items) {
        NodeAndCategorySorter<T> sorter = new NodeAndCategorySorter<>();

        List<T> result = new ArrayList<>(items.size());

        List<T> knimeCategories = filter(items,
            object -> !object.isNode() && KNIME_PATTERN.matcher(object.getContributingPlugin()).matches());
        result.addAll(sorter.sortItems(knimeCategories));

        List<T> externalCategories = filter(items,
            object -> !object.isNode() && !KNIME_PATTERN.matcher(object.getContributingPlugin()).matches());
        result.addAll(sorter.sortItems(externalCategories));

        List<T> knimeNodes =
            filter(items, object -> object.isNode() && KNIME_PATTERN.matcher(object.getContributingPlugin()).matches());
        result.addAll(sorter.sortItems(knimeNodes));

        List<T> externalNodes = filter(items,
            object -> object.isNode() && !KNIME_PATTERN.matcher(object.getContributingPlugin()).matches());
        result.addAll(sorter.sortItems(externalNodes));

        return result;
    }

    private List<T> sortItems(final List<T> items) {
        if (items.size() < 2) {
            return items;
        }


        // first build a map between category/node IDs and the respective objects
        Item first = new Item(null);
        Item last = new Item(null);
        Map<String, Item> idToItemMap = createIdToItemMap(items, first, last);

        /*
         * Now we iterate over all items and check their after-IDs. Each item is added to the successor list of
         * its "predecessor" (the object specified in the after-ID). If no after-ID has been specified the item will
         * be a successor of the "first" item. If the specified after-ID does not exist, it will be a successor of the
         * "last" item.
         */
        for (Item item : idToItemMap.values()) {
            if (item.repositoryObject == null) {
                continue;
            }

            String afterId = item.repositoryObject.getAfterID();
            if (afterId != null) {
                // insert relation according to "afterID"
                Item predecessor = idToItemMap.get(afterId);
                if (predecessor != null) {
                    predecessor.successors.add(item);
                } else {
                    LOGGER.coding("After-ID '" + afterId + "' of [" + item.repositoryObject
                        + "] does not exist - in plug-in " + item.repositoryObject.getContributingPlugin());
                    last.successors.add(item);
                }
            } else {
                last.successors.add(item);
            }
        }
        if (first.successors.isEmpty()) {
            // This means we have a cycle somewhere because no item is attached to the virtual root, i.e. has an
            // empty after-ID. We randomly pick the first item in the original list.
            first.successors.add(idToItemMap.get(items.get(0).getID()));
        }

        first.successors.add(last);

        // now sort each successor list independently (starting with the "first" list) and add the objects to the
        // result list
        List<T> result = new ArrayList<>();
        sortItemSuccessors(first, result);
        return result;
    }

    private Map<String, Item> createIdToItemMap(final List<T> items, final Item first, final Item last) {
        Map<String, Item> idToItemMap = new HashMap<>();
        idToItemMap.put("", first);
        for (T o : items) {
            Item existing = idToItemMap.get(o.getID());

            if (existing != null) {
                LOGGER.coding(
                    "Duplicate repository entry IDs detected: [" + existing.repositoryObject + "] and [" + o + "].");
                idToItemMap.put(o.getID() + "_" + System.identityHashCode(o), new Item(o));
            } else {
                idToItemMap.put(o.getID(), new Item(o));
            }
        }
        idToItemMap.put(LAST_ID, last);
        return idToItemMap;
    }

    private void sortItemSuccessors(final Item item, final List<T> result) {
        if (item.processed) {
            LOGGER.coding("A cycle in after-relationships involving [" + item.repositoryObject
                + "] was detected. Please check the relations.");
            return;
        }

        if (item.repositoryObject != null) {
            result.add(item.repositoryObject);
            item.processed = true;
        }
        Collections.sort(item.successors);
        for (Item i : item.successors) {
            sortItemSuccessors(i, result);
        }
    }

    private static <T> List<T> filter(final Collection<T> target, final IPredicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T element : target) {
            if (predicate.applies(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Summarizes the properties that nodes and categories have in common and also makes them comparable.
     *
     * @param <C> the type to compare to
     */
    public static interface NodeOrCategory<C extends NodeOrCategory<C>> extends Comparable<C> {

        /**
         * @return node or category id
         */
        String getID();

        /**
         * @return node or category name
         */
        String getName();

        /**
         * @return a identifier for the plugin that contributes the node or category
         */
        String getContributingPlugin();

        /**
         * @return if <code>true</code> it represents a node, otherwise a category
         */
        boolean isNode();

        /**
         * @return the after id which allows the extension contributer to control the sorting
         */
        String getAfterID();
    }

    private interface IPredicate<T> {
        boolean applies(T object);
    }

    @SuppressWarnings("java:S116")
    private class Item implements Comparable<Item> {
        final T repositoryObject;

        final List<Item> successors = new ArrayList<>();

        boolean processed = false;

        Item(final T repoObject) {
            this.repositoryObject = repoObject;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return ((repositoryObject != null) ? repositoryObject.getName() : "DUMMY");
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("java:S1210")
        @Override
        public int compareTo(final Item o) {
            if ((this.repositoryObject != null) && (o.repositoryObject != null)) {
                return this.repositoryObject.compareTo(o.repositoryObject);
            } else if (this.repositoryObject != null) {
                return -1;
            } else if (o.repositoryObject != null) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private static final class CategoryExtensionInternal implements NodeOrCategory<CategoryExtensionInternal> {

        private CategoryExtension m_ext;

        private CategoryExtensionInternal(final CategoryExtension ext) {
            m_ext = ext;
        }

        @Override
        public int compareTo(final CategoryExtensionInternal o) {
            int nameOrder = getName().compareTo(o.getName());
            return (nameOrder == 0 ? getID().compareTo(o.getID()) : nameOrder);
        }

        @Override
        public String getID() {
            return m_ext.getLevelId();
        }

        @Override
        public String getName() {
            return m_ext.getName();
        }

        @Override
        public String getContributingPlugin() {
            return m_ext.getContributingPlugin();
        }

        @Override
        public boolean isNode() {
            return false;
        }

        @Override
        public String getAfterID() {
            return m_ext.getAfterID();
        }

    }

}
