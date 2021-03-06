/*
 * Copyright 2012-2013 Continuuity,Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.continuuity.weave.internal.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class to help instantiate object instance from class.
 */
public final class Instances {

  private static final Object UNSAFE;
  private static final Method UNSAFE_NEW_INSTANCE;

  static {
    Object unsafe;
    Method newInstance;
    try {
      Class<?> clz = Class.forName("sun.misc.Unsafe");
      Field f = clz.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      unsafe = f.get(null);

      newInstance = clz.getMethod("allocateInstance", Class.class);
    } catch (Exception e) {
      unsafe = null;
      newInstance = null;
    }
    UNSAFE = unsafe;
    UNSAFE_NEW_INSTANCE = newInstance;
  }

  /**
   * Creates a new instance of the given class. It will use the default constructor if it is presents.
   * Otherwise it will try to use {@link sun.misc.Unsafe#allocateInstance(Class)} to create the instance.
   * @param clz Class of object to be instantiated.
   * @param <T> Type of the class
   * @return An instance of type {@code <T>}
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class<T> clz) {
    try {
      try {
        Constructor<T> cons = clz.getDeclaredConstructor();
        if (!cons.isAccessible()) {
          cons.setAccessible(true);
        }
        return cons.newInstance();
      } catch (Exception e) {
        // Try to use Unsafe
        Preconditions.checkState(UNSAFE != null, "Fail to instantiate with Unsafe.");
        return (T) UNSAFE_NEW_INSTANCE.invoke(UNSAFE, clz);
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }


  private Instances() {
    // Protect instantiation of this class
  }
}
