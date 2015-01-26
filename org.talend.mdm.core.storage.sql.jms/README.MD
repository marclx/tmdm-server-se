# Full text JMS Module

This project allows setting up a shared full text index Lucene. It consists in:

* A set of slave nodes that pushes changes to a JMS queue, and periodically fetch a copy the master index.
* A master node that reads the messages pushed by slave nodes

The new following sections present a setup example.

## Install MDM cluster

Set up nodes:

* Install MDM server node1 (port 8180)
* Install MDM server node1 (port 8280)
* Each MDM Server has its *own* full text directory (no share at this point).
* Start both nodes and checks initialization is ok.
* Stop MDM instances

## Description and options
Each MDM node should work on its own index directory: in a slow paced environment, 2 MDM nodes can accommodate
a shared directory. But when you start stressing the nodes, lock issues appears as Lucene tries to
concurrently access and modify the indexes. Hibernate Search (the extension of Hibernate used in MDM to serve
full text queries) provides support for distributed indexes. From documentation of Hibernate Search 3.2.1:

```
Out of the box support for the Apache Lucene back end and the JMS back end. Default to lucene. Supports also jms, blackhole, jgroupsMaster and jgroupsSlave.
```

Available options are:

* lucene (<- the one used by default)
* jms
* blackhole: not very interesting (according to javadoc: "This backend does not do anything: the Documents are not sent to any index but are discarded.").
* jgroups

This leave 2 options for distributed indexes:

* jms
* jgroups

This setup only covers the "jms" option, as configuring "jgroups" requires heavy JBoss configuration that may have
bigger impacts than "jms" option ("jgroups" implies to enable JBoss in cluster mode).

## Set up JMS-based replication

This solution uses the JBoss JMS of the Master MDM server.

### In MASTER

* Add a queue "jms/mdm/fulltext"
Adds following content in jbossmq-destinations-service.xml (.../server/default/deploy/jms):

```
  <mbean code="org.jboss.mq.server.jmx.Queue"
	 name="jboss.mq.destination:service=Queue,name=jms/mdm/fulltext">
    <depends optional-attribute-name="DestinationManager">jboss.mq:service=DestinationManager</depends>
  </mbean>
```

* Restart MDM Server, look for line:

```
2015-01-20 11:18:16,810 INFO  [org.jboss.mq.server.jmx.Queue.jms/mdm/fulltext] Bound to JNDI name: queue/jms/mdm/fulltext
```

* In tem.ear (jboss_dir/server/default/deploy):
** Add jar "org.talend.mdm.storage.sql.jms-5.6-SNAPSHOT.jar" to tem.ear: this jar contains the JMS consumer that will receive updates from slave nodes.
** Edit META-INF/application.xml: add the following module:

```
  <module>
    <ejb>org.talend.mdm.storage.sql.jms-5.6-SNAPSHOT.jar</ejb>
  </module>
```

* Restart MDM Server, look for line:

```
"2015-01-20 13:16:05,781 INFO  [org.jboss.ejb.EjbModule] Deploying JMSFullTextController"
"... Enabled JMS sharing for full text indexes."
```

* Modify datasources.xml (jboss_dir/server/default/conf):

```
<property name="hibernate.show_sql">false</property>
<property name="hibernate.search.default.sourceBase">/home/fhuaulme/tmp/Talend-MDMServer-cluster/data/indexes/MySQL-Default</property>
<property name="hibernate.search.default.indexBase">/home/fhuaulme/tmp/Talend-MDMServer-20141207_1530-V5.6.1_node1/jboss-4.2.2.GA/server/default/data/indexes/MySQL-Default</property>
<property name="hibernate.search.default.refresh">15</property>
<property name="hibernate.search.default.directory_provider">org.hibernate.search.store.FSMasterDirectoryProvider</property>
```

### In SLAVE

* Modify datasources.xml (jboss_dir/server/default/conf):

```
<property name="hibernate.show_sql">false</property>
<property name="hibernate.search.default.sourceBase">/home/fhuaulme/tmp/Talend-MDMServer-cluster/data/indexes/MySQL-Default</property>
<property name="hibernate.search.default.indexBase">/home/fhuaulme/tmp/Talend-MDMServer-20141207_1530-V5.6.1_node2/jboss-4.2.2.GA/server/default/data/indexes/MySQL-Default</property>
<property name="hibernate.search.default.refresh">1800</property>
<property name="hibernate.search.default.directory_provider">org.hibernate.search.store.FSSlaveDirectoryProvider</property>
<property name="hibernate.search.worker.backend">jms</property>
<property name="hibernate.search.worker.jms.connection_factory">/ConnectionFactory</property>
<property name="hibernate.search.worker.jndi.java.naming.provider.url">jnp://localhost:1199</property> <!-- (1) -->
```

In (1), this is the RMI port of the *master* server.