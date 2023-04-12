# A simple Makefile for UNSW cs3161/cs9102 programming languages and compilers
# Written by Chris Hall
#
# Place this Makefile inside your VC/ folder, so your directory layout
# should look like:
#
#   VC/
#     ErrorReporter.java
#     vc.java
#     Makefile
#     Scanner/
#       <a bunch of .vc files>
#       <a bunch of .sol files>
#       <a bunch of .java files>
#
# if you change directory to VC/
#   $ cd VC/
#
# you can then use the below make commands
#   $ make run
JC = javac
JAVA = java
EXE = VC.vc

MODULE = CodeGen
FILE_NAME = gcd
ARGS = ./$(MODULE)/$(FILE_NAME).vc
SOL = ./$(MODULE)/$(FILE_NAME).sol
TESTS := $(wildcard ./$(MODULE)/*.vc)


.POSIX:

# `make` is the same as `make run`
all: run

# This is the rule for actually doing the compile of vc.java into vc.class
../vc.class:
	CLASSPATH=../ $(JC) vc.java

# `make clean` will remove "tokens.out" and any .class files (compiled java)
clean:
	find ./ -iname '*.class' -delete
	find ./ -iname '*.vct' -delete
	find ./ -iname '*.vcu' -delete
	rm -rf out.txt diff_out.txt

# `make run` will compile and run your scanner
run: clean ../vc.class
	clear
	CLASSPATH=../ $(JAVA) $(EXE) $(ARGS)
	CLASSPATH=../ $(JAVA) $(EXE) $(ARGS) > out.txt


	#diff --text ./out.txt ./assets/$(CURR_TEST).sol ; \

test: clean ../vc.class
	 for file in $(TESTS); do \
   		echo $${file} ; \
		CLASSPATH=../ $(JAVA) $(EXE) $${file} ; \
     done

diff: clean ../vc.class
	 for file in $(TESTS); do \
		CLASSPATH=../ $(JAVA) $(EXE) $${file} > out.txt ; \
	  	diff --text out.txt ./$(MODULE)/$$(basename -- $${file%.*}).sol >> diff_out.txt ; \
     done


.PHONY: all clean run

