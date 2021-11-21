# Developer guidelines for contributing to Querqy

We welcome your contribution to developping Querqy further! Let us know about your idea or submit a pull request!

## Principles

We haven't compiled comprehensive developer guidelines yet but we ask you to adhere to the following:

Please...

* Create an issue on GitHub before submitting a pull request. This not only helps when putting together releases and release notes 
  but it is always useful to discuss evolving ideas at an early stage.
* Mark your commits with the GitHub issue number: `'Added XYRewriter #1234'`. The commit will then show up in the discussion of the issue. 
* Avoid adding library dependencies where possible. Querqy artifacts are plugged into Solr and Elasticsearch. The fewer dependencies we add, the farther away
  we stay from jar hell.
* Use 4 spaces for indentation. Opening `{` should be put on the same line like statement that this code block belongs to:
```
void myMethod() {
    if (isSunday()) {
    
    }
}
```
NOT:
```
void myMethod() 
{
    if (isSunday()) 
    {
    
    }
}
```
* Make variables `final` where possible. We know this is not everyone's favourite coding style but we did see performance gains from this. 
  We won't mark a missing `final` in a test class.
  
## Running integration tests

To run integration tests locally, start up your Docker and then execute

`mvn verify -DskipITs=false`


