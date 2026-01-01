# Code Quality Analysis Guide for openHAB Core

## Available Tools (Already Configured!)

The openHAB project has **three static analysis tools** already configured:

1. **Checkstyle** - Code style and formatting
2. **PMD** - Bug detection and code quality  
3. **SpotBugs** - Find bugs in Java code

## Quick Start: Run All Analysis Tools

### Option 1: Run All Static Analysis (Recommended)

```bash
# From project root
mvn verify
```

This runs:
- ✅ Checkstyle
- ✅ PMD
- ✅ SpotBugs
- ✅ Generates reports

### Option 2: Run Individual Tools

```bash
# Checkstyle only
mvn checkstyle:check

# PMD only
mvn pmd:check

# SpotBugs only
mvn spotbugs:check
```

### Option 3: Run for Specific Bundle

```bash
# Analyze a specific bundle
cd bundles/org.openhab.core.addon
mvn verify
```

## Viewing Results

### Reports Location

After running analysis, reports are generated in:

```
bundles/org.openhab.core.addon/target/
├── checkstyle-result.xml          # Checkstyle results
├── pmd.xml                         # PMD results
├── spotbugsXml.xml                 # SpotBugs results
└── site/
    ├── checkstyle.html             # HTML report
    ├── pmd.html                    # HTML report
    └── spotbugs.html               # HTML report
```

### View HTML Reports

1. **Open in Browser**:
   ```bash
   # Windows
   start bundles\org.openhab.core.addon\target\site\checkstyle.html
   
   # Or navigate to the file and open it
   ```

2. **Or use Maven site**:
   ```bash
   mvn site
   # Then open target/site/index.html
   ```

## Using Cursor's Built-in Linter

Cursor has a built-in Java linter that works automatically:

### How to Use

1. **Open a Java file** (e.g., `AddonParameter.java`)
2. **Issues appear automatically**:
   - Red squiggles = Errors
   - Yellow squiggles = Warnings
   - Blue squiggles = Info

3. **View All Issues**:
   - Press `Ctrl+Shift+M` (Problems panel)
   - Or click "Problems" in bottom panel

4. **Check Linter Errors Programmatically**:
   ```bash
   # I can check linter errors for you using the read_lints tool
   # Just ask me to check a specific file!
   ```

## Checkstyle - Code Style Analysis

### What It Checks

- Code formatting
- Naming conventions
- Code structure
- Best practices

### Run Checkstyle

```bash
# Check code style
mvn checkstyle:check

# Generate report
mvn checkstyle:checkstyle
```

### View Checkstyle Results

```bash
# Open the report
start bundles\org.openhab.core.addon\target\site\checkstyle.html
```

### Common Checkstyle Issues

- **Line length** (max 120 characters)
- **Import order** (must follow openHAB style)
- **Missing Javadoc**
- **Code formatting** (use Spotless to fix)

## PMD - Bug Detection

### What It Checks

- Potential bugs
- Dead code
- Code complexity
- Best practices violations

### Run PMD

```bash
# Run PMD analysis
mvn pmd:check

# Generate report
mvn pmd:pmd
```

### View PMD Results

```bash
# Open the report
start bundles\org.openhab.core.addon\target\site\pmd.html
```

### Common PMD Issues

- **Unused variables**
- **Complex methods** (too many lines/complexity)
- **Empty catch blocks**
- **Dead code**

## SpotBugs - Find Bugs

### What It Checks

- Null pointer dereferences
- Resource leaks
- Thread safety issues
- Performance problems

### Run SpotBugs

```bash
# Run SpotBugs analysis
mvn spotbugs:check

# Generate report
mvn spotbugs:spotbugs
```

### View SpotBugs Results

```bash
# Open the report
start bundles\org.openhab.core.addon\target\site\spotbugs.html
```

### Common SpotBugs Issues

- **Null pointer dereferences**
- **Resource leaks** (unclosed streams)
- **Thread safety** (synchronization issues)
- **Performance** (inefficient operations)

## Spotless - Code Formatting

### What It Does

- Auto-formats code
- Fixes formatting issues
- Ensures consistent style

### Run Spotless

```bash
# Check formatting (doesn't modify files)
mvn spotless:check

# Auto-fix formatting issues
mvn spotless:apply
```

### Common Spotless Fixes

- **Import order**
- **Code formatting**
- **Trailing whitespace**
- **End of file newline**

## Complete Workflow

### 1. Check Your Code

```bash
# Run all checks
mvn verify
```

### 2. Fix Formatting Issues

```bash
# Auto-fix formatting
mvn spotless:apply
```

### 3. Fix Remaining Issues

- Check the HTML reports
- Fix issues manually
- Re-run analysis

### 4. Verify Everything Passes

```bash
# Final check
mvn verify
```

## Example: Analyzing AddonParameter.java

### Step 1: Run Analysis

```bash
cd bundles/org.openhab.core.addon
mvn verify
```

### Step 2: Check Results

```bash
# View Checkstyle
start target\site\checkstyle.html

# View PMD
start target\site\pmd.html

# View SpotBugs
start target\site\spotbugs.html
```

### Step 3: Fix Issues

1. **Formatting issues**: Run `mvn spotless:apply`
2. **Code issues**: Fix manually based on reports
3. **Re-run**: `mvn verify` to confirm fixes

## Integration with Cursor

### Using Cursor's Built-in Features

1. **Problems Panel** (`Ctrl+Shift+M`):
   - Shows compiler errors
   - Shows warnings
   - Shows linter issues

2. **Terminal Integration**:
   - Run Maven commands in integrated terminal
   - View output directly in Cursor

3. **File Explorer**:
   - Right-click → "Open in Browser" for HTML reports

### Ask Me to Check Code

You can ask me to check linter errors:
- "Check linter errors in AddonParameter.java"
- "What are the code quality issues in this file?"

I'll use the `read_lints` tool to analyze your code!

## Configuration Files

The analysis tools are configured in:

```
tools/static-code-analysis/
├── checkstyle/
│   ├── ruleset.properties
│   └── suppressions.xml
├── pmd/
│   └── suppressions.properties
└── spotbugs/
    └── suppressions.xml
```

## Suppressing False Positives

If you need to suppress a false positive:

### Checkstyle

Add to `tools/static-code-analysis/checkstyle/suppressions.xml`:

```xml
<suppress checks=".*" files="AddonParameter.java"/>
```

### PMD

Add to `tools/static-code-analysis/pmd/suppressions.properties`:

```
AddonParameter.java    # Suppress all rules for this file
```

### SpotBugs

Add to `tools/static-code-analysis/spotbugs/suppressions.xml`:

```xml
<Match>
  <Class name="org.openhab.core.addon.AddonParameter"/>
  <Bug pattern="NP_NULL_ON_SOME_PATH"/>
</Match>
```

## Best Practices

1. **Run Before Committing**:
   ```bash
   mvn verify
   ```

2. **Fix Formatting First**:
   ```bash
   mvn spotless:apply
   ```

3. **Address Critical Issues**:
   - Fix bugs (SpotBugs)
   - Fix code smells (PMD)
   - Fix style issues (Checkstyle)

4. **Review Reports**:
   - Check HTML reports for details
   - Understand why issues are flagged

5. **Don't Suppress Without Reason**:
   - Only suppress false positives
   - Document why suppression is needed

## Quick Reference Commands

| Task | Command |
|------|---------|
| Run all checks | `mvn verify` |
| Fix formatting | `mvn spotless:apply` |
| Check formatting | `mvn spotless:check` |
| Run Checkstyle | `mvn checkstyle:check` |
| Run PMD | `mvn pmd:check` |
| Run SpotBugs | `mvn spotbugs:check` |
| Generate reports | `mvn site` |

## Troubleshooting

### Build Fails Due to Checkstyle

```bash
# Fix formatting first
mvn spotless:apply

# Then re-run
mvn verify
```

### Too Many Issues

1. Focus on **critical** issues first
2. Fix **one bundle at a time**
3. Use suppressions for **false positives only**

### Reports Not Generated

```bash
# Generate site reports
mvn site

# Reports will be in target/site/
```

## Summary

✅ **Use Maven tools** (already configured):
- `mvn verify` - Run all checks
- `mvn spotless:apply` - Fix formatting
- View HTML reports in `target/site/`

✅ **Use Cursor's built-in linter**:
- Problems panel (`Ctrl+Shift+M`)
- Automatic error highlighting

✅ **Ask me to check code**:
- I can analyze files using `read_lints`
- I can help fix issues

These tools are **better than SonarLint** for this project because they're:
- Already configured
- Integrated with the build
- Follow openHAB coding standards
- Generate detailed reports

