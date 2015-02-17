StaticSpotter (Reclipse)
======================
Reclipse is a reverse engineering tool for the automatic detection of patterns in source code. A pattern is a general, reusable solution to a commonly occurring problem in software design. Detecting pattern occurrences can help understand a piece of code and thereby provide deeper insight into a software.

Reclipse provides a static and a dynamic pattern detection. The static analysis in Reclipse uses a pattern specification language based on object diagrams, the so-called structural patterns. The static analysis requires the source code of the software system and a library of structural patterns. An inference algorithm recovers the structure of design patterns by graph matching. It results in an annotated class diagram which shows the possible implementations of design patterns, so-called pattern candidates. A following dynamic analysis can be used to reject or confirm these pattern candidates. For this purpose the candidates’ behavior can be monitored during the program execution, using the Reclipse tracer. The tracer generates a number of traces for each candidate.

Version
======================
17.2.2016 - https://code.google.com/p/reclipse-emf/

