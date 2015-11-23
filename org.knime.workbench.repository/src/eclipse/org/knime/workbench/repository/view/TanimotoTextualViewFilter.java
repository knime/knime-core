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
 * ------------------------------------------------------------------------
 */

package org.knime.workbench.repository.view;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.Viewer;
import org.knime.workbench.repository.model.AbstractNodeTemplate;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.Root;

/**
 * A filter for items which computes the tanimoto distance to a given query and filters all nodes which have a distance
 * greater or equal to UPPER_DISTANCE_BOUND=0.6.
 *
 * @author Marcel Hanser, KNIME.com, Zurich, Switzerland
 */
final class TanimotoTextualViewFilter extends TextualViewFilter {
    /**
     * Upper excluding bound of distances to a query a node may have to be labeled as a match.
     */
    private static final double UPPER_DISTANCE_BOUND = 0.85;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

        // this means that the filter has been cleared
        if (!hasNonEmptyQuery()) {
            return true;
        }
        // call helper method
        return doSelect(parentElement, element, true);
    }

    /**
     * Copied from {@link TextualViewFilter}.
     */
    @Override
    protected boolean doSelect(final Object parentElement, final Object element, final boolean recurse) {
        boolean selectThis = false;
        // Node Template : Match against name
        if (element instanceof AbstractNodeTemplate) {

            // check against node name
            selectThis = match(((AbstractNodeTemplate)element).getName());
            if (element instanceof MetaNodeTemplate) {
                // with meta nodes also check the name of the workflow manager
                selectThis |= match(((MetaNodeTemplate)element).getManager().getName());
            }
            if (selectThis) {
                return true;
            }
            // we must also check towards root, as we want to include all
            // children of a selected category
            IRepositoryObject temp = (IRepositoryObject)parentElement;
            while (!(temp instanceof Root)) {

                // check parent category, but do *not* recurse !!!!
                if (doSelect(temp.getParent(), temp, false)) {
                    return true;
                }
                temp = temp.getParent();
            }
        } else
        // Category: Match against name and children
        if (element instanceof Category) {
            // check against node name
            selectThis = match(((Category)element).getName());
            if (selectThis) {
                return true;
            }

            // check recursively against children, if needed
            if (recurse) {
                Category category = (Category)element;
                IRepositoryObject[] children = category.getChildren();
                for (int i = 0; i < children.length; i++) {
                    // recursively check. return true on first matching child
                    if (doSelect(category, children[i], true)) {
                        return true;
                    }

                }
            }
        }

        return false;
    }

    /**
     * @param test String to test
     * @return <code>true</code> if the test is contained in the m_query String (ignoring case)
     */
    @Override
    protected boolean match(final String test) {
        if (test == null) {
            return false;
        }
        boolean contains = test.toUpperCase().contains(getQueryString());
        if (!contains) {
            return computeTanimotoBiGramDistance(test, getQueryString()) < UPPER_DISTANCE_BOUND;
        } else {
            return true;
        }
    }


    /**
     * Copied from the Tanimoto BiGram distance from the distmatrix package.
     */
    private static double computeTanimotoBiGramDistance(final String textA, final String textB) {

        String a = textA.toUpperCase();
        String b = textB.toUpperCase();

        Set<String> gramsA = split(a, 2);
        Set<String> gramsB = split(b, 2);

        int nominator = cardinalityOfIntersection(gramsA, gramsB);
        int inAButNotInB = cardinalityOfRelativeComplement(gramsA, gramsB);
        int inBButNotInA = cardinalityOfRelativeComplement(gramsB, gramsA);

        double denominator = nominator + inAButNotInB + inBButNotInA;

        if (denominator > 0) {
            return 1.0 - nominator / denominator;
        } else {
            return 1.0;
        }
    }

    private static int cardinalityOfIntersection(final Set<String> a, final Set<String> b) {
        int toReturn = 0;
        for (String gram : a) {
            if (b.contains(gram)) {
                toReturn++;
            }
        }
        return toReturn;
    }

    private static int cardinalityOfRelativeComplement(final Set<String> a, final Set<String> b) {
        int toReturn = 0;
        for (String gram : a) {
            if (!b.contains(gram)) {
                toReturn++;
            }
        }
        return toReturn;
    }

    private static Set<String> split(final String a, final int count) {
        Set<String> toReturn = new HashSet<String>(a.length() > 1 ? a.length() - 1 : 12);

        for (int i = 0; i < a.length() - count + 1; i++) {
            toReturn.add(a.substring(i, i + count));
        }
        return toReturn;
    }

    /**
     * @return a comparator computing the tanimoto n-gram distance with each given string. The one with the smaller
     *         distance wins.
     */
    @Override
    public Comparator<String> createComparator() {
        // Actually there seems to be a concurrent execution somewhere,
        // so i decided to use a thread safe implementation.
        if (hasNonEmptyQuery()) {
            final String currentQuery = getQueryString();

            return new Comparator<String>() {

                @Override
                public int compare(final String o1, final String o2) {
                    double computeTanimotoBiGramDistanceO1 = computeTanimotoBiGramDistance(currentQuery, o1);
                    double computeTanimotoBiGramDistanceO2 = computeTanimotoBiGramDistance(currentQuery, o2);
                    return Double.compare(computeTanimotoBiGramDistanceO1, computeTanimotoBiGramDistanceO2);
                }
            };

        }
        return String.CASE_INSENSITIVE_ORDER;
    }
}
