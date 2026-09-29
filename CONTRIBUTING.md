# Contributing

Contributions are always welcome, no matter how large or small!

We want this community to be friendly and respectful to each other. Please follow it in all your interactions with the project. Before contributing, please read the [code of conduct](./CODE_OF_CONDUCT.md).

## Development workflow

This project is a monorepo managed using [Yarn workspaces](https://yarnpkg.com/features/workspaces). It contains the following packages:

- The library package in the root directory.
- An example app in the `example/` directory.

To get started with the project, make sure you have the correct version of [Node.js](https://nodejs.org/) installed. See the [`.nvmrc`](./.nvmrc) file for the version used in this project.

Run `yarn` in the root directory to install the required dependencies for each package:

```sh
yarn
```

> Since the project relies on Yarn workspaces, you cannot use [`npm`](https://github.com/npm/cli) for development without manually migrating.

The [example app](/example/) demonstrates usage of the library. You need to run it to test any changes you make.

It is configured to use the local version of the library, so any changes you make to the library's source code will be reflected in the example app. Changes to the library's JavaScript code will be reflected in the example app without a rebuild, but native code changes will require a rebuild of the example app.

If you want to use Android Studio or Xcode to edit the native code, you can open the `example/android` or `example/ios` directories respectively in those editors. To edit the Objective-C or Swift files, open `example/ios/PlaynestUnionadExample.xcworkspace` in Xcode and find the source files at `Pods > Development Pods > react-native-playnest-unionad`.

To edit the Java or Kotlin files, open `example/android` in Android studio and find the source files at `react-native-playnest-unionad` under `Android`.

You can use various commands from the root directory to work with the project.

To start the packager:

```sh
yarn example start
```

To run the example app on Android:

```sh
yarn example android
```

To run the example app on iOS:

```sh
yarn example ios
```

To confirm that the app is running with the new architecture, you can check the Metro logs for a message like this:

```sh
Running "PlaynestUnionadExample" with {"fabric":true,"initialProps":{"concurrentRoot":true},"rootTag":1}
```

Note the `"fabric":true` and `"concurrentRoot":true` properties.

Make sure your code passes TypeScript:

```sh
yarn typecheck
```

To check for linting errors, run the following:

```sh
yarn lint
```

To fix formatting errors, run the following:

```sh
yarn lint --fix
```



### Scripts

The `package.json` file contains various scripts for common tasks:

- `yarn`: setup project by installing dependencies.
- `yarn typecheck`: type-check files with TypeScript.
  - `yarn lint`: lint files with [ESLint](https://eslint.org/).
    - `yarn example start`: start the Metro server for the example app.
- `yarn example android`: run the example app on Android.
- `yarn example ios`: run the example app on iOS.
  
### Sending a pull request

> **Working on your first pull request?** You can learn how from this _free_ series: [How to Contribute to an Open Source Project on GitHub](https://app.egghead.io/playlists/how-to-contribute-to-an-open-source-project-on-github).

When you're sending a pull request:

- Prefer small pull requests focused on one change.
- Verify that linters and tests are passing.
- Review the documentation to make sure it looks good.
- Follow the pull request template when opening a pull request.
- For pull requests that change the API or implementation, discuss with maintainers first by opening an issue.

## Release process（发版流程）

发版由 **GitHub Release** 触发 Actions 自动发布到 npm（见 `.github/workflows/publish.yml`）。步骤：

1. **更新 `CHANGELOG.md`**：把 `[Unreleased]` 下的改动移到新版本号标题下并补日期，底部加一行 compare 链接。按需分区 🚀 New Features / 🐛 Bug Fixes / 🔧 Changed / 📝 Docs。
2. **升 `package.json` 的 `version`**（语义化版本：`patch` = 修复/依赖升级、`minor` = 新功能、`major` = 破坏性变更）。
3. **提交并打 tag**：`git commit` → `git tag -a vX.Y.Z -m "vX.Y.Z"` → push 分支与 tag。
4. **建 Release**：GitHub → Releases → *Draft a new release* → 选 tag `vX.Y.Z` → 填 **Release notes**（见下）→ Release label 选 *None*（正式版）→ *Publish release*。
5. Publish 后 `Publish to npm` 工作流自动 `npm publish`，到 **Actions** 看绿勾确认；`npm view react-native-playnest-unionad version` 核对。

### Release notes 怎么写

两种方式，二选一或组合：

- **粘贴 CHANGELOG（推荐，直接 push 到 main 的场景最稳）**：把 `CHANGELOG.md` 里该版本那一段复制进 Release notes。
- **Generate release notes（需走 PR）**：点该按钮，GitHub 按 `.github/release.yml` 的分区规则自动生成 “What's Changed / Contributors / Full Changelog”。

也可组合：先 *Generate release notes*，再把 CHANGELOG 的分区（New Features/Bug Fixes）补在最上面。

### Contributors 是怎么来的

Release 里的 **“What's Changed”** 和 **“New Contributors”**（`* @user made their first contribution in #123`）是 GitHub 的 *Generate release notes* 按**合并的 Pull Request 作者**自动生成的：

- 想有这一块，改动必须**通过 PR 合并**（哪怕是你自己：开分支 → 提 PR → 合并）。这样 GitHub 才知道每条改动归属谁、谁是首次贡献者。
- 若**直接 `push` 到 `main`（无 PR）**，*Generate release notes* 没有 PR 可归属，Contributors 就是空的。
- 注意区分：仓库侧栏 / `graphs/contributors` 的 Contributors 是另一套统计（按 default 分支的 commit 作者），与 Release 无关，直接 push 也会计入。

**结论**：想让每个 Release 都带 Contributors，就把改动走 **PR 流程**；发版时用 *Generate release notes* 拿到 Contributors，再把 CHANGELOG 的分区补在上面。
