

[![Build Querqy](https://github.com/querqy/querqy/actions/workflows/ci.yml/badge.svg)](https://github.com/querqy/querqy/actions/workflows/ci.yml)
[![Docker Integration Tests for Querqy](https://github.com/querqy/querqy/actions/workflows/integration-tests.yml/badge.svg)](https://github.com/querqy/querqy/actions/workflows/integration-tests.yml)
[![Querqy for Solr](https://img.shields.io/maven-central/v/org.querqy/querqy-solr.svg?label=Download%20Querqy%20for%20Solr%20(Maven%20Central))](https://search.maven.org/search?q=g:%22org.querqy%22%20AND%20a:%22querqy-solr%22) [![Querqy Core](https://img.shields.io/maven-central/v/org.querqy/querqy-core.svg?label=Querqy%20core%20(Maven%20Central))](https://search.maven.org/search?q=g:%22org.querqy%22%20AND%20a:%22querqy-core%22)

:warning: **IMPORTANT: Querqy 5.5 for Solr introduces some breaking changes** that will affect you if you are upgrading from an older version and if
* you are using Info Logging, or
* rely on the debug output format, or
* you are using a custom rewriter implementation

See here for the release notes: https://querqy.org/docs/querqy/release-notes.html#major-changes-in-querqy-for-solr-5-5-1


# Querqy

Querqy is a framework for query preprocessing in Java-based search engines.

This is the repository for `querqy-core`, `querqy-lucene` and `querqy-solr`. Repositories 
for further Querqy integrations can be found at:

* https://github.com/querqy/querqy-elasticsearch/ (Querqy for Elasticsearch)
* https://github.com/querqy/querqy-opensearch/ (Querqy for OpenSearch)
* https://github.com/querqy/querqy-unplugged/ (Querqy for rewriting queries outside the search engine)


## Documentation and 'Getting started'

[Visit docs.querqy.org/querqy/ for detailed documentation.](https://docs.querqy.org/querqy/index.html) 

**Please make sure you read the [release notes](https://docs.querqy.org/querqy/release-notes.html)!** 

Check out [Querqy.org](https://querqy.org) for related projects that help you speed up search software development.

Developer channel: Join #querqy on the [Relevance & Matching Tech Slack space](https://relevancy.slack.com)

## License

Querqy is licensed under the [Apache License, Version 2](http://www.apache.org/licenses/LICENSE-2.0.html).

## Contributing

Please read our [developer guidelines](contributing.md) before contributing.




