package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseFunction extends Value {
    public String name;
    public BaseFunction(String name) {
        super();
        this.name = name != null ? name : "<anonymous>";
        jptype = Constants.JPType.BaseFunction;
    }

    // Functions

    public Context newContext() {
        Context newContext = new Context(name, context, pos_start);
        newContext.symbolTable = new SymbolTable(newContext.parent.symbolTable);
        return newContext;
    }

    public RTResult checkArgs(List<Obj> defaults, List<String> argTypes, HashMap<String, String> genericKey,
                              List<Obj> args, int minArgs, int maxArgs) {
        RTResult res = new RTResult();

        int size = args.size();
        if (size > maxArgs) return res.failure(new RTError(
                pos_start, pos_end,
                String.format("%s too many args passed into '%s'", args.size() - maxArgs, name),
                context
        ));
        if (size < minArgs) return res.failure(new RTError(
                pos_start, pos_end,
                String.format("%s too few args passed into '%s'", minArgs - args.size(), name),
                context
        ));

        int tSize = argTypes.size();
        for (int i = 0; i < tSize; i++) {
            String type = argTypes.get(i);
            String generictype = genericKey.get(type);
            if (type.equals("any") || (generictype != null && generictype.equals("any"))) continue;

            Obj arg;
            if (i >= size)
                arg = defaults.get(i - size);
            else
                arg = args.get(i);

            Obj oType = arg.type().astring();
            if (oType.jptype != Constants.JPType.String) return res.failure(new RTError(
                    arg.get_start(), arg.get_end(),
                    "Type is not a string",
                    arg.get_ctx()
            ));

            String oT = ((Str) oType).trueValue();
            if (!oT.equals(type) && (generictype == null || !generictype.equals(oT))) return res.failure(new RTError(
                    arg.get_start(), arg.get_end(),
                    String.format("Expected type %s, got %s", generictype != null ? generictype: type, oT),
                    arg.get_ctx()
            ));

        }

        return res.success(null);
    }

    public void populateArgs(List<String> argNames, List<Obj> args, List<Obj> defaults, Context execCtx) {
        int size = argNames.size();
        int aSize = args.size();
        for (int i = 0; i < size; i++) {
            Obj argValue;
            if (i >= aSize)
                argValue = defaults.get(i - aSize);
            else
                argValue = args.get(i);
            argValue.set_context(execCtx);
            String argName = argNames.get(i);
            execCtx.symbolTable.define(argName, argValue);
        }
    }

    public RTResult checkPopArgs(List<String> argNames, List<String> argTypes, List<Obj> args, Context execCtx,
                                 List<Obj> defaults, int minArgs, int maxArgs, HashMap<String, String> genericKey) {
        RTResult res = new RTResult();
        res.register(checkArgs(defaults, argTypes, genericKey, args, minArgs, maxArgs));
        if (res.shouldReturn())
            return res;
        populateArgs(argNames, args, defaults, execCtx);
        if (res.shouldReturn())
            return res;
        return res.success(null);
    }


    // Methods

    // Conversions

    // Defaults

    public Obj copy() { return new BaseFunction(name).set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("<base-function>").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<base-function>"; }
    public boolean isAsync() { return false; }
    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        return new RTResult().success(new Null());
    }
}