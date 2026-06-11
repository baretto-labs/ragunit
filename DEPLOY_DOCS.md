# Deploy Documentation Setup

One-time setup to enable automatic documentation publishing to
[baretto-labs/ragunit-documentation](https://github.com/baretto-labs/ragunit-documentation)
on every release tag.

## 1. Generate SSH keypair

```bash
ssh-keygen -t ed25519 -C "ragunit-docs-deploy" -f ragunit_docs_deploy -N ""
```

## 2. Add public key to ragunit-documentation

1. Go to `github.com/baretto-labs/ragunit-documentation` → **Settings** → **Deploy keys** → **Add deploy key**
2. Title: `ragunit-ci`
3. Key: paste contents of `ragunit_docs_deploy.pub`
4. Check **Allow write access**
5. Click **Add key**

## 3. Add private key to ragunit (this repo)

1. Go to this repo → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**
2. Name: `DOCS_DEPLOY_KEY`
3. Secret: paste contents of `ragunit_docs_deploy`
4. Click **Add secret**

## 4. Enable GitHub Pages on ragunit-documentation

1. Go to `github.com/baretto-labs/ragunit-documentation` → **Settings** → **Pages**
2. Source: **Deploy from a branch**
3. Branch: `main`, folder `/ (root)`
4. Click **Save**

## 5. Clean up local key files

```bash
rm ragunit_docs_deploy ragunit_docs_deploy.pub
```

## 6. Test

Push a release tag to trigger the workflow:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Or trigger manually: **Actions** → **Publish Documentation** → **Run workflow**.

The documentation will be live at `https://baretto-labs.github.io/ragunit-documentation`.
