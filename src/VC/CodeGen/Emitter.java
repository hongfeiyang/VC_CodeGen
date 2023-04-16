/*
 *** Emitter.java 
 *** Mon 03 Apr 2023 12:42:25 AEST
 */

// A new frame object is created for every function just before the
// function is being translated in visitFuncDecl.
//
// All the information about the translation of a function should be
// placed in this Frame object and passed across the AST nodes as the
// 2nd argument of every visitor method in Emitter.java.

package VC.CodeGen;

import java.util.HashMap;
import java.util.HashSet;

import VC.ErrorReporter;
import VC.StdEnvironment;
import VC.ASTs.AST;
import VC.ASTs.Arg;
import VC.ASTs.ArgList;
import VC.ASTs.ArrayExpr;
import VC.ASTs.ArrayExprList;
import VC.ASTs.ArrayInitExpr;
import VC.ASTs.ArrayType;
import VC.ASTs.AssignExpr;
import VC.ASTs.BinaryExpr;
import VC.ASTs.BooleanExpr;
import VC.ASTs.BooleanLiteral;
import VC.ASTs.BooleanType;
import VC.ASTs.BreakStmt;
import VC.ASTs.CallExpr;
import VC.ASTs.CompoundStmt;
import VC.ASTs.ContinueStmt;
import VC.ASTs.Decl;
import VC.ASTs.DeclList;
import VC.ASTs.EmptyArgList;
import VC.ASTs.EmptyArrayExprList;
import VC.ASTs.EmptyCompStmt;
import VC.ASTs.EmptyDeclList;
import VC.ASTs.EmptyExpr;
import VC.ASTs.EmptyParaList;
import VC.ASTs.EmptyStmt;
import VC.ASTs.EmptyStmtList;
import VC.ASTs.ErrorType;
import VC.ASTs.Expr;
import VC.ASTs.ExprStmt;
import VC.ASTs.FloatExpr;
import VC.ASTs.FloatLiteral;
import VC.ASTs.FloatType;
import VC.ASTs.ForStmt;
import VC.ASTs.FuncDecl;
import VC.ASTs.GlobalVarDecl;
import VC.ASTs.Ident;
import VC.ASTs.IfStmt;
import VC.ASTs.IntExpr;
import VC.ASTs.IntLiteral;
import VC.ASTs.IntType;
import VC.ASTs.List;
import VC.ASTs.LocalVarDecl;
import VC.ASTs.Operator;
import VC.ASTs.ParaDecl;
import VC.ASTs.ParaList;
import VC.ASTs.Program;
import VC.ASTs.ReturnStmt;
import VC.ASTs.SimpleVar;
import VC.ASTs.StmtList;
import VC.ASTs.StringExpr;
import VC.ASTs.StringLiteral;
import VC.ASTs.StringType;
import VC.ASTs.Type;
import VC.ASTs.UnaryExpr;
import VC.ASTs.VarExpr;
import VC.ASTs.Visitor;
import VC.ASTs.VoidType;
import VC.ASTs.WhileStmt;

public final class Emitter implements Visitor {

  private ErrorReporter errorReporter;
  private String inputFilename;
  private String classname;
  private String outputFilename;

  public Emitter(String inputFilename, ErrorReporter reporter) {
    this.inputFilename = inputFilename;
    errorReporter = reporter;

    int i = inputFilename.lastIndexOf('.');
    if (i > 0)
      classname = inputFilename.substring(0, i);
    else
      classname = inputFilename;

  }

  // PRE: ast must be a Program node

  public final void gen(AST ast) {
    ast.visit(this, null);
    JVM.dump(classname + ".j");
  }

  // Programs
  public Object visitProgram(Program ast, Object o) {
    /**
     * This method works for scalar variables only. You need to modify
     * it to handle all array-related declarations and initialisations.
     **/

    // Generates the default constructor initialiser
    emit(JVM.CLASS, "public", classname);
    emit(JVM.SUPER, "java/lang/Object");

    emit("");

    // Three subpasses:

    // (1) Generate .field definition statements since
    // these are required to appear before method definitions
    List list = ast.FL;
    while (!list.isEmpty()) {
      DeclList dlAST = (DeclList) list;
      if (dlAST.D instanceof GlobalVarDecl) {
        GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
        emit(JVM.STATIC_FIELD, vAST.I.spelling, VCtoJavaType(vAST.T));
      }
      list = dlAST.DL;
    }

    emit("");

    // (2) Generate <clinit> for global variables (assumed to be static)

    emit("; standard class static initializer ");
    emit(JVM.METHOD_START, "static <clinit>()V");
    emit("");

    // create a Frame for <clinit>

    Frame frame = new Frame(false);

    list = ast.FL;
    while (!list.isEmpty()) {
      DeclList dlAST = (DeclList) list;
      if (dlAST.D instanceof GlobalVarDecl) {
        GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
        if (!vAST.E.isEmptyExpr()) {
          vAST.E.visit(this, frame);
        } else {
          if (vAST.T.equals(StdEnvironment.floatType))
            emit(JVM.FCONST_0);
          else
            emit(JVM.ICONST_0);
          frame.push();
        }
        emitPUTSTATIC(VCtoJavaType(vAST.T), vAST.I.spelling);
        frame.pop();
      }
      list = dlAST.DL;
    }

    emit("");
    emit("; set limits used by this method");
    emit(JVM.LIMIT, "locals", frame.getNewIndex());

    emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
    // changed by the marker
    // emit(JVM.LIMIT, "stack", 50);

    emit(JVM.RETURN);
    emit(JVM.METHOD_END, "method");

    emit("");

    // (3) Generate Java bytecode for the VC program

    emit("; standard constructor initializer ");
    emit(JVM.METHOD_START, "public <init>()V");
    emit(JVM.LIMIT, "stack 1");
    emit(JVM.LIMIT, "locals 1");
    emit(JVM.ALOAD_0);
    emit(JVM.INVOKESPECIAL, "java/lang/Object/<init>()V");
    emit(JVM.RETURN);
    emit(JVM.METHOD_END, "method");

    return ast.FL.visit(this, o);
  }

  // Statements

  public Object visitStmtList(StmtList ast, Object o) {
    ast.S.visit(this, o);
    ast.SL.visit(this, o);
    return null;
  }

  public Object visitCompoundStmt(CompoundStmt ast, Object o) {
    Frame frame = (Frame) o;

    String scopeStart = frame.getNewLabel();
    String scopeEnd = frame.getNewLabel();
    frame.scopeStart.push(scopeStart);
    frame.scopeEnd.push(scopeEnd);

    emit(scopeStart + ":");
    if (ast.parent instanceof FuncDecl) {
      if (((FuncDecl) ast.parent).I.spelling.equals("main")) {
        emit(JVM.VAR, "0 is argv [Ljava/lang/String; from " + (String) frame.scopeStart.peek() + " to "
            + (String) frame.scopeEnd.peek());
        emit(JVM.VAR, "1 is vc$ L" + classname + "; from " + (String) frame.scopeStart.peek() + " to "
            + (String) frame.scopeEnd.peek());
        // Generate code for the initialiser vc$ = new classname();
        emit(JVM.NEW, classname);
        emit(JVM.DUP);
        frame.push(2);
        emit("invokenonvirtual", classname + "/<init>()V");
        frame.pop();
        emit(JVM.ASTORE_1);
        frame.pop();
      } else {
        emit(JVM.VAR, "0 is this L" + classname + "; from " + (String) frame.scopeStart.peek() + " to "
            + (String) frame.scopeEnd.peek());
        ((FuncDecl) ast.parent).PL.visit(this, o);
      }
    }
    ast.DL.visit(this, o);
    ast.SL.visit(this, o);
    emit(scopeEnd + ":");

    frame.scopeStart.pop();
    frame.scopeEnd.pop();
    return null;
  }

  public Object visitReturnStmt(ReturnStmt ast, Object o) {
    Frame frame = (Frame) o;

    /*
     * int main() { return 0; } must be interpretted as
     * public static void main(String[] args) { return ; }
     * Therefore, "return expr", if present in the main of a VC program
     * must be translated into a RETURN rather than IRETURN instruction.
     */

    if (frame.isMain()) {
      emit(JVM.RETURN);
      return null;
    }
    ast.E.visit(this, o);
    if (ast.E instanceof EmptyExpr) {
      // also handles error type set by empty expressions, so we dont need to handle
      // if (ast.E.type.isErrorType())
      emit(JVM.RETURN);
    } else if (ast.E.type.isFloatType()) {
      emit(JVM.FRETURN);
    } else if (ast.E.type.isIntType() || ast.E.type.isBooleanType()) {
      // Int type and boolean type (which also translates to int)
      // Array Var also eventually resolves to boolean float and int
      emit(JVM.IRETURN);
    }
    // What about void expressions? e.g:
    // void f() {
    // return f();
    // }
    return null;
  }

  public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
    return null;
  }

  public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
    return null;
  }

  public Object visitEmptyStmt(EmptyStmt ast, Object o) {
    return null;
  }

  // Expressions

  public Object visitCallExpr(CallExpr ast, Object o) {
    Frame frame = (Frame) o;
    String fname = ast.I.spelling;

    if (fname.equals("getInt")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System.getInt()I");
      frame.push();
    } else if (fname.equals("putInt")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System.putInt(I)V");
      frame.pop();
    } else if (fname.equals("putIntLn")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System/putIntLn(I)V");
      frame.pop();
    } else if (fname.equals("getFloat")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System/getFloat()F");
      frame.push();
    } else if (fname.equals("putFloat")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System/putFloat(F)V");
      frame.pop();
    } else if (fname.equals("putFloatLn")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System/putFloatLn(F)V");
      frame.pop();
    } else if (fname.equals("putBool")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System/putBool(Z)V");
      frame.pop();
    } else if (fname.equals("putBoolLn")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System/putBoolLn(Z)V");
      frame.pop();
    } else if (fname.equals("putString")) {
      ast.AL.visit(this, o);
      emit(JVM.INVOKESTATIC, "VC/lang/System/putString(Ljava/lang/String;)V");
      frame.pop();
    } else if (fname.equals("putStringLn")) {
      ast.AL.visit(this, o);
      emit(JVM.INVOKESTATIC, "VC/lang/System/putStringLn(Ljava/lang/String;)V");
      frame.pop();
    } else if (fname.equals("putLn")) {
      ast.AL.visit(this, o); // push args (if any) into the op stack
      emit("invokestatic VC/lang/System/putLn()V");
    } else { // programmer-defined functions

      FuncDecl fAST = (FuncDecl) ast.I.decl;

      // all functions except main are assumed to be instance methods
      if (frame.isMain())
        emit("aload_1"); // vc.funcname(...)
      else
        emit("aload_0"); // this.funcname(...)
      frame.push();

      ast.AL.visit(this, o);

      String retType = VCtoJavaType(fAST.T);

      // The types of the parameters of the called function are not
      // directly available in the FuncDecl node but can be gathered
      // by traversing its field PL.

      StringBuffer argsTypes = new StringBuffer("");
      List fpl = fAST.PL;

      int numberOfArgs = 0;

      while (!fpl.isEmpty()) {
        if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType))
          argsTypes.append("Z");
        else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType))
          argsTypes.append("I");
        else if (((ParaList) fpl).P.T.equals(StdEnvironment.floatType))
          argsTypes.append("F");
        else if (((ParaList) fpl).P.T.isArrayType()) {
          ArrayType arrayType = (ArrayType) ((ParaList) fpl).P.T;
          argsTypes.append(VCtoJavaType(arrayType));
        }
        numberOfArgs++;

        fpl = ((ParaList) fpl).PL;
      }

      emit("invokevirtual", classname + "/" + fname + "(" + argsTypes + ")" + retType);
      frame.pop(numberOfArgs + 1);

      if (!retType.equals("V"))
        frame.push();
    }
    return null;
  }

  public Object visitEmptyExpr(EmptyExpr ast, Object o) {
    return null;
  }

  public Object visitIntExpr(IntExpr ast, Object o) {
    ast.IL.visit(this, o);
    return null;
  }

  public Object visitFloatExpr(FloatExpr ast, Object o) {
    ast.FL.visit(this, o);
    return null;
  }

  public Object visitBooleanExpr(BooleanExpr ast, Object o) {
    ast.BL.visit(this, o);
    return null;
  }

  public Object visitStringExpr(StringExpr ast, Object o) {
    ast.SL.visit(this, o);
    return null;
  }

  // Declarations

  public Object visitDeclList(DeclList ast, Object o) {
    ast.D.visit(this, o);
    ast.DL.visit(this, o);
    return null;
  }

  public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
    return null;
  }

  public Object visitFuncDecl(FuncDecl ast, Object o) {

    Frame frame;

    if (ast.I.spelling.equals("main")) {

      frame = new Frame(true);

      // Assume that main has one String parameter and reserve 0 for it
      frame.getNewIndex();

      emit(JVM.METHOD_START, "public static main([Ljava/lang/String;)V");
      // Assume implicitly that
      // classname vc$;
      // appears before all local variable declarations.
      // (1) Reserve 1 for this object reference.

      frame.getNewIndex();

    } else {

      frame = new Frame(false);

      // all other programmer-defined functions are treated as if
      // they were instance methods
      frame.getNewIndex(); // reserve 0 for "this"

      String retType = VCtoJavaType(ast.T);

      // The types of the parameters of the called function are not
      // directly available in the FuncDecl node but can be gathered
      // by traversing its field PL.

      StringBuffer argsTypes = new StringBuffer("");
      List fpl = ast.PL;
      while (!fpl.isEmpty()) {
        if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType))
          argsTypes.append("Z");
        else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType))
          argsTypes.append("I");
        else if (((ParaList) fpl).P.T.equals(StdEnvironment.floatType))
          argsTypes.append("F");
        else if (((ParaList) fpl).P.T.isArrayType()) {
          ArrayType type = (ArrayType) ((ParaList) fpl).P.T;
          argsTypes.append(VCtoJavaType(type));
        }
        fpl = ((ParaList) fpl).PL;
      }

      emit(JVM.METHOD_START, ast.I.spelling + "(" + argsTypes + ")" + retType);
    }

    ast.S.visit(this, frame);

    // JVM requires an explicit return in every method.
    // In VC, a function returning void may not contain a return, and
    // a function returning int or float is not guaranteed to contain
    // a return. Therefore, we add one at the end just to be sure.

    if (ast.T.equals(StdEnvironment.voidType)) {
      emit("");
      emit("; return may not be present in a VC function returning void");
      emit("; The following return inserted by the VC compiler");
      emit(JVM.RETURN);
    } else if (ast.I.spelling.equals("main")) {
      // In case VC's main does not have a return itself
      emit(JVM.RETURN);
    } else
      emit(JVM.NOP);

    emit("");
    emit("; set limits used by this method");
    emit(JVM.LIMIT, "locals", frame.getNewIndex());

    emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
    // changed by the marker
    // emit(JVM.LIMIT, "stack", 50);
    emit(JVM.METHOD_END, "method");

    return null;
  }

  public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
    // nothing to be done
    return null;
  }

  public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
    Frame frame = (Frame) o;
    ast.index = frame.getNewIndex();
    String T = VCtoJavaType(ast.T);

    emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + (String) frame.scopeStart.peek()
        + " to " + (String) frame.scopeEnd.peek());

    if (ast.T.isArrayType()) {
      // handle array decl eg. int a[5];
      ArrayType arrayType = (ArrayType) ast.T;
      // put size on top of the stack, size is guarenteed by checker?
      arrayType.E.visit(this, o);
      emit(JVM.NEWARRAY, arrayType.T.toString());
      frame.push();

      if (ast.E.isEmptyExpr()) {
        emitASTORE(ast.index);
        frame.pop();
      }
    }

    if (!ast.E.isEmptyExpr()) {
      ast.E.visit(this, o);

      if (ast.T.isArrayType()) {
        emitASTORE(ast.index);
      } else if (ast.T.equals(StdEnvironment.floatType)) {
        emitFSTORE(ast.I);
      } else { // for int and boolean
        emitISTORE(ast.I);
      }
      frame.pop();
    }

    return null;
  }

  // Parameters

  public Object visitParaList(ParaList ast, Object o) {
    ast.P.visit(this, o);
    ast.PL.visit(this, o);
    return null;
  }

  public Object visitParaDecl(ParaDecl ast, Object o) {
    Frame frame = (Frame) o;
    ast.index = frame.getNewIndex();
    String T = VCtoJavaType(ast.T);

    emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + (String) frame.scopeStart.peek()
        + " to " + (String) frame.scopeEnd.peek());
    return null;
  }

  public Object visitEmptyParaList(EmptyParaList ast, Object o) {
    return null;
  }

  // Arguments

  public Object visitArgList(ArgList ast, Object o) {
    ast.A.visit(this, o);
    ast.AL.visit(this, o);
    return null;
  }

  public Object visitArg(Arg ast, Object o) {
    ast.E.visit(this, o);
    return null;
  }

  public Object visitEmptyArgList(EmptyArgList ast, Object o) {
    return null;
  }

  // Types

  public Object visitIntType(IntType ast, Object o) {
    return null;
  }

  public Object visitFloatType(FloatType ast, Object o) {
    return null;
  }

  public Object visitBooleanType(BooleanType ast, Object o) {
    return null;
  }

  public Object visitVoidType(VoidType ast, Object o) {
    return null;
  }

  public Object visitErrorType(ErrorType ast, Object o) {
    return null;
  }

  // Literals, Identifiers and Operators

  public Object visitIdent(Ident ast, Object o) {
    return null;
  }

  public Object visitIntLiteral(IntLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emitICONST(Integer.parseInt(ast.spelling));
    frame.push();
    return null;
  }

  public Object visitFloatLiteral(FloatLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emitFCONST(Float.parseFloat(ast.spelling));
    frame.push();
    return null;
  }

  public Object visitBooleanLiteral(BooleanLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emitBCONST(ast.spelling.equals("true"));
    frame.push();
    return null;
  }

  public Object visitStringLiteral(StringLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emit(JVM.LDC, "\"" + ast.spelling + "\"");
    frame.push();
    return null;
  }

  public Object visitOperator(Operator ast, Object o) {
    return null;
  }

  // Variables

  public Object visitSimpleVar(SimpleVar ast, Object o) {
    // For load rather than store
    // store handled at visitAssignExpr
    Frame frame = (Frame) o;

    Decl decl = null;

    if (ast.I.decl instanceof GlobalVarDecl) {
      decl = (GlobalVarDecl) ast.I.decl;
      emitGETSTATIC(VCtoJavaType(decl.T), decl.I.spelling);
    } else {
      decl = (Decl) ast.I.decl; // Both ParaDecl and LocalVarDecl
      if (decl.T.isArrayType()) {
        emitALOAD(decl.index);
      } else if (decl.T.equals(StdEnvironment.floatType)) {
        emitFLOAD(decl.index);
      } else { // boolean and int type, var cannot be void type (checked by checker)
        emitILOAD(decl.index);
      }
    }

    frame.push();

    return null;
  }

  // Auxiliary methods for byte code generation

  // The following method appends an instruction directly into the JVM
  // Code Store. It is called by all other overloaded emit methods.

  private void emitASTORE(int index) {
    if (index < 4) {
      emit(JVM.ASTORE + "_" + index);
    } else {
      emit(JVM.ASTORE, index);
    }
  }

  private void emitALOAD(int index) {
    if (index < 4) {
      emit(JVM.ALOAD + "_" + index);
    } else {
      emit(JVM.ALOAD, index);
    }
  }

  private void emit(String s) {
    JVM.append(new Instruction(s));
    // uncomment to debug // JVM.dump("debug$.j");
  }

  private void emit(String s1, String s2) {
    emit(s1 + " " + s2);
  }

  private void emit(String s1, int i) {
    emit(s1 + " " + i);
  }

  private void emit(String s1, float f) {
    emit(s1 + " " + f);
  }

  private void emit(String s1, String s2, int i) {
    emit(s1 + " " + s2 + " " + i);
  }

  private void emit(String s1, String s2, String s3) {
    emit(s1 + " " + s2 + " " + s3);
  }

  private void emitIF_ICMPCOND(String op, Frame frame) {
    String opcode;

    if (op.equals("i!="))
      opcode = JVM.IF_ICMPNE;
    else if (op.equals("i=="))
      opcode = JVM.IF_ICMPEQ;
    else if (op.equals("i<"))
      opcode = JVM.IF_ICMPLT;
    else if (op.equals("i<="))
      opcode = JVM.IF_ICMPLE;
    else if (op.equals("i>"))
      opcode = JVM.IF_ICMPGT;
    else // if (op.equals("i>="))
      opcode = JVM.IF_ICMPGE;

    String falseLabel = frame.getNewLabel();
    String nextLabel = frame.getNewLabel();

    emit(opcode, falseLabel);
    frame.pop(2);
    emit(JVM.ICONST_0);
    emit(JVM.GOTO, nextLabel);
    emit(falseLabel + ":");
    emit(JVM.ICONST_1);
    frame.push();
    emit(nextLabel + ":");
  }

  private void emitFCMP(String op, Frame frame) {
    String opcode;

    if (op.equals("f!="))
      opcode = JVM.IFNE;
    else if (op.equals("f=="))
      opcode = JVM.IFEQ;
    else if (op.equals("f<"))
      opcode = JVM.IFLT;
    else if (op.equals("f<="))
      opcode = JVM.IFLE;
    else if (op.equals("f>"))
      opcode = JVM.IFGT;
    else // if (op.equals("f>="))
      opcode = JVM.IFGE;

    String falseLabel = frame.getNewLabel();
    String nextLabel = frame.getNewLabel();

    emit(JVM.FCMPG);
    frame.pop(2);
    emit(opcode, falseLabel);
    emit(JVM.ICONST_0);
    emit(JVM.GOTO, nextLabel);
    emit(falseLabel + ":");
    emit(JVM.ICONST_1);
    frame.push();
    emit(nextLabel + ":");

  }

  private void emitILOAD(int index) {
    if (index >= 0 && index <= 3)
      emit(JVM.ILOAD + "_" + index);
    else
      emit(JVM.ILOAD, index);
  }

  private void emitFLOAD(int index) {
    if (index >= 0 && index <= 3)
      emit(JVM.FLOAD + "_" + index);
    else
      emit(JVM.FLOAD, index);
  }

  private void emitGETSTATIC(String T, String I) {
    emit(JVM.GETSTATIC, classname + "/" + I, T);
  }

  private void emitISTORE(Ident ast) {
    int index;
    if (ast.decl instanceof ParaDecl)
      index = ((ParaDecl) ast.decl).index;
    else
      index = ((LocalVarDecl) ast.decl).index;

    if (index >= 0 && index <= 3)
      emit(JVM.ISTORE + "_" + index);
    else
      emit(JVM.ISTORE, index);
  }

  private void emitFSTORE(Ident ast) {
    int index;
    if (ast.decl instanceof ParaDecl)
      index = ((ParaDecl) ast.decl).index;
    else
      index = ((LocalVarDecl) ast.decl).index;
    if (index >= 0 && index <= 3)
      emit(JVM.FSTORE + "_" + index);
    else
      emit(JVM.FSTORE, index);
  }

  private void emitPUTSTATIC(String T, String I) {
    emit(JVM.PUTSTATIC, classname + "/" + I, T);
  }

  private void emitICONST(int value) {
    if (value == -1)
      emit(JVM.ICONST_M1);
    else if (value >= 0 && value <= 5)
      emit(JVM.ICONST + "_" + value);
    else if (value >= -128 && value <= 127)
      emit(JVM.BIPUSH, value);
    else if (value >= -32768 && value <= 32767)
      emit(JVM.SIPUSH, value);
    else
      emit(JVM.LDC, value);
  }

  private void emitFCONST(float value) {
    if (value == 0.0)
      emit(JVM.FCONST_0);
    else if (value == 1.0)
      emit(JVM.FCONST_1);
    else if (value == 2.0)
      emit(JVM.FCONST_2);
    else
      emit(JVM.LDC, value);
  }

  private void emitBCONST(boolean value) {
    if (value)
      emit(JVM.ICONST_1);
    else
      emit(JVM.ICONST_0);
  }

  private String VCtoJavaType(Type t) {
    if (t.isArrayType()) {
      return "[" + VCtoJavaType(((ArrayType) t).T);
    } else {
      if (t.equals(StdEnvironment.booleanType))
        return "Z";
      else if (t.equals(StdEnvironment.intType))
        return "I";
      else if (t.equals(StdEnvironment.floatType))
        return "F";
      else // if (t.equals(StdEnvironment.voidType))
        return "V";
    }
  }

  @Override
  public Object visitIfStmt(IfStmt ast, Object o) {
    Frame frame = (Frame) o;
    String l1 = frame.getNewLabel();
    String l2 = frame.getNewLabel();
    ast.E.visit(this, o);
    emit(JVM.IFEQ, l1);
    frame.pop();
    ast.S1.visit(this, o);
    emit(JVM.GOTO, l2);
    emit(l1 + ":");
    ast.S2.visit(this, o);
    emit(l2 + ":");
    return null;
  }

  @Override
  public Object visitWhileStmt(WhileStmt ast, Object o) {
    Frame frame = (Frame) o;
    String l1 = frame.getNewLabel();
    String l2 = frame.getNewLabel();
    // l1 is the start of the loop, continue needs to go to the start of the loop
    frame.conStack.push(l1);
    // l2 is the end of the loop, break needs to go to the end of the loop
    frame.brkStack.push(l2);
    emit(l1 + ":");
    ast.E.visit(this, o);
    // while condition is false, jump to l2, which is the end of the while loop
    emit(JVM.IFEQ, l2);
    frame.pop();
    ast.S.visit(this, o);
    emit(JVM.GOTO, l1);
    emit(l2 + ":");
    frame.conStack.pop();
    frame.brkStack.pop();
    return null;
  }

  @Override
  public Object visitForStmt(ForStmt ast, Object o) {
    Frame frame = (Frame) o;
    String l1 = frame.getNewLabel();
    String l2 = frame.getNewLabel();
    String l3 = frame.getNewLabel();

    frame.conStack.push(l3); // l1 is the start of the update statement, after which the loop repeats
    frame.brkStack.push(l2); // l2 is the end of the loop

    ast.E1.visit(this, o);
    // need to pop if this precondition is some useless shit
    // eg int a; for (1; ; ) {}
    // it will generate a iconst_1 that will need to be poped
    popIfNeeded(ast.E1, frame);

    emit(l1 + ":");
    ast.E2.visit(this, o);
    // if guard is false, jump to l2, which is the end of the for loop

    if (!(ast.E2 instanceof EmptyExpr)) {
      // handles the case where the guard is empty
      // eg. for (i = 0; ; i++) {}
      // do not generate any instructions, becuase there is nothing on the operand
      // stack for any IFNE or IFEQ to operate on
      emit(JVM.IFEQ, l2);
      frame.pop();
    }

    ast.S.visit(this, o);

    emit(l3 + ":"); // for continue to work correctly
    // visit post operation after the body of the loop
    ast.E3.visit(this, o);

    // same reason as above E1 popIfNeeded, because you cannot prevent programmers
    // from putting some random shit in the post expressions
    popIfNeeded(ast.E1, frame);

    emit(JVM.GOTO, l1);
    emit(l2 + ":");

    frame.brkStack.pop();
    frame.conStack.pop();
    return null;
  }

  @Override
  public Object visitBreakStmt(BreakStmt ast, Object o) {
    Frame frame = (Frame) o;
    String label = frame.brkStack.peek();
    emit(JVM.GOTO, label);
    return null;
  }

  @Override
  public Object visitContinueStmt(ContinueStmt ast, Object o) {
    Frame frame = (Frame) o;
    String label = frame.conStack.peek();
    emit(JVM.GOTO, label);
    return null;
  }

  private void popIfNeeded(Expr expr, Frame frame) {

    if (expr instanceof CallExpr
        && expr.type.isVoidType()) {
      // f();
      // If function has a return type, it has to leave something on the stack, as it
      // is invoked inside another function, its return value can be discarded or
      // poped by the other function
      // so:
      // int f() { return 1; }
      // int g() { f(); return 2; }
      // when f is called in g, its return value is left on top of the stack, we need
      // to generate a pop to discard it becuase it is not assigned
      //
      // if f is a void function, it does not leave anything on the stack, so we do
      // not need to generate a pop
      return;
    }

    if (expr instanceof EmptyExpr) {
      // ;
      return;
    }

    if (expr instanceof AssignExpr) {
      // a = b;
      return;
    }

    emit(JVM.POP);
    frame.pop();
  }

  @Override
  public Object visitExprStmt(ExprStmt ast, Object o) {
    ast.E.visit(this, o);

    popIfNeeded(ast.E, (Frame) o);

    return null;
  }

  @Override
  public Object visitUnaryExpr(UnaryExpr ast, Object o) {
    Frame frame = (Frame) o;
    String op = ast.O.spelling;

    ast.E.visit(this, o);
    if (op.equals("i2f")) {
      emit(JVM.I2F);
    } else if (op.equals("i-")) {
      emit(JVM.INEG);
    } else if (op.equals("f-")) {
      emit(JVM.FNEG);
    } else if (op.equals("i!")) {
      emit(JVM.ICONST_1);
      frame.push();
      emit(JVM.IXOR);
      // IXOR consumes 2 operands and pushes 1
      frame.pop();
    }
    return null;
  }

  @Override
  public Object visitBinaryExpr(BinaryExpr ast, Object o) {
    Frame frame = (Frame) o;
    String op = ast.O.spelling;
    HashMap<String, String> arithmeticOp = new HashMap<>() {
      {
        put("i+", JVM.IADD);
        put("i-", JVM.ISUB);
        put("i*", JVM.IMUL);
        put("i/", JVM.IDIV);
        put("f+", JVM.FADD);
        put("f-", JVM.FSUB);
        put("f*", JVM.FMUL);
        put("f/", JVM.FDIV);
      }
    };

    HashSet<String> cmpOp = new HashSet<>() {
      {
        add("i==");
        add("i!=");
        add("i<");
        add("i<=");
        add("i>");
        add("i>=");
        add("f==");
        add("f!=");
        add("f<");
        add("f<=");
        add("f>");
        add("f>=");
      }
    };

    if (arithmeticOp.containsKey(op)) {
      ast.E1.visit(this, o);
      ast.E2.visit(this, o);
      emit(arithmeticOp.get(op));
      // arithmetic operations consume 2 operands and push 1
      frame.pop();
    } else if (cmpOp.contains(op)) {
      ast.E1.visit(this, o);
      ast.E2.visit(this, o);
      if (op.contains("f")) {
        emitFCMP(op, frame);
      } else if (op.contains("i")) {
        emitIF_ICMPCOND(op, frame);
      }
    } else if (op.equals("i&&")) {
      String L1 = frame.getNewLabel();
      String L2 = frame.getNewLabel();
      ast.E1.visit(this, o);
      emit(JVM.IFEQ, L1);
      frame.pop();
      ast.E2.visit(this, o);
      emit(JVM.IFEQ, L1);
      emitICONST(1);
      emit(JVM.GOTO, L2);
      emit(L1 + ":");
      emitICONST(0);
      frame.push();
      emit(L2 + ":");
    } else if (op.equals("i||")) {
      String L1 = frame.getNewLabel();
      String L2 = frame.getNewLabel();
      ast.E1.visit(this, o);
      emit(JVM.IFNE, L1);
      frame.pop();
      ast.E2.visit(this, o);
      emit(JVM.IFNE, L1);
      emitICONST(0);
      emit(JVM.GOTO, L2);
      emit(L1 + ":");
      emitICONST(1);
      frame.push();
      emit(L2 + ":");
    }
    return null;
  }

  @Override
  public Object visitArrayInitExpr(ArrayInitExpr ast, Object o) {

    Frame frame = (Frame) o;

    List arrayExprList = ast.IL;

    while (!(arrayExprList instanceof EmptyArrayExprList)) {

      ArrayExprList currExprList = (ArrayExprList) arrayExprList;

      // we have access to E
      emit(JVM.DUP);
      frame.push();

      emitICONST(currExprList.index);
      frame.push();

      currExprList.E.visit(this, o);

      // generate store instruction
      if (currExprList.E.type.isFloatType()) {
        emit(JVM.FASTORE);
      } else if (currExprList.E.type.isBooleanType()) {
        emit(JVM.BASTORE);
      } else { // if (currExprList.E.type.isIntType())
        emit(JVM.IASTORE);
      }

      // iastore, bastore, fastore consumes 3 operands

      frame.pop(3);

      arrayExprList = currExprList.EL;
    }

    return null;
  }

  @Override
  public Object visitArrayExprList(ArrayExprList ast, Object o) {
    // dont need to do anything, handled in visitArrayInitExpr
    return null;
  }

  @Override
  public Object visitEmptyArrayExprList(EmptyArrayExprList ast, Object o) {
    return null;
  }

  @Override
  public Object visitArrayExpr(ArrayExpr ast, Object o) {
    Frame frame = (Frame) o;

    ast.V.visit(this, o); // generates aload
    ast.E.visit(this, o); // gnerates iconst_<n>
    // generates iaload or faload
    if (ast.type.isFloatType()) {
      emit(JVM.FALOAD);
    } else if (ast.type.isBooleanType()) {
      emit(JVM.BALOAD);
    } else {
      emit(JVM.IALOAD);
    }

    // iaload, baload, faload consumes 2 operands and pushes 1 operand
    frame.pop();
    return null;
  }

  @Override
  public Object visitVarExpr(VarExpr ast, Object o) {
    ast.V.visit(this, o);
    return null;
  }

  @Override
  public Object visitAssignExpr(AssignExpr ast, Object o) {

    Frame frame = (Frame) o;
    if (ast.E1 instanceof VarExpr) {
      ast.E2.visit(this, o);
      VarExpr varExpr = (VarExpr) ast.E1;
      // currently we only have SimpleVar for Var type, safe to cast
      SimpleVar simpleVar = (SimpleVar) varExpr.V;
      Decl decl = (Decl) simpleVar.I.decl;

      if (ast.parent instanceof AssignExpr) {
        // For assignment chaining
        // int a = b = c = 1;
        emit(JVM.DUP);
        frame.push();
      }

      // Generate store instruction

      if (decl instanceof GlobalVarDecl) {
        emitPUTSTATIC(VCtoJavaType(simpleVar.type), simpleVar.I.spelling);
      } else {
        // LocalVarDecl and ParaDecl
        if (ast.E1.type.isFloatType()) {
          emitFSTORE(simpleVar.I);
        } else if (ast.E1.type.isBooleanType() || ast.E1.type.isIntType()) { // int, boolean
          emitISTORE(simpleVar.I);
        }
      }
      // istore, fstore, puststatic consumes 1 operand
      frame.pop();

    } else if (ast.E1 instanceof ArrayExpr) {

      // Dont need the iaload or faload instruction for LHS array expressions, so
      // instead of calling ast.E1.visit(this, o); we manually visit both V and E of
      // ast.E1

      ArrayExpr arrayExpr = (ArrayExpr) ast.E1;
      arrayExpr.V.visit(this, o);
      arrayExpr.E.visit(this, o);
      // eg
      // aload_1
      // iconst_<index>

      ast.E2.visit(this, o);
      // eg iconst_1 ...

      // generate lvalue store instruction
      if (ast.E2.type.isFloatType()) {
        emit(JVM.FASTORE);
      } else if (ast.E2.type.isBooleanType()) {
        emit(JVM.BASTORE);
      } else {
        emit(JVM.IASTORE);
      }
      frame.pop();
    }

    return null;
  }

  @Override
  public Object visitStringType(StringType ast, Object o) {
    return null;
  }

  @Override
  public Object visitArrayType(ArrayType ast, Object o) {
    return null;
  }

}
