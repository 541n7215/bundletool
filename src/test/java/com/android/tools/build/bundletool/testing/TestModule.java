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
package com.android.tools.build.bundletool.testing;

import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.androidManifest;
import static com.google.common.base.Preconditions.checkArgument;

import com.android.bundle.Config.BundleConfig;
import com.android.bundle.Config.Bundletool;
import com.android.bundle.Devices.DeviceSpec;
import com.android.tools.build.bundletool.commands.BuildApksCommand;
import com.android.tools.build.bundletool.commands.BuildApksCommand.ApkBuildMode;
import com.android.tools.build.bundletool.commands.BuildApksManagerComponent.UseBundleCompression;
import com.android.tools.build.bundletool.commands.CommandScoped;
import com.android.tools.build.bundletool.io.AppBundleSerializer;
import com.android.tools.build.bundletool.io.TempDirectory;
import com.android.tools.build.bundletool.io.ZipReader;
import com.android.tools.build.bundletool.model.ApkListener;
import com.android.tools.build.bundletool.model.ApkModifier;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleMetadata;
import com.android.tools.build.bundletool.model.OptimizationDimension;
import com.android.tools.build.bundletool.model.SigningConfiguration;
import com.android.tools.build.bundletool.model.SourceStamp;
import com.android.tools.build.bundletool.model.version.BundleToolVersion;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

/** Dagger module for bundletool tests. */
@Module
public class TestModule {

  private final BuildApksCommand buildApksCommand;
  private final AppBundle appBundle;
  private final boolean useBundleCompression;

  private TestModule(
      BuildApksCommand buildApksCommand, AppBundle appBundle, boolean useBundleCompression) {
    this.buildApksCommand = buildApksCommand;
    this.appBundle = appBundle;
    this.useBundleCompression = useBundleCompression;
  }

  @Provides
  AppBundle provideAppBundle() {
    return appBundle;
  }

  @SuppressWarnings({"CloseableProvides", "MustBeClosedChecker"}) // Only for tests.
  @Provides
  ZipReader provideZipReader(BuildApksCommand command) {
    return ZipReader.createFromFile(command.getBundlePath());
  }

  @Provides
  BuildApksCommand provideBuildApksCommand() {
    return buildApksCommand;
  }

  @Provides
  @CommandScoped
  TempDirectory provideTempDirectory() {
    return new TempDirectory();
  }

  @Provides
  @UseBundleCompression
  boolean provideUseBundleCompression() {
    return useBundleCompression;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder for the TestModule. */
  public static class Builder {
    private static final BundleConfig DEFAULT_BUNDLE_CONFIG =
        BundleConfig.newBuilder()
            .setBundletool(
                Bundletool.newBuilder()
                    .setVersion(BundleToolVersion.getCurrentVersion().toString()))
            .build();

    private static final BundleMetadata DEFAULT_BUNDLE_METADATA = BundleMetadata.builder().build();

    @Nullable private TempDirectory tempDirectory;
    @Nullable private Path outputDirectory;
    @Nullable private Path bundlePath;
    @Nullable private AppBundle appBundle;
    private BundleConfig bundleConfig = DEFAULT_BUNDLE_CONFIG;
    @Nullable private SigningConfiguration signingConfig;
    @Nullable private ApkModifier apkModifier;
    @Nullable private ApkListener apkListener;
    @Nullable private Integer firstVariantNumber;
    @Nullable private ListeningExecutorService executorService;
    @Nullable private Path outputPath;
    @Nullable private ApkBuildMode apkBuildMode;
    @Nullable private String[] moduleNames;
    @Nullable private DeviceSpec deviceSpec;
    @Nullable private Consumer<BuildApksCommand.Builder> buildApksCommandSetter;
    @Nullable private OptimizationDimension[] optimizationDimensions;
    @Nullable private PrintStream printStream;
    @Nullable private Boolean localTestingEnabled;
    @Nullable private SourceStamp sourceStamp;
    private boolean useBundleCompression = true;
    private BundleMetadata bundleMetadata = DEFAULT_BUNDLE_METADATA;

    public Builder withAppBundle(AppBundle appBundle) {
      this.appBundle = appBundle;

      // If not set, set a default BundleConfig with the latest bundletool version.
      if (appBundle.getBundleConfig().equals(BundleConfig.getDefaultInstance())) {
        this.appBundle = appBundle.toBuilder().setBundleConfig(DEFAULT_BUNDLE_CONFIG).build();
      }
      return this;
    }

    /**
     * Note: this actually merges the given BundleConfig with the default BundleConfig to allow
     * clients to specify only partial BundleConfig in their tests.
     */
    public Builder withBundleConfig(BundleConfig.Builder bundleConfig) {
      this.bundleConfig = this.bundleConfig.toBuilder().mergeFrom(bundleConfig.build()).build();
      return this;
    }

    public Builder withBundleMetadata(BundleMetadata bundleMetadata) {
      this.bundleMetadata = bundleMetadata;
      return this;
    }

    public Builder withSigningConfig(SigningConfiguration signingConfig) {
      this.signingConfig = signingConfig;
      return this;
    }

    public Builder withBundletoolVersion(String bundletoolVersion) {
      this.bundleConfig =
          this.bundleConfig.toBuilder()
              .mergeFrom(
                  BundleConfig.newBuilder()
                      .setBundletool(Bundletool.newBuilder().setVersion(bundletoolVersion))
                      .build())
              .build();
      return this;
    }

    public Builder withApkModifier(ApkModifier apkModifier) {
      this.apkModifier = apkModifier;
      return this;
    }

    public Builder withApkListener(ApkListener apkListener) {
      this.apkListener = apkListener;
      return this;
    }

    public Builder withFirstVariantNumber(int firstVariantNumber) {
      this.firstVariantNumber = firstVariantNumber;
      return this;
    }

    public Builder withExecutorService(ListeningExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public Builder withBundlePath(Path bundlePath) {
      this.bundlePath = bundlePath;
      return this;
    }

    public Builder withOutputPath(Path outputPath) {
      this.outputPath = outputPath;
      return this;
    }

    public Builder withApkBuildMode(ApkBuildMode apkBuildMode) {
      this.apkBuildMode = apkBuildMode;
      return this;
    }

    public Builder withModules(String... moduleNames) {
      this.moduleNames = moduleNames;
      return this;
    }

    public Builder withDeviceSpec(DeviceSpec deviceSpec) {
      this.deviceSpec = deviceSpec;
      return this;
    }

    public Builder withOptimizationDimensions(OptimizationDimension... optimizationDimensions) {
      this.optimizationDimensions = optimizationDimensions;
      return this;
    }

    public Builder withCustomBuildApksCommandSetter(
        Consumer<BuildApksCommand.Builder> buildApksCommandSetter) {
      this.buildApksCommandSetter = buildApksCommandSetter;
      return this;
    }

    public Builder withOutputPrintStream(PrintStream printStream) {
      this.printStream = printStream;
      return this;
    }

    public Builder withLocalTestingEnabled(boolean enabled) {
      this.localTestingEnabled = enabled;
      return this;
    }

    public Builder withSourceStamp(SourceStamp sourceStamp) {
      this.sourceStamp = sourceStamp;
      return this;
    }

    public Builder useBundleCompression(boolean useBundleCompression) {
      this.useBundleCompression = useBundleCompression;
      return this;
    }

    public TestModule build() {
      try {
        if (tempDirectory == null) {
          tempDirectory = new TempDirectory();
        }
        if (outputDirectory == null) {
          outputDirectory = tempDirectory.getPath();
        }

        checkArgument(
            appBundle == null || bundlePath == null,
            "Cannot call both withAppBundle() and withBundlePath().");
        if (appBundle == null) {
          if (bundlePath != null) {
            appBundle = AppBundle.buildFromZip(new ZipFile(bundlePath.toFile()));
          } else {
            appBundle =
                new AppBundleBuilder()
                    .setBundleConfig(bundleConfig)
                    .addModule("base", module -> module.setManifest(androidManifest("com.package")))
                    .build();
          }
        } else {
          if (!bundleConfig.equals(DEFAULT_BUNDLE_CONFIG)) {
            BundleConfig newBundleConfig =
                appBundle.getBundleConfig().toBuilder().mergeFrom(bundleConfig).build();
            appBundle = appBundle.toBuilder().setBundleConfig(newBundleConfig).build();
          }
        }
        if (!bundleMetadata.equals(DEFAULT_BUNDLE_METADATA)) {
          appBundle = appBundle.toBuilder().setBundleMetadata(bundleMetadata).build();
        }
        if (bundlePath == null) {
          bundlePath = tempDirectory.getPath().resolve("bundle.aab");
          new AppBundleSerializer().writeToDisk(appBundle, bundlePath);
        }
        if (outputPath == null) {
          outputPath = outputDirectory.resolve("bundle.apks");
        }

        BuildApksCommand.Builder command =
            BuildApksCommand.builder()
                .setAapt2Command(Aapt2Helper.getAapt2Command())
                .setBundlePath(bundlePath)
                .setOutputFile(outputPath);
        if (signingConfig != null) {
          command.setSigningConfiguration(signingConfig);
        }
        if (apkModifier != null) {
          command.setApkModifier(apkModifier);
        }
        if (apkListener != null) {
          command.setApkListener(apkListener);
        }
        if (firstVariantNumber != null) {
          command.setFirstVariantNumber(firstVariantNumber);
        }
        if (executorService != null) {
          command.setExecutorService(executorService);
        }
        if (apkBuildMode != null) {
          command.setApkBuildMode(apkBuildMode);
        }
        if (moduleNames != null) {
          command.setModules(ImmutableSet.copyOf(moduleNames));
        }
        if (deviceSpec != null) {
          command.setDeviceSpec(deviceSpec);
        }
        if (optimizationDimensions != null) {
          command.setOptimizationDimensions(ImmutableSet.copyOf(optimizationDimensions));
        }
        if (printStream != null) {
          command.setOutputPrintStream(printStream);
        }
        if (localTestingEnabled != null) {
          command.setLocalTestingMode(localTestingEnabled);
        }
        if (sourceStamp != null) {
          command.setSourceStamp(sourceStamp);
        }
        if (buildApksCommandSetter != null) {
          buildApksCommandSetter.accept(command);
        }

        return new TestModule(command.build(), appBundle, useBundleCompression);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
