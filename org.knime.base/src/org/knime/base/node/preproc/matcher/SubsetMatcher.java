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
 */

package org.knime.base.node.preproc.matcher;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionCellFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;


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
public class SubsetMatcher {

    private final Comparator<DataCell> m_comparator;

    private final Collection<SubsetMatcher> m_children =
        new LinkedList<SubsetMatcher>();

    private boolean m_end = false;

    private final DataCell m_item;
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
     *         given value is less than, equal to, or greater than the
     *         matching item.
     */
    public int compare(final DataCell val) {
        return m_comparator.compare(val, m_item);
    }

    /**
     * @param transactionItems the sorted transaction item list
     * @param idx the index to process
     * @param itemSets all matching item sets
     * @param items all processed items
     */
    public void match(final DataCell[] transactionItems, final int idx,
            final Collection<DataCell> itemSets,
            final Collection<DataCell> items) {
        final int itemSize = transactionItems.length;
        if (idx >= itemSize) {
            return;
        }
        int itemIdx = idx;
        final DataCell item = transactionItems[itemIdx++];
        if (matches(item)) {
          //add this item to the items list first
            items.add(m_item);
            if (isEnd()) {
                //this is an end item create an item set with all
                //previous items an this item
                itemSets.add(CollectionCellFactory.createSetCell(items));
            }
            final Collection<SubsetMatcher> processedMatcher =
                new HashSet<SubsetMatcher>(m_children.size());
            //try to match the transaction items until all items or all
            //matchers are processed
            while (itemIdx < itemSize
                    && processedMatcher.size() != m_children.size()) {
                final DataCell subItem = transactionItems[itemIdx];
                //try to match the current item with all remaining
                //child matchers
                for (final SubsetMatcher matcher : m_children) {
                    if (processedMatcher.contains(matcher)) {
                        continue;
                    }
                    final int result = matcher.compare(subItem);
                    if (result == 0) {
                        //we have found the only matching matcher and can return
                        matcher.match(transactionItems, itemIdx, itemSets,
                                new LinkedList<DataCell>(items));
                        return;
                    } else if (result > 0) {
                        //the sub item is bigger than the match
                        //since all subsequent items are bigger than the
                        //current one the matcher will match none of the
                        //following
                        processedMatcher.add(matcher);
                    }
                }
                //go to the next index
                itemIdx++;
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
        int itemSetCount = 0;
        if (isEnd()) {
            itemSetCount++;
        }
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
}
