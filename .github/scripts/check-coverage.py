"""Check JaCoCo line coverage of the production Java code changed since a Git revision."""

import argparse
import os
import re
import subprocess
from collections import defaultdict
from pathlib import Path
from xml.etree import ElementTree


MINIMUM_COVERAGE = 80
COVERAGE_DIR = Path("target/coverage")
JAVA_SOURCE = re.compile(r"((?:bundles|tools)/[^/]+)/src/(?:main/java/|(?=org/))(.+\.java)")
HUNK = re.compile(rb"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", re.MULTILINE)


def git_diff(*args):
    return subprocess.check_output([
        "git", "--literal-pathspecs", "diff", "--no-color", "--no-ext-diff", "--no-textconv", "--find-renames", *args,
    ])


def changed_sources(base):
    fields = iter(git_diff("--name-status", "--diff-filter=AMRT", "-z", base, "HEAD").split(b"\0")[:-1])
    for status in fields:
        old_path = os.fsdecode(next(fields))
        path = os.fsdecode(next(fields)) if status.startswith(b"R") else old_path
        source = JAVA_SOURCE.fullmatch(path)
        if not source or Path(path).name in {"module-info.java", "package-info.java"}:
            continue
        patch = git_diff("--text", "--unified=0", base, "HEAD", "--", *dict.fromkeys((old_path, path)))
        lines = set()
        for first, count in HUNK.findall(patch):
            start = int(first)
            lines.update(range(start, start + int(count or b"1")))
        if lines:
            module, package_path = source.groups()
            yield Path(module), package_path, path, lines


def jacoco(command, *args):
    subprocess.run(["java", "-jar", COVERAGE_DIR / "jacococli.jar", command, *args, "--quiet"], check=True)


def merge_execution_data():
    unit_data = sorted(
        path
        for directory in ("bundles", "tools")
        for path in Path(directory).glob("*/target/jacoco.exec")
        if path.stat().st_size
    )
    integration_data = sorted(path for path in Path("itests").glob("*/target/jacoco.exec") if path.stat().st_size)
    if not unit_data or not integration_data:
        raise SystemExit("Missing JaCoCo data. Run the unit and OSGi integration tests with -Pcoverage.")
    merged = COVERAGE_DIR / "jacoco.exec"
    jacoco("merge", *unit_data, *integration_data, "--destfile", merged)
    return merged


def module_coverage(module, execution_data):
    classes = module / "target/classes"
    if not classes.is_dir():
        raise SystemExit(f"Missing compiled classes for {module}.")
    # Some bundles contain classes with the same qualified name. Analyze each module separately.
    report = COVERAGE_DIR / module / "jacoco.xml"
    report.parent.mkdir(parents=True, exist_ok=True)
    jacoco("report", execution_data, "--classfiles", classes, "--xml", report)
    return {
        Path(package.attrib["name"], source.attrib["name"]).as_posix(): {
            int(line.attrib["nr"]): int(line.attrib["ci"]) > 0 for line in source.findall("line")
        }
        for package in ElementTree.parse(report).getroot().findall("package")
        for source in package.findall("sourcefile")
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("base", help="Git revision to compare with the tested HEAD")
    args = parser.parse_args()

    print("## Coverage of changed Java lines\n")
    sources_by_module = defaultdict(list)
    for module, package_path, path, lines in changed_sources(args.base):
        sources_by_module[module].append((package_path, path, lines))

    covered = total = 0
    missed = {}
    if sources_by_module:
        execution_data = merge_execution_data()
        for module, sources in sources_by_module.items():
            coverage = module_coverage(module, execution_data)
            for package_path, path, lines in sources:
                if package_path not in coverage:
                    raise SystemExit(f"Missing JaCoCo source data for {path}.")
                source = coverage[package_path]
                executable = lines & source.keys()
                uncovered = sorted(line for line in executable if not source[line])
                total += len(executable)
                covered += len(executable) - len(uncovered)
                if uncovered:
                    missed[path] = uncovered

    if not total:
        print("No changed executable Java lines.")
        return 0

    passed = 100 * covered >= MINIMUM_COVERAGE * total
    print(f"**{'Passed' if passed else 'Failed'}** · "
          f"{covered / total:.2%} ({covered}/{total} lines), required {MINIMUM_COVERAGE}%.")
    if missed:
        print("\nUncovered lines:\n")
        for path, lines in missed.items():
            print(f"- `{path}` lines {', '.join(map(str, lines))}")
    return int(not passed)


if __name__ == "__main__":
    raise SystemExit(main())
