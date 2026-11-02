#!/usr/bin/env python3
"""
Single-command Android Fat APK Builder for mdtools using standard toolchain.
Cross-platform (Linux, macOS, Windows) and fully self-contained.
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
import xml.etree.ElementTree as ET
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

# Exact, deterministic dependencies for the standalone Kotlin compiler runner
KOTLIN_COMPILER_DEPS = [
    ('org.jetbrains.kotlin', 'kotlin-compiler', '2.0.21', 'jar'),
    ('org.jetbrains.kotlin', 'kotlin-stdlib', '2.0.21', 'jar'),
    ('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.0.21', 'jar'),
    ('org.jetbrains.kotlin', 'kotlin-reflect', '1.6.10', 'jar'),
    ('org.jetbrains.intellij.deps', 'trove4j', '1.0.20200330', 'jar'),
    ('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.6.4', 'jar'),
    ('org.jetbrains', 'annotations', '24.0.1', 'jar'),
]

TEST_DEPENDENCIES = [
    ('junit', 'junit', '4.13.2', 'jar'),
    ('org.hamcrest', 'hamcrest-core', '1.3', 'jar'),
]


def find_android_sdk(custom_path=None):
    if custom_path and os.path.isdir(custom_path):
        return os.path.abspath(custom_path)

    # 1. Environment variables
    for ev in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
        val = os.environ.get(ev)
        if val and os.path.isdir(val):
            return os.path.abspath(val)

    # 2. Check if aapt2 or d8 is in PATH and infer SDK root
    for tool in ["aapt2", "d8"]:
        tool_path = shutil.which(tool)
        if tool_path:
            bt_dir = os.path.dirname(os.path.abspath(tool_path))
            parent_dir = os.path.dirname(bt_dir)
            if os.path.basename(parent_dir) == "build-tools":
                sdk_candidate = os.path.dirname(parent_dir)
                if os.path.isdir(os.path.join(sdk_candidate, "platforms")):
                    return sdk_candidate

    # 3. Standard platform candidate directories
    candidates = [
        # macOS
        os.path.expanduser("~/Library/Android/sdk"),
        "/opt/homebrew/share/android-commandlinetools",
        "/usr/local/share/android-commandlinetools",
        "/usr/local/share/android-sdk",
        # Linux
        os.path.expanduser("~/Android/Sdk"),
        "/usr/lib/android-sdk",
        "/usr/local/android-sdk",
        "/opt/android-sdk",
        # Windows
        os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk"),
        os.path.expandvars(r"%ProgramFiles(x86)%\Android\android-sdk"),
    ]
    for c in candidates:
        if c and os.path.isdir(c):
            return os.path.abspath(c)

    sys.exit("Error: Could not locate Android SDK. Set ANDROID_HOME environment variable or pass --android-sdk.")


def get_java_major_version(java_bin):
    try:
        out = subprocess.check_output([java_bin, "-version"], stderr=subprocess.STDOUT, text=True)
        for line in out.splitlines():
            if "version" in line:
                parts = line.split('"')
                if len(parts) >= 2:
                    v_str = parts[1]
                    seg = v_str.split(".")[0]
                    if seg == "1":
                        return int(v_str.split(".")[1])
                    return int(seg)
    except Exception:
        pass
    return 0


def find_java(custom_home=None):
    is_win = sys.platform.startswith("win")
    ext = ".exe" if is_win else ""

    def check_jdk(jh):
        if not jh or not os.path.isdir(jh):
            return None
        java = os.path.join(jh, "bin", f"java{ext}")
        javac = os.path.join(jh, "bin", f"javac{ext}")
        keytool = os.path.join(jh, "bin", f"keytool{ext}")
        if os.path.isfile(java):
            major = get_java_major_version(java)
            return java, (javac if os.path.isfile(javac) else "javac"), (keytool if os.path.isfile(keytool) else "keytool"), major
        return None

    # 1. Custom home argument
    if custom_home:
        res = check_jdk(custom_home)
        if res:
            return res[0], res[1], res[2]
        sys.exit(f"Error: Java not found in specified --java-home ({custom_home})")

    candidates = []
    # 2. JAVA_HOME environment variable
    if os.environ.get("JAVA_HOME"):
        candidates.append(os.environ.get("JAVA_HOME"))

    # 3. Common platform directories
    candidates.extend([
        "/opt/homebrew/opt/openjdk@17",
        "/opt/homebrew/opt/openjdk@21",
        "/usr/local/opt/openjdk@17",
        "/usr/local/opt/openjdk@21",
        "/usr/lib/jvm/default-java",
    ])

    # Glob search for JDK installations
    glob_patterns = [
        "/Library/Java/JavaVirtualMachines/*/Contents/Home",
        os.path.expanduser("~/Library/Java/JavaVirtualMachines/*/Contents/Home"),
        "/usr/lib/jvm/java-17*",
        "/usr/lib/jvm/java-21*",
        "/usr/lib/jvm/*-17*",
        "/usr/lib/jvm/*-21*",
        r"C:\Program Files\Java\jdk-17*",
        r"C:\Program Files\Java\jdk-21*",
        r"C:\Program Files\Eclipse Adoptium\jdk-17*",
        r"C:\Program Files\Eclipse Adoptium\jdk-21*",
    ]
    for pattern in glob_patterns:
        candidates.extend(sorted(glob.glob(pattern), reverse=True))

    # Evaluate candidates
    found = []
    for c in candidates:
        res = check_jdk(c)
        if res and res not in found:
            found.append(res)

    # 4. Also check PATH
    java_in_path = shutil.which("java")
    javac_in_path = shutil.which("javac")
    keytool_in_path = shutil.which("keytool")
    if java_in_path:
        major = get_java_major_version(java_in_path)
        found.append((java_in_path, (javac_in_path or "javac"), (keytool_in_path or "keytool"), major))

    # Kotlin 2.0.x compiler officially supports running on JDK 17 through 21
    compatible = [f for f in found if 17 <= f[3] <= 21]
    if compatible:
        return compatible[0][0], compatible[0][1], compatible[0][2]

    # Fallback to any Java >= 17
    fallback = [f for f in found if f[3] >= 17]
    if fallback:
        return fallback[0][0], fallback[0][1], fallback[0][2]

    sys.exit("Error: Could not locate Java 17-21 or keytool. Please set JAVA_HOME or pass --java-home.")


def find_tool(name, directory):
    for ext in ["", ".exe", ".bat", ".cmd"]:
        p = os.path.join(directory, name + ext)
        if os.path.isfile(p):
            return p
    w = shutil.which(name)
    if w:
        return w
    sys.exit(f"Error: Tool '{name}' not found in {directory} or in PATH.")


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

    # Check ~/.m2/repository (if present)
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

    # Download from Maven Central or Google Maven
    grp_path = grp.replace(".", "/")
    urls = [
        f"https://repo.maven.apache.org/maven2/{grp_path}/{name}/{ver}/{filename}",
        f"https://maven.google.com/{grp_path}/{name}/{ver}/{filename}",
    ]
    for url in urls:
        if download_file(url, cached_file):
            return cached_file

    sys.exit(f"Error: Unable to resolve dependency {grp}:{name}:{ver}.{ext}")


def main():
    parser = argparse.ArgumentParser(description="Build mdtools fat APK (cross-platform & portable)")
    parser.add_argument("--project-dir", default=os.path.abspath(os.path.dirname(__file__)), help="Project root directory")
    parser.add_argument("--output-dir", default=None, help="Target build output directory")
    parser.add_argument("--android-sdk", default=None, help="Path to Android SDK directory")
    parser.add_argument("--java-home", default=None, help="Path to Java 17+ JDK home directory")
    parser.add_argument("--build-tools", default=None, help="Build-tools version or specific folder")
    parser.add_argument("--platform", default=None, help="Android platform (e.g. android-34, android-36)")
    parser.add_argument("--skip-tests", action="store_true", help="Skip running unit tests")
    args = parser.parse_args()

    project_dir = os.path.abspath(args.project_dir)
    target_dir = args.output_dir or os.path.join(project_dir, "target")
    os.makedirs(target_dir, exist_ok=True)

    sdk_dir = find_android_sdk(args.android_sdk)
    java_bin, javac_bin, keytool_bin = find_java(args.java_home)

    # Locate build-tools
    bt_dir = os.path.join(sdk_dir, "build-tools")
    if not os.path.isdir(bt_dir):
        sys.exit(f"Error: No build-tools directory found in Android SDK at {sdk_dir}")

    if args.build_tools:
        if os.path.isdir(args.build_tools):
            build_tools = os.path.abspath(args.build_tools)
        else:
            build_tools = os.path.join(bt_dir, args.build_tools)
            if not os.path.isdir(build_tools):
                sys.exit(f"Error: Specified build-tools '{args.build_tools}' not found in {bt_dir}")
    else:
        bt_versions = sorted(os.listdir(bt_dir), reverse=True)
        if not bt_versions:
            sys.exit("Error: No build-tools installed in Android SDK.")
        build_tools = os.path.join(bt_dir, bt_versions[0])

    aapt2 = find_tool("aapt2", build_tools)
    d8 = find_tool("d8", build_tools)
    zipalign = find_tool("zipalign", build_tools)
    apksigner = find_tool("apksigner", build_tools)

    # Locate platforms
    plat_dir = os.path.join(sdk_dir, "platforms")
    if not os.path.isdir(plat_dir):
        sys.exit(f"Error: No platforms directory found in Android SDK at {sdk_dir}")

    if args.platform:
        plat_target = os.path.join(plat_dir, args.platform)
        if not os.path.isdir(plat_target):
            sys.exit(f"Error: Specified platform '{args.platform}' not found in {plat_dir}")
        android_jar = os.path.join(plat_target, "android.jar")
    else:
        plat_versions = sorted([p for p in os.listdir(plat_dir) if p.startswith("android-")], reverse=True)
        if not plat_versions:
            sys.exit("Error: No platform installed in Android SDK.")
        android_jar = os.path.join(plat_dir, plat_versions[0], "android.jar")

    if not os.path.isfile(android_jar):
        sys.exit(f"Error: android.jar not found at {android_jar}")

    # Infer target SDK version from selected platform (e.g. android-36 -> 36)
    plat_name = os.path.basename(os.path.dirname(android_jar))
    target_sdk = plat_name.replace("android-", "") if plat_name.startswith("android-") else "36"

    print("=== mdtools Android APK Build ===")
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

    # Compile app resources
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
        "--target-sdk-version", target_sdk,
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

    compile_classpath = os.pathsep.join([android_jar] + jars + aar_classes_jars)

    # Resolve standalone Kotlin compiler runner classpath deterministically
    kotlinc_runner_jars = []
    for grp, name, ver, ext in KOTLIN_COMPILER_DEPS:
        kotlinc_runner_jars.append(resolve_artifact(grp, name, ver, ext, cache_dir))
    runner_cp = os.pathsep.join(kotlinc_runner_jars)

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
            "-cp", compile_classpath + os.pathsep + classes_out,
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
            test_compile_cp = os.pathsep.join([classes_out, android_jar] + jars + aar_classes_jars + test_jars)
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

            # Automatically discover test class names
            test_classes = []
            for tf in sorted(test_files):
                pkg = None
                with open(tf, "r", encoding="utf-8") as f:
                    for line in f:
                        line_str = line.strip()
                        if line_str.startswith("package "):
                            pkg = line_str.split()[1]
                            break
                cls_name = os.path.splitext(os.path.basename(tf))[0]
                if pkg:
                    test_classes.append(f"{pkg}.{cls_name}")
                else:
                    test_classes.append(cls_name)

            run_cp = os.pathsep.join([test_classes_out, classes_out, android_jar] + jars + aar_classes_jars + test_jars)
            test_runner_cmd = [
                java_bin,
                "-cp", run_cp,
                "org.junit.runner.JUnitCore"
            ] + test_classes
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

    apk_size_mb = os.path.getsize(final_apk) / (1024 * 1024)
    print("========================================")
    print(f"BUILD SUCCESS!")
    print(f"Fat APK created: {final_apk}")
    print(f"Versioned APK:   {versioned_apk}")
    print(f"APK Size:        {apk_size_mb:.2f} MB")
    print(f"Package:         it.ilpelle.mdtools")
    print(f"Signing:         Verified (v1, v2, v3)")
    print("========================================")


if __name__ == "__main__":
    main()
