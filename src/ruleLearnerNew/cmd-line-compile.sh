#!/usr/bin/env bash

# TO DO:  Figure out which ones of these aren't in Michelle's Eclipse version; remove them from here and from SVN if they're not being used.
# TO DO:  The command below leaves out two Hadoop-related classes that probably need an external Hadoop JAR.  Make a command for compiling the Hadoop version too.

mkdir -p ../../bin
javac -d ../../bin AvenueLabeler.java BaseGrammarExtractor.java BiSpan.java ExtractedRule.java GrammarExtractionState.java GrammarExtractor.java Labeler.java MalformedAlignmentException.java MalformedTreeException.java NodeAligner.java NodeAlignmentList.java NodeAlignmentState.java Node.java NodeSpanMap.java NullParseNode.java ParseNode.java ParseNodeRulePart.java ParseTreeYield.java ReorderingList.java RuleLearner.java RuleListener.java RulePart.java Span.java StringNode.java TerminalNode.java VamshiNodeAligner.java VirtualParseNode.java WordAlignment.java
