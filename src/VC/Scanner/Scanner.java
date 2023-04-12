//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package VC.Scanner;

import VC.ErrorReporter;

public final class Scanner {
    private SourceFile sourceFile;
    private ErrorReporter errorReporter;
    private boolean debug;
    private StringBuffer currentSpelling;
    private char currentChar;
    private Token tokenInspected;
    private boolean currentlyScanningToken;
    private SourcePosition sourcePos;
    private int lineNumber;
    private int charStart;
    private int charFinish;
    private char[] escapeChars = new char[]{'b', 'f', 'n', 'r', 't', '\'', '"', '\\'};
    private char[] escapeAndChars = new char[]{'\b', '\f', '\n', '\r', '\t', '\'', '"', '\\'};

    public Scanner(SourceFile var1, ErrorReporter var2) {
        this.sourceFile = var1;
        this.errorReporter = var2;
        this.tokenInspected = null;
        this.lineNumber = 1;
        this.charStart = 0;
        this.debug = false;
        this.currentlyScanningToken = false;
        this.accept();
    }

    public void enableDebugging() {
        this.debug = true;
    }

    public Token inspectNextToken() {
        this.tokenInspected = this.getToken();
        return this.tokenInspected;
    }

    private void accept() {
        if (this.currentlyScanningToken) {
            this.currentSpelling.append(this.currentChar);
        }

        this.getNextChar();
    }

    private void getNextChar() {
        this.currentChar = this.sourceFile.getNextChar();
        this.charFinish = this.charStart;
        if (this.currentChar == '\n') {
            ++this.lineNumber;
            this.charStart = 0;
        } else if (this.currentChar == '\t') {
            this.charStart += 8 - this.charStart % 8;
        } else {
            ++this.charStart;
        }

    }

    private char inspectChar(int var1) {
        return this.sourceFile.inspectChar(var1);
    }

    void tokenError(String var1, String var2) {
        this.errorReporter.reportError(var1, var2, this.sourcePos);
    }

    private int nextToken() {
        switch (this.currentChar) {
            case '\u0000':
                this.currentSpelling.append(Token.spell(39));
                this.charFinish = this.charStart;
                return 39;
            case '!':
                this.accept();
                if (this.currentChar == '=') {
                    this.accept();
                    return 16;
                }

                return 15;
            case '"':
                return this.parseStringLiteral();
            case '&':
                this.accept();
                if (this.currentChar == '&') {
                    this.accept();
                    return 23;
                }

                return 38;
            case '(':
                this.accept();
                return 27;
            case ')':
                this.accept();
                return 28;
            case '*':
                this.accept();
                return 13;
            case '+':
                this.accept();
                return 11;
            case ',':
                this.accept();
                return 32;
            case '-':
                this.accept();
                return 12;
            case '.':
                if (Character.isDigit(this.inspectChar(1))) {
                    return this.parseFloatFraction();
                }

                this.accept();
                return 38;
            case '/':
                this.accept();
                return 14;
            case ';':
                this.accept();
                return 31;
            case '<':
                this.accept();
                if (this.currentChar == '=') {
                    this.accept();
                    return 20;
                }

                return 19;
            case '=':
                this.accept();
                if (this.currentChar == '=') {
                    this.accept();
                    return 18;
                }

                return 17;
            case '>':
                this.accept();
                if (this.currentChar == '=') {
                    this.accept();
                    return 22;
                }

                return 21;
            case '[':
                this.accept();
                return 29;
            case ']':
                this.accept();
                return 30;
            case '{':
                this.accept();
                return 25;
            case '|':
                this.accept();
                if (this.currentChar == '|') {
                    this.accept();
                    return 24;
                }

                return 38;
            case '}':
                this.accept();
                return 26;
            default:
                if (this.isVCIdentifierStart(this.currentChar)) {
                    return this.parseIdentifier();
                } else if (Character.isDigit(this.currentChar)) {
                    return this.parseIntOrFloatLiteral();
                } else {
                    this.accept();
                    return 38;
                }
        }
    }

    int parseIntOrFloatLiteral() {
        while(Character.isDigit(this.currentChar)) {
            this.accept();
        }

        if (this.currentChar == '.') {
            return this.parseFloatFraction();
        } else if (this.currentChar != 'e' && this.currentChar != 'E') {
            return 34;
        } else {
            return this.parseFloatExponent(34);
        }
    }

    int parseFloatFraction() {
        this.accept();

        while(Character.isDigit(this.currentChar)) {
            this.accept();
        }

        return this.currentChar != 'e' && this.currentChar != 'E' ? 35 : this.parseFloatExponent(35);
    }

    int parseFloatExponent(int var1) {
        char var2 = this.inspectChar(1);
        char var3 = this.inspectChar(2);
        if (!Character.isDigit(var2) && (var2 != '+' && var2 != '-' || !Character.isDigit(var3))) {
            return var1;
        } else {
            this.accept();
            if (this.currentChar == '+' || this.currentChar == '-') {
                this.accept();
            }

            while(Character.isDigit(this.currentChar)) {
                this.accept();
            }

            return 35;
        }
    }

    int parseStringLiteral() {
        boolean var1 = false;
        this.getNextChar();

        while(var1 || this.currentChar != '"') {
            if (this.currentChar == '\n') {
                this.sourcePos.charFinish = this.sourcePos.charStart;
                this.tokenError("%: unterminated string", this.currentSpelling.toString());
                return 37;
            }

            if (var1) {
                var1 = false;
                int var2 = this.escapeCharPos(this.currentChar);
                if (var2 >= 0) {
                    this.currentChar = this.escapeAndChars[var2];
                    if (this.currentChar == '\f') {
                        System.out.println("formfeed =" + this.currentChar);
                    }

                    this.accept();
                } else {
                    StringBuffer var3 = new StringBuffer("\\");
                    var3.append(this.currentChar);
                    this.sourcePos.charFinish = this.charFinish;
                    this.tokenError("%: illegal escape character", var3.toString());
                    this.currentSpelling.append('\\');
                    this.accept();
                }
            } else if (this.currentChar == '\\') {
                var1 = true;
                this.getNextChar();
            } else {
                this.accept();
            }
        }

        this.getNextChar();
        return 37;
    }

    int escapeCharPos(char var1) {
        for(int var2 = 0; var2 < this.escapeChars.length; ++var2) {
            if (this.escapeChars[var2] == var1) {
                return var2;
            }
        }

        return -1;
    }

    boolean isVCIdentifierStart(char var1) {
        return var1 == '_' || var1 >= 'A' && var1 <= 'Z' || var1 >= 'a' && var1 <= 'z';
    }

    boolean isVCIdentifierPart(char var1) {
        return var1 == '_' || var1 >= 'A' && var1 <= 'Z' || var1 >= 'a' && var1 <= 'z' || var1 >= '0' && var1 <= '9';
    }

    int parseIdentifier() {
        while(this.isVCIdentifierPart(this.currentChar)) {
            this.accept();
        }

        if (this.currentSpelling.toString().compareTo("true") != 0 && this.currentSpelling.toString().compareTo("false") != 0) {
            return 33;
        } else {
            return 36;
        }
    }

    void skipSpaceAndComments() {
        this.currentlyScanningToken = false;

        label63:
        while(this.currentChar == ' ' || this.currentChar == '\n' || this.currentChar == '\r' || this.currentChar == '\t' || this.currentChar == '/') {
            if (this.currentChar != '/') {
                this.accept();
            } else if (this.inspectChar(1) == '/') {
                while(this.currentChar != '\n') {
                    this.accept();
                }
            } else {
                if (this.inspectChar(1) != '*') {
                    break;
                }

                this.sourcePos = new SourcePosition();
                this.sourcePos.lineStart = this.sourcePos.lineFinish = this.lineNumber;
                this.sourcePos.charStart = this.sourcePos.charFinish = this.charStart;
                this.accept();

                do {
                    do {
                        this.accept();
                    } while(this.currentChar != '*' && this.currentChar != 0);

                    if (this.currentChar == 0) {
                        this.tokenError("%: unterminated comment", "");
                        continue label63;
                    }

                    do {
                        this.accept();
                    } while(this.currentChar == '*' && this.currentChar != 0);

                    if (this.currentChar == 0) {
                        this.tokenError("%: unterminated comment", "");
                        continue label63;
                    }
                } while(this.currentChar != '/');

                this.accept();
            }
        }

        this.currentlyScanningToken = true;
    }

    public Token getToken() {
        if (this.tokenInspected != null) {
            Token var3 = this.tokenInspected;
            this.tokenInspected = null;
            return var3;
        } else {
            this.skipSpaceAndComments();
            this.currentSpelling = new StringBuffer("");
            this.sourcePos = new SourcePosition();
            this.sourcePos.lineStart = this.sourcePos.lineFinish = this.lineNumber;
            this.sourcePos.charStart = this.charStart;
            int var2 = this.nextToken();
            this.sourcePos.charFinish = this.charFinish;
            Token var1 = new Token(var2, this.currentSpelling.toString(), this.sourcePos);
            if (this.debug) {
                System.out.println(var1);
            }

            return var1;
        }
    }
}
