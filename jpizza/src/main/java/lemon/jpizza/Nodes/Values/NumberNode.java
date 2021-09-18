package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

public class NumberNode extends ValueNode {
    public double val;
    public boolean flt;
    public boolean hex;

    public NumberNode(Token tok) {
        super(tok);
        val = (double) tok.value;
        flt = tok.type == Tokens.TT.FLOAT;
        hex = false;
        jptype = Constants.JPType.Number;
    }

    public NumberNode(Token tok, boolean hex) {
        super(tok);
        val = (double) tok.value;
        flt = tok.type == Tokens.TT.FLOAT;
        this.hex = hex;
        jptype = Constants.JPType.Number;
    }

    public NumberNode(int v, Position pos_start, Position pos_end) {
        super(new Token(Tokens.TT.IDENTIFIER, "null", pos_start, pos_end));
        val = v;
        hex = true;
        flt = false;
        jptype = Constants.JPType.Number;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Num(val, flt, hex).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}