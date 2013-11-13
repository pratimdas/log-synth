package org.apache.mahout.math.stats;

import com.google.common.collect.Lists;
import org.apache.mahout.common.RandomUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class GroupTreeTest {
    @Test
    public void testSimpleAdds() {
        GroupTree x = new GroupTree();
        assertNull(x.floor(new Histo.Group(34)));
        assertNull(x.ceiling(new Histo.Group(34)));
        assertEquals(0, x.size());
        assertEquals(0, x.sum());

        x.add(new Histo.Group(1));
        Histo.Group group = new Histo.Group(2);
        group.add(3);
        group.add(4);
        x.add(group);

        assertEquals(2, x.size());
        assertEquals(4, x.sum());
    }

    @Test
    public void testBalancing() {
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Histo.Group(i));
        }

        assertEquals(101, x.sum());
        assertEquals(101, x.size());

        x.checkBalance();
    }

    @Test
    public void testIterators() {
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Histo.Group(i / 2));
        }

        assertEquals(0, x.first().mean(), 0);
        assertEquals(50, x.last().mean(), 0);

        Iterator<Histo.Group> ix = x.iterator();
        for (int i = 0; i < 101; i++) {
            assertTrue(ix.hasNext());
            Histo.Group z = ix.next();
            assertEquals(i / 2, z.mean(), 0);
        }
        assertFalse(ix.hasNext());

        // 34 is special since it is the smallest element of the right hand sub-tree
        Iterable<Histo.Group> z = x.tailSet(new Histo.Group(34, 0));
        ix = z.iterator();
        for (int i = 68; i < 101; i++) {
            assertTrue(ix.hasNext());
            Histo.Group v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());

        ix = z.iterator();
        for (int i = 68; i < 101; i++) {
            Histo.Group v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }

        z = x.tailSet(new Histo.Group(33, 0));
        ix = z.iterator();
        for (int i = 66; i < 101; i++) {
            assertTrue(ix.hasNext());
            Histo.Group v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());

        z = x.tailSet(x.ceiling(new Histo.Group(34, 0)));
        ix = z.iterator();
        for (int i = 68; i < 101; i++) {
            assertTrue(ix.hasNext());
            Histo.Group v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());

        z = x.tailSet(x.floor(new Histo.Group(34, 0)));
        ix = z.iterator();
        for (int i = 67; i < 101; i++) {
            assertTrue(ix.hasNext());
            Histo.Group v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());
    }

    @Test
    public void testFloor() {
        // mostly tested in other tests
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Histo.Group(i / 2));
        }

        assertNull(x.floor(new Histo.Group(-30)));
    }


    @Test
    public void testRemoveAndSums() {
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Histo.Group(i / 2));
        }
        Histo.Group g = x.ceiling(new Histo.Group(2, 0));
        x.remove(g);
        g.add(3);
        x.add(g);

        assertEquals(0, x.headCount(new Histo.Group(-1)));
        assertEquals(0, x.headSum(new Histo.Group(-1)));
        assertEquals(0, x.headCount(new Histo.Group(0, 0)));
        assertEquals(0, x.headSum(new Histo.Group(0, 0)));
        assertEquals(0, x.headCount(x.ceiling(new Histo.Group(0, 0))));
        assertEquals(0, x.headSum(x.ceiling(new Histo.Group(0, 0))));
        assertEquals(2, x.headCount(new Histo.Group(1, 0)));
        assertEquals(2, x.headSum(new Histo.Group(1, 0)));

        g = x.tailSet(new Histo.Group(2.1)).iterator().next();
        assertEquals(2.5, g.mean(), 1e-9);

        int i = 0;
        for (Histo.Group gx : x) {
            if (i > 10) {
                break;
            }
            System.out.printf("%d:%.1f(%d)\t", i++, gx.mean(), gx.count());
        }
        assertEquals(5, x.headCount(new Histo.Group(2.1, 0)));
        assertEquals(5, x.headSum(new Histo.Group(2.1, 0)));

        assertEquals(6, x.headCount(new Histo.Group(2.7, 0)));
        assertEquals(7, x.headSum(new Histo.Group(2.7, 0)));

        assertEquals(101, x.headCount(new Histo.Group(200)));
        assertEquals(102, x.headSum(new Histo.Group(200)));
    }

    @Test
    public void testRandomRebalance() {
        RandomUtils.useTestSeed();
        Random gen = RandomUtils.getRandom();
        GroupTree x = new GroupTree();
        List<Double> y = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            double v = gen.nextDouble();
            x.add(new Histo.Group(v));
            y.add(v);
            x.checkBalance();
        }

        Collections.sort(y);

        Iterator<Double> i = y.iterator();
        for (Histo.Group group : x) {
            assertEquals(i.next(), group.mean(), 0.0);
        }

        for (int j = 0; j < 100; j++) {
            double v = y.get(gen.nextInt(y.size()));
            y.remove(v);
            x.remove(x.floor(new Histo.Group(v)));
        }

        Collections.sort(y);
        i = y.iterator();
        for (Histo.Group group : x) {
            assertEquals(i.next(), group.mean(), 0.0);
        }

        for (int j = 0; j < y.size(); j++) {
            double v = y.get(j);
            y.set(j, v + 10);
            Histo.Group g = x.floor(new Histo.Group(v));
            x.remove(g);
            x.checkBalance();
            g.add(g.mean() + 20);
            x.add(g);
            x.checkBalance();
        }

        i = y.iterator();
        for (Histo.Group group : x) {
            assertEquals(i.next(), group.mean(), 0.0);
        }
    }
}
