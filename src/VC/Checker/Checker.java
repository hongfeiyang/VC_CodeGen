//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package VC.Checker;

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
import VC.Scanner.SourcePosition;

public final class Checker implements Visitor {
  private String[] errMesg = new String[]{"*0: main function is missing", "*1: return type of main is not int", "*2: identifier redeclared", "*3: identifier declared void", "*4: identifier declared void[]", "*5: identifier undeclared", "*6: incompatible type for =", "*7: invalid lvalue in assignment", "*8: incompatible type for return", "*9: incompatible type for this binary operator", "*10: incompatible type for this unary operator", "*11: attempt to use an array/function as a scalar", "*12: attempt to use a scalar/function as an array", "*13: wrong type for element in array initialiser", "*14: invalid initialiser: array initialiser for scalar", "*15: invalid initialiser: scalar initialiser for array", "*16: excess elements in array initialiser", "*17: array subscript is not an integer", "*18: array size missing", "*19: attempt to reference a scalar/array as a function", "*20: if conditional is not boolean", "*21: for conditional is not boolean", "*22: while conditional is not boolean", "*23: break must be in a while/for", "*24: continue must be in a while/for", "*25: too many actual parameters", "*26: too few actual parameters", "*27: wrong type for actual parameter", "*28: misc 1", "*29: misc 2", "*30: statement(s) not reached", "*31: missing return statement"};
  private SymbolTable idTable;
  private static SourcePosition dummyPos = new SourcePosition();
  private ErrorReporter reporter;
  private int whileLevel = 0;
  private static final Ident dummyI;

  public Checker(ErrorReporter var1) {
    this.reporter = var1;
    this.idTable = new SymbolTable();
    this.establishStdEnvironment();
  }

  private Expr i2f(Expr var1) {
    UnaryExpr var2 = new UnaryExpr(new Operator("i2f", var1.position), var1, var1.position);
    var2.type = StdEnvironment.floatType;
    var2.parent = var1;
    return var2;
  }

  private Expr checkAssignment(Type var1, Expr var2, String var3, SourcePosition var4) {
    if (!var1.assignable(var2.type)) {
      this.reporter.reportError(var3, "", var4);
    } else if (!var1.equals(var2.type)) {
      return this.i2f(var2);
    }

    return var2;
  }

  private void declareVariable(Ident var1, Decl var2) {
    IdEntry var3 = this.idTable.retrieveOneLevel(var1.spelling);
    if (var3 != null) {
      this.reporter.reportError(this.errMesg[2] + ": %", var1.spelling, var1.position);
    }

    this.idTable.insert(var1.spelling, var2);
    var1.visit(this, (Object)null);
  }

  private void declareFunction(Ident var1, Decl var2) {
    IdEntry var3 = this.idTable.retrieveOneLevel(var1.spelling);
    if (var3 != null) {
      this.reporter.reportError(this.errMesg[2] + ": %", var1.spelling, var1.position);
    }

    this.idTable.insert(var1.spelling, var2);
  }

  void reportError(String var1, Type var2, String var3, SourcePosition var4) {
    if (var2 == StdEnvironment.errorType) {
      this.reporter.reportError(var1, "", var4);
    } else {
      this.reporter.reportError(var1 + " (found: " + var2 + ", required: " + var3 + ")", "", var4);
    }

  }

  public void check(AST var1) {
    var1.visit(this, (Object)null);
  }

  public Object visitProgram(Program var1, Object var2) {
    var1.FL.visit(this, (Object)null);
    Decl var3 = this.idTable.retrieve("main");
    if (var3 != null && var3 instanceof FuncDecl) {
      if (!StdEnvironment.intType.equals(((FuncDecl)var3).T)) {
        this.reporter.reportError(this.errMesg[1], "", var1.position);
      }
    } else {
      this.reporter.reportError(this.errMesg[0], "", var1.position);
    }

    return null;
  }

  public Object visitIfStmt(IfStmt var1, Object var2) {
    Type var3 = (Type)var1.E.visit(this, (Object)null);
    if (!var3.equals(StdEnvironment.booleanType)) {
      this.reporter.reportError(this.errMesg[20] + " (found: " + var3.toString() + ")", "", var1.E.position);
    }

    var1.S1.visit(this, var2);
    var1.S2.visit(this, var2);
    return null;
  }

  public Object visitCompoundStmt(CompoundStmt var1, Object var2) {
    this.idTable.openScope();
    if (var2 != null && var2 instanceof FuncDecl) {
      FuncDecl var3 = (FuncDecl)var2;
      var3.PL.visit(this, (Object)null);
      var1.DL.visit(this, (Object)null);
      var1.SL.visit(this, (Type)var3.T.visit(this, (Object)null));
    } else {
      var1.DL.visit(this, (Object)null);
      var1.SL.visit(this, var2);
    }

    this.idTable.closeScope();
    return null;
  }

  public Object visitStmtList(StmtList var1, Object var2) {
    var1.S.visit(this, var2);
    if (var1.S instanceof ReturnStmt && var1.SL instanceof StmtList) {
      this.reporter.reportError(this.errMesg[30], "", var1.SL.position);
    }

    var1.SL.visit(this, var2);
    return null;
  }

  public Object visitForStmt(ForStmt var1, Object var2) {
    ++this.whileLevel;
    var1.E1.visit(this, (Object)null);
    Type var3 = (Type)var1.E2.visit(this, (Object)null);
    if (!var1.E2.isEmptyExpr() && !var3.equals(StdEnvironment.booleanType)) {
      this.reporter.reportError(this.errMesg[21] + " (found: " + var3.toString() + ")", "", var1.E2.position);
    }

    var1.E3.visit(this, (Object)null);
    var1.S.visit(this, var2);
    --this.whileLevel;
    return null;
  }

  public Object visitWhileStmt(WhileStmt var1, Object var2) {
    ++this.whileLevel;
    Type var3 = (Type)var1.E.visit(this, (Object)null);
    if (!var3.equals(StdEnvironment.booleanType)) {
      this.reporter.reportError(this.errMesg[22] + " (found: " + var3.toString() + ")", "", var1.E.position);
    }

    var1.S.visit(this, var2);
    --this.whileLevel;
    return null;
  }

  public Object visitBreakStmt(BreakStmt var1, Object var2) {
    if (this.whileLevel < 1) {
      this.reporter.reportError(this.errMesg[23], "", var1.position);
    }

    return null;
  }

  public Object visitContinueStmt(ContinueStmt var1, Object var2) {
    if (this.whileLevel < 1) {
      this.reporter.reportError(this.errMesg[24], "", var1.position);
    }

    return null;
  }

  public Object visitReturnStmt(ReturnStmt var1, Object var2) {
    Type var3 = (Type)var2;
    var1.E.visit(this, var2);
    var1.E = this.checkAssignment(var3, var1.E, this.errMesg[8], var1.position);
    return null;
  }

  public Object visitExprStmt(ExprStmt var1, Object var2) {
    var1.E.visit(this, var2);
    return null;
  }

  public Object visitEmptyCompStmt(EmptyCompStmt var1, Object var2) {
    this.idTable.openScope();
    if (var2 != null && var2 instanceof FuncDecl) {
      FuncDecl var3 = (FuncDecl)var2;
      var3.PL.visit(this, (Object)null);
    }

    this.idTable.closeScope();
    return null;
  }

  public Object visitEmptyStmt(EmptyStmt var1, Object var2) {
    return null;
  }

  public Object visitEmptyStmtList(EmptyStmtList var1, Object var2) {
    return null;
  }

  public Object visitAssignExpr(AssignExpr var1, Object var2) {
    var1.E1.visit(this, var2);
    var1.E2.visit(this, (Object)null);
    if (!(var1.E1 instanceof VarExpr) && !(var1.E1 instanceof ArrayExpr)) {
      this.reporter.reportError(this.errMesg[7], "", var1.position);
    } else if (var1.E1 instanceof VarExpr) {
      SimpleVar var3 = (SimpleVar)((VarExpr)var1.E1).V;
      Decl var4 = (Decl)var3.I.decl;
      if (var4 instanceof FuncDecl) {
        this.reporter.reportError(this.errMesg[7] + ": %", var3.I.spelling, var1.position);
      }
    }

    var1.E2 = this.checkAssignment(var1.E1.type, var1.E2, this.errMesg[6], var1.position);
    if (!var1.E1.type.assignable(var1.E2.type)) {
      var1.type = StdEnvironment.errorType;
    } else {
      var1.type = var1.E2.type;
    }

    return var1.type;
  }

  public Object visitBinaryExpr(BinaryExpr var1, Object var2) {
    Type var3 = (Type)var1.E1.visit(this, var2);
    Type var4 = (Type)var1.E2.visit(this, var2);
    Type var5 = var3;
    String var6 = var1.O.spelling;
    boolean var7 = false;
    boolean var8 = var6.equals("&&") || var6.equals("||");
    boolean var9 = var6.equals("==") || var6.equals("!=");
    boolean var10 = var6.equals("<=") || var6.equals(">=") || var6.equals("<") || var6.equals(">");
    if (!var3.isErrorType() && !var4.isErrorType()) {
      if (!var3.isVoidType() && !var4.isVoidType()) {
        if (!var3.isStringType() && !var4.isStringType()) {
          if (!var3.isArrayType() && !var4.isArrayType()) {
            if (!var3.isBooleanType() && !var4.isBooleanType()) {
              if (var8) {
                var7 = true;
              } else if (!var3.equals(var4)) {
                var5 = StdEnvironment.floatType;
                var1.O.spelling = "f" + var1.O.spelling;
                if (!var5.equals(var3)) {
                  var1.E1 = this.i2f(var1.E1);
                } else {
                  var1.E2 = this.i2f(var1.E2);
                }
              } else if (var3.isFloatType()) {
                var1.O.spelling = "f" + var1.O.spelling;
              } else {
                var1.O.spelling = "i" + var1.O.spelling;
              }
            } else {
              if (!var3.equals(var4) || !var8 && !var9) {
                var7 = true;
              }

              var1.O.spelling = "i" + var1.O.spelling;
            }
          } else {
            var7 = true;
          }
        } else {
          var7 = true;
        }
      } else {
        var7 = true;
      }
    } else {
      var5 = StdEnvironment.errorType;
    }

    if (var7) {
      this.reporter.reportError(this.errMesg[9] + ": %", var6, var1.position);
      var5 = StdEnvironment.errorType;
    }

    var1.type = !var9 && !var10 ? var5 : StdEnvironment.booleanType;
    return var1.type;
  }

  public Object visitUnaryExpr(UnaryExpr var1, Object var2) {
    Type var3 = (Type)var1.E.visit(this, var2);
    String var4 = var1.O.spelling;
    boolean var5 = false;
    if (var3.isErrorType()) {
      var3 = StdEnvironment.errorType;
    } else if (!var3.isVoidType() && !var3.isStringType() && !var3.isArrayType()) {
      if (var4.equals("!") && !var3.isBooleanType() || !var4.equals("!") && var3.isBooleanType()) {
        var5 = true;
      }
    } else {
      var5 = true;
    }

    if (var5) {
      this.reporter.reportError(this.errMesg[10] + ": %", var4, var1.position);
      var3 = StdEnvironment.errorType;
    } else if (var3.isFloatType()) {
      var1.O.spelling = "f" + var1.O.spelling;
    } else {
      var1.O.spelling = "i" + var1.O.spelling;
    }

    var1.type = var3;
    return var1.type;
  }

  public Object visitCallExpr(CallExpr var1, Object var2) {
    Decl var3 = (Decl)var1.I.visit(this, (Object)null);
    if (var3 == null) {
      this.reporter.reportError(this.errMesg[5] + ": %", var1.I.spelling, var1.position);
      var1.type = StdEnvironment.errorType;
    } else if (var3 instanceof FuncDecl) {
      var1.AL.visit(this, ((FuncDecl)var3).PL);
      var1.type = ((FuncDecl)var3).T;
    } else {
      this.reporter.reportError(this.errMesg[19] + ": %", var1.I.spelling, var1.I.position);
      var1.type = StdEnvironment.errorType;
    }

    return var1.type;
  }

  public Object visitArrayExpr(ArrayExpr var1, Object var2) {
    Type var3 = (Type)var1.V.visit(this, var2);
    if (var3.isArrayType()) {
      var3 = ((ArrayType)var3).T;
    } else if (!var3.isErrorType()) {
      this.reporter.reportError(this.errMesg[12], "", var1.position);
      var3 = StdEnvironment.errorType;
    }

    Type var4 = (Type)var1.E.visit(this, var2);
    if (!var4.isIntType() && !var4.isErrorType()) {
      this.reporter.reportError(this.errMesg[17], "", var1.position);
    }

    var1.type = var3;
    return var3;
  }

  public Object visitArrayInitExpr(ArrayInitExpr var1, Object var2) {
    Type var3 = (Type)var2;
    if (!var3.isArrayType()) {
      this.reporter.reportError(this.errMesg[14], " ", var1.position);
      var1.type = StdEnvironment.errorType;
      return var1.type;
    } else {
      return var1.IL.visit(this, ((ArrayType)var3).T);
    }
  }

  public Object visitArrayExprList(ArrayExprList var1, Object var2) {
    Type var3 = (Type)var2;
    var1.E.visit(this, var2);
    var1.E = this.checkAssignment(var3, var1.E, this.errMesg[13] + ": at position " + var1.index, var1.E.position);
    if (var1.EL instanceof ArrayExprList) {
      ((ArrayExprList)var1.EL).index = var1.index + 1;
      return var1.EL.visit(this, var2);
    } else {
      return var1.index + 1;
    }
  }

  public Object visitEmptyArrayExprList(EmptyArrayExprList var1, Object var2) {
    return null;
  }

  public Object visitEmptyExpr(EmptyExpr var1, Object var2) {
    if (var1.parent instanceof ReturnStmt) {
      var1.type = StdEnvironment.voidType;
    } else {
      var1.type = StdEnvironment.errorType;
    }

    return var1.type;
  }

  public Object visitBooleanExpr(BooleanExpr var1, Object var2) {
    var1.type = StdEnvironment.booleanType;
    return var1.type;
  }

  public Object visitIntExpr(IntExpr var1, Object var2) {
    var1.type = StdEnvironment.intType;
    return var1.type;
  }

  public Object visitFloatExpr(FloatExpr var1, Object var2) {
    var1.type = StdEnvironment.floatType;
    return var1.type;
  }

  public Object visitVarExpr(VarExpr var1, Object var2) {
    var1.type = (Type)var1.V.visit(this, (Object)null);
    return var1.type;
  }

  public Object visitStringExpr(StringExpr var1, Object var2) {
    var1.type = StdEnvironment.stringType;
    return var1.type;
  }

  public Object visitFuncDecl(FuncDecl var1, Object var2) {
    this.declareFunction(var1.I, var1);
    if (var1.S.isEmptyCompStmt() && !var1.T.equals(StdEnvironment.voidType)) {
      this.reporter.reportError(this.errMesg[31], "", var1.position);
    }

    var1.S.visit(this, var1);
    return null;
  }

  public Object visitDeclList(DeclList var1, Object var2) {
    var1.D.visit(this, (Object)null);
    var1.DL.visit(this, (Object)null);
    return null;
  }

  public Object visitEmptyDeclList(EmptyDeclList var1, Object var2) {
    return null;
  }

  public Object visitGlobalVarDecl(GlobalVarDecl var1, Object var2) {
    this.declareVariable(var1.I, var1);
    if (var1.T.isVoidType()) {
      this.reporter.reportError(this.errMesg[3] + ": %", var1.I.spelling, var1.I.position);
    } else if (var1.T.isArrayType()) {
      if (((ArrayType)var1.T).T.isVoidType()) {
        this.reporter.reportError(this.errMesg[4] + ": %", var1.I.spelling, var1.I.position);
      }

      if (((ArrayType)var1.T).E.isEmptyExpr() && !(var1.E instanceof ArrayInitExpr)) {
        this.reporter.reportError(this.errMesg[18] + ": %", var1.I.spelling, var1.I.position);
      }
    }

    Object var3 = var1.E.visit(this, var1.T);
    if (var1.T.isArrayType()) {
      if (var1.E instanceof ArrayInitExpr) {
        Integer var4 = (Integer)var3;
        ArrayType var5 = (ArrayType)var1.T;
        if (var5.E.isEmptyExpr()) {
          var5.E = new IntExpr(new IntLiteral(var4.toString(), dummyPos), dummyPos);
        } else {
          int var6 = Integer.parseInt(((IntExpr)var5.E).IL.spelling);
          int var7 = var4;
          if (var6 < var7) {
            this.reporter.reportError(this.errMesg[16] + ": %", var1.I.spelling, var1.position);
          }
        }
      } else if (!var1.E.isEmptyExpr()) {
        this.reporter.reportError(this.errMesg[15] + ": %", var1.I.spelling, var1.position);
      }
    } else {
      var1.E = this.checkAssignment(var1.T, var1.E, this.errMesg[6], var1.position);
    }

    return null;
  }

  public Object visitLocalVarDecl(LocalVarDecl var1, Object var2) {
    this.declareVariable(var1.I, var1);
    if (var1.T.isVoidType()) {
      this.reporter.reportError(this.errMesg[3] + ": %", var1.I.spelling, var1.I.position);
    } else if (var1.T.isArrayType()) {
      if (((ArrayType)var1.T).T.isVoidType()) {
        this.reporter.reportError(this.errMesg[4] + ": %", var1.I.spelling, var1.I.position);
      }

      if (((ArrayType)var1.T).E.isEmptyExpr() && !(var1.E instanceof ArrayInitExpr)) {
        this.reporter.reportError(this.errMesg[18] + ": %", var1.I.spelling, var1.I.position);
      }
    }

    Object var3 = var1.E.visit(this, var1.T);
    if (var1.T.isArrayType()) {
      if (var1.E instanceof ArrayInitExpr) {
        Integer var4 = (Integer)var3;
        ArrayType var5 = (ArrayType)var1.T;
        if (var5.E.isEmptyExpr()) {
          var5.E = new IntExpr(new IntLiteral(var4.toString(), dummyPos), dummyPos);
        } else {
          int var6 = Integer.parseInt(((IntExpr)var5.E).IL.spelling);
          int var7 = var4;
          if (var6 < var7) {
            this.reporter.reportError(this.errMesg[16] + ": %", var1.I.spelling, var1.position);
          }
        }
      } else if (!var1.E.isEmptyExpr()) {
        this.reporter.reportError(this.errMesg[15] + ": %", var1.I.spelling, var1.position);
      }
    } else {
      var1.E = this.checkAssignment(var1.T, var1.E, this.errMesg[6], var1.position);
    }

    return null;
  }

  public Object visitParaList(ParaList var1, Object var2) {
    var1.P.visit(this, (Object)null);
    var1.PL.visit(this, (Object)null);
    return null;
  }

  public Object visitParaDecl(ParaDecl var1, Object var2) {
    this.declareVariable(var1.I, var1);
    if (var1.T.isVoidType()) {
      this.reporter.reportError(this.errMesg[3] + ": %", var1.I.spelling, var1.I.position);
    } else if (var1.T.isArrayType() && ((ArrayType)var1.T).T.isVoidType()) {
      this.reporter.reportError(this.errMesg[4] + ": %", var1.I.spelling, var1.I.position);
    }

    return null;
  }

  public Object visitEmptyParaList(EmptyParaList var1, Object var2) {
    return null;
  }

  public Object visitEmptyArgList(EmptyArgList var1, Object var2) {
    List var3 = (List)var2;
    if (!var3.isEmptyParaList()) {
      this.reporter.reportError(this.errMesg[26], "", var1.position);
    }

    return null;
  }

  public Object visitArgList(ArgList var1, Object var2) {
    List var3 = (List)var2;
    if (var3.isEmptyParaList()) {
      this.reporter.reportError(this.errMesg[25], "", var1.position);
    } else {
      var1.A.visit(this, ((ParaList)var3).P);
      var1.AL.visit(this, ((ParaList)var3).PL);
    }

    return null;
  }

  public Object visitArg(Arg var1, Object var2) {
    ParaDecl var3 = (ParaDecl)var2;
    Type var4 = (Type)var1.E.visit(this, (Object)null);
    boolean var6 = false;
    Type var5 = var3.T;
    if (var5.isArrayType()) {
      if (!var4.isArrayType()) {
        var6 = true;
      } else {
        Type var7 = ((ArrayType)var5).T;
        Type var8 = ((ArrayType)var4).T;
        if (!var7.assignable(var8)) {
          var6 = true;
        }
      }
    } else if (!var3.T.assignable(var4)) {
      var6 = true;
    }

    if (var6) {
      this.reporter.reportError(this.errMesg[27] + ": %", var3.I.spelling, var1.E.position);
    }

    if (var3.T.equals(StdEnvironment.floatType) && var4.equals(StdEnvironment.intType)) {
      var1.E = this.i2f(var1.E);
    }

    return null;
  }

  public Object visitErrorType(ErrorType var1, Object var2) {
    return StdEnvironment.errorType;
  }

  public Object visitBooleanType(BooleanType var1, Object var2) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntType(IntType var1, Object var2) {
    return StdEnvironment.intType;
  }

  public Object visitFloatType(FloatType var1, Object var2) {
    return StdEnvironment.floatType;
  }

  public Object visitStringType(StringType var1, Object var2) {
    return StdEnvironment.stringType;
  }

  public Object visitVoidType(VoidType var1, Object var2) {
    return StdEnvironment.voidType;
  }

  public Object visitArrayType(ArrayType var1, Object var2) {
    return var1;
  }

  public Object visitIdent(Ident var1, Object var2) {
    Decl var3 = this.idTable.retrieve(var1.spelling);
    if (var3 != null) {
      var1.decl = var3;
    }

    return var3;
  }

  public Object visitBooleanLiteral(BooleanLiteral var1, Object var2) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntLiteral(IntLiteral var1, Object var2) {
    return StdEnvironment.intType;
  }

  public Object visitFloatLiteral(FloatLiteral var1, Object var2) {
    return StdEnvironment.floatType;
  }

  public Object visitStringLiteral(StringLiteral var1, Object var2) {
    return StdEnvironment.stringType;
  }

  public Object visitOperator(Operator var1, Object var2) {
    return null;
  }

  public Object visitSimpleVar(SimpleVar var1, Object var2) {
    var1.type = StdEnvironment.errorType;
    Decl var3 = (Decl)var1.I.visit(this, (Object)null);
    if (var3 == null) {
      this.reporter.reportError(this.errMesg[5] + ": %", var1.I.spelling, var1.position);
    } else if (var3 instanceof FuncDecl) {
      this.reporter.reportError(this.errMesg[11] + ": %", var1.I.spelling, var1.I.position);
    } else {
      var1.type = var3.T;
    }

    if (var1.type.isArrayType() && var1.parent instanceof VarExpr && !(var1.parent.parent instanceof Arg)) {
      this.reporter.reportError(this.errMesg[11] + ": %", var1.I.spelling, var1.I.position);
    }

    return var1.type;
  }

  private FuncDecl declareStdFunc(Type var1, String var2, List var3) {
    FuncDecl var4 = new FuncDecl(var1, new Ident(var2, dummyPos), var3, new EmptyStmt(dummyPos), dummyPos);
    this.idTable.insert(var2, var4);
    return var4;
  }

  private void establishStdEnvironment() {
    StdEnvironment.booleanType = new BooleanType(dummyPos);
    StdEnvironment.intType = new IntType(dummyPos);
    StdEnvironment.floatType = new FloatType(dummyPos);
    StdEnvironment.stringType = new StringType(dummyPos);
    StdEnvironment.voidType = new VoidType(dummyPos);
    StdEnvironment.errorType = new ErrorType(dummyPos);
    StdEnvironment.getIntDecl = this.declareStdFunc(StdEnvironment.intType, "getInt", new EmptyParaList(dummyPos));
    StdEnvironment.putIntDecl = this.declareStdFunc(StdEnvironment.voidType, "putInt", new ParaList(new ParaDecl(StdEnvironment.intType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putIntLnDecl = this.declareStdFunc(StdEnvironment.voidType, "putIntLn", new ParaList(new ParaDecl(StdEnvironment.intType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.getFloatDecl = this.declareStdFunc(StdEnvironment.floatType, "getFloat", new EmptyParaList(dummyPos));
    StdEnvironment.putFloatDecl = this.declareStdFunc(StdEnvironment.voidType, "putFloat", new ParaList(new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putFloatLnDecl = this.declareStdFunc(StdEnvironment.voidType, "putFloatLn", new ParaList(new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putBoolDecl = this.declareStdFunc(StdEnvironment.voidType, "putBool", new ParaList(new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putBoolLnDecl = this.declareStdFunc(StdEnvironment.voidType, "putBoolLn", new ParaList(new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putStringLnDecl = this.declareStdFunc(StdEnvironment.voidType, "putStringLn", new ParaList(new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putStringDecl = this.declareStdFunc(StdEnvironment.voidType, "putString", new ParaList(new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
    StdEnvironment.putLnDecl = this.declareStdFunc(StdEnvironment.voidType, "putLn", new EmptyParaList(dummyPos));
  }

  static {
    dummyI = new Ident("x", dummyPos);
  }
}
