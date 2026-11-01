#!/usr/bin/env python3
"""
Single-command Android Fat APK Builder for mdtools using standard toolchain.
Invoked by Maven during 'mvn package' or run directly with:
    python3 build-apk.py
"""

import argparse
import glob
import os
import shutil
import ssl
import subprocess
import sys
import urllib.request
import zipfile

DEPENDENCIES = [
    ('androidx.activity', 'activity', '1.8.0', 'aar'),
    ('androidx.annotation', 'annotation-experimental', '1.3.0', 'aar'),
    ('androidx.annotation', 'annotation-jvm', '1.6.0', 'jar'),
    ('androidx.appcompat', 'appcompat', '1.6.1', 'aar'),
    ('androidx.appcompat', 'appcompat-resources', '1.6.1', 'aar'),
    ('androidx.arch.core', 'core-common', '2.2.0', 'jar'),
    ('androidx.arch.core', 'core-runtime', '2.2.0', 'aar'),
    ('androidx.cardview', 'cardview', '1.0.0', 'aar'),
    ('androidx.collection', 'collection', '1.1.0', 'jar'),
    ('androidx.concurrent', 'concurrent-futures', '1.1.0', 'jar'),
    ('androidx.constraintlayout', 'constraintlayout', '2.0.1', 'aar'),
    ('androidx.constraintlayout', 'constraintlayout-solver', '2.0.1', 'jar'),
    ('androidx.coordinatorlayout', 'coordinatorlayout', '1.1.0', 'aar'),
    ('androidx.core', 'core', '1.10.1', 'aar'),
    ('androidx.core', 'core-ktx', '1.10.1', 'aar'),
    ('androidx.cursoradapter', 'cursoradapter', '1.0.0', 'aar'),
    ('androidx.customview', 'customview', '1.1.0', 'aar'),
    ('androidx.documentfile', 'documentfile', '1.0.0', 'aar'),
    ('androidx.drawerlayout', 'drawerlayout', '1.1.1', 'aar'),
    ('androidx.dynamicanimation', 'dynamicanimation', '1.0.0', 'aar'),
    ('androidx.emoji2', 'emoji2', '1.2.0', 'aar'),
    ('androidx.emoji2', 'emoji2-views-helper', '1.2.0', 'aar'),
    ('androidx.fragment', 'fragment', '1.3.6', 'aar'),
    ('androidx.interpolator', 'interpolator', '1.0.0', 'aar'),
    ('androidx.legacy', 'legacy-support-core-utils', '1.0.0', 'aar'),
    ('androidx.lifecycle', 'lifecycle-common', '2.6.1', 'jar'),
    ('androidx.lifecycle', 'lifecycle-livedata', '2.6.1', 'aar'),
    ('androidx.lifecycle', 'lifecycle-livedata-core', '2.6.1', 'aar'),
    ('androidx.lifecycle', 'lifecycle-process', '2.6.1', 'aar'),
    ('androidx.lifecycle', 'lifecycle-runtime', '2.6.1', 'aar'),
    ('androidx.lifecycle', 'lifecycle-viewmodel', '2.6.1', 'aar'),
    ('androidx.lifecycle', 'lifecycle-viewmodel-savedstate', '2.6.1', 'aar'),
    ('androidx.loader', 'loader', '1.0.0', 'aar'),
    ('androidx.localbroadcastmanager', 'localbroadcastmanager', '1.0.0', 'aar'),
    ('androidx.print', 'print', '1.0.0', 'aar'),
    ('androidx.profileinstaller', 'profileinstaller', '1.3.0', 'aar'),
    ('androidx.recyclerview', 'recyclerview', '1.1.0', 'aar'),
    ('androidx.resourceinspection', 'resourceinspection-annotation', '1.0.1', 'jar'),
    ('androidx.savedstate', 'savedstate', '1.2.1', 'aar'),
    ('androidx.startup', 'startup-runtime', '1.1.1', 'aar'),
    ('androidx.tracing', 'tracing', '1.0.0', 'aar'),
    ('androidx.transition', 'transition', '1.2.0', 'aar'),
    ('androidx.vectordrawable', 'vectordrawable', '1.1.0', 'aar'),
    ('androidx.vectordrawable', 'vectordrawable-animated', '1.1.0', 'aar'),
    ('androidx.versionedparcelable', 'versionedparcelable', '1.1.1', 'aar'),
    ('androidx.viewpager', 'viewpager', '1.0.0', 'aar'),
    ('androidx.viewpager2', 'viewpager2', '1.0.0', 'aar'),
    ('com.google.android.material', 'material', '1.10.0', 'aar'),
    ('com.google.errorprone', 'error_prone_annotations', '2.15.0', 'jar'),
    ('com.google.guava', 'listenablefuture', '1.0', 'jar'),
    ('com.vladsch.flexmark', 'flexmark', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-ext-tables', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-ast', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-builder', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-collection', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-data', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-dependency', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-format', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-html', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-misc', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-options', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-sequence', '0.64.8', 'jar'),
    ('com.vladsch.flexmark', 'flexmark-util-visitor', '0.64.8', 'jar'),
    ('org.jetbrains', 'annotations', '24.0.1', 'jar'),
    ('org.jetbrains.kotlin', 'kotlin-stdlib', '2.0.21', 'jar'),
    ('org.jetbrains.kotlin', 'kotlin-stdlib-jdk7', '1.8.22', 'jar'),
    ('org.jetbrains.kotlin', 'kotlin-stdlib-jdk8', '1.8.22', 'jar'),
    ('org.jetbrains.kotlinx', 'kotlinx-coroutines-android', '1.6.4', 'jar'),
    ('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.6.4', 'jar'),
]

KOTLIN_COMPILER_DEPS = [
    ('org.jetbrains.kotlin', 'kotlin-compiler', '2.0.21', 'jar'),
    ('org.jetbrains.kotlin', 'kotlin-stdlib', '2.0.21', 'jar'),
]

TEST_DEPENDENCIES = [
    ('junit', 'junit', '4.13.2', 'jar'),
    ('org.hamcrest', 'hamcrest-core', '1.3', 'jar'),
]


def find_android_sdk():
    candidates = [
        os.environ.get("ANDROID_HOME"),
        os.environ.get("ANDROID_SDK_ROOT"),
        "/opt/homebrew/share/android-commandlinetools",
        os.path.expanduser("~/Library/Android/sdk"),
        "/usr/local/share/android-sdk",
        os.path.expanduser("~/Android/Sdk"),
    ]
    for c in candidates:
        if c and os.path.isdir(c):
            return c
    sys.exit("Error: Could not locate Android SDK. Set ANDROID_HOME or ANDROID_SDK_ROOT.")


def find_java():
    candidates = [
        os.environ.get("JAVA_HOME"),
        "/opt/homebrew/opt/openjdk@17",
    ]
    for c in candidates:
        if c and os.path.isfile(os.path.join(c, "bin", "java")):
            return os.path.join(c, "bin", "java"), os.path.join(c, "bin", "javac"), os.path.join(c, "bin", "keytool")
    java_in_path = shutil.which("java")
    javac_in_path = shutil.which("javac")
    keytool_in_path = shutil.which("keytool")
    if java_in_path and keytool_in_path:
        return java_in_path, (javac_in_path or "javac"), keytool_in_path
    sys.exit("Error: Could not locate Java 17+ or keytool. Set JAVA_HOME.")


def download_file(url, dest_path):
    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
    ctx = ssl._create_unverified_context()
    req = urllib.request.Request(url, headers={"User-Agent": "mdtools-build/1.0"})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=30) as resp, open(dest_path, "wb") as out:
            shutil.copyfileobj(resp, out)
        return True
    except Exception:
        if os.path.exists(dest_path):
            os.remove(dest_path)
        return False


def resolve_artifact(grp, name, ver, ext, cache_dir):
    filename = f"{name}-{ver}.{ext}"
    cached_file = os.path.join(cache_dir, filename)
    if os.path.exists(cached_file) and os.path.getsize(cached_file) > 0:
        return cached_file

    # Check ~/.m2/repository
    m2_path = os.path.join(
        os.path.expanduser("~/.m2/repository"),
        *grp.split("."),
        name,
        ver,
        filename
    )
    if os.path.exists(m2_path) and os.path.getsize(m2_path) > 0:
        shutil.copyfile(m2_path, cached_file)
        return cached_file

    # Check ~/.gradle/caches
    gradle_pattern = os.path.join(
        os.path.expanduser("~/.gradle/caches/modules-2/files-2.1"),
        grp,
        name,
        ver,
        "**",
        filename
    )
    gradle_matches = glob.glob(gradle_pattern, recursive=True)
    if gradle_matches:
        shutil.copyfile(gradle_matches[0], cached_file)
        return cached_file

    # Download from Google or Central
    grp_path = grp.replace(".", "/")
    urls = [
        f"https://maven.google.com/{grp_path}/{name}/{ver}/{filename}",
        f"https://repo.maven.apache.org/maven2/{grp_path}/{name}/{ver}/{filename}",
    ]
    for url in urls:
        if download_file(url, cached_file):
            return cached_file

    sys.exit(f"Error: Unable to resolve dependency {grp}:{name}:{ver}.{ext}")


def main():
    parser = argparse.ArgumentParser(description="Build mdtools fat APK")
    parser.add_argument("--project-dir", default=os.path.abspath(os.path.dirname(__file__)))
    parser.add_argument("--output-dir", default=None)
    parser.add_argument("--skip-tests", action="store_true", help="Skip running unit tests")
    args = parser.parse_args()

    project_dir = os.path.abspath(args.project_dir)
    target_dir = args.output_dir or os.path.join(project_dir, "target")
    os.makedirs(target_dir, exist_ok=True)

    sdk_dir = find_android_sdk()
    java_bin, javac_bin, keytool_bin = find_java()

    # Find build-tools
    bt_dir = os.path.join(sdk_dir, "build-tools")
    bt_versions = sorted(os.listdir(bt_dir), reverse=True)
    if not bt_versions:
        sys.exit("Error: No build-tools installed in Android SDK.")
    build_tools = os.path.join(bt_dir, bt_versions[0])

    aapt2 = os.path.join(build_tools, "aapt2")
    d8 = os.path.join(build_tools, "d8")
    zipalign = os.path.join(build_tools, "zipalign")
    apksigner = os.path.join(build_tools, "apksigner")

    # Find platforms
    plat_dir = os.path.join(sdk_dir, "platforms")
    plat_versions = sorted([p for p in os.listdir(plat_dir) if p.startswith("android-")], reverse=True)
    if not plat_versions:
        sys.exit("Error: No platform installed in Android SDK.")
    android_jar = os.path.join(plat_dir, plat_versions[0], "android.jar")

    print(f"=== mdtools Android APK Build ===")
    print(f"Project directory: {project_dir}")
    print(f"Target directory:  {target_dir}")
    print(f"Build tools:       {build_tools}")
    print(f"Platform:          {android_jar}")
    print(f"Java runtime:      {java_bin}")

    # Step 1: Resolve Dependencies
    cache_dir = os.path.join(target_dir, "dependency_cache")
    os.makedirs(cache_dir, exist_ok=True)
    print("Resolving dependencies...")
    aars = []
    jars = []
    for grp, name, ver, ext in DEPENDENCIES:
        path = resolve_artifact(grp, name, ver, ext, cache_dir)
        if ext == "aar":
            aars.append(path)
        else:
            jars.append(path)

    # Step 2: Unpack AARs & Compile Resources
    unpacked_aars_dir = os.path.join(target_dir, "unpacked_aars")
    compiled_res_dir = os.path.join(target_dir, "compiled_res")
    os.makedirs(unpacked_aars_dir, exist_ok=True)
    os.makedirs(compiled_res_dir, exist_ok=True)

    aar_classes_jars = []
    compiled_res_zips = []

    for aar in aars:
        aar_base = os.path.splitext(os.path.basename(aar))[0]
        out_dir = os.path.join(unpacked_aars_dir, aar_base)
        if not os.path.exists(out_dir):
            with zipfile.ZipFile(aar, "r") as z:
                z.extractall(out_dir)
        c_jar = os.path.join(out_dir, "classes.jar")
        if os.path.exists(c_jar):
            aar_classes_jars.append(c_jar)
        res_dir = os.path.join(out_dir, "res")
        if os.path.exists(res_dir) and os.listdir(res_dir):
            res_zip = os.path.join(compiled_res_dir, f"{aar_base}.zip")
            if not os.path.exists(res_zip):
                res = subprocess.run([aapt2, "compile", "--dir", res_dir, "-o", res_zip], capture_output=True, text=True)
                if res.returncode != 0:
                    print(f"Warning compiling res for {aar_base}: {res.stderr}")
            if os.path.exists(res_zip):
                compiled_res_zips.append(res_zip)

    # Compile app res
    app_res_dir = os.path.join(project_dir, "app", "src", "main", "res")
    app_res_zip = os.path.join(compiled_res_dir, "app_res.zip")
    res = subprocess.run([aapt2, "compile", "--dir", app_res_dir, "-o", app_res_zip], capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"Error compiling application resources: {res.stderr}")

    # Step 3: AAPT2 Link
    gen_r_dir = os.path.join(target_dir, "generated-sources", "r")
    os.makedirs(gen_r_dir, exist_ok=True)
    unaligned_apk = os.path.join(target_dir, "unaligned.apk")
    if os.path.exists(unaligned_apk):
        os.remove(unaligned_apk)
    manifest_file = os.path.join(project_dir, "app", "src", "main", "AndroidManifest.xml")
    assets_dir = os.path.join(project_dir, "app", "src", "main", "assets")

    link_cmd = [
        aapt2, "link",
        "-I", android_jar,
        "--manifest", manifest_file,
        "--min-sdk-version", "24",
        "--target-sdk-version", "36",
        "--version-code", "1",
        "--version-name", "1.0",
        "-A", assets_dir,
        "-o", unaligned_apk,
        "--java", gen_r_dir,
        "--auto-add-overlay",
        "-R", app_res_zip
    ]
    for rz in compiled_res_zips:
        link_cmd.extend(["-R", rz])

    # Collect library package names for R.java generation
    import xml.etree.ElementTree as ET
    extra_packages = []
    for aar_manifest in glob.glob(os.path.join(unpacked_aars_dir, "*", "AndroidManifest.xml")):
        try:
            tree = ET.parse(aar_manifest)
            pkg = tree.getroot().attrib.get("package")
            if pkg and pkg != "it.ilpelle.mdtools" and pkg not in extra_packages:
                extra_packages.append(pkg)
        except Exception:
            pass

    if extra_packages:
        link_cmd.extend(["--extra-packages", ":".join(extra_packages)])

    print("Linking resources with aapt2...")
    res = subprocess.run(link_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"aapt2 link failed: {res.stderr}")

    # Step 4: Compile Kotlin and Java
    r_java_files = glob.glob(os.path.join(gen_r_dir, "**", "*.java"), recursive=True)
    kt_src_dir = os.path.join(project_dir, "app", "src", "main", "java")
    kt_files = glob.glob(os.path.join(kt_src_dir, "**", "*.kt"), recursive=True)

    classes_out = os.path.join(target_dir, "classes")
    shutil.rmtree(classes_out, ignore_errors=True)
    os.makedirs(classes_out, exist_ok=True)

    compile_classpath = ":".join([android_jar] + jars + aar_classes_jars)

    # Locate Kotlin compiler jars
    kotlinc_jar = os.path.expanduser("~/.m2/repository/org/jetbrains/kotlin/kotlin-compiler/2.0.21/kotlin-compiler-2.0.21.jar")
    if not os.path.exists(kotlinc_jar):
        kotlinc_jar = resolve_artifact("org.jetbrains.kotlin", "kotlin-compiler", "2.0.21", "jar", cache_dir)

    # All dependencies needed for kotlin compiler runner
    m2_jars = glob.glob(os.path.expanduser("~/.m2/repository/**/*.jar"), recursive=True)
    runner_cp = ":".join([kotlinc_jar] + m2_jars + jars)

    compile_cmd = [
        java_bin,
        "-cp", runner_cp,
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-no-stdlib",
        "-cp", compile_classpath,
        "-d", classes_out,
        "-jvm-target", "17"
    ] + kt_files + r_java_files

    print("Compiling Kotlin sources...")
    res = subprocess.run(compile_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"Kotlin compilation failed: {res.stderr}\n{res.stdout}")

    if r_java_files:
        print(f"Compiling {len(r_java_files)} generated R.java files with javac...")
        javac_cmd = [
            javac_bin,
            "-cp", compile_classpath + ":" + classes_out,
            "-d", classes_out,
            "--release", "17"
        ] + r_java_files
        res = subprocess.run(javac_cmd, capture_output=True, text=True)
        if res.returncode != 0:
            sys.exit(f"javac compilation failed: {res.stderr}\n{res.stdout}")

    # Step 4.5: Run Unit Tests
    if not args.skip_tests:
        test_src_dir = os.path.join(project_dir, "app", "src", "test", "java")
        test_files = glob.glob(os.path.join(test_src_dir, "**", "*.kt"), recursive=True)
        if test_files:
            print("Compiling and running unit tests...")
            test_jars = []
            for grp, name, ver, ext in TEST_DEPENDENCIES:
                test_jars.append(resolve_artifact(grp, name, ver, ext, cache_dir))
            test_classes_out = os.path.join(target_dir, "test-classes")
            shutil.rmtree(test_classes_out, ignore_errors=True)
            os.makedirs(test_classes_out, exist_ok=True)
            test_compile_cp = ":".join([classes_out, android_jar] + jars + aar_classes_jars + test_jars)
            test_compile_cmd = [
                java_bin,
                "-cp", runner_cp,
                "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                "-no-stdlib",
                "-cp", test_compile_cp,
                "-d", test_classes_out,
                "-jvm-target", "17"
            ] + test_files
            res = subprocess.run(test_compile_cmd, capture_output=True, text=True)
            if res.returncode != 0:
                sys.exit(f"Test compilation failed: {res.stderr}\n{res.stdout}")

            run_cp = ":".join([test_classes_out, classes_out, android_jar] + jars + aar_classes_jars + test_jars)
            test_runner_cmd = [
                java_bin,
                "-cp", run_cp,
                "org.junit.runner.JUnitCore",
                "it.ilpelle.mdtools.ExampleUnitTest",
                "it.ilpelle.mdtools.table.MarkdownTableTest",
                "it.ilpelle.mdtools.table.MarkdownTableParserTest",
                "it.ilpelle.mdtools.table.MarkdownTableFormatterTest"
            ]
            res = subprocess.run(test_runner_cmd, capture_output=True, text=True)
            print(res.stdout)
            if res.returncode != 0:
                sys.exit(f"Unit tests failed:\n{res.stdout}\n{res.stderr}")

    # Step 5: Dex with D8
    dex_out = os.path.join(target_dir, "dex")
    shutil.rmtree(dex_out, ignore_errors=True)
    os.makedirs(dex_out, exist_ok=True)

    class_files = glob.glob(os.path.join(classes_out, "**", "*.class"), recursive=True)
    d8_inputs = class_files + jars + aar_classes_jars

    d8_cmd = [
        d8,
        "--release",
        "--min-api", "24",
        "--output", dex_out,
        "--lib", android_jar
    ] + d8_inputs

    print("Dexing classes and dependencies with D8 (single fat APK)...")
    res = subprocess.run(d8_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"D8 failed: {res.stderr}")

    # Step 6: Add DEX to APK
    dex_files = [os.path.join(dex_out, f) for f in os.listdir(dex_out) if f.endswith(".dex")]
    with zipfile.ZipFile(unaligned_apk, "a") as z:
        for df in dex_files:
            z.write(df, os.path.basename(df))

    # Step 7: Zipalign
    final_apk = os.path.join(target_dir, "mdtools.apk")
    versioned_apk = os.path.join(target_dir, "mdtools-1.0.apk")
    if os.path.exists(final_apk):
        os.remove(final_apk)
    if os.path.exists(versioned_apk):
        os.remove(versioned_apk)

    print("Aligning APK with zipalign...")
    res = subprocess.run([zipalign, "-f", "-p", "4", unaligned_apk, final_apk], capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"zipalign failed: {res.stderr}")

    # Step 8: Sign APK
    default_keystore = os.path.expanduser("~/.android/debug.keystore")
    if os.path.exists(default_keystore):
        keystore = default_keystore
    else:
        keystore = os.path.join(target_dir, "debug.keystore")
        if not os.path.exists(keystore):
            subprocess.run([
                keytool_bin, "-genkeypair", "-v",
                "-keystore", keystore,
                "-storepass", "android",
                "-alias", "androiddebugkey",
                "-keypass", "android",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=Android Debug,O=Android,C=US"
            ], check=True, capture_output=True)

    print("Signing APK with apksigner...")
    res = subprocess.run([
        apksigner, "sign",
        "--ks", keystore,
        "--ks-pass", "pass:android",
        "--ks-key-alias", "androiddebugkey",
        "--key-pass", "pass:android",
        "--v1-signing-enabled", "true",
        "--v2-signing-enabled", "true",
        "--v3-signing-enabled", "true",
        final_apk
    ], capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"apksigner failed: {res.stderr}")

    # Verify signature
    res = subprocess.run([apksigner, "verify", "--verbose", final_apk], capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"Signature verification failed: {res.stderr}")

    shutil.copyfile(final_apk, versioned_apk)

    # Also sync to app/build/outputs/apk/debug/app-debug.apk for backwards compatibility
    gradle_debug_apk = os.path.join(project_dir, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
    os.makedirs(os.path.dirname(gradle_debug_apk), exist_ok=True)
    shutil.copyfile(final_apk, gradle_debug_apk)

    apk_size_mb = os.path.getsize(final_apk) / (1024 * 1024)
    print("========================================")
    print(f"BUILD SUCCESS!")
    print(f"Fat APK created: {final_apk}")
    print(f"Versioned APK:   {versioned_apk}")
    print(f"Debug APK alias: {gradle_debug_apk}")
    print(f"APK Size:        {apk_size_mb:.2f} MB")
    print(f"Package:         it.ilpelle.mdtools")
    print(f"Signing:         Verified (v1, v2, v3)")
    print("========================================")


if __name__ == "__main__":
    main()
