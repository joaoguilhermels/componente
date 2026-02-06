# Security Approval Template

Use this template to document approval for known vulnerabilities that cannot
be immediately remediated. Each entry must be reviewed and signed off by the
security lead before the pipeline suppression is added.

---

## Vulnerability Approval

| Field | Value |
|-------|-------|
| **CVE/Advisory** | CVE-XXXX-XXXXX |
| **Component** | `group:artifact:version` |
| **Severity** | Critical / High / Medium / Low |
| **CVSS Score** | X.X |
| **Discovery Date** | YYYY-MM-DD |
| **Approval Date** | YYYY-MM-DD |
| **Review Date** | YYYY-MM-DD (max 90 days from approval) |

### Description
Brief description of the vulnerability and affected component.

### Justification
Why this vulnerability cannot be immediately fixed (e.g., no patch available,
upstream dependency, breaking change required).

### Mitigating Controls
- [ ] Input validation prevents exploitation path
- [ ] Network segmentation limits exposure
- [ ] WAF rules block known attack vectors
- [ ] Monitoring/alerting configured for exploitation attempts
- [ ] Other: _______________

### Impact Assessment
- **Exploitability**: Describe if/how this can be exploited in our context
- **Affected Data**: What data could be compromised
- **Blast Radius**: Which systems/services are affected

### Remediation Plan
| Action | Owner | Target Date |
|--------|-------|-------------|
| Upgrade to patched version | @developer | YYYY-MM-DD |
| Apply workaround | @developer | YYYY-MM-DD |
| Re-assess if no fix available | @security | YYYY-MM-DD |

### Approvals
| Role | Name | Date | Signature |
|------|------|------|-----------|
| Security Lead | | | |
| Tech Lead | | | |
| Product Owner | | | |
