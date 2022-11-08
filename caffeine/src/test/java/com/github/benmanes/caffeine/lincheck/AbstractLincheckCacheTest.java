/*
 * Copyright 2021 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.benmanes.caffeine.lincheck;

import java.util.Map;

import org.jetbrains.kotlinx.lincheck.LinChecker;
import org.jetbrains.kotlinx.lincheck.annotations.Operation;
import org.jetbrains.kotlinx.lincheck.annotations.Param;
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen;
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions;
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState;
import org.testng.annotations.Test;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * Linearizability checks. This property is guaranteed for per-key operations in the absence of
 * evictions, refresh, and other non-deterministic features. This property is not supported by
 * operations that span across multiple entries such as {@link Map#isEmpty()}, {@link Map#size()},
 * and {@link Map#clear()}.
 *
 * @author afedorov2602@gmail.com (Alexander Fedorov)
 * @author ben.manes@gmail.com (Ben Manes)
 */
public abstract class AbstractLincheckCacheTest extends VerifierState {
  private final LoadingCache<Integer, Integer> cache;

  public AbstractLincheckCacheTest(Caffeine<Object, Object> builder) {
    cache = builder.executor(Runnable::run).build(key -> -key);
  }

  /**
   * This test checks linearizability with bounded model checking. Unlike stress testing, this
   * approach can also provide a trace of an incorrect execution. However, it uses sequential
   * consistency model, so it can not find any low-level bugs (e.g., missing 'volatile'), and thus,
   * it is recommended to have both test modes.
   * <p>
   * This test requires the following JVM arguments,
   * <ul>
   *   <li>--add-opens java.base/jdk.internal.misc=ALL-UNNAMED
   *   <li>--add-exports java.base/jdk.internal.util=ALL-UNNAMED
   * </ul>
   */
  public void modelCheckingTest() {
    var options = new ModelCheckingOptions()
        .iterations(100)                 // the number of different scenarios
        .invocationsPerIteration(1_000); // how deeply each scenario is tested
    new LinChecker(getClass(), options).check();
  }

  /** This test checks linearizability with stress testing. */
  public void stressTest() {
    var options = new StressOptions()
        .iterations(100)                  // the number of different scenarios
        .invocationsPerIteration(10_000); // how deeply each scenario is tested
    new LinChecker(getClass(), options).check();
  }

  /**
   * Provides something with correct <tt>equals</tt> and <tt>hashCode</tt> methods that can be
   * interpreted as an internal data structure state for faster verification. The only limitation is
   * that it should be different for different data structure states.
   *
   * @return object representing internal state
   */
  @Override
  protected Object extractState() {
    return cache.asMap();
  }

  /* --------------- Cache --------------- */

  public Integer getIfPresent(int key) {
    return cache.getIfPresent(key);
  }

  public Integer get_function(int key, int nextValue) {
    return cache.get(key, k -> nextValue);
  }

  public void put(int key, int value) {
    cache.put(key, value);
  }

  public void invalidate(int key) {
    cache.invalidate(key);
  }

  /* --------------- LoadingCache --------------- */

  public Integer get(int key) {
    return cache.get(key);
  }

  /* --------------- Concurrent Map --------------- */

  public boolean containsKey(int key) {
    return cache.asMap().containsKey(key);
  }

  public boolean containsValue(int value) {
    return cache.asMap().containsValue(value);
  }

  public Integer get_asMap(int key) {
    return cache.asMap().get(key);
  }

  public Integer put_asMap(int key, int value) {
    return cache.asMap().put(key, value);
  }

  public Integer putIfAbsent(int key, int value) {
    return cache.asMap().putIfAbsent(key, value);
  }

  public Integer replace(int key, int nextValue) {
    return cache.asMap().replace(key, nextValue);
  }

  public boolean replaceConditionally(int key,
      int previousValue, int nextValue) {
    return cache.asMap().replace(key, previousValue, nextValue);
  }

  public Integer remove(int key) {
    return cache.asMap().remove(key);
  }

  public boolean removeConditionally(int key,
      int previousValue) {
    return cache.asMap().remove(key, previousValue);
  }

  public Integer computeIfAbsent(int key,
      int nextValue) {
    return cache.asMap().computeIfAbsent(key, k -> nextValue);
  }

  public Integer computeIfPresent(int key,
      int nextValue) {
    return cache.asMap().computeIfPresent(key, (k, v) -> nextValue);
  }

  public Integer compute(int key, int nextValue) {
    return cache.asMap().compute(key, (k, v) -> nextValue);
  }

  public Integer merge(int key, int nextValue) {
    return cache.asMap().merge(key, nextValue, (k, v) -> v + nextValue);
  }
}
