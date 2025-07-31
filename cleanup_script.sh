#!/bin/bash

# IL to LD Converter - Cleanup Script
# This script removes unused files and cleans up the project

echo "Cleaning up IL to LD Converter project..."

# Remove unused backup file
if [ -f "src/il_to_ld_converter/core_save.clj" ]; then
    echo "Removing unused backup file: core_save.clj"
    rm "src/il_to_ld_converter/core_save.clj"
fi

# Clean build artifacts
echo "Cleaning build artifacts..."
rm -rf target/
rm -rf .lein-*
rm -rf .nrepl-port
rm -rf .prepl-port

# Clean IDE files
echo "Cleaning IDE files..."
rm -rf .calva/
rm -rf .clj-kondo/
rm -rf .lsp/

echo "Cleanup complete!"
echo ""
echo "Next steps:"
echo "1. Run 'lein deps' to download dependencies"
echo "2. Run 'lein test' to run tests"
echo "3. Run 'lein run' to start the GUI"
echo "4. Run 'lein run --console' to run in console mode"