//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package VC.Parser;

import VC.ErrorReporter;
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
import VC.ASTs.EmptyStmtList;
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
import VC.ASTs.Stmt;
import VC.ASTs.StmtList;
import VC.ASTs.StringExpr;
import VC.ASTs.StringLiteral;
import VC.ASTs.Type;
import VC.ASTs.UnaryExpr;
import VC.ASTs.VarExpr;
import VC.ASTs.VoidType;
import VC.ASTs.WhileStmt;
import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

public class Parser {
  private Scanner DL;
  private ErrorReporter I;
  private Token T;
  private SourcePosition charFinish;
  private SourcePosition charStart = new SourcePosition();

  public Parser(Scanner var1, ErrorReporter var2) {
    this.DL = var1;
    this.I = var2;
    this.charFinish = new SourcePosition();
    this.T = this.DL.getToken();
  }

  private final void DL(int var1) throws SyntaxError {
    if (this.T.kind == var1) {
      this.charFinish = this.T.position;
      this.T = this.DL.getToken();
    } else {
      this.T("\"%\" expected here", Token.spell(var1));
    }

  }

  private final void I() {
    this.charFinish = this.T.position;
    this.T = this.DL.getToken();
  }

  private final void T(String var1, String var2) throws SyntaxError {
    SourcePosition var3 = this.T.position;
    this.I.reportError(var1, var2, var3);
    throw new SyntaxError();
  }

  private final void charFinish(SourcePosition var1) {
    var1.lineStart = this.T.position.lineStart;
    var1.charStart = this.T.position.charStart;
  }

  private final void charStart(SourcePosition var1) {
    var1.lineFinish = this.charFinish.lineFinish;
    var1.charFinish = this.charFinish.charFinish;
  }

  private final void getToken(SourcePosition var1, SourcePosition var2) {
    var2.lineStart = var1.lineStart;
    var2.charStart = var1.charStart;
  }

  private final Type kind(Type var1) {
    SourcePosition var3 = var1.position;
    Object var2;
    if (var1 instanceof IntType) {
      var2 = new IntType(var3);
    } else if (var1 instanceof FloatType) {
      var2 = new FloatType(var3);
    } else if (var1 instanceof BooleanType) {
      var2 = new BooleanType(var3);
    } else {
      var2 = new VoidType(var3);
    }

    return (Type) var2;
  }

  public final Program parseProgram() {
    Program var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);

    try {
      List var3 = this.lineFinish();
      this.charStart(var2);
      var1 = new Program(var3, var2);
      if (this.T.kind != 39) {
        this.T("\"%\" unknown type", this.T.spelling);
      }

      return var1;
    } catch (SyntaxError var4) {
      return null;
    }
  }

  private final List lineFinish() throws SyntaxError {
    Decl var1 = null;
    Object var2 = null;
    Object var3 = null;
    SourcePosition var4 = new SourcePosition();
    this.charFinish(var4);
    if (this.T.kind == 9 || this.T.kind == 0 || this.T.kind == 7 || this.T.kind == 4) {
      Type var5 = this.charStart();
      Ident var6 = this.H();
      if (this.T.kind == 27) {
        var1 = this.lineStart(var5, var6);
      } else {
        var2 = this.position(var5, var6, true);
      }
    }

    if (this.T.kind != 9 && this.T.kind != 0 && this.T.kind != 7 && this.T.kind != 4) {
      var3 = new EmptyDeclList(this.charStart);
    } else {
      var3 = this.lineFinish();
    }

    if (var1 != null) {
      this.charStart(var4);
      var2 = new DeclList(var1, (List) var3, var4);
    } else if (var2 != null) {
      DeclList var7;
      for (var7 = (DeclList) var2; !(var7.DL instanceof EmptyDeclList); var7 = (DeclList) var7.DL) {
      }

      if (!(var3 instanceof EmptyDeclList)) {
        var7.DL = (DeclList) var3;
      }
    } else {
      var2 = var3;
    }

    return (List) var2;
  }

  private final Decl lineStart(Type var1, Ident var2) throws SyntaxError {
    FuncDecl var3 = null;
    new SourcePosition();
    SourcePosition var4 = var1.position;
    List var5 = this.F();
    Stmt var6 = this.getToken();
    this.charStart(var4);
    var3 = new FuncDecl(var1, var2, var5, var6, var4);
    return var3;
  }

  private final List position(Type var1, Ident var2, boolean var3) throws SyntaxError {
    List var4 = null;
    Object var5 = null;
    SourcePosition var7 = new SourcePosition();
    this.getToken(((Type) var1).position, var7);
    Object var6;
    if (this.T.kind == 29) {
      this.I();
      if (this.T.kind == 34) {
        var6 = this.L();
      } else {
        var6 = new EmptyExpr(this.charStart);
      }

      this.DL(30);
      this.charStart(var7);
      var1 = new ArrayType((Type) var1, (Expr) var6, var7);
    }

    if (this.T.kind == 17) {
      this.I();
      var6 = this.charFinish();
    } else {
      var6 = new EmptyExpr(this.charStart);
    }

    SourcePosition var8 = new SourcePosition();
    this.getToken(var2.position, var8);
    this.charStart(var8);
    if (var3) {
      var5 = new GlobalVarDecl((Type) var1, var2, (Expr) var6, var8);
    } else {
      var5 = new LocalVarDecl((Type) var1, var2, (Expr) var6, var8);
    }

    SourcePosition var9 = new SourcePosition();
    this.getToken(var2.position, var9);
    DeclList var10;
    if (this.T.kind == 32) {
      this.I();
      if (var1 instanceof ArrayType) {
        var1 = ((ArrayType) var1).T;
      }

      var4 = this.spell((Type) var1, var3);
      this.charStart(var9);
      var10 = new DeclList((Decl) var5, var4, var9);
    } else {
      this.charStart(var9);
      var10 = new DeclList((Decl) var5, new EmptyDeclList(this.charStart), var9);
    }

    this.DL(31);
    return var10;
  }

  private final List reportError() throws SyntaxError {
    List var1 = null;
    Type var2 = this.charStart();
    var1 = this.spell(var2, false);
    this.DL(31);
    return var1;
  }

  private final List spell(Type var1, boolean var2) throws SyntaxError {
    List var3 = null;
    SourcePosition var4 = new SourcePosition();
    this.charFinish(var4);
    var1 = this.kind(var1);
    Decl var5 = this.spelling(var1, var2);
    DeclList var6;
    if (this.T.kind == 32) {
      this.I();
      var3 = this.spell(var1, var2);
      this.charStart(var4);
      var6 = new DeclList(var5, var3, var4);
    } else {
      this.charStart(var4);
      var6 = new DeclList(var5, new EmptyDeclList(this.charStart), var4);
    }

    return var6;
  }

  private final Decl spelling(Type var1, boolean var2) throws SyntaxError {
    Object var3 = null;
    SourcePosition var4 = new SourcePosition();
    this.charFinish(var4);
    Parser$TypeAndIdent var5 = this.DL(var1);
    Object var6 = null;
    if (this.T.kind == 17) {
      this.I();
      var6 = this.charFinish();
    } else {
      var6 = new EmptyExpr(this.charStart);
    }

    this.charStart(var4);
    if (var2) {
      var3 = new GlobalVarDecl(var5.I, var5.Z, (Expr) var6, var4);
    } else {
      var3 = new LocalVarDecl(var5.I, var5.Z, (Expr) var6, var4);
    }

    return (Decl) var3;
  }

  private final Parser$TypeAndIdent DL(Type var1) throws SyntaxError {
    Object var2 = null;
    SourcePosition var3 = new SourcePosition();
    this.getToken(((Type) var1).position, var3);
    Ident var4 = this.H();
    if (this.T.kind == 29) {
      this.I();
      if (this.T.kind == 34) {
        SourcePosition var5 = new SourcePosition();
        this.charFinish(var5);
        IntLiteral var6 = this.X();
        this.charStart(var5);
        var2 = new IntExpr(var6, var5);
      } else {
        var2 = new EmptyExpr(this.charStart);
      }

      this.DL(30);
      this.charStart(var3);
      var1 = new ArrayType((Type) var1, (Expr) var2, var3);
    }

    return new Parser$TypeAndIdent(this, (Type) var1, var4);
  }

  private final List T() throws SyntaxError {
    SourcePosition var1 = new SourcePosition();
    this.charFinish(var1);
    List var2 = null;
    Expr var3 = this.L();
    ArrayExprList var4;
    if (this.T.kind == 32) {
      this.I();
      var2 = this.T();
      this.charStart(var1);
      var4 = new ArrayExprList(var3, var2, var1);
    } else {
      this.charStart(var1);
      var4 = new ArrayExprList(var3, new EmptyArrayExprList(this.charStart), var1);
    }

    return var4;
  }

  private final Expr charFinish() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    if (this.T.kind == 25) {
      this.I();
      List var3 = this.T();
      this.DL(26);
      this.charStart(var2);
      var1 = new ArrayInitExpr(var3, var2);
    } else {
      var1 = this.L();
    }

    return (Expr) var1;
  }

  private final Type charStart() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    if (this.T.kind == 9) {
      this.I();
      this.charStart(var2);
      var1 = new VoidType(var2);
    } else if (this.T.kind == 0) {
      this.I();
      this.charStart(var2);
      var1 = new BooleanType(var2);
    } else if (this.T.kind == 7) {
      this.I();
      this.charStart(var2);
      var1 = new IntType(var2);
    } else if (this.T.kind == 4) {
      this.I();
      this.charStart(var2);
      var1 = new FloatType(var2);
    } else {
      this.T("\"%\" illegal type (must be one of void, int, float and boolean)", this.T.spelling);
    }

    return (Type) var1;
  }

  private final Stmt getToken() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    this.DL(25);
    List var3 = this.kind();
    List var4 = this.lineStart();
    this.DL(26);
    this.charStart(var2);
    if (var3 instanceof EmptyDeclList && var4 instanceof EmptyStmtList) {
      var1 = new EmptyCompStmt(var2);
    } else {
      this.charStart(var2);
      var1 = new CompoundStmt(var3, var4, var2);
    }

    return (Stmt) var1;
  }

  private final List kind() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    if (this.T.kind == 7 || this.T.kind == 4 || this.T.kind == 0 || this.T.kind == 9) {
      var1 = this.reportError();
    }

    while (this.T.kind == 7 || this.T.kind == 4 || this.T.kind == 0 || this.T.kind == 9) {
      List var3 = this.reportError();

      DeclList var4;
      for (var4 = (DeclList) var1; !(var4.DL instanceof EmptyDeclList); var4 = (DeclList) var4.DL) {
      }

      var4.DL = (DeclList) var3;
    }

    if (var1 == null) {
      var1 = new EmptyDeclList(this.charStart);
    }

    return (List) var1;
  }

  private final List lineStart() throws SyntaxError {
    List var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    Object var4;
    if (this.T.kind != 26) {
      Stmt var3 = this.position();
      if (this.T.kind != 26) {
        var1 = this.lineStart();
        this.charStart(var2);
        var4 = new StmtList(var3, var1, var2);
      } else {
        this.charStart(var2);
        var4 = new StmtList(var3, new EmptyStmtList(this.charStart), var2);
      }
    } else {
      var4 = new EmptyStmtList(this.charStart);
    }

    return (List) var4;
  }

  private final Stmt position() throws SyntaxError {
    Object var1 = null;
    switch (this.T.kind) {
      case 1:
        var1 = this.Z();
        break;
      case 2:
        var1 = this.C();
        break;
      case 3:
      case 4:
      case 7:
      case 9:
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      default:
        var1 = this.D();
        break;
      case 5:
        var1 = this.spelling();
        break;
      case 6:
        var1 = this.spell();
        break;
      case 8:
        var1 = this.B();
        break;
      case 10:
        var1 = this.DL();
        break;
      case 25:
        var1 = this.getToken();
    }

    return (Stmt) var1;
  }

  private final Stmt spell() throws SyntaxError {
    IfStmt var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    this.DL(6);
    this.DL(27);
    Expr var3 = this.L();
    this.DL(28);
    Stmt var4 = this.position();
    if (this.T.kind == 3) {
      this.I();
      Stmt var5 = this.position();
      this.charStart(var2);
      var1 = new IfStmt(var3, var4, var5, var2);
    } else {
      this.charStart(var2);
      var1 = new IfStmt(var3, var4, var2);
    }

    return var1;
  }

  private final ForStmt spelling() throws SyntaxError {
    ForStmt var1 = null;
    Stmt var2 = null;
    Object var3 = null;
    Object var4 = null;
    Object var5 = null;
    SourcePosition var6 = new SourcePosition();
    this.DL(5);
    this.DL(27);
    if (this.T.kind != 31) {
      var3 = this.L();
    } else {
      var3 = new EmptyExpr(this.charStart);
    }

    this.DL(31);
    if (this.T.kind != 31) {
      var4 = this.L();
    } else {
      var4 = new EmptyExpr(this.charStart);
    }

    this.DL(31);
    if (this.T.kind != 28) {
      var5 = this.L();
    } else {
      var5 = new EmptyExpr(this.charStart);
    }

    this.DL(28);
    var2 = this.position();
    var1 = new ForStmt((Expr) var3, (Expr) var4, (Expr) var5, var2, var6);
    return var1;
  }

  private final Stmt DL() throws SyntaxError {
    Stmt var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    this.DL(10);
    this.DL(27);
    Expr var3 = this.L();
    this.DL(28);
    var1 = this.position();
    this.charStart(var2);
    WhileStmt var4 = new WhileStmt(var3, var1, var2);
    return var4;
  }

  private final Stmt Z() throws SyntaxError {
    BreakStmt var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    this.DL(1);
    this.DL(31);
    this.charStart(var2);
    var1 = new BreakStmt(var2);
    return var1;
  }

  private final Stmt C() throws SyntaxError {
    ContinueStmt var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    this.DL(2);
    this.DL(31);
    this.charStart(var2);
    var1 = new ContinueStmt(var2);
    return var1;
  }

  private final Stmt B() throws SyntaxError {
    ReturnStmt var1 = null;
    SourcePosition var3 = new SourcePosition();
    this.charFinish(var3);
    this.DL(8);
    Object var2;
    if (this.T.kind != 31) {
      var2 = this.L();
    } else {
      var2 = new EmptyExpr(this.charStart);
    }

    this.DL(31);
    this.charStart(var3);
    var1 = new ReturnStmt((Expr) var2, var3);
    return var1;
  }

  private final Stmt D() throws SyntaxError {
    ExprStmt var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    if (this.T.kind != 33 && this.T.kind != 15 && this.T.kind != 11 && this.T.kind != 12 && this.T.kind != 15 && this.T.kind != 34 && this.T.kind != 35 && this.T.kind != 36 && this.T.kind != 37 && this.T.kind != 27) {
      this.DL(31);
      this.charStart(var2);
      var1 = new ExprStmt(new EmptyExpr(this.charStart), var2);
    } else {
      Expr var3 = this.L();
      this.DL(31);
      this.charStart(var2);
      var1 = new ExprStmt(var3, var2);
    }

    return var1;
  }

  private final List F() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    this.DL(27);
    if (this.T.kind == 28) {
      this.I();
      this.charStart(var2);
      var1 = new EmptyParaList(var2);
    } else {
      var1 = this.J();
      this.DL(28);
    }

    return (List) var1;
  }

  private final List J() throws SyntaxError {
    List var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    ParaDecl var3 = this.S();
    ParaList var4;
    if (this.T.kind == 32) {
      this.I();
      var1 = this.J();
      this.charStart(var2);
      var4 = new ParaList(var3, var1, var2);
    } else {
      this.charStart(var2);
      var4 = new ParaList(var3, new EmptyParaList(this.charStart), var2);
    }

    return var4;
  }

  private final ParaDecl S() throws SyntaxError {
    ParaDecl var1 = null;
    Type var2 = this.charStart();
    SourcePosition var3 = new SourcePosition();
    this.charFinish(var3);
    Parser$TypeAndIdent var4 = this.DL(var2);
    this.charStart(var3);
    var1 = new ParaDecl(var4.I, var4.Z, var3);
    return var1;
  }

  private final List A() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    this.DL(27);
    if (this.T.kind == 28) {
      this.DL(28);
      this.charStart(var2);
      var1 = new EmptyArgList(var2);
    } else {
      var1 = this.E();
      this.DL(28);
    }

    return (List) var1;
  }

  private final List E() throws SyntaxError {
    List var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    Arg var3 = this.G();
    ArgList var4;
    if (this.T.kind == 32) {
      this.I();
      var1 = this.E();
      this.charStart(var2);
      var4 = new ArgList(var3, var1, var2);
    } else {
      this.charStart(var2);
      var4 = new ArgList(var3, new EmptyArgList(var2), var2);
    }

    return var4;
  }

  private final Arg G() throws SyntaxError {
    Arg var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    Expr var3 = this.L();
    this.charStart(var2);
    var1 = new Arg(var3, var2);
    return var1;
  }

  private final Ident H() throws SyntaxError {
    Ident var1 = null;
    if (this.T.kind == 33) {
      String var2 = this.T.spelling;
      this.I();
      var1 = new Ident(var2, this.charFinish);
    } else {
      this.T("identifier expected here", "");
    }

    return var1;
  }

  private final Operator K() {
    Operator var1 = null;
    String var2 = this.T.spelling;
    this.I();
    var1 = new Operator(var2, this.charFinish);
    return var1;
  }

  private final Expr L() throws SyntaxError {
    Expr var1 = null;
    var1 = this.M();
    return var1;
  }

  private final Expr M() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    var1 = this.N();
    if (this.T.kind == 17) {
      this.K();
      Expr var3 = this.M();
      this.charStart(var2);
      var1 = new AssignExpr((Expr) var1, var3, var2);
    }

    return (Expr) var1;
  }

  private final Expr N() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);

    Operator var3;
    Expr var4;
    SourcePosition var5;
    for (var1 = this.O(); this.T.kind == 24; var1 = new BinaryExpr((Expr) var1, var3, var4, var5)) {
      var3 = this.K();
      var4 = this.O();
      var5 = new SourcePosition();
      this.getToken(var2, var5);
      this.charStart(var5);
    }

    return (Expr) var1;
  }

  private final Expr O() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);

    Operator var3;
    Expr var4;
    SourcePosition var5;
    for (var1 = this.P(); this.T.kind == 23; var1 = new BinaryExpr((Expr) var1, var3, var4, var5)) {
      var3 = this.K();
      var4 = this.P();
      var5 = new SourcePosition();
      this.getToken(var2, var5);
      this.charStart(var5);
    }

    return (Expr) var1;
  }

  private final Expr P() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);

    Operator var3 = null;
    Expr var4 = null;
    SourcePosition var5 = null;
    for (var1 = this.Q(); this.T.kind == 18 || this.T.kind == 16; var1 = new BinaryExpr((Expr) var1, var3, var4, var5)) {
      var3 = this.K();
      var4 = this.Q();
      var5 = new SourcePosition();
      this.getToken(var2, var5);
      this.charStart(var5);
    }

    return (Expr) var1;
  }

  private final Expr Q() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);

    Operator var3 = null;
    Expr var4 = null;
    SourcePosition var5 = null;
    for (var1 = this.R(); this.T.kind == 19 || this.T.kind == 20 || this.T.kind == 21 || this.T.kind == 22; var1 = new BinaryExpr((Expr) var1, var3, var4, var5)) {
      var3 = this.K();
      var4 = this.R();
      var5 = new SourcePosition();
      this.getToken(var2, var5);
      this.charStart(var5);
    }

    return (Expr) var1;
  }

  private final Expr R() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);

    Operator var3 = null;
    Expr var4 = null;
    SourcePosition var5 = null;
    for (var1 = this.U(); this.T.kind == 11 || this.T.kind == 12; var1 = new BinaryExpr((Expr) var1, var3, var4, var5)) {
      var3 = this.K();
      var4 = this.U();
      var5 = new SourcePosition();
      this.getToken(var2, var5);
      this.charStart(var5);
    }

    return (Expr) var1;
  }

  private final Expr U() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);

    Operator var3 = null;
    Expr var4 = null;
    SourcePosition var5 = null;
    for (var1 = this.V(); this.T.kind == 13 || this.T.kind == 14; var1 = new BinaryExpr((Expr) var1, var3, var4, var5)) {
      var3 = this.K();
      var4 = this.V();
      var5 = new SourcePosition();
      this.getToken(var2, var5);
      this.charStart(var5);
    }

    return (Expr) var1;
  }

  private final Expr V() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    switch (this.T.kind) {
      case 11:
      case 12:
      case 15:
        Operator var3 = this.K();
        Expr var4 = this.V();
        this.charStart(var2);
        var1 = new UnaryExpr(var3, var4, var2);
        break;
      case 13:
      case 14:
      default:
        var1 = this.W();
    }

    return (Expr) var1;
  }

  private final Expr W() throws SyntaxError {
    Object var1 = null;
    SourcePosition var2 = new SourcePosition();
    this.charFinish(var2);
    switch (this.T.kind) {
      case 27:
        this.I();
        var1 = this.L();
        this.DL(28);
        break;
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      default:
        this.T("illegal primary expression", this.T.spelling);
        break;
      case 33:
        Ident var3 = this.H();
        if (this.T.kind == 27) {
          List var8 = this.A();
          this.charStart(var2);
          var1 = new CallExpr(var3, var8, var2);
        } else {
          SimpleVar var9;
          if (this.T.kind == 29) {
            this.charStart(var2);
            var9 = new SimpleVar(var3, var2);
            this.I();
            Expr var10 = this.L();
            this.charStart(var2);
            var1 = new ArrayExpr(var9, var10, var2);
            this.DL(30);
          } else {
            this.charStart(var2);
            var9 = new SimpleVar(var3, var2);
            var1 = new VarExpr(var9, var2);
          }
        }
        break;
      case 34:
        IntLiteral var4 = this.X();
        this.charStart(var2);
        var1 = new IntExpr(var4, var2);
        break;
      case 35:
        FloatLiteral var5 = this.Y();
        this.charStart(var2);
        var1 = new FloatExpr(var5, var2);
        break;
      case 36:
        BooleanLiteral var6 = this.i();
        this.charStart(var2);
        var1 = new BooleanExpr(var6, var2);
        break;
      case 37:
        StringLiteral var7 = this.z();
        this.charStart(var2);
        var1 = new StringExpr(var7, var2);
    }

    return (Expr) var1;
  }

  private final IntLiteral X() throws SyntaxError {
    IntLiteral var1 = null;
    if (this.T.kind == 34) {
      String var2 = this.T.spelling;
      this.I();
      var1 = new IntLiteral(var2, this.charFinish);
    } else {
      this.T("integer literal expected here", "");
    }

    return var1;
  }

  private final FloatLiteral Y() throws SyntaxError {
    FloatLiteral var1 = null;
    if (this.T.kind == 35) {
      String var2 = this.T.spelling;
      this.I();
      var1 = new FloatLiteral(var2, this.charFinish);
    } else {
      this.T("float literal expected here", "");
    }

    return var1;
  }

  private final BooleanLiteral i() throws SyntaxError {
    BooleanLiteral var1 = null;
    if (this.T.kind == 36) {
      String var2 = this.T.spelling;
      this.I();
      var1 = new BooleanLiteral(var2, this.charFinish);
    } else {
      this.T("string literal expected here", "");
    }

    return var1;
  }

  private final StringLiteral z() throws SyntaxError {
    StringLiteral var1 = null;
    if (this.T.kind == 37) {
      this.charFinish = this.T.position;
      String var2 = this.T.spelling;
      var1 = new StringLiteral(var2, this.charFinish);
      this.T = this.DL.getToken();
    } else {
      this.T("string literal expected here", "");
    }

    return var1;
  }
}
