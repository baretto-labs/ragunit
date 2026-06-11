## Summary

<!-- What does this PR do? Link the related task: `.tasks/tasks/TASK-XXX.md` -->

Closes #

## Checklist

### Code
- [ ] Tests written first (TDD — failing test before production code)
- [ ] Test naming: `should_[expectedBehavior]_when_[condition]`
- [ ] One logical assertion per test
- [ ] `mvn verify` passes (Checkstyle + SpotBugs — 0 violations)
- [ ] `ragunit-core` has zero new production dependencies

### Quality
- [ ] Mutation coverage ≥ 80% on new classes (`pitest`)
- [ ] Method length ≤ 20 lines · Class length ≤ 200 lines · Cyclomatic complexity ≤ 5

### Documentation
- [ ] Javadoc on all new/modified public classes, interfaces, and methods
- [ ] `docs/` page created or updated (if metric or public API changed)
- [ ] `DOMAIN.md` updated (if a new term is introduced)

## Notes for reviewer

<!-- Anything that needs extra attention or context during review -->
