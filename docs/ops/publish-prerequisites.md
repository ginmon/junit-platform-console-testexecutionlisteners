# Publish prerequisites

Repo settings required by `.github/workflows/publish.yml` (CodeArtifact publish on push to `production`). Values are never stored in git.

| Name | Kind | Used as | Purpose |
|---|---|---|---|
| `OIDC_ROLE_ARN` | repository variable | `vars.OIDC_ROLE_ARN` | IAM role assumed via GitHub OIDC (`eu-central-1`). Must allow `codeartifact:DescribePackageVersion` and publish to domain `ginmon`, repository `internal`, owner `735822157553`. |
| `CODEARTIFACT_TOKEN` | repository secret | `secrets.CODEARTIFACT_TOKEN` | CodeArtifact auth token for `ginmon/internal`, consumed by `ginmon/gh-actions/setup-maven`. Short-lived (12h max). |

`verify-pr.yml` needs neither. `publish.yml` needs both.

## Setup

```sh
gh variable set OIDC_ROLE_ARN --repo ginmon/junit-platform-console-testexecutionlisteners --body '<role-arn>'
gh secret set CODEARTIFACT_TOKEN --repo ginmon/junit-platform-console-testexecutionlisteners
```

## Acceptance

- [ ] `OIDC_ROLE_ARN` set as a repository variable
- [ ] `CODEARTIFACT_TOKEN` set as a repository secret
- [ ] `publish.yml` on a `production` push succeeds, or skips with "already published"
