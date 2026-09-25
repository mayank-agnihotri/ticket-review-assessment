# Command: Review Generated Code for Security Issues

Use on AI-generated or large diffs before merge.

## Prompt template

```
Security review for Ticket Management System code changes.

Read rules/security.md and scan the diff for:

1. Hardcoded secrets, API keys, passwords, tokens
2. Credentials in application.yml committed to git
3. SQL injection (native queries without binding)
4. Mass assignment or missing validation on REST inputs
5. Overly permissive CORS or disabled security controls
6. Logging of sensitive fields
7. Dependency versions with known critical CVEs (if lockfile present)

Assessment note: auth is NOT SPECIFIED—if security code exists, verify it matches spec/architecture.md only.

Output:
- Severity (high/medium/low)
- File:line
- Remediation
- false positives noted
```

## Constraint

Do not auto-fix without human approval on high-severity items.
