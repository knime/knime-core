/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */

package org.knime.base.node.preproc.matcher;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionCellFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


/**
 * An <code>ItemSetMatcher</code> matches only one element of a
 * transaction list.
 * <code>ItemSetMatcher</code> are hierarchically organized.
 * Thus the item sets and the transaction lists need to be sorted in
 * the same way.
 * An <code>ItemSetMatcher</code> hierarchy for the two item sets (A,B,D; B,C,E)
 * A->B->D; B->C->E.
 * @author Tobias Koetter, University of Konstanz
 */
public class SubsetMatcher implements Comparable<SubsetMatcher> {

    private final Comparator<DataCell> m_comparator;

    private final List<SubsetMatcher> m_children =
        new ArrayList<SubsetMatcher>(2);

    private boolean m_end = false;

    private final DataCell m_item;

    private boolean m_sortChildren = true;

    /**Constructor for class ItemSetMatcher.
     * @param item the item this matcher matches
     * @param comparator the comparator to use
     */
    public SubsetMatcher(final DataCell item,
            final Comparator<DataCell> comparator) {
        if (item == null) {
            throw new NullPointerException("item must not be null");
        }
        if (comparator == null) {
            throw new NullPointerException("comparator must not be null");
        }
        m_item = item;
        m_comparator = comparator;
    }

    /**
     * @param itemSet the item set to create the child matcher for
     * @param idx2process the index of the item to match
     */
    public void appendChildMatcher(final DataCell[] itemSet,
            final int idx2process) {
        if (idx2process >= itemSet.length) {
            //nothing left to process this the last item in the item set itself
            m_end = true;
            return;
        }
        int idx = idx2process;
        final DataCell subItem = itemSet[idx++];
        for (final SubsetMatcher matcher : m_children) {
            if (matcher.matches(subItem)) {
                matcher.appendChildMatcher(itemSet, idx);
                //their can be only one matcher an we have found it
                return;
            }
        }
        //no matcher exists yet for the given item create a new one and proceed
        final SubsetMatcher newChildMatcher =
            new SubsetMatcher(subItem, m_comparator);
        m_children.add(newChildMatcher);
        m_sortChildren = true;
        newChildMatcher.appendChildMatcher(itemSet, idx);
    }


    /**
     * @return <code>true</code> if this matcher matches the last
     * item of an item set
     */
    public boolean isEnd() {
        return m_end;
    }


    /**
     * @return the children {@link SubsetMatcher} of this matcher if any
     */
    public Collection<SubsetMatcher> getChildren() {
        return Collections.unmodifiableCollection(m_children);
    }

    /**
     * @return the children sorted
     */
    private synchronized List<SubsetMatcher> getSortedChildren() {
        if (m_sortChildren) {
            Collections.sort(m_children);
            m_sortChildren = false;
        }
        return m_children;
    }

    /**
     * @param item the item to match
     * @return <code>true</code> if the matcher matches the given item
     */
    public boolean matches(final DataCell item) {
        return m_item.equals(item);
    }

    /**
     * @return the item this matcher matches
     */
    public DataCell getItem() {
        return m_item;
    }

    /**
     * @param val the value to compare with the one this matcher matches
     * @return a negative integer, zero, or a positive integer as the
     *         matching item is less than, equal to, or greater than the
     *         given value.
     */
    public int compare(final DataCell val) {
        return m_comparator.compare(m_item, val);
    }

    /**
     * @param transactionItems the sorted transaction item list
     * @param idx the index to process
     * @param matchingSets all matching item sets
     * @param items all processed items
     * @param mismatches the {@link MismatchCounter}
     */
    public void match(final DataCell[] transactionItems, final int idx,
            final Collection<SetMissmatches> matchingSets,
            final Collection<DataCell> items,
            final MismatchCounter mismatches) {
        //use this method to ensure that the children are sorted
        final List<SubsetMatcher> sortedChildren = getSortedChildren();
        final int itemSize = transactionItems.length;
        int itemIdx = idx;
        if (itemIdx < itemSize && matches(transactionItems[itemIdx])) {
            //the item matches this matcher process with the next item
            itemIdx++;
        } else if (!mismatches.mismatch()) {
            //this item does not match and we do not have any mismatches left
            return;
        }
        //add this item to the items list first
        items.add(m_item);
        if (isEnd()) {
            //this is an end item create an item set with all
            //previous items an this item
            matchingSets.add(new SetMissmatches(
                    CollectionCellFactory.createSetCell(items),
                    mismatches.getMismatches()));
        }
        //try to match the sorted transaction items and the sorted children
        //until all items or all matchers are processed
        int matcherStartIdx = 0;
        while (itemIdx < itemSize
                && matcherStartIdx < sortedChildren.size()) {
            final DataCell subItem = transactionItems[itemIdx];
            //try to match the current item with all remaining
            //child matchers
            for (int i = matcherStartIdx; i < sortedChildren.size(); i++) {
                final SubsetMatcher matcher = sortedChildren.get(i);
                final int result = matcher.compare(subItem);
                if (result > 0) {
                    //the smallest matcher is bigger then this item
                    //exit the loop and continue with the next bigger item
                    break;
                }
                //the item either matches or is bigger than the matcher
                //if it is bigger the matcher increases the mismatch and
                //processes the next item
                matcher.match(transactionItems, itemIdx, matchingSets,
                        new LinkedList<DataCell>(items), mismatches.copy());
                matcherStartIdx++;
            }
            //go to the next index
            itemIdx++;
        }
        if (itemIdx >= itemSize && mismatches.mismatchesLeft()
                && matcherStartIdx < sortedChildren.size()) {
            //no items left in the item set to match but we have still
            //some mismatches left that we have to use on the remaining
            //children matchers
            for (int i = matcherStartIdx; i < sortedChildren.size(); i++) {
                final SubsetMatcher matcher = sortedChildren.get(i);
                matcher.match(transactionItems, itemIdx, matchingSets,
                        new LinkedList<DataCell>(items), mismatches.copy());
                matcherStartIdx++;
            }
        }
    }

    /**
    * @param itemSets all item sets that are matched by the given matchers
    * @param previousItems the previous items
    */
   public void getItemSets(
           final Collection<DataCell> itemSets,
           final Collection<DataCell> previousItems) {
       //add this item to the items list first
       previousItems.add(m_item);
       if (isEnd()) {
           //this is an end item create an item set with all previous items
           //an this item
           itemSets.add(CollectionCellFactory.createListCell(previousItems));
       }
       for (final SubsetMatcher child : m_children) {
           child.getItemSets(itemSets, new LinkedList<DataCell>(previousItems));
       }
   }

   /**

    /**
     * @return the number of children and sub children
     */
    public int getChildCount() {
        int childCount = 1;
        for (final SubsetMatcher child : m_children) {
            childCount += child.getChildCount();
        }
        return childCount;
    }


    /**
     * @return the number of item sets that are matched by this matcher and its
     * children
     */
    public int getItemSetCount() {
        int itemSetCount = isEnd() ? 1 : 0;
        for (final SubsetMatcher child : m_children) {
            itemSetCount += child.getItemSetCount();
        }
        return itemSetCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(m_item);
        buf.append("->");
        buf.append(isEnd());
        buf.append(" Child count: ");
        buf.append(getChildCount());
        buf.append(" Item set count: ");
        buf.append(getItemSetCount());
        return buf.toString();
    }

    /**
     * Compares two {@link SubsetMatcher} objects based on the item they match
     * using their comparator.
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final SubsetMatcher o) {
        if (o == this) {
            return 0;
        }
        if (o == null) {
            return 1;
        }
        return compare(o.getItem());
    }
}
