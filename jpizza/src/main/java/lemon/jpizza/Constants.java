package lemon.jpizza;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Constants {
    public static final char[] NUMBERS = "0123456789".toCharArray();
    public static final char[] NUMDOT = "0123456789.".toCharArray();
    public static final char[] LETTERS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final char[] LETTERS_DIGITS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();
    public static final List<Tokens.TT> TYPETOKS = Arrays.asList(
            Tokens.TT.IDENTIFIER,
            Tokens.TT.KEYWORD,
            Tokens.TT.FLOAT,
            Tokens.TT.INT,
            Tokens.TT.LPAREN,
            Tokens.TT.RPAREN,
            Tokens.TT.LSQUARE,
            Tokens.TT.RSQUARE,
            Tokens.TT.OPEN,
            Tokens.TT.CLOSE
    );
    public static final String[] KEYWORDS = {
            "free",
            "assert",
            "let",
            "throw",
            "struct",
            "do",
            "loop",
            "pass",
            "cal",
            "yourmom",
            "async",
            "import",
            "scope",
            "as",
            "extend",
            "bin",
            "function",
            "attr",
            "bake",
            "const",
            "var",
            "for",
            "while",
            "null",
            "if",
            "elif",
            "else",
            "return",
            "continue",
            "break",
            "method",
            "mthd",
            "md",
            "ingredients",
            "recipe",
            "class",
            "obj",
            "switch",
            "case",
            "fn",
            "enum",
            "default",
            "match",
            "pub",
            "prv",
            "static",
            "stc"
    };
    @SuppressWarnings("unused") public static char BREAK = ';';
    @SuppressWarnings("unused") public static char[] IGNORE = new char[]{' ', '\n', '\t'};
    public static final Map<String, Context> LIBRARIES = new HashMap<>();
    public static final char splitter = '\n';
    
    public static final Map<Tokens.TT, Operations.OP> tto = new HashMap<>(){{
        put(Tokens.TT.PLUS, Operations.OP.ADD);
        put(Tokens.TT.MINUS, Operations.OP.SUB);
        put(Tokens.TT.MUL, Operations.OP.MUL);
        put(Tokens.TT.DIV, Operations.OP.DIV);
        put(Tokens.TT.POWER, Operations.OP.FASTPOW);
        put(Tokens.TT.EE, Operations.OP.EQ);
        put(Tokens.TT.NE, Operations.OP.NE);
        put(Tokens.TT.LT, Operations.OP.LT);
        put(Tokens.TT.LTE, Operations.OP.LTE);
        put(Tokens.TT.AND, Operations.OP.INCLUDING);
        put(Tokens.TT.OR, Operations.OP.ALSO);
        put(Tokens.TT.MOD, Operations.OP.MOD);
        put(Tokens.TT.DOT, Operations.OP.GET);
        put(Tokens.TT.LSQUARE, Operations.OP.BRACKET);
    }};

    public static final Map<String, JPType> methTypes = new HashMap<>(){{
        put("eq", JPType.Boolean);
        put("lt", JPType.Boolean);
        put("lte", JPType.Boolean);
        put("ne", JPType.Boolean);
        put("also", JPType.Boolean);
        put("including", JPType.Boolean);

        put("type", JPType.String);
    }};

    public enum JPType {
        Pattern,
        Bytes,
        ClassInstance,
        ClassPlate,
        CMethod,
        Function,
        Library,
        BaseFunction,
        AttrAssign,
        ClassDef,
        DynAssign,
        FuncDef,
        MethDef,
        VarAssign,
        Break,
        Call,
        Claccess,
        Continue,
        For,
        Import,
        Extend,
        Iter,
        Pass,
        Query,
        Return,
        Use,
        While,
        BinOp,
        UnaryOp,
        Boolean,
        Dict,
        List,
        Null,
        Number,
        String,
        Value,
        AttrAccess,
        Attr,
        VarAccess,
        Var,
        Generic,
        Switch,
        Enum,
        EnumChild,
        Res,
        Assert,
        Spread,
    }

    public static int nonWhitespace(String string){
        char[] characters = string.toCharArray();
        for(int i = 0; i < string.length(); i++){
            if(!Character.isWhitespace(characters[i])){
                return i;
            }
        }
        return 0;
    }

    public static String stringWithArrows(String text, Position pos_start, Position pos_end) {
        StringBuilder result = new StringBuilder();

        int idxStart = Math.max(0, text.lastIndexOf(splitter, pos_start.tidx));
        int idxEnd = text.indexOf(splitter, idxStart + 1);

        if (idxEnd < 0) idxEnd = text.length();

        int line_count = pos_end.ln - pos_start.ln + 1;
        int offs = 0;
        int colStart, colEnd, dist;
        for (int i = 0; i < line_count; i++) {
            String line = text.substring(idxStart, idxEnd);

            colStart = i == 0 ? pos_start.tcol : nonWhitespace(line);
            colEnd = i == line_count - 1 ? pos_end.tcol : line.length() - 1;
            dist = colEnd - colStart;

            String grouping;
            if (dist >= 2) {
                if (Shell.fileEncoding.equals("UTF-8"))
                    grouping = "╰" + "─".repeat(colEnd - colStart - 2) + "╯";
                else
                    grouping = "\\" + "_".repeat(colEnd - colStart - 2) + "/";
            } else {
                grouping = "^";
            }

            result.append(line).append("\n")
                    .append(" ".repeat(Math.max(0, colStart + offs))).append(grouping);

            idxStart = idxEnd;
            idxEnd = text.indexOf(splitter, idxStart + 1);

            if (idxEnd < 0) idxEnd = text.length();
        }

        return result.toString().replace("\t", "");
    }

    public static Obj getFromValue(Object val) {
        if (val instanceof String)
            return new Str((String) val);
        else if (val instanceof Double)
            return new Num((double) val, false);
        else if (val instanceof List) {
            List<Obj> lst = new ArrayList<>();
            List<Object> list = (List<Object>) val;

            for (Object item : list)
                lst.add(getFromValue(item));

            return new PList(lst);
        }
        else if (val instanceof Map) {
            Map<Obj, Obj> mp = new HashMap<>();
            Map<Object, Object> map = (Map<Object, Object>) val;

            for (Object key : map.keySet())
                mp.put(getFromValue(key), getFromValue(map.get(key)));

            return new Dict(mp);
        }
        else if (val instanceof byte[])
            return new Bytes((byte[]) val);
        else if (val instanceof Boolean)
            return new Bool((boolean) val);
        else return new Null();
    }

    public static byte[] objToBytes(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public static Object toObject(Obj obj) {
        if (obj.jptype == JPType.Dict) {
            Map<Object, Object> objMap = new ConcurrentHashMap<>();
            ConcurrentHashMap<Obj, Obj> deMap = obj.map;

            for (Obj k : deMap.keySet())
                objMap.put(toObject(k), toObject(deMap.get(k)));

            return objMap;
        }
        else if (obj.jptype == JPType.List) {
            List<Object> objLst = new ArrayList<>();
            List<Obj> olst = new ArrayList<>(obj.list);

            for (int i = 0; i < olst.size(); i++)
                objLst.add(toObject(olst.get(i)));

            return objLst;
        }
        else if (obj.jptype == JPType.Generic) {
            return obj.value;
        }
        return null;
    }

}
