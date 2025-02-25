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

package org.apache.datasketches.tuple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("javadoc")
public class TestUtil {
  public static List<Double> asList(double[] array) {
    List<Double> list = new ArrayList<>(array.length);
    for (int i = 0; i < array.length; i++) {
      list.add(array[i]);
    }
    return list;
  }

  public static List<Float> asList(float[] array) {
    List<Float> list = new ArrayList<>(array.length);
    for (int i = 0; i < array.length; i++) {
      list.add(array[i]);
    }
    return list;
  }

  public static List<Long> asList(long[] array) {
    List<Long> list = new ArrayList<>(array.length);
    for (int i = 0; i < array.length; i++) {
      list.add(array[i]);
    }
    return list;
  }

  public static void writeBytesToFile(byte[] bytes, String fileName) throws IOException {
    try (FileOutputStream out = new FileOutputStream(new File(fileName))) {
      out.write(bytes);
    }
  }

}
