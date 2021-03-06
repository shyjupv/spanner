/*
 * Copyright (C) 2011 Google Inc.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that identifies a given method as an "arbitrary measurement" method. This means the method is itself
 * responsible for taking measurements.
 *
 * The method should take no parameters and return a double, which is the measured value.
 *
 * A benchmark class cannot mix @Benchmark and @CustomMeasurement methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CustomMeasurement {

    /**
     * The units for the value returned by this measurement method.
     */
    String units() default "";

    /**
     * Text description of the quantity measured by this measurement method.
     */
    String description() default "";
}
