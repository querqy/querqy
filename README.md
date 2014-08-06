![travis ci build status](https://travis-ci.org/shopping24/querqy.png)

Querqy is a rule-based query rewriter for Java-based search engines.

This is still work in process - check back frequently, we're going to release Querqy soon. Meanwhile you can get in touch at post@rene-kriegler.com

Querqy is licensed under the [Apache License, Version 2](http://www.apache.org/licenses/LICENSE-2.0.html).

[Querqy is build using Travis CI](https://travis-ci.org/shopping24/querqy).

## Building the project

    $ git submodule update --init
    $ export JAVA_HOME=$(/usr/libexec/java_home -v 1.7)
    $ export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true -Dgpg.skip=true"
    $ mvn clean install
    
