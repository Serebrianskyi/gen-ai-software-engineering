# Generate README Skill

A custom Claude Code skill that generates comprehensive README.md files for homework assignments based on HW1 and HW2 templates.

## Usage

### Basic Usage

Invoke the skill from within Claude Code:

```
/generate-readme <homework-number> "<title>" "<description>" "<technology>"
```

### Examples

**Generate a README for Homework 3 with student name:**
```
/generate-readme 3 "Banking Transactions API" "John Smith" "A REST API for managing banking transactions" "Kotlin + Spring Boot 3.2.3"
```

**Generate a student template README (HW2) with student name:**
```
/generate-readme 2 "Homework 2 Assignment" "Jane Doe" "Your project description" "Kotlin + Spring Boot"
```

**With custom output path:**
```
/generate-readme 1 "API Project" "Alex Johnson" "Description" "Technology" "custom-path/README.md"
```

## Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `homework-number` | Yes | - | Homework number (1, 2, 3, etc.) |
| `title` | Yes | - | Project title |
| `student-name` | No | [Your Name] | Student name (auto-populated in README) |
| `description` | No | "A comprehensive project implementation." | Brief project description |
| `technology` | No | "Kotlin + Spring Boot" | Technology stack |
| `path` | No | `homework-{number}/README.md` | Output file path |

## Output

The skill generates two types of READMEs:

### Full Template (HW1-style)
For homework assignments 1 and 3+, generates a comprehensive README with:
- Project overview with status
- Startup guide with prerequisites
- Running instructions (Gradle and JAR options)
- Project structure
- Dependency architecture diagram
- API endpoints documentation
- Example requests
- Data models
- Implementation notes
- Design decisions

### Student Template (HW2-style)
For homework 2, generates a student submission template with:
- Student info section
- Project overview placeholder
- Key features checklist
- Getting started guide
- API endpoints table
- Validation rules
- Data models section
- Implementation notes

## Features

✅ **Template-based** - Consistent structure matching HW1 and HW2 examples
✅ **Customizable** - Fill in your project details
✅ **Auto-placeholder** - Includes sections ready for your content
✅ **Professional formatting** - Markdown with proper structure
✅ **Multi-homework support** - Works for HW1, HW2, HW3, etc.

## Notes

- The generated README includes placeholder text marked with `[Your...]` that you should customize
- API endpoint details, data models, and implementation notes should be filled in with your specific project details
- The script automatically creates necessary directories if they don't exist