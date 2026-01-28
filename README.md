# Java AOT Cache Diagnostics Tool

This is an interactive console to help debug what is happening within and with the [AOT Cache](https://www.youtube.com/watch?v=fiBNDT9r_4I).

This is a work-in-progress and there is no stable interface, commands change as we evolve and use it. Use the `help` command to guide you, don't trust this README blindly.

To be able to run this analyzer, you need to:
1. Train your app to generate an AOT cache and log using the arguments `-XX:AOTCacheOutput=${FOLDER}/app.aot -Xlog:aot+map=trace,aot+map+oops=trace,aot=warning:file=${FOLDER}/aot.map:none:filesize=0 -Xlog:aot+resolve*=trace,aot+codecache+exit=debug:file=${FOLDER}/training.log:level,tags`
1. Run your app to generate a production log using the arguments `-XX:AOTCache=${FOLDER}/app.aot -Xlog:class+load=info,aot+codecache=debug:file=${FOLDER}/production.log:level,tags`
2. Use the previously generated files in `${FOLDER}` on this application as described below

## Running the application

### Pre-packaged files

You can find some already-packaged jar files on the [releases page](https://github.com/Delawen/leyden-analyzer/releases) that you can download and execute with a simple 
```bash
java -jar leyden-analyzer-*-runner.jar
```

### Building and Running

Or if you clone this source code, just `mvn package` and then `java -jar target/quarkus-app/quarkus-run.jar` to run it.

### Using JBang

Or if you have [JBang](https://jbang.dev) installed, just run:

```bash
jbang analyzer@delawen/leyden-analyzer
```

> NB: The analyzer is not published officially, so JBang might not always detect that a new version is available.
In that case run JBang with `--fresh` to force it to download the latest version: `jbang --fresh analyzer@delawen/leyden-analyzer`.
You might need to be patient because it uses [JitPack](https://jitpack.io) to build the tool on demand.

## How to use it

There is a `help` command that is very self-explanatory. Please, use it. 

You can jump to the **[Examples](#examples-of-usage)** if you are in a hurry to quickly answer common questions:
* [Why is this class in my AOT cache?](#why-is-a-class-stored-in-my-AOT-cache)
* [Which classes do this class drag into the cache?](#which-classes-do-a-class-drag-into-the-cache)
* [Why is this class NOT in my AOT cache?](#why-is-a-class-not-in-my-aot-cache)
* [Why is this method not properly trained?](#why-is-a-method-not-properly-trained)
* [Is my application properly trained?](#is-my-application-properly-trained)

But they don't contain all the possibilities this tool offers. 

Really, use the `help` command. Documentation matters.

### Colors

This tool uses a lot of colors to make reading and understanding of the content easier. As a general guide, this is their meaning:

* Green: Good, as expected, you can ignore it.
* Red: Warning, bad, note this.
* Yellow: Type of asset. Typically, it can be Method, Class, TrainingData,...
* Cyan: Identifier for a Class, Method, Warning,...

### Load some information

You should start by using the `load` command to load information from different sources.

Loading an AOT Cache Map File gives a better overview on what the cache contains.

Loading log files gives a better overview of why things are (or are not) in the cache and detect potential errors.

> **Do not mix logs and caches from different runs.**
> That will lead to inconsistent and wrong analysis.

You may mix logs and aot map files from the same training or production run. Then, the information will complement each other. It is recommended to load first the AOT cache map so when processing the log we already have the details about the elements inside the cache.

There is a status bar on the bottom of the interactive console showing the current elements loaded in our playground:
```
Our Playground contains: 9014 elements | 768 packages | 2 element types | 42 warnings  
```

#### Load an AOT Map File

This is usually the first step. We can add an AOT map file, which are generated with the argument `-Xlog:aot+map=trace:file=aot.
map:none:filesize=0` when executing a training run.

```bash
load aotCache aot.map
```
```
Adding aot.map to our analysis...
This is a big file. The size of this file is 331 MB. This may take a while.
Consider using the `--background` option to load this file.
File aot.map added in 5478ms.
```

After loading the AOT Map File, we can explore the elements that have been saved in the AOT Cache.

#### Load logs

We can load logs for the training or the production run.

```bash
load productionLog production.log
```
```
Adding production.log to our analysis...
File production.log added in 280ms.
```

```bash
load trainingLog training.log
```
```
Adding training.log to our analysis...
File training.log added in 2925ms.
```

After loading some information, we can start the analysis.

### Show summarized information

The `info` command is very useful to get a general idea of what is happening in your application:

```bash
info
```
```
RUN SUMMARY: 
Classes loaded: 
  -> Cached:12.362 (98,18 %)
  -> Not Cached:229 (1,82 %)
Lambda Methods loaded: 
  -> Cached:765 (94,80 %)
  -> Not Cached:42 (5,20 %)
  -> Portion of methods that are lambda: 807 (0,51 %)
AOT CACHE SUMMARY: 
Classes in AOT Cache: 12.362
  -> KlassTrainingData: 2.661 (21,53 %)
Objects in AOT Cache: 148.681
Methods in AOT Cache: 157.361
  -> MethodCounters: 10.847 (6,89 %)
  -> MethodData: 7.108 (4,52 %)
  -> MethodTrainingData: 8.660 (5,50 %)
  -> CompileTrainingData: 
      -> Level 1: 1.118 (0,71 %)
      -> Level 2: 53 (0,03 %)
      -> Level 3: 5.163 (3,28 %)
      -> Level 4: 2.125 (1,35 %)
```

### Listing assets

We have the `ls` command to list what we know is on the cache. Most options in all the commands are autocompletable, so you can use `tab` to understand what to fill in there.

```bash
ls 
```
```
[....]
[Symbol] (Ljava/lang/classfile/ClassFileElement;)Ljava/lang/classfile/ClassFileBuilder;
[Symbol] (Ljava/lang/classfile/ClassFileElement;)V
[Symbol] (Ljava/lang/classfile/ClassFileTransform;Ljava/lang/classfile/ClassFileBuilder;)Ljdk/internal/classfile/impl/
TransformImpl$ResolvedTransform;
[ConstMethod] void org.infinispan.remoting.transport.jgroups.JGroupsMetricsManagerImpl.lambda$stop$1(org.infinispan.re
moting.transport.jgroups.JGroupsMetricsManagerImpl$ClusterMetrics)
[Untrained][Method] void org.infinispan.remoting.transport.jgroups.JGroupsMetricsManagerImpl.lambda$stop$1(org.infinis
pan.remoting.transport.jgroups.JGroupsMetricsManagerImpl$ClusterMetrics)
[ConstMethod] void org.infinispan.remoting.transport.jgroups.JGroupsMetricsManagerImpl.onChannelConnected(org.jgroups.
JChannel, boolean)
[Untrained][Method] void org.infinispan.remoting.transport.jgroups.JGroupsMetricsManagerImpl.onChannelConnected(org.jg
roups.JChannel, boolean)
Found 685694 elements.
```

We can filter by type of element and package (the parameters are auto-completable with suggestions):
```bash
ls -t=ConstantPool -pn=sun.util.locale
```
```
[...]
[ConstantPool] sun.util.locale.provider.NumberFormatProviderImpl
[ConstantPool] sun.util.locale.provider.LocaleProviderAdapter$Type
[ConstantPool] sun.util.locale.provider.LocaleProviderAdapter$$Lambda/0x800000099
[ConstantPool] sun.util.locale.provider.LocaleResources$ResourceReference
Found 32 elements.
```

### Search for warnings

We can also explore the potential errors/warnings/incidents. They may have been loaded from a log file, or they can be auto-detected.

```bash
warning
```
```
000 [Unknown] Preload Warning: Verification failed for org.infinispan.remoting.transport.jgroups.JGroupsRaftManager
001 [Unknown] Preload Warning: Verification failed for org.apache.logging.log4j.core.async.AsyncLoggerContext
002 [StoringIntoAOTCache] Element 'org.apache.logging.log4j.core.async.AsyncLoggerContext' of type 'Class' couldn't be
 stored into the AOTcache because: Failed verification
003 [StoringIntoAOTCache] Element 'org.infinispan.remoting.transport.jgroups.JGroupsRaftManager' of type 'Class' could
n't be stored into the AOTcache because: Failed verification
Found 4 warnings.
```

When listing, the search can be limited for each type of warning with `warning <n>`, which will show the `n`-most relevant warnings per type.

If you want to auto-detect issues, you can run the command `warning check`.

```bash
warning check
```
```
Trying to detect problems...
000 [Unknown] Preload Warning: Verification failed for org.infinispan.remoting.transport.jgroups.JGroupsRaftManager
001 [Unknown] Preload Warning: Verification failed for org.apache.logging.log4j.core.async.AsyncLoggerContext
002 [StoringIntoAOTCache] Element 'org.apache.logging.log4j.core.async.AsyncLoggerContext' of type 'Class' couldn't be
stored into the AOTcache because: Failed verification
003 [StoringIntoAOTCache] Element 'org.infinispan.remoting.transport.jgroups.JGroupsRaftManager' of type 'Class' could
n't be stored into the AOTcache because: Failed verification
024 [Training] Package 'org.apache.logging' contains 763 classes loaded and not cached.
025 [Training] Package 'io.reactivex.rxjava3' contains 724 classes loaded and not cached.
026 [Training] Package 'org.infinispan.server' contains 528 classes loaded and not cached.
027 [Training] Package 'org.infinispan.protostream' contains 42 methods that were called during training run but lack
full training (don't have some of the TrainingData objects associated to them).
028 [Training] Package 'io.reactivex.rxjava3' contains 30 methods that were called during training run but lack full t
raining (don't have some of the TrainingData objects associated to them).
029 [Training] Package 'org.apache.logging' contains 20 methods that were called during training run but lack full tra
ining (don't have some of the TrainingData objects associated to them).
Found 10 warnings.
The auto-detected issues may or may not be problematic.
It is up to the developer to decide that.
```

> **The auto-detected issues may or may not be problematic. It is up to the developer to decide that.**

You can clean up the list by using the `warning rm <id>` command. 

### Looking for details

To explore a bit more about what is on stored on the cache, we can use the command `describe`. 

Depending on if it was loaded from one type of file or another, the details may vary:

```bash
describe -i=java.util.stream.Collectors -t=Class
```
```
-----
|  Class java.util.stream.Collectors on address 0x00000008008b3b18 with size 528.
|  This information comes from: 
|    > AOT Map
|    > Production log
|    > Referenced from a KlassTrainingData.
|  This class is included in the AOT cache.
|  This class has 132 Methods, of which 6 have been run and 5 have been trained.
|  It has a KlassTrainingData associated to it.
-----
```

It has a verbose option to show a bit more info:

```bash
describe -i=org.infinispan.server.loader.Loader -t=Class -v
```
```
-----
|  Class org.infinispan.server.loader.Loader on address 0x0000000800ad6070 with size 528.
|  This information comes from: 
|    > AOT Map
|    > Production log
|  This class is included in the AOT cache.
|  This class has 5 Methods, of which 0 have been run and 0 have been trained.
|  This class doesn't seem to have training data. If you think this class and its methods should be part of the training, make sure your training run use them.
|  There are no elements referenced from this element.
|  Elements that refer to this element: 
|    _____
|    | [Untrained][Method] void org.infinispan.server.loader.Loader.main(java.lang.String[])
|    | [Untrained][Method] void org.infinispan.server.loader.Loader.<init>()
|    | [Untrained][Method] void org.infinispan.server.loader.Loader.run(java.lang.String[], java.util.Properties)
|    | [Untrained][Method] java.lang.ClassLoader org.infinispan.server.loader.Loader.classLoaderFromPath(java.nio.file
.Path, java.lang.ClassLoader)
|    | [Untrained][Method] java.lang.String org.infinispan.server.loader.Loader.extractArtifactName(java.lang.String)
|    | [Object] (0xffe1af68) java.lang.Class Lorg/infinispan/server/loader/Loader;
|    | [Symbol] org/infinispan/server/loader/Loader
|    _____
|  
|  Where does this element come from: 
|    _____
|    > Loaded from source: shared objects file
|    _____
-----
```

This command also shows if an element is part of the Heap root.

```bash
describe -i="(0xffd2d9b8) java.lang.ModuleLayer" 
```
```
-----
|  Object (0xffd2d9b8) java.lang.ModuleLayer on address 0x00000000ffd2d9b8 with size -1.This is a HEAP ROOT element.
|  This information comes from: 
|    > AOT Map
|  This element refers to 7 other elements.
-----
```

#### Tree information

The `tree` command shows related classes and objects (although you can tweak the parameters to show more than classes). It can be used with the `describe` command to check details on elements inside the AOT Cache.

The basic tree command shows the graph dependency of what classes are used by the root class. This is useful to understand what classes does the root class trigger to be inside the cache. 

**This graph is strongly based on a training log, so you must load it before getting the right information.**

```bash
tree -i=java.util.List  -max=5
```
```
Showing which classes [Trained][Class] java.util.List uses.
Calculating dependency graph... 
+ [Trained][Class] java.util.List
 \
  + [Trained][Class] java.util.Collections$UnmodifiableCollection
   \
    + [Trained][Class] java.lang.invoke.VarHandleByteArrayAsShorts$ArrayHandle
     \
      - [Trained][Class] java.util.Collections$UnmodifiableCollection
      |
      + [Untrained][Class] [Ljava.time.temporal.ChronoField;
      |
      + [Untrained][Class] java.security.PrivilegedExceptionAction
       \
        + [Trained][Class] java.lang.Object
```
To avoid infinite loops and circular references, each element will be iterated over on the tree only once. Elements that have already appeared on the tree will be colored blue, will be preceded by a `-` sign, and will not have children.

Adding more types of assets help explain the traceability of why those classes are related.

```bash
tree -i=java.util.List  -max=7 -t=Class,Symbol,Object,Method
```
```
Showing which classes [Trained][Class] java.util.List uses.
Calculating dependency graph... 
+ [Trained][Class] java.util.List
 \
  + [Untrained][Method] java.lang.Object java.util.List.remove(int)
   \
    - [Trained][Class] java.util.List
    |
    + [Trained][Class] java.lang.Object
     \
      + [Trained][Method] java.lang.Class java.lang.Object.getClass()
       \
        - [Trained][Class] java.lang.Object
        |
        + [Trained][Class] java.lang.Class
      |
      + [Symbol] Ljava/lang/Object;
       \
        - [Trained][Class] java.lang.Object
        |
        + [Untrained][Class] Ljava.lang.Object;
      |
      + [Untrained][Method] void java.lang.Object.notify()
```

There is also a `reverse` argument to show which classes use the root class. This is useful to understand why a class was loaded into the cache, as it shows who triggered its allocation in memory.

By default, we will see both classes and objects:

```bash
tree -i=java.util.List  -max=8 --reverse --level=1
```
```
Showing which classes are used by [Trained][Class] java.util.List.
Calculating dependency graph... 
+ [Trained][Class] java.util.List
 \
  + [Object] (0xffe456c0) java.lang.Class Ljdk/jfr/internal/jfc/JFC;
   \
    + [Object] (0xffe3ffc0) [Ljdk.internal.vm.FillerElement; length: 12
    |
    + [Trained][Class] java.util.Map
    |
    + [Trained][Class] java.lang.Object
    |
    - [Trained][Class] java.util.List
    |
    + [Trained][Class] java.lang.ref.SoftReference
    |
    + [Trained][Class] java.lang.ClassValue$ClassValueMap
    |
    + [Trained][Class] java.lang.reflect.Constructor
    |
    + [Trained][Class] sun.reflect.annotation.AnnotationType
```

If you are not interested in seeing the Objects themselves, filter by type `Class`:
```bash
tree -i=java.util.List  -max=10 --reverse -t=Class
```
```
Showing which classes are used by [Trained][Class] java.util.List.
Calculating dependency graph... 
+ [Trained][Class] java.util.List
 \
  + [Trained][Class] java.util.Collections$UnmodifiableCollection
   \
    + [Untrained][Class] com.fasterxml.jackson.annotation.JsonFormat$Shape
     \
      + [Untrained][Class] com.fasterxml.jackson.annotation.JsonFormat$Value
       \
        + [Untrained][Class] com.fasterxml.jackson.databind.SerializerProvider
        |
        + [Untrained][Class] com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
        |
        + [Untrained][Class] com.fasterxml.jackson.databind.deser.std.StdDeserializer
        |
        + [Untrained][Class] com.fasterxml.jackson.databind.DatabindContext
        |
        + [Untrained][Class] com.fasterxml.jackson.databind.cfg.ConfigOverrides
        |
        + [Untrained][Class] com.fasterxml.jackson.databind.introspect.BasicBeanDescription
        |
        + [Untrained][Class] com.fasterxml.jackson.databind.introspect.ConcreteBeanPropertyBase
```

Note that on all these examples we are using a limitation on the numbers of elements shown (`max` argument) and that the order of the tree is not warranteed.

### Cleanup

We can clean the loaded files and start from scratch

```bash
clean
```
```
Cleaned the elements. Load again files to start a new analysis.
```
```bash
ls
```
```
Found 0 elements.
```

### Exiting

Just `exit`.

## Examples of Usage

The following section contains examples on how to use this tool to improve your training runs and get better performance thanks to the AOT Cache in Java.

### Why is a class stored in my AOT cache?

We start by loading an aot cache map file to the tool:

```bash
load aotCache aot.map
```

And then we load a training log run to generate the dependency graph of classes.

```bash
load trainingLog training.log
```

Now we just simply run the `tree` command to see which classes use my root class. 

Remember that this command can generate a very long tree. You can use `n`, `-epn`, `-max`, and `--level` arguments to filter them out.

If we wanted to know why the class `org.infinispan.configuration.cache.Configuration` is on stored on the AOT cache, we build a reverse tree on it:

```bash
tree -i=org.infinispan.configuration.cache.Configuration --reverse
```
```
tree -i=org.infinispan.configuration.cache.Configuration -pn=org.infinispan --reverse 
Showing which classes are used by [Untrained][Class] org.infinispan.configuration.cache.Configuration.
Calculating dependency graph... 
+ [Untrained][Class] org.infinispan.configuration.cache.Configuration
 \
  + [Untrained][Class] org.infinispan.conflict.impl.DefaultConflictManager
   \
    + [Untrained][Class] org.infinispan.conflict.impl.CorePackageImpl$1
  |
  + [Untrained][Class] org.infinispan.configuration.cache.IndexWriterConfigurationBuilder
   \
    + [Untrained][Class] org.infinispan.configuration.cache.AbstractIndexingConfigurationChildBuilder
     \
      - [Untrained][Class] org.infinispan.configuration.cache.IndexWriterConfigurationBuilder
      |
      + [Untrained][Class] org.infinispan.configuration.cache.IndexMergeConfigurationBuilder
[...]
```

If we are only interested on the direct classes that make use of `org.infinispan.configuration.cache.Configuration`, we can use the argument `--level=0`

```bash
tree -i=org.infinispan.configuration.cache.Configuration --reverse --level=0
```
```
Showing which classes are used by [Untrained][Class] org.infinispan.configuration.cache.Configuration.
Calculating dependency graph... 
+ [Untrained][Class] org.infinispan.configuration.cache.Configuration
 \
  + [Untrained][Class] org.infinispan.configuration.cache.IndexWriterConfigurationBuilder
  |
  + [Untrained][Class] org.infinispan.factories.RecoveryManagerFactory
  |
  + [Untrained][Class] org.infinispan.configuration.cache.StoreConfigurationBuilder
  |
  + [Untrained][Class] org.infinispan.util.logging.Log_$logger
  |
```

If we want to know more about why those classes use our class, we can add more asset types to the tree:

```bash
tree -i=org.infinispan.configuration.cache.Configuration -pn=org.infinispan --reverse --level=1 -max=5 -t=Method,Symbol,Class,Object
```
```
Showing which classes are used by [Untrained][Class] org.infinispan.configuration.cache.Configuration.
Calculating dependency graph... 
+ [Untrained][Class] org.infinispan.configuration.cache.Configuration
 \
  + [Untrained][Method] void org.infinispan.configuration.serializing.CoreConfigurationSerializer.writeEncoding(org.in
finispan.commons.configuration.io.ConfigurationWriter, org.infinispan.configuration.cache.Configuration)
   \
    + [Untrained][Class] org.infinispan.configuration.serializing.CoreConfigurationSerializer
  |
  + [Untrained][Method] void org.infinispan.configuration.serializing.CoreConfigurationSerializer.writeQuery(org.infin
ispan.commons.configuration.io.ConfigurationWriter, org.infinispan.configuration.cache.Configuration)
```

This means that, for example, the class `org.infinispan.configuration.serializing.CoreConfigurationSerializer` uses the class `org.infinispan.configuration.cache.Configuration`, which will mean that if `CoreConfigurationSerializer` class is loaded into the cache, it will have to drag `Configuration` to be stored into the cache too.

That doesn't mean any of those classes or their methods are trained. This explains why the metadata of those classes are added into the cache.

### Which classes do a class drag into the cache?

We start by loading an aot cache map file to the tool:

```bash
load aotCache aot.map
```
And then we load a training log run to generate the dependency graph of classes.

```bash
load trainingLog training.log
```

Now we just simply run the `tree` command to see which classes are used by my root class. 

Note that this can be a very long graph, you can use `n`, `-epn`, `-max`, and `--level` arguments to filter them out.

```bash
tree -i=org.infinispan.configuration.cache.Configuration -pn=org.infinispan --level=1 
```
```
Showing which classes [Untrained][Class] org.infinispan.configuration.cache.Configuration uses.
Calculating dependency graph... 
+ [Untrained][Class] org.infinispan.configuration.cache.Configuration
 \
  + [Untrained][Class] org.infinispan.configuration.cache.SecurityConfiguration
   \
    + [Untrained][Class] org.infinispan.commons.configuration.attributes.ConfigurationElement
    |
    + [Untrained][Class] org.infinispan.configuration.cache.AuthorizationConfiguration
    |
    + [Untrained][Class] org.infinispan.commons.configuration.attributes.AttributeSet
  |
  + [Untrained][Class] org.infinispan.configuration.cache.TracingConfiguration
   \
    - [Untrained][Class] org.infinispan.commons.configuration.attributes.ConfigurationElement
    |
    + [Untrained][Class] org.infinispan.telemetry.SpanCategory
    |
    - [Untrained][Class] org.infinispan.commons.configuration.attributes.AttributeSet
  |
  + [Untrained][Class] org.infinispan.commons.dataconversion.MediaType
  |
  + [Untrained][Class] org.infinispan.configuration.cache.TransactionConfiguration
   \
    + [Untrained][Class] org.infinispan.transaction.LockingMode
    |
    - [Untrained][Class] org.infinispan.commons.configuration.attributes.ConfigurationElement
    |
    + [Untrained][Class] org.infinispan.commons.tx.lookup.TransactionManagerLookup
[...]
```

This means that, for example, the class `org.infinispan.configuration.cache.Configuration` uses the class `org.infinispan.configuration.cache.SecurityConfiguration`, which will mean that if `Configuration` class is loaded into the cache, it will have to drag `SecurityConfiguration` to be stored into the cache too.

That doesn't mean any of those classes or their methods are trained. This explains why the metadata of those classes are added into the cache.

### Why is a class NOT in my AOT cache?

[Storing metadata related to classes in the AOT cache reduces startup time](https://openjdk.org/jeps/483), so it is in your best interest to make sure all the classes used in your application are stored in the AOT cache.

On this case, we know the metadata of a certain class is not added to the AOT cache when it should have been. Maybe we loaded an aot cache map previously and didn't find it there, even when the production run shows that those classes are being used.

To find the root cause, we load a production log run to add information about which classes were used during production and where they were loaded from. 

```bash
load productionLog production.log
```

We can now list and `describe` classes that are outside the AOT cache using the `--use` parameter.

```bash
ls -t=Class --use=notCached
```
```
[...]
[Untrained][Class] org.infinispan.notifications.impl.AbstractListenerImpl$ListenerInvocationImpl$$Lambda/0x00000000500be230
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500cf6a0
[Untrained][Class] org.infinispan.objectfilter.impl.syntax.parser.ProtobufPropertyHelper$$Lambda/0x000000005012d8f0
[Untrained][Class] org.infinispan.persistence.file.SingleFileStore$$Lambda/0x00000000500fd000
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500cf8e8
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500cfb30
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500cfd78
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500d0000
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500d0248
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500d0490
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500d06d8
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500d0920
[Untrained][Class] org.infinispan.remoting.transport.jgroups.JGroupsMetricsMetadata$$Lambda/0x00000000500d0b68
Found 1819 elements.
```

If the class is not on this list, this means that the class was not even loaded during the production run. Which means it was never used during the production run. There is no fixing here, the problem is that you expect a class to be used when your application doesn't use it. If you think the class should have been loaded during production run, check your code and logs to understand why it was not used. Maybe some configuration issue?

**If the class is on this list**, we can determine if it is part of the AOT cache by using the `describe` command.

```bash
describe -i=org.infinispan.remoting.transport.jgroups.JGroupsRaftManager --use=both -v
```
```
-----
|  Class org.infinispan.remoting.transport.jgroups.JGroupsRaftManager.
|  This information comes from: 
|    > Production log
|  This class is NOT included in the AOT cache.
|  This class doesn't seem to have training data. If you think this class and its methods should be part of the training, make sure your training run use them.
|  There are no elements referenced from this element.
|  There are no other elements of the cache that refer to this element.
-----
```
Note the phrase `This class is NOT included in the AOT cache.`. Which means: this class has been loaded during the production run, it was somehow used or close to be used in your app, but the training run didn't think it should be included in the AOT cache.

There are multiple reasons why this happened and we can't cover all of them. But one thing we can do is to try to find some clue on the training run log.

```bash
load trainingLog training.log
```

We can try to find some warning related to our class:

```bash
warning -i=org.infinispan.remoting.transport.jgroups.JGroupsRaftManager
```
```
0074 [CacheCreation] Preload Warning: Verification failed for org.infinispan.remoting.transport.jgroups.JGroupsRaftMan
ager
0076 [CacheCreation]  Skipping org/infinispan/remoting/transport/jgroups/JGroupsRaftManager: Failed verification
25941 [CacheCreationRevertedKlass] reverted klass  CP entry [ 32]: org/infinispan/remoting/transport/jgroups/RaftUtil 
unreg => org/infinispan/remoting/transport/jgroups/JGroupsRaftManager
```

We find there three warnings associated to this element. Now we have something to investigate.

If the previous command didn't show any related warning, we can also explore all the warnings trying to find another cause for the missing class.

Note that if some class is excluded from the cache, that means all classes depending on it will also be excluded, as we only store on the cache complete dependency graphs. This means that looking for related classes (classes used by your excluded class, parent classes, interfaces,...) may give you a hint on why your class got excluded.

### Why is a method not properly trained?

We start by loading an aot cache map file to the tool:

```bash
load aotCache aot.map
```

Note that as I haven't loaded any log file, we only work with information coming from the AOT cache, which is limited. But it is enough for the purpose of this example.

I can ask for automatic checks on our playground to find potential improvement areas. This will try to detect automatically some weird behaviours that may or may not be a problem.

Note that this may find thousands of warnings. Do not be afraid, it doesn't mean your training has thousands of errors. They are just potential areas to explore. No one expects you to explore all of them.

```bash 
warning check
```
```
Trying to detect problems...
000 [Training] Package 'org.infinispan.protostream' contains 42 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
001 [Training] Package 'io.reactivex.rxjava3' contains 31 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
002 [Training] Package 'org.apache.logging' contains 20 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
003 [Training] Package 'org.infinispan.factories' contains 15 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
004 [Training] Package 'org.infinispan.configuration' contains 11 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
005 [Training] Package 'org.infinispan.commons' contains 11 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
006 [Training] Package 'io.micrometer.core' contains 5 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
007 [Training] Package 'org.infinispan.xsite' contains 5 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
008 [Training] Package 'org.infinispan.metrics' contains 5 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
009 [Training] Package 'org.jboss.logging' contains 5 methods that were called during training run but lack full training (don't have  some of the TrainingData objects associated to them).
Found 10 warnings.
The auto-detected issues may or may not be problematic.
It is up to the developer to decide that.
```

So I take for example warning `007`: the `org.infinispan.xsite` package contains methods that were run but not trained.

Now I can list all methods in that package that were run.

```bash
ls --run -pn=org.infinispan.xsite
```
```
[Trained][Method] void org.infinispan.xsite.ClusteredCacheBackupReceiver.<init>()
[Trained][Method] org.infinispan.xsite.status.NoOpTakeOfflineManager org.infinispan.xsite.status.NoOpTakeOfflineManager.getInstance()
[Trained][Method] org.infinispan.xsite.metrics.NoOpXSiteMetricsCollector org.infinispan.xsite.metrics.NoOpXSiteMetricsCollector.getInstance()
[Trained][Method] org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()
[Trained][Method] void org.infinispan.xsite.statetransfer.NoOpXSiteStateTransferManager.<init>()
Found 5 elements.
```

And I found five elements. I can take any of them and see what is happening to it:

```bash
 describe -t=Method -i="org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()"
 ```
```
-----
|  Method org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance() on address 0x00000008017116c0 with size 88.
|  This information comes from: 
|    > AOT Map
|  This element refers to 1 other elements.
|  Belongs to the class org.infinispan.xsite.NoOpBackupSender
|  It has a MethodCounters associated to it, which means it was called at least once during training run.
|  It has no CompileTrainingData associated to it.
|  It has a MethodData associated to it.
|  It has a MethodTrainingData associated to it.
-----
```

So I discover there is no CompileTrainingData associated to it. 

This may mean that this method was not executed enough times during the training run to be worth recompiled and have CompileTrainingData associated to it. Or it may be that the associated data was rejected for some reason. But now I have something to investigate further (see [Is my application properly trained?](#is-my-application-properly-trained)) .

I can also check what is happening on the main class for that method:

```bash
 describe -t=Class -i=org.infinispan.xsite.NoOpBackupSender
 ```
```
-----
|  Class org.infinispan.xsite.NoOpBackupSender on address 0x000000080172b2e8 with size 624.
|  This information comes from: 
|    > AOT Map
|  This class is included in the AOT cache.
|  This class has 9 Methods, of which 0 have been run and 0 have been trained.
|  This class doesn't seem to have training data. If you think this class and its methods should be part of the training, make sure your training run use them.
-----
```

### Is my application properly trained?

This is a nuanced question. "Properly trained" is very dependent on the context and the application you are trying to train. But we can explore what methods are compiled, at what level, and if there are methods that should be trained but are not.

**Note that Project Leyden is a work-in-progress and there may be missing assets just because the implementation to get those assets into the cache is not yet on mainstream.**

[A good training will improve warmup time and will help the JIT compiler to generate native code quicker.](https://openjdk.org/jeps/515) So, whatever your definition of properly trained, you should try to aim for a well-trained cache.

The basic command to run here is just an `ls` to list classes and methods that have been `--run` or are `--trained`. If you are looking at specific packages, you should filter by `--packageName`(`-pn`) because this list can be quite long.

First we can make sure that all methods that we know that should be run have been run:

```bash
ls --run -pn=org.infinispan.configuration.parsing
```
```
[Trained][Method] java.lang.String org.infinispan.configuration.parsing.Element.toString()
[Trained][Method] java.lang.String org.infinispan.configuration.parsing.Attribute.getLocalName()
[Trained][Method] java.lang.String org.infinispan.configuration.parsing.Element.getLocalName()
[Trained][Method] java.lang.String org.infinispan.configuration.parsing.Attribute.toString()
[Trained][Method] int org.infinispan.configuration.parsing.ParserRegistry$QName.hashCode()
[Trained][Method] void org.infinispan.configuration.parsing.ParserRegistry$QName.<init>(java.lang.String, java.lang.St
ring)
[Trained][Method] void org.infinispan.configuration.parsing.ParserRegistry$NamespaceParserPair.<init>(org.infinispan.c
onfiguration.parsing.Namespace, org.infinispan.configuration.parsing.ConfigurationParser)
Found 7 elements.
```

The very first step is to make sure that all the methods that your app will run are listed here. If there are methods that are not listed here but your app uses on startup, or regularly, it means your training was not good enough and didn't run those methods. Modify your training to make sure you use your app on a way that is as closer to a production run as possible.

Notice the `[Trained]` string that already gives us an indicator that those methods have not only been run, but also have training data available.

It could be that some methods were run but not trained:

```bash
ls --run -pn=jdk.internal.loader 
```
```
[Trained][Method] long jdk.internal.loader.NativeLibraries$NativeLibraryImpl.find(java.lang.String)
[Untrained][Method] long jdk.internal.loader.NativeLibraries.find(java.lang.String)
[Untrained][Method] long jdk.internal.loader.NativeLibrary.findEntry0(long, java.lang.String)
[Trained][Method] void jdk.internal.loader.AbstractClassLoaderValue$Memoizer.<init>(java.lang.ClassLoader, jdk.interna
l.loader.AbstractClassLoaderValue, java.util.function.BiFunction)
[...]
```

If some method was used by your app (was run) but is run only once or twice, it is normal to not have trained profile data associated to it.

Methods sometimes get called during training runs that are never going to be called in production runs -- for example, framework code that might be used to set up or terminate the training run. These methods will be presented as having been run and, potentially, as trained.  Changing the training plan might let us remove unnecessary methods and training information from the AOT cache. Unfortunately, there is no manual removal of data from the cache.

Not only we can list which methods have been run, we can check the classes and methods that are trained. This means: there is profile information about them on the AOT cache:

```bash
ls --trained -pn=io.quarkus.bootstrap
```
```
[Trained][Method] java.lang.Class io.quarkus.bootstrap.runner.RunnerClassLoader.loadClass(java.lang.String, boolean)
[Trained][Method] void io.quarkus.bootstrap.runner.JarResource.close()
[Trained][Method] void io.quarkus.bootstrap.runner.JarResource.resetInternalCaches()
[Trained][Class] io.quarkus.bootstrap.runner.JarResource$JarResourceDataProvider
[Trained][Method] int io.quarkus.bootstrap.runner.JarResource.hashCode()
[Trained][Class] io.quarkus.bootstrap.runner.JarResource$JarResourceURLProvider
[Trained][Method] void io.quarkus.bootstrap.runner.JarFileReference.<init>(java.util.jar.JarFile, java.util.concurrent
.CompletableFuture)
[Trained][Class] io.quarkus.bootstrap.graal.ImageInfo
[Trained][Method] void io.quarkus.bootstrap.runner.JarFileReference.closeJarResources(io.quarkus.bootstrap.runner.JarR
esource)
[Trained][Class] io.quarkus.bootstrap.runner.JarResource$JarUrlStreamHandler
[Trained][Class] io.quarkus.bootstrap.runner.ManifestInfo
[...]
```
You may fail to find any training data for your application classes. That can happen if your training run does not exercise very much application code or if the application code that it did exercise is loaded by a custom, application class loader.

```bash
describe -i=io.quarkus.bootstrap.runner.JarResource -t=Class
```
```
-----
|  Class io.quarkus.bootstrap.runner.JarResource on address 0x0000000800be88d0 with size 664.
|  This information comes from: 
|    > AOT Map
|    > Referenced from a KlassTrainingData.
|  This class is included in the AOT cache.
|  This class has 11 Methods, of which 8 have been run and 8 have been trained.
|  It has a KlassTrainingData associated to it.
-----
```

To get more information on what training does a method have, you can use the `describe` command:

```bash
describe -i="long org.infinispan.commons.util.TimeQuantity.longValue()" -t=Method
```
```
-----
|  Method long org.infinispan.commons.util.TimeQuantity.longValue() on address 0x0000000800fa54f0 with size 88.
|  This information comes from: 
|    > AOT Map
|  This element refers to 1 other elements.
|  Belongs to the class org.infinispan.commons.util.TimeQuantity
|  It has a MethodCounters associated to it, which means it was called at least once during training run.
|  It has CompileTrainingData associated to it on level: 1
|  It has a MethodTrainingData associated to it.
-----
```

The training information we can find here relates to the [code generated by the JVM and the codecache](https://docs.oracle.com/en/database/oracle/oracle-database/26/jjdev/Oracle-JVM-JIT.html). 

`It has a MethodCounters associated to it, which means it was called at least once during training run.` MethodCounters contains information like how many times a method has been called. If the method has one associated to it, it means it was run at least once during the training run.

`It has a MethodTrainingData associated to it.` or `It has a MethodData associated to it.` MethodTrainingData and MethodData are auxiliary elements that contain profiling statistic information detailing how the method was executed. For example, which branches were more probable to be reached. This information is useful when compiling, as it can reorder and rearrange parts of the code to make it more efficient based on those statistics.

`It has CompileTrainingData associated to it on level: 1` To learn more about what tiered compilation is, and what it means for your app, you can look at [How Tiered Compilation works in OpenJDK](https://devblogs.microsoft.com/java/how-tiered-compilation-works-in-openjdk/) or [Runtime profiling in OpenJDK's HotSpot JVM ](https://developers.redhat.com/articles/2021/11/18/runtime-profiling-openjdks-hotspot-jvm#hot_methods_and_tiered_compilation) 
**TL;DR: The higher the number here, the more optimized the code is (range from 1 to 4).**

Most of the hot method code should be compiled to levels 3 or 4 in a good training.

Code on level 2 usually means your compilation queue got too big and the JVM decided to compile at a lower level in the meantime. Longer trainings should end up eventually compiling those methods to level 3 or 4.

Code on level 1 usually means the method is deemed trivial and not worthy of a higher level of compilation. This is fine.

> This tool does not allow you to read the profiling training information on methods, as that information is not easily reachable.
> But the fact that your method has (or hasn't) these elements should give you an indication of the type of information that it stores and how good the training is for that method.
