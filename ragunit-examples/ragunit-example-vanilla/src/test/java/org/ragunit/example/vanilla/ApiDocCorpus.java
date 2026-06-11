package org.ragunit.example.vanilla;

import org.ragunit.core.domain.Document;

import java.util.List;

/**
 * API cloud support documentation corpus shared across vanilla evaluation examples.
 *
 * <p>Eight realistic documents covering: authentication, rate limiting, pagination,
 * error formats, webhooks, SLA, GDPR/PII, and the mobile SDK (intentional noise).
 * Each document is 80-150 words, mimicking production REST API documentation.
 */
final class ApiDocCorpus {

    static final Document AUTH = new Document("""
            Authentication uses OAuth 2.0 with JWT access tokens. To obtain a token, send a POST \
            request to /auth/token with your client_id and client_secret in the request body as \
            application/x-www-form-urlencoded. Access tokens expire after 3600 seconds and must \
            be renewed using a refresh token. Refresh tokens remain valid for 30 days. Include the \
            bearer token in all API requests using the Authorization header: \
            'Authorization: Bearer <token>'. Token introspection is available at \
            GET /auth/token/introspect to verify validity and retrieve associated scopes. \
            Revoke compromised tokens immediately via POST /auth/token/revoke. \
            Never embed client_secret in client-side code or mobile applications.""");

    static final Document RATE_LIMITING = new Document("""
            API rate limits depend on your subscription plan. Free tier accounts are allowed \
            100 requests per minute. Pro plan accounts can make up to 1000 requests per minute. \
            Enterprise customers have no rate limit. Exceeding the limit returns HTTP 429 \
            Too Many Requests with a Retry-After header specifying the number of seconds to wait. \
            Implement exponential backoff with jitter: start with a 1-second delay and double it \
            on each retry, adding random jitter between 0 and 500 milliseconds to prevent \
            synchronized retries across clients. Monitor current usage in real time via \
            GET /usage/current.""");

    static final Document PAGINATION = new Document("""
            All list endpoints use cursor-based pagination for consistent results across large \
            datasets. Each response includes a next_cursor field when more results are available; \
            pass it as the cursor query parameter to fetch the following page. The maximum page \
            size is 100 items per request; the default is 20 items. Use the limit query parameter \
            to request a specific page size. Cursor-based pagination is preferable to offset-based \
            pagination for large collections because it avoids result skipping during concurrent \
            writes. When next_cursor is null or absent from the response, you have reached the \
            last page of results.""");

    static final Document ERRORS = new Document("""
            All API errors conform to the RFC 7807 Problem Details standard. Every error response \
            includes: type, a URI identifying the error class; title, a human-readable summary; \
            status, the HTTP status code; detail, a specific description of this occurrence; and \
            instance, a URI uniquely identifying this error event. Common codes: 400 Bad Request \
            for invalid parameters, 401 Unauthorized when the token is missing or expired, \
            403 Forbidden for insufficient permissions, 404 Not Found when the resource does not \
            exist, and 429 Too Many Requests when the rate limit is exceeded. Error responses use \
            Content-Type: application/problem+json.""");

    static final Document WEBHOOKS = new Document("""
            Register webhook endpoints to receive real-time event notifications. Create a webhook \
            by sending POST /webhooks with your callback URL and list of subscribed event types. \
            Each delivery includes an X-Signature-256 header containing an HMAC-SHA256 signature \
            computed from the request body using your webhook secret. Always verify this signature \
            before processing any payload to prevent spoofing attacks. Your endpoint must return \
            HTTP 200 within 10 seconds; slower responses are treated as delivery failures. Failed \
            deliveries are automatically retried up to 5 times using exponential backoff. Review \
            delivery history and trigger manual retries via GET /webhooks/{id}/deliveries.""");

    static final Document SLA = new Document("""
            The API SLA guarantees 99.9% monthly uptime, corresponding to less than 8.7 hours of \
            downtime per year. Scheduled maintenance windows are announced at least 72 hours in \
            advance via the status page and email notifications. Incidents are classified by \
            severity: P1 (complete outage) requires a first response within 15 minutes, P2 \
            (degraded service affecting most users) within 1 hour, and P3 (partial degradation \
            affecting some users) within 4 hours. Service credits are issued automatically: 10% of \
            the monthly fee when uptime falls between 99.5% and 99.9%, and 25% for uptime below \
            99.5%. Real-time status is available at status.api.example.com.""");

    static final Document PII_GDPR = new Document("""
            Personal data processed by the API includes names, email addresses, and IP addresses. \
            All personal data is stored exclusively in EU-West data centers to satisfy GDPR data \
            residency requirements. Upon account deletion, personal data is retained for 30 days to \
            meet legal obligations before permanent erasure. Users may request a full export of \
            their personal data by calling GET /users/me/export; the export is generated \
            asynchronously and delivered to the registered email address. Data Processing Agreements \
            are available for enterprise customers. The API does not sell or share personal data \
            with third parties. Submit data subject requests to privacy@api.example.com.""");

    static final Document MOBILE_SDK = new Document("""
            The official Mobile SDK supports iOS 15 and above and Android 8.0 (API level 26) and \
            above. Add the iOS SDK using CocoaPods: include pod 'ExampleSDK', '~> 2.1' in your \
            Podfile and run pod install. For Android, add \
            implementation 'com.example:sdk:2.1.0' to your build.gradle dependencies. The SDK \
            handles access token refresh automatically, reducing boilerplate in client apps. \
            Offline caching uses SQLite and stores up to 50 MB of API responses locally. Push \
            notification support requires additional setup in AppDelegate for iOS or \
            FirebaseMessagingService for Android. UI components are available as SwiftUI views and \
            Jetpack Compose composables.""");

    /** All eight corpus documents in order. */
    static final List<Document> ALL = List.of(
            AUTH, RATE_LIMITING, PAGINATION, ERRORS, WEBHOOKS, SLA, PII_GDPR, MOBILE_SDK);

    private ApiDocCorpus() {
    }
}
