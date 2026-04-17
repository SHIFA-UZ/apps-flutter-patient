# Create Test Patient User

The test patient user migration may have an incorrect password hash. Use one of these methods to create the test user properly:

## Option 1: Use the Test Endpoint (Recommended)

After starting the backend, call this endpoint once to create the test user with correct password hash:

```bash
curl -X POST http://localhost:8080/api/test/create-test-patient
```

Or use Postman/Insomnia to make a POST request to:
- URL: `http://localhost:8080/api/test/create-test-patient`
- Method: POST
- No body required

This will create the test user with:
- **Email:** patient@test.com
- **Phone:** +998901234567
- **Password:** patient123

## Option 2: Use Registration Endpoint

Use the Flutter app's registration flow to create the test user:

1. Open the Flutter app
2. Click "Create Account"
3. Fill in the form:
   - Name: Test
   - Surname: Patient
   - Phone: +998901234567
   - Email: patient@test.com (optional)
   - Password: patient123
   - Confirm Password: patient123
4. Click "Next"
5. Fill in account info:
   - Birth Date: 01.01.1990
   - Gender: (select any)
   - Address: Tashkent, Uzbekistan
6. Click "Next"
7. Upload profile image (or skip)
8. Click "Complete"

This will create the user with correct password hash automatically.

## Option 3: Fix Migration Hash

If you want to fix the migration directly, you need to generate a correct BCrypt hash for "patient123". 

You can generate it using the backend:
1. Create a simple test class that uses BCryptPasswordEncoder
2. Or use the test endpoint above to get the correct hash
3. Update the migration with the correct hash

## Current Test Credentials

Once the user is created (via any method above):
- **Username (Email or Phone):** `+998901234567` or `patient@test.com`
- **Password:** `patient123`

## Troubleshooting

If login still fails after creating the user:
1. Check backend logs for actual error message
2. Verify the user exists in the database: `SELECT * FROM users WHERE role = 'PATIENT'`
3. Verify password hash is correct (should start with `$2a$10$`)
4. Check if CORS is properly configured
5. Check if the request format is correct (JSON with `username` and `password` fields)
