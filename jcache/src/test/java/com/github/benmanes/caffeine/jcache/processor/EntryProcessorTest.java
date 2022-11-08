/*
 * Copyright 2017 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.jcache.processor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static java.util.function.Function.identity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.processor.MutableEntry;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.github.benmanes.caffeine.jcache.AbstractJCacheTest;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;

/**
 * @author chrisstockton (Chris Stockton)
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings("PreferJavaTimeOverload")
public final class EntryProcessorTest extends AbstractJCacheTest {
  private final Map<Integer, Integer> map = new HashMap<>();

  private int loads;
  private int writes;

  public void beforeMethod() {
    map.clear();
    loads = 0;
    writes = 0;
  }

  @Override
  protected CaffeineConfiguration<Integer, Integer> getConfiguration() {
    var config = new CaffeineConfiguration<Integer, Integer>();
    config.setExpiryPolicyFactory(() -> new CreatedExpiryPolicy(Duration.FIVE_MINUTES));
    config.setCacheLoaderFactory(MapLoader::new);
    config.setCacheWriterFactory(MapWriter::new);
    config.setTickerFactory(() -> ticker::read);
    config.setMaximumSize(OptionalLong.of(200));
    config.setWriteThrough(true);
    config.setReadThrough(true);
    return config;
  }

  public void reload() {
    jcache.invoke(KEY_1, this::process);
    assertThat(loads).isEqualTo(1);

    ticker.advance(1, TimeUnit.MINUTES);
    jcache.invoke(KEY_1, this::process);
    assertThat(loads).isEqualTo(1);

    // Expire the entry
    ticker.advance(5, TimeUnit.MINUTES);

    jcache.invoke(KEY_1, this::process);
    assertThat(loads).isEqualTo(2);

    ticker.advance(1, TimeUnit.MINUTES);
    jcache.invoke(KEY_1, this::process);
    assertThat(loads).isEqualTo(2);
  }

  public void writeOccursForInitialLoadOfEntry() {
    map.put(KEY_1, 100);
    jcache.invoke(KEY_1, this::process);
    assertThat(loads).isEqualTo(1);
    assertThat(writes).isEqualTo(1);
  }

  private Object process(MutableEntry<Integer, Integer> entry, Object... arguments) {
    Integer value = firstNonNull(entry.getValue(), 0);
    entry.setValue(++value);
    return null;
  }

  final class MapWriter implements CacheWriter<Integer, Integer> {

    @Override
    public void write(Cache.Entry<? extends Integer, ? extends Integer> entry) {
      writes++;
      map.put(entry.getKey(), entry.getValue());
    }

    @Override
    public void writeAll(Collection<Cache.Entry<? extends Integer, ? extends Integer>> entries) {
      entries.forEach(this::write);
    }

    @Override
    public void delete(Object key) {
      map.remove(key);
    }

    @Override
    public void deleteAll(Collection<?> keys) {
      keys.forEach(this::delete);
    }
  }

  final class MapLoader implements CacheLoader<Integer, Integer> {

    @Override
    public Integer load(Integer key) throws CacheLoaderException {
      loads++;
      return map.get(key);
    }

    @Override
    public ImmutableMap<Integer, Integer> loadAll(Iterable<? extends Integer> keys) {
      return Streams.stream(keys).collect(toImmutableMap(identity(), this::load));
    }
  }
}
