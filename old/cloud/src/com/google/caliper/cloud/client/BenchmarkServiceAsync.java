/**
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.cloud.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;
import java.util.Map;

/**
 * The async counterpart of {@code BenchmarkService}.
 */
public interface BenchmarkServiceAsync {
  void createSnapshot(Benchmark benchmark, AsyncCallback<Long> callback);

  void fetchBenchmarkNames(String benchmarkOwner, AsyncCallback<List<String>> callback);

  void fetchBenchmark(String benchmarkOwner, String benchmarkName,
      Long snapshotId, AsyncCallback<BenchmarkMeta> callback);

  void nameRun(long id, String name, AsyncCallback<Void> callback);

  void nameEnvironment(long id, String name, AsyncCallback<Void> callback);

  void setRunDeleted(long id, boolean deleted, AsyncCallback<Void> callback);

  void setSnapshotDeleted(long id, boolean deleted, AsyncCallback<Void> callback);

  void reorderVariables(String benchmarkOwner, String benchmarkName,
      List<String> rVariables, String cVariable, AsyncCallback<Void> callback);

  void setVariableValuesShown(String benchmarkOwner, String benchmarkName,
      Map<String, Map<String, Boolean>> variableValuesShown, AsyncCallback<Void> callback);
}