# Example Security Load Tester

A Java 17 Swing load generator with virtual users, ramp-up, global requests-per-
second cap, think time, optional session login, live statistics and CSV/HTML
report export.

The default scenario is deliberately safe: five users, ten requests per second,
60 seconds and `GET /health` only. Multiple GET paths may be comma-separated.
Use a dedicated non-MFA test account for authenticated paths.

```bash
mvn clean package
java -jar target/example-security-load-tester-1.0.0.jar
```

The application asks for explicit authorisation and an additional confirmation
above 50 users or 100 requests per second. It never invokes password reset,
email verification, Authy, Twilio or data-changing endpoints.

