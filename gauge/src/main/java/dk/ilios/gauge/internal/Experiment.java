/*
 * Copyright (C) 2011 Google Inc.
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

package dk.ilios.gauge.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedMap;

import java.util.Map;

/**
 * A single "premise" for making benchmark measurements: which class and method to invoke, which VM
 * to use, which choices for user parameters and vmArguments to fill in and which instrument to use
 * to measure. A caliper run will compute all possible scenarios using
 * FullCartesianExperimentSelector, and will run one or more trials of each.
 */
public final class Experiment {
    private final Instrument.Instrumentation instrumentation;
    private final ImmutableSortedMap<String, String> userParameters;

    Experiment(Instrument.Instrumentation instrumentation, Map<String, String> userParameters) {
        this.instrumentation = checkNotNull(instrumentation);
        this.userParameters = ImmutableSortedMap.copyOf(userParameters);
    }

    public Instrument.Instrumentation instrumentation() {
        return instrumentation;
    }

    public ImmutableSortedMap<String, String> userParameters() {
        return userParameters;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Experiment) {
            Experiment that = (Experiment) object;
            return this.instrumentation.equals(that.instrumentation)
                    && this.userParameters.equals(that.userParameters);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(instrumentation, userParameters);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper("")
                .add("instrument", instrumentation.instrument())
                .add("benchmarkMethod", instrumentation.benchmarkMethod.getName())
                .add("parameters", userParameters)
                .toString();
    }
}
