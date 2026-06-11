GRADLEW := ./gradlew

# ─── ビルド ───────────────────────────────────────────────────────────────────

.PHONY: build
build: ## 全モジュールをビルド
	$(GRADLEW) build

.PHONY: build-mvi
build-mvi: ## :feature:weather-mvi をビルド
	$(GRADLEW) :feature:weather-mvi:build

.PHONY: build-mvvm
build-mvvm: ## :feature:weather-mvvm をビルド
	$(GRADLEW) :feature:weather-mvvm:build

.PHONY: assemble
assemble: ## debug/release APK を生成（テストなし）
	$(GRADLEW) :app:assembleDebug

.PHONY: clean
clean: ## ビルド成果物を削除
	$(GRADLEW) clean

# ─── テスト ───────────────────────────────────────────────────────────────────

.PHONY: test
test: ## 全モジュールの unit test を実行
	$(GRADLEW) test

.PHONY: test-mvi
test-mvi: ## :feature:weather-mvi の unit test を実行
	$(GRADLEW) :feature:weather-mvi:testDebugUnitTest

.PHONY: test-mvvm
test-mvvm: ## :feature:weather-mvvm の unit test を実行
	$(GRADLEW) :feature:weather-mvvm:testDebugUnitTest

.PHONY: test-domain
test-domain: ## :core:domain の unit test を実行
	$(GRADLEW) :core:domain:testDebugUnitTest

# ─── コード品質 ───────────────────────────────────────────────────────────────

.PHONY: lint
lint: ## 全モジュールで lint を実行
	$(GRADLEW) lint

.PHONY: ktlint
ktlint: ## 全モジュールで ktlint チェックを実行
	$(GRADLEW) ktlintCheck

.PHONY: ktlint-format
ktlint-format: ## ktlint で自動フォーマット
	$(GRADLEW) ktlintFormat

.PHONY: detekt
detekt: ## 全モジュールで detekt 静的解析を実行
	$(GRADLEW) detekt

.PHONY: check
check: ## lint + ktlint + test をまとめて実行
	$(GRADLEW) check

# ─── 依存関係 ─────────────────────────────────────────────────────────────────

.PHONY: deps
deps: ## :app の依存関係ツリーを表示
	$(GRADLEW) :app:dependencies

.PHONY: deps-mvi
deps-mvi: ## :feature:weather-mvi の依存関係ツリーを表示
	$(GRADLEW) :feature:weather-mvi:dependencies

.PHONY: deps-mvvm
deps-mvvm: ## :feature:weather-mvvm の依存関係ツリーを表示
	$(GRADLEW) :feature:weather-mvvm:dependencies

# ─── バージョン確認（Maven）──────────────────────────────────────────────────

.PHONY: versions-nav3
versions-nav3: ## Google Maven で navigation3 の最新バージョンを確認
	curl -s "https://dl.google.com/dl/android/maven2/androidx/navigation3/group-index.xml" \
	  | grep navigation3-ui-android \
	  | grep -o 'versions="[^"]*"' \
	  | tr ',' '\n' | tail -5

.PHONY: versions-ksp
versions-ksp: ## Maven Central で KSP の最新バージョンを確認
	curl -s "https://repo1.maven.org/maven2/com/google/devtools/ksp/symbol-processing-gradle-plugin/maven-metadata.xml" \
	  | grep '<version>' | tail -5

# ─── ヘルプ ───────────────────────────────────────────────────────────────────

.PHONY: help
help: ## このヘルプを表示
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
	  | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
