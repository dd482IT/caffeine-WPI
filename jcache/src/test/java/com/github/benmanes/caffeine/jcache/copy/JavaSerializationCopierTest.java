/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.jcache.copy;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Locale.US;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;

import javax.cache.CacheException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

/**
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class JavaSerializationCopierTest {

  public void constructor_null(Set<Class<?>> immutableClasses,
      Map<Class<?>, Function<Object, Object>> deepCopyStrategies) {
    new JavaSerializationCopier(immutableClasses, deepCopyStrategies);
  }

  public void null_object(Copier copier) {
    copy(copier, null);
  }

  public void null_classLoader(Copier copier) {
    copier.copy(1, null);
  }

  public void serializable_fail(JavaSerializationCopier copier) {
    copier.serialize(new Object());
  }

  public void deserializable_resolveClass() {
    var copier = new JavaSerializationCopier();
    copier.copy(ImmutableSet.of(), ClassLoader.getPlatformClassLoader());
  }

  public void deserializable_badData(JavaSerializationCopier copier) {
    copier.deserialize(new byte[0], Thread.currentThread().getContextClassLoader());
  }

  public void deserializable_classNotFound() {
    var copier = new JavaSerializationCopier() {
      @Override protected ObjectInputStream newInputStream(
          InputStream in, ClassLoader classLoader) throws IOException {
        return new ObjectInputStream(in) {
          @Override protected Class<?> resolveClass(ObjectStreamClass desc)
              throws IOException, ClassNotFoundException {
            throw new ClassNotFoundException();
          }
        };
      }
    };
    copier.roundtrip(100, Thread.currentThread().getContextClassLoader());
  }

  public void mutable(Copier copier) {
    var ints = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
    assertThat(copy(copier, ints)).containsExactlyElementsIn(ints).inOrder();
  }

  public void immutable() {
    String text = "test";
    assertThat(copy(new JavaSerializationCopier(), text)).isSameInstanceAs(text);
  }

  @SuppressWarnings({"JavaUtilDate", "JdkObsolete", "UndefinedEquals"})
  public void deepCopy_date(Copier copier) {
    Date date = new Date();
    assertThat(copy(copier, date)).isEqualTo(date);
  }

  @SuppressWarnings({"JavaUtilDate", "JdkObsolete"})
  public void deepCopy_calendar(Copier copier) {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), US);
    calendar.setTime(new Date());
    assertThat(copy(copier, calendar)).isEqualTo(calendar);
  }

  public void array_primitive(Copier copier) {
    int[] ints = { 0, 1, 2, 3, 4 };
    assertThat(copy(copier, ints)).isEqualTo(ints);
  }

  public void array_immutable(Copier copier) {
    Integer[] ints = { 0, 1, 2, 3, 4 };
    assertThat(copy(copier, ints)).asList().containsExactlyElementsIn(ints).inOrder();
  }

  public void array_mutable(Copier copier) {
    Object[] array = { new ArrayList<>(List.of(0, 1, 2, 3, 4)) };
    assertThat(copy(copier, array)).asList().containsExactlyElementsIn(array).inOrder();
  }

  private <T> T copy(Copier copier, T object) {
    return copier.copy(object, Thread.currentThread().getContextClassLoader());
  }

  public Object[] providesCopiers() {
    return new Object[] {
        new JavaSerializationCopier(),
        new JavaSerializationCopier(Set.of(), Map.of())
    };
  }

  public Object[][] providesNullArgs() {
    return new Object[][] { { null, null }, { null, Map.of() }, { Set.of(), null } };
  }
}
