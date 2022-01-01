package lemon.jpizza;

import lemon.jpizza.compiler.Compiler;
import lemon.jpizza.compiler.FunctionType;
import lemon.jpizza.compiler.values.Var;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.compiler.vm.VMResult;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.Error;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.generators.Lexer;
import lemon.jpizza.generators.Optimizer;
import lemon.jpizza.generators.Parser;
import lemon.jpizza.libraries.*;
import lemon.jpizza.libraries.httpretzel.HTTPretzel;
import lemon.jpizza.libraries.jdraw.JDraw;
import lemon.jpizza.libraries.pdl.SafeSocks;
import lemon.jpizza.libraries.socks.SockLib;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.TreePrinter;
import lemon.jpizza.nodes.expressions.BodyNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.ParseResult;
import lemon.jpizza.results.RTResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Shell {

    public static final Logger logger = new Logger();
    public static final SymbolTable globalSymbolTable = new SymbolTable();
    public static String root;
    public static VM vm;
    public static Map<String, Var> globals;
    public static final String fileEncoding = System.getProperty("file.encoding");

    public static String[] getFNDirs(String dir) {
        int ind = dir.lastIndexOf('\\');
        if (ind == -1)
            return new String[]{dir, "."};
        return new String[]{
                dir.substring(ind),
                dir.substring(0, ind)
        };
    }

    public static void initLibs() {
        // Load librarys
        BuiltInFunction.initialize();
        SysLib.initialize();
        JGens.initialize();
        GUIs.initialize();
        FileLib.initialize();
        SockLib.initialize();
        HTTPLIB.initialize();
        JasonLib.initialize();
        JDraw.initialize();
        HTTPretzel.initialize();
        Time.initialize();
        SafeSocks.initialize();
    }

    @SuppressWarnings("DuplicatedCode")
    public static void main(String[] args) throws IOException {
        root = System.getenv("JPIZZA_DATA_DIR") == null ? System.getProperty("user.home") + "/.jpizza" : System.getenv("JPIZZA_DATA_DIR");
        initLibs();

        PList cmdargs = new PList(new ArrayList<>());
        for (String arg : args) {
            cmdargs.append(new Str(arg));
        }

        if (args.length != 2 && args.length > 0) {
            if (args[0].equals("help")) {
                Shell.logger.outln("""
                        jpizza        ->   Open venv
                        jpizza help   ->   List commands
                        jpizza docs   ->   Link to documentation
                        
                        jpizza <file> ->            Run file
                        jpizza <file> --compile ->  Compile file
                        jpizza <file> --refactor -> Run file with refactoring tips
                        """);
            }
            else if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    Pair<JFunc, Error> res = compile(fn, scrpt);
                    if (res.b != null)
                        Shell.logger.fail(res.b.asString());
                    runCompiled(fn, res.a, args);
                }
                else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            else if (args[0].equals("docs")) {
                Shell.logger.outln("Documentation: https://bit.ly/3vM8G0a");
            }
            else if (args[0].endsWith(".jbox")) {
                if (Files.exists(Path.of(args[0]))) {
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    Error res = runCompiled(fn, args[0], args);
                    if (res != null)
                        Shell.logger.fail(res.asString());
                }
                else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            return;
        }

        if (args.length == 2) {
            if (args[0].endsWith(".devp")) {
                if (Files.exists(Path.of(args[0]))) {
                    String scrpt = Files.readString(Path.of(args[0]));
                    String dir = Path.of(args[0]).toString();
                    String[] dsfn = getFNDirs(dir);
                    String fn = dsfn[0]; String newDir = dsfn[1];
                    System.setProperty("user.dir", newDir);
                    switch (args[1]) {
                        case "--compile" -> {
                            Error res = compile(fn, scrpt,
                                    newDir + "\\" + fn.substring(0, fn.length() - 5) + ".jbox");
                            if (res != null) {
                                Shell.logger.fail(res.asString());
                            }
                            return;
                        }
                        case "--refactor" -> logger.enableTips();
                    }
                    Pair<JFunc, Error> res = compile(args[0], scrpt);
                    if (res.b != null) {
                        Shell.logger.fail(res.b.asString());
                        return;
                    }
                    runCompiled(args[0], res.a, args);
                    return;
                }
                else {
                    Shell.logger.outln("File does not exist.");
                }
            }
            return;
        }

        repl(args);

    }

    public static void repl(String[] args) {
        Scanner in = new Scanner(System.in);

        Shell.logger.outln("Exit with 'quit'");
        Shell.logger.enableTips();

        while (true) {
            Shell.logger.out("-> ");
            String input = in.nextLine() + ";";

            if (input.equals("quit;"))
                break;
            //  compile("<shell>", input, "shell.jbox");
            Pair<JFunc, Error> a = compile("<shell>", input);
            if (a.b != null) {
                Shell.logger.fail(a.b.asString());
            }
            else {
                runCompiled("<shell>", a.a, new String[]{"<shell>"});
            }
        }
        in.close();
    }

    public static Pair<List<Node>, Error> getAst(String fn, String text) {
        Lexer lexer = new Lexer(fn, text);
        Pair<List<Token>, Error> x = lexer.make_tokens();
        List<Token> tokens = x.a;
        Error error = x.b;
        if (error != null)
            return new Pair<>(null, error);
        Parser parser = new Parser(tokens);
        ParseResult<Node> ast = parser.parse();
        if (ast.error != null)
            return new Pair<>(null, ast.error);
        Shell.logger.debug(TreePrinter.print(ast.node));
        BodyNode body = (BodyNode) Optimizer.optimize(ast.node);
        return new Pair<>(body.statements, null);
    }

    public static Pair<Obj, Error> run(String fn, String text, boolean log) {
        return run(fn, text, true, log);
    }
    public static Pair<Obj, Error> run(String fn, String text, boolean main, boolean log) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        Context context = new Context(fn, null, null);
        context.symbolTable = globalSymbolTable;
        Interpreter inter = new Interpreter();
        if (main) inter.makeMain();
        RTResult result;
        try {
            result = inter.interpret(ast.a, context, log);
            if (result.error != null) return new Pair<>(result.value, result.error);
            result.register(inter.finish(context));
        } catch (OutOfMemoryError e) {
            return new Pair<>(null, RTError.Internal(
                    null, null,
                    "Out of memory",
                    context
            ));
        }
        return new Pair<>(result.value, result.error);
    }

    public static Pair<JFunc, Error> compile(String fn, String text) {
        return compile(fn, text, false);
    }

    public static Pair<JFunc, Error> compile(String fn, String text, boolean scope) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        List<Node> outNode = ast.a;

        Compiler compiler = new Compiler(FunctionType.Script, text);

        if (scope)
            compiler.beginScope();
        JFunc func = compiler.compileBlock(outNode);
        if (scope)
            compiler.endScope(ast.a.get(0).pos_start, ast.a.get(ast.a.size() - 1).pos_end);

        return new Pair<>(func, null);
    }

    public static Error compile(String fn, String text, String outpath) {
        Pair<JFunc, Error> res = compile(fn, text);
        if (res.b != null) return res.b;
        JFunc func = res.a;

        try {
            FileOutputStream fout;
            fout = new FileOutputStream(outpath);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(func);
            oos.close();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
            return RTError.Internal(
                    null, null,
                    "Could not write to file",
                    null
            );
        }

        return null;
    }

    public static JFunc load(String inpath) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(inpath);
        } catch (FileNotFoundException e) {
            Shell.logger.fail(RTError.FileNotFound(null, null,
                    "File does not exist!\n" + inpath, null).asString());
            return null;
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object ost = ois.readObject();
            ois.close();
            fis.close();

            if (!(ost instanceof JFunc)) {
                Shell.logger.fail(RTError.FileNotFound(null, null,
                        "File is not JPizza bytecode!" + inpath, null).asString());
                return null;
            }

            return (JFunc) ost;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void runCompiled(String fn, JFunc func, String[] args) {
        vm = new VM(func).trace(fn);
        VMResult res = vm.run();
        if (res == VMResult.ERROR) return;
        vm.finish(args);
    }

    public static Error runCompiled(String fn, String inpath, String[] args) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(inpath);
        } catch (FileNotFoundException e) {
            return RTError.FileNotFound(null, null,
                    "File does not exist!\n" + inpath, null);
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object ost = ois.readObject();
            ois.close();
            fis.close();
            if (!(ost instanceof JFunc)) return RTError.FileNotFound(null, null,
                    "File is not JPizza bytecode!", null);

            JFunc func = (JFunc) ost;
            vm = new VM(func).trace(fn);

            VMResult res = vm.run();
            if (res == VMResult.ERROR) return null;
            vm.finish(args);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Another public static 
    public static Pair<ClassInstance, Error> imprt(String fn, String text) {
        Pair<List<Node>, Error> ast = getAst(fn, text);
        if (ast.b != null) return new Pair<>(null, ast.b);
        Context context = new Context(fn, null, null);
        context.symbolTable = new SymbolTable(globalSymbolTable);
        RTResult result = new Interpreter().interpret(ast.a, context, false);
        return new Pair<>(new ClassInstance(context, fn), result.error);
    }

}
