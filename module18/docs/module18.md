# Module 18 - Effective Java (3rd edition): all 90 items in Java 17 and 21

Package root: `module18.chNN` (one package per book chapter, one class `ItemNNTitle` per item). Domain: payments, ledgers,
cards, FX, settlement, compliance. Tests: one JUnit test per item, named `itemNN_...`, in `ChapterNNTest`.

## Overview
*Effective Java* (Joshua Bloch, 3rd edition, 2018, written for Java 9) is the standard reference for writing correct, clear
and maintainable Java. This module gives every one of its 90 items:
- an **original, runnable fintech example** that shows the problem (usually a BAD version) and the fix (GOOD version);
- a **test that proves the claim** (the bug really happens, the fix really works);
- **documentation** for the item: a summary in our own words, why it matters in banking, Java 17/21 updates, pitfalls,
  and where the proof lives.

The explanations are our own summaries for study purposes, and the item titles are used as references. Read the book for
Bloch's full reasoning; use this module to see each idea run, fail and get fixed in code you can modify.

## Learning objectives
- State each item's rule and its justification in one or two sentences, as you would in an interview.
- Recognise each anti-pattern in a code review, and name the item it violates.
- Know which 2018 advice changed with Java 10-21 (records, sealed types, pattern matching, `var`, virtual threads,
  `Cleaner`/finalizer removal, serialization filters, `SequencedCollection`, `-Xlint:this-escape`).

## Prerequisites
Modules 01-02 (OOP, equals/hashCode), 05 (generics) and 09 (concurrency basics) make chapters 3, 5 and 11 easier.

## How to study it
1. Read the item section in the chapter doc.
2. Open the class and predict what the test asserts.
3. Run the test (`mvn -pl module18 test -Dtest=Chapter05Test`), then break the GOOD version and watch it fail.
4. Use the "say it in an interview" line at the end of each chapter.

## Java version notes for this module
Compiled with `--release 21` and **no preview features**. Most examples use only Java 17 features. Java 21-only usage is
labelled where it appears:

| Feature | Java | Used in |
|---|---|---|
| Records, sealed types, `instanceof` patterns, switch expressions, text blocks | 17 (final) | throughout |
| `RandomGenerator` API (JEP 356), `HexFormat` | 17 | items 28, 59 |
| `@Serial` annotation | 14 | chapter 12 |
| `{@snippet}` Javadoc tag (JEP 413) | 18 | item 56 |
| Finalization deprecated for removal (JEP 421) | 18 | item 8 |
| `ExecutorService` is `AutoCloseable` | 19 | item 80 |
| Record patterns + pattern-matching `switch` | **21** (preview in 19/20) | items 23, 25 |
| Virtual threads (`newVirtualThreadPerTaskExecutor`) | **21** (preview in 19/20) | item 80 |
| `SequencedCollection` (`getFirst`/`getLast`) | **21** | item 64 |
| `javac -Xlint:this-escape` | **21** | item 19 |
| Foreign Function & Memory API | preview 19-21, final 22 | item 66 (mentioned only) |

To compile everything on Java 17, replace the Java 21 constructs above: use `instanceof` chains instead of record patterns
(see module07 `TransactionLifecycle#applyJava17`), a fixed thread pool instead of virtual threads, and `List`/`Deque`
instead of `SequencedCollection`.

## The 90 items
| Chapter | Items | Doc | Test |
|---|---|---|---|
| 2 Creating and destroying objects | 1-9 | [chapter02_creating_and_destroying_objects.md](chapter02_creating_and_destroying_objects.md) | `Chapter02Test` |
| 3 Methods common to all objects | 10-14 | [chapter03_methods_common_to_all_objects.md](chapter03_methods_common_to_all_objects.md) | `Chapter03Test` |
| 4 Classes and interfaces | 15-25 | [chapter04_classes_and_interfaces.md](chapter04_classes_and_interfaces.md) | `Chapter04Test` |
| 5 Generics | 26-33 | [chapter05_generics.md](chapter05_generics.md) | `Chapter05Test` |
| 6 Enums and annotations | 34-41 | [chapter06_enums_and_annotations.md](chapter06_enums_and_annotations.md) | `Chapter06Test` |
| 7 Lambdas and streams | 42-48 | [chapter07_lambdas_and_streams.md](chapter07_lambdas_and_streams.md) | `Chapter07Test` |
| 8 Methods | 49-56 | [chapter08_methods.md](chapter08_methods.md) | `Chapter08Test` |
| 9 General programming | 57-68 | [chapter09_general_programming.md](chapter09_general_programming.md) | `Chapter09Test` |
| 10 Exceptions | 69-77 | [chapter10_exceptions.md](chapter10_exceptions.md) | `Chapter10Test` |
| 11 Concurrency | 78-84 | [chapter11_concurrency.md](chapter11_concurrency.md) | `Chapter11Test` |
| 12 Serialization | 85-90 | [chapter12_serialization.md](chapter12_serialization.md) | `Chapter12Test` |

### Full item index
| # | Item | Class |
|---|---|---|
| 1 | Consider static factory methods instead of constructors | `ch02.Item01StaticFactories` |
| 2 | Consider a builder when faced with many constructor parameters | `ch02.Item02Builder` |
| 3 | Enforce the singleton property with a private constructor or an enum type | `ch02.Item03Singleton` |
| 4 | Enforce noninstantiability with a private constructor | `ch02.Item04Noninstantiable` |
| 5 | Prefer dependency injection to hardwiring resources | `ch02.Item05DependencyInjection` |
| 6 | Avoid creating unnecessary objects | `ch02.Item06UnnecessaryObjects` |
| 7 | Eliminate obsolete object references | `ch02.Item07ObsoleteReferences` |
| 8 | Avoid finalizers and cleaners | `ch02.Item08Cleaners` |
| 9 | Prefer try-with-resources to try-finally | `ch02.Item09TryWithResources` |
| 10 | Obey the general contract when overriding equals | `ch03.Item10EqualsContract` |
| 11 | Always override hashCode when you override equals | `ch03.Item11HashCode` |
| 12 | Always override toString | `ch03.Item12ToString` |
| 13 | Override clone judiciously | `ch03.Item13Clone` |
| 14 | Consider implementing Comparable | `ch03.Item14Comparable` |
| 15 | Minimize the accessibility of classes and members | `ch04.Item15Accessibility` |
| 16 | In public classes, use accessor methods, not public fields | `ch04.Item16Accessors` |
| 17 | Minimize mutability | `ch04.Item17Immutability` |
| 18 | Favor composition over inheritance | `ch04.Item18Composition` |
| 19 | Design and document for inheritance or else prohibit it | `ch04.Item19DesignForInheritance` |
| 20 | Prefer interfaces to abstract classes | `ch04.Item20InterfacesOverAbstract` |
| 21 | Design interfaces for posterity | `ch04.Item21InterfacesForPosterity` |
| 22 | Use interfaces only to define types | `ch04.Item22InterfacesOnlyForTypes` |
| 23 | Prefer class hierarchies to tagged classes | `ch04.Item23ClassHierarchies` |
| 24 | Favor static member classes over nonstatic | `ch04.Item24StaticMemberClasses` |
| 25 | Limit source files to a single top-level class | `ch04.Item25SingleTopLevelClass` |
| 26 | Don't use raw types | `ch05.Item26RawTypes` |
| 27 | Eliminate unchecked warnings | `ch05.Item27UncheckedWarnings` |
| 28 | Prefer lists to arrays | `ch05.Item28ListsOverArrays` |
| 29 | Favor generic types | `ch05.Item29GenericTypes` |
| 30 | Favor generic methods | `ch05.Item30GenericMethods` |
| 31 | Use bounded wildcards to increase API flexibility | `ch05.Item31BoundedWildcards` |
| 32 | Combine generics and varargs judiciously | `ch05.Item32GenericsAndVarargs` |
| 33 | Consider typesafe heterogeneous containers | `ch05.Item33HeterogeneousContainer` |
| 34 | Use enums instead of int constants | `ch06.Item34Enums` |
| 35 | Use instance fields instead of ordinals | `ch06.Item35InstanceFieldsNotOrdinals` |
| 36 | Use EnumSet instead of bit fields | `ch06.Item36EnumSet` |
| 37 | Use EnumMap instead of ordinal indexing | `ch06.Item37EnumMap` |
| 38 | Emulate extensible enums with interfaces | `ch06.Item38ExtensibleEnums` |
| 39 | Prefer annotations to naming patterns | `ch06.Item39Annotations` |
| 40 | Consistently use the Override annotation | `ch06.Item40Override` |
| 41 | Use marker interfaces to define types | `ch06.Item41MarkerInterfaces` |
| 42 | Prefer lambdas to anonymous classes | `ch07.Item42Lambdas` |
| 43 | Prefer method references to lambdas | `ch07.Item43MethodReferences` |
| 44 | Favor the use of standard functional interfaces | `ch07.Item44StandardFunctionalInterfaces` |
| 45 | Use streams judiciously | `ch07.Item45StreamsJudiciously` |
| 46 | Prefer side-effect-free functions in streams | `ch07.Item46SideEffectFreeStreams` |
| 47 | Prefer Collection to Stream as a return type | `ch07.Item47CollectionReturnType` |
| 48 | Use caution when making streams parallel | `ch07.Item48ParallelStreams` |
| 49 | Check parameters for validity | `ch08.Item49ParameterValidity` |
| 50 | Make defensive copies when needed | `ch08.Item50DefensiveCopies` |
| 51 | Design method signatures carefully | `ch08.Item51MethodSignatures` |
| 52 | Use overloading judiciously | `ch08.Item52Overloading` |
| 53 | Use varargs judiciously | `ch08.Item53Varargs` |
| 54 | Return empty collections or arrays, not nulls | `ch08.Item54EmptyNotNull` |
| 55 | Return optionals judiciously | `ch08.Item55Optionals` |
| 56 | Write doc comments for all exposed API elements | `ch08.Item56DocComments` |
| 57 | Minimize the scope of local variables | `ch09.Item57LocalVariableScope` |
| 58 | Prefer for-each loops to traditional for loops | `ch09.Item58ForEach` |
| 59 | Know and use the libraries | `ch09.Item59KnowTheLibraries` |
| 60 | Avoid float and double if exact answers are required | `ch09.Item60AvoidFloatDouble` |
| 61 | Prefer primitive types to boxed primitives | `ch09.Item61PrimitivesOverBoxed` |
| 62 | Avoid strings where other types are more appropriate | `ch09.Item62AvoidStrings` |
| 63 | Beware the performance of string concatenation | `ch09.Item63StringConcatenation` |
| 64 | Refer to objects by their interfaces | `ch09.Item64ReferToInterfaces` |
| 65 | Prefer interfaces to reflection | `ch09.Item65InterfacesOverReflection` |
| 66 | Use native methods judiciously | `ch09.Item66NativeMethods` |
| 67 | Optimize judiciously | `ch09.Item67OptimizeJudiciously` |
| 68 | Adhere to generally accepted naming conventions | `ch09.Item68NamingConventions` |
| 69 | Use exceptions only for exceptional conditions | `ch10.Item69ExceptionsForExceptional` |
| 70 | Use checked exceptions for recoverable conditions and runtime exceptions for programming errors | `ch10.Item70CheckedVsRuntime` |
| 71 | Avoid unnecessary use of checked exceptions | `ch10.Item71UnnecessaryChecked` |
| 72 | Favor the use of standard exceptions | `ch10.Item72StandardExceptions` |
| 73 | Throw exceptions appropriate to the abstraction | `ch10.Item73ExceptionTranslation` |
| 74 | Document all exceptions thrown by each method | `ch10.Item74DocumentExceptions` |
| 75 | Include failure-capture information in detail messages | `ch10.Item75FailureCaptureInfo` |
| 76 | Strive for failure atomicity | `ch10.Item76FailureAtomicity` |
| 77 | Don't ignore exceptions | `ch10.Item77DontIgnoreExceptions` |
| 78 | Synchronize access to shared mutable data | `ch11.Item78SynchronizeSharedData` |
| 79 | Avoid excessive synchronization | `ch11.Item79ExcessiveSynchronization` |
| 80 | Prefer executors, tasks, and streams to threads | `ch11.Item80Executors` |
| 81 | Prefer concurrency utilities to wait and notify | `ch11.Item81ConcurrencyUtilities` |
| 82 | Document thread safety | `ch11.Item82DocumentThreadSafety` (+ `ch11.ThreadSafety` annotations) |
| 83 | Use lazy initialization judiciously | `ch11.Item83LazyInitialization` |
| 84 | Don't depend on the thread scheduler | `ch11.Item84ThreadScheduler` |
| 85 | Prefer alternatives to Java serialization | `ch12.Item85AlternativesToSerialization` |
| 86 | Implement Serializable with great caution | `ch12.Item86SerializableWithCaution` |
| 87 | Consider using a custom serialized form | `ch12.Item87CustomSerializedForm` |
| 88 | Write readObject methods defensively | `ch12.Item88DefensiveReadObject` |
| 89 | For instance control, prefer enum types to readResolve | `ch12.Item89EnumOverReadResolve` |
| 90 | Prefer serialization proxies to readObject | `ch12.Item90SerializationProxy` |

## What changed since the book (Java 9 -> 21) - the short list
| Book advice | Modern update |
|---|---|
| Item 2 builders | Records cover many immutable value cases; builders still win for many optional parameters |
| Item 8 cleaners/finalizers | Finalization deprecated for removal (Java 18). Use `AutoCloseable`, with a `Cleaner` only as a safety net |
| Items 10-12 equals/hashCode/toString | Records generate them; still override `toString` for sensitive components |
| Item 19 inheritance | `sealed` (17) restricts subclasses precisely; `-Xlint:this-escape` (21) detects constructor escapes |
| Item 23 tagged classes | Sealed interface + records + exhaustive pattern `switch` (21) |
| Item 24 inner classes | javac 18+ drops an unused `this$0`, but any use of the outer instance brings it back |
| Item 47 Collection return types | `Stream.toList()` (16), `SequencedCollection` (21) |
| Item 64 interfaces | `SequencedCollection`, `SequencedMap` (21) |
| Item 66 native methods | Foreign Function & Memory API (final in 22) replaces most JNI |
| Item 80 executors | Virtual threads (21) for blocking I/O; `ExecutorService` is `AutoCloseable` (19) |
| Items 85-90 serialization | Deserialization filters (9, 17), records deserialize via the canonical constructor (16), `@Serial` (14) |

## Common pitfalls when applying the book
- Applying an item mechanically. Each item has a stated scope ("judiciously", "consider"), so know the exceptions.
- Using records as a blanket fix: they are shallowly immutable and print every component.
- Treating Item 3 enum singletons as a substitute for dependency injection (Item 5); singletons are still global state.
- Copying 2018 concurrency advice without the Java 21 update (virtual threads, structured concurrency in preview).

## Checklist
- [ ] Can explain every item in two sentences and point to its test.
- [ ] Can spot items 10, 11, 17, 18, 26, 49, 50, 60, 69, 73, 76, 78 and 85 in a code review (the most frequently asked).
- [ ] Knows which items changed with records, sealed types, pattern matching and virtual threads.
