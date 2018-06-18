package com.example.john.mybluetoothbledemo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void intCasting() {
        int num = 44;

        long l = (long)num;
        long l2 = num & 0xFFFFFFFF;

        assertEquals(l, l2);
    }

    @Test
    public void testLongParsing() throws Exception {
        long parsed = Long.parseLong("1234", 16);
        assertEquals(0x1234L, parsed);
    }
}