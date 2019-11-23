![travis ci build status](https://travis-ci.org/renekrie/querqy.png) 
[ ![Download Querqy for Lucene/Solr](https://api.bintray.com/packages/renekrie/maven/querqy-for-lucene/images/download.svg) ](https://bintray.com/renekrie/maven/querqy-for-lucene/_latestVersion)

Support & Community:  [![Gitter community](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/querqy/users)
# Querqy

Querqy is a framework for query preprocessing in Java-based search engines. It comes with a powerful, rule-based preprocessor named 'Common Rules Preprocessor', which provides query-time synonyms, query-dependent boosting and down-ranking, and query-dependent filters. While the Common Rules Preprocessor is not specific to any search engine, Querqy provides a plugin to run it within the Solr search engine.

**Querqy is now available for Solr/Lucene 8.x. You will have to re-test your search result orders due to changes in Lucene if you are migrating from an earlier Solr/Lucene version.** See [Release Notes](https://github.com/renekrie/querqy/wiki#15-june-2019---querqy-44lucene8000) for details.

**NEW: There is now a web UI for managing common query rewriting rules with Querqy. Make sure you check out [SMUI](https://github.com/pbartusch/smui)!**


## Getting started: setting up Common Rules under Solr

### Getting Querqy and deploying it to Solr
Starting from Querqy v4 / Solr/Lucene 7 the naming scheme is ``<Querqy major.minor version>.<Lucene/Solr version>.<bugfix version>``.

Detailed Solr version mapping:


|Solr version|Querqy version    |  |
|----|-----------|-------------|
|8.3.x| 4.5.lucene810.x||
|8.2.x| 4.5.lucene810.x||
|8.1.x| 4.5.lucene810.x|Please check out the [Release Notes from 5 August 2019 and later](https://github.com/renekrie/querqy/wiki#5-august-2019---querqy-44lucene8101-and-44lucene8001).|
|8.0.0| 4.5.lucene800.x|Please check out the [Release Notes](https://github.com/renekrie/querqy/wiki#5-august-2019---querqy-44lucene8101-and-44lucene8001). Many thanks to [Martin Grigorov](https://github.com/martin-g) for major contributions|
|7.7.0| 4.4.lucene720.x||
|7.6.0| 4.4.lucene720.x||
|7.5.0| 4.4.lucene720.x||
|7.4.0| 4.4.lucene720.x||
|7.3.x| 4.4.lucene720.x||
|7.2.x| 4.4.lucene720.x||
|7.1.0| 4.1.lucene700.x||
|7.0.x| 4.1.lucene700.x|Many thanks to [Matthias Krüger](https://github.com/mkr) for major contributions to Querqy for Solr 7|

For older Solr versions, please see [here](https://github.com/renekrie/querqy/wiki/Older-Querqy-versions).

You can download a .jar file that includes Querqy and all required dependencies from Bintray: [https://bintray.com/renekrie/maven/querqy-for-lucene](https://bintray.com/renekrie/maven/querqy-for-lucene) (Files - querqy/querqy-solr/\<version\>/querqy-solr-\<version\>-jar-with-dependencies.jar) and simply put it into [Solr's lib folder](https://cwiki.apache.org/confluence/display/solr/Lib+Directives+in+SolrConfig).

Alternatively, if you already have a Maven build for your Solr plugins, you can add the artifact 'querqy-solr' as a dependency to your pom.xml:


~~~xml
<!-- Add the Querqy repository URL -->
<repository>
    <id>querqy-repo</id>
    <name>Querqy repo</name>
    <url>http://dl.bintray.com/renekrie/maven</url>
</repository>

<!-- Add the querqy-solr dependency -->
<dependencies>
	<dependency>
		<groupId>querqy</groupId>
		<artifactId>querqy-solr</artifactId>
		<version>...</version>
	</dependency>
</dependencies>
     
~~~

### Configuring Solr for Querqy
Querqy provides a [QParserPlugin](http://lucene.apache.org/solr/5_5_0/solr-core/org/apache/solr/search/QParserPlugin.html) and a [search component](https://cwiki.apache.org/confluence/display/solr/RequestHandlers+and+SearchComponents+in+SolrConfig) that need to be configured in file [solrconfig.xml](https://cwiki.apache.org/confluence/display/solr/Configuring+solrconfig.xml) of your Solr core:

~~~xml
<!-- 
    Add the Querqy query parser. 
 -->
<queryParser name="querqy" class="querqy.solr.DefaultQuerqyDismaxQParserPlugin">

    <!--
        Querqy has to parse the user's query text into a query object.
        We use WhiteSpaceQuerqyParser, which only provides a very
        limited syntax (no field names, just -/+ as boolean
        operators). 
        
        Note that the Querqy query parser must not be confused with
        Solr or Lucene query parsers: it is completely independent
        from Lucene/Solr and parses the input into Querqy's internal
        query object model.
    -->
    <lst name="parser">
      <str name="factory">querqy.solr.SimpleQuerqyQParserFactory</str>
      <!-- 
        The parser is provided by a factory, in our case
        by a SimpleQuerqyQParserFactory, which is a very generic 
        factory that just creates an instance for the configured class:
      -->
      <str name="class">querqy.parser.WhiteSpaceQuerqyParser</str>
    </lst>
     	 
	
	<!--
		Define a chain of query rewriters. We'll use just one rewriter
		- SimpleCommonRulesRewriter - which provides 'Common Rules'
		preprocessing.
    --> 
    <lst name="rewriteChain">
    
        <lst name="rewriter">
            <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
            <!-- 
           	   The file that contains rules for synonyms, 
           	   boosting etc.
            -->
            <str name="rules">rules.txt</str>
            <!--
           	   If true, case will be ignored while trying to find
           	   rules that match the user query input: 
            -->
            <bool name="ignoreCase">true</bool>
            <!-- 
                Some rules in the rules file declare boost queries,
                synonym queries or filter queries that need to be added
                to the user query. This query parser parses the
                additional queries from the rules file:
            -->
            <str name="querqyParser">querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory</str>
        </lst>
       
        <!--
            You can add further rewriters to the chain. For example, 
            you could add a second SimpleCommonRulesRewriter for
            a different group of rules, which would consume the 
            output of the first rewriter. Or you might add a completely
            different rewriter imlementation, like the ShingleRewriter,
            that would combine pairs of tokens of the query input and
            add the concatenated forms as synonyms.
        -->
        <!--
        <lst name="rewriter">
            <str name="class">querqy.solr.contrib.ShingleRewriterFactory</str>
            <bool name="acceptGeneratedTerms">false</bool>
        </lst>
        -->
       
   </lst>
     	 
</queryParser>

<!-- Override the default QueryComponent -->
<searchComponent name="query" class="querqy.solr.QuerqyQueryComponent"/>

~~~

Also see [Advanced configuration: caching](#advanced-configuration-caching).

### Making requests to Solr using Querqy
You can activate the Querqy query parser in Solr by setting the defType request parameter - in other words, just like you would enable any other query parser in a Solr search request:

~~~
defType=querqy
~~~

Alternatively, you can activate and control Querqy using [local parameters](https://cwiki.apache.org/confluence/display/solr/Local+Parameters+in+Queries).

You'll have to set further parameters for Querqy to process the query. These parameters are exactly the same like for the [Extended DisMax Query Parser](https://cwiki.apache.org/confluence/display/solr/The+Extended+DisMax+Query+Parser), including [DisMax parameters](https://cwiki.apache.org/confluence/display/solr/The+DisMax+Query+Parser), with the following exceptions:

-  ``q.alt`` - not implemented yet
-  ``uf, lowercaseOperators`` and query field aliasing - not implemented. Work on this will depend on the availability of a Querqy-internal query parser that accepts field names and boolean (non-prefix) operators. The currently recommended parser, the querqy.parser.WhiteSpaceQuerqyParser, does not provide these features, though field names and boolean operators are part of Querqy's internal query object model.
-  ``stopwords`` - no plans to implement


Example:

~~~
q=personal computer&defType=querqy&qf=name^2.0 description^0.5&pf=name
~~~

With the exception of the defType parameter this query looks like a standard ExtendedDisMax query, and if you haven't configured any rules for the query 'personal computer', the results and their order would be the same like for ExtendedDisMax. If, on the other hand, you have configured a rule

~~~
personal computer =>
    SYNONYM: pc
~~~
Querqy would also search for 'pc' in the 'name' and 'description' fields. You'll learn how to write such rules in the next section.

Querqy has the following optional parameters in addition to those shared with the ExtendedDisMax query parser (you can savely skip this list for the moment):

|Name|Meaning    |Value  |Example|Default value|
|----|-----------|-------------|-------|-------------|
|`gqf` |"generated query fields" - where to query generated terms like synonyms, boost queries etc.|space-separated list of field names and boost factors|`gqf=name^1.1 color^0.9`|use values from param `qf`|
|`gfb`|"generated field boost" - a global boost factor that is multiplied with field-specific boosts of generated fields (use this to quickly give a lower boost to all generated terms and queries) |decimal number (float)|`gfb=0.8`|1.0

### Configuring rules

The rules for the 'Common Rules Rewriter' are maintained in the file that you configured as attribute 'rules' for the SimpleCommonRulesRewriterFactory, i.e. file rules.txt in the following example configuration:

~~~xml

<queryParser name="querqy" class="querqy.solr.DefaultQuerqyDismaxQParserPlugin">

    <lst name="rewriteChain">
    
        <lst name="rewriter">
            <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
            <!-- 
           	   The file that contains rules for synonyms, 
           	   boosting etc.
            -->
            <str name="rules">rules.txt</str>
~~~

Note that the expected character encoding is UTF-8 and that the maximum size of this file is 1 MB if Solr runs as SolrCloud and if you didn't change the maximum file size in Zookeeper (see [this issue](https://github.com/renekrie/querqy/issues/14) on GitHub).


#### Input matching
The first line of a rule declaration defines the matching criteria for the input query. This line must end in an arrow (`=>`). The next line defines an instruction that shall be applied if the input matches. The same input line can be used for multiple instructions, one per line:

~~~
# if the input contains 'personal computer', add two synonyms, 'pc' and
# 'desktop computer', and rank down by factor 50 documents that 
# match 'software':
personal computer =>
    SYNONYM: pc
    SYNONYM: desktop computer
    DOWN(50): software
~~~

Querqy applies the above rule if it can find the matching criteria 'personal computer' anywhere in the query, provided that there is no other term between 'personal' and 'computer'. It would thus also match the input 'cheap personal computer'. If you want to match the input exactly, or at the beginning or end of the input, you have to mark the input boundaries using double quotation marks:

~~~
# only match the exact query 'personal computer'.
"personal computer" => 
    ....
    
# only match queries starting with 'personal computer'
"personal computer =>
    ....

# only match queries ending with 'personal computer'
personal computer" =>
    ....

~~~

Each input token is matched exactly. Matching is even case-sensitive, but you can make it case-insensitive in the configuration:

~~~xml

<lst name="rewriter">
            <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
                        <str name="rules">rules.txt</str>
            <!--
           	   If true, case will be ignored while trying to find
           	   rules that match the user query input: 
            -->
            <bool name="ignoreCase">true</bool>


~~~


There is no stemming or fuzzy matching applied to the input. If you want to make 'pc' a synonym for both, 'personal computer' and 'personal computer*s*', you will have to declare two rules:

~~~
personal computer =>
    SYNONYM: pc

personal computers =>
    SYNONYM: pc


~~~ 


You can use a wildcard at the very end of the input declaration:

~~~
sofa* =>
    SYNONYM: sofa $1

~~~ 

The above rule matches if the input contains a token that starts with 'sofa-' and adds a synonym 'sofa + *wildcard matching string*' to the query. For example, a user query 'sofabed' would yield the synonym 'sofa bed'.

The wildcard matches 1 (!) or more characters. It is not intended as a replacement for stemming but to provide some support for decompounding in languages like German where compounding is very productive. For example, compounds of the structure 'material + product type' and 'intended audience + product type' are very common in German. Wildcards in Querqy can help to decompound them and allow to search the components accross multiple fields:

~~~
# match queries like 'kinderschuhe' (= kids' shoes) and 
# 'kinderjacke' (= kids' jacket) and search for 
# 'kinder schuhe'/'kinder jacke' etc. in all search fields
kinder* =>
	SYNONYM: kinder $1

~~~

Wildcard matching can be used for all rule types. There are some restrictions in the current wildcard implementation, which might be removed in the future: 

  - Synonyms and boostings (UP/DOWN) are the only rule types that can pick up the '$1' placeholder.
  - The wildcard can only occur at the very end of the input matching.
  - It cannot be combined with the right-hand input boundary marker (...").
   

#### SYNONYM rules

Querqy gives you a mighty toolset for using synonyms at query time. As opposed to analysis-based query-time synonyms in Solr, Querqy matches multi-term input and avoids scoring issues related to different document frequencies of the original input and synonym terms (see [this blog post](http://opensourceconnections.com/blog/2013/10/27/why-is-multi-term-synonyms-so-hard-in-solr/) and the [discussion on index-time vs. query-time synonyms in the Solr wiki](https://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.SynonymFilterFactory)). It also allows to configure synonyms in a field-independent manner, making the maintenance of synonyms a lot more intuitive than in Solr.

You have already seen rules for synonyms:

~~~
personal computer =>
    SYNONYM: pc
    
sofa* =>
    SYNONYM: sofa $1
~~~

Synonyms work in only one direction in Querqy. It always tries to match the input that is specified in the rule and adds a synonym if a given user query matches this input. If you need bi-directional synonyms or synonym groups, you have to declare a rule for each direction. For example, if the query 'personal computer' should also search for 'pc' while query 'pc' should also search for 'personal computer', you would write these two rules:

~~~
personal computer =>
    SYNONYM: pc

pc =>
	SYNONYM: personal computer
~~~

##### The right-hand side of synonym rules

The right-hand side of the synonym expression will be parsed by the parser that you configured as `queryParser` for the Common Rules rewriter:

~~~xml
<lst name="rewriteChain">
    
        <lst name="rewriter">
            <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
            <str name="rules">rules.txt</str>
            ..
            <str name="querqyParser">querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory</str>
        </lst>

~~~

Thus, in the following example, the `WhiteSpaceQuerqyParser` is used to parse "personal computer" into Querqy's internal query object model:

~~~
pc =>
	SYNONYM: personal computer
~~~


Querqy will assign fields in which it searches for the synonym query only after applying all rules and all rewriters when it finally creates a Lucene query from the Querqy-internal query object model. The search fields for synonyms are taken from the `gqf` parameter (priority) or from `qf` (see [request parameters](#making-requests-to-solr-using-querqy)).

##### Expert: Structure of expanded queries

Querqy preserves the 'minimum should match' semantics for boolean queries (parameter `mm` for the DisMax query parser) when constructing synonyms. In order to provide this semantics, given mm=1, the rule

~~~
personal computer =>
    SYNONYM: pc
~~~

produces the query 

~~~
boolean_query (mm=1) (
	dismax('personal','pc'),
	dismax('computer','pc')
)
~~~

and *NOT*

~~~
boolean_query(mm=??) (
	boolean_query(mm=1) (
		dismax('personal'),
		dismax('computer')
	),
	dismax('pc')
)
~~~

#### UP/DOWN rules

UP and DOWN rules add a positive or negative boost query to the user query, which helps to bring documents that match the boost query further up or down in the result list.

The following rules add UP and DOWN queries to the input query 'iphone'. The UP instruction promotes documents also containing 'apple' further to the top of the result list, while the DOWN query puts documents containing 'case' further down the search results:

~~~
iphone =>
	UP(10): apple
	DOWN(20): case

~~~

UP and DOWN both take boost factors as parameters. The default boost factor is 1.0. The interpretation of the boost factor is left to the search engine and it might differ between UP and DOWN, which means that UP(10):x and DOWN(10):x do not necessarily equal out each other.

The right-hand side of UP and DOWN instructions will either be parsed using the configured query parser (see [The right-hand side of synonym rules](#the-right-hand-side-of-synonym-rules)), or it will be treated as a query in the syntax of the search engine if the right-hand-side of the query is prefixed by `*`.

In the following example we favour a certain price range as an interpretation of 'cheap' and penalise documents from category 'accessories' using raw Solr queries:


~~~
cheap notebook =>
	UP(10): * price:[350 TO 450]
	DOWN(20): * category:accessories

~~~

#### FILTER rules
Filter rules work similar to UP and DOWN rules, but instead of moving search results up or down the result list they restrict search results to those that match the filter query. The following rule looks similar to the 'iphone' example above but it restricts the search results to documents that contain 'apple' and not 'case':

~~~
iphone =>
	FILTER: apple
	FILTER: -case
~~~

The filter is applied to all fields given in the `gqf` or `qf` parameters. In the case of a required keyword ('apple') the filter matches if the keyword occurs in one or more query fields. The negative filter ('-case') only matches documents where the keyword occurs in none of the query fields. (Note [this issue](https://github.com/renekrie/querqy/issues/16) for purely negative queries.)

The right-hand side of filter instructions accepts raw queries. To completely exclude results from category 'accessories' for query 'notebook' you would write:

~~~
notebook =>
	FILTER: * -category:accessories

~~~
   
#### DELETE rules

Delete rules allow you to remove keywords from a query. This is comparable to stopwords in Solr but in Querqy keywords are removed before starting the field analysis chain. Delete rules are thus field-independent. It is also possible to apply delete rules before all other rules (see [Rule ordering](#rule-ordering)), which helps to remove stopwords that could otherwise prevent further Querqy rules from matching.  

The following rule declares that whenever Querqy sees the input 'cheap iphone' it should remove keyword 'cheap' from the query and only search for 'iphone':

~~~
cheap iphone =>
	DELETE: cheap
~~~

While in this example the keyword 'cheap' will only be deleted if it is followed by 'iphone', you can also delete keywords regardless of the context:

~~~
cheap =>
	DELETE: cheap
~~~

or simply:

~~~
cheap =>
	DELETE
~~~

If the right-hand side of the delete instruction contains more than one term, each term will be removed from the query individually (= they are not considered a phrase and further terms can occur between them):

~~~
cheap iphone unlocked =>
	DELETE: cheap unlocked
~~~	

The above rule would turn the input query 'cheap iphone unlocked' into search query 'iphone'.

The following restrictions apply to delete rules:

   - Terms to be deleted must be part of the input declaration.
   - Querqy will not delete the only term in a query.

#### DECORATE rules

Decorate rules are not strictly query rewriting rules but they are quite handy to add query-dependent information to search results. For example, in online shops there are almost always a few search queries that have nothing to do with the products in the shop but with deliveries, T&C, FAQs and other service information. A decorate rule matches those search terms and adds the configured information to the search results:

~~~
faq =>
	DECORATE: redirect, /service/faq

~~~

The Solr response will then contain an array 'querqy_decorations' with the right-hand side expressions of the matching decorate rules:

~~~xml
<response>
    <lst name="responseHeader">...</lst>
    <result name="response" numFound="0" start="0">...</result>
    <lst name="facet_counts">...</lst>
    <arr name="querqy_decorations">
        <str>redirect, /service/faq</str>
        ...
    </arr>
</response>

~~~

Querqy does not inspect the right-hand side of the decorate instruction ('redirect, /service/faq') but returns the configured value 'as is'. You could even configure a JSON-formatted value in this place but you have to assure that the value does not contain any line break.

#### Rule properties, rule ordering and rule filtering
(from version Querqy 4.4.lucene720.0, Querqy core 3.3.0)

##### Defining properties
Properties can be defined for each set of rules that share an input declaration (```input =>```) in the rules.txt file. These properties are used in a number of features, such as rule selection and ordering and [info logging](#advanced-configuration-info-logging).

There are two ways to define properties in the rules.txt file, both using the `@` character. The first syntax declares one property per line:

~~~
notebook =>
    SYNONYM: laptop
    DOWN(100): case
    @_id: "ID1"
    @_log: "notebook,modified 2019-04-03"
    @group: "electronics"
    @enabled: true
    @priority: 100
    @tenant: ["t1", "t2", "t3"]
    @culture: {"lang": "en", "country": ["gb", "us"]}

~~~

The format of this syntax is 

~~~
 @<property name>: <property value> (on a single line)
~~~

where the property value allows all value formats that are known in JSON. 

The second format represents the properties as a JSON object. It can span over multiple lines. The beginning is marked by ```@{``` and the end by ```}@```:

~~~
notebook =>
    SYNONYM: laptop
    DOWN(100): case
    @{
        _id: "ID1",
        _log: "notebook,modified 2019-04-03",
        group: "electronics",
        enabled: true,
        priority: 100,
        tenant: ["t1", 	"t2", "t3"],
        culture: {
            "lang": "en", 
            "country": ["gb", "us"]
        }
    }@

~~~ 

Property names can also be put in quotes. Both, single quotes and double quotes, can be used.

Property names starting with ```_``` (such as ```_id``` and ```_log```) have a specific meaning in Querqy (see for example: [Advanced configuration: Info Logging](#advanced-configuration-info-logging))

Both formats can be mixed, however, the JSON object format must be used only once per input declaration:

~~~
notebook =>
    SYNONYM: laptop
    DOWN(100): case
    @_id: "ID1"
    @_log: "notebook,modified 2019-04-03"
    @enabled: true
    @{
        group: "electronics",
        priority: 100,
        tenant: ["t1", 	"t2", "t3"],
        culture: {
            "lang": "en", 
            "country": ["gb", "us"]
        }
    }@

~~~ 

##### Using properties for rule ordering and selection

We will use the following example to explain the use of properties for rule ordering and selection:

~~~

notebook =>
    DOWN(100): bag
    @enabled: false
    @{
        _id: "ID1",
        priority: 5,
        group: "electronics",
        tenant: ["t1", "t3"]
    }@
    
    
notebook bag =>
    UP(100): backbag
    @enabled: true
    @{
        _id: "ID2",
        priority: 10,
        group: "accessories in electronics",
        tenant: ["t2", "t3"],
        culture: {
            "lang": "en", 
            "country": ["gb", "us"]
        }
    }@
~~~ 

The first rule ('ID1') defines a down boost for all documents containing 'bag' if the query contains 'notebook'. This makes sense as users probably are less interested in notebook bags when they search for a notebook. Except, if they search for 'notebook bag' - in this case we would not want to apply rule ID1. Properties will help us solve this problem by ordering and selecting rules depending on the context.

In order to enable rule selection and ordering we have to define a SelectionStrategy for the Common Rules rewriter in solrconfig.xml:

~~~xml

<queryParser name="querqy" class="querqy.solr.DefaultQuerqyDismaxQParserPlugin">
	
	<lst name="rewriteChain">
            <lst name="rewriter">
<!-- 
	Note the rewriter ID: 
-->            
                <str name="id">common1</str>
                <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
                <str name="rules">rules.txt</str>
                <!-- ... -->
                           
<!--
   Define a selection strategy, named 'expr': 
 -->                  
                <lst name="rules.selectionStrategy">
                    <lst name="strategy">
                        <str name="id">expr</str>
<!-- 
	This selection strategy implementation allows us to select and order rules by properties: 
-->                        
                        <str name="class">querqy.solr.ExpressionSelectionStrategyFactory</str>
                    </lst>
                 </lst>
            </lst>
     	 </lst>  

~~~

We can now order the rules by the value of the ```priority``` property in descending order and tell Querqy that it should only apply the rule with the highest ```priority``` using the following request parameters:

~~~
querqy.common1.criteria.strategy=expr
querqy.common1.criteria.sort=priority desc
querqy.common1.criteria.limit=1
~~~

These parameters have a common prefix ```querqy.common1.criteria``` in which ```common1``` matches the rewriter ID that was configured in solrconfig.xml. This allows us to scope the rule selection and ordering per rewriter.  The ```.strategy``` parameter selects the strategy ```expr``` that was defined in solrconfig.xml. This strategy interprets ```.sort=priority desc``` so that rules are ordered by property ```priority``` in descending order and then only the top rule will be applied because of (```.limit=1```).

In the example, both rules match the input 'notebook bag'. Sorting them by descending value of the ```priority``` property puts the rule with 'ID2' first and the rule with 'ID1' second, and finally only rule 'ID1' will be selected due to limit=1.

Rules can also be filtered by properties using [JsonPath](https://github.com/json-path/JsonPath) expressions, where the general parameter syntax is:

~~~
querqy.common1.criteria.filter=<JsonPath expression>
~~~

The properties that where defined for a single set of rules are considered a JSON document and a rule filter matches the rule if the JsonPath expression matches this JSON document. What follows is a list of examples that relate to the above rules:

 - `$[?(@.enabled == true)]` matches ID2 but not ID1
 - `$[?(@.group == 'electronics')]` matches ID1 but not ID2
 - `$[?(@.priority > 5)]` matches ID2 but not ID1 
 - `$[?('t1' in @.tenant)]` matches ID1 but not ID2 
 - `$[?(@.priority > 1 && @.culture)].culture[?(@.lang=='en)]` matches ID2 but not ID1. The expression ```[?(@.priority > 1 && @.culture)]``` first tests for priority > 1 (both rules matching) and then for the existence of 'culture', which only matches ID2. Then the 'lang' property of 'culture' is tested for matching 'en'.

If more than one filter parameters are passed to a Common Rules rewriter, a rule must match all filters before it will be applied. 

#### Rewriter ordering

While properties can be used to define the ordering and selection of rules within a Common Rules rewriter, the rewrite chain defines the overall order of the query rewriters. Thus, the order of the rewriters will have priority over the order of rules within Common Rules rewriters.

When a rewriter sees a query it tries to find all operations that match the input, possibly - like in the case of the Common Rules rewriter - orders and selects these operations and finally applies them. The output of the first rewriter becomes the input of the second rewriter, which will then try to find all applicable rewrite operations, rewrite the query and so on until the last rewriter of the chain has been applied.

For Common Rules rewriters it is often useful to split rules across multiple rewriters, each with its own rules file. 

For example, it seems handy to first apply delete rules before applying further rule types:

~~~xml

<queryParser name="querqy" class="querqy.solr.DefaultQuerqyDismaxQParserPlugin">

    <lst name="parser">
      <str name="factory">querqy.solr.SimpleQuerqyQParserFactory</str>
      <str name="class">querqy.parser.WhiteSpaceQuerqyParser</str>
    </lst>
     	 	
	<!--
		The chain of query rewriters.
    --> 
    <lst name="rewriteChain">
    
        <lst name="rewriter">
            <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
            <!-- 
           	   The file only contains delete rules.
            -->
            <str name="rules">delete-rules.txt</str>
            <bool name="ignoreCase">true</bool>
            <str name="querqyParser">querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory</str>
        </lst>
        <!-- 
        	The rewritten query of the above rewriter becomes
        	the input for the rewriter below:
        -->
        <lst name="rewriter">
            <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
            <!-- 
           	   The file only contains further rules (synonyms etc.)
            -->
            <str name="rules">rules.txt</str>
            <bool name="ignoreCase">true</bool>
            <str name="querqyParser">querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory</str>
        </lst>

   </lst>

</queryParser>

~~~

### Advanced configuration: Caching
When you configure rewrite rules for Querqy, in most cases you will not specify field names. For example, you would use a synonym rule to say that if the user enters a query 'personal computer', Solr should also search for 'pc' and Querqy would automatically create field-specific queries like 'name:pc', 'description:pc', 'color:pc' etc. for the right-hand side of the synonym rule. The fields for which Solr creates queries depend on the gqf or qf parameters. On the other hand, it is very unlikely that an input term would have matches in all fields that are given in 'gqf'/'qf'. In the example, it is very unlikely that there would be a document having the term 'pc' in the 'color' field.

You can configure Querqy to check on startup/core reloading/when opening a searcher whether the terms on the right-hand side of the rules have matches in the query fields and cache this information. If there is no document matching the right-hand side term in a given field, the field-specific query will not be executed again until Solr opens a new searcher. Caching this information can speed up Querqy considerably, expecially if there are many query fields.

Cache configuration (solrconfig.xml):

~~~
<query>
   <!-- Place a custom cache in the <query> section: -->
	<cache name="querqyTermQueryCache"
              class="solr.LFUCache"
              size="1024"
              initialSize="1024"
              autowarmCount="0"
              regenerator="solr.NoOpRegenerator"
    />
    
    <!-- 
    	A preloader for the cache, called when Solr is started up or when
    	the core is reloaded.
    -->
    <listener event="firstSearcher" class="querqy.solr.TermQueryCachePreloader">
    		<!-- 
    			The fields for which Querqy pre-checks and caches whether the 
    			right-hand side terms match. Normally the same set of fields like
    			in qf/gqf but you could omit fields that are very quick to query.
    		-->
      		<str name="fields">f1 f2</str>
      		
      		<!-- The name of the configered Querqy QParserPlugin -->
      		<str name="qParserPlugin">querqy</str>
      		
      		<!-- The name of the custom cache -->
      		<str name="cacheName">querqyTermQueryCache</str>
      		
      		<!-- 
      			If false, the preloader would not test for matches of the right-hand side
      			terms but only cache the rewritten (text-analysed) query. This can already
      			save query time if there are many query fields and if the rewritten query 
      			is very complex. You would normally set this to 'true' to completely avoid 
      			executing non-matching term queries later.
      		-->
      		<bool name="testForHits">true</bool>
    </listener>
    	
    <!-- 
    	Same preloader as above but listening to 'newSearcher' events (for example,
    	commits with openSearcher=true)
    -->
    <listener event="newSearcher" class="querqy.solr.TermQueryCachePreloader">
      		<str name="fields">f1 f2</str>
      		<str name="qParserPlugin">querqy</str>
      		<str name="cacheName">querqyTermQueryCache</str>
      		<bool name="testForHits">true</bool>
    </listener>
    	
    	
</query> 


<!-- Tell the Querqy query parser to use the custom cache: -->
<queryParser name="querqy" class="querqy.solr.DefaultQuerqyDismaxQParserPlugin">
	    
	    <!-- 
	          A reference to the custom cache. It must match the 
	          cache name that you have used in the cache definition.
	    --> 
	    <str name="termQueryCache.name">querqyTermQueryCache</str>
	    
	    
	    <!--
	    		If true, the cache will be updated after preloading for terms 
	    		from all user queries, including those that were not rewritten. 
	    		In most cases this should be set to 'false' in order to make sure 
	    		that the information for the right-hand side terms of your rewrite rules 
	    		is never evicted from the cache.
	    -->
	    <bool name="termQueryCache.update">false</bool>
	    
		<lst name="rewriteChain">
           ...
       </lst>    
</queryParser>          
~~~

### Advanced configuration: Info Logging

(from version Querqy 4.4.lucene720.0, Querqy core 3.3.0)

Querqy rewriters can return logging information together with the request
response or send this information to other receivers. Currently only the
common rules rewriter emits this information and it can be returned together
with the Solr response.

Configuration:

~~~xml
<queryParser name="querqy" class="querqy.solr.DefaultQuerqyDismaxQParserPlugin">

		<lst name="rewriteChain">
            <lst name="rewriter">

<!--
    Note the rewriter id 'common1':
-->
                <str name="id">common1</str>
                <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
                <str name="rules">rules1.txt</str>
                <!-- ... -->
            </lst>
            <lst name="rewriter">
                <str name="id">common2</str>
                <str name="class">querqy.solr.SimpleCommonRulesRewriterFactory</str>
                <str name="rules">rules2.txt</str>
                <!-- ... -->
            </lst>
        </lst>
     	<!-- ... -->

        <lst name="infoLogging">
<!--
    Define a 'sink' named 'responseSink' to which the logging information will be sent:
-->

            <lst name="sink">
                <str name="id">responseSink</str>
                <str name="class">querqy.solr.ResponseSink</str>
            </lst>
 <!--
     Direct the logging information from rewriter 'common1' to sink 'responseSink':
 -->
            <lst name="mapping">
                <str name="rewriter">common1</str>
                <str name="sink">responseSink</str>
            </lst>
 <!--
     Direct the logging information from rewriter 'common2' to sink 'responseSink' too:
 -->
            <lst name="mapping">
                <str name="rewriter">common2</str>
                <str name="sink">responseSink</str>
            </lst>

        </lst>

</queryParser>
~~~

The logging output must be enabled per request, using the request parameter ```querqy.infoLogging```:

~~~
querqy.infoLogging=on (default: off)
~~~

This will add a section 'querqy.infoLog' to the Solr response:

~~~xml
<lst name="querqy.infoLog">
    <arr name="common1">
        <lst>
            <arr name="APPLIED_RULES">
                <str>(message for rule 1.1)</str>
                <str>(message for rule 1.2)</str>
            </arr>
        </lst>
    </arr>
    <arr name="common2">
        <lst>
            <arr name="APPLIED_RULES">
                <str>(message for rule 2.1)</str>
                <str>(message for rule 2.2)</str>
            </arr>
        </lst>
    </arr>
</lst>    
~~~
Each rewriter can emit a list of log objects. In this case CommonRulesRewriter 'common1' emitted just a single log object (```lst```), which holds an array ```APPLIED_RULES``` that contains a log message for each rule that the rewriter has applied  (```<str>(message for rule 1.1)</str>``` etc).

The log message can be defined in ```rules.txt``` using the ```_log``` property:

~~~
notebook => 
	SYNONYM: laptop
	DELETE: cheap
	@_id: "ID1"
	@_log: "Log message for notebook"
		
samusng =>
   SYNONYM: samsung
   @{
       "_id": "ID2",
       "_log": "Log message for samusng typo",
        
   }
   
32g =>
  SYNONYM: 32gb
  @_id: "ID3"   

~~~

The query ```samusng notebook 32g``` will now produce the following log output:

~~~xml
<lst name="querqy.infoLog">
    <arr name="common1">
        <lst>
            <arr name="APPLIED_RULES">
                <str>Log message for notebook</str>
                <str>Log message for samusng typo</str>
                <str>ID3</str>
            </arr>
        </lst>
    </arr>
    
</lst>    
~~~

As the third rule doesn't have a ```_log``` property, the ```_id``` property will be used as the message. If both, the ```_log```and the ```_id``` property are missing, a default message will be created based on the input expression of the rule and a rule counter (```samusng#1```, ```32g#2``` etc.) 

Custom info logging sinks can be created by implementing the ```querqy.infologging.Sink``` interface.   


## License
Querqy is licensed under the [Apache License, Version 2](http://www.apache.org/licenses/LICENSE-2.0.html).

## Development

### Modules

 - `querqy-antlr` - An [ANTLR-based](http://www.antlr.org/) Querqy query parser (incomplete, do not use)
 - `querqy-core` - The core component. Search-engine independent, Querqy's query object model, Common Rules Rewriter
 - `querqy-for-lucene/querqy-lucene` - Lucene-specific components. Builder for creating a Lucene query from Querqy's query object model
 - `querqy-for-lucene/querqy-solr` - Solr-specific components. QParserPlugin, SearchComponent.

 querqy-core and querqy-for-lucene are released separately and version numbers will diverge.

### Branches
Please base development on the branch for the corresponding Solr version. querqy-core development should be based on the branch for the latest Solr version.


### Contributors

 - [Anton Dumler](https://github.com/jagile)
 - [Johannes Peter](https://github.com/JohannesDaniel)
 - [Lucky Sharma](https://github.com/MighTguY)
 - [Markus Heiden](https://github.com/markus-s24)
 - [Markus Müllenborn](https://github.com/muellenborn)
 - [Martin Grigorov](https://github.com/martin-g)
 - [Martin Grotzke](https://github.com/magro)
 - [Matthias Krüger](https://github.com/mkr), Committer
 - [René Kriegler](https://github.com/renekrie), Committer/Maintainer
 - [Robert Giacinto](https://github.com/lichtsprung)
 - [Thomas Wünsche](https://github.com/bzrk)
 - [Tobias Kässmann](https://github.com/tkaessmann)
 - [Torsten Bøgh Köster](https://github.com/tboeghk)

Many thanks to [Galeria Kaufhof](https://github.com/Galeria-Kaufhof), [shopping24](https://github.com/shopping24/) and [inoio](https://github.com/inoio) for their support.

[Querqy is built using Travis CI](https://travis-ci.org/renekrie/querqy).



