<img src="https://github.com/YugaByte/yugabyte-db/raw/master/architecture/images/ybDB_horizontal.jpg" align="center" alt="YugaByte DB"/>

---------------------------------------

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Documentation Status](https://readthedocs.org/projects/ansicolortags/badge/?version=latest)](https://docs.yugabyte.com/)
[![Ask in forum](https://img.shields.io/badge/ask%20us-forum-orange.svg)](https://forum.yugabyte.com/)
[![Slack chat](https://img.shields.io/badge/Slack:-%23yugabyte_db-blueviolet.svg?logo=slack)](https://www.yugabyte.com/slack)
[![Analytics](https://yugabyte.appspot.com/UA-104956980-4/home?pixel&useReferer)](https://github.com/YugaByte/ga-beacon)

- [What is YugaByte DB?](#what-is-yugabyte-db)
- [Get Started](#get-started)
- [Build Apps](#build-apps)
- [Architecture](#architecture)
- [Need Help?](#need-help)
- [Contribute](#contribute)
- [License](#license)
- [Read More](#read-more)

# What is YugaByte DB?

YugaByte DB is a high-performance, cloud-native distributed SQL database. Here are the salient points about it:
* Has a pluggable query layer, and supports two distributed SQL APIs:
    * **[YugaByte SQL (YSQL)](https://docs.yugabyte.com/latest/api/ysql/)** - PostgreSQL-compatible fully relational API
    * **[YugaByte Cloud QL (YCQL)](https://docs.yugabyte.com/latest/api/ycql/)** - Semi-relational SQL-like API with documents/indexing support and Apache Cassandra QL roots 
* Automated sharding, consensus replication and distributed transactions architecture based on the Google Spanner design
* Offers horizontal scalability, strong consistency, high availability
* Extremely resilient - can tolerate disk, node, zone and region failures automatically
* Supports geo-distributed deployments (multi-zone, multi-region, multi-cloud)
* Can be deployed in public clouds and natively inside Kubernetes
* Open source under the [Apache 2.0 license](https://github.com/YugaByte/yugabyte-db/blob/master/LICENSE.md)

Read more about YugaByte DB in our [Docs](https://docs.yugabyte.com/latest/introduction/).

# Get Started

* [Install YugaByte DB](https://docs.yugabyte.com/latest/quick-start/install/)
* [Create a local cluster](https://docs.yugabyte.com/latest/quick-start/create-local-cluster/)
* [Connect and try out SQL commands](https://docs.yugabyte.com/latest/quick-start/explore-ysql/)
* [Build an app](https://docs.yugabyte.com/latest/quick-start/build-apps/) using a PostgreSQL-compatible driver or ORM.
* Try a real-world app:
    * [Microservices-oriented e-commerce app](https://github.com/YugaByte/yugastore-java)
    * [Streaming IoT app with Kafka and Spark Streaming](https://docs.yugabyte.com/latest/develop/realworld-apps/iot-spark-kafka-ksql/)

Cannot find what you are looking for? Have a question? Please post your questions or comments on our Community [Slack](https://www.yugabyte.com/slack) or [Forum](https://forum.yugabyte.com).

# Build Apps

YugaByte DB supports a number of languages and client drivers. Below is a brief list.

| Language  | ORM | YSQL Drivers | YCQL Drivers |
| --------- | --- | ------------ | ------------ |
| Java  | [Spring/Hibernate](https://docs.yugabyte.com/latest/quick-start/build-apps/java/ysql-spring-data/) | [PostgreSQL JDBC](https://docs.yugabyte.com/latest/quick-start/build-apps/java/ysql-jdbc/) | [cassandra-driver-core-yb](https://docs.yugabyte.com/latest/quick-start/build-apps/java/ycql/)
| Go  | [Gorm](https://github.com/YugaByte/orm-examples) | [pq](https://docs.yugabyte.com/latest/quick-start/build-apps/go/#ysql) | [gocql](https://docs.yugabyte.com/latest/quick-start/build-apps/go/#ycql)
| NodeJS  | [Sequelize](https://github.com/YugaByte/orm-examples) | [pg](https://docs.yugabyte.com/latest/quick-start/build-apps/nodejs/#ysql) | [cassandra-driver](https://docs.yugabyte.com/latest/quick-start/build-apps/nodejs/#ycql)
| Python  | [SQLAlchemy](https://github.com/YugaByte/orm-examples) | [psycopg2](https://docs.yugabyte.com/latest/quick-start/build-apps/python/#ysql) | [yb-cassandra-driver](https://docs.yugabyte.com/latest/quick-start/build-apps/python/#ycql)
| Ruby  | [ActiveRecord](https://github.com/YugaByte/orm-examples) | [pg](https://docs.yugabyte.com/latest/quick-start/build-apps/ruby/#ysql) | [yugabyte-ycql-driver](https://docs.yugabyte.com/latest/quick-start/build-apps/ruby/#ycql)
| C#  | Not tested | Not tested | [CassandraCSharpDriver](https://docs.yugabyte.com/latest/quick-start/build-apps/csharp/#ycql)
| C++ | Not tested | [libpqxx](https://docs.yugabyte.com/latest/quick-start/build-apps/cpp/#ysql) | [cassandra-cpp-driver](https://docs.yugabyte.com/latest/quick-start/build-apps/cpp/#ycql)
| C   | Not tested | [libpq](https://docs.yugabyte.com/latest/quick-start/build-apps/c/#ysql) | Not tested

# Architecture

<img src="https://raw.githubusercontent.com/YugaByte/yugabyte-db/master/architecture/images/yb-architecture.png" align="center" alt="YugaByte DB Architecture"/>

Review detailed architecture in our [Docs](https://docs.yugabyte.com/latest/architecture/).

# Need Help?

* You can ask questions, find answers, help others on our Community [Slack](https://www.yugabyte.com/slack) and [Forum](https://forum.yugabyte.com) as well as [Stack Overflow](https://stackoverflow.com/questions/tagged/yugabyte-db)

* Please use [GitHub issues](https://github.com/YugaByte/yugabyte-db/issues) to report issues.

# Contribute

As an open source project with a strong focus on the user community, we welcome contributions as GitHub pull requests. See our [Contributor Guides](https://docs.yugabyte.com/latest/contribute/) to get going. Discussions and RFCs for features happen on the design discussions section of [our Forum](https://forum.yugabyte.com).

# License

Source code in this repository is variously licensed under the Apache License 2.0 and the Polyform Free Trial License 1.0.0. A copy of each license can be found in the [licenses](licenses) directory.

The build produces two sets of binaries:
* The entire database with all its features are licensed under the Apache License 2.0
* The  binaries that contain `-managed` in the artifact and help run a managed service are licensed under the Polyform Free Trial License 1.0.0.

> By default, the build options generate only the Apache License 2.0 binaries.


# Read More

* To see our updates, go to [The Distributed SQL Blog](https://blog.yugabyte.com/).
* See how YugaByte DB [compares with other databases](https://docs.yugabyte.com/latest/comparisons/). 
