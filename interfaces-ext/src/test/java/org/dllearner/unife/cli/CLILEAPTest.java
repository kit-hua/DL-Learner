/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dllearner.unife.cli;

import org.dllearner.unife.cli.CLILEAP;
import java.io.IOException;
import org.dllearner.confparser.ParseException;
import org.dllearner.core.ReasoningMethodUnsupportedException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Giuseppe Cota <giuseppe.cota@unife.it>
 */
public class CLILEAPTest {

    public CLILEAPTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class CLILEAP.
     * @throws java.lang.Exception
     */
//    @Test
    public void testMain1() throws Exception {
        System.out.println("main");
        String[] args = {"../examples/probabilistic/family/run.conf"};
        try {
            CLILEAP.main(args);
        } catch (RuntimeException| ParseException | IOException | ReasoningMethodUnsupportedException e) {
            fail();
        }
    }

    @Test
    public void testMain2() throws Exception {
        System.out.println("main");
        String[] args = {"../examples/probabilistic/carcinogenesis/run3.conf"};
        try {
            CLILEAP.main(args);
        } catch (RuntimeException| ParseException | IOException | ReasoningMethodUnsupportedException e) {
            fail();
        }
    }

//    @Test
    public void testMain3() throws Exception {
        System.out.println("main");
        String[] args = {"../examples/probabilistic/premierleague/run.conf"};
        try {
            CLILEAP.main(args);
        } catch (RuntimeException| ParseException | IOException | ReasoningMethodUnsupportedException e) {
            fail();
        }
    }

//    @Test
    public void testMainTest() {
        System.out.println("main");
        String[] args = {"../examples/probabilistic/test/run.conf"};
        try {
            CLILEAP.main(args);
        } catch (Exception e) {
            fail();
        }
    }

}