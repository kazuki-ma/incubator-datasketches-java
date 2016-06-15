package com.yahoo.sketches.quantiles;

import java.util.Comparator;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sketches.ArrayOfItemsSerDe;
import com.yahoo.sketches.ArrayOfLongsSerDe;
import com.yahoo.sketches.ArrayOfStringsSerDe;
import com.yahoo.sketches.memory.NativeMemory;

public class ItemsQuantilesSketchTest {

  @Test
  public void empty() {
    ItemsQuantilesSketch<String> sketch = ItemsQuantilesSketch.getInstance(128, Comparator.naturalOrder());
    Assert.assertNotNull(sketch);
    Assert.assertEquals(sketch.getN(), 0);
    Assert.assertEquals(sketch.getRetainedEntries(), 0);
    Assert.assertNull(sketch.getMinValue());
    Assert.assertNull(sketch.getMaxValue());
    Assert.assertNull(sketch.getQuantile(0.5));

    {
      double[] pmf = sketch.getPMF(new String[0]);
      Assert.assertEquals(pmf.length, 1);
      Assert.assertEquals(pmf[0], Double.NaN);
    }

    {
      double[] pmf = sketch.getPMF(new String[] {"a"});
      Assert.assertEquals(pmf.length, 2);
      Assert.assertEquals(pmf[0], Double.NaN);
      Assert.assertEquals(pmf[1], Double.NaN);
    }

    {
      double[] cdf = sketch.getCDF(new String[0]);
      Assert.assertEquals(cdf.length, 1);
      Assert.assertEquals(cdf[0], Double.NaN);
    }

    {
      double[] cdf = sketch.getCDF(new String[] {"a"});
      Assert.assertEquals(cdf.length, 2);
      Assert.assertEquals(cdf[0], Double.NaN);
      Assert.assertEquals(cdf[1], Double.NaN);
    }
  }

  @Test
  public void oneItem() {
    ItemsQuantilesSketch<String> sketch = ItemsQuantilesSketch.getInstance(128, Comparator.naturalOrder());
    sketch.update("a");
    Assert.assertEquals(sketch.getN(), 1);
    Assert.assertEquals(sketch.getRetainedEntries(), 1);
    Assert.assertEquals(sketch.getMinValue(), "a");
    Assert.assertEquals(sketch.getMaxValue(), "a");
    Assert.assertEquals(sketch.getQuantile(0.5), "a");

    {
      double[] pmf = sketch.getPMF(new String[0]);
      Assert.assertEquals(pmf.length, 1);
      Assert.assertEquals(pmf[0], 1.0);
    }

    {
      double[] pmf = sketch.getPMF(new String[] {"a"});
      Assert.assertEquals(pmf.length, 2);
      Assert.assertEquals(pmf[0], 0.0);
      Assert.assertEquals(pmf[1], 1.0);
    }

    {
      double[] cdf = sketch.getCDF(new String[0]);
      Assert.assertEquals(cdf.length, 1);
      Assert.assertEquals(cdf[0], 1.0);
    }

    {
      double[] cdf = sketch.getCDF(new String[] {"a"});
      Assert.assertEquals(cdf.length, 2);
      Assert.assertEquals(cdf[0], 0.0);
      Assert.assertEquals(cdf[1], 1.0);
    }
  }

  @Test
  public void estimation() {
    ItemsQuantilesSketch<Integer> sketch = ItemsQuantilesSketch.getInstance(128, Comparator.naturalOrder());
    for (int i = 1; i <= 1000; i++) sketch.update(i);
    Assert.assertEquals(sketch.getN(), 1000);
    Assert.assertTrue(sketch.getRetainedEntries() < 1000);
    Assert.assertEquals(sketch.getMinValue(), Integer.valueOf(1));
    Assert.assertEquals(sketch.getMaxValue(), Integer.valueOf(1000));
    // based on ~1.7% normalized rank error for this particular case
    Assert.assertEquals(sketch.getQuantile(0.5), Integer.valueOf(500), 17);
    Integer[] quantiles = sketch.getQuantiles(new double[] {0, 0.5, 1});
    Assert.assertEquals(quantiles[0], Integer.valueOf(1)); // min value
    Assert.assertEquals(quantiles[1], Integer.valueOf(500), 20); // median
    Assert.assertEquals(quantiles[2], Integer.valueOf(1000)); // max value

    {
      double[] pmf = sketch.getPMF(new Integer[0]);
      Assert.assertEquals(pmf.length, 1);
      Assert.assertEquals(pmf[0], 1.0);
    }

    {
      double[] pmf = sketch.getPMF(new Integer[] {500});
      Assert.assertEquals(pmf.length, 2);
      Assert.assertEquals(pmf[0], 0.5, 0.05);
      Assert.assertEquals(pmf[1], 0.5, 0.05);
    }

    {
      double[] cdf = sketch.getCDF(new Integer[0]);
      Assert.assertEquals(cdf.length, 1);
      Assert.assertEquals(cdf[0], 1.0);
    }

    {
      double[] cdf = sketch.getCDF(new Integer[] {500});
      Assert.assertEquals(cdf.length, 2);
      Assert.assertEquals(cdf[0], 0.5, 0.05);
      Assert.assertEquals(cdf[1], 1.0, 0.05);
    }
  }

  @Test
  public void serializeDeserializeLong() {
    ItemsQuantilesSketch<Long> sketch1 = ItemsQuantilesSketch.getInstance(128, Comparator.naturalOrder());
    for (int i = 1; i <= 500; i++) sketch1.update((long) i);

    ArrayOfItemsSerDe<Long> serDe = new ArrayOfLongsSerDe();
    byte[] bytes = sketch1.toByteArray(serDe);
    ItemsQuantilesSketch<Long> sketch2 = ItemsQuantilesSketch.getInstance(new NativeMemory(bytes), Comparator.naturalOrder(), serDe);

    for (int i = 501; i <= 1000; i++) sketch2.update((long) i);
    Assert.assertEquals(sketch2.getN(), 1000);
    Assert.assertTrue(sketch2.getRetainedEntries() < 1000);
    Assert.assertEquals(sketch2.getMinValue(), Long.valueOf(1));
    Assert.assertEquals(sketch2.getMaxValue(), Long.valueOf(1000));
    // based on ~1.7% normalized rank error for this particular case
    Assert.assertEquals(sketch2.getQuantile(0.5), Integer.valueOf(500), 17);
  }

  @Test
  public void serializeDeserializeString() {
    // numeric order to be able to make meaningful assertions
    Comparator<String> numericOrder = new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        Integer i1 = Integer.parseInt(s1, 2);
        Integer i2 = Integer.parseInt(s2, 2);
        return i1.compareTo(i2);
      }
    };
    ItemsQuantilesSketch<String> sketch1 = ItemsQuantilesSketch.getInstance(128, numericOrder);
    for (int i = 1; i <= 500; i++) sketch1.update(Integer.toBinaryString(i << 10)); // to make strings longer

    ArrayOfItemsSerDe<String> serDe = new ArrayOfStringsSerDe();
    byte[] bytes = sketch1.toByteArray(serDe);
    ItemsQuantilesSketch<String> sketch2 = ItemsQuantilesSketch.getInstance(new NativeMemory(bytes), numericOrder, serDe);

    for (int i = 501; i <= 1000; i++) sketch2.update(Integer.toBinaryString(i << 10));
    Assert.assertEquals(sketch2.getN(), 1000);
    Assert.assertTrue(sketch2.getRetainedEntries() < 1000);
    Assert.assertEquals(sketch2.getMinValue(), Integer.toBinaryString(1 << 10));
    Assert.assertEquals(sketch2.getMaxValue(), Integer.toBinaryString(1000 << 10));
    // based on ~1.7% normalized rank error for this particular case
    Assert.assertEquals(Integer.parseInt(sketch2.getQuantile(0.5), 2) >> 10, Integer.valueOf(500), 17);
  }

  @Test
  public void toStringCrudeCheck() {
    ItemsQuantilesSketch<String> sketch = ItemsQuantilesSketch.getInstance(Comparator.naturalOrder());
    sketch.update("a");
    String brief = sketch.toString();
    String full = sketch.toString(true, true);
    Assert.assertTrue(brief.length() < full.length());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void unorderedSplitPoints() {
    ItemsQuantilesSketch<Integer> sketch = ItemsQuantilesSketch.getInstance(Comparator.naturalOrder());
    sketch.getPMF(new Integer[] {2, 1});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nonUniqueSplitPoints() {
    ItemsQuantilesSketch<Integer> sketch = ItemsQuantilesSketch.getInstance(Comparator.naturalOrder());
    sketch.getPMF(new Integer[] {1, 1});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullInSplitPoints() {
    ItemsQuantilesSketch<Integer> sketch = ItemsQuantilesSketch.getInstance(Comparator.naturalOrder());
    sketch.getPMF(new Integer[] {1, null});
  }

}
