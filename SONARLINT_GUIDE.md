# Code Quality Analysis Guide for openHAB Core

## ⚠️ Note: SonarLint Not Available in Cursor

Since SonarLint extension is not available in Cursor's marketplace, this guide provides **alternative solutions** that are already configured in the openHAB project.

## Available Static Analysis Tools

The openHAB project already has **three powerful static analysis tools** configured via Maven:

1. **Checkstyle** - Code style and formatting checks
2. **PMD** - Bug detection and code quality
3. **SpotBugs** - Find bugs in Java code

These tools are **better integrated** with the project and run automatically during builds!

## Installation in Cursor/VS Code

### Step 1: Install the Extension

1. **Open Extensions Panel**:
   - Press `Ctrl+Shift+X` (Windows/Linux) or `Cmd+Shift+X` (Mac)
   - Or click the Extensions icon in the sidebar

2. **Search for SonarLint**:
   - Type "SonarLint" in the search box
   - Look for the official "SonarLint" extension by SonarSource

3. **Install**:
   - Click "Install" button
   - Wait for installation to complete
   - You may need to reload Cursor/VS Code

### Step 2: Verify Installation

1. **Check Status Bar**:
   - Look at the bottom status bar
   - You should see "SonarLint" icon/status

2. **Open Command Palette**:
   - Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
   - Type "SonarLint" to see available commands

## How to Use SonarLint

### Automatic Analysis

Once installed, SonarLint **automatically analyzes** your code as you type:

1. **Open any Java file** (e.g., `AddonParameter.java`)
2. **Issues appear automatically**:
   - Red squiggles = Bugs (critical issues)
   - Orange squiggles = Code Smells (maintainability issues)
   - Yellow squiggles = Vulnerabilities (security issues)

3. **Hover over squiggles** to see:
   - Issue description
   - Rule name
   - Suggested fix (if available)

### Viewing All Issues

1. **Problems Panel**:
   - Press `Ctrl+Shift+M` (Windows/Linux) or `Cmd+Shift+M` (Mac)
   - Or click "Problems" in the bottom panel
   - Filter by "SonarLint" to see only SonarLint issues

2. **SonarLint Output**:
   - Go to View → Output
   - Select "SonarLint" from the dropdown
   - See detailed analysis logs

### Running Analysis Manually

1. **Command Palette**:
   - Press `Ctrl+Shift+P` / `Cmd+Shift+P`
   - Type: `SonarLint: Analyze Current File`
   - Or: `SonarLint: Analyze All Files in Workspace`

2. **Right-Click Context Menu**:
   - Right-click on a file in Explorer
   - Select "SonarLint: Analyze File"

## Configuration

### Basic Configuration

SonarLint works out of the box, but you can configure it:

1. **Open Settings**:
   - Press `Ctrl+,` (Windows/Linux) or `Cmd+,` (Mac)
   - Search for "SonarLint"

2. **Key Settings**:
   - **sonarlint.rules**: Customize which rules to use
   - **sonarlint.pathToNodeExecutable**: For JavaScript analysis
   - **sonarlint.analyzerProperties**: Custom analyzer properties

### Project-Specific Configuration

Create `.vscode/settings.json` in your project root:

```json
{
  "sonarlint.rules": {
    "java:S1067": "off",  // Disable specific rule
    "java:S1172": "warning"  // Change severity
  },
  "sonarlint.pathToCompileCommands": "${workspaceFolder}/target",
  "sonarlint.analyzerProperties": {
    "sonar.java.source": "21",
    "sonar.java.target": "21"
  }
}
```

### SonarLint Configuration File

Create `.sonarlint/sonarlint.json` in your project root:

```json
{
  "sonarLintVersion": "10.0.0",
  "rules": {
    "java:S1067": {
      "level": "off"
    },
    "java:S1172": {
      "level": "warning"
    }
  },
  "analyzerProperties": {
    "sonar.java.source": "21",
    "sonar.java.target": "21"
  }
}
```

## Common SonarLint Rules for Java

### Code Quality Issues

1. **S1067**: Expressions should not be too complex
2. **S1172**: Unused method parameters should be removed
3. **S1142**: Methods should not have too many lines
4. **S138**: Methods should not have too many lines
5. **S3776**: Cognitive Complexity of functions should not be too high

### Bug Detection

1. **S2259**: Null pointers should not be dereferenced
2. **S2583**: Conditions should not always evaluate to "true" or "false"
3. **S1854**: Dead stores should be removed
4. **S1481**: Unused local variables should be removed

### Security Issues

1. **S2083**: Path traversal vulnerabilities
2. **S5131**: SQL injection vulnerabilities
3. **S2068**: Hard-coded credentials

### Best Practices

1. **S1118**: Utility classes should not have public constructors
2. **S106**: Standard outputs should not be used directly
3. **S1186**: Methods should not be empty

## Interpreting SonarLint Results

### Issue Severity

- **Blocker**: Critical bugs that must be fixed immediately
- **Critical**: Bugs that should be fixed as soon as possible
- **Major**: Code smells that significantly impact maintainability
- **Minor**: Code smells that slightly impact maintainability
- **Info**: Informational issues

### Issue Types

- **Bug**: Code that is demonstrably wrong
- **Vulnerability**: Security-related issues
- **Code Smell**: Maintainability issues

## Example: Analyzing AddonParameter.java

1. **Open the file**: `bundles/org.openhab.core.addon/src/main/java/org/openhab/core/addon/AddonParameter.java`

2. **SonarLint will automatically check**:
   - Null safety issues
   - Code duplication
   - Complexity issues
   - Best practices violations

3. **Common issues you might see**:
   - `java:S1067`: Complex boolean expressions
   - `java:S1172`: Unused parameters
   - `java:S1118`: Utility class constructor
   - `java:S1186`: Empty methods

## Integration with SonarQube/SonarCloud (Optional)

If your project uses SonarQube or SonarCloud:

1. **Connect SonarLint to SonarQube**:
   - Open Command Palette
   - Type: `SonarLint: Update All Project Bindings to SonarQube`
   - Enter SonarQube server URL and token

2. **Benefits**:
   - Sync rules from SonarQube
   - See issues that match server-side analysis
   - Consistent rules across team

## Troubleshooting

### SonarLint Not Working

1. **Check Extension Status**:
   - View → Output → Select "SonarLint"
   - Look for error messages

2. **Reload Window**:
   - Command Palette → "Developer: Reload Window"

3. **Check Java Language Support**:
   - Ensure Java extension is installed
   - SonarLint requires Java language support

### Issues Not Showing

1. **Check File Type**:
   - SonarLint analyzes Java files automatically
   - Make sure file has `.java` extension

2. **Check Settings**:
   - Verify SonarLint is enabled
   - Check if rules are disabled

3. **Manual Analysis**:
   - Try running manual analysis
   - Command Palette → "SonarLint: Analyze Current File"

### Performance Issues

1. **Exclude Large Files**:
   - Add to settings: `"sonarlint.excludedPatterns": ["**/generated/**"]`

2. **Limit Analysis Scope**:
   - Analyze only open files
   - Disable workspace-wide analysis

## Best Practices

1. **Fix Issues Early**: Address issues as you code, not later
2. **Understand Rules**: Don't blindly disable rules - understand why they exist
3. **Customize Rules**: Adjust rules to match your team's coding standards
4. **Regular Updates**: Keep SonarLint extension updated
5. **Review False Positives**: Some issues may be false positives - review carefully

## Quick Reference Commands

| Command | Shortcut | Description |
|---------|----------|-------------|
| Analyze Current File | - | Analyze active editor file |
| Analyze All Files | - | Analyze entire workspace |
| Show Rule Description | - | View detailed rule information |
| Deactivate Rule | - | Disable a rule for current file |

## Resources

- **SonarLint Documentation**: https://www.sonarlint.org/
- **Java Rules**: https://rules.sonarsource.com/java
- **VS Code Extension**: https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarlint-vscode

## Example Workflow

1. **Write Code**: Start coding in `AddonParameter.java`
2. **See Issues**: SonarLint highlights issues in real-time
3. **Hover**: Hover over squiggles to see issue details
4. **Fix**: Apply suggested fixes or fix manually
5. **Verify**: Issues disappear when fixed
6. **Review**: Check Problems panel for remaining issues

## Next Steps

1. Install SonarLint extension
2. Open `AddonParameter.java` to see it in action
3. Review any issues found
4. Fix critical and major issues
5. Configure rules to match your preferences

