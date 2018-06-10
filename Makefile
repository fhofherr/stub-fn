# This is a self-documenting Makefile.
# See https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html

CLJ=clj

SRC_FILES = $(shell find src -type f -iname '*.clj')
TEST_FILES = $(shell find test -type f -iname '*.clj')

.PHONY: cider-nrepl
cider-nrepl: ## Start an nrepl-with the cider-nrepl middleware applied
	@$(CLJ) -A:test:dev -Sdeps '{:deps {cider/cider-nrepl {:mvn/version "0.18.0-SNAPSHOT"} }}' -e '(require (quote cider-nrepl.main)) (cider-nrepl.main/init ["cider.nrepl/cider-middleware"])'

.PHONY: test
test: $(SRC_FILES) $(TEST_FILES) ## Run all tests
	@$(CLJ) -A:test:run-tests

.PHONY: help
help: ## Print this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
