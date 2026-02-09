## Summary

<!-- Brief description of what this PR does and why -->

## Changes

<!-- List the key changes made in this PR -->

-

## Test Plan

<!-- How was this tested? Include relevant test commands and scenarios -->

- [ ] `make test` passes
- [ ] `make verify` passes (ArchUnit module boundary checks)
- [ ] Manual testing performed (describe below if applicable)

## Checklist

- [ ] Tests added/updated for the changes
- [ ] Documentation updated (README, CLAUDE.md, etc.)
- [ ] No breaking changes (or documented above)
- [ ] Follows project naming conventions (`cr_` tables, `crui-` selectors, `customer.registry.*` properties)
- [ ] New beans use `@ConditionalOnMissingBean`
- [ ] New features gated by `@ConditionalOnProperty`
- [ ] No infrastructure dependencies added to `core/` module
- [ ] Security scan clean (`make security-scan`)
