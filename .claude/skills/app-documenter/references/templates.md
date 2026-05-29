# README Templates by Language

Templates for generating module-level README.md files. Select the appropriate
template based on detected language.

## Table of Contents

- [Go Module](#go-module)
- [TypeScript/React Module](#typescriptreact-module)
- [Python Module](#python-module)
- [Rust Module](#rust-module)
- [Java Module](#java-module)
- [Generic Module](#generic-module)

---

## Go Module

```markdown
# {Package Name}

{Brief description of the package purpose}

## Import

\`\`\`go import "{module_path}/{package}" \`\`\`

## Overview

| Export | Type                  | Description       |
| ------ | --------------------- | ----------------- |
| `Name` | func/struct/interface | Brief description |

---

## {Export Name}

{One-line description}

### Signature

\`\`\`go func FunctionName(param1 Type1, param2 Type2) (ReturnType, error)
\`\`\`

### Parameters

| Parameter | Type    | Description  |
| --------- | ------- | ------------ |
| `param1`  | `Type1` | What it does |

### Returns

| Field  | Type         | Description     |
| ------ | ------------ | --------------- |
| result | `ReturnType` | What it returns |

### Example

\`\`\`go result, err := package.FunctionName(arg1, arg2) if err != nil {
log.Fatal(err) } \`\`\`

---

{Repeat for each exported function/struct/interface}

---

## Deprecated / To Be Removed

{Include this section ONLY if deprecated code exists}

| Function/Struct | Reason                     | Alternative           |
| --------------- | -------------------------- | --------------------- |
| `OldFunction`   | Deprecated: outdated logic | Use `NewFunction`     |
| `LegacyStruct`  | TODO: remove in v2.0       | Use `ModernStruct`    |
| `BackCompat`    | FIXME: backward compat     | Migrate to new API v2 |
```

---

## TypeScript/React Module

```markdown
# {Folder Name}

{Brief description of the module}

\`\`\`typescript import { Export1, Export2 } from '@/{category}/{folder}';
import type { Type1, Type2 } from '@/{category}/{folder}'; \`\`\`

## Exports

| Export    | Type                    | Description       |
| --------- | ----------------------- | ----------------- |
| `Export1` | component/hook/function | Brief description |

---

## {Export Name}

{One-line description}

### Props / Parameters

| Prop       | Type     | Default | Description  |
| ---------- | -------- | ------- | ------------ |
| `propName` | `string` | -       | What it does |

### Returns

\`\`\`typescript interface ReturnType { data: DataType; loading: boolean; error:
Error | null; } \`\`\`

### Example

\`\`\`tsx const { data, loading } = useHook({ param: 'value' }); \`\`\`

---

{Repeat for each export}

---

## Deprecated / To Be Removed

{Include this section ONLY if deprecated code exists}

| Export       | Reason                  | Alternative        |
| ------------ | ----------------------- | ------------------ |
| `oldHook`    | Deprecated: use new API | Use `useNewHook`   |
| `LegacyComp` | TODO: remove after v3   | Use `ModernComp`   |
| `backCompat` | FIXME: backward compat  | Migrate to new API |
```

---

## Python Module

```markdown
# {Module Name}

{Brief description of the module}

\`\`\`python from {package}.{module} import Class1, function1 \`\`\`

## Overview

| Export      | Type     | Description       |
| ----------- | -------- | ----------------- |
| `Class1`    | class    | Brief description |
| `function1` | function | Brief description |

---

## {Export Name}

{One-line description}

### Signature

\`\`\`python def function_name(param1: str, param2: int = 0) -> ReturnType:
\`\`\`

### Parameters

| Parameter | Type  | Default  | Description    |
| --------- | ----- | -------- | -------------- |
| `param1`  | `str` | required | What it does   |
| `param2`  | `int` | `0`      | Optional param |

### Returns

| Type         | Description     |
| ------------ | --------------- |
| `ReturnType` | What it returns |

### Example

\`\`\`python result = function_name("input", param2=42) \`\`\`

---

{Repeat for each export}

---

## Deprecated / To Be Removed

{Include this section ONLY if deprecated code exists}

| Function/Class | Reason                  | Alternative     |
| -------------- | ----------------------- | --------------- |
| `old_function` | Deprecated: legacy code | Use `new_func`  |
| `OldClass`     | TODO: remove in v2.0    | Use `ModernCls` |
| `back_compat`  | FIXME: backward compat  | Migrate to new  |
```

---

## Rust Module

```markdown
# {Module Name}

{Brief description of the module}

\`\`\`rust use crate::{module}::{Item1, Item2}; \`\`\`

## Overview

| Item    | Type            | Description       |
| ------- | --------------- | ----------------- |
| `Item1` | struct/trait/fn | Brief description |

---

## {Item Name}

{One-line description}

### Definition

\`\`\`rust pub struct StructName { pub field1: Type1, pub field2: Option<Type2>,
} \`\`\`

### Methods

| Method   | Signature                           | Description  |
| -------- | ----------------------------------- | ------------ |
| `new`    | `() -> Self`                        | Constructor  |
| `method` | `(&self, param: T) -> Result<R, E>` | What it does |

### Example

\`\`\`rust let instance = StructName::new(); let result =
instance.method(value)?; \`\`\`

---

{Repeat for each public item}

---

## Deprecated / To Be Removed

{Include this section ONLY if deprecated code exists}

| Item          | Reason                  | Alternative        |
| ------------- | ----------------------- | ------------------ |
| `old_fn`      | Deprecated: legacy impl | Use `new_fn`       |
| `LegacyType`  | TODO: remove in v2.0    | Use `ModernType`   |
| `back_compat` | FIXME: backward compat  | Migrate to new API |
```

---

## Java Module

```markdown
# {Package/Class Name}

{Brief description}

\`\`\`java import com.example.{package}.{Class}; \`\`\`

## Overview

| Class    | Description       |
| -------- | ----------------- |
| `Class1` | Brief description |

---

## {Class Name}

{One-line description}

### Constructor

\`\`\`java public ClassName(Type1 param1, Type2 param2) \`\`\`

### Methods

| Method       | Parameters    | Returns      | Description  |
| ------------ | ------------- | ------------ | ------------ |
| `methodName` | `(String id)` | `ReturnType` | What it does |

### Example

\`\`\`java ClassName instance = new ClassName(arg1, arg2); ReturnType result =
instance.methodName("id-123"); \`\`\`

---

{Repeat for each public class/method}

---

## Deprecated / To Be Removed

{Include this section ONLY if deprecated code exists}

| Method/Class  | Reason                   | Alternative        |
| ------------- | ------------------------ | ------------------ |
| `oldMethod`   | @Deprecated: use new API | Use `newMethod`    |
| `LegacyClass` | TODO: remove in v2.0     | Use `ModernClass`  |
| `backCompat`  | FIXME: backward compat   | Migrate to new API |
```

---

## Generic Module

Use for any language not listed above. Adapt the structure:

```markdown
# {Module Name}

{Brief description}

## Overview

| Export | Type | Description       |
| ------ | ---- | ----------------- |
| `name` | type | Brief description |

---

## {Export Name}

{One-line description}

### Usage

\`\`\` code example \`\`\`

---

{Repeat for each export}

---

## Deprecated / To Be Removed

{Include this section ONLY if deprecated code exists}

| Export    | Reason                 | Alternative    |
| --------- | ---------------------- | -------------- |
| `oldFunc` | Deprecated: legacy     | Use `newFunc`  |
| `Legacy`  | TODO: remove after v2  | Use `Modern`   |
| `compat`  | FIXME: backward compat | Migrate to new |
```

---

## Style Rules

1. **Be concise** - One-line descriptions, no fluff
2. **Show imports first** - Always start with import/use example
3. **Use tables** - For parameters, methods, exports
4. **Include examples** - Real, working code snippets
5. **Skip trivial** - Don't document getters/setters, constructors with no logic
6. **Group related** - Similar exports together
7. **Preserve custom content** - When updating, keep "Notes", "Gotchas",
   "Architecture" sections
