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
 *   15.02.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.List;

import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;

import com.google.common.collect.ArrayListMultimap;

/**
 * This class creates and stores TreeNodeSignature for reuse in the building process of new trees.
 * Needs to be synchronized because trees are build in parallel.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class TreeNodeSignatureFactory {
    private static final int DEFAULT_CAPACITY = 2048;
    private static final int DEFAULT_NUMBER_OF_CHILDREN = 2;

    private final ArrayListMultimap<TreeNodeSignature, TreeNodeSignature> m_knownSignatures;

    /**
     * Creates a TreeNodeSignature that initially is able to store <b>capacity</b> many signatures.
     * In case the depth of trees is limited, this can be calculated easily (2^^numLevels).
     * @param capacity
     */
    public TreeNodeSignatureFactory(final int capacity) {
        m_knownSignatures = ArrayListMultimap.create(2 * capacity / 3, DEFAULT_NUMBER_OF_CHILDREN);
    }

    /**
     *
     */
    public TreeNodeSignatureFactory() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * @return the standard root signature
     */
    public TreeNodeSignature getRootSignature() {
        return TreeNodeSignature.ROOT_SIGNATURE;
    }

    /**
     * Looks up whether a signature with given <b>parent</b> and <b>childIndex</b> exists and returns
     * it if it does and otherwise creates a new signature stores it and returns the newly created signature.
     * This function is synchronized because different threads will access it during the tree building process.
     *
     * @param parentSignature
     * @param childIndex
     * @return signature for child node
     */
    public synchronized TreeNodeSignature getChildSignatureFor(final TreeNodeSignature parentSignature, final byte childIndex) {
        List<TreeNodeSignature> knownChildren = m_knownSignatures.get(parentSignature);
        // case that the child signature does not exist yet
        if (knownChildren.size() <= childIndex) {
            TreeNodeSignature childSignature = parentSignature.createChildSignature(childIndex);
            knownChildren.add(childIndex, childSignature);
            return childSignature;
        } else {
            // there are already signatures registered for parent
            TreeNodeSignature childSignature = knownChildren.get(childIndex);
            // Unlikely but possible case that a signature with larger childIndex already exists
            // but not the signature we currently want to get
            // (unlikely because we usually build the children in order)
            if (childSignature == null) {
                childSignature = parentSignature.createChildSignature(childIndex);
                knownChildren.add(childIndex, childSignature);
            }
            return childSignature;
        }
    }


}
