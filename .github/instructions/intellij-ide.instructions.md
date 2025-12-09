---
applyTo: '**'
description: 'Instruktionen für den Zugriff auf die IntelliJ IDE'
---

## IntelliJ IDE Integration

Use the IDE integrations below for coding tasks whenever deeper context or more accurate information is needed:

### Control:

Use tools from the `jetbrains-mcp` MCP Server to access general IntelliJ IDE functions (e.g., editor actions, project
navigation).

### Index:

Always use the `jetbrains-index` MCP server when applicable for:

- **Finding references** — Use `ide_find_references` instead of grep/search
- **Go to definition** — Use `ide_find_definition` for accurate navigation
- **Renaming symbols** — Use `ide_refactor_rename` for safe, project-wide renames
- **Type hierarchy** — Use `ide_type_hierarchy` to understand class relationships
- **Finding implementations** — Use `ide_find_implementations` for interfaces/abstract classes
- **Diagnostics** — Use `ide_diagnostics` to check for code problems

**Path Formatting:**
When a tool expects a path as a parameter, always use forward slashes (`/`) as separators, never backslashes (`\`).

The IDE's semantic understanding is far more accurate than text-based search. Prefer IDE tools over grep, ripgrep, or
manual file searching when working with code symbols.

### Debugging:

Use tools from the `jetbrains-debugger` MCP Server to access the IntelliJ IDE debugger (e.g., setting breakpoints,
inspecting variables, stepping).

---

Please ensure that these integrations are explicitly prioritized for requests involving these areas.

**Error Handling:**
If any of the mentioned tools or MCP servers are unavailable or cannot be accessed, please issue a clear warning to
inform the user immediately.