/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.cpc;

import static org.apache.datasketches.cpc.CompressionData.decodingTablesForHighEntropyByte;
import static org.apache.datasketches.cpc.CompressionData.encodingTablesForHighEntropyByte;
import static org.apache.datasketches.cpc.CompressionData.lengthLimitedUnaryDecodingTable65;
import static org.apache.datasketches.cpc.CompressionData.lengthLimitedUnaryEncodingTable65;
import static org.apache.datasketches.cpc.CpcCompression.BIT_BUF;
import static org.apache.datasketches.cpc.CpcCompression.BUF_BITS;
import static org.apache.datasketches.cpc.CpcCompression.NEXT_WORD_IDX;
import static org.apache.datasketches.cpc.CpcCompression.lowLevelCompressBytes;
import static org.apache.datasketches.cpc.CpcCompression.lowLevelCompressPairs;
import static org.apache.datasketches.cpc.CpcCompression.lowLevelUncompressBytes;
import static org.apache.datasketches.cpc.CpcCompression.lowLevelUncompressPairs;
import static org.apache.datasketches.cpc.CpcCompression.readUnary;
import static org.apache.datasketches.cpc.CpcCompression.writeUnary;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
@SuppressWarnings("javadoc")
public class CpcCompressionTest {

  @Test
  public void checkWriteReadUnary() {
    int[] compressedWords = new int[256];

    final long[] ptrArr = new long[3];
    int nextWordIndex = 0; //must be int
    long bitBuf = 0;       //must be long
    int bufBits = 0;       //could be byte

    for (int i = 0; i < 100; i++) {

      //TODO Inline WriteUnary
      ptrArr[NEXT_WORD_IDX] = nextWordIndex;
      ptrArr[BIT_BUF] = bitBuf;
      ptrArr[BUF_BITS] = bufBits;
      assert nextWordIndex == ptrArr[NEXT_WORD_IDX]; //catch sign extension error
      writeUnary(compressedWords, ptrArr, i);
      nextWordIndex = (int) ptrArr[NEXT_WORD_IDX];
      bitBuf = ptrArr[BIT_BUF];
      bufBits = (int) ptrArr[BUF_BITS];
      assert nextWordIndex == ptrArr[NEXT_WORD_IDX]; //catch truncation error
      //END Inline WriteUnary

    }

    // Pad the bitstream so that the decompressor's 12-bit peek can't overrun its input.
    long padding = 7;
    bufBits += padding;
    //MAYBE_FLUSH_BITBUF(compressedWords, nextWordIndex);
    if (bufBits >= 32) {
      compressedWords[nextWordIndex++] = (int) bitBuf;
      bitBuf >>>= 32;
      bufBits -= 32;
    }

    if (bufBits > 0) { // We are done encoding now, so we flush the bit buffer.
      assert (bufBits < 32);
      compressedWords[nextWordIndex++] = (int) bitBuf;
    }
    int numWordsUsed = nextWordIndex;
    println("Words used: " + numWordsUsed);
    nextWordIndex = 0; //must be int
    bitBuf = 0;       //must be long
    bufBits = 0;       //could be byte

    for (int i = 0; i < 100; i++) {

      //TODO Inline ReadUnary
      ptrArr[NEXT_WORD_IDX] = nextWordIndex;
      ptrArr[BIT_BUF] = bitBuf;
      ptrArr[BUF_BITS] = bufBits;
      assert nextWordIndex == ptrArr[NEXT_WORD_IDX];
      final long result = readUnary(compressedWords, ptrArr);
      println("Result: " + result + ", expected: " + i);

      assertEquals(result, i);
      nextWordIndex = (int) ptrArr[NEXT_WORD_IDX];
      bitBuf = ptrArr[BIT_BUF];
      bufBits = (int) ptrArr[BUF_BITS];
      assert nextWordIndex == ptrArr[NEXT_WORD_IDX];
      //END Inline ReadUnary

    }
    assertTrue(nextWordIndex <= numWordsUsed);
  }

  @Test
  public void checkWriteReadBytes() {
    int[] compressedWords = new int[128];
    byte[] byteArray = new byte[256];
    byte[] byteArray2 = new byte[256]; //output
    for (int i = 0; i < 256; i++) { byteArray[i] = (byte) i; }

    for (int j = 0; j < 22; j++) {
      long numWordsWritten = lowLevelCompressBytes(
          byteArray, 256, encodingTablesForHighEntropyByte[j], compressedWords);

      lowLevelUncompressBytes(byteArray2, 256, decodingTablesForHighEntropyByte[j],
          compressedWords, numWordsWritten);

      println("Words used: " + numWordsWritten);
      assertEquals(byteArray2, byteArray);
    }
  }

  @Test
  public void checkWriteReadBytes65() {
    int size = 65;
    int[] compressedWords = new int[128];
    byte[] byteArray = new byte[size];
    byte[] byteArray2 = new byte[size]; //output
    for (int i = 0; i < size; i++) { byteArray[i] = (byte) i; }

    long numWordsWritten = lowLevelCompressBytes(
        byteArray, size, lengthLimitedUnaryEncodingTable65, compressedWords);

    lowLevelUncompressBytes(byteArray2, size, lengthLimitedUnaryDecodingTable65,
        compressedWords, numWordsWritten);

    println("Words used: " + numWordsWritten);
    assertEquals(byteArray2, byteArray);
  }


  @Test
  public void checkWriteReadPairs() {
    Random rgen = new Random(1);
    int lgK = 14;
    int N = 3000;
    final int MAX_WORDS = 4000;
    int[] pairArray  = new int[N];
    int[] pairArray2 = new int[N];
    int i;
    for (i = 0; i < N; i++) {
      int rand = rgen.nextInt(1 << (lgK + 6));
      pairArray[i] = rand;
    }
    Arrays.sort(pairArray);   //must be unsigned sort! So keep lgK < 26
    int prev = -1;
    int nxt = 0;
    for (i = 0; i < N; i++) { // uniquify
      if (pairArray[i] != prev) {
        prev = pairArray[i];
        pairArray[nxt++] = pairArray[i];
      }
    }
    int numPairs = nxt;
    println("numCsv = " + numPairs);

    int[] compressedWords = new int[MAX_WORDS];
    int bb; // numBaseBits

    for (bb = 0; bb <= 11; bb++) {
      Long numWordsWritten =
        lowLevelCompressPairs(pairArray, numPairs, bb, compressedWords);
        println("numWordsWritten = " + numWordsWritten + ", bb = " + bb);

      lowLevelUncompressPairs(pairArray2, numPairs, bb, compressedWords, numWordsWritten);

      for (i = 0; i < numPairs; i++) {
        assert (pairArray[i] == pairArray2[i]);
      }
    }
  }

  @Test
  public void printlnTest() {
    println("PRINTING: " + this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }

}
