# UI strings (extracted from Flutter)

Source: the `_localizedValues` map in `lib/core/localization/app_localizations.dart:18-3608`. Runtime semantics are described in `../01-architecture-and-core.md` §10.

## Format

- One file per locale: `en.json`, `de.json`, `uz.json`, `ru.json`.
- Each is a flat `{ "key": "value" }` object, keys sorted, UTF-8, non-ASCII kept as-is (not `\u` escaped), 2-space indent.
- Values are the exact runtime strings. Dart escapes are already resolved: `\$` → `$`, `\'` → `'`, `\"` → `"`.
- Key counts:

  | Locale | Keys |
  |---|---|
  | en | 830 |
  | de | 489 |
  | uz | 834 |
  | ru | 833 |
  | Union | 834 |

- Checks run during extraction: no duplicate keys, no string concatenation, no values containing `\n`.
- Two keys contain a space: `ent specialist` and `general practitioner` (profession lookups).

## Fallback semantics (must be preserved)

A lookup tries three things in order:
1. the current locale
2. `en`
3. the **raw key string**

Compose Resources behaves the same way when `values/` (the default) holds English. The last step is the exception: a key missing from `values/` is a build error, which is desirable.

## Missing keys

| Locale | Missing | Effect today |
|---|---|---|
| en | `discardChanges`, `noTasksFound`, `profileUpdatedSuccessfully`, `saveChanges` | The raw key is shown in en and de (bug). Used at `lib/features/profile/presentation/edit_profile_screen.dart:182,379,386` and `lib/features/tasks/presentation/tasks_screen.dart:155`. **Add English values in KMP**, e.g. "Discard changes", "No tasks found", "Profile updated successfully", "Save changes". |
| ru | `noTasksFound` | Falls back to en, which is also missing, so the raw key shows. |
| de | 345 keys | They fall back to English. List them with `jq -r 'keys[]' en.json uz.json \| sort -u > all; jq -r 'keys[]' de.json \| sort > de; comm -23 all de`. |
| uz | none | — |

## Placeholder conventions (four styles coexist)

| Style | Example key → value fragment | How the Flutter code fills it |
|---|---|---|
| `{{name}}` (most keys) | `loginAttemptsRemaining` → `{{count}}` | `replaceAll('{{count}}', v)` |
| `{name}` | `otpSentToPhone` → `{phone}`; `documentAccessRequestMessage` → `{requesterName}`, `{fileName}`, `{patientName}` | `replaceAll('{phone}', v)` |
| Literal `${…}` | `errorAccessDeniedThisAppRequiresRole` → `${requiredRole.name}`; `errorPatientProfileNotFoundForUser` → `${user.id}` | `replaceAll(r'${requiredRole.name}', role)` |
| `%s` | `minutesAgo`, `hoursAgo`, `daysAgo` | `replaceAll('%s', n)` |

A literal `$` that is **not** a placeholder: `priceUnder20` ("Under $20") and `passwordRequirementSpecialChar` ("!@#$%^&*").

There are no plurals. Counts are substituted into fixed text. Add `<plurals>` in KMP only if you also change the UX copy.

## Converting to Compose Multiplatform resources

Target layout:

```
composeResources/values/strings.xml      <- en.json (default)
composeResources/values-de/strings.xml   <- de.json
composeResources/values-uz/strings.xml   <- uz.json
composeResources/values-ru/strings.xml   <- ru.json
```

Rules:

1. **Key names** must be valid identifiers.
   - Rename `ent specialist` → `ent_specialist` and `general practitioner` → `general_practitioner`.
   - camelCase keys and keys like `documentCategory_EEG` are fine as they are.
   - Keep a map from the old key to the new one wherever keys come from data (profession lookups, notification types).
2. **Escaping** in values:
   - Escape `&` → `&amp;` and `<` → `&lt;`.
   - Escape `'` → `\'` and `"` → `\"`.
   - A value that starts with `@` or `?` needs a leading `\`.
   - Newlines are `\n`.
3. **Placeholders**:
   - Convert every style above to positional `%1$s`, `%2$s`, … in first-appearance order, per key.
   - Checked at extraction: for every key, all locales use the same placeholder names in the same order, so numbering by first appearance gives the same index everywhere.
   - If a future translation reorders placeholders, number them in the `en` order.
   - Afterwards, escape any remaining literal `%` as `%%` (for example in `passwordRequirementSpecialChar`).
   - Call as `stringResource(Res.string.otpSentToPhone, phone)`.
4. Put the missing `en` values in `values/` so every key has a default.
5. Strings the backend returns (error messages) are **not** keys. They are mapped to keys by the table in `../01-architecture-and-core.md` §9.2.

Minimal converter sketch (Python):

```python
import json, re, html
ph = re.compile(r"\{\{(\w+)\}\}|\$\{[^}]+\}|\{(\w+)\}|%s")
def conv(v):
    seen = []
    def rep(m):
        t = m.group(0)
        if t not in seen: seen.append(t)
        return f"%{seen.index(t)+1}$s"
    v = v.replace("%s", "\0")                  # protect %s before escaping %
    v = v.replace("%", "%%").replace("\0", "%s")
    v = ph.sub(rep, v)
    v = html.escape(v, quote=False).replace("'", "\\'").replace('"', '\\"')
    return ("\\" + v) if v[:1] in "@?" else v
```

This sketch was run over all four files without errors. Sample output: `documentAccessRequestMessage` → `%1$s requested access to %2$s for %3$s.`
