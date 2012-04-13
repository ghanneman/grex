#!/usr/bin/env bash

#set -e   # exit at end of line on failures
#set -x   # show each command as it's run

BASE_DIR=~/svn/RuleLearnerNew
START_DIR=$(pwd)
cd $BASE_DIR/bin


# Test 1: Extract rules from the example sentence in the node alignment
# algorithm documentation:

rm -f $BASE_DIR/test/test-1-diff.txt
java ruleLearnerNew.RuleLearner $BASE_DIR/test/test-1-src-trees.txt $BASE_DIR/test/test-1-tgt-trees.txt $BASE_DIR/test/test-1-word-aligns.txt | sort -k 1,3 > $BASE_DIR/test/test-1-output.txt
diff $BASE_DIR/test/test-1-gold.txt $BASE_DIR/test/test-1-output.txt > $BASE_DIR/test/test-1-diff.txt
if [[ $(wc -l < $BASE_DIR/test/test-1-diff.txt) != "0" ]];
then
	echo "TEST 1 FAILED"
else
	echo "TEST 1 SUCCEEDED"
fi


# Test 2: Cases where projected alignments are more numerous:

rm -f $BASE_DIR/test/test-2-diff.txt
java ruleLearnerNew.RuleLearner $BASE_DIR/test/test-2-src-trees.txt $BASE_DIR/test/test-2-tgt-trees.txt $BASE_DIR/test/test-2-word-aligns.txt | sort -k 1,3 > $BASE_DIR/test/test-2-output.txt
diff $BASE_DIR/test/test-2-gold.txt $BASE_DIR/test/test-2-output.txt > $BASE_DIR/test/test-2-diff.txt
if [[ $(wc -l < $BASE_DIR/test/test-2-diff.txt) != "0" ]];
then
	echo "TEST 2 FAILED"
else
	echo "TEST 2 SUCCEEDED"
fi


cd $START_DIR
