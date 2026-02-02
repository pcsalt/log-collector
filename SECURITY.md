# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take the security of Log Collector seriously. If you believe you have found a security vulnerability, please report it to us as described below.

### How to Report

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via email to **pcsalt@example.com** with the subject line: `[SECURITY] Log Collector Vulnerability Report`

Please include the following information:

- Type of issue (e.g., buffer overflow, SQL injection, cross-site scripting, etc.)
- Full paths of source file(s) related to the issue
- Location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

### What to Expect

- **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours.
- **Communication**: We will keep you informed about our progress toward fixing the vulnerability.
- **Resolution**: We aim to resolve critical vulnerabilities within 7 days.
- **Disclosure**: We will coordinate with you on the public disclosure timeline.

### Safe Harbor

We consider security research conducted in good faith to be:

- Authorized in accordance with this policy
- Not subject to legal action from us
- Helpful and appreciated

We ask that you:

- Make a good faith effort to avoid privacy violations and data destruction
- Do not access or modify data that does not belong to you
- Give us reasonable time to fix the issue before public disclosure
- Do not exploit a security issue for purposes other than verification

## Security Best Practices

When deploying Log Collector:

1. **Network Security**
   - Deploy behind a reverse proxy (nginx, traefik)
   - Use HTTPS in production
   - Restrict access to trusted networks

2. **Docker Security**
   - Use the official image from Docker Hub
   - Run as non-root user (default in our image)
   - Keep the image updated

3. **Data Security**
   - Regularly backup the SQLite database
   - Monitor log retention settings
   - Be mindful of sensitive data in logs

4. **Access Control**
   - Restrict dashboard access
   - Use firewall rules to limit API access
   - Monitor for unusual activity

## Known Security Considerations

- **No Authentication by Default**: The current version does not include authentication. Plan to deploy behind an authenticating proxy in production.
- **CORS**: Default CORS allows all origins. Configure `CORS_ALLOWED_ORIGINS` for production.
- **Log Data**: Logs may contain sensitive information. Configure appropriate retention periods.

## Security Updates

Security updates will be released as patch versions (e.g., 1.0.1) and announced via:

- GitHub Releases
- CHANGELOG.md
- Security advisories (for critical issues)

## Contact

For security-related inquiries: **pcsalt@example.com**
