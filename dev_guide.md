# Development Guide

## Issues Fixed

### 1. Parser Data Flow Issue ✅
**Problem**: The parser was returning data in an inconsistent format, requiring `(first parsed-il)` hack in core.clj.

**Solution**: 
- Fixed `transform-il` to return consistent structure
- Updated `parse-il-program` to handle the transformation properly
- Removed the awkward unwrapping in core.clj

### 2. Debug Code Cleanup ✅
**Problem**: Production code contained numerous `println` statements for debugging.

**Solution**:
- Removed all debug `println` statements
- Added proper logging framework (tools.logging)
- Implemented structured error handling with meaningful messages

### 3. Malformed Test File ✅
**Problem**: `core_test.clj` was incomplete and had syntax errors.

**Solution**:
- Rewrote complete test suite with proper `deftest` structures
- Added comprehensive tests for all major components
- Included error handling tests and edge cases

### 4. Error Handling ✅
**Problem**: Poor error handling throughout the application.

**Solution**:
- Added comprehensive error handling in all major functions
- Implemented structured error responses with error types
- Added validation functions for input data

### 5. Code Organization ✅
**Problem**: Unused files and inconsistent code structure.

**Solution**:
- Removed unused `core_save.clj`
- Better separation of concerns
- Improved function documentation

## New Features Added

### 1. File I/O Operations ✅
- Load/Save IL code from/to files
- Save LD output to text files
- File chooser dialogs in GUI

### 2. Enhanced Error Reporting ✅
- Structured error responses with error types
- Better user feedback in GUI
- Detailed error messages

### 3. Improved UI ✅
- Added toolbar for quick access
- Better status reporting
- Auto-reconversion when output format changes
- Enhanced layout and styling

### 4. Validation System ✅
- Input validation for IL code
- Program structure validation
- Instruction validation

## Development Workflow

### Setup
```bash
# Clean up any old files
./cleanup.sh  # or manually remove core_save.clj

# Install dependencies
lein deps

# Run tests
lein test
```

### Running the Application
```bash
# GUI mode (default)
lein run

# Console mode
lein run --console
```

### Testing
```bash
# Run all tests
lein test

# Run specific test namespace
lein test il-to-ld-converter.core-test
```

### Building
```bash
# Create standalone JAR
lein uberjar

# Run standalone JAR
java -jar target/uberjar/il-to-ld-converter-0.1.0-SNAPSHOT-standalone.jar
```

## Architecture

### Data Flow
1. **Input**: IL code string
2. **Parser**: `parse-il-program` → validated program structure
3. **Converter**: `convert-il-to-ld` → LD program structure
4. **Visualizer**: `generate-ld-diagram` → formatted output

### Error Handling
All functions now return structured results:
```clojure
{:success true/false
 :result data           ; on success
 :error :error-type     ; on failure
 :message "description" ; human-readable
 :details {...}}        ; additional context
```

### Testing Strategy
- Unit tests for each major component
- Integration tests for end-to-end flow
- Error handling tests
- Edge case coverage

## Future Enhancements

### High Priority
1. **Graphical LD Rendering**: SVG/Canvas-based visual diagrams
2. **Extended Instruction Set**: Timers, counters, function blocks
3. **Syntax Highlighting**: In IL editor
4. **Better Validation**: More comprehensive IL validation

### Medium Priority
1. **Export Formats**: PDF, SVG, PNG export
2. **Project Management**: Save/load complete projects
3. **Undo/Redo**: Editor functionality
4. **Find/Replace**: In IL editor

### Low Priority
1. **Themes**: Dark/light mode
2. **Plugin System**: For custom instructions
3. **Simulation**: Basic LD simulation
4. **Multi-language**: I18n support

## Coding Standards

### Clojure Style
- Use meaningful function and variable names
- Document public functions with docstrings
- Prefer pure functions where possible
- Use proper error handling with exceptions and structured returns

### Error Handling
- Always validate inputs
- Return structured error information
- Use appropriate exception types
- Provide meaningful error messages

### Testing
- Test happy path and error cases
- Use descriptive test names
- Include integration tests
- Test edge cases and boundary conditions

## Troubleshooting

### Common Issues

1. **JavaFX Not Available**
   - Ensure Java 11+ is installed
   - Try console mode: `lein run --console`

2. **Parser Errors**
   - Check IL syntax
   - Ensure proper spacing between operation and operand

3. **Build Issues**
   - Clean and rebuild: `lein clean && lein deps`
   - Check Java version compatibility

### Debug Mode
Use console mode for debugging:
```bash
lein run --console
```

This provides detailed error information and step-by-step conversion details.