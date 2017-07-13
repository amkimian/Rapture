# RestServer

### Overview ###
This is a Rapture application that exposes various document, blob, and series Rapture API calls using a REST interface.  It listens on port 4567.

### System Diagram ###
![System Diagram](/Apps/RestServer/images/restserver.png)

# Installation and Running #

## Using Docker ##
The entire setup can be run using all docker containers.  This is the easiest way to run the stack without doing builds or compiling.  Here are the relevant commands.

**Start RabbitMQ**
```
docker run -d -p 5672:5672 -p 15672:15672 --name rabbit incapture/rabbit
```
**Start MongoDB**
```
docker run -d -p 27017:27017 --name mongo incapture/mongo
```
**Start ElasticSearch**
```
docker run -d -p 9300:9300 -p 9200:9200 --name elasticsearch incapture/elasticsearch
```
**Start Postgres**
```
docker run -d -p 5432:5432 --name postgres incapture/postgres
```
**Start Curtis**
```
docker run -d -p 8080:8080 -p 8665:8665 --link mongo --link rabbit --link elasticsearch --link postgres --name curtis incapture/apiserver
```
**Start Rapture UI**
```
docker run -d -p 8000:8000 --link curtis --name rim incapture/rim
```
**Start RestServer**
```
docker run -d -p 4567:4567 --link mongo --link rabbit --link elasticsearch --link postgres --name restserver incapture/restserver
```
The RestServer REST API will be available at [https://localhost:4567](https://localhost:4567) or https://192.168.99.100:4567 or equivalent depending on your docker configuration (native or virtual).
The Rapture UI web application will be available at [http://localhost:8000](http://localhost:8000) or http://192.168.99.100:8000 or equivalent depending on your docker configuration (native or virtual).

# Obtaining an API key #
An API key is required to access the REST API.  You can obtain an api key for this 'restserver' application by running the following in a Rapture REPL screen which can be accessed in the Rapture UI application
```
println(#user.addApiKey("restserver"));
```
All REST API calls must include the api key in a HTTP header 'x-api-key'.
You can also obtain an api key by making a direct call to the Rapture User API using the same function in any of the supported languages.

# REST API #
* [Document](#document)
* [Blob](#blob) 
* [Series](#series)
* [Workflow](#workflow)
* [StructuredStore](#sstore)

<a name="document"/>
**Create a document repo**
```
POST /doc/:authority
Example: /doc/mydocs
Body: {"config":"NREP USING MONGODB {prefix=\"mydocs\"}"}
```
Response:
```
HTTP 200: document://mydocs
```
**Create/Update a document**
```
PUT /doc/:uri
Example: /doc/mydocs/a
Body: {"a":"b"}
```
Response:
```
HTTP 200: {"a":"b"}
```
**Get a document**
```
GET /doc/:uri
Example: /doc/mydocs/a
```
Response:
```
{"a":"b"}
```
**Get a document's meta information**
```
GET /doc/:uri?meta=true
Example: /doc/mydocs/a?meta=true
```
Response:
```
{"displayName":"a","metaData":{"version":2,"createdTimestamp":1471973694426,"modifiedTimestamp":1471973722727,"user":"rapture","comment":"","deleted":false,"tags":{},"semanticUri":""},"content":"{\"a\":\"b\"}"}
```
**Get a document using versions**
```
GET /doc/:uri
Example: /doc/mydocs/a@2
```
Response:
```
{"a":"b"}
```
**Delete a document**
```
DELETE /doc/:uri
Example: /doc/mydocs/a
```
Response:
```
HTTP 200: true
```
**Delete a document repo**
```
DELETE /doc/:authority
Example: /doc/mydocs
```
Response:
```
HTTP 200: true
```
<a name="blob"/>
**Create a blob repo**
```
POST /blob/:authority
Example: /blob/myblobs
Body: {"config":"BLOB USING MONGODB {prefix=\"mydocs\"}", "metaConfig":"REP USING MONGODB {prefix=\"mydocs\"}"}
```
Response:
```
HTTP 200: blob://myblobs
```
**Create/Update a blob**
```
PUT /blob/:uri
Content-Type: :mime-type
Example: /blob/myblobs/a
Content-Type: text/plain
Body: some text value
```
Response:
```
HTTP 200: blob://myblobs/a
```
**Get a blob**
```
GET /blob/:uri
Example: /blob/myblobs/a
```
Response:
```
{"headers":{"Content-Length":"14","Content-Type":"text/plain"},"content":[97,32,115,116,114,105,110,103,32,118,97,108,117,101]}
```
**Delete a blob**
```
DELETE /blob/:uri
Example: /blob/myblobs/a
```
Response:
```
HTTP 200: true
```
**Delete a blob repo**
```
DELETE /blob/:authority
Example: /blob/myblobs
```
Response:
```
HTTP 200: true
```
<a name="series"/>
**Create a series repo**
```
POST /series/:authority
Example: /series/myseries
Body: {"config":"SREP USING MONGODB {prefix=\"myseries\"}"}
```
Response:
```
HTTP 200: series://myseries
```
**Create/Update a series**
```
PUT /series/:uri
Example: /series/myseries/a
Body: {"keys":["k1", "k2"], "values":["v1", "v2"]}
```
Response:
```
HTTP 200: series://myseries/a
```
**Get a series points**
```
GET /series/:uri
Example: /series/myseries/a
```
Response:
```
[{"column":"k1","value":"v1"},{"column":"k2","value":"v2"}]

```
**Delete a series**
```
DELETE /series/:uri
Example: /series/myseries/a
```
Response:
```
HTTP 200: true
```
**Delete a series repo**
```
DELETE /series/:authority
Example: /series/myseries
```
Response:
```
HTTP 200: true
```
<a name="workflow"/>
**Create a workorder (run a workflow)**
```
POST /workorder/:uri
Example: /workorder/workflows/myworkflow
```
Response:
```
HTTP 200: workorder://1471910400/workflows/myworkflow/WO00000008
```
**Create a workorder (run a workflow) with parameters**
```
POST /workorder/:uri
Example: /workorder/workflows/myworkflow
Body: {"params":{"key1":"value1","key2":"value2"}}
```
Response:
```
HTTP 200: workorder://1471910400/workflows/myworkflow/WO00000009
```

<a name="sstore"/>

**Create a structured store repository**
```
POST /sstore/:authority
Example: /sstore/mysstore
Body: {"config":"STRUCTURED {} USING POSTGRES {}"}
```
Response:
```
HTTP 200: structured://mysstore
```
**Create a structured store table with a defined schema**
```
POST /sstore/:authority/:table
Example: /sstore/mysstore/mytable
Body: {"id":"int","firstname":"varchar(30)","lastname":"varchar(30)","age":"int"}
```
Response:
```
HTTP 200: /mysstore/mytable
```
**Add a row to a structured store table**
```
PUT /sstore/:authority/:table
Example: /mysstore/mytable
Body: {"id":3,"firstname":"jim","lastname":"brown","age":41}
```
Response:
```
HTTP 200: mysstore/mytable
```
**Get all rows from a structured store table**
```
GET /sstore/:authority/:table
Example: 
Body:  _empty_
```
Response: [{row1,row2,row3,row4}]
```
HTTP 200: 
```
**Get all rows from a structured store table with a limit**
```
GET /sstore/:authority/:table?limit=
Example: /sstore/order2/table1?limit=10
Body:  _empty_
```
Response:
```
HTTP 200:[{row1,row2}]
```
**Get specific rows from a structured store table with a limit and a where clause**
```
GET /sstore/:authority/:table?columns=?limit=?&where=?
Example: /sstore/order2/table1?columns=age,firstname&limit=10&where=age>10
Body:  _empty_
```
Response:
```
HTTP 200: [{row1},{row2}]
```
**Get rows from a structured store table with a limit, where clause and order by descending age**
```
GET /sstore/:authority/:table?limit=?&where=?
Example: /sstore/order2/table1?limit=10&where=age>10&order=age&ascending=false
Body:  _empty_
Note: ascending by default = false i.e. results are in descending order 
```
Response:
```
HTTP 200: [{row2},{row1}]
```
**Get rows from a structured store table with raw sql**
```
GET /sstore/:authority/:table?sql=?
Example: /sstore/order2/table1?sql=select * from order2.table1 where age > 1
Body:  _empty_
```
Response:
```
HTTP 200: [{row1,row2}]
```
**Delete rows from a structured store table using where clause**
```
DELETE /sstore/:authority/:table 
Example: /mysstore/mytable
Body: {"where":"age>50"}
```
Response:
```
HTTP 200: 
```
**Delete rows from a structured store table using primary key**
```
DELETE /sstore/:authority/:table/:pkid 
Example: /sstore/order2/table1/age?pkvalue=39
Body: _empty_
```
Response:
```
HTTP 200:
```
**Delete a structured store table**
```
DELETE /sstore/:authority/:table 
Example: /mysstore/mytable
Body: _empty_
```
Response:
```
HTTP 200: 
```
**Delete a structured store repository**
```
DELETE /sstore/:authority
Example: /sstore/mysstore
Body: _empty_
```
Response:
```
HTTP 200:
```
# REST API Examples #
Refer to the integration test [here](src/integrationTest/java/rapture/server/rest)
