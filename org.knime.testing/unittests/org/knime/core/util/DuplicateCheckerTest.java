/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 28, 2007 (wiswedel): created
 */
package org.knime.core.util;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Random;

import junit.framework.TestCase;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class DuplicateCheckerTest extends TestCase {
    
    public void testNoDuplicateManyRows() {
        long t = System.currentTimeMillis();
        DuplicateChecker dc = new DuplicateChecker();

        try {
            int[] indices = new int[10000000];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
            Random r = new Random();
            for (int i = indices.length - 1; i >= 0; i--) {
                int swpIndex = r.nextInt(indices.length);
                int swp = indices[i];
                indices[i] = indices[swpIndex];
                indices[swpIndex] = swp;
            }
            for (int i : indices) {
                dc.addKey("Row " + i);
            }
            dc.checkForDuplicates();
        } catch (DuplicateKeyException ex) {
            ex.printStackTrace();
            fail("No duplicates inserted but exception was thrown");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("Unexpected io exception" + ioe.getMessage());
        }
        dc.clear();
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }
    
    public void testNoStringsAtAll() {
        DuplicateChecker dc = new DuplicateChecker();
        try {
            dc.checkForDuplicates();
        } catch (Exception dke) {
            dke.printStackTrace();
            fail();
        }
    }
    
    public void testArbitraryStringsNoDuplicates() {
        internalTestArbitraryStrings(false);
    }
    
    public void testArbitraryStringsDuplicates() {
        internalTestArbitraryStrings(true);
    }
    
    
    
    private void internalTestArbitraryStrings(final boolean isAddDuplicates) {
        LinkedHashSet<String> hash = new LinkedHashSet<String>();
        long seed = System.currentTimeMillis();
        System.out.println("Using seed " + seed);
        Random r = new Random(seed);
        while (hash.size() < 300000) {
            int length = 5 + r.nextInt(30);
            char[] c = new char[length];
            for (int i = 0; i < length; i++) {
                double prob = r.nextDouble();
                if (prob < 0.05) {
                    c[i] = getRandomSpecialChar(r);
                } else if (prob < 0.1) {
                    c[i] = getRandomArbitraryChar(r);
                } else {
                    c[i] = getRandomASCIIChar(r);
                }
            }
            hash.add(new String(c));
        }
        DuplicateChecker dc = new DuplicateChecker(1000, 50);
        int duplIndex = r.nextInt(hash.size());
        int indexToInsert = r.nextInt(hash.size());
        while (indexToInsert == duplIndex) {
            indexToInsert = r.nextInt(hash.size());
        }
        if (duplIndex > indexToInsert) {
            int swap = duplIndex;
            duplIndex = indexToInsert;
            indexToInsert = swap;
        }
        String dupl = null;
        try {
            int index = 0;
            for (String c : hash) {
                if (index == duplIndex) {
                    dupl = c;
                } else if (isAddDuplicates && index == indexToInsert) {
                    assert dupl != null;
                    dc.addKey(dupl);
                }
                dc.addKey(c);
                index++;
            }
            dc.checkForDuplicates();
        } catch (DuplicateKeyException e) {
            if (isAddDuplicates) {
                assertEquals(dupl, e.getKey());
            } else {
                e.printStackTrace();
                fail("No duplicates were added");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        dc.clear();
    }
    
    private static char getRandomSpecialChar(final Random rand) {
        switch (rand.nextInt(3)) {
        case 0 : return '%';
        case 1 : return '\n';
        case 2 : return '\r';
        default: throw new RuntimeException("Invalid random number");
        }
    }
    
    private static char getRandomArbitraryChar(final Random rand) {
        return (char)rand.nextInt(Character.MAX_VALUE + 1); 
    }
    
    private static char getRandomASCIIChar(final Random rand) {
        return (char)(' ' + rand.nextInt(Byte.MAX_VALUE - ' '));
    }

}
