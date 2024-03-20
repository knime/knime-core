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
 *   07.07.2005 (mb): created
 *   20.06.2006 (bw & po): reviewed
 */
package org.knime.core.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.BlobWrapperDataCell;

/**
 * <p>
 * Abstract base class of all <code>DataCell</code>s, which acts as a container
 * for arbitrary values and defines the common abilities all cells must provide,
 * that is: retrieve the cell type, a string representation of the value,
 * find out if this cell is missing, and test whether it is equal to another
 * one.
 * </p>
 *
 * <p>
 * Subclasses have to implement at least one interface
 * derived from {@link DataValue}. DataCells must be
 * read-only, i.e. setter methods must not be implemented and
 * objects returned by any of the get methods must be immutable.
 * </p>
 *
 * <p>
 * This class implements {@link java.io.Serializable}. However, if
 * you define a custom <code>DataCell</code> implementation, consider to
 * implement a factory that takes care of reading and writing a cell to a
 * {@link DataCellDataInput} or
 * {@link DataCellDataOutput} source. Ordinary Java
 * serialization is considerably slower than implementing your own
 * {@link DataCellSerializer}. Register this serializer at the extension point
 * <code>org.knime.core.DataType</code>.
 * </p>
 *
 * <a name="preferredvalueclass"></a>
 * <p>
 * Since <code>DataCell</code>s may implement several {@link DataValue}
 * interfaces but only one is the <i>preferred</i> value class, the order of the implemented interfaces is important.
 * The first implemented {@link DataValue} interface is taken as the preferred value class. The {@link DataType}
 * associated with this cell implementation will then provide the renderer, icon, and comparator of this preferred
 * value.
 * </p>
 *
 * <p>
 * For further details on data types, see also the <a
 * href="package-summary.html">package description</a> and the <a
 * href="doc-files/newtypes.html">manual</a> on defining new cell types.
 *
 * @see DataType
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class DataCell implements DataValue, Serializable {
    private static final long serialVersionUID = 7415713938002260608L;

    private static final Map<Class<? extends DataCell>, DataType> classToTypeMap = new ConcurrentHashMap<>(100, 1 / 3f);

    @Override
    public DataCell materializeDataCell() {
        return this;
    }

    /**
     * Returns this cell's <code>DataType</code>. This method is provided for
     * convenience only, it is a shortcut for
     * <code>DataType.getType(o.getClass())</code>, where <i>o</i> is the
     * runtime <code>DataCell</code> object.
     *
     * @return the <code>DataType</code>
     * @see DataType#getType(Class)
     */
    public final DataType getType() {
        final var type = classToTypeMap.get(getClass());
        if (type != null) {
            return type;
        }
        DataType elementType = null;
        List<Class<? extends DataValue>> adapterValueList = null;
        if (this instanceof CollectionDataValue coll) {
            elementType = coll.getElementType();
        }
        if (this instanceof AdapterValue adapter) {
            adapterValueList = new ArrayList<>(adapter.getAdapterMap().keySet());
        }
        final var newType = DataType.getType(getClass(), elementType, adapterValueList);
        if (adapterValueList == null && elementType == null) {
            classToTypeMap.put(getClass(), newType);
        }
        return newType;
    }

    /**
     * Returns <code>true</code> if this represents missing cell,
     * <code>false</code> otherwise. The default implementation returns
     * <code>false</code>. If you need a missing cell use the static method
     * {@link DataType#getMissingCell()}.
     *
     * @return <code>true</code> if the cell represents a missing value,
     *         <code>false</code> otherwise
     * @see DataType#getMissingCell()
     */
    public boolean isMissing() {
        return isMissingInternal();
    }

    /**
     * Returns <code>true</code> if this represents missing cell,
     * <code>false</code> otherwise. The default implementation returns
     * <code>false</code>. This method is package-scope and intended
     * to be overridden by DataCell implementations implementing
     * {@link MissingValue}.
     *
     * @return <code>true</code> if the cell represents a missing value,
     *         <code>false</code> otherwise
     */
    boolean isMissingInternal() {
        return false;
    }

    /**
     * Returns the String representation of this cell's value.
     *
     * @return a String representation of this cell's value
     */
    @Override
    public abstract String toString();

    /**
     * Implements the equals method which returns <code>true</code> only if both
     * cells are of the same class and {@link #equalsDataCell(DataCell)} returns
     * <code>true</code>. For that, this final method calls the type specific
     * {@link #equalsDataCell(DataCell)} method, which all derived
     * <code>DataCell</code>s must provide. This method handles the missing
     * value and <code>null</code> cases, in all other cases it delegates
     * to the specific method.
     *
     * @param o the other object to check
     * @return <code>true</code> if this instance and the given object are
     *         instances of the same class and of equal value (or both
     *         representing missing values)
     */
    @Override
    public final boolean equals(final Object o) {

        // true of called on the same objects
        if (this == o) {
            return true;
        }
        // also handles null cases
        if (!(o instanceof DataCell)) {
            return false;
        }
        DataCell thisDelegate = this;
        while (thisDelegate instanceof BlobWrapperDataCell blob) {
            thisDelegate = blob.getCell();
        }

        DataCell otherDelegate = (DataCell)o;
        while (otherDelegate instanceof BlobWrapperDataCell blob) {
            otherDelegate = blob.getCell();
        }

        // if both cells are missing they are equal
        if (thisDelegate.isMissing() && otherDelegate.isMissing()) {
            return true;
        }

        // if only one of both cells is missing they cannot be equal
        if (thisDelegate.isMissing() || otherDelegate.isMissing()) {
            return false;
        }

        // only cells of identical classes can possibly be equal
        if (thisDelegate.getClass().equals(otherDelegate.getClass())) {
            // now call the datacell class specific equals method
            boolean b = thisDelegate.equalsDataCell(otherDelegate);
            assert(!b || thisDelegate.hashCode() == otherDelegate.hashCode()) : "\"hashCode\" implementation of "
                + thisDelegate.getClass() + " is not compatible with equalsDataCell. Please check the implementations!";
            return b;
        } else if (thisDelegate.getType().getPreferredValueClass()
            .equals(otherDelegate.getType().getPreferredValueClass())) {
            // Unequal cell classes but the preferred value class is identical. This happens e.g. for blob and non-blob
            // cells or for normal and adapter cells.
            assert thisDelegate.equalContent(otherDelegate) == otherDelegate
                .equalContent(thisDelegate) : "\"equalContent\" implementation of " + thisDelegate.getClass() + " and "
                    + otherDelegate.getClass() + " behave differently. Please check the implementations!";
            return thisDelegate.equalContent(otherDelegate);

        }

        // not of the same class or content
        return false;
    }

    /**
     * Derived classes implement their specific equals function here. The
     * argument is guaranteed to be not <code>null</code> or a missing value,
     * to be of the same class like this.
     *
     * @param dc the cell to compare this to
     * @return <code>true</code> if this is equal to the argument,
     *         <code>false</code> if not
     */
    protected abstract boolean equalsDataCell(final DataCell dc);

    /**
     * This method is called when two cell of different classes but with the same preferred value are compared (e.g.
     * SdfCell and SdfAdapterCell or MolCell and MolBlobCell). All such cell classes should override this method
     * with the same implementation, usually by comparing both preferred values. You can assume the this cell and the
     * other cell have the same preferred value class, i.e. you can cast the argument to the preferred value.
     * <br>
     * The default implementation returns <code>false</code>.
     *
     * @param otherValue the other data value
     * @return <code>true</code> if the content of both cells is the same, <code>false</code> otherwise
     * @since 3.0
     */
    protected boolean equalContent(final DataValue otherValue) {
        return false;
    }

    /**
     * This method must be implemented in order to ensure that two equal
     * <code>DataCell</code> objects return the same hash code. Note that two cells can be equal according to
     * {@link #equalsDataCell(DataCell)} and {@link #equalContent(DataValue)} so two different cells implementations
     * with the same preferred value must have the same hash code.
     *
     * @return the hash code of your specific <code>DataCell</code>
     *
     * @see java.lang.Object#hashCode()
     * @see #equals
     */
    @Override
    public abstract int hashCode();

}
