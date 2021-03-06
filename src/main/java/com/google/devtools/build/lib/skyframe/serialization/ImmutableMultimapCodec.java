// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.skyframe.serialization;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A codec for {@link ImmutableMultimap}. Handles both {@link ImmutableListMultimap} and {@link
 * ImmutableSetMultimap}.
 */
public class ImmutableMultimapCodec<K, V> implements ObjectCodec<ImmutableMultimap<K, V>> {

  @SuppressWarnings("unchecked")
  @Override
  public Class<? extends ImmutableMultimap<K, V>> getEncodedClass() {
    return (Class<ImmutableMultimap<K, V>>) ((Class<?>) ImmutableMultimap.class);
  }

  @Override
  public void serialize(
      SerializationContext context, ImmutableMultimap<K, V> obj, CodedOutputStream codedOut)
      throws SerializationException, IOException {
    if (obj instanceof ImmutableListMultimap) {
      codedOut.writeBoolNoTag(true);
    } else if (obj instanceof ImmutableSetMultimap) {
      codedOut.writeBoolNoTag(false);
    } else {
      throw new SerializationException("Unexpected multimap type: " + obj.getClass());
    }
    codedOut.writeInt32NoTag(obj.asMap().size());
    for (Map.Entry<K, Collection<V>> entry : obj.asMap().entrySet()) {
      context.serialize(entry.getKey(), codedOut);
      context.serialize(entry.getValue(), codedOut);
    }
  }

  @Override
  public ImmutableMultimap<K, V> deserialize(
      DeserializationContext context, CodedInputStream codedIn)
      throws SerializationException, IOException {
    ImmutableMultimap.Builder<K, V> result;
    if (codedIn.readBool()) {
      result = ImmutableListMultimap.builder();
    } else {
      result = ImmutableSetMultimap.builder();
    }
    int length = codedIn.readInt32();
    for (int i = 0; i < length; i++) {
      K key = context.deserialize(codedIn);
      Collection<V> values = context.deserialize(codedIn);
      result.putAll(key, values);
    }
    return result.build();
  }
}
