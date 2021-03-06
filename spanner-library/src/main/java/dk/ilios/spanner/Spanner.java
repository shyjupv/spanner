/*
 * Copyright (C) 2015 Christian Melchior.
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

package dk.ilios.spanner;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;

import org.threeten.bp.Instant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import dk.ilios.spanner.benchmark.BenchmarkClass;
import dk.ilios.spanner.config.InstrumentConfig;
import dk.ilios.spanner.exception.InvalidCommandException;
import dk.ilios.spanner.http.HttpUploader;
import dk.ilios.spanner.internal.AndroidExperimentSelector;
import dk.ilios.spanner.internal.ExperimentSelector;
import dk.ilios.spanner.internal.ExperimentingSpannerRun;
import dk.ilios.spanner.internal.Instrument;
import dk.ilios.spanner.internal.InvalidBenchmarkException;
import dk.ilios.spanner.internal.SpannerRun;
import dk.ilios.spanner.json.AnnotationExclusionStrategy;
import dk.ilios.spanner.json.InstantTypeAdapter;
import dk.ilios.spanner.log.AndroidStdOut;
import dk.ilios.spanner.log.StdOut;
import dk.ilios.spanner.model.Run;
import dk.ilios.spanner.model.Trial;
import dk.ilios.spanner.output.OutputFileDumper;
import dk.ilios.spanner.output.ResultProcessor;
import dk.ilios.spanner.util.NanoTimeGranularityTester;
import dk.ilios.spanner.util.ShortDuration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main class for starting a benchmark.
 */
public class Spanner {

    private final BenchmarkClass benchmarkClass;
    private final Callback callback;
    private SpannerConfig benchmarkConfig;

    public static void runBenchmarks(Class benchmarkClass, ArrayList<Method> methods) {
        runBenchmarks(benchmarkClass, methods, new RethrowCallback());
    }

    public static void runBenchmarks(Class benchmarkClass, List<Method> methods, Callback callback) {
        new Spanner(benchmarkClass, methods, callback).start();
    }

    public static void runAllBenchmarks(Class benchmarkClass) {
        runAllBenchmarks(benchmarkClass, new RethrowCallback());
    }

    public static void runAllBenchmarks(Class benchmarkClass, Callback callback) {
        new Spanner(benchmarkClass, null, callback).start();
    }

    private Spanner(Class benchmarkClass, List<Method> benchmarkMethods, Callback callback) {
        checkNotNull(callback);
        this.callback = callback;
        try {
            this.benchmarkClass = new BenchmarkClass(benchmarkClass, benchmarkMethods);
        } catch (InvalidBenchmarkException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void start() {
        try {
            callback.onStart();
            benchmarkConfig = benchmarkClass.getConfiguration();
            File baseline = benchmarkConfig.getBaseLineFile();

            ImmutableSet<Instrument> instruments = getInstruments(benchmarkConfig);

            int poolSize = benchmarkConfig.getNoBenchmarkThreads();
            ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(poolSize));

            StdOut stdOut = new AndroidStdOut();
            Run runInfo = new Run.Builder(UUID.randomUUID())
                    .label("Spanner benchmark test")
                    .startTime(Instant.now())
                    .configuration(benchmarkConfig)
                    .build();

            ExperimentSelector experimentSelector = new AndroidExperimentSelector(benchmarkClass, instruments);

            // GSON config
            GsonBuilder gsonBuilder = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy());
            gsonBuilder.registerTypeAdapterFactory(TypeAdapters.newFactory(Instant.class, new InstantTypeAdapter()));
            Gson gson = gsonBuilder.create();

            // Configure baseline data
            Trial[] baselineData;
            if (baseline != null) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(baseline));
                    baselineData = gson.fromJson(br, Trial[].class);
                    br.close();
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            } else {
                baselineData = new Trial[0];
            }

            // Configure ResultProcessors
            Set<ResultProcessor> processors = new HashSet<>();
            if (benchmarkConfig.getResultsFile() != null) {
                OutputFileDumper dumper = new OutputFileDumper(gson, benchmarkConfig.getResultsFile());
                processors.add(dumper);
            }
            if (benchmarkConfig.isUploadResults()) {
                HttpUploader uploader = new HttpUploader(stdOut, gson, benchmarkConfig);
                processors.add(uploader);
            }
            processors.addAll(benchmarkConfig.getResultProcessors());
            ImmutableSet<ResultProcessor> resultProcessors = ImmutableSet.copyOf(processors);

            // Configure runner
            SpannerRun run = new ExperimentingSpannerRun(
                    benchmarkConfig,
                    stdOut,
                    runInfo,
                    resultProcessors,
                    experimentSelector,
                    executor,
                    baselineData,
                    callback
            );

            // Run benchmark
            run.run();
            callback.onComplete();
        } catch (Exception e) {
            // Report all exceptions
            callback.onError(e);
        }
    }

    public ImmutableSet<Instrument> getInstruments(SpannerConfig benchmarkConfig) throws InvalidCommandException {
        ImmutableSet.Builder<Instrument> builder = ImmutableSet.builder();
        Set<InstrumentConfig> configuredInstruments = benchmarkConfig.getInstrumentConfigurations();
        for (InstrumentConfig instrumentConfig : configuredInstruments) {
            try {
                Class<? extends Instrument> clazz = instrumentConfig.getInstrumentClass();
                ShortDuration timerGranularity = new NanoTimeGranularityTester().testNanoTimeGranularity();
                Instrument instrument = (Instrument) clazz.getDeclaredConstructors()[0].newInstance(
                        timerGranularity, instrumentConfig);
                builder.add(instrument);
            } catch (Exception e) {
                callback.onError(e);
                break;
            }
        }
        return builder.build();
    }

    /**
     * Callback for outside listeners to get notified on the progress of the Benchmarks running.
     */
    public interface Callback {
        void onStart();
        void trialStarted(Trial trial);
        void trialSuccess(Trial trial, Trial.Result result);
        void trialFailure(Trial trial, Throwable error);
        void trialEnded(Trial trial);
        void onComplete();
        void onError(Exception exception);
    }

    private static class RethrowCallback extends SpannerCallbackAdapter {
        @Override
        public void onError(Exception error) {
            throw new RuntimeException(error);
        }
    }
}
