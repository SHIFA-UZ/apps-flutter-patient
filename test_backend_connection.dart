import 'dart:io';
import 'dart:convert';

void main() async {
  // Try both with and without www
  final urls = [
    'https://www.shifa-doc-backend-mvp-production.up.railway.app',
    'https://shifa-doc-backend-mvp-production.up.railway.app',
  ];
  
  for (final backendUrl in urls) {
    print('🔍 Testing Railway Backend Connection\n');
    print('Backend URL: $backendUrl\n');
    
    // Test 1: Health Check
    print('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    print('Test 1: Health Check Endpoint');
    print('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    try {
      final healthClient = HttpClient();
      final healthUri = Uri.parse('$backendUrl/actuator/health');
      final healthRequest = await healthClient.getUrl(healthUri);
      healthRequest.headers.set('Accept', 'application/json');
      
      final healthResponse = await healthRequest.close();
      final healthBody = await healthResponse.transform(utf8.decoder).join();
      
      print('Status Code: ${healthResponse.statusCode}');
      print('Response: $healthBody');
      
      if (healthResponse.statusCode == 200) {
        print('✅ Health check PASSED\n');
        healthClient.close();
        print('✅✅✅ WORKING URL: $backendUrl ✅✅✅\n');
        break; // Found working URL, exit loop
      } else {
        print('❌ Health check FAILED\n');
      }
      
      healthClient.close();
    } catch (e) {
      print('❌ Health check ERROR: $e\n');
      if (e is HandshakeException) {
        print('   → SSL Certificate issue (hostname mismatch)\n');
      }
    }
    
    // Test 2: Login Endpoint
    print('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    print('Test 2: Login Endpoint');
    print('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    try {
      final loginClient = HttpClient();
      final loginUri = Uri.parse('$backendUrl/api/auth/login');
      final loginRequest = await loginClient.postUrl(loginUri);
      loginRequest.headers.set('Content-Type', 'application/json');
      loginRequest.headers.set('Accept', 'application/json');
      
      loginRequest.write(jsonEncode({
        'username': 'test',
        'password': 'test',
      }));
      
      final loginResponse = await loginRequest.close();
      final loginBody = await loginResponse.transform(utf8.decoder).join();
      
      print('Status Code: ${loginResponse.statusCode}');
      print('Response: $loginBody');
      
      if (loginResponse.statusCode == 401 || loginResponse.statusCode == 400) {
        print('✅ Login endpoint is accessible (expected auth error)\n');
        loginClient.close();
        print('✅✅✅ WORKING URL: $backendUrl ✅✅✅\n');
        break; // Found working URL, exit loop
      } else if (loginResponse.statusCode == 200) {
        print('✅ Login endpoint is accessible\n');
        loginClient.close();
        print('✅✅✅ WORKING URL: $backendUrl ✅✅✅\n');
        break; // Found working URL, exit loop
      } else {
        print('⚠️ Unexpected status code\n');
      }
      
      loginClient.close();
    } catch (e) {
      print('❌ Login endpoint ERROR: $e\n');
      if (e is HandshakeException) {
        print('   → SSL Certificate issue (hostname mismatch)\n');
      }
    }
    
    print('\n');
  }
  
  print('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  print('Test Complete');
  print('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  print('\n💡 If both URLs failed, the issue might be:');
  print('   1. Backend is down');
  print('   2. SSL certificate configuration issue');
  print('   3. Network/firewall blocking the connection');
  print('\n💡 Check Railway dashboard to verify:');
  print('   - Service is running');
  print('   - Custom domain is configured correctly');
  print('   - SSL certificate is valid');
}
