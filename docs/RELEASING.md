# 发版指南（维护者）

面向**维护者**：如何发布新版本、写 Release notes、让每个 Release 带上 Contributors。
（贡献者只需看 [CONTRIBUTING.md](../CONTRIBUTING.md)。）

发版由 **GitHub Release** 触发 Actions 自动发布到 npm（见 `.github/workflows/publish.yml`）。

## 发版步骤

1. **更新 `CHANGELOG.md`**：把 `[Unreleased]` 下的改动移到新版本号标题下并补日期，底部加一行 compare 链接。按需分区 🚀 New Features / 🐛 Bug Fixes / 🔧 Changed / 📝 Docs。
2. **升 `package.json` 的 `version`**（语义化版本：`patch` = 修复/依赖升级、`minor` = 新功能、`major` = 破坏性变更）。
3. **提交并打 tag**：`git commit` → `git tag -a vX.Y.Z -m "vX.Y.Z"` → push 分支与 tag。
4. **建 Release**：GitHub → Releases → *Draft a new release* → 选 tag `vX.Y.Z` → 填 **Release notes**（见下）→ Release label 选 *None*（正式版）→ *Publish release*。
5. Publish 后 `Publish to npm` 工作流自动 `npm publish`，到 **Actions** 看绿勾确认；`npm view react-native-playnest-unionad version` 核对。

## Release notes 怎么写

- **粘贴 CHANGELOG（直接 push 到 main 的场景最稳）**：把 `CHANGELOG.md` 里该版本那一段复制进 Release notes。
- **Generate release notes（需走 PR）**：GitHub 按 `.github/release.yml` 的分区规则自动生成 “What's Changed / Contributors / Full Changelog”。
- **组合（推荐）**：先 *Generate release notes* 拿到 Contributors，再把 CHANGELOG 的分区（New Features/Bug Fixes）补在最上面。

## Contributors 是怎么来的

Release 里的 **“What's Changed”** 和 **“New Contributors”**（`* @user made their first contribution in #123`）是 *Generate release notes* 按**合并的 Pull Request 作者**自动生成的：

- 想有这一块，改动必须**通过 PR 合并**（哪怕维护者自己）。否则直接 `push` 到 `main` 时没有 PR 可归属，Contributors 为空。
- 区分：仓库侧栏 / `graphs/contributors` 的 Contributors 是另一套统计（按 default 分支 commit 作者），与 Release 无关。

## 让每个 Release 都有 Contributors —— 个人项目也能优雅走 PR

### 一次性：建好 label（供 `.github/release.yml` 分区）

在仓库 Labels 页建：`feature`、`bug`、`dependencies`、`documentation`（`enhancement`/`docs` GitHub 默认已有）。

### 每次改动

```bash
# 1. 从 main 开分支
git switch -c feat/xxx                 # feat/... fix/... docs/...

# 2. 改代码 + 更新 CHANGELOG 的 [Unreleased] + 提交
git add -A
git commit -m "Add xxx"

# 3. 推分支
git push -u github feat/xxx
```

开 PR 并合并（二选一）：

- **网页**：push 后点 “Compare & pull request” → 加 label（如 `feature`）→ Create → **Squash and merge** → Delete branch。
- **gh CLI**（`brew install gh`）：`gh pr create --fill --label feature` → `gh pr merge --squash --delete-branch`。

合并后：

```bash
git switch main
git pull github main
```

功能性改动走 PR（进 Contributors）；纯收尾提交（改 CHANGELOG/版本号）可直接在 main 提交，不影响。若另配了内部镜像 remote，发版后 `git push <mirror> main && git push <mirror> vX.Y.Z` 同步即可。
