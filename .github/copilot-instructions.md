# GitHub Copilot Instructions for openHAB Core

## Project Overview

openHAB Core is the foundation of the openHAB smart home automation platform. This repository contains the core framework components that provide the runtime environment for openHAB. It is not a standalone product but a framework used by the main [openHAB distribution](https://github.com/openhab/openhab-distro).

The project consists of approximately 100 OSGi bundles that implement various aspects of the smart home automation framework, including:
- Thing/Item model and registry
- Automation engine and rule execution
- Communication protocols and discovery
- Persistence and storage
- UI and REST APIs
- Internationalization and configuration

## Technology Stack

- **Language:** Java (JDK 21 required)
- **Build Tool:** Maven 3.x
- **Architecture:** OSGi (Open Services Gateway Initiative) bundles
- **Framework:** Eclipse SmartHome components
- **Code Formatting:** Spotless Maven Plugin with Eclipse formatter
- **Testing:** JUnit 5
- **License:** Eclipse Public License 2.0 (EPL-2.0)

## Project Structure

```
openhab-core/
├── bundles/          # Core OSGi bundles (100+ modules)
├── bom/              # Bill of materials for dependency management
├── features/         # Apache Karaf feature definitions
├── itests/           # Integration tests
├── tools/            # Development tools
│   ├── archetype/    # Maven archetypes for creating new bindings
│   ├── i18n-plugin/  # Internationalization tools
│   └── static-code-analysis/  # Code quality tools
└── pom.xml           # Root Maven POM
```

## Building and Testing

### Quick Build Commands

```bash
# Full build with formatting and tests
mvn clean spotless:apply install

# Skip tests for faster build
mvn -DskipTests=true clean install

# Ultra-fast build (skip all checks)
mvn clean install -T1C -DskipChecks -DskipTests -Dspotless.check.skip=true
```

### Code Formatting

This project uses Spotless with Eclipse Java formatter:
```bash
# Apply code formatting
mvn spotless:apply

# Check formatting without modifying files
mvn spotless:check
```

**Important:** Always run `mvn spotless:apply` before committing to ensure code adheres to the project's style guidelines.

## Coding Guidelines

### General Principles

1. **OSGi Best Practices:**
   - Use OSGi Declarative Services (DS) annotations (`@Component`, `@Reference`, etc.)
   - Follow OSGi bundle lifecycle patterns
   - Properly handle service dependencies and dynamics

2. **Code Style:**
   - Follow Eclipse Java formatter rules (applied via Spotless)
   - Use meaningful variable and method names
   - Add Javadoc for public APIs
   - Keep methods focused and concise

3. **Null Safety:**
   - Use `@Nullable` and `@NonNull` annotations from `org.eclipse.jdt.annotation`
   - Handle null cases explicitly
   - Prefer `Optional<T>` for return values that may be absent

4. **Logging:**
   - Use SLF4J for logging (`org.slf4j.Logger`)
   - Use appropriate log levels (trace, debug, info, warn, error)
   - Include contextual information in log messages

5. **Testing:**
   - Write unit tests for new functionality
   - Use JUnit 5 and Mockito for mocking
   - Place tests in `src/test/java` with same package structure
   - Integration tests go in the `itests/` directory

### Common Patterns

#### OSGi Component Declaration
```java
@Component(service = MyService.class)
@NonNullByDefault
public class MyServiceImpl implements MyService {
    private final Logger logger = LoggerFactory.getLogger(MyServiceImpl.class);
    
    @Reference
    protected void setOtherService(OtherService service) {
        // Handle service reference
    }
}
```

#### Configuration Properties
```java
@Component(configurationPid = "my.service", service = MyService.class)
public class MyServiceImpl implements MyService {
    private @Nullable MyConfig config;
    
    @Activate
    protected void activate(Map<String, Object> properties) {
        config = new Configuration(properties).as(MyConfig.class);
    }
    
    @Modified
    protected void modified(Map<String, Object> properties) {
        activate(properties);
    }
}
```

## Contribution Workflow

1. **Branch Naming:**
   - Bugfix: `<issue-number>-short-description`
   - Feature: `<issue-number>-short-description`

2. **Commit Messages:**
   - Start with capitalized, short summary (max 50 chars)
   - Use imperative mood ("Add feature" not "Added feature")
   - Reference issues: "Fixes #123" or "Closes #456"
   - Sign-off required: `Signed-off-by: Name <email>`

3. **Pull Requests:**
   - Create against `main` branch
   - Include clear description of changes
   - Update documentation if needed
   - Ensure all tests pass
   - Run `mvn spotless:apply` before committing
   - Squash commits into logical units before merge

4. **Do NOT use `git merge`:**
   - Always use `git rebase` to avoid cluttering history
   - See: https://community.openhab.org/t/rebase-your-code-or-how-to-fix-your-git-history-before-requesting-a-pull/129358

## Common Tasks

### Adding a New Bundle

1. Create directory under `bundles/`
2. Add `pom.xml` with parent reference
3. Create OSGi bundle manifest via Maven Bundle Plugin
4. Register bundle in parent `pom.xml`

### Creating a New Binding

Use the Maven archetype:
```bash
mvn archetype:generate -DarchetypeGroupId=org.openhab.core.tools.archetypes \
  -DarchetypeArtifactId=org.openhab.core.tools.archetypes.binding
```

### Running Static Code Analysis

```bash
cd tools/static-code-analysis
mvn clean install
```

## Important Files

- `openhab_codestyle.xml` - Eclipse Java formatter configuration
- `CONTRIBUTING.md` - Detailed contribution guidelines
- `CODEOWNERS` - Code ownership and review assignments
- `.github/workflows/ci-build.yml` - GitHub Actions CI configuration

## Documentation

- Main docs: https://www.openhab.org/docs/developer/
- Developer guidelines: https://www.openhab.org/docs/developer/guidelines.html
- Community forum: https://community.openhab.org/

## Additional Notes

- This is a framework project, not an end-user application
- Changes should maintain backward compatibility when possible
- Breaking changes require release notes update in openhab-distro
- Focus on stability and extensibility
- Consider impact on the broader openHAB ecosystem
