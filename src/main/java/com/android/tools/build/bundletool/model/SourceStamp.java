/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.android.tools.build.bundletool.model;

import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.Immutable;

/**
 * Stamp improves traceability of apps with respect to unauthorized distribution.
 *
 * <p>The stamp is part of the APK that is protected by the signing block.
 *
 * <p>The APK contents hash is signed using the stamp key, and is saved as part of the signing
 * block.
 */
@Immutable
@AutoValue
public abstract class SourceStamp {

  public static final String LOCAL_SOURCE = "local-unstamped";

  public static final String STAMP_SOURCE_METADATA_KEY = "com.google.android.stamp.source";
  public static final String STAMP_TYPE_METADATA_KEY = "com.google.android.stamp.type";
  public static final String STAMP_CERT_SHA256_METADATA_KEY =
      "com.google.android.stamp.stamp-cert-sha256";

  /** Returns the signing configuration used for signing the stamp. */
  public abstract SigningConfiguration getSigningConfiguration();

  /**
   * Returns the name of source generating the stamp for the APK.
   *
   * <p>For stores, it is their package names.
   *
   * <p>For local stamps, it is "local-unstamped". Local stamps are unverifiable.
   */
  public abstract String getSource();

  public static Builder builder() {
    return new AutoValue_SourceStamp.Builder().setSource(LOCAL_SOURCE);
  }

  /** Builder of {@link SourceStamp} instances. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSigningConfiguration(SigningConfiguration signingConfiguration);

    public abstract Builder setSource(String source);

    public abstract SourceStamp build();
  }

  /** Type of stamp generated. */
  public enum StampType {
    // Stamp generated for all APKs except universal APKs.
    STAMP_TYPE_DEFAULT,
    // Stamp generated for a universal APK.
    STAMP_TYPE_UNIVERSAL,
    // Stamp generated for a local APK, regardless of the APK type.
    STAMP_TYPE_LOCAL,
  }
}
