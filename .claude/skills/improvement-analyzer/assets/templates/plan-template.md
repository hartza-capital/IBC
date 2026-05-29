# Project Improvement Implementation Plan

**Generated:** {{.Date}}
**Project:** {{.ProjectName}}
**Languages Analyzed:** {{.Languages}}

---

## Summary

**Total Improvements:** {{.TotalFindings}}

- **High Priority:** {{.HighPriorityCount}}
- **Medium Priority:** {{.MediumPriorityCount}}
- **Low Priority:** {{.LowPriorityCount}}

**By Category:**

- Functional: {{.FunctionalCount}}
- Performance: {{.PerformanceCount}}
- Security: {{.SecurityCount}}
- Quality: {{.QualityCount}}
- Architecture: {{.ArchitectureCount}}
- Tests: {{.TestsCount}}

**By Module (top 5):**
{{range .TopModules}}

- {{.ModuleName}}: {{.Count}} improvements
  {{end}}

**Estimated Total Effort:** {{.TotalEffort}}

---

## How to Use This Plan

**Two organizational views for different workflows:**

### View 1: By Priority (below)

- **Best for:** Quick wins and critical fixes
- **When to use:** You want to tackle the most important items first, regardless of where they are in the codebase
- **Start here if:** You need to fix urgent issues or want measurable impact fast

### View 2: By Module (further down)

- **Best for:** Focused refactoring sessions
- **When to use:** You're working on a specific area of the codebase
- **Start here if:** You have a large project and want to improve one module at a time for better context

**Workflow recommendations:**

- **Option A (Quick Wins):** Work through High Priority from View 1
- **Option B (Focused):** Pick a module from View 2 and complete all its improvements
- **Option C (Hybrid):** High priority items first, then tackle modules one by one

---

# VIEW 1: By Priority

## High Priority Improvements

{{range .HighPriorityFindings}}

### Task {{.TaskNumber}}: {{.Title}}

**Category:** {{.Category}}
**Impact:** High
**Effort:** {{.Effort}}
**Language:** {{.Language}}

**Files Affected:**
{{range .Files}}

- `{{.}}`
  {{end}}

**Description:**

{{.Description}}

**Recommendation:**

{{.Recommendation}}

**Implementation Steps:**

{{range .Steps}}
{{.Number}}. {{.Description}}
{{end}}

{{if .Dependencies}}
**Dependencies:**
This task depends on: {{.Dependencies}}
{{end}}

---

{{end}}

## Medium Priority Improvements

{{range .MediumPriorityFindings}}

### Task {{.TaskNumber}}: {{.Title}}

**Category:** {{.Category}}
**Impact:** Medium
**Effort:** {{.Effort}}
**Language:** {{.Language}}

**Files Affected:**
{{range .Files}}

- `{{.}}`
  {{end}}

**Description:**

{{.Description}}

**Recommendation:**

{{.Recommendation}}

**Implementation Steps:**

{{range .Steps}}
{{.Number}}. {{.Description}}
{{end}}

{{if .Dependencies}}
**Dependencies:**
This task depends on: {{.Dependencies}}
{{end}}

---

{{end}}

## Low Priority Improvements

{{range .LowPriorityFindings}}

### Task {{.TaskNumber}}: {{.Title}}

**Category:** {{.Category}}
**Impact:** Low
**Effort:** {{.Effort}}
**Language:** {{.Language}}

**Files Affected:**
{{range .Files}}

- `{{.}}`
  {{end}}

**Description:**

{{.Description}}

**Recommendation:**

{{.Recommendation}}

**Implementation Steps:**

{{range .Steps}}
{{.Number}}. {{.Description}}
{{end}}

{{if .Dependencies}}
**Dependencies:**
This task depends on: {{.Dependencies}}
{{end}}

---

{{end}}

---

# VIEW 2: By Module

This view organizes the same improvements by module/area. Useful for large projects where you want to refactor one component at a time for better context efficiency.

{{range .ModuleGroups}}

## Module: {{.ModuleName}} ({{.TotalCount}} improvements)

{{if .ModuleDescription}}
**Overview:** {{.ModuleDescription}}
{{end}}

**Breakdown:**

- High Priority: {{.HighCount}}
- Medium Priority: {{.MediumCount}}
- Low Priority: {{.LowCount}}

### High Priority in this module

{{range .HighPriorityTasks}}

- **Task {{.TaskNumber}}**: {{.Title}} (Effort: {{.Effort}})
  - Category: {{.Category}}
  - Files: {{.FilesCount}} affected
    {{end}}

### Medium Priority in this module

{{range .MediumPriorityTasks}}

- **Task {{.TaskNumber}}**: {{.Title}} (Effort: {{.Effort}})
  - Category: {{.Category}}
  - Files: {{.FilesCount}} affected
    {{end}}

### Low Priority in this module

{{range .LowPriorityTasks}}

- **Task {{.TaskNumber}}**: {{.Title}} (Effort: {{.Effort}})
  - Category: {{.Category}}
  - Files: {{.FilesCount}} affected
    {{end}}

**To implement all improvements for this module:**

1. Work through High Priority tasks first
2. Test after each change
3. Then tackle Medium and Low Priority
4. Create a PR for this module when complete

---

{{end}}

{{if .FailedAgents}}

## Analysis Notes

⚠️ **Some analyses did not complete:**

{{range .FailedAgents}}

- **{{.Language}}** ({{.Agent}}): {{.Reason}}
  {{end}}

You may want to manually review {{.FailedLanguages}} code or re-run the analysis.

{{end}}

---

## Next Steps

### To implement an improvement:

1. **Pick a task** from the list above (recommend starting with High Priority)
2. **Copy the implementation steps** for that task
3. **Paste into Claude** with the message: "Please implement Task {{.TaskNumber}}"
4. **Review the changes** that Claude makes
5. **Test the changes** to ensure they work correctly
6. **Commit** with a message like: "feat: implement improvement - {{.Title}}"

### To implement all improvements:

1. Work through tasks in order (High → Medium → Low)
2. Test after each implementation
3. Create a separate commit for each task
4. Create a PR when a logical group of improvements is complete

### To skip improvements:

Some improvements may not fit your project goals. It's perfectly fine to skip tasks that:

- Don't align with current priorities
- Would require architectural changes you're not ready for
- Have acceptable trade-offs in the current implementation

---

**🚀 Ready to start? Pick any task and say:**

> "Implement Task N"

**Questions?** Ask Claude for clarification on any task.
