# How to Add Missing Translations

This guide will teach you how to add missing translations to the Shifa Patient App.

## 📍 Location

All translations are in: `lib/core/localization/app_localizations.dart`

## 📋 Step-by-Step Guide

### Step 1: Open the Translation File

Open `shifa_patient_app_v1/lib/core/localization/app_localizations.dart`

### Step 2: Find the Language Section

The file has a map structure with language codes:
- `'en'` - English
- `'de'` - German  
- `'uz'` - Uzbek (Latin)
- `'ru'` - Russian

Each language section looks like this:

```dart
'en': {
  // Common
  'appName': 'Shifa Patient',
  'hello': 'Hello',
  // ... more translations
},
```

### Step 3: Add Your Translation

**Example**: Let's say you want to add a translation for "Welcome" that's missing.

1. **Find where to add it** - Look for a logical section (Common, Auth, Profile, etc.)

2. **Add to English section first** (around line 15-203):
```dart
'en': {
  // Common
  'appName': 'Shifa Patient',
  'hello': 'Hello',
  'welcome': 'Welcome',  // ← Add your new translation here
  // ...
},
```

3. **Add to Uzbek section** (around line 352-546):
```dart
'uz': {
  // Common
  'appName': 'Shifa Patient',
  'hello': 'Salom',
  'welcome': 'Xush kelibsiz',  // ← Add Uzbek translation
  // ...
},
```

4. **Add to other languages** (German and Russian sections):
```dart
'de': {
  'welcome': 'Willkommen',  // German
  // ...
},
'ru': {
  'welcome': 'Добро пожаловать',  // Russian
  // ...
},
```

### Step 4: Add the Getter Method

Scroll down to the bottom of the file (around line 700+) where you'll see getter methods like:

```dart
String get hello => translate('hello');
String get patient => translate('patient');
```

Add your new getter:

```dart
String get welcome => translate('welcome');
```

**Note**: If the translation might be missing, use nullable:
```dart
String? get welcome => translate('welcome');
```

### Step 5: Use in Your Code

In any widget file, use it like this:

```dart
final l10n = AppLocalizations.of(context)!;
Text(l10n.welcome)  // Will show "Welcome" in English, "Xush kelibsiz" in Uzbek
```

## 📝 Translation Structure

The file has these main sections:

1. **Common** - General words (hello, error, cancel, save, etc.)
2. **Auth** - Login, signup, password related
3. **Profile** - User profile, settings
4. **Home** - Home screen content
5. **Bookings** - Appointments, scheduling
6. **Doctors** - Doctor listings, reviews
7. **Documents** - Document management
8. **Chat** - Messaging
9. **App Lock** - Security features
10. **Notifications** - Push notifications
11. **Status** - Status labels (confirmed, cancelled, etc.)

## 🔍 Quick Reference

### Finding Where a Translation is Used

If you see a missing translation in the app:

1. **Look at the UI** - Note the English text shown
2. **Search in code** - Use your IDE's search to find where it's used:
   ```dart
   // Search for: l10n.someText or AppLocalizations.of(context)!.someText
   ```
3. **Find the key** - The key name is usually camelCase of the English text
   - "Select Date" → `selectDate`
   - "My Tasks" → `myTasks`

### Common Patterns

- **Keys are camelCase**: `myTasks`, `selectDate`, `enableAppLock`
- **Keys match English text**: Usually the English text converted to camelCase
- **Nullable getters**: Use `String?` if translation might be missing
- **Non-nullable getters**: Use `String` if translation always exists

## ✅ Checklist When Adding Translation

- [ ] Added to English section (`'en'`)
- [ ] Added to Uzbek section (`'uz'`)
- [ ] Added to German section (`'de'`) - if needed
- [ ] Added to Russian section (`'ru'`) - if needed
- [ ] Added getter method at the bottom
- [ ] Tested in the app

## 🎯 Example: Complete Translation Addition

Let's add "Settings" translation:

**1. Add to English (line ~49):**
```dart
'preferences': 'Preferences',
'settings': 'Settings',  // ← New
```

**2. Add to Uzbek (line ~386):**
```dart
'preferences': 'Sozlamalar',
'settings': 'Sozlamalar',  // ← New (or use different word if needed)
```

**3. Add getter (line ~755):**
```dart
String get preferences => translate('preferences');
String get settings => translate('settings');  // ← New
```

**4. Use in code:**
```dart
Text(l10n.settings)
```

## 🚨 Important Notes

1. **Always add to English first** - English is the base language
2. **Keep keys consistent** - Use the same key name across all languages
3. **Use proper Uzbek** - Make sure Uzbek translations are correct (Latin script)
4. **Test after adding** - Run the app and switch languages to verify
5. **Don't break existing** - Be careful not to delete or modify existing translations accidentally

## 📚 Translation Tips

- **Context matters**: "Book" can mean "reserve" or "read a book" - use descriptive keys
- **Keep it short**: Mobile UI has limited space
- **Be consistent**: Use same terminology throughout (e.g., always "Appointment" not sometimes "Meeting")
- **Test on device**: Some languages have longer words that might overflow

## 🔧 Troubleshooting

**Translation not showing?**
- Check if key exists in all language sections
- Check if getter method exists
- Check if you're using `l10n.yourKey` correctly
- Restart the app after adding translations

**Getting null errors?**
- Use nullable getter: `String? get yourKey => translate('yourKey');`
- Use null-safe access: `l10n.yourKey ?? 'Fallback text'`

---

**Happy Translating! 🌍**
