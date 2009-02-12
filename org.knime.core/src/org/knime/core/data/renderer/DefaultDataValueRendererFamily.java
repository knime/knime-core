/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.data.renderer;

import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import javax.swing.JList;
import javax.swing.JTable;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;


/**
 * Default container for <code>DataValueRenderer</code>. This implementation
 * only has one renderer type, i.e. <code>DefaultDataValueRenderer</code>.
 * 
 * <p>
 * If you intend to derive this class, you should consider to override the
 * methods <code>getNrAvailableRenderer</code>,
 * <code>getRendererDescription</code>, and <code>getRenderer</code>.
 * 
 * @see DefaultDataValueRenderer
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultDataValueRendererFamily implements DataValueRendererFamily {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DefaultDataValueRendererFamily.class);

    /**
     * Singleton to be used.
     */
    private static final DataValueRenderer DEFAULT = 
        new DefaultDataValueRenderer();

    /**
     * Currently active renderer (where to delegate to). It's initialized in a
     * lazy way. Don't do this in the constructor because the getRenderer(int)
     * method may be overridden. This will be potentially harmful.
     */

    private DataValueRenderer[] m_renderers;

    private DataValueRenderer m_active;

    /**
     * Helper method to get the renderer family for a particular
     * <code>DataCell</code> class. This method uses java reflection to invoke
     * the static <code>getNewRenderer</code> method in the appropriate
     * <code>DataCell</code>.
     * 
     * <p>
     * If this fail, an error message is printed to standard error output and a
     * new <code>DefaultDataValueRendererFamily</code> is returned.
     * 
     * @param cellClass The class of the <code>DataCell</code> of interest.
     * @return The renderer according to the cell's <code>getNewRenderer</code>
     *         method. If this method is not defined the super class of the cell
     *         is used (at latest <code>DataCell</code> itself should have an
     *         renderer)
     * @throws IllegalArgumentException If the argument is not a subclass of
     *             <code>DataCell</code>
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public static final DataValueRendererFamily findRendererFamily(
            final Class<?> cellClass) {
        if (cellClass == null) {
            throw new NullPointerException("Class argument must not be null");
        }
        if (!DataCell.class.isAssignableFrom(cellClass)) {
            throw new IllegalArgumentException("Class argument must be "
                    + "subclass of DataCell: " + cellClass.getName());
        }
        Method m = null;
        Class<?> tClass = cellClass;
        // this loop should theoretically not be traversed more than once.
        // More than once can only occur if some implements the method with
        // another return type than DataValueRendererFamily
        do {
            try {
                m = tClass.getMethod("getNewRenderer");
                // must have proper return type
                if (m.getReturnType() == DataValueRendererFamily.class) {
                    Object r = m.invoke(null);
                    return (DataValueRendererFamily)r;
                }
            } catch (NoSuchMethodException e) {
                // the selected class doesn't implement (the correct)
                // method. Try its super.
            } catch (SecurityException e) {
                // the selected class implements it with the wrong
                // scope. Try its super.
            } catch (IllegalArgumentException e) {
                // wrong parameter passed, shouldn't happen
            } catch (IllegalAccessException e) {
                // if this Method object enforces Java language access control
                // and the underlying method is inaccessible.
            } catch (InvocationTargetException e) {
                LOGGER.warn("Can't invoke getNewRenderer in class "
                        + cellClass.getName() + "; using standard renderer "
                        + "instead");
                LOGGER.debug("", e);
                return new DefaultDataValueRendererFamily();
            }
            // proceed one step up the class hierarchy
            tClass = tClass.getSuperclass();
        } while (true);
    }

    /**
     * Constructor that uses a single default renderer with a default
     * description, i.e. renderer: <code>DefaultDataValueRenderer</code>,
     * description: "Default Renderer"
     */
    public DefaultDataValueRendererFamily() {
        this(DEFAULT);
    }

    /**
     * Constructs a renderer family given a set of renders and their
     * description. This constructor certainly has the disadvantage that all
     * available renders need to be instantiated in advance (and most of them
     * are not going to be used). If you consider this a problem for your kind
     * of renderes you need to implement an own family that does it on request
     * only.
     * 
     * @param renderers Set of all available renderer.
     * @throws NullPointerException If the argument is <code>null</code> or
     *             any entry is <code>null</code>.
     * @throws IllegalArgumentException If the array is empty.
     */
    public DefaultDataValueRendererFamily(
            final DataValueRenderer... renderers) {
        if (renderers.length == 0) {
            throw new IllegalArgumentException("No renderer available");
        }
        LinkedHashMap<String, DataValueRenderer> hash = 
            new LinkedHashMap<String, DataValueRenderer>();
        for (DataValueRenderer r : renderers) {
            hash.put(r.getDescription(), r);
        }
        m_renderers = hash.values().toArray(new DataValueRenderer[0]);
        m_active = m_renderers[0];
    }

    /**
     * Render this object in the list using the current renderer.
     * 
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(
     *      javax.swing.JList, java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        return m_active.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
    }

    /**
     * Render this object in a table using the current renderer.
     * 
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(
     *      javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        return m_active.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize() {
        return m_active.getPreferredSize();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getRendererDescriptions() {
        String[] result = new String[m_renderers.length];
        for (int i = 0; i < m_renderers.length; i++) {
            result[i] = m_renderers[i].getDescription();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void setActiveRenderer(final String description) {
        for (DataValueRenderer r : m_renderers) {
            if (r.getDescription().equals(description)) {
                m_active = r;
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return m_active.getDescription();
    }
    
    /**
     * {@inheritDoc}
     */
    public Component getRendererComponent(final Object val) {
        return m_active.getRendererComponent(val);
    }
    
    /**
     * Delegates to renderer.
     * @see DataValueRendererFamily#accepts(String, DataColumnSpec)
     */
    public boolean accepts(final String desc, final DataColumnSpec spec) {
        for (DataValueRenderer r : m_renderers) {
            if (r.getDescription().equals(desc)) {
                return r.accepts(spec);
            }
        }
        throw new IllegalArgumentException(
                "Unknown renderer description: " + desc);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean accepts(final DataColumnSpec spec) {
        return m_active.accepts(spec);
    }
}
