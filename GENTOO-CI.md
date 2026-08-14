# Gentoo CI

GitHub-hosted Actions runners are Ubuntu-based and do not provide a hosted Gentoo runner. The regular CI therefore stays on the explicit `ubuntu-24.04` image.

An optional workflow is available at `.github/workflows/gentoo.yml`. To use it, register a self-hosted GitHub Actions runner on a Gentoo machine with these labels:

```text
self-hosted, linux, gentoo
```

The runner must provide Java 17 and Maven. Start the workflow manually from the Actions tab after registration.
