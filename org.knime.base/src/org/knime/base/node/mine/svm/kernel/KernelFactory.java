/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.base.node.mine.svm.kernel;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for managing all the kernels that are known
 * to the SVM. 
 * 
 * @author Stefan Ciobaca, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public final class KernelFactory {
    
    /* Map with all known kernels */
    private Map<String, Kernel> m_kernels;

    /* The singleton instance. */
    private static KernelFactory instance = null; 

    /*
     * Constructor of the kernel factory. If someone wants to add a kernel,
     * this is what needs to be modified (and the getType function).
     */
    private KernelFactory() {
        m_kernels = new HashMap<String, Kernel>();
        m_kernels.put("Polynomial", new PolynomialKernel());
        m_kernels.put("HyperTangent", new HyperTangentKernel());
        m_kernels.put("RBF", new RBFKernel());
    }
    
    /**
     * Return a default kernel.
     * @return String
     */
    public static String getDefaultKernelType() {
        assureInstance();
        return getKernelNames()[1];
    }

    /**
     * Given a pointer to a kernel, get its type.
     * If you modify this function, don't forget the constructor.
     * @param kernel instance
     * @return kernel name 
     */
    public static String getType(final Kernel kernel) {
        if (kernel.getClass().getSimpleName().equals("PolynomialKernel")) {
            return "Polynomial";
        }
        if (kernel.getClass().getSimpleName().equals("HyperTangentKernel")) {
            return "HyperTangent";
        }
        if (kernel.getClass().getSimpleName().equals("RBFKernel")) {
            return "RBF";
        }
        assert false : "Trying to get name of unknown kernel type";
        return "";
    }
    
    /*
     * Make sure the singleton instance has been created.
     */
    private static void assureInstance() {
        if (instance == null) {
            instance = new KernelFactory();
        }
    }

    /**
     * Returns a kernel given by its name.
     * @param name the name of the kernel 
     * @return the requested kernel 
     */
    public static Kernel getKernel(final String name) {
        assureInstance();
        return instance.m_kernels.get(name);
    }
    
    /**
     * Returns all the kernels known by this factory.
     * @return the array of kernel names 
     */
    public static String [] getKernelNames() {
        assureInstance();
        String [] names = new String [instance.m_kernels.keySet().size()];
        names = instance.m_kernels.keySet().toArray(names);
        return names; 
    }
    
    /**
     * For each kernel, look at the number of parameters it requires, and
     * return the highest.
     * @return the maximal number of parameters from any of the kernels
     */
    public static int getMaximalParameters() {
        String [] kernelNames = getKernelNames();
        int result = 0;
        for (int i = 0; i < kernelNames.length; ++i) {
            if (getKernel(kernelNames[i]).getNumberParameters() > result) {
                result = getKernel(kernelNames[i]).getNumberParameters();
            }
        }
        return result;
    }
}
